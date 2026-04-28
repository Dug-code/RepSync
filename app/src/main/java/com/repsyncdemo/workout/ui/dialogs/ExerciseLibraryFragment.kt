package com.repsyncdemo.workout.ui.dialogs

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.data.model.ExerciseType
import com.repsyncdemo.workout.databinding.FragmentExerciseLibraryBinding
import com.repsyncdemo.workout.ui.adapter.ExerciseLibraryAdapter
import com.repsyncdemo.workout.viewmodel.ExerciseLibraryFilterState
import com.repsyncdemo.workout.viewmodel.WorkoutViewModel
import kotlinx.coroutines.launch

class ExerciseLibraryFragment : Fragment() {

    private val viewModel: WorkoutViewModel by activityViewModels()
    private var _binding: FragmentExerciseLibraryBinding? = null
    private val binding get() = _binding!!

    private lateinit var pickerAdapter: ExerciseLibraryAdapter
    private var isSyncingFilterUi = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExerciseLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pickerAdapter = ExerciseLibraryAdapter(
            onDeleteCustom = { id -> viewModel.deleteCustomExercise(id) },
            onClick = { exerciseDef ->
                findNavController().previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("selectedExerciseName", exerciseDef.name)
                findNavController().popBackStack()
            }
        )

        binding.rvExercises.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = pickerAdapter
        }

        // Observe the combined and ranked library
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.filteredExercises.collect { newList ->
                    pickerAdapter.submitList(newList)
                    binding.tvResultCount.text = "${newList.size} exercises found"
                    binding.tvResultCount.visibility = if (newList.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.exerciseLibraryFilterState.collect { state ->
                    syncFilterUi(state)
                }
            }
        }

        // Setup filter chip listeners
        binding.chipExerciseType.setOnClickListener {
            if (viewModel.exerciseLibraryFilterState.value.type != null) {
                viewModel.clearExerciseTypeFilter()
            } else {
                syncFilterUi(viewModel.exerciseLibraryFilterState.value)
                viewModel.selectedFilterCategory("Exercise Type")
                findNavController().navigate(R.id.exercisePickerFilterDialog)
            }
        }

        binding.chipBodyPart.setOnClickListener {
            if (viewModel.exerciseLibraryFilterState.value.bodyPart != null) {
                viewModel.clearBodyPartFilter()
            } else {
                syncFilterUi(viewModel.exerciseLibraryFilterState.value)
                viewModel.selectedFilterCategory("Body Part")
                findNavController().navigate(R.id.exercisePickerFilterDialog)
            }
        }

        // Make the Custom chip a toggle
        binding.chipCustomOnly.setOnCheckedChangeListener { _, isChecked ->
            if (!isSyncingFilterUi) {
                viewModel.toggleCustomFilter(isChecked)
            }
        }

        binding.chipClearFilters.setOnClickListener {
            viewModel.clearFilter()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!isSyncingFilterUi) {
                    viewModel.searchExercises(s.toString().trim())
                }
            }
        })
        
        binding.fabAddCustom.setOnClickListener {
            findNavController().navigate(R.id.createCustomExerciseFragment)
        }
    }

    private fun syncFilterUi(state: ExerciseLibraryFilterState) {
        if (_binding == null) return

        isSyncingFilterUi = true

        if (binding.etSearch.text?.toString() != state.searchQuery) {
            binding.etSearch.setText(state.searchQuery)
            binding.etSearch.setSelection(state.searchQuery.length)
        }

        binding.chipExerciseType.isChecked = state.type != null
        binding.chipExerciseType.text = state.type?.let { "Type: ${it.toDisplayName()}" } ?: "Exercise Type"

        binding.chipBodyPart.isChecked = state.bodyPart != null
        binding.chipBodyPart.text = state.bodyPart?.let { "Body: $it" } ?: "Body Part"

        binding.chipCustomOnly.isChecked = state.onlyCustom
        binding.chipCustomOnly.text = if (state.onlyCustom) "Custom only" else "Custom"

        binding.chipClearFilters.visibility = if (state.hasActiveFilters) View.VISIBLE else View.GONE

        isSyncingFilterUi = false
    }

    private fun ExerciseType.toDisplayName(): String = when (this) {
        ExerciseType.STRENGTH -> "Weight Lifting"
        ExerciseType.CARDIO -> "Cardio"
        ExerciseType.CALISTHENICS -> "Calisthenics"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
