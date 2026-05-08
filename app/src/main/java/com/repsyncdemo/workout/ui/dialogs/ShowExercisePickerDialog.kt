package com.repsyncdemo.workout.ui.dialogs

/**
 * File overview: Presents a focused dialog or picker flow and returns the selected data to the calling screen.
 */

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

class ShowExercisePickerDialog : Fragment() {

    private val viewModel: WorkoutViewModel by activityViewModels()
    private var _binding: FragmentExerciseLibraryBinding? = null
    private val binding get() = _binding!!

    private lateinit var pickerAdapter: ExerciseLibraryAdapter

    // Sets up this screen.
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExerciseLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Connects views, clicks, and data.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pickerAdapter = ExerciseLibraryAdapter(onClick = { exerciseDef ->
            viewModel.selectExercise(exerciseDef.name)
            findNavController().popBackStack()
        })

        binding.rvExercises.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = pickerAdapter
        }

        // Observe the combined library (Built-in + Custom)
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
        binding.chipBodyPart.setOnClickListener {
            viewModel.selectedFilterCategory(binding.chipBodyPart.text.toString())
            findNavController().navigate(R.id.exercisePickerFilterDialog)
        }

        binding.chipClearFilters.setOnClickListener {
            viewModel.clearFilter()
            binding.tvResultCount.visibility = View.GONE
            binding.chipClearFilters.visibility = View.GONE
            binding.chipBodyPart.isChecked = false
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.searchExercises(s.toString().trim())
                binding.chipClearFilters.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }
        })
        
        binding.fabAddCustom.setOnClickListener {
            findNavController().navigate(R.id.createCustomExerciseFragment)
        }
    }

    // Clears the view binding.
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
