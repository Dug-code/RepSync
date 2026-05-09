package com.repsyncdemo.workout.ui.workout

/**
 * File overview: Displays the completed workout log summary after a session is saved.
 */

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.repsyncdemo.workout.databinding.FragmentWorkoutSummaryBinding
import com.repsyncdemo.workout.ui.adapter.ExerciseSummaryAdapter
import com.repsyncdemo.workout.viewmodel.WorkoutViewModel
import java.text.SimpleDateFormat
import java.util.*

class WorkoutSummaryFragment : Fragment() {

    private var _binding: FragmentWorkoutSummaryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WorkoutViewModel by activityViewModels()

    // Sets up this screen.
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkoutSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Connects views, clicks, and data.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val logId = arguments?.getString("logId") ?: return
        viewModel.loadWorkoutLog(logId)

        viewModel.selectedLog.observe(viewLifecycleOwner) { log ->
            if (log != null && log.id == logId) {
                binding.tvWorkoutName.text = log.workoutName
                
                val sdf = SimpleDateFormat("EEEE, MMM dd • h:mm a", Locale.getDefault())
                binding.tvDateTime.text = sdf.format(Date(log.completedAt))
                
                binding.tvDuration.text = "${log.durationMinutes} min"
                
                val totalVolume = log.exercises.sumOf { exercise ->
                    exercise.sets.filter { set -> set.completed }.sumOf { set ->
                        (set.reps ?: 0) * (set.weight ?: 0.0)
                    }
                }
                binding.tvVolume.text = String.format(Locale.getDefault(), "%.0f lbs", totalVolume)

                if (log.notes.isNotEmpty()) {
                    binding.cardNotes.visibility = View.VISIBLE
                    binding.tvNotes.text = log.notes
                } else {
                    binding.cardNotes.visibility = View.GONE
                }

                binding.rvExercises.apply {
                    layoutManager = LinearLayoutManager(requireContext())
                    adapter = ExerciseSummaryAdapter(log.exercises)
                }

                // Hide loader and show content
                binding.progressBar.visibility = View.GONE
                binding.scrollView.visibility = View.VISIBLE
            }
        }

        binding.btnDone.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    // Clears the view binding.
    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.clearSelection() // Clear data when leaving
        _binding = null
    }
}
