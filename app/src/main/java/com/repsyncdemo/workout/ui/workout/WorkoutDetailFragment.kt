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

class WorkoutDetailFragment : Fragment() {

    private var _binding: FragmentWorkoutDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WorkoutViewModel by activityViewModels()

    private lateinit var exerciseAdapter: ExerciseAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkoutDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        exerciseAdapter = ExerciseAdapter()

        binding.rvExercises.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = exerciseAdapter
        }

        val workoutId = arguments?.getString("workoutId") ?: return
        viewModel.loadWorkout(workoutId)

        viewModel.selectedWorkout.observe(viewLifecycleOwner) { workout ->
            workout?.let {
                binding.tvName.text = it.name
                binding.tvDescription.text = it.description
                exerciseAdapter.submitList(it.exercises)
            }
        }

        binding.btnStartWorkout.setOnClickListener {
            val bundle = Bundle().apply { putString("workoutId", workoutId) }
            findNavController().navigate(R.id.action_workoutDetail_to_logWorkout, bundle)
        }

        binding.btnDelete.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setMessage(R.string.confirm_delete)
                .setPositiveButton(R.string.yes) { _, _ ->
                    viewModel.deleteWorkout(workoutId)
                    Toast.makeText(requireContext(), "Workout deleted", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
                .setNegativeButton(R.string.no, null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
