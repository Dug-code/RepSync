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
import com.repsyncdemo.workout.viewmodel.ProfileViewModel
import com.repsyncdemo.workout.viewmodel.WorkoutViewModel
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val workoutViewModel: WorkoutViewModel by activityViewModels()
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

        setupAdapters()
        setupListeners()
        setupTabs()
        observeData()
    }

    private fun setupAdapters() {
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
    }

    private fun setupListeners() {
        binding.layoutGoals.setOnClickListener {
            findNavController().navigate(R.id.goalsFragment)
        }

        binding.layoutCalendar.setOnClickListener {
            findNavController().navigate(R.id.historyFragment)
        }

        binding.btnStartWorkout.setOnClickListener {
            showStartWorkoutDialog()
        }
    }

    private fun setupTabs() {
        binding.homeTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                refreshTabContent()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun observeData() {
        // Observe profile for real-time username updates
        profileViewModel.myProfile.observe(viewLifecycleOwner) {
            updateCongratsMessage()
        }
        
        // Observe logs for workout count and list content
        workoutViewModel.workoutLogs.observe(viewLifecycleOwner) {
            updateCongratsMessage()
            refreshTabContent()
        }

        // Observe workouts for "Saved" tab
        workoutViewModel.workouts.observe(viewLifecycleOwner) {
            refreshTabContent()
        }
    }

    private fun updateCongratsMessage() {
        val profile = profileViewModel.myProfile.value
        val logs = workoutViewModel.workoutLogs.value
        
        val username = profile?.username ?: ""
        val greeting = if (username.isNotEmpty()) "Congrats $username!" else "Congrats!"
        
        if (logs != null) {
            val thirtyDaysAgo = Calendar.getInstance().apply { 
                add(Calendar.DAY_OF_YEAR, -30) 
            }.timeInMillis
            
            val count = logs.count { it.completedAt >= thirtyDaysAgo }
            binding.tvCongrats.text = "$greeting You worked out $count times in the last 30 days"
        } else {
            // Initial placeholder while logs fetch
            binding.tvCongrats.text = if (username.isNotEmpty()) "Congrats $username! Checking your progress..." else "Loading your progress..."
        }
    }

    private fun refreshTabContent() {
        val position = binding.homeTabs.selectedTabPosition
        val logs = workoutViewModel.workoutLogs.value ?: emptyList()
        val workouts = workoutViewModel.workouts.value ?: emptyList()

        if (position == 0) {
            // Recent Tab
            binding.rvHomeContent.adapter = historyAdapter
            val sortedLogs = logs.take(10)
            historyAdapter.submitList(sortedLogs)
            binding.tvEmpty.visibility = if (sortedLogs.isEmpty()) View.VISIBLE else View.GONE
            binding.tvEmpty.text = "No recent workouts logged."
        } else {
            // Saved Tab
            binding.rvHomeContent.adapter = workoutAdapter
            workoutAdapter.submitList(workouts)
            binding.tvEmpty.visibility = if (workouts.isEmpty()) View.VISIBLE else View.GONE
            binding.tvEmpty.text = "No saved workouts found."
        }
    }

    private fun showStartWorkoutDialog() {
        val options = arrayOf("Start Saved Workout", "Create New Workout")
        AlertDialog.Builder(requireContext())
            .setTitle("Start Workout")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> binding.homeTabs.getTabAt(1)?.select()
                    1 -> findNavController().navigate(R.id.action_home_to_createWorkout)
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
