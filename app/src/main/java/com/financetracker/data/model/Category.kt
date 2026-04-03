package com.financetracker.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Category(
    val id: Long,
    val name: String,
    val type: TransactionType,
    val iconRes: Int,
    val colorHex: String
) : Parcelable
