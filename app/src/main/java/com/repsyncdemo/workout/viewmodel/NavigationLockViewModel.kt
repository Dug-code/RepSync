package com.repsyncdemo.workout.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class NavigationLockViewModel : ViewModel() {
    private val _isLocked = MutableLiveData(false)
    val isLocked: LiveData<Boolean> = _isLocked

    fun setLocked(locked: Boolean) {
        _isLocked.value = locked
    }
}
