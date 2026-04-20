package com.repsyncdemo.workout.data.model

data class ContactMessage(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
