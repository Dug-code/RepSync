package com.repsyncdemo.workout.data.model

import com.repsyncdemo.workout.R

data class Trophy(
    val id: String,
    val name: String,
    val description: String,
    val currentProgress: Int,
    val type: TrophyType
) {
    val rank: TrophyRank
        get() = when (type) {
            TrophyType.GYM_RAT -> when {
                currentProgress >= 500 -> TrophyRank.DIAMOND
                currentProgress >= 100 -> TrophyRank.GOLD
                currentProgress >= 50 -> TrophyRank.SILVER
                currentProgress >= 10 -> TrophyRank.BRONZE
                else -> TrophyRank.LOCKED
            }
            TrophyType.RECOVERY -> when {
                currentProgress >= 500 -> TrophyRank.DIAMOND
                currentProgress >= 300 -> TrophyRank.GOLD
                currentProgress >= 200 -> TrophyRank.SILVER
                currentProgress >= 50 -> TrophyRank.BRONZE
                else -> TrophyRank.LOCKED
            }
            else -> TrophyRank.LOCKED
        }
}

enum class TrophyType {
    GYM_RAT, // Rank based on total workouts completed
    LIFT_KING, // Rank based on total volume lifted
    RECOVERY, // Rank based on total rest days
    BENCH_PRESS, // Rank based on max bench press
    SQUAT, // Rank based on max squat
    DEADLIFT, // Rank based on max deadlift
    SHOULDER_PRESS, // Rank based on max shoulder press
    CARDIO_BUNNY, // Rank based on total cardio minutes
    DUMBBELL_MASTER, // Rank based on max weight lifted with dumbbell
    ABS_MASTER, // Rank based on total abs minutes
    ALL_STAR, // Rank based on combined total SBD
    FULL_TIME, // Rank based on total time working out
    GYM_BRO, //Rank based on total amount of Friends
    PUSHUP_MASTER, // Rank based on total pushups
    PULLUP_MASTER, // Rank based on total pullups
    SITUP_MASTER, // Rank based on total situps




}

enum class TrophyRank(val colorRes: Int, val label: String) {
    LOCKED(R.color.text_secondary, "Locked"),
    BRONZE(R.color.rank_bronze, "Bronze"),
    SILVER(R.color.rank_silver, "Silver"),
    GOLD(R.color.rank_gold, "Gold"),
    DIAMOND(R.color.rank_diamond, "Diamond")
}
