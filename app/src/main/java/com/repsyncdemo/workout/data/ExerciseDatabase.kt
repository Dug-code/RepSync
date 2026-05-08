package com.repsyncdemo.workout.data

/**
 * File overview: Defines the built-in exercise catalog and helper lookups used by templates, logs, and analytics.
 */

import com.repsyncdemo.workout.data.model.ExerciseDefinition
import com.repsyncdemo.workout.data.model.ExerciseType

object ExerciseDatabase {

    val allExercises: List<ExerciseDefinition> = listOf(
        // ===== CHEST =====
        ExerciseDefinition("", "bench_barbell", "Bench Press (Barbell)", "Chest", "Triceps, Shoulders", ExerciseType.STRENGTH),
        ExerciseDefinition("", "bench_dumbbell", "Bench Press (Dumbbell)", "Chest", "Triceps, Shoulders", ExerciseType.STRENGTH),
        ExerciseDefinition("", "incline_bench_barbell", "Incline Bench Press (Barbell)", "Chest", "Triceps, Shoulders", ExerciseType.STRENGTH),
        ExerciseDefinition("", "incline_bench_dumbbell", "Incline Bench Press (Dumbbell)", "Chest", "Triceps, Shoulders", ExerciseType.STRENGTH),
        ExerciseDefinition("", "decline_bench_barbell", "Decline Bench Press (Barbell)", "Chest", "Triceps, Shoulders", ExerciseType.STRENGTH),
        ExerciseDefinition("", "chest_fly_dumbbell", "Chest Fly (Dumbbell)", "Chest", "Shoulders", ExerciseType.STRENGTH),
        ExerciseDefinition("", "chest_fly_cable", "Chest Fly (Cable)", "Chest", "Shoulders", ExerciseType.STRENGTH),
        ExerciseDefinition("", "machine_chest_press", "Chest Press (Machine)", "Chest", "Triceps", ExerciseType.STRENGTH),
        ExerciseDefinition("", "pec_deck", "Pec Deck", "Chest", "Shoulders", ExerciseType.STRENGTH),
        ExerciseDefinition("", "push_up", "Push-Up", "Chest", "Triceps, Shoulders", ExerciseType.CALISTHENICS),
        ExerciseDefinition("", "diamond_push_up", "Diamond Push-Up", "Triceps", "Chest", ExerciseType.CALISTHENICS),
        ExerciseDefinition("", "chest_dip", "Chest Dip", "Chest", "Triceps, Shoulders", ExerciseType.CALISTHENICS),

        // ===== BACK =====
        ExerciseDefinition("", "pull_up", "Pull-Up", "Back", "Biceps, Core", ExerciseType.CALISTHENICS),
        ExerciseDefinition("", "chin_up", "Chin-Up", "Back", "Biceps", ExerciseType.CALISTHENICS),
        ExerciseDefinition("", "lat_pulldown", "Lat Pulldown", "Back", "Biceps", ExerciseType.STRENGTH),
        ExerciseDefinition("", "seated_row_cable", "Seated Row (Cable)", "Back", "Biceps", ExerciseType.STRENGTH),
        ExerciseDefinition("", "bent_over_row_barbell", "Bent-Over Row (Barbell)", "Back", "Shoulders", ExerciseType.STRENGTH),
        ExerciseDefinition("", "one_arm_row_dumbbell", "One-Arm Row (Dumbbell)", "Back", "Biceps", ExerciseType.STRENGTH),
        ExerciseDefinition("", "deadlift_barbell", "Deadlift (Barbell)", "Back", "Legs, Core", ExerciseType.STRENGTH),
        ExerciseDefinition("", "t_bar_row", "T-Bar Row", "Back", "Biceps", ExerciseType.STRENGTH),
        ExerciseDefinition("", "back_extension", "Back Extension", "Back", "Hamstrings", ExerciseType.STRENGTH),
        ExerciseDefinition("", "face_pull_cable", "Face Pull (Cable)", "Back", "Shoulders", ExerciseType.STRENGTH),

        // ===== SHOULDERS =====
        ExerciseDefinition("", "overhead_press_barbell", "Overhead Press (Barbell)", "Shoulders", "Triceps", ExerciseType.STRENGTH),
        ExerciseDefinition("", "overhead_press_dumbbell", "Overhead Press (Dumbbell)", "Shoulders", "Triceps", ExerciseType.STRENGTH),
        ExerciseDefinition("", "arnold_press", "Arnold Press", "Shoulders", "Triceps", ExerciseType.STRENGTH),
        ExerciseDefinition("", "lateral_raise_dumbbell", "Lateral Raise (Dumbbell)", "Shoulders", "None", ExerciseType.STRENGTH),
        ExerciseDefinition("", "front_raise_dumbbell", "Front Raise (Dumbbell)", "Shoulders", "None", ExerciseType.STRENGTH),
        ExerciseDefinition("", "rear_delt_fly_dumbbell", "Rear Delt Fly (Dumbbell)", "Shoulders", "Back", ExerciseType.STRENGTH),
        ExerciseDefinition("", "shrug_dumbbell", "Shrug (Dumbbell)", "Shoulders", "Traps", ExerciseType.STRENGTH),
        ExerciseDefinition("", "upright_row", "Upright Row", "Shoulders", "Traps", ExerciseType.STRENGTH),

        // ===== ARMS =====
        ExerciseDefinition("", "bicep_curl_barbell", "Bicep Curl (Barbell)", "Biceps", "Forearms", ExerciseType.STRENGTH),
        ExerciseDefinition("", "bicep_curl_dumbbell", "Bicep Curl (Dumbbell)", "Biceps", "Forearms", ExerciseType.STRENGTH),
        ExerciseDefinition("", "hammer_curl_dumbbell", "Hammer Curl (Dumbbell)", "Biceps", "Forearms", ExerciseType.STRENGTH),
        ExerciseDefinition("", "preacher_curl", "Preacher Curl", "Biceps", "Forearms", ExerciseType.STRENGTH),
        ExerciseDefinition("", "tricep_pushdown_cable", "Tricep Pushdown (Cable)", "Triceps", "None", ExerciseType.STRENGTH),
        ExerciseDefinition("", "skullcrusher_barbell", "Skullcrusher (Barbell)", "Triceps", "None", ExerciseType.STRENGTH),
        ExerciseDefinition("", "overhead_tricep_extension", "Overhead Tricep Extension", "Triceps", "None", ExerciseType.STRENGTH),
        ExerciseDefinition("", "tricep_dip_bench", "Tricep Dip (Bench)", "Triceps", "Chest", ExerciseType.CALISTHENICS),

        // ===== LEGS =====
        ExerciseDefinition("", "squat_barbell", "Squat (Barbell)", "Quads", "Glutes, Back", ExerciseType.STRENGTH),
        ExerciseDefinition("", "leg_press", "Leg Press", "Quads", "Glutes", ExerciseType.STRENGTH),
        ExerciseDefinition("", "hack_squat", "Hack Squat", "Quads", "Glutes", ExerciseType.STRENGTH),
        ExerciseDefinition("", "lunge_dumbbell", "Lunge (Dumbbell)", "Quads", "Glutes", ExerciseType.STRENGTH),
        ExerciseDefinition("", "leg_extension_machine", "Leg Extension", "Quads", "None", ExerciseType.STRENGTH),
        ExerciseDefinition("", "leg_curl_machine", "Leg Curl", "Hamstrings", "None", ExerciseType.STRENGTH),
        ExerciseDefinition("", "calf_raise_standing", "Calf Raise (Standing)", "Calves", "None", ExerciseType.STRENGTH),
        ExerciseDefinition("", "calf_raise_seated", "Calf Raise (Seated)", "Calves", "None", ExerciseType.STRENGTH),
        ExerciseDefinition("", "hip_thrust_barbell", "Hip Thrust (Barbell)", "Glutes", "Hamstrings", ExerciseType.STRENGTH),
        ExerciseDefinition("", "romanian_deadlift_barbell", "Romanian Deadlift (Barbell)", "Hamstrings", "Glutes", ExerciseType.STRENGTH),
        ExerciseDefinition("", "goblet_squat_dumbbell", "Goblet Squat (Dumbbell)", "Quads", "Glutes", ExerciseType.STRENGTH),

        // ===== CORE =====
        ExerciseDefinition("", "plank", "Plank", "Abs", "Shoulders, Legs", ExerciseType.CALISTHENICS),
        ExerciseDefinition("", "crunch", "Crunch", "Abs", "None", ExerciseType.CALISTHENICS),
        ExerciseDefinition("", "hanging_leg_raise", "Hanging Leg Raise", "Abs", "Hip Flexors", ExerciseType.CALISTHENICS),
        ExerciseDefinition("", "russian_twist", "Russian Twist", "Abs", "Obliques", ExerciseType.CALISTHENICS),
        ExerciseDefinition("", "ab_wheel_rollout", "Ab Wheel Rollout", "Abs", "Back, Shoulders", ExerciseType.CALISTHENICS),
        ExerciseDefinition("", "mountain_climber", "Mountain Climber", "Abs", "Full Body", ExerciseType.CALISTHENICS),

        // ===== CARDIO =====
        ExerciseDefinition("", "running_treadmill", "Running (Treadmill)", "Cardio", "Full Body", ExerciseType.CARDIO),
        ExerciseDefinition("", "running_outdoor", "Running (Outdoor)", "Cardio", "Full Body", ExerciseType.CARDIO),
        ExerciseDefinition("", "walking", "Walking", "Cardio", "Full Body", ExerciseType.CARDIO),
        ExerciseDefinition("", "cycling_stationary", "Stationary Bike", "Cardio", "Legs", ExerciseType.CARDIO),
        ExerciseDefinition("", "rowing_machine", "Rowing Machine", "Cardio", "Full Body", ExerciseType.CARDIO),
        ExerciseDefinition("", "stair_climber", "Stair Climber", "Cardio", "Legs", ExerciseType.CARDIO),
        ExerciseDefinition("", "elliptical", "Elliptical", "Cardio", "Full Body", ExerciseType.CARDIO),
        ExerciseDefinition("", "jump_rope", "Jump Rope", "Cardio", "Full Body", ExerciseType.CARDIO),
        ExerciseDefinition("", "swimming", "Swimming", "Cardio", "Full Body", ExerciseType.CARDIO),
        ExerciseDefinition("", "burpee", "Burpee", "Full Body", "Cardio", ExerciseType.CALISTHENICS)
    )

    val bodyParts: List<String> = listOf(
        "Chest", "Back", "Shoulders", "Biceps", "Triceps", "Quads", "Hamstrings", "Glutes", "Calves", "Abs", "Full Body", "Cardio"
    )

    val exerciseTypes: List<String> = listOf("Weight Lifting", "Cardio", "Calisthenics")

    fun filter(
        bodyPart: String? = null,
        searchQuery: String? = null
    ): List<ExerciseDefinition> {
        return allExercises.filter { exercise ->
            val matchesBodyPart = bodyPart == null || exercise.primaryBodyPart.equals(bodyPart, ignoreCase = true) || exercise.secondaryBodyParts.contains(bodyPart ?: "", ignoreCase = true)
            val matchesSearch = searchQuery == null || exercise.name.contains(searchQuery, ignoreCase = true)
            
            matchesBodyPart && matchesSearch
        }.sortedWith(compareByDescending<ExerciseDefinition> { 
            bodyPart == null || it.primaryBodyPart.equals(bodyPart, ignoreCase = true) 
        })
    }

    // Reads data.
    fun getExerciseByName(name: String): ExerciseDefinition? {
        return allExercises.find { it.name.equals(name, ignoreCase = true) }
    }
}
