package com.repsyncdemo.workout.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.data.ExerciseDatabase
import com.repsyncdemo.workout.data.model.ExerciseType
import com.repsyncdemo.workout.databinding.FragmentCreateCustomExerciseBinding
import com.repsyncdemo.workout.viewmodel.WorkoutViewModel
import java.util.Locale

class CreateCustomExerciseFragment : Fragment() {

    private var _binding: FragmentCreateCustomExerciseBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WorkoutViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreateCustomExerciseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDropdowns()
        setupListeners()
    }

    private fun setupDropdowns() {
        // Exercise Types
        val types = ExerciseType.values().map { it.name.lowercase().replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString() } }
        binding.spinnerType.setAdapter(ArrayAdapter(requireContext(), R.layout.list_item_dropdown, types))
        binding.spinnerType.setText(types[0], false)

        // Muscle Groups
        val muscles = listOf("None") + ExerciseDatabase.bodyParts
        val muscleAdapter = ArrayAdapter(requireContext(), R.layout.list_item_dropdown, muscles)
        binding.spinnerPrimaryMuscle.setAdapter(muscleAdapter)
        binding.spinnerSecondaryMuscle.setAdapter(muscleAdapter)
        binding.spinnerPrimaryMuscle.setText("None", false)
        binding.spinnerSecondaryMuscle.setText("None", false)
    }

    private fun setupListeners() {
        binding.btnCreate.setOnClickListener {
            val name = binding.etCustomName.text.toString().trim()
            val typeStr = binding.spinnerType.text.toString().uppercase(Locale.getDefault())
            val type = ExerciseType.valueOf(typeStr)
            val primary = binding.spinnerPrimaryMuscle.text.toString()
            val secondary = binding.spinnerSecondaryMuscle.text.toString()

            if (name.isEmpty()) {
                binding.etCustomName.error = "Name required"
                return@setOnClickListener
            }

            viewModel.addCustomExercise(name, type, primary, secondary)
            Toast.makeText(requireContext(), "$name added to library", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
