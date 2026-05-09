package com.repsyncdemo.workout.data.model

/**
 * File overview: Defines the Notification data model used by repositories, view models, and UI binding.
 */

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Data class representing a system or user-specific notification.
 * Used for moderation alerts, friend requests, and system updates.
 */
data class Notification(
    @DocumentId
    val id: String = "",
    
    @get:PropertyName("userId")
    @set:PropertyName("userId")
    var userId: String = "",
    
    val title: String = "",
    val message: String = "",
    val type: NotificationType = NotificationType.SYSTEM,
    
    val relatedId: String = "",
    
    val createdAt: Long = System.currentTimeMillis(),
    
    @get:PropertyName("isRead")
    @set:PropertyName("isRead")
    var isRead: Boolean = false
)

/**
 * Defines the category of the notification to handle different UI behaviors.
 */
enum class NotificationType {
    MODERATION,      // Used when a moderator takes action on user content
    SYSTEM,          // General system updates
    FRIEND_REQUEST,  // Social alerts
    ACHIEVEMENT      // Trophy or goal related alerts
}
