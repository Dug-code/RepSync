package com.repsyncdemo.workout.ui.home

/**
 * File overview: Home dashboard that combines the activity header, quick actions, home tabs, rest-day logging, and start-workout flow.
 */

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.databinding.FragmentHomeBinding
import com.repsyncdemo.workout.viewmodel.ProfileViewModel
import com.repsyncdemo.workout.viewmodel.WorkoutViewModel
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.*
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val workoutViewModel: WorkoutViewModel by activityViewModels()
    private val profileViewModel: ProfileViewModel by activityViewModels()

    private var logoClickCount = 0
    private var lastClickTime: Long = 0

    // Sets up this screen.
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Connects views, clicks, and data.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewPager()
        setupListeners()
        observeData()
        setupEasterEgg()
    }

    // Sets up this section.
    private fun setupViewPager() {
        val adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 2
            override fun createFragment(position: Int): Fragment {
                return if (position == 0) HomeRecentFragment() else HomeSavedFragment()
            }
        }
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.homeTabs, binding.viewPager) { tab, position ->
            tab.text = if (position == 0) "Logs" else "Templates"
        }.attach()
    }

    fun showTemplatesTab() {
        binding.viewPager.setCurrentItem(1, true)
    }

    // Sets up this section.
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

        binding.btnRestDay.setOnClickListener {
            showRestDayConfirmation()
        }
    }

    // Sets up this section.
    private fun setupEasterEgg() {
        binding.ivLogo.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime < 1000) { // Must click within 1 second of last click
                logoClickCount++
            } else {
                logoClickCount = 1
            }
            lastClickTime = currentTime

            if (logoClickCount >= 5) {
                triggerConfetti()
                logoClickCount = 0
            }
        }
    }

    private fun triggerConfetti() {
        val party = Party(
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            spread = 360,
            colors = listOf(0xE31E24, 0xfce18a, 0xff726d, 0xf4306d, 0xb48def),
            position = Position.Relative(0.5, -0.1),
            emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100)
        )
        binding.konfettiView.start(party)
    }

    // Shows a dialog or popup.
    private fun showRestDayConfirmation() {
        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setTitle("Log Rest Day")
            .setMessage("Ready to take a break? This will mark today as a rest day on your calendar and count towards your Recovery trophy.")
            .setPositiveButton("Confirm") { _, _ ->
                profileViewModel.incrementRestDays()
                workoutViewModel.addRestDay()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Watches data and updates the UI.
    private fun observeData() {
        profileViewModel.myProfile.observe(viewLifecycleOwner) {
            updateCongratsMessage()
        }
        
        workoutViewModel.workoutLogs.observe(viewLifecycleOwner) {
            updateCongratsMessage()
        }
    }

    // Updates data or UI state.
    private fun updateCongratsMessage() {
        val profile = profileViewModel.myProfile.value
        val logs = workoutViewModel.workoutLogs.value
        
        val username = profile?.username ?: ""
        val greeting = if (username.isNotEmpty()) "Nice work, $username" else "Nice work"
        
        if (logs != null) {
            val thirtyDaysAgo = Calendar.getInstance().apply { 
                add(Calendar.DAY_OF_YEAR, -30) 
            }.timeInMillis
            
            val count = logs.count { it.completedAt >= thirtyDaysAgo }
            val workoutText = if (count == 1) "workout" else "workouts"
            binding.tvCongrats.text = greeting
            binding.tvWorkoutCount.text = "$count $workoutText in the last 30 days"
            
            // Update motivational message
            binding.tvMotivationalMessage.text = when {
                count == 0 -> "Time to lock in."
                count in 1..4 -> "Keep up the momentum."
                count in 5..9 -> "Consistency is key."
                else -> "Keep crushing it."
            }
        } else {
            binding.tvCongrats.text = if (username.isNotEmpty()) "Nice work, $username" else "Nice work"
            binding.tvWorkoutCount.text = "Checking your 30-day activity..."
            binding.tvMotivationalMessage.text = "Keep up the momentum."
        }
    }

    // Shows a dialog or popup.
    private fun showStartWorkoutDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_start_workout, null)
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setView(dialogView)
            .create()

        dialogView.findViewById<View>(R.id.btnSavedWorkout).setOnClickListener {
            binding.homeTabs.getTabAt(1)?.select()
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.btnCreateNew).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_createWorkout)
            dialog.dismiss()
        }

        dialog.show()
    }

    // Clears the view binding.
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
