package com.repsyncdemo.workout.data.model

import com.google.firebase.firestore.DocumentId

data class WorkoutLog(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val workoutId: String = "",
    val workoutName: String = "",
    val exercises: List<ExerciseLog> = emptyList(),
    val startedAt: Long = System.currentTimeMillis() - 3600000, // Default 1 hour ago
    val completedAt: Long = System.currentTimeMillis(),
    val durationMinutes: Int = 60,
    val notes: String = ""
)

data class ExerciseLog(
    val exerciseName: String = "",
    val sets: List<SetLog> = emptyList()
)

data class SetLog(
    val reps: Int = 0,
    val weight: Double = 0.0,
    val completed: Boolean = false
)
