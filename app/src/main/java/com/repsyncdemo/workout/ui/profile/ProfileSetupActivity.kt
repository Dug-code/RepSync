package com.repsyncdemo.workout.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.repsyncdemo.workout.MainActivity
import com.repsyncdemo.workout.data.model.UserProfile
import com.repsyncdemo.workout.databinding.ActivityProfileSetupBinding
import com.repsyncdemo.workout.viewmodel.ProfileViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ProfileSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileSetupBinding
    private val viewModel: ProfileViewModel by viewModels()
    private var usernameCheckJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.etUsername.addTextChangedListener { text ->
            val username = text?.toString()?.trim() ?: ""
            binding.tilUsername.error = null
            
            usernameCheckJob?.cancel()
            if (username.length >= 3) {
                usernameCheckJob = lifecycleScope.launch {
                    delay(500)
                    val available = viewModel.isUsernameAvailable(username)
                    if (!available) {
                        binding.tilUsername.error = "Username is already taken"
                    }
                }
            }
        }

        binding.btnSaveProfile.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val feet = binding.etHeightFeet.text.toString().toIntOrNull() ?: 0
            val inches = binding.etHeightInches.text.toString().toIntOrNull() ?: 0
            val weight = binding.etWeight.text.toString().toDoubleOrNull() ?: 0.0

            var hasError = false

            if (username.isEmpty()) {
                binding.tilUsername.error = "Username is required"
                hasError = true
            } else if (username.length < 3) {
                binding.tilUsername.error = "Username must be at least 3 characters"
                hasError = true
            }

            if (feet <= 0 && inches <= 0) {
                Toast.makeText(this, "Please enter your height", Toast.LENGTH_SHORT).show()
                hasError = true
            }

            if (weight <= 0) {
                binding.etWeight.error = "Please enter your weight"
                hasError = true
            }

            if (hasError || binding.tilUsername.error != null) {
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
            val loading = isLoading == true
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnSaveProfile.isEnabled = !loading
        }

        viewModel.profileResult.observe(this) { result ->
            result.onSuccess {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            result.onFailure { e ->
                if (e.message == "Username is already taken") {
                    binding.tilUsername.error = e.message
                } else {
                    Toast.makeText(this, e.message ?: "Failed to save profile", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
