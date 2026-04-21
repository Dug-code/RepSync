package com.repsyncdemo.workout.viewmodel

import android.util.Log
import androidx.lifecycle.*
import com.repsyncdemo.workout.data.model.FeedPost
import com.repsyncdemo.workout.data.model.UserProfile
import com.repsyncdemo.workout.data.repository.FeedRepository
import com.repsyncdemo.workout.data.repository.ProfileRepository
import com.repsyncdemo.workout.util.SingleLiveEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val repository = ProfileRepository()
    private val feedRepository = FeedRepository()

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

    /**
     * Start observing a profile in real-time.
     * Cancels any previous observation job to prevent multiple listeners.
     */
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

    // Restored for backward compatibility during migration
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

    fun updateProfile(profile: UserProfile) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.updateProfile(profile)
            _profileResult.value = result
            _isLoading.value = false
        }
    }

    fun incrementRestDays() {
        val profile = myProfile.value ?: return
        val updatedProfile = profile.copy(totalRestDays = profile.totalRestDays + 1)
        updateProfile(updatedProfile)
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
