package com.repsyncdemo.workout.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.RecyclerView
import com.repsyncdemo.workout.data.ExerciseDatabase
import com.repsyncdemo.workout.data.model.Exercise
import com.repsyncdemo.workout.data.model.ExerciseType
import com.repsyncdemo.workout.databinding.ItemExerciseInputBinding

class ExerciseInputAdapter(
    private val onDataChanged: () -> Unit = {}
) : RecyclerView.Adapter<ExerciseInputAdapter.ViewHolder>() {

    private val items = mutableListOf<ExerciseInputItem>()
    private val exerciseNames = ExerciseDatabase.allExercises.map { it.name }

    data class ExerciseInputItem(
        var name: String = "",
        var sets: String = "",
        var reps: String = "",
        var weight: String = "",
        var isBodyweight: Boolean = false,
        var type: ExerciseType = ExerciseType.STRENGTH,
        var cardioTime: String = "",
        var cardioDist: String = "",
        var cardioFloors: String = "",
        var primaryMuscle: String? = null,
        var secondaryMuscle: String? = null
    )

    fun addExercise() {
        items.add(ExerciseInputItem())
        notifyItemInserted(items.size - 1)
        onDataChanged()
    }

    fun addExercise(name: String, type: ExerciseType, primary: String? = null, secondary: String? = null) {
        items.add(ExerciseInputItem(
            name = name, 
            type = type, 
            primaryMuscle = if (primary == "None") null else primary, 
            secondaryMuscle = if (secondary == "None") null else secondary
        ))
        notifyItemInserted(items.size - 1)
        onDataChanged()
    }

    fun addExerciseFromLibrary(name: String) {
        val exerciseDef = ExerciseDatabase.getExerciseByName(name)
        val isBW = exerciseDef?.equipment?.contains("Bodyweight", ignoreCase = true) == true
        val type = exerciseDef?.type ?: ExerciseType.STRENGTH
        items.add(ExerciseInputItem(
            name = name, 
            isBodyweight = isBW, 
            type = type,
            primaryMuscle = exerciseDef?.primaryBodyPart,
            secondaryMuscle = exerciseDef?.secondaryBodyParts
        ))
        notifyItemInserted(items.size - 1)
        onDataChanged()
    }

    fun getExercises(): List<Exercise> {
        return items.map { item ->
            Exercise(
                name = item.name,
                sets = item.sets.toIntOrNull() ?: 0,
                reps = item.reps.toIntOrNull() ?: 0,
                weight = if (item.isBodyweight) 0.0 else (item.weight.toDoubleOrNull() ?: 0.0),
                isBodyWeight = item.isBodyweight,
                type = item.type,
                primaryMuscleGroup = item.primaryMuscle,
                secondaryMuscleGroup = item.secondaryMuscle
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
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    fun moveExercise(fromPosition: Int, toPosition: Int) {
        val movedItem = items.removeAt(fromPosition)
        items.add(toPosition, movedItem)
        notifyItemMoved(fromPosition, toPosition)
        onDataChanged()
    }

    inner class ViewHolder(
        private val binding: ItemExerciseInputBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ExerciseInputItem) {
            val context = binding.root.context
            
            val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, exerciseNames)
            binding.etExerciseName.setAdapter(adapter)
            
            binding.etExerciseName.setText(item.name)
            binding.etSets.setText(item.sets)
            binding.etReps.setText(item.reps)
            binding.etWeight.setText(item.weight)
            binding.cbBodyweight.isChecked = item.isBodyweight
            
            binding.etCardioTime.setText(item.cardioTime)
            binding.etCardioDist.setText(item.cardioDist)
            binding.etCardioFloors.setText(item.cardioFloors)

            updateUiForType(item)
            updateMuscleGroupDisplay(item)

            binding.etExerciseName.setOnItemClickListener { _, _, _, _ ->
                val selectedName = binding.etExerciseName.text.toString()
                item.name = selectedName
                
                val exerciseDef = ExerciseDatabase.getExerciseByName(selectedName)
                item.type = exerciseDef?.type ?: ExerciseType.STRENGTH
                item.isBodyweight = exerciseDef?.equipment?.contains("Bodyweight", ignoreCase = true) == true
                item.primaryMuscle = exerciseDef?.primaryBodyPart
                item.secondaryMuscle = exerciseDef?.secondaryBodyParts
                
                updateUiForType(item)
                updateMuscleGroupDisplay(item)
                onDataChanged()
            }

            binding.etExerciseName.addTextChangedListener(createWatcher { item.name = it })
            binding.etSets.addTextChangedListener(createWatcher { item.sets = it })
            binding.etReps.addTextChangedListener(createWatcher { item.reps = it })
            binding.etWeight.addTextChangedListener(createWatcher { item.weight = it })
            binding.etCardioTime.addTextChangedListener(createWatcher { item.cardioTime = it })
            binding.etCardioDist.addTextChangedListener(createWatcher { item.cardioDist = it })
            binding.etCardioFloors.addTextChangedListener(createWatcher { item.cardioFloors = it })
            
            binding.cbBodyweight.setOnCheckedChangeListener { _, isChecked ->
                item.isBodyweight = isChecked
                binding.tilWeight.visibility = if (isChecked) View.GONE else View.VISIBLE
                onDataChanged()
            }

            binding.btnRemove.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    items.removeAt(pos)
                    notifyItemRemoved(pos)
                    onDataChanged()
                }
            }
        }

        private fun updateMuscleGroupDisplay(item: ExerciseInputItem) {
            val primary = item.primaryMuscle
            val secondary = item.secondaryMuscle
            
            if (!primary.isNullOrEmpty()) {
                val text = if (!secondary.isNullOrEmpty()) "$primary, $secondary" else primary
                binding.tvMuscleGroups.text = text
                binding.tvMuscleGroups.visibility = View.VISIBLE
            } else {
                binding.tvMuscleGroups.visibility = View.GONE
            }
        }

        private fun updateUiForType(item: ExerciseInputItem) {
            if (item.type == ExerciseType.CARDIO) {
                binding.layoutStrengthInputs.visibility = View.GONE
                binding.layoutCardioInputs.visibility = View.VISIBLE
                
                val isStepMachine = item.name.lowercase().contains("stair") || item.name.lowercase().contains("step")
                binding.tilFloors.visibility = if (isStepMachine) View.VISIBLE else View.GONE
            } else {
                binding.layoutStrengthInputs.visibility = View.VISIBLE
                binding.layoutCardioInputs.visibility = View.GONE
                binding.tilWeight.visibility = if (item.isBodyweight) View.GONE else View.VISIBLE
                binding.cbBodyweight.isChecked = item.isBodyweight
            }
        }

        private fun createWatcher(onChanged: (String) -> Unit) = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                onChanged(s.toString())
                onDataChanged()
            }
        }
    }
}
