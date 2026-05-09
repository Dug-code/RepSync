package com.repsyncdemo.workout.viewmodel

/**
 * File overview: Builds analytics-ready workout, rest-day, PR, volume, and weight-tracking data for the analytics screens.
 */

import androidx.lifecycle.*
import com.repsyncdemo.workout.data.ExerciseDatabase
import com.repsyncdemo.workout.data.model.RestDay
import com.repsyncdemo.workout.data.model.UserProfile
import com.repsyncdemo.workout.data.model.WeightLog
import com.repsyncdemo.workout.data.model.WorkoutLog
import com.repsyncdemo.workout.data.repository.ProfileRepository
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

data class StatItem(val name: String, val count: Int)

data class ExerciseVolumeBreakdown(
    val name: String,
    val totalVolume: Double,
    val workoutCount: Int
)

class AnalyticsViewModel : ViewModel() {

    private val repository = WorkoutRepository()
    private val profileRepository = ProfileRepository()

    private val _selectedTimeRange = MutableLiveData(TimeRange.LAST_30)
    val selectedTimeRange: LiveData<TimeRange> = _selectedTimeRange

    val workoutLogs: LiveData<List<WorkoutLog>> = repository.getWorkoutLogs()
        .catch { emit(emptyList()) }
        .asLiveData()

    val restDays: LiveData<List<RestDay>> = repository.getRestDays()
        .catch { emit(emptyList()) }
        .asLiveData()

    val weightHistory: LiveData<List<WeightLog>> = repository.getWeightLogs()
        .catch { emit(emptyList()) }
        .asLiveData()

    val currentProfile: LiveData<UserProfile?> = profileRepository.observeProfile()
        .catch { emit(null) }
        .asLiveData()

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

    val filteredWeightHistory: LiveData<List<WeightLog>> = _selectedTimeRange.switchMap { range ->
        MediatorLiveData<List<WeightLog>>().apply {
            // Updates data or UI state.
            fun update() {
                value = buildDailyWeightHistory(
                    history = weightHistory.value.orEmpty(),
                    profile = currentProfile.value,
                    range = range
                )
            }

            addSource(weightHistory) { update() }
            addSource(currentProfile) { update() }
        }
    }

    val totalVolume: LiveData<Double> = filteredWorkouts.map { logs ->
        logs.sumOf { calculateLogVolume(it) }
    }

    val totalDurationMinutes: LiveData<Int> = filteredWorkouts.map { logs ->
        logs.sumOf { it.durationMinutes }
    }

    val averageDurationMinutes: LiveData<Int> = filteredWorkouts.map { logs ->
        if (logs.isEmpty()) 0
        else logs.sumOf { it.durationMinutes } / logs.size
    }

    val muscleGroupDistribution: LiveData<Map<String, Int>> = filteredWorkouts.map { logs ->
        val distribution = mutableMapOf<String, Int>()
        logs.forEach { log ->
            log.exercises.forEach { exercise ->
                val muscle = ExerciseDatabase.getExerciseByName(exercise.exerciseName)?.primaryBodyPart ?: "Other"
                val completedSets = exercise.sets.count { it.completed }
                if (completedSets > 0) {
                    distribution[muscle] = distribution.getOrDefault(muscle, 0) + completedSets
                }
            }
        }
        distribution
    }

    val weeklyConsistency: LiveData<List<Pair<String, Int>>> = workoutLogs.map { logs ->
        val calendar = Calendar.getInstance()
        val weeks = mutableListOf<Pair<String, Int>>()
        
        // Count workouts for each of the last 8 weeks
        for (i in 7 downTo 0) {
            val weekCalendar = Calendar.getInstance()
            weekCalendar.add(Calendar.WEEK_OF_YEAR, -i)
            val weekNum = weekCalendar.get(Calendar.WEEK_OF_YEAR)
            val year = weekCalendar.get(Calendar.YEAR)
            
            val count = logs.count { log ->
                calendar.timeInMillis = log.completedAt
                calendar.get(Calendar.WEEK_OF_YEAR) == weekNum && calendar.get(Calendar.YEAR) == year
            }
            
            // Format label as "MMM d" (start of week)
            weekCalendar.set(Calendar.DAY_OF_WEEK, weekCalendar.firstDayOfWeek)
            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
            weeks.add(sdf.format(weekCalendar.time) to count)
        }
        weeks
    }

    val volumeBreakdown: LiveData<List<ExerciseVolumeBreakdown>> = filteredWorkouts.map { logs ->
        val breakdownMap = mutableMapOf<String, Pair<Double, MutableSet<String>>>()
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
        breakdownMap.map { ExerciseVolumeBreakdown(it.key, it.value.first, it.value.second.size) }
            .sortedByDescending { it.totalVolume }
    }

    // --- Favorites Logic ---

    val topWorkouts: LiveData<List<StatItem>> = filteredWorkouts.map { logs ->
        logs.groupingBy { it.workoutName }.eachCount()
            .map { StatItem(it.key, it.value) }
            .sortedByDescending { it.count }
            .take(10)
    }

    val topMuscleGroups: LiveData<List<StatItem>> = filteredWorkouts.map { logs ->
        logs.flatMap { log -> 
            log.exercises.mapNotNull { exercise ->
                ExerciseDatabase.getExerciseByName(exercise.exerciseName)?.primaryBodyPart
            }
        }.groupingBy { it }.eachCount()
            .map { StatItem(it.key, it.value) }
            .sortedByDescending { it.count }
            .take(10)
    }

    val favoriteWorkout: LiveData<StatItem?> = topWorkouts.map { it.firstOrNull() }
    val favoriteMuscle: LiveData<StatItem?> = topMuscleGroups.map { it.firstOrNull() }

    // --- PR Tracking Logic ---

    private val _selectedExerciseForPR = MutableLiveData<String?>(null)
    val selectedExerciseForPR: LiveData<String?> = _selectedExerciseForPR

    val availableExercises: LiveData<List<String>> = workoutLogs.map { logs ->
        logs.flatMap { log -> log.exercises.map { it.exerciseName } }.distinct().sorted()
    }

    val prHistory: LiveData<List<Pair<Long, Double>>> = _selectedExerciseForPR.switchMap { exerciseName ->
        workoutLogs.map { logs ->
            if (exerciseName == null) emptyList()
            else {
                logs.filter { log -> log.exercises.any { it.exerciseName == exerciseName } }
                    .map { log ->
                        val maxWeight = log.exercises.find { it.exerciseName == exerciseName }
                            ?.sets?.mapNotNull { it.weight }?.maxOrNull() ?: 0.0
                        log.completedAt to maxWeight
                    }
                    .sortedBy { it.first }
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

    // Calculates values.
    private fun calculateLogVolume(log: WorkoutLog): Double {
        return log.exercises.sumOf { exercise ->
            exercise.sets.sumOf { set ->
                if (set.completed && set.weight != null && set.reps != null) {
                    set.weight * set.reps.toDouble()
                } else 0.0
            }
        }
    }

    private fun buildDailyWeightHistory(
        history: List<WeightLog>,
        profile: UserProfile?,
        range: TimeRange
    ): List<WeightLog> {
        val seededHistory = if (history.any { it.weightLbs > 0 }) {
            history
        } else {
            val profileWeight = profile?.weightLbs ?: 0.0
            if (profileWeight > 0) {
                listOf(
                    WeightLog(
                        userId = profile?.userId.orEmpty(),
                        weightLbs = profileWeight,
                        date = profile?.lastWeighInDate?.takeIf { it > 0 }
                            ?: profile?.updatedAt?.takeIf { it > 0 }
                            ?: System.currentTimeMillis()
                    )
                )
            } else {
                emptyList()
            }
        }

        val logsByDay = seededHistory
            .filter { it.weightLbs > 0 }
            .groupBy { startOfDay(it.date) }
            .mapValues { (_, logs) -> logs.maxByOrNull { it.date }!! }

        if (logsByDay.isEmpty()) return emptyList()

        val sortedDays = logsByDay.keys.sorted()
        val today = startOfDay(System.currentTimeMillis())
        val firstLoggedDay = sortedDays.first()
        val requestedStartDay = range.days?.let { days ->
            Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -days)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }

        val startDay = requestedStartDay?.takeIf { start ->
            sortedDays.any { it <= start }
        } ?: firstLoggedDay

        var lastKnownWeight = sortedDays
            .filter { it <= startDay }
            .maxOrNull()
            ?.let { logsByDay[it]?.weightLbs }

        val points = mutableListOf<WeightLog>()
        val cursor = Calendar.getInstance().apply { timeInMillis = startDay }

        while (cursor.timeInMillis <= today) {
            val day = cursor.timeInMillis
            logsByDay[day]?.let { lastKnownWeight = it.weightLbs }

            lastKnownWeight?.let { weight ->
                points.add(
                    WeightLog(
                        id = logsByDay[day]?.id.orEmpty(),
                        userId = logsByDay[day]?.userId.orEmpty(),
                        weightLbs = weight,
                        date = day
                    )
                )
            }

            cursor.add(Calendar.DAY_OF_YEAR, 1)
        }

        return points
    }

    private fun startOfDay(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun setTimeRange(range: TimeRange) { _selectedTimeRange.value = range }
    fun selectExerciseForPR(name: String) { _selectedExerciseForPR.value = name }
    fun clearAllHistory() { viewModelScope.launch { repository.deleteAllWorkoutLogs() } }
}
