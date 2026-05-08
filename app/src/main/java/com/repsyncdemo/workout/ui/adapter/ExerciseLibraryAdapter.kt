package com.repsyncdemo.workout.ui.adapter

/**
 * File overview: Binds exerciselibrary data into RecyclerView rows and forwards user actions to the owning screen.
 */

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.data.model.ExerciseDefinition
import com.repsyncdemo.workout.data.model.ExerciseType
import com.repsyncdemo.workout.databinding.ItemExerciseLibraryBinding


/**
 * Adapter for the Exercise Library screen.
 * Displays a list of predefined exercises that the user can pick from.
 */
class ExerciseLibraryAdapter(
    private val onDeleteCustom: ((String) -> Unit)? = null,
    private val onClick: ((ExerciseDefinition) -> Unit)? = null
) : ListAdapter<ExerciseDefinition, ExerciseLibraryAdapter.ViewHolder>(ExerciseDefDiffCallback()) {

    // Creates the item row.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExerciseLibraryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    // Shows the item row.
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        // Note: Selection state was simplified out for better UX in full-screen mode
        holder.bind(item, false)
    }

    inner class ViewHolder(
        private val binding: ItemExerciseLibraryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        // Fills this row with data.
        fun bind(exercise: ExerciseDefinition, isSelected: Boolean) {
            binding.tvExerciseName.text = exercise.name
            
            val typeText = when(exercise.type) {
                ExerciseType.STRENGTH -> "Weight Lifting"
                ExerciseType.CARDIO -> "Cardio"
                ExerciseType.CALISTHENICS -> "Calisthenics"
            }

            // Tag 1: Primary Muscle Group
            if (exercise.primaryBodyPart.isNotEmpty() && exercise.primaryBodyPart != typeText) {
                binding.tvPrimaryMuscle.text = exercise.primaryBodyPart
                binding.tvPrimaryMuscle.visibility = View.VISIBLE
            } else {
                binding.tvPrimaryMuscle.visibility = View.GONE
            }

            // Tag 2: Exercise Type
            binding.tvMovement.text = typeText
            binding.tvMovement.visibility = View.VISIBLE

            // Custom Identifier (Star & Tag)
            if (exercise.isCustom) {
                binding.ivCustomStar.visibility = View.VISIBLE
                binding.tvEquipment.text = "Custom"
                binding.tvEquipment.visibility = View.VISIBLE
                binding.tvEquipment.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.primary))
            } else {
                binding.ivCustomStar.visibility = View.GONE
                binding.tvEquipment.visibility = View.GONE
            }

            // Optional: Show secondary muscles in the subtle footer text
            val secondary = exercise.secondaryBodyParts
            if (secondary.isNotEmpty() && secondary != "None") {
                binding.tvMuscleGroups.text = "Focus: $secondary"
                binding.tvMuscleGroups.visibility = View.VISIBLE
            } else {
                binding.tvMuscleGroups.visibility = View.GONE
            }

            binding.root.setOnClickListener {
                onClick?.invoke(exercise)
            }

            binding.root.setOnLongClickListener {
                if (exercise.isCustom && onDeleteCustom != null) {
                    showDeleteConfirmation(exercise)
                    true
                } else false
            }
        }

        // Shows a dialog or popup.
        private fun showDeleteConfirmation(exercise: ExerciseDefinition) {
            AlertDialog.Builder(itemView.context, R.style.ThemeOverlay_App_MaterialAlertDialog)
                .setTitle("Delete Custom Exercise")
                .setMessage("Are you sure you want to delete \"${exercise.name}\" from your library?")
                .setPositiveButton("Delete") { _, _ ->
                    // Use firebaseId which is the actual Firestore document ID
                    onDeleteCustom?.invoke(exercise.firebaseId)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    class ExerciseDefDiffCallback : DiffUtil.ItemCallback<ExerciseDefinition>() {
        override fun areItemsTheSame(oldItem: ExerciseDefinition, newItem: ExerciseDefinition) =
            oldItem.firebaseId == newItem.firebaseId

        override fun areContentsTheSame(oldItem: ExerciseDefinition, newItem: ExerciseDefinition) =
            oldItem == newItem
    }
}
