package com.repsyncdemo.workout.ui.dialogs

/**
 * File overview: Presents a focused dialog or picker flow and returns the selected data to the calling screen.
 */

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.viewmodel.WorkoutViewModel

class ExercisePickerFilterDialog : DialogFragment() {

    private val viewModel: WorkoutViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val currentList = viewModel.filterList.value?.toTypedArray() ?: arrayOf()
        val category = viewModel.filterName.value ?: "Filter"

        return MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setTitle("Select $category")
            .setItems(currentList) { _, which ->
                val selectedItem = currentList[which]
                viewModel.updateFilter(category, selectedItem)
                dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
    }
}
