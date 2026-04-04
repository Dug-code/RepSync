package com.repsyncdemo.workout.ui.exercise

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.data.model.WorkoutLog
import com.repsyncdemo.workout.databinding.FragmentLogWorkoutBinding
import com.repsyncdemo.workout.ui.adapter.ExerciseLogAdapter
import com.repsyncdemo.workout.ui.dialogs.ShowExercisePickerDialog
import com.repsyncdemo.workout.viewmodel.NavigationLockViewModel
import com.repsyncdemo.workout.viewmodel.WorkoutViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class LogWorkoutFragment : Fragment() {

    private var _binding: FragmentLogWorkoutBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WorkoutViewModel by activityViewModels()
    private val navigationLockViewModel: NavigationLockViewModel by activityViewModels()

    private lateinit var exerciseLogAdapter: ExerciseLogAdapter
    private var startCalendar = Calendar.getInstance()
    private var endCalendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    
    private var existingLogId: String? = null
    private var isWorkoutModified = false

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

        // Handle Back Navigation with Warning
        val backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (hasUnsavedChanges()) {
                    showUnsavedChangesDialog {
                        navigationLockViewModel.setLocked(false)
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)

        exerciseLogAdapter = ExerciseLogAdapter(onDataChanged = {
            isWorkoutModified = true
            updateLockState()
        })

        binding.rvExerciseLogs.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = exerciseLogAdapter
        }

        existingLogId = arguments?.getString("logId")
        val workoutId = arguments?.getString("workoutId")

        if (existingLogId != null) {
            viewModel.loadWorkoutLog(existingLogId!!)
            binding.btnComplete.text = "Update Workout"
            binding.btnDeleteLog.visibility = View.VISIBLE
        } else if (workoutId != null) {
            viewModel.loadWorkout(workoutId)
            binding.btnComplete.text = "Save Workout"
            binding.btnDeleteLog.visibility = View.GONE
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

        binding.btnAddExercise.setOnClickListener {
            ShowExercisePickerDialog().show(parentFragmentManager, "exercise_picker")
        }

        // Observe exercise selection from library
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedExercises.collect { exerciseName ->
                    if (!exerciseName.isNullOrEmpty()) {
                        exerciseLogAdapter.addExercise(exerciseName)
                        isWorkoutModified = true
                        updateLockState()
                        viewModel.clearSelectedExercises()
                    }
                }
            }
        }

        binding.btnComplete.setOnClickListener {
            saveWorkout()
        }

        binding.btnDeleteLog.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun updateLockState() {
        navigationLockViewModel.setLocked(hasUnsavedChanges())
    }

    private fun hasUnsavedChanges(): Boolean {
        // Workout is modified if any field is changed or exercises added/removed
        return isWorkoutModified || 
               binding.etNotes.text.toString().isNotEmpty() || 
               exerciseLogAdapter.itemCount > 0
    }

    private fun showUnsavedChangesDialog(onDiscard: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle("Discard Changes?")
            .setMessage("You have unsaved changes. Are you sure you want to discard them?")
            .setPositiveButton("Discard") { _, _ -> onDiscard() }
            .setNegativeButton("Keep Editing", null)
            .show()
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Workout")
            .setMessage("Are you sure you want to delete this workout log? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                existingLogId?.let { id ->
                    viewModel.deleteWorkoutLog(id)
                    Toast.makeText(requireContext(), "Workout deleted", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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
                isWorkoutModified = true
                updateLockState()
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
                isWorkoutModified = true
                updateLockState()
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

        navigationLockViewModel.setLocked(false)
        viewModel.logWorkout(log)
        Toast.makeText(requireContext(), if (existingLogId == null) "Workout logged!" else "Workout updated!", Toast.LENGTH_SHORT).show()
        viewModel.clearSelection()
        isWorkoutModified = false // Reset before navigating
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
