package com.repsyncdemo.workout.viewmodel

/**
 * File overview: Manages the signed-in user profile, profile edits, weight synchronization, trophies, and profile-related posts.
 */

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.repsyncdemo.workout.data.model.FeedPost
import com.repsyncdemo.workout.data.model.GoalType
import com.repsyncdemo.workout.data.model.UserProfile
import com.repsyncdemo.workout.data.repository.FeedRepository
import com.repsyncdemo.workout.data.repository.GoalRepository
import com.repsyncdemo.workout.data.repository.ProfileRepository
import com.repsyncdemo.workout.data.repository.WorkoutRepository
import com.repsyncdemo.workout.util.SingleLiveEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: ProfileRepository = ProfileRepository(),
    private val feedRepository: FeedRepository = FeedRepository(),
    private val goalRepository: GoalRepository = GoalRepository(),
    private val workoutRepository: WorkoutRepository = WorkoutRepository()
) : ViewModel() {

    companion object {
        private val USERNAME_PATTERN = Regex("^[A-Za-z0-9._]+$")
        private const val USERNAME_RULE_MESSAGE = "Username can only use letters, numbers, periods, and underscores"
    }

    private val _profileResult = SingleLiveEvent<Result<Unit>>()
    val profileResult: LiveData<Result<Unit>> = _profileResult

    private val _hasProfile = MutableLiveData<Boolean>()
    val hasProfile: LiveData<Boolean> = _hasProfile

    // Real-time observation of the logged-in user's profile
    val myProfile: LiveData<UserProfile?> = repository.observeProfile()
        .catch { e -> 
            Log.e("ProfileViewModel", "Error observing my profile", e)
            emit(null) 
        }
        .asLiveData()

    private val _currentProfile = MutableLiveData<UserProfile?>()
    val currentProfile: LiveData<UserProfile?> = _currentProfile

    private val _searchResults = MutableLiveData<List<UserProfile>>()
    val searchResults: LiveData<List<UserProfile>> = _searchResults

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _userPosts = MutableLiveData<List<FeedPost>>()
    val userPosts: LiveData<List<FeedPost>> = _userPosts

    private var profileObservationJob: Job? = null

    private val _myPosts = MutableLiveData<List<FeedPost>>()
    val myPosts: LiveData<List<FeedPost>> = _myPosts

    fun checkHasProfile() {
        viewModelScope.launch {
            _hasProfile.value = repository.hasProfile()
        }
    }

    fun observeProfile(userId: String? = null) {
        profileObservationJob?.cancel()
        profileObservationJob = viewModelScope.launch {
            repository.observeProfile(userId).collect { profile ->
                _currentProfile.value = profile
            }
        }
    }

    // Loads data.
    fun loadProfile(userId: String? = null) {
        observeProfile(userId)
    }

    // Loads data.
    fun loadMyPosts() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            feedRepository.getUserPosts(userId)
                .catch { e ->
                    Log.e("ProfileViewModel", "Error fetching my posts", e)
                    emit(emptyList())
                }
                .collect { posts ->
                    _myPosts.value = posts
                }
        }
    }

    // Loads data.
    fun loadUserPosts(userId: String) {
        viewModelScope.launch {
            feedRepository.getUserPosts(userId, includeChat = false)
                .catch { e ->
                    Log.e("ProfileViewModel", "Error fetching user posts", e)
                    emit(emptyList())
                }
                .collect { posts ->
                    _userPosts.value = posts
                }
        }
    }

    fun createProfile(profile: UserProfile) {
        _isLoading.value = true
        viewModelScope.launch {
            if (!isValidUsername(profile.username)) {
                _profileResult.value = Result.failure(Exception(USERNAME_RULE_MESSAGE))
                _isLoading.value = false
                return@launch
            }

            // Check availability one last time before creating
            if (!repository.isUsernameAvailable(profile.username)) {
                _profileResult.value = Result.failure(Exception("Username is already taken"))
                _isLoading.value = false
                return@launch
            }
            val result = repository.createProfile(profile)
            if (result.isSuccess && profile.weightLbs > 0) {
                workoutRepository.addWeightLog(profile.weightLbs)
            }
            _profileResult.value = result
            _isLoading.value = false
        }
    }

    // Reads data.
    suspend fun isUsernameAvailable(username: String): Boolean {
        return repository.isUsernameAvailable(username)
    }

    /**
     * Updates specific fields in the profile. This is the preferred method
     * to avoid overwriting data or encountering race conditions.
     */
    fun updateProfileFields(updates: Map<String, Any>) {
        _isLoading.value = true
        viewModelScope.launch {
            val requestedUsername = updates["username"] as? String
            val currentProfile = myProfile.value
            if (requestedUsername != null && !isValidUsername(requestedUsername)) {
                _profileResult.value = Result.failure(Exception(USERNAME_RULE_MESSAGE))
                _isLoading.value = false
                return@launch
            }

            if (
                requestedUsername != null &&
                currentProfile != null &&
                !requestedUsername.equals(currentProfile.username, ignoreCase = true) &&
                !repository.isUsernameAvailable(requestedUsername, currentProfile.userId)
            ) {
                _profileResult.value = Result.failure(Exception("Username is already taken"))
                _isLoading.value = false
                return@launch
            }

            val result = repository.updateProfileFields(updates)
            
            // Post-update logic for weight
            if (result.isSuccess && updates.containsKey("weightLbs")) {
                val newWeight = updates["weightLbs"] as? Double ?: 0.0
                if (newWeight > 0) {
                    syncWeightWithGoals(newWeight)
                    workoutRepository.addWeightLog(newWeight)
                }
            }
            
            _profileResult.value = result
            _isLoading.value = false
        }
    }

    /**
     * Legacy update method. Use updateProfileFields for specific changes.
     */
    fun updateProfile(profile: UserProfile) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.updateProfile(profile)
            if (result.isSuccess) {
                syncWeightWithGoals(profile.weightLbs)
                if (profile.weightLbs > 0) {
                    workoutRepository.addWeightLog(profile.weightLbs)
                }
            }
            _profileResult.value = result
            _isLoading.value = false
        }
    }

    fun pinTrophy(trophyId: String) {
        updateProfileFields(mapOf("pinnedTrophyId" to trophyId))
    }

    // Updates data or UI state.
    fun updateWeightAndHeight(weight: Double, heightInches: Int) {
        updateProfileFields(mapOf(
            "weightLbs" to weight,
            "heightInches" to heightInches
        ))
    }

    // Updates data or UI state.
    fun updateWeight(newWeight: Double) {
        updateProfileFields(mapOf("weightLbs" to newWeight))
    }

    private fun syncWeightWithGoals(newWeight: Double) {
        viewModelScope.launch {
            try {
                val goals = goalRepository.getGoals().first()
                goals.forEach { goal ->
                    if (goal.type == GoalType.WEIGHT_LOSS || goal.type == GoalType.WEIGHT_GAIN) {
                        goalRepository.updateProgress(goal.id, newWeight)
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error syncing weight with goals", e)
            }
        }
    }

    private fun isValidUsername(username: String): Boolean {
        return USERNAME_PATTERN.matches(username)
    }

    fun incrementRestDays() {
        val profile = myProfile.value ?: return
        updateProfileFields(mapOf("totalRestDays" to profile.totalRestDays + 1))
    }

    // Updates data or UI state.
    fun updateLocation(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            repository.updateLocation(latitude, longitude)
        }
    }

    fun searchUsers(query: String) {
        if (query.length < 2) return
        viewModelScope.launch {
            repository.searchUsers(query).onSuccess {
                _searchResults.value = it
            }
        }
    }
}
