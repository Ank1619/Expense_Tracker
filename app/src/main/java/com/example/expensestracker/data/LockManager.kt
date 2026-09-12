package com.example.expensestracker.data

import android.content.Context
import android.content.SharedPreferences

class LockManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("expense_tracker_lock_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PIN_LOCK_ENABLED = "pin_lock_enabled"
        private const val KEY_PIN_HASH = "pin_hash"
        private var isUnlockedForSession = false
    }

    fun isLockEnabled(): Boolean {
        return prefs.getBoolean(KEY_PIN_LOCK_ENABLED, false) && getPin().isNotEmpty()
    }

    fun setLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PIN_LOCK_ENABLED, enabled).apply()
    }

    fun setPin(pin: String) {
        prefs.edit().putString(KEY_PIN_HASH, pin).apply()
        setLockEnabled(true)
    }

    fun getPin(): String {
        return prefs.getString(KEY_PIN_HASH, "") ?: ""
    }

    fun verifyPin(inputPin: String): Boolean {
        return getPin() == inputPin
    }

    fun isSessionUnlocked(): Boolean {
        return isUnlockedForSession
    }

    fun setSessionUnlocked(unlocked: Boolean) {
        isUnlockedForSession = unlocked
    }
}