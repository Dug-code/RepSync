package com.repsyncdemo.workout

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class RepSyncApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Force dark mode ONLY
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }
}
