package com.repsyncdemo.workout.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.repsyncdemo.workout.data.model.ExerciseDefinition
import com.repsyncdemo.workout.databinding.ItemExerciseLibraryBinding

class ExerciseLibraryAdapter(
    private val onClick: ((ExerciseDefinition) -> Unit)? = null
) : ListAdapter<ExerciseDefinition, ExerciseLibraryAdapter.ViewHolder>(ExerciseDefDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExerciseLibraryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemExerciseLibraryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(exercise: ExerciseDefinition) {
            binding.tvExerciseName.text = exercise.name
            binding.tvPrimaryMuscle.text = exercise.primaryBodyPart
            binding.tvSecondaryMuscles.text = exercise.secondaryBodyParts
            binding.tvEquipment.text = exercise.equipment
            binding.tvMovement.text = exercise.movementPattern

            onClick?.let { click ->
                binding.root.setOnClickListener { click(exercise) }
            }
        }
    }

    class ExerciseDefDiffCallback : DiffUtil.ItemCallback<ExerciseDefinition>() {
        override fun areItemsTheSame(oldItem: ExerciseDefinition, newItem: ExerciseDefinition) = oldItem.name == newItem.name
        override fun areContentsTheSame(oldItem: ExerciseDefinition, newItem: ExerciseDefinition) = oldItem == newItem
    }
}
