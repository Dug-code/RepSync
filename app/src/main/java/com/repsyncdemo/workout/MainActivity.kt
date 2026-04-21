package com.repsyncdemo.workout

import android.content.Context
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.databinding.ActivityMainBinding
import com.repsyncdemo.workout.viewmodel.NavigationLockViewModel
import com.repsyncdemo.workout.viewmodel.ProfileViewModel
import com.repsyncdemo.workout.viewmodel.WorkoutViewModel
import java.util.*
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val navigationLockViewModel: NavigationLockViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()
    private val workoutViewModel: WorkoutViewModel by viewModels()
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.homeFragment, R.id.workoutListFragment, R.id.feedFragment, R.id.analyticsFragment, R.id.profileFragment)
        )
        
        setupActionBarWithNavController(navController, appBarConfiguration)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        navController.addOnDestinationChangedListener { _, destination, arguments ->
            val userId = arguments?.getString("userId")
            val isOtherUserProfile = destination.id == R.id.profileFragment && userId != null
            
            // Hide action bar for main tabs, but SHOW it for other users' profiles
            if (appBarConfiguration.topLevelDestinations.contains(destination.id) && !isOtherUserProfile) {
                supportActionBar?.hide()
            } else {
                supportActionBar?.show()
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
            }
            // Hide keyboard on any destination change
            hideKeyboard()
        }

        binding.bottomNav.setupWithNavController(navController)
        binding.bottomNav.setOnItemSelectedListener { item ->
            if (navigationLockViewModel.isLocked.value == true) {
                showLockWarning {
                    navigationLockViewModel.setLocked(false)
                    if (item.itemId == R.id.profileFragment) {
                        navController.navigate(R.id.profileFragment, null)
                    } else {
                        navController.navigate(item.itemId)
                    }
                }
                false
            } else {
                if (item.itemId != navController.currentDestination?.id) {
                    hideKeyboard()
                    if (item.itemId == R.id.profileFragment) {
                        navController.navigate(R.id.profileFragment, null)
                    } else {
                        navController.navigate(item.itemId)
                    }
                }
                true
            }
        }

        setupBottomNavSlide()

        navigationLockViewModel.isLocked.observe(this) { isLocked ->
            binding.bottomNav.alpha = if (isLocked) 0.5f else 1.0f
        }

        // Check for weigh-in on startup
        profileViewModel.myProfile.observe(this) { profile ->
            profile?.let { checkWeighInSchedule(it) }
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    private fun checkWeighInSchedule(profile: com.repsyncdemo.workout.data.model.UserProfile) {
        if (profile.weighInFrequency == "never") return

        val now = Calendar.getInstance()
        val lastLog = Calendar.getInstance().apply { timeInMillis = profile.lastWeighInDate }
        
        // Already weighed in today
        if (now.get(Calendar.YEAR) == lastLog.get(Calendar.YEAR) && 
            now.get(Calendar.DAY_OF_YEAR) == lastLog.get(Calendar.DAY_OF_YEAR)) return

        val shouldWeighIn = when (profile.weighInFrequency) {
            "daily" -> true
            "custom" -> {
                val dayOfWeek = now.get(Calendar.DAY_OF_WEEK) // 1 (Sun) to 7 (Sat)
                val ourDay = if (dayOfWeek == Calendar.SUNDAY) 7 else dayOfWeek - 1
                profile.weighInDays.contains(ourDay)
            }
            else -> false
        }

        if (shouldWeighIn) {
            showWeighInReminder()
        }
    }

    private fun showWeighInReminder() {
        MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setTitle("Time to Weigh In!")
            .setMessage("Keep your progress tracking accurate by logging your weight for today.")
            .setCancelable(false)
            .setPositiveButton("Weigh In Now") { _, _ ->
                showWeighInDialog()
            }
            .setNegativeButton("Skip for Today", null)
            .show()
    }

    private fun showWeighInDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_weigh_in, null)
        val etWeight = dialogView.findViewById<EditText>(R.id.etWeight)
        val spinnerFreq = dialogView.findViewById<AutoCompleteTextView>(R.id.spinnerFrequency)
        val layoutCustom = dialogView.findViewById<LinearLayout>(R.id.layoutCustomDays)
        
        val freqOptions = arrayOf("Never", "Every Day", "Select Days")
        spinnerFreq.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, freqOptions))

        profileViewModel.myProfile.value?.let { p ->
            etWeight.setText(p.weightLbs.toString())
            spinnerFreq.setText(when(p.weighInFrequency) {
                "daily" -> "Every Day"
                "custom" -> "Select Days"
                else -> "Never"
            }, false)
            if (p.weighInFrequency == "custom") {
                layoutCustom.visibility = View.VISIBLE
                dialogView.findViewById<MaterialCheckBox>(R.id.cbMon).isChecked = p.weighInDays.contains(1)
                dialogView.findViewById<MaterialCheckBox>(R.id.cbTue).isChecked = p.weighInDays.contains(2)
                dialogView.findViewById<MaterialCheckBox>(R.id.cbWed).isChecked = p.weighInDays.contains(3)
                dialogView.findViewById<MaterialCheckBox>(R.id.cbThu).isChecked = p.weighInDays.contains(4)
                dialogView.findViewById<MaterialCheckBox>(R.id.cbFri).isChecked = p.weighInDays.contains(5)
                dialogView.findViewById<MaterialCheckBox>(R.id.cbSat).isChecked = p.weighInDays.contains(6)
                dialogView.findViewById<MaterialCheckBox>(R.id.cbSun).isChecked = p.weighInDays.contains(7)
            }
        }

        spinnerFreq.setOnItemClickListener { _, _, position, _ ->
            layoutCustom.visibility = if (position == 2) View.VISIBLE else View.GONE
        }

        MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val weight = etWeight.text.toString().toDoubleOrNull() ?: 0.0
                if (weight > 0) {
                    saveWeighInData(weight, spinnerFreq.text.toString(), dialogView)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveWeighInData(weight: Double, freqText: String, view: View) {
        val freq = when(freqText) {
            "Every Day" -> "daily"
            "Select Days" -> "custom"
            else -> "never"
        }
        
        val selectedDays = mutableListOf<Int>()
        if (freq == "custom") {
            if (view.findViewById<MaterialCheckBox>(R.id.cbMon).isChecked) selectedDays.add(1)
            if (view.findViewById<MaterialCheckBox>(R.id.cbTue).isChecked) selectedDays.add(2)
            if (view.findViewById<MaterialCheckBox>(R.id.cbWed).isChecked) selectedDays.add(3)
            if (view.findViewById<MaterialCheckBox>(R.id.cbThu).isChecked) selectedDays.add(4)
            if (view.findViewById<MaterialCheckBox>(R.id.cbFri).isChecked) selectedDays.add(5)
            if (view.findViewById<MaterialCheckBox>(R.id.cbSat).isChecked) selectedDays.add(6)
            if (view.findViewById<MaterialCheckBox>(R.id.cbSun).isChecked) selectedDays.add(7)
        }

        val updates = mapOf(
            "weightLbs" to weight,
            "weighInFrequency" to freq,
            "weighInDays" to selectedDays,
            "lastWeighInDate" to System.currentTimeMillis()
        )
        
        profileViewModel.updateProfileFields(updates)
        Toast.makeText(this, "Weight updated!", Toast.LENGTH_SHORT).show()
    }

    private fun setupBottomNavSlide() {
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null) return false
                val diffX = e2.x - e1.x
                if (abs(diffX) > 100 && abs(velocityX) > 100) {
                    if (navigationLockViewModel.isLocked.value == true) {
                        showLockWarning {
                            navigationLockViewModel.setLocked(false)
                            performFlingNavigation(diffX)
                        }
                    } else {
                        performFlingNavigation(diffX)
                    }
                    return true
                }
                return false
            }
        })
        binding.bottomNav.setOnTouchListener { v, event -> if (gestureDetector.onTouchEvent(event)) true else v.onTouchEvent(event) }
    }

    private fun performFlingNavigation(diffX: Float) {
        val menu = binding.bottomNav.menu
        val currentId = binding.bottomNav.selectedItemId
        var currentIndex = -1
        for (i in 0 until menu.size()) {
            if (menu.getItem(i).itemId == currentId) {
                currentIndex = i
                break
            }
        }
        if (diffX > 0) { 
            if (currentIndex > 0) {
                hideKeyboard()
                binding.bottomNav.selectedItemId = menu.getItem(currentIndex - 1).itemId 
            }
        }
        else { 
            if (currentIndex < menu.size() - 1) {
                hideKeyboard()
                binding.bottomNav.selectedItemId = menu.getItem(currentIndex + 1).itemId 
            }
        }
    }

    private fun showLockWarning(onDiscard: () -> Unit) {
        MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setTitle("Unsaved Changes")
            .setMessage("You have unsaved changes. Are you sure you want to discard them?")
            .setPositiveButton("Discard") { _, _ -> onDiscard() }
            .setNegativeButton("Keep Editing", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        if (navigationLockViewModel.isLocked.value == true) {
            showLockWarning {
                navigationLockViewModel.setLocked(false)
                navController.navigateUp()
            }
            return true
        }
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
