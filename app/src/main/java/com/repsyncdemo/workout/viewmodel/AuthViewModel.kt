package com.repsyncdemo.workout.viewmodel

/**
 * File overview: Handles login and registration requests through the authentication repository.
 */

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.repsyncdemo.workout.data.repository.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val auth = FirebaseAuth.getInstance()

    private val _loginResult = MutableLiveData<Result<FirebaseUser>>()
    val loginResult: LiveData<Result<FirebaseUser>> = _loginResult

    private val _registerResult = MutableLiveData<Result<FirebaseUser>>()
    val registerResult: LiveData<Result<FirebaseUser>> = _registerResult

    // LiveData to observe the result of a password reset request
    private val _resetPasswordResult = MutableLiveData<Result<Unit>>()
    val resetPasswordResult: LiveData<Result<Unit>> = _resetPasswordResult

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    val isLoggedIn: Boolean
        get() = repository.isLoggedIn

    fun login(email: String, password: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.login(email, password)
            _loginResult.value = result
            _isLoading.value = false
        }
    }

    fun register(email: String, password: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.register(email, password)
            _registerResult.value = result
            _isLoading.value = false
        }
    }

    /**
     * Initiates a password reset process by sending an email to the user via the AuthRepository.
     * The result is posted to [resetPasswordResult].
     */
    fun resetPassword(email: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.sendPasswordResetEmail(email)
            _resetPasswordResult.value = result
            _isLoading.value = false
        }
    }

    fun signInWithGoogle(idToken: String) {
        _isLoading.value = true
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if (task.isSuccessful) {
                    val user = task.result?.user
                    if (user != null) {
                        _loginResult.value = Result.success(user)
                    } else {
                        _loginResult.value = Result.failure(Exception("User is null"))
                    }
                } else {
                    _loginResult.value = Result.failure(
                        task.exception ?: Exception("Google Sign-In failed")
                    )
                }
            }
    }

    fun logout() {
        repository.logout()
    }
}
