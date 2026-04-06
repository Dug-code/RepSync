package com.repsyncdemo.workout.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.repsyncdemo.workout.data.model.ExerciseDefinition
import com.repsyncdemo.workout.data.model.RestDay
import com.repsyncdemo.workout.data.model.Workout
import com.repsyncdemo.workout.data.model.WorkoutLog
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class WorkoutRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val workoutCollection = db.collection("workouts")
    private val logsCollection = db.collection("workout_logs")
    private val restDaysCollection = db.collection("rest_days")
    private val customExercisesCollection = db.collection("custom_exercises")

    private val userId: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")

    fun getWorkouts(targetUserId: String? = null): Flow<List<Workout>> = callbackFlow {
        val id = targetUserId ?: userId
        val listener = workoutCollection
            .whereEqualTo("userId", id)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val workouts = snapshot?.toObjects(Workout::class.java) ?: emptyList()
                trySend(workouts.sortedByDescending { it.createdAt })
            }
        awaitClose { listener.remove() }
    }

    suspend fun getWorkout(workoutId: String): Result<Workout> {
        return try {
            val doc = workoutCollection.document(workoutId).get().await()
            val workout = doc.toObject(Workout::class.java)
            if (workout != null) Result.success(workout)
            else Result.failure(Exception("Workout not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addWorkout(workout: Workout): Result<String> {
        return try {
            val workoutWithUser = workout.copy(userId = userId)
            val doc = workoutCollection.add(workoutWithUser).await()
            Result.success(doc.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateWorkout(workout: Workout): Result<Unit> {
        return try {
            workoutCollection.document(workout.id).set(workout).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteWorkout(workoutId: String): Result<Unit> {
        return try {
            workoutCollection.document(workoutId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getWorkoutLogs(targetUserId: String? = null): Flow<List<WorkoutLog>> = callbackFlow {
        val id = targetUserId ?: userId
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

    suspend fun addWorkoutLog(log: WorkoutLog): Result<String> {
        return try {
            val logWithUser = log.copy(userId = userId)
            val doc = logsCollection.add(logWithUser).await()
            Result.success(doc.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateWorkoutLog(log: WorkoutLog): Result<Unit> {
        return try {
            logsCollection.document(log.id).set(log).await()
            Result.success(Unit)
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

    suspend fun deleteAllWorkoutLogs(): Result<Unit> {
        return try {
            val snapshot = logsCollection.whereEqualTo("userId", userId).get().await()
            val batch = db.batch()
            snapshot.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getRestDays(): Flow<List<RestDay>> = callbackFlow {
        val listener = restDaysCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val days = snapshot?.toObjects(RestDay::class.java) ?: emptyList()
                trySend(days)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addRestDay(restDay: RestDay): Result<Unit> {
        return try {
            restDaysCollection.add(restDay.copy(userId = userId)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Custom Exercises ---

    fun getCustomExercises(): Flow<List<ExerciseDefinition>> = callbackFlow {
        val listener = customExercisesCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val exercises = snapshot?.toObjects(ExerciseDefinition::class.java) ?: emptyList()
                trySend(exercises)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addCustomExercise(exercise: ExerciseDefinition): Result<Unit> {
        return try {
            customExercisesCollection.add(exercise.copy(userId = userId, isCustom = true)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
