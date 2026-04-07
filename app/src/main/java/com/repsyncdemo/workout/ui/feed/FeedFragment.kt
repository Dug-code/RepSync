package com.repsyncdemo.workout.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.repsyncdemo.workout.databinding.FragmentFeedBinding

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

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
        
        // Pre-load all tabs to prevent glitches during swipe
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
