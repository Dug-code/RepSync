package com.repsyncdemo.workout.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.databinding.LayoutTabListBinding
import com.repsyncdemo.workout.ui.adapter.HistoryAdapter
import com.repsyncdemo.workout.viewmodel.WorkoutViewModel

class HomeRecentFragment : Fragment() {
    private var _binding: LayoutTabListBinding? = null
    private val binding get() = _binding!!
    private val workoutViewModel: WorkoutViewModel by activityViewModels()
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = LayoutTabListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        historyAdapter = HistoryAdapter(
            onItemClick = { log ->
                val bundle = Bundle().apply { 
                    putString("logId", log.id)
                }
                findNavController().navigate(R.id.workoutSummaryFragment, bundle)
            }
        )

        binding.rvContent.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }

        workoutViewModel.workoutLogs.observe(viewLifecycleOwner) { logs ->
            val sortedLogs = logs.sortedByDescending { it.completedAt }.take(10)
            historyAdapter.submitList(sortedLogs)
            val isEmpty = sortedLogs.isEmpty()
            binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.tvEmptyTitle.text = "No completed sessions yet"
            binding.tvEmptySubtitle.text = "Workout logs appear here after you finish a session."
            binding.btnEmptyAction.text = "Browse Templates"
            binding.btnEmptyAction.setOnClickListener {
                findNavController().navigate(R.id.workoutListFragment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
