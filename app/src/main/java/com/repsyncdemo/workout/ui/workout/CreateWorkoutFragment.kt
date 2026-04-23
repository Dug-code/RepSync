package com.repsyncdemo.workout.ui.workout

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.data.ExerciseDatabase
import com.repsyncdemo.workout.data.model.ExerciseType
import com.repsyncdemo.workout.data.model.FeedPost
import com.repsyncdemo.workout.data.model.FeedPostType
import com.repsyncdemo.workout.data.model.Workout
import com.repsyncdemo.workout.databinding.FragmentCreateWorkoutBinding
import com.repsyncdemo.workout.ui.adapter.ExerciseInputAdapter
import com.repsyncdemo.workout.ui.dialogs.ShowExercisePickerDialog
import com.repsyncdemo.workout.util.DragToReorderCallBack
import com.repsyncdemo.workout.viewmodel.FeedViewModel
import com.repsyncdemo.workout.viewmodel.NavigationLockViewModel
import com.repsyncdemo.workout.viewmodel.ProfileViewModel
import com.repsyncdemo.workout.viewmodel.WorkoutViewModel
import kotlinx.coroutines.launch

class CreateWorkoutFragment : Fragment() {

    private var _binding: FragmentCreateWorkoutBinding? = null
    private val binding get() = _binding!!

    private val workoutViewModel: WorkoutViewModel by activityViewModels()
    private val feedViewModel: FeedViewModel by activityViewModels()
    private val profileViewModel: ProfileViewModel by activityViewModels()
    private val navigationLockViewModel: NavigationLockViewModel by activityViewModels()

    private lateinit var exerciseInputAdapter: ExerciseInputAdapter
    private var existingWorkoutId: String? = null
    private var isPublic: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateWorkoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        existingWorkoutId = arguments?.getString("workoutId")

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

        setupChangeListeners()

        profileViewModel.loadProfile()

        exerciseInputAdapter = ExerciseInputAdapter(onDataChanged = {
            updateLockState()
        })

        binding.rvExercises.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = exerciseInputAdapter
        }

        val dragHandler = DragToReorderCallBack { fromPosition, toPosition ->
            exerciseInputAdapter.moveExercise(fromPosition, toPosition)
        }
        val itemTouchHelper = ItemTouchHelper(dragHandler)
        itemTouchHelper.attachToRecyclerView(binding.rvExercises)

        if (existingWorkoutId != null) {
            workoutViewModel.loadWorkout(existingWorkoutId!!)
            binding.btnSave.text = "Update Workout"
        } else {
            if (exerciseInputAdapter.itemCount == 0) {
                exerciseInputAdapter.addExercise()
            }
        }

        workoutViewModel.selectedWorkout.observe(viewLifecycleOwner) { workout ->
            if (existingWorkoutId != null && workout != null) {
                binding.etName.setText(workout.name)
                binding.etDescription.setText(workout.description)
                isPublic = workout.isPublic
                updatePrivacyIcon()
                
                exerciseInputAdapter.clearItems()
                workout.exercises.forEach { exercise ->
                    exerciseInputAdapter.addExerciseFromObject(exercise)
                }
            }
        }

        binding.btnPrivacyMenu.setOnClickListener { showPrivacyPopupMenu(it) }

        binding.btnAddExercise.setOnClickListener {
            showCustomExerciseDialog()
        }

        binding.btnPickExercise.setOnClickListener {
            ShowExercisePickerDialog().show(parentFragmentManager, "exercise_picker")
        }

        binding.btnSave.setOnClickListener {
            saveWorkout()
        }

        workoutViewModel.operationResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess { workoutId ->
                navigationLockViewModel.setLocked(false)
                if (isPublic && existingWorkoutId == null) {
                    val username = profileViewModel.myProfile.value?.username ?: ""
                    val post = FeedPost(
                        userId = profileViewModel.myProfile.value?.userId ?: "",
                        username = username,
                        type = FeedPostType.WORKOUT_SHARED,
                        workoutId = workoutId,
                        workoutName = binding.etName.text.toString().trim(),
                        description = binding.etDescription.text.toString().trim()
                    )
                    feedViewModel.createPost(post)
                }
                Toast.makeText(requireContext(), if (existingWorkoutId == null) "Workout saved!" else "Workout updated!", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
            result.onFailure { e ->
                Toast.makeText(requireContext(), e.message ?: "Save failed", Toast.LENGTH_SHORT).show()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                workoutViewModel.selectedExercises.collect { exerciseName ->
                    if (!exerciseName.isNullOrEmpty()) {
                        exerciseInputAdapter.addExerciseFromLibrary(exerciseName)
                        updateLockState()
                        workoutViewModel.clearSelectedExercises()
                    }
                }
            }
        }
    }

    private fun showPrivacyPopupMenu(view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.menu_workout_privacy, popup.menu)
        
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_public -> {
                    isPublic = true
                    updatePrivacyIcon()
                    updateLockState()
                    true
                }
                R.id.action_private -> {
                    isPublic = false
                    updatePrivacyIcon()
                    updateLockState()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun updatePrivacyIcon() {
        binding.ivPrivacyIcon.setImageResource(if (isPublic) R.drawable.ic_public else R.drawable.ic_private)
    }

    private fun showCustomExerciseDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_custom_exercise, null)
        val etName = dialogView.findViewById<EditText>(R.id.etCustomName)
        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerType)
        val spinnerPrimary = dialogView.findViewById<Spinner>(R.id.spinnerPrimaryMuscle)
        val spinnerSecondary = dialogView.findViewById<Spinner>(R.id.spinnerSecondaryMuscle)

        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, ExerciseType.values())
        spinnerType.adapter = typeAdapter

        val muscleGroups = listOf("None") + ExerciseDatabase.bodyParts
        val muscleAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, muscleGroups)
        spinnerPrimary.adapter = muscleAdapter
        spinnerSecondary.adapter = muscleAdapter

        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setTitle("New Custom Exercise")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = etName.text.toString().trim()
                val type = spinnerType.selectedItem as ExerciseType
                val primary = spinnerPrimary.selectedItem.toString()
                val secondary = spinnerSecondary.selectedItem.toString()
                
                if (name.isNotEmpty()) {
                    exerciseInputAdapter.addExercise(name, type, primary, secondary)
                    updateLockState()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupChangeListeners() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateLockState()
            }
        }

        binding.etName.addTextChangedListener(watcher)
        binding.etDescription.addTextChangedListener(watcher)
    }

    private fun updateLockState() {
        navigationLockViewModel.setLocked(hasUnsavedChanges())
    }

    private fun hasUnsavedChanges(): Boolean {
        val name = binding.etName.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val exercises = exerciseInputAdapter.getExercises()
        
        return name.isNotEmpty() || description.isNotEmpty() || exercises.isNotEmpty()
    }

    private fun showUnsavedChangesDialog(onDiscard: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setTitle("Discard Changes?")
            .setMessage("You have unsaved changes. Are you sure you want to discard them?")
            .setPositiveButton("Discard") { _, _ -> onDiscard() }
            .setNegativeButton("Keep Editing", null)
            .show()
    }

    private fun saveWorkout() {
        val name = binding.etName.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()

        if (name.isEmpty()) {
            binding.etName.error = "Name is required"
            Toast.makeText(requireContext(), "Please enter a workout name", Toast.LENGTH_SHORT).show()
            return
        }

        val exercises = exerciseInputAdapter.getExercises().filter { it.name.isNotEmpty() }

        if (exercises.isEmpty()) {
            Toast.makeText(requireContext(), "Add at least one exercise", Toast.LENGTH_SHORT).show()
            return
        }

        val workout = Workout(
            id = existingWorkoutId ?: "",
            name = name,
            description = description,
            exercises = exercises,
            isPublic = isPublic
        )

        if (existingWorkoutId == null) {
            workoutViewModel.addWorkout(workout)
        } else {
            workoutViewModel.updateWorkout(workout)
            Toast.makeText(requireContext(), "Workout updated!", Toast.LENGTH_SHORT).show()
            navigationLockViewModel.setLocked(false)
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
