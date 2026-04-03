package com.financetracker.ui.expenses

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financetracker.data.model.Category
import com.financetracker.data.model.Transaction
import com.financetracker.data.model.TransactionType
import com.financetracker.data.repository.MockDataRepository
import com.financetracker.utils.DateUtils
import kotlinx.coroutines.launch

class ExpensesViewModel : ViewModel() {

    // ════════════════════════════════════════════════════════════════
    //  ФІЛЬТРИ
    // ════════════════════════════════════════════════════════════════

    enum class PeriodFilter { WEEK, MONTH, YEAR, ALL }

    private var currentUserId: Long = -1L
    private var currentPeriod: PeriodFilter = PeriodFilter.MONTH
    private var currentCategoryId: Long? = null // null = всі категорії

    // ════════════════════════════════════════════════════════════════
    //  LIVE DATA
    // ════════════════════════════════════════════════════════════════

    private val _expenses = MutableLiveData<List<Transaction>>()
    val expenses: LiveData<List<Transaction>> = _expenses

    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories

    private val _categoryMap = MutableLiveData<Map<Long, Category>>()
    val categoryMap: LiveData<Map<Long, Category>> = _categoryMap

    private val _totalForPeriod = MutableLiveData<Double>()
    val totalForPeriod: LiveData<Double> = _totalForPeriod

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // ════════════════════════════════════════════════════════════════
    //  ІНІЦІАЛІЗАЦІЯ
    // ════════════════════════════════════════════════════════════════

    fun init(userId: Long) {
        currentUserId = userId
        viewModelScope.launch {
            val allCategories = MockDataRepository.getCategoriesByType(TransactionType.EXPENSE)
            _categories.value = allCategories
            _categoryMap.value = MockDataRepository.getAllCategories().associateBy { it.id }
            loadExpenses()
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  ЗМІНА ФІЛЬТРІВ
    // ════════════════════════════════════════════════════════════════

    fun setPeriodFilter(filter: PeriodFilter) {
        currentPeriod = filter
        loadExpenses()
    }

    fun setCategoryFilter(categoryId: Long?) {
        currentCategoryId = categoryId
        loadExpenses()
    }

    fun refresh() {
        loadExpenses()
    }

    // ════════════════════════════════════════════════════════════════
    //  ЗАВАНТАЖЕННЯ СПИСКУ
    // ════════════════════════════════════════════════════════════════

    private fun loadExpenses() {
        _isLoading.value = true

        viewModelScope.launch {
            val dateRange = getDateRange()
            var list: List<Transaction>

            if (dateRange != null) {
                list = MockDataRepository.getTransactionsByTypeAndDateRange(
                    currentUserId, TransactionType.EXPENSE, dateRange.first, dateRange.second
                )
            } else {
                list = MockDataRepository.getTransactionsByType(
                    currentUserId, TransactionType.EXPENSE
                )
            }

            // Фільтр по категорії
            if (currentCategoryId != null) {
                list = list.filter { it.categoryId == currentCategoryId }
            }

            _expenses.value = list
            _totalForPeriod.value = list.sumOf { it.amount }
            _isLoading.value = false
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  ВИДАЛЕННЯ
    // ════════════════════════════════════════════════════════════════

    fun deleteTransaction(transactionId: Long) {
        viewModelScope.launch {
            MockDataRepository.deleteTransaction(transactionId)
            loadExpenses()
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  УТИЛІТИ
    // ════════════════════════════════════════════════════════════════

    private fun getDateRange(): Pair<Long, Long>? {
        val now = DateUtils.now()
        return when (currentPeriod) {
            PeriodFilter.WEEK -> Pair(DateUtils.getStartOfWeek(), now)
            PeriodFilter.MONTH -> Pair(DateUtils.getStartOfCurrentMonth(), now)
            PeriodFilter.YEAR -> Pair(DateUtils.getStartOfYear(), now)
            PeriodFilter.ALL -> null
        }
    }
}
