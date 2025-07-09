package com.spark.edtech.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.spark.edtech.data.model.User
import com.spark.edtech.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _userProfile = MutableLiveData<Result<User>>()
    val userProfile: LiveData<Result<User>> get() = _userProfile

    private val _logoutResult = MutableLiveData<Boolean>()
    val logoutResult: LiveData<Boolean> get() = _logoutResult

    private val _updateProfileResult = MutableLiveData<Result<Unit>>()
    val updateProfileResult: LiveData<Result<Unit>> get() = _updateProfileResult

    private val _uploadPictureResult = MutableLiveData<Result<Unit>>()
    val uploadPictureResult: LiveData<Result<Unit>> get() = _uploadPictureResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    fun loadUserProfile() {
        viewModelScope.launch(Dispatchers.Main) {
            _isLoading.value = true // Menggunakan value di thread utama
            val startTime = System.currentTimeMillis()
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                val result = authRepository.getUser(currentUser.uid)
                _userProfile.postValue(result)
                val elapsedTime = System.currentTimeMillis() - startTime
                val remainingTime = 2000L - elapsedTime
                if (remainingTime > 0) {
                    delay(remainingTime)
                }
                _isLoading.value = false
            } else {
                _userProfile.postValue(Result.failure(Exception("No user logged in")))
                val elapsedTime = System.currentTimeMillis() - startTime
                val remainingTime = 2000L - elapsedTime
                if (remainingTime > 0) {
                    delay(remainingTime)
                }
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                firebaseAuth.signOut()
                _logoutResult.postValue(true)
            } catch (e: Exception) {
                _logoutResult.postValue(false)
            }
        }
    }

    fun updateUserProfile(name: String, bio: String, imageName: String? = null) {
        viewModelScope.launch(Dispatchers.Main) {
            _isLoading.value = true
            val startTime = System.currentTimeMillis()
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                val user = User(
                    uid = currentUser.uid,
                    name = name,
                    email = currentUser.email ?: "",
                    redeemCode = _userProfile.value?.getOrNull()?.redeemCode,
                    bio = bio,
                    image = imageName ?: _userProfile.value?.getOrNull()?.image
                )
                val result = authRepository.updateUser(user)
                _updateProfileResult.postValue(result)
                if (result.isSuccess) {
                    _userProfile.postValue(Result.success(user))
                }
                val elapsedTime = System.currentTimeMillis() - startTime
                val remainingTime = 2000L - elapsedTime
                if (remainingTime > 0) {
                    delay(remainingTime)
                }
                _isLoading.value = false
            } else {
                _updateProfileResult.postValue(Result.failure(Exception("No user logged in")))
                val elapsedTime = System.currentTimeMillis() - startTime
                val remainingTime = 2000L - elapsedTime
                if (remainingTime > 0) {
                    delay(remainingTime)
                }
                _isLoading.value = false
            }
        }
    }

    suspend fun uploadProfilePicture(imageFile: File): Result<String> {
        viewModelScope.launch(Dispatchers.Main) {
            _isLoading.value = true
        }
        val startTime = System.currentTimeMillis()
        return try {
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                val imageName = "profile_${currentUser.uid}.jpg"
                val result = authRepository.uploadProfilePicture(currentUser.uid, imageFile)
                viewModelScope.launch(Dispatchers.Main) {
                    _uploadPictureResult.postValue(result.map { Unit })
                    val elapsedTime = System.currentTimeMillis() - startTime
                    val remainingTime = 2000L - elapsedTime
                    if (remainingTime > 0) {
                        delay(remainingTime)
                    }
                    _isLoading.value = false
                }
                result
            } else {
                viewModelScope.launch(Dispatchers.Main) {
                    _uploadPictureResult.postValue(Result.failure(Exception("No user logged in")))
                    val elapsedTime = System.currentTimeMillis() - startTime
                    val remainingTime = 2000L - elapsedTime
                    if (remainingTime > 0) {
                        delay(remainingTime)
                    }
                    _isLoading.value = false
                }
                Result.failure(Exception("No user logged in"))
            }
        } catch (e: Exception) {
            viewModelScope.launch(Dispatchers.Main) {
                _uploadPictureResult.postValue(Result.failure(e))
                val elapsedTime = System.currentTimeMillis() - startTime
                val remainingTime = 2000L - elapsedTime
                if (remainingTime > 0) {
                    delay(remainingTime)
                }
                _isLoading.value = false
            }
            Result.failure(e)
        }
    }
}