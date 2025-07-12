package com.bravy.app.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bravy.app.R
import com.bravy.app.data.model.Message
import com.bravy.app.databinding.ItemMessageBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            val isCurrentUser = message.sender_uid == currentUserId

            // Tampilkan container yang sesuai dan sembunyikan yang lain
            if (isCurrentUser) {
                binding.clCurrentUserMessage.visibility = View.VISIBLE
                binding.clOtherUserMessage.visibility = View.GONE
                bindMessageContent(binding, message, true)
            } else {
                binding.clCurrentUserMessage.visibility = View.GONE
                binding.clOtherUserMessage.visibility = View.VISIBLE
                bindMessageContent(binding, message, false)
            }
        }

        private fun bindMessageContent(binding: ItemMessageBinding, message: Message, isCurrentUser: Boolean) {
            // Pilih view berdasarkan siapa pengirimnya
            val tvMessageContent = if (isCurrentUser) binding.tvMessageContentCurrent else binding.tvMessageContentOther
            val ivMessageImage = if (isCurrentUser) binding.ivMessageImageCurrent else binding.ivMessageImageOther
            val llAudioContainer = if (isCurrentUser) binding.llAudioContainerCurrent else binding.llAudioContainerOther
            val tvTimestamp = if (isCurrentUser) binding.tvTimestampCurrent else binding.tvTimestampOther
            val tvReplyContent = if (isCurrentUser) binding.tvReplyContentCurrent else binding.tvReplyContentOther
            val btnPlayAudio = if (isCurrentUser) binding.btnPlayAudioCurrent else binding.btnPlayAudioOther
            val tvAudioDuration = if (isCurrentUser) binding.tvAudioDurationCurrent else binding.tvAudioDurationOther

            // Atur visibilitas berdasarkan tipe pesan
            tvMessageContent.visibility = if (message.type == "text") View.VISIBLE else View.GONE
            ivMessageImage.visibility = if (message.type == "image") View.VISIBLE else View.GONE
            llAudioContainer.visibility = if (message.type == "audio") View.VISIBLE else View.GONE

            // Isi konten
            when (message.type) {
                "text" -> tvMessageContent.text = message.content
                "image" -> {
                    Glide.with(binding.root.context)
                        .load(message.content)
                        .error(R.drawable.sample) // Gambar fallback jika error
                        .into(ivMessageImage)
                }
                "audio" -> {
                    tvAudioDuration.text = "Audio" // Anda bisa menambahkan logika durasi di sini
                    btnPlayAudio.setOnClickListener { onAudioClick(message.content) }
                }
            }

            // Atur timestamp
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            tvTimestamp.text = sdf.format(Date(message.timestamp))
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