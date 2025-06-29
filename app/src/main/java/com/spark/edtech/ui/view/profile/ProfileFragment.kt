package com.spark.edtech.ui.view.profile

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
import com.spark.edtech.databinding.FragmentProfileBinding
import com.spark.edtech.ui.view.auth.LoginActivity
import com.spark.edtech.ui.viewmodel.ProfileViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    companion object {
        fun newInstance() = ProfileFragment()
    }

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ditambahkan: set animasi transisi fade
        enterTransition = Fade().setDuration(300)
        exitTransition = Fade().setDuration(300)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        // Ditambahkan: set visibilitas awal secara eksplisit
        binding.lottieLoading.visibility = View.VISIBLE
        binding.tvProfileTitle.visibility = View.VISIBLE // Tetap visible sesuai XML
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
            // Show UI even if animation fails
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
                // Ditambahkan: gunakan transisi fade saat menampilkan UI
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
            }.onFailure { exception ->
                Toast.makeText(requireContext(), "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                binding.tvUserName.text = "User Name"
                binding.tvUserEmail.text = "email@gmail.com"
                binding.tvUserBio.text = "No bio available"
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

        // Placeholder for other buttons
        binding.btnChangePicture.setOnClickListener {
            Toast.makeText(requireContext(), "Change profile picture not implemented", Toast.LENGTH_SHORT).show()
        }
        binding.switchNotification.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(requireContext(), "Notification setting: $isChecked", Toast.LENGTH_SHORT).show()
        }

        // Ditambahkan: panggil loadUserProfile di akhir
        viewModel.loadUserProfile()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}