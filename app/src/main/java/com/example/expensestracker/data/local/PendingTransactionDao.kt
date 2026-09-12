package com.example.expensestracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PendingTransactionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPendingTransaction(
        transaction: PendingTransactionEntity
    )

    @Query(
        "SELECT * FROM pending_transactions " +
                "ORDER BY rowid DESC"
    )
    suspend fun getAllPendingTransactions():
            List<PendingTransactionEntity>

    @Query(
        "SELECT * FROM pending_transactions " +
                "ORDER BY rowid DESC"
    )
    fun getAllPendingTransactionsFlow():
            kotlinx.coroutines.flow.Flow<List<PendingTransactionEntity>>

    @Query(
        "SELECT * FROM pending_transactions " +
                "WHERE transactionId = :transactionId " +
                "LIMIT 1"
    )
    suspend fun getPendingTransaction(
        transactionId: String
    ): PendingTransactionEntity?

    @Query(
        "DELETE FROM pending_transactions " +
                "WHERE transactionId = :transactionId"
    )
    suspend fun deletePendingTransaction(
        transactionId: String
    )
}