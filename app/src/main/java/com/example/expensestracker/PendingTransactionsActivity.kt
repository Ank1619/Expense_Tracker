package com.example.expensestracker

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.expensestracker.data.local.ExpenseDatabase
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.util.Locale

class PendingTransactionsActivity : AppCompatActivity() {

    private lateinit var database: ExpenseDatabase
    private lateinit var adapter: PendingTransactionAdapter

    private lateinit var cardSummaryBanner: MaterialCardView
    private lateinit var tvPendingCountSummary: TextView
    private lateinit var tvPendingAmountSummary: TextView
    private lateinit var rvPendingTransactions: RecyclerView
    private lateinit var cardNoPendingPlaceholder: MaterialCardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_pending_transactions
        )

        database = ExpenseDatabase.getDatabase(this)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        cardSummaryBanner = findViewById(R.id.cardSummaryBanner)
        tvPendingCountSummary = findViewById(R.id.tvPendingCountSummary)
        tvPendingAmountSummary = findViewById(R.id.tvPendingAmountSummary)
        rvPendingTransactions = findViewById(R.id.rvPendingTransactions)
        cardNoPendingPlaceholder = findViewById(R.id.cardNoPendingPlaceholder)

        adapter = PendingTransactionAdapter(
            emptyList()
        ) { transaction ->

            val intent = Intent(
                this,
                UnknownMerchantActivity::class.java
            ).apply {
                putExtra(
                    "transactionId",
                    transaction.transactionId
                )
            }

            startActivity(intent)
        }

        rvPendingTransactions.layoutManager = LinearLayoutManager(this)
        rvPendingTransactions.adapter = adapter

        observePendingTransactions()
    }

    private fun observePendingTransactions() {
        lifecycleScope.launch {
            database.pendingTransactionDao().getAllPendingTransactionsFlow().collect { transactions ->
                adapter.updateTransactions(transactions)

                if (transactions.isEmpty()) {
                    cardSummaryBanner.visibility = View.GONE
                    rvPendingTransactions.visibility = View.GONE
                    cardNoPendingPlaceholder.visibility = View.VISIBLE
                } else {
                    cardSummaryBanner.visibility = View.VISIBLE
                    rvPendingTransactions.visibility = View.VISIBLE
                    cardNoPendingPlaceholder.visibility = View.GONE

                    val count = transactions.size
                    val totalAmount = transactions.sumOf { it.amount }

                    tvPendingCountSummary.text = if (count == 1) {
                        "1 Pending Transaction"
                    } else {
                        "$count Pending Transactions"
                    }

                    tvPendingAmountSummary.text = String.format(
                        Locale.getDefault(),
                        "Total pending amount: ₹%.2f",
                        totalAmount
                    )
                }
            }
        }
    }
}