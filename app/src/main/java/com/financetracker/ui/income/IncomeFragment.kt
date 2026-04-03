package com.financetracker.ui.income

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ViewFlipper
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.financetracker.FinanceTrackerApp
import com.financetracker.R
import com.financetracker.data.model.Transaction
import com.financetracker.data.model.TransactionType
import com.financetracker.data.repository.MockDataRepository
import com.financetracker.ui.adapter.CategoryStat
import com.financetracker.ui.adapter.CategoryStatAdapter
import com.financetracker.ui.adapter.TransactionAdapter
import com.financetracker.utils.CurrencyUtils
import com.financetracker.utils.DateUtils
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class IncomeFragment : Fragment() {

    private val viewModel: IncomeViewModel by viewModels()
    private lateinit var transactionAdapter: TransactionAdapter
    private val statAdapter = CategoryStatAdapter()

    private lateinit var tabLayout: TabLayout
    private lateinit var viewFlipper: ViewFlipper
    private lateinit var chipGroupPeriod: ChipGroup
    private lateinit var chipGroupCategories: ChipGroup
    private lateinit var rvIncome: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var chipGroupPeriodA: ChipGroup
    private lateinit var tvAnalyticsTotal: TextView
    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart
    private lateinit var rvTopCategories: RecyclerView

    private val chartColors = listOf("#2E7D32","#1565C0","#6A1B9A","#0D47A1","#EF6C00","#FF6D00","#D32F2F","#5F6368").map { Color.parseColor(it) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_income, container, false)
        tabLayout = view.findViewById(R.id.tabLayout)
        viewFlipper = view.findViewById(R.id.viewFlipper)
        chipGroupPeriod = view.findViewById(R.id.chipGroupPeriod)
        chipGroupCategories = view.findViewById(R.id.chipGroupCategories)
        rvIncome = view.findViewById(R.id.rvExpenses)
        fabAdd = view.findViewById(R.id.fabAddIncome)
        layoutEmpty = view.findViewById(R.id.layoutEmptyState)
        chipGroupPeriodA = view.findViewById(R.id.chipGroupPeriodAnalytics)
        tvAnalyticsTotal = view.findViewById(R.id.tvAnalyticsTotalSum)
        pieChart = view.findViewById(R.id.pieChart)
        barChart = view.findViewById(R.id.barChart)
        rvTopCategories = view.findViewById(R.id.rvTopCategories)

        transactionAdapter = TransactionAdapter(onItemLongClick = { showDeleteDialog(it) })
        rvIncome.layoutManager = LinearLayoutManager(requireContext())
        rvIncome.adapter = transactionAdapter
        rvTopCategories.layoutManager = LinearLayoutManager(requireContext())
        rvTopCategories.adapter = statAdapter
        rvTopCategories.isNestedScrollingEnabled = false

        setupTabs(); setupPeriodChips(); setupAnalyticsPeriodChips(); setupPieChart(); setupBarChart()
        fabAdd.setOnClickListener { findNavController().navigate(R.id.action_income_to_addIncome) }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
        viewModel.init((requireActivity().application as FinanceTrackerApp).getCurrentUserId())
    }

    override fun onResume() { super.onResume(); viewModel.refresh() }

    private fun setupTabs() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewFlipper.displayedChild = tab.position
                fabAdd.visibility = if (tab.position == 0) View.VISIBLE else View.GONE
                if (tab.position == 1) loadAnalytics()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun setupPeriodChips() {
        chipGroupPeriod.setOnCheckedStateChangeListener { _, ids ->
            if (ids.isEmpty()) return@setOnCheckedStateChangeListener
            viewModel.setPeriodFilter(when (ids.first()) {
                R.id.chipWeek -> IncomeViewModel.PeriodFilter.WEEK
                R.id.chipYear -> IncomeViewModel.PeriodFilter.YEAR
                R.id.chipAll -> IncomeViewModel.PeriodFilter.ALL
                else -> IncomeViewModel.PeriodFilter.MONTH
            })
        }
    }

    private fun buildCategoryChips() {
        chipGroupCategories.removeAllViews()
        val cats = viewModel.categories.value ?: return
        val chipAll = Chip(requireContext()).apply { text = getString(R.string.filter_all); isCheckable = true; isChecked = true; setChipBackgroundColorResource(R.color.surface_variant); setTextColor(resources.getColor(R.color.text_secondary, null)); chipCornerRadius = resources.getDimension(R.dimen.radius_xl); id = View.generateViewId() }
        chipGroupCategories.addView(chipAll)
        for (c in cats) { val chip = Chip(requireContext()).apply { text = c.name; isCheckable = true; tag = c.id; setChipBackgroundColorResource(R.color.surface_variant); setTextColor(resources.getColor(R.color.text_secondary, null)); chipCornerRadius = resources.getDimension(R.dimen.radius_xl); id = View.generateViewId() }; chipGroupCategories.addView(chip) }
        chipGroupCategories.isSingleSelection = true
        chipGroupCategories.setOnCheckedStateChangeListener { g, ids -> if (ids.isEmpty() || ids.first() == chipAll.id) { chipAll.isChecked = true; viewModel.setCategoryFilter(null) } else viewModel.setCategoryFilter(g.findViewById<Chip>(ids.first())?.tag as? Long) }
    }

    private fun observeViewModel() {
        viewModel.categories.observe(viewLifecycleOwner) { buildCategoryChips() }
        viewModel.incomeList.observe(viewLifecycleOwner) { txns ->
            val map = viewModel.categoryMap.value ?: emptyMap()
            if (txns.isEmpty()) { rvIncome.visibility = View.GONE; layoutEmpty.visibility = View.VISIBLE }
            else { rvIncome.visibility = View.VISIBLE; layoutEmpty.visibility = View.GONE; transactionAdapter.submitGroupedList(txns, map) }
        }
    }

    private fun showDeleteDialog(txn: Transaction) {
        MaterialAlertDialogBuilder(requireContext()).setTitle(getString(R.string.delete)).setMessage("Видалити цей дохід?")
            .setPositiveButton(getString(R.string.delete)) { _, _ -> viewModel.deleteTransaction(txn.id) }
            .setNegativeButton(getString(R.string.cancel), null).show()
    }

    // ══ ANALYTICS ═════════════════════════════════════════════
    private var aStart = 0L; private var aEnd = 0L

    private fun setupAnalyticsPeriodChips() {
        aStart = DateUtils.getStartOfCurrentMonth(); aEnd = DateUtils.now()
        chipGroupPeriodA.setOnCheckedStateChangeListener { _, ids ->
            if (ids.isEmpty()) return@setOnCheckedStateChangeListener
            val now = DateUtils.now()
            when (ids.first()) { R.id.chipAWeek -> { aStart = DateUtils.getStartOfWeek(); aEnd = now }; R.id.chipAMonth -> { aStart = DateUtils.getStartOfCurrentMonth(); aEnd = now }; R.id.chipAQuarter -> { aStart = DateUtils.getStartOfQuarter(); aEnd = now }; R.id.chipAYear -> { aStart = DateUtils.getStartOfYear(); aEnd = now } }
            loadAnalytics()
        }
    }

    private fun loadAnalytics() {
        val uid = (requireActivity().application as FinanceTrackerApp).getCurrentUserId()
        lifecycleScope.launch {
            val catMap = MockDataRepository.getAllCategories().associateBy { it.id }
            val sumByCat = MockDataRepository.getSumByCategoryForPeriod(uid, TransactionType.INCOME, aStart, aEnd)
            val total = sumByCat.values.sum()
            tvAnalyticsTotal.text = CurrencyUtils.formatSignedAmount(total, true)

            val pieEntries = mutableListOf<PieEntry>(); val stats = mutableListOf<CategoryStat>()
            for (e in sumByCat.entries.sortedByDescending { it.value }) { val cat = catMap[e.key] ?: continue; val pct = if (total > 0) (e.value / total * 100).toFloat() else 0f; pieEntries.add(PieEntry(e.value.toFloat(), cat.name)); stats.add(CategoryStat(cat, e.value, pct)) }
            updatePieChart(pieEntries); statAdapter.submitList(stats)

            // Comparison chart: income vs expenses
            val totalExp = MockDataRepository.getTotalByTypeAndDateRange(uid, TransactionType.EXPENSE, aStart, aEnd)
            updateComparisonChart(total, totalExp)
        }
    }

    private fun setupPieChart() { pieChart.apply { description.isEnabled = false; setUsePercentValues(true); setDrawEntryLabels(false); isDrawHoleEnabled = true; holeRadius = 55f; transparentCircleRadius = 60f; setHoleColor(Color.TRANSPARENT); legend.isEnabled = true; legend.textSize = 11f; legend.textColor = Color.parseColor("#5F6368"); animateY(600) } }
    private fun updatePieChart(entries: List<PieEntry>) { if (entries.isEmpty()) { pieChart.clear(); return }; val ds = PieDataSet(entries, "").apply { colors = chartColors.take(entries.size); sliceSpace = 2f; valueTextSize = 12f; valueTextColor = Color.WHITE; valueFormatter = object : ValueFormatter() { override fun getFormattedValue(v: Float) = if (v >= 5f) "${v.toInt()}%" else "" } }; pieChart.data = PieData(ds); pieChart.invalidate() }

    private fun setupBarChart() { barChart.apply { description.isEnabled = false; legend.isEnabled = true; legend.textSize = 11f; setTouchEnabled(false); setDrawGridBackground(false); setFitBars(true); axisRight.isEnabled = false; axisLeft.apply { setDrawGridLines(true); gridColor = Color.parseColor("#E1E3E6"); textColor = Color.parseColor("#9AA0A6"); axisMinimum = 0f; setDrawAxisLine(false) }; xAxis.apply { position = XAxis.XAxisPosition.BOTTOM; setDrawGridLines(false); textColor = Color.parseColor("#9AA0A6"); granularity = 1f; setDrawAxisLine(false) } } }
    private fun updateComparisonChart(income: Double, expense: Double) {
        val incDs = BarDataSet(listOf(BarEntry(0f, income.toFloat())), "Доходи").apply { color = Color.parseColor("#2E7D32") }
        val expDs = BarDataSet(listOf(BarEntry(1f, expense.toFloat())), "Витрати").apply { color = Color.parseColor("#D32F2F") }
        barChart.xAxis.valueFormatter = object : ValueFormatter() { override fun getFormattedValue(v: Float) = when (v.toInt()) { 0 -> "Доходи"; 1 -> "Витрати"; else -> "" } }
        barChart.data = BarData(incDs, expDs).apply { barWidth = 0.4f }; barChart.invalidate()
    }
}
