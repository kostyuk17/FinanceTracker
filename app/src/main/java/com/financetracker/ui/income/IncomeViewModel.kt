package com.financetracker.ui.income

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

class IncomeViewModel : ViewModel() {

    enum class PeriodFilter { WEEK, MONTH, YEAR, ALL }

    private var currentUserId: Long = -1L
    private var currentPeriod: PeriodFilter = PeriodFilter.MONTH
    private var currentCategoryId: Long? = null

    private val _incomeList = MutableLiveData<List<Transaction>>()
    val incomeList: LiveData<List<Transaction>> = _incomeList

    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories

    private val _categoryMap = MutableLiveData<Map<Long, Category>>()
    val categoryMap: LiveData<Map<Long, Category>> = _categoryMap

    private val _totalForPeriod = MutableLiveData<Double>()
    val totalForPeriod: LiveData<Double> = _totalForPeriod

    fun init(userId: Long) {
        currentUserId = userId
        viewModelScope.launch {
            _categories.value = MockDataRepository.getCategoriesByType(TransactionType.INCOME)
            _categoryMap.value = MockDataRepository.getAllCategories().associateBy { it.id }
            loadIncome()
        }
    }

    fun setPeriodFilter(filter: PeriodFilter) {
        currentPeriod = filter
        loadIncome()
    }

    fun setCategoryFilter(categoryId: Long?) {
        currentCategoryId = categoryId
        loadIncome()
    }

    fun refresh() { loadIncome() }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            MockDataRepository.deleteTransaction(id)
            loadIncome()
        }
    }

    private fun loadIncome() {
        viewModelScope.launch {
            val range = getDateRange()
            var list = if (range != null) {
                MockDataRepository.getTransactionsByTypeAndDateRange(
                    currentUserId, TransactionType.INCOME, range.first, range.second
                )
            } else {
                MockDataRepository.getTransactionsByType(currentUserId, TransactionType.INCOME)
            }
            if (currentCategoryId != null) {
                list = list.filter { it.categoryId == currentCategoryId }
            }
            _incomeList.value = list
            _totalForPeriod.value = list.sumOf { it.amount }
        }
    }

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
