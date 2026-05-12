package com.repsyncdemo.workout.ui.home

/**
 * File overview: Hosts the analytics tabs and routes users between summary and advanced analytics views.
 */

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.TextView
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.databinding.FragmentAnalyticsBinding
import com.takusemba.spotlight.OnSpotlightListener
import com.takusemba.spotlight.Spotlight
import com.takusemba.spotlight.Target
import com.takusemba.spotlight.shape.RoundedRectangle

class AnalyticsFragment : Fragment(R.layout.fragment_analytics) {

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!
    private var spotlight: Spotlight? = null

    companion object {
        private const val PREFS_NAME = "repsync_prefs"
        private const val KEY_ANALYTICS_TUTORIAL_COMPLETED = "analytics_tutorial_completed"
    }

    // Connects views, clicks, and data.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAnalyticsBinding.bind(view)

        setupViewPager()
        checkTutorial()
    }

    // Sets up this section.
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

    private fun checkTutorial() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isCompleted = prefs.getBoolean(KEY_ANALYTICS_TUTORIAL_COMPLETED, false)
        if (!isCompleted) {
            binding.root.post { showTutorial() }
        }
    }

    private fun showTutorial() {
        val targets = ArrayList<Target>()

        binding.tabLayout.getTabAt(0)?.view?.let {
            targets.add(createTarget(it, "Summary", "Start with a quick view of training volume, consistency, and favorites."))
        }

        binding.tabLayout.getTabAt(1)?.view?.let {
            targets.add(createTarget(it, "Advanced", "Open deeper analytics when you want more detail."))
        }

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

    fun nextSpotlight() {
        spotlight?.next()
    }

    fun finishSpotlight() {
        spotlight?.finish()
    }

    private fun markTutorialCompleted() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_ANALYTICS_TUTORIAL_COMPLETED, true) }
    }

    // Clears the view binding.
    override fun onDestroyView() {
        super.onDestroyView()
        spotlight?.finish()
        spotlight = null
        _binding = null
    }
}
