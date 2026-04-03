package com.financetracker.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyUtils {

    private val symbols = DecimalFormatSymbols(Locale("uk")).apply {
        groupingSeparator = ' '
        decimalSeparator = '.'
    }

    private val amountFormat = DecimalFormat("#,##0.00", symbols)
    private val amountNoDecimalFormat = DecimalFormat("#,##0", symbols)

    /**
     * Форматує суму: "1 250.00 ₴"
     */
    fun formatAmount(amount: Double): String {
        return "${amountFormat.format(amount)} ₴"
    }

    /**
     * Форматує суму з префіксом: "+1 250.00 ₴" або "-1 250.00 ₴"
     */
    fun formatSignedAmount(amount: Double, isIncome: Boolean): String {
        val sign = if (isIncome) "+" else "−"
        return "$sign${amountFormat.format(amount)} ₴"
    }

    /**
     * Форматує суму без копійок: "1 250 ₴"
     */
    fun formatAmountShort(amount: Double): String {
        return "${amountNoDecimalFormat.format(amount)} ₴"
    }
}
