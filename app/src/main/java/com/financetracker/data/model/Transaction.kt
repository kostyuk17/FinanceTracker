package com.financetracker.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Transaction(
    val id: Long,
    val userId: Long,
    val type: TransactionType,
    val amount: Double,
    val categoryId: Long,
    val date: Long,       // timestamp у мілісекундах
    val note: String,
    val createdAt: Long   // timestamp створення запису
) : Parcelable
