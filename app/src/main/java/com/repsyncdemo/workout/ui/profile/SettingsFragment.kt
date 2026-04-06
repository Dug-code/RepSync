package com.repsyncdemo.workout.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.material.imageview.ShapeableImageView
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

        profileViewModel.myProfile.observe(viewLifecycleOwner) { profile ->
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
                
                updateProfilePicturePreview(it.profilePictureUrl)
            }
        }

        binding.btnChangePic.setOnClickListener {
            showProfilePictureDialog()
        }

        binding.btnUseCustomUrl.setOnClickListener {
            binding.tilProfilePicUrl.visibility = View.VISIBLE
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

    private fun updateProfilePicturePreview(url: String) {
        if (url.isNotEmpty() && (url.startsWith("http") || url.startsWith("https"))) {
            binding.ivProfilePic.load(url) {
                crossfade(true)
                transformations(CircleCropTransformation())
            }
        } else {
            // Handle local resource URLs or defaults
            val resId = when(url) {
                "red" -> R.drawable.ic_profile_red
                "blue" -> R.drawable.ic_profile_blue
                "green" -> R.drawable.ic_profile_green
                "yellow" -> R.drawable.ic_profile_yellow
                "purple" -> R.drawable.ic_profile_purple
                else -> R.drawable.ic_profile_grey
            }
            binding.ivProfilePic.setImageResource(resId)
        }
    }

    private fun showProfilePictureDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_profile_picture_picker, null)
        val builder = AlertDialog.Builder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).setView(dialogView)
        val dialog = builder.create()

        val currentSelection = binding.etProfilePicUrl.text.toString()

        val icons = mapOf(
            dialogView.findViewById<ShapeableImageView>(R.id.iconRed) to "red",
            dialogView.findViewById<ShapeableImageView>(R.id.iconBlue) to "blue",
            dialogView.findViewById<ShapeableImageView>(R.id.iconGreen) to "green",
            dialogView.findViewById<ShapeableImageView>(R.id.iconYellow) to "yellow",
            dialogView.findViewById<ShapeableImageView>(R.id.iconPurple) to "purple",
            dialogView.findViewById<ShapeableImageView>(R.id.iconGrey) to "grey"
        )

        // Highlight the currently selected icon
        icons.forEach { (view, color) ->
            if (color == currentSelection) {
                view.strokeWidth = resources.getDimension(R.dimen.selected_stroke_width)
            } else {
                view.strokeWidth = 0f
            }

            view.setOnClickListener {
                binding.etProfilePicUrl.setText(color)
                binding.tilProfilePicUrl.visibility = View.GONE
                updateProfilePicturePreview(color)
                updateLockState()
                dialog.dismiss()
            }
        }

        dialogView.findViewById<View>(R.id.btnUseUrl).setOnClickListener {
            binding.tilProfilePicUrl.visibility = View.VISIBLE
            dialog.dismiss()
        }

        dialog.show()
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
        binding.switchHeightPublic.setOnClickListener { updateLockState() }
        binding.switchWeightPublic.setOnClickListener { updateLockState() }
        binding.switchWorkoutsPublic.setOnClickListener { updateLockState() }
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
