package com.spark.edtech.ui.view.profile

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.spark.edtech.databinding.ActivityProfileSettingBinding
import com.spark.edtech.ui.viewmodel.ProfileViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileSettingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileSettingBinding
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load user data
        viewModel.loadUserProfile()

        // Observe user data
        viewModel.userProfile.observe(this) { result ->
            result.onSuccess { user ->
                binding.inputEditName.setText(user.name)
                binding.inputEditBio.setText(user.bio ?: "")
            }.onFailure { exception ->
                Toast.makeText(this, "Error loading profile: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Handle change picture button (placeholder)
        binding.btnChangePicture.setOnClickListener {
            Toast.makeText(this, "Change profile picture not implemented", Toast.LENGTH_SHORT).show()
        }

        // Handle save button
        binding.btnSave.setOnClickListener {
            val name = binding.inputEditName.text.toString().trim()
            val bio = binding.inputEditBio.text.toString().trim()
            if (name.isBlank()) {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.updateUserProfile(name, bio)
        }

        // Observe update result
        viewModel.updateProfileResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            }.onFailure { exception ->
                Toast.makeText(this, "Error updating profile: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}