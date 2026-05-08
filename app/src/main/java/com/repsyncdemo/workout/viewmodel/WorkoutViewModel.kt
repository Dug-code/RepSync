package com.repsyncdemo.workout.viewmodel

/**
 * File overview: Coordinates workout templates, workout logs, exercise library state, rest days, and save/update operations for workout screens.
 */

import android.util.Log
import androidx.lifecycle.*
import com.google.firebase.auth.FirebaseAuth
import com.repsyncdemo.workout.data.ExerciseDatabase
import com.repsyncdemo.workout.data.model.*
import com.repsyncdemo.workout.data.repository.WorkoutRepository
import com.repsyncdemo.workout.util.SingleLiveEvent
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class ExerciseLibraryFilterState(
    val searchQuery: String = "",
    val bodyPart: String? = null,
    val type: ExerciseType? = null,
    val onlyCustom: Boolean = false
) {
    val hasActiveFilters: Boolean
        get() = searchQuery.isNotEmpty() || bodyPart != null || type != null || onlyCustom
}

class WorkoutViewModel : ViewModel() {

    private val repository = WorkoutRepository()
    private val auth = FirebaseAuth.getInstance()

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

    val weightLogs: LiveData<List<WeightLog>> = repository.getWeightLogs()
        .catch { e ->
            Log.e("WorkoutViewModel", "Error in weight logs flow", e)
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

    // --- Loading State Flags ---
    var isWorkoutDataLoaded = false
        private set

    var isLogDataLoaded = false
        private set

    private val _operationResult = SingleLiveEvent<Result<String>>()
    val operationResult: LiveData<Result<String>> = _operationResult

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // --- Selection State ---
    val selectedExerciseEvent = SingleLiveEvent<String>()

    private val _selectedExerciseName = MutableStateFlow<String?>(null)
    val selectedExerciseName: StateFlow<String?> = _selectedExerciseName

    // --- Cumulative Filtering State ---
    private val _searchQuery = MutableStateFlow("")
    private val _filterBodyPart = MutableStateFlow<String?>(null)
    private val _filterType = MutableStateFlow<ExerciseType?>(null)
    private val _filterOnlyCustom = MutableStateFlow(false)

    val exerciseLibraryFilterState: StateFlow<ExerciseLibraryFilterState> = combine(
        _searchQuery,
        _filterBodyPart,
        _filterType,
        _filterOnlyCustom
    ) { query, bodyPart, type, onlyCustom ->
        ExerciseLibraryFilterState(query, bodyPart, type, onlyCustom)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ExerciseLibraryFilterState())

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

    val filteredExercises: StateFlow<List<ExerciseDefinition>> = combine(
        allLibraryExercises,
        _searchQuery,
        _filterBodyPart,
        _filterType,
        _filterOnlyCustom
    ) { library, query, bodyPart, type, onlyCustom ->
        var list = library
        
        if (onlyCustom) {
            list = list.filter { it.isCustom }
        }
        
        if (type != null) {
            list = list.filter { it.type == type }
        }
        
        if (bodyPart != null) {
            list = list.filter { 
                it.primaryBodyPart.equals(bodyPart, ignoreCase = true) || 
                it.secondaryBodyParts.contains(bodyPart, ignoreCase = true) 
            }
        }
        
        if (query.isNotEmpty()) {
            list = list.filter { 
                it.name.contains(query, ignoreCase = true) || 
                it.primaryBodyPart.contains(query, ignoreCase = true) ||
                it.secondaryBodyParts.contains(query, ignoreCase = true)
            }
        }
        
        sortLibraryExercises(list, query, bodyPart)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private fun sortLibraryExercises(
        exercises: List<ExerciseDefinition>,
        query: String,
        bodyPart: String?
    ): List<ExerciseDefinition> {
        val normalizedQuery = query.lowercase()
        return exercises.sortedWith(
            compareBy<ExerciseDefinition> {
                searchRank(it, normalizedQuery)
            }.thenByDescending {
                bodyPart != null && it.primaryBodyPart.equals(bodyPart, ignoreCase = true)
            }.thenBy {
                it.name.lowercase()
            }.thenBy {
                it.type.name
            }
        )
    }

    private fun searchRank(exercise: ExerciseDefinition, query: String): Int {
        if (query.isEmpty()) return 0

        val name = exercise.name.lowercase()
        val primary = exercise.primaryBodyPart.lowercase()
        val secondary = exercise.secondaryBodyParts.lowercase()

        return when {
            name == query -> 0
            name.startsWith(query) -> 1
            name.split(" ", "-", "(", ")").any { it.startsWith(query) } -> 2
            name.contains(query) -> 3
            primary.contains(query) -> 4
            secondary.contains(query) -> 5
            else -> 6
        }
    }

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

    // --- Filter Helper LiveData (for the Picker UI) ---
    private val _filterName = MutableLiveData<String?>(null)
    val filterName: LiveData<String?> = _filterName

    private val _filterList = MutableLiveData<List<String>?>(null)
    val filterList: LiveData<List<String>?> = _filterList

    // --- Methods ---

    fun selectedFilterCategory(filter: String) {
        _filterName.value = filter
        val list = when (filter) {
            "Body Part" -> ExerciseDatabase.bodyParts
            "Exercise Type" -> ExerciseDatabase.exerciseTypes
            else -> emptyList()
        }
        _filterList.value = list
    }

    // Updates data or UI state.
    fun updateFilter(category: String, filter: String) {
        when (category) {
            "Body Part" -> _filterBodyPart.value = filter
            "Exercise Type" -> {
                _filterType.value = when(filter) {
                    "Weight Lifting" -> ExerciseType.STRENGTH
                    "Cardio" -> ExerciseType.CARDIO
                    "Calisthenics" -> ExerciseType.CALISTHENICS
                    else -> null
                }
            }
        }
    }

    fun clearBodyPartFilter() {
        _filterBodyPart.value = null
    }

    fun clearExerciseTypeFilter() {
        _filterType.value = null
    }

    fun toggleCustomFilter(active: Boolean) {
        _filterOnlyCustom.value = active
    }

    fun clearFilter() {
        _filterBodyPart.value = null
        _filterType.value = null
        _filterOnlyCustom.value = false
        _searchQuery.value = ""
        _filterName.value = null
        _filterList.value = null
    }

    fun searchExercises(query: String) {
        _searchQuery.value = query
    }

    fun selectExercise(exerciseName: String) {
        selectedExerciseEvent.value = exerciseName
    }

    // Calculates values.
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

    // Loads data.
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

    // Loads data.
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
            val uid = auth.currentUser?.uid ?: return@launch
            
            // Sync custom exercises to the copier's library
            workout.exercises.forEach { exercise ->
                if (exercise.isCustom) {
                    val exists = allLibraryExercises.value.any { it.name.equals(exercise.name, ignoreCase = true) }
                    if (!exists) {
                        addCustomExercise(
                            name = exercise.name,
                            type = exercise.type,
                            primary = exercise.primaryMuscleGroup,
                            secondary = exercise.secondaryMuscleGroup
                        )
                    }
                }
            }

            val newWorkout = workout.copy(
                id = "",
                userId = uid,
                createdAt = System.currentTimeMillis()
            )
            repository.addWorkout(newWorkout)
        }
    }

    // Loads data.
    fun loadWorkout(workoutId: String) {
        _isLoading.value = true
        isWorkoutDataLoaded = false // Reset load flag
        viewModelScope.launch {
            val result = repository.getWorkout(workoutId)
            result.onSuccess { _selectedWorkout.value = it }
            result.onFailure { _operationResult.value = Result.failure(it) }
            _isLoading.value = false
        }
    }

    // Reads data.
    suspend fun getWorkoutTemplate(workoutId: String): Result<Workout> {
        return repository.getWorkout(workoutId)
    }

    fun notifyWorkoutLoaded() {
        isWorkoutDataLoaded = true
    }

    // Loads data.
    fun loadWorkoutLog(logId: String) {
        _isLoading.value = true
        isLogDataLoaded = false // Reset load flag
        viewModelScope.launch {
            val result = repository.getWorkoutLog(logId)
            result.onSuccess { _selectedLog.value = it }
            result.onFailure { _operationResult.value = Result.failure(it) }
            _isLoading.value = false
        }
    }

    fun notifyLogLoaded() {
        isLogDataLoaded = true
    }

    fun addWorkout(workout: Workout) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.addWorkout(workout)
            _operationResult.value = result
            _isLoading.value = false
        }
    }

    // Updates data or UI state.
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

    // Updates data or UI state.
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
        isWorkoutDataLoaded = false
        isLogDataLoaded = false
    }

    fun addCustomExercise(name: String, type: ExerciseType, primary: String?, secondary: String?) {
        viewModelScope.launch {
            val definition = ExerciseDefinition(
                name = name,
                type = type,
                primaryBodyPart = primary ?: "",
                secondaryBodyParts = if (secondary == "None" || secondary == null) "" else secondary,
                isCustom = true
            )
            val result = repository.addCustomExercise(definition)
            if (result.isSuccess) {
                // Automatically select the new exercise so it gets added to the current workout
                selectExercise(name)
            }
        }
    }

    fun deleteCustomExercise(exerciseId: String) {
        viewModelScope.launch {
            repository.deleteCustomExercise(exerciseId)
        }
    }
}
