package com.repsyncdemo.workout.ui.home

/**
 * File overview: Displays high-level workout analytics, charts, streaks, totals, and volume breakdowns.
 */

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
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
import java.text.NumberFormat
import java.util.*

class AnalyticsSummaryFragment : Fragment() {

    private var _binding: FragmentAnalyticsSummaryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AnalyticsViewModel by activityViewModels()
    private var muscleDistribution: Map<String, Int> = emptyMap()
    private var muscleColors: Map<String, Int> = emptyMap()

    private val chartColors = listOf(
        Color.rgb(227, 30, 36),
        Color.rgb(255, 143, 0),
        Color.rgb(76, 175, 80),
        Color.rgb(3, 169, 244),
        Color.rgb(156, 39, 176),
        Color.rgb(255, 193, 7),
        Color.rgb(0, 188, 212),
        Color.rgb(233, 30, 99)
    )

    // Sets up this screen.
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAnalyticsSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Connects views, clicks, and data.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCharts()
        setupObservers()
        setupListeners()
    }

    // Sets up this section.
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
            centerText = "Set balance"
            setCenterTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            setCenterTextSize(12f)
            setUsePercentValues(false)
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    val entry = e as? PieEntry ?: return
                    val color = muscleColors[entry.label] ?: return
                    showMuscleColorDialog(entry.label, entry.value.toInt(), color)
                }

                override fun onNothingSelected() = Unit
            })
        }

        // Weekly Consistency Bar Chart
        binding.barChartConsistency.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)
            setPinchZoom(false)
            setScaleEnabled(false)
            setExtraOffsets(4f, 8f, 8f, 4f)
            
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

    // Sets up this section.
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

    // Updates data or UI state.
    private fun updatePieChart(distribution: Map<String, Int>) {
        if (distribution.isEmpty()) {
            binding.pieChartMuscle.clear()
            binding.layoutMuscleLegend.removeAllViews()
            binding.tvMuscleFocusHint.text = "Log completed sets to see your muscle balance"
            return
        }

        muscleDistribution = distribution
        muscleColors = distribution.keys.mapIndexed { index, muscle ->
            muscle to chartColors[index % chartColors.size]
        }.toMap()

        val entries = distribution.map { PieEntry(it.value.toFloat(), it.key) }
        val dataSet = PieDataSet(entries, "")
        dataSet.colors = entries.map { muscleColors[it.label] ?: ContextCompat.getColor(requireContext(), R.color.primary) }
        
        dataSet.sliceSpace = 3f
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return value.toInt().toString()
            }
        }

        binding.pieChartMuscle.data = PieData(dataSet)
        binding.pieChartMuscle.invalidate()
        binding.tvMuscleFocusHint.text = "Completed sets by primary muscle group"
        updateMuscleLegend(distribution)
    }

    // Updates data or UI state.
    private fun updateMuscleLegend(distribution: Map<String, Int>) {
        val total = distribution.values.sum().coerceAtLeast(1)
        binding.layoutMuscleLegend.removeAllViews()

        distribution.entries.sortedByDescending { it.value }.forEach { (muscle, count) ->
            val color = muscleColors[muscle] ?: ContextCompat.getColor(requireContext(), R.color.primary)
            val percent = (count * 100) / total
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, dp(8), 0, dp(8))
                isClickable = true
                isFocusable = true
                foreground = android.util.TypedValue().let { typedValue ->
                    requireContext().theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
                    ContextCompat.getDrawable(requireContext(), typedValue.resourceId)
                }
                setOnClickListener { showMuscleColorDialog(muscle, count, color) }
            }

            val swatch = View(requireContext()).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(8).toFloat()
                    setColor(color)
                }
                layoutParams = LinearLayout.LayoutParams(dp(18), dp(18))
            }

            val label = TextView(requireContext()).apply {
                text = muscle
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                textSize = 13f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dp(12)
                }
            }

            val value = TextView(requireContext()).apply {
                text = "$count sets • $percent%"
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                textSize = 12f
            }

            row.addView(swatch)
            row.addView(label)
            row.addView(value)
            binding.layoutMuscleLegend.addView(row)
        }
    }

    // Shows a dialog or popup.
    private fun showMuscleColorDialog(muscle: String, count: Int, color: Int) {
        val total = muscleDistribution.values.sum().coerceAtLeast(1)
        val percent = (count * 100) / total
        val swatch = TextView(requireContext()).apply {
            text = "$muscle\n$count completed sets • $percent% of this range"
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            textSize = 15f
            setPadding(dp(32), dp(28), dp(32), dp(28))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(20).toFloat()
                setColor(Color.argb(36, Color.red(color), Color.green(color), Color.blue(color)))
                setStroke(dp(1), color)
            }
        }

        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setTitle("Color key")
            .setView(swatch)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    // Updates data or UI state.
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

    // Sets up this section.
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
            binding.tvMuscleFocusHint.visibility = if (isVisible) View.GONE else View.VISIBLE
            binding.layoutMuscleLegend.visibility = if (isVisible) View.GONE else View.VISIBLE
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
        binding.cardWorkouts.setOnClickListener { showCalendarDialog("Completed Sessions", true) }
        binding.cardRestDays.setOnClickListener { showCalendarDialog("Rest Day History", false) }

        binding.cardFavWorkout.setOnClickListener {
            viewModel.topWorkouts.value?.let { showTopTenDialog("Top Sessions", it, "sessions") }
        }

        binding.cardFavMuscle.setOnClickListener {
            viewModel.topMuscleGroups.value?.let { showTopTenDialog("Top Muscle Groups", it, "times") }
        }
    }

    // Shows a dialog or popup.
    private fun showTopTenDialog(title: String, items: List<StatItem>, unit: String) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_volume_breakdown, null)
        val rv = dialogView.findViewById<RecyclerView>(R.id.rvVolumeBreakdown)
        
        val adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            // Creates the item row.
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = object : RecyclerView.ViewHolder(
                ItemStatRowBinding.inflate(LayoutInflater.from(parent.context), parent, false).root
            ) {}
            // Shows the item row.
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

    // Shows a dialog or popup.
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

    // Shows a dialog or popup.
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

    // Clears the view binding.
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
