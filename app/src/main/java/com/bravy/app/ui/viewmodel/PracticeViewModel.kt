package com.bravy.app.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.bravy.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PracticeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _anxietyLevel = MutableLiveData<Result<String>>()
    val anxietyLevel: LiveData<Result<String>> get() = _anxietyLevel

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    fun loadAnxietyLevel() {
        _isLoading.postValue(true)
        val startTime = System.currentTimeMillis()
        viewModelScope.launch {
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                // TODO: Replace with actual logic to fetch anxiety level from Firebase
                // Example: Assume anxiety level is stored in user data or a separate collection
                val result = try {
                    // Mock implementation; replace with actual Firebase call
                    Result.success("None")
                } catch (e: Exception) {
                    Result.failure(e)
                }
                _anxietyLevel.postValue(result)
                val elapsedTime = System.currentTimeMillis() - startTime
                val remainingTime = 2000L - elapsedTime // Minimal 2 detik
                if (remainingTime > 0) {
                    delay(remainingTime)
                }
                _isLoading.postValue(false)
            } else {
                _anxietyLevel.postValue(Result.failure(Exception("No user logged in")))
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