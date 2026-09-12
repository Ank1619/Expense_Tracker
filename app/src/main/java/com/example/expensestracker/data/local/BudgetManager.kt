package com.example.expensestracker.data.local

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.expensestracker.MainActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BudgetManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("expense_tracker_budget_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_MONTHLY_BUDGET = "monthly_budget_amount"
        private const val KEY_LAST_NOTIFIED_MONTH = "last_notified_month"
        private const val KEY_LAST_NOTIFIED_LEVEL = "last_notified_level"

        private const val BUDGET_CHANNEL_ID = "budget_alerts_channel"
        private const val BUDGET_CHANNEL_NAME = "Budget Alert Notifications"
        private const val BUDGET_NOTIFICATION_ID = 2001
    }

    fun getMonthlyBudget(): Double {
        return prefs.getFloat(KEY_MONTHLY_BUDGET, 0f).toDouble()
    }

    fun setMonthlyBudget(amount: Double) {
        prefs.edit().putFloat(KEY_MONTHLY_BUDGET, amount.toFloat()).apply()
    }

    fun isBudgetSet(): Boolean {
        return getMonthlyBudget() > 0.0
    }

    fun checkAndNotifyBudgetThreshold(currentMonthSpent: Double) {
        val targetBudget = getMonthlyBudget()
        if (targetBudget <= 0.0) return

        val currentMonthKey = SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date())
        val savedMonth = prefs.getString(KEY_LAST_NOTIFIED_MONTH, "") ?: ""
        var savedLevel = prefs.getInt(KEY_LAST_NOTIFIED_LEVEL, 0)

        // Reset threshold level for a new month
        if (savedMonth != currentMonthKey) {
            savedLevel = 0
        }

        val usagePercentage = (currentMonthSpent / targetBudget * 100).toInt()

        if (usagePercentage >= 100 && savedLevel < 100) {
            // Trigger 100% Exceeded Notification
            sendBudgetNotification(
                title = "🚨 Budget Exceeded!",
                message = "You have exceeded your monthly budget of ₹${String.format(Locale.getDefault(), "%.0f", targetBudget)}! Total spent: ₹${String.format(Locale.getDefault(), "%.2f", currentMonthSpent)}."
            )
            prefs.edit()
                .putString(KEY_LAST_NOTIFIED_MONTH, currentMonthKey)
                .putInt(KEY_LAST_NOTIFIED_LEVEL, 100)
                .apply()

        } else if (usagePercentage >= 80 && savedLevel < 80) {
            // Trigger 80% Warning Notification
            sendBudgetNotification(
                title = "⚠️ Budget Warning (80% Reached)",
                message = "You have used ${usagePercentage}% of your monthly budget (Spent ₹${String.format(Locale.getDefault(), "%.2f", currentMonthSpent)} of ₹${String.format(Locale.getDefault(), "%.0f", targetBudget)})."
            )
            prefs.edit()
                .putString(KEY_LAST_NOTIFIED_MONTH, currentMonthKey)
                .putInt(KEY_LAST_NOTIFIED_LEVEL, 80)
                .apply()
        }
    }

    private fun sendBudgetNotification(title: String, message: String) {
        createNotificationChannel()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            BUDGET_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, BUDGET_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(BUDGET_NOTIFICATION_ID, notification)
            Log.d("BudgetManager", "Budget notification posted: $title")
        } catch (e: SecurityException) {
            Log.e("BudgetManager", "Permission denied for posting notification", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                BUDGET_CHANNEL_ID,
                BUDGET_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for monthly budget warnings and limits"
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}