package com.repsyncdemo.workout.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.databinding.FragmentMainBinding
import com.repsyncdemo.workout.ui.feed.FeedFragment
import com.repsyncdemo.workout.ui.home.AnalyticsFragment
import com.repsyncdemo.workout.ui.home.HomeFragment
import com.repsyncdemo.workout.ui.profile.ProfileFragment
import com.repsyncdemo.workout.ui.workout.WorkoutListFragment
import com.repsyncdemo.workout.viewmodel.NavigationLockViewModel

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private val navigationLockViewModel: NavigationLockViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewPager()
        
        navigationLockViewModel.isLocked.observe(viewLifecycleOwner) { isLocked ->
            binding.mainViewPager.isUserInputEnabled = !isLocked
            binding.bottomNav.alpha = if (isLocked) 0.5f else 1.0f
        }
    }

    private fun setupViewPager() {
        val fragments = listOf(
            HomeFragment(),
            FeedFragment(),
            AnalyticsFragment(),
            WorkoutListFragment(),
            ProfileFragment()
        )

        val adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = fragments.size
            override fun createFragment(position: Int): Fragment = fragments[position]
        }

        binding.mainViewPager.adapter = adapter
        binding.mainViewPager.offscreenPageLimit = 4

        binding.mainViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val menuId = when (position) {
                    0 -> R.id.homeFragment
                    1 -> R.id.feedFragment
                    2 -> R.id.analyticsFragment
                    3 -> R.id.workoutListFragment
                    4 -> R.id.profileFragment
                    else -> R.id.homeFragment
                }
                binding.bottomNav.selectedItemId = menuId
            }
        })

        binding.bottomNav.setOnItemSelectedListener { item ->
            val page = when (item.itemId) {
                R.id.homeFragment -> 0
                R.id.feedFragment -> 1
                R.id.analyticsFragment -> 2
                R.id.workoutListFragment -> 3
                R.id.profileFragment -> 4
                else -> 0
            }
            binding.mainViewPager.setCurrentItem(page, true)
            true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
