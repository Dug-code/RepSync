package com.repsyncdemo.workout.ui.home

/**
 * File overview: Displays advanced PR and body-weight trend charts.
 */

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.data.model.WeightLog
import com.repsyncdemo.workout.databinding.FragmentAnalyticsAdvancedBinding
import com.repsyncdemo.workout.viewmodel.AnalyticsViewModel
import java.text.SimpleDateFormat
import java.util.*

class AnalyticsAdvancedFragment : Fragment() {

    private var _binding: FragmentAnalyticsAdvancedBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AnalyticsViewModel by activityViewModels()

    // Sets up this screen.
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAnalyticsAdvancedBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Connects views, clicks, and data.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupChart(binding.lineChartPR, "Select an exercise to track your PR")
        setupChart(binding.lineChartWeight, "Log your weight to see progress")
        observeData()
    }

    // Sets up this section.
    private fun setupChart(chart: LineChart, emptyText: String) {
        chart.description.isEnabled = false
        chart.setTouchEnabled(true)
        chart.setPinchZoom(true)
        chart.setScaleEnabled(true)
        chart.setDrawGridBackground(false)
        chart.setExtraOffsets(8f, 12f, 12f, 8f)
        chart.setNoDataText(emptyText)
        chart.setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.valueFormatter = object : ValueFormatter() {
            private val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
            override fun getFormattedValue(value: Float): String {
                return sdf.format(Date(value.toLong()))
            }
        }

        chart.axisLeft.apply {
            textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
            gridColor = ContextCompat.getColor(requireContext(), R.color.divider)
            axisLineColor = ContextCompat.getColor(requireContext(), R.color.divider)
            setDrawZeroLine(false)
        }
        chart.axisRight.isEnabled = false
        chart.legend.apply {
            textColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
            textSize = 11f
        }
    }

    // Watches data and updates the UI.
    private fun observeData() {
        viewModel.availableExercises.observe(viewLifecycleOwner) { exercises ->
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, exercises)
            binding.autoCompleteExercise.setAdapter(adapter)
        }

        viewModel.prHistory.observe(viewLifecycleOwner) { history ->
            updatePRChartData(history)
        }

        viewModel.filteredWeightHistory.observe(viewLifecycleOwner) { history ->
            updateWeightChartData(history)
        }

        binding.autoCompleteExercise.setOnItemClickListener { _, _, position, _ ->
            val exercise = binding.autoCompleteExercise.adapter.getItem(position) as String
            viewModel.selectExerciseForPR(exercise)
        }
    }

    // Updates data or UI state.
    private fun updatePRChartData(history: List<Pair<Long, Double>>) {
        if (history.isEmpty()) {
            binding.lineChartPR.clear()
            binding.tvEmptyChart.visibility = View.VISIBLE
            return
        }
        binding.tvEmptyChart.visibility = View.GONE

        val entries = history.map { Entry(it.first.toFloat(), it.second.toFloat()) }
        val dataSet = LineDataSet(entries, "Max Weight (lbs)")
        styleDataSet(dataSet, ContextCompat.getColor(requireContext(), R.color.primary))

        binding.lineChartPR.data = LineData(dataSet).apply {
            setValueTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            setValueTextSize(10f)
        }
        binding.lineChartPR.animateX(600)
        binding.lineChartPR.invalidate()
    }

    // Updates data or UI state.
    private fun updateWeightChartData(history: List<WeightLog>) {
        if (history.isEmpty()) {
            binding.lineChartWeight.clear()
            binding.tvEmptyWeightChart.visibility = View.VISIBLE
            return
        }
        binding.tvEmptyWeightChart.visibility = View.GONE

        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        binding.lineChartWeight.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return history.getOrNull(index)?.let { dateFormat.format(Date(it.date)) }.orEmpty()
            }
        }
        binding.lineChartWeight.xAxis.labelCount = history.size.coerceAtMost(6)

        val entries = history.mapIndexed { index, log -> Entry(index.toFloat(), log.weightLbs.toFloat()) }
        val dataSet = LineDataSet(entries, "Body Weight (lbs)")
        styleDataSet(dataSet, android.graphics.Color.rgb(3, 169, 244))
        dataSet.mode = LineDataSet.Mode.LINEAR

        binding.lineChartWeight.data = LineData(dataSet).apply {
            setValueTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            setValueTextSize(10f)
        }
        binding.lineChartWeight.animateX(600)
        binding.lineChartWeight.invalidate()
    }

    private fun styleDataSet(dataSet: LineDataSet, color: Int) {
        dataSet.color = color
        dataSet.setCircleColor(color)
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 4f
        dataSet.setDrawCircleHole(false)
        dataSet.valueTextColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        dataSet.setDrawFilled(true)
        dataSet.fillColor = color
        dataSet.fillAlpha = 50
    }

    // Clears the view binding.
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
