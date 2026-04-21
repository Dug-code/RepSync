package com.repsyncdemo.workout.ui.feed

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.gms.location.LocationServices
import com.google.android.material.tabs.TabLayoutMediator
import com.repsyncdemo.workout.databinding.FragmentFeedBinding
import com.repsyncdemo.workout.viewmodel.FeedViewModel
import com.repsyncdemo.workout.viewmodel.ProfileViewModel

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!
    private val feedViewModel: FeedViewModel by activityViewModels()
    private val profileViewModel: ProfileViewModel by activityViewModels()

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            fetchLocation()
        } else {
            feedViewModel.setLocationDisabled()
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
        setupViewPager()
        checkLocationPermission()
        
        // Initial load of the background tabs
        feedViewModel.loadFriendsFeed()
        feedViewModel.loadChatFeed()
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
                    feedViewModel.setLocationDisabled()
                }
            }.addOnFailureListener {
                feedViewModel.setLocationDisabled()
            }
        } catch (e: SecurityException) {
            feedViewModel.setLocationDisabled()
        }
    }

    private fun setupViewPager() {
        val adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 3
            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> FeedExploreFragment()
                    1 -> FeedFriendsFragment()
                    else -> FeedChatFragment()
                }
            }
        }
        binding.viewPager.adapter = adapter
        binding.viewPager.offscreenPageLimit = 2

        TabLayoutMediator(binding.feedTabs, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Explore"
                1 -> "Friends"
                else -> "Chat"
            }
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
