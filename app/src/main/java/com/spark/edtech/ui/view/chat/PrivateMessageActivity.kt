package com.spark.edtech.ui.view.chat

import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.spark.edtech.R
import com.spark.edtech.databinding.ActivityPrivateMessageBinding
import com.spark.edtech.ui.adapter.MessageAdapter
import com.spark.edtech.ui.viewmodel.PrivateChatViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream

@AndroidEntryPoint
class PrivateMessageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPrivateMessageBinding
    private val viewModel: PrivateChatViewModel by viewModels()
    private val storageRef = FirebaseStorage.getInstance().getReference("picture")
    private lateinit var messageAdapter: MessageAdapter
    private var selectedMessageId: String? = null // Untuk balasan
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var audioFile: File? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val tempFile = File.createTempFile("chat", ".jpg", cacheDir)
                    inputStream?.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    val chatId = intent.getStringExtra("CHAT_ID") ?: return@let
                    viewModel.sendMediaMessage(chatId, tempFile, "image", selectedMessageId)
                } catch (e: Exception) {
                    Toast.makeText(this, "Error selecting image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrivateMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val chatId = intent.getStringExtra("CHAT_ID") ?: return
        val otherUserName = intent.getStringExtra("OTHER_USER_NAME") ?: "User"
        val otherUserId = intent.getStringExtra("OTHER_USER_ID") ?: ""

        binding.tvPrivateChatTitle.text = otherUserName

        // Load other user's profile picture
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val imageName = intent.getStringExtra("OTHER_USER_IMAGE") ?: "default.jpg"
                val imageRef = storageRef.child(imageName)
                val downloadUrl = imageRef.downloadUrl.await()
                Glide.with(this@PrivateMessageActivity)
                    .load(downloadUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .error(R.drawable.sample)
                    .into(binding.ivPrivateChatProfile)
            } catch (e: Exception) {
                Glide.with(this@PrivateMessageActivity)
                    .load(R.drawable.sample)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(binding.ivPrivateChatProfile)
            }
        }

        // Setup RecyclerView
        messageAdapter = MessageAdapter(
            currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
            onReplyClick = { messageId ->
                selectedMessageId = messageId
                binding.etMessage.hint = "Replying to message..."
            },
            onAudioClick = { url ->
                playAudio(url)
            }
        )
        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(this@PrivateMessageActivity).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            binding.lottieLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe messages
        viewModel.chatMessages.observe(this) { result ->
            result.onSuccess { messages ->
                messageAdapter.submitList(messages)
                binding.rvMessages.scrollToPosition(messages.size - 1)
            }.onFailure { exception ->
                Toast.makeText(this, "Error loading messages: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Observe send message result
        viewModel.sendMessageResult.observe(this) { result ->
            result.onSuccess {
                binding.etMessage.text?.clear()
                selectedMessageId = null
                binding.etMessage.hint = "Type a message..."
            }.onFailure { exception ->
                Toast.makeText(this, "Error sending message: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Handle send message
        binding.btnSend.setOnClickListener {
            val content = binding.etMessage.text.toString().trim()
            if (content.isNotBlank()) {
                viewModel.sendTextMessage(chatId, content, selectedMessageId)
            } else {
                Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle image attachment
        binding.btnAttachImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        }

        // Handle audio recording
        binding.btnRecordAudio.setOnClickListener {
            if (isRecording) {
                stopRecording(chatId)
            } else {
                startRecording()
            }
        }

        viewModel.loadChatMessages(chatId)
    }

    private fun startRecording() {
        try {
            audioFile = File.createTempFile("chat_audio", ".mp3", cacheDir)
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            binding.btnRecordAudio.setImageResource(R.drawable.ic_stop)
            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error starting recording: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording(chatId: String) {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            binding.btnRecordAudio.setImageResource(R.drawable.ic_mic)
            audioFile?.let { file ->
                viewModel.sendMediaMessage(chatId, file, "audio", selectedMessageId)
            }
            Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error stopping recording: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playAudio(url: String) {
        try {
            val mediaPlayer = MediaPlayer().apply {
                setDataSource(url)
                prepare()
                start()
            }
            mediaPlayer.setOnCompletionListener { it.release() }
        } catch (e: Exception) {
            Toast.makeText(this, "Error playing audio: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaRecorder?.release()
        mediaRecorder = null
    }
}