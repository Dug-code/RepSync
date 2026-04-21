package com.repsyncdemo.workout.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.repsyncdemo.workout.data.model.WorkoutLog
import com.repsyncdemo.workout.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private val onEditClick: (WorkoutLog) -> Unit
) : ListAdapter<WorkoutLog, HistoryAdapter.ViewHolder>(HistoryDiffCallback()) {

    private val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(log: WorkoutLog) {
            val date = Date(log.completedAt)
            binding.tvDateMonth.text = monthFormat.format(date).uppercase()
            binding.tvDateDay.text = getDayWithSuffix(log.completedAt)
            
            binding.tvWorkoutName.text = log.workoutName
            binding.tvDuration.text = "${log.durationMinutes} min"
            val count = log.exercises.size
            binding.tvExerciseCount.text = "$count ${if (count == 1) "exercise" else "exercises"}"
            
            binding.btnEdit.setOnClickListener { onEditClick(log) }
        }

        private fun getDayWithSuffix(timestamp: Long): String {
            val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val suffix = when {
                day in 11..13 -> "th"
                day % 10 == 1 -> "st"
                day % 10 == 2 -> "nd"
                day % 10 == 3 -> "rd"
                else -> "th"
            }
            return String.format("%02d%s", day, suffix)
        }
    }

    class HistoryDiffCallback : DiffUtil.ItemCallback<WorkoutLog>() {
        override fun areItemsTheSame(oldItem: WorkoutLog, newItem: WorkoutLog) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: WorkoutLog, newItem: WorkoutLog) = oldItem == newItem
    }
}
