package com.repsyncdemo.workout.data

import android.util.Log
import com.repsyncdemo.workout.data.model.ExerciseDefinition

object ExerciseDatabase {

    val allExercises: List<ExerciseDefinition> = listOf(
        // ===== CHEST =====
        ExerciseDefinition("Bench Press (Barbell)", "Chest", "Arms – Triceps; Shoulders", "Barbell", "Horizontal Push"),
        ExerciseDefinition("Bench Press (Dumbbells)", "Chest", "Arms – Triceps; Shoulders", "Dumbbells", "Horizontal Push"),
        ExerciseDefinition("Incline Bench Press (Barbell)", "Chest", "Arms – Triceps; Shoulders", "Barbell", "Horizontal Push"),
        ExerciseDefinition("Incline Bench Press (Dumbbells)", "Chest", "Arms – Triceps; Shoulders", "Dumbbells", "Horizontal Push"),
        ExerciseDefinition("Decline Bench Press (Barbell)", "Chest", "Arms – Triceps", "Barbell", "Horizontal Push"),
        ExerciseDefinition("Chest Press (Machine)", "Chest", "Arms – Triceps; Shoulders", "Machine", "Horizontal Push"),
        ExerciseDefinition("Push-Up", "Chest", "Arms – Triceps; Shoulders; Core", "Bodyweight", "Horizontal Push"),
        ExerciseDefinition("Incline Push-Up", "Chest", "Arms – Triceps; Shoulders; Core", "Bodyweight", "Horizontal Push"),
        ExerciseDefinition("Decline Push-Up", "Chest", "Arms – Triceps; Shoulders; Core", "Bodyweight", "Horizontal Push"),
        ExerciseDefinition("Chest Dip", "Chest", "Arms – Triceps; Shoulders", "Bodyweight", "Horizontal Push"),
        ExerciseDefinition("Assisted Chest Dip", "Chest", "Arms – Triceps; Shoulders", "Machine", "Horizontal Push"),
        ExerciseDefinition("Cable Fly (High to Low)", "Chest", "Shoulders", "Cable", "Horizontal Push"),
        ExerciseDefinition("Cable Fly (Low to High)", "Chest", "Shoulders", "Cable", "Horizontal Push"),
        ExerciseDefinition("Dumbbell Fly (Flat)", "Chest", "Shoulders", "Dumbbells", "Horizontal Push"),
        ExerciseDefinition("Dumbbell Fly (Incline)", "Chest", "Shoulders", "Dumbbells", "Horizontal Push"),
        ExerciseDefinition("Pec Deck (Machine Fly)", "Chest", "Shoulders", "Machine", "Horizontal Push"),
        ExerciseDefinition("Pullover (Dumbbells)", "Chest", "Back", "Dumbbells", "Extension"),
        ExerciseDefinition("Landmine Press", "Chest", "Shoulders; Core", "Barbell", "Horizontal Push"),

        // ===== BACK =====
        ExerciseDefinition("Pull-Up", "Back", "Arms – Biceps; Core", "Bodyweight", "Vertical Pull"),
        ExerciseDefinition("Chin-Up", "Back", "Arms – Biceps", "Bodyweight", "Vertical Pull"),
        ExerciseDefinition("Assisted Pull-Up", "Back", "Arms – Biceps", "Machine", "Vertical Pull"),
        ExerciseDefinition("Lat Pulldown (Wide Grip)", "Back", "Arms – Biceps", "Cable", "Vertical Pull"),
        ExerciseDefinition("Lat Pulldown (Close Grip)", "Back", "Arms – Biceps", "Cable", "Vertical Pull"),
        ExerciseDefinition("Straight-Arm Pulldown", "Back", "Core", "Cable", "Vertical Pull"),
        ExerciseDefinition("Seated Cable Row", "Back", "Arms – Biceps", "Cable", "Horizontal Pull"),
        ExerciseDefinition("Bent-Over Row (Barbell)", "Back", "Arms – Biceps; Core", "Barbell", "Horizontal Pull"),
        ExerciseDefinition("One-Arm Dumbbell Row", "Back", "Arms – Biceps; Core", "Dumbbells", "Horizontal Pull"),
        ExerciseDefinition("Chest-Supported Row (Machine)", "Back", "Arms – Biceps", "Machine", "Horizontal Pull"),
        ExerciseDefinition("T-Bar Row", "Back", "Arms – Biceps", "Machine", "Horizontal Pull"),
        ExerciseDefinition("Inverted Row", "Back", "Arms – Biceps; Core", "Bodyweight", "Horizontal Pull"),
        ExerciseDefinition("Face Pull", "Back", "Shoulders", "Cable", "Horizontal Pull"),
        ExerciseDefinition("Reverse Pec Deck", "Back", "Shoulders", "Machine", "Horizontal Pull"),
        ExerciseDefinition("Back Extension (Roman Chair)", "Back", "Legs; Core", "Bodyweight", "Extension"),
        ExerciseDefinition("Good Morning (Barbell)", "Back", "Legs; Core", "Barbell", "Hip Hinge"),
        ExerciseDefinition("Scapular Pull-Up", "Back", "Shoulders", "Bodyweight", "Vertical Pull"),
        ExerciseDefinition("Dead Hang", "Back", "Arms – Biceps; Core", "Bodyweight", "Carry"),

        // ===== SHOULDERS =====
        ExerciseDefinition("Overhead Press (Barbell)", "Shoulders", "Arms – Triceps; Core", "Barbell", "Vertical Push"),
        ExerciseDefinition("Overhead Press (Dumbbells)", "Shoulders", "Arms – Triceps; Core", "Dumbbells", "Vertical Push"),
        ExerciseDefinition("Seated Shoulder Press (Machine)", "Shoulders", "Arms – Triceps", "Machine", "Vertical Push"),
        ExerciseDefinition("Arnold Press", "Shoulders", "Arms – Triceps", "Dumbbells", "Vertical Push"),
        ExerciseDefinition("Push Press", "Shoulders", "Legs; Arms – Triceps", "Barbell", "Vertical Push"),
        ExerciseDefinition("Lateral Raise (Dumbbells)", "Shoulders", "None", "Dumbbells", "Extension"),
        ExerciseDefinition("Lateral Raise (Cable)", "Shoulders", "None", "Cable", "Extension"),
        ExerciseDefinition("Lateral Raise (Machine)", "Shoulders", "None", "Machine", "Extension"),
        ExerciseDefinition("Front Raise (Dumbbells)", "Shoulders", "Chest", "Dumbbells", "Extension"),
        ExerciseDefinition("Reverse Fly (Dumbbells)", "Shoulders", "Back", "Dumbbells", "Horizontal Pull"),
        ExerciseDefinition("Reverse Fly (Cable)", "Shoulders", "Back", "Cable", "Horizontal Pull"),
        ExerciseDefinition("Upright Row (Barbell)", "Shoulders", "Back", "Barbell", "Vertical Pull"),
        ExerciseDefinition("Shrug (Dumbbells)", "Shoulders", "Back", "Dumbbells", "Carry"),
        ExerciseDefinition("Face Pull (Shoulder Focus)", "Shoulders", "Back", "Cable", "Horizontal Pull"),

        // ===== ARMS – BICEPS =====
        ExerciseDefinition("Biceps Curl (Barbell)", "Arms – Biceps", "Forearms", "Barbell", "Flexion"),
        ExerciseDefinition("Biceps Curl (Dumbbells)", "Arms – Biceps", "Forearms", "Dumbbells", "Flexion"),
        ExerciseDefinition("Alternating Dumbbell Curl", "Arms – Biceps", "Forearms", "Dumbbells", "Flexion"),
        ExerciseDefinition("Hammer Curl", "Arms – Biceps", "Forearms", "Dumbbells", "Flexion"),
        ExerciseDefinition("Cable Curl", "Arms – Biceps", "Forearms", "Cable", "Flexion"),
        ExerciseDefinition("Preacher Curl (Barbell)", "Arms – Biceps", "Forearms", "Barbell", "Flexion"),
        ExerciseDefinition("Preacher Curl (Machine)", "Arms – Biceps", "Forearms", "Machine", "Flexion"),
        ExerciseDefinition("Concentration Curl", "Arms – Biceps", "Forearms", "Dumbbells", "Flexion"),
        ExerciseDefinition("Incline Dumbbell Curl", "Arms – Biceps", "Forearms", "Dumbbells", "Flexion"),
        ExerciseDefinition("Reverse Curl (Barbell)", "Arms – Biceps", "Forearms", "Barbell", "Flexion"),

        // ===== ARMS – TRICEPS =====
        ExerciseDefinition("Triceps Pushdown (Straight Bar)", "Arms – Triceps", "Chest", "Cable", "Extension"),
        ExerciseDefinition("Triceps Pushdown (Rope)", "Arms – Triceps", "Chest", "Cable", "Extension"),
        ExerciseDefinition("Overhead Triceps Extension (Dumbbells)", "Arms – Triceps", "Shoulders", "Dumbbells", "Extension"),
        ExerciseDefinition("Overhead Triceps Extension (Cable)", "Arms – Triceps", "Shoulders", "Cable", "Extension"),
        ExerciseDefinition("Skullcrusher (Barbell)", "Arms – Triceps", "Chest", "Barbell", "Extension"),
        ExerciseDefinition("Skullcrusher (Dumbbells)", "Arms – Triceps", "Chest", "Dumbbells", "Extension"),
        ExerciseDefinition("Close-Grip Bench Press", "Arms – Triceps", "Chest; Shoulders", "Barbell", "Horizontal Push"),
        ExerciseDefinition("Bench Dip", "Arms – Triceps", "Chest; Shoulders", "Bodyweight", "Extension"),
        ExerciseDefinition("Triceps Extension (Machine)", "Arms – Triceps", "None", "Machine", "Extension"),
        ExerciseDefinition("Diamond Push-Up", "Arms – Triceps", "Chest; Core", "Bodyweight", "Horizontal Push"),

        // ===== LEGS =====
        ExerciseDefinition("Back Squat (Barbell)", "Legs", "Core; Back", "Barbell", "Squat"),
        ExerciseDefinition("Front Squat (Barbell)", "Legs", "Core; Back", "Barbell", "Squat"),
        ExerciseDefinition("Goblet Squat", "Legs", "Core", "Dumbbells", "Squat"),
        ExerciseDefinition("Hack Squat (Machine)", "Legs", "Glutes", "Machine", "Squat"),
        ExerciseDefinition("Smith Machine Squat", "Legs", "Glutes; Core", "Smith Machine", "Squat"),
        ExerciseDefinition("Leg Press", "Legs", "Glutes", "Machine", "Squat"),
        ExerciseDefinition("Leg Extension", "Legs", "None", "Machine", "Extension"),
        ExerciseDefinition("Seated Leg Curl", "Legs", "None", "Machine", "Flexion"),
        ExerciseDefinition("Lying Leg Curl", "Legs", "None", "Machine", "Flexion"),
        ExerciseDefinition("Romanian Deadlift (Barbell)", "Legs", "Back; Core", "Barbell", "Hip Hinge"),
        ExerciseDefinition("Romanian Deadlift (Dumbbells)", "Legs", "Back; Core", "Dumbbells", "Hip Hinge"),
        ExerciseDefinition("Deadlift (Conventional)", "Legs", "Back; Core", "Barbell", "Hip Hinge"),
        ExerciseDefinition("Deadlift (Sumo)", "Legs", "Back; Core", "Barbell", "Hip Hinge"),
        ExerciseDefinition("Hip Thrust (Barbell)", "Legs", "Core", "Barbell", "Hip Hinge"),
        ExerciseDefinition("Glute Bridge (Bodyweight)", "Legs", "Core", "Bodyweight", "Hip Hinge"),
        ExerciseDefinition("Cable Pull-Through", "Legs", "Back; Core", "Cable", "Hip Hinge"),
        ExerciseDefinition("Bulgarian Split Squat", "Legs", "Core", "Dumbbells", "Lunge / Split Squat"),
        ExerciseDefinition("Reverse Lunge", "Legs", "Core", "Bodyweight", "Lunge / Split Squat"),
        ExerciseDefinition("Walking Lunge", "Legs", "Core", "Dumbbells", "Lunge / Split Squat"),
        ExerciseDefinition("Step-Up", "Legs", "Core", "Dumbbells", "Lunge / Split Squat"),
        ExerciseDefinition("Calf Raise (Standing)", "Legs", "None", "Machine", "Extension"),
        ExerciseDefinition("Calf Raise (Seated)", "Legs", "None", "Machine", "Extension"),
        ExerciseDefinition("Hip Abductor (Machine)", "Legs", "Core", "Machine", "Extension"),
        ExerciseDefinition("Hip Adductor (Machine)", "Legs", "Core", "Machine", "Extension"),
        ExerciseDefinition("Jump Squat", "Legs", "Core", "Bodyweight", "Plyometric"),
        ExerciseDefinition("Box Jump", "Legs", "Core", "Bodyweight", "Plyometric"),
        ExerciseDefinition("Wall Sit", "Legs", "Core", "Bodyweight", "Squat"),
        ExerciseDefinition("Kettlebell Goblet Squat", "Legs", "Core", "Kettlebell", "Squat"),

        // ===== CORE =====
        ExerciseDefinition("Plank", "Core", "Shoulders; Legs", "Bodyweight", "Anti-Rotation"),
        ExerciseDefinition("Side Plank", "Core", "Shoulders", "Bodyweight", "Anti-Rotation"),
        ExerciseDefinition("Dead Bug", "Core", "None", "Bodyweight", "Anti-Rotation"),
        ExerciseDefinition("Bird Dog", "Core", "Back; Shoulders", "Bodyweight", "Anti-Rotation"),
        ExerciseDefinition("Pallof Press", "Core", "Shoulders", "Cable", "Anti-Rotation"),
        ExerciseDefinition("Cable Woodchop", "Core", "Shoulders", "Cable", "Rotation"),
        ExerciseDefinition("Russian Twist", "Core", "None", "Bodyweight", "Rotation"),
        ExerciseDefinition("Bicycle Crunch", "Core", "None", "Bodyweight", "Flexion"),
        ExerciseDefinition("Crunch", "Core", "None", "Bodyweight", "Flexion"),
        ExerciseDefinition("Decline Crunch", "Core", "None", "Bodyweight", "Flexion"),
        ExerciseDefinition("Cable Crunch", "Core", "None", "Cable", "Flexion"),
        ExerciseDefinition("Hanging Knee Raise", "Core", "Legs", "Bodyweight", "Flexion"),
        ExerciseDefinition("Hanging Leg Raise", "Core", "Legs", "Bodyweight", "Flexion"),
        ExerciseDefinition("Toes-to-Bar", "Core", "Back; Arms – Biceps", "Bodyweight", "Flexion"),
        ExerciseDefinition("Ab Wheel Rollout", "Core", "Shoulders", "Other", "Extension"),
        ExerciseDefinition("Back Extension", "Core", "Back", "Bodyweight", "Extension"),
        ExerciseDefinition("Sit-Up", "Core", "None", "Bodyweight", "Flexion"),
        ExerciseDefinition("Suitcase Carry", "Core", "Shoulders", "Dumbbells", "Carry"),

        // ===== FULL BODY =====
        ExerciseDefinition("Burpee", "Full Body", "Cardio; Core", "Bodyweight", "Plyometric"),
        ExerciseDefinition("Kettlebell Swing", "Full Body", "Legs; Core", "Kettlebell", "Hip Hinge"),
        ExerciseDefinition("Thruster (Barbell)", "Full Body", "Shoulders; Legs; Core", "Barbell", "Squat"),
        ExerciseDefinition("Thruster (Dumbbells)", "Full Body", "Shoulders; Legs; Core", "Dumbbells", "Squat"),
        ExerciseDefinition("Bear Crawl", "Full Body", "Core; Shoulders", "Bodyweight", "Cyclical"),
        ExerciseDefinition("Farmer's Carry", "Full Body", "Core; Back; Shoulders", "Dumbbells", "Carry"),
        ExerciseDefinition("Kettlebell Turkish Get-Up", "Full Body", "Core; Shoulders", "Kettlebell", "Carry"),
        ExerciseDefinition("Medicine Ball Slam", "Full Body", "Core; Back", "Other", "Plyometric"),
        ExerciseDefinition("Jumping Jack", "Full Body", "Cardio", "Bodyweight", "Cyclical"),
        ExerciseDefinition("Sled Push", "Full Body", "Legs; Core", "Other", "Carry"),
        ExerciseDefinition("Sled Pull", "Full Body", "Legs; Core", "Other", "Carry"),
        ExerciseDefinition("Battle Rope Waves", "Full Body", "Shoulders; Core", "Other", "Cyclical"),

        // ===== OLYMPIC =====
        ExerciseDefinition("Power Clean", "Olympic", "Legs; Back; Shoulders", "Barbell", "Hip Hinge"),
        ExerciseDefinition("Hang Clean", "Olympic", "Legs; Back; Shoulders", "Barbell", "Hip Hinge"),
        ExerciseDefinition("Clean and Jerk", "Olympic", "Full Body", "Barbell", "Vertical Push"),
        ExerciseDefinition("Push Jerk", "Olympic", "Legs; Shoulders", "Barbell", "Vertical Push"),
        ExerciseDefinition("Split Jerk", "Olympic", "Legs; Shoulders", "Barbell", "Vertical Push"),
        ExerciseDefinition("Snatch", "Olympic", "Full Body", "Barbell", "Hip Hinge"),
        ExerciseDefinition("Power Snatch", "Olympic", "Legs; Back; Shoulders", "Barbell", "Hip Hinge"),
        ExerciseDefinition("Hang Snatch", "Olympic", "Legs; Back; Shoulders", "Barbell", "Hip Hinge"),
        ExerciseDefinition("Overhead Squat", "Olympic", "Shoulders; Core; Legs", "Barbell", "Squat"),
        ExerciseDefinition("High Pull (Barbell)", "Olympic", "Back; Shoulders", "Barbell", "Vertical Pull"),

        // ===== CARDIO =====
        ExerciseDefinition("Running (Outdoor)", "Cardio", "Legs", "Other", "Cyclical"),
        ExerciseDefinition("Treadmill Run", "Cardio", "Legs", "Cardio Machine", "Cyclical"),
        ExerciseDefinition("Walking", "Cardio", "Legs", "Other", "Cyclical"),
        ExerciseDefinition("Incline Treadmill Walk", "Cardio", "Legs", "Cardio Machine", "Cyclical"),
        ExerciseDefinition("Cycling (Outdoor)", "Cardio", "Legs", "Other", "Cyclical"),
        ExerciseDefinition("Stationary Bike", "Cardio", "Legs", "Cardio Machine", "Cyclical"),
        ExerciseDefinition("Rowing Machine", "Cardio", "Back; Legs", "Cardio Machine", "Cyclical"),
        ExerciseDefinition("Elliptical", "Cardio", "Legs", "Cardio Machine", "Cyclical"),
        ExerciseDefinition("Stair Climber", "Cardio", "Legs", "Cardio Machine", "Cyclical"),
        ExerciseDefinition("Jump Rope", "Cardio", "Legs; Core", "Other", "Plyometric"),
        ExerciseDefinition("Swimming", "Cardio", "Full Body", "Other", "Cyclical"),
        ExerciseDefinition("Assault Bike / Air Bike", "Cardio", "Legs; Full Body", "Cardio Machine", "Cyclical")
    )

    val bodyParts: List<String> by lazy {
        allExercises.map { it.primaryBodyPart }.distinct().sorted()
    }

    val equipmentTypes: List<String> by lazy {
        allExercises.map { it.equipment }.distinct().sorted()
    }

    val movementPatterns: List<String> by lazy {
        allExercises.map { it.movementPattern }.distinct().sorted()
    }

    fun filter(
        bodyPart: String? = null,
        equipment: String? = null,
        movementPattern: String? = null,
        searchQuery: String? = null
    ): List<ExerciseDefinition> {
        return allExercises.map { exercise ->
            val score = calculateMatchScore(exercise, bodyPart, equipment, movementPattern, searchQuery)
            exercise to score
        }.filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    private fun calculateMatchScore(
        exercise: ExerciseDefinition,
        bodyPart: String?,
        equipment: String?,
        movementPattern: String?,
        searchQuery: String?
    ): Int {
        var score = 0

        // Equipment filters
        if (equipment != null) {
            val eqLower = equipment.lowercase()

            if (exercise.equipment.equals(eqLower, ignoreCase = true)) {
                score += 500
            } else {
                return 0
            }
        }

        //Movement filter
        if (movementPattern != null) {
            val mpLower = movementPattern.lowercase()

            if (exercise.movementPattern.equals(mpLower, ignoreCase = true)) {
                score += 500
            } else {
                return 0
            }
        }

        // Body part filtering logic (allowing secondary matches)
        if (bodyPart != null) {
            val bpLower = bodyPart.lowercase()
            val primaryLower = exercise.primaryBodyPart.lowercase()
            val secondaryList = exercise.secondaryBodyParts.split(";").map { it.trim().lowercase() }

            if (primaryLower == bpLower) {
                score += 500 // Strong primary match
            } else if (secondaryList.any { it.contains(bpLower) }) {
                score += 100 // Secondary match
            } else {
                return 0 // No match for selected body part
            }
        }

        // Search Query logic
        if (!searchQuery.isNullOrBlank()) {
            val q = searchQuery.lowercase()
            val name = exercise.name.lowercase()
            val primary = exercise.primaryBodyPart.lowercase()
            val secondary = exercise.secondaryBodyParts.lowercase()

            when {
                name.contains(q) -> score += 1000 // High priority for name matches
                primary.contains(q) -> score += 50 // Primary muscle match
                secondary.contains(q) -> score += 10 // Secondary muscle match
                else -> if (score == 0) return 0 // Doesn't match search query and no bodyPart match
            }
        }

        return if (score == 0 && bodyPart == null && equipment == null && movementPattern == null && searchQuery == null) 1 else score
    }

    fun getExerciseByName(name: String): ExerciseDefinition? {
        return allExercises.find { it.name.equals(name, ignoreCase = true) }
    }
}
