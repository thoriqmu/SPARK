package com.spark.edtech.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.spark.edtech.R
import com.spark.edtech.data.model.Message
import com.spark.edtech.databinding.ItemMessageBinding

class MessageAdapter(
    private val currentUserId: String,
    private val onReplyClick: (String) -> Unit,
    private val onAudioClick: (String) -> Unit
) : ListAdapter<Message, MessageAdapter.MessageViewHolder>(MessageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MessageViewHolder(private val binding: ItemMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.tvMessageContent.visibility = if (message.type == "text") View.VISIBLE else View.GONE
            binding.ivMessageImage.visibility = if (message.type == "image") View.VISIBLE else View.GONE
            binding.llAudioContainer.visibility = if (message.type == "audio") View.VISIBLE else View.GONE

            // Adjust layout based on sender
            val isCurrentUser = message.senderUid == currentUserId
            binding.tvMessageContent.text = message.content
            binding.tvTimestamp.text = android.text.format.DateFormat.format("HH:mm", message.timestamp)

            val layoutParams = binding.tvMessageContent.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            layoutParams.startToStart = if (isCurrentUser) androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET else androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            layoutParams.endToEnd = if (isCurrentUser) androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID else androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
            binding.tvMessageContent.layoutParams = layoutParams
            binding.ivMessageImage.layoutParams = layoutParams
            binding.llAudioContainer.layoutParams = layoutParams
            binding.tvTimestamp.layoutParams = layoutParams

            if (message.type == "image") {
                Glide.with(binding.root.context)
                    .load(message.content)
                    .error(R.drawable.sample)
                    .into(binding.ivMessageImage)
            }

            if (message.type == "audio") {
                binding.tvAudioDuration.text = "0:00" // Placeholder, update with actual duration if available
                binding.btnPlayAudio.setOnClickListener { onAudioClick(message.content) }
            }

            if (message.replyTo != null) {
                binding.tvReplyContent.visibility = View.VISIBLE
                binding.tvReplyContent.text = "Replying to: ${getReplyContent(message.replyTo)}"
            } else {
                binding.tvReplyContent.visibility = View.GONE
            }
        }

        private fun getReplyContent(replyTo: String): String {
            val replyMessage = currentList.find { it.messageId == replyTo }
            return replyMessage?.content?.take(50) ?: "Message"
        }
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.messageId == newItem.messageId
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }
}