package com.repsyncdemo.workout.data.repository

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
     * Used to trigger pop-ups in the UI as soon as a notification is created.
     */
    fun observeUnreadNotifications(userId: String): Flow<List<Notification>> = callbackFlow {
        val listener = notificationsCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                val notifications = snapshot?.toObjects(Notification::class.java) ?: emptyList()
                trySend(notifications)
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
