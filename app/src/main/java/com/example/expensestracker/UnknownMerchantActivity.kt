package com.example.expensestracker

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.expensestracker.data.local.BudgetManager
import com.example.expensestracker.data.local.ExpenseDatabase
import com.example.expensestracker.data.local.ExpenseEntity
import com.example.expensestracker.data.local.MerchantCategoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.android.material.button.MaterialButton

class UnknownMerchantActivity : AppCompatActivity() {

    private lateinit var database: ExpenseDatabase

    private var merchant: String = ""
    private var amount: Double = 0.0
    private var transactionId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_unknown_merchant
        )

        database = ExpenseDatabase.getDatabase(this)

        val btnBack = findViewById<ImageView>(R.id.btnBackUnknown)
        btnBack.setOnClickListener {
            finish()
        }

        transactionId =
            intent.getStringExtra(
                "transactionId"
            ) ?: ""

        if (transactionId.isEmpty()) {

            Toast.makeText(
                this,
                "Transaction not found",
                Toast.LENGTH_SHORT
            ).show()

            finish()

            return
        }

        val tvMerchantName =
            findViewById<TextView>(
                R.id.tvMerchantName
            )

        val tvTransactionAmount =
            findViewById<TextView>(
                R.id.tvTransactionAmount
            )

        val etMerchantCategory =
            findViewById<AutoCompleteTextView>(
                R.id.etMerchantCategory
            )

        val btnSaveCategory =
            findViewById<MaterialButton>(
                R.id.btnSaveCategory
            )

        val btnSkip =
            findViewById<MaterialButton>(
                R.id.btnSkip
            )

        val categories =
            resources.getStringArray(
                R.array.expense_categories
            )

        val categoryAdapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                categories
            )

        etMerchantCategory.setAdapter(
            categoryAdapter
        )

        lifecycleScope.launch(
            Dispatchers.IO
        ) {

            val pendingTransaction =
                database
                    .pendingTransactionDao()
                    .getPendingTransaction(
                        transactionId
                    )

            if (pendingTransaction == null) {

                runOnUiThread {

                    Toast.makeText(
                        this@UnknownMerchantActivity,
                        "Pending transaction not found",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                }

                return@launch
            }

            merchant =
                pendingTransaction.merchant

            amount =
                pendingTransaction.amount

            runOnUiThread {

                tvMerchantName.text =
                    merchant

                tvTransactionAmount.text =
                    "₹${String.format(
                        Locale.getDefault(),
                        "%.2f",
                        amount
                    )}"
            }
        }

        btnSaveCategory.setOnClickListener {

            val selectedCategory =
                etMerchantCategory.text
                    .toString()
                    .trim()

            if (
                selectedCategory.isEmpty()
            ) {

                etMerchantCategory.error =
                    "Select a category"

                return@setOnClickListener
            }

            lifecycleScope.launch(
                Dispatchers.IO
            ) {

                // Save merchant-category mapping
                database
                    .merchantCategoryDao()
                    .saveMerchantCategory(
                        MerchantCategoryEntity(
                            merchant =
                                merchant.uppercase(),
                            category =
                                selectedCategory
                        )
                    )

                // Save pending transaction as expense
                saveExpense(
                    category =
                        selectedCategory
                )

                // Remove transaction from pending table
                deletePendingTransaction()

                runOnUiThread {

                    Toast.makeText(
                        this@UnknownMerchantActivity,
                        "Expense added as $selectedCategory",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                }
            }
        }

        btnSkip.setOnClickListener {

            lifecycleScope.launch(
                Dispatchers.IO
            ) {

                // Save transaction as Other
                saveExpense(
                    category = "Other"
                )

                // Remove transaction from pending table
                deletePendingTransaction()

                runOnUiThread {

                    Toast.makeText(
                        this@UnknownMerchantActivity,
                        "Expense added as Other",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                }
            }
        }
    }

    private suspend fun saveExpense(
        category: String
    ) {

        val dateFormat =
            SimpleDateFormat(
                "dd-MM-yyyy",
                Locale.getDefault()
            )

        val currentDate =
            dateFormat.format(Date())

        val expense =
            ExpenseEntity(
                title = merchant,
                amount = amount,
                category = category,
                date = currentDate,
                transactionId = transactionId
            )

        database
            .expenseDao()
            .insertExpense(
                expense
            )

        // Check budget threshold & notify if 80% or 100%+ reached
        val currentMonthKey = SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date())
        val allExpenses = database.expenseDao().getAllExpensesOnce()
        val currentMonthSpent = allExpenses
            .filter { exp ->
                val parts = exp.date.split("-")
                if (parts.size == 3) "${parts[1]}-${parts[2]}" == currentMonthKey else false
            }
            .sumOf { it.amount }

        val budgetManager = BudgetManager(this@UnknownMerchantActivity)
        budgetManager.checkAndNotifyBudgetThreshold(currentMonthSpent)
    }

    private suspend fun deletePendingTransaction() {

        database
            .pendingTransactionDao()
            .deletePendingTransaction(
                transactionId
            )
    }
}