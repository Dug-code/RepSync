package com.repsyncdemo.workout.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.data.model.*
import com.repsyncdemo.workout.databinding.FragmentProfileBinding
import com.repsyncdemo.workout.ui.adapter.*
import com.repsyncdemo.workout.viewmodel.*

/**
 * Fragment that displays a user's profile.
 * Handles both the logged-in user's profile and other users' profiles.
 * Includes integration for Admin features and Friend management.
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    
    private val profileViewModel: ProfileViewModel by activityViewModels()
    private val socialViewModel: SocialViewModel by activityViewModels()
    private val workoutViewModel: WorkoutViewModel by activityViewModels()
    private val goalViewModel: GoalViewModel by activityViewModels()
    
    private lateinit var miniGoalAdapter: MiniGoalAdapter
    
    private var targetUserId: String? = null
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // If userId is passed in arguments, we are viewing another user's profile.
        // If null, we are viewing the logged-in user's own profile.
        targetUserId = arguments?.getString("userId")

        setupViewPager()
        setupListeners()
        observeViewModel()

        loadData()
    }

    /**
     * Sets up the ViewPager with tabs for Posts, Friends, Workouts, and Goals.
     */
    private fun setupViewPager() {
        val adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 4
            override fun createFragment(position: Int): Fragment {
                val bundle = Bundle().apply { putString("userId", targetUserId) }
                return when (position) {
                    0 -> ProfilePostsFragment().apply { arguments = bundle }
                    1 -> ProfileFriendsFragment().apply { arguments = bundle }
                    2 -> ProfileWorkoutsFragment().apply { arguments = bundle }
                    else -> ProfileGoalsFragment().apply { arguments = bundle }
                }
            }
        }
        binding.viewPager.adapter = adapter
        
        // Pre-load profile tabs for smooth swiping
        binding.viewPager.offscreenPageLimit = 3

        TabLayoutMediator(binding.profileTabs, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> if (targetUserId == null) "My Posts" else "Posts"
                1 -> "Friends"
                2 -> "Workouts"
                else -> "Goals"
            }
        }.attach()
    }

    /**
     * Initializes click listeners for settings, trophy shelf, and admin dashboard.
     */
    private fun setupListeners() {
        if (targetUserId == null) {
            // UI elements only visible on the user's own profile
            binding.btnSettings.visibility = View.VISIBLE
            binding.btnTrophyShelf.visibility = View.VISIBLE
            binding.btnFriendAction.visibility = View.GONE
            
            binding.btnSettings.setOnClickListener {
                findNavController().navigate(R.id.action_profile_to_settings)
            }
            binding.btnTrophyShelf.setOnClickListener {
                findNavController().navigate(R.id.action_profile_to_trophyShelf)
            }
            // ADMIN: Navigate to the management dashboard
            binding.btnAdminDashboard.setOnClickListener {
                findNavController().navigate(R.id.action_profile_to_adminDashboard)
            }
        } else {
            // UI elements for viewing another user
            binding.btnSettings.visibility = View.GONE
            binding.btnTrophyShelf.visibility = View.GONE
            binding.btnAdminDashboard.visibility = View.GONE
            binding.btnFriendAction.visibility = View.VISIBLE
        }

        miniGoalAdapter = MiniGoalAdapter()
        binding.rvMiniGoals.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = miniGoalAdapter
        }
    }

    /**
     * Triggers data loading from repositories based on the profile being viewed.
     */
    private fun loadData() {
        if (targetUserId != null) {
            profileViewModel.loadProfile(targetUserId)
            socialViewModel.loadFriendshipWithUser(targetUserId!!)
            workoutViewModel.loadWorkoutLogsForUser(targetUserId!!)
        } else {
            profileViewModel.loadProfile()
            workoutViewModel.loadWorkoutLogsForUser(currentUserId)
        }
    }

    /**
     * Sets up observers for ViewModel data to update the UI reactively.
     */
    private fun observeViewModel() {
        // Observes the profile of the user being viewed
        profileViewModel.currentProfile.observe(viewLifecycleOwner) { profile ->
            profile?.let {
                binding.tvUsername.text = "@${it.username}"
                binding.tvBio.text = it.bio.ifEmpty { "No bio set." }
                
                updateProfilePicture(it.profilePictureUrl)
                updateTrophyUI()

                val feet = it.heightInches / 12
                val inches = it.heightInches % 12
                binding.tvHeightValue.text = if (it.isHeightPublic || targetUserId == null) "${feet}' ${inches}\"" else "Private"
                binding.tvWeightValue.text = if (it.isWeightPublic || targetUserId == null) "${it.weightLbs.toInt()} lbs" else "Private"

                setupSocialIcon(binding.btnInstagram, it.instagramUrl)
                setupSocialIcon(binding.btnFacebook, it.facebookUrl)
                setupSocialIcon(binding.btnTwitter, it.twitterUrl)
            }
        }

        // Observes the logged-in user's profile to handle specific permissions
        profileViewModel.myProfile.observe(viewLifecycleOwner) { profile ->
            if (targetUserId == null) {
                // ADMIN: Only show the dashboard button if the user is an admin
                binding.btnAdminDashboard.visibility = if (profile?.isAdmin == true) View.VISIBLE else View.GONE
            }
            updateTrophyUI()
        }

        socialViewModel.friendshipWithTarget.observe(viewLifecycleOwner) { friendship ->
            updateFriendButtonUI(friendship)
        }

        val activeGoalsSource = if (targetUserId != null) goalViewModel.targetUserGoals else goalViewModel.goals
        activeGoalsSource.observe(viewLifecycleOwner) { goals ->
            val activeGoals = goals.filter { !it.isCompleted }.take(5)
            miniGoalAdapter.submitList(activeGoals)
            binding.tvGoalsHeader.visibility = if (activeGoals.isNotEmpty()) View.VISIBLE else View.GONE
            binding.rvMiniGoals.visibility = if (activeGoals.isNotEmpty()) View.VISIBLE else View.GONE
        }

        workoutViewModel.workoutLogs.observe(viewLifecycleOwner) { updateTrophyUI() }
    }

    /**
     * Updates the Friend Action button based on the current friendship status (Accepted, Pending, None).
     */
    private fun updateFriendButtonUI(friendship: Friendship?) {
        if (targetUserId == null) return
        
        val button = binding.btnFriendAction
        button.visibility = View.VISIBLE
        
        when {
            friendship == null -> {
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
                    button.text = "Requested"
                    button.setIconResource(R.drawable.ic_check_simple)
                    button.strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.text_secondary)
                    button.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                    button.setOnClickListener {
                        showUnfriendConfirmation(friendship, "Cancel friend request?")
                    }
                } else {
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
     * Loads and displays the profile picture (either a URL or a built-in color avatar).
     */
    private fun updateProfilePicture(url: String) {
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
     * Updates the UI for the pinned trophy based on user achievements and preferences.
     */
    private fun updateTrophyUI() {
        val profile = profileViewModel.myProfile.value ?: return
        val workoutCount = workoutViewModel.workoutLogs.value?.size ?: 0
        val restDayCount = profile.totalRestDays

        val pinnedTrophyId = profile.pinnedTrophyId
        val trophyToDisplay = if (pinnedTrophyId == "recovery") {
            Trophy("recovery", "Recovery", "Total rest days recorded", restDayCount, TrophyType.RECOVERY)
        } else {
            Trophy("gym_rat", "Gym Rat", "Total workouts completed", workoutCount, TrophyType.GYM_RAT)
        }
        
        val rank = trophyToDisplay.rank
        binding.ivPinnedTrophy.visibility = View.VISIBLE
        
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

    /**
     * Sets up visibility and external link behavior for social media icons.
     */
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
