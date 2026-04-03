package com.financetracker.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val id: Long,
    val username: String,
    val email: String,
    val passwordHash: String,
    val currency: String = "UAH",
    val createdAt: Long
) : Parcelable
