package com.financetracker.ui.expenses

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.financetracker.FinanceTrackerApp
import com.financetracker.R
import com.financetracker.data.model.TransactionType
import com.financetracker.data.repository.MockDataRepository
import com.financetracker.ui.adapter.CategoryStat
import com.financetracker.ui.adapter.CategoryStatAdapter
import com.financetracker.utils.CurrencyUtils
import com.financetracker.utils.DateUtils
import com.financetracker.utils.ExportUtils
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ExpenseAnalyticsFragment : Fragment() {

    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart
    private lateinit var tvTotalSum: TextView
    private lateinit var chipGroupPeriod: ChipGroup
    private lateinit var rvTopCategories: RecyclerView
    private val statAdapter = CategoryStatAdapter()

    private var startDate: Long = 0L
    private var endDate: Long = 0L

    private val chartColors = listOf(
        "#EF6C00", "#1565C0", "#6A1B9A", "#D32F2F", "#2E7D32",
        "#FF6D00", "#0D47A1", "#5F6368", "#1B5E20", "#FF8A80"
    ).map { Color.parseColor(it) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_expense_analytics, container, false)
        pieChart = view.findViewById(R.id.pieChart)
        barChart = view.findViewById(R.id.barChart)
        tvTotalSum = view.findViewById(R.id.tvTotalSum)
        chipGroupPeriod = view.findViewById(R.id.chipGroupPeriod)
        rvTopCategories = view.findViewById(R.id.rvTopCategories)

        view.findViewById<ImageView>(R.id.btnBack).setOnClickListener { findNavController().navigateUp() }

        rvTopCategories.layoutManager = LinearLayoutManager(requireContext())
        rvTopCategories.adapter = statAdapter
        rvTopCategories.isNestedScrollingEnabled = false

        setupPieChart()
        setupBarChart()
        setupPeriodChips()

        view.findViewById<View>(R.id.btnExport).setOnClickListener { exportData() }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setMonthPeriod()
        loadData()
    }

    private fun setMonthPeriod() {
        startDate = DateUtils.getStartOfCurrentMonth()
        endDate = DateUtils.now()
    }

    private fun setupPeriodChips() {
        chipGroupPeriod.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val now = DateUtils.now()
            when (checkedIds.first()) {
                R.id.chipWeek -> { startDate = DateUtils.getStartOfWeek(); endDate = now }
                R.id.chipMonth -> { startDate = DateUtils.getStartOfCurrentMonth(); endDate = now }
                R.id.chipQuarter -> { startDate = DateUtils.getStartOfQuarter(); endDate = now }
                R.id.chipYear -> { startDate = DateUtils.getStartOfYear(); endDate = now }
            }
            loadData()
        }
    }

    private fun loadData() {
        val userId = (requireActivity().application as FinanceTrackerApp).getCurrentUserId()

        lifecycleScope.launch {
            val catMap = MockDataRepository.getAllCategories().associateBy { it.id }
            val sumByCategory = MockDataRepository.getSumByCategoryForPeriod(
                userId, TransactionType.EXPENSE, startDate, endDate
            )
            val totalSum = sumByCategory.values.sum()
            tvTotalSum.text = CurrencyUtils.formatSignedAmount(totalSum, false)

            // PieChart
            val pieEntries = mutableListOf<PieEntry>()
            val stats = mutableListOf<CategoryStat>()

            val sorted = sumByCategory.entries.sortedByDescending { it.value }
            for (entry in sorted) {
                val cat = catMap[entry.key] ?: continue
                val percent = if (totalSum > 0) (entry.value / totalSum * 100).toFloat() else 0f
                pieEntries.add(PieEntry(entry.value.toFloat(), cat.name))
                stats.add(CategoryStat(cat, entry.value, percent))
            }

            updatePieChart(pieEntries)
            statAdapter.submitList(stats)

            // BarChart — daily data
            val dayData = MockDataRepository.getSumByDayForPeriod(
                userId, TransactionType.EXPENSE, startDate, endDate
            )
            updateBarChart(dayData)
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  PIE CHART
    // ════════════════════════════════════════════════════════════════

    private fun setupPieChart() {
        pieChart.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            setDrawEntryLabels(false)
            isDrawHoleEnabled = true
            holeRadius = 55f
            transparentCircleRadius = 60f
            setHoleColor(Color.TRANSPARENT)
            legend.isEnabled = true
            legend.textSize = 11f
            legend.textColor = Color.parseColor("#5F6368")
            setTouchEnabled(true)
            animateY(600)
        }
    }

    private fun updatePieChart(entries: List<PieEntry>) {
        if (entries.isEmpty()) {
            pieChart.clear()
            return
        }
        val dataSet = PieDataSet(entries, "").apply {
            colors = chartColors.take(entries.size)
            sliceSpace = 2f
            valueTextSize = 12f
            valueTextColor = Color.WHITE
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float) =
                    if (value >= 5f) "${value.toInt()}%" else ""
            }
        }
        pieChart.data = PieData(dataSet)
        pieChart.invalidate()
    }

    // ════════════════════════════════════════════════════════════════
    //  BAR CHART
    // ════════════════════════════════════════════════════════════════

    private fun setupBarChart() {
        barChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)
            setDrawGridBackground(false)
            setFitBars(true)
            axisRight.isEnabled = false
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#E1E3E6")
                textColor = Color.parseColor("#9AA0A6")
                textSize = 10f
                axisMinimum = 0f
                setDrawAxisLine(false)
            }
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = Color.parseColor("#9AA0A6")
                textSize = 9f
                granularity = 1f
                setDrawAxisLine(false)
            }
            animateY(500)
        }
    }

    private fun updateBarChart(dayData: Map<Long, Double>) {
        val fmt = SimpleDateFormat("dd.MM", Locale("uk"))
        val sorted = dayData.entries.sortedBy { it.key }
        val entries = sorted.mapIndexed { i, e -> BarEntry(i.toFloat(), e.value.toFloat()) }
        val labels = sorted.map { fmt.format(Date(it.key)) }

        if (entries.isEmpty()) { barChart.clear(); return }

        val dataSet = BarDataSet(entries, "").apply {
            color = Color.parseColor("#D32F2F")
            setDrawValues(false)
        }
        barChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val i = value.toInt()
                return if (i in labels.indices) labels[i] else ""
            }
        }
        barChart.data = BarData(dataSet).apply { barWidth = 0.6f }
        barChart.invalidate()
    }

    // ════════════════════════════════════════════════════════════════
    //  EXPORT
    // ════════════════════════════════════════════════════════════════

    private fun exportData() {
        val userId = (requireActivity().application as FinanceTrackerApp).getCurrentUserId()
        lifecycleScope.launch {
            val transactions = MockDataRepository.getTransactionsByTypeAndDateRange(
                userId, TransactionType.EXPENSE, startDate, endDate
            )
            val catMap = MockDataRepository.getAllCategories().associateBy { it.id }
            val success = ExportUtils.exportToExcel(requireContext(), transactions, catMap, "expenses")
            if (success) {
                Snackbar.make(requireView(), "Файл збережено у Downloads", Snackbar.LENGTH_LONG).show()
            } else {
                Snackbar.make(requireView(), "Помилка експорту", Snackbar.LENGTH_SHORT).show()
            }
        }
    }
}
