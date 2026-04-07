package com.repsyncdemo.workout.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.databinding.FragmentAnalyticsBinding
import com.repsyncdemo.workout.ui.adapter.VolumeBreakdownAdapter
import com.repsyncdemo.workout.viewmodel.AnalyticsViewModel
import com.repsyncdemo.workout.viewmodel.TimeRange
import java.text.NumberFormat
import java.util.*

class AnalyticsFragment : Fragment(R.layout.fragment_analytics) {

    private lateinit var binding: FragmentAnalyticsBinding
    private val viewModel: AnalyticsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAnalyticsBinding.bind(view)

        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        // Observe Total Volume
        viewModel.totalVolume.observe(viewLifecycleOwner) { volume ->
            val formatted = NumberFormat.getNumberInstance(Locale.US).format(volume ?: 0.0)
            binding.tvTotalVolume.text = "$formatted lbs"
        }

        // Observe Workouts count
        viewModel.workoutsCount.observe(viewLifecycleOwner) { count ->
            binding.tvWorkoutsCount.text = count.toString()
        }

        // Observe Rest Days count
        viewModel.restDaysCount.observe(viewLifecycleOwner) { count ->
            binding.tvRestDaysCount.text = count.toString()
        }

        // Observe Date Range Text
        viewModel.dateRangeText.observe(viewLifecycleOwner) { text ->
            binding.tvDateRangeWorkouts.text = text
            binding.tvDateRangeRestDays.text = text
        }
    }

    private fun setupListeners() {
        // Time Range Toggle
        binding.toggleTimeRange.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val range = when (checkedId) {
                    R.id.btn7D -> TimeRange.LAST_7
                    R.id.btn30D -> TimeRange.LAST_30
                    R.id.btn90D -> TimeRange.LAST_90
                    else -> TimeRange.LIFETIME
                }
                viewModel.setTimeRange(range)
            }
        }

        // Card Clicks
        binding.cardTotalVolume.setOnClickListener {
            showVolumeBreakdownDialog()
        }

        binding.cardWorkouts.setOnClickListener {
            showCalendarDialog("Workout History", isWorkouts = true)
        }

        binding.cardRestDays.setOnClickListener {
            showCalendarDialog("Rest Day History", isWorkouts = false)
        }

        // Clear History
        binding.btnClearHistory.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun showVolumeBreakdownDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_volume_breakdown, null)
        val rvBreakdown = dialogView.findViewById<RecyclerView>(R.id.rvVolumeBreakdown)
        val adapter = VolumeBreakdownAdapter()

        rvBreakdown.layoutManager = LinearLayoutManager(requireContext())
        rvBreakdown.adapter = adapter

        viewModel.volumeBreakdown.observe(viewLifecycleOwner) { breakdown ->
            adapter.submitList(breakdown)
        }

        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setTitle("Exercise Volume Breakdown")
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showCalendarDialog(title: String, isWorkouts: Boolean) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_analytics_calendar, null)
        val calendarView = dialogView.findViewById<MaterialCalendarView>(R.id.calendarView)
        
        calendarView.setHeaderTextAppearance(R.style.CalendarHeaderStyle)
        calendarView.setDateTextAppearance(R.style.CalendarDateStyle)
        calendarView.setWeekDayTextAppearance(R.style.CalendarWeekStyle)
        calendarView.selectionMode = MaterialCalendarView.SELECTION_MODE_NONE

        if (isWorkouts) {
            viewModel.filteredWorkouts.value?.let { logs ->
                val primaryColor = ContextCompat.getColor(requireContext(), R.color.primary)
                logs.map { log ->
                    val cal = Calendar.getInstance().apply { timeInMillis = log.completedAt }
                    CalendarDay.from(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
                }.distinct().forEach { day ->
                    calendarView.addDecorator(WorkoutCountDecorator(primaryColor, day))
                }
            }
        } else {
            viewModel.filteredRestDays.value?.let { days ->
                val blueColor = android.graphics.Color.parseColor("#81D4FA")
                days.map { day ->
                    val cal = Calendar.getInstance().apply { timeInMillis = day.date }
                    CalendarDay.from(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
                }.distinct().forEach { date ->
                    calendarView.addDecorator(WorkoutCountDecorator(blueColor, date))
                }
            }
        }

        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showDeleteConfirmation() {
        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setTitle("Clear All Data?")
            .setMessage("This will permanently delete all your workout logs and reset your stats. This cannot be undone.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Clear Everything") { _, _ ->
                viewModel.clearAllHistory()
            }
            .show()
    }
}
