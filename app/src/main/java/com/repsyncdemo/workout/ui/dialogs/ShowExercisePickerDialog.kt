package com.repsyncdemo.workout.ui.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.data.ExerciseDatabase
import com.repsyncdemo.workout.data.model.ExerciseType
import com.repsyncdemo.workout.databinding.FragmentExerciseLibraryBinding
import com.repsyncdemo.workout.ui.adapter.ExerciseLibraryAdapter
import com.repsyncdemo.workout.viewmodel.WorkoutViewModel
import kotlinx.coroutines.launch

class ShowExercisePickerDialog : DialogFragment() {

    private val viewModel: WorkoutViewModel by activityViewModels()
    private var _binding: FragmentExerciseLibraryBinding? = null
    private val binding get() = _binding!!

    private lateinit var pickerAdapter: ExerciseLibraryAdapter

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = FragmentExerciseLibraryBinding.inflate(layoutInflater)

         pickerAdapter = ExerciseLibraryAdapter { exerciseDef ->
            viewModel.selectedExercises(exerciseDef.name)
            dismiss()
        }

        binding.rvExercises.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = pickerAdapter
        }

        // Observe the combined library (Built-in + Custom)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.filteredExercises.collect { newList ->
                    pickerAdapter.submitList(newList)
                    binding.tvResultCount.text = "${newList.size} exercises found"
                    binding.tvResultCount.visibility = if (newList.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }

        // Setup filter chip listeners
        binding.chipBodyPart.setOnClickListener {
            viewModel.selectedFilterCategory(binding.chipBodyPart.text.toString())
            ExercisePickerFilterDialog().show(parentFragmentManager, "filter_dialog")
        }

        binding.chipEquipment.setOnClickListener {
            viewModel.selectedFilterCategory(binding.chipEquipment.text.toString())
            ExercisePickerFilterDialog().show(parentFragmentManager, "filter_dialog")
        }

        binding.chipMovement.setOnClickListener {
            viewModel.selectedFilterCategory(binding.chipMovement.text.toString())
            ExercisePickerFilterDialog().show(parentFragmentManager, "filter_dialog")
        }

        binding.chipClearFilters.setOnClickListener {
            viewModel.clearFilter()
            binding.tvResultCount.visibility = View.GONE
            binding.chipClearFilters.visibility = View.GONE
            binding.chipBodyPart.isChecked = false
            binding.chipEquipment.isChecked = false
            binding.chipMovement.isChecked = false
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.searchExercises(s.toString().trim())
            }
        })

        return AlertDialog.Builder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setTitle("Select Exercise")
            .setView(binding.root)
            .setNegativeButton("Cancel", null)
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
