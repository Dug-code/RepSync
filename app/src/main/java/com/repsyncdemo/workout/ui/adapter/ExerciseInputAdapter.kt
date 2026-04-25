package com.repsyncdemo.workout.ui.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.RecyclerView
import com.repsyncdemo.workout.data.ExerciseDatabase
import com.repsyncdemo.workout.data.model.Exercise
import com.repsyncdemo.workout.data.model.ExerciseDefinition
import com.repsyncdemo.workout.data.model.ExerciseType
import com.repsyncdemo.workout.databinding.ItemExerciseInputBinding

class ExerciseInputAdapter(
    private val onDataChanged: () -> Unit = {}
) : RecyclerView.Adapter<ExerciseInputAdapter.ViewHolder>() {

    private val items = mutableListOf<ExerciseInputItem>()
    private var allLibraryExercises: List<ExerciseDefinition> = emptyList()

    init {
        setHasStableIds(true)
    }

    fun updateLibrary(exercises: List<ExerciseDefinition>) {
        this.allLibraryExercises = exercises
    }

    private val exerciseNames: List<String> get() = allLibraryExercises.map { it.name }.ifEmpty { ExerciseDatabase.allExercises.map { it.name } }

    data class ExerciseInputItem(
        val stableId: Long = System.nanoTime(),
        var name: String = "",
        var sets: String = "",
        var reps: String = "",
        var weight: String = "",
        var isBodyweight: Boolean = false,
        var type: ExerciseType = ExerciseType.STRENGTH,
        var isCustom: Boolean = false,
        var isExpanded: Boolean = true,
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

    fun addExercise(name: String, type: ExerciseType, primary: String? = null, secondary: String? = null, isCustom: Boolean = false) {
        items.add(ExerciseInputItem(
            name = name, 
            type = type, 
            isCustom = isCustom,
            primaryMuscle = if (primary == "None") null else primary, 
            secondaryMuscle = if (secondary == "None") null else secondary,
            isBodyweight = type == ExerciseType.CALISTHENICS
        ))
        notifyItemInserted(items.size - 1)
        onDataChanged()
    }

    fun addExerciseFromLibrary(name: String) {
        val exerciseDef = allLibraryExercises.find { it.name.equals(name, ignoreCase = true) } 
            ?: ExerciseDatabase.getExerciseByName(name)
            
        val type = exerciseDef?.type ?: ExerciseType.STRENGTH
        val isBW = type == ExerciseType.CALISTHENICS
        items.add(ExerciseInputItem(
            name = name, 
            isBodyweight = isBW, 
            type = type,
            isCustom = exerciseDef?.isCustom ?: false,
            primaryMuscle = exerciseDef?.primaryBodyPart,
            secondaryMuscle = exerciseDef?.secondaryBodyParts
        ))
        notifyItemInserted(items.size - 1)
        onDataChanged()
    }

    fun addExerciseFromObject(exercise: Exercise) {
        items.add(ExerciseInputItem(
            name = exercise.name,
            sets = exercise.sets.toString(),
            reps = exercise.reps.toString(),
            weight = exercise.weight.toString(),
            isBodyweight = exercise.isBodyWeight,
            type = exercise.type,
            isCustom = exercise.isCustom,
            primaryMuscle = exercise.primaryMuscleGroup,
            secondaryMuscle = exercise.secondaryMuscleGroup,
            isExpanded = false // Collapse existing exercises by default
        ))
        notifyItemInserted(items.size - 1)
    }

    fun clearItems() {
        items.clear()
        notifyDataSetChanged()
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
                secondaryMuscleGroup = item.secondaryMuscle,
                isCustom = item.isCustom
            )
        }
    }

    override fun getItemId(position: Int): Long = items[position].stableId

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
        if (fromPosition < items.size && toPosition < items.size) {
            val movedItem = items.removeAt(fromPosition)
            items.add(toPosition, movedItem)
            notifyItemMoved(fromPosition, toPosition)
        }
    }

    inner class ViewHolder(
        private val binding: ItemExerciseInputBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var nameWatcher: TextWatcher? = null
        private var setsWatcher: TextWatcher? = null
        private var repsWatcher: TextWatcher? = null
        private var weightWatcher: TextWatcher? = null
        private var timeWatcher: TextWatcher? = null
        private var distWatcher: TextWatcher? = null
        private var floorsWatcher: TextWatcher? = null

        fun bind(item: ExerciseInputItem) {
            val context = binding.root.context
            
            val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, exerciseNames)
            binding.etExerciseName.setAdapter(adapter)
            
            binding.etExerciseName.removeTextChangedListener(nameWatcher)
            binding.etSets.removeTextChangedListener(setsWatcher)
            binding.etReps.removeTextChangedListener(repsWatcher)
            binding.etWeight.removeTextChangedListener(weightWatcher)
            binding.etCardioTime.removeTextChangedListener(timeWatcher)
            binding.etCardioDist.removeTextChangedListener(distWatcher)
            binding.etCardioFloors.removeTextChangedListener(floorsWatcher)

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
            updateExpandedState(item)

            binding.ivExpandIcon.setOnClickListener {
                item.isExpanded = !item.isExpanded
                updateExpandedState(item)
            }

            binding.etExerciseName.setOnItemClickListener { _, _, _, _ ->
                val selectedName = binding.etExerciseName.text.toString()
                val exerciseDef = allLibraryExercises.find { it.name.equals(selectedName, ignoreCase = true) }
                    ?: ExerciseDatabase.getExerciseByName(selectedName)
                
                if (exerciseDef != null) {
                    item.name = exerciseDef.name
                    item.type = exerciseDef.type
                    item.isBodyweight = item.type == ExerciseType.CALISTHENICS
                    item.isCustom = exerciseDef.isCustom
                    item.primaryMuscle = exerciseDef.primaryBodyPart
                    item.secondaryMuscle = exerciseDef.secondaryBodyParts
                }
                updateUiForType(item)
                updateMuscleGroupDisplay(item)
                onDataChanged()
            }

            binding.etExerciseName.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val currentText = binding.etExerciseName.text.toString().trim()
                    val match = allLibraryExercises.find { it.name.equals(currentText, ignoreCase = true) }
                        ?: ExerciseDatabase.getExerciseByName(currentText)
                    
                    if (match != null) {
                        binding.etExerciseName.setText(match.name)
                        item.name = match.name
                        item.type = match.type
                        item.isBodyweight = item.type == ExerciseType.CALISTHENICS
                        item.isCustom = match.isCustom
                        item.primaryMuscle = match.primaryBodyPart
                        item.secondaryMuscle = match.secondaryBodyParts
                        updateUiForType(item)
                        updateMuscleGroupDisplay(item)
                    } else if (item.name.isEmpty()) {
                        binding.etExerciseName.setText("")
                    } else if (!currentText.equals(item.name, ignoreCase = true)) {
                        binding.etExerciseName.setText(item.name)
                    }
                    onDataChanged()
                }
            }

            nameWatcher = createWatcher { item.name = it }
            setsWatcher = createWatcher { item.sets = it }
            repsWatcher = createWatcher { item.reps = it }
            weightWatcher = createWatcher { item.weight = it }
            timeWatcher = createWatcher { item.cardioTime = it }
            distWatcher = createWatcher { item.cardioDist = it }
            floorsWatcher = createWatcher { item.cardioFloors = it }

            binding.etSets.addTextChangedListener(setsWatcher)
            binding.etReps.addTextChangedListener(repsWatcher)
            binding.etWeight.addTextChangedListener(weightWatcher)
            binding.etCardioTime.addTextChangedListener(timeWatcher)
            binding.etCardioDist.addTextChangedListener(distWatcher)
            binding.etCardioFloors.addTextChangedListener(floorsWatcher)
            
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

        private fun updateExpandedState(item: ExerciseInputItem) {
            binding.layoutCollapsibleContent.visibility = if (item.isExpanded) View.VISIBLE else View.GONE
            binding.ivExpandIcon.rotation = if (item.isExpanded) 180f else 0f
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
            binding.tvCustomLabel.visibility = if (item.isCustom) View.VISIBLE else View.GONE
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

        private fun createWatcher(onChanged: (String) -> Unit) = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                onChanged(s.toString())
                onDataChanged()
            }
        }
    }
}
