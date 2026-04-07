package com.repsyncdemo.workout.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
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

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    
    private val profileViewModel: ProfileViewModel by activityViewModels()
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

        targetUserId = arguments?.getString("userId")

        setupViewPager()
        setupListeners()
        observeViewModel()

        loadData()
    }

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

    private fun setupListeners() {
        if (targetUserId == null) {
            binding.btnSettings.visibility = View.VISIBLE
            binding.btnTrophyShelf.visibility = View.VISIBLE
            binding.btnFriendAction.visibility = View.GONE
            
            binding.btnSettings.setOnClickListener {
                findNavController().navigate(R.id.action_profile_to_settings)
            }
            binding.btnTrophyShelf.setOnClickListener {
                findNavController().navigate(R.id.action_profile_to_trophyShelf)
            }
        } else {
            binding.btnSettings.visibility = View.GONE
            binding.btnTrophyShelf.visibility = View.GONE
            binding.btnFriendAction.visibility = View.VISIBLE
        }

        miniGoalAdapter = MiniGoalAdapter()
        binding.rvMiniGoals.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = miniGoalAdapter
        }
    }

    private fun loadData() {
        if (targetUserId != null) {
            profileViewModel.loadProfile(targetUserId)
            workoutViewModel.loadWorkoutLogsForUser(targetUserId!!)
        } else {
            profileViewModel.loadProfile()
            workoutViewModel.loadWorkoutLogsForUser(currentUserId)
        }
    }

    private fun observeViewModel() {
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

        val activeGoalsSource = if (targetUserId != null) goalViewModel.targetUserGoals else goalViewModel.goals
        activeGoalsSource.observe(viewLifecycleOwner) { goals ->
            val activeGoals = goals.filter { !it.isCompleted }.take(5)
            miniGoalAdapter.submitList(activeGoals)
            binding.tvGoalsHeader.visibility = if (activeGoals.isNotEmpty()) View.VISIBLE else View.GONE
            binding.rvMiniGoals.visibility = if (activeGoals.isNotEmpty()) View.VISIBLE else View.GONE
        }

        workoutViewModel.workoutLogs.observe(viewLifecycleOwner) { updateTrophyUI() }
        profileViewModel.myProfile.observe(viewLifecycleOwner) { updateTrophyUI() }
    }

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
