package com.repsyncdemo.workout.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.repsyncdemo.workout.data.model.*
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
    private val weightLogsCollection = db.collection("weight_logs")

    private val userId: String?
        get() = auth.currentUser?.uid

    fun getWorkouts(targetUserId: String? = null): Flow<List<Workout>> = callbackFlow {
        val id = targetUserId ?: userId
        if (id == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val listener = workoutCollection
            .whereEqualTo("userId", id)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val workouts = snapshot?.toObjects(Workout::class.java) ?: emptyList()
                trySend(workouts.sortedWith(compareBy({ it.order }, { -it.createdAt })))
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
            val uid = userId ?: throw IllegalStateException("User not logged in")
            val workoutWithUser = workout.copy(userId = uid)
            val doc = workoutCollection.add(workoutWithUser).await()
            Result.success(doc.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateWorkout(workout: Workout): Result<Unit> {
        return try {
            val uid = userId ?: throw IllegalStateException("User not logged in")
            val workoutWithUser = workout.copy(userId = uid)
            workoutCollection.document(workout.id).set(workoutWithUser).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateWorkoutOrder(workouts: List<Workout>): Result<Unit> {
        return try {
            val batch = db.batch()
            workouts.forEachIndexed { index, workout ->
                val ref = workoutCollection.document(workout.id)
                batch.update(ref, "order", index)
            }
            batch.commit().await()
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
        if (id == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val listener = logsCollection
            .whereEqualTo("userId", id)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
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

    suspend fun addWorkoutLog(log: WorkoutLog): Result<String> {
        return try {
            val uid = userId ?: throw IllegalStateException("User not logged in")
            val logWithUser = log.copy(userId = uid)
            val doc = logsCollection.add(logWithUser).await()
            Result.success(doc.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateWorkoutLog(log: WorkoutLog): Result<Unit> {
        return try {
            val uid = userId ?: throw IllegalStateException("User not logged in")
            val logWithUser = log.copy(userId = uid)
            logsCollection.document(log.id).set(logWithUser).await()
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
            val uid = userId ?: throw IllegalStateException("User not logged in")
            val snapshot = logsCollection.whereEqualTo("userId", uid).get().await()
            val batch = db.batch()
            snapshot.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getRestDays(): Flow<List<RestDay>> = callbackFlow {
        val uid = userId
        if (uid == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val listener = restDaysCollection
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val days = snapshot?.toObjects(RestDay::class.java) ?: emptyList()
                trySend(days)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addRestDay(restDay: RestDay): Result<Unit> {
        return try {
            val uid = userId ?: throw IllegalStateException("User not logged in")
            restDaysCollection.add(restDay.copy(userId = uid)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteRestDay(restDayId: String): Result<Unit> {
        return try {
            restDaysCollection.document(restDayId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Weight Logging ---

    suspend fun addWeightLog(weight: Double): Result<Unit> {
        return try {
            val uid = userId ?: throw IllegalStateException("User not logged in")
            val log = WeightLog(
                userId = uid,
                weightLbs = weight,
                date = System.currentTimeMillis()
            )
            weightLogsCollection.add(log).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getWeightLogs(): Flow<List<WeightLog>> = callbackFlow {
        val uid = userId
        if (uid == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val listener = weightLogsCollection
            .whereEqualTo("userId", uid)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val logs = snapshot?.toObjects(WeightLog::class.java) ?: emptyList()
                trySend(logs)
            }
        awaitClose { listener.remove() }
    }

    // --- Custom Exercises ---

    fun getCustomExercises(): Flow<List<ExerciseDefinition>> = callbackFlow {
        val uid = userId
        if (uid == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val listener = customExercisesCollection
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val exercises = snapshot?.toObjects(ExerciseDefinition::class.java) ?: emptyList()
                trySend(exercises)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addCustomExercise(exercise: ExerciseDefinition): Result<Unit> {
        return try {
            val uid = userId ?: throw IllegalStateException("User not logged in")
            customExercisesCollection.add(exercise.copy(userId = uid, isCustom = true)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
