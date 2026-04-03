package com.financetracker.ui.income

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.financetracker.FinanceTrackerApp
import com.financetracker.R
import com.financetracker.data.model.Transaction
import com.financetracker.ui.adapter.TransactionAdapter
import com.financetracker.utils.CurrencyUtils
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton

class IncomeFragment : Fragment() {

    private val viewModel: IncomeViewModel by viewModels()
    private lateinit var adapter: TransactionAdapter

    private lateinit var tvTotalIncome: TextView
    private lateinit var chipGroupPeriod: ChipGroup
    private lateinit var chipGroupCategories: ChipGroup
    private lateinit var rvIncome: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var layoutEmpty: LinearLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_income, container, false)
        tvTotalIncome = view.findViewById(R.id.tvTotalIncome)
        chipGroupPeriod = view.findViewById(R.id.chipGroupPeriod)
        chipGroupCategories = view.findViewById(R.id.chipGroupCategories)
        rvIncome = view.findViewById(R.id.rvIncome)
        fabAdd = view.findViewById(R.id.fabAddIncome)
        layoutEmpty = view.findViewById(R.id.layoutEmptyState)

        adapter = TransactionAdapter(
            onItemLongClick = { txn -> showDeleteDialog(txn) }
        )
        rvIncome.layoutManager = LinearLayoutManager(requireContext())
        rvIncome.adapter = adapter

        setupPeriodChips()
        fabAdd.setOnClickListener {
            findNavController().navigate(R.id.action_income_to_addIncome)
        }
        view.findViewById<android.widget.ImageView>(R.id.btnAnalytics).setOnClickListener {
            findNavController().navigate(R.id.action_income_to_analytics)
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
        val userId = (requireActivity().application as FinanceTrackerApp).getCurrentUserId()
        viewModel.init(userId)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun setupPeriodChips() {
        chipGroupPeriod.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val filter = when (checkedIds.first()) {
                R.id.chipWeek -> IncomeViewModel.PeriodFilter.WEEK
                R.id.chipMonth -> IncomeViewModel.PeriodFilter.MONTH
                R.id.chipYear -> IncomeViewModel.PeriodFilter.YEAR
                R.id.chipAll -> IncomeViewModel.PeriodFilter.ALL
                else -> IncomeViewModel.PeriodFilter.MONTH
            }
            viewModel.setPeriodFilter(filter)
        }
    }

    private fun buildCategoryChips() {
        chipGroupCategories.removeAllViews()
        val categories = viewModel.categories.value ?: return

        val chipAll = Chip(requireContext()).apply {
            text = getString(R.string.filter_all)
            isCheckable = true
            isChecked = true
            setChipBackgroundColorResource(R.color.surface_variant)
            setTextColor(resources.getColor(R.color.text_secondary, null))
            chipCornerRadius = resources.getDimension(R.dimen.radius_xl)
            id = View.generateViewId()
        }
        chipGroupCategories.addView(chipAll)

        for (cat in categories) {
            val chip = Chip(requireContext()).apply {
                text = cat.name
                isCheckable = true
                tag = cat.id
                setChipBackgroundColorResource(R.color.surface_variant)
                setTextColor(resources.getColor(R.color.text_secondary, null))
                chipCornerRadius = resources.getDimension(R.dimen.radius_xl)
                id = View.generateViewId()
            }
            chipGroupCategories.addView(chip)
        }
        chipGroupCategories.isSingleSelection = true
        chipGroupCategories.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty() || checkedIds.first() == chipAll.id) {
                chipAll.isChecked = true
                viewModel.setCategoryFilter(null)
            } else {
                val checkedChip = group.findViewById<Chip>(checkedIds.first())
                viewModel.setCategoryFilter(checkedChip?.tag as? Long)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.categories.observe(viewLifecycleOwner) { buildCategoryChips() }

        viewModel.incomeList.observe(viewLifecycleOwner) { transactions ->
            val catMap = viewModel.categoryMap.value ?: emptyMap()
            if (transactions.isEmpty()) {
                rvIncome.visibility = View.GONE
                layoutEmpty.visibility = View.VISIBLE
            } else {
                rvIncome.visibility = View.VISIBLE
                layoutEmpty.visibility = View.GONE
                adapter.submitGroupedList(transactions, catMap)
            }
        }

        viewModel.totalForPeriod.observe(viewLifecycleOwner) { total ->
            tvTotalIncome.text = CurrencyUtils.formatSignedAmount(total, true)
        }
    }

    private fun showDeleteDialog(txn: Transaction) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete))
            .setMessage("Видалити цей дохід?")
            .setPositiveButton(getString(R.string.delete)) { _, _ -> viewModel.deleteTransaction(txn.id) }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
}
