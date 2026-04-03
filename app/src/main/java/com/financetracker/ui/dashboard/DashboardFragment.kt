package com.financetracker.ui.dashboard

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.financetracker.FinanceTrackerApp
import com.financetracker.R
import com.financetracker.ui.adapter.TransactionAdapter
import com.financetracker.utils.CurrencyUtils
import com.financetracker.utils.DateUtils
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DashboardFragment : Fragment() {

    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var transactionAdapter: TransactionAdapter

    // Views
    private lateinit var tvGreeting: TextView
    private lateinit var tvTotalBalance: TextView
    private lateinit var tvMonthIncome: TextView
    private lateinit var tvMonthExpenses: TextView
    private lateinit var chartLast7Days: LineChart
    private lateinit var rvRecentTransactions: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)
        bindViews(view)
        setupRecyclerView()
        setupChart()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
        loadData()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun bindViews(view: View) {
        tvGreeting = view.findViewById(R.id.tvGreeting)
        tvTotalBalance = view.findViewById(R.id.tvTotalBalance)
        tvMonthIncome = view.findViewById(R.id.tvMonthIncome)
        tvMonthExpenses = view.findViewById(R.id.tvMonthExpenses)
        chartLast7Days = view.findViewById(R.id.chartLast7Days)
        rvRecentTransactions = view.findViewById(R.id.rvRecentTransactions)
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState)
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter()
        rvRecentTransactions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun loadData() {
        val userId = (requireActivity().application as FinanceTrackerApp).getCurrentUserId()
        viewModel.loadDashboard(userId)
    }

    // ════════════════════════════════════════════════════════════════
    //  OBSERVERS
    // ════════════════════════════════════════════════════════════════

    private fun observeViewModel() {
        viewModel.userName.observe(viewLifecycleOwner) { name ->
            tvGreeting.text = getString(R.string.welcome_message, name)
        }

        viewModel.totalBalance.observe(viewLifecycleOwner) { balance ->
            tvTotalBalance.text = CurrencyUtils.formatAmount(balance)
        }

        viewModel.monthIncome.observe(viewLifecycleOwner) { income ->
            tvMonthIncome.text = CurrencyUtils.formatSignedAmount(income, true)
        }

        viewModel.monthExpenses.observe(viewLifecycleOwner) { expenses ->
            tvMonthExpenses.text = CurrencyUtils.formatSignedAmount(expenses, false)
        }

        viewModel.recentTransactions.observe(viewLifecycleOwner) { transactions ->
            val catMap = viewModel.categoryMap.value ?: emptyMap()
            if (transactions.isEmpty()) {
                rvRecentTransactions.visibility = View.GONE
                layoutEmptyState.visibility = View.VISIBLE
            } else {
                rvRecentTransactions.visibility = View.VISIBLE
                layoutEmptyState.visibility = View.GONE
                transactionAdapter.submitGroupedList(transactions, catMap)
            }
        }

        viewModel.categoryMap.observe(viewLifecycleOwner) { catMap ->
            val transactions = viewModel.recentTransactions.value ?: return@observe
            if (transactions.isNotEmpty()) {
                transactionAdapter.submitGroupedList(transactions, catMap)
            }
        }

        viewModel.last7DaysExpenses.observe(viewLifecycleOwner) { dayData ->
            updateChart(dayData)
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  ГРАФІК (MPAndroidChart — LineChart)
    // ════════════════════════════════════════════════════════════════

    private fun setupChart() {
        chartLast7Days.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)
            setDrawGridBackground(false)
            setViewPortOffsets(40f, 16f, 20f, 40f)

            axisRight.isEnabled = false

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#E1E3E6")
                gridLineWidth = 0.5f
                textColor = Color.parseColor("#9AA0A6")
                textSize = 10f
                axisMinimum = 0f
                setDrawAxisLine(false)
            }

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = Color.parseColor("#9AA0A6")
                textSize = 10f
                granularity = 1f
                setDrawAxisLine(false)
            }
        }
    }

    private fun updateChart(dayData: Map<Long, Double>) {
        val dayFormat = SimpleDateFormat("dd.MM", Locale("uk"))

        // Генеруємо 7 днів (включно з сьогодні)
        val entries = mutableListOf<Entry>()
        val labels = mutableListOf<String>()
        val cal = Calendar.getInstance()

        for (i in 6 downTo 0) {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -i)
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)

            val dayTs = c.timeInMillis
            val amount = dayData[dayTs] ?: 0.0

            entries.add(Entry((6 - i).toFloat(), amount.toFloat()))
            labels.add(dayFormat.format(Date(dayTs)))
        }

        val primaryColor = ContextCompat.getColor(requireContext(), R.color.primary)
        val primaryLightColor = ContextCompat.getColor(requireContext(), R.color.primary_light)

        val dataSet = LineDataSet(entries, "").apply {
            color = primaryColor
            lineWidth = 2.5f
            setDrawCircles(true)
            circleRadius = 4f
            setCircleColor(primaryColor)
            setDrawCircleHole(true)
            circleHoleRadius = 2f
            circleHoleColor = Color.WHITE
            setDrawValues(false)
            setDrawFilled(true)
            fillColor = primaryColor
            fillAlpha = 30
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        chartLast7Days.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index in labels.indices) labels[index] else ""
            }
        }

        chartLast7Days.data = LineData(dataSet)
        chartLast7Days.invalidate()
    }
}
