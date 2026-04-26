package com.repsyncdemo.workout.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.repsyncdemo.workout.data.model.Notification
import com.repsyncdemo.workout.data.model.NotificationType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository responsible for managing notifications in Firestore.
 * Handles sending, observing, and updating notification statuses.
 */
class NotificationRepository {
    private val db = FirebaseFirestore.getInstance()
    private val notificationsCollection = db.collection("notifications")

    /**
     * General purpose method to add a notification to the database.
     */
    suspend fun sendNotification(notification: Notification): Result<Unit> {
        return try {
            notificationsCollection.add(notification).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Specialized helper to send a moderation alert to a user.
     * @param targetUserId The user whose content was removed.
     * @param postDescription A brief snippet of the content that was deleted.
     */
    suspend fun sendModerationNotification(targetUserId: String, postDescription: String): Result<Unit> {
        val notification = Notification(
            userId = targetUserId,
            title = "Post Removed",
            message = "A moderator has removed your post: \"$postDescription\"",
            type = NotificationType.MODERATION
        )
        return sendNotification(notification)
    }

    /**
     * Provides a real-time stream of unread notifications for a specific user.
     * Sorting is done in-memory to avoid mandatory Firestore composite index requirements.
     */
    fun observeUnreadNotifications(userId: String): Flow<List<Notification>> = callbackFlow {
        val listener = notificationsCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("NotificationRepo", "Error observing unread notifications", error)
                    trySend(emptyList()) // Emit empty list so UI can show empty state or handle error
                    return@addSnapshotListener
                }
                val notifications = snapshot?.toObjects(Notification::class.java) ?: emptyList()
                // Sort by creation date descending
                trySend(notifications.sortedByDescending { it.createdAt })
            }
        awaitClose { listener.remove() }
    }

    /**
     * Provides a real-time stream of ALL notifications for a specific user.
     * Sorting is done in-memory to avoid mandatory Firestore composite index requirements.
     */
    fun observeAllNotifications(userId: String): Flow<List<Notification>> = callbackFlow {
        val listener = notificationsCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("NotificationRepo", "Error observing all notifications", error)
                    trySend(emptyList()) // Emit empty list so UI can show empty state or handle error
                    return@addSnapshotListener
                }
                val notifications = snapshot?.toObjects(Notification::class.java) ?: emptyList()
                // Sort by creation date descending
                trySend(notifications.sortedByDescending { it.createdAt })
            }
        awaitClose { listener.remove() }
    }

    /**
     * Updates the status of a notification to read.
     * Prevents the notification from appearing in the unread pop-up stream again.
     */
    suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            notificationsCollection.document(notificationId).update("isRead", true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
