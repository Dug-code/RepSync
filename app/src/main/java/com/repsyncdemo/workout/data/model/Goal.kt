package com.repsyncdemo.workout.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class Goal(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val type: GoalType = GoalType.WEIGHT_LOSS,
    val title: String = "",
    val exerciseName: String = "",
    val startingValue: Double = 0.0,
    val targetValue: Double = 0.0,
    val currentValue: Double = 0.0,
    val unit: String = "",
    
    @get:PropertyName("isCompleted")
    @set:PropertyName("isCompleted")
    var isCompleted: Boolean = false,
    
    @get:PropertyName("isPublic")
    @set:PropertyName("isPublic")
    var isPublic: Boolean = false,

    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

enum class GoalType {
    PR,
    WEIGHT_LOSS,
    WEIGHT_GAIN,
    STREAK
}
