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
import com.repsyncdemo.workout.databinding.FragmentExerciseLibraryBinding
import com.repsyncdemo.workout.ui.adapter.ExerciseLibraryAdapter
import com.repsyncdemo.workout.viewmodel.WorkoutViewModel
import kotlinx.coroutines.launch

class ExerciseLibraryFragment : Fragment() {

    private val viewModel: WorkoutViewModel by activityViewModels()
    private var _binding: FragmentExerciseLibraryBinding? = null
    private val binding get() = _binding!!

    private lateinit var pickerAdapter: ExerciseLibraryAdapter

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

        // Setup filter chip listeners
        binding.chipExerciseType.setOnClickListener {
            viewModel.selectedFilterCategory("Exercise Type")
            findNavController().navigate(R.id.exercisePickerFilterDialog)
        }

        binding.chipBodyPart.setOnClickListener {
            viewModel.selectedFilterCategory("Body Part")
            findNavController().navigate(R.id.exercisePickerFilterDialog)
        }

        // Make the Custom chip a toggle
        binding.chipCustomOnly.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleCustomFilter(isChecked)
            updateClearButtonVisibility()
        }

        binding.chipClearFilters.setOnClickListener {
            viewModel.clearFilter()
            binding.chipBodyPart.isChecked = false
            binding.chipExerciseType.isChecked = false
            binding.chipCustomOnly.isChecked = false
            binding.chipClearFilters.visibility = View.GONE
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.searchExercises(s.toString().trim())
                updateClearButtonVisibility()
            }
        })
        
        binding.fabAddCustom.setOnClickListener {
            findNavController().navigate(R.id.createCustomExerciseFragment)
        }
    }

    private fun updateClearButtonVisibility() {
        val hasActiveFilter = binding.chipBodyPart.isChecked || 
                             binding.chipExerciseType.isChecked || 
                             binding.chipCustomOnly.isChecked ||
                             binding.etSearch.text?.isNotEmpty() == true
        
        binding.chipClearFilters.visibility = if (hasActiveFilter) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
