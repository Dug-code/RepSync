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

class ExerciseLibraryFragment : Fragment() {

    private var _binding: FragmentExerciseLibraryBinding? = null
    private val binding get() = _binding!!

    private lateinit var exerciseLibraryAdapter: ExerciseLibraryAdapter

    private var selectedBodyPart: String? = null
    private var selectedEquipment: String? = null
    private var selectedMovement: String? = null
    private var searchQuery: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExerciseLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        exerciseLibraryAdapter = ExerciseLibraryAdapter()

        binding.rvExercises.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = exerciseLibraryAdapter
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s.toString().trim().takeIf { it.isNotEmpty() }
                applyFilters()
            }
        })

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

        binding.chipClearFilters.setOnClickListener {
            clearFilters()
        }

        applyFilters()
    }

    private fun applyFilters() {
        val filtered = ExerciseDatabase.filter(
            bodyPart = selectedBodyPart,
            equipment = selectedEquipment,
            movementPattern = selectedMovement,
            searchQuery = searchQuery
        )
        exerciseLibraryAdapter.submitList(filtered)
        binding.tvResultCount.text = "${filtered.size} exercises"

        val hasFilters = selectedBodyPart != null || selectedEquipment != null || selectedMovement != null
        binding.chipClearFilters.visibility = if (hasFilters) View.VISIBLE else View.GONE
    }

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

    private fun showFilterDialog(title: String, options: List<String>, onSelected: (String?) -> Unit) {
        val items = arrayOf("All") + options.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setItems(items) { _, which ->
                onSelected(if (which == 0) null else options[which - 1])
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
