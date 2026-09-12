package com.example.expensestracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_transactions")
data class PendingTransactionEntity(

    @PrimaryKey
    val transactionId: String,

    val merchant: String,

    val amount: Double,

    val date: String,

    val paymentSource: String = "Cash/Other",

    val transactionType: String = "EXPENSE"
)