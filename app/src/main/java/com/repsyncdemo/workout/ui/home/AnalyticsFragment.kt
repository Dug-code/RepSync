package com.repsyncdemo.workout.ui.home

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.LinearLayout
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAnalyticsBinding.bind(view)

        setupViewPager()
        checkTutorial()

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

    /** ---------------- Tutorial Code Begins ----------------- **/

    private fun checkTutorial() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        //set to false for testing
        val isCompleted = prefs.getBoolean(KEY_ANALYTICS_TUTORIAL_COMPLETED, false)
        if (!isCompleted) {
            binding.root.post {
                showTutorial()
            }
        }
    }

    private fun showTutorial() {
        val targets = ArrayList<com.takusemba.spotlight.Target>()

        // Spotlight Summary Tab
        val exploreView = binding.tabLayout.getTabAt(0)?.view
        exploreView?.let {
            targets.add(summaryCreateTarget(it, "Summary Analytics", "Check out a quick summary of your progress."))
        }

        //Summary Analytics
        targets.add(
            Target.Builder()
                .setAnchor(binding.viewPager)
                .setShape(RoundedRectangle(binding.viewPager.height.toFloat(), binding.viewPager.width.toFloat(), 16f))
                .setOverlay(createOverlay())
                .build()
        )

        // Advanced Analytics Tab
        val friendsTab = binding.tabLayout.getTabAt(1)?.view
        friendsTab?.let {
            targets.add(createTarget(it, "Advanced Analytics", "See a more detailed breakdown of your progress."))
        }

        spotlight = Spotlight.Builder(requireActivity())
            .setTargets(targets)
            .setBackgroundColorRes(R.color.spotlight_background)
            .setDuration(400L)
            .setAnimation(DecelerateInterpolator(2f))
            .setOnSpotlightListener(object : OnSpotlightListener {
                override fun onStarted() {}
                override fun onEnded() {
                    markTutorialCompleted()
                }
            })
            .build()

        spotlight?.start()
    }

    private fun createTarget(view: View, title: String, description: String): com.takusemba.spotlight.Target {
        return Target.Builder()
            .setAnchor(view)
            .setShape(RoundedRectangle(view.height.toFloat(), view.width.toFloat(), 8f))
            .setOverlay(createOverlay(title, description))
            .build()
    }

    private fun summaryCreateTarget(view: View, title: String, description: String): com.takusemba.spotlight.Target {
        return Target.Builder()
            .setAnchor(view)
            .setShape(RoundedRectangle(view.height.toFloat(), view.width.toFloat(), 8f))
            .setOverlay(summaryCreateOverlay(title, description))
            .build()
    }
    private fun createOverlay(title: String, description: String): View {
        val overlay = layoutInflater.inflate(R.layout.layout_spotlight_overlay, binding.root, false)
        overlay.findViewById<TextView>(R.id.tvTitle).text = title
        overlay.findViewById<TextView>(R.id.tvDescription).text = description

        overlay.findViewById<Button>(R.id.btnNext).setOnClickListener {
            spotlight?.next()
        }

        overlay.findViewById<Button>(R.id.btnSkip).setOnClickListener {
            spotlight?.finish()
            markTutorialCompleted()
        }

        return overlay
    }

    private fun summaryCreateOverlay(title: String, description: String): View {
        val overlay = layoutInflater.inflate(R.layout.layout_spotlight_overlay, binding.root, false)
        overlay.findViewById<TextView>(R.id.tvTitle).text = title
        overlay.findViewById<TextView>(R.id.tvDescription).text = description

        overlay.findViewById<Button>(R.id.btnNext).setOnClickListener {
            spotlight?.next()
            // Trigger the child summary fragment's localized tutorial
            childFragmentManager.fragments
                .filterIsInstance<AnalyticsSummaryFragment>()
                .firstOrNull()?.checkTutorial()
        }

        overlay.findViewById<Button>(R.id.btnSkip).setOnClickListener {
            spotlight?.finish()
            markTutorialCompleted()
        }

        return overlay
    }

    private fun createOverlay(): View {
        val overlay = layoutInflater.inflate(R.layout.layout_spotlight_overlay, binding.root, false)
        overlay.findViewById<LinearLayout>(R.id.containerInfo).visibility = View.GONE

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

    /** ---------------- Tutorial Code Ends ----------------- **/


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
