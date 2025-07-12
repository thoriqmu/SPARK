package com.bravy.app.ui.view.practice

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bravy.app.databinding.ActivityPracticeBinding
import com.bravy.app.ui.viewmodel.PracticeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PracticeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPracticeBinding
    private val viewModel: PracticeViewModel by viewModels()

    private val anxietyTestLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val anxietyLevel = result.data?.getStringExtra("ANXIETY_RESULT") ?: "None"
            binding.tvTitle.text = anxietyLevel
            Toast.makeText(this, "Test complete! Your level: $anxietyLevel", Toast.LENGTH_LONG).show()
            // Opsi: Muat ulang data dari ViewModel untuk memastikan konsistensi
            viewModel.loadAnxietyLevel()
        } else {
            Toast.makeText(this, "Anxiety test was cancelled.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPracticeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set failure listener for Lottie
        binding.lottieLoading.setFailureListener { throwable ->
            Toast.makeText(this, "Failed to load animation: ${throwable.message}", Toast.LENGTH_SHORT).show()
            binding.lottieLoading.visibility = android.view.View.GONE
            // Show UI even if animation fails
            binding.tvLearningTitle.visibility = android.view.View.VISIBLE
            binding.btnBack.visibility = android.view.View.VISIBLE
            binding.tvPracticeTitle.visibility = android.view.View.VISIBLE
            binding.tvCheckTitle.visibility = android.view.View.VISIBLE
            binding.cardView.visibility = android.view.View.VISIBLE
            binding.scrollView.visibility = android.view.View.VISIBLE
        }

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                binding.lottieLoading.visibility = android.view.View.VISIBLE
                binding.btnBack.visibility = android.view.View.VISIBLE
                binding.tvPracticeTitle.visibility = android.view.View.VISIBLE
                binding.tvCheckTitle.visibility = android.view.View.GONE
                binding.cardView.visibility = android.view.View.GONE
                binding.tvLearningTitle.visibility = android.view.View.GONE
                binding.scrollView.visibility = android.view.View.GONE
            } else {
                binding.lottieLoading.visibility = android.view.View.GONE
                binding.btnBack.visibility = android.view.View.VISIBLE
                binding.tvPracticeTitle.visibility = android.view.View.VISIBLE
                binding.tvCheckTitle.visibility = android.view.View.VISIBLE
                binding.cardView.visibility = android.view.View.VISIBLE
                binding.tvLearningTitle.visibility = android.view.View.VISIBLE
                binding.scrollView.visibility = android.view.View.VISIBLE
            }
        }

        // Observe anxiety level
        viewModel.anxietyLevel.observe(this) { result ->
            result.onSuccess { level ->
                binding.tvTitle.text = level
            }.onFailure { exception ->
                Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                binding.tvTitle.text = "None"
            }
        }

        // Handle back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Load anxiety level
        viewModel.loadAnxietyLevel()

        // Handle button clicks (example for btn_check and practice buttons)
        binding.btnCheck.setOnClickListener {
            val intent = Intent(this, AnxietyTestActivity::class.java)
            anxietyTestLauncher.launch(intent)
        }

        binding.btn1.setOnClickListener {
            Toast.makeText(this, "Watch & Practice: Very Relaxing", Toast.LENGTH_SHORT).show()
        }
        binding.btn2.setOnClickListener {
            Toast.makeText(this, "Watch & Practice: Relaxing", Toast.LENGTH_SHORT).show()
        }
        binding.btn3.setOnClickListener {
            Toast.makeText(this, "Watch & Practice: Midly Anxious", Toast.LENGTH_SHORT).show()
        }
        binding.btn4.setOnClickListener {
            Toast.makeText(this, "Watch & Practice: Anxious", Toast.LENGTH_SHORT).show()
        }
        binding.btn5.setOnClickListener {
            Toast.makeText(this, "Watch & Practice: Very Anxious", Toast.LENGTH_SHORT).show()
        }
    }
}