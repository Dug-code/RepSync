package com.repsyncdemo.workout.data.repository

/**
 * File overview: Owns Firestore reads and writes for notification records.
 */

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.repsyncdemo.workout.data.model.Notification
import com.repsyncdemo.workout.data.model.NotificationType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository responsible for managing notifications in Firestore.
 */
class NotificationRepository {
    private val db = FirebaseFirestore.getInstance()
    private val notificationsCollection = db.collection("notifications")

    suspend fun sendNotification(notification: Notification): Result<Unit> {
        return try {
            Log.d("NotificationRepo", "Sending notification to ${notification.userId}")
            notificationsCollection.add(notification).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Failed to send notification", e)
            Result.failure(e)
        }
    }

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
     * Observes unread notifications. 
     * IMPORTANT: Field names "userId" and "isRead" must match Firestore document fields exactly.
     */
    fun observeUnreadNotifications(userId: String): Flow<List<Notification>> = callbackFlow {
        Log.d("NotificationRepo", "Starting unread observation for: $userId")
        val listener = notificationsCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("NotificationRepo", "Error observing unread", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val notifications = snapshot?.toObjects(Notification::class.java) ?: emptyList()
                Log.d("NotificationRepo", "Found ${notifications.size} unread notifications for $userId")
                
                // Debugging: If you see documents in console but 0 here, it's a field name mismatch.
                if (notifications.isEmpty() && snapshot != null && !snapshot.isEmpty) {
                    Log.w("NotificationRepo", "Match found in Firestore, but failed to map! Check Notification.kt property names.")
                }

                trySend(notifications.sortedByDescending { it.createdAt })
            }
        awaitClose { listener.remove() }
    }

    fun observeAllNotifications(userId: String): Flow<List<Notification>> = callbackFlow {
        val listener = notificationsCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("NotificationRepo", "Error observing all", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val notifications = snapshot?.toObjects(Notification::class.java) ?: emptyList()
                trySend(notifications.sortedByDescending { it.createdAt })
            }
        awaitClose { listener.remove() }
    }

    suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            notificationsCollection.document(notificationId).update("isRead", true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return try {
            notificationsCollection.document(notificationId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAllNotifications(userId: String): Result<Unit> {
        return try {
            val snapshot = notificationsCollection.whereEqualTo("userId", userId).get().await()
            val batch = db.batch()
            snapshot.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
