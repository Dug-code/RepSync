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
            TrophyType.LIFT_KING -> when {
                currentProgress >= 1000000 -> TrophyRank.DIAMOND
                currentProgress >= 500000 -> TrophyRank.GOLD
                currentProgress >= 100000 -> TrophyRank.SILVER
                currentProgress >= 50000 -> TrophyRank.BRONZE
                else -> TrophyRank.LOCKED
            }
            TrophyType.RECOVERY -> when {
                currentProgress >= 500 -> TrophyRank.DIAMOND
                currentProgress >= 300 -> TrophyRank.GOLD
                currentProgress >= 200 -> TrophyRank.SILVER
                currentProgress >= 50 -> TrophyRank.BRONZE
                else -> TrophyRank.LOCKED
            }
            TrophyType.BENCH_PRESS -> when {
                currentProgress >= 315 -> TrophyRank.DIAMOND
                currentProgress >= 225 -> TrophyRank.GOLD
                currentProgress >= 185 -> TrophyRank.SILVER
                currentProgress >= 135 -> TrophyRank.BRONZE
                else -> TrophyRank.LOCKED
            }
            TrophyType.SQUAT -> when {
                currentProgress >= 405 -> TrophyRank.DIAMOND
                currentProgress >= 315 -> TrophyRank.GOLD
                currentProgress >= 225 -> TrophyRank.SILVER
                currentProgress >= 135 -> TrophyRank.BRONZE
                else -> TrophyRank.LOCKED
            }
            TrophyType.DEADLIFT -> when {
                currentProgress >= 495 -> TrophyRank.DIAMOND
                currentProgress >= 405 -> TrophyRank.GOLD
                currentProgress >= 315 -> TrophyRank.SILVER
                currentProgress >= 225 -> TrophyRank.BRONZE
                else -> TrophyRank.LOCKED
            }
            TrophyType.SHOULDER_PRESS -> when {
                currentProgress >= 225 -> TrophyRank.DIAMOND
                currentProgress >= 185 -> TrophyRank.GOLD
                currentProgress >= 135 -> TrophyRank.SILVER
                currentProgress >= 95 -> TrophyRank.BRONZE
                else -> TrophyRank.LOCKED
            }
            TrophyType.CARDIO_BUNNY -> when {
                currentProgress >= 5000 -> TrophyRank.DIAMOND
                currentProgress >= 2000 -> TrophyRank.GOLD
                currentProgress >= 1000 -> TrophyRank.SILVER
                currentProgress >= 500 -> TrophyRank.BRONZE
                else -> TrophyRank.LOCKED
            }
            TrophyType.DUMBBELL_MASTER -> when {
                currentProgress >= 120 -> TrophyRank.DIAMOND
                currentProgress >= 100 -> TrophyRank.GOLD
                currentProgress >= 80 -> TrophyRank.SILVER
                currentProgress >= 50 -> TrophyRank.BRONZE
                else -> TrophyRank.LOCKED
            }
            TrophyType.ABS_MASTER -> when {
                currentProgress >= 1000 -> TrophyRank.DIAMOND
                currentProgress >= 500 -> TrophyRank.GOLD
                currentProgress >= 200 -> TrophyRank.SILVER
                currentProgress >= 100 -> TrophyRank.BRONZE
                else -> TrophyRank.LOCKED
            }
            TrophyType.ALL_STAR -> when {
                currentProgress >= 1200 -> TrophyRank.DIAMOND
                currentProgress >= 1000 -> TrophyRank.GOLD
                currentProgress >= 800 -> TrophyRank.SILVER
                currentProgress >= 500 -> TrophyRank.BRONZE
                else -> TrophyRank.LOCKED
            }
            TrophyType.FULL_TIME -> when {
                currentProgress >= 10000 -> TrophyRank.DIAMOND
                currentProgress >= 5000 -> TrophyRank.GOLD
                currentProgress >= 2500 -> TrophyRank.SILVER
                currentProgress >= 1000 -> TrophyRank.BRONZE
                else -> TrophyRank.LOCKED
            }
            TrophyType.GYM_BRO -> when {
                currentProgress >= 100 -> TrophyRank.DIAMOND
                currentProgress >= 50 -> TrophyRank.GOLD
                currentProgress >= 20 -> TrophyRank.SILVER
                currentProgress >= 10 -> TrophyRank.BRONZE
                else -> TrophyRank.LOCKED
            }
            TrophyType.PUSHUP_MASTER -> when {
                currentProgress >= 10000 -> TrophyRank.DIAMOND
                currentProgress >= 5000 -> TrophyRank.GOLD
                currentProgress >= 2000 -> TrophyRank.SILVER
                currentProgress >= 500 -> TrophyRank.BRONZE
                else -> TrophyRank.LOCKED
            }
            TrophyType.PULLUP_MASTER -> when {
                currentProgress >= 2000 -> TrophyRank.DIAMOND
                currentProgress >= 1000 -> TrophyRank.GOLD
                currentProgress >= 500 -> TrophyRank.SILVER
                currentProgress >= 100 -> TrophyRank.BRONZE
                else -> TrophyRank.LOCKED
            }
            TrophyType.SITUP_MASTER -> when {
                currentProgress >= 10000 -> TrophyRank.DIAMOND
                currentProgress >= 5000 -> TrophyRank.GOLD
                currentProgress >= 2000 -> TrophyRank.SILVER
                currentProgress >= 500 -> TrophyRank.BRONZE
                else -> TrophyRank.LOCKED
            }
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
