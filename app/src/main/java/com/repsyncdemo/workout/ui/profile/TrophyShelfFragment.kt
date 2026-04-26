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
import com.repsyncdemo.workout.data.model.ExerciseType
import com.repsyncdemo.workout.data.model.Trophy
import com.repsyncdemo.workout.data.model.TrophyType
import com.repsyncdemo.workout.data.model.WorkoutLog
import com.repsyncdemo.workout.databinding.FragmentTrophyShelfBinding
import com.repsyncdemo.workout.ui.adapter.TrophyAdapter
import com.repsyncdemo.workout.viewmodel.ProfileViewModel
import com.repsyncdemo.workout.viewmodel.SocialViewModel
import com.repsyncdemo.workout.viewmodel.WorkoutViewModel

/**
 * Fragment that displays the user's "Trophy Shelf".
 * It calculates and displays various achievements based on workout history, 
 * profile stats, and social connections.
 */
class TrophyShelfFragment : Fragment() {

    private var _binding: FragmentTrophyShelfBinding? = null
    private val binding get() = _binding!!
    private val workoutViewModel: WorkoutViewModel by activityViewModels()
    private val profileViewModel: ProfileViewModel by activityViewModels()
    private val socialViewModel: SocialViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrophyShelfBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize adapter with a click listener to "pin" a trophy to the user's public profile
        val trophyAdapter = TrophyAdapter { trophy ->
            profileViewModel.pinTrophy(trophy.id)
            Toast.makeText(requireContext(), "${trophy.name} pinned to profile!", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }

        binding.rvTrophies.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = trophyAdapter
        }

        // Observe all necessary data streams to refresh trophies in real-time
        profileViewModel.myProfile.observe(viewLifecycleOwner) { 
            refreshTrophies(trophyAdapter)
        }
        workoutViewModel.workoutLogs.observe(viewLifecycleOwner) { 
            refreshTrophies(trophyAdapter)
        }
        socialViewModel.friends.observe(viewLifecycleOwner) {
            refreshTrophies(trophyAdapter)
        }
    }

    /**
     * Aggregates data from workout logs and profile settings to calculate progress for each trophy.
     * This iterates through the entire workout history to find personal records (PRs) and totals.
     */
    private fun refreshTrophies(adapter: TrophyAdapter) {
        val logs = workoutViewModel.workoutLogs.value ?: emptyList()
        val profile = profileViewModel.myProfile.value
        val friendsCount = socialViewModel.friends.value?.size ?: 0
        val restDayCount = profile?.totalRestDays ?: 0

        // Local variables to accumulate statistics from the user's history
        var totalVolume = 0.0
        var benchMax = 0.0
        var squatMax = 0.0
        var deadliftMax = 0.0
        var overheadMax = 0.0
        var cardioMinutes = 0
        var dumbbellMax = 0.0
        var absMinutes = 0
        var totalTimeMinutes = 0
        var totalPushups = 0
        var totalPullups = 0
        var totalSitups = 0

        // Process every workout log recorded by the user
        logs.forEach { log ->
            totalTimeMinutes += log.durationMinutes
            log.exercises.forEach { ex ->
                val name = ex.exerciseName.lowercase()
                
                // Determine the highest weight ever lifted for major movements (PR tracking)
                val maxWeight = ex.sets.mapNotNull { it.weight }.maxOrNull() ?: 0.0
                if (name.contains("bench press")) benchMax = maxOf(benchMax, maxWeight)
                if (name.contains("squat")) squatMax = maxOf(squatMax, maxWeight)
                if (name.contains("deadlift")) deadliftMax = maxOf(deadliftMax, maxWeight)
                if (name.contains("shoulder press") || name.contains("overhead press")) overheadMax = maxOf(overheadMax, maxWeight)
                if (name.contains("dumbbell")) dumbbellMax = maxOf(dumbbellMax, maxWeight)

                // Accumulate volume and rep totals for endurance and strength trophies
                ex.sets.forEach { set ->
                    if (ex.type == ExerciseType.STRENGTH || ex.type == ExerciseType.CALISTHENICS) {
                        val weight = set.weight ?: 0.0
                        val reps = set.reps ?: 0
                        totalVolume += (weight * reps)
                        
                        // Track cumulative reps for specific bodyweight master trophies
                        if (name.contains("pushup")) totalPushups += reps
                        if (name.contains("pullup")) totalPullups += reps
                        if (name.contains("situp") || name.contains("crunch")) totalSitups += reps
                    }
                    
                    // Track total time for cardio-focused achievements
                    if (ex.type == ExerciseType.CARDIO) {
                        cardioMinutes += (set.durationSeconds ?: 0) / 60
                    }
                }
                
                // Track time dedicated to core work (Abs Master)
                if (name.contains("abs") || name.contains("plank") || name.contains("leg raise")) {
                    absMinutes += ex.sets.sumOf { (it.durationSeconds ?: 0) / 60 + if (it.reps != null) 1 else 0 }
                }
            }
        }

        // Instantiate the Trophy objects with the final calculated statistics.
        // The rank of each trophy (Bronze, Silver, etc.) is determined automatically inside the Trophy class based on these values.
        val trophies = listOf(
            Trophy("gym_rat", "Gym Rat", "Total workouts completed", logs.size, TrophyType.GYM_RAT),
            Trophy("recovery", "Recovery", "Total rest days recorded", restDayCount, TrophyType.RECOVERY),
            Trophy("lift_king", "Lift King", "Total volume lifted (lbs)", totalVolume.toInt(), TrophyType.LIFT_KING),
            Trophy("bench_press", "Bench Master", "Max Bench Press weight", benchMax.toInt(), TrophyType.BENCH_PRESS),
            Trophy("squat", "Squat Master", "Max Squat weight", squatMax.toInt(), TrophyType.SQUAT),
            Trophy("deadlift", "Deadlift Master", "Max Deadlift weight", deadliftMax.toInt(), TrophyType.DEADLIFT),
            Trophy("shoulder_press", "Iron Shoulders", "Max Overhead Press weight", overheadMax.toInt(), TrophyType.SHOULDER_PRESS),
            Trophy("cardio_bunny", "Cardio Bunny", "Total cardio minutes", cardioMinutes, TrophyType.CARDIO_BUNNY),
            Trophy("dumbbell_master", "Dumbbell Master", "Max Dumbbell weight used", dumbbellMax.toInt(), TrophyType.DUMBBELL_MASTER),
            Trophy("abs_master", "Core Crusher", "Total time spent on abs", absMinutes, TrophyType.ABS_MASTER),
            Trophy("all_star", "All Star", "Combined SBD Max (Squat + Bench + Deadlift)", (squatMax + benchMax + deadliftMax).toInt(), TrophyType.ALL_STAR),
            Trophy("full_time", "Full Time", "Total minutes spent working out", totalTimeMinutes, TrophyType.FULL_TIME),
            Trophy("gym_bro", "Gym Bro", "Total friends made", friendsCount, TrophyType.GYM_BRO),
            Trophy("pushup_master", "Pushup Master", "Total pushups completed", totalPushups, TrophyType.PUSHUP_MASTER),
            Trophy("pullup_master", "Pullup Master", "Total pullups completed", totalPullups, TrophyType.PULLUP_MASTER),
            Trophy("situp_master", "Situp Master", "Total situps completed", totalSitups, TrophyType.SITUP_MASTER)
        )
        adapter.submitList(trophies)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
