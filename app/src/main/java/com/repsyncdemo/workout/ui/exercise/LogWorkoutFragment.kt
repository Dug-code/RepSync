package com.repsyncdemo.workout.ui.exercise

/**
 * File overview: Lets users log or edit a workout session, including timer controls, exercise sets, notes, date, and duration.
 */

import android.app.AlertDialog
import android.app.DatePickerDialog
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.data.model.WorkoutLog
import com.repsyncdemo.workout.databinding.FragmentLogWorkoutBinding
import com.repsyncdemo.workout.ui.adapter.ExerciseLogAdapter
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

    private var exerciseLogAdapter: ExerciseLogAdapter? = null
    
    private var startCalendar = Calendar.getInstance()
    private var endCalendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    private var existingLogId: String? = null
    private var loadedLog: WorkoutLog? = null
    private var isWorkoutModified = false
    private var isBindingLog = false
    private var selectedDateChanged = false
    private var activeWorkoutId: String? = null
    private var activeWorkoutName: String? = null
    private var templateWorkoutIdApplied: String? = null
    private val pendingExerciseAdds = mutableListOf<String>()

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

    // Sets up this screen.
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogWorkoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Connects views, clicks, and data.
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

        if (exerciseLogAdapter == null) {
            exerciseLogAdapter = ExerciseLogAdapter(onDataChanged = {
                isWorkoutModified = true
                updateLockState()
            })
        }

        binding.rvExerciseLogs.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = exerciseLogAdapter
        }

        existingLogId = arguments?.getString("logId")
        activeWorkoutId = arguments?.getString("workoutId")

        if (existingLogId != null) {
            viewModel.loadWorkoutLog(existingLogId!!)
            binding.btnComplete.text = "UPDATE WORKOUT"
            binding.btnDeleteLog.visibility = View.VISIBLE
            binding.cardTimer.visibility = View.GONE // Hide timer for past logs
        } else if (activeWorkoutId != null) {
            if (templateWorkoutIdApplied == activeWorkoutId && activeWorkoutName != null) {
                binding.tvWorkoutName.text = activeWorkoutName
            } else {
                binding.tvWorkoutName.text = "Loading workout..."
                loadWorkoutTemplate(activeWorkoutId!!)
            }
            binding.btnComplete.text = "COMPLETE WORKOUT"
            binding.btnDeleteLog.visibility = View.GONE
            startWorkoutTimer()
        }

        setupObservers()
        setupListeners()
    }

    // Sets up this section.
    private fun setupObservers() {
        viewModel.selectedLog.observe(viewLifecycleOwner) { log ->
            log?.let {
                if (existingLogId != it.id || loadedLog?.id == it.id) return@observe

                loadedLog = it
                isBindingLog = true
                activeWorkoutName = it.workoutName
                binding.tvWorkoutName.text = it.workoutName
                binding.etNotes.setText(it.notes)
                exerciseLogAdapter?.setExerciseLogs(it.exercises)
                startCalendar.timeInMillis = it.startedAt
                endCalendar.timeInMillis = it.completedAt
                secondsElapsed = (it.durationMinutes.coerceAtLeast(1) * 60).toLong()
                
                val durationMin = it.durationMinutes
                binding.tvManualDuration.text = "$durationMin min"
                updateTimerDisplay()
                
                updateDateTimeDisplays()
                isWorkoutModified = false
                isBindingLog = false
                selectedDateChanged = false
                updateLockState()
            }
        }

        // Observe exercise selection from library event stream
        findNavController().currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<String>("selectedExerciseName")
            ?.observe(viewLifecycleOwner) { exerciseName ->
                if (!exerciseName.isNullOrEmpty()) {
                    addOrQueueExerciseFromLibrary(exerciseName)
                    findNavController().currentBackStackEntry
                        ?.savedStateHandle
                        ?.remove<String>("selectedExerciseName")
                }
            }

        viewModel.selectedExerciseEvent.observe(viewLifecycleOwner) { exerciseName ->
            if (!exerciseName.isNullOrEmpty()) {
                addOrQueueExerciseFromLibrary(exerciseName)
            }
        }

        viewModel.operationResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess { logId ->
                if (logId.isNotEmpty() && !isBindingLog) {
                    val bundle = Bundle().apply { putString("logId", logId) }
                    // Navigate using action to trigger popUpTo behavior
                    findNavController().navigate(R.id.action_logWorkout_to_workoutSummary, bundle)
                }
            }
        }
    }

    // Loads data.
    private fun loadWorkoutTemplate(workoutId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = viewModel.getWorkoutTemplate(workoutId)
            result.onSuccess { workout ->
                if (_binding == null || existingLogId != null) return@onSuccess
                if (templateWorkoutIdApplied == workoutId) return@onSuccess

                templateWorkoutIdApplied = workoutId
                activeWorkoutName = workout.name
                binding.tvWorkoutName.text = workout.name
                exerciseLogAdapter?.setExercises(workout.exercises)
                drainPendingExerciseAdds()
            }.onFailure { error ->
                Toast.makeText(
                    requireContext(),
                    error.message ?: "Could not load workout template",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun addOrQueueExerciseFromLibrary(exerciseName: String) {
        val isStartingFromTemplate = existingLogId == null && activeWorkoutId != null
        if (isStartingFromTemplate && templateWorkoutIdApplied == null) {
            pendingExerciseAdds.add(exerciseName)
            return
        }

        addExerciseFromLibrary(exerciseName)
    }

    private fun drainPendingExerciseAdds() {
        if (pendingExerciseAdds.isEmpty()) return

        val exercisesToAdd = pendingExerciseAdds.toList()
        pendingExerciseAdds.clear()
        exercisesToAdd.forEach { addExerciseFromLibrary(it) }
    }

    private fun addExerciseFromLibrary(exerciseName: String) {
        val exerciseDefinition = viewModel.allLibraryExercises.value
            .find { it.name.equals(exerciseName, ignoreCase = true) }

        if (exerciseDefinition != null) {
            exerciseLogAdapter?.addExercise(exerciseDefinition)
        } else {
            exerciseLogAdapter?.addExercise(exerciseName)
        }
        isWorkoutModified = true
        updateLockState()
    }

    // Sets up this section.
    private fun setupListeners() {
        binding.tvSelectedDate.setOnClickListener { showDatePicker() }

        binding.etNotes.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: android.text.Editable?) {
                if (!isBindingLog) {
                    isWorkoutModified = true
                    updateLockState()
                }
            }
        })
        
        binding.tvManualDuration.setOnClickListener { showManualDurationDialog() }

        binding.btnPauseResume.setOnClickListener {
            toggleTimer()
        }

        binding.btnAddExercise.setOnClickListener {
            findNavController().navigate(R.id.exerciseLibraryFragment)
        }

        binding.btnScanExercise.setOnClickListener {
            findNavController().navigate(R.id.qrExerciseScannerFragment)
        }

        binding.btnComplete.setOnClickListener {
            checkCompletionAndSave()
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

    // Updates data or UI state.
    private fun updateTimerDisplay() {
        val hours = secondsElapsed / 3600
        val minutes = (secondsElapsed % 3600) / 60
        val secs = secondsElapsed % 60
        binding.tvTimer.text = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs)
    }

    // Shows a dialog or popup.
    private fun showManualDurationDialog() {
        val input = EditText(requireContext())
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        input.hint = "Minutes"
        
        // Pre-fill with current duration if available
        val currentMin = if (secondsElapsed > 0) {
            secondsElapsed / 60
        } else {
            loadedLog?.durationMinutes?.toLong() ?: 0L
        }
        input.setText(currentMin.toString())

        AlertDialog.Builder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setTitle("Manual Duration")
            .setMessage("Enter the total session time in minutes:")
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

    private fun checkCompletionAndSave() {
        val totalCompleted = exerciseLogAdapter?.getTotalCompletedSets() ?: 0
        if (totalCompleted == 0) {
            AlertDialog.Builder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
                .setTitle("Finish Workout?")
                .setMessage("You haven't checked off any sets. Are you sure you want to finish without completing any?")
                .setPositiveButton("Finish Anyway") { _, _ ->
                    completeWorkout()
                }
                .setNegativeButton("Keep Training", null)
                .show()
        } else {
            completeWorkout()
        }
    }

    private fun completeWorkout() {
        if (existingLogId == null && activeWorkoutId != null && templateWorkoutIdApplied == null) {
            Toast.makeText(requireContext(), "Routine template is still loading", Toast.LENGTH_SHORT).show()
            return
        }

        stopTimer()

        val durationMin = (secondsElapsed / 60).toInt().coerceAtLeast(1)
        val completedAt = when {
            existingLogId != null -> endCalendar.timeInMillis
            selectedDateChanged -> endCalendar.timeInMillis
            else -> System.currentTimeMillis()
        }
        val startedAt = completedAt - (durationMin * 60_000L)

        val workoutName = binding.tvWorkoutName.text.toString()
        val workoutId = arguments?.getString("workoutId") ?: loadedLog?.workoutId ?: ""

        val log = WorkoutLog(
            id = existingLogId ?: "",
            workoutId = workoutId,
            workoutName = workoutName,
            exercises = exerciseLogAdapter?.getExerciseLogs() ?: emptyList(),
            startedAt = startedAt,
            completedAt = completedAt,
            durationMinutes = durationMin,
            notes = binding.etNotes.text.toString().trim()
        )

        navigationLockViewModel.setLocked(false)
        viewModel.logWorkout(log)
        Toast.makeText(requireContext(), if (existingLogId == null) "Session completed!" else "Session log updated!", Toast.LENGTH_SHORT).show()
        isWorkoutModified = false
    }

    // Updates data or UI state.
    private fun updateLockState() {
        navigationLockViewModel.setLocked(hasUnsavedChanges())
    }

    private fun hasUnsavedChanges(): Boolean {
        if (existingLogId != null) {
            return isWorkoutModified
        }

        return isWorkoutModified || isTimerRunning || secondsElapsed > 0 ||
               binding.etNotes.text.toString().isNotEmpty() ||
               (exerciseLogAdapter?.itemCount ?: 0) > 0
    }

    // Shows a dialog or popup.
    private fun showUnsavedChangesDialog(onDiscard: () -> Unit) {
        AlertDialog.Builder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setTitle("Discard Session?")
            .setMessage("Your current progress will be lost. Are you sure?")
            .setPositiveButton("Discard") { _, _ -> onDiscard() }
            .setNegativeButton("Keep Training", null)
            .show()
    }

    // Shows a dialog or popup.
    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setTitle("Delete Log")
            .setMessage("Are you sure you want to delete this session log? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                existingLogId?.let { id ->
                    viewModel.deleteWorkoutLog(id)
                    Toast.makeText(requireContext(), "Session log deleted", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Updates data or UI state.
    private fun updateDateTimeDisplays() {
        binding.tvSelectedDate.text = dateFormat.format(endCalendar.time)
    }

    // Shows a dialog or popup.
    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                startCalendar.set(Calendar.YEAR, year)
                startCalendar.set(Calendar.MONTH, month)
                startCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                endCalendar.set(Calendar.YEAR, year)
                endCalendar.set(Calendar.MONTH, month)
                endCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                selectedDateChanged = true
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

    // Clears the view binding.
    override fun onDestroyView() {
        stopTimer()
        binding.rvExerciseLogs.adapter = null
        super.onDestroyView()
        _binding = null
    }
}
