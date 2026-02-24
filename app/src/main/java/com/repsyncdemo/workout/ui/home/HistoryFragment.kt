package com.repsyncdemo.workout.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.data.model.WorkoutLog
import com.repsyncdemo.workout.databinding.FragmentHistoryBinding
import com.repsyncdemo.workout.ui.adapter.HistoryAdapter
import com.repsyncdemo.workout.viewmodel.WorkoutViewModel
import java.util.*

class HistoryFragment : Fragment(), OnDateSelectedListener {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WorkoutViewModel by activityViewModels()

    private lateinit var historyAdapter: HistoryAdapter
    private var allLogs: List<WorkoutLog> = emptyList()
    private var selectedDate: CalendarDay = CalendarDay.today()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        historyAdapter = HistoryAdapter { log ->
            val bundle = Bundle().apply { 
                putString("workoutId", log.workoutId)
                putString("logId", log.id)
            }
            findNavController().navigate(R.id.logWorkoutFragment, bundle)
        }

        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }

        binding.calendarView.setOnDateChangedListener(this)
        binding.calendarView.setSelectedDate(CalendarDay.today())

        viewModel.workoutLogs.observe(viewLifecycleOwner) { logs ->
            allLogs = logs
            updateCalendarDecorators()
            filterLogsForSelectedDate()
        }

        viewModel.restDays.observe(viewLifecycleOwner) {
            updateCalendarDecorators()
            filterLogsForSelectedDate() // Re-filter to show rest day message if needed
        }
    }

    override fun onDateSelected(
        widget: MaterialCalendarView,
        date: CalendarDay,
        selected: Boolean
    ) {
        selectedDate = date
        filterLogsForSelectedDate()
    }

    private fun updateCalendarDecorators() {
        binding.calendarView.removeDecorators()
        
        // 1. Draw Workout Dots (Red)
        val workoutDays = allLogs.map { log ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = log.completedAt
            CalendarDay.from(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
        }.distinct()

        val primaryColor = ContextCompat.getColor(requireContext(), R.color.primary)
        workoutDays.forEach { day ->
            binding.calendarView.addDecorator(WorkoutCountDecorator(primaryColor, day))
        }

        // 2. Draw Rest Day Dots (Light Blue)
        val restDays = viewModel.restDays.value ?: emptyList()
        val restDayDates = restDays.map { 
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.date
            CalendarDay.from(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
        }

        val blueColor = android.graphics.Color.parseColor("#81D4FA")
        restDayDates.forEach { day ->
            binding.calendarView.addDecorator(WorkoutCountDecorator(blueColor, day))
        }
    }

    private fun filterLogsForSelectedDate() {
        val filteredWorkouts = allLogs.filter { log ->
            val logCal = Calendar.getInstance()
            logCal.timeInMillis = log.completedAt
            val logDay = CalendarDay.from(logCal.get(Calendar.YEAR), logCal.get(Calendar.MONTH) + 1, logCal.get(Calendar.DAY_OF_MONTH))
            logDay == selectedDate
        }

        val restDays = viewModel.restDays.value ?: emptyList()
        val isRestDay = restDays.any { 
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.date
            val day = CalendarDay.from(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
            day == selectedDate
        }
        
        if (filteredWorkouts.isNotEmpty()) {
            historyAdapter.submitList(filteredWorkouts)
            binding.rvHistory.visibility = View.VISIBLE
            binding.tvEmpty.visibility = View.GONE
        } else if (isRestDay) {
            historyAdapter.submitList(emptyList())
            binding.rvHistory.visibility = View.GONE
            binding.tvEmpty.visibility = View.VISIBLE
            binding.tvEmpty.text = "Hope you enjoyed the day off\nDon't make it a habit"
        } else {
            historyAdapter.submitList(emptyList())
            binding.rvHistory.visibility = View.GONE
            binding.tvEmpty.visibility = View.VISIBLE
            binding.tvEmpty.text = "No workouts on this day."
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
