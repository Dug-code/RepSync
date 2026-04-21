package com.repsyncdemo.workout.ui.exercise

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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

    // Timer Variables
    private var timerHandler = Handler(Looper.getMainLooper())
    private var startTimeMillis: Long = 0L
    private var isTimerRunning = false
    private var secondsElapsed = 0L

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isTimerRunning) {
                secondsElapsed++
                updateTimerDisplay()
                timerHandler.postDelayed(this, 1000)
            }
        }
    }

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
                        stopTimer()
                        navigationLockViewModel.setLocked(false)
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                } else {
                    stopTimer()
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
            binding.btnComplete.text = "UPDATE WORKOUT"
            binding.btnDeleteLog.visibility = View.VISIBLE
            binding.cardTimer.visibility = View.GONE // Hide timer for past logs
        } else if (workoutId != null) {
            viewModel.loadWorkout(workoutId)
            binding.btnComplete.text = "COMPLETE WORKOUT"
            binding.btnDeleteLog.visibility = View.GONE
            startWorkoutTimer()
        }

        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
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
                
                // For past logs, show the duration manually
                val durationMin = it.durationMinutes
                binding.tvManualDuration.text = "$durationMin min"
                
                updateDateTimeDisplays()
            }
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
    }

    private fun setupListeners() {
        binding.tvSelectedDate.setOnClickListener { showDatePicker() }
        
        binding.tvManualDuration.setOnClickListener { showManualDurationDialog() }

        binding.btnPauseResume.setOnClickListener {
            toggleTimer()
        }

        binding.btnAddExercise.setOnClickListener {
            ShowExercisePickerDialog().show(parentFragmentManager, "exercise_picker")
        }

        binding.btnComplete.setOnClickListener {
            completeWorkout()
        }

        binding.btnDeleteLog.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun startWorkoutTimer() {
        if (!isTimerRunning) {
            startTimeMillis = System.currentTimeMillis()
            startCalendar.timeInMillis = startTimeMillis
            isTimerRunning = true
            timerHandler.post(timerRunnable)
            updateLockState()
        }
    }

    private fun toggleTimer() {
        if (isTimerRunning) {
            isTimerRunning = false
            binding.btnPauseResume.text = "RESUME"
            binding.btnPauseResume.setIconResource(android.R.drawable.ic_media_play)
        } else {
            isTimerRunning = true
            binding.btnPauseResume.text = "PAUSE"
            binding.btnPauseResume.setIconResource(android.R.drawable.ic_media_pause)
            timerHandler.post(timerRunnable)
        }
    }

    private fun stopTimer() {
        isTimerRunning = false
        timerHandler.removeCallbacks(timerRunnable)
    }

    private fun updateTimerDisplay() {
        val hours = secondsElapsed / 3600
        val minutes = (secondsElapsed % 3600) / 60
        val secs = secondsElapsed % 60
        binding.tvTimer.text = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs)
    }

    private fun showManualDurationDialog() {
        val input = EditText(requireContext())
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        input.hint = "Minutes"
        
        // Pre-fill with current duration if available
        val currentMin = if (secondsElapsed > 0) (secondsElapsed / 60) else 0
        input.setText(currentMin.toString())

        AlertDialog.Builder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setTitle("Manual Duration")
            .setMessage("Enter the total workout time in minutes:")
            .setView(input)
            .setPositiveButton("Set") { _, _ ->
                val min = input.text.toString().toLongOrNull() ?: 0L
                secondsElapsed = min * 60
                updateTimerDisplay()
                binding.tvManualDuration.text = "$min min"
                isWorkoutModified = true
                updateLockState()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun completeWorkout() {
        stopTimer()
        
        val now = System.currentTimeMillis()
        endCalendar.timeInMillis = now
        
        val durationMin = (secondsElapsed / 60).toInt()

        val workoutName = binding.tvWorkoutName.text.toString()
        val workoutId = arguments?.getString("workoutId") ?: viewModel.selectedLog.value?.workoutId ?: ""

        val log = WorkoutLog(
            id = existingLogId ?: "",
            workoutId = workoutId,
            workoutName = workoutName,
            exercises = exerciseLogAdapter.getExerciseLogs(),
            startedAt = startCalendar.timeInMillis,
            completedAt = endCalendar.timeInMillis,
            durationMinutes = if (durationMin > 0) durationMin else 1,
            notes = binding.etNotes.text.toString().trim()
        )

        navigationLockViewModel.setLocked(false)
        viewModel.logWorkout(log)
        Toast.makeText(requireContext(), if (existingLogId == null) "Workout Completed!" else "Workout Updated!", Toast.LENGTH_SHORT).show()
        viewModel.clearSelection()
        isWorkoutModified = false
        findNavController().popBackStack()
    }

    private fun updateLockState() {
        navigationLockViewModel.setLocked(hasUnsavedChanges())
    }

    private fun hasUnsavedChanges(): Boolean {
        return isWorkoutModified || isTimerRunning || secondsElapsed > 0 ||
               binding.etNotes.text.toString().isNotEmpty() || 
               exerciseLogAdapter.itemCount > 0
    }

    private fun showUnsavedChangesDialog(onDiscard: () -> Unit) {
        AlertDialog.Builder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setTitle("Discard Workout?")
            .setMessage("Your current progress will be lost. Are you sure?")
            .setPositiveButton("Discard") { _, _ -> onDiscard() }
            .setNegativeButton("Keep Training", null)
            .show()
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setTitle("Delete Log")
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
    }

    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                startCalendar.set(Calendar.YEAR, year)
                startCalendar.set(Calendar.MONTH, month)
                startCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateTimeDisplays()
                isWorkoutModified = true
                updateLockState()
            },
            startCalendar.get(Calendar.YEAR),
            startCalendar.get(Calendar.MONTH),
            startCalendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis() + (24 * 60 * 60 * 1000)
        datePickerDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopTimer()
        _binding = null
    }
}
