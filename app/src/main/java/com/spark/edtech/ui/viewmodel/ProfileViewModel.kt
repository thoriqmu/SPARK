package com.spark.edtech.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.spark.edtech.data.model.User
import com.spark.edtech.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

    private val _isLoading = MutableLiveData<Boolean>(true) // Ditambahkan: inisialisasi ke true
    val isLoading: LiveData<Boolean> get() = _isLoading

    fun loadUserProfile() {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                val result = authRepository.getUser(currentUser.uid)
                _userProfile.postValue(result)
                val elapsedTime = System.currentTimeMillis() - startTime
                val remainingTime = 2000L - elapsedTime // Minimal 2 detik
                if (remainingTime > 0) {
                    delay(remainingTime)
                }
                _isLoading.postValue(false)
            } else {
                _userProfile.postValue(Result.failure(Exception("No user logged in")))
                val elapsedTime = System.currentTimeMillis() - startTime
                val remainingTime = 2000L - elapsedTime
                if (remainingTime > 0) {
                    delay(remainingTime)
                }
                _isLoading.postValue(false)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                firebaseAuth.signOut()
                _logoutResult.postValue(true)
            } catch (e: Exception) {
                _logoutResult.postValue(false)
            }
        }
    }

    fun updateUserProfile(name: String, bio: String) {
        _isLoading.postValue(true)
        val startTime = System.currentTimeMillis()
        viewModelScope.launch {
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                val user = User(
                    uid = currentUser.uid,
                    name = name,
                    email = currentUser.email ?: "",
                    redeemCode = null,
                    bio = bio
                )
                val result = authRepository.updateUser(user)
                _updateProfileResult.postValue(result)
                val elapsedTime = System.currentTimeMillis() - startTime
                val remainingTime = 2000L - elapsedTime
                if (remainingTime > 0) {
                    delay(remainingTime)
                }
                _isLoading.postValue(false)
            } else {
                _updateProfileResult.postValue(Result.failure(Exception("No user logged in")))
                val elapsedTime = System.currentTimeMillis() - startTime
                val remainingTime = 2000L - elapsedTime
                if (remainingTime > 0) {
                    delay(remainingTime)
                }
                _isLoading.postValue(false)
            }
        }
    }
}