package com.repsyncdemo.workout.ui.feed

/**
 * File overview: Hosts feed tabs for friends, chat, and explore content.
 */

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.tabs.TabLayoutMediator
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.databinding.FragmentFeedBinding
import com.repsyncdemo.workout.viewmodel.FeedViewModel
import com.repsyncdemo.workout.viewmodel.NotificationViewModel
import com.repsyncdemo.workout.viewmodel.ProfileViewModel
import com.takusemba.spotlight.OnSpotlightListener
import com.takusemba.spotlight.Spotlight
import com.takusemba.spotlight.Target
import com.takusemba.spotlight.shape.Circle
import com.takusemba.spotlight.shape.RoundedRectangle

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!
    private val feedViewModel: FeedViewModel by activityViewModels()
    private val profileViewModel: ProfileViewModel by activityViewModels()
    private val notificationViewModel: NotificationViewModel by activityViewModels()
    private var spotlight: Spotlight? = null

    companion object {
        private const val PREFS_NAME = "repsync_prefs"
        private const val KEY_FEED_TUTORIAL_COMPLETED = "feed_tutorial_completed"
    }

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

    // Sets up this screen.
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Connects views, clicks, and data.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewPager()
        checkLocationPermission()
        setupNotificationButton()
        checkTutorial()
        
        // Initial load of the background tabs
        feedViewModel.loadFriendsFeed()
        feedViewModel.loadChatFeed()
    }

    private fun checkTutorial() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isCompleted = prefs.getBoolean(KEY_FEED_TUTORIAL_COMPLETED, false)
        if (!isCompleted) {
            binding.root.post { showTutorial() }
        }
    }

    private fun showTutorial() {
        val targets = ArrayList<Target>()

        binding.feedTabs.getTabAt(0)?.view?.let {
            targets.add(createTarget(it, "Explore", "Find posts from nearby lifters or switch to the global feed."))
        }

        binding.feedTabs.getTabAt(1)?.view?.let {
            targets.add(createTarget(it, "Friends", "See training updates from people you follow."))
        }

        binding.feedTabs.getTabAt(2)?.view?.let {
            targets.add(createTarget(it, "Chat", "Jump into community conversations."))
        }

        targets.add(
            Target.Builder()
                .setAnchor(binding.btnNotifications)
                .setShape(Circle(binding.btnNotifications.height.toFloat() / 2 + 20f))
                .setOverlay(createOverlay("Notifications", "Review friend requests, reactions, and other updates."))
                .build()
        )

        spotlight = Spotlight.Builder(requireActivity())
            .setTargets(targets)
            .setBackgroundColorRes(R.color.spotlight_background)
            .setDuration(400L)
            .setAnimation(DecelerateInterpolator(2f))
            .setOnSpotlightListener(object : OnSpotlightListener {
                override fun onStarted() = Unit
                override fun onEnded() {
                    markTutorialCompleted()
                }
            })
            .build()

        spotlight?.start()
    }

    private fun createTarget(view: View, title: String, description: String): Target {
        return Target.Builder()
            .setAnchor(view)
            .setShape(RoundedRectangle(view.height.toFloat(), view.width.toFloat(), 8f))
            .setOverlay(createOverlay(title, description))
            .build()
    }

    private fun createOverlay(title: String, description: String): View {
        val overlay = layoutInflater.inflate(R.layout.layout_spotlight_overlay, binding.root, false)
        overlay.findViewById<TextView>(R.id.tvTitle).text = title
        overlay.findViewById<TextView>(R.id.tvDescription).text = description
        overlay.findViewById<Button>(R.id.btnNext).setOnClickListener { spotlight?.next() }
        overlay.findViewById<Button>(R.id.btnSkip).setOnClickListener {
            spotlight?.finish()
            markTutorialCompleted()
        }
        return overlay
    }

    private fun markTutorialCompleted() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_FEED_TUTORIAL_COMPLETED, true) }
    }

    // Sets up this section.
    private fun setupNotificationButton() {
        binding.btnNotifications.setOnClickListener {
            findNavController().navigate(R.id.action_feed_to_notifications)
            spotlight?.finish()
        }

        notificationViewModel.unreadNotifications.observe(viewLifecycleOwner) { notifications ->
            if (notifications.isNotEmpty()) {
                binding.tvNotificationBadge.text = notifications.size.toString()
                binding.tvNotificationBadge.visibility = View.VISIBLE
            } else {
                binding.tvNotificationBadge.visibility = View.GONE
            }
        }
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

    @SuppressLint("MissingPermission")
    private fun fetchLocation() {
        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
            val cancellationTokenSource = CancellationTokenSource()

            fusedLocationClient
                .getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                )
                .addOnSuccessListener { location ->
                    if (isUsableLocation(location)) {
                        applyLocation(location.latitude, location.longitude)
                    } else {
                        fetchRecentCachedLocation(fusedLocationClient)
                    }
                }
                .addOnFailureListener {
                    fetchRecentCachedLocation(fusedLocationClient)
                }
        } catch (e: SecurityException) {
            feedViewModel.setLocationDisabled()
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchRecentCachedLocation(fusedLocationClient: FusedLocationProviderClient) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (isUsableLocation(location)) {
                location?.let {
                    applyLocation(it.latitude, it.longitude)
                }
            } else {
                feedViewModel.setLocationDisabled()
            }
        }.addOnFailureListener {
            feedViewModel.setLocationDisabled()
        }
    }

    private fun isUsableLocation(location: Location?): Boolean {
        if (location == null) return false

        val maxLocationAgeMillis = 10 * 60 * 1000L
        val ageMillis = System.currentTimeMillis() - location.time
        val isRecent = ageMillis in 0..maxLocationAgeMillis
        val isAccurateEnough = !location.hasAccuracy() || location.accuracy <= 5_000f

        return isRecent && isAccurateEnough
    }

    // Filters or sorts the list.
    private fun applyLocation(latitude: Double, longitude: Double) {
        feedViewModel.setUserLocation(latitude, longitude)
        profileViewModel.updateLocation(latitude, longitude)
    }

    // Sets up this section.
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

        // Dismiss keyboard when switching sub-tabs
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                hideKeyboard()
            }
        })

        TabLayoutMediator(binding.feedTabs, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Explore"
                1 -> "Friends"
                else -> "Chat"
            }
        }.attach()
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = activity?.currentFocus ?: view
        view?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    // Clears the view binding.
    override fun onDestroyView() {
        super.onDestroyView()
        spotlight?.finish()
        spotlight = null
        _binding = null
    }
}
