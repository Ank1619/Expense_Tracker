package com.example.expensestracker

import android.app.DatePickerDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.expensestracker.data.local.ExpenseDatabase
import com.example.expensestracker.data.local.ExpenseEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ExpenseDetailActivity : AppCompatActivity() {

    private lateinit var database: ExpenseDatabase
    private var expenseId: Int = -1
    private var currentExpense: ExpenseEntity? = null

    private lateinit var tvDetailTitle: TextView
    private lateinit var tvDetailAmount: TextView
    private lateinit var tvDetailTypeBadge: TextView
    private lateinit var tvDetailCategoryBadge: TextView
    private lateinit var tvDetailDate: TextView
    private lateinit var tvDetailPaymentSource: TextView
    private lateinit var tvDetailNotes: TextView
    private lateinit var tvDetailTxId: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_detail)

        database = ExpenseDatabase.getDatabase(this)

        expenseId = intent.getIntExtra("expenseId", -1)
        if (expenseId == -1) {
            Toast.makeText(this, "Expense not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val btnBack = findViewById<ImageView>(R.id.btnBackDetail)
        btnBack.setOnClickListener { finish() }

        val btnDelete = findViewById<ImageView>(R.id.btnDeleteDetail)
        btnDelete.setOnClickListener { confirmDelete() }

        val btnEdit = findViewById<Button>(R.id.btnEditExpense)
        btnEdit.setOnClickListener { showEditDialog() }

        tvDetailTitle = findViewById(R.id.tvDetailTitle)
        tvDetailAmount = findViewById(R.id.tvDetailAmount)
        tvDetailTypeBadge = findViewById(R.id.tvDetailTypeBadge)
        tvDetailCategoryBadge = findViewById(R.id.tvDetailCategoryBadge)
        tvDetailDate = findViewById(R.id.tvDetailDate)
        tvDetailPaymentSource = findViewById(R.id.tvDetailPaymentSource)
        tvDetailNotes = findViewById(R.id.tvDetailNotes)
        tvDetailTxId = findViewById(R.id.tvDetailTxId)

        loadExpenseDetails()
    }

    private fun loadExpenseDetails() {
        lifecycleScope.launch {
            val expense = withContext(Dispatchers.IO) {
                database.expenseDao().getAllExpensesOnce().find { it.id == expenseId }
            }

            if (expense == null) {
                Toast.makeText(this@ExpenseDetailActivity, "Expense deleted", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            currentExpense = expense
            bindExpenseToUI(expense)
        }
    }

    private fun bindExpenseToUI(expense: ExpenseEntity) {
        tvDetailTitle.text = expense.title

        val isIncome = expense.transactionType == "INCOME"
        val prefix = if (isIncome) "+₹" else "₹"
        tvDetailAmount.text = String.format(Locale.getDefault(), "%s%.2f", prefix, expense.amount)
        tvDetailAmount.setTextColor(
            ContextCompat.getColor(
                this,
                if (isIncome) R.color.secondary else R.color.primary
            )
        )

        tvDetailTypeBadge.text = expense.transactionType
        if (isIncome) {
            tvDetailTypeBadge.setBackgroundResource(R.drawable.bg_pending_badge)
            tvDetailTypeBadge.setTextColor(ContextCompat.getColor(this, R.color.secondary))
        } else {
            tvDetailTypeBadge.setBackgroundResource(R.drawable.bg_chip_selected)
            tvDetailTypeBadge.setTextColor(ContextCompat.getColor(this, R.color.primary))
        }

        tvDetailCategoryBadge.text = expense.category
        tvDetailDate.text = expense.date
        tvDetailPaymentSource.text = expense.paymentSource
        tvDetailNotes.text = if (expense.note.isNotBlank()) expense.note else "None"
        tvDetailTxId.text = if (expense.transactionId.isNotBlank()) expense.transactionId else "Manual Entry"
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("Delete Transaction?")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete") { _, _ ->
                currentExpense?.let { exp ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        database.expenseDao().deleteExpense(exp)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@ExpenseDetailActivity, "Transaction deleted", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditDialog() {
        val exp = currentExpense ?: return

        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_add_expenses)

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        val etExpenseName = dialog.findViewById<EditText>(R.id.etExpenseName)
        val etAmount = dialog.findViewById<EditText>(R.id.etAmount)
        val etCategory = dialog.findViewById<AutoCompleteTextView>(R.id.etCategory)
        val etDate = dialog.findViewById<EditText>(R.id.etDate)

        etExpenseName.setText(exp.title)
        etAmount.setText(String.format(Locale.getDefault(), "%.2f", exp.amount))
        etCategory.setText(exp.category)
        etDate.setText(exp.date)

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

        etDate.isFocusable = false
        etDate.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val selectedCal = Calendar.getInstance()
                    selectedCal.set(selectedYear, selectedMonth, selectedDay)
                    etDate.setText(dateFormat.format(selectedCal.time))
                },
                year,
                month,
                day
            ).show()
        }

        val categories = resources.getStringArray(R.array.expense_categories)
        val categoryAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            categories
        )

        etCategory.setAdapter(categoryAdapter)
        etCategory.setOnClickListener { etCategory.showDropDown() }

        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialog.findViewById<Button>(R.id.btnSaveExpense)

        btnSave.text = "Update Transaction"

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val title = etExpenseName.text.toString().trim()
            val amountText = etAmount.text.toString().trim()
            val category = etCategory.text.toString().trim()
            val date = etDate.text.toString().trim()

            if (title.isEmpty() || amountText.isEmpty() || category.isEmpty() || date.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountText.toDoubleOrNull() ?: return@setOnClickListener

            val updatedExpense = exp.copy(
                title = title,
                amount = amount,
                category = category,
                date = date
            )

            lifecycleScope.launch(Dispatchers.IO) {
                database.expenseDao().updateExpense(updatedExpense)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ExpenseDetailActivity, "Transaction updated!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    loadExpenseDetails()
                }
            }
        }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }
}