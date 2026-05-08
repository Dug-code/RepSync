package com.repsyncdemo.workout.ui.profile

/**
 * File overview: Displays and manages a profile-related screen for user identity, social, goals, trophies, or notifications.
 */

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.repsyncdemo.workout.databinding.FragmentNotificationsBinding
import com.repsyncdemo.workout.ui.adapter.NotificationAdapter
import com.repsyncdemo.workout.viewmodel.NotificationViewModel

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private val notificationViewModel: NotificationViewModel by activityViewModels()

    // Sets up this screen.
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Connects views, clicks, and data.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = NotificationAdapter()
        binding.rvNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNotifications.adapter = adapter

        // Observe ALL notifications so they stay visible even after being read
        notificationViewModel.allNotifications.observe(viewLifecycleOwner) { notifications ->
            adapter.submitList(notifications)
            binding.tvEmpty.visibility = if (notifications.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    // Clears the view binding.
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
