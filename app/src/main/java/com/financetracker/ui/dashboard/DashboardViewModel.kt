package com.financetracker.ui.dashboard

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

class DashboardViewModel : ViewModel() {

    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    private val _totalBalance = MutableLiveData<Double>()
    val totalBalance: LiveData<Double> = _totalBalance

    private val _monthIncome = MutableLiveData<Double>()
    val monthIncome: LiveData<Double> = _monthIncome

    private val _monthExpenses = MutableLiveData<Double>()
    val monthExpenses: LiveData<Double> = _monthExpenses

    private val _recentTransactions = MutableLiveData<List<Transaction>>()
    val recentTransactions: LiveData<List<Transaction>> = _recentTransactions

    private val _categoryMap = MutableLiveData<Map<Long, Category>>()
    val categoryMap: LiveData<Map<Long, Category>> = _categoryMap

    private val _last7DaysExpenses = MutableLiveData<Map<Long, Double>>()
    val last7DaysExpenses: LiveData<Map<Long, Double>> = _last7DaysExpenses

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadDashboard(userId: Long) {
        _isLoading.value = true

        viewModelScope.launch {
            val user = MockDataRepository.getUserById(userId)
            _userName.value = user?.username ?: ""

            val allCategories = MockDataRepository.getAllCategories()
            _categoryMap.value = allCategories.associateBy { it.id }

            val totalIncome = MockDataRepository.getTotalByType(userId, TransactionType.INCOME)
            val totalExpense = MockDataRepository.getTotalByType(userId, TransactionType.EXPENSE)
            _totalBalance.value = totalIncome - totalExpense

            val monthStart = DateUtils.getStartOfCurrentMonth()
            val monthEnd = DateUtils.getEndOfCurrentMonth()

            _monthIncome.value = MockDataRepository.getTotalByTypeAndDateRange(
                userId, TransactionType.INCOME, monthStart, monthEnd
            )
            _monthExpenses.value = MockDataRepository.getTotalByTypeAndDateRange(
                userId, TransactionType.EXPENSE, monthStart, monthEnd
            )

            _recentTransactions.value = MockDataRepository.getRecentTransactions(userId, 5)

            val sevenDaysAgo = DateUtils.getDaysAgo(6)
            val now = DateUtils.now()
            _last7DaysExpenses.value = MockDataRepository.getSumByDayForPeriod(
                userId, TransactionType.EXPENSE, sevenDaysAgo, now
            )

            _isLoading.value = false
        }
    }
}
