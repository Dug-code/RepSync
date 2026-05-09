package com.repsyncdemo.workout.data.repository

/**
 * File overview: Owns Firestore reads and writes for feed posts, comments, reactions, and moderation operations.
 */

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import com.repsyncdemo.workout.data.model.FeedPost
import com.repsyncdemo.workout.data.model.FeedPostType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.math.*

class FeedRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val feedCollection = db.collection("feed")

    private val userId: String?
        get() = auth.currentUser?.uid

    // Reads data.
    fun getFeed(
        userLocation: GeoPoint?,
        friendIds: List<String> = emptyList(),
        showChat: Boolean = true,
        onlyFriends: Boolean = false,
        radius: Double? = null,
        showMyPosts: Boolean = true
    ): Flow<List<FeedPost>> = callbackFlow {
        val currentUid = userId
        val listener = feedCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                var posts = snapshot?.toObjects(FeedPost::class.java) ?: emptyList()

                // Filter by type
                posts = if (showChat) {
                    posts.filter { it.type == FeedPostType.CHAT_MESSAGE }
                } else {
                    posts.filter { it.type != FeedPostType.CHAT_MESSAGE }
                }

                // Filter by my posts
                if (!showMyPosts && currentUid != null) {
                    posts = posts.filter { it.userId != currentUid }
                }

                // Filter by friends
                if (onlyFriends) {
                    val targetIds = friendIds + if (showMyPosts && currentUid != null) listOf(currentUid) else emptyList()
                    posts = posts.filter { targetIds.contains(it.userId) }
                }

                // Only attach/display distances while the user is actively using radius mode.
                val currentLocation = userLocation
                val activeRadius = radius
                if (currentLocation != null && activeRadius != null && !onlyFriends) {
                    posts.forEach { post ->
                        post.distanceMiles = post.location?.let { location ->
                            distanceMiles(currentLocation, location)
                        }
                    }
                    posts = posts.filter {
                        it.distanceMiles != null && it.distanceMiles!! <= activeRadius
                    }
                } else {
                    posts.forEach { it.distanceMiles = null }
                }

                trySend(posts.sortedByDescending { it.createdAt })
            }
        awaitClose { listener.remove() }
    }

    // Reads data.
    fun getUserPosts(targetUserId: String, includeChat: Boolean = true): Flow<List<FeedPost>> = callbackFlow {
        val listener = feedCollection
            .whereEqualTo("userId", targetUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                var posts = snapshot?.toObjects(FeedPost::class.java) ?: emptyList()
                
                if (!includeChat) {
                    posts = posts.filter { it.type != FeedPostType.CHAT_MESSAGE }
                }
                
                trySend(posts.sortedByDescending { it.createdAt })
            }
        awaitClose { listener.remove() }
    }

    // Reads data.
    fun getMyPosts(includeChat: Boolean = true): Flow<List<FeedPost>> {
        val currentUid = userId ?: return callbackFlow { 
            trySend(emptyList())
            awaitClose { }
        }
        return getUserPosts(currentUid, includeChat)
    }

    // Writes data.
    suspend fun createPost(post: FeedPost): Result<String> {
        return try {
            val currentUid = userId ?: throw IllegalStateException("User not logged in")
            val postWithUser = post.copy(userId = currentUid)
            val doc = feedCollection.add(postWithUser).await()
            Result.success(doc.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Writes data.
    suspend fun updatePost(postId: String, description: String): Result<Unit> {
        return try {
            feedCollection.document(postId).update("description", description).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Reads data.
    suspend fun toggleLike(postId: String): Result<Unit> {
        return try {
            val currentUid = userId ?: throw IllegalStateException("User not logged in")
            val docRef = feedCollection.document(postId)
            val doc = docRef.get().await()
            val post = doc.toObject(FeedPost::class.java) ?: return Result.failure(Exception("Post not found"))
            
            val updatedLikes = post.likes.toMutableList()
            if (updatedLikes.contains(currentUid)) {
                updatedLikes.remove(currentUid)
            } else {
                updatedLikes.add(currentUid)
            }
            docRef.update("likes", updatedLikes).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Reads data.
    suspend fun toggleReaction(postId: String?, emoji: String): Result<Unit> {
        if (postId == null) return Result.failure(Exception("Post ID is null"))
        return try {
            val currentUid = userId ?: throw IllegalStateException("User not logged in")
            val docRef = feedCollection.document(postId)
            val doc = docRef.get().await()
            val post = doc.toObject(FeedPost::class.java) ?: return Result.failure(Exception("Post not found"))

            val updatedReactions = post.reactions.toMutableMap()
            if (updatedReactions[currentUid] == emoji) {
                updatedReactions.remove(currentUid)
            } else {
                updatedReactions[currentUid] = emoji
            }
            
            docRef.update("reactions", updatedReactions).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Writes data.
    suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            feedCollection.document(postId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun distanceMiles(a: GeoPoint, b: GeoPoint): Double {
        val earthRadiusMiles = 3958.8
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)

        val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        return 2 * earthRadiusMiles * asin(sqrt(h))
    }
}
