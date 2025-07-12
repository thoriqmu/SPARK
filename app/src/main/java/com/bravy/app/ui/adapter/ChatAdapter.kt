package com.bravy.app.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.storage.FirebaseStorage
import com.bravy.app.R
import com.bravy.app.data.model.Message
import com.bravy.app.data.model.User
import com.bravy.app.databinding.ItemChatBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChatAdapter(
    private val onChatClick: (User, String) -> Unit
) : ListAdapter<Triple<User, String, Message?>, ChatAdapter.ChatViewHolder>(ChatDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val (user, chatId, lastMessage) = getItem(position)
        holder.bind(user, chatId, lastMessage)
    }

    inner class ChatViewHolder(
        private val binding: ItemChatBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User, chatId: String, lastMessage: Message?) {
            binding.tvChatName.text = user.name
            binding.tvChatMessage.text = when (lastMessage?.type) {
                "text" -> lastMessage.content
                "image" -> "Image"
                "audio" -> "Audio"
                else -> "No messages"
            }
            binding.tvChatTime.text = lastMessage?.timestamp?.let {
                val currentTime = System.currentTimeMillis()
                val timeDifference = currentTime - it
                val seconds = timeDifference / 1000
                val minutes = seconds / 60
                val hours = minutes / 60
                val days = hours / 24
                when {
                    days > 0 -> "$days days ago"
                    hours > 0 -> "$hours hours ago"
                    minutes > 0 -> "$minutes minutes ago"
                    else -> "$seconds seconds ago"
                }
            }.toString()
            val imageName = user.image ?: "default.jpg"
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val storageRef = FirebaseStorage.getInstance().getReference("picture")
                    val imageRef = storageRef.child(imageName)
                    val downloadUrl = imageRef.downloadUrl.await().toString()
                    Log.d("ChatAdapter", "Loading image URL: $downloadUrl")
                    Glide.with(binding.root.context)
                        .load(downloadUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .error(R.drawable.sample)
                        .into(binding.ivChatProfile)
                } catch (e: Exception) {
                    Log.e("ChatAdapter", "Error loading image $imageName: ${e.message}")
                    Glide.with(binding.root.context)
                        .load(R.drawable.sample)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(binding.ivChatProfile)
                }
            }
            binding.root.setOnClickListener {
                onChatClick(user, chatId)
            }
        }
    }

    class ChatDiffCallback : DiffUtil.ItemCallback<Triple<User, String, Message?>>() {
        override fun areItemsTheSame(oldItem: Triple<User, String, Message?>, newItem: Triple<User, String, Message?>): Boolean {
            return oldItem.first.uid == newItem.first.uid && oldItem.second == newItem.second
        }

        override fun areContentsTheSame(oldItem: Triple<User, String, Message?>, newItem: Triple<User, String, Message?>): Boolean {
            return oldItem == newItem
        }
    }
}