package com.repsyncdemo.workout.data.model

import com.google.firebase.firestore.PropertyName

/**
 * Represents a single exercise instance performed within a workout.
 * Includes performance metrics like sets, reps, and weight.
 */
data class Exercise(
    val name: String = "",
    val sets: Int = 0,
    val reps: Int = 0,
    val weight: Double = 0.0,
    val notes: String = "",
    
    /**
     * Flag to indicate if the exercise is bodyweight-based.
     * Firebase PropertyName annotations ensure correct mapping even if the boolean 
     * getter/setter naming convention differs in Firestore.
     */
    @get:PropertyName("isBodyWeight")
    @set:PropertyName("isBodyWeight")
    var isBodyWeight: Boolean = false
)

/**
 * Defines a type of exercise in the library.
 * This contains metadata about the movement rather than performance data.
 */
data class ExerciseDefinition(
    val name: String,
    val primaryBodyPart: String,
    val secondaryBodyParts: String,
    val equipment: String,
    val movementPattern: String
)
