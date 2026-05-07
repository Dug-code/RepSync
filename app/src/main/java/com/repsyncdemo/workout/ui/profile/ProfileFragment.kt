package com.repsyncdemo.workout.ui.profile

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.data.model.*
import com.repsyncdemo.workout.databinding.FragmentProfileBinding
import com.repsyncdemo.workout.ui.adapter.*
import com.repsyncdemo.workout.viewmodel.*

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

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
            binding.btnSettings.setOnClickListener { findNavController().navigate(R.id.action_profile_to_settings) }
            binding.btnTrophyShelf.setOnClickListener { findNavController().navigate(R.id.action_profile_to_trophyShelf) }
            binding.btnAdminDashboard.setOnClickListener { findNavController().navigate(R.id.action_profile_to_adminDashboard) }
            binding.btnCreateGoalFromProfile.setOnClickListener { findNavController().navigate(R.id.goalsFragment) }
            
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

        // Trophies depend on workout logs
        workoutViewModel.workoutLogs.observe(viewLifecycleOwner) { updateTrophyUI() }
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
        val workoutCount = workoutViewModel.workoutLogs.value?.size ?: 0
        val restDayCount = profile.totalRestDays
        val pinnedTrophyId = profile.pinnedTrophyId
        
        // Determine which trophy to display
        val trophyToDisplay = if (pinnedTrophyId == "recovery") {
            Trophy("recovery", "Recovery", "Total rest days recorded", restDayCount, TrophyType.RECOVERY)
        } else {
            Trophy("gym_rat", "Gym Rat", "Total workouts completed", workoutCount, TrophyType.GYM_RAT)
        }
        
        val rank = trophyToDisplay.rank
        binding.ivPinnedTrophy.visibility = View.VISIBLE
        
        // Resolve icon resource based on type and rank
        val iconRes = when (trophyToDisplay.type) {
            TrophyType.GYM_RAT -> when (rank) {
                TrophyRank.BRONZE -> R.drawable.gym_rat_bronze
                TrophyRank.SILVER -> R.drawable.gym_rat_silver
                TrophyRank.GOLD -> R.drawable.gym_rat_gold
                TrophyRank.DIAMOND -> R.drawable.gym_rat_diamond
                else -> R.drawable.gym_rat_locked
            }
            TrophyType.RECOVERY -> when (rank) {
                TrophyRank.BRONZE -> R.drawable.zzz_icon_bronze
                TrophyRank.SILVER -> R.drawable.zzz_icon_silver
                TrophyRank.GOLD -> R.drawable.zzz_icon_gold
                TrophyRank.DIAMOND -> R.drawable.zzz_icon_diamond
                else -> R.drawable.zzz_icon_locked
            }
            else -> R.drawable.ic_trophy
        }
        binding.ivPinnedTrophy.setImageResource(iconRes)
        binding.ivPinnedTrophy.imageTintList = null
        binding.ivPinnedTrophy.background = null
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

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = activity?.currentFocus ?: view
        view?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tabMediator?.detach()
        tabMediator = null
        _binding = null
    }
}
