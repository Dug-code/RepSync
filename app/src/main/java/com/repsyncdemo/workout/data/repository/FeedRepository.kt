package com.repsyncdemo.workout.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.repsyncdemo.workout.data.model.FeedPost
import com.repsyncdemo.workout.data.model.FeedPostType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class FeedRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val feedCollection = db.collection("feed")

    private val userId: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")

    fun getFeed(
        userLocation: GeoPoint?,
        friendIds: List<String> = emptyList(),
        showChat: Boolean = true,
        onlyFriends: Boolean = false,
        radius: Double? = null,
        showMyPosts: Boolean = true
    ): Flow<List<FeedPost>> = callbackFlow {
        val listener = feedCollection
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                var posts = snapshot?.toObjects(FeedPost::class.java) ?: emptyList()

                // Filter by type: Chat only if requested, or everything else
                if (showChat) {
                    posts = posts.filter { it.type == FeedPostType.CHAT_MESSAGE }
                } else {
                    posts = posts.filter { it.type != FeedPostType.CHAT_MESSAGE }
                }

                // Filter by my posts
                if (!showMyPosts) {
                    posts = posts.filter { it.userId != userId }
                }

                // Filter by friends
                if (onlyFriends) {
                    // When in friends tab, always show my own posts unless explicitly hidden by showMyPosts
                    val targetIds = friendIds + if (showMyPosts) listOf(userId) else emptyList()
                    posts = posts.filter { targetIds.contains(it.userId) }
                }

                // Filter by distance (only if not global)
                if (radius != null && userLocation != null) {
                    posts = posts.filter { post ->
                        if (post.location == null) return@filter false
                        distanceMiles(userLocation, post.location) <= radius
                    }
                }

                // Sort in memory
                trySend(posts.sortedByDescending { it.createdAt })
            }
        awaitClose { listener.remove() }
    }

    fun getUserPosts(targetUserId: String, includeChat: Boolean = true): Flow<List<FeedPost>> = callbackFlow {
        val listener = feedCollection
            .whereEqualTo("userId", targetUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
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

    fun getMyPosts(includeChat: Boolean = true): Flow<List<FeedPost>> = getUserPosts(userId, includeChat)

    suspend fun createPost(post: FeedPost): Result<String> {
        return try {
            val postWithUser = post.copy(userId = userId)
            val doc = feedCollection.add(postWithUser).await()
            Result.success(doc.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePost(postId: String, description: String): Result<Unit> {
        return try {
            feedCollection.document(postId).update("description", description).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleLike(postId: String): Result<Unit> {
        return try {
            val docRef = feedCollection.document(postId)
            val doc = docRef.get().await()
            val post = doc.toObject(FeedPost::class.java) ?: return Result.failure(Exception("Post not found"))
            
            val updatedLikes = post.likes.toMutableList()
            if (updatedLikes.contains(userId)) {
                updatedLikes.remove(userId)
            } else {
                updatedLikes.add(userId)
            }
            docRef.update("likes", updatedLikes).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleReaction(postId: String?, emoji: String): Result<Unit> {
        if (postId == null) return Result.failure(Exception("Post ID is null"))
        return try {
            val docRef = feedCollection.document(postId)
            val doc = docRef.get().await()
            val post = doc.toObject(FeedPost::class.java) ?: return Result.failure(Exception("Post not found"))

            // Reactions are stored as Map<UserId, ReactionEmoji>
            val updatedReactions = post.reactionsMap.toMutableMap()
            
            if (updatedReactions[userId] == emoji) {
                // If user already reacted with the same emoji removes it
                updatedReactions.remove(userId)
            } else {
                // If user hasn't reacted or used a different emoji update it
                updatedReactions[userId] = emoji
            }
            
            docRef.update("reactions", updatedReactions).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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
