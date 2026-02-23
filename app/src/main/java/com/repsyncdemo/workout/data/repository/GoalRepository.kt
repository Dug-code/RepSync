package com.repsyncdemo.workout.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.repsyncdemo.workout.data.model.Goal
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class GoalRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val goalsCollection = db.collection("goals")

    private val currentUserId: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")

    fun getGoals(userId: String? = null): Flow<List<Goal>> = callbackFlow {
        val id = userId ?: currentUserId
        val listener = goalsCollection
            .whereEqualTo("userId", id)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("GoalRepository", "Error fetching goals", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val goals = snapshot?.toObjects(Goal::class.java) ?: emptyList()
                trySend(goals)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addGoal(goal: Goal): Result<String> {
        return try {
            val goalWithUser = goal.copy(userId = currentUserId)
            val doc = goalsCollection.add(goalWithUser).await()
            Result.success(doc.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateGoal(goal: Goal): Result<Unit> {
        return try {
            goalsCollection.document(goal.id).set(goal).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProgress(goalId: String, newValue: Double): Result<Unit> {
        return try {
            goalsCollection.document(goalId).update("currentValue", newValue).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun completeGoal(goalId: String): Result<Unit> {
        return try {
            goalsCollection.document(goalId).update(
                mapOf(
                    "isCompleted" to true,
                    "completedAt" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteGoal(goalId: String): Result<Unit> {
        return try {
            goalsCollection.document(goalId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
