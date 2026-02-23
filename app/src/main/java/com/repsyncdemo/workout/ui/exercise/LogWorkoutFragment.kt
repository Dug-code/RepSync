package com.repsyncdemo.workout.ui.exercise

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.repsyncdemo.workout.data.model.WorkoutLog
import com.repsyncdemo.workout.databinding.FragmentLogWorkoutBinding
import com.repsyncdemo.workout.ui.adapter.ExerciseLogAdapter
import com.repsyncdemo.workout.viewmodel.WorkoutViewModel
import java.text.SimpleDateFormat
import java.util.*

class LogWorkoutFragment : Fragment() {

    private var _binding: FragmentLogWorkoutBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WorkoutViewModel by activityViewModels()

    private lateinit var exerciseLogAdapter: ExerciseLogAdapter
    private var startCalendar = Calendar.getInstance()
    private var endCalendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    
    private var existingLogId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogWorkoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        exerciseLogAdapter = ExerciseLogAdapter()

        binding.rvExerciseLogs.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = exerciseLogAdapter
        }

        existingLogId = arguments?.getString("logId")
        val workoutId = arguments?.getString("workoutId")

        if (existingLogId != null) {
            viewModel.loadWorkoutLog(existingLogId!!)
            binding.btnComplete.text = "Update Workout"
        } else if (workoutId != null) {
            viewModel.loadWorkout(workoutId)
            binding.btnComplete.text = "Save Workout"
            // Set default end time 1 hour from now
            endCalendar.add(Calendar.HOUR_OF_DAY, 1)
        }

        viewModel.selectedWorkout.observe(viewLifecycleOwner) { workout ->
            if (existingLogId == null) {
                workout?.let {
                    binding.tvWorkoutName.text = it.name
                    exerciseLogAdapter.setExercises(it.exercises)
                }
            }
        }

        viewModel.selectedLog.observe(viewLifecycleOwner) { log ->
            log?.let {
                binding.tvWorkoutName.text = it.workoutName
                binding.etNotes.setText(it.notes)
                exerciseLogAdapter.setExerciseLogs(it.exercises)
                startCalendar.timeInMillis = it.startedAt
                endCalendar.timeInMillis = it.completedAt
                updateDateTimeDisplays()
            }
        }

        updateDateTimeDisplays()
        
        binding.tvSelectedDate.setOnClickListener { showDatePicker() }
        binding.tvStartTime.setOnClickListener { showTimePicker(startCalendar, true) }
        binding.tvEndTime.setOnClickListener { showTimePicker(endCalendar, false) }

        binding.btnComplete.setOnClickListener {
            saveWorkout()
        }
    }

    private fun updateDateTimeDisplays() {
        binding.tvSelectedDate.text = dateFormat.format(startCalendar.time)
        binding.tvStartTime.text = timeFormat.format(startCalendar.time)
        binding.tvEndTime.text = timeFormat.format(endCalendar.time)
    }

    private fun showDatePicker() {
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                startCalendar.set(Calendar.YEAR, year)
                startCalendar.set(Calendar.MONTH, month)
                startCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                // Sync end date with start date
                endCalendar.set(Calendar.YEAR, year)
                endCalendar.set(Calendar.MONTH, month)
                endCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateTimeDisplays()
            },
            startCalendar.get(Calendar.YEAR),
            startCalendar.get(Calendar.MONTH),
            startCalendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker(calendar: Calendar, isStart: Boolean) {
        TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                updateDateTimeDisplays()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        ).show()
    }

    private fun saveWorkout() {
        val workoutName = binding.tvWorkoutName.text.toString()
        val workoutId = arguments?.getString("workoutId") ?: viewModel.selectedLog.value?.workoutId ?: ""
        
        val duration = ((endCalendar.timeInMillis - startCalendar.timeInMillis) / 60000).toInt()

        val log = WorkoutLog(
            id = existingLogId ?: "",
            workoutId = workoutId,
            workoutName = workoutName,
            exercises = exerciseLogAdapter.getExerciseLogs(),
            startedAt = startCalendar.timeInMillis,
            completedAt = endCalendar.timeInMillis,
            durationMinutes = if (duration > 0) duration else 0,
            notes = binding.etNotes.text.toString().trim()
        )

        viewModel.logWorkout(log)
        Toast.makeText(requireContext(), if (existingLogId == null) "Workout logged!" else "Workout updated!", Toast.LENGTH_SHORT).show()
        viewModel.clearSelection()
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
