package com.repsyncdemo.workout.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.databinding.FragmentAnalyticsAdvancedBinding
import com.repsyncdemo.workout.viewmodel.AnalyticsViewModel
import java.text.SimpleDateFormat
import java.util.*

class AnalyticsAdvancedFragment : Fragment() {

    private var _binding: FragmentAnalyticsAdvancedBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AnalyticsViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAnalyticsAdvancedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupChart()
        observeData()
    }

    private fun setupChart() {
        val chart = binding.lineChartPR
        chart.description.isEnabled = false
        chart.setTouchEnabled(true)
        chart.setPinchZoom(true)
        chart.setNoDataText("Select an exercise to track your PR")
        chart.setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        xAxis.setDrawGridLines(false)
        xAxis.valueFormatter = object : ValueFormatter() {
            private val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
            override fun getFormattedValue(value: Float): String {
                return sdf.format(Date(value.toLong()))
            }
        }

        chart.axisLeft.textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        chart.axisRight.isEnabled = false
        chart.legend.textColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
    }

    private fun observeData() {
        viewModel.availableExercises.observe(viewLifecycleOwner) { exercises ->
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, exercises)
            binding.autoCompleteExercise.setAdapter(adapter)
        }

        viewModel.prHistory.observe(viewLifecycleOwner) { history ->
            updateChartData(history)
        }

        binding.autoCompleteExercise.setOnItemClickListener { _, _, position, _ ->
            val exercise = binding.autoCompleteExercise.adapter.getItem(position) as String
            viewModel.selectExerciseForPR(exercise)
        }
    }

    private fun updateChartData(history: List<Pair<Long, Double>>) {
        if (history.isEmpty()) {
            binding.lineChartPR.clear()
            binding.tvEmptyChart.visibility = View.VISIBLE
            return
        }
        binding.tvEmptyChart.visibility = View.GONE

        val entries = history.map { Entry(it.first.toFloat(), it.second.toFloat()) }
        val dataSet = LineDataSet(entries, "Max Weight (lbs)")
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.primary)
        dataSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.primary))
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 4f
        dataSet.setDrawCircleHole(false)
        dataSet.valueTextColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

        binding.lineChartPR.data = LineData(dataSet)
        binding.lineChartPR.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
