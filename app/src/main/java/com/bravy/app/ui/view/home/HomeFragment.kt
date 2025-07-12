package com.bravy.app.ui.view.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.storage.FirebaseStorage
import com.bravy.app.R
import com.bravy.app.databinding.FragmentHomeBinding
import com.bravy.app.ui.view.practice.PracticeActivity
import com.bravy.app.ui.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@AndroidEntryPoint
class HomeFragment : Fragment() {

    companion object {
        fun newInstance() = HomeFragment()
    }

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
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
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        binding.lottieLoading.visibility = View.VISIBLE
        binding.tvUserName.visibility = View.GONE
        binding.ivUserPhoto.visibility = View.GONE
        binding.tvGreeting.visibility = View.GONE
        binding.nestedScrollView.visibility = View.GONE
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set failure listener for Lottie
        binding.lottieLoading.setFailureListener { throwable ->
            Toast.makeText(requireContext(), "Failed to load animation: ${throwable.message}", Toast.LENGTH_SHORT).show()
            binding.lottieLoading.visibility = View.GONE
            TransitionManager.beginDelayedTransition(binding.root as ViewGroup, Fade().setDuration(300))
            binding.tvUserName.visibility = View.VISIBLE
            binding.ivUserPhoto.visibility = View.VISIBLE
            binding.tvGreeting.visibility = View.VISIBLE
            binding.nestedScrollView.visibility = View.VISIBLE
        }

        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                binding.lottieLoading.visibility = View.VISIBLE
                binding.tvUserName.visibility = View.GONE
                binding.ivUserPhoto.visibility = View.GONE
                binding.tvGreeting.visibility = View.GONE
                binding.nestedScrollView.visibility = View.GONE
            } else {
                TransitionManager.beginDelayedTransition(binding.root as ViewGroup, Fade().setDuration(300))
                binding.lottieLoading.visibility = View.GONE
                binding.tvUserName.visibility = View.VISIBLE
                binding.ivUserPhoto.visibility = View.VISIBLE
                binding.tvGreeting.visibility = View.VISIBLE
                binding.nestedScrollView.visibility = View.VISIBLE
            }
        }

        // Observe user data
        viewModel.userProfile.observe(viewLifecycleOwner) { result ->
            result.onSuccess { user ->
                binding.tvUserName.text = "Hello,\n${user.name.split(" ").firstOrNull() ?: "User"}!"

                // Use viewLifecycleOwner.lifecycleScope for coroutines tied to the view's lifecycle
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val imageName = user.image ?: "default.jpg"
                        val imageRef = storageRef.child(imageName)
                        val downloadUrl = imageRef.downloadUrl.await()

                        // Ensure fragment is still added before using Glide
                        if (!isAdded) return@launch

                        Glide.with(this@HomeFragment)
                            .load(downloadUrl)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .error(R.drawable.sample)
                            .into(binding.ivUserPhoto)
                    } catch (e: Exception) {
                        if (!isAdded) return@launch // Check again in case of error before next Glide call
                        Toast.makeText(requireContext(), "Failed to load profile picture: ${e.message}", Toast.LENGTH_SHORT).show()
                        // Muat default.jpg jika gagal
                        try {
                            val defaultUrl = storageRef.child("default.jpg").downloadUrl.await()
                            if (!isAdded) return@launch

                            Glide.with(this@HomeFragment)
                                .load(defaultUrl)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .error(R.drawable.sample)
                                .into(binding.ivUserPhoto)
                        } catch (innerE: Exception) {
                            if (!isAdded) return@launch

                            Glide.with(this@HomeFragment)
                                .load(R.drawable.sample) // Load placeholder directly
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(binding.ivUserPhoto)
                        }
                    }
                }
            }.onFailure { exception ->
                // Ensure fragment is still added before UI updates from LiveData observer
                if (!isAdded) return@observe

                Toast.makeText(requireContext(), "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                binding.tvUserName.text = "Hello,\nUser!"

                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val defaultUrl = storageRef.child("default.jpg").downloadUrl.await()
                        if (!isAdded) return@launch

                        Glide.with(this@HomeFragment)
                            .load(defaultUrl)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .error(R.drawable.sample)
                            .into(binding.ivUserPhoto)
                    } catch (e: Exception) {
                        if (!isAdded) return@launch

                        Glide.with(this@HomeFragment)
                            .load(R.drawable.sample) // Load placeholder directly
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(binding.ivUserPhoto)
                    }
                }
            }
        }

        // Handle Speaking Learning card click
        binding.cardSpeak.setOnClickListener {
            startActivity(Intent(requireContext(), PracticeActivity::class.java))
        }

        viewModel.loadUserProfile()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}