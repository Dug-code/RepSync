package com.repsyncdemo.workout.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import com.repsyncdemo.workout.data.model.FeedPost
import com.repsyncdemo.workout.data.model.FeedPostType
import com.repsyncdemo.workout.data.repository.FeedRepository
import com.repsyncdemo.workout.data.repository.ProfileRepository
import com.repsyncdemo.workout.data.repository.SocialRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FeedViewModel : ViewModel() {

    private val repository = FeedRepository()
    private val socialRepository = SocialRepository()
    private val profileRepository = ProfileRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _feedPosts = MutableLiveData<List<FeedPost>>()
    val feedPosts: LiveData<List<FeedPost>> = _feedPosts

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private var userLocation: GeoPoint? = null
    
    data class FeedFilters(
        val showChat: Boolean = true,
        val onlyFriends: Boolean = false,
        val radius: Double? = null, // null means Global
        val showMyPosts: Boolean = true
    )
    
    private var currentFilters = FeedFilters(showChat = false, onlyFriends = false, radius = null)

    private val currentUserId: String
        get() = auth.currentUser?.uid ?: ""

    fun setUserLocation(latitude: Double, longitude: Double) {
        userLocation = GeoPoint(latitude, longitude)
        loadFeed()
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
                    _feedPosts.value = posts
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("FeedViewModel", "Failed to load feed", e)
                _isLoading.value = false
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
                userProfilePicture = profile?.profilePictureUrl ?: "",
                description = message,
                type = FeedPostType.CHAT_MESSAGE,
                location = userLocation
            )
            repository.createPost(post)
        }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            repository.toggleLike(postId)
        }
    }

    fun createPost(post: FeedPost) {
        _isLoading.value = true
        viewModelScope.launch {
            val profile = profileRepository.getProfile().getOrNull()
            val postWithExtras = post.copy(
                location = userLocation,
                userProfilePicture = profile?.profilePictureUrl ?: ""
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
}
