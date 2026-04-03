package com.financetracker.ui.expenses

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
import com.financetracker.data.model.Category
import com.financetracker.data.model.Transaction
import com.financetracker.data.model.TransactionType
import com.financetracker.data.repository.MockDataRepository
import com.financetracker.ui.adapter.CategorySelectAdapter
import com.financetracker.utils.DateUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.util.Calendar

class AddExpenseFragment : Fragment() {

    private lateinit var btnBack: ImageView
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_add_expense, container, false)
        bindViews(view)
        setupCategoryPicker()
        setupDatePicker()
        setupSaveButton()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadCategories()
        setDefaultDate()
    }

    private fun bindViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        etAmount = view.findViewById(R.id.etAmount)
        tvAmountError = view.findViewById(R.id.tvAmountError)
        rvCategories = view.findViewById(R.id.rvCategories)
        tvCategoryError = view.findViewById(R.id.tvCategoryError)
        etDate = view.findViewById(R.id.etDate)
        etNote = view.findViewById(R.id.etNote)
        btnSave = view.findViewById(R.id.btnSave)

        btnBack.setOnClickListener { findNavController().navigateUp() }
    }

    // ════════════════════════════════════════════════════════════════
    //  КАТЕГОРІЇ
    // ════════════════════════════════════════════════════════════════

    private fun setupCategoryPicker() {
        categoryAdapter = CategorySelectAdapter { category ->
            selectedCategoryId = category.id
            tvCategoryError.visibility = View.GONE
        }
        rvCategories.apply {
            layoutManager = LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false
            )
            adapter = categoryAdapter
        }
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            val categories = MockDataRepository.getCategoriesByType(TransactionType.EXPENSE)
            categoryAdapter.submitList(categories)
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  ДАТА
    // ════════════════════════════════════════════════════════════════

    private fun setDefaultDate() {
        selectedDateTimestamp = System.currentTimeMillis()
        etDate.setText(DateUtils.formatShortDate(selectedDateTimestamp))
    }

    private fun setupDatePicker() {
        etDate.setOnClickListener { showDatePicker() }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        cal.timeInMillis = selectedDateTimestamp

        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val selected = Calendar.getInstance()
                selected.set(year, month, day, 12, 0, 0)
                selected.set(Calendar.MILLISECOND, 0)
                selectedDateTimestamp = selected.timeInMillis
                etDate.setText(DateUtils.formatShortDate(selectedDateTimestamp))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
            show()
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  ЗБЕРЕЖЕННЯ
    // ════════════════════════════════════════════════════════════════

    private fun setupSaveButton() {
        btnSave.setOnClickListener { validateAndSave() }
    }

    private fun validateAndSave() {
        var isValid = true

        // Валідація суми
        val amountText = etAmount.text.toString().trim()
        val amount = amountText.toDoubleOrNull()

        if (amountText.isEmpty()) {
            tvAmountError.text = getString(R.string.error_amount_empty)
            tvAmountError.visibility = View.VISIBLE
            isValid = false
        } else if (amount == null || amount <= 0) {
            tvAmountError.text = getString(R.string.error_amount_zero)
            tvAmountError.visibility = View.VISIBLE
            isValid = false
        } else {
            tvAmountError.visibility = View.GONE
        }

        // Валідація категорії
        if (selectedCategoryId == -1L) {
            tvCategoryError.text = getString(R.string.error_category_not_selected)
            tvCategoryError.visibility = View.VISIBLE
            isValid = false
        } else {
            tvCategoryError.visibility = View.GONE
        }

        if (!isValid) return

        // Збереження
        val userId = (requireActivity().application as FinanceTrackerApp).getCurrentUserId()
        val note = etNote.text.toString().trim()

        btnSave.isEnabled = false

        lifecycleScope.launch {
            val transaction = Transaction(
                id = 0,
                userId = userId,
                type = TransactionType.EXPENSE,
                amount = amount!!,
                categoryId = selectedCategoryId,
                date = selectedDateTimestamp,
                note = note,
                createdAt = System.currentTimeMillis()
            )

            MockDataRepository.addTransaction(transaction)

            Snackbar.make(requireView(), "Витрату додано", Snackbar.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }
}
