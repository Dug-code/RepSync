package com.repsyncdemo.workout.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.data.model.ExerciseType
import com.repsyncdemo.workout.data.model.Workout
import com.repsyncdemo.workout.databinding.ItemWorkoutBinding

class WorkoutAdapter(
    private var isReorderable: Boolean = false,
    private val onClick: (Workout) -> Unit
) : ListAdapter<Workout, WorkoutAdapter.ViewHolder>(WorkoutDiffCallback()) {

    fun setReorderable(reorderable: Boolean) {
        if (this.isReorderable != reorderable) {
            this.isReorderable = reorderable
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWorkoutBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemWorkoutBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(workout: Workout) {
            binding.tvName.text = workout.name
            val count = workout.exercises.size
            binding.tvExerciseCount.text = "$count ${if (count == 1) "exercise" else "exercises"}"
            
            // Set privacy icon based on workout status
            if (workout.isPublic) {
                binding.ivPrivacyStatus.setImageResource(R.drawable.ic_public)
                binding.ivPrivacyStatus.alpha = 0.6f
            } else {
                binding.ivPrivacyStatus.setImageResource(R.drawable.ic_private)
                binding.ivPrivacyStatus.alpha = 0.3f
            }

            // Determine which workout type icons to show
            val types = workout.exercises.map { it.type }.distinct()
            
            if (types.size > 1) {
                // Multi-type: Show COMBO label
                binding.ivStrengthIcon.visibility = View.GONE
                binding.ivCardioIcon.visibility = View.GONE
                binding.ivCalisthenicsIcon.visibility = View.GONE
                binding.tvComboLabel.visibility = View.VISIBLE
            } else {
                // Single-type or empty
                binding.tvComboLabel.visibility = View.GONE
                binding.ivStrengthIcon.visibility = if (types.contains(ExerciseType.STRENGTH)) View.VISIBLE else View.GONE
                binding.ivCardioIcon.visibility = if (types.contains(ExerciseType.CARDIO)) View.VISIBLE else View.GONE
                binding.ivCalisthenicsIcon.visibility = if (types.contains(ExerciseType.CALISTHENICS)) View.VISIBLE else View.GONE
            }

            // Show drag handle ONLY if reorderable
            binding.ivDragHandle.visibility = if (isReorderable) View.VISIBLE else View.GONE

            binding.root.setOnClickListener { onClick(workout) }
        }
    }

    class WorkoutDiffCallback : DiffUtil.ItemCallback<Workout>() {
        override fun areItemsTheSame(oldItem: Workout, newItem: Workout) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Workout, newItem: Workout) = oldItem == newItem
    }
}
