package com.bravy.app.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.bravy.app.data.model.User
import com.bravy.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _userProfile = MutableLiveData<Result<User>>()
    val userProfile: LiveData<Result<User>> get() = _userProfile

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
}