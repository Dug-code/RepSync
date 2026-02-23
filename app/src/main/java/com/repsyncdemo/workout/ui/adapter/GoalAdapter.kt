package com.repsyncdemo.workout.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.repsyncdemo.workout.data.model.Goal
import com.repsyncdemo.workout.data.model.GoalType
import com.repsyncdemo.workout.databinding.ItemGoalBinding
import kotlin.math.abs

class GoalAdapter(
    private val isMyProfile: Boolean,
    private val onUpdateProgress: (Goal) -> Unit,
    private val onDelete: (Goal) -> Unit
) : ListAdapter<Goal, GoalAdapter.ViewHolder>(GoalDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGoalBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemGoalBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(goal: Goal) {
            binding.tvGoalTitle.text = goal.title
            binding.tvGoalDescription.text = goal.description

            binding.tvGoalType.text = when (goal.type) {
                GoalType.PR -> "PR"
                GoalType.WEIGHT_LOSS -> "Weight Loss"
                GoalType.WEIGHT_GAIN -> "Weight Gain"
                GoalType.STREAK -> "Streak"
            }

            // Handle legacy data where startingValue might be 0
            val start = if (goal.startingValue == 0.0) goal.currentValue else goal.startingValue
            
            val totalDiff = abs(goal.targetValue - start)
            val currentDiff = if (goal.type == GoalType.WEIGHT_LOSS || (start > goal.targetValue)) {
                (start - goal.currentValue).coerceAtLeast(0.0)
            } else {
                (goal.currentValue - start).coerceAtLeast(0.0)
            }

            val progress = if (totalDiff > 0) {
                ((currentDiff / totalDiff) * 100).toInt().coerceIn(0, 100)
            } else 0

            binding.progressBar.progress = progress
            
            val change = goal.currentValue - start
            val changeText = if (change >= 0) "+${change.toInt()}" else "${change.toInt()}"
            
            binding.tvCurrentValue.text = "${goal.currentValue.toInt()} ${goal.unit} ($changeText)"
            binding.tvTargetValue.text = "${start.toInt()} -> ${goal.targetValue.toInt()} ${goal.unit}"

            if (isMyProfile) {
                binding.btnUpdateProgress.visibility = View.VISIBLE
                binding.btnDeleteGoal.visibility = View.VISIBLE
                if (goal.isCompleted) {
                    binding.progressBar.progress = 100
                    binding.btnUpdateProgress.isEnabled = false
                    binding.btnUpdateProgress.text = "Completed"
                } else {
                    binding.btnUpdateProgress.isEnabled = true
                    binding.btnUpdateProgress.text = "Update"
                }
            } else {
                binding.btnUpdateProgress.visibility = View.GONE
                binding.btnDeleteGoal.visibility = View.GONE
                if (goal.isCompleted) {
                    binding.progressBar.progress = 100
                }
            }

            binding.btnUpdateProgress.setOnClickListener { onUpdateProgress(goal) }
            binding.btnDeleteGoal.setOnClickListener { onDelete(goal) }
        }
    }

    class GoalDiffCallback : DiffUtil.ItemCallback<Goal>() {
        override fun areItemsTheSame(oldItem: Goal, newItem: Goal) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Goal, newItem: Goal) = oldItem == newItem
    }
}
