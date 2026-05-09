package com.repsyncdemo.workout.ui.profile

/**
 * File overview: Displays and manages a profile-related screen for user identity, social, goals, trophies, or notifications.
 */

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.repsyncdemo.workout.data.model.ExerciseType
import com.repsyncdemo.workout.data.model.Trophy
import com.repsyncdemo.workout.data.model.TrophyType
import com.repsyncdemo.workout.databinding.FragmentTrophyShelfBinding
import com.repsyncdemo.workout.ui.adapter.TrophyAdapter
import com.repsyncdemo.workout.viewmodel.GoalViewModel
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
    private val goalViewModel: GoalViewModel by activityViewModels()

    // Sets up this screen.
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrophyShelfBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Connects views, clicks, and data.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize adapter with a click listener to "pin" a trophy to the user's public profile
        val trophyAdapter = TrophyAdapter { trophy ->
            profileViewModel.pinTrophy(trophy.id)
            Toast.makeText(requireContext(), "${trophy.name} pinned to profile!", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }

        binding.rvTrophies.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = trophyAdapter
        }

        // Observe all necessary data streams to refresh trophies in real-time
        profileViewModel.myProfile.observe(viewLifecycleOwner) { 
            refreshTrophies(trophyAdapter)
        }
        workoutViewModel.workoutLogs.observe(viewLifecycleOwner) { 
            refreshTrophies(trophyAdapter)
        }
        workoutViewModel.workouts.observe(viewLifecycleOwner) {
            refreshTrophies(trophyAdapter)
        }
        workoutViewModel.weightLogs.observe(viewLifecycleOwner) {
            refreshTrophies(trophyAdapter)
        }
        goalViewModel.goals.observe(viewLifecycleOwner) {
            refreshTrophies(trophyAdapter)
        }
        socialViewModel.friends.observe(viewLifecycleOwner) {
            refreshTrophies(trophyAdapter)
        }
    }

    // Builds the 12 trophy stats from data we already store.
    private fun refreshTrophies(adapter: TrophyAdapter) {
        val logs = workoutViewModel.workoutLogs.value ?: emptyList()
        val routines = workoutViewModel.workouts.value ?: emptyList()
        val weightLogs = workoutViewModel.weightLogs.value ?: emptyList()
        val goals = goalViewModel.goals.value ?: emptyList()
        val profile = profileViewModel.myProfile.value
        val friendsCount = socialViewModel.friends.value?.size ?: 0
        val restDayCount = profile?.totalRestDays ?: 0
        val weighInCount = maxOf(weightLogs.size, if ((profile?.lastWeighInDate ?: 0L) > 0L) 1 else 0)

        var totalVolume = 0.0
        var totalTimeMinutes = 0
        var totalReps = 0
        var totalSets = 0
        var cardioMinutes = 0
        var heaviestSet = 0.0

        logs.forEach { log ->
            totalTimeMinutes += log.durationMinutes
            log.exercises.forEach { ex ->
                ex.sets.forEach { set ->
                    if (set.completed) {
                        totalSets++
                    }

                    if (set.completed && (ex.type == ExerciseType.STRENGTH || ex.type == ExerciseType.CALISTHENICS)) {
                        val weight = set.weight ?: 0.0
                        val reps = set.reps ?: 0
                        totalVolume += (weight * reps)
                        totalReps += reps
                        heaviestSet = maxOf(heaviestSet, weight)
                    }

                    if (set.completed && ex.type == ExerciseType.CARDIO) {
                        cardioMinutes += (set.durationSeconds ?: 0) / 60
                    }
                }
            }
        }

        val goalsCreatedOrCompleted = goals.count { it.isCompleted } + goals.size
        val trophies = listOf(
            Trophy("gym_rat", "Gym Rat", "Total workouts completed", logs.size, TrophyType.GYM_RAT),
            Trophy("recovery", "Recovery", "Total rest days recorded", restDayCount, TrophyType.RECOVERY),
            Trophy("lift_king", "Lift King", "Total volume lifted (lbs)", totalVolume.toInt(), TrophyType.LIFT_KING),
            Trophy("tick_tock", "Tick Tock", "Total minutes in the gym", totalTimeMinutes, TrophyType.TICK_TOCK),
            Trophy("scale_check", "Scale Check", "Total weigh-ins logged", weighInCount, TrophyType.SCALE_CHECK),
            Trophy("rep_machine", "Rep Machine", "Total completed reps", totalReps, TrophyType.REP_MACHINE),
            Trophy("set_collector", "Set Collector", "Total completed sets", totalSets, TrophyType.SET_COLLECTOR),
            Trophy("cardio_champ", "Cardio Champ", "Total cardio minutes", cardioMinutes, TrophyType.CARDIO_CHAMP),
            Trophy("routine_builder", "Routine Builder", "Saved routines created", routines.size, TrophyType.ROUTINE_BUILDER),
            Trophy("goal_getter", "Goal Getter", "Goals created and completed", goalsCreatedOrCompleted, TrophyType.GOAL_GETTER),
            Trophy("heavy_hitter", "Heavy Hitter", "Heaviest set logged (lbs)", heaviestSet.toInt(), TrophyType.HEAVY_HITTER),
            Trophy("gym_bro", "Gym Bro", "Total friends made", friendsCount, TrophyType.GYM_BRO)
        )
        adapter.submitList(trophies)
    }

    // Clears the view binding.
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
