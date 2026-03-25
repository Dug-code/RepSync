package com.repsyncdemo.workout.ui.workout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.repsyncdemo.workout.data.model.FeedPost
import com.repsyncdemo.workout.data.model.FeedPostType
import com.repsyncdemo.workout.data.model.Workout
import com.repsyncdemo.workout.databinding.FragmentCreateWorkoutBinding
import com.repsyncdemo.workout.ui.adapter.ExerciseInputAdapter
import com.repsyncdemo.workout.ui.dialogs.ShowExercisePickerDialog
import com.repsyncdemo.workout.util.DragToReorderCallBack
import com.repsyncdemo.workout.viewmodel.FeedViewModel
import com.repsyncdemo.workout.viewmodel.ProfileViewModel
import com.repsyncdemo.workout.viewmodel.WorkoutViewModel
import kotlinx.coroutines.launch

/**
 * Fragment responsible for creating a new workout.
 * It allows users to input a name, description, and a list of exercises.
 * Users can also choose to share the workout publicly to the feed.
 */
class CreateWorkoutFragment : Fragment() {

    private var _binding: FragmentCreateWorkoutBinding? = null
    private val binding get() = _binding!!

    private val workoutViewModel: WorkoutViewModel by activityViewModels()
    private val feedViewModel: FeedViewModel by activityViewModels()
    private val profileViewModel: ProfileViewModel by activityViewModels()

    private lateinit var exerciseInputAdapter: ExerciseInputAdapter

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

        // Initialize user profile to ensure username is available for sharing
        profileViewModel.loadProfile()

        // Setup RecyclerView for exercise inputs
        exerciseInputAdapter = ExerciseInputAdapter()

        binding.rvExercises.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = exerciseInputAdapter
        }

        //Initializes Drag controller to the RecyclerView
        val dragHandler = DragToReorderCallBack { fromPosition, toPosition ->
            exerciseInputAdapter.notifyItemMoved(
                fromPosition,
                toPosition
            )
        }

        //attaches drag controller to the RecyclerView
        val itemTouchHelper = ItemTouchHelper(dragHandler)
        itemTouchHelper.attachToRecyclerView(binding.rvExercises)



        // Add an initial empty exercise row
        exerciseInputAdapter.addExercise()

        // Button listeners
        binding.btnAddExercise.setOnClickListener {
            exerciseInputAdapter.addExercise()
        }

        //Click listener that passes info to the exercise picker dialog
        binding.btnPickExercise.setOnClickListener {
            ShowExercisePickerDialog().show(parentFragmentManager, "exercise_picker")
        }


        binding.btnSave.setOnClickListener {
            saveWorkout()
        }

        // Observe results of the save operation from the ViewModel
        workoutViewModel.operationResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess { workoutId ->
                // If the workout is public, create a feed post automatically
                if (binding.switchPublic.isChecked) {
                    val username = profileViewModel.currentProfile.value?.username ?: ""
                    val post = FeedPost(
                        username = username,
                        type = FeedPostType.WORKOUT_SHARED,
                        workoutId = workoutId,
                        workoutName = binding.etName.text.toString().trim(),
                        description = binding.etDescription.text.toString().trim()
                    )
                    feedViewModel.createPost(post)
                }
                Toast.makeText(requireContext(), "Workout saved!", Toast.LENGTH_SHORT).show()
                // Return to the previous screen on success
                findNavController().popBackStack()
            }
            result.onFailure { e ->
                Toast.makeText(requireContext(), e.message ?: "Save failed", Toast.LENGTH_SHORT).show()
            }
        }

        // Observe exercise selection from library
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                workoutViewModel.selectedExercises.collect { exerciseName ->
                    if (!exerciseName.isNullOrEmpty()) {
                        exerciseInputAdapter.addExerciseFromLibrary(exerciseName)
                        workoutViewModel.clearSelectedExercises() // Prevent re-triggering
                    }
                }
            }
        }
    }

    /**
     * Gathers input from the UI, validates it, and requests the ViewModel to save the workout.
     */
    private fun saveWorkout() {
        val name = binding.etName.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()

        // Basic validation for workout name
        if (name.isEmpty()) {
            binding.etName.error = "Name is required"
            return
        }

        // Extract exercises from the adapter, ignoring any that haven't been named
        val exercises = exerciseInputAdapter.getExercises().filter { it.name.isNotEmpty() }

        if (exercises.isEmpty()) {
            Toast.makeText(requireContext(), "Add at least one exercise", Toast.LENGTH_SHORT).show()
            return
        }

        // Construct the workout object
        val workout = Workout(
            name = name,
            description = description,
            exercises = exercises,
            isPublic = binding.switchPublic.isChecked
        )

        // Trigger the database save
        workoutViewModel.addWorkout(workout)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear binding reference to prevent memory leaks
        _binding = null
    }
}
