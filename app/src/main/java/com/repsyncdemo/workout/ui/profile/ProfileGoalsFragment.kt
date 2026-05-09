package com.repsyncdemo.workout.ui.profile

/**
 * File overview: Displays and manages a profile-related screen for user identity, social, goals, trophies, or notifications.
 */

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.data.model.Goal
import com.repsyncdemo.workout.databinding.LayoutTabListBinding
import com.repsyncdemo.workout.ui.adapter.GoalAdapter
import com.repsyncdemo.workout.viewmodel.GoalViewModel

class ProfileGoalsFragment : Fragment() {
    private var _binding: LayoutTabListBinding? = null
    private val binding get() = _binding!!
    
    private val goalViewModel: GoalViewModel by activityViewModels()
    private lateinit var goalAdapter: GoalAdapter
    private var targetUserId: String? = null

    // Sets up this screen.
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = LayoutTabListBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Connects views, clicks, and data.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        targetUserId = arguments?.getString("userId")

        goalAdapter = GoalAdapter(
            isMyProfile = targetUserId == null,
            onUpdateProgress = { goal -> 
                val bundle = Bundle().apply { putString("goalId", goal.id) }
                findNavController().navigate(R.id.goalsFragment, bundle)
            },
            onEdit = { goal -> showRenameGoalDialog(goal) },
            onTogglePrivacy = { goal ->
                val newStatus = !goal.isPublic
                goalViewModel.updateGoal(goal.copy(isPublic = newStatus))
                val statusText = if (newStatus) "Public" else "Private"
                Toast.makeText(requireContext(), "Goal is now $statusText", Toast.LENGTH_SHORT).show()
            },
            onDelete = { goal -> goalViewModel.deleteGoal(goal.id) }
        )

        binding.rvContent.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = goalAdapter
        }

        binding.btnEmptyAction.setOnClickListener {
            findNavController().navigate(R.id.goalsFragment)
        }

        val goalsSource = if (targetUserId != null) goalViewModel.targetUserGoals else goalViewModel.goals
        goalsSource.observe(viewLifecycleOwner) { goals ->
            goalAdapter.submitList(goals)
            val isEmpty = goals.isNullOrEmpty()
            binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
            
            if (targetUserId != null) {
                binding.tvEmptyTitle.text = "No public goals."
                binding.tvEmptySubtitle.text = "This user currently has no public goals."
            } else {
                binding.tvEmptyTitle.text = "Create a goal here"
                binding.tvEmptySubtitle.text = "Set your first goal to start tracking progress."
            }
            binding.btnEmptyAction.text = getString(R.string.create_goal)
            binding.btnEmptyAction.visibility = if (targetUserId == null) View.VISIBLE else View.GONE
        }
    }

    // Shows a dialog or popup.
    private fun showRenameGoalDialog(goal: Goal) {
        val input = EditText(requireContext())
        input.setText(goal.title)
        input.setSelection(goal.title.length)
        input.setPadding(64, 32, 64, 32)

        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setTitle("Rename Goal")
            .setView(input)
            .setPositiveButton("Update") { _, _ ->
                val newTitle = input.text.toString().trim()
                if (newTitle.isNotEmpty()) {
                    goalViewModel.updateGoal(goal.copy(title = newTitle))
                    Toast.makeText(requireContext(), "Goal renamed", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Clears the view binding.
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
