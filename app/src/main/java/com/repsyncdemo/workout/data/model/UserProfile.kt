package com.repsyncdemo.workout.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.PropertyName

data class UserProfile(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val usernameLowercase: String = "", // Added for case-insensitive search
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

    //isAdmin Distinction
    @get:PropertyName("isAdmin")
    @set:PropertyName("isAdmin")
    var isAdmin: Boolean = false,

    //is Moderator Distinction
    @get:PropertyName("isModerator")
    @set:PropertyName("isModerator")
    var isModerator: Boolean = false,

    val instagramUrl: String = "",
    val facebookUrl: String = "",
    val twitterUrl: String = "",

    val preferredUnit: String = "lbs", // "lbs" or "kg"
    val theme: String = "dark", // "light" or "dark"
    
    val pinnedTrophyId: String? = null, // ID of the pinned trophy

    val location: GeoPoint? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
