package com.repsyncdemo.workout.data.model

import com.google.firebase.firestore.PropertyName

data class Exercise(
    val name: String = "",
    val sets: Int = 0,
    val reps: Int = 0,
    val weight: Double = 0.0,
    val notes: String = "",
    
    @get:PropertyName("isBodyWeight")
    @set:PropertyName("isBodyWeight")
    var isBodyWeight: Boolean = false
)

data class ExerciseDefinition(
    val name: String,
    val primaryBodyPart: String,
    val secondaryBodyParts: String,
    val equipment: String,
    val movementPattern: String
)
