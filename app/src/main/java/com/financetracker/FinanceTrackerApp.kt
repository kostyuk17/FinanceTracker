package com.financetracker

import android.app.Application
import android.content.SharedPreferences

class FinanceTrackerApp : Application() {

    companion object {
        lateinit var instance: FinanceTrackerApp
            private set

        val prefs: SharedPreferences
            get() = instance.getSharedPreferences("finance_tracker_prefs", MODE_PRIVATE)

        // ── Ключі SharedPreferences ──
        const val PREF_IS_LOGGED_IN = "is_logged_in"
        const val PREF_CURRENT_USER_ID = "current_user_id"
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    // ── Допоміжні методи для сесії ──

    fun saveSession(userId: Long) {
        prefs.edit()
            .putBoolean(PREF_IS_LOGGED_IN, true)
            .putLong(PREF_CURRENT_USER_ID, userId)
            .apply()
    }

    fun clearSession() {
        prefs.edit()
            .putBoolean(PREF_IS_LOGGED_IN, false)
            .putLong(PREF_CURRENT_USER_ID, -1L)
            .apply()
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(PREF_IS_LOGGED_IN, false)
    }

    fun getCurrentUserId(): Long {
        return prefs.getLong(PREF_CURRENT_USER_ID, -1L)
    }
}
