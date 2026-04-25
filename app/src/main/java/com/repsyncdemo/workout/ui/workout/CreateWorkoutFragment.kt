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
import com.repsyncdemo.workout.util.DragToReorderCallBack
import com.repsyncdemo.workout.viewmodel.FeedViewModel
import com.repsyncdemo.workout.viewmodel.NavigationLockViewModel
import com.repsyncdemo.workout.viewmodel.ProfileViewModel
import com.repsyncdemo.workout.viewmodel.WorkoutViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CreateWorkoutFragment : Fragment() {

    private var _binding: FragmentCreateWorkoutBinding? = null
    private val binding get() = _binding!!

    private val workoutViewModel: WorkoutViewModel by activityViewModels()
    private val feedViewModel: FeedViewModel by activityViewModels()
    private val profileViewModel: ProfileViewModel by activityViewModels()
    private val navigationLockViewModel: NavigationLockViewModel by activityViewModels()

    private var exerciseInputAdapter: ExerciseInputAdapter? = null
    private var existingWorkoutId: String? = null
    private var isPublic: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (exerciseInputAdapter == null) {
            exerciseInputAdapter = ExerciseInputAdapter(onDataChanged = {
                updateLockState()
                updateTopIcons()
            })
        }
    }

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

        // Handle Back Navigation
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
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
        })

        setupChangeListeners()

        profileViewModel.loadProfile()

        binding.rvExercises.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = exerciseInputAdapter
            // Disable animations to prevent the "flipping" or "flash" effect during reordering
            itemAnimator = null
        }

        val dragHandler = DragToReorderCallBack(
            onItemMove = { fromPosition, toPosition ->
                exerciseInputAdapter?.moveExercise(fromPosition, toPosition)
            },
            onDragFinished = {
                updateLockState()
                updateTopIcons()
            }
        )
        ItemTouchHelper(dragHandler).attachToRecyclerView(binding.rvExercises)

        if (existingWorkoutId != null) {
            if (!workoutViewModel.isWorkoutDataLoaded) {
                workoutViewModel.loadWorkout(existingWorkoutId!!)
            }
            binding.btnSave.text = "Update Workout"
        } else {
            if (exerciseInputAdapter?.itemCount == 0) {
                exerciseInputAdapter?.addExercise()
            }
        }

        workoutViewModel.selectedWorkout.observe(viewLifecycleOwner) { workout ->
            if (existingWorkoutId != null && workout != null && workout.id == existingWorkoutId && !workoutViewModel.isWorkoutDataLoaded) {
                binding.etName.setText(workout.name)
                binding.etDescription.setText(workout.description)
                isPublic = workout.isPublic
                updatePrivacyIcon()
                
                exerciseInputAdapter?.clearItems()
                workout.exercises.forEach { exercise ->
                    exerciseInputAdapter?.addExerciseFromObject(exercise)
                }
                workoutViewModel.notifyWorkoutLoaded()
                updateTopIcons()
            }
        }

        binding.btnPrivacyMenu.setOnClickListener { showPrivacyPopupMenu(it) }
        binding.btnAddExercise.setOnClickListener { findNavController().navigate(R.id.createCustomExerciseFragment) }
        binding.btnPickExercise.setOnClickListener { findNavController().navigate(R.id.exerciseLibraryFragment) }
        binding.btnSave.setOnClickListener { saveWorkout() }

        // Observer for library selection
        workoutViewModel.selectedExerciseEvent.observe(viewLifecycleOwner) { exerciseName ->
            if (!exerciseName.isNullOrEmpty()) {
                exerciseInputAdapter?.addExerciseFromLibrary(exerciseName)
                updateLockState()
                updateTopIcons()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                workoutViewModel.allLibraryExercises.collectLatest { exercises ->
                    exerciseInputAdapter?.updateLibrary(exercises)
                }
            }
        }
        
        workoutViewModel.operationResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                navigationLockViewModel.setLocked(false)
                Toast.makeText(requireContext(), "Workout saved!", Toast.LENGTH_SHORT).show()
                resetState()
                findNavController().popBackStack()
            }
            result.onFailure { e ->
                Toast.makeText(requireContext(), e.message ?: "Save failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resetState() {
        exerciseInputAdapter?.clearItems()
        workoutViewModel.clearSelection()
        existingWorkoutId = null
    }

    private fun showPrivacyPopupMenu(view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.menu_workout_privacy, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_public -> { isPublic = true; updatePrivacyIcon(); updateLockState(); true }
                R.id.action_private -> { isPublic = false; updatePrivacyIcon(); updateLockState(); true }
                else -> false
            }
        }
        popup.show()
    }

    private fun updatePrivacyIcon() {
        binding.ivPrivacyIcon.setImageResource(if (isPublic) R.drawable.ic_public else R.drawable.ic_private)
    }

    private fun updateTopIcons() {
        val exercises = exerciseInputAdapter?.getExercises() ?: emptyList()
        val types = exercises.map { it.type }.distinct()
        
        if (types.size > 1) {
            // It's a combination
            binding.ivStrengthIcon.visibility = View.GONE
            binding.ivCardioIcon.visibility = View.GONE
            binding.ivCalisthenicsIcon.visibility = View.GONE
            binding.tvComboLabel.visibility = View.VISIBLE
        } else {
            // It's all one type or empty
            binding.tvComboLabel.visibility = View.GONE
            binding.ivStrengthIcon.visibility = if (types.contains(ExerciseType.STRENGTH)) View.VISIBLE else View.GONE
            binding.ivCardioIcon.visibility = if (types.contains(ExerciseType.CARDIO)) View.VISIBLE else View.GONE
            binding.ivCalisthenicsIcon.visibility = if (types.contains(ExerciseType.CALISTHENICS)) View.VISIBLE else View.GONE
        }
    }

    private fun setupChangeListeners() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { updateLockState() }
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
        val exerciseCount = exerciseInputAdapter?.itemCount ?: 0
        
        return name.isNotEmpty() || description.isNotEmpty() || exerciseCount > 1 || 
               (exerciseCount == 1 && exerciseInputAdapter?.getExercises()?.get(0)?.name?.isNotEmpty() == true)
    }

    private fun showUnsavedChangesDialog(onDiscard: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setTitle("Discard Changes?")
            .setMessage("You have unsaved changes. Are you sure you want to discard them?")
            .setPositiveButton("Discard") { _, _ -> resetState(); onDiscard() }
            .setNegativeButton("Keep Editing", null)
            .show()
    }

    private fun saveWorkout() {
        val name = binding.etName.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        if (name.isEmpty()) { binding.etName.error = "Name is required"; return }
        val exercises = exerciseInputAdapter?.getExercises()?.filter { it.name.isNotEmpty() } ?: emptyList()
        if (exercises.isEmpty()) { Toast.makeText(requireContext(), "Add at least one exercise", Toast.LENGTH_SHORT).show(); return }

        val workout = Workout(id = existingWorkoutId ?: "", name = name, description = description, exercises = exercises, isPublic = isPublic)
        if (existingWorkoutId == null) workoutViewModel.addWorkout(workout)
        else {
            workoutViewModel.updateWorkout(workout)
            navigationLockViewModel.setLocked(false)
            resetState()
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
