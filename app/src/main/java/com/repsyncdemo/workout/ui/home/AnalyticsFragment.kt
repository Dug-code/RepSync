package com.repsyncdemo.workout.ui.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.databinding.FragmentAnalyticsBinding
import com.repsyncdemo.workout.viewmodel.AnalyticsViewModel
import java.text.NumberFormat
import java.util.Locale

class AnalyticsFragment : Fragment(R.layout.fragment_analytics) {

    private lateinit var binding: FragmentAnalyticsBinding
    private val viewModel: AnalyticsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAnalyticsBinding.bind(view)

        // Observe Total Volume
        viewModel.totalVolume.observe(viewLifecycleOwner) { volume ->
            // Format number with commas (e.g., 10,500)
            val formatted = NumberFormat.getNumberInstance(Locale.US).format(volume ?: 0.0)
            binding.tvTotalVolume.text = "$formatted lbs"
        }

        // Setup Clear History Button
        binding.btnClearHistory.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun showDeleteConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Clear All Data?")
            .setMessage("This will permanently delete all your workout logs and reset your total volume. This cannot be undone.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Clear Everything") { _, _ ->
                viewModel.clearAllHistory()
            }
            .show()
    }
}