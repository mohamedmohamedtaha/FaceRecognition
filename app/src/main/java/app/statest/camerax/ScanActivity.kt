package app.statest.camerax

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import app.statest.camerax.database.CameraVieWModel
import app.statest.camerax.databinding.ActivityScanBinding
import app.statest.camerax.model.User
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val VIBRATE_PATTERN = 500L

@AndroidEntryPoint
class ScanActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScanBinding
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private lateinit var cameraExcutor: ExecutorService
    private val cameraVieWModel by viewModels<CameraVieWModel>()
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    //    private var inputImage: InputImage? = null
    var data = ""
    private var textFace = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //Request Camera permission
        data = intent.getStringExtra("DATA").toString()
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
        cameraExcutor = Executors.newSingleThreadExecutor()


        cameraVieWModel.getUserLiveData.observe(this) {
            if (it != null) {
                binding.root.showSnackBar("Found face", true)
            } else {
                val user = User()
                user.userId = Utilities.getStampTimeId()
                user.userName = "Moahmed"
                user.userFace = textFace
                if (cameraVieWModel.saveUser(user)) {
                    val intent = Intent()
                    intent.putExtra("input", user.userFace)
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                }
            }
        }

    }

    private fun captureVideo() {
    }

    private fun takePhoto() {
        //Get a stable reference of the modifiable image capture use case
        //This will be null If we tap the photo button before image capture is set up.
        val imageCapture = imageCapture ?: return

        //Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }
        //Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
        ).build()

        // Set up image capture listener, which is triggered after photo has been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val msg = "Photo capture succeeded: ${outputFileResults.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                }

                override fun onError(exception: ImageCaptureException) {
                    val msg = "Photo capture failed: ${exception.message}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({

            // Used to bind the lifecycle of camera to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            //Preview
            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(binding.viewFinder.surfaceProvider) }

            imageCapture = ImageCapture.Builder().build()
            val imageAnalyzer = ImageAnalysis.Builder().build()
//                .also {
//                    it.setAnalyzer(
//                        cameraExcutor,
//                        LuminosityAnalyzer { luma -> Log.e("TAG", "Average Luminosit: $luma") })
//                }
//                .also {
//                    it.setAnalyzer(
//                        cameraExcutor,
//                        YourImageAnalyzer { luma -> Log.e("TAG", "image : ${luma.planes}")
//                            runTextRecognition()})
//                }
                .also {
                    it.setAnalyzer(
                        cameraExcutor
                    ) { image: ImageProxy ->
                        val mediaImage = image.image
                        if (mediaImage != null) {
                            val inputImage = InputImage.fromMediaImage(
                                mediaImage,
                                image.imageInfo.rotationDegrees
                            )
                            //Pass image to an ML Kit Vision API
                            if (inputImage != null) {
                                if (data == "1")
                                    runNumberRecognition(image, inputImage)
                                else if (data == "2") {
                                    runFaceContourDetection(image, inputImage)
                                } else {
                                    runTextRecognition(image, inputImage)
                                }
                            }
                        }
//                        image.close()
                    }
                }
            //Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                //Unbind use cases before rebinding
                cameraProvider.unbindAll()
                //Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalyzer
                )
            } catch (e: Exception) {
                Log.e("TAG", "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    fun onTouch(x: Float, y: Float, previewView: PreviewView, cameraSelector: CameraSelector) {
        //Create a factory for creating a MeteringPoint
        val factory = previewView.meteringPointFactory

        // Convert UI coordinates into camera sensor coordinates
        val point = factory.createPoint(x, y)

        // Prepare foucs action to be triggered
        val action = FocusMeteringAction.Builder(point).build()

        // Execute focus action
        // cameraControl.startFoucsAndMetering(action)
    }

    private fun runFaceContourDetection(image: ImageProxy, inputImage: InputImage) {
        //  val image = InputImage.fromMediaImage(mediaImage,0)
        // High-accuracy landmark detection and face classification
        val faceDetectorOptions = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
         //   .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build()
        //Real-time contour detection
        val realTimeOpts = FaceDetectorOptions.Builder()
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build()

        val faceDetector = com.google.mlkit.vision.face.FaceDetection.getClient(faceDetectorOptions)
        faceDetector.process(inputImage)
            .addOnSuccessListener { faces -> processFaceControurDetetionResult(faces) }
            .addOnFailureListener { }
            .addOnCompleteListener { task ->
                //  Log.e("TAG", "addOnCompleteListener: ${task.result.text}")
                image.close()
            }
    }

    private fun processFaceControurDetetionResult(faces: List<Face>) {
        //Task completed successfully
        if (faces.isNotEmpty()) {
            var textFace = ""
            for (face in faces) {
            //    face.getContour()
                val bounds = face.boundingBox
                val rotY = face.headEulerAngleY // Head is rotated to the right rotY degrees
                val rotx = face.headEulerAngleX  // HEad is titled sideways rotx degrees
                val rotz = face.headEulerAngleZ  // HEad is titled sideways rotz degrees
                // If landmark detection was enabled (moutn, ears, eyes, cheeks, and nose available.
                val leftEar = face.getLandmark(FaceLandmark.LEFT_EAR)
                leftEar?.let { val leftEarPos = leftEar.position }

                //If contour detection was enabled:
                val leftEyeConyour = face.getContour(FaceContour.LEFT_EYE)?.points
                val rightEyeConyour = face.getContour(FaceContour.RIGHT_EYE)?.points
                val upperLipBottomContour = face.getContour(FaceContour.UPPER_LIP_BOTTOM)?.points
                val upperLipTopContour = face.getContour(FaceContour.UPPER_LIP_TOP)?.points

                // If the classification detection was enabled:
                if (face.smilingProbability != null){
                    val smileProb = face.smilingProbability
                }
                if (face.rightEyeOpenProbability != null){
                    val rightEyeOpenProb = face.rightEyeOpenProbability
                }
                // If face tracking was enabled:
                if (face.trackingId != null){
                    val id = face.trackingId
                }
                textFace += face.toString()
            }
            this.textFace = textFace
            cameraVieWModel.getUser(textFace)


//            for (i in 0..faces.size) {
//                val face = faces.get(i)
//                //  val faceGraphic = FaceContourGraphic(mGraphicOverlay)
//
//                //val faceGraphic = FaceContour
//            }
        } else {
            binding.root.showSnackBar("No found face", false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExcutor.shutdown()
    }

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted())
                startCamera()
            else {
                Toast.makeText(this, "Permissions not grnated by the user.", Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun runTextRecognition(image: ImageProxy, inputImage: InputImage) {
        if (inputImage != null) {
            val result = recognizer.process(inputImage!!).addOnSuccessListener { visionText ->
                binding.root.showSnackBar(visionText.text, true)
            }.addOnFailureListener { e ->
                // Task failed with an exception
                Log.e("TAG", "addOnFailureListener ", e)

            }.addOnCompleteListener { task ->
                //  Log.e("TAG", "addOnCompleteListener: ${task.result.text}")
                image.close()
            }
        }
    }

    private fun runNumberRecognition(image: ImageProxy, inputImage: InputImage) {
//        val image = InputImage.fromBitmap(mSelectedImage, 0)
//        val recognizer = TextRecognition.getClient()
//       // mTextButton.setEnabled(false)
//        recognizer.process(image)
//            .addOnSuccessListener { texts ->
//             //   mTextButton.setEnabled(true)
//                processTextRecognitionResult(texts)
//            }
//            .addOnFailureListener { e -> // Task failed with an exception
//              //  mTextButton.setEnabled(true)
//                e.printStackTrace()
//            }
            val result = recognizer.process(inputImage).addOnSuccessListener { visionText ->

                // Task completed successfully
                if (isNumberOrString(visionText.text)) {
                    vibrate()
                    val intent = Intent()
                    intent.putExtra("input", visionText.text)
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                    Log.e(
                        "TAG",
                        "addOnSuccessListener   text is: ${visionText.text} and textBlocks is: ${visionText.textBlocks.size}"
                    )
                } else
                    Log.e("TAG", "Error")
            }.addOnFailureListener { e ->
                // Task failed with an exception
                Log.e("TAG", "addOnFailureListener ", e)

            }.addOnCompleteListener { task ->
                //  Log.e("TAG", "addOnCompleteListener: ${task.result.text}")
                image.close()
            }
    }

    //  @SuppressLint("ServiceCast")
    var vibrator: Vibrator? = null
    fun vibrate() {
        if (Build.VERSION.SDK_INT >= 31) {
            val vibratorManager =
                getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibrator = vibratorManager.defaultVibrator

        } else {
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(VIBRATE_PATTERN, 0))

        } else {
            vibrator?.vibrate(VIBRATE_PATTERN)
        }
    }

    private fun isNumberOrString(toCheck: String): Boolean {
        return toCheck.toIntOrNull() != null
    }

    fun playToastSound(context: Context) {
        try {
            val afd = context.assets.openFd("toast.mp3")
            val player = MediaPlayer()
            player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            player.prepare()
            player.start()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun processTextRecognitionResult(texts: Text) {
//        val blocks: List<Text.TextBlock> = texts.getTextBlocks()
//        if (blocks.size == 0) {
//          //  showToast("No text found")
//            return
//        }
//        mGraphicOverlay.clear()
//        for (i in blocks.indices) {
//            val lines: List<Text.Line> = blocks[i].getLines()
//            for (j in lines.indices) {
//                val elements: List<Text.Element> = lines[j].getElements()
//                for (k in elements.indices) {
//                    val textGraphic: Graphic = TextGraphic(mGraphicOverlay, elements[k])
//                    mGraphicOverlay.add(textGraphic)
//                }
//            }
//        }
    }
}
