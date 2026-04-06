package com.repsyncdemo.workout.data

import com.repsyncdemo.workout.data.model.ExerciseDefinition
import com.repsyncdemo.workout.data.model.ExerciseType

object ExerciseDatabase {

    val allExercises: List<ExerciseDefinition> = listOf(
        // ===== CHEST =====
        ExerciseDefinition("bench_barbell", "Bench Press (Barbell)", "Chest", "Triceps, Shoulders", "Barbell", "Horizontal Push", ExerciseType.STRENGTH),
        ExerciseDefinition("bench_dumbbell", "Bench Press (Dumbbells)", "Chest", "Triceps, Shoulders", "Dumbbells", "Horizontal Push", ExerciseType.STRENGTH),
        ExerciseDefinition("incline_bench_barbell", "Incline Bench Press (Barbell)", "Chest", "Triceps, Shoulders", "Barbell", "Horizontal Push", ExerciseType.STRENGTH),
        ExerciseDefinition("push_up", "Push-Up", "Chest", "Triceps, Shoulders", "Bodyweight", "Horizontal Push", ExerciseType.STRENGTH),
        ExerciseDefinition("chest_dip", "Chest Dip", "Chest", "Triceps, Shoulders", "Bodyweight", "Horizontal Push", ExerciseType.STRENGTH),

        // ===== BACK =====
        ExerciseDefinition("pull_up", "Pull-Up", "Back", "Biceps, Core", "Bodyweight", "Vertical Pull", ExerciseType.STRENGTH),
        ExerciseDefinition("lat_pulldown", "Lat Pulldown (Wide)", "Back", "Biceps", "Cable", "Vertical Pull", ExerciseType.STRENGTH),
        ExerciseDefinition("seated_row", "Seated Cable Row", "Back", "Biceps", "Cable", "Horizontal Pull", ExerciseType.STRENGTH),
        ExerciseDefinition("bent_over_row", "Bent-Over Row (Barbell)", "Back", "Biceps", "Barbell", "Horizontal Pull", ExerciseType.STRENGTH),
        ExerciseDefinition("deadlift", "Deadlift", "Back", "Legs, Core", "Barbell", "Hip Hinge", ExerciseType.STRENGTH),

        // ===== SHOULDERS =====
        ExerciseDefinition("overhead_press_barbell", "Overhead Press (Barbell)", "Shoulders", "Triceps", "Barbell", "Vertical Push", ExerciseType.STRENGTH),
        ExerciseDefinition("lateral_raise_dumbbell", "Lateral Raise (Dumbbells)", "Shoulders", "None", "Dumbbells", "Extension", ExerciseType.STRENGTH),
        ExerciseDefinition("face_pull", "Face Pull", "Shoulders", "Back", "Cable", "Horizontal Pull", ExerciseType.STRENGTH),

        // ===== ARMS =====
        ExerciseDefinition("bicep_curl_barbell", "Bicep Curl (Barbell)", "Biceps", "Forearms", "Barbell", "Flexion", ExerciseType.STRENGTH),
        ExerciseDefinition("tricep_pushdown", "Tricep Pushdown", "Triceps", "None", "Cable", "Extension", ExerciseType.STRENGTH),
        ExerciseDefinition("skullcrusher", "Skullcrusher", "Triceps", "Chest", "Barbell", "Extension", ExerciseType.STRENGTH),

        // ===== LEGS =====
        ExerciseDefinition("back_squat", "Back Squat (Barbell)", "Quads", "Glutes, Back", "Barbell", "Squat", ExerciseType.STRENGTH),
        ExerciseDefinition("leg_press", "Leg Press", "Quads", "Glutes", "Machine", "Squat", ExerciseType.STRENGTH),
        ExerciseDefinition("leg_extension", "Leg Extension", "Quads", "None", "Machine", "Extension", ExerciseType.STRENGTH),
        ExerciseDefinition("leg_curl", "Leg Curl", "Hamstrings", "None", "Machine", "Flexion", ExerciseType.STRENGTH),
        ExerciseDefinition("calf_raise", "Calf Raise", "Calves", "None", "Machine", "Extension", ExerciseType.STRENGTH),
        ExerciseDefinition("hip_thrust", "Hip Thrust", "Glutes", "Hamstrings", "Barbell", "Hip Hinge", ExerciseType.STRENGTH),

        // ===== CORE =====
        ExerciseDefinition("plank", "Plank", "Abs", "Shoulders, Legs", "Bodyweight", "Anti-Rotation", ExerciseType.STRENGTH),
        ExerciseDefinition("russian_twist", "Russian Twist", "Abs", "None", "Bodyweight", "Rotation", ExerciseType.STRENGTH),
        ExerciseDefinition("leg_raise", "Hanging Leg Raise", "Abs", "Legs", "Bodyweight", "Flexion", ExerciseType.STRENGTH),

        // ===== CARDIO =====
        ExerciseDefinition("running_outdoor", "Running (Outdoor)", "Cardio", "Legs", "Other", "Cyclical", ExerciseType.CARDIO),
        ExerciseDefinition("treadmill_run", "Treadmill Run", "Cardio", "Legs", "Cardio Machine", "Cyclical", ExerciseType.CARDIO),
        ExerciseDefinition("stair_climber", "Stair Climber", "Cardio", "Legs", "Cardio Machine", "Cyclical", ExerciseType.CARDIO),
        ExerciseDefinition("cycling_stationary", "Stationary Bike", "Cardio", "Legs", "Cardio Machine", "Cyclical", ExerciseType.CARDIO),
        ExerciseDefinition("rowing_machine", "Rowing Machine", "Cardio", "Full Body", "Cardio Machine", "Cyclical", ExerciseType.CARDIO)
    )

    val bodyParts: List<String> = listOf(
        "Chest", "Back", "Shoulders", "Biceps", "Triceps", "Quads", "Hamstrings", "Glutes", "Calves", "Abs", "Forearms", "Full Body", "Olympic", "Cardio", "Other"
    )

    val equipmentTypes: List<String> = listOf(
        "Barbell", "Dumbbells", "Cable", "Machine", "Bodyweight", "Kettlebell", "Cardio Machine", "Other"
    )

    val movementPatterns: List<String> = listOf(
        "Horizontal Push", "Horizontal Pull", "Vertical Push", "Vertical Pull", "Squat", "Hip Hinge", "Lunge", "Flexion", "Extension", "Rotation", "Cyclical", "Other"
    )

    fun filter(
        bodyPart: String? = null,
        equipment: String? = null,
        movementPattern: String? = null,
        searchQuery: String? = null
    ): List<ExerciseDefinition> {
        return allExercises.filter { exercise ->
            val matchesBodyPart = bodyPart == null || exercise.primaryBodyPart.equals(bodyPart, ignoreCase = true)
            val matchesEquipment = equipment == null || exercise.equipment.equals(equipment, ignoreCase = true)
            val matchesMovement = movementPattern == null || exercise.movementPattern.equals(movementPattern, ignoreCase = true)
            val matchesSearch = searchQuery == null || exercise.name.contains(searchQuery, ignoreCase = true)
            
            matchesBodyPart && matchesEquipment && matchesMovement && matchesSearch
        }
    }

    fun getExerciseByName(name: String): ExerciseDefinition? {
        return allExercises.find { it.name.equals(name, ignoreCase = true) }
    }
}
