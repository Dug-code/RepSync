package com.repsyncdemo.workout.viewmodel

import android.util.Log
import androidx.lifecycle.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import com.repsyncdemo.workout.data.model.FeedPost
import com.repsyncdemo.workout.data.model.FeedPostType
import com.repsyncdemo.workout.data.model.UserProfile
import com.repsyncdemo.workout.data.repository.FeedRepository
import com.repsyncdemo.workout.data.repository.NotificationRepository
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
    private val notificationRepository = NotificationRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _rawExplorePosts = MutableLiveData<List<FeedPost>>()
    private val _rawFriendsPosts = MutableLiveData<List<FeedPost>>()
    private val _rawChatPosts = MutableLiveData<List<FeedPost>>()

    private val _userProfiles = MutableLiveData<Map<String, UserProfile>>(emptyMap())
    val userProfiles: LiveData<Map<String, UserProfile>> = _userProfiles

    val explorePosts: LiveData<List<FeedPost>> = _rawExplorePosts
    val friendsPosts: LiveData<List<FeedPost>> = _rawFriendsPosts
    val chatPosts: LiveData<List<FeedPost>> = _rawChatPosts

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isLocationAvailable = MutableLiveData(false)
    val isLocationAvailable: LiveData<Boolean> = _isLocationAvailable

    private var userLocation: GeoPoint? = null
    
    private var exploreJob: Job? = null
    private var friendsJob: Job? = null
    private var chatJob: Job? = null
    private var profileObservationJob: Job? = null
    
    private var exploreRadius: Double? = null
    private var showMyPostsInFriends = true
    private var userLocationUpdatedAt: Long = 0L

    private val maxLocationAgeMillis = 10 * 60 * 1000L

    private val currentUserId: String
        get() = auth.currentUser?.uid ?: ""

    fun setUserLocation(latitude: Double, longitude: Double) {
        userLocation = GeoPoint(latitude, longitude)
        userLocationUpdatedAt = System.currentTimeMillis()
        _isLocationAvailable.value = true
        loadExploreFeed()
        loadChatFeed()
    }

    fun setLocationDisabled() {
        userLocation = null
        userLocationUpdatedAt = 0L
        _isLocationAvailable.value = false
        if (exploreRadius != null) {
            applyExploreFilters(radius = null)
        } else {
            loadExploreFeed()
        }
    }

    fun applyExploreFilters(radius: Double?) {
        exploreRadius = radius
        loadExploreFeed()
    }

    fun applyFriendsFilters(showMyPosts: Boolean) {
        showMyPostsInFriends = showMyPosts
        loadFriendsFeed()
    }

    fun refreshAllFeeds() {
        // Clear profile cache to force re-download of latest avatars
        _userProfiles.value = emptyMap()
        loadExploreFeed()
        loadFriendsFeed()
        loadChatFeed()
    }

    fun loadExploreFeed() {
        exploreJob?.cancel()
        _isLoading.value = true
        val locationForFeed = getFreshUserLocation()
        exploreJob = viewModelScope.launch {
            try {
                repository.getFeed(
                    userLocation = locationForFeed,
                    friendIds = emptyList(),
                    showChat = false,
                    onlyFriends = false,
                    radius = exploreRadius,
                    showMyPosts = true
                ).catch { e ->
                    Log.e("FeedViewModel", "Error in explore feed", e)
                    emit(emptyList())
                }.collect { posts ->
                    _rawExplorePosts.value = posts
                    observeUserProfiles()
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("FeedViewModel", "Failed to load explore feed", e)
                _isLoading.value = false
            }
        }
    }

    fun loadFriendsFeed() {
        friendsJob?.cancel()
        val locationForFeed = getFreshUserLocation()
        friendsJob = viewModelScope.launch {
            try {
                val friendIds = socialRepository.getFriends().first().map { 
                    if (it.requesterId == currentUserId) it.receiverId else it.requesterId 
                }
                repository.getFeed(
                    userLocation = locationForFeed,
                    friendIds = friendIds,
                    showChat = false,
                    onlyFriends = true,
                    radius = null,
                    showMyPosts = showMyPostsInFriends
                ).catch { e ->
                    Log.e("FeedViewModel", "Error in friends feed", e)
                    emit(emptyList())
                }.collect { posts ->
                    _rawFriendsPosts.value = posts
                    observeUserProfiles()
                }
            } catch (e: Exception) {
                Log.e("FeedViewModel", "Failed to load friends feed", e)
            }
        }
    }

    fun loadChatFeed() {
        chatJob?.cancel()
        val locationForFeed = getFreshUserLocation()
        chatJob = viewModelScope.launch {
            try {
                repository.getFeed(
                    userLocation = locationForFeed,
                    friendIds = emptyList(),
                    showChat = true,
                    onlyFriends = false,
                    radius = null,
                    showMyPosts = true
                ).catch { e ->
                    Log.e("FeedViewModel", "Error in chat feed", e)
                    emit(emptyList())
                }.collect { posts ->
                    _rawChatPosts.value = posts
                    observeUserProfiles()
                }
            } catch (e: Exception) {
                Log.e("FeedViewModel", "Failed to load chat feed", e)
            }
        }
    }

    private fun observeUserProfiles() {
        val allPosts = (_rawExplorePosts.value ?: emptyList()) + 
                       (_rawFriendsPosts.value ?: emptyList()) + 
                       (_rawChatPosts.value ?: emptyList())
                       
        val userIds = allPosts.map { it.userId }.distinct()
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
                location = getFreshUserLocation()
            )
            repository.createPost(post)
        }
    }

    fun updateChatMessage(postId: String, newText: String) {
        viewModelScope.launch {
            repository.updatePost(postId, newText)
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
                location = getFreshUserLocation(),
                userProfilePicture = profile?.profilePictureUrl ?: "red"
            )
            repository.createPost(postWithExtras)
            _isLoading.value = false
        }
    }

    fun deletePost(post: FeedPost) {
        viewModelScope.launch {
            val result = repository.deletePost(post.id)
            if (result.isSuccess) {
                // If the post being deleted belongs to someone else, it's a moderation action
                if (post.userId != currentUserId) {
                    val displayDescription = if (post.type == FeedPostType.CHAT_MESSAGE) {
                        post.description
                    } else {
                        post.workoutName.ifEmpty { post.goalTitle.ifEmpty { "Post" } }
                    }
                    notificationRepository.sendModerationNotification(post.userId, displayDescription)
                }
            }
        }
    }

    private fun getFreshUserLocation(): GeoPoint? {
        val location = userLocation ?: return null
        val isFresh = System.currentTimeMillis() - userLocationUpdatedAt <= maxLocationAgeMillis
        if (isFresh) return location

        userLocation = null
        userLocationUpdatedAt = 0L
        _isLocationAvailable.value = false
        return null
    }

    override fun onCleared() {
        super.onCleared()
        exploreJob?.cancel()
        friendsJob?.cancel()
        chatJob?.cancel()
        profileObservationJob?.cancel()
    }
}
