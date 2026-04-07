package com.repsyncdemo.workout.ui.workout

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.data.model.Workout
import com.repsyncdemo.workout.databinding.FragmentWorkoutListBinding
import com.repsyncdemo.workout.ui.adapter.WorkoutAdapter
import com.repsyncdemo.workout.viewmodel.WorkoutViewModel

class WorkoutListFragment : Fragment() {

    private var _binding: FragmentWorkoutListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WorkoutViewModel by activityViewModels()

    private lateinit var workoutAdapter: WorkoutAdapter
    private var allWorkouts: List<Workout> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkoutListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        workoutAdapter = WorkoutAdapter { workout ->
            val bundle = Bundle().apply { putString("workoutId", workout.id) }
            findNavController().navigate(R.id.action_workoutList_to_workoutDetail, bundle)
        }

        binding.rvWorkouts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = workoutAdapter
        }

        binding.fabAdd.setOnClickListener {
            findNavController().navigate(R.id.action_workoutList_to_createWorkout)
        }

        binding.btnEmptyAction.setOnClickListener {
            findNavController().navigate(R.id.action_workoutList_to_createWorkout)
        }

        setupSearch()

        viewModel.workouts.observe(viewLifecycleOwner) { workouts ->
            allWorkouts = workouts
            filterWorkouts(binding.etSearch.text.toString())
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterWorkouts(s.toString())
            }
        })
    }

    private fun filterWorkouts(query: String) {
        val filteredList = if (query.isEmpty()) {
            allWorkouts
        } else {
            allWorkouts.filter { it.name.contains(query, ignoreCase = true) }
        }
        
        workoutAdapter.submitList(filteredList)
        
        val isEmpty = filteredList.isEmpty()
        binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvWorkouts.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
