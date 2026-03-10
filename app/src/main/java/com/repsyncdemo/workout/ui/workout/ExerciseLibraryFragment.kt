package com.repsyncdemo.workout.ui.workout

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.repsyncdemo.workout.data.ExerciseDatabase
import com.repsyncdemo.workout.databinding.FragmentExerciseLibraryBinding
import com.repsyncdemo.workout.ui.adapter.ExerciseLibraryAdapter

/**
 * Fragment that displays a searchable and filterable library of exercises.
 * Users can browse exercises by body part, equipment, or movement pattern.
 */
class ExerciseLibraryFragment : Fragment() {

    // View Binding for accessing layout elements
    private var _binding: FragmentExerciseLibraryBinding? = null
    private val binding get() = _binding!!

    // Adapter for the exercise list
    private lateinit var exerciseLibraryAdapter: ExerciseLibraryAdapter

    // Current filter states
    private var selectedBodyPart: String? = null
    private var selectedEquipment: String? = null
    private var selectedMovement: String? = null
    private var searchQuery: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentExerciseLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize the adapter and RecyclerView
        exerciseLibraryAdapter = ExerciseLibraryAdapter()
        binding.rvExercises.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = exerciseLibraryAdapter
        }

        // Setup search bar listener to filter as the user types
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s.toString().trim().takeIf { it.isNotEmpty() }
                applyFilters()
            }
        })

        // Setup filter chip listeners
        binding.chipBodyPart.setOnClickListener {
            showFilterDialog("Body Part", ExerciseDatabase.bodyParts) { selected ->
                selectedBodyPart = selected
                binding.chipBodyPart.text = selected ?: "Body Part"
                binding.chipBodyPart.isChecked = selected != null
                applyFilters()
            }
        }

        binding.chipEquipment.setOnClickListener {
            showFilterDialog("Equipment", ExerciseDatabase.equipmentTypes) { selected ->
                selectedEquipment = selected
                binding.chipEquipment.text = selected ?: "Equipment"
                binding.chipEquipment.isChecked = selected != null
                applyFilters()
            }
        }

        binding.chipMovement.setOnClickListener {
            showFilterDialog("Movement", ExerciseDatabase.movementPatterns) { selected ->
                selectedMovement = selected
                binding.chipMovement.text = selected ?: "Movement"
                binding.chipMovement.isChecked = selected != null
                applyFilters()
            }
        }

        // Button to reset all active filters
        binding.chipClearFilters.setOnClickListener {
            clearFilters()
        }

        // Initial data load
        applyFilters()
    }

    /**
     * Retrieves the filtered list from the database based on current UI state
     * and submits it to the adapter for display.
     */
    private fun applyFilters() {
        val filtered = ExerciseDatabase.filter(
            bodyPart = selectedBodyPart,
            equipment = selectedEquipment,
            movementPattern = selectedMovement,
            searchQuery = searchQuery
        )
        
        // ListAdapter handles the diffing and animations automatically
        exerciseLibraryAdapter.submitList(filtered)
        
        // Update the result count display
        binding.tvResultCount.text = "${filtered.size} exercises"

        // Toggle visibility of the "Clear Filters" button
        val hasFilters = selectedBodyPart != null || selectedEquipment != null || selectedMovement != null
        binding.chipClearFilters.visibility = if (hasFilters) View.VISIBLE else View.GONE
    }

    /**
     * Resets all filter variables and updates the UI components.
     */
    private fun clearFilters() {
        selectedBodyPart = null
        selectedEquipment = null
        selectedMovement = null
        binding.chipBodyPart.text = "Body Part"
        binding.chipBodyPart.isChecked = false
        binding.chipEquipment.text = "Equipment"
        binding.chipEquipment.isChecked = false
        binding.chipMovement.text = "Movement"
        binding.chipMovement.isChecked = false
        applyFilters()
    }

    /**
     * Helper to show a simple selection dialog for a specific category.
     */
    private fun showFilterDialog(title: String, options: List<String>, onSelected: (String?) -> Unit) {
        val items = arrayOf("All") + options.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setItems(items) { _, which ->
                // "All" returns null to clear that specific filter
                onSelected(if (which == 0) null else options[which - 1])
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Prevent memory leaks
        _binding = null
    }
}
