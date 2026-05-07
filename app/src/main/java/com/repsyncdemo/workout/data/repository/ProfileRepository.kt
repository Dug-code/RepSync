package com.repsyncdemo.workout.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.SetOptions
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

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    /**
     * Creates a new user profile in the database.
     */
    suspend fun createProfile(profile: UserProfile): Result<Unit> {
        return try {
            val id = currentUserId ?: return Result.failure(Exception("User not logged in"))
            
            val profileWithUser = profile.copy(
                userId = id,
                usernameLowercase = profile.username.lowercase()
            )
            profilesCollection.document(id).set(profileWithUser).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieves a profile for a specific user ID.
     */
    suspend fun getProfile(userId: String? = null): Result<UserProfile> {
        val id = userId ?: currentUserId ?: return Result.failure(Exception("User not logged in"))
        
        return try {
            val doc = profilesCollection.document(id).get().await()
            val profile = doc.toObject(UserProfile::class.java)
            if (profile != null) Result.success(profile)
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
        if (id == null) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        
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
     * Updates specific fields in a user profile.
     * Uses Firestore update() which is much more reliable for concurrent edits.
     */
    suspend fun updateProfileFields(updates: Map<String, Any>): Result<Unit> {
        val id = currentUserId ?: return Result.failure(Exception("User not logged in"))
        
        return try {
            val finalUpdates = updates.toMutableMap()
            finalUpdates["updatedAt"] = System.currentTimeMillis()
            
            if (updates.containsKey("username")) {
                finalUpdates["usernameLowercase"] = (updates["username"] as String).lowercase()
            }
            
            profilesCollection.document(id).update(finalUpdates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Update failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Updates the user profile using merge to avoid overwriting fields not present in the object.
     */
    suspend fun updateProfile(profile: UserProfile): Result<Unit> {
        return try {
            val targetId = profile.userId.ifEmpty { currentUserId } 
                ?: return Result.failure(Exception("User not logged in"))
            
            profilesCollection.document(targetId).set(
                profile.copy(
                    userId = targetId,
                    updatedAt = System.currentTimeMillis(),
                    usernameLowercase = profile.username.lowercase()
                ),
                SetOptions.merge()
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateLocation(latitude: Double, longitude: Double): Result<Unit> {
        return updateProfileFields(mapOf("location" to GeoPoint(latitude, longitude)))
    }

    suspend fun hasProfile(): Boolean {
        val id = currentUserId ?: return false
        return try {
            val doc = profilesCollection.document(id).get().await()
            doc.exists()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun searchUsers(query: String): Result<List<UserProfile>> {
        return try {
            val lowerQuery = query.lowercase().trim()
            if (lowerQuery.isEmpty()) return Result.success(emptyList())

            val snapshot = profilesCollection
                .whereGreaterThanOrEqualTo("usernameLowercase", lowerQuery)
                .whereLessThanOrEqualTo("usernameLowercase", lowerQuery + "\uf8ff")
                .limit(20)
                .get()
                .await()
            
            val profiles = snapshot.toObjects(UserProfile::class.java)
            val currentUid = currentUserId
            val filteredResults = if (currentUid != null) {
                profiles.filter { it.userId != currentUid }
            } else {
                profiles
            }
            Result.success(filteredResults)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isUsernameAvailable(username: String, excludedUserId: String? = null): Boolean {
        return try {
            val snapshot = profilesCollection
                .whereEqualTo("usernameLowercase", username.lowercase().trim())
                .limit(1)
                .get()
                .await()
            snapshot.documents.none { doc ->
                val profile = doc.toObject(UserProfile::class.java)
                doc.id != excludedUserId && profile?.userId != excludedUserId
            }
        } catch (e: Exception) {
            false
        }
    }
}
