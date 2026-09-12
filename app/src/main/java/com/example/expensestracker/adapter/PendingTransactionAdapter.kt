package com.example.expensestracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.expensestracker.data.local.PendingTransactionEntity

class PendingTransactionAdapter(
    private var transactions: List<PendingTransactionEntity>,
    private val onTransactionClick: (PendingTransactionEntity) -> Unit
) : RecyclerView.Adapter<PendingTransactionAdapter.PendingViewHolder>() {

    class PendingViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        val tvMerchant: TextView =
            itemView.findViewById(
                R.id.tvPendingMerchant
            )

        val tvAmount: TextView =
            itemView.findViewById(
                R.id.tvPendingAmount
            )

        val tvDate: TextView =
            itemView.findViewById(
                R.id.tvPendingDate
            )
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PendingViewHolder {

        val view =
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_pending_transaction,
                    parent,
                    false
                )

        return PendingViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: PendingViewHolder,
        position: Int
    ) {

        val transaction =
            transactions[position]

        holder.tvMerchant.text =
            transaction.merchant

        holder.tvAmount.text =
            "₹${String.format(
                java.util.Locale.getDefault(),
                "%.2f",
                transaction.amount
            )}"

        holder.tvDate.text =
            transaction.date

        holder.itemView.setOnClickListener {

            onTransactionClick(
                transaction
            )
        }
    }

    override fun getItemCount(): Int =
        transactions.size

    fun updateTransactions(
        newTransactions: List<PendingTransactionEntity>
    ) {

        transactions =
            newTransactions

        notifyDataSetChanged()
    }
}

