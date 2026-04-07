package com.repsyncdemo.workout.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.repsyncdemo.workout.data.model.WorkoutLog
import com.repsyncdemo.workout.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private val onEditClick: (WorkoutLog) -> Unit
) : ListAdapter<WorkoutLog, HistoryAdapter.ViewHolder>(HistoryDiffCallback()) {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())

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
            binding.tvWorkoutName.text = log.workoutName
            binding.tvDate.text = dateFormat.format(Date(log.completedAt))
            binding.tvDuration.text = "${log.durationMinutes} min"
            val count = log.exercises.size
            binding.tvExerciseCount.text = "$count ${if (count == 1) "exercise" else "exercises"}"
            binding.btnEdit.setOnClickListener { onEditClick(log) }
        }
    }

    class HistoryDiffCallback : DiffUtil.ItemCallback<WorkoutLog>() {
        override fun areItemsTheSame(oldItem: WorkoutLog, newItem: WorkoutLog) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: WorkoutLog, newItem: WorkoutLog) = oldItem == newItem
    }
}
