package com.repsyncdemo.workout

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.repsyncdemo.workout.data.repository.ProfileRepository
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class RepSyncApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Load theme preference on startup
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val theme = prefs.getString("theme", "dark")
        
        if (theme == "light") {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }
}
