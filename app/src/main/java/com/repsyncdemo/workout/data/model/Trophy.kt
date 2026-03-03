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
    GYM_RAT,
    LIFT_KING,
    RECOVERY
}

enum class TrophyRank(val colorRes: Int, val label: String) {
    LOCKED(R.color.text_secondary, "Locked"),
    BRONZE(R.color.rank_bronze, "Bronze"),
    SILVER(R.color.rank_silver, "Silver"),
    GOLD(R.color.rank_gold, "Gold"),
    DIAMOND(R.color.rank_diamond, "Diamond")
}
