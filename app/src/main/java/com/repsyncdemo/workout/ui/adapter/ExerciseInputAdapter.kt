package com.repsyncdemo.workout.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.RecyclerView
import com.repsyncdemo.workout.data.ExerciseDatabase
import com.repsyncdemo.workout.data.model.Exercise
import com.repsyncdemo.workout.databinding.ItemExerciseInputBinding

class ExerciseInputAdapter : RecyclerView.Adapter<ExerciseInputAdapter.ViewHolder>() {

    private val items = mutableListOf<ExerciseInputItem>()
    private val exerciseNames = ExerciseDatabase.allExercises.map { it.name }

    data class ExerciseInputItem(
        var name: String = "",
        var sets: String = "",
        var reps: String = "",
        var weight: String = "",
        var isBodyweight: Boolean = false
    )

    fun addExercise() {
        items.add(ExerciseInputItem())
        notifyItemInserted(items.size - 1)
    }

    fun addExerciseFromLibrary(name: String) {
        val exerciseDef = ExerciseDatabase.getExerciseByName(name)
        val isBW = exerciseDef?.equipment?.contains("Bodyweight", ignoreCase = true) == true
        items.add(ExerciseInputItem(name = name, isBodyweight = isBW))
        notifyItemInserted(items.size - 1)
    }

    fun getExercises(): List<Exercise> {
        return items.map { item ->
            Exercise(
                name = item.name,
                sets = item.sets.toIntOrNull() ?: 0,
                reps = item.reps.toIntOrNull() ?: 0,
                weight = if (item.isBodyweight) 0.0 else (item.weight.toDoubleOrNull() ?: 0.0),
                isBodyWeight = item.isBodyweight
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExerciseInputBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(
        private val binding: ItemExerciseInputBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ExerciseInputItem, position: Int) {
            val context = binding.root.context
            
            // Set up Autocomplete
            val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, exerciseNames)
            binding.etExerciseName.setAdapter(adapter)
            
            binding.etExerciseName.setText(item.name)
            binding.etSets.setText(item.sets)
            binding.etReps.setText(item.reps)
            binding.etWeight.setText(item.weight)
            binding.cbBodyweight.isChecked = item.isBodyweight
            
            binding.tilWeight.visibility = if (item.isBodyweight) View.GONE else View.VISIBLE

            binding.etExerciseName.setOnItemClickListener { _, _, _, _ ->
                val selectedName = binding.etExerciseName.text.toString()
                item.name = selectedName
                
                // Auto-check bodyweight if applicable
                val exerciseDef = ExerciseDatabase.getExerciseByName(selectedName)
                if (exerciseDef?.equipment?.contains("Bodyweight", ignoreCase = true) == true) {
                    item.isBodyweight = true
                    binding.cbBodyweight.isChecked = true
                    binding.tilWeight.visibility = View.GONE
                }
            }

            binding.etExerciseName.setOnFocusChangeListener { _, _ ->
                item.name = binding.etExerciseName.text.toString()
            }
            binding.etSets.setOnFocusChangeListener { _, _ ->
                item.sets = binding.etSets.text.toString()
            }
            binding.etReps.setOnFocusChangeListener { _, _ ->
                item.reps = binding.etReps.text.toString()
            }
            binding.etWeight.setOnFocusChangeListener { _, _ ->
                item.weight = binding.etWeight.text.toString()
            }
            
            binding.cbBodyweight.setOnCheckedChangeListener { _, isChecked ->
                item.isBodyweight = isChecked
                binding.tilWeight.visibility = if (isChecked) View.GONE else View.VISIBLE
            }

            binding.btnRemove.setOnClickListener {
                if (items.size > 1) {
                    items.removeAt(position)
                    notifyDataSetChanged()
                }
            }
        }
    }
}
