package com.repsyncdemo.workout.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.repsyncdemo.workout.data.ExerciseDatabase
import com.repsyncdemo.workout.data.model.ExerciseDefinition
import com.repsyncdemo.workout.data.model.RestDay
import com.repsyncdemo.workout.data.model.Workout
import com.repsyncdemo.workout.data.model.WorkoutLog
import com.repsyncdemo.workout.data.repository.WorkoutRepository
import com.repsyncdemo.workout.util.SingleLiveEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * ViewModel responsible for managing workout-related data and logic.
 * It interacts with the [WorkoutRepository] to perform CRUD operations on workouts and logs.
 */
class WorkoutViewModel : ViewModel() {

    private val repository = WorkoutRepository()

    /**
     * Observable list of all workouts for the current user.
     */
    val workouts: LiveData<List<Workout>> = repository.getWorkouts()
        .catch { e -> 
            Log.e("WorkoutViewModel", "Error in workouts flow", e)
            emit(emptyList()) 
        }
        .asLiveData()

    private val _targetUserWorkouts = MutableLiveData<List<Workout>>()
    /**
     * Observable list of workouts for a specific target user (e.g., a friend).
     */
    val targetUserWorkouts: LiveData<List<Workout>> = _targetUserWorkouts

    private val _targetUserLogs = MutableLiveData<List<WorkoutLog>>()
    /**
     * Observable list of workout logs for a specific target user.
     */
    val targetUserLogs: LiveData<List<WorkoutLog>> = _targetUserLogs

    /**
     * Observable list of all workout logs for the current user.
     */
    val workoutLogs: LiveData<List<WorkoutLog>> = repository.getWorkoutLogs()
        .catch { e -> 
            Log.e("WorkoutViewModel", "Error in workoutLogs flow", e)
            emit(emptyList()) 
        }
        .asLiveData()

    /**
     * Observable list of all rest days for the current user.
     */
    val restDays: LiveData<List<RestDay>> = repository.getRestDays()
        .catch { e ->
            Log.e("WorkoutViewModel", "Error in restDays flow", e)
            emit(emptyList())
        }
        .asLiveData()

    /**
     * Observable integer representing the current consecutive workout streak.
     */
    val currentStreak: LiveData<Int> = workoutLogs.map { logs ->
        calculateStreak(logs)
    }

    private val _selectedWorkout = MutableLiveData<Workout?>()
    /**
     * Currently selected workout for viewing or editing.
     */
    val selectedWorkout: LiveData<Workout?> = _selectedWorkout

    private val _selectedLog = MutableLiveData<WorkoutLog?>()
    /**
     * Currently selected workout log for viewing or editing.
     */
    val selectedLog: LiveData<WorkoutLog?> = _selectedLog

    private val _operationResult = SingleLiveEvent<Result<String>>()
    /**
     * Result of the last database operation (success or failure with a message).
     * Uses [SingleLiveEvent] to ensure events are only handled once.
     */
    val operationResult: LiveData<Result<String>> = _operationResult

    private val _isLoading = MutableLiveData(false)
    /**
     * Observable boolean indicating if a background operation is currently in progress.
     */
    val isLoading: LiveData<Boolean> = _isLoading

    private val _selectedExercises = MutableStateFlow<String?>(null)
    /**
     * Exercises currently selected for a workout being created or edited.
     */
    val selectedExercises: StateFlow<String?> = _selectedExercises




    private val _filterName = MutableStateFlow<String?>(null)
    /**
     *Filter Category name currently selected for filter types to filter exercises by.
     */
    val filterName: StateFlow<String?> = _filterName

    /**
     * Might need to rename as this is a list of filters and might be confused
     * for the final filtered list of exercises
     */
    private val _filterList = MutableStateFlow<List<String>?>(null)
    /**
     *List of filters currently available for exercises being displayed in library.
     */
    val filterList: StateFlow<List<String>?> = _filterList

    private val _selectedFilter = MutableStateFlow<String?>(null)
    /**
     *Filter currently selected for filter types to filter exercises by.
     */
    val selectedFilter: StateFlow<String?> = _selectedFilter

    private val _filteredExercises = MutableStateFlow<List<ExerciseDefinition>>(ExerciseDatabase.allExercises)
    //Stores the final filtered list of exercises to be displayed to the user.
    val filteredExercises: StateFlow<List<ExerciseDefinition>> = _filteredExercises



    /**
     * Calculates the current consecutive workout streak based on the provided logs.
     * A streak continues if there's a workout log for each consecutive day,
     * including today or starting from yesterday.
     */
    private fun calculateStreak(logs: List<WorkoutLog>): Int {
        if (logs.isEmpty()) return 0
        
        val sortedDates = logs.map { 
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.completedAt
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }.distinct().sortedDescending()

        if (sortedDates.isEmpty()) return 0

        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        val todayMs = today.timeInMillis

        var streak = 0
        var currentCheckMs = todayMs
        
        // If they didn't workout today, check if they worked out yesterday
        if (sortedDates[0] < todayMs) {
            val yesterday = Calendar.getInstance()
            yesterday.add(Calendar.DAY_OF_YEAR, -1)
            yesterday.set(Calendar.HOUR_OF_DAY, 0)
            yesterday.set(Calendar.MINUTE, 0)
            yesterday.set(Calendar.SECOND, 0)
            yesterday.set(Calendar.MILLISECOND, 0)
            
            if (sortedDates[0] < yesterday.timeInMillis) {
                return 0 // Streak broken
            }
            currentCheckMs = sortedDates[0]
        }

        for (dateMs in sortedDates) {
            if (dateMs == currentCheckMs) {
                streak++
                // Move check to previous day
                val cal = Calendar.getInstance()
                cal.timeInMillis = currentCheckMs
                cal.add(Calendar.DAY_OF_YEAR, -1)
                currentCheckMs = cal.timeInMillis
            } else if (dateMs < currentCheckMs) {
                break
            }
        }
        
        return streak
    }

    /**
     * Loads workouts for a specific user ID into [targetUserWorkouts].
     */
    fun loadWorkoutsForUser(userId: String) {
        viewModelScope.launch {
            repository.getWorkouts(userId).catch { e ->
                Log.e("WorkoutViewModel", "Error loading user workouts", e)
                emit(emptyList())
            }.collect {
                _targetUserWorkouts.value = it
            }
        }
    }

    /**
     * Loads workout logs for a specific user ID into [targetUserLogs].
     */
    fun loadWorkoutLogsForUser(userId: String) {
        viewModelScope.launch {
            repository.getWorkoutLogs(userId).catch { e ->
                Log.e("WorkoutViewModel", "Error loading user logs", e)
                emit(emptyList())
            }.collect {
                _targetUserLogs.value = it
            }
        }
    }

    /**
     * Creates a copy of an existing workout for the current user.
     */
    fun copyWorkout(workout: Workout) {
        viewModelScope.launch {
            val newWorkout = workout.copy(
                id = "",
                createdAt = System.currentTimeMillis()
            )
            repository.addWorkout(newWorkout)
        }
    }

    /**
     * Loads a single workout by its ID into [selectedWorkout].
     */
    fun loadWorkout(workoutId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.getWorkout(workoutId)
            result.onSuccess { _selectedWorkout.value = it }
            result.onFailure { _operationResult.value = Result.failure(it) }
            _isLoading.value = false
        }
    }

    /**
     * Loads a single workout log by its ID into [selectedLog].
     */
    fun loadWorkoutLog(logId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.getWorkoutLog(logId)
            result.onSuccess { _selectedLog.value = it }
            result.onFailure { _operationResult.value = Result.failure(it) }
            _isLoading.value = false
        }
    }

    /**
     * Adds a new workout to the database.
     */
    fun addWorkout(workout: Workout) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.addWorkout(workout)
            _operationResult.value = result
            _isLoading.value = false
        }
    }

    /**
     * Updates an existing workout in the database.
     */
    fun updateWorkout(workout: Workout) {
        _isLoading.value = true
        viewModelScope.launch {
            repository.updateWorkout(workout)
            _isLoading.value = false
        }
    }

    /**
     * Deletes a workout from the database by its ID.
     */
    fun deleteWorkout(workoutId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            repository.deleteWorkout(workoutId)
            _isLoading.value = false
        }
    }

    /**
     * Deletes a workout log from the database by its ID.
     */
    fun deleteWorkoutLog(logId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            repository.deleteWorkoutLog(logId)
            _isLoading.value = false
        }
    }

    /**
     * Logs a completed workout. If the log has an ID, it updates the existing log;
     * otherwise, it adds a new log.
     */
    fun logWorkout(log: WorkoutLog) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = if (log.id.isEmpty()) {
                repository.addWorkoutLog(log)
            } else {
                repository.updateWorkoutLog(log).map { log.id }
            }
            _operationResult.value = result
            _isLoading.value = false
        }
    }

    /**
     * Adds a rest day for the current date.
     */
    fun addRestDay() {
        viewModelScope.launch {
            repository.addRestDay(RestDay(date = System.currentTimeMillis()))
        }
    }

    /**
     * Sets the currently selected workout.
     */
    fun selectWorkout(workout: Workout) {
        _selectedWorkout.value = workout
    }

    /**
     * Adds a new exercise with the given name to the [selectedExercises] list.
     */
    fun selectedExercises(exerciseName: String) {
        _selectedExercises.value = exerciseName
    }

    fun clearSelectedExercises() {
        _selectedExercises.value = null
    }



    /**
     * Adds a filter category with the given filter name to the [filterName] variable.
     * and updates the [filterList] based on the selected filter category.
     */
    fun selectedFilterCategory(filter: String) {
        //Stores the name of the chip filter the user selects
        _filterName.value = filter

        //Logic for filter list for user to select Body Parts = Arms, Leg, back
        val filtered = when (filter) {
            "Body Part" -> ExerciseDatabase.bodyParts
            "Equipment" -> ExerciseDatabase.equipmentTypes
            "Movement" -> ExerciseDatabase.movementPatterns
            else -> emptyList()
        }

        //Stores the list of filters for the user to select from
        _filterList.value = filtered
    }


    fun clearFilter() {
        _selectedFilter.value = null
        _filterName.value = null

        _filteredExercises.value = ExerciseDatabase.allExercises
    }

    //Stores the filter the user selects from the filter list
    fun selectFilter(filter: String) {
        _selectedFilter.value = filter
    }


    //Updates the filter the user selects from the filter list
    fun updateFilter(category: String, filter: String) {
        _filterName.value = category
        _selectedFilter.value = filter

        // Perform the database filtering here
        _filteredExercises.value = when (category) {
            "Body Part" -> ExerciseDatabase.filter(bodyPart = filter)
            "Equipment" -> ExerciseDatabase.filter(equipment = filter)
            "Movement Pattern" -> ExerciseDatabase.filter(movementPattern = filter)
            else -> ExerciseDatabase.allExercises
        }
    }


    /**
     * Clears the current selection for workout and log.
     */
    fun clearSelection() {
        _selectedWorkout.value = null
        _selectedLog.value = null
    }
}
