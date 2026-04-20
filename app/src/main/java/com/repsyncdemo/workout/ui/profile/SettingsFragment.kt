package com.repsyncdemo.workout.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.data.model.ContactMessage
import com.repsyncdemo.workout.databinding.FragmentSettingsBinding
import com.repsyncdemo.workout.ui.auth.LoginActivity
import com.repsyncdemo.workout.viewmodel.AnalyticsViewModel
import com.repsyncdemo.workout.viewmodel.NavigationLockViewModel
import com.repsyncdemo.workout.viewmodel.ProfileViewModel

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val profileViewModel: ProfileViewModel by activityViewModels()
    private val analyticsViewModel: AnalyticsViewModel by activityViewModels()
    private val navigationLockViewModel: NavigationLockViewModel by activityViewModels()

    private var isInitialLoad = true

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

                if (it.heightInches > 0) {
                    binding.etHeightFeet.setText((it.heightInches / 12).toString())
                    binding.etHeightInches.setText((it.heightInches % 12).toString())
                }

                binding.switchHeightPublic.isChecked = it.isHeightPublic
                binding.switchWeightPublic.isChecked = it.isWeightPublic
                binding.switchWorkoutsPublic.isChecked = it.isWorkoutsPublic
                binding.switchFriendsPublic.isChecked = it.isFriendsListPublic
                
                updateProfilePicturePreview(it.profilePictureUrl)
                
                // Mark initial load as finished so listeners can start tracking changes
                isInitialLoad = false
                navigationLockViewModel.setLocked(false)
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

        binding.btnClearHistory.setOnClickListener {
            showClearHistoryConfirmation()
        }

        binding.btnDeleteAccount.setOnClickListener {
            showDeleteAccountFlow()
        }

        binding.btnContactUs.setOnClickListener {
            showContactUsDialog()
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
        
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setTitle("Choose Profile Picture")
            .setView(dialogView)
            .create()

        val currentSelection = binding.etProfilePicUrl.text.toString()

        val icons = mapOf(
            dialogView.findViewById<ShapeableImageView>(R.id.iconRed) to "red",
            dialogView.findViewById<ShapeableImageView>(R.id.iconBlue) to "blue",
            dialogView.findViewById<ShapeableImageView>(R.id.iconGreen) to "green",
            dialogView.findViewById<ShapeableImageView>(R.id.iconYellow) to "yellow",
            dialogView.findViewById<ShapeableImageView>(R.id.iconPurple) to "purple",
            dialogView.findViewById<ShapeableImageView>(R.id.iconGrey) to "grey"
        )

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

    private fun showClearHistoryConfirmation() {
        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setTitle("Clear All Data?")
            .setMessage("This will permanently delete all your workout logs and reset your stats. This cannot be undone.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Clear Everything") { _, _ ->
                analyticsViewModel.clearAllHistory()
                Toast.makeText(requireContext(), "History cleared", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showDeleteAccountFlow() {
        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setTitle("Delete Account?")
            .setMessage("This action is permanent and will delete all your workout data, routines, and profile information. Are you sure you want to continue?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Continue") { _, _ ->
                showDeleteVerificationDialog()
            }
            .show()
    }

    private fun showDeleteVerificationDialog() {
        val input = EditText(requireContext())
        input.hint = "Type DELETE here"
        input.setPadding(64, 32, 64, 32)

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setTitle("Final Confirmation")
            .setMessage("Please type the word \"DELETE\" below to confirm permanent account removal.")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete Permanently", null)
            .create()

        dialog.setOnShowListener {
            val deleteButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            deleteButton.isEnabled = false
            deleteButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.error))

            input.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    deleteButton.isEnabled = s.toString() == "DELETE"
                }
            })

            deleteButton.setOnClickListener {
                FirebaseAuth.getInstance().currentUser?.delete()?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(requireContext(), "Account deleted", Toast.LENGTH_SHORT).show()
                        val intent = Intent(requireContext(), LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    } else {
                        Toast.makeText(requireContext(), "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun showContactUsDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_contact_us, null)
        val etName = dialogView.findViewById<EditText>(R.id.etContactName)
        val etEmail = dialogView.findViewById<EditText>(R.id.etContactEmail)
        val etMessage = dialogView.findViewById<EditText>(R.id.etContactMessage)

        // Pre-fill with user info if available
        profileViewModel.myProfile.value?.let { profile ->
            etName.setText(profile.username)
            etEmail.setText(profile.email)
        }

        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setView(dialogView)
            .setPositiveButton("Submit") { dialog, _ ->
                val name = etName.text.toString().trim()
                val email = etEmail.text.toString().trim()
                val message = etMessage.text.toString().trim()

                if (name.isNotEmpty() && email.isNotEmpty() && message.isNotEmpty()) {
                    val contactMessage = ContactMessage(
                        userId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                        name = name,
                        email = email,
                        message = message,
                        timestamp = System.currentTimeMillis()
                    )
                    profileViewModel.submitContactMessage(contactMessage)
                    Toast.makeText(requireContext(), "Sending message...", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Please fill out all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupChangeListeners() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!isInitialLoad) updateLockState()
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

        binding.switchHeightPublic.setOnClickListener { if (!isInitialLoad) updateLockState() }
        binding.switchWeightPublic.setOnClickListener { if (!isInitialLoad) updateLockState() }
        binding.switchWorkoutsPublic.setOnClickListener { if (!isInitialLoad) updateLockState() }
        binding.switchFriendsPublic.setOnClickListener { if (!isInitialLoad) updateLockState() }
    }

    private fun updateLockState() {
        navigationLockViewModel.setLocked(hasUnsavedChanges())
    }

    private fun hasUnsavedChanges(): Boolean {
        if (isInitialLoad) return false
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
               binding.switchFriendsPublic.isChecked != original.isFriendsListPublic
    }

    private fun showUnsavedChangesDialog(onDiscard: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
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

        val isHeightPublic = binding.switchHeightPublic.isChecked
        val isWeightPublic = binding.switchWeightPublic.isChecked
        val isWorkoutsPublic = binding.switchWorkoutsPublic.isChecked
        val isFriendsPublic = binding.switchFriendsPublic.isChecked

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
                theme = "dark",
                isHeightPublic = isHeightPublic,
                isWeightPublic = isWeightPublic,
                isWorkoutsPublic = isWorkoutsPublic,
                isFriendsListPublic = isFriendsPublic
            )
            
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
