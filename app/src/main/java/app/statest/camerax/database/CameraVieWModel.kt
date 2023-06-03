package app.statest.camerax.database

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.statest.camerax.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CameraVieWModel @Inject constructor(private val cameraRepository: CameraRepository) :
    ViewModel() {
    private val _getAllUserLiveData = MutableLiveData<ArrayList<User>?>()
    val getAllUserLiveData get() = _getAllUserLiveData
    fun getAllUser() {
        viewModelScope.launch {
            _getAllUserLiveData.value = cameraRepository.getAllUser()
        }
    }
    private val _getUserLiveData = MutableLiveData<User?>()
    val getUserLiveData get() = _getUserLiveData
    fun getUser(faceUser: String) {
        viewModelScope.launch {
            _getUserLiveData.value = cameraRepository.getUser(faceUser = faceUser)
        }
    }

    fun saveUser(user: User):Boolean {
        var resultFinal = false
        viewModelScope.launch {
            val result = async {
                cameraRepository.saveUser(user = user)
            }
            resultFinal = result.await()
        }
        return resultFinal
    }
}