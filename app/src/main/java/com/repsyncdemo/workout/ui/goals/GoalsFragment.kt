package com.repsyncdemo.workout.ui.goals

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.switchmaterial.SwitchMaterial
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.data.model.FeedPost
import com.repsyncdemo.workout.data.model.FeedPostType
import com.repsyncdemo.workout.data.model.Goal
import com.repsyncdemo.workout.data.model.GoalType
import com.repsyncdemo.workout.databinding.FragmentGoalsBinding
import com.repsyncdemo.workout.ui.adapter.GoalAdapter
import com.repsyncdemo.workout.viewmodel.FeedViewModel
import com.repsyncdemo.workout.viewmodel.GoalViewModel
import com.repsyncdemo.workout.viewmodel.ProfileViewModel

class GoalsFragment : Fragment() {

    private var _binding: FragmentGoalsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GoalViewModel by activityViewModels()
    private val profileViewModel: ProfileViewModel by activityViewModels()
    private val feedViewModel: FeedViewModel by activityViewModels()

    private lateinit var goalAdapter: GoalAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGoalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        goalAdapter = GoalAdapter(
            onUpdateProgress = { goal -> showUpdateProgressDialog(goal) },
            onDelete = { goal -> viewModel.deleteGoal(goal.id) }
        )

        binding.rvGoals.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = goalAdapter
        }

        binding.fabAddGoal.setOnClickListener {
            showCreateGoalDialog()
        }

        viewModel.goals.observe(viewLifecycleOwner) { goals ->
            goalAdapter.submitList(goals)
            binding.tvEmpty.visibility = if (goals.isEmpty()) View.VISIBLE else View.GONE
            binding.rvGoals.visibility = if (goals.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun showCreateGoalDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_create_goal, null)

        val etTitle = dialogView.findViewById<EditText>(R.id.etGoalTitle)
        val etDescription = dialogView.findViewById<EditText>(R.id.etGoalDescription)
        val rgGoalType = dialogView.findViewById<RadioGroup>(R.id.rgGoalType)
        val rbPR = dialogView.findViewById<RadioButton>(R.id.rbPR)
        val tilExerciseName = dialogView.findViewById<View>(R.id.tilExerciseName)
        val etExerciseName = dialogView.findViewById<EditText>(R.id.etExerciseName)
        val etCurrentValue = dialogView.findViewById<EditText>(R.id.etCurrentValue)
        val etTargetValue = dialogView.findViewById<EditText>(R.id.etTargetValue)
        val etUnit = dialogView.findViewById<EditText>(R.id.etUnit)
        val switchPublic = dialogView.findViewById<SwitchMaterial>(R.id.switchPublicGoal)

        rgGoalType.setOnCheckedChangeListener { _, checkedId ->
            tilExerciseName.visibility = if (checkedId == R.id.rbPR) View.VISIBLE else View.GONE
            etUnit.setText("lbs")
        }

        rbPR.isChecked = true

        AlertDialog.Builder(requireContext())
            .setTitle("Create Goal")
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ ->
                val title = etTitle.text.toString().trim()
                if (title.isEmpty()) {
                    Toast.makeText(requireContext(), "Title is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val goalType = when (rgGoalType.checkedRadioButtonId) {
                    R.id.rbPR -> GoalType.PR
                    R.id.rbWeightLoss -> GoalType.WEIGHT_LOSS
                    R.id.rbWeightGain -> GoalType.WEIGHT_GAIN
                    else -> GoalType.PR
                }

                val initialVal = etCurrentValue.text.toString().toDoubleOrNull() ?: 0.0
                val goal = Goal(
                    title = title,
                    description = etDescription.text.toString().trim(),
                    type = goalType,
                    exerciseName = if (goalType == GoalType.PR) etExerciseName.text.toString().trim() else "",
                    startingValue = initialVal,
                    currentValue = initialVal,
                    targetValue = etTargetValue.text.toString().toDoubleOrNull() ?: 0.0,
                    unit = etUnit.text.toString().trim().ifEmpty { "lbs" },
                    isPublic = switchPublic.isChecked
                )

                viewModel.addGoal(goal)

                // Sync profile weight immediately
                if (goalType == GoalType.WEIGHT_LOSS || goalType == GoalType.WEIGHT_GAIN) {
                    profileViewModel.currentProfile.value?.let { profile ->
                        profileViewModel.updateProfile(profile.copy(weightLbs = initialVal))
                    }
                }
                
                if (switchPublic.isChecked) {
                    shareGoalToFeed(goal, FeedPostType.GOAL_CREATED)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showUpdateProgressDialog(goal: Goal) {
        val input = EditText(requireContext())
        input.hint = "New value (${goal.unit})"
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        input.setText(goal.currentValue.toString())

        AlertDialog.Builder(requireContext())
            .setTitle("Update Progress")
            .setMessage("${goal.title}\nTarget: ${goal.targetValue} ${goal.unit}")
            .setView(input)
            .setPositiveButton("Update") { _, _ ->
                val newValue = input.text.toString().toDoubleOrNull() ?: return@setPositiveButton
                viewModel.updateProgress(goal.id, newValue)

                // Sync profile weight on update
                if (goal.type == GoalType.WEIGHT_LOSS || goal.type == GoalType.WEIGHT_GAIN) {
                    profileViewModel.currentProfile.value?.let { profile ->
                        profileViewModel.updateProfile(profile.copy(weightLbs = newValue))
                    }
                }

                val isComplete = when (goal.type) {
                    GoalType.PR -> newValue >= goal.targetValue
                    GoalType.WEIGHT_LOSS -> newValue <= goal.targetValue
                    GoalType.WEIGHT_GAIN -> newValue >= goal.targetValue
                    GoalType.STREAK -> newValue >= goal.targetValue
                }
                
                if (isComplete) {
                    viewModel.completeGoal(goal.id)
                    Toast.makeText(requireContext(), "Goal completed!", Toast.LENGTH_SHORT).show()
                    if (goal.isPublic) {
                        shareGoalToFeed(goal.copy(currentValue = newValue, isCompleted = true), FeedPostType.GOAL_COMPLETED)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun shareGoalToFeed(goal: Goal, type: FeedPostType) {
        val profile = profileViewModel.currentProfile.value ?: return
        val post = FeedPost(
            userId = profile.userId,
            username = profile.username,
            type = type,
            goalId = goal.id,
            goalTitle = goal.title,
            description = when (type) {
                FeedPostType.GOAL_CREATED -> "Just set a new goal: ${goal.title} (${goal.targetValue} ${goal.unit})"
                FeedPostType.GOAL_COMPLETED -> "Goal Achieved! ${goal.title}"
                else -> ""
            }
        )
        feedViewModel.createPost(post)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
