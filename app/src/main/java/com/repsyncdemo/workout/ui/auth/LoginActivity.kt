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
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels()


    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                viewModel.signInWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign in failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force Dark Mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        if (viewModel.isLoggedIn) {
            checkProfileAndNavigate()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("201248764409-3qs290rkuph1vpdf9lq24sm05pthsnnn.apps.googleusercontent.com") // REPLACE THIS WITH YOUR WEB CLIENT ID
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

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

        // --- CONTINUE WITH GOOGLE: Click Listener ---
        binding.btnGoogle.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnLogin.isEnabled = !isLoading
            // --- CONTINUE WITH GOOGLE: Disable while loading ---
            binding.btnGoogle.isEnabled = !isLoading
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

            result.onSuccess {
                // Always Dark Mode
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()
            }.onFailure {
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
