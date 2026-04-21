package com.repsyncdemo.workout.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.repsyncdemo.workout.data.model.Goal
import com.repsyncdemo.workout.data.repository.GoalRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class GoalViewModel : ViewModel() {

    private val repository = GoalRepository()

    val goals: LiveData<List<Goal>> = repository.getGoals()
        .catch { e ->
            Log.e("GoalViewModel", "Error in goals flow", e)
            emit(emptyList())
        }
        .asLiveData()

    private val _targetUserGoals = MutableLiveData<List<Goal>>()
    val targetUserGoals: LiveData<List<Goal>> = _targetUserGoals

    private val _operationResult = MutableLiveData<Result<String>>()
    val operationResult: LiveData<Result<String>> = _operationResult

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadGoalsForUser(userId: String) {
        viewModelScope.launch {
            repository.getGoals(userId)
                .catch { e ->
                    Log.e("GoalViewModel", "Error loading goals for user $userId", e)
                    emit(emptyList())
                }
                .collect {
                    _targetUserGoals.value = it
                }
        }
    }

    fun addGoal(goal: Goal) {
        _isLoading.value = true
        viewModelScope.launch {
            _operationResult.value = repository.addGoal(goal)
            _isLoading.value = false
        }
    }

    fun updateGoal(goal: Goal) {
        viewModelScope.launch {
            repository.updateGoal(goal)
        }
    }

    fun updateProgress(goalId: String, newValue: Double) {
        viewModelScope.launch {
            repository.updateProgress(goalId, newValue)
        }
    }

    fun completeGoal(goalId: String) {
        viewModelScope.launch {
            repository.completeGoal(goalId)
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            repository.deleteGoal(goalId)
        }
    }
}
