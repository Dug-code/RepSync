package com.repsyncdemo.workout.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.repsyncdemo.workout.data.model.Goal
import com.repsyncdemo.workout.data.model.GoalType
import com.repsyncdemo.workout.databinding.ItemMiniGoalBinding
import kotlin.math.abs

class MiniGoalAdapter : ListAdapter<Goal, MiniGoalAdapter.ViewHolder>(GoalDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMiniGoalBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemMiniGoalBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(goal: Goal) {
            binding.tvMiniGoalTitle.text = goal.title
            
            val start = if (goal.startingValue == 0.0) goal.currentValue else goal.startingValue
            val change = goal.currentValue - start
            val changeText = if (change >= 0) "+${change.toInt()}" else "${change.toInt()}"
            
            // Format: "198 lbs (-2) | Goal: 190"
            binding.tvMiniGoalValue.text = "${goal.currentValue.toInt()} ${goal.unit} ($changeText)\nGoal: ${goal.targetValue.toInt()}"
            
            val totalDiff = abs(goal.targetValue - start)
            val currentDiff = if (goal.type == GoalType.WEIGHT_LOSS || (start > goal.targetValue)) {
                (start - goal.currentValue).coerceAtLeast(0.0)
            } else {
                (goal.currentValue - start).coerceAtLeast(0.0)
            }

            val progress = if (totalDiff > 0) {
                ((currentDiff / totalDiff) * 100).toInt().coerceIn(0, 100)
            } else 0
            
            binding.miniProgressBar.progress = if (goal.isCompleted) 100 else progress
        }
    }

    class GoalDiffCallback : DiffUtil.ItemCallback<Goal>() {
        override fun areItemsTheSame(oldItem: Goal, newItem: Goal) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Goal, newItem: Goal) = oldItem == newItem
    }
}
