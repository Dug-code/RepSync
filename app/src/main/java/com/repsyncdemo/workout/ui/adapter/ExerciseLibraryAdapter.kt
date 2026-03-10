package com.repsyncdemo.workout.ui.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
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
 * Uses [ListAdapter] with [DiffUtil] for efficient list updates.
 */
class ExerciseLibraryAdapter(
    private val onClick: ((ExerciseDefinition) -> Unit)? = null
) : ListAdapter<ExerciseDefinition, ExerciseLibraryAdapter.ViewHolder>(ExerciseDefDiffCallback()) {

    val selectIds = mutableSetOf<String>()




    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Inflate the item layout using View Binding
        val binding = ItemExerciseLibraryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Bind the exercise data at the given position to the ViewHolder
        holder.bind(getItem(position), selectIds.contains(getItem(position).name))
    }

    /**
     * ViewHolder class that holds and binds the views for an individual exercise item.
     */
    inner class ViewHolder(
        private val binding: ItemExerciseLibraryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Maps the [ExerciseDefinition] properties to the UI components.
         * Also sets up the click listener for the item.
         */
        fun bind(exercise: ExerciseDefinition, isSelected: Boolean) {


            binding.tvExerciseName.text = exercise.name
            binding.tvPrimaryMuscle.text = exercise.primaryBodyPart
            binding.tvSecondaryMuscles.text = exercise.secondaryBodyParts
            binding.tvEquipment.text = exercise.equipment
            binding.tvMovement.text = exercise.movementPattern



            val strokeColor = if (isSelected) "#E31E24".toColorInt() else "#1E1E1E".toColorInt()
            binding.root.setStrokeColor(ColorStateList.valueOf(strokeColor))

            binding.root.setOnClickListener {
                selectIds.add(exercise.name)


                // Notify the adapter that the item has changed to refresh the item UI immediately
                notifyItemChanged(bindingAdapterPosition)

                onClick?.invoke(exercise)
            }
        }
    }

    /**
     * Callback for calculating the diff between two non-null items in a list.
     * Used by ListAdapter to calculate minimum number of changes for the UI.
     */
    class ExerciseDefDiffCallback : DiffUtil.ItemCallback<ExerciseDefinition>() {
        override fun areItemsTheSame(oldItem: ExerciseDefinition, newItem: ExerciseDefinition) =
            oldItem.name == newItem.name

        override fun areContentsTheSame(oldItem: ExerciseDefinition, newItem: ExerciseDefinition) =
            oldItem == newItem
    }
}
