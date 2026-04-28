package com.repsyncdemo.workout.ui.adapter

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.data.model.Exercise
import com.repsyncdemo.workout.data.model.ExerciseDefinition
import com.repsyncdemo.workout.data.model.ExerciseLog
import com.repsyncdemo.workout.data.model.ExerciseType
import com.repsyncdemo.workout.data.model.SetLog
import com.repsyncdemo.workout.databinding.ItemExerciseLogBinding
import java.util.Locale

class ExerciseLogAdapter(
    private val onDataChanged: () -> Unit = {}
) : RecyclerView.Adapter<ExerciseLogAdapter.ViewHolder>() {

    private val exercises = mutableListOf<ExerciseInfo>()
    private val setData = mutableMapOf<Int, MutableList<SetLogData>>()

    data class ExerciseInfo(val name: String, val type: ExerciseType)

    data class SetLogData(
        var reps: String = "",
        var weight: String = "",
        var duration: Int = 0,
        var distance: String = "",
        var floors: String = "",
        var completed: Boolean = false,
        var isTimerRunning: Boolean = false
    )

    fun setExercises(list: List<Exercise>) {
        exercises.clear()
        setData.clear()
        list.forEachIndexed { index, exercise ->
            exercises.add(ExerciseInfo(exercise.name, exercise.type))
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
            exercises.add(ExerciseInfo(log.exerciseName, log.type))
            setData[index] = log.sets.map { 
                SetLogData(
                    reps = it.reps?.toString() ?: "",
                    weight = it.weight?.toString() ?: "",
                    duration = it.durationSeconds ?: 0,
                    distance = it.distance?.toString() ?: "",
                    floors = it.floors?.toString() ?: "",
                    completed = it.completed
                )
            }.toMutableList()
        }
        notifyDataSetChanged()
    }

    fun addExercise(exerciseName: String, type: ExerciseType = ExerciseType.STRENGTH) {
        exercises.add(ExerciseInfo(exerciseName, type))
        setData[exercises.size - 1] = mutableListOf(SetLogData())
        notifyItemInserted(exercises.size - 1)
        onDataChanged()
    }

    fun addExercise(exercise: ExerciseDefinition) {
        addExercise(exercise.name, exercise.type)
    }

    fun getExerciseLogs(): List<ExerciseLog> {
        return exercises.mapIndexed { index, info ->
            ExerciseLog(
                exerciseName = info.name,
                type = info.type,
                sets = setData[index]?.map { data ->
                    SetLog(
                        reps = data.reps.toIntOrNull(),
                        weight = data.weight.toDoubleOrNull(),
                        durationSeconds = if (data.duration > 0) data.duration else null,
                        distance = data.distance.toDoubleOrNull(),
                        floors = data.floors.toIntOrNull(),
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

        private val handler = Handler(Looper.getMainLooper())

        fun bind(info: ExerciseInfo, exerciseIndex: Int) {
            binding.tvExerciseName.text = info.name
            updateSets(exerciseIndex, info)

            binding.btnAddSet.setOnClickListener {
                setData[exerciseIndex]?.add(SetLogData())
                updateSets(exerciseIndex, info)
                onDataChanged()
            }
        }

        private fun updateSets(exerciseIndex: Int, info: ExerciseInfo) {
            binding.layoutSets.removeAllViews()
            val sets = setData[exerciseIndex] ?: return

            sets.forEachIndexed { setIndex, data ->
                val setView = LayoutInflater.from(binding.root.context)
                    .inflate(R.layout.item_set_log, binding.layoutSets, false)

                val tvSetLabel = setView.findViewById<TextView>(R.id.tvSetLabel)
                val layoutStrength = setView.findViewById<LinearLayout>(R.id.layoutStrengthInputs)
                val layoutCardio = setView.findViewById<LinearLayout>(R.id.layoutCardioInputs)
                val cbCompleted = setView.findViewById<CheckBox>(R.id.cbCompleted)
                val btnRemoveSet = setView.findViewById<ImageButton>(R.id.btnRemoveSet)

                tvSetLabel.text = "Set ${setIndex + 1}"
                cbCompleted.isChecked = data.completed

                if (info.type == ExerciseType.CARDIO) {
                    layoutStrength.visibility = View.GONE
                    layoutCardio.visibility = View.VISIBLE
                    setupCardioSet(setView, data, info.name)
                } else {
                    layoutStrength.visibility = View.VISIBLE
                    layoutCardio.visibility = View.GONE
                    setupStrengthSet(setView, data)
                }

                cbCompleted.setOnCheckedChangeListener { _, isChecked ->
                    data.completed = isChecked
                    onDataChanged()
                }

                btnRemoveSet.setOnClickListener {
                    sets.removeAt(setIndex)
                    updateSets(exerciseIndex, info)
                    onDataChanged()
                }

                binding.layoutSets.addView(setView)
            }
        }

        private fun setupStrengthSet(setView: View, data: SetLogData) {
            val etReps = setView.findViewById<EditText>(R.id.etReps)
            val etWeight = setView.findViewById<EditText>(R.id.etWeight)

            etReps.setText(data.reps)
            etWeight.setText(data.weight)

            etReps.addTextChangedListener(createWatcher { data.reps = it })
            etWeight.addTextChangedListener(createWatcher { data.weight = it })
        }

        private fun setupCardioSet(setView: View, data: SetLogData, name: String) {
            val etDuration = setView.findViewById<EditText>(R.id.etDuration)
            val etDistance = setView.findViewById<EditText>(R.id.etDistance)
            val etFloors = setView.findViewById<EditText>(R.id.etFloors)
            val btnTimer = setView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnTimer)

            if (name.lowercase().contains("stair") || name.lowercase().contains("step")) {
                etFloors.visibility = View.VISIBLE
            } else {
                etFloors.visibility = View.GONE
            }

            etDuration.setText(formatTime(data.duration))
            etDistance.setText(data.distance)
            etFloors.setText(data.floors)

            etDuration.addTextChangedListener(createWatcher { data.duration = parseDurationInput(it) })
            etDistance.addTextChangedListener(createWatcher { data.distance = it })
            etFloors.addTextChangedListener(createWatcher { data.floors = it })

            btnTimer.setOnClickListener {
                data.isTimerRunning = !data.isTimerRunning
                if (data.isTimerRunning) {
                    btnTimer.setIconResource(android.R.drawable.ic_media_pause)
                    startTimer(data, etDuration)
                } else {
                    btnTimer.setIconResource(android.R.drawable.ic_media_play)
                    onDataChanged()
                }
            }
        }

        private fun startTimer(data: SetLogData, etDuration: EditText) {
            handler.post(object : Runnable {
                override fun run() {
                    if (data.isTimerRunning) {
                        data.duration++
                        etDuration.setText(formatTime(data.duration))
                        handler.postDelayed(this, 1000)
                    }
                }
            })
        }

        private fun createWatcher(onChanged: (String) -> Unit) = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                onChanged(s.toString())
                onDataChanged()
            }
        }

        private fun formatTime(seconds: Int): String {
            val mins = seconds / 60
            val secs = seconds % 60
            return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
        }

        private fun parseDurationInput(value: String): Int {
            val parts = value.split(":")
            return if (parts.size == 2) {
                val minutes = parts[0].toIntOrNull() ?: 0
                val seconds = parts[1].toIntOrNull() ?: 0
                (minutes * 60) + seconds.coerceIn(0, 59)
            } else {
                (value.toIntOrNull() ?: 0) * 60
            }
        }
    }
}
