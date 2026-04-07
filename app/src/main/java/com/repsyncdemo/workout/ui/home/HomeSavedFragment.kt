package com.repsyncdemo.workout.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.databinding.LayoutTabListBinding
import com.repsyncdemo.workout.ui.adapter.WorkoutAdapter
import com.repsyncdemo.workout.viewmodel.WorkoutViewModel

class HomeSavedFragment : Fragment() {
    private var _binding: LayoutTabListBinding? = null
    private val binding get() = _binding!!
    private val workoutViewModel: WorkoutViewModel by activityViewModels()
    private lateinit var workoutAdapter: WorkoutAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = LayoutTabListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        workoutAdapter = WorkoutAdapter { workout ->
            val bundle = Bundle().apply { putString("workoutId", workout.id) }
            findNavController().navigate(R.id.action_home_to_workoutDetail, bundle)
        }

        binding.rvContent.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = workoutAdapter
        }

        workoutViewModel.workouts.observe(viewLifecycleOwner) { workouts ->
            workoutAdapter.submitList(workouts)
            val isEmpty = workouts.isEmpty()
            binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.tvEmptyTitle.text = "No saved routines"
            binding.tvEmptySubtitle.text = "Create a custom workout plan to stay consistent."
            binding.btnEmptyAction.text = "Create Routine"
            binding.btnEmptyAction.setOnClickListener {
                findNavController().navigate(R.id.action_home_to_createWorkout)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
