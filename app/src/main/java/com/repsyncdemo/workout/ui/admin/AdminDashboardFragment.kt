package com.repsyncdemo.workout.ui.admin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.repsyncdemo.workout.databinding.FragmentAdminDashboardBinding
import com.repsyncdemo.workout.ui.adapter.UserAdapter
import com.repsyncdemo.workout.viewmodel.ProfileViewModel

/**
 * Fragment for administrative tasks. Allows admins to search for users
 * and manage their account roles (Admin/Moderator status).
 */
class AdminDashboardFragment : Fragment() {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!
    private val profileViewModel: ProfileViewModel by activityViewModels()
    private lateinit var userAdapter: UserAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize adapter with toggle logic for roles
        userAdapter = UserAdapter(
            onToggleAdmin = { user ->
                // Flip admin status and persist to Firestore
                val updated = user.copy(isAdmin = !user.isAdmin)
                profileViewModel.updateProfile(updated)
            },
            onToggleModerator = { user ->
                // Flip moderator status and persist to Firestore
                val updated = user.copy(isModerator = !user.isModerator)
                profileViewModel.updateProfile(updated)
            }
        )

        binding.rvUsers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = userAdapter
        }

        // Display results whenever the search query returns data
        profileViewModel.searchResults.observe(viewLifecycleOwner) { users ->
            userAdapter.submitList(users)
        }

        // Real-time search implementation
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                // Only trigger search if at least 2 characters are typed to reduce API calls
                if (query.length >= 2) {
                    profileViewModel.searchUsers(query)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
