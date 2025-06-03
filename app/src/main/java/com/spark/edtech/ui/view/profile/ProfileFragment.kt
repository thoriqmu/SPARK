package com.spark.edtech.ui.view.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load user data
        viewModel.loadUserProfile()

        // Observe user data
        viewModel.userProfile.observe(viewLifecycleOwner) { result ->
            result.onSuccess { user ->
                binding.tvUserName.text = user.name
                binding.tvUserEmail.text = user.email
                binding.tvUserBio.text = user.bio ?: "No bio available"
            }.onFailure { exception ->
                Toast.makeText(requireContext(), "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}