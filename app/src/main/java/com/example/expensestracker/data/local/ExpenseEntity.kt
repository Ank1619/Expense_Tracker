package com.example.expensestracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val title: String,

    val amount: Double,

    val category: String,

    val date: String,

    val note: String = "",

    val transactionId: String = "",

    val paymentSource: String = "Cash/Other",

    val transactionType: String = "EXPENSE"
)