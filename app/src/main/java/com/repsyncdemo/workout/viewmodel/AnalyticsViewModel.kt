package com.repsyncdemo.workout.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.repsyncdemo.workout.data.model.RestDay
import com.repsyncdemo.workout.data.model.WorkoutLog
import com.repsyncdemo.workout.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class TimeRange(val days: Int?, val label: String) {
    LAST_7(7, "7D"),
    LAST_30(30, "30D"),
    LAST_90(90, "90D"),
    LAST_365(365, "1Y"),
    LIFETIME(null, "All")
}

data class ExerciseVolumeBreakdown(
    val name: String,
    val totalVolume: Double,
    val workoutCount: Int
)

class AnalyticsViewModel : ViewModel() {

    private val repository = WorkoutRepository()

    private val _selectedTimeRange = MutableLiveData(TimeRange.LAST_30)
    val selectedTimeRange: LiveData<TimeRange> = _selectedTimeRange

    /**
     * Observe the raw logs from the repository
     */
    val workoutLogs: LiveData<List<WorkoutLog>> = repository.getWorkoutLogs()
        .catch { emit(emptyList()) }
        .asLiveData()

    /**
     * Observe the rest days from the repository
     */
    val restDays: LiveData<List<RestDay>> = repository.getRestDays()
        .catch { emit(emptyList()) }
        .asLiveData()

    /**
     * Filtered Workouts based on range
     */
    val filteredWorkouts: LiveData<List<WorkoutLog>> = _selectedTimeRange.switchMap { range ->
        workoutLogs.map { logs ->
            if (range.days == null) logs
            else {
                val cutOff = Calendar.getInstance().apply { 
                    add(Calendar.DAY_OF_YEAR, -range.days) 
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                logs.filter { it.completedAt >= cutOff }
            }
        }
    }

    /**
     * Filtered Rest Days based on range
     */
    val filteredRestDays: LiveData<List<RestDay>> = _selectedTimeRange.switchMap { range ->
        restDays.map { days ->
            if (range.days == null) days
            else {
                val cutOff = Calendar.getInstance().apply { 
                    add(Calendar.DAY_OF_YEAR, -range.days)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                days.filter { it.date >= cutOff }
            }
        }
    }

    /**
     * Total Volume (Filtered)
     */
    val totalVolume: LiveData<Double> = filteredWorkouts.map { logs ->
        logs.sumOf { calculateLogVolume(it) }
    }

    /**
     * Volume breakdown by exercise
     */
    val volumeBreakdown: LiveData<List<ExerciseVolumeBreakdown>> = filteredWorkouts.map { logs ->
        val breakdownMap = mutableMapOf<String, Pair<Double, MutableSet<String>>>() // Name -> (Volume, Set of workout IDs)

        logs.forEach { log ->
            log.exercises.forEach { exercise ->
                val exerciseVolume = exercise.sets.sumOf { set ->
                    if (set.completed && set.weight != null && set.reps != null) {
                        set.weight * set.reps.toDouble()
                    } else 0.0
                }
                
                if (exerciseVolume > 0) {
                    val current = breakdownMap.getOrDefault(exercise.exerciseName, Pair(0.0, mutableSetOf()))
                    breakdownMap[exercise.exerciseName] = Pair(
                        current.first + exerciseVolume,
                        (current.second + log.id).toMutableSet()
                    )
                }
            }
        }

        breakdownMap.map { (name, stats) ->
            ExerciseVolumeBreakdown(name, stats.first, stats.second.size)
        }.sortedByDescending { it.totalVolume }
    }

    private fun calculateLogVolume(log: WorkoutLog): Double {
        return log.exercises.sumOf { exercise ->
            exercise.sets.sumOf { set ->
                if (set.completed && set.weight != null && set.reps != null) {
                    set.weight * set.reps.toDouble()
                } else 0.0
            }
        }
    }

    val workoutsCount: LiveData<Int> = filteredWorkouts.map { it.size }
    val restDaysCount: LiveData<Int> = filteredRestDays.map { it.size }

    val dateRangeText: LiveData<String> = _selectedTimeRange.map { range ->
        if (range == TimeRange.LIFETIME) "All time activity"
        else {
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            val end = Calendar.getInstance()
            val start = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -(range.days ?: 0)) }
            "${sdf.format(start.time)} - ${sdf.format(end.time)}"
        }
    }

    fun setTimeRange(range: TimeRange) {
        _selectedTimeRange.value = range
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.deleteAllWorkoutLogs()
        }
    }
}
