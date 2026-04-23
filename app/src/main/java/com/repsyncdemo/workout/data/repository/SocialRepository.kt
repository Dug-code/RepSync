package com.repsyncdemo.workout.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.repsyncdemo.workout.data.model.Friendship
import com.repsyncdemo.workout.data.model.FriendshipStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SocialRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val friendshipsCollection = db.collection("friendships")

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    fun getFriends(userId: String? = null): Flow<List<Friendship>> = callbackFlow {
        val targetId = userId ?: currentUserId
        if (targetId == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val listener = friendshipsCollection
            .whereEqualTo("status", FriendshipStatus.ACCEPTED.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("SocialRepository", "Error fetching friends", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val friendships = snapshot?.toObjects(Friendship::class.java)
                    ?.filter { it.requesterId == targetId || it.receiverId == targetId }
                    ?: emptyList()
                trySend(friendships)
            }
        awaitClose { listener.remove() }
    }

    // New: Observe ALL friendships (Pending, Accepted, etc) involving the current user
    fun getMyFriendships(): Flow<List<Friendship>> = callbackFlow {
        val uid = currentUserId
        if (uid == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val listener = friendshipsCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val friendships = snapshot?.toObjects(Friendship::class.java)
                    ?.filter { it.requesterId == uid || it.receiverId == uid }
                    ?: emptyList()
                trySend(friendships)
            }
        awaitClose { listener.remove() }
    }

    fun getPendingRequests(): Flow<List<Friendship>> = callbackFlow {
        val uid = currentUserId
        if (uid == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val listener = friendshipsCollection
            .whereEqualTo("receiverId", uid)
            .whereEqualTo("status", FriendshipStatus.PENDING.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("SocialRepository", "Error fetching pending requests", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val requests = snapshot?.toObjects(Friendship::class.java)
                    ?.sortedByDescending { it.createdAt } ?: emptyList()
                trySend(requests)
            }
        awaitClose { listener.remove() }
    }

    fun getFriendshipWithUser(otherUserId: String): Flow<Friendship?> = callbackFlow {
        val uid = currentUserId
        if (uid == null) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val listener = friendshipsCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                val friendship = snapshot?.toObjects(Friendship::class.java)
                    ?.find { (it.requesterId == uid && it.receiverId == otherUserId) || 
                             (it.requesterId == otherUserId && it.receiverId == uid) }
                trySend(friendship)
            }
        awaitClose { listener.remove() }
    }

    suspend fun sendFriendRequest(
        receiverId: String,
        receiverUsername: String,
        senderUsername: String
    ): Result<Unit> {
        return try {
            val uid = currentUserId ?: throw IllegalStateException("User not logged in")
            val friendship = Friendship(
                requesterId = uid,
                requesterUsername = senderUsername,
                receiverId = receiverId,
                receiverUsername = receiverUsername,
                status = FriendshipStatus.PENDING
            )
            friendshipsCollection.add(friendship).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptRequest(friendshipId: String): Result<Unit> {
        return try {
            friendshipsCollection.document(friendshipId)
                .update("status", FriendshipStatus.ACCEPTED.name)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun declineRequest(friendshipId: String): Result<Unit> {
        return try {
            friendshipsCollection.document(friendshipId)
                .update("status", FriendshipStatus.DECLINED.name)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFriendship(friendshipId: String): Result<Unit> {
        return try {
            friendshipsCollection.document(friendshipId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
