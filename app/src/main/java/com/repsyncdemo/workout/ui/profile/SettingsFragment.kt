package com.repsyncdemo.workout.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.google.firebase.auth.FirebaseAuth
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.databinding.FragmentSettingsBinding
import com.repsyncdemo.workout.ui.auth.LoginActivity
import com.repsyncdemo.workout.viewmodel.NavigationLockViewModel
import com.repsyncdemo.workout.viewmodel.ProfileViewModel

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val profileViewModel: ProfileViewModel by activityViewModels()
    private val navigationLockViewModel: NavigationLockViewModel by activityViewModels()

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

        // Handle Back Navigation with Warning
        val backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (hasUnsavedChanges()) {
                    showUnsavedChangesDialog {
                        navigationLockViewModel.setLocked(false)
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)

        setupChangeListeners()

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

                // Load height
                if (it.heightInches > 0) {
                    binding.etHeightFeet.setText((it.heightInches / 12).toString())
                    binding.etHeightInches.setText((it.heightInches % 12).toString())
                }

                // Check correct theme button based on profile
                if (it.theme == "light") {
                    binding.toggleTheme.check(R.id.btnLightTheme)
                } else {
                    binding.toggleTheme.check(R.id.btnDarkTheme)
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
            saveChanges()
        }

        profileViewModel.profileResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                navigationLockViewModel.setLocked(false)
                Toast.makeText(requireContext(), "Settings saved", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
            result.onFailure {
                Toast.makeText(requireContext(), "Save failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun setupChangeListeners() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateLockState()
            }
        }

        binding.etUsername.addTextChangedListener(watcher)
        binding.etBio.addTextChangedListener(watcher)
        binding.etProfilePicUrl.addTextChangedListener(watcher)
        binding.etInstagramUrl.addTextChangedListener(watcher)
        binding.etFacebookUrl.addTextChangedListener(watcher)
        binding.etTwitterUrl.addTextChangedListener(watcher)
        binding.etHeightFeet.addTextChangedListener(watcher)
        binding.etHeightInches.addTextChangedListener(watcher)

        binding.toggleTheme.addOnButtonCheckedListener { _, _, _ -> updateLockState() }
        binding.switchHeightPublic.setOnCheckedChangeListener { _, _ -> updateLockState() }
        binding.switchWeightPublic.setOnCheckedChangeListener { _, _ -> updateLockState() }
        binding.switchWorkoutsPublic.setOnCheckedChangeListener { _, _ -> updateLockState() }
    }

    private fun updateLockState() {
        navigationLockViewModel.setLocked(hasUnsavedChanges())
    }

    private fun hasUnsavedChanges(): Boolean {
        val original = profileViewModel.myProfile.value ?: return false
        
        val currentFeet = binding.etHeightFeet.text.toString().toIntOrNull() ?: 0
        val currentInches = binding.etHeightInches.text.toString().toIntOrNull() ?: 0
        val currentHeight = (currentFeet * 12) + currentInches

        return binding.etUsername.text.toString() != original.username ||
               binding.etBio.text.toString() != original.bio ||
               binding.etProfilePicUrl.text.toString() != original.profilePictureUrl ||
               binding.etInstagramUrl.text.toString() != original.instagramUrl ||
               binding.etFacebookUrl.text.toString() != original.facebookUrl ||
               binding.etTwitterUrl.text.toString() != original.twitterUrl ||
               currentHeight != original.heightInches ||
               binding.switchHeightPublic.isChecked != original.isHeightPublic ||
               binding.switchWeightPublic.isChecked != original.isWeightPublic ||
               binding.switchWorkoutsPublic.isChecked != original.isWorkoutsPublic ||
               (binding.toggleTheme.checkedButtonId == R.id.btnLightTheme && original.theme != "light") ||
               (binding.toggleTheme.checkedButtonId == R.id.btnDarkTheme && original.theme != "dark")
    }

    private fun showUnsavedChangesDialog(onDiscard: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle("Unsaved Changes")
            .setMessage("You have unsaved changes. Are you sure you want to discard them?")
            .setPositiveButton("Discard") { _, _ -> onDiscard() }
            .setNegativeButton("Keep Editing", null)
            .show()
    }

    private fun saveChanges() {
        val username = binding.etUsername.text.toString().trim()
        val bio = binding.etBio.text.toString().trim()
        val picUrl = binding.etProfilePicUrl.text.toString().trim()
        
        val instagramUrl = binding.etInstagramUrl.text.toString().trim()
        val facebookUrl = binding.etFacebookUrl.text.toString().trim()
        val twitterUrl = binding.etTwitterUrl.text.toString().trim()
        
        val feet = binding.etHeightFeet.text.toString().toIntOrNull() ?: 0
        val inches = binding.etHeightInches.text.toString().toIntOrNull() ?: 0
        val totalHeightInches = (feet * 12) + inches

        val themePreference = if (binding.toggleTheme.checkedButtonId == R.id.btnLightTheme) "light" else "dark"
        
        val isHeightPublic = binding.switchHeightPublic.isChecked
        val isWeightPublic = binding.switchWeightPublic.isChecked
        val isWorkoutsPublic = binding.switchWorkoutsPublic.isChecked

        if (username.isEmpty()) {
            binding.etUsername.error = "Username required"
            return
        }

        if (!isValidUrl(instagramUrl, listOf("instagram.com"))) {
            binding.etInstagramUrl.error = "Invalid Instagram URL"
            return
        }
        if (!isValidUrl(facebookUrl, listOf("facebook.com"))) {
            binding.etFacebookUrl.error = "Invalid Facebook URL"
            return
        }
        if (!isValidUrl(twitterUrl, listOf("x.com", "twitter.com"))) {
            binding.etTwitterUrl.error = "Invalid X/Twitter URL"
            return
        }

        val currentProfile = profileViewModel.myProfile.value
        currentProfile?.let {
            val updatedProfile = it.copy(
                username = username,
                bio = bio,
                profilePictureUrl = picUrl,
                instagramUrl = instagramUrl,
                facebookUrl = facebookUrl,
                twitterUrl = twitterUrl,
                heightInches = totalHeightInches,
                preferredUnit = "lbs",
                theme = themePreference,
                isHeightPublic = isHeightPublic,
                isWeightPublic = isWeightPublic,
                isWorkoutsPublic = isWorkoutsPublic
            )
            
            // 1. Save theme locally
            val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
            prefs.edit().putString("theme", themePreference).apply()

            // 2. Apply theme
            if (themePreference == "light") {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }

            // 3. Sync to Firebase
            profileViewModel.updateProfile(updatedProfile)
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
