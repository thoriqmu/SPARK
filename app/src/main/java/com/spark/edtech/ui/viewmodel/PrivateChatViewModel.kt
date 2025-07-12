package com.spark.edtech.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.spark.edtech.data.model.Message
import com.spark.edtech.data.source.FirebaseDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PrivateChatViewModel @Inject constructor(
    private val firebaseDataSource: FirebaseDataSource,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _chatMessages = MutableLiveData<Result<List<Message>>>()
    val chatMessages: LiveData<Result<List<Message>>> get() = _chatMessages

    private val _sendMessageResult = MutableLiveData<Result<Unit>>()
    val sendMessageResult: LiveData<Result<Unit>> get() = _sendMessageResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    fun loadChatMessages(chatId: String) {
        viewModelScope.launch(Dispatchers.Main) {
            _isLoading.value = true
            val startTime = System.currentTimeMillis()
            try {
                val messages = firebaseDataSource.getChatMessages(chatId)
                _chatMessages.value = Result.success(messages)
                val elapsedTime = System.currentTimeMillis() - startTime
                val remainingTime = 2000L - elapsedTime
                if (remainingTime > 0) {
                    delay(remainingTime)
                }
                _isLoading.value = false
            } catch (e: Exception) {
                _chatMessages.value = Result.failure(e)
                val elapsedTime = System.currentTimeMillis() - startTime
                val remainingTime = 2000L - elapsedTime
                if (remainingTime > 0) {
                    delay(remainingTime)
                }
                _isLoading.value = false
            }
        }
    }

    fun sendTextMessage(chatId: String, content: String, replyTo: String? = null) {
        viewModelScope.launch(Dispatchers.Main) {
            _isLoading.value = true
            val startTime = System.currentTimeMillis()
            try {
                val currentUser = firebaseAuth.currentUser
                if (currentUser != null) {
                    val message = Message(
                        sender_uid = currentUser.uid,
                        type = "text",
                        content = content,
                        timestamp = System.currentTimeMillis(),
                        reply_to = replyTo
                    )
                    firebaseDataSource.sendMessage(chatId, message)
                    _sendMessageResult.value = Result.success(Unit)
                    loadChatMessages(chatId) // Refresh messages
                } else {
                    _sendMessageResult.value = Result.failure(Exception("No user logged in"))
                }
                val elapsedTime = System.currentTimeMillis() - startTime
                val remainingTime = 2000L - elapsedTime
                if (remainingTime > 0) {
                    delay(remainingTime)
                }
                _isLoading.value = false
            } catch (e: Exception) {
                _sendMessageResult.value = Result.failure(e)
                val elapsedTime = System.currentTimeMillis() - startTime
                val remainingTime = 2000L - elapsedTime
                if (remainingTime > 0) {
                    delay(remainingTime)
                }
                _isLoading.value = false
            }
        }
    }

    fun sendMediaMessage(chatId: String, mediaFile: File, type: String, replyTo: String? = null) {
        viewModelScope.launch(Dispatchers.Main) {
            _isLoading.value = true
            val startTime = System.currentTimeMillis()
            try {
                val currentUser = firebaseAuth.currentUser
                if (currentUser != null) {
                    val mediaName = "chat_${System.currentTimeMillis()}.${if (type == "image") "jpg" else "mp3"}"
                    val downloadUrl = firebaseDataSource.uploadChatMedia(mediaFile, mediaName)
                    val message = Message(
                        sender_uid = currentUser.uid,
                        type = type,
                        content = downloadUrl,
                        timestamp = System.currentTimeMillis(),
                        reply_to = replyTo
                    )
                    firebaseDataSource.sendMessage(chatId, message)
                    _sendMessageResult.value = Result.success(Unit)
                    loadChatMessages(chatId) // Refresh messages
                } else {
                    _sendMessageResult.value = Result.failure(Exception("No user logged in"))
                }
                val elapsedTime = System.currentTimeMillis() - startTime
                val remainingTime = 2000L - elapsedTime
                if (remainingTime > 0) {
                    delay(remainingTime)
                }
                _isLoading.value = false
            } catch (e: Exception) {
                _sendMessageResult.value = Result.failure(e)
                val elapsedTime = System.currentTimeMillis() - startTime
                val remainingTime = 2000L - elapsedTime
                if (remainingTime > 0) {
                    delay(remainingTime)
                }
                _isLoading.value = false
            }
        }
    }
}