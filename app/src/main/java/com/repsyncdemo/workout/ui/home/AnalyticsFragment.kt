package com.repsyncdemo.workout.ui.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.databinding.FragmentAnalyticsBinding

class AnalyticsFragment : Fragment(R.layout.fragment_analytics) {

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAnalyticsBinding.bind(view)

        setupViewPager()
    }

    private fun setupViewPager() {
        val adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 2
            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> AnalyticsSummaryFragment()
                    else -> AnalyticsAdvancedFragment()
                }
            }
        }
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Summary"
                else -> "Advanced"
            }
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
