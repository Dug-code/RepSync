package com.repsyncdemo.workout.ui.workout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.databinding.FragmentWorkoutDetailBinding
import com.repsyncdemo.workout.ui.adapter.ExerciseAdapter
import com.repsyncdemo.workout.viewmodel.WorkoutViewModel

/**
 * Fragment that displays the details of a specific workout.
 * It shows the workout name, description, and the list of exercises included.
 * Users can start a session based on this workout or delete the workout.
 */
class WorkoutDetailFragment : Fragment() {

    // View Binding for accessing UI elements safely
    private var _binding: FragmentWorkoutDetailBinding? = null
    private val binding get() = _binding!!

    // Shared ViewModel to fetch and manage workout data
    private val viewModel: WorkoutViewModel by activityViewModels()

    // Adapter for the list of exercises
    private lateinit var exerciseAdapter: ExerciseAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment using View Binding
        _binding = FragmentWorkoutDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize the exercise list adapter
        exerciseAdapter = ExerciseAdapter()

        // Setup RecyclerView with a linear layout and the adapter
        binding.rvExercises.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = exerciseAdapter
        }

        // Retrieve the workout ID from navigation arguments
        val workoutId = arguments?.getString("workoutId") ?: return
        
        // Request the ViewModel to load data for this specific workout
        viewModel.loadWorkout(workoutId)

        // Observe the selected workout state and update the UI when it changes
        viewModel.selectedWorkout.observe(viewLifecycleOwner) { workout ->
            workout?.let {
                binding.tvName.text = it.name
                binding.tvDescription.text = it.description
                // Update the exercise list in the adapter
                exerciseAdapter.submitList(it.exercises)
            }
        }

        // Navigate to the workout logging screen
        binding.btnStartWorkout.setOnClickListener {
            val bundle = Bundle().apply { putString("workoutId", workoutId) }
            findNavController().navigate(R.id.action_workoutDetail_to_logWorkout, bundle)
        }

        // Handle workout deletion with a confirmation dialog
        binding.btnDelete.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setMessage(R.string.confirm_delete)
                .setPositiveButton(R.string.yes) { _, _ ->
                    viewModel.deleteWorkout(workoutId)
                    Toast.makeText(requireContext(), "Workout deleted", Toast.LENGTH_SHORT).show()
                    // Return to the previous screen after deletion
                    findNavController().popBackStack()
                }
                .setNegativeButton(R.string.no, null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear binding reference to avoid memory leaks
        _binding = null
    }
}
