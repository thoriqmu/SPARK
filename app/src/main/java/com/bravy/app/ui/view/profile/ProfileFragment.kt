package com.bravy.app.ui.view.profile

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.storage.FirebaseStorage
import com.bravy.app.R
import com.bravy.app.databinding.FragmentProfileBinding
import com.bravy.app.ui.view.auth.LoginActivity
import com.bravy.app.ui.viewmodel.ProfileViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    companion object {
        fun newInstance() = ProfileFragment()
    }

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()
    private val storageRef = FirebaseStorage.getInstance().getReference("picture")

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    val inputStream = requireContext().contentResolver.openInputStream(uri)
                    val tempFile = File.createTempFile("profile", ".jpg", requireContext().cacheDir)
                    inputStream?.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    // Ditambahkan: panggil uploadProfilePicture di dalam coroutine
                    CoroutineScope(Dispatchers.Main).launch {
                        viewModel.uploadProfilePicture(tempFile)
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error selecting image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = Fade().setDuration(300)
        exitTransition = Fade().setDuration(300)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        binding.lottieLoading.visibility = View.VISIBLE
        binding.tvProfileTitle.visibility = View.VISIBLE
        binding.ivProfile.visibility = View.GONE
        binding.btnChangePicture.visibility = View.GONE
        binding.layoutDetailProfile.visibility = View.GONE
        binding.layoutNotification.visibility = View.GONE
        binding.btnProfileSetting.visibility = View.GONE
        binding.btnLogout.visibility = View.GONE
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set failure listener for Lottie
        binding.lottieLoading.setFailureListener { throwable ->
            Toast.makeText(requireContext(), "Failed to load animation: ${throwable.message}", Toast.LENGTH_SHORT).show()
            binding.lottieLoading.visibility = View.GONE
            TransitionManager.beginDelayedTransition(binding.root as ViewGroup, Fade().setDuration(300))
            binding.tvProfileTitle.visibility = View.VISIBLE
            binding.ivProfile.visibility = View.VISIBLE
            binding.btnChangePicture.visibility = View.VISIBLE
            binding.layoutDetailProfile.visibility = View.VISIBLE
            binding.layoutNotification.visibility = View.VISIBLE
            binding.btnProfileSetting.visibility = View.VISIBLE
            binding.btnLogout.visibility = View.VISIBLE
        }

        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                binding.lottieLoading.visibility = View.VISIBLE
                binding.tvProfileTitle.visibility = View.VISIBLE
                binding.ivProfile.visibility = View.GONE
                binding.btnChangePicture.visibility = View.GONE
                binding.layoutDetailProfile.visibility = View.GONE
                binding.layoutNotification.visibility = View.GONE
                binding.btnProfileSetting.visibility = View.GONE
                binding.btnLogout.visibility = View.GONE
            } else {
                TransitionManager.beginDelayedTransition(binding.root as ViewGroup, Fade().setDuration(300))
                binding.lottieLoading.visibility = View.GONE
                binding.tvProfileTitle.visibility = View.VISIBLE
                binding.ivProfile.visibility = View.VISIBLE
                binding.btnChangePicture.visibility = View.VISIBLE
                binding.layoutDetailProfile.visibility = View.VISIBLE
                binding.layoutNotification.visibility = View.VISIBLE
                binding.btnProfileSetting.visibility = View.VISIBLE
                binding.btnLogout.visibility = View.VISIBLE
            }
        }

        // Observe user data
        viewModel.userProfile.observe(viewLifecycleOwner) { result ->
            result.onSuccess { user ->
                binding.tvUserName.text = user.name
                binding.tvUserEmail.text = user.email
                binding.tvUserBio.text = user.bio ?: "No bio available"
                // Muat foto profil dengan URL unduhan
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val imageName = user.image ?: "default.jpg"
                        val imageRef = storageRef.child(imageName)
                        val downloadUrl = imageRef.downloadUrl.await()
                        Glide.with(this@ProfileFragment)
                            .load(downloadUrl)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .error(R.drawable.sample)
                            .into(binding.ivProfile)
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Failed to load profile picture: ${e.message}", Toast.LENGTH_SHORT).show()
                        // Muat default.jpg jika gagal
                        try {
                            val defaultUrl = storageRef.child("default.jpg").downloadUrl.await()
                            Glide.with(this@ProfileFragment)
                                .load(defaultUrl)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .error(R.drawable.sample)
                                .into(binding.ivProfile)
                        } catch (e: Exception) {
                            Glide.with(this@ProfileFragment)
                                .load(R.drawable.sample)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(binding.ivProfile)
                        }
                    }
                }
            }.onFailure { exception ->
                Toast.makeText(requireContext(), "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                binding.tvUserName.text = "User Name"
                binding.tvUserEmail.text = "email@gmail.com"
                binding.tvUserBio.text = "No bio available"
                // Muat gambar default saat error
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val defaultUrl = storageRef.child("default.jpg").downloadUrl.await()
                        Glide.with(this@ProfileFragment)
                            .load(defaultUrl)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .error(R.drawable.sample)
                            .into(binding.ivProfile)
                    } catch (e: Exception) {
                        Glide.with(this@ProfileFragment)
                            .load(R.drawable.sample)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(binding.ivProfile)
                    }
                }
            }
        }

        // Observe upload picture result
        viewModel.uploadPictureResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Toast.makeText(requireContext(), "Profile picture updated", Toast.LENGTH_SHORT).show()
                // Ditambahkan: muat ulang profil untuk memperbarui gambar
                viewModel.loadUserProfile()
            }.onFailure { exception ->
                Toast.makeText(requireContext(), "Failed to update picture: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle logout button
        binding.btnLogout.setOnClickListener {
            viewModel.logout()
        }

        // Observe logout result
        viewModel.logoutResult.observe(viewLifecycleOwner) { success ->
            if (success) {
                val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                requireActivity().finish()
            } else {
                Toast.makeText(requireContext(), "Logout failed", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle profile setting button
        binding.btnProfileSetting.setOnClickListener {
            startActivity(Intent(requireContext(), ProfileSettingActivity::class.java))
        }

        // Handle change picture button
        binding.btnChangePicture.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        }

        // Placeholder for notification switch
        binding.switchNotification.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(requireContext(), "Notification setting: $isChecked", Toast.LENGTH_SHORT).show()
        }

        viewModel.loadUserProfile()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}