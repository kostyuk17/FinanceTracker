package com.financetracker.ui.income

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

class IncomeAnalyticsFragment : Fragment() {

    private lateinit var pieChart: PieChart
    private lateinit var barChartComparison: BarChart
    private lateinit var tvTotalSum: TextView
    private lateinit var chipGroupPeriod: ChipGroup
    private lateinit var rvTopCategories: RecyclerView
    private val statAdapter = CategoryStatAdapter()

    private var startDate = 0L
    private var endDate = 0L

    private val chartColors = listOf(
        "#2E7D32", "#1565C0", "#6A1B9A", "#0D47A1", "#EF6C00",
        "#FF6D00", "#D32F2F", "#5F6368", "#1B5E20", "#FF8A80"
    ).map { Color.parseColor(it) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_income_analytics, container, false)
        pieChart = view.findViewById(R.id.pieChart)
        barChartComparison = view.findViewById(R.id.barChartComparison)
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
        startDate = DateUtils.getStartOfCurrentMonth()
        endDate = DateUtils.now()
        loadData()
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

            // Pie chart — income by category
            val sumByCategory = MockDataRepository.getSumByCategoryForPeriod(
                userId, TransactionType.INCOME, startDate, endDate
            )
            val totalIncome = sumByCategory.values.sum()
            tvTotalSum.text = CurrencyUtils.formatSignedAmount(totalIncome, true)

            val pieEntries = mutableListOf<PieEntry>()
            val stats = mutableListOf<CategoryStat>()
            for (entry in sumByCategory.entries.sortedByDescending { it.value }) {
                val cat = catMap[entry.key] ?: continue
                val pct = if (totalIncome > 0) (entry.value / totalIncome * 100).toFloat() else 0f
                pieEntries.add(PieEntry(entry.value.toFloat(), cat.name))
                stats.add(CategoryStat(cat, entry.value, pct))
            }
            updatePieChart(pieEntries)
            statAdapter.submitList(stats)

            // Comparison bar chart: income vs expenses
            val totalExpense = MockDataRepository.getTotalByTypeAndDateRange(
                userId, TransactionType.EXPENSE, startDate, endDate
            )
            updateComparisonChart(totalIncome, totalExpense)
        }
    }

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
            animateY(600)
        }
    }

    private fun updatePieChart(entries: List<PieEntry>) {
        if (entries.isEmpty()) { pieChart.clear(); return }
        val ds = PieDataSet(entries, "").apply {
            colors = chartColors.take(entries.size)
            sliceSpace = 2f
            valueTextSize = 12f
            valueTextColor = Color.WHITE
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float) = if (value >= 5f) "${value.toInt()}%" else ""
            }
        }
        pieChart.data = PieData(ds)
        pieChart.invalidate()
    }

    private fun setupBarChart() {
        barChartComparison.apply {
            description.isEnabled = false
            legend.isEnabled = true
            legend.textSize = 11f
            legend.textColor = Color.parseColor("#5F6368")
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
                granularity = 1f
                setDrawAxisLine(false)
            }
            animateY(500)
        }
    }

    private fun updateComparisonChart(income: Double, expense: Double) {
        val incomeEntries = listOf(BarEntry(0f, income.toFloat()))
        val expenseEntries = listOf(BarEntry(1f, expense.toFloat()))

        val incomeDs = BarDataSet(incomeEntries, "Доходи").apply { color = Color.parseColor("#2E7D32") }
        val expenseDs = BarDataSet(expenseEntries, "Витрати").apply { color = Color.parseColor("#D32F2F") }

        barChartComparison.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float) = when (value.toInt()) {
                0 -> "Доходи"; 1 -> "Витрати"; else -> ""
            }
        }
        barChartComparison.data = BarData(incomeDs, expenseDs).apply { barWidth = 0.4f }
        barChartComparison.invalidate()
    }

    private fun exportData() {
        val userId = (requireActivity().application as FinanceTrackerApp).getCurrentUserId()
        lifecycleScope.launch {
            val transactions = MockDataRepository.getTransactionsByTypeAndDateRange(
                userId, TransactionType.INCOME, startDate, endDate
            )
            val catMap = MockDataRepository.getAllCategories().associateBy { it.id }
            val success = ExportUtils.exportToExcel(requireContext(), transactions, catMap, "income")
            Snackbar.make(
                requireView(),
                if (success) "Файл збережено у Downloads" else "Помилка експорту",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }
}
