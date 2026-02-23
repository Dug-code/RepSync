package com.repsyncdemo.workout.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.repsyncdemo.workout.MainActivity
import com.repsyncdemo.workout.data.model.UserProfile
import com.repsyncdemo.workout.databinding.ActivityProfileSetupBinding
import com.repsyncdemo.workout.viewmodel.ProfileViewModel

class ProfileSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileSetupBinding
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnSaveProfile.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val feet = binding.etHeightFeet.text.toString().toIntOrNull() ?: 0
            val inches = binding.etHeightInches.text.toString().toIntOrNull() ?: 0
            val weight = binding.etWeight.text.toString().toDoubleOrNull() ?: 0.0

            if (username.isEmpty()) {
                binding.etUsername.error = "Username is required"
                return@setOnClickListener
            }
            if (username.length < 3) {
                binding.etUsername.error = "Username must be at least 3 characters"
                return@setOnClickListener
            }

            val totalInches = (feet * 12) + inches
            val email = FirebaseAuth.getInstance().currentUser?.email ?: ""

            val profile = UserProfile(
                username = username,
                email = email,
                heightInches = totalInches,
                weightLbs = weight,
                isPublicAccount = binding.switchPublicAccount.isChecked,
                shareGoalsAndProgress = binding.switchShareProgress.isChecked
            )

            viewModel.createProfile(profile)
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSaveProfile.isEnabled = !isLoading
        }

        viewModel.profileResult.observe(this) { result ->
            result.onSuccess {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            result.onFailure { e ->
                Toast.makeText(this, e.message ?: "Failed to save profile", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
