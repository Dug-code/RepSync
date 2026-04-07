package com.repsyncdemo.workout.ui.profile

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
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.databinding.FragmentProfileFriendsTabBinding
import com.repsyncdemo.workout.ui.adapter.*
import com.repsyncdemo.workout.viewmodel.*
import com.google.firebase.auth.FirebaseAuth

class ProfileFriendsFragment : Fragment() {
    private var _binding: FragmentProfileFriendsTabBinding? = null
    private val binding get() = _binding!!
    
    private val profileViewModel: ProfileViewModel by activityViewModels()
    private val socialViewModel: SocialViewModel by activityViewModels()
    
    private lateinit var friendsAdapter: FriendAdapter
    private lateinit var requestAdapter: FriendRequestAdapter
    private lateinit var searchAdapter: UserSearchAdapter
    
    private var targetUserId: String? = null
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileFriendsTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        targetUserId = arguments?.getString("userId")
        setupAdapters()
        setupRecyclerViews()
        setupSearch()
        observeData()
    }

    private fun setupRecyclerViews() {
        binding.rvFriends.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = friendsAdapter
        }
        binding.rvFriendRequests.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = requestAdapter
        }
    }

    private fun setupAdapters() {
        friendsAdapter = FriendAdapter(isMyProfile = targetUserId == null) { friendship ->
            socialViewModel.removeFriendship(friendship.id)
            Toast.makeText(requireContext(), "Friend removed", Toast.LENGTH_SHORT).show()
        }

        requestAdapter = FriendRequestAdapter(
            onAccept = { request -> socialViewModel.acceptRequest(request.id) },
            onDecline = { request -> socialViewModel.declineRequest(request.id) }
        )

        searchAdapter = UserSearchAdapter(
            currentUserId = currentUserId,
            onUserClick = { user ->
                if (user.userId != currentUserId && user.userId != targetUserId) {
                    val bundle = Bundle().apply { putString("userId", user.userId) }
                    findNavController().navigate(R.id.profileFragment, bundle)
                }
            },
            onAddFriend = { user ->
                val myUsername = profileViewModel.myProfile.value?.username ?: "User"
                socialViewModel.sendFriendRequest(user.userId, user.username, myUsername)
            },
            onCancelRequest = { id -> socialViewModel.removeFriendship(id) }
        )
    }

    private fun setupSearch() {
        if (targetUserId != null) {
            binding.layoutSearch.visibility = View.GONE
            return
        }

        binding.etFriendSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.isEmpty()) {
                    binding.rvFriends.adapter = friendsAdapter
                } else {
                    binding.rvFriends.adapter = searchAdapter
                    profileViewModel.searchUsers(query)
                }
            }
        })
    }

    private fun observeData() {
        if (targetUserId == null) {
            socialViewModel.friends.observe(viewLifecycleOwner) { 
                friendsAdapter.submitList(it)
                updateEmptyState(it.isEmpty())
            }
            socialViewModel.pendingRequests.observe(viewLifecycleOwner) {
                requestAdapter.submitList(it)
                binding.tvRequestsLabel.visibility = if (it.isNotEmpty()) View.VISIBLE else View.GONE
                binding.rvFriendRequests.visibility = if (it.isNotEmpty()) View.VISIBLE else View.GONE
            }
        } else {
            socialViewModel.targetUserFriends.observe(viewLifecycleOwner) {
                friendsAdapter.submitList(it)
                updateEmptyState(it.isEmpty())
            }
        }

        profileViewModel.searchResults.observe(viewLifecycleOwner) {
            if (!binding.etFriendSearch.text.isNullOrEmpty()) {
                searchAdapter.submitList(it)
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
