package com.repsyncdemo.workout.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint

data class FeedPost(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfilePicture: String = "",
    val type: FeedPostType = FeedPostType.WORKOUT_SHARED,
    val workoutId: String = "",
    val workoutName: String = "",
    val goalId: String = "",
    val goalTitle: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val location: GeoPoint? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val likes: List<String> = emptyList(),
    val reactions: Map<String, String> = emptyMap()
)

enum class FeedPostType {
    WORKOUT_SHARED,
    GOAL_CREATED,
    GOAL_COMPLETED,
    PR_ACHIEVED,
    CHAT_MESSAGE
}
