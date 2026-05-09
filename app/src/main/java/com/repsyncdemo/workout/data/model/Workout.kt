package com.repsyncdemo.workout.data.model

/**
 * File overview: Defines the Workout data model used by repositories, view models, and UI binding.
 */

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class Workout(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val exercises: List<Exercise> = emptyList(),
    @get:PropertyName("isPublic")
    @set:PropertyName("isPublic")
    var isPublic: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val order: Int = 0 // Added field for custom reordering
)
