package com.example.expensestracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MerchantCategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMerchantCategory(
        merchantCategory: MerchantCategoryEntity
    )

    @Query(
        "SELECT category FROM merchant_categories " +
                "WHERE merchant = :merchant LIMIT 1"
    )
    suspend fun getCategoryForMerchant(
        merchant: String
    ): String?

    @Query("DELETE FROM merchant_categories WHERE merchant = :merchant")
    suspend fun deleteMerchantCategory(
        merchant: String
    )
}