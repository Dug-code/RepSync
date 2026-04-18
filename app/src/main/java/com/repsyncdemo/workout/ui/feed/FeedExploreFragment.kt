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
import com.repsyncdemo.workout.databinding.FragmentFeedExploreBinding
import com.repsyncdemo.workout.ui.adapter.FeedAdapter
import com.repsyncdemo.workout.ui.dialogs.ReactionDialogFragment
import com.repsyncdemo.workout.viewmodel.FeedViewModel

class FeedExploreFragment : Fragment() {
    private var _binding: FragmentFeedExploreBinding? = null
    private val binding get() = _binding!!
    private val feedViewModel: FeedViewModel by activityViewModels()
    private lateinit var feedAdapter: FeedAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFeedExploreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        feedAdapter = FeedAdapter(
            onUserClick = { userId ->
                val bundle = Bundle().apply { putString("userId", userId) }
                findNavController().navigate(R.id.action_feed_to_profile, bundle)
            },
            onLikeClick = { postId -> feedViewModel.toggleLike(postId) },
            onReactionClick = { view, postId -> // Open the ReactionDialogFragment
                val reactionDialog = ReactionDialogFragment.newInstance(postId, view)
                reactionDialog.show(childFragmentManager, "ReactionDialog")
            },
            onDeleteClick = { postId -> feedViewModel.deletePost(postId) },
            onEditChatClick = { post ->
                // Note: The parent FeedFragment handles the dialog, or we can add local handling if needed
            }
        )

        binding.rvFeed.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = feedAdapter
        }

        feedViewModel.feedPosts.observe(viewLifecycleOwner) { posts ->
            feedAdapter.submitList(posts)
            binding.layoutEmpty.visibility = if (posts.isNullOrEmpty()) View.VISIBLE else View.GONE
        }

        feedViewModel.userProfiles.observe(viewLifecycleOwner) { profiles ->
            feedAdapter.updateProfiles(profiles)
        }

        binding.switchGlobal.setOnCheckedChangeListener { _, isChecked ->
            binding.radiusSlider.visibility = if (isChecked) View.GONE else View.VISIBLE
            updateFilters()
        }

        binding.radiusSlider.addOnChangeListener { _, _, _ -> updateFilters() }

        // Fix: Trigger initial load so the tab isn't empty on first open
        updateFilters()
    }

    private fun updateFilters() {
        if (_binding == null) return
        val radius = if (binding.switchGlobal.isChecked) null else binding.radiusSlider.value.toDouble()
        binding.tvRadiusLabel.text = if (radius == null) "Radius: Global" else "Radius: ${radius.toInt()} miles"
        feedViewModel.applyFilters(showChat = false, onlyFriends = false, radius = radius)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
