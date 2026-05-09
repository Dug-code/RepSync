package com.repsyncdemo.workout.ui.home

import android.content.Context
import android.graphics.Color.argb
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.TextView
import androidx.core.content.edit
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
import com.takusemba.spotlight.OnSpotlightListener
import com.takusemba.spotlight.Spotlight
import com.takusemba.spotlight.Target
import com.takusemba.spotlight.effet.FlickerEffect
import com.takusemba.spotlight.shape.Circle
import com.takusemba.spotlight.shape.RoundedRectangle
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.Calendar
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val workoutViewModel: WorkoutViewModel by activityViewModels()
    private val profileViewModel: ProfileViewModel by activityViewModels()

    private var logoClickCount = 0
    private var lastClickTime: Long = 0
    private var spotlight: Spotlight? = null

    companion object {
        private const val PREFS_NAME = "repsync_prefs"
        private const val KEY_HOME_TUTORIAL_COMPLETED = "home_tutorial_completed"
    }

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

        setupViewPager()
        setupListeners()
        observeData()
        setupEasterEgg()

        //Tutorial
        checkTutorial()
    }

    /** ------------- Tutorial Code Begins ---------------- **/

    private fun checkTutorial() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        //set to false for testing
        val isCompleted = prefs.getBoolean(KEY_HOME_TUTORIAL_COMPLETED, false)
        if (!isCompleted) {
            binding.root.post {
                showTutorial()
            }
        }
    }

    private fun showTutorial() {
        val targets = ArrayList<Target>()

        // Recent Tab
        val recentTab = binding.homeTabs.getTabAt(0)?.view
        recentTab?.let {
            targets.add(createTarget(it, "Recent Workouts", "Quickly access your most recent training sessions and see your progress."))
        }

        // Saved Tab
        val savedTab = binding.homeTabs.getTabAt(1)?.view
        savedTab?.let {
            targets.add(createTarget(it, "Saved Workouts", "See your stored workouts and pick your favorite routines here for easy access."))
        }

        // Start Workout Button
        targets.add(
            Target.Builder()
                .setAnchor(binding.btnStartWorkout)
                .setShape(RoundedRectangle(binding.btnStartWorkout.height.toFloat(), binding.btnStartWorkout.width.toFloat(), 16f))
                .setOverlay(createOverlay("Start Training", "Ready to hit the gym? Tap here to start a new workout or pick a saved one."))
                .build()
        )

        // Rest Day Button
        targets.add(
            Target.Builder()
                .setAnchor(binding.btnRestDay)
                .setShape(Circle(binding.btnRestDay.height.toFloat() / 2 + 20f))
                .setOverlay(createOverlay("Log Recovery", "Recovery is just as important as training. Log your rest days to keep your streak!"))
                .build()
        )

        // Calendar Button
        targets.add(
            Target.Builder()
                .setAnchor(binding.layoutCalendar)
                .setShape(Circle(binding.layoutCalendar.height.toFloat() / 2 + 10f))
                .setOverlay(createOverlay("Calendar", "View your workouts and rest consistency over time in the calendar."))
                .build()
        )

        // Goals Button
        targets.add(
            Target.Builder()
                .setAnchor(binding.layoutGoals)
                .setShape(Circle(binding.layoutGoals.height.toFloat() / 2 + 10f))
                .setEffect(FlickerEffect(100f, argb(255,68,71,78)))
                .setOverlay(goalCreateOverlay("Goals", "Set and track personal milestones like PRs and weight targets.\n\n Lets set your first goal click on the flashing icon."))
                .build()
        )

        spotlight = Spotlight.Builder(requireActivity())
            .setTargets(targets)
            .setBackgroundColorRes(R.color.spotlight_background)
            .setDuration(400L)
            .setAnimation(DecelerateInterpolator(2f))
            .setOnSpotlightListener(object : OnSpotlightListener {
                override fun onStarted() {}
                override fun onEnded() {
                    markTutorialCompleted()
                }
            })
            .build()

        spotlight?.start()
    }

    private fun createTarget(view: View, title: String, description: String): Target {
        return Target.Builder()
            .setAnchor(view)
            .setShape(RoundedRectangle(view.height.toFloat(), view.width.toFloat(), 8f))
            .setOverlay(createOverlay(title, description))
            .build()
    }

    private fun createOverlay(title: String, description: String): View {
        val overlay = layoutInflater.inflate(R.layout.layout_spotlight_overlay, binding.root, false)
        overlay.findViewById<TextView>(R.id.tvTitle).text = title
        overlay.findViewById<TextView>(R.id.tvDescription).text = description

        overlay.findViewById<Button>(R.id.btnNext).setOnClickListener {
            spotlight?.next()
        }

        overlay.findViewById<Button>(R.id.btnSkip).setOnClickListener {
            spotlight?.finish()
            markTutorialCompleted()
        }
        
        return overlay
    }

    private fun goalCreateOverlay(title: String, description: String): View {
        val overlay = layoutInflater.inflate(R.layout.layout_spotlight_overlay, binding.root, false)
        overlay.findViewById<TextView>(R.id.tvTitle).text = title
        overlay.findViewById<TextView>(R.id.tvDescription).text = description

        overlay.findViewById<Button>(R.id.btnNext).visibility = View.GONE

        overlay.findViewById<Button>(R.id.btnSkip).setOnClickListener {
            spotlight?.finish()
            markTutorialCompleted()
        }

        return overlay
    }

    private fun markTutorialCompleted() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_HOME_TUTORIAL_COMPLETED, true) }
    }

    /** ------------- Tutorial Code Ends ---------------- **/

    fun showTemplatesTab() {
        binding.viewPager.currentItem = 1
    }

    private fun setupViewPager() {
        val adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 2
            override fun createFragment(position: Int): Fragment {
                return if (position == 0) HomeRecentFragment() else HomeSavedFragment()
            }
        }
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.homeTabs, binding.viewPager) { tab, position ->
            tab.text = if (position == 0) "Recent" else "Saved"
        }.attach()
    }

    private fun setupListeners() {
        binding.layoutGoals.setOnClickListener {
            findNavController().navigate(R.id.goalsFragment)
            spotlight?.finish()
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

    private fun observeData() {
        profileViewModel.myProfile.observe(viewLifecycleOwner) {
            updateCongratsMessage()
        }
        
        workoutViewModel.workoutLogs.observe(viewLifecycleOwner) {
            updateCongratsMessage()
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
            val timeText = if (count == 1) "time" else "times"
            binding.tvCongrats.text = "$greeting You worked out $count $timeText in the last 30 days"
            
            // Update motivational message
            binding.tvMotivationalMessage.text = when {
                count == 0 -> "Time to lock in!"
                count in 1..4 -> "Keep up the momentum!"
                count in 5..9 -> "Consistency is key!"
                else -> "Keep crushing it!"
            }
        } else {
            binding.tvCongrats.text = if (username.isNotEmpty()) "Congrats $username! Checking your progress..." else "Loading your progress..."
            binding.tvMotivationalMessage.text = "Keep up the momentum!"
        }
    }

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
