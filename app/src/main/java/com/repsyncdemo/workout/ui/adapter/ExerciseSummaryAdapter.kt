package com.repsyncdemo.workout.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.data.model.ExerciseLog
import com.repsyncdemo.workout.data.model.ExerciseType
import com.repsyncdemo.workout.databinding.ItemExerciseSummaryBinding

class ExerciseSummaryAdapter(private val logs: List<ExerciseLog>) :
    RecyclerView.Adapter<ExerciseSummaryAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExerciseSummaryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(logs[position])
    }

    override fun getItemCount() = logs.size

    class ViewHolder(private val binding: ItemExerciseSummaryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(log: ExerciseLog) {
            binding.tvExerciseName.text = log.exerciseName
            binding.layoutSets.removeAllViews()

            log.sets.filter { it.completed }.forEachIndexed { index, set ->
                val tvSet = TextView(binding.root.context).apply {
                    val text = if (log.type == ExerciseType.CARDIO) {
                        val time = formatTime(set.durationSeconds ?: 0)
                        val dist = if (set.distance != null) " • ${set.distance} mi" else ""
                        "Set ${index + 1}: $time$dist"
                    } else {
                        "Set ${index + 1}: ${set.reps} reps • ${set.weight} lbs"
                    }
                    this.text = text
                    textSize = 14f
                    setTextColor(resources.getColor(R.color.text_secondary, null))
                    setPadding(0, 4, 0, 4)
                }
                binding.layoutSets.addView(tvSet)
            }
        }

        private fun formatTime(seconds: Int): String {
            val mins = seconds / 60
            val secs = seconds % 60
            return String.format("%02d:%02d", mins, secs)
        }
    }
}
