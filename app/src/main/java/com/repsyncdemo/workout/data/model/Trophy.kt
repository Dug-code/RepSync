package com.repsyncdemo.workout.data.model

/**
 * File overview: Defines trophy progress, trophy types, and rank colors.
 */

import com.repsyncdemo.workout.R

data class Trophy(
    val id: String,
    val name: String,
    val description: String,
    val currentProgress: Int,
    val type: TrophyType
) {
    val rank: TrophyRank = when (type) {
        TrophyType.GYM_RAT -> currentProgress.rankAt(10, 40, 100, 250)
        TrophyType.RECOVERY -> currentProgress.rankAt(5, 20, 50, 100)
        TrophyType.LIFT_KING -> currentProgress.rankAt(25_000, 150_000, 500_000, 1_000_000)
        TrophyType.TICK_TOCK -> currentProgress.rankAt(300, 1_500, 5_000, 12_000)
        TrophyType.SCALE_CHECK -> currentProgress.rankAt(5, 25, 75, 150)
        TrophyType.REP_MACHINE -> currentProgress.rankAt(2_500, 15_000, 50_000, 100_000)
        TrophyType.SET_COLLECTOR -> currentProgress.rankAt(200, 1_000, 3_000, 7_500)
        TrophyType.CARDIO_CHAMP -> currentProgress.rankAt(100, 500, 2_000, 5_000)
        TrophyType.ROUTINE_BUILDER -> currentProgress.rankAt(2, 7, 15, 30)
        TrophyType.GOAL_GETTER -> currentProgress.rankAt(3, 10, 25, 50)
        TrophyType.HEAVY_HITTER -> currentProgress.rankAt(135, 225, 315, 405)
        TrophyType.GYM_BRO -> currentProgress.rankAt(3, 10, 25, 50)
    }

    private fun Int.rankAt(
        bronze: Int,
        silver: Int,
        gold: Int,
        diamond: Int
    ): TrophyRank = when {
        this >= diamond -> TrophyRank.DIAMOND
        this >= gold -> TrophyRank.GOLD
        this >= silver -> TrophyRank.SILVER
        this >= bronze -> TrophyRank.BRONZE
        else -> TrophyRank.LOCKED
    }
}

enum class TrophyType {
    GYM_RAT,
    RECOVERY,
    LIFT_KING,
    TICK_TOCK,
    SCALE_CHECK,
    REP_MACHINE,
    SET_COLLECTOR,
    CARDIO_CHAMP,
    ROUTINE_BUILDER,
    GOAL_GETTER,
    HEAVY_HITTER,
    GYM_BRO
}

enum class TrophyRank(val colorRes: Int, val label: String) {
    LOCKED(R.color.text_secondary, "Locked"),
    BRONZE(R.color.rank_bronze, "Bronze"),
    SILVER(R.color.rank_silver, "Silver"),
    GOLD(R.color.rank_gold, "Gold"),
    DIAMOND(R.color.rank_diamond, "Diamond")
}
