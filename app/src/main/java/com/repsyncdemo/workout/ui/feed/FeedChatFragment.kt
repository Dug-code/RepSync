package com.repsyncdemo.workout.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.databinding.FragmentFeedChatBinding
import com.repsyncdemo.workout.ui.adapter.FeedAdapter
import com.repsyncdemo.workout.ui.dialogs.ReactionDialogFragment
import com.repsyncdemo.workout.viewmodel.FeedViewModel
import com.repsyncdemo.workout.viewmodel.ProfileViewModel

class FeedChatFragment : Fragment() {
    private var _binding: FragmentFeedChatBinding? = null
    private val binding get() = _binding!!
    private val feedViewModel: FeedViewModel by activityViewModels()
    private val profileViewModel: ProfileViewModel by activityViewModels()
    private lateinit var feedAdapter: FeedAdapter
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFeedChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        feedAdapter = FeedAdapter(
            onUserClick = { userId ->
                if (userId == currentUserId) {
                    // Navigate to root profile tab
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
            onDeleteClick = { postId -> feedViewModel.deletePost(postId) },
            onEditChatClick = { post ->
                showEditChatDialog(post.id, post.description)
            }
        )

        binding.rvFeed.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = feedAdapter
        }

        // Use chatPosts specifically to avoid MediatorLiveData interference during swipes
        feedViewModel.chatPosts.observe(viewLifecycleOwner) { posts ->
            feedAdapter.submitList(posts)
            binding.layoutEmpty.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE
        }

        feedViewModel.userProfiles.observe(viewLifecycleOwner) { profiles ->
            feedAdapter.updateProfiles(profiles)
        }

        profileViewModel.myProfile.observe(viewLifecycleOwner) { profile ->
            feedAdapter.setCurrentUserProfile(profile)
        }

        binding.btnSendChat.setOnClickListener {
            val message = binding.etChatMessage.text.toString()
            if (message.isNotBlank()) {
                feedViewModel.sendChatMessage(message)
                binding.etChatMessage.setText("")
            }
        }
    }

    private fun showEditChatDialog(postId: String, currentText: String) {
        val input = EditText(requireContext())
        input.setText(currentText)
        input.setSelection(currentText.length)

        AlertDialog.Builder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setTitle("Edit Chat Message")
            .setView(input)
            .setPositiveButton("Update") { _, _ ->
                val newText = input.text.toString().trim()
                if (newText.isNotEmpty()) {
                    feedViewModel.updateChatMessage(postId, newText)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        feedViewModel.loadChatFeed()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
