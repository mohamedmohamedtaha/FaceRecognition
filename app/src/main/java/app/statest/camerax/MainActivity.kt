package app.statest.camerax

import android.Manifest.permission.*
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import app.statest.camerax.database.CameraVieWModel
import app.statest.camerax.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.AndroidEntryPoint
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

typealias LumaListener = (luma: Double) -> Unit
typealias ImageListener = (luma: InputImage) -> Unit

//private class YourImageAnalyzer(private val imageListener: ImageListener) : ImageAnalysis.Analyzer {
//    override fun analyze(image: ImageProxy) {
//        val mediaImage = image.image
//        if (mediaImage != null) {
//            inputImage = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
//            //Pass image to an ML Kit Vision API
//            if (inputImage != null) {
//                imageListener(inputImage!!)
//            }
//        }
//        image.close()
//    }
//}

private class LuminosityAnalyzer(private val listener: LumaListener) : ImageAnalysis.Analyzer {
    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()// Rewind the buffer to zero
        val data = ByteArray(remaining())
        get(data) // Copy the buffer into a byte array
        return data // Return the byte array
    }

    override fun analyze(image: ImageProxy) {
        val buffer = image.planes[0].buffer
        val data = buffer.toByteArray()
        val pixels = data.map { it.toInt() and 0xFF }
        val luma = pixels.average()
        listener(luma)
        image.close()
    }
}
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val cameraVieWModel by viewModels<CameraVieWModel>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //Request Camera permission
        cameraVieWModel.getAllUser()
        cameraVieWModel.getAllUserLiveData.observe(this){
            if (it != null){

            }
        }
        if (allPermissionsGranted()) {

        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        binding.numberEt.setOnClickListener {
            val intent = Intent(this, ScanActivity::class.java)
            intent.putExtra("DATA","1")
            getResult.launch(intent)
        }

        binding.checkYourFace.setOnClickListener {
            val intent = Intent(this, ScanActivity::class.java)
            intent.putExtra("DATA","2")
            getResult.launch(intent)
        }

        binding.checkYourText.setOnClickListener {
            val intent = Intent(this, ScanActivity::class.java)
            intent.putExtra("DATA","3")
            getResult.launch(intent)
        }
        binding.saveBt.setOnClickListener {
            if (binding.numberEt.text.toString().isEmpty() || binding.nameEt.text.toString().isEmpty() || binding.ageEt.text.toString().isEmpty() )
                Toast.makeText(this,"Must fill all data.",Toast.LENGTH_SHORT).show()
            else
                Toast.makeText(this,"The data saved.",Toast.LENGTH_SHORT).show()

        }
    }
    private val getResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if (it.resultCode == Activity.RESULT_OK){
            val value = it.data?.getStringExtra("input")
            binding.numberEt.setText(value.toString())
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }


    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = mutableListOf(
            CAMERA,
            RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(WRITE_EXTERNAL_STORAGE)
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

            else {
                Toast.makeText(this, "Permissions not grnated by the user.", Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
