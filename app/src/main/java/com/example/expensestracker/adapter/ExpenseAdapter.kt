package com.example.expensestracker.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.expensestracker.R
import com.example.expensestracker.data.local.ExpenseEntity
import java.util.Locale

class ExpenseAdapter(
    private val onItemClick: (ExpenseEntity) -> Unit,
    private val onDeleteClick: (ExpenseEntity) -> Unit
) : ListAdapter<ExpenseEntity, ExpenseAdapter.ExpenseViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvExpenseTitle)
        private val tvSubtitle: TextView = itemView.findViewById(R.id.tvExpenseSubtitle)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvExpenseAmount)
        private val ivCategoryIcon: ImageView = itemView.findViewById(R.id.ivCategoryIcon)
        private val btnDelete: ImageView = itemView.findViewById(R.id.btnDeleteExpense)

        fun bind(expense: ExpenseEntity) {
            tvTitle.text = expense.title

            val subtitleText = "${expense.category} • ${expense.paymentSource} • ${expense.date}"
            tvSubtitle.text = subtitleText

            val isIncome = expense.transactionType == "INCOME"
            val prefix = if (isIncome) "+₹" else "₹"
            val formattedAmount = if (expense.amount % 1.0 == 0.0) {
                String.format(Locale.getDefault(), "%s%.0f", prefix, expense.amount)
            } else {
                String.format(Locale.getDefault(), "%s%.2f", prefix, expense.amount)
            }
            tvAmount.text = formattedAmount
            tvAmount.setTextColor(
                ContextCompat.getColor(
                    itemView.context,
                    if (isIncome) R.color.secondary else R.color.primary
                )
            )

            val iconRes = when (expense.category.lowercase(Locale.getDefault())) {
                "food & dining", "food" -> R.drawable.ic_category
                "transportation", "travel" -> R.drawable.ic_category
                "shopping" -> R.drawable.ic_label
                "bills & utilities", "bills" -> R.drawable.ic_money
                "income", "salary" -> R.drawable.ic_wallet
                else -> R.drawable.ic_category
            }
            ivCategoryIcon.setImageResource(iconRes)

            itemView.setOnClickListener {
                onItemClick(expense)
            }

            btnDelete.setOnClickListener {
                onDeleteClick(expense)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ExpenseEntity>() {
        override fun areItemsTheSame(oldItem: ExpenseEntity, newItem: ExpenseEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ExpenseEntity, newItem: ExpenseEntity): Boolean {
            return oldItem == newItem
        }
    }
}