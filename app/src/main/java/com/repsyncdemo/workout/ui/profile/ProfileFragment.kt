package com.repsyncdemo.workout.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.material.tabs.TabLayout
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
    private val socialViewModel: SocialViewModel by activityViewModels()
    private val goalViewModel: GoalViewModel by activityViewModels()
    private val feedViewModel: FeedViewModel by activityViewModels()
    private val workoutViewModel: WorkoutViewModel by activityViewModels()
    
    private lateinit var feedAdapter: FeedAdapter
    private lateinit var friendsAdapter: FriendAdapter
    private lateinit var requestAdapter: FriendRequestAdapter
    private lateinit var searchAdapter: UserSearchAdapter
    private lateinit var miniGoalAdapter: MiniGoalAdapter
    private lateinit var workoutAdapter: WorkoutAdapter
    private lateinit var goalAdapter: GoalAdapter
    
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

        setupAdapters()
        setupRecyclerViews()
        setupListeners()
        setupTabs()
        setupSearch()
        observeViewModel()

        loadData()
    }

    private fun setupRecyclerViews() {
        binding.rvProfileContent.layoutManager = LinearLayoutManager(requireContext())
        
        binding.rvFriendRequests.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = requestAdapter
        }

        binding.rvMiniGoals.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = miniGoalAdapter
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        if (targetUserId == null) {
            binding.btnSettings.visibility = View.VISIBLE
            binding.btnBack.visibility = View.GONE
            binding.btnFriendAction.visibility = View.GONE
            binding.btnSettings.setOnClickListener {
                findNavController().navigate(R.id.action_profile_to_settings)
            }
        } else {
            binding.btnSettings.visibility = View.GONE
            binding.btnBack.visibility = View.VISIBLE
            binding.btnFriendAction.visibility = View.VISIBLE
        }
    }

    private fun loadData() {
        if (targetUserId != null) {
            profileViewModel.loadProfile(targetUserId)
            socialViewModel.loadFriendsForUser(targetUserId!!)
            socialViewModel.loadFriendshipWithUser(targetUserId!!)
            goalViewModel.loadGoalsForUser(targetUserId!!)
            workoutViewModel.loadWorkoutsForUser(targetUserId!!)
        } else {
            profileViewModel.loadProfile()
        }
    }

    private fun setupAdapters() {
        feedAdapter = FeedAdapter(
            onUserClick = { userId ->
                if (userId != targetUserId) {
                    val bundle = Bundle().apply { putString("userId", userId) }
                    findNavController().navigate(R.id.profileFragment, bundle)
                }
            },
            onLikeClick = { postId -> feedViewModel.toggleLike(postId) },
            onDeleteClick = { postId -> feedViewModel.deletePost(postId) }
        )
        
        friendsAdapter = FriendAdapter { friendship -> 
            socialViewModel.removeFriendship(friendship.id)
            Toast.makeText(requireContext(), "Friend removed", Toast.LENGTH_SHORT).show()
        }

        requestAdapter = FriendRequestAdapter(
            onAccept = { request ->
                socialViewModel.acceptRequest(request.id)
                Toast.makeText(requireContext(), "Request accepted", Toast.LENGTH_SHORT).show()
            },
            onDecline = { request -> socialViewModel.declineRequest(request.id) }
        )

        searchAdapter = UserSearchAdapter(
            currentUserId = currentUserId,
            onUserClick = { user ->
                if (user.userId != targetUserId) {
                    val bundle = Bundle().apply { putString("userId", user.userId) }
                    findNavController().navigate(R.id.profileFragment, bundle)
                }
            },
            onAddFriend = { user ->
                val myUsername = profileViewModel.myProfile.value?.username ?: "User"
                socialViewModel.sendFriendRequest(user.userId, user.username, myUsername)
                Toast.makeText(requireContext(), "Friend request sent to @${user.username}", Toast.LENGTH_SHORT).show()
            },
            onCancelRequest = { friendshipId ->
                socialViewModel.removeFriendship(friendshipId)
                Toast.makeText(requireContext(), "Request cancelled", Toast.LENGTH_SHORT).show()
            }
        )

        miniGoalAdapter = MiniGoalAdapter()

        workoutAdapter = WorkoutAdapter { workout ->
            if (targetUserId != null) {
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Copy Workout")
                    .setMessage("Do you want to copy this workout routine to your collection?")
                    .setPositiveButton("Copy") { _, _ ->
                        workoutViewModel.copyWorkout(workout)
                        Toast.makeText(requireContext(), "Workout copied!", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                val bundle = Bundle().apply { putString("workoutId", workout.id) }
                findNavController().navigate(R.id.workoutDetailFragment, bundle)
            }
        }

        goalAdapter = GoalAdapter(
            isMyProfile = targetUserId == null,
            onUpdateProgress = { goal -> 
                val bundle = Bundle().apply { putString("goalId", goal.id) }
                findNavController().navigate(R.id.goalsFragment, bundle)
            },
            onDelete = { goal -> goalViewModel.deleteGoal(goal.id) }
        )
    }

    private fun setupSearch() {
        binding.etFriendSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.isNotEmpty()) {
                    profileViewModel.searchUsers(query)
                }
                if (binding.profileTabs.selectedTabPosition == 1) {
                    updateContent(1)
                }
            }
        })
    }

    private fun setupTabs() {
        binding.profileTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                updateContent(tab?.position ?: 0)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        
        updateContent(0)
    }

    private fun updateContent(position: Int) {
        if (_binding == null) return
        
        binding.layoutFriendSearch.visibility = View.GONE
        binding.tvEmptyProfile.visibility = View.GONE
        
        when (position) {
            0 -> { // Posts
                binding.rvProfileContent.adapter = feedAdapter
                val posts = if (targetUserId != null) profileViewModel.userPosts.value else profileViewModel.myPosts.value
                feedAdapter.submitList(posts)
                if (posts.isNullOrEmpty()) {
                    binding.tvEmptyProfile.text = "No posts yet."
                    binding.tvEmptyProfile.visibility = View.VISIBLE
                }
            }
            1 -> { // Friends
                if (targetUserId == null) {
                    binding.layoutFriendSearch.visibility = View.VISIBLE
                    if (binding.etFriendSearch.text.isNullOrEmpty()) {
                        binding.rvProfileContent.adapter = friendsAdapter
                        val friends = socialViewModel.friends.value
                        friendsAdapter.submitList(friends)
                        if (friends.isNullOrEmpty()) {
                            binding.tvEmptyProfile.text = "No friends yet."
                            binding.tvEmptyProfile.visibility = View.VISIBLE
                        }
                    } else {
                        binding.rvProfileContent.adapter = searchAdapter
                        val results = profileViewModel.searchResults.value
                        searchAdapter.submitList(results)
                        if (results.isNullOrEmpty()) {
                            binding.tvEmptyProfile.text = "No users found."
                            binding.tvEmptyProfile.visibility = View.VISIBLE
                        }
                    }
                } else {
                    binding.rvProfileContent.adapter = friendsAdapter
                    val friends = socialViewModel.targetUserFriends.value
                    friendsAdapter.submitList(friends)
                    if (friends.isNullOrEmpty()) {
                        binding.tvEmptyProfile.text = "No friends yet."
                        binding.tvEmptyProfile.visibility = View.VISIBLE
                    }
                }
            }
            2 -> { // Workouts
                binding.rvProfileContent.adapter = workoutAdapter
                val profile = profileViewModel.currentProfile.value
                val isWorkoutsPublic = profile?.isWorkoutsPublic ?: true
                
                if (targetUserId != null && !isWorkoutsPublic) {
                    workoutAdapter.submitList(emptyList())
                    binding.tvEmptyProfile.text = "This user's workouts are private."
                    binding.tvEmptyProfile.visibility = View.VISIBLE
                } else {
                    val workouts = if (targetUserId != null) workoutViewModel.targetUserWorkouts.value else workoutViewModel.workouts.value
                    workoutAdapter.submitList(workouts)
                    if (workouts.isNullOrEmpty()) {
                        binding.tvEmptyProfile.text = "No saved workouts."
                        binding.tvEmptyProfile.visibility = View.VISIBLE
                    }
                }
            }
            3 -> { // Goals
                binding.rvProfileContent.adapter = goalAdapter
                val goals = if (targetUserId != null) goalViewModel.targetUserGoals.value else goalViewModel.goals.value
                goalAdapter.submitList(goals)
                if (goals.isNullOrEmpty()) {
                    binding.tvEmptyProfile.text = "No goals set."
                    binding.tvEmptyProfile.visibility = View.VISIBLE
                }
            }
            4 -> { // Analytics
                binding.rvProfileContent.adapter = null
                binding.tvEmptyProfile.text = "Analytics coming soon."
                binding.tvEmptyProfile.visibility = View.VISIBLE
            }
        }
    }

    private fun observeViewModel() {
        profileViewModel.currentProfile.observe(viewLifecycleOwner) { profile ->
            profile?.let {
                binding.tvUsername.text = "@${it.username}"
                binding.tvBio.text = it.bio.ifEmpty { "No bio set." }
                
                if (it.profilePictureUrl.isNotEmpty()) {
                    binding.ivProfilePic.load(it.profilePictureUrl) {
                        crossfade(true)
                        placeholder(android.R.drawable.ic_menu_gallery)
                        error(android.R.drawable.ic_menu_gallery)
                        transformations(CircleCropTransformation())
                    }
                } else {
                    binding.ivProfilePic.setImageResource(android.R.drawable.ic_menu_gallery)
                }

                val feet = it.heightInches / 12
                val inches = it.heightInches % 12
                binding.tvHeightValue.text = if (it.isHeightPublic || targetUserId == null) "${feet}' ${inches}\"" else "Private"
                binding.tvWeightValue.text = if (it.isWeightPublic || targetUserId == null) "${it.weightLbs.toInt()} lbs" else "Private"

                setupSocialIcon(binding.btnInstagram, it.instagramUrl)
                setupSocialIcon(binding.btnFacebook, it.facebookUrl)
                setupSocialIcon(binding.btnTwitter, it.twitterUrl)
                
                // Hide Analytics tab if it's someone else's profile
                if (targetUserId != null) {
                    binding.profileTabs.getTabAt(4)?.view?.visibility = View.GONE
                } else {
                    binding.profileTabs.getTabAt(4)?.view?.visibility = View.VISIBLE
                }
                
                if (binding.profileTabs.selectedTabPosition == 2) refreshCurrentTab()
            }
        }

        socialViewModel.pendingRequests.observe(viewLifecycleOwner) { requests ->
            if (targetUserId == null) {
                requestAdapter.submitList(requests)
                binding.tvRequestsLabel.visibility = if (requests.isNotEmpty()) View.VISIBLE else View.GONE
                binding.rvFriendRequests.visibility = if (requests.isNotEmpty()) View.VISIBLE else View.GONE
            }
        }

        profileViewModel.searchResults.observe(viewLifecycleOwner) { users ->
            if (binding.profileTabs.selectedTabPosition == 1 && targetUserId == null && !binding.etFriendSearch.text.isNullOrEmpty()) {
                searchAdapter.submitList(users)
                binding.tvEmptyProfile.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
                binding.tvEmptyProfile.text = "No users found."
            }
        }

        socialViewModel.friendshipWithTarget.observe(viewLifecycleOwner) { friendship ->
            if (targetUserId != null) {
                updateFriendButton(friendship)
            }
        }

        // Data Observers
        profileViewModel.myPosts.observe(viewLifecycleOwner) { if (targetUserId == null && binding.profileTabs.selectedTabPosition == 0) updateContent(0) }
        profileViewModel.userPosts.observe(viewLifecycleOwner) { if (targetUserId != null && binding.profileTabs.selectedTabPosition == 0) updateContent(0) }
        socialViewModel.friends.observe(viewLifecycleOwner) { 
            if (targetUserId == null && binding.profileTabs.selectedTabPosition == 1) updateContent(1) 
            // Update search list with friendship status
            searchAdapter.updateFriendships(it)
        }
        socialViewModel.myFriendships.observe(viewLifecycleOwner) { friendships ->
            searchAdapter.updateFriendships(friendships)
        }
        socialViewModel.targetUserFriends.observe(viewLifecycleOwner) { if (targetUserId != null && binding.profileTabs.selectedTabPosition == 1) updateContent(1) }
        workoutViewModel.workouts.observe(viewLifecycleOwner) { if (targetUserId == null && binding.profileTabs.selectedTabPosition == 2) updateContent(2) }
        workoutViewModel.targetUserWorkouts.observe(viewLifecycleOwner) { if (targetUserId != null && binding.profileTabs.selectedTabPosition == 2) updateContent(2) }
        goalViewModel.goals.observe(viewLifecycleOwner) { if (targetUserId == null && binding.profileTabs.selectedTabPosition == 3) updateContent(3) }
        goalViewModel.targetUserGoals.observe(viewLifecycleOwner) { if (targetUserId != null && binding.profileTabs.selectedTabPosition == 3) updateContent(3) }

        val activeGoalsSource = if (targetUserId != null) goalViewModel.targetUserGoals else goalViewModel.goals
        activeGoalsSource.observe(viewLifecycleOwner) { goals ->
            val activeGoals = goals.filter { !it.isCompleted }.take(5)
            miniGoalAdapter.submitList(activeGoals)
            binding.tvGoalsHeader.visibility = if (activeGoals.isNotEmpty()) View.VISIBLE else View.GONE
            binding.rvMiniGoals.visibility = if (activeGoals.isNotEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun updateFriendButton(friendship: Friendship?) {
        if (targetUserId == null) return
        
        binding.btnFriendAction.visibility = View.VISIBLE
        when (friendship?.status) {
            FriendshipStatus.ACCEPTED -> {
                binding.btnFriendAction.text = "Friends"
                binding.btnFriendAction.isEnabled = false
                binding.btnFriendAction.alpha = 0.6f
            }
            FriendshipStatus.PENDING -> {
                if (friendship.requesterId == currentUserId) {
                    binding.btnFriendAction.text = "Requested"
                    binding.btnFriendAction.isEnabled = true
                    binding.btnFriendAction.alpha = 0.8f
                    binding.btnFriendAction.setOnClickListener {
                        socialViewModel.removeFriendship(friendship.id)
                    }
                } else {
                    binding.btnFriendAction.text = "Accept"
                    binding.btnFriendAction.isEnabled = true
                    binding.btnFriendAction.alpha = 1.0f
                    binding.btnFriendAction.setOnClickListener {
                        socialViewModel.acceptRequest(friendship.id)
                    }
                }
            }
            else -> {
                binding.btnFriendAction.text = "Friend +"
                binding.btnFriendAction.isEnabled = true
                binding.btnFriendAction.alpha = 1.0f
                binding.btnFriendAction.setOnClickListener {
                    val targetUser = profileViewModel.currentProfile.value
                    val myProfile = profileViewModel.myProfile.value
                    if (targetUser != null && myProfile != null) {
                        socialViewModel.sendFriendRequest(targetUser.userId, targetUser.username, myProfile.username)
                    }
                }
            }
        }
    }

    private fun refreshCurrentTab() {
        updateContent(binding.profileTabs.selectedTabPosition)
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
