package com.repsyncdemo.workout.ui.workout

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.data.ExerciseDatabase
import com.repsyncdemo.workout.data.model.Exercise
import com.repsyncdemo.workout.data.model.FeedPost
import com.repsyncdemo.workout.data.model.FeedPostType
import com.repsyncdemo.workout.data.model.Workout
import com.repsyncdemo.workout.databinding.FragmentCreateWorkoutBinding
import com.repsyncdemo.workout.ui.adapter.ExerciseInputAdapter
import com.repsyncdemo.workout.ui.adapter.ExerciseLibraryAdapter
import com.repsyncdemo.workout.viewmodel.FeedViewModel
import com.repsyncdemo.workout.viewmodel.ProfileViewModel
import com.repsyncdemo.workout.viewmodel.WorkoutViewModel

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

        profileViewModel.loadProfile()

        exerciseInputAdapter = ExerciseInputAdapter()

        binding.rvExercises.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = exerciseInputAdapter
        }

        exerciseInputAdapter.addExercise()

        binding.btnAddExercise.setOnClickListener {
            exerciseInputAdapter.addExercise()
        }

        binding.btnPickExercise.setOnClickListener {
            showExercisePickerDialog()
        }

        binding.btnSave.setOnClickListener {
            saveWorkout()
        }

        workoutViewModel.operationResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess { workoutId ->
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
                findNavController().popBackStack()
            }
            result.onFailure { e ->
                Toast.makeText(requireContext(), e.message ?: "Save failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showExercisePickerDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.fragment_exercise_library, null)

        val etSearch = dialogView.findViewById<EditText>(R.id.etSearch)
        val rvExercises = dialogView.findViewById<RecyclerView>(R.id.rvExercises)

        val pickerAdapter = ExerciseLibraryAdapter { exerciseDef ->
            exerciseInputAdapter.addExerciseFromLibrary(exerciseDef.name)
        }

        rvExercises.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = pickerAdapter
        }

        pickerAdapter.submitList(ExerciseDatabase.allExercises)

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                val filtered = if (query.isEmpty()) ExerciseDatabase.allExercises
                else ExerciseDatabase.filter(searchQuery = query)
                pickerAdapter.submitList(filtered)
            }
        })

        AlertDialog.Builder(requireContext())
            .setTitle("Pick Exercise")
            .setView(dialogView)
            .setNegativeButton("Done", null)
            .show()
    }

    private fun saveWorkout() {
        val name = binding.etName.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()

        if (name.isEmpty()) {
            binding.etName.error = "Name is required"
            return
        }

        val exercises = exerciseInputAdapter.getExercises().filter { it.name.isNotEmpty() }

        if (exercises.isEmpty()) {
            Toast.makeText(requireContext(), "Add at least one exercise", Toast.LENGTH_SHORT).show()
            return
        }

        val workout = Workout(
            name = name,
            description = description,
            exercises = exercises,
            isPublic = binding.switchPublic.isChecked
        )

        workoutViewModel.addWorkout(workout)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
