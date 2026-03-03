package com.repsyncdemo.workout.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.repsyncdemo.workout.data.model.RestDay
import com.repsyncdemo.workout.data.model.Workout
import com.repsyncdemo.workout.data.model.WorkoutLog
import com.repsyncdemo.workout.data.repository.WorkoutRepository
import com.repsyncdemo.workout.util.SingleLiveEvent
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.*

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

    val workoutLogs: LiveData<List<WorkoutLog>> = repository.getWorkoutLogs()
        .catch { e -> 
            Log.e("WorkoutViewModel", "Error in workoutLogs flow", e)
            emit(emptyList()) 
        }
        .asLiveData()

    private val _targetUserWorkoutLogs = MutableLiveData<List<WorkoutLog>>()
    val targetUserWorkoutLogs: LiveData<List<WorkoutLog>> = _targetUserWorkoutLogs

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
                Log.e("WorkoutViewModel", "Error loading user workout logs", e)
                emit(emptyList())
            }.collect {
                _targetUserWorkoutLogs.value = it
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
            _isLoading.value = false
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
            repository.addRestDay(RestDay(date = System.currentTimeMillis()))
        }
    }

    fun selectWorkout(workout: Workout) {
        _selectedWorkout.value = workout
    }

    fun clearSelection() {
        _selectedWorkout.value = null
        _selectedLog.value = null
    }
}
