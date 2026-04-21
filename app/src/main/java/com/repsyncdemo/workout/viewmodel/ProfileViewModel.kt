package com.repsyncdemo.workout.viewmodel

import android.util.Log
import androidx.lifecycle.*
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

class ProfileViewModel : ViewModel() {

    private val repository = ProfileRepository()
    private val feedRepository = FeedRepository()
    private val goalRepository = GoalRepository()
    private val workoutRepository = WorkoutRepository()

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

    val myPosts: LiveData<List<FeedPost>> = feedRepository.getMyPosts()
        .catch { e ->
            Log.e("ProfileViewModel", "Error fetching my posts", e)
            emit(emptyList())
        }
        .asLiveData()

    fun checkHasProfile() {
        viewModelScope.launch {
            _hasProfile.value = repository.hasProfile()
        }
    }

    fun observeProfile(userId: String? = null) {
        profileObservationJob?.cancel()
        profileObservationJob = viewModelScope.launch {
            repository.observeProfile(userId).collect {
                _currentProfile.value = it
                if (it != null) {
                    loadUserPosts(it.userId)
                }
            }
        }
    }

    fun loadProfile(userId: String? = null) {
        observeProfile(userId)
    }

    private fun loadUserPosts(userId: String) {
        viewModelScope.launch {
            feedRepository.getUserPosts(userId)
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
            _profileResult.value = repository.createProfile(profile)
            _isLoading.value = false
        }
    }

    /**
     * Updates specific fields in the profile. This is the preferred method
     * to avoid overwriting data or encountering race conditions.
     */
    fun updateProfileFields(updates: Map<String, Any>) {
        _isLoading.value = true
        viewModelScope.launch {
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

    fun updateWeightAndHeight(weight: Double, heightInches: Int) {
        updateProfileFields(mapOf(
            "weightLbs" to weight,
            "heightInches" to heightInches
        ))
    }

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

    fun incrementRestDays() {
        val profile = myProfile.value ?: return
        updateProfileFields(mapOf("totalRestDays" to profile.totalRestDays + 1))
    }

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
