package com.bravy.app.ui.view.chat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.bravy.app.R
import com.bravy.app.data.model.User
import com.bravy.app.data.source.FirebaseDataSource
import com.bravy.app.databinding.ActivityPrivateChatBinding
import com.bravy.app.ui.adapter.ChatAdapter
import com.bravy.app.ui.viewmodel.ChatViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@AndroidEntryPoint
class PrivateChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPrivateChatBinding
    private val viewModel: ChatViewModel by viewModels()
    private val storageRef = FirebaseStorage.getInstance().getReference("picture")
    private lateinit var chatAdapter: ChatAdapter
    private val TAG = "PrivateChatActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrivateChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup RecyclerView
        chatAdapter = ChatAdapter { user, chatId ->
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val intent = Intent(this@PrivateChatActivity, PrivateMessageActivity::class.java).apply {
                        putExtra("CHAT_ID", chatId)
                        putExtra("OTHER_USER_ID", user.uid)
                        putExtra("OTHER_USER_NAME", user.name)
                        putExtra("OTHER_USER_IMAGE", user.image)
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error opening chat: ${e.message}")
                    Toast.makeText(this@PrivateChatActivity, "Error opening chat: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding.rvChat.apply {
            layoutManager = LinearLayoutManager(this@PrivateChatActivity)
            adapter = chatAdapter
        }

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            binding.lottieLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe recent chat users
        viewModel.recentChatUsers.observe(this) { result ->
            result.onSuccess { usersWithChatIds ->
                Log.d(TAG, "Fetched ${usersWithChatIds.size} chat users")
                chatAdapter.submitList(usersWithChatIds)
                binding.rvChat.visibility = View.VISIBLE
            }.onFailure { exception ->
                Log.e(TAG, "Error loading chat users: ${exception.message}")
                Toast.makeText(this, "Error loading chats: ${exception.message}", Toast.LENGTH_SHORT).show()
                binding.rvChat.visibility = View.GONE
            }
        }

        // Handle back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Handle add chat (placeholder for user selection)
        binding.addChat.setOnClickListener {
            Toast.makeText(this, "User selection not implemented", Toast.LENGTH_SHORT).show()
        }

        viewModel.loadRecentChatUsers()
    }
}