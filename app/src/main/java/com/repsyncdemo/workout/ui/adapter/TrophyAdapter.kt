package com.repsyncdemo.workout.ui.adapter

/**
 * File overview: Binds trophy shelf cards and pin actions.
 */

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.data.model.Trophy
import com.repsyncdemo.workout.data.model.TrophyRank
import com.repsyncdemo.workout.data.model.TrophyType
import com.repsyncdemo.workout.databinding.DialogTrophyDetailBinding
import com.repsyncdemo.workout.databinding.ItemTrophyBinding

class TrophyAdapter(
    private val onPinClick: (Trophy) -> Unit
) : ListAdapter<Trophy, TrophyAdapter.ViewHolder>(TrophyDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTrophyBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemTrophyBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(trophy: Trophy) {
            val rank = trophy.rank
            val rankColor = ContextCompat.getColor(binding.root.context, rank.colorRes)
            val milestones = trophy.type.milestones()
            val nextMilestone = milestones.firstOrNull { trophy.currentProgress < it } ?: milestones.last()

            binding.tvTrophyName.text = trophy.name
            binding.tvTrophyRank.text = rank.label
            binding.tvTrophyRank.setTextColor(rankColor)
            binding.ivTrophyIcon.setImageResource(trophy.iconRes(rank))
            binding.ivTrophyIcon.imageTintList = null

            binding.trophyProgressBar.max = nextMilestone
            binding.trophyProgressBar.progress = trophy.currentProgress.coerceAtMost(nextMilestone)
            binding.trophyProgressBar.setIndicatorColor(rankColor)
            binding.tvTrophyProgressText.text =
                "${trophy.currentProgress} / $nextMilestone"

            binding.root.setOnClickListener { showTrophyDetails(trophy) }
            binding.ivTrophyIcon.setOnClickListener { showTrophyDetails(trophy) }
            binding.btnPinTrophy.setOnClickListener { onPinClick(trophy) }
        }

        private fun showTrophyDetails(trophy: Trophy) {
            val context = binding.root.context
            val rank = trophy.rank
            val rankColor = ContextCompat.getColor(context, rank.colorRes)
            val detailBinding = DialogTrophyDetailBinding.inflate(LayoutInflater.from(context))

            detailBinding.ivDialogTrophy.setImageResource(trophy.iconRes(rank))
            detailBinding.ivDialogTrophy.imageTintList = null
            detailBinding.tvDialogTitle.text = trophy.name
            detailBinding.tvDialogRank.text = rank.label
            detailBinding.tvDialogRank.setTextColor(rankColor)
            detailBinding.tvDialogDescription.text = trophy.description
            detailBinding.tvDialogProgress.text =
                "${trophy.currentProgress} ${trophy.type.unitLabel()} logged"

            buildTierRows(detailBinding.layoutTierRows, trophy)

            MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_App_MaterialAlertDialog)
                .setView(detailBinding.root)
                .setPositiveButton("Close", null)
                .show()
        }

        private fun buildTierRows(container: LinearLayout, trophy: Trophy) {
            container.removeAllViews()

            val tiers = listOf(
                TrophyRank.LOCKED to "< ${trophy.type.milestones().first()} ${trophy.type.unitLabel()}",
                TrophyRank.BRONZE to "${trophy.type.milestones()[0]} ${trophy.type.unitLabel()}",
                TrophyRank.SILVER to "${trophy.type.milestones()[1]} ${trophy.type.unitLabel()}",
                TrophyRank.GOLD to "${trophy.type.milestones()[2]} ${trophy.type.unitLabel()}",
                TrophyRank.DIAMOND to "${trophy.type.milestones()[3]} ${trophy.type.unitLabel()}"
            )

            tiers.forEach { (rank, requirement) ->
                val context = container.context
                val row = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(0, 8.dp(context), 0, 8.dp(context))
                }

                val icon = ImageView(context).apply {
                    setImageResource(trophy.iconRes(rank))
                    imageTintList = null
                    layoutParams = LinearLayout.LayoutParams(44.dp(context), 44.dp(context))
                }

                val label = TextView(context).apply {
                    text = "${rank.label}: $requirement"
                    setTextColor(ContextCompat.getColor(context, rank.colorRes))
                    textSize = 14f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginStart = 12.dp(context)
                    }
                }

                row.addView(icon)
                row.addView(label)
                container.addView(row)
            }
        }
    }

    class TrophyDiffCallback : DiffUtil.ItemCallback<Trophy>() {
        override fun areItemsTheSame(oldItem: Trophy, newItem: Trophy) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Trophy, newItem: Trophy) = oldItem == newItem
    }
}

fun TrophyType.milestones(): List<Int> = when (this) {
    TrophyType.GYM_RAT -> listOf(10, 40, 100, 250)
    TrophyType.RECOVERY -> listOf(5, 20, 50, 100)
    TrophyType.LIFT_KING -> listOf(25_000, 150_000, 500_000, 1_000_000)
    TrophyType.TICK_TOCK -> listOf(300, 1_500, 5_000, 12_000)
    TrophyType.SCALE_CHECK -> listOf(5, 25, 75, 150)
    TrophyType.REP_MACHINE -> listOf(2_500, 15_000, 50_000, 100_000)
    TrophyType.SET_COLLECTOR -> listOf(200, 1_000, 3_000, 7_500)
    TrophyType.CARDIO_CHAMP -> listOf(100, 500, 2_000, 5_000)
    TrophyType.ROUTINE_BUILDER -> listOf(2, 7, 15, 30)
    TrophyType.GOAL_GETTER -> listOf(3, 10, 25, 50)
    TrophyType.HEAVY_HITTER -> listOf(135, 225, 315, 405)
    TrophyType.GYM_BRO -> listOf(3, 10, 25, 50)
}

fun TrophyType.unitLabel(): String = when (this) {
    TrophyType.GYM_RAT -> "workouts"
    TrophyType.RECOVERY -> "rest days"
    TrophyType.LIFT_KING -> "lbs"
    TrophyType.TICK_TOCK -> "minutes"
    TrophyType.SCALE_CHECK -> "weigh-ins"
    TrophyType.REP_MACHINE -> "reps"
    TrophyType.SET_COLLECTOR -> "sets"
    TrophyType.CARDIO_CHAMP -> "minutes"
    TrophyType.ROUTINE_BUILDER -> "routines"
    TrophyType.GOAL_GETTER -> "goal points"
    TrophyType.HEAVY_HITTER -> "lbs"
    TrophyType.GYM_BRO -> "friends"
}

fun TrophyType.tierText(): String {
    val labels = listOf("Bronze", "Silver", "Gold", "Diamond")
    return labels.zip(milestones()).joinToString("\n") { (label, value) ->
        "$label: $value ${unitLabel()}"
    }
}

fun Trophy.iconRes(rank: TrophyRank): Int = when (type) {
    TrophyType.GYM_RAT -> rank.iconFor(
        R.drawable.gym_rat_locked,
        R.drawable.gym_rat_bronze,
        R.drawable.gym_rat_silver,
        R.drawable.gym_rat_gold,
        R.drawable.gym_rat_diamond
    )
    TrophyType.RECOVERY -> rank.iconFor(
        R.drawable.zzz_icon_locked,
        R.drawable.zzz_icon_bronze,
        R.drawable.zzz_icon_silver,
        R.drawable.zzz_icon_gold,
        R.drawable.zzz_icon_diamond
    )
    TrophyType.LIFT_KING -> rank.iconFor(
        R.drawable.lift_king_locked,
        R.drawable.lift_king_bronze,
        R.drawable.lift_king_silver,
        R.drawable.lift_king_gold,
        R.drawable.lift_king_diamond
    )
    TrophyType.TICK_TOCK -> rank.iconFor(
        R.drawable.tick_tock_locked,
        R.drawable.tick_tock_bronze,
        R.drawable.tick_tock_silver,
        R.drawable.tick_tock_gold,
        R.drawable.tick_tock_diamond
    )
    TrophyType.SCALE_CHECK -> rank.iconFor(
        R.drawable.scale_check_locked,
        R.drawable.scale_check_bronze,
        R.drawable.scale_check_silver,
        R.drawable.scale_check_gold,
        R.drawable.scale_check_diamond
    )
    TrophyType.REP_MACHINE -> rank.iconFor(
        R.drawable.rep_machine_locked,
        R.drawable.rep_machine_bronze,
        R.drawable.rep_machine_silver,
        R.drawable.rep_machine_gold,
        R.drawable.rep_machine_diamond
    )
    TrophyType.SET_COLLECTOR -> rank.iconFor(
        R.drawable.set_collector_locked,
        R.drawable.set_collector_bronze,
        R.drawable.set_collector_silver,
        R.drawable.set_collector_gold,
        R.drawable.set_collector_diamond
    )
    TrophyType.CARDIO_CHAMP -> rank.iconFor(
        R.drawable.cardio_champ_locked,
        R.drawable.cardio_champ_bronze,
        R.drawable.cardio_champ_silver,
        R.drawable.cardio_champ_gold,
        R.drawable.cardio_champ_diamond
    )
    TrophyType.ROUTINE_BUILDER -> rank.iconFor(
        R.drawable.routine_builder_locked,
        R.drawable.routine_builder_bronze,
        R.drawable.routine_builder_silver,
        R.drawable.routine_builder_gold,
        R.drawable.routine_builder_diamond
    )
    TrophyType.GOAL_GETTER -> rank.iconFor(
        R.drawable.goal_getter_locked,
        R.drawable.goal_getter_bronze,
        R.drawable.goal_getter_silver,
        R.drawable.goal_getter_gold,
        R.drawable.goal_getter_diamond
    )
    TrophyType.HEAVY_HITTER -> rank.iconFor(
        R.drawable.heavy_hitter_locked,
        R.drawable.heavy_hitter_bronze,
        R.drawable.heavy_hitter_silver,
        R.drawable.heavy_hitter_gold,
        R.drawable.heavy_hitter_diamond
    )
    TrophyType.GYM_BRO -> rank.iconFor(
        R.drawable.gym_bro_locked,
        R.drawable.gym_bro_bronze,
        R.drawable.gym_bro_silver,
        R.drawable.gym_bro_gold,
        R.drawable.gym_bro_diamond
    )
}

private fun TrophyRank.iconFor(
    locked: Int,
    bronze: Int,
    silver: Int,
    gold: Int,
    diamond: Int
): Int = when (this) {
    TrophyRank.BRONZE -> bronze
    TrophyRank.SILVER -> silver
    TrophyRank.GOLD -> gold
    TrophyRank.DIAMOND -> diamond
    else -> locked
}

private fun Int.dp(context: android.content.Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}
