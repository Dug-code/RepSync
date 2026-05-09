package com.repsyncdemo.workout.ui.home

/**
 * File overview: Shows saved workout templates inside the Home tab and routes users into template details.
 */

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.data.model.Workout
import com.repsyncdemo.workout.databinding.LayoutTabListBinding
import com.repsyncdemo.workout.ui.adapter.WorkoutAdapter
import com.repsyncdemo.workout.viewmodel.WorkoutViewModel

class HomeSavedFragment : Fragment() {
    private var _binding: LayoutTabListBinding? = null
    private val binding get() = _binding!!
    private val workoutViewModel: WorkoutViewModel by activityViewModels()
    private lateinit var workoutAdapter: WorkoutAdapter
    private var pendingScrollWorkoutId: String? = null

    // Sets up this screen.
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = LayoutTabListBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Connects views, clicks, and data.
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

        findNavController().currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<String>("scrollToWorkoutId")
            ?.observe(viewLifecycleOwner) { workoutId ->
                (parentFragment as? HomeFragment)?.showTemplatesTab()
                pendingScrollWorkoutId = workoutId
                findNavController().currentBackStackEntry
                    ?.savedStateHandle
                    ?.remove<String>("scrollToWorkoutId")
                scrollToWorkoutIfVisible(workoutAdapter.currentList)
            }

        workoutViewModel.workouts.observe(viewLifecycleOwner) { workouts ->
            workoutAdapter.submitList(workouts) {
                scrollToWorkoutIfVisible(workouts)
            }
            val isEmpty = workouts.isEmpty()
            binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.tvEmptyTitle.text = "No routine templates yet"
            binding.tvEmptySubtitle.text = "Templates are reusable starting points. Each workout session can be changed as you log it."
            binding.btnEmptyAction.text = "Create Template"
            binding.btnEmptyAction.setOnClickListener {
                findNavController().navigate(R.id.action_home_to_createWorkout)
            }
        }
    }

    private fun scrollToWorkoutIfVisible(workouts: List<Workout>) {
        val workoutId = pendingScrollWorkoutId ?: return
        val position = workouts.indexOfFirst { it.id == workoutId }
        if (position != -1) {
            binding.rvContent.post {
                (binding.rvContent.layoutManager as? LinearLayoutManager)
                    ?.scrollToPositionWithOffset(position, 0)
            }
            pendingScrollWorkoutId = null
        }
    }

    // Clears the view binding.
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
