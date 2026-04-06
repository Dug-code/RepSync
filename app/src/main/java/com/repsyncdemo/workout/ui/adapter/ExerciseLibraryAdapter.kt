package com.repsyncdemo.workout.ui.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.repsyncdemo.workout.data.model.ExerciseDefinition
import com.repsyncdemo.workout.databinding.ItemExerciseLibraryBinding


/**
 * Adapter for the Exercise Library screen.
 * Displays a list of predefined exercises that the user can pick from.
 */
class ExerciseLibraryAdapter(
    private val onClick: ((ExerciseDefinition) -> Unit)? = null
) : ListAdapter<ExerciseDefinition, ExerciseLibraryAdapter.ViewHolder>(ExerciseDefDiffCallback()) {

    private val selectedNames = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExerciseLibraryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, selectedNames.contains(item.name))
    }

    inner class ViewHolder(
        private val binding: ItemExerciseLibraryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(exercise: ExerciseDefinition, isSelected: Boolean) {
            binding.tvExerciseName.text = exercise.name
            binding.tvEquipment.text = exercise.equipment
            binding.tvMovement.text = exercise.movementPattern

            // Display Muscle Groups (Optional)
            val primary = exercise.primaryBodyPart
            val secondary = exercise.secondaryBodyParts
            if (primary.isNotEmpty()) {
                val text = if (secondary.isNotEmpty() && secondary != "None") "$primary, $secondary" else primary
                binding.tvMuscleGroups.text = text
                binding.tvMuscleGroups.visibility = View.VISIBLE
            } else {
                binding.tvMuscleGroups.visibility = View.GONE
            }

            val strokeColor = if (isSelected) "#E31E24".toColorInt() else "#44474E".toColorInt()
            binding.root.setStrokeColor(ColorStateList.valueOf(strokeColor))

            binding.root.setOnClickListener {
                if (selectedNames.contains(exercise.name)) {
                    selectedNames.remove(exercise.name)
                } else {
                    selectedNames.add(exercise.name)
                }
                notifyItemChanged(bindingAdapterPosition)
                onClick?.invoke(exercise)
            }
        }
    }

    class ExerciseDefDiffCallback : DiffUtil.ItemCallback<ExerciseDefinition>() {
        override fun areItemsTheSame(oldItem: ExerciseDefinition, newItem: ExerciseDefinition) =
            oldItem.name == newItem.name

        override fun areContentsTheSame(oldItem: ExerciseDefinition, newItem: ExerciseDefinition) =
            oldItem == newItem
    }
}
