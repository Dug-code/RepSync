package com.repsyncdemo.workout.ui.profile

/**
 * File overview: Displays the current or target user profile, social actions, mini goals, trophies, and profile tabs.
 */

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.data.model.*
import com.repsyncdemo.workout.databinding.FragmentProfileBinding
import com.repsyncdemo.workout.ui.adapter.*
import com.repsyncdemo.workout.viewmodel.*
import com.takusemba.spotlight.OnSpotlightListener
import com.takusemba.spotlight.Spotlight
import com.takusemba.spotlight.Target
import com.takusemba.spotlight.shape.Circle
import com.takusemba.spotlight.shape.RoundedRectangle

/**
 * ProfileFragment displays user information, social links, trophies, and active goals.
 * It supports viewing both the logged-in user's profile and other users' profiles.
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    
    // ViewModels shared across the activity scope
    private val profileViewModel: ProfileViewModel by activityViewModels()
    private val socialViewModel: SocialViewModel by activityViewModels()
    private val workoutViewModel: WorkoutViewModel by activityViewModels()
    private val goalViewModel: GoalViewModel by activityViewModels()
    
    private lateinit var miniGoalAdapter: MiniGoalAdapter
    
    // ID of the user being viewed. If null, displays the current logged-in user's profile.
    private var targetUserId: String? = null
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private var tabMediator: TabLayoutMediator? = null
    private var spotlight: Spotlight? = null
    private var isHandingOffToGoalTabTutorial = false

    companion object {
        private const val PREFS_NAME = "repsync_prefs"
        private const val KEY_PROFILE_TUTORIAL_COMPLETED = "profile_tutorial_completed"
    }

    // Sets up this screen.
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Connects views, clicks, and data.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Retrieve target userId from navigation arguments if viewing someone else
        targetUserId = arguments?.getString("userId")?.takeUnless { it == currentUserId }
        
        setupListeners()
        observeViewModel()
        loadData()
    }

    /**
     * Configures the ViewPager2 and TabLayout based on user settings and privacy.
     * @param isFriendsListPublic Whether the user's friends list is visible to others.
     */
    private fun setupViewPager(isFriendsListPublic: Boolean) {
        val showFriendsTab = isFriendsListPublic || targetUserId == null
        val totalTabs = if (showFriendsTab) 4 else 3

        val adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = totalTabs
            override fun createFragment(position: Int): Fragment {
                val bundle = Bundle().apply { putString("userId", targetUserId) }
                return if (showFriendsTab) {
                    when (position) {
                        0 -> ProfilePostsFragment().apply { arguments = bundle }
                        1 -> ProfileFriendsFragment().apply { arguments = bundle }
                        2 -> ProfileWorkoutsFragment().apply { arguments = bundle }
                        else -> ProfileGoalsFragment().apply { arguments = bundle }
                    }
                } else {
                    when (position) {
                        0 -> ProfilePostsFragment().apply { arguments = bundle }
                        1 -> ProfileWorkoutsFragment().apply { arguments = bundle }
                        else -> ProfileGoalsFragment().apply { arguments = bundle }
                    }
                }
            }
        }
        binding.viewPager.adapter = adapter
        binding.viewPager.offscreenPageLimit = 3
        
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                hideKeyboard()
            }
        })

        tabMediator?.detach()
        tabMediator = TabLayoutMediator(binding.profileTabs, binding.viewPager) { tab, position ->
            tab.text = if (showFriendsTab) {
                when (position) {
                    0 -> if (targetUserId == null) "My Posts" else "Posts"
                    1 -> "Friends"
                    2 -> "Shared Templates"
                    else -> "Goals"
                }
            } else {
                when (position) {
                    0 -> if (targetUserId == null) "My Posts" else "Posts"
                    1 -> "Shared Templates"
                    else -> "Goals"
                }
            }
        }
        tabMediator?.attach()
        checkTutorial()
    }

    /**
     * Initializes UI component listeners and basic visibility logic.
     */
    private fun setupListeners() {
        if (targetUserId == null) {
            // Own profile: show settings, trophy shelf, and admin dashboard (if applicable)
            binding.btnSettings.visibility = View.VISIBLE
            binding.btnTrophyShelf.visibility = View.VISIBLE
            binding.btnFriendAction.visibility = View.GONE
            binding.btnSettings.setOnClickListener {
                finishProfileTutorialBeforeNavigation()
                findNavController().navigate(R.id.action_profile_to_settings)
            }
            binding.btnTrophyShelf.setOnClickListener {
                finishProfileTutorialBeforeNavigation()
                findNavController().navigate(R.id.action_profile_to_trophyShelf)
            }
            binding.btnAdminDashboard.setOnClickListener {
                finishProfileTutorialBeforeNavigation()
                findNavController().navigate(R.id.action_profile_to_adminDashboard)
            }
            binding.btnCreateGoalFromProfile.setOnClickListener {
                finishProfileTutorialBeforeNavigation()
                findNavController().navigate(R.id.goalsFragment)
            }
            
            // Allow clicking on weight layout to update it
            binding.layoutWeight.setOnClickListener { showWeighInDialog() }
            binding.layoutWeight.isClickable = true
            binding.layoutWeight.isFocusable = true
        } else {
            // Target profile: hide settings/trophies, show friend action button
            binding.btnSettings.visibility = View.GONE
            binding.btnTrophyShelf.visibility = View.GONE
            binding.btnFriendAction.visibility = View.VISIBLE
            binding.layoutWeight.isClickable = false
        }

        miniGoalAdapter = MiniGoalAdapter()
        binding.rvMiniGoals.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = miniGoalAdapter
        }
    }

    /**
     * Displays a dialog for the user to log their current weight and reminder preferences.
     */
    private fun showWeighInDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_weigh_in, null)
        val etWeight = dialogView.findViewById<EditText>(R.id.etWeight)
        val spinnerFreq = dialogView.findViewById<AutoCompleteTextView>(R.id.spinnerFrequency)
        val layoutCustom = dialogView.findViewById<LinearLayout>(R.id.layoutCustomDays)
        
        val freqOptions = arrayOf("Never", "Every Day", "Select Days")
        spinnerFreq.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, freqOptions))

        // Pre-fill existing data
        profileViewModel.myProfile.value?.let { p ->
            etWeight.setText(p.weightLbs.toString())
            spinnerFreq.setText(when(p.weighInFrequency) {
                "daily" -> "Every Day"
                "custom" -> "Select Days"
                else -> "Never"
            }, false)
            if (p.weighInFrequency == "custom") {
                layoutCustom.visibility = View.VISIBLE
                dialogView.findViewById<MaterialCheckBox>(R.id.cbMon).isChecked = p.weighInDays.contains(1)
                dialogView.findViewById<MaterialCheckBox>(R.id.cbTue).isChecked = p.weighInDays.contains(2)
                dialogView.findViewById<MaterialCheckBox>(R.id.cbWed).isChecked = p.weighInDays.contains(3)
                dialogView.findViewById<MaterialCheckBox>(R.id.cbThu).isChecked = p.weighInDays.contains(4)
                dialogView.findViewById<MaterialCheckBox>(R.id.cbFri).isChecked = p.weighInDays.contains(5)
                dialogView.findViewById<MaterialCheckBox>(R.id.cbSat).isChecked = p.weighInDays.contains(6)
                dialogView.findViewById<MaterialCheckBox>(R.id.cbSun).isChecked = p.weighInDays.contains(7)
            }
        }

        spinnerFreq.setOnItemClickListener { _, _, position, _ ->
            layoutCustom.visibility = if (position == 2) View.VISIBLE else View.GONE
        }

        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val weight = etWeight.text.toString().toDoubleOrNull() ?: 0.0
                if (weight > 1400) {
                    Toast.makeText(requireContext(), "Weight cannot exceed 1400 lbs", Toast.LENGTH_SHORT).show()
                } else if (weight > 0) {
                    saveWeighInData(weight, spinnerFreq.text.toString(), dialogView)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Updates the user's profile with new weight and reminder settings.
     */
    private fun saveWeighInData(weight: Double, freqText: String, view: View) {
        val freq = when(freqText) {
            "Every Day" -> "daily"
            "Select Days" -> "custom"
            else -> "never"
        }
        val selectedDays = mutableListOf<Int>()
        if (freq == "custom") {
            if (view.findViewById<MaterialCheckBox>(R.id.cbMon).isChecked) selectedDays.add(1)
            if (view.findViewById<MaterialCheckBox>(R.id.cbTue).isChecked) selectedDays.add(2)
            if (view.findViewById<MaterialCheckBox>(R.id.cbWed).isChecked) selectedDays.add(3)
            if (view.findViewById<MaterialCheckBox>(R.id.cbThu).isChecked) selectedDays.add(4)
            if (view.findViewById<MaterialCheckBox>(R.id.cbFri).isChecked) selectedDays.add(5)
            if (view.findViewById<MaterialCheckBox>(R.id.cbSat).isChecked) selectedDays.add(6)
            if (view.findViewById<MaterialCheckBox>(R.id.cbSun).isChecked) selectedDays.add(7)
        }
        
        val updates = mapOf(
            "weightLbs" to weight,
            "weighInFrequency" to freq,
            "weighInDays" to selectedDays,
            "lastWeighInDate" to System.currentTimeMillis()
        )
        
        profileViewModel.updateProfileFields(updates)
        Toast.makeText(requireContext(), "Weight updated!", Toast.LENGTH_SHORT).show()
    }

    /**
     * Triggers data loading in ViewModels.
     */
    private fun loadData() {
        if (targetUserId != null) {
            // Loading data for a specific user
            profileViewModel.observeProfile(targetUserId)
            profileViewModel.loadUserPosts(targetUserId!!)
            socialViewModel.loadFriendshipWithUser(targetUserId!!)
            socialViewModel.loadFriendsForUser(targetUserId!!)
            workoutViewModel.loadWorkoutLogsForUser(targetUserId!!)
            workoutViewModel.loadWorkoutsForUser(targetUserId!!)
            goalViewModel.loadGoalsForUser(targetUserId!!)
        } else {
            // Loading current user's own data
            profileViewModel.observeProfile()
            profileViewModel.loadMyPosts()
            workoutViewModel.loadWorkoutLogsForUser(currentUserId)
        }
    }

    /**
     * Sets up observers for ViewModel LiveData to update UI dynamically.
     */
    private fun observeViewModel() {
        profileViewModel.currentProfile.observe(viewLifecycleOwner) { profile ->
            profile?.let {
                binding.tvUsername.text = "@${it.username}"
                binding.ivAdminBadge.visibility = if (it.isAdmin) View.VISIBLE else View.GONE
                fitUsernameText()
                binding.tvBio.text = it.bio.ifEmpty { "No bio set." }
                updateProfilePicture(it.profilePictureUrl)
                
                // Height formatting
                val feet = it.heightInches / 12
                val inches = it.heightInches % 12
                binding.tvHeightValue.text = if (it.isHeightPublic || targetUserId == null) "${feet}' ${inches}\"" else "Private"
                binding.tvWeightValue.text = if (it.isWeightPublic || targetUserId == null) "${it.weightLbs.toInt()} lbs" else "Private"
                
                // Social links
                setupSocialIcon(binding.btnInstagram, it.instagramUrl)
                setupSocialIcon(binding.btnFacebook, it.facebookUrl)
                setupSocialIcon(binding.btnTwitter, it.twitterUrl)
                
                updateTrophyUI()

                if (targetUserId == null) {
                    binding.btnAdminDashboard.visibility = if (it.isAdmin) View.VISIBLE else View.GONE
                } else {
                    binding.btnAdminDashboard.visibility = View.GONE
                }

                // Re-setup viewpager based on privacy setting
                setupViewPager(it.isFriendsListPublic)
            }
        }
        
        // Friendship status observer
        socialViewModel.friendshipWithTarget.observe(viewLifecycleOwner) { updateFriendButtonUI(it) }
        
        // Active goals observer
        val activeGoalsSource = if (targetUserId != null) goalViewModel.targetUserGoals else goalViewModel.goals
        activeGoalsSource.observe(viewLifecycleOwner) { goals ->
            val activeGoals = goals.filter { !it.isCompleted }.take(5)
            val showActiveGoals = activeGoals.isNotEmpty()
            val showEmptyGoals = !showActiveGoals && targetUserId == null

            miniGoalAdapter.submitList(activeGoals)
            binding.tvGoalsHeader.visibility = if (showActiveGoals) View.VISIBLE else View.GONE
            binding.rvMiniGoals.visibility = if (showActiveGoals) View.VISIBLE else View.GONE
            binding.layoutEmptyGoals.visibility = if (showEmptyGoals) View.VISIBLE else View.GONE
        }

        // Trophies depend on workout and profile stats.
        workoutViewModel.workoutLogs.observe(viewLifecycleOwner) { updateTrophyUI() }
        workoutViewModel.workouts.observe(viewLifecycleOwner) { updateTrophyUI() }
        workoutViewModel.weightLogs.observe(viewLifecycleOwner) { updateTrophyUI() }
        goalViewModel.goals.observe(viewLifecycleOwner) { updateTrophyUI() }
        socialViewModel.friends.observe(viewLifecycleOwner) { updateTrophyUI() }
        profileViewModel.myProfile.observe(viewLifecycleOwner) { updateTrophyUI() }
    }

    /**
     * Updates the "Friend" button UI based on the current friendship status with the target user.
     */
    private fun updateFriendButtonUI(friendship: Friendship?) {
        if (targetUserId == null) return
        val button = binding.btnFriendAction
        button.visibility = View.VISIBLE
        when {
            friendship == null -> {
                // Not friends
                button.text = "Friend +"
                button.setIconResource(R.drawable.ic_plus_simple)
                button.strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.primary)
                button.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
                button.setOnClickListener {
                    val targetUser = profileViewModel.currentProfile.value
                    val myProfile = profileViewModel.myProfile.value
                    if (targetUser != null && myProfile != null) {
                        socialViewModel.sendFriendRequest(targetUser.userId, targetUser.username, myProfile.username)
                        Toast.makeText(requireContext(), "Friend request sent!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            friendship.status == FriendshipStatus.PENDING -> {
                if (friendship.requesterId == currentUserId) {
                    // Outgoing request
                    button.text = "Requested"
                    button.setIconResource(R.drawable.ic_check_simple)
                    button.strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.text_secondary)
                    button.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                    button.setOnClickListener { showUnfriendConfirmation(friendship, "Cancel friend request?") }
                } else {
                    // Incoming request
                    button.text = "Accept"
                    button.setIconResource(R.drawable.ic_check_simple)
                    button.strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.success)
                    button.setTextColor(ContextCompat.getColor(requireContext(), R.color.success))
                    button.setOnClickListener {
                        socialViewModel.acceptRequest(friendship.id)
                        Toast.makeText(requireContext(), "Request accepted!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            friendship.status == FriendshipStatus.ACCEPTED -> {
                // Already friends
                button.text = "Friends"
                button.setIconResource(R.drawable.ic_check_simple)
                button.strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.primary)
                button.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
                button.setOnClickListener {
                    val username = profileViewModel.currentProfile.value?.username ?: "this user"
                    showUnfriendConfirmation(friendship, "Are you sure you want to unfriend @$username?")
                }
            }
        }
    }

    /**
     * Shows a confirmation dialog before removing a friend or cancelling a request.
     */
    private fun showUnfriendConfirmation(friendship: Friendship, message: String) {
        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setTitle("Manage Friendship")
            .setMessage(message)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Remove") { _, _ ->
                socialViewModel.removeFriendship(friendship.id)
                Toast.makeText(requireContext(), "Friendship updated", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    /**
     * Loads the profile picture from a URL or uses a local resource placeholder.
     */
    private fun updateProfilePicture(url: String) {
        binding.ivProfilePic.setImageDrawable(null)

        if (url.isNotEmpty() && (url.startsWith("http") || url.startsWith("https"))) {
            binding.ivProfilePic.load(url) {
                crossfade(true)
                placeholder(R.drawable.ic_profile_red)
                error(R.drawable.ic_profile_red)
                transformations(CircleCropTransformation())
            }
        } else {
            val resId = when(url) { 
                "red" -> R.drawable.ic_profile_red
                "blue" -> R.drawable.ic_profile_blue
                "green" -> R.drawable.ic_profile_green
                "yellow" -> R.drawable.ic_profile_yellow
                "purple" -> R.drawable.ic_profile_purple
                else -> R.drawable.ic_profile_grey
            }
            binding.ivProfilePic.setImageResource(resId)
        }
    }

    /**
     * Updates the UI for the "Pinned Trophy" based on user progress and selection.
     */
    private fun updateTrophyUI() {
        val profile = profileViewModel.myProfile.value ?: return
        val pinnedTrophyId = profile.pinnedTrophyId.toCurrentTrophyId()
        val trophies = buildProfileTrophies(profile)
        val trophyToDisplay = trophies
            .firstOrNull { it.id == pinnedTrophyId }
            ?: trophies.first()

        val rank = trophyToDisplay.rank
        binding.ivPinnedTrophy.visibility = View.VISIBLE
        binding.ivPinnedTrophy.setImageResource(trophyToDisplay.iconRes(rank))
        binding.ivPinnedTrophy.imageTintList = null
        binding.ivPinnedTrophy.background = null
    }

    private fun String?.toCurrentTrophyId(): String? = when (this) {
        "full_time" -> "tick_tock"
        "cardio_bunny" -> "cardio_champ"
        "bench_press", "squat", "deadlift", "shoulder_press", "dumbbell_master" -> "heavy_hitter"
        "pushup_master", "pullup_master", "situp_master" -> "rep_machine"
        "abs_master" -> "set_collector"
        "all_star" -> "lift_king"
        else -> this
    }

    private fun buildProfileTrophies(profile: UserProfile): List<Trophy> {
        val logs = workoutViewModel.workoutLogs.value ?: emptyList()
        val routines = workoutViewModel.workouts.value ?: emptyList()
        val weightLogs = workoutViewModel.weightLogs.value ?: emptyList()
        val goals = goalViewModel.goals.value ?: emptyList()
        val friendsCount = socialViewModel.friends.value?.size ?: 0
        val weighInCount = maxOf(weightLogs.size, if (profile.lastWeighInDate > 0L) 1 else 0)

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
                    if (set.completed) totalSets++
                    if (set.completed && (ex.type == ExerciseType.STRENGTH || ex.type == ExerciseType.CALISTHENICS)) {
                        val weight = set.weight ?: 0.0
                        val reps = set.reps ?: 0
                        totalVolume += weight * reps
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
        return listOf(
            Trophy("gym_rat", "Gym Rat", "Total workouts completed", logs.size, TrophyType.GYM_RAT),
            Trophy("recovery", "Recovery", "Total rest days recorded", profile.totalRestDays, TrophyType.RECOVERY),
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
    }


    //Shows social media icon if the URL is present and sets up its click listener.

    private fun setupSocialIcon(button: View, url: String) {
        if (url.isNotEmpty()) {
            button.visibility = View.VISIBLE
            button.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
        } else {
            button.visibility = View.GONE
        }
    }

    private fun fitUsernameText() {
        val usernameView = binding.tvUsername
        usernameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        usernameView.post {
            val availableWidth = usernameView.width - usernameView.paddingLeft - usernameView.paddingRight
            if (availableWidth <= 0) return@post

            val text = usernameView.text.toString()
            for (sizeSp in 20 downTo 12) {
                usernameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp.toFloat())
                if (usernameView.paint.measureText(text) <= availableWidth) {
                    return@post
                }
            }
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = activity?.currentFocus ?: view
        view?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    private fun checkTutorial() {
        if (targetUserId != null || spotlight != null) return
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isCompleted = prefs.getBoolean(KEY_PROFILE_TUTORIAL_COMPLETED, false)
        if (!isCompleted) {
            binding.root.doOnPreDraw {
                binding.root.postDelayed({ showTutorial() }, 250L)
            }
        }
    }

    private fun showTutorial() {
        if (binding.btnTrophyShelf.visibility != View.VISIBLE || binding.btnSettings.visibility != View.VISIBLE) return

        val targets = ArrayList<Target>()
        targets.add(createCircleTarget(binding.btnTrophyShelf, "Trophy Shelf", "Review unlocked trophies and choose what to show on your profile."))
        targets.add(createCircleTarget(binding.btnSettings, "Settings", "Manage account details, privacy, and profile preferences."))
        targets.add(createTarget(binding.llHeightWeight, "Height & Weight", "Track profile measurements and tap weight to log updates.", 20f))

        binding.profileTabs.getTabAt(0)?.view?.let {
            targets.add(createTarget(it, "Posts", "See your shared feed posts.", overlayVerticalBias = 0.18f))
        }
        binding.profileTabs.getTabAt(1)?.view?.let {
            targets.add(createTarget(it, "Friends", "View your friends list.", overlayVerticalBias = 0.18f))
        }
        binding.profileTabs.getTabAt(2)?.view?.let {
            targets.add(
                createTarget(
                    it,
                    "Shared Templates",
                    "Browse workout templates shared from this profile.",
                    overlayVerticalBias = 0.18f,
                    onNext = {
                        isHandingOffToGoalTabTutorial = true
                        spotlight?.finish()
                        spotlight = null
                        scrollProfileTabsToEnd()
                        binding.profileTabs.postDelayed({ showGoalTabTutorial() }, 300L)
                    }
                )
            )
        }

        if (targets.isEmpty()) return

        isHandingOffToGoalTabTutorial = false
        setTutorialScrollingEnabled(false)
        spotlight = Spotlight.Builder(requireActivity())
            .setContainer(binding.root as ViewGroup)
            .setTargets(targets)
            .setBackgroundColorRes(R.color.spotlight_background)
            .setDuration(400L)
            .setAnimation(DecelerateInterpolator(2f))
            .setOnSpotlightListener(object : OnSpotlightListener {
                override fun onStarted() = Unit
                override fun onEnded() {
                    if (!isHandingOffToGoalTabTutorial) {
                        setTutorialScrollingEnabled(true)
                        markTutorialCompleted()
                    }
                }
            })
            .build()

        spotlight?.start()
    }

    private fun showGoalTabTutorial() {
        val goalTab = binding.profileTabs.getTabAt(3)?.view ?: run {
            setTutorialScrollingEnabled(true)
            markTutorialCompleted()
            return
        }

        isHandingOffToGoalTabTutorial = false
        spotlight = Spotlight.Builder(requireActivity())
            .setContainer(binding.root as ViewGroup)
            .setTargets(
                listOf(
                    createTarget(
                        goalTab,
                        "Goals",
                        "Check active and completed goals.",
                        overlayVerticalBias = 0.18f
                    )
                )
            )
            .setBackgroundColorRes(R.color.spotlight_background)
            .setDuration(400L)
            .setAnimation(DecelerateInterpolator(2f))
            .setOnSpotlightListener(object : OnSpotlightListener {
                override fun onStarted() = Unit
                override fun onEnded() {
                    setTutorialScrollingEnabled(true)
                    markTutorialCompleted()
                }
            })
            .build()

        spotlight?.start()
    }

    private fun createCircleTarget(
        view: View,
        title: String,
        description: String,
        overlayVerticalBias: Float = 0.8f,
        onNext: (() -> Unit)? = null
    ): Target {
        val radius = (maxOf(view.width, view.height).toFloat() / 2f).coerceAtLeast(1f)
        return Target.Builder()
            .setAnchor(view)
            .setShape(Circle(radius))
            .setOverlay(createOverlay(title, description, overlayVerticalBias, onNext))
            .build()
    }

    private fun createTarget(
        view: View,
        title: String,
        description: String,
        cornerRadius: Float = 8f,
        overlayVerticalBias: Float = 0.8f,
        onNext: (() -> Unit)? = null
    ): Target {
        return Target.Builder()
            .setAnchor(view)
            .setShape(RoundedRectangle(view.height.toFloat(), view.width.toFloat(), cornerRadius))
            .setOverlay(createOverlay(title, description, overlayVerticalBias, onNext))
            .build()
    }

    private fun createOverlay(
        title: String,
        description: String,
        verticalBias: Float,
        onNext: (() -> Unit)?
    ): View {
        val overlay = layoutInflater.inflate(R.layout.layout_spotlight_overlay, binding.root, false)
        val containerInfo = overlay.findViewById<LinearLayout>(R.id.containerInfo)
        val params = containerInfo.layoutParams as ConstraintLayout.LayoutParams
        params.verticalBias = verticalBias
        containerInfo.layoutParams = params

        overlay.findViewById<TextView>(R.id.tvTitle).text = title
        overlay.findViewById<TextView>(R.id.tvDescription).text = description
        overlay.findViewById<Button>(R.id.btnNext).setOnClickListener {
            if (onNext != null) {
                onNext()
            } else {
                spotlight?.next()
            }
        }
        overlay.findViewById<Button>(R.id.btnSkip).setOnClickListener {
            spotlight?.finish()
            setTutorialScrollingEnabled(true)
            markTutorialCompleted()
        }
        return overlay
    }

    private fun scrollProfileTabsToEnd() {
        val goalTab = binding.profileTabs.getTabAt(3)?.view
        val targetScrollX = goalTab?.left ?: run {
            val tabStrip = binding.profileTabs.getChildAt(0) ?: return
            (tabStrip.width - binding.profileTabs.width).coerceAtLeast(0)
        }
        binding.profileTabs.smoothScrollTo(targetScrollX, 0)
    }

    private fun setTutorialScrollingEnabled(enabled: Boolean) {
        val binding = _binding ?: return
        binding.viewPager.isUserInputEnabled = enabled
        binding.profileTabs.isEnabled = enabled
        binding.appBarLayout.setExpanded(true, false)

        val params = binding.appBarLayout.layoutParams as? CoordinatorLayout.LayoutParams ?: return
        val behavior = (params.behavior as? AppBarLayout.Behavior) ?: AppBarLayout.Behavior().also {
            params.behavior = it
            binding.appBarLayout.layoutParams = params
        }

        behavior.setDragCallback(object : AppBarLayout.Behavior.DragCallback() {
            override fun canDrag(appBarLayout: AppBarLayout): Boolean = enabled
        })
    }

    private fun finishProfileTutorialBeforeNavigation() {
        isHandingOffToGoalTabTutorial = false
        setTutorialScrollingEnabled(true)
        spotlight?.finish()
        spotlight = null
    }

    private fun markTutorialCompleted() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_PROFILE_TUTORIAL_COMPLETED, true) }
    }

    // Clears the view binding.
    override fun onDestroyView() {
        setTutorialScrollingEnabled(true)
        spotlight?.finish()
        spotlight = null
        tabMediator?.detach()
        tabMediator = null
        _binding = null
        super.onDestroyView()
    }
}
