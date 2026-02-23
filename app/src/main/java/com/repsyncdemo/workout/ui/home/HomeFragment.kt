package com.repsyncdemo.workout.ui.home

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.databinding.FragmentHomeBinding
import com.repsyncdemo.workout.ui.adapter.HistoryAdapter
import com.repsyncdemo.workout.ui.adapter.WorkoutAdapter
import com.repsyncdemo.workout.viewmodel.AuthViewModel
import com.repsyncdemo.workout.viewmodel.ProfileViewModel
import com.repsyncdemo.workout.viewmodel.WorkoutViewModel
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val workoutViewModel: WorkoutViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()
    private val profileViewModel: ProfileViewModel by activityViewModels()

    private lateinit var workoutAdapter: WorkoutAdapter
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        workoutAdapter = WorkoutAdapter { workout ->
            val bundle = Bundle().apply { putString("workoutId", workout.id) }
            findNavController().navigate(R.id.action_home_to_workoutDetail, bundle)
        }
        
        historyAdapter = HistoryAdapter { log ->
            val bundle = Bundle().apply { 
                putString("workoutId", log.workoutId)
                putString("logId", log.id)
            }
            findNavController().navigate(R.id.logWorkoutFragment, bundle)
        }

        binding.rvHomeContent.apply {
            layoutManager = LinearLayoutManager(requireContext())
        }

        binding.layoutGoals.setOnClickListener {
            findNavController().navigate(R.id.goalsFragment)
        }

        binding.layoutCalendar.setOnClickListener {
            findNavController().navigate(R.id.historyFragment)
        }

        binding.btnStartWorkout.setOnClickListener {
            showStartWorkoutDialog()
        }

        binding.homeTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                updateHomeContent(tab?.position ?: 0)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Initialize with "Recent" (History)
        updateHomeContent(0)

        profileViewModel.currentProfile.observe(viewLifecycleOwner) { profile ->
            val username = profile?.username ?: ""
            workoutViewModel.workoutLogs.observe(viewLifecycleOwner) { logs ->
                val last30Days = Calendar.getInstance()
                last30Days.add(Calendar.DAY_OF_YEAR, -30)
                val count = logs.count { it.completedAt >= last30Days.timeInMillis }
                binding.tvCongrats.text = "Congrats $username! You worked out $count times in the last 30 days"
            }
        }
    }

    private fun showStartWorkoutDialog() {
        val options = arrayOf("Start Saved Workout", "Create New Workout")
        AlertDialog.Builder(requireContext())
            .setTitle("Start Workout")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        binding.homeTabs.getTabAt(1)?.select()
                    }
                    1 -> {
                        findNavController().navigate(R.id.action_home_to_createWorkout)
                    }
                }
            }
            .show()
    }

    private fun updateHomeContent(position: Int) {
        if (position == 0) {
            // Recent Tab (History Logs)
            binding.rvHomeContent.adapter = historyAdapter
            workoutViewModel.workoutLogs.observe(viewLifecycleOwner) { logs ->
                if (binding.homeTabs.selectedTabPosition == 0) {
                    val sortedLogs = logs.sortedByDescending { it.completedAt }.take(10)
                    historyAdapter.submitList(sortedLogs)
                    binding.tvEmpty.visibility = if (sortedLogs.isEmpty()) View.VISIBLE else View.GONE
                    binding.tvEmpty.text = "No recent workouts logged."
                }
            }
        } else {
            // Saved Tab (Workout Templates)
            binding.rvHomeContent.adapter = workoutAdapter
            workoutViewModel.workouts.observe(viewLifecycleOwner) { workouts ->
                if (binding.homeTabs.selectedTabPosition == 1) {
                    workoutAdapter.submitList(workouts)
                    binding.tvEmpty.visibility = if (workouts.isEmpty()) View.VISIBLE else View.GONE
                    binding.tvEmpty.text = "No saved workouts found."
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
