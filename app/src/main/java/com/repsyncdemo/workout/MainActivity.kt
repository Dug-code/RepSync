package com.repsyncdemo.workout

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.databinding.ActivityMainBinding
import com.repsyncdemo.workout.viewmodel.NavigationLockViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val navigationLockViewModel: NavigationLockViewModel by viewModels()

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

        // Prevent tab switching when locked with a confirmation dialog
        binding.bottomNav.setOnItemSelectedListener { item ->
            if (navigationLockViewModel.isLocked.value == true) {
                AlertDialog.Builder(this)
                    .setTitle("Unsaved Changes")
                    .setMessage("You have unsaved changes. Are you sure you want to discard them?")
                    .setPositiveButton("Discard") { _, _ ->
                        navigationLockViewModel.setLocked(false)
                        navController.navigate(item.itemId)
                    }
                    .setNegativeButton("Keep Editing", null)
                    .show()
                false
            } else {
                if (item.itemId != navController.currentDestination?.id) {
                    navController.navigate(item.itemId)
                }
                true
            }
        }

        navigationLockViewModel.isLocked.observe(this) { isLocked ->
            binding.bottomNav.alpha = if (isLocked) 0.5f else 1.0f
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
