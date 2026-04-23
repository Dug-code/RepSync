package com.repsyncdemo.workout.viewmodel

import android.util.Log
import androidx.lifecycle.*
import com.repsyncdemo.workout.data.ExerciseDatabase
import com.repsyncdemo.workout.data.model.*
import com.repsyncdemo.workout.data.repository.WorkoutRepository
import com.repsyncdemo.workout.util.SingleLiveEvent
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class WorkoutViewModel : ViewModel() {

    private val repository = WorkoutRepository()

    val workouts: LiveData<List<Workout>> = repository.getWorkouts()
        .catch { e -> 
            Log.e("WorkoutViewModel", "Error in workouts flow", e)
            emit(emptyList()) 
        }
        .asLiveData()

    private val _targetUserWorkouts = MutableLiveData<List<Workout>>()
    val targetUserWorkouts: LiveData<List<Workout>> = _targetUserWorkouts

    private val _targetUserLogs = MutableLiveData<List<WorkoutLog>>()
    val targetUserLogs: LiveData<List<WorkoutLog>> = _targetUserLogs

    val workoutLogs: LiveData<List<WorkoutLog>> = repository.getWorkoutLogs()
        .catch { e -> 
            Log.e("WorkoutViewModel", "Error in workoutLogs flow", e)
            emit(emptyList()) 
        }
        .asLiveData()

    val restDays: LiveData<List<RestDay>> = repository.getRestDays()
        .catch { e ->
            Log.e("WorkoutViewModel", "Error in restDays flow", e)
            emit(emptyList())
        }
        .asLiveData()

    val currentStreak: LiveData<Int> = workoutLogs.map { logs ->
        calculateStreak(logs)
    }

    private val _selectedWorkout = MutableLiveData<Workout?>()
    val selectedWorkout: LiveData<Workout?> = _selectedWorkout

    private val _selectedLog = MutableLiveData<WorkoutLog?>()
    val selectedLog: LiveData<WorkoutLog?> = _selectedLog

    private val _operationResult = SingleLiveEvent<Result<String>>()
    val operationResult: LiveData<Result<String>> = _operationResult

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _selectedExercises = MutableStateFlow<String?>(null)
    val selectedExercises: StateFlow<String?> = _selectedExercises

    private val _filterName = MutableStateFlow<String?>(null)
    val filterName: StateFlow<String?> = _filterName

    private val _filterList = MutableStateFlow<List<String>?>(null)
    val filterList: StateFlow<List<String>?> = _filterList

    private val _selectedFilter = MutableStateFlow<String?>(null)
    val selectedFilter: StateFlow<String?> = _selectedFilter

    // Combined library flow: Static Database + Firestore Custom Exercises
    private val _customExercises = repository.getCustomExercises()
        .onStart { emit(emptyList()) }
        .catch { e ->
            Log.e("WorkoutViewModel", "Error loading custom exercises", e)
            emit(emptyList()) 
        }

    private val _libraryExercises = MutableStateFlow<List<ExerciseDefinition>>(ExerciseDatabase.allExercises)

    val allLibraryExercises: StateFlow<List<ExerciseDefinition>> = combine(
        _libraryExercises,
        _customExercises
    ) { static, custom ->
        (static + custom).distinctBy { it.name.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ExerciseDatabase.allExercises)

    // Comprehensive list of all exercises the user has interacted with
    val allUniqueExerciseNames: StateFlow<List<String>> = combine(
        allLibraryExercises,
        repository.getWorkouts(),
        repository.getWorkoutLogs()
    ) { library, templates, logs ->
        val names = mutableSetOf<String>()
        library.forEach { names.add(it.name) }
        templates.forEach { workout ->
            workout.exercises.forEach { names.add(it.name) }
        }
        logs.forEach { log ->
            log.exercises.forEach { names.add(it.exerciseName) }
        }
        names.toList().filter { it.isNotEmpty() }.sorted()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _filteredExercises = MutableStateFlow<List<ExerciseDefinition>>(ExerciseDatabase.allExercises)
    val filteredExercises: StateFlow<List<ExerciseDefinition>> = _filteredExercises

    init {
        viewModelScope.launch {
            allLibraryExercises.collect {
                if (_filterName.value == null && selectedFilter.value == null) {
                    _filteredExercises.value = it
                }
            }
        }
    }

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
        
        if (sortedDates[0] < todayMs) {
            val yesterday = Calendar.getInstance()
            yesterday.add(Calendar.DAY_OF_YEAR, -1)
            yesterday.set(Calendar.HOUR_OF_DAY, 0)
            yesterday.set(Calendar.MINUTE, 0)
            yesterday.set(Calendar.SECOND, 0)
            yesterday.set(Calendar.MILLISECOND, 0)
            
            if (sortedDates[0] < yesterday.timeInMillis) {
                return 0
            }
            currentCheckMs = sortedDates[0]
        }

        for (dateMs in sortedDates) {
            if (dateMs == currentCheckMs) {
                streak++
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

    fun copyWorkout(workout: Workout) {
        viewModelScope.launch {
            val newWorkout = workout.copy(
                id = "",
                createdAt = System.currentTimeMillis()
            )
            repository.addWorkout(newWorkout)
        }
    }

    fun loadWorkout(workoutId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.getWorkout(workoutId)
            result.onSuccess { _selectedWorkout.value = it }
            result.onFailure { _operationResult.value = Result.failure(it) }
            _isLoading.value = false
        }
    }

    fun loadWorkoutLog(logId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.getWorkoutLog(logId)
            result.onSuccess { _selectedLog.value = it }
            result.onFailure { _operationResult.value = Result.failure(it) }
            _isLoading.value = false
        }
    }

    fun addWorkout(workout: Workout) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.addWorkout(workout)
            _operationResult.value = result
            _isLoading.value = false
        }
    }

    fun updateWorkout(workout: Workout) {
        _isLoading.value = true
        viewModelScope.launch {
            repository.updateWorkout(workout)
            if (_selectedWorkout.value?.id == workout.id) {
                _selectedWorkout.value = workout
            }
            _isLoading.value = false
        }
    }

    fun updateWorkoutOrder(workouts: List<Workout>) {
        viewModelScope.launch {
            repository.updateWorkoutOrder(workouts)
        }
    }

    fun deleteWorkout(workoutId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            repository.deleteWorkout(workoutId)
            _isLoading.value = false
        }
    }

    fun deleteWorkoutLog(logId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            repository.deleteWorkoutLog(logId)
            _isLoading.value = false
        }
    }

    fun logWorkout(log: WorkoutLog) {
        _isLoading.value = true
        viewModelScope.launch {
            // 1. Automatically remove any rest day for this date
            removeRestDayForDate(log.completedAt)

            // 2. Log the workout
            val result = if (log.id.isEmpty()) {
                repository.addWorkoutLog(log)
            } else {
                repository.updateWorkoutLog(log).map { log.id }
            }
            _operationResult.value = result
            _isLoading.value = false
        }
    }

    fun addRestDay() {
        viewModelScope.launch {
            val today = System.currentTimeMillis()
            
            // Check if a workout exists for today
            val hasWorkoutToday = workoutLogs.value?.any { isSameDay(it.completedAt, today) } ?: false
            
            if (hasWorkoutToday) {
                _operationResult.value = Result.failure(Exception("Cannot log rest day: A workout was already performed today."))
                return@launch
            }

            // Check if a rest day already exists for today to avoid duplicates
            val hasRestDayToday = restDays.value?.any { isSameDay(it.date, today) } ?: false
            if (hasRestDayToday) {
                _operationResult.value = Result.failure(Exception("Rest day already logged for today."))
                return@launch
            }

            repository.addRestDay(RestDay(date = today))
            _operationResult.value = Result.success("Rest day logged successfully")
        }
    }

    private suspend fun removeRestDayForDate(timestamp: Long) {
        val restDaysList = restDays.value ?: return
        val dayToRemove = restDaysList.find { isSameDay(it.date, timestamp) }
        dayToRemove?.let {
            repository.deleteRestDay(it.id)
        }
    }

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun addWeightLog(weight: Double) {
        viewModelScope.launch {
            repository.addWeightLog(weight)
        }
    }

    fun clearSelection() {
        _selectedWorkout.value = null
        _selectedLog.value = null
    }

    // RESTORED: These methods were accidentally removed during cleanup and are required for the Exercise Picker
    fun selectWorkout(workout: Workout) {
        _selectedWorkout.value = workout
    }

    fun selectedExercises(exerciseName: String) {
        _selectedExercises.value = exerciseName
    }

    fun clearSelectedExercises() {
        _selectedExercises.value = null
    }

    fun selectedFilterCategory(filter: String) {
        _filterName.value = filter
        val filtered = when (filter) {
            "Body Part" -> ExerciseDatabase.bodyParts
            "Equipment" -> ExerciseDatabase.equipmentTypes
            "Movement" -> ExerciseDatabase.movementPatterns
            else -> emptyList()
        }
        _filterList.value = filtered
    }

    fun clearFilter() {
        _selectedFilter.value = null
        _filterName.value = null
        _filteredExercises.value = allLibraryExercises.value
    }

    fun selectFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun updateFilter(category: String, filter: String) {
        _filterName.value = category
        _selectedFilter.value = filter
        
        val baseList = allLibraryExercises.value
        _filteredExercises.value = when (category) {
            "Body Part" -> baseList.filter { it.primaryBodyPart.equals(filter, ignoreCase = true) }
            "Equipment" -> baseList.filter { it.equipment.equals(filter, ignoreCase = true) }
            "Movement" -> baseList.filter { it.movementPattern.equals(filter, ignoreCase = true) }
            else -> baseList
        }
    }

    fun searchExercises(query: String) {
        val baseList = allLibraryExercises.value
        if (query.isEmpty()) {
            _filteredExercises.value = baseList
        } else {
            _filteredExercises.value = baseList.filter { 
                it.name.contains(query, ignoreCase = true) || 
                it.primaryBodyPart.contains(query, ignoreCase = true)
            }
        }
    }

    fun addCustomExercise(name: String, type: ExerciseType, primary: String?, secondary: String?) {
        viewModelScope.launch {
            val definition = ExerciseDefinition(
                name = name,
                type = type,
                primaryBodyPart = primary ?: "",
                secondaryBodyParts = if (secondary == "None") "" else (secondary ?: ""),
                isCustom = true
            )
            repository.addCustomExercise(definition)
        }
    }
}
