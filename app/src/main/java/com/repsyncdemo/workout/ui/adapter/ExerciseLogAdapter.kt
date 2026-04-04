package com.repsyncdemo.workout.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.data.model.Exercise
import com.repsyncdemo.workout.data.model.ExerciseLog
import com.repsyncdemo.workout.data.model.SetLog
import com.repsyncdemo.workout.databinding.ItemExerciseLogBinding

class ExerciseLogAdapter(
    private val onDataChanged: () -> Unit = {}
) : RecyclerView.Adapter<ExerciseLogAdapter.ViewHolder>() {

    private val exercises = mutableListOf<String>()
    private val setData = mutableMapOf<Int, MutableList<SetLogData>>()

    data class SetLogData(
        var reps: String = "",
        var weight: String = "",
        var completed: Boolean = false
    )

    fun setExercises(list: List<Exercise>) {
        exercises.clear()
        setData.clear()
        list.forEachIndexed { index, exercise ->
            exercises.add(exercise.name)
            setData[index] = MutableList(exercise.sets) {
                SetLogData(reps = exercise.reps.toString(), weight = exercise.weight.toString())
            }
        }
        notifyDataSetChanged()
    }

    fun setExerciseLogs(list: List<ExerciseLog>) {
        exercises.clear()
        setData.clear()
        list.forEachIndexed { index, log ->
            exercises.add(log.exerciseName)
            setData[index] = log.sets.map { 
                SetLogData(reps = it.reps.toString(), weight = it.weight.toString(), completed = it.completed)
            }.toMutableList()
        }
        notifyDataSetChanged()
    }

    fun addExercise(exerciseName: String) {
        exercises.add(exerciseName)
        setData[exercises.size - 1] = mutableListOf(SetLogData())
        notifyItemInserted(exercises.size - 1)
        onDataChanged()
    }

    fun getExerciseLogs(): List<ExerciseLog> {
        return exercises.mapIndexed { index, name ->
            ExerciseLog(
                exerciseName = name,
                sets = setData[index]?.map { data ->
                    SetLog(
                        reps = data.reps.toIntOrNull() ?: 0,
                        weight = data.weight.toDoubleOrNull() ?: 0.0,
                        completed = data.completed
                    )
                } ?: emptyList()
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExerciseLogBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(exercises[position], position)
    }

    override fun getItemCount() = exercises.size

    inner class ViewHolder(
        private val binding: ItemExerciseLogBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(exerciseName: String, exerciseIndex: Int) {
            binding.tvExerciseName.text = exerciseName
            updateSets(exerciseIndex)

            binding.btnAddSet.setOnClickListener {
                setData[exerciseIndex]?.add(SetLogData())
                updateSets(exerciseIndex)
                onDataChanged()
            }
        }

        private fun updateSets(exerciseIndex: Int) {
            binding.layoutSets.removeAllViews()
            val sets = setData[exerciseIndex] ?: return

            sets.forEachIndexed { setIndex, data ->
                val setView = LayoutInflater.from(binding.root.context)
                    .inflate(R.layout.item_set_log, binding.layoutSets, false)

                val tvLabel = setView.findViewById<TextView>(R.id.tvSetLabel)
                val etReps = setView.findViewById<EditText>(R.id.etReps)
                val etWeight = setView.findViewById<EditText>(R.id.etWeight)
                val cbCompleted = setView.findViewById<CheckBox>(R.id.cbCompleted)
                val btnRemoveSet = setView.findViewById<ImageButton>(R.id.btnRemoveSet)

                tvLabel.text = "Set ${setIndex + 1}"
                etReps.setText(data.reps)
                etWeight.setText(data.weight)
                cbCompleted.isChecked = data.completed

                etReps.addTextChangedListener(object : android.text.TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: android.text.Editable?) {
                        data.reps = s.toString()
                        onDataChanged()
                    }
                })

                etWeight.addTextChangedListener(object : android.text.TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: android.text.Editable?) {
                        data.weight = s.toString()
                        onDataChanged()
                    }
                })

                cbCompleted.setOnCheckedChangeListener { _, isChecked ->
                    data.completed = isChecked
                    onDataChanged()
                }

                btnRemoveSet.setOnClickListener {
                    sets.removeAt(setIndex)
                    updateSets(exerciseIndex)
                    onDataChanged()
                }

                binding.layoutSets.addView(setView)
            }
        }
    }
}
