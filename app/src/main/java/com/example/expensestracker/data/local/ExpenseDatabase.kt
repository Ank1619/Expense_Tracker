package com.example.expensestracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ExpenseEntity::class,
        MerchantCategoryEntity::class,
        PendingTransactionEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class ExpenseDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao

    abstract fun merchantCategoryDao(): MerchantCategoryDao

    abstract fun pendingTransactionDao(): PendingTransactionDao

    companion object {

        @Volatile
        private var INSTANCE: ExpenseDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {

            override fun migrate(
                database: SupportSQLiteDatabase
            ) {

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS merchant_categories (
                        merchant TEXT NOT NULL,
                        category TEXT NOT NULL,
                        PRIMARY KEY(merchant)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {

            override fun migrate(
                database: SupportSQLiteDatabase
            ) {

                database.execSQL(
                    """
                    ALTER TABLE expenses
                    ADD COLUMN transactionId TEXT NOT NULL DEFAULT ''
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {

            override fun migrate(
                database: SupportSQLiteDatabase
            ) {

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS pending_transactions (
                        transactionId TEXT NOT NULL,
                        merchant TEXT NOT NULL,
                        amount REAL NOT NULL,
                        date TEXT NOT NULL,
                        PRIMARY KEY(transactionId)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {

            override fun migrate(
                database: SupportSQLiteDatabase
            ) {

                database.execSQL(
                    """
                    ALTER TABLE expenses
                    ADD COLUMN paymentSource TEXT NOT NULL DEFAULT 'Cash/Other'
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    ALTER TABLE expenses
                    ADD COLUMN transactionType TEXT NOT NULL DEFAULT 'EXPENSE'
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    ALTER TABLE pending_transactions
                    ADD COLUMN paymentSource TEXT NOT NULL DEFAULT 'Cash/Other'
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    ALTER TABLE pending_transactions
                    ADD COLUMN transactionType TEXT NOT NULL DEFAULT 'EXPENSE'
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(
            context: Context
        ): ExpenseDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance =
                    Room.databaseBuilder(
                        context.applicationContext,
                        ExpenseDatabase::class.java,
                        "expense_database"
                    )

                        .addMigrations(
                            MIGRATION_1_2,
                            MIGRATION_2_3,
                            MIGRATION_3_4,
                            MIGRATION_4_5
                        )
                        .build()

                INSTANCE = instance

                instance
            }
        }
    }
}