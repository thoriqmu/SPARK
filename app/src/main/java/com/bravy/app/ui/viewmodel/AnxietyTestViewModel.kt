package com.bravy.app.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AnxietyTestViewModel @Inject constructor(
    private val storage: FirebaseStorage,
    private val database: FirebaseDatabase, // Diubah dari Firestore ke Realtime Database
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uploadResult = MutableLiveData<Result<Unit>>()
    val uploadResult: LiveData<Result<Unit>> get() = _uploadResult

    fun uploadVideo(videoUri: Uri, classificationResult: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: run {
                _uploadResult.postValue(Result.failure(Exception("User not logged in")))
                return@launch
            }
            val videoRef = storage.reference.child("practice_videos/${userId}/${System.currentTimeMillis()}.mp4")

            try {
                // 1. Upload video ke Firebase Storage
                val uploadTask = videoRef.putFile(videoUri).await()
                val downloadUrl = uploadTask.storage.downloadUrl.await().toString()

                // 2. Siapkan path dan data untuk Realtime Database
                val userPracticeRef = database.reference.child("users").child(userId)
                val newHistoryRef = userPracticeRef.child("practice_history").push() // push() untuk ID unik

                val practiceData = mapOf(
                    "videoUrl" to downloadUrl,
                    "anxietyLevel" to classificationResult,
                    "timestamp" to System.currentTimeMillis()
                )

                // 3. Simpan riwayat latihan
                newHistoryRef.setValue(practiceData).await()

                // 4. Update level kecemasan terakhir pada data user
                userPracticeRef.child("lastAnxietyLevel").setValue(classificationResult).await()

                _uploadResult.postValue(Result.success(Unit))

            } catch (e: Exception) {
                _uploadResult.postValue(Result.failure(e))
            }
        }
    }
}