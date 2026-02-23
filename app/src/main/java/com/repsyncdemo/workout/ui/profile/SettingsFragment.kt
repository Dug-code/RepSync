package com.repsyncdemo.workout.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.google.firebase.auth.FirebaseAuth
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.databinding.FragmentSettingsBinding
import com.repsyncdemo.workout.ui.auth.LoginActivity
import com.repsyncdemo.workout.viewmodel.ProfileViewModel

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val profileViewModel: ProfileViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        profileViewModel.currentProfile.observe(viewLifecycleOwner) { profile ->
            profile?.let {
                binding.etUsername.setText(it.username)
                binding.etBio.setText(it.bio)
                binding.etProfilePicUrl.setText(it.profilePictureUrl)
                
                binding.etInstagramUrl.setText(it.instagramUrl)
                if (it.instagramUrl.isNotEmpty()) {
                    binding.tilInstagram.visibility = View.VISIBLE
                    binding.btnEnableInstagram.visibility = View.GONE
                }

                binding.etFacebookUrl.setText(it.facebookUrl)
                if (it.facebookUrl.isNotEmpty()) {
                    binding.tilFacebook.visibility = View.VISIBLE
                    binding.btnEnableFacebook.visibility = View.GONE
                }

                binding.etTwitterUrl.setText(it.twitterUrl)
                if (it.twitterUrl.isNotEmpty()) {
                    binding.tilTwitter.visibility = View.VISIBLE
                    binding.btnEnableTwitter.visibility = View.GONE
                }

                if (it.preferredUnit == "kg") {
                    binding.toggleUnit.check(R.id.btnKg)
                } else {
                    binding.toggleUnit.check(R.id.btnLbs)
                }

                binding.switchHeightPublic.isChecked = it.isHeightPublic
                binding.switchWeightPublic.isChecked = it.isWeightPublic
                binding.switchWorkoutsPublic.isChecked = it.isWorkoutsPublic
                
                if (it.profilePictureUrl.isNotEmpty()) {
                    binding.ivProfilePic.load(it.profilePictureUrl) {
                        crossfade(true)
                        transformations(CircleCropTransformation())
                    }
                }
            }
        }

        binding.btnEnableInstagram.setOnClickListener {
            binding.tilInstagram.visibility = View.VISIBLE
            binding.btnEnableInstagram.visibility = View.GONE
        }

        binding.btnEnableFacebook.setOnClickListener {
            binding.tilFacebook.visibility = View.VISIBLE
            binding.btnEnableFacebook.visibility = View.GONE
        }

        binding.btnEnableTwitter.setOnClickListener {
            binding.tilTwitter.visibility = View.VISIBLE
            binding.btnEnableTwitter.visibility = View.GONE
        }

        binding.btnSaveSettings.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val bio = binding.etBio.text.toString().trim()
            val picUrl = binding.etProfilePicUrl.text.toString().trim()
            
            val instagramUrl = binding.etInstagramUrl.text.toString().trim()
            val facebookUrl = binding.etFacebookUrl.text.toString().trim()
            val twitterUrl = binding.etTwitterUrl.text.toString().trim()
            
            val preferredUnit = if (binding.toggleUnit.checkedButtonId == R.id.btnKg) "kg" else "lbs"
            
            val isHeightPublic = binding.switchHeightPublic.isChecked
            val isWeightPublic = binding.switchWeightPublic.isChecked
            val isWorkoutsPublic = binding.switchWorkoutsPublic.isChecked

            if (username.isEmpty()) {
                binding.etUsername.error = "Username required"
                return@setOnClickListener
            }

            if (!isValidUrl(instagramUrl, listOf("instagram.com"))) {
                binding.etInstagramUrl.error = "Invalid Instagram URL"
                return@setOnClickListener
            }
            if (!isValidUrl(facebookUrl, listOf("facebook.com"))) {
                binding.etFacebookUrl.error = "Invalid Facebook URL"
                return@setOnClickListener
            }
            if (!isValidUrl(twitterUrl, listOf("x.com", "twitter.com"))) {
                binding.etTwitterUrl.error = "Invalid X/Twitter URL"
                return@setOnClickListener
            }

            val currentProfile = profileViewModel.currentProfile.value
            currentProfile?.let {
                val updatedProfile = it.copy(
                    username = username,
                    bio = bio,
                    profilePictureUrl = picUrl,
                    instagramUrl = instagramUrl,
                    facebookUrl = facebookUrl,
                    twitterUrl = twitterUrl,
                    preferredUnit = preferredUnit,
                    isHeightPublic = isHeightPublic,
                    isWeightPublic = isWeightPublic,
                    isWorkoutsPublic = isWorkoutsPublic
                )
                profileViewModel.updateProfile(updatedProfile)
            }
        }

        profileViewModel.profileResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
            result.onFailure {
                Toast.makeText(requireContext(), "Update failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun isValidUrl(url: String, allowedDomains: List<String>): Boolean {
        if (url.isEmpty()) return true
        return allowedDomains.any { domain ->
            url.contains(domain) && (url.startsWith("http://") || url.startsWith("https://"))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
