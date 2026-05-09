package com.repsyncdemo.workout.data.model

/**
 * File overview: Defines the RestDay data model used by repositories, view models, and UI binding.
 */

import com.google.firebase.firestore.DocumentId

data class RestDay(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val date: Long = System.currentTimeMillis() // Timestamp for the start of the day
)
