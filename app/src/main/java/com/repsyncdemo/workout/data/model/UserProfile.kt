package com.repsyncdemo.workout.data.model

/**
 * File overview: Defines the UserProfile data model used by repositories, view models, and UI binding.
 */

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.PropertyName

/**
 * Data class representing a user's profile information.
 * Stores personal details, privacy settings, social links, and administrative roles.
 */
data class UserProfile(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val usernameLowercase: String = "", // Added for case-insensitive search in Firestore
    val bio: String = "",
    val email: String = "",
    val profilePictureUrl: String = "red",
    val heightInches: Int = 0,
    val weightLbs: Double = 0.0,
    val totalRestDays: Int = 0,
    
    @get:PropertyName("isHeightPublic")
    @set:PropertyName("isHeightPublic")
    var isHeightPublic: Boolean = false,
    
    @get:PropertyName("isWeightPublic")
    @set:PropertyName("isWeightPublic")
    var isWeightPublic: Boolean = false,
    
    @get:PropertyName("isPublicAccount")
    @set:PropertyName("isPublicAccount")
    var isPublicAccount: Boolean = false,
    
    @get:PropertyName("shareGoalsAndProgress")
    @set:PropertyName("shareGoalsAndProgress")
    var shareGoalsAndProgress: Boolean = false,

    @get:PropertyName("isWorkoutsPublic")
    @set:PropertyName("isWorkoutsPublic")
    var isWorkoutsPublic: Boolean = true,

    @get:PropertyName("isFriendsListPublic")
    @set:PropertyName("isFriendsListPublic")
    var isFriendsListPublic: Boolean = true,

    // Admin role: Grants full access to all moderation tools and the Admin Dashboard.
    @get:PropertyName("isAdmin")
    @set:PropertyName("isAdmin")
    var isAdmin: Boolean = false,

    // Moderator role: Grants access to content moderation tools (e.g., deleting any post).
    @get:PropertyName("isModerator")
    @set:PropertyName("isModerator")
    var isModerator: Boolean = false,

    val instagramUrl: String = "",
    val facebookUrl: String = "",
    val twitterUrl: String = "",

    val preferredUnit: String = "lbs", // "lbs" or "kg"
    val theme: String = "dark", // "light" or "dark"
    
    val pinnedTrophyId: String? = null, // ID of the pinned trophy displayed on profile

    // Weigh-in settings
    val weighInFrequency: String = "never", // "never", "daily", "custom"
    val weighInDays: List<Int> = emptyList(), // 1 (Mon) to 7 (Sun)
    val lastWeighInDate: Long = 0, // Timestamp of last weigh-in

    val location: GeoPoint? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
