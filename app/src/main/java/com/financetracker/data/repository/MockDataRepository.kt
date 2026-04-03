package com.financetracker.data.repository

import com.financetracker.R
import com.financetracker.data.model.Category
import com.financetracker.data.model.Transaction
import com.financetracker.data.model.TransactionType
import com.financetracker.data.model.User
import kotlinx.coroutines.delay
import java.security.MessageDigest
import java.util.Calendar

/**
 * Єдине джерело даних додатку.
 * Імітує роботу з базою даних через статичні списки у пам'яті.
 * Усі методи мають штучну затримку (delay) для імітації мережевих запитів.
 */
object MockDataRepository {

    // ════════════════════════════════════════════════════════════════
    //  КОНСТАНТИ
    // ════════════════════════════════════════════════════════════════

    private const val SIMULATED_DELAY = 150L  // мс

    // ════════════════════════════════════════════════════════════════
    //  USERS
    // ════════════════════════════════════════════════════════════════

    private val users = mutableListOf(
        User(
            id = 1L,
            username = "Святослав",
            email = "sviat@gmail.com",
            passwordHash = hashPassword("123456"),
            currency = "UAH",
            createdAt = timestamp(2025, 1, 1)
        ),
        User(
            id = 2L,
            username = "Тестовий",
            email = "test@test.com",
            passwordHash = hashPassword("test123"),
            currency = "UAH",
            createdAt = timestamp(2025, 2, 15)
        )
    )

    private var nextUserId = 3L

    // ════════════════════════════════════════════════════════════════
    //  CATEGORIES
    // ════════════════════════════════════════════════════════════════

    private val categories = mutableListOf(
        // ── Витрати ──────────────────────────────────────────────
        Category(1L, "Їжа", TransactionType.EXPENSE, R.drawable.ic_category_food, "#EF6C00"),
        Category(2L, "Транспорт", TransactionType.EXPENSE, R.drawable.ic_category_transport, "#1565C0"),
        Category(3L, "Розваги", TransactionType.EXPENSE, R.drawable.ic_category_entertainment, "#6A1B9A"),
        Category(4L, "Комунальні", TransactionType.EXPENSE, R.drawable.ic_category_utilities, "#FF6D00"),
        Category(5L, "Здоров'я", TransactionType.EXPENSE, R.drawable.ic_category_health, "#D32F2F"),
        Category(6L, "Одяг", TransactionType.EXPENSE, R.drawable.ic_category_clothing, "#1565C0"),
        Category(7L, "Кафе / Ресторани", TransactionType.EXPENSE, R.drawable.ic_category_cafe, "#EF6C00"),
        Category(8L, "Інше", TransactionType.EXPENSE, R.drawable.ic_category_other, "#5F6368"),

        // ── Доходи ───────────────────────────────────────────────
        Category(9L, "Зарплата", TransactionType.INCOME, R.drawable.ic_category_salary, "#2E7D32"),
        Category(10L, "Фріланс", TransactionType.INCOME, R.drawable.ic_category_freelance, "#1565C0"),
        Category(11L, "Подарунки", TransactionType.INCOME, R.drawable.ic_category_gift, "#6A1B9A"),
        Category(12L, "Бізнес", TransactionType.INCOME, R.drawable.ic_category_business, "#0D47A1"),
        Category(13L, "Інше", TransactionType.INCOME, R.drawable.ic_category_other, "#5F6368")
    )

    private var nextCategoryId = 14L

    // ════════════════════════════════════════════════════════════════
    //  TRANSACTIONS  (мінімум 15 тестових записів)
    // ════════════════════════════════════════════════════════════════

    private val transactions = mutableListOf(
        // ── Витрати ──────────────────────────────────────────────
        Transaction(
            id = 1L, userId = 1L, type = TransactionType.EXPENSE,
            amount = 450.0, categoryId = 1L,
            date = timestamp(2026, 3, 28),
            note = "Продукти в АТБ",
            createdAt = timestamp(2026, 3, 28)
        ),
        Transaction(
            id = 2L, userId = 1L, type = TransactionType.EXPENSE,
            amount = 85.0, categoryId = 2L,
            date = timestamp(2026, 3, 29),
            note = "Метро + маршрутка",
            createdAt = timestamp(2026, 3, 29)
        ),
        Transaction(
            id = 3L, userId = 1L, type = TransactionType.EXPENSE,
            amount = 320.0, categoryId = 7L,
            date = timestamp(2026, 3, 29),
            note = "Обід у піцерії",
            createdAt = timestamp(2026, 3, 29)
        ),
        Transaction(
            id = 4L, userId = 1L, type = TransactionType.EXPENSE,
            amount = 1200.0, categoryId = 4L,
            date = timestamp(2026, 3, 1),
            note = "Комунальні за березень",
            createdAt = timestamp(2026, 3, 1)
        ),
        Transaction(
            id = 5L, userId = 1L, type = TransactionType.EXPENSE,
            amount = 250.0, categoryId = 5L,
            date = timestamp(2026, 3, 15),
            note = "Аптека — ліки",
            createdAt = timestamp(2026, 3, 15)
        ),
        Transaction(
            id = 6L, userId = 1L, type = TransactionType.EXPENSE,
            amount = 2100.0, categoryId = 6L,
            date = timestamp(2026, 3, 20),
            note = "Нова куртка",
            createdAt = timestamp(2026, 3, 20)
        ),
        Transaction(
            id = 7L, userId = 1L, type = TransactionType.EXPENSE,
            amount = 180.0, categoryId = 3L,
            date = timestamp(2026, 3, 30),
            note = "Кіно з друзями",
            createdAt = timestamp(2026, 3, 30)
        ),
        Transaction(
            id = 8L, userId = 1L, type = TransactionType.EXPENSE,
            amount = 560.0, categoryId = 1L,
            date = timestamp(2026, 3, 31),
            note = "Закупка на тиждень",
            createdAt = timestamp(2026, 3, 31)
        ),
        Transaction(
            id = 9L, userId = 1L, type = TransactionType.EXPENSE,
            amount = 75.0, categoryId = 2L,
            date = timestamp(2026, 4, 1),
            note = "Таксі до університету",
            createdAt = timestamp(2026, 4, 1)
        ),
        Transaction(
            id = 10L, userId = 1L, type = TransactionType.EXPENSE,
            amount = 340.0, categoryId = 8L,
            date = timestamp(2026, 4, 2),
            note = "Канцтовари",
            createdAt = timestamp(2026, 4, 2)
        ),

        // ── Доходи ───────────────────────────────────────────────
        Transaction(
            id = 11L, userId = 1L, type = TransactionType.INCOME,
            amount = 25000.0, categoryId = 9L,
            date = timestamp(2026, 3, 5),
            note = "Зарплата за лютий",
            createdAt = timestamp(2026, 3, 5)
        ),
        Transaction(
            id = 12L, userId = 1L, type = TransactionType.INCOME,
            amount = 5000.0, categoryId = 10L,
            date = timestamp(2026, 3, 12),
            note = "Фріланс — верстка сайту",
            createdAt = timestamp(2026, 3, 12)
        ),
        Transaction(
            id = 13L, userId = 1L, type = TransactionType.INCOME,
            amount = 1500.0, categoryId = 11L,
            date = timestamp(2026, 3, 18),
            note = "Подарунок на день народження",
            createdAt = timestamp(2026, 3, 18)
        ),
        Transaction(
            id = 14L, userId = 1L, type = TransactionType.INCOME,
            amount = 25000.0, categoryId = 9L,
            date = timestamp(2026, 4, 1),
            note = "Зарплата за березень",
            createdAt = timestamp(2026, 4, 1)
        ),
        Transaction(
            id = 15L, userId = 1L, type = TransactionType.INCOME,
            amount = 3200.0, categoryId = 10L,
            date = timestamp(2026, 4, 2),
            note = "Фріланс — мобільний дизайн",
            createdAt = timestamp(2026, 4, 2)
        )
    )

    private var nextTransactionId = 16L

    // ════════════════════════════════════════════════════════════════
    //  USER OPERATIONS
    // ════════════════════════════════════════════════════════════════

    suspend fun login(email: String, password: String): User? {
        delay(SIMULATED_DELAY)
        val hash = hashPassword(password)
        return users.find { it.email == email && it.passwordHash == hash }
    }

    suspend fun register(username: String, email: String, password: String): Result<User> {
        delay(SIMULATED_DELAY)
        if (users.any { it.email == email }) {
            return Result.failure(IllegalArgumentException("Email вже зареєстровано"))
        }
        val newUser = User(
            id = nextUserId++,
            username = username,
            email = email,
            passwordHash = hashPassword(password),
            currency = "UAH",
            createdAt = System.currentTimeMillis()
        )
        users.add(newUser)
        return Result.success(newUser)
    }

    suspend fun getUserById(userId: Long): User? {
        delay(SIMULATED_DELAY)
        return users.find { it.id == userId }
    }

    suspend fun updateUser(user: User): Boolean {
        delay(SIMULATED_DELAY)
        val index = users.indexOfFirst { it.id == user.id }
        if (index == -1) return false
        users[index] = user
        return true
    }

    // ════════════════════════════════════════════════════════════════
    //  CATEGORY OPERATIONS
    // ════════════════════════════════════════════════════════════════

    suspend fun getAllCategories(): List<Category> {
        delay(SIMULATED_DELAY)
        return categories.toList()
    }

    suspend fun getCategoriesByType(type: TransactionType): List<Category> {
        delay(SIMULATED_DELAY)
        return categories.filter { it.type == type }
    }

    suspend fun getCategoryById(categoryId: Long): Category? {
        delay(SIMULATED_DELAY)
        return categories.find { it.id == categoryId }
    }

    suspend fun addCategory(category: Category): Category {
        delay(SIMULATED_DELAY)
        val newCategory = category.copy(id = nextCategoryId++)
        categories.add(newCategory)
        return newCategory
    }

    suspend fun updateCategory(category: Category): Boolean {
        delay(SIMULATED_DELAY)
        val index = categories.indexOfFirst { it.id == category.id }
        if (index == -1) return false
        categories[index] = category
        return true
    }

    suspend fun deleteCategory(categoryId: Long): Boolean {
        delay(SIMULATED_DELAY)
        return categories.removeAll { it.id == categoryId }
    }

    suspend fun getTransactionCountByCategory(categoryId: Long): Int {
        delay(SIMULATED_DELAY)
        return transactions.count { it.categoryId == categoryId }
    }

    // ════════════════════════════════════════════════════════════════
    //  TRANSACTION OPERATIONS
    // ════════════════════════════════════════════════════════════════

    suspend fun getAllTransactions(userId: Long): List<Transaction> {
        delay(SIMULATED_DELAY)
        return transactions
            .filter { it.userId == userId }
            .sortedByDescending { it.date }
    }

    suspend fun getTransactionsByType(userId: Long, type: TransactionType): List<Transaction> {
        delay(SIMULATED_DELAY)
        return transactions
            .filter { it.userId == userId && it.type == type }
            .sortedByDescending { it.date }
    }

    suspend fun getTransactionsByDateRange(
        userId: Long,
        startDate: Long,
        endDate: Long
    ): List<Transaction> {
        delay(SIMULATED_DELAY)
        return transactions
            .filter { it.userId == userId && it.date in startDate..endDate }
            .sortedByDescending { it.date }
    }

    suspend fun getTransactionsByTypeAndDateRange(
        userId: Long,
        type: TransactionType,
        startDate: Long,
        endDate: Long
    ): List<Transaction> {
        delay(SIMULATED_DELAY)
        return transactions
            .filter {
                it.userId == userId &&
                        it.type == type &&
                        it.date in startDate..endDate
            }
            .sortedByDescending { it.date }
    }

    suspend fun getTransactionsByCategory(
        userId: Long,
        categoryId: Long
    ): List<Transaction> {
        delay(SIMULATED_DELAY)
        return transactions
            .filter { it.userId == userId && it.categoryId == categoryId }
            .sortedByDescending { it.date }
    }

    suspend fun getRecentTransactions(userId: Long, limit: Int = 5): List<Transaction> {
        delay(SIMULATED_DELAY)
        return transactions
            .filter { it.userId == userId }
            .sortedByDescending { it.date }
            .take(limit)
    }

    suspend fun getTotalByType(userId: Long, type: TransactionType): Double {
        delay(SIMULATED_DELAY)
        return transactions
            .filter { it.userId == userId && it.type == type }
            .sumOf { it.amount }
    }

    suspend fun getTotalByTypeAndDateRange(
        userId: Long,
        type: TransactionType,
        startDate: Long,
        endDate: Long
    ): Double {
        delay(SIMULATED_DELAY)
        return transactions
            .filter {
                it.userId == userId &&
                        it.type == type &&
                        it.date in startDate..endDate
            }
            .sumOf { it.amount }
    }

    suspend fun addTransaction(transaction: Transaction): Transaction {
        delay(SIMULATED_DELAY)
        val newTransaction = transaction.copy(
            id = nextTransactionId++,
            createdAt = System.currentTimeMillis()
        )
        transactions.add(newTransaction)
        return newTransaction
    }

    suspend fun updateTransaction(transaction: Transaction): Boolean {
        delay(SIMULATED_DELAY)
        val index = transactions.indexOfFirst { it.id == transaction.id }
        if (index == -1) return false
        transactions[index] = transaction
        return true
    }

    suspend fun deleteTransaction(transactionId: Long): Boolean {
        delay(SIMULATED_DELAY)
        return transactions.removeAll { it.id == transactionId }
    }

    suspend fun clearAllTransactions(userId: Long) {
        delay(SIMULATED_DELAY)
        transactions.removeAll { it.userId == userId }
    }

    /**
     * Агреговані дані для аналітики — сума за категоріями за період.
     */
    suspend fun getSumByCategoryForPeriod(
        userId: Long,
        type: TransactionType,
        startDate: Long,
        endDate: Long
    ): Map<Long, Double> {
        delay(SIMULATED_DELAY)
        return transactions
            .filter {
                it.userId == userId &&
                        it.type == type &&
                        it.date in startDate..endDate
            }
            .groupBy { it.categoryId }
            .mapValues { (_, txns) -> txns.sumOf { it.amount } }
    }

    /**
     * Агреговані дані — сума по днях за період.
     */
    suspend fun getSumByDayForPeriod(
        userId: Long,
        type: TransactionType,
        startDate: Long,
        endDate: Long
    ): Map<Long, Double> {
        delay(SIMULATED_DELAY)
        val cal = Calendar.getInstance()
        return transactions
            .filter {
                it.userId == userId &&
                        it.type == type &&
                        it.date in startDate..endDate
            }
            .groupBy { txn ->
                cal.timeInMillis = txn.date
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            .mapValues { (_, txns) -> txns.sumOf { it.amount } }
    }

    // ════════════════════════════════════════════════════════════════
    //  УТИЛІТИ
    // ════════════════════════════════════════════════════════════════

    fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Хелпер для створення timestamp із дати (рік, місяць 1-12, день).
     */
    private fun timestamp(year: Int, month: Int, day: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, day, 12, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
