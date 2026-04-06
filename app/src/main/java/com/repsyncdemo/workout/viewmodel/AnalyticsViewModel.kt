package com.repsyncdemo.workout.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.repsyncdemo.workout.data.model.WorkoutLog
import com.repsyncdemo.workout.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch


class AnalyticsViewModel : ViewModel() {

    private val repository = WorkoutRepository()

    /**
     * Clears all workout logs for the current user to reset analytics.
     */
    fun clearAllHistory() {
        // viewModelScope creates the coroutine needed to call suspend functions
        viewModelScope.launch {
            repository.deleteAllWorkoutLogs()
        }
    }


    /**
     * Observe the raw logs from the repository
     */
    private val workoutLogs: LiveData<List<WorkoutLog>> = repository.getWorkoutLogs()
        .catch { emit(emptyList()) }
        .asLiveData()

    /**
     * Transformation: Calculate the total volume lifted across all logs.
     * Logic: Iterate through every Log -> every Exercise -> every Set.
     */
    val totalVolume: LiveData<Double> = workoutLogs.map { logs ->
        logs.sumOf { log ->
            log.exercises.sumOf { exercise ->
                exercise.sets.sumOf { set ->
                    // Logic check: ensure we are multiplying reps * weight for strength sets
                    if (set.completed && set.weight != null && set.reps != null) {
                        set.weight * set.reps.toDouble()
                    } else {
                        0.0
                    }
                }
            }
        }
    }

    /**
     * Transformation: Total number of completed sets
     */
    val totalSets: LiveData<Int> = workoutLogs.map { logs ->
        logs.sumOf { log ->
            log.exercises.sumOf { it.sets.count { set -> set.completed } }
        }
    }
}
