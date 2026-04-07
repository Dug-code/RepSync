package com.repsyncdemo.workout

import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.databinding.ActivityMainBinding
import com.repsyncdemo.workout.viewmodel.NavigationLockViewModel
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val navigationLockViewModel: NavigationLockViewModel by viewModels()
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        // Top-level destinations (Home, Workouts, Feed, Analytics, and root Profile)
        // Back arrow is automatically hidden for these by AppBarConfiguration
        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.homeFragment, R.id.workoutListFragment, R.id.feedFragment, R.id.analyticsFragment, R.id.profileFragment)
        )
        
        setupActionBarWithNavController(navController, appBarConfiguration)
        
        // Hide the "RepSync" title text on the action bar
        supportActionBar?.setDisplayShowTitleEnabled(false)

        navController.addOnDestinationChangedListener { _, destination, arguments ->
            val isRootProfile = destination.id == R.id.profileFragment && arguments?.getString("userId") == null
            val isTopLevel = appBarConfiguration.topLevelDestinations.contains(destination.id) || isRootProfile

            // Hide action bar completely on top-level tabs to keep it clean
            if (isTopLevel) {
                supportActionBar?.hide()
            } else {
                supportActionBar?.show()
            }
        }

        binding.bottomNav.setupWithNavController(navController)

        // Custom listener to handle navigation lock and prevents switching when changes are unsaved
        binding.bottomNav.setOnItemSelectedListener { item ->
            if (navigationLockViewModel.isLocked.value == true) {
                showLockWarning(item.itemId)
                false
            } else {
                if (item.itemId != navController.currentDestination?.id) {
                    navController.navigate(item.itemId)
                }
                true
            }
        }

        setupBottomNavSlide()

        navigationLockViewModel.isLocked.observe(this) { isLocked ->
            binding.bottomNav.alpha = if (isLocked) 0.5f else 1.0f
        }
    }

    /**
     * Implements a sliding/swipe interaction specifically on the bottom navigation bar.
     * Allows users to glide between tabs by swiping across the bottom icons.
     */
    private fun setupBottomNavSlide() {
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null || navigationLockViewModel.isLocked.value == true) return false
                
                val diffX = e2.x - e1.x
                // threshold for detecting a swipe
                if (abs(diffX) > 100 && abs(velocityX) > 100) {
                    val menu = binding.bottomNav.menu
                    val currentId = binding.bottomNav.selectedItemId
                    var currentIndex = -1
                    
                    for (i in 0 until menu.size()) {
                        if (menu.getItem(i).itemId == currentId) {
                            currentIndex = i
                            break
                        }
                    }

                    if (diffX > 0) { // Swipe Right -> Previous Tab
                        if (currentIndex > 0) {
                            binding.bottomNav.selectedItemId = menu.getItem(currentIndex - 1).itemId
                        }
                    } else { // Swipe Left -> Next Tab
                        if (currentIndex < menu.size() - 1) {
                            binding.bottomNav.selectedItemId = menu.getItem(currentIndex + 1).itemId
                        }
                    }
                    return true
                }
                return false
            }
        })

        binding.bottomNav.setOnTouchListener { v, event ->
            if (gestureDetector.onTouchEvent(event)) {
                true
            } else {
                v.onTouchEvent(event)
            }
        }
    }

    private fun showLockWarning(targetMenuId: Int) {
        AlertDialog.Builder(this)
            .setTitle("Unsaved Changes")
            .setMessage("You have unsaved changes. Are you sure you want to discard them?")
            .setPositiveButton("Discard") { _, _ ->
                navigationLockViewModel.setLocked(false)
                navController.navigate(targetMenuId)
            }
            .setNegativeButton("Keep Editing", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
