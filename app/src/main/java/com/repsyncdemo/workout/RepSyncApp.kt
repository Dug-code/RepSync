package com.repsyncdemo.workout

/**
 * File overview: Application entry point for process-wide setup before activities and fragments are shown.
 */

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class RepSyncApp : Application() {
    // Sets up this screen.
    override fun onCreate() {
        super.onCreate()
        
        // Force dark mode ONLY
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }
}
