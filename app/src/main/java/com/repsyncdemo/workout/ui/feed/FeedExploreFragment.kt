package com.repsyncdemo.workout.ui.feed

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.databinding.FragmentFeedExploreBinding
import com.repsyncdemo.workout.ui.adapter.FeedAdapter
import com.repsyncdemo.workout.ui.dialogs.ReactionDialogFragment
import com.repsyncdemo.workout.viewmodel.FeedViewModel
import com.repsyncdemo.workout.viewmodel.ProfileViewModel

class FeedExploreFragment : Fragment() {
    private var _binding: FragmentFeedExploreBinding? = null
    private val binding get() = _binding!!
    private val feedViewModel: FeedViewModel by activityViewModels()
    private val profileViewModel: ProfileViewModel by activityViewModels()
    private lateinit var feedAdapter: FeedAdapter
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFeedExploreBinding.inflate(inflater, container, false)
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

        // Use explorePosts specifically
        feedViewModel.explorePosts.observe(viewLifecycleOwner) { posts ->
            feedAdapter.submitList(posts)
            updateEmptyState(posts.isNullOrEmpty())
        }

        feedViewModel.userProfiles.observe(viewLifecycleOwner) { profiles ->
            feedAdapter.updateProfiles(profiles)
        }
        
        profileViewModel.myProfile.observe(viewLifecycleOwner) { profile ->
            feedAdapter.setCurrentUserProfile(profile)
        }

        feedViewModel.isLocationAvailable.observe(viewLifecycleOwner) { available ->
            if (available) {
                binding.layoutLocationRequired.visibility = View.GONE
                binding.rvFeed.visibility = if (feedAdapter.itemCount > 0) View.VISIBLE else View.GONE
                binding.layoutRadiusFilter.alpha = 1.0f
                binding.radiusSlider.isEnabled = true
                binding.switchGlobal.isEnabled = true
            } else {
                if (!binding.switchGlobal.isChecked) {
                    binding.layoutLocationRequired.visibility = View.VISIBLE
                    binding.rvFeed.visibility = View.GONE
                    binding.layoutEmpty.visibility = View.GONE
                }
            }
        }

        binding.switchGlobal.setOnCheckedChangeListener { _, isChecked ->
            binding.radiusSlider.visibility = if (isChecked) View.GONE else View.VISIBLE
            
            if (!isChecked && feedViewModel.isLocationAvailable.value == false) {
                binding.layoutLocationRequired.visibility = View.VISIBLE
                binding.rvFeed.visibility = View.GONE
            } else {
                binding.layoutLocationRequired.visibility = View.GONE
                updateFilters()
            }
        }

        binding.radiusSlider.addOnChangeListener { _, _, _ -> updateFilters() }

        binding.btnEnableLocation.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", requireContext().packageName, null)
            intent.data = uri
            startActivity(intent)
        }

        updateFilters()
    }

    private fun updateFilters() {
        if (_binding == null) return
        val isGlobal = binding.switchGlobal.isChecked
        val radius = if (isGlobal) null else binding.radiusSlider.value.toDouble()
        
        binding.tvRadiusLabel.text = if (isGlobal) "Radius: Global" else "Radius: ${radius?.toInt()} miles"
        
        feedViewModel.applyExploreFilters(radius = radius)
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (feedViewModel.isLocationAvailable.value == false && !binding.switchGlobal.isChecked) {
            binding.layoutEmpty.visibility = View.GONE
            return
        }
        binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvFeed.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
