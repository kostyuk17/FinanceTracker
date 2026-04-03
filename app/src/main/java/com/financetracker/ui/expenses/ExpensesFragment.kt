package com.financetracker.ui.expenses

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

class ExpensesFragment : Fragment() {

    private val viewModel: ExpensesViewModel by viewModels()
    private lateinit var transactionAdapter: TransactionAdapter

    // Views
    private lateinit var tvTotalExpenses: TextView
    private lateinit var chipGroupPeriod: ChipGroup
    private lateinit var chipGroupCategories: ChipGroup
    private lateinit var rvExpenses: RecyclerView
    private lateinit var fabAddExpense: FloatingActionButton
    private lateinit var layoutEmptyState: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_expenses, container, false)
        bindViews(view)
        setupRecyclerView()
        setupPeriodChips()
        setupFab()
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

    private fun bindViews(view: View) {
        tvTotalExpenses = view.findViewById(R.id.tvTotalExpenses)
        chipGroupPeriod = view.findViewById(R.id.chipGroupPeriod)
        chipGroupCategories = view.findViewById(R.id.chipGroupCategories)
        rvExpenses = view.findViewById(R.id.rvExpenses)
        fabAddExpense = view.findViewById(R.id.fabAddExpense)
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState)
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(
            onItemClick = { /* майбутнє — перехід на деталі */ },
            onItemLongClick = { transaction -> showDeleteDialog(transaction) }
        )
        rvExpenses.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionAdapter
        }
    }

    private fun setupFab() {
        fabAddExpense.setOnClickListener {
            findNavController().navigate(R.id.action_expenses_to_addExpense)
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  ФІЛЬТРИ ПЕРІОДУ
    // ════════════════════════════════════════════════════════════════

    private fun setupPeriodChips() {
        chipGroupPeriod.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val filter = when (checkedIds.first()) {
                R.id.chipWeek -> ExpensesViewModel.PeriodFilter.WEEK
                R.id.chipMonth -> ExpensesViewModel.PeriodFilter.MONTH
                R.id.chipYear -> ExpensesViewModel.PeriodFilter.YEAR
                R.id.chipAll -> ExpensesViewModel.PeriodFilter.ALL
                else -> ExpensesViewModel.PeriodFilter.MONTH
            }
            viewModel.setPeriodFilter(filter)
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  ФІЛЬТРИ КАТЕГОРІЙ (динамічно створювані чіпи)
    // ════════════════════════════════════════════════════════════════

    private fun buildCategoryChips() {
        chipGroupCategories.removeAllViews()
        val categories = viewModel.categories.value ?: return

        // Чіп "Усі"
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
                val categoryId = checkedChip?.tag as? Long
                viewModel.setCategoryFilter(categoryId)
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  OBSERVERS
    // ════════════════════════════════════════════════════════════════

    private fun observeViewModel() {
        viewModel.categories.observe(viewLifecycleOwner) {
            buildCategoryChips()
        }

        viewModel.expenses.observe(viewLifecycleOwner) { transactions ->
            val catMap = viewModel.categoryMap.value ?: emptyMap()
            if (transactions.isEmpty()) {
                rvExpenses.visibility = View.GONE
                layoutEmptyState.visibility = View.VISIBLE
            } else {
                rvExpenses.visibility = View.VISIBLE
                layoutEmptyState.visibility = View.GONE
                transactionAdapter.submitGroupedList(transactions, catMap)
            }
        }

        viewModel.totalForPeriod.observe(viewLifecycleOwner) { total ->
            tvTotalExpenses.text = CurrencyUtils.formatSignedAmount(total, false)
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  ВИДАЛЕННЯ ТРАНЗАКЦІЇ
    // ════════════════════════════════════════════════════════════════

    private fun showDeleteDialog(transaction: Transaction) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete))
            .setMessage("Видалити цю витрату?")
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.deleteTransaction(transaction.id)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
}
