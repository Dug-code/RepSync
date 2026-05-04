package com.repsyncdemo.workout.ui.goals

import android.content.Context
import android.graphics.Color.argb
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.data.model.FeedPost
import com.repsyncdemo.workout.data.model.FeedPostType
import com.repsyncdemo.workout.data.model.Goal
import com.repsyncdemo.workout.data.model.GoalType
import com.repsyncdemo.workout.databinding.FragmentGoalsBinding
import com.repsyncdemo.workout.ui.adapter.GoalAdapter
import com.repsyncdemo.workout.ui.home.HomeFragment
import com.repsyncdemo.workout.viewmodel.FeedViewModel
import com.repsyncdemo.workout.viewmodel.GoalViewModel
import com.repsyncdemo.workout.viewmodel.ProfileViewModel
import com.repsyncdemo.workout.viewmodel.WorkoutViewModel
import com.takusemba.spotlight.OnSpotlightListener
import com.takusemba.spotlight.Spotlight
import com.takusemba.spotlight.Target
import com.takusemba.spotlight.effet.FlickerEffect
import com.takusemba.spotlight.shape.Circle
import com.takusemba.spotlight.shape.RoundedRectangle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class GoalsFragment : Fragment() {

    private var _binding: FragmentGoalsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GoalViewModel by activityViewModels()
    private val profileViewModel: ProfileViewModel by activityViewModels()
    private val feedViewModel: FeedViewModel by activityViewModels()
    private val workoutViewModel: WorkoutViewModel by activityViewModels()

    private lateinit var goalAdapter: GoalAdapter

    private var spotlight: Spotlight? = null

    companion object {
        private const val PREFS_NAME = "repsync_prefs"
        private const val KEY_GOAL_TUTORIAL_COMPLETED = "goal_tutorial_completed"
    }

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

        checkTutorial()

        goalAdapter = GoalAdapter(
            isMyProfile = true,
            onUpdateProgress = { goal -> showUpdateProgressDialog(goal) },
            onEdit = { goal -> showRenameGoalDialog(goal) },
            onTogglePrivacy = { goal -> 
                val newStatus = !goal.isPublic
                viewModel.updateGoal(goal.copy(isPublic = newStatus))
                val statusText = if (newStatus) "Public" else "Private"
                Toast.makeText(requireContext(), "Goal is now $statusText", Toast.LENGTH_SHORT).show()
                
                if (newStatus) {
                    shareGoalToFeed(goal.copy(isPublic = true), FeedPostType.GOAL_CREATED)
                }
            },
            onDelete = { goal -> viewModel.deleteGoal(goal.id) }
        )

        binding.rvGoals.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = goalAdapter
        }

        binding.fabAddGoal.setOnClickListener {
            showCreateGoalDialog()
            spotlight?.finish()
        }

        viewModel.goals.observe(viewLifecycleOwner) { goals ->
            goalAdapter.submitList(goals)
            binding.tvEmpty.visibility = if (goals.isEmpty()) View.VISIBLE else View.GONE
            binding.rvGoals.visibility = if (goals.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun checkTutorial() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        //set to false for testing
        val isCompleted = prefs.getBoolean(KEY_GOAL_TUTORIAL_COMPLETED, false)
        if (!isCompleted) {
            binding.root.post {
                showTutorial()
            }


        }
    }

    private fun showTutorial() {
        val targets = ArrayList<com.takusemba.spotlight.Target>()

        // Start Workout Button
        targets.add(
            com.takusemba.spotlight.Target.Builder()
                .setAnchor(binding.fabAddGoal)
                .setShape(Circle(binding.fabAddGoal.height.toFloat() / 2))
                .setEffect(FlickerEffect(100f, argb(255,68,71,78)))
                .setOverlay(goalCreateOverlay("Add Goal", "Lets add a goal to track your progress \n\n Tap the flashing icon to add a new goal."))
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
                    //removing markTutorialCompleted() because if user leaves app tutorial will not show again
                    //markTutorialCompleted()
                }
            })
            .build()

        spotlight?.start()
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
        prefs.edit { putBoolean(KEY_GOAL_TUTORIAL_COMPLETED, true) }
    }

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
                    viewModel.updateGoal(goal.copy(title = newTitle))
                    Toast.makeText(requireContext(), "Goal renamed", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCreateGoalDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_create_goal, null)

        val etTitle = dialogView.findViewById<EditText>(R.id.etGoalTitle)
        val rgGoalType = dialogView.findViewById<RadioGroup>(R.id.rgGoalType)
        val rbPR = dialogView.findViewById<RadioButton>(R.id.rbPR)
        val ivPreview = dialogView.findViewById<ImageView>(R.id.ivGoalPreviewIcon)
        val tilExerciseName = dialogView.findViewById<View>(R.id.tilExerciseName)
        val etExerciseName = dialogView.findViewById<AutoCompleteTextView>(R.id.etExerciseName)
        val llPRUnits = dialogView.findViewById<LinearLayout>(R.id.llPRUnits)
        val cbUnitLbs = dialogView.findViewById<CheckBox>(R.id.cbUnitLbs)
        val cbUnitReps = dialogView.findViewById<CheckBox>(R.id.cbUnitReps)
        val etCurrentValue = dialogView.findViewById<EditText>(R.id.etCurrentValue)
        val etTargetValue = dialogView.findViewById<EditText>(R.id.etTargetValue)
        val tilUnit = dialogView.findViewById<View>(R.id.tilUnit)
        val etUnit = dialogView.findViewById<EditText>(R.id.etUnit)
        val switchPublic = dialogView.findViewById<SwitchMaterial>(R.id.switchPublicGoal)

        // Setup exercise autocomplete
        viewLifecycleOwner.lifecycleScope.launch {
            workoutViewModel.allUniqueExerciseNames.collectLatest { names ->
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names)
                etExerciseName.setAdapter(adapter)
            }
        }

        rgGoalType.setOnCheckedChangeListener { _, checkedId ->
            hideKeyboard(dialogView)
            etTitle.clearFocus()
            etExerciseName.clearFocus()
            activity?.currentFocus?.clearFocus()
            
            tilExerciseName.visibility = if (checkedId == R.id.rbPR) View.VISIBLE else View.GONE
            llPRUnits.visibility = if (checkedId == R.id.rbPR) View.VISIBLE else View.GONE
            
            val previewIcon = when (checkedId) {
                R.id.rbPR -> R.drawable.ic_medal
                R.id.rbWeightLoss, R.id.rbWeightGain -> R.drawable.ic_scale
                else -> R.drawable.ic_checkered_flag
            }
            ivPreview.setImageResource(previewIcon)

            when (checkedId) {
                R.id.rbPR -> {
                    tilUnit.visibility = View.GONE
                }
                R.id.rbWeightLoss, R.id.rbWeightGain -> {
                    tilUnit.visibility = View.VISIBLE
                    etUnit.setText("lbs")
                    etUnit.isEnabled = false
                }
                R.id.rbOther -> {
                    tilUnit.visibility = View.VISIBLE
                    etUnit.setText("sets")
                    etUnit.isEnabled = true
                }
            }
        }

        cbUnitLbs.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) cbUnitReps.isChecked = false
        }
        cbUnitReps.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) cbUnitLbs.isChecked = false
        }


        // Set default values for create goal dialog
        rbPR.isChecked = true
        ivPreview.setImageResource(R.drawable.ic_medal)
        tilUnit.visibility = View.GONE

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setTitle("Create Goal")
            .setView(dialogView)
            .setPositiveButton("Create", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val title = etTitle.text.toString().trim()
            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "Title is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val goalType = when (rgGoalType.checkedRadioButtonId) {
                R.id.rbPR -> GoalType.PR
                R.id.rbWeightLoss -> GoalType.WEIGHT_LOSS
                R.id.rbWeightGain -> GoalType.WEIGHT_GAIN
                else -> GoalType.PR
            }

            val unit = when (rgGoalType.checkedRadioButtonId) {
                R.id.rbPR -> if (cbUnitLbs.isChecked) "lbs" else "reps"
                R.id.rbWeightLoss, R.id.rbWeightGain -> "lbs"
                else -> etUnit.text.toString().trim().ifEmpty { "sets" }
            }

            val initialVal = etCurrentValue.text.toString().toDoubleOrNull() ?: 0.0
            
            if (goalType == GoalType.WEIGHT_LOSS || goalType == GoalType.WEIGHT_GAIN) {
                if (initialVal > 1400) {
                    Toast.makeText(requireContext(), "Weight cannot exceed 1400 lbs", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                profileViewModel.updateWeight(initialVal)
            }

            val goal = Goal(
                title = title,
                type = goalType,
                exerciseName = if (goalType == GoalType.PR) etExerciseName.text.toString().trim() else "",
                startingValue = initialVal,
                currentValue = initialVal,
                targetValue = etTargetValue.text.toString().toDoubleOrNull() ?: 0.0,
                unit = unit,
                isPublic = switchPublic.isChecked
            )

            viewModel.addGoal(goal)
            
            if (switchPublic.isChecked) {
                shareGoalToFeed(goal, FeedPostType.GOAL_CREATED)
            }

            markTutorialCompleted()
            dialog.dismiss()
        }

        checkTutorial(dialogView, dialog)

    }

    //overloaded method for checking tutorial
    private fun checkTutorial(dialogView: View, dialog: androidx.appcompat.app.AlertDialog) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        //set to false for testing
        val isCompleted = prefs.getBoolean(KEY_GOAL_TUTORIAL_COMPLETED, false)
        if (!isCompleted) {
            showDialogTutorial(dialogView, dialog)
        }
    }
    private fun showDialogTutorial(dialogView: View, dialog: androidx.appcompat.app.AlertDialog) {
        val inflater = LayoutInflater.from(requireContext())
        val overlayLayout = inflater.inflate(R.layout.fragment_goal_overlay, null)

        //finds the root of the dailog window
        val dialogWindowDecor = dialog.window?.decorView as? ViewGroup

        //adds the overlay layout to the dialog window
        dialogWindowDecor?.addView(overlayLayout)

        val focusView = overlayLayout.findViewById<View>(R.id.tutorialFocus)
        val titleTxt = overlayLayout.findViewById<TextView>(R.id.tvTutorialTitle)
        val nextBtn = overlayLayout.findViewById<Button>(R.id.btnTutorialNext)
        val skipBtn = overlayLayout.findViewById<Button>(R.id.btnSkip2)

        // Define our steps
        var currentStep = 0
        val etTitle = dialogView.findViewById<EditText>(R.id.etGoalTitle)
        val rgType = dialogView.findViewById<RadioGroup>(R.id.rgGoalType)
        val llCurrentTarget = dialogView.findViewById<LinearLayout>(R.id.llCurrentTarget)
        val etUnit = dialogView.findViewById<EditText>(R.id.etUnit)
        val switchPublic = dialogView.findViewById<SwitchMaterial>(R.id.switchPublicGoal)

        //Goal type text view and radio button for tutorial ease
        val tvGoalType = dialogView.findViewById<TextView>(R.id.tvGoalType)
        val rbPR = dialogView.findViewById<RadioButton>(R.id.rbPR)
        val rbWeightLoss = dialogView.findViewById<RadioButton>(R.id.rbWeightLoss)
        val rbWeightGain = dialogView.findViewById<RadioButton>(R.id.rbWeightGain)
        val rbOther = dialogView.findViewById<RadioButton>(R.id.rbOther)


        //goal type weight loss is checked for ease of tutorial
        rbWeightLoss.isChecked = true

        //Maybe used for create button at bottom of dialog
        val createBtn = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)

        fun moveToStep(step: Int) {
            val target = when (step) {
                0 -> etTitle
                1 -> rgType
                2 -> llCurrentTarget
                3 -> etUnit
                4 -> switchPublic
                5 -> createBtn
                else -> {
                    overlayLayout.visibility = View.GONE
                    return
                }
            }

            target.post {

                //gets the X and Y location of targets to be used for positioning the overlay
                val location = IntArray(2)
                target.getLocationOnScreen(location)

                //gets the X and Y location of the dialog window for positioning targets
                val dialogLocation = IntArray(2)
                dialogWindowDecor?.getLocationOnScreen(dialogLocation)

                // Calculate the relative position
                val relativeX = location[0] - dialogLocation[0]
                val relativeY = location[1] - dialogLocation[1]

                val params = focusView.layoutParams as ConstraintLayout.LayoutParams
                params.width = target.width + 40
                params.height = target.height + 50
                params.leftMargin = relativeX - 85
                params.topMargin = relativeY - 235
                focusView.layoutParams = params

                when(step) {
                    0 -> {
                        rbPR.isClickable = false
                        rbWeightGain.isClickable = false
                        rbOther.isClickable = false
                        titleTxt.text = "Lets give your goal a title"
                    }
                    1 -> {
                        tvGoalType.alpha = 0.0f
                        titleTxt.text = "For this tutorial we will use weight loss as your goal type"
                    }
                    2 -> {
                        tvGoalType.alpha = 1.0f
                        rbOther.alpha = 0.0f
                        titleTxt.text = "Lets set your current weight value and weight target"
                    }
                    3 -> {
                        rbOther.alpha = 1.0f
                        llCurrentTarget.alpha = 0.0f
                        titleTxt.text = "unit of measurement is set to lbs for this goal"
                    }
                    4 -> {
                        llCurrentTarget.alpha = 1.0f
                        etUnit.alpha = 0.0f
                        titleTxt.text = "Toggle switch to allow anyone to view your goals"
                    }
                    5 ->  {
                        //adjusting highlight size for create button
                        val params = focusView.layoutParams as ConstraintLayout.LayoutParams
                        params.width = target.width + 0
                        params.height = target.height - 20
                        params.leftMargin = relativeX - 60
                        params.topMargin = relativeY - 205
                        focusView.layoutParams = params

                        //Makes next button invisible to allow user to hit create
                        nextBtn.visibility = View.GONE

                        //makes radio buttons clickable again after tutorial completed
                        rbPR.isClickable = true
                        rbWeightGain.isClickable = true
                        rbOther.isClickable = true
                        etUnit.alpha = 1.0f
                        titleTxt.text = "Ready to create"
                    }
                }
            }


        }

        nextBtn.setOnClickListener {
            currentStep++
            moveToStep(currentStep)
        }

        skipBtn.setOnClickListener {
            rbPR.isClickable = true
            rbWeightGain.isClickable = true
            rbOther.isClickable = true
            markTutorialCompleted()
            overlayLayout.visibility = View.GONE
        }
        moveToStep(0)
    }

    private fun hideKeyboard(view: View) {
        val imm = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun showUpdateProgressDialog(goal: Goal) {
        val input = EditText(requireContext())
        input.hint = "New value (${goal.unit})"
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        input.setText(goal.currentValue.toString())
        input.setPadding(64, 32, 64, 32)

        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setTitle("Update Progress")
            .setMessage("${goal.title}\nTarget: ${goal.targetValue} ${goal.unit}")
            .setView(input)
            .setPositiveButton("Update") { _, _ ->
                val newValue = input.text.toString().toDoubleOrNull() ?: return@setPositiveButton
                viewModel.updateProgress(goal.id, newValue)

                if (goal.type == GoalType.WEIGHT_LOSS || goal.type == GoalType.WEIGHT_GAIN) {
                    if (newValue > 1400) {
                        Toast.makeText(requireContext(), "Weight cannot exceed 1400 lbs", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    profileViewModel.updateWeight(newValue)
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
        val profile = profileViewModel.myProfile.value ?: return
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
