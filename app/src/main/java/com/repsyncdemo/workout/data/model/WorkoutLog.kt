package com.repsyncdemo.workout.data.model

/**
 * File overview: Defines the WorkoutLog data model used by repositories, view models, and UI binding.
 */

import com.google.firebase.firestore.DocumentId

data class WorkoutLog(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val workoutId: String = "",
    val workoutName: String = "",
    val exercises: List<ExerciseLog> = emptyList(),
    val startedAt: Long = System.currentTimeMillis() - 3600000,
    val completedAt: Long = System.currentTimeMillis(),
    val durationMinutes: Int = 60,
    val notes: String = ""
)

data class ExerciseLog(
    val exerciseName: String = "",
    val type: ExerciseType = ExerciseType.STRENGTH,
    val sets: List<SetLog> = emptyList()
)

data class SetLog(
    // Strength fields
    val reps: Int? = null,
    val weight: Double? = null,
    
    // Cardio fields
    val durationSeconds: Int? = null,
    val distance: Double? = null,
    val floors: Int? = null,

    val completed: Boolean = false
)
