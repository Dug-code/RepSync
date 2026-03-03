package com.repsyncdemo.workout.ui.adapter

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.data.model.Trophy
import com.repsyncdemo.workout.data.model.TrophyRank
import com.repsyncdemo.workout.data.model.TrophyType
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
            binding.tvTrophyName.text = trophy.name
            val rank = trophy.rank
            binding.tvTrophyRank.text = rank.label

            // Set the Icon based on Rank and Type
            val iconRes = when (trophy.type) {
                TrophyType.GYM_RAT -> when (rank) {
                    TrophyRank.BRONZE -> R.drawable.gym_rat_bronze
                    TrophyRank.SILVER -> R.drawable.gym_rat_silver
                    TrophyRank.GOLD -> R.drawable.gym_rat_gold
                    TrophyRank.DIAMOND -> R.drawable.gym_rat_diamond
                    else -> R.drawable.gym_rat_locked
                }
                TrophyType.RECOVERY -> when (rank) {
                    TrophyRank.BRONZE -> R.drawable.zzz_icon_bronze
                    TrophyRank.SILVER -> R.drawable.zzz_icon_silver
                    TrophyRank.GOLD -> R.drawable.zzz_icon_gold
                    TrophyRank.DIAMOND -> R.drawable.zzz_icon_diamond
                    else -> R.drawable.zzz_icon_locked
                }
                else -> R.drawable.ic_trophy
            }
            binding.ivTrophyIcon.setImageResource(iconRes)

            // Setup Progress Bar and Text
            val (nextMilestone, progressText) = when (trophy.type) {
                TrophyType.GYM_RAT -> {
                    val milestone = when (rank) {
                        TrophyRank.LOCKED -> 10
                        TrophyRank.BRONZE -> 50
                        TrophyRank.SILVER -> 100
                        TrophyRank.GOLD -> 500
                        TrophyRank.DIAMOND -> 500
                    }
                    Pair(milestone, "${trophy.currentProgress} / $milestone workouts")
                }
                TrophyType.RECOVERY -> {
                    val milestone = when (rank) {
                        TrophyRank.LOCKED -> 50
                        TrophyRank.BRONZE -> 200
                        TrophyRank.SILVER -> 300
                        TrophyRank.GOLD -> 500
                        TrophyRank.DIAMOND -> 500
                    }
                    Pair(milestone, "${trophy.currentProgress} / $milestone rest days")
                }
                else -> Pair(0, "")
            }

            binding.trophyProgressBar.max = nextMilestone
            binding.trophyProgressBar.progress = trophy.currentProgress.coerceAtMost(nextMilestone)
            binding.tvTrophyProgressText.text = progressText

            binding.btnPinTrophy.setOnClickListener { onPinClick(trophy) }

            binding.btnInfo.setOnClickListener { 
                val message = when (trophy.type) {
                    TrophyType.GYM_RAT -> "Locked: < 10 Workouts\n" +
                                        "Bronze: 10 Workouts\n" +
                                        "Silver: 50 Workouts\n" +
                                        "Gold: 100 Workouts\n" +
                                        "Diamond: 500 Workouts"
                    TrophyType.RECOVERY -> "Locked: < 50 Rest Days\n" +
                                           "Bronze: 50 Rest Days\n" +
                                           "Silver: 200 Rest Days\n" +
                                           "Gold: 300 Rest Days\n" +
                                           "Diamond: 500 Rest Days"
                    else -> "No tiers available."
                }
                
                AlertDialog.Builder(binding.root.context)
                    .setTitle("${trophy.name} Tiers")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    class TrophyDiffCallback : DiffUtil.ItemCallback<Trophy>() {
        override fun areItemsTheSame(oldItem: Trophy, newItem: Trophy) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Trophy, newItem: Trophy) = oldItem == newItem
    }
}
