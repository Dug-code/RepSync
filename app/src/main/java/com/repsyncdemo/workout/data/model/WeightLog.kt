package com.repsyncdemo.workout.data.model

import com.google.firebase.firestore.DocumentId

data class WeightLog(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val weightLbs: Double = 0.0,
    val date: Long = System.currentTimeMillis()
)
