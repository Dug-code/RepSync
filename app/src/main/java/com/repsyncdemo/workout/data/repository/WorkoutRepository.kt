package com.repsyncdemo.workout.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.repsyncdemo.workout.data.model.RestDay
import com.repsyncdemo.workout.data.model.Workout
import com.repsyncdemo.workout.data.model.WorkoutLog
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*

class WorkoutRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val currentUserId: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")

    private val workoutsCollection
        get() = db.collection("workouts")

    private val logsCollection
        get() = db.collection("workout_logs")

    private val restDaysCollection
        get() = db.collection("rest_days")

    fun getWorkouts(userId: String? = null): Flow<List<Workout>> = callbackFlow {
        val id = userId ?: currentUserId
        val listener = workoutsCollection
            .whereEqualTo("userId", id)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("WorkoutRepository", "Error fetching workouts", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val workouts = snapshot?.toObjects(Workout::class.java) ?: emptyList()
                trySend(workouts.sortedByDescending { it.createdAt })
            }
        awaitClose { listener.remove() }
    }

    suspend fun getWorkout(workoutId: String): Result<Workout> {
        return try {
            val doc = workoutsCollection.document(workoutId).get().await()
            val workout = doc.toObject(Workout::class.java)
            if (workout != null) Result.success(workout)
            else Result.failure(Exception("Workout not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addWorkout(workout: Workout): Result<String> {
        return try {
            val workoutWithUser = workout.copy(userId = currentUserId)
            val doc = workoutsCollection.add(workoutWithUser).await()
            Result.success(doc.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateWorkout(workout: Workout): Result<Unit> {
        return try {
            workoutsCollection.document(workout.id).set(workout.copy(userId = currentUserId)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteWorkout(workoutId: String): Result<Unit> {
        return try {
            workoutsCollection.document(workoutId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getWorkoutLogs(userId: String? = null): Flow<List<WorkoutLog>> = callbackFlow {
        val id = userId ?: currentUserId
        val listener = logsCollection
            .whereEqualTo("userId", id)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val logs = snapshot?.toObjects(WorkoutLog::class.java) ?: emptyList()
                trySend(logs.sortedByDescending { it.completedAt })
            }
        awaitClose { listener.remove() }
    }

    fun getRestDays(userId: String? = null): Flow<List<RestDay>> = callbackFlow {
        val id = userId ?: currentUserId
        val listener = restDaysCollection
            .whereEqualTo("userId", id)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val restDays = snapshot?.toObjects(RestDay::class.java) ?: emptyList()
                trySend(restDays)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addRestDay(restDay: RestDay): Result<Unit> {
        return try {
            val startOfDay = getStartOfDay(restDay.date)
            val existing = restDaysCollection
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("date", startOfDay)
                .get().await()
            
            if (existing.isEmpty) {
                restDaysCollection.add(restDay.copy(userId = currentUserId, date = startOfDay)).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeRestDay(timestamp: Long): Result<Unit> {
        return try {
            val startOfDay = getStartOfDay(timestamp)
            val snapshot = restDaysCollection
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("date", startOfDay)
                .get().await()
            
            snapshot.documents.forEach { it.reference.delete() }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getStartOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    suspend fun getWorkoutLog(logId: String): Result<WorkoutLog> {
        return try {
            val doc = logsCollection.document(logId).get().await()
            val log = doc.toObject(WorkoutLog::class.java)
            if (log != null) Result.success(log)
            else Result.failure(Exception("Log not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateWorkoutLog(log: WorkoutLog): Result<Unit> {
        return try {
            val logWithUser = log.copy(userId = currentUserId)
            logsCollection.document(log.id).set(logWithUser).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addWorkoutLog(log: WorkoutLog): Result<String> {
        return try {
            removeRestDay(log.completedAt)
            val logWithUser = log.copy(userId = currentUserId)
            val doc = logsCollection.add(logWithUser).await()
            Result.success(doc.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteWorkoutLog(logId: String): Result<Unit> {
        return try {
            logsCollection.document(logId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
