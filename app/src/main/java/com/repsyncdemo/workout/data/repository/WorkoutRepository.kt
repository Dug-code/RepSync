package com.repsyncdemo.workout.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.repsyncdemo.workout.data.model.Workout
import com.repsyncdemo.workout.data.model.WorkoutLog
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class WorkoutRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val currentUserId: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")

    private val workoutsCollection
        get() = db.collection("workouts")

    private val logsCollection
        get() = db.collection("workout_logs")

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
                    Log.e("WorkoutRepository", "Error fetching workout logs", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val logs = snapshot?.toObjects(WorkoutLog::class.java) ?: emptyList()
                trySend(logs.sortedByDescending { it.completedAt })
            }
        awaitClose { listener.remove() }
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
            // Ensure userId is preserved so it doesn't "disappear" from the query
            val logWithUser = log.copy(userId = currentUserId)
            logsCollection.document(log.id).set(logWithUser).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addWorkoutLog(log: WorkoutLog): Result<String> {
        return try {
            val logWithUser = log.copy(userId = currentUserId)
            val doc = logsCollection.add(logWithUser).await()
            Result.success(doc.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
