package com.spark.edtech.ui.view.profile

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.storage.FirebaseStorage
import com.spark.edtech.R
import com.spark.edtech.databinding.ActivityProfileSettingBinding
import com.spark.edtech.ui.viewmodel.ProfileViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream

@AndroidEntryPoint
class ProfileSettingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileSettingBinding
    private val viewModel: ProfileViewModel by viewModels()
    private val storageRef = FirebaseStorage.getInstance().getReference("picture")
    private var selectedImageFile: File? = null // Menyimpan file gambar sementara

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val tempFile = File.createTempFile("profile", ".jpg", cacheDir)
                    inputStream?.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    selectedImageFile = tempFile // Simpan file untuk diunggah nanti
                    // Tampilkan pratinjau gambar
                    Glide.with(this)
                        .load(tempFile)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .error(R.drawable.sample)
                        .into(binding.ivProfilePicture)
                    Toast.makeText(this, "Image selected, click Save to apply", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Error selecting image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set failure listener for Lottie
        binding.lottieLoading.setFailureListener { throwable ->
            Toast.makeText(this, "Failed to load animation: ${throwable.message}", Toast.LENGTH_SHORT).show()
            binding.lottieLoading.visibility = View.GONE
            binding.btnBack.visibility = View.VISIBLE
            binding.tvProfileSetting.visibility = View.VISIBLE
            binding.tvPicture.visibility = View.VISIBLE
            binding.ivProfilePicture.visibility = View.VISIBLE
            binding.btnChangePicture.visibility = View.VISIBLE
            binding.tvProfileDescription.visibility = View.VISIBLE
            binding.tvEditName.visibility = View.VISIBLE
            binding.inputEditName.visibility = View.VISIBLE
            binding.tvEditBio.visibility = View.VISIBLE
            binding.inputEditBio.visibility = View.VISIBLE
            binding.btnSave.visibility = View.VISIBLE
        }

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                binding.lottieLoading.visibility = View.VISIBLE
                binding.btnBack.visibility = View.VISIBLE
                binding.tvProfileSetting.visibility = View.VISIBLE
                binding.tvPicture.visibility = View.GONE
                binding.ivProfilePicture.visibility = View.GONE
                binding.btnChangePicture.visibility = View.GONE
                binding.tvProfileDescription.visibility = View.GONE
                binding.tvEditName.visibility = View.GONE
                binding.inputEditName.visibility = View.GONE
                binding.tvEditBio.visibility = View.GONE
                binding.inputEditBio.visibility = View.GONE
                binding.btnSave.visibility = View.GONE
            } else {
                binding.lottieLoading.visibility = View.GONE
                binding.btnBack.visibility = View.VISIBLE
                binding.tvProfileSetting.visibility = View.VISIBLE
                binding.tvPicture.visibility = View.VISIBLE
                binding.ivProfilePicture.visibility = View.VISIBLE
                binding.btnChangePicture.visibility = View.VISIBLE
                binding.tvProfileDescription.visibility = View.VISIBLE
                binding.tvEditName.visibility = View.VISIBLE
                binding.inputEditName.visibility = View.VISIBLE
                binding.tvEditBio.visibility = View.VISIBLE
                binding.inputEditBio.visibility = View.VISIBLE
                binding.btnSave.visibility = View.VISIBLE
            }
        }

        // Observe user data
        viewModel.userProfile.observe(this) { result ->
            result.onSuccess { user ->
                binding.inputEditName.setText(user.name)
                binding.inputEditBio.setText(user.bio ?: "")
                // Muat foto profil dengan URL unduhan
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val imageName = user.image ?: "default.jpg"
                        val imageRef = storageRef.child(imageName)
                        val downloadUrl = imageRef.downloadUrl.await()
                        Glide.with(this@ProfileSettingActivity)
                            .load(downloadUrl)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .error(R.drawable.sample)
                            .into(binding.ivProfilePicture)
                    } catch (e: Exception) {
                        Toast.makeText(this@ProfileSettingActivity, "Failed to load profile picture: ${e.message}", Toast.LENGTH_SHORT).show()
                        // Muat default.jpg jika gagal
                        try {
                            val defaultUrl = storageRef.child("default.jpg").downloadUrl.await()
                            Glide.with(this@ProfileSettingActivity)
                                .load(defaultUrl)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .error(R.drawable.sample)
                                .into(binding.ivProfilePicture)
                        } catch (e: Exception) {
                            Glide.with(this@ProfileSettingActivity)
                                .load(R.drawable.sample)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(binding.ivProfilePicture)
                        }
                    }
                }
            }.onFailure { exception ->
                Toast.makeText(this, "Error loading profile: ${exception.message}", Toast.LENGTH_SHORT).show()
                binding.inputEditName.setText("User Name")
                binding.inputEditBio.setText("No bio available")
                // Muat gambar default saat error
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val defaultUrl = storageRef.child("default.jpg").downloadUrl.await()
                        Glide.with(this@ProfileSettingActivity)
                            .load(defaultUrl)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .error(R.drawable.sample)
                            .into(binding.ivProfilePicture)
                    } catch (e: Exception) {
                        Glide.with(this@ProfileSettingActivity)
                            .load(R.drawable.sample)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(binding.ivProfilePicture)
                    }
                }
            }
        }

        // Observe update result
        viewModel.updateProfileResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                selectedImageFile = null // Reset file setelah simpan
                finish()
            }.onFailure { exception ->
                Toast.makeText(this, "Error updating profile: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Handle change picture button
        binding.btnChangePicture.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        }

        // Handle save button
        binding.btnSave.setOnClickListener {
            val name = binding.inputEditName.text.toString().trim()
            val bio = binding.inputEditBio.text.toString().trim()
            if (name.isBlank()) {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Mulai loading
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    var imageName: String? = viewModel.userProfile.value?.getOrNull()?.image
                    // Unggah gambar jika ada file yang dipilih
                    if (selectedImageFile != null) {
                        val uploadResult = viewModel.uploadProfilePicture(selectedImageFile!!)
                        uploadResult.onSuccess { uploadedImageName ->
                            imageName = uploadedImageName
                        }.onFailure { exception ->
                            Toast.makeText(this@ProfileSettingActivity, "Failed to upload picture: ${exception.message}", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                    }
                    // Perbarui profil dengan nama, bio, dan gambar (jika ada)
                    viewModel.updateUserProfile(name, bio, imageName)
                } catch (e: Exception) {
                    Toast.makeText(this@ProfileSettingActivity, "Error saving profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Load user data
        viewModel.loadUserProfile()
    }
}