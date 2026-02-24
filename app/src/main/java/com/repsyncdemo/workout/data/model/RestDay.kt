package com.repsyncdemo.workout.data.model

import com.google.firebase.firestore.DocumentId

data class RestDay(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val date: Long = System.currentTimeMillis() // Timestamp for the start of the day
)
