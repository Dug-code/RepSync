package com.repsyncdemo.workout

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.homeFragment, R.id.workoutListFragment, R.id.feedFragment)
        )
        
        setupActionBarWithNavController(navController, appBarConfiguration)
        
        supportActionBar?.setDisplayShowTitleEnabled(false)

        navController.addOnDestinationChangedListener { _, destination, arguments ->
            val isRootProfile = destination.id == R.id.profileFragment && arguments?.getString("userId") == null
            val isTopLevel = appBarConfiguration.topLevelDestinations.contains(destination.id) || isRootProfile

            if (isTopLevel) {
                supportActionBar?.hide()
            } else {
                supportActionBar?.show()
            }
        }

        binding.bottomNav.setupWithNavController(navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        // This ensures that the Action Bar back button triggers the OnBackPressedDispatcher
        // which will catch our "Unsaved Changes" warnings in fragments.
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
