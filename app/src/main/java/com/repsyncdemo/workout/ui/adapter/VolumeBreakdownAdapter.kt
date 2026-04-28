package com.repsyncdemo.workout.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.repsyncdemo.workout.databinding.ItemVolumeBreakdownBinding
import com.repsyncdemo.workout.viewmodel.ExerciseVolumeBreakdown
import java.text.NumberFormat
import java.util.Locale

class VolumeBreakdownAdapter : ListAdapter<ExerciseVolumeBreakdown, VolumeBreakdownAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVolumeBreakdownBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemVolumeBreakdownBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ExerciseVolumeBreakdown) {
            binding.tvExerciseName.text = item.name
            val formattedVolume = NumberFormat.getNumberInstance(Locale.US).format(item.totalVolume)
            binding.tvVolumeDetails.text = "$formattedVolume lbs over ${item.workoutCount} sessions"
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ExerciseVolumeBreakdown>() {
        override fun areItemsTheSame(oldItem: ExerciseVolumeBreakdown, newItem: ExerciseVolumeBreakdown) = oldItem.name == newItem.name
        override fun areContentsTheSame(oldItem: ExerciseVolumeBreakdown, newItem: ExerciseVolumeBreakdown) = oldItem == newItem
    }
}
