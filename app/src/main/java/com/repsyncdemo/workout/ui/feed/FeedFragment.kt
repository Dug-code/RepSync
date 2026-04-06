package com.repsyncdemo.workout.ui.feed

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.LocationServices
import com.google.android.material.tabs.TabLayout
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.databinding.FragmentFeedBinding
import com.repsyncdemo.workout.ui.adapter.FeedAdapter
import com.repsyncdemo.workout.viewmodel.FeedViewModel
import com.repsyncdemo.workout.viewmodel.ProfileViewModel

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!
    private val feedViewModel: FeedViewModel by activityViewModels()
    private val profileViewModel: ProfileViewModel by activityViewModels()

    private lateinit var feedAdapter: FeedAdapter

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            fetchLocation()
        } else {
            feedViewModel.loadFeed()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        feedAdapter = FeedAdapter(
            onUserClick = { userId ->
                val bundle = Bundle().apply { putString("userId", userId) }
                findNavController().navigate(R.id.action_feed_to_profile, bundle)
            },
            onLikeClick = { postId ->
                feedViewModel.toggleLike(postId)
            },
            onDeleteClick = { postId ->
                feedViewModel.deletePost(postId)
            },
            onEditChatClick = { post ->
                showEditChatDialog(post.id, post.description)
            }
        )

        binding.rvFeed.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = feedAdapter
        }

        setupTabs()
        setupFilters()
        checkLocationPermission()

        feedViewModel.feedPosts.observe(viewLifecycleOwner) { posts ->
            feedAdapter.submitList(posts)
            binding.layoutEmpty.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE
            binding.rvFeed.visibility = if (posts.isEmpty()) View.GONE else View.VISIBLE
            binding.progressBar.visibility = View.GONE
        }

        feedViewModel.userProfiles.observe(viewLifecycleOwner) { profiles ->
            feedAdapter.updateProfiles(profiles)
        }

        feedViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        binding.btnEmptyAction.setOnClickListener {
            // "Discover Friends" button in empty state
            binding.feedTabs.getTabAt(1)?.select()
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

        AlertDialog.Builder(requireContext())
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

    private fun setupTabs() {
        binding.feedTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> { // Explore
                        binding.layoutRadiusFilter.visibility = View.VISIBLE
                        binding.cbShowMyPosts.visibility = View.GONE
                        binding.layoutChatInput.visibility = View.GONE
                        updateExploreFilters()
                    }
                    1 -> { // Friends
                        binding.layoutRadiusFilter.visibility = View.GONE
                        binding.cbShowMyPosts.visibility = View.VISIBLE
                        binding.layoutChatInput.visibility = View.GONE
                        feedViewModel.applyFilters(
                            showChat = false, 
                            onlyFriends = true, 
                            radius = null,
                            showMyPosts = binding.cbShowMyPosts.isChecked
                        )
                    }
                    2 -> { // Chat
                        binding.layoutRadiusFilter.visibility = View.GONE
                        binding.cbShowMyPosts.visibility = View.GONE
                        binding.layoutChatInput.visibility = View.VISIBLE
                        feedViewModel.applyFilters(showChat = true, onlyFriends = false, radius = null)
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupFilters() {
        binding.switchGlobal.setOnCheckedChangeListener { _, isChecked ->
            binding.radiusSlider.visibility = if (isChecked) View.GONE else View.VISIBLE
            updateExploreFilters()
        }

        binding.radiusSlider.addOnChangeListener { _, value, _ ->
            updateExploreFilters()
        }

        binding.cbShowMyPosts.setOnCheckedChangeListener { _, isChecked ->
            if (binding.feedTabs.selectedTabPosition == 1) {
                feedViewModel.applyFilters(showMyPosts = isChecked)
            }
        }
    }

    private fun updateExploreFilters() {
        if (binding.feedTabs.selectedTabPosition != 0) return
        
        val isGlobal = binding.switchGlobal.isChecked
        val radius = if (isGlobal) null else binding.radiusSlider.value.toDouble()
        
        binding.tvRadiusLabel.text = if (isGlobal) "Radius: Global" else "Radius: ${radius?.toInt()} miles"
        
        feedViewModel.applyFilters(
            showChat = false, 
            onlyFriends = false, 
            radius = radius,
            showMyPosts = true
        )
    }

    private fun checkLocationPermission() {
        val fineLocation = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseLocation = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (fineLocation == PackageManager.PERMISSION_GRANTED ||
            coarseLocation == PackageManager.PERMISSION_GRANTED
        ) {
            fetchLocation()
        } else {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun fetchLocation() {
        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    feedViewModel.setUserLocation(location.latitude, location.longitude)
                    profileViewModel.updateLocation(location.latitude, location.longitude)
                } else {
                    feedViewModel.loadFeed()
                }
            }.addOnFailureListener {
                feedViewModel.loadFeed()
            }
        } catch (e: SecurityException) {
            feedViewModel.loadFeed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
