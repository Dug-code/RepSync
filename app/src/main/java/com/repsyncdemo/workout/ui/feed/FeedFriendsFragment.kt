package com.repsyncdemo.workout.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.databinding.FragmentFeedFriendsBinding
import com.repsyncdemo.workout.ui.adapter.FeedAdapter
import com.repsyncdemo.workout.ui.dialogs.ReactionDialogFragment
import com.repsyncdemo.workout.viewmodel.FeedViewModel
import com.repsyncdemo.workout.viewmodel.ProfileViewModel

class FeedFriendsFragment : Fragment() {
    private var _binding: FragmentFeedFriendsBinding? = null
    private val binding get() = _binding!!
    private val feedViewModel: FeedViewModel by activityViewModels()
    private val profileViewModel: ProfileViewModel by activityViewModels()
    private lateinit var feedAdapter: FeedAdapter
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFeedFriendsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        feedAdapter = FeedAdapter(
            onUserClick = { userId ->
                if (userId == currentUserId) {
                    findNavController().navigate(R.id.profileFragment)
                } else {
                    val bundle = Bundle().apply { putString("userId", userId) }
                    findNavController().navigate(R.id.action_feed_to_profile, bundle)
                }
            },
            onReactionClick = { view, postId -> 
                val reactionDialog = ReactionDialogFragment.newInstance(postId, view)
                reactionDialog.show(childFragmentManager, "ReactionDialog")
            },
            onDeleteClick = { postId -> 
                val post = feedAdapter.currentList.find { it.id == postId }
                post?.let { feedViewModel.deletePost(it) }
            }
        )

        binding.rvFeed.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = feedAdapter
        }

        feedViewModel.friendsPosts.observe(viewLifecycleOwner) { posts ->
            feedAdapter.submitList(posts)
            binding.layoutEmpty.visibility = if (posts.isNullOrEmpty()) View.VISIBLE else View.GONE
            binding.rvFeed.visibility = if (posts.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        feedViewModel.userProfiles.observe(viewLifecycleOwner) { profiles ->
            feedAdapter.updateProfiles(profiles)
        }
        
        profileViewModel.myProfile.observe(viewLifecycleOwner) { profile ->
            feedAdapter.setCurrentUserProfile(profile)
        }

        binding.cbShowMyPosts.setOnCheckedChangeListener { _, isChecked ->
            feedViewModel.applyFriendsFilters(showMyPosts = isChecked)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
