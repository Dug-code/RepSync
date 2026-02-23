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
import com.repsyncdemo.workout.R
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
    
    private var targetUserId: String? = null

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
        
        binding.rvProfileContent.apply {
            layoutManager = LinearLayoutManager(requireContext())
        }
        
        binding.rvFriendRequests.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = requestAdapter
        }

        binding.rvMiniGoals.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = miniGoalAdapter
        }

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        setupTabs()
        setupSearch()
        observeViewModel()

        if (targetUserId != null) {
            binding.btnSettings.visibility = View.GONE
            binding.btnBack.visibility = View.VISIBLE
            // Hide Analytics tab if it's someone else's profile
            binding.profileTabs.getTabAt(3)?.view?.visibility = View.GONE

            profileViewModel.loadProfile(targetUserId)
            socialViewModel.loadFriendsForUser(targetUserId!!)
            goalViewModel.loadGoalsForUser(targetUserId!!)
            workoutViewModel.loadWorkoutsForUser(targetUserId!!)
        } else {
            binding.btnSettings.visibility = View.VISIBLE
            binding.btnBack.visibility = View.GONE
            binding.btnSettings.setOnClickListener {
                findNavController().navigate(R.id.action_profile_to_settings)
            }
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
            onLikeClick = { postId ->
                feedViewModel.toggleLike(postId)
            },
            onDeleteClick = { postId ->
                feedViewModel.deletePost(postId)
            }
        )
        
        friendsAdapter = FriendAdapter { friendship -> 
            socialViewModel.removeFriend(friendship.id)
            Toast.makeText(requireContext(), "Friend removed", Toast.LENGTH_SHORT).show()
        }

        requestAdapter = FriendRequestAdapter(
            onAccept = { request ->
                socialViewModel.acceptRequest(request.id)
                Toast.makeText(requireContext(), "Request accepted", Toast.LENGTH_SHORT).show()
            },
            onDecline = { request ->
                socialViewModel.declineRequest(request.id)
            }
        )

        searchAdapter = UserSearchAdapter { user ->
            val myUsername = profileViewModel.currentProfile.value?.username ?: "User"
            socialViewModel.sendFriendRequest(user.userId, user.username, myUsername)
            Toast.makeText(requireContext(), "Friend request sent to @${user.username}", Toast.LENGTH_SHORT).show()
        }

        miniGoalAdapter = MiniGoalAdapter()

        workoutAdapter = WorkoutAdapter { workout ->
            if (targetUserId != null) {
                // Option to copy workout
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
    }

    private fun setupSearch() {
        binding.etFriendSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.isNotEmpty()) {
                    profileViewModel.searchUsers(query)
                    if (binding.rvProfileContent.adapter != searchAdapter) {
                        binding.rvProfileContent.adapter = searchAdapter
                    }
                } else {
                    if (binding.rvProfileContent.adapter != friendsAdapter) {
                        binding.rvProfileContent.adapter = friendsAdapter
                    }
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
        
        // Initial state
        updateContent(0)
    }

    private fun updateContent(position: Int) {
        binding.layoutFriendSearch.visibility = View.GONE
        
        when (position) {
            0 -> { // Posts
                binding.rvProfileContent.adapter = feedAdapter
                val postsSource = if (targetUserId != null) profileViewModel.userPosts else profileViewModel.myPosts
                postsSource.observe(viewLifecycleOwner) { posts ->
                    if (binding.profileTabs.selectedTabPosition == 0) {
                        feedAdapter.submitList(posts)
                        binding.tvEmptyProfile.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE
                        binding.tvEmptyProfile.text = "No posts yet."
                    }
                }
            }
            1 -> { // Friends
                if (targetUserId == null) {
                    binding.layoutFriendSearch.visibility = View.VISIBLE
                    if (binding.etFriendSearch.text.isNullOrEmpty()) {
                        binding.rvProfileContent.adapter = friendsAdapter
                    } else {
                        binding.rvProfileContent.adapter = searchAdapter
                    }
                } else {
                    binding.rvProfileContent.adapter = friendsAdapter
                }

                val friendsSource = if (targetUserId != null) socialViewModel.targetUserFriends else socialViewModel.friends
                friendsSource.observe(viewLifecycleOwner) { friends ->
                    if (binding.profileTabs.selectedTabPosition == 1 && (targetUserId != null || binding.etFriendSearch.text.isNullOrEmpty())) {
                        friendsAdapter.submitList(friends)
                        binding.tvEmptyProfile.visibility = if (friends.isEmpty()) View.VISIBLE else View.GONE
                        binding.tvEmptyProfile.text = "No friends yet."
                    }
                }
            }
            2 -> { // Workouts (Shared routines)
                binding.rvProfileContent.adapter = workoutAdapter
                
                val currentProfile = profileViewModel.currentProfile.value
                val isWorkoutsPublic = currentProfile?.isWorkoutsPublic ?: true
                
                if (targetUserId != null && !isWorkoutsPublic) {
                    workoutAdapter.submitList(emptyList())
                    binding.tvEmptyProfile.visibility = View.VISIBLE
                    binding.tvEmptyProfile.text = "This user's workouts are private."
                } else {
                    val workoutsSource = if (targetUserId != null) workoutViewModel.targetUserWorkouts else workoutViewModel.workouts
                    workoutsSource.observe(viewLifecycleOwner) { workouts ->
                        if (binding.profileTabs.selectedTabPosition == 2) {
                            workoutAdapter.submitList(workouts)
                            binding.tvEmptyProfile.visibility = if (workouts.isEmpty()) View.VISIBLE else View.GONE
                            binding.tvEmptyProfile.text = "No saved workouts."
                        }
                    }
                }
            }
            3 -> { // Analytics
                binding.rvProfileContent.adapter = null
                binding.tvEmptyProfile.visibility = View.VISIBLE
                binding.tvEmptyProfile.text = "Analytics coming soon."
            }
        }
    }

    private fun observeViewModel() {
        profileViewModel.currentProfile.observe(viewLifecycleOwner) { profile ->
            profile?.let {
                binding.tvUsername.text = "@${it.username}"
                binding.tvBio.text = if (it.bio.isNotEmpty()) it.bio else "No bio set."
                
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

                // Setup social media icons
                setupSocialIcon(binding.btnInstagram, it.instagramUrl)
                setupSocialIcon(binding.btnFacebook, it.facebookUrl)
                setupSocialIcon(binding.btnTwitter, it.twitterUrl)
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
            if (targetUserId == null && !binding.etFriendSearch.text.isNullOrEmpty()) {
                searchAdapter.submitList(users)
                binding.tvEmptyProfile.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
                binding.tvEmptyProfile.text = "No users found."
            }
        }

        val goalsSource = if (targetUserId != null) goalViewModel.targetUserGoals else goalViewModel.goals
        goalsSource.observe(viewLifecycleOwner) { goals ->
            val activeGoals = goals.filter { !it.isCompleted }.take(5)
            miniGoalAdapter.submitList(activeGoals)
            binding.tvGoalsHeader.visibility = if (activeGoals.isNotEmpty()) View.VISIBLE else View.GONE
            binding.rvMiniGoals.visibility = if (activeGoals.isNotEmpty()) View.VISIBLE else View.GONE
        }
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
