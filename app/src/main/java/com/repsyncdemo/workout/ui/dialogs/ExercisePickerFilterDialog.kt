package com.repsyncdemo.workout.ui.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.repsyncdemo.workout.viewmodel.WorkoutViewModel

class ExercisePickerFilterDialog : DialogFragment() {

    //viewModel used to share data between fragments
    private val viewModel: WorkoutViewModel by activityViewModels()


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let { fragmentActivity ->
            // Use the Builder class for convenient dialog construction.
            val builder = AlertDialog.Builder(fragmentActivity)

            //Adds data from ViewModel to adapter
            val currentList = viewModel.filterList.value?.toTypedArray() ?: arrayOf()

            //Sets title of dialog from chosen filter
            builder.setTitle("Select ${viewModel.filterName.value ?: "Filter"}")

            //Sets adapter for dialog displaying the list
            builder.setAdapter(
                android.widget.ArrayAdapter(
                    fragmentActivity,
                    android.R.layout.simple_selectable_list_item,
                    currentList
                )
            ) { _, which ->
                    // The 'which' argument contains the index position of the selected item.
                val selectedItem = currentList[which]
                val category = viewModel.filterName.value ?: ""

                // Update the ViewModel with the selected item and category.
                viewModel.selectedFilterCategory(selectedItem)
                viewModel.updateFilter(category, selectedItem)
                dismiss()
            }

            // cancels dialog.
            builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss()

            }

            // Create the AlertDialog object and return it.
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}