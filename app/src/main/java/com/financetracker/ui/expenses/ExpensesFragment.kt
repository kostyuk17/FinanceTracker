package com.financetracker.ui.expenses

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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExpensesFragment : Fragment() {

    private val viewModel: ExpensesViewModel by viewModels()
    private lateinit var transactionAdapter: TransactionAdapter
    private val statAdapter = CategoryStatAdapter()

    private lateinit var tabLayout: TabLayout
    private lateinit var viewFlipper: ViewFlipper
    private lateinit var chipGroupPeriod: ChipGroup
    private lateinit var chipGroupCategories: ChipGroup
    private lateinit var rvExpenses: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var layoutEmpty: LinearLayout

    // Analytics views
    private lateinit var chipGroupPeriodA: ChipGroup
    private lateinit var tvAnalyticsTotal: TextView
    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart
    private lateinit var rvTopCategories: RecyclerView

    private val chartColors = listOf("#EF6C00","#1565C0","#6A1B9A","#D32F2F","#2E7D32","#FF6D00","#0D47A1","#5F6368").map { Color.parseColor(it) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_expenses, container, false)
        tabLayout = view.findViewById(R.id.tabLayout)
        viewFlipper = view.findViewById(R.id.viewFlipper)
        chipGroupPeriod = view.findViewById(R.id.chipGroupPeriod)
        chipGroupCategories = view.findViewById(R.id.chipGroupCategories)
        rvExpenses = view.findViewById(R.id.rvExpenses)
        fabAdd = view.findViewById(R.id.fabAddExpense)
        layoutEmpty = view.findViewById(R.id.layoutEmptyState)
        chipGroupPeriodA = view.findViewById(R.id.chipGroupPeriodAnalytics)
        tvAnalyticsTotal = view.findViewById(R.id.tvAnalyticsTotalSum)
        pieChart = view.findViewById(R.id.pieChart)
        barChart = view.findViewById(R.id.barChart)
        rvTopCategories = view.findViewById(R.id.rvTopCategories)

        transactionAdapter = TransactionAdapter(onItemLongClick = { showDeleteDialog(it) })
        rvExpenses.layoutManager = LinearLayoutManager(requireContext())
        rvExpenses.adapter = transactionAdapter

        rvTopCategories.layoutManager = LinearLayoutManager(requireContext())
        rvTopCategories.adapter = statAdapter
        rvTopCategories.isNestedScrollingEnabled = false

        setupTabs()
        setupPeriodChips()
        setupAnalyticsPeriodChips()
        setupPieChart()
        setupBarChart()

        fabAdd.setOnClickListener { findNavController().navigate(R.id.action_expenses_to_addExpense) }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
        val userId = (requireActivity().application as FinanceTrackerApp).getCurrentUserId()
        viewModel.init(userId)
    }

    override fun onResume() { super.onResume(); viewModel.refresh() }

    // ══ TABS ═══════════════════════════════════════════════════
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

    // ══ LIST PERIOD CHIPS ═════════════════════════════════════
    private fun setupPeriodChips() {
        chipGroupPeriod.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val f = when (checkedIds.first()) {
                R.id.chipWeek -> ExpensesViewModel.PeriodFilter.WEEK
                R.id.chipMonth -> ExpensesViewModel.PeriodFilter.MONTH
                R.id.chipYear -> ExpensesViewModel.PeriodFilter.YEAR
                R.id.chipAll -> ExpensesViewModel.PeriodFilter.ALL
                else -> ExpensesViewModel.PeriodFilter.MONTH
            }
            viewModel.setPeriodFilter(f)
        }
    }

    private fun buildCategoryChips() {
        chipGroupCategories.removeAllViews()
        val categories = viewModel.categories.value ?: return
        val chipAll = Chip(requireContext()).apply {
            text = getString(R.string.filter_all); isCheckable = true; isChecked = true
            setChipBackgroundColorResource(R.color.surface_variant)
            setTextColor(resources.getColor(R.color.text_secondary, null))
            chipCornerRadius = resources.getDimension(R.dimen.radius_xl)
            id = View.generateViewId()
        }
        chipGroupCategories.addView(chipAll)
        for (cat in categories) {
            val chip = Chip(requireContext()).apply {
                text = cat.name; isCheckable = true; tag = cat.id
                setChipBackgroundColorResource(R.color.surface_variant)
                setTextColor(resources.getColor(R.color.text_secondary, null))
                chipCornerRadius = resources.getDimension(R.dimen.radius_xl)
                id = View.generateViewId()
            }
            chipGroupCategories.addView(chip)
        }
        chipGroupCategories.isSingleSelection = true
        chipGroupCategories.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty() || checkedIds.first() == chipAll.id) { chipAll.isChecked = true; viewModel.setCategoryFilter(null) }
            else viewModel.setCategoryFilter(group.findViewById<Chip>(checkedIds.first())?.tag as? Long)
        }
    }

    // ══ OBSERVERS ═════════════════════════════════════════════
    private fun observeViewModel() {
        viewModel.categories.observe(viewLifecycleOwner) { buildCategoryChips() }
        viewModel.expenses.observe(viewLifecycleOwner) { txns ->
            val catMap = viewModel.categoryMap.value ?: emptyMap()
            if (txns.isEmpty()) { rvExpenses.visibility = View.GONE; layoutEmpty.visibility = View.VISIBLE }
            else { rvExpenses.visibility = View.VISIBLE; layoutEmpty.visibility = View.GONE; transactionAdapter.submitGroupedList(txns, catMap) }
        }
    }

    private fun showDeleteDialog(txn: Transaction) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete)).setMessage("Видалити цю витрату?")
            .setPositiveButton(getString(R.string.delete)) { _, _ -> viewModel.deleteTransaction(txn.id) }
            .setNegativeButton(getString(R.string.cancel), null).show()
    }

    // ══ ANALYTICS ═════════════════════════════════════════════
    private var analyticsStart = 0L
    private var analyticsEnd = 0L

    private fun setupAnalyticsPeriodChips() {
        analyticsStart = DateUtils.getStartOfCurrentMonth()
        analyticsEnd = DateUtils.now()
        chipGroupPeriodA.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val now = DateUtils.now()
            when (checkedIds.first()) {
                R.id.chipAWeek -> { analyticsStart = DateUtils.getStartOfWeek(); analyticsEnd = now }
                R.id.chipAMonth -> { analyticsStart = DateUtils.getStartOfCurrentMonth(); analyticsEnd = now }
                R.id.chipAQuarter -> { analyticsStart = DateUtils.getStartOfQuarter(); analyticsEnd = now }
                R.id.chipAYear -> { analyticsStart = DateUtils.getStartOfYear(); analyticsEnd = now }
            }
            loadAnalytics()
        }
    }

    private fun loadAnalytics() {
        val userId = (requireActivity().application as FinanceTrackerApp).getCurrentUserId()
        lifecycleScope.launch {
            val catMap = MockDataRepository.getAllCategories().associateBy { it.id }
            val sumByCat = MockDataRepository.getSumByCategoryForPeriod(userId, TransactionType.EXPENSE, analyticsStart, analyticsEnd)
            val total = sumByCat.values.sum()
            tvAnalyticsTotal.text = CurrencyUtils.formatSignedAmount(total, false)

            val pieEntries = mutableListOf<PieEntry>()
            val stats = mutableListOf<CategoryStat>()
            for (e in sumByCat.entries.sortedByDescending { it.value }) {
                val cat = catMap[e.key] ?: continue
                val pct = if (total > 0) (e.value / total * 100).toFloat() else 0f
                pieEntries.add(PieEntry(e.value.toFloat(), cat.name))
                stats.add(CategoryStat(cat, e.value, pct))
            }
            updatePieChart(pieEntries)
            statAdapter.submitList(stats)

            val dayData = MockDataRepository.getSumByDayForPeriod(userId, TransactionType.EXPENSE, analyticsStart, analyticsEnd)
            updateBarChart(dayData)
        }
    }

    private fun setupPieChart() {
        pieChart.apply { description.isEnabled = false; setUsePercentValues(true); setDrawEntryLabels(false); isDrawHoleEnabled = true; holeRadius = 55f; transparentCircleRadius = 60f; setHoleColor(Color.TRANSPARENT); legend.isEnabled = true; legend.textSize = 11f; legend.textColor = Color.parseColor("#5F6368"); animateY(600) }
    }
    private fun updatePieChart(entries: List<PieEntry>) {
        if (entries.isEmpty()) { pieChart.clear(); return }
        val ds = PieDataSet(entries, "").apply { colors = chartColors.take(entries.size); sliceSpace = 2f; valueTextSize = 12f; valueTextColor = Color.WHITE; valueFormatter = object : ValueFormatter() { override fun getFormattedValue(v: Float) = if (v >= 5f) "${v.toInt()}%" else "" } }
        pieChart.data = PieData(ds); pieChart.invalidate()
    }

    private fun setupBarChart() {
        barChart.apply { description.isEnabled = false; legend.isEnabled = false; setTouchEnabled(false); setDrawGridBackground(false); setFitBars(true); axisRight.isEnabled = false; axisLeft.apply { setDrawGridLines(true); gridColor = Color.parseColor("#E1E3E6"); textColor = Color.parseColor("#9AA0A6"); textSize = 10f; axisMinimum = 0f; setDrawAxisLine(false) }; xAxis.apply { position = XAxis.XAxisPosition.BOTTOM; setDrawGridLines(false); textColor = Color.parseColor("#9AA0A6"); textSize = 9f; granularity = 1f; setDrawAxisLine(false) }; animateY(500) }
    }
    private fun updateBarChart(dayData: Map<Long, Double>) {
        val fmt = SimpleDateFormat("dd", Locale("uk"))
        val sorted = dayData.entries.sortedBy { it.key }
        val entries = sorted.mapIndexed { i, e -> BarEntry(i.toFloat(), e.value.toFloat()) }
        val labels = sorted.map { fmt.format(Date(it.key)) }
        if (entries.isEmpty()) { barChart.clear(); return }
        val ds = BarDataSet(entries, "").apply { color = Color.parseColor("#D32F2F"); setDrawValues(true); valueTextSize = 8f; valueFormatter = object : ValueFormatter() { override fun getFormattedValue(v: Float) = if (v > 0) "${v.toInt()}" else "" } }
        barChart.xAxis.valueFormatter = object : ValueFormatter() { override fun getFormattedValue(v: Float): String { val i = v.toInt(); return if (i in labels.indices) labels[i] else "" } }
        barChart.data = BarData(ds).apply { barWidth = 0.6f }; barChart.invalidate()
    }
}
