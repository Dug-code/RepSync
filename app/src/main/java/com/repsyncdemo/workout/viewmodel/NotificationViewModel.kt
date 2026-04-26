package com.repsyncdemo.workout.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.repsyncdemo.workout.data.repository.NotificationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing and providing notification data to the UI.
 * Watches for both unread (pop-ups) and all (list view) notifications.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationViewModel : ViewModel() {
    private val repository = NotificationRepository()
    private val auth = FirebaseAuth.getInstance()

    // Holds the current user's ID to reactively update the notification stream on login/logout
    private val _userId = MutableStateFlow(auth.currentUser?.uid)

    /**
     * A LiveData stream of unread notifications for the currently logged-in user.
     * Used for real-time pop-up alerts and badges.
     */
    val unreadNotifications = _userId.flatMapLatest { id ->
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
    val allNotifications = _userId.flatMapLatest { id ->
        if (id != null) {
            repository.observeAllNotifications(id)
        } else {
            flowOf(emptyList())
        }
    }.asLiveData()

    /**
     * Updates the internal user ID state. Should be called when the authentication state changes.
     */
    fun refreshUser() {
        _userId.value = auth.currentUser?.uid
    }

    /**
     * Marks a specific notification as read in the database.
     */
    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            repository.markAsRead(notificationId)
        }
    }
}
