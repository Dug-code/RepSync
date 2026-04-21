package com.repsyncdemo.workout.ui.workout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.data.model.Workout
import com.repsyncdemo.workout.databinding.FragmentWorkoutDetailBinding
import com.repsyncdemo.workout.ui.adapter.ExerciseAdapter
import com.repsyncdemo.workout.viewmodel.WorkoutViewModel

/**
 * Fragment that displays the details of a specific workout.
 * It shows the workout name, description, and the list of exercises included.
 * Users can start a session based on this workout or delete the workout.
 */
class WorkoutDetailFragment : Fragment() {

    private var _binding: FragmentWorkoutDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WorkoutViewModel by activityViewModels()
    private lateinit var exerciseAdapter: ExerciseAdapter
    private var currentWorkout: Workout? = null

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
                currentWorkout = it
                binding.tvName.text = it.name
                binding.tvDescription.text = it.description
                exerciseAdapter.submitList(it.exercises)
                
                // Update privacy icon based on state in real-time
                updatePrivacyIcon(it.isPublic)
            }
        }

        binding.btnEdit.setOnClickListener {
            val bundle = Bundle().apply { putString("workoutId", workoutId) }
            findNavController().navigate(R.id.createWorkoutFragment, bundle)
        }

        binding.btnPrivacyMenu.setOnClickListener { view ->
            showPrivacyPopupMenu(view)
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

    private fun updatePrivacyIcon(isPublic: Boolean) {
        val iconRes = if (isPublic) R.drawable.ic_public else R.drawable.ic_private
        binding.ivPrivacyIcon.setImageResource(iconRes)
    }

    private fun showPrivacyPopupMenu(view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.menu_workout_privacy, popup.menu)
        
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_public -> {
                    updateWorkoutPrivacy(true)
                    true
                }
                R.id.action_private -> {
                    updateWorkoutPrivacy(false)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun updateWorkoutPrivacy(isPublic: Boolean) {
        currentWorkout?.let {
            val updated = it.copy(isPublic = isPublic)
            viewModel.updateWorkout(updated)
            // The LiveData observer in onViewCreated will catch this update and call updatePrivacyIcon automatically
            Toast.makeText(requireContext(), if (isPublic) "Workout is now Public" else "Workout is now Private", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
