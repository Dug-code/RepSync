package com.repsyncdemo.workout.ui.friends

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
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.databinding.FragmentFriendsBinding
import com.repsyncdemo.workout.ui.adapter.FriendAdapter
import com.repsyncdemo.workout.ui.adapter.FriendRequestAdapter
import com.repsyncdemo.workout.ui.adapter.UserSearchAdapter
import com.repsyncdemo.workout.viewmodel.ProfileViewModel
import com.repsyncdemo.workout.viewmodel.SocialViewModel

class FriendsFragment : Fragment() {

    private var _binding: FragmentFriendsBinding? = null
    private val binding get() = _binding!!
    private val socialViewModel: SocialViewModel by activityViewModels()
    private val profileViewModel: ProfileViewModel by activityViewModels()

    private lateinit var friendAdapter: FriendAdapter
    private lateinit var requestAdapter: FriendRequestAdapter
    private lateinit var searchAdapter: UserSearchAdapter
    
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFriendsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        profileViewModel.loadProfile()

        // Pass isMyProfile = true since this fragment is for the current user's social management
        friendAdapter = FriendAdapter(isMyProfile = true) { friendship ->
            socialViewModel.removeFriendship(friendship.id)
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

        searchAdapter = UserSearchAdapter(
            currentUserId = currentUserId,
            onUserClick = { user ->
                val bundle = Bundle().apply { putString("userId", user.userId) }
                findNavController().navigate(R.id.profileFragment, bundle)
            },
            onAddFriend = { user ->
                val myUsername = profileViewModel.myProfile.value?.username ?: ""
                socialViewModel.sendFriendRequest(user.userId, user.username, myUsername)
                Toast.makeText(requireContext(), "Friend request sent!", Toast.LENGTH_SHORT).show()
            },
            onCancelRequest = { friendshipId ->
                socialViewModel.removeFriendship(friendshipId)
                Toast.makeText(requireContext(), "Request cancelled", Toast.LENGTH_SHORT).show()
            }
        )

        binding.rvFriends.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = friendAdapter
        }
        binding.rvRequests.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = requestAdapter
        }
        binding.rvSearch.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchAdapter
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showTab(friends = true)
                    1 -> showTab(requests = true)
                    2 -> showTab(search = true)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.length >= 2) {
                    profileViewModel.searchUsers(query)
                }
            }
        })

        socialViewModel.friends.observe(viewLifecycleOwner) { friends ->
            friendAdapter.submitList(friends)
            if (binding.tabLayout.selectedTabPosition == 0) {
                binding.tvEmpty.visibility = if (friends.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        socialViewModel.myFriendships.observe(viewLifecycleOwner) { friendships ->
            searchAdapter.updateFriendships(friendships)
        }

        socialViewModel.pendingRequests.observe(viewLifecycleOwner) { requests ->
            requestAdapter.submitList(requests)
        }

        profileViewModel.searchResults.observe(viewLifecycleOwner) { users ->
            searchAdapter.submitList(users)
        }
    }

    private fun showTab(friends: Boolean = false, requests: Boolean = false, search: Boolean = false) {
        binding.rvFriends.visibility = if (friends) View.VISIBLE else View.GONE
        binding.rvRequests.visibility = if (requests) View.VISIBLE else View.GONE
        binding.rvSearch.visibility = if (search) View.VISIBLE else View.GONE
        binding.etSearch.visibility = if (search) View.VISIBLE else View.GONE
        binding.tvEmpty.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
