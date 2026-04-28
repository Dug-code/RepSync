package com.repsyncdemo.workout.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.databinding.LayoutTabListBinding
import com.repsyncdemo.workout.ui.adapter.WorkoutAdapter
import com.repsyncdemo.workout.viewmodel.ProfileViewModel
import com.repsyncdemo.workout.viewmodel.WorkoutViewModel

class ProfileWorkoutsFragment : Fragment() {
    private var _binding: LayoutTabListBinding? = null
    private val binding get() = _binding!!
    
    private val profileViewModel: ProfileViewModel by activityViewModels()
    private val workoutViewModel: WorkoutViewModel by activityViewModels()
    
    private lateinit var workoutAdapter: WorkoutAdapter
    private var targetUserId: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = LayoutTabListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        targetUserId = arguments?.getString("userId")

        workoutAdapter = WorkoutAdapter { workout ->
            if (targetUserId != null) {
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Copy Routine Template")
                    .setMessage("Do you want to copy this routine template to your collection?")
                    .setPositiveButton("Copy") { _, _ ->
                        workoutViewModel.copyWorkout(workout)
                        Toast.makeText(requireContext(), "Template copied!", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                val bundle = Bundle().apply { putString("workoutId", workout.id) }
                findNavController().navigate(R.id.workoutDetailFragment, bundle)
            }
        }

        binding.rvContent.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = workoutAdapter
        }

        val profile = profileViewModel.currentProfile.value
        val isWorkoutsPublic = profile?.isWorkoutsPublic ?: true
        
        if (targetUserId != null && !isWorkoutsPublic) {
            workoutAdapter.submitList(emptyList())
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.tvEmptyTitle.text = "Private Routine Templates"
            binding.tvEmptySubtitle.text = "This user's routine templates are private."
            binding.btnEmptyAction.visibility = View.GONE
        } else {
            val workoutsSource = if (targetUserId != null) workoutViewModel.targetUserWorkouts else workoutViewModel.workouts
            workoutsSource.observe(viewLifecycleOwner) { workouts ->
                // Fix: Only show PUBLIC workouts on the profile, regardless of who is viewing.
                val sharedWorkouts = workouts.filter { it.isPublic }
                workoutAdapter.submitList(sharedWorkouts)
                
                val isEmpty = sharedWorkouts.isEmpty()
                binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
                
                if (targetUserId != null) {
                    binding.tvEmptyTitle.text = "No shared templates."
                    binding.tvEmptySubtitle.text = "This user hasn't made any routines public yet."
                } else {
                    binding.tvEmptyTitle.text = "Nothing shared yet."
                    binding.tvEmptySubtitle.text = "Mark your favorite routines as 'Public' to show them off here!"
                }
                binding.btnEmptyAction.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
