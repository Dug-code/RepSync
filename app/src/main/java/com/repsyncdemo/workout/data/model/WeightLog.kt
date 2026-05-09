package com.repsyncdemo.workout.data.model

/**
 * File overview: Defines the WeightLog data model used by repositories, view models, and UI binding.
 */

import com.google.firebase.firestore.DocumentId

data class WeightLog(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val weightLbs: Double = 0.0,
    val date: Long = System.currentTimeMillis()
)
