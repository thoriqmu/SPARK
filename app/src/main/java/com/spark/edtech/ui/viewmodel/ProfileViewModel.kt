package com.spark.edtech.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.spark.edtech.data.model.User
import com.spark.edtech.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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

    fun loadUserProfile() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                val result = authRepository.getUser(currentUser.uid)
                _userProfile.postValue(result)
            }
        } else {
            _userProfile.postValue(Result.failure(Exception("No user logged in")))
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
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                val user = User(
                    uid = currentUser.uid,
                    name = name,
                    email = currentUser.email ?: "",
                    redeemCode = null,
                    bio = bio
                )
                val result = authRepository.updateUser(user)
                _updateProfileResult.postValue(result)
            }
        } else {
            _updateProfileResult.postValue(Result.failure(Exception("No user logged in")))
        }
    }
}