package com.repsyncdemo.workout.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.databinding.FragmentAnalyticsSummaryBinding
import com.repsyncdemo.workout.databinding.ItemStatRowBinding
import com.repsyncdemo.workout.ui.adapter.VolumeBreakdownAdapter
import com.repsyncdemo.workout.viewmodel.AnalyticsViewModel
import com.repsyncdemo.workout.viewmodel.StatItem
import com.repsyncdemo.workout.viewmodel.TimeRange
import java.text.NumberFormat
import java.util.*

class AnalyticsSummaryFragment : Fragment() {

    private var _binding: FragmentAnalyticsSummaryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AnalyticsViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAnalyticsSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        viewModel.totalVolume.observe(viewLifecycleOwner) { volume ->
            val formatted = NumberFormat.getNumberInstance(Locale.US).format(volume ?: 0.0)
            binding.tvTotalVolume.text = "$formatted lbs"
        }

        viewModel.workoutsCount.observe(viewLifecycleOwner) { count ->
            binding.tvWorkoutsCount.text = count.toString()
        }

        viewModel.restDaysCount.observe(viewLifecycleOwner) { count ->
            binding.tvRestDaysCount.text = count.toString()
        }

        viewModel.dateRangeText.observe(viewLifecycleOwner) { text ->
            binding.tvDateRangeWorkouts.text = text
            binding.tvDateRangeRestDays.text = text
        }

        viewModel.favoriteWorkout.observe(viewLifecycleOwner) { item ->
            binding.tvFavWorkoutName.text = item?.name ?: "None"
            binding.tvFavWorkoutSessions.text = "${item?.count ?: 0} sessions"
        }

        viewModel.favoriteMuscle.observe(viewLifecycleOwner) { item ->
            binding.tvFavMuscleName.text = item?.name ?: "None"
            binding.tvFavMuscleSessions.text = "${item?.count ?: 0} times"
        }
    }

    private fun setupListeners() {
        binding.toggleTimeRange.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val range = when (checkedId) {
                    R.id.btn7D -> TimeRange.LAST_7
                    R.id.btn30D -> TimeRange.LAST_30
                    else -> TimeRange.LIFETIME
                }
                viewModel.setTimeRange(range)
            }
        }

        binding.cardTotalVolume.setOnClickListener { showVolumeBreakdownDialog() }
        binding.cardWorkouts.setOnClickListener { showCalendarDialog("Workout History", true) }
        binding.cardRestDays.setOnClickListener { showCalendarDialog("Rest Day History", false) }

        binding.cardFavWorkout.setOnClickListener {
            viewModel.topWorkouts.value?.let { showTopTenDialog("Top Workouts", it, "sessions") }
        }

        binding.cardFavMuscle.setOnClickListener {
            viewModel.topMuscleGroups.value?.let { showTopTenDialog("Top Muscle Groups", it, "times") }
        }
    }

    private fun showTopTenDialog(title: String, items: List<StatItem>, unit: String) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_volume_breakdown, null)
        val rv = dialogView.findViewById<RecyclerView>(R.id.rvVolumeBreakdown)
        
        val adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = object : RecyclerView.ViewHolder(
                ItemStatRowBinding.inflate(LayoutInflater.from(parent.context), parent, false).root
            ) {}
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val item = items[position]
                val b = ItemStatRowBinding.bind(holder.itemView)
                b.tvStatName.text = item.name
                b.tvStatCount.text = "${item.count} $unit"
            }
            override fun getItemCount() = items.size
        }

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showVolumeBreakdownDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_volume_breakdown, null)
        val rvBreakdown = dialogView.findViewById<RecyclerView>(R.id.rvVolumeBreakdown)
        val adapter = VolumeBreakdownAdapter()
        rvBreakdown.layoutManager = LinearLayoutManager(requireContext())
        rvBreakdown.adapter = adapter
        viewModel.volumeBreakdown.observe(viewLifecycleOwner) { adapter.submitList(it) }

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
