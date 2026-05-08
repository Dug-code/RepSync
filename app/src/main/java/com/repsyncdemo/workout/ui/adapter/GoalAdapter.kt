package com.repsyncdemo.workout.ui.adapter

/**
 * File overview: Binds full goal cards with progress display, privacy state, update actions, and options menu.
 */

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.data.model.Goal
import com.repsyncdemo.workout.data.model.GoalType
import com.repsyncdemo.workout.databinding.ItemGoalBinding
import kotlin.math.abs

class GoalAdapter(
    private val isMyProfile: Boolean,
    private val onUpdateProgress: (Goal) -> Unit,
    private val onEdit: (Goal) -> Unit,
    private val onTogglePrivacy: (Goal) -> Unit,
    private val onDelete: (Goal) -> Unit
) : ListAdapter<Goal, GoalAdapter.ViewHolder>(GoalDiffCallback()) {

    // Creates the item row.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGoalBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    // Shows the item row.
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemGoalBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        // Fills this row with data.
        fun bind(goal: Goal) {
            binding.tvGoalTitle.text = goal.title

            binding.tvGoalType.text = when (goal.type) {
                GoalType.PR -> "PR"
                GoalType.WEIGHT_LOSS -> "Loss"
                GoalType.WEIGHT_GAIN -> "Gain"
                GoalType.STREAK -> "Streak"
            }

            // Set dynamic goal icon
            val iconRes = when (goal.type) {
                GoalType.WEIGHT_LOSS, GoalType.WEIGHT_GAIN -> R.drawable.ic_scale
                GoalType.PR -> R.drawable.ic_medal
                else -> R.drawable.ic_checkered_flag
            }
            binding.ivGoalIcon.setImageResource(iconRes)

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

            // Privacy Icon
            if (goal.isPublic) {
                binding.ivPrivacyStatus.setImageResource(R.drawable.ic_public)
                binding.ivPrivacyStatus.alpha = 0.4f
            } else {
                binding.ivPrivacyStatus.setImageResource(R.drawable.ic_private)
                binding.ivPrivacyStatus.alpha = 0.2f
            }

            // Setup Options Menu (Pencil Icon)
            if (isMyProfile) {
                binding.root.visibility = View.VISIBLE
                binding.root.layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                binding.btnUpdateProgress.visibility = if (goal.isCompleted) View.GONE else View.VISIBLE
                binding.btnUpdateProgress.setOnClickListener { view ->
                    showOptionsPopup(view, goal)
                }
            } else {
                binding.btnUpdateProgress.visibility = View.GONE
                // Only show public goals if not my profile
                if (!goal.isPublic) {
                    binding.root.visibility = View.GONE
                    binding.root.layoutParams = RecyclerView.LayoutParams(0, 0)
                } else {
                    binding.root.visibility = View.VISIBLE
                    binding.root.layoutParams = RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
            }

            // Hide the redundant delete button
            binding.btnDeleteGoal.visibility = View.GONE

            if (goal.isCompleted) {
                binding.progressBar.progress = 100
                binding.tvGoalType.text = "Done"
            }
        }

        // Shows a dialog or popup.
        private fun showOptionsPopup(view: View, goal: Goal) {
            val popup = PopupMenu(view.context, view)
            popup.menu.add("Log Progress")
            popup.menu.add("Rename Goal")
            popup.menu.add(if (goal.isPublic) "Make Private" else "Make Public")
            popup.menu.add("Delete Goal")
            
            popup.setOnMenuItemClickListener { item ->
                when (item.title) {
                    "Log Progress" -> onUpdateProgress(goal)
                    "Rename Goal" -> onEdit(goal)
                    "Make Private", "Make Public" -> onTogglePrivacy(goal)
                    "Delete Goal" -> {
                        androidx.appcompat.app.AlertDialog.Builder(view.context, R.style.ThemeOverlay_App_MaterialAlertDialog)
                            .setTitle("Delete Goal")
                            .setMessage("Are you sure you want to delete \"${goal.title}\"?")
                            .setPositiveButton("Delete") { _, _ -> onDelete(goal) }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                }
                true
            }
            popup.show()
        }
    }

    class GoalDiffCallback : DiffUtil.ItemCallback<Goal>() {
        override fun areItemsTheSame(oldItem: Goal, newItem: Goal) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Goal, newItem: Goal) = oldItem == newItem
    }
}
