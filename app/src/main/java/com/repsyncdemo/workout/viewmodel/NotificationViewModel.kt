package com.repsyncdemo.workout.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.repsyncdemo.workout.data.repository.NotificationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing and providing notification data to the UI.
 * Watches for both unread (pop-ups) and all (list view) notifications.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationViewModel : ViewModel() {
    private val repository = NotificationRepository()
    private val auth = FirebaseAuth.getInstance()

    // Create a flow that emits the current user ID and updates whenever auth state changes
    private val userIdFlow = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.uid)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), auth.currentUser?.uid)

    /**
     * A LiveData stream of unread notifications for the currently logged-in user.
     * Used for real-time pop-up alerts and badges.
     */
    val unreadNotifications = userIdFlow.flatMapLatest { id ->
        if (id != null) {
            repository.observeUnreadNotifications(id)
        } else {
            flowOf(emptyList())
        }
    }.asLiveData()

    /**
     * A LiveData stream of ALL notifications for the currently logged-in user.
     * Used for the main Notifications history screen.
     */
    val allNotifications = userIdFlow.flatMapLatest { id ->
        if (id != null) {
            repository.observeAllNotifications(id)
        } else {
            flowOf(emptyList())
        }
    }.asLiveData()

    /**
     * Marks a specific notification as read in the database.
     */
    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            repository.markAsRead(notificationId)
        }
    }

    /**
     * Deletes a specific notification.
     */
    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            repository.deleteNotification(notificationId)
        }
    }

    /**
     * Deletes all notifications for the current user.
     */
    fun clearAllNotifications() {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            repository.deleteAllNotifications(uid)
        }
    }
}
