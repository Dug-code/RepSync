package com.repsyncdemo.workout.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.repsyncdemo.workout.MainActivity
import com.repsyncdemo.workout.data.repository.ProfileRepository
import com.repsyncdemo.workout.databinding.ActivityLoginBinding
import com.repsyncdemo.workout.ui.profile.ProfileSetupActivity
import com.repsyncdemo.workout.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (viewModel.isLoggedIn) {
            checkProfileAndNavigate()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty()) {
                binding.etEmail.error = "Email is required"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.etPassword.error = "Password is required"
                return@setOnClickListener
            }

            viewModel.login(email, password)
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnLogin.isEnabled = !isLoading
        }

        viewModel.loginResult.observe(this) { result ->
            result.onSuccess {
                checkProfileAndNavigate()
            }
            result.onFailure { e ->
                Toast.makeText(this, e.message ?: "Login failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkProfileAndNavigate() {
        lifecycleScope.launch {
            val profileRepo = ProfileRepository()
            val result = profileRepo.getProfile()
            
            result.onSuccess { profile ->
                // Apply saved theme preference immediately
                if (profile.theme == "light") {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
                
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()
            }.onFailure {
                // If no profile, default to Dark Mode for the setup process
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

                val hasProfile = profileRepo.hasProfile()
                if (hasProfile) {
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                } else {
                    startActivity(Intent(this@LoginActivity, ProfileSetupActivity::class.java))
                }
                finish()
            }
        }
    }
}
