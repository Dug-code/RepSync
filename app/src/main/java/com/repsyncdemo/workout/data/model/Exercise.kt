package com.repsyncdemo.workout.data.model

import com.google.firebase.firestore.PropertyName

/**
 * Represents a single exercise instance performed within a workout template.
 */
data class Exercise(
    val name: String = "",
    val sets: Int = 0,
    val reps: Int = 0,
    val weight: Double = 0.0,
    val notes: String = "",
    @get:PropertyName("isBodyWeight")
    @set:PropertyName("isBodyWeight")
    var isBodyWeight: Boolean = false,
    val type: ExerciseType = ExerciseType.STRENGTH,
    val primaryMuscleGroup: String? = null,
    val secondaryMuscleGroup: String? = null
)

/**
 * Defines a type of exercise in the library.
 */
data class ExerciseDefinition(
    val id: String = "",
    val name: String = "",
    val primaryBodyPart: String = "",
    val secondaryBodyParts: String = "",
    val equipment: String = "",
    val movementPattern: String = "",
    val type: ExerciseType = ExerciseType.STRENGTH,
    val isCustom: Boolean = false,
    val userId: String? = null // For custom exercises
)

enum class ExerciseType {
    STRENGTH,
    CARDIO,
    CALISTHENICS
}
