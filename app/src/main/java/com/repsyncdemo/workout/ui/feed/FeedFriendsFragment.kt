package com.repsyncdemo.workout.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.databinding.FragmentFeedFriendsBinding
import com.repsyncdemo.workout.ui.adapter.FeedAdapter
import com.repsyncdemo.workout.ui.dialogs.ReactionDialogFragment
import com.repsyncdemo.workout.viewmodel.FeedViewModel

class FeedFriendsFragment : Fragment() {
    private var _binding: FragmentFeedFriendsBinding? = null
    private val binding get() = _binding!!
    private val feedViewModel: FeedViewModel by activityViewModels()
    private lateinit var feedAdapter: FeedAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFeedFriendsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        feedAdapter = FeedAdapter(
            onUserClick = { userId ->
                val bundle = Bundle().apply { putString("userId", userId) }
                findNavController().navigate(R.id.action_feed_to_profile, bundle)
            },
            onReactionClick = { view, postId -> // Open the ReactionDialogFragment
                val reactionDialog = ReactionDialogFragment.newInstance(postId, view)
                reactionDialog.show(childFragmentManager, "ReactionDialog")
            },
            onLikeClick = { postId -> feedViewModel.toggleLike(postId) },
            onDeleteClick = { postId -> feedViewModel.deletePost(postId) }
        )

        binding.rvFeed.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = feedAdapter
        }

        feedViewModel.feedPosts.observe(viewLifecycleOwner) { posts ->
            // In a real app, filtering might happen in ViewModel, but we reuse the shared list here if filtered
            feedAdapter.submitList(posts)
            binding.layoutEmpty.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE
        }

        feedViewModel.userProfiles.observe(viewLifecycleOwner) { profiles ->
            feedAdapter.updateProfiles(profiles)
        }

        binding.cbShowMyPosts.setOnCheckedChangeListener { _, isChecked ->
            feedViewModel.applyFilters(showChat = false, onlyFriends = true, showMyPosts = isChecked)
        }
    }

    override fun onResume() {
        super.onResume()
        // Ensure filters are applied when this tab is selected
        feedViewModel.applyFilters(showChat = false, onlyFriends = true, showMyPosts = binding.cbShowMyPosts.isChecked)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
