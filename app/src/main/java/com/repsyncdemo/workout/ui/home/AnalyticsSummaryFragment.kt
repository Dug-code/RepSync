package com.repsyncdemo.workout.ui.home

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.databinding.FragmentAnalyticsSummaryBinding
import com.repsyncdemo.workout.databinding.ItemStatRowBinding
import com.repsyncdemo.workout.ui.adapter.VolumeBreakdownAdapter
import com.repsyncdemo.workout.viewmodel.AnalyticsViewModel
import com.repsyncdemo.workout.viewmodel.StatItem
import com.repsyncdemo.workout.viewmodel.TimeRange
import com.takusemba.spotlight.OnSpotlightListener
import com.takusemba.spotlight.Spotlight
import com.takusemba.spotlight.Target
import com.takusemba.spotlight.shape.RoundedRectangle
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class AnalyticsSummaryFragment : Fragment() {

    private var _binding: FragmentAnalyticsSummaryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AnalyticsViewModel by activityViewModels()

    private var spotlight: Spotlight? = null

    companion object {
        private const val PREFS_NAME = "repsync_prefs"
        private const val KEY_ANALYTICS_TUTORIAL_COMPLETED = "analytics_tutorial_completed"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAnalyticsSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCharts()
        setupObservers()
        setupListeners()
    }

    private fun setupCharts() {
        // Muscle Focus Pie Chart
        binding.pieChartMuscle.apply {
            description.isEnabled = false
            holeRadius = 45f
            transparentCircleRadius = 50f
            setHoleColor(Color.TRANSPARENT)
            setEntryLabelColor(Color.WHITE)
            setEntryLabelTextSize(12f)
            legend.isEnabled = false
            setNoDataText("No data to show balance")
            setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        }

        // Weekly Consistency Bar Chart
        binding.barChartConsistency.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)
            setPinchZoom(false)
            setScaleEnabled(false)
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
                textSize = 10f
                granularity = 1f
            }
            
            axisLeft.apply {
                setDrawGridLines(true)
                textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
                axisMinimum = 0f
                granularity = 1f
                // Custom formatter to remove decimal points from the Left Y-Axis
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return value.toInt().toString()
                    }
                }
            }
            axisRight.isEnabled = false
            legend.isEnabled = false
            setNoDataText("Start working out to see momentum")
            setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        }
    }

    private fun setupObservers() {
        viewModel.totalVolume.observe(viewLifecycleOwner) { volume ->
            val formatted = NumberFormat.getNumberInstance(Locale.US).format(volume ?: 0.0)
            binding.tvTotalVolume.text = "$formatted lbs"
        }

        viewModel.totalDurationMinutes.observe(viewLifecycleOwner) { totalMinutes ->
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            binding.tvTotalTime.text = "${hours}h ${minutes}m"
        }

        viewModel.averageDurationMinutes.observe(viewLifecycleOwner) { avgMinutes ->
            binding.tvAvgTime.text = "${avgMinutes}m"
        }

        viewModel.workoutsCount.observe(viewLifecycleOwner) { count ->
            binding.tvWorkoutsCount.text = count.toString()
        }

        viewModel.restDaysCount.observe(viewLifecycleOwner) { count ->
            binding.tvRestDaysCount.text = count.toString()
        }

        viewModel.dateRangeText.observe(viewLifecycleOwner) { text ->
            binding.tvDateRangeWorkouts.text = text
            binding.tvDateRangeRestDays.text = text
        }

        viewModel.favoriteWorkout.observe(viewLifecycleOwner) { item ->
            binding.tvFavWorkoutName.text = item?.name ?: "None"
            binding.tvFavWorkoutSessions.text = "${item?.count ?: 0} sessions"
        }

        viewModel.favoriteMuscle.observe(viewLifecycleOwner) { item ->
            binding.tvFavMuscleName.text = item?.name ?: "None"
            binding.tvFavMuscleSessions.text = "${item?.count ?: 0} times"
        }

        viewModel.muscleGroupDistribution.observe(viewLifecycleOwner) { distribution ->
            updatePieChart(distribution)
        }

        viewModel.weeklyConsistency.observe(viewLifecycleOwner) { weeklyData ->
            updateBarChart(weeklyData)
        }
    }

    private fun updatePieChart(distribution: Map<String, Int>) {
        if (distribution.isEmpty()) {
            binding.pieChartMuscle.clear()
            return
        }

        val entries = distribution.map { PieEntry(it.value.toFloat(), it.key) }
        val dataSet = PieDataSet(entries, "")
        
        val colors = mutableListOf<Int>()
        for (c in ColorTemplate.MATERIAL_COLORS) colors.add(c)
        for (c in ColorTemplate.VORDIPLOM_COLORS) colors.add(c)
        dataSet.colors = colors
        
        dataSet.sliceSpace = 3f
        dataSet.valueTextSize = 13f
        dataSet.valueTextColor = Color.WHITE
        // Format values on the pie chart slices to be integers
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return value.toInt().toString()
            }
        }

        binding.pieChartMuscle.data = PieData(dataSet)
        binding.pieChartMuscle.invalidate()
    }

    private fun updateBarChart(weeklyData: List<Pair<String, Int>>) {
        if (weeklyData.isEmpty()) {
            binding.barChartConsistency.clear()
            return
        }

        val entries = weeklyData.mapIndexed { index, pair -> BarEntry(index.toFloat(), pair.second.toFloat()) }
        val labels = weeklyData.map { it.first }

        binding.barChartConsistency.xAxis.valueFormatter = IndexAxisValueFormatter(labels)

        val dataSet = BarDataSet(entries, "")
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.primary)
        dataSet.valueTextColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
        dataSet.valueTextSize = 11f
        // Custom formatter to remove decimal points from the values above the bars
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return value.toInt().toString()
            }
        }

        val barData = BarData(dataSet)
        barData.barWidth = 0.6f
        
        binding.barChartConsistency.data = barData
        binding.barChartConsistency.invalidate()
    }

    private fun setupListeners() {
        binding.toggleTimeRange.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val range = when (checkedId) {
                    R.id.btn7D -> TimeRange.LAST_7
                    R.id.btn30D -> TimeRange.LAST_30
                    else -> TimeRange.LIFETIME
                }
                viewModel.setTimeRange(range)
            }
        }

        // Collapsible Logic for Muscle Focus
        binding.headerMuscleFocus.setOnClickListener {
            val isVisible = binding.pieChartMuscle.visibility == View.VISIBLE
            binding.pieChartMuscle.visibility = if (isVisible) View.GONE else View.VISIBLE
            binding.ivExpandMuscle.rotation = if (isVisible) 0f else 180f
            if (!isVisible) {
                binding.pieChartMuscle.animateY(1000)
            }
        }

        // Collapsible Logic for Weekly Momentum
        binding.headerWeeklyMomentum.setOnClickListener {
            val isVisible = binding.barChartConsistency.visibility == View.VISIBLE
            binding.barChartConsistency.visibility = if (isVisible) View.GONE else View.VISIBLE
            binding.ivExpandMomentum.rotation = if (isVisible) 0f else 180f
            if (!isVisible) {
                binding.barChartConsistency.animateY(1000)
            }
        }

        binding.cardTotalVolume.setOnClickListener { showVolumeBreakdownDialog() }
        binding.cardWorkouts.setOnClickListener { showCalendarDialog("Workout History", true) }
        binding.cardRestDays.setOnClickListener { showCalendarDialog("Rest Day History", false) }

        binding.cardFavWorkout.setOnClickListener {
            viewModel.topWorkouts.value?.let { showTopTenDialog("Top Workouts", it, "sessions") }
        }

        binding.cardFavMuscle.setOnClickListener {
            viewModel.topMuscleGroups.value?.let { showTopTenDialog("Top Muscle Groups", it, "times") }
        }
    }

    private fun showTopTenDialog(title: String, items: List<StatItem>, unit: String) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_volume_breakdown, null)
        val rv = dialogView.findViewById<RecyclerView>(R.id.rvVolumeBreakdown)
        
        val adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = object : RecyclerView.ViewHolder(
                ItemStatRowBinding.inflate(LayoutInflater.from(parent.context), parent, false).root
            ) {}
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val item = items[position]
                val b = ItemStatRowBinding.bind(holder.itemView)
                b.tvStatName.text = item.name
                b.tvStatCount.text = "${item.count} $unit"
            }
            override fun getItemCount() = items.size
        }

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showVolumeBreakdownDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_volume_breakdown, null)
        val rvBreakdown = dialogView.findViewById<RecyclerView>(R.id.rvVolumeBreakdown)
        val adapter = VolumeBreakdownAdapter()
        rvBreakdown.layoutManager = LinearLayoutManager(requireContext())
        rvBreakdown.adapter = adapter
        viewModel.volumeBreakdown.observe(viewLifecycleOwner) { adapter.submitList(it) }

        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setTitle("Exercise Volume Breakdown")
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showCalendarDialog(title: String, isWorkouts: Boolean) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_analytics_calendar, null)
        val calendarView = dialogView.findViewById<MaterialCalendarView>(R.id.calendarView)
        calendarView.setHeaderTextAppearance(R.style.CalendarHeaderStyle)
        calendarView.setDateTextAppearance(R.style.CalendarDateStyle)
        calendarView.setWeekDayTextAppearance(R.style.CalendarWeekStyle)
        calendarView.selectionMode = MaterialCalendarView.SELECTION_MODE_NONE

        if (isWorkouts) {
            viewModel.filteredWorkouts.value?.let { logs ->
                val primaryColor = ContextCompat.getColor(requireContext(), R.color.primary)
                logs.map { log ->
                    val cal = Calendar.getInstance().apply { timeInMillis = log.completedAt }
                    CalendarDay.from(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
                }.distinct().forEach { day ->
                    calendarView.addDecorator(WorkoutCountDecorator(primaryColor, day))
                }
            }
        } else {
            viewModel.filteredRestDays.value?.let { days ->
                val blueColor = android.graphics.Color.parseColor("#81D4FA")
                days.map { day ->
                    val cal = Calendar.getInstance().apply { timeInMillis = day.date }
                    CalendarDay.from(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
                }.distinct().forEach { date ->
                    calendarView.addDecorator(WorkoutCountDecorator(blueColor, date))
                }
            }
        }

        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .show()
    }

    /** ---------------- Tutorial Code Begins ----------------- **/
    internal fun checkTutorial() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        //set to false for testing
        val isCompleted = prefs.getBoolean(KEY_ANALYTICS_TUTORIAL_COMPLETED, false)
        if (!isCompleted) {
            binding.root.post {
                showTutorial()
            }
        }
    }

    private fun showTutorial() {
        val targets = ArrayList<com.takusemba.spotlight.Target>()

        //dateRange target
        targets.add(
            Target.Builder()
                .setAnchor(binding.toggleTimeRange)
                .setShape(RoundedRectangle(binding.toggleTimeRange.height.toFloat(), binding.toggleTimeRange.width.toFloat(), 64f))
                .setOverlay(createOverlay("Date Range", "Choose the date range you want to view"))
                .build()
        )

        targets.add(
            Target.Builder()
                .setAnchor(binding.cardTotalVolume)
                .setShape(RoundedRectangle(binding.cardTotalVolume.height.toFloat(), binding.cardTotalVolume.width.toFloat(), 16f))
                .setOverlay(createOverlay("Total Volume", "check out the total amount of lbs you've logged"))
                .build()
        )

        targets.add(
            Target.Builder()
                .setAnchor(binding.cardMuscleFocus)
                .setShape(RoundedRectangle(binding.cardMuscleFocus.height.toFloat(), binding.cardMuscleFocus.width.toFloat(), 16f))
                .setOverlay(createOverlay("Muscle Focus", "see what muscle groups your workout focus"))
                .build()
        )

        targets.add(
            Target.Builder()
                .setAnchor(binding.cardWeeklyMomentum)
                .setShape(RoundedRectangle(binding.cardWeeklyMomentum.height.toFloat(), binding.cardWeeklyMomentum.width.toFloat(), 16f))
                .setOverlay(createOverlay("Weekly Momentum", "see what muscle groups your workout focus"))
                .build()
        )

        targets.add(
            Target.Builder()
                .setAnchor(binding.cardTotalTime)
                .setShape(RoundedRectangle(binding.cardTotalTime.height.toFloat(), binding.cardTotalTime.width.toFloat(), 16f))
                .setOverlay(createOverlay("Total Time", "see total time logged doing your workouts"))
                .build()
        )

        targets.add(
            Target.Builder()
                .setAnchor(binding.cardAvgTime)
                .setShape(RoundedRectangle(binding.cardAvgTime.height.toFloat(), binding.cardAvgTime.width.toFloat(), 16f))
                .setOverlay(createOverlay("Average Workout", "see your average workout time"))
                .build()
        )

        targets.add(
            Target.Builder()
                .setAnchor(binding.cardFavWorkout)
                .setShape(RoundedRectangle(binding.cardFavWorkout.height.toFloat(), binding.cardFavWorkout.width.toFloat(), 16f))
                .setOverlay(createOverlay("Favorite Workout", "see your favorite workout "))
                .build()
        )

        targets.add(
            Target.Builder()
                .setAnchor(binding.cardFavMuscle)
                .setShape(RoundedRectangle(binding.cardFavMuscle.height.toFloat(), binding.cardFavMuscle.width.toFloat(), 16f))
                .setOverlay(createOverlay("Favorite Muscle", "see your favorite muscle group to workout "))
                .build()
        )

        targets.add(
            Target.Builder()
                .setAnchor(binding.cardWorkouts)
                .setShape(RoundedRectangle(binding.cardWorkouts.height.toFloat(), binding.cardWorkouts.width.toFloat(), 16f))
                .setOverlay(createOverlay("Workouts", "see your total number of workouts and workout history"))
                .build()
        )

        targets.add(
            Target.Builder()
                .setAnchor(binding.cardRestDays)
                .setShape(RoundedRectangle(binding.cardRestDays.height.toFloat(), binding.cardRestDays.width.toFloat(), 16f))
                .setOverlay(finishCreateOverlay("Rest Days", "see your total number of rest days and rest day history"))
                .build()
        )


        spotlight = Spotlight.Builder(requireActivity())
            .setContainer(binding.root as android.view.ViewGroup)
            .setTargets(targets)
            .setBackgroundColorRes(R.color.spotlight_background)
            .setDuration(0L)
            .setAnimation(DecelerateInterpolator(1f))
            .setOnSpotlightListener(object : OnSpotlightListener {
                override fun onStarted() {}
                override fun onEnded() {
                    markTutorialCompleted()
                }
            })
            .build()

        spotlight?.start()
    }

    private fun createOverlay(title: String, description: String): View {
        val overlay = layoutInflater.inflate(R.layout.layout_spotlight_overlay, binding.root, false)

        val containerInfo = overlay.findViewById<LinearLayout>(R.id.containerInfo)
        val params = containerInfo.layoutParams as ViewGroup.MarginLayoutParams

        params.bottomMargin = (-50 * resources.displayMetrics.density).toInt()
        containerInfo.layoutParams = params

        overlay.findViewById<TextView>(R.id.tvTitle).text = title
        overlay.findViewById<TextView>(R.id.tvDescription).text = description

        overlay.findViewById<Button>(R.id.btnNext).setOnClickListener {
            spotlight?.next()
        }

        overlay.findViewById<Button>(R.id.btnSkip).setOnClickListener {
            spotlight?.finish()
            //Marks Whole Analytics tutorial complete
            (parentFragment as? AnalyticsFragment)?.finishSpotlight()
            markTutorialCompleted()
        }

        return overlay
    }

    /** Handles the last overlay in the tutorial to finish the tutorial on AnalyticsFragment**/
    private fun finishCreateOverlay(title: String, description: String): View {
        val overlay = layoutInflater.inflate(R.layout.layout_spotlight_overlay, binding.root, false)
        overlay.findViewById<TextView>(R.id.tvTitle).text = title
        overlay.findViewById<TextView>(R.id.tvDescription).text = description
        overlay.findViewById<TextView>(R.id.btnNext).text = "Finish"

        overlay.findViewById<Button>(R.id.btnNext).setOnClickListener {
            spotlight?.finish()

            (parentFragment as? AnalyticsFragment)?.nextSpotlight()
        }

        overlay.findViewById<Button>(R.id.btnSkip).setOnClickListener {
            spotlight?.finish()
            (parentFragment as? AnalyticsFragment)?.finishSpotlight()
            markTutorialCompleted()
        }

        return overlay
    }

    private fun markTutorialCompleted() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_ANALYTICS_TUTORIAL_COMPLETED, true) }
    }

    /** ---------------- Tutorial Code Ends ----------------- **/



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
