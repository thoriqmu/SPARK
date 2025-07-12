package com.bravy.app.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.bravy.app.data.repository.AuthRepository
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class PracticeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val firebaseAuth: FirebaseAuth,
    private val database: FirebaseDatabase
) : ViewModel() {

    private val _anxietyLevel = MutableLiveData<Result<String>>()
    val anxietyLevel: LiveData<Result<String>> get() = _anxietyLevel

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    fun loadAnxietyLevel() {
        _isLoading.postValue(true)
        viewModelScope.launch {
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                try {
                    // Ambil data dari Realtime Database
                    val snapshot = database.reference.child("users")
                        .child(currentUser.uid)
                        .child("lastAnxietyLevel")
                        .get().await()

                    val level = snapshot.getValue(String::class.java) ?: "None"
                    _anxietyLevel.postValue(Result.success(level))
                } catch (e: Exception) {
                    _anxietyLevel.postValue(Result.failure(e))
                }
            } else {
                _anxietyLevel.postValue(Result.failure(Exception("No user logged in")))
            }
            delay(1500)
            _isLoading.postValue(false)
        }
    }
}