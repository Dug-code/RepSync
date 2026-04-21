package com.repsyncdemo.workout.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.repsyncdemo.workout.data.model.UserProfile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing user profiles in Firestore.
 */
class ProfileRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val profilesCollection = db.collection("profiles")

    /**
     * Helper to get current Firebase User ID.
     * Throws if no user is authenticated.
     */
    private val currentUserId: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")

    /**
     * Creates a new user profile in the database.
     */
    suspend fun createProfile(profile: UserProfile): Result<Unit> {
        return try {
            val profileWithUser = profile.copy(
                userId = currentUserId,
                usernameLowercase = profile.username.lowercase()
            )
            profilesCollection.document(currentUserId).set(profileWithUser).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieves a profile for a specific user ID, or the current user if null.
     */
    suspend fun getProfile(userId: String? = null): Result<UserProfile> {
        val id = userId ?: currentUserId
        return try {
            val doc = profilesCollection.document(id).get().await()
            val profile = doc.toObject(UserProfile::class.java)
            if (profile != null) {
                // Migration: If lowercase field is missing, update it now
                if (profile.usernameLowercase.isEmpty() && profile.username.isNotEmpty()) {
                    val updated = profile.copy(usernameLowercase = profile.username.lowercase())
                    profilesCollection.document(id).set(updated)
                    Result.success(updated)
                } else {
                    Result.success(profile)
                }
            }
            else Result.failure(Exception("Profile not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Returns a real-time Flow of a specific user's profile.
     */
    fun observeProfile(userId: String? = null): Flow<UserProfile?> = callbackFlow {
        val id = userId ?: currentUserId
        val listener = profilesCollection.document(id)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ProfileRepository", "Error observing profile", error)
                    trySend(null)
                    return@addSnapshotListener
                }
                val profile = snapshot?.toObject(UserProfile::class.java)
                trySend(profile)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Returns a Flow mapping user IDs to their respective profiles for a list of IDs.
     */
    fun observeProfiles(userIds: List<String>): Flow<Map<String, UserProfile>> = callbackFlow {
        if (userIds.isEmpty()) {
            trySend(emptyMap())
            return@callbackFlow
        }

        // Limit to 10 at a time for 'whereIn' (Firestore limit)
        val batches = userIds.chunked(10)
        val profileMap = mutableMapOf<String, UserProfile>()

        val listeners = batches.map { batch ->
            profilesCollection.whereIn("userId", batch)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    snapshot?.toObjects(UserProfile::class.java)?.forEach { profile ->
                        profileMap[profile.userId] = profile
                    }
                    trySend(profileMap.toMap())
                }
        }

        awaitClose { listeners.forEach { it.remove() } }
    }

    /**
     * Updates a user profile. 
     * CRITICAL FIX: Now uses the userId from the profile object itself to determine
     * the target document, rather than always overwriting the current logged-in user.
     */
    suspend fun updateProfile(profile: UserProfile): Result<Unit> {
        return try {
            // Use the userId from the profile, falling back to current user only if empty
            val targetId = if (profile.userId.isNotEmpty()) profile.userId else currentUserId
            
            profilesCollection.document(targetId).set(
                profile.copy(
                    userId = targetId, // Ensure internal userId matches the document ID
                    updatedAt = System.currentTimeMillis(),
                    usernameLowercase = profile.username.lowercase()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates the user's location coordinates.
     */
    suspend fun updateLocation(latitude: Double, longitude: Double): Result<Unit> {
        return try {
            profilesCollection.document(currentUserId)
                .update("location", GeoPoint(latitude, longitude))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Checks if a profile document exists for the current user.
     */
    suspend fun hasProfile(): Boolean {
        return try {
            val doc = profilesCollection.document(currentUserId).get().await()
            doc.exists()
        } catch (e: Exception) {
            // Fix for "Parameter 'e' is never used" warning - Log the error for tracking
            Log.e("ProfileRepository", "Error checking for profile", e)
            false
        }
    }

    /**
     * Searches for users by username (case-insensitive).
     */
    suspend fun searchUsers(query: String): Result<List<UserProfile>> {
        return try {
            val lowerQuery = query.lowercase().trim()
            if (lowerQuery.isEmpty()) return Result.success(emptyList())

            // Try searching by lowercase username
            val snapshot = profilesCollection
                .whereGreaterThanOrEqualTo("usernameLowercase", lowerQuery)
                .whereLessThanOrEqualTo("usernameLowercase", lowerQuery + "\uf8ff")
                .limit(20)
                .get()
                .await()
            
            var profiles = snapshot.toObjects(UserProfile::class.java)
            
            // If no results, try searching the original username field (case sensitive) as a fallback
            if (profiles.isEmpty()) {
                val fallbackSnapshot = profilesCollection
                    .whereGreaterThanOrEqualTo("username", query)
                    .whereLessThanOrEqualTo("username", query + "\uf8ff")
                    .limit(20)
                    .get()
                    .await()
                profiles = fallbackSnapshot.toObjects(UserProfile::class.java)
            }

            val filteredResults = profiles.filter { it.userId != currentUserId }
            Result.success(filteredResults)
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Search failed", e)
            Result.failure(e)
        }
    }
}
