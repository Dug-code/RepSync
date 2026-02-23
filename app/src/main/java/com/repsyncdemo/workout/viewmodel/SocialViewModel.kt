package com.repsyncdemo.workout.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.repsyncdemo.workout.data.model.Friendship
import com.repsyncdemo.workout.data.repository.SocialRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class SocialViewModel : ViewModel() {

    private val repository = SocialRepository()

    val friends: LiveData<List<Friendship>> = repository.getFriends()
        .catch { e ->
            Log.e("SocialViewModel", "Error in friends flow", e)
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

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

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

    fun removeFriend(friendshipId: String) {
        viewModelScope.launch {
            repository.removeFriend(friendshipId)
        }
    }
}
