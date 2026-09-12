package com.example.expensestracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.expensestracker.data.local.BudgetManager
import com.example.expensestracker.data.local.ExpenseDatabase
import com.example.expensestracker.data.local.ExpenseEntity
import com.example.expensestracker.data.local.PendingTransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionSmsReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID = "transaction_channel"
        private const val CHANNEL_NAME = "Transaction Alerts"
        private const val CHANNEL_DESCRIPTION = "Notifications for automatically detected transactions"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

        for (message in messages) {
            val sender = message.originatingAddress ?: "Unknown"
            val messageBody = message.messageBody ?: ""

            Log.d("TransactionSMS", "Sender: $sender | Message: $messageBody")

            if (!isTransactionMessage(messageBody)) {
                Log.d("TransactionSMS", "Not a transaction SMS")
                continue
            }

            val amount = extractAmount(messageBody) ?: continue
            val merchant = extractMerchant(messageBody) ?: "Bank Transfer"
            val paymentSource = extractPaymentSource(messageBody)
            val transactionType = detectTransactionType(messageBody)

            val transactionId = createTransactionId(
                sender = sender,
                message = messageBody,
                timestamp = message.timestampMillis
            )

            val database = ExpenseDatabase.getDatabase(context)

            CoroutineScope(Dispatchers.IO).launch {
                val alreadyExists = database.expenseDao().transactionExists(transactionId)
                if (alreadyExists) return@launch

                val alreadyPending = database.pendingTransactionDao().getPendingTransaction(transactionId)
                if (alreadyPending != null) return@launch

                if (transactionType == "INCOME") {
                    // Auto-save credit transactions as Income
                    saveExpense(
                        context = context,
                        database = database,
                        merchant = merchant,
                        amount = amount,
                        category = "Income",
                        transactionId = transactionId,
                        paymentSource = paymentSource,
                        transactionType = "INCOME"
                    )
                    return@launch
                }

                val savedCategory = database.merchantCategoryDao().getCategoryForMerchant(merchant.uppercase(Locale.getDefault()))

                if (savedCategory != null) {
                    saveExpense(
                        context = context,
                        database = database,
                        merchant = merchant,
                        amount = amount,
                        category = savedCategory,
                        transactionId = transactionId,
                        paymentSource = paymentSource,
                        transactionType = "EXPENSE"
                    )
                } else {
                    val defaultCategory = detectCategory(merchant)
                    if (defaultCategory != "Other") {
                        saveExpense(
                            context = context,
                            database = database,
                            merchant = merchant,
                            amount = amount,
                            category = defaultCategory,
                            transactionId = transactionId,
                            paymentSource = paymentSource,
                            transactionType = "EXPENSE"
                        )
                    } else {
                        savePendingTransaction(
                            database = database,
                            merchant = merchant,
                            amount = amount,
                            transactionId = transactionId,
                            paymentSource = paymentSource,
                            transactionType = "EXPENSE"
                        )

                        showUnknownMerchantNotification(
                            context = context,
                            merchant = merchant,
                            amount = amount,
                            transactionId = transactionId
                        )
                    }
                }
            }
        }
    }

    private fun createTransactionId(sender: String, message: String, timestamp: Long): String {
        val data = "$sender|$timestamp|$message".trim().lowercase(Locale.getDefault())
        val digest = MessageDigest.getInstance("SHA-256").digest(data.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private suspend fun saveExpense(
        context: Context,
        database: ExpenseDatabase,
        merchant: String,
        amount: Double,
        category: String,
        transactionId: String,
        paymentSource: String = "Cash/Other",
        transactionType: String = "EXPENSE"
    ) {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        val expense = ExpenseEntity(
            title = merchant,
            amount = amount,
            category = category,
            date = currentDate,
            transactionId = transactionId,
            paymentSource = paymentSource,
            transactionType = transactionType
        )

        database.expenseDao().insertExpense(expense)

        if (transactionType == "EXPENSE") {
            val currentMonthKey = SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date())
            val allExpenses = database.expenseDao().getAllExpensesOnce()
            val currentMonthSpent = allExpenses
                .filter { exp ->
                    exp.transactionType == "EXPENSE" && getMonthYearKey(exp.date) == currentMonthKey
                }
                .sumOf { it.amount }

            val budgetManager = BudgetManager(context)
            budgetManager.checkAndNotifyBudgetThreshold(currentMonthSpent)
        }
    }

    private suspend fun savePendingTransaction(
        database: ExpenseDatabase,
        merchant: String,
        amount: Double,
        transactionId: String,
        paymentSource: String = "Cash/Other",
        transactionType: String = "EXPENSE"
    ) {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        val pendingTransaction = PendingTransactionEntity(
            transactionId = transactionId,
            merchant = merchant,
            amount = amount,
            date = currentDate,
            paymentSource = paymentSource,
            transactionType = transactionType
        )

        database.pendingTransactionDao().insertPendingTransaction(pendingTransaction)
    }

    private fun showUnknownMerchantNotification(
        context: Context,
        merchant: String,
        amount: Double,
        transactionId: String
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, UnknownMerchantActivity::class.java).apply {
            putExtra("merchant", merchant)
            putExtra("amount", amount)
            putExtra("transactionId", transactionId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            transactionId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("New merchant detected")
            .setContentText("$merchant • ₹$amount")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Transaction of ₹$amount detected at $merchant. Tap to choose a category."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(transactionId.hashCode(), notification)
        } catch (e: SecurityException) {
            Log.e("TransactionSMS", "Notification permission not granted", e)
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun isTransactionMessage(message: String): Boolean {
        val text = message.lowercase(Locale.getDefault())
        val keywords = listOf(
            "debited", "debit", "paid", "payment", "spent", "purchase", "withdrawn", "sent",
            "credited", "credit", "received", "deposit", "salary"
        )
        return keywords.any { text.contains(it) }
    }

    private fun detectTransactionType(message: String): String {
        val text = message.lowercase(Locale.getDefault())
        val creditKeywords = listOf("credited", "credit", "received", "deposit", "added to", "salary")
        return if (creditKeywords.any { text.contains(it) }) "INCOME" else "EXPENSE"
    }

    private fun extractPaymentSource(message: String): String {
        val text = message.lowercase(Locale.getDefault())
        return when {
            text.contains("upi") || text.contains("vpa") || text.contains("gpay") || text.contains("phonepe") || text.contains("paytm") -> "UPI"
            text.contains("credit card") || text.contains("credit-card") || text.contains("cc card") -> "Credit Card"
            text.contains("debit card") || text.contains("debit-card") || text.contains("dc card") || text.contains("atm") -> "Debit Card"
            text.contains("netbanking") || text.contains("neft") || text.contains("rtgs") || text.contains("imps") || text.contains("transfer") -> "Bank Transfer"
            else -> "Cash/Other"
        }
    }

    private fun extractAmount(message: String): Double? {
        val patterns = listOf(
            Regex("""₹\s*([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE),
            Regex("""rs\.?\s*([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE),
            Regex("""inr\s*([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE),
            Regex("""\b([0-9,]+(?:\.[0-9]{1,2})?)\s*(?:debited|credited|spent|paid|received)""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(message)
            if (match != null) {
                return match.groupValues.getOrNull(1)?.replace(",", "")?.toDoubleOrNull()
            }
        }
        return null
    }

    private fun extractMerchant(message: String): String? {
        val patterns = listOf(
            Regex("""(?:paid\s+to|payment\s+to|sent\s+to|from|by)\s+([A-Za-z0-9&._-]+(?:\s+[A-Za-z0-9&._-]+){0,3})""", RegexOption.IGNORE_CASE),
            Regex("""(?:at|towards)\s+([A-Za-z0-9&._-]+(?:\s+[A-Za-z0-9&._-]+){0,3})""", RegexOption.IGNORE_CASE),
            Regex("""merchant\s*[:\-]\s*([A-Za-z0-9&._-]+(?:\s+[A-Za-z0-9&._-]+){0,3})""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(message)
            if (match != null) {
                var merchant = match.groupValues.getOrNull(1)?.trim()
                if (!merchant.isNullOrEmpty()) {
                    merchant = merchant.replace(Regex("""\s+(on|using|via|from|for|with|through)\s+.*$""", RegexOption.IGNORE_CASE), "").trim()
                    return merchant
                }
            }
        }
        return null
    }

    private fun detectCategory(merchant: String): String {
        val name = merchant.lowercase(Locale.getDefault())
        return when {
            name.contains("swiggy") || name.contains("zomato") || name.contains("dominos") || name.contains("restaurant") -> "Food"
            name.contains("uber") || name.contains("ola") || name.contains("rapido") || name.contains("metro") || name.contains("irctc") -> "Transport"
            name.contains("amazon") || name.contains("flipkart") || name.contains("myntra") || name.contains("ajio") -> "Shopping"
            name.contains("netflix") || name.contains("spotify") || name.contains("youtube") || name.contains("prime") -> "Entertainment"
            name.contains("pharmacy") || name.contains("medical") || name.contains("apollo") -> "Health"
            else -> "Other"
        }
    }

    private fun getMonthYearKey(dateStr: String): String {
        return try {
            val parts = dateStr.split("-")
            if (parts.size == 3) "${parts[1]}-${parts[2]}" else ""
        } catch (e: Exception) {
            ""
        }
    }
}