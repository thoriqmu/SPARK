package com.bravy.app.ui.view.chat

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.storage.FirebaseStorage
import com.bravy.app.R
import com.bravy.app.databinding.FragmentChatBinding
import com.bravy.app.ui.viewmodel.ChatViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@AndroidEntryPoint
class ChatFragment : Fragment() {

    companion object {
        fun newInstance() = ChatFragment()
    }

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChatViewModel by viewModels()
    private val storageRef = FirebaseStorage.getInstance().getReference("picture")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = Fade().setDuration(300)
        exitTransition = Fade().setDuration(300)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        binding.lottieLoading.visibility = View.VISIBLE
        binding.tvChatTitle.visibility = View.VISIBLE
        binding.layoutPrivateChat.visibility = View.GONE
        binding.cardPrivateChat.visibility = View.GONE
        binding.layoutRecentChat.visibility = View.GONE
        binding.layoutCommunity.visibility = View.GONE
        binding.cardCommunity.visibility = View.GONE
        binding.layoutRecentCommunity.visibility = View.GONE
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set failure listener for Lottie
        binding.lottieLoading.setFailureListener { throwable ->
            Toast.makeText(requireContext(), "Failed to load animation: ${throwable.message}", Toast.LENGTH_SHORT).show()
            binding.lottieLoading.visibility = View.GONE
            TransitionManager.beginDelayedTransition(binding.root as ViewGroup, Fade().setDuration(300))
            binding.layoutPrivateChat.visibility = View.VISIBLE
            binding.cardPrivateChat.visibility = View.VISIBLE
            binding.layoutRecentChat.visibility = View.VISIBLE
            binding.layoutCommunity.visibility = View.VISIBLE
            binding.cardCommunity.visibility = View.VISIBLE
            binding.layoutRecentCommunity.visibility = View.VISIBLE
        }

        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                binding.lottieLoading.visibility = View.VISIBLE
                binding.tvChatTitle.visibility = View.VISIBLE
                binding.layoutPrivateChat.visibility = View.GONE
                binding.cardPrivateChat.visibility = View.GONE
                binding.layoutRecentChat.visibility = View.GONE
                binding.layoutCommunity.visibility = View.GONE
                binding.cardCommunity.visibility = View.GONE
                binding.layoutRecentCommunity.visibility = View.GONE
            } else {
                TransitionManager.beginDelayedTransition(binding.root as ViewGroup, Fade().setDuration(300))
                binding.lottieLoading.visibility = View.GONE
                binding.tvChatTitle.visibility = View.VISIBLE
                binding.layoutPrivateChat.visibility = View.VISIBLE
                binding.cardPrivateChat.visibility = View.VISIBLE
                binding.layoutRecentChat.visibility = View.VISIBLE
                binding.layoutCommunity.visibility = View.VISIBLE
                binding.cardCommunity.visibility = View.VISIBLE
                binding.layoutRecentCommunity.visibility = View.VISIBLE
            }
        }

        // Observe recent chat users
        viewModel.recentChatUsers.observe(viewLifecycleOwner) { result ->
            result.onSuccess { usersWithChatIds ->
                if (usersWithChatIds.isNotEmpty()) {
                    val (user, chatId) = usersWithChatIds.first() // Ambil user dan chatId
                    binding.recentChatName.text = user.name

                    // AMBIL DAN TAMPILKAN PESAN TERAKHIR
                    viewModel.getLastMessage(chatId).observe(viewLifecycleOwner) { lastMessage ->
                        binding.recentChatText.text = when (lastMessage?.type) {
                            "text" -> lastMessage.content
                            "image" -> "Image Media"
                            "audio" -> "Audio Media"
                            else -> "No messages"
                        }
                    }

                    viewModel.getLastTimestamp(chatId).observe(viewLifecycleOwner) { lastTimestamp ->
                        binding.recentChatTime.text = lastTimestamp?.let {
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
                    }

                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            val imageName = user.image ?: "default.jpg"
                            val imageRef = storageRef.child(imageName)
                            val downloadUrl = imageRef.downloadUrl.await()
                            Glide.with(this@ChatFragment)
                                .load(downloadUrl)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .error(R.drawable.sample)
                                .into(binding.ivRecentChat)
                        } catch (e: Exception) {
                            Glide.with(this@ChatFragment)
                                .load(R.drawable.sample)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(binding.ivRecentChat)
                        }
                    }
                    binding.layoutRecentChat.setOnClickListener {
                        val intent = Intent(requireContext(), PrivateMessageActivity::class.java).apply {
                            putExtra("CHAT_ID", chatId) // Gunakan chatId yang benar
                            putExtra("OTHER_USER_ID", user.uid)
                            putExtra("OTHER_USER_NAME", user.name)
                            putExtra("OTHER_USER_IMAGE", user.image)
                        }
                        startActivity(intent)
                    }
                } else {
                    binding.recentChatName.text = "No Recent Chat"
                    binding.recentChatText.text = "Start a new conversation"
                    Glide.with(this@ChatFragment)
                        .load(R.drawable.sample)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(binding.ivRecentChat)
                }
            }.onFailure { exception ->
                binding.recentChatName.text = "User Name"
                binding.recentChatText.text = "Failed to load chat"
                Glide.with(this@ChatFragment)
                    .load(R.drawable.sample)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(binding.ivRecentChat)
                Toast.makeText(requireContext(), "Error loading recent chat: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle private chat click
        binding.layoutPrivateChat.setOnClickListener {
            val intent = Intent(requireContext(), PrivateChatActivity::class.java)
            startActivity(intent)
        }

        // Handle community click
        binding.layoutCommunity.setOnClickListener {
            val intent = Intent(requireContext(), CommunityActivity::class.java)
            startActivity(intent)
        }

        viewModel.loadRecentChatUsers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}