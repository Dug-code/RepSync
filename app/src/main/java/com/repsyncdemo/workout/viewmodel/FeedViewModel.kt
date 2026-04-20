package com.repsyncdemo.workout.viewmodel

import android.util.Log
import androidx.lifecycle.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import com.repsyncdemo.workout.data.model.FeedPost
import com.repsyncdemo.workout.data.model.FeedPostType
import com.repsyncdemo.workout.data.model.UserProfile
import com.repsyncdemo.workout.data.repository.FeedRepository
import com.repsyncdemo.workout.data.repository.ProfileRepository
import com.repsyncdemo.workout.data.repository.SocialRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FeedViewModel : ViewModel() {

    private val repository = FeedRepository()
    private val socialRepository = SocialRepository()
    private val profileRepository = ProfileRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _rawPosts = MutableLiveData<List<FeedPost>>()
    private val _userProfiles = MutableLiveData<Map<String, UserProfile>>(emptyMap())
    val userProfiles: LiveData<Map<String, UserProfile>> = _userProfiles

    /**
     * The unified feed stream.
     * Enriches raw posts with real-time profile data and filters distance display.
     */
    val feedPosts = MediatorLiveData<List<FeedPost>>().apply {
        addSource(_rawPosts) { posts -> value = enrichAndFilter(posts, _userProfiles.value ?: emptyMap()) }
        addSource(_userProfiles) { profiles -> value = enrichAndFilter(_rawPosts.value ?: emptyList(), profiles) }
    }

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isLocationAvailable = MutableLiveData(false)
    val isLocationAvailable: LiveData<Boolean> = _isLocationAvailable

    private var userLocation: GeoPoint? = null
    private var profileObservationJob: Job? = null
    
    data class FeedFilters(
        val showChat: Boolean = true,
        val onlyFriends: Boolean = false,
        val radius: Double? = null,
        val showMyPosts: Boolean = true
    )
    
    private var currentFilters = FeedFilters(showChat = false, onlyFriends = false, radius = null)

    private val currentUserId: String
        get() = auth.currentUser?.uid ?: ""

    /**
     * Enriches posts with latest profile data and determines if distance should be shown.
     */
    private fun enrichAndFilter(posts: List<FeedPost>, profiles: Map<String, UserProfile>): List<FeedPost> {
        return posts.map { post ->
            val profile = profiles[post.userId]
            val distance = if (!currentFilters.onlyFriends && !currentFilters.showChat && currentFilters.radius != null) post.distanceMiles else null
            
            if (profile != null) {
                post.copy(
                    username = profile.username,
                    userProfilePicture = profile.profilePictureUrl
                ).apply { distanceMiles = distance }
            } else {
                post.apply { distanceMiles = distance }
            }
        }
    }

    fun setUserLocation(latitude: Double, longitude: Double) {
        userLocation = GeoPoint(latitude, longitude)
        _isLocationAvailable.value = true
        loadFeed()
    }

    fun setLocationDisabled() {
        userLocation = null
        _isLocationAvailable.value = false
        if (currentFilters.radius != null) {
            applyFilters(radius = null)
        } else {
            loadFeed()
        }
    }

    fun applyFilters(
        showChat: Boolean = currentFilters.showChat,
        onlyFriends: Boolean = currentFilters.onlyFriends,
        radius: Double? = currentFilters.radius,
        showMyPosts: Boolean = currentFilters.showMyPosts
    ) {
        currentFilters = FeedFilters(showChat, onlyFriends, radius, showMyPosts)
        loadFeed()
    }

    fun loadFeed() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val friendIds = if (currentFilters.onlyFriends) {
                    socialRepository.getFriends().first().map { 
                        if (it.requesterId == currentUserId) it.receiverId else it.requesterId 
                    }
                } else emptyList()

                repository.getFeed(
                    userLocation = userLocation,
                    friendIds = friendIds,
                    showChat = currentFilters.showChat,
                    onlyFriends = currentFilters.onlyFriends,
                    radius = currentFilters.radius,
                    showMyPosts = currentFilters.showMyPosts
                ).catch { e ->
                    Log.e("FeedViewModel", "Error in feed", e)
                    emit(emptyList())
                }.collect { posts ->
                    _rawPosts.value = posts
                    observeUserProfiles(posts)
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("FeedViewModel", "Failed to load feed", e)
                _isLoading.value = false
            }
        }
    }

    private fun observeUserProfiles(posts: List<FeedPost>) {
        val userIds = posts.map { it.userId }.distinct()
        if (userIds.isEmpty()) return

        profileObservationJob?.cancel()
        profileObservationJob = viewModelScope.launch {
            profileRepository.observeProfiles(userIds).collect { profiles ->
                _userProfiles.value = profiles
            }
        }
    }

    fun sendChatMessage(message: String) {
        if (message.isBlank()) return
        viewModelScope.launch {
            val profile = profileRepository.getProfile().getOrNull()
            val post = FeedPost(
                userId = currentUserId,
                username = profile?.username ?: "User",
                userProfilePicture = profile?.profilePictureUrl ?: "red",
                description = message,
                type = FeedPostType.CHAT_MESSAGE,
                location = userLocation
            )
            repository.createPost(post)
        }
    }

    fun toggleReaction(postId: String?, emoji: String) {
        viewModelScope.launch {
            repository.toggleReaction(postId, emoji)
        }
    }

    fun createPost(post: FeedPost) {
        _isLoading.value = true
        viewModelScope.launch {
            val profile = profileRepository.getProfile().getOrNull()
            val postWithExtras = post.copy(
                location = userLocation,
                userProfilePicture = profile?.profilePictureUrl ?: "red"
            )
            repository.createPost(postWithExtras)
            _isLoading.value = false
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            repository.deletePost(postId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        profileObservationJob?.cancel()
    }
}
