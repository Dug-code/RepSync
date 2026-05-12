package com.repsyncdemo.workout.util

import android.content.Context
import androidx.core.content.edit

object TutorialPreferences {
    private const val PREFS_NAME = "repsync_prefs"
    const val KEY_REPEAT_TUTORIALS_ON_RESTART = "repeat_tutorials_on_restart"

    private val tutorialCompletedKeys = listOf(
        "home_tutorial_completed",
        "feed_tutorial_completed",
        "goal_tutorial_completed",
        "analytics_tutorial_completed",
        "profile_tutorial_completed",
        "workout_tutorial_completed"
    )

    fun shouldRepeatOnRestart(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_REPEAT_TUTORIALS_ON_RESTART, false)
    }

    fun setRepeatOnRestart(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_REPEAT_TUTORIALS_ON_RESTART, enabled)
        }
    }

    fun resetCompletedTutorials(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            tutorialCompletedKeys.forEach { remove(it) }
        }
    }
}
