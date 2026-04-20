package com.repsyncdemo.workout.viewmodel

import android.util.Log
import androidx.lifecycle.*
import com.repsyncdemo.workout.data.model.Friendship
import com.repsyncdemo.workout.data.model.UserProfile
import com.repsyncdemo.workout.data.repository.ProfileRepository
import com.repsyncdemo.workout.data.repository.SocialRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class SocialViewModel : ViewModel() {

    private val repository = SocialRepository()
    private val profileRepository = ProfileRepository()

    val friends: LiveData<List<Friendship>> = repository.getFriends()
        .catch { e ->
            Log.e("SocialViewModel", "Error in friends flow", e)
            emit(emptyList())
        }
        .asLiveData()

    val myFriendships: LiveData<List<Friendship>> = repository.getMyFriendships()
        .catch { e ->
            Log.e("SocialViewModel", "Error in myFriendships flow", e)
            emit(emptyList())
        }
        .asLiveData()

    private val _targetUserFriends = MutableLiveData<List<Friendship>>()
    val targetUserFriends: LiveData<List<Friendship>> = _targetUserFriends

    val pendingRequests: LiveData<List<Friendship>> = repository.getPendingRequests()
        .catch { e ->
            Log.e("SocialViewModel", "Error in pendingRequests flow", e)
            emit(emptyList())
        }
        .asLiveData()

    private val _friendshipWithTarget = MutableLiveData<Friendship?>()
    val friendshipWithTarget: LiveData<Friendship?> = _friendshipWithTarget

    private val _userProfiles = MutableLiveData<Map<String, UserProfile>>(emptyMap())
    val userProfiles: LiveData<Map<String, UserProfile>> = _userProfiles

    private var profileObservationJob: Job? = null

    init {
        // Observe profiles for the current user's friends/requests
        friends.observeForever { updateProfileObservation(it, pendingRequests.value ?: emptyList()) }
        pendingRequests.observeForever { updateProfileObservation(friends.value ?: emptyList(), it) }
    }

    private fun updateProfileObservation(friends: List<Friendship>, requests: List<Friendship>) {
        val userIds = (friends + requests).flatMap { listOf(it.requesterId, it.receiverId) }.distinct()
        if (userIds.isEmpty()) return

        profileObservationJob?.cancel()
        profileObservationJob = viewModelScope.launch {
            profileRepository.observeProfiles(userIds).collect { profiles ->
                _userProfiles.value = profiles
            }
        }
    }

    fun loadFriendsForUser(userId: String) {
        viewModelScope.launch {
            repository.getFriends(userId)
                .catch { e ->
                    Log.e("SocialViewModel", "Error loading friends for user $userId", e)
                    emit(emptyList())
                }
                .collect {
                    _targetUserFriends.value = it
                }
        }
    }

    fun loadFriendshipWithUser(otherUserId: String) {
        viewModelScope.launch {
            repository.getFriendshipWithUser(otherUserId).collect {
                _friendshipWithTarget.value = it
            }
        }
    }

    fun sendFriendRequest(receiverId: String, receiverUsername: String, senderUsername: String) {
        viewModelScope.launch {
            repository.sendFriendRequest(receiverId, receiverUsername, senderUsername)
        }
    }

    fun acceptRequest(friendshipId: String) {
        viewModelScope.launch {
            repository.acceptRequest(friendshipId)
        }
    }

    fun declineRequest(friendshipId: String) {
        viewModelScope.launch {
            repository.declineRequest(friendshipId)
        }
    }

    fun removeFriendship(friendshipId: String) {
        viewModelScope.launch {
            repository.removeFriendship(friendshipId)
        }
    }
}
