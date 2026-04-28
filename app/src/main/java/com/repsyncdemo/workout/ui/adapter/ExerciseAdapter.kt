package com.repsyncdemo.workout.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.repsyncdemo.workout.data.model.Exercise
import com.repsyncdemo.workout.databinding.ItemExerciseBinding

class ExerciseAdapter : ListAdapter<Exercise, ExerciseAdapter.ViewHolder>(ExerciseDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExerciseBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemExerciseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(exercise: Exercise) {
            binding.tvExerciseName.text = exercise.name
            binding.tvSetsReps.text = "${exercise.sets} sets x ${exercise.reps} reps"
            if (exercise.weight > 0) {
                binding.tvWeight.text = "${exercise.weight.toCleanString()} lbs"
                binding.tvWeight.visibility = View.VISIBLE
            } else {
                binding.tvWeight.visibility = View.GONE
            }
            
            // Show subtle "Custom" label for user-created exercises
            binding.tvCustomLabel.visibility = if (exercise.isCustom) View.VISIBLE else View.GONE
        }

        private fun Double.toCleanString(): String {
            return if (this % 1.0 == 0.0) {
                toInt().toString()
            } else {
                toString()
            }
        }
    }

    class ExerciseDiffCallback : DiffUtil.ItemCallback<Exercise>() {
        override fun areItemsTheSame(oldItem: Exercise, newItem: Exercise) = oldItem.name == newItem.name
        override fun areContentsTheSame(oldItem: Exercise, newItem: Exercise) = oldItem == newItem
    }
}
