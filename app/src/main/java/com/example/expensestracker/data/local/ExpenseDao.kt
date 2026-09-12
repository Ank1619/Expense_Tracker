package com.example.expensestracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Insert
    suspend fun insertExpense(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses ORDER BY id DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>
    @Query("SELECT * FROM expenses ORDER BY id DESC")
    suspend fun getAllExpensesOnce(): List<ExpenseEntity>

    @Query(
        "SELECT EXISTS(" +
                "SELECT 1 FROM expenses " +
                "WHERE transactionId = :transactionId" +
                ")"
    )
    suspend fun transactionExists(
        transactionId: String
    ): Boolean

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)
}