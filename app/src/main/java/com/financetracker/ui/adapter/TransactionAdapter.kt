package com.financetracker.ui.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.financetracker.R
import com.financetracker.data.model.Category
import com.financetracker.data.model.Transaction
import com.financetracker.data.model.TransactionType
import com.financetracker.utils.CurrencyUtils
import com.financetracker.utils.DateUtils

/**
 * Адаптер для списку транзакцій з груповими заголовками по датах.
 * Підтримує два типи елементів: DATE_HEADER та TRANSACTION_ITEM.
 */
class TransactionAdapter(
    private val onItemClick: (Transaction) -> Unit = {},
    private val onItemLongClick: (Transaction) -> Unit = {}
) : ListAdapter<TransactionAdapter.ListItem, RecyclerView.ViewHolder>(DiffCallback()) {

    // ════════════════════════════════════════════════════════════════
    //  SEALED CLASS ДЛЯ ЕЛЕМЕНТІВ СПИСКУ
    // ════════════════════════════════════════════════════════════════

    sealed class ListItem {
        data class DateHeader(val dateTimestamp: Long, val dateText: String) : ListItem()
        data class TransactionItem(
            val transaction: Transaction,
            val category: Category?
        ) : ListItem()
    }

    companion object {
        private const val TYPE_DATE_HEADER = 0
        private const val TYPE_TRANSACTION = 1
    }

    // ════════════════════════════════════════════════════════════════
    //  ПЕРЕТВОРЕННЯ СПИСКУ ТРАНЗАКЦІЙ У ЗГРУПОВАНІ ЕЛЕМЕНТИ
    // ════════════════════════════════════════════════════════════════

    fun submitGroupedList(
        transactions: List<Transaction>,
        categoryMap: Map<Long, Category>
    ) {
        val items = mutableListOf<ListItem>()
        var lastDateKey = ""

        for (txn in transactions.sortedByDescending { it.date }) {
            val dateKey = DateUtils.formatFullDate(txn.date)
            if (dateKey != lastDateKey) {
                items.add(ListItem.DateHeader(txn.date, dateKey))
                lastDateKey = dateKey
            }
            items.add(ListItem.TransactionItem(txn, categoryMap[txn.categoryId]))
        }

        submitList(items)
    }

    // ════════════════════════════════════════════════════════════════
    //  VIEW HOLDERS
    // ════════════════════════════════════════════════════════════════

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ListItem.DateHeader -> TYPE_DATE_HEADER
            is ListItem.TransactionItem -> TYPE_TRANSACTION
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_DATE_HEADER -> {
                val view = inflater.inflate(R.layout.item_date_header, parent, false)
                DateHeaderViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_transaction, parent, false)
                TransactionViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ListItem.DateHeader -> (holder as DateHeaderViewHolder).bind(item)
            is ListItem.TransactionItem -> (holder as TransactionViewHolder).bind(item)
        }
    }

    // ── DateHeader ViewHolder ────────────────────────────────────

    inner class DateHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDateHeader: TextView = itemView.findViewById(R.id.tvDateHeader)

        fun bind(item: ListItem.DateHeader) {
            tvDateHeader.text = item.dateText
        }
    }

    // ── Transaction ViewHolder ───────────────────────────────────

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val flCategoryIcon: FrameLayout = itemView.findViewById(R.id.flCategoryIcon)
        private val ivCategoryIcon: ImageView = itemView.findViewById(R.id.ivCategoryIcon)
        private val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
        private val tvNote: TextView = itemView.findViewById(R.id.tvNote)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)

        fun bind(item: ListItem.TransactionItem) {
            val txn = item.transaction
            val category = item.category

            // Назва категорії
            tvCategoryName.text = category?.name ?: "Без категорії"

            // Нотатка
            tvNote.text = txn.note.ifBlank { category?.name ?: "" }
            tvNote.visibility = if (tvNote.text.isBlank()) View.GONE else View.VISIBLE

            // Дата
            tvDate.text = DateUtils.formatDayMonth(txn.date)

            // Сума з кольором
            val isIncome = txn.type == TransactionType.INCOME
            tvAmount.text = CurrencyUtils.formatSignedAmount(txn.amount, isIncome)
            tvAmount.setTextColor(
                ContextCompat.getColor(
                    itemView.context,
                    if (isIncome) R.color.income_green else R.color.expense_red
                )
            )

            // Іконка категорії
            if (category != null) {
                ivCategoryIcon.setImageResource(category.iconRes)

                // Кольоровий кружок
                val bgDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(parseColorSafe(category.colorHex, 0x20))
                }
                flCategoryIcon.background = bgDrawable

                // Тонування іконки в колір категорії
                try {
                    ivCategoryIcon.setColorFilter(Color.parseColor(category.colorHex))
                } catch (_: Exception) {
                    ivCategoryIcon.clearColorFilter()
                }
            }

            // Кліки
            itemView.setOnClickListener { onItemClick(txn) }
            itemView.setOnLongClickListener {
                onItemLongClick(txn)
                true
            }
        }

        /**
         * Парсить hex-колір та додає прозорість (alpha).
         */
        private fun parseColorSafe(hex: String, alpha: Int): Int {
            return try {
                val color = Color.parseColor(hex)
                Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
            } catch (_: Exception) {
                Color.argb(alpha, 158, 158, 158)
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  DIFF CALLBACK
    // ════════════════════════════════════════════════════════════════

    class DiffCallback : DiffUtil.ItemCallback<ListItem>() {
        override fun areItemsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
            return when {
                oldItem is ListItem.DateHeader && newItem is ListItem.DateHeader ->
                    oldItem.dateText == newItem.dateText

                oldItem is ListItem.TransactionItem && newItem is ListItem.TransactionItem ->
                    oldItem.transaction.id == newItem.transaction.id

                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
            return oldItem == newItem
        }
    }
}
