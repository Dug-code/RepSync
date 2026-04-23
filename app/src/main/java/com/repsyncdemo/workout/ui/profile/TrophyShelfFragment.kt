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
import com.repsyncdemo.workout.data.model.Trophy
import com.repsyncdemo.workout.data.model.TrophyType
import com.repsyncdemo.workout.databinding.FragmentTrophyShelfBinding
import com.repsyncdemo.workout.ui.adapter.TrophyAdapter
import com.repsyncdemo.workout.viewmodel.ProfileViewModel
import com.repsyncdemo.workout.viewmodel.WorkoutViewModel

class TrophyShelfFragment : Fragment() {

    private var _binding: FragmentTrophyShelfBinding? = null
    private val binding get() = _binding!!
    private val workoutViewModel: WorkoutViewModel by activityViewModels()
    private val profileViewModel: ProfileViewModel by activityViewModels()

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

        val trophyAdapter = TrophyAdapter { trophy ->
            // Pin Trophy logic
            profileViewModel.pinTrophy(trophy.id)
            Toast.makeText(requireContext(), "${trophy.name} pinned to profile!", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }

        binding.rvTrophies.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = trophyAdapter
        }

        // Combine observers to update trophies when either profile or logs change
        profileViewModel.myProfile.observe(viewLifecycleOwner) { profile ->
            updateTrophies(workoutViewModel.workoutLogs.value?.size ?: 0, profile?.totalRestDays ?: 0, trophyAdapter)
        }

        workoutViewModel.workoutLogs.observe(viewLifecycleOwner) { logs ->
            updateTrophies(logs.size, profileViewModel.myProfile.value?.totalRestDays ?: 0, trophyAdapter)
        }
    }

    private fun updateTrophies(workoutCount: Int, restDayCount: Int, adapter: TrophyAdapter) {
        val trophies = listOf(
            Trophy("gym_rat", "Gym Rat", "Total workouts completed in RepSync", workoutCount, TrophyType.GYM_RAT),
            Trophy("recovery", "Recovery", "Total rest days recorded", restDayCount, TrophyType.RECOVERY)
        )
        adapter.submitList(trophies)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
