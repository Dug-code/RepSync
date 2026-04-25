package com.repsyncdemo.workout.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

/**
 * Represents a single exercise instance performed within a workout template.
 */
@IgnoreExtraProperties
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
    val secondaryMuscleGroup: String? = null,
    @get:PropertyName("isCustom")
    @set:PropertyName("isCustom")
    var isCustom: Boolean = false
)

/**
 * Defines a type of exercise in the library.
 */
@IgnoreExtraProperties
data class ExerciseDefinition(
    @DocumentId
    val firebaseId: String = "", // Internal Firestore document ID
    
    @get:PropertyName("id")
    @set:PropertyName("id")
    var idInData: String = "", // Legacy or descriptive ID
    
    val name: String = "",
    val primaryBodyPart: String = "",
    val secondaryBodyParts: String = "",
    val type: ExerciseType = ExerciseType.STRENGTH,
    
    @get:PropertyName("isCustom")
    @set:PropertyName("isCustom")
    var isCustom: Boolean = false,
    
    val userId: String? = null // Owner ID for custom exercises
)

enum class ExerciseType {
    STRENGTH,
    CARDIO,
    CALISTHENICS
}
