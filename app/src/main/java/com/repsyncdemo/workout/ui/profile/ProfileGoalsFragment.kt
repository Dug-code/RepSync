package com.repsyncdemo.workout.ui.profile

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
import com.repsyncdemo.workout.ui.adapter.GoalAdapter
import com.repsyncdemo.workout.viewmodel.GoalViewModel

class ProfileGoalsFragment : Fragment() {
    private var _binding: LayoutTabListBinding? = null
    private val binding get() = _binding!!
    
    private val goalViewModel: GoalViewModel by activityViewModels()
    private lateinit var goalAdapter: GoalAdapter
    private var targetUserId: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = LayoutTabListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        targetUserId = arguments?.getString("userId")

        goalAdapter = GoalAdapter(
            isMyProfile = targetUserId == null,
            onUpdateProgress = { goal -> 
                val bundle = Bundle().apply { putString("goalId", goal.id) }
                findNavController().navigate(R.id.goalsFragment, bundle)
            },
            onDelete = { goal -> goalViewModel.deleteGoal(goal.id) }
        )

        binding.rvContent.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = goalAdapter
        }

        val goalsSource = if (targetUserId != null) goalViewModel.targetUserGoals else goalViewModel.goals
        goalsSource.observe(viewLifecycleOwner) { goals ->
            goalAdapter.submitList(goals)
            val isEmpty = goals.isNullOrEmpty()
            binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.tvEmptyTitle.text = "No goals set."
            binding.tvEmptySubtitle.text = ""
            binding.btnEmptyAction.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
