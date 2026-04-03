package com.financetracker.ui.income

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.financetracker.FinanceTrackerApp
import com.financetracker.R
import com.financetracker.data.model.Transaction
import com.financetracker.data.model.TransactionType
import com.financetracker.data.repository.MockDataRepository
import com.financetracker.ui.adapter.CategorySelectAdapter
import com.financetracker.utils.DateUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.util.Calendar

class AddIncomeFragment : Fragment() {

    private lateinit var etAmount: EditText
    private lateinit var tvAmountError: TextView
    private lateinit var rvCategories: RecyclerView
    private lateinit var tvCategoryError: TextView
    private lateinit var etDate: EditText
    private lateinit var etNote: EditText
    private lateinit var btnSave: MaterialButton
    private lateinit var categoryAdapter: CategorySelectAdapter

    private var selectedCategoryId: Long = -1L
    private var selectedDateTimestamp: Long = System.currentTimeMillis()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_add_income, container, false)
        etAmount = view.findViewById(R.id.etAmount)
        tvAmountError = view.findViewById(R.id.tvAmountError)
        rvCategories = view.findViewById(R.id.rvCategories)
        tvCategoryError = view.findViewById(R.id.tvCategoryError)
        etDate = view.findViewById(R.id.etDate)
        etNote = view.findViewById(R.id.etNote)
        btnSave = view.findViewById(R.id.btnSave)

        view.findViewById<ImageView>(R.id.btnBack).setOnClickListener { findNavController().navigateUp() }

        categoryAdapter = CategorySelectAdapter { cat ->
            selectedCategoryId = cat.id
            tvCategoryError.visibility = View.GONE
        }
        rvCategories.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvCategories.adapter = categoryAdapter

        etDate.setOnClickListener { showDatePicker() }
        btnSave.setOnClickListener { validateAndSave() }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        selectedDateTimestamp = System.currentTimeMillis()
        etDate.setText(DateUtils.formatShortDate(selectedDateTimestamp))
        lifecycleScope.launch {
            val cats = MockDataRepository.getCategoriesByType(TransactionType.INCOME)
            categoryAdapter.submitList(cats)
        }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDateTimestamp }
        DatePickerDialog(requireContext(), { _, y, m, d ->
            val sel = Calendar.getInstance()
            sel.set(y, m, d, 12, 0, 0)
            sel.set(Calendar.MILLISECOND, 0)
            selectedDateTimestamp = sel.timeInMillis
            etDate.setText(DateUtils.formatShortDate(selectedDateTimestamp))
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).apply {
            datePicker.maxDate = System.currentTimeMillis()
            show()
        }
    }

    private fun validateAndSave() {
        var valid = true
        val amountText = etAmount.text.toString().trim()
        val amount = amountText.toDoubleOrNull()

        if (amountText.isEmpty()) {
            tvAmountError.text = getString(R.string.error_amount_empty)
            tvAmountError.visibility = View.VISIBLE
            valid = false
        } else if (amount == null || amount <= 0) {
            tvAmountError.text = getString(R.string.error_amount_zero)
            tvAmountError.visibility = View.VISIBLE
            valid = false
        } else {
            tvAmountError.visibility = View.GONE
        }

        if (selectedCategoryId == -1L) {
            tvCategoryError.text = getString(R.string.error_category_not_selected)
            tvCategoryError.visibility = View.VISIBLE
            valid = false
        } else {
            tvCategoryError.visibility = View.GONE
        }

        if (!valid) return

        val userId = (requireActivity().application as FinanceTrackerApp).getCurrentUserId()
        btnSave.isEnabled = false

        lifecycleScope.launch {
            MockDataRepository.addTransaction(
                Transaction(0, userId, TransactionType.INCOME, amount!!, selectedCategoryId,
                    selectedDateTimestamp, etNote.text.toString().trim(), System.currentTimeMillis())
            )
            Snackbar.make(requireView(), "Дохід додано", Snackbar.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }
}
