package com.repsyncdemo.workout.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.databinding.LayoutTabListBinding
import com.repsyncdemo.workout.ui.adapter.FeedAdapter
import com.repsyncdemo.workout.ui.dialogs.ReactionDialogFragment
import com.repsyncdemo.workout.viewmodel.FeedViewModel
import com.repsyncdemo.workout.viewmodel.ProfileViewModel

class ProfilePostsFragment : Fragment() {
    private var _binding: LayoutTabListBinding? = null
    private val binding get() = _binding!!
    
    private val profileViewModel: ProfileViewModel by activityViewModels()
    private val feedViewModel: FeedViewModel by activityViewModels()
    
    private lateinit var feedAdapter: FeedAdapter
    private var targetUserId: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = LayoutTabListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        targetUserId = arguments?.getString("userId")

        feedAdapter = FeedAdapter(
            onUserClick = { userId ->
                if (userId != targetUserId) {
                    val bundle = Bundle().apply { putString("userId", userId) }
                    findNavController().navigate(R.id.profileFragment, bundle)
                }
            },
            onReactionClick = { view, postId -> // Open the ReactionDialogFragment
                val reactionDialog = ReactionDialogFragment.newInstance(postId)
                reactionDialog.show(childFragmentManager, "ReactionDialog")
            },
            onDeleteClick = { postId -> feedViewModel.deletePost(postId) }
        )

        binding.rvContent.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = feedAdapter
        }

        val postsSource = if (targetUserId != null) profileViewModel.userPosts else profileViewModel.myPosts
        postsSource.observe(viewLifecycleOwner) { posts ->
            feedAdapter.submitList(posts)
            binding.layoutEmpty.visibility = if (posts.isNullOrEmpty()) View.VISIBLE else View.GONE
            binding.tvEmptyTitle.text = "No posts yet."
            binding.tvEmptySubtitle.text = ""
            binding.btnEmptyAction.visibility = View.GONE
        }

        feedViewModel.userProfiles.observe(viewLifecycleOwner) { profiles ->
            feedAdapter.updateProfiles(profiles)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
