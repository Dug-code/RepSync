package com.repsyncdemo.workout.ui.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.repsyncdemo.workout.data.ExerciseDatabase
import com.repsyncdemo.workout.databinding.FragmentExerciseLibraryBinding
import com.repsyncdemo.workout.ui.adapter.ExerciseLibraryAdapter
import com.repsyncdemo.workout.viewmodel.WorkoutViewModel
import kotlinx.coroutines.launch

class ShowExercisePickerDialog : DialogFragment() {

    //viewModel used to share data between fragments
    private val viewModel: WorkoutViewModel by activityViewModels()

    // View Binding for accessing layout elements
    private var _binding: FragmentExerciseLibraryBinding? = null
    private val binding get() = _binding!!

    private lateinit var pickerAdapter: ExerciseLibraryAdapter

    /**
     * Initializes and returns the [Dialog] instance for this [DialogFragment].
     * Handles layout inflation, view references, and adapter setup for the exercise selection list.
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = FragmentExerciseLibraryBinding.inflate(layoutInflater)

        // Set up the adapter with a click callback that notifies the listener of the selected exercise name
         pickerAdapter = ExerciseLibraryAdapter { exerciseDef ->
            // Invoke the callback if it has been set by the calling fragment/activity
            viewModel.selectedExercises(exerciseDef.name)
        }

        // Sets up the RecyclerView with a vertical layout and the picker adapter
        binding.rvExercises.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = pickerAdapter
        }

        // Populate the library with all exercises from the database initially
        pickerAdapter.submitList(ExerciseDatabase.allExercises)

        // Setup filter chip listeners
        binding.chipBodyPart.setOnClickListener {
            viewModel.selectedFilterCategory(binding.chipBodyPart.text.toString())
            ExercisePickerFilterDialog().show(parentFragmentManager, "exercise_picker")
            binding.chipEquipment.visibility = View.GONE
            binding.chipMovement.visibility = View.GONE
            applyFilters()
        }

        binding.chipEquipment.setOnClickListener {
            viewModel.selectedFilterCategory(binding.chipEquipment.text.toString())
            ExercisePickerFilterDialog().show(parentFragmentManager, "exercise_picker")
            binding.chipBodyPart.visibility = View.GONE
            binding.chipMovement.visibility = View.GONE
            applyFilters()
        }

        binding.chipMovement.setOnClickListener {
            viewModel.selectedFilterCategory(binding.chipMovement.text.toString())
            ExercisePickerFilterDialog().show(parentFragmentManager, "exercise_picker")
            binding.chipBodyPart.visibility = View.GONE
            binding.chipEquipment.visibility = View.GONE
            applyFilters()
        }
        binding.chipClearFilters.setOnClickListener {
            viewModel.clearFilter() // Clear selection
            pickerAdapter.submitList(viewModel.filteredExercises.value)

            //Hides clear chip and result count after clear
            binding.tvResultCount.visibility = View.GONE
            binding.chipClearFilters.visibility = View.GONE

            //shows all chips after filters are cleared
            binding.chipBodyPart.visibility = View.VISIBLE
            binding.chipEquipment.visibility = View.VISIBLE
            binding.chipMovement.visibility = View.VISIBLE

            //remove check from all chips
            binding.chipBodyPart.isChecked = false
            binding.chipEquipment.isChecked = false
            binding.chipMovement.isChecked = false

        }


        // Filter logic for the exercise search bar
        /**
         * Top surface logic for search bar. Checks to see if there was anything in the typed in the search bar. if there is
         * entry is sent to ExerciseDatabase to be filtered.
         */
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                // Filter the database based on the search query
                val filtered = if (query.isEmpty()) ExerciseDatabase.allExercises
                else ExerciseDatabase.filter(searchQuery = query)
                // Update the adapter with the filtered list
                pickerAdapter.submitList(filtered)
            }
        })



        // Build and display the selection dialog
        return AlertDialog.Builder(requireContext())
            .setTitle("Pick Exercise")
            .setView(binding.root) // Set custom layout containing search and list
            .setNegativeButton("Done", null)
            .create()
    }

    /**
     * Filters the list of exercises based on the currently selected criteria (body part, equipment, and movement pattern).
     * Updates the UI by submitting the filtered list to the adapter, updating the result count text,
     * and toggling the visibility of the "Clear Filters" button.
     */
    private fun applyFilters() {
        // Uses lifecycleScope the Fragment's lifecycle instead of viewLifecycleOwner as onDialogCreate may cause issues with viewLifeCycleOwner
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.filteredExercises.collect { newList ->

                    //Updates the UI by submitting the filtered list to the adapter, updating the result count text,
                    // and toggling the visibility of the "Clear Filters" button
                    pickerAdapter.submitList(newList)
                    binding.tvResultCount.text = "${newList.size} exercises"

                    // Show/Hide Clear button
                    binding.chipClearFilters.visibility =
                        if (newList.isNullOrEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
    }
}




