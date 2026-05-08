package com.repsyncdemo.workout.ui.workout

/**
 * File overview: Displays saved workout templates with search, sorting, drag-to-reorder, and create-template navigation.
 */

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.data.model.Workout
import com.repsyncdemo.workout.databinding.FragmentWorkoutListBinding
import com.repsyncdemo.workout.ui.adapter.WorkoutAdapter
import com.repsyncdemo.workout.viewmodel.WorkoutViewModel
import java.util.Collections

class WorkoutListFragment : Fragment() {

    private var _binding: FragmentWorkoutListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WorkoutViewModel by activityViewModels()

    private lateinit var workoutAdapter: WorkoutAdapter
    private var allWorkouts: MutableList<Workout> = mutableListOf()
    private var currentSort = SortType.MANUAL
    private var pendingScrollWorkoutId: String? = null

    enum class SortType { MANUAL, DATE, NAME }

    // Sets up this screen.
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkoutListBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Connects views, clicks, and data.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        workoutAdapter = WorkoutAdapter(isReorderable = true) { workout ->
            val bundle = Bundle().apply { putString("workoutId", workout.id) }
            findNavController().navigate(R.id.action_workoutList_to_workoutDetail, bundle)
        }

        binding.rvWorkouts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = workoutAdapter
            // Disable animations to prevent the "double animation" or "flipping" effect 
            // that happens after letting go of a dragged item.
            itemAnimator = null
        }

        setupDragAndDrop()
        setupListeners()
        setupObservers()
    }

    // Sets up this section.
    private fun setupListeners() {
        binding.fabAdd.setOnClickListener {
            findNavController().navigate(R.id.action_workoutList_to_createWorkout)
        }

        binding.btnEmptyAction.setOnClickListener {
            findNavController().navigate(R.id.action_workoutList_to_createWorkout)
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                applyFiltersAndSort()
            }
        })

        binding.chipGroupSort.setOnCheckedStateChangeListener { _, checkedIds ->
            currentSort = when (checkedIds.firstOrNull()) {
                R.id.chipSortDate -> SortType.DATE
                R.id.chipSortName -> SortType.NAME
                else -> SortType.MANUAL
            }
            applyFiltersAndSort()
        }
    }

    // Sets up this section.
    private fun setupObservers() {
        findNavController().currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<String>("scrollToWorkoutId")
            ?.observe(viewLifecycleOwner) { workoutId ->
                pendingScrollWorkoutId = workoutId
                findNavController().currentBackStackEntry
                    ?.savedStateHandle
                    ?.remove<String>("scrollToWorkoutId")
                applyFiltersAndSort()
            }

        viewModel.workouts.observe(viewLifecycleOwner) { workouts ->
            // Only update if we aren't actively reordering to avoid interrupting the user
            allWorkouts = workouts.toMutableList()
            applyFiltersAndSort()
        }
    }

    // Sets up this section.
    private fun setupDragAndDrop() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                if (currentSort != SortType.MANUAL) return false
                
                val fromPos = viewHolder.bindingAdapterPosition
                val toPos = target.bindingAdapterPosition
                
                if (fromPos != toPos && fromPos != RecyclerView.NO_POSITION && toPos != RecyclerView.NO_POSITION) {
                    Collections.swap(allWorkouts, fromPos, toPos)
                    workoutAdapter.notifyItemMoved(fromPos, toPos)
                }
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                // When the drag is finished, persist the order and snap into place
                if (currentSort == SortType.MANUAL) {
                    viewModel.updateWorkoutOrder(allWorkouts)
                }
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.rvWorkouts)
    }

    // Filters or sorts the list.
    private fun applyFiltersAndSort() {
        val query = binding.etSearch.text.toString().trim()
        
        // Update reorder handle visibility based on sort type
        workoutAdapter.setReorderable(currentSort == SortType.MANUAL)

        var filteredList = if (query.isEmpty()) {
            allWorkouts.toList()
        } else {
            allWorkouts.filter { it.name.contains(query, ignoreCase = true) }
        }

        filteredList = when (currentSort) {
            SortType.DATE -> filteredList.sortedByDescending { it.createdAt }
            SortType.NAME -> filteredList.sortedBy { it.name.lowercase() }
            SortType.MANUAL -> filteredList // Already in order from Firestore/Drag
        }
        
        val scrollWorkoutId = pendingScrollWorkoutId
        workoutAdapter.submitList(filteredList) {
            scrollWorkoutId?.let { workoutId ->
                val position = filteredList.indexOfFirst { it.id == workoutId }
                if (position != -1) {
                    binding.rvWorkouts.post {
                        (binding.rvWorkouts.layoutManager as? LinearLayoutManager)
                            ?.scrollToPositionWithOffset(position, 0)
                    }
                    pendingScrollWorkoutId = null
                }
            }
        }
        
        val isEmpty = filteredList.isEmpty()
        binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvWorkouts.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    // Clears the view binding.
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
