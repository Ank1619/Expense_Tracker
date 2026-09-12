package com.example.expensestracker

import android.Manifest
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.expensestracker.adapter.ExpenseAdapter
import com.example.expensestracker.data.LockManager
import com.example.expensestracker.data.ThemeManager
import com.example.expensestracker.data.local.BudgetManager
import com.example.expensestracker.data.local.ExpenseDatabase
import com.example.expensestracker.data.local.ExpenseEntity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var database: ExpenseDatabase
    private lateinit var budgetManager: BudgetManager
    private lateinit var themeManager: ThemeManager
    private lateinit var lockManager: LockManager
    private lateinit var expenseAdapter: ExpenseAdapter

    private lateinit var tvTotalExpenseAmount: TextView
    private lateinit var tvTotalIncomeAmount: TextView
    private lateinit var tvNetBalanceAmount: TextView
    private lateinit var tvOverviewPill: TextView
    private lateinit var rvExpenses: RecyclerView
    private lateinit var cardPlaceholder: MaterialCardView
    private lateinit var tvPlaceholderTitle: TextView
    private lateinit var tvPlaceholderSub: TextView

    private lateinit var cardMonthlyBudget: MaterialCardView
    private lateinit var btnSetBudget: TextView
    private lateinit var tvBudgetSpentInfo: TextView
    private lateinit var progressBudget: ProgressBar
    private lateinit var layoutBudgetAlert: View
    private lateinit var tvBudgetAlertText: TextView

    private lateinit var cardPendingBanner: MaterialCardView
    private lateinit var tvPendingBannerTitle: TextView
    private lateinit var btnPendingTransactions: MaterialButton

    private lateinit var etSearch: EditText
    private lateinit var btnClearSearch: ImageView

    private lateinit var chipMonth: TextView
    private lateinit var chipCategory: TextView
    private lateinit var chipDate: TextView
    private lateinit var chipSort: TextView
    private lateinit var chipClear: TextView
    private lateinit var tvFilterSummary: TextView

    // Raw list from Room DB
    private var rawExpenseList: List<ExpenseEntity> = emptyList()

    // Filter & Search State
    private var searchQuery: String = ""
    private var selectedCategory: String = "All"
    private var selectedMonthKey: String = "All" // Format "MM-yyyy" or "All"
    private var selectedMonthLabel: String = "All Months"
    private var selectedDate: String = "All" // Format "dd-MM-yyyy" or "All"
    private var currentSortOrder: SortOrder = SortOrder.DEFAULT

    enum class SortOrder {
        DEFAULT,
        AMOUNT_HIGH_TO_LOW,
        AMOUNT_LOW_TO_HIGH,
        DATE_NEWEST,
        DATE_OLDEST
    }

    companion object {
        private const val SMS_PERMISSION_REQUEST_CODE = 100
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        themeManager = ThemeManager(this)
        themeManager.applySavedTheme()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Room Database Singleton, Budget Manager & Lock Manager
        database = ExpenseDatabase.getDatabase(this)
        budgetManager = BudgetManager(this)
        lockManager = LockManager(this)

        // Bind UI Elements
        tvTotalExpenseAmount = findViewById(R.id.tvTotalExpenseAmount)
        tvTotalIncomeAmount = findViewById(R.id.tvTotalIncomeAmount)
        tvNetBalanceAmount = findViewById(R.id.tvNetBalanceAmount)
        tvOverviewPill = findViewById(R.id.tvOverviewPill)
        rvExpenses = findViewById(R.id.rvExpenses)
        cardPlaceholder = findViewById(R.id.cardPlaceholder)
        tvPlaceholderTitle = findViewById(R.id.tvPlaceholderTitle)
        tvPlaceholderSub = findViewById(R.id.tvPlaceholderSub)

        cardMonthlyBudget = findViewById(R.id.cardMonthlyBudget)
        btnSetBudget = findViewById(R.id.btnSetBudget)
        tvBudgetSpentInfo = findViewById(R.id.tvBudgetSpentInfo)
        progressBudget = findViewById(R.id.progressBudget)
        layoutBudgetAlert = findViewById(R.id.layoutBudgetAlert)
        tvBudgetAlertText = findViewById(R.id.tvBudgetAlertText)

        cardPendingBanner = findViewById(R.id.cardPendingBanner)
        tvPendingBannerTitle = findViewById(R.id.tvPendingBannerTitle)
        btnPendingTransactions = findViewById(R.id.btnPendingTransactions)

        etSearch = findViewById(R.id.etSearch)
        btnClearSearch = findViewById(R.id.btnClearSearch)

        chipMonth = findViewById(R.id.chipMonth)
        chipCategory = findViewById(R.id.chipCategory)
        chipDate = findViewById(R.id.chipDate)
        chipSort = findViewById(R.id.chipSort)
        chipClear = findViewById(R.id.chipClear)
        tvFilterSummary = findViewById(R.id.tvFilterSummary)

        val btnAddExpense = findViewById<Button>(R.id.btnAddExpense)
        val btnSettings = findViewById<View>(R.id.btnSettings)

        btnSettings.setOnClickListener { showSettingsDialog() }

        // Open PendingTransactionsActivity
        val openPendingListener = View.OnClickListener {
            val intent = Intent(
                this,
                PendingTransactionsActivity::class.java
            )
            startActivity(intent)
        }

        btnPendingTransactions.setOnClickListener(openPendingListener)
        cardPendingBanner.setOnClickListener(openPendingListener)

        // Open AnalyticsActivity
        val cardTotalExpense = findViewById<View>(R.id.cardTotalExpense)
        val btnAnalytics = findViewById<View>(R.id.btnAnalytics)

        val openAnalyticsListener = View.OnClickListener {
            val intent = Intent(
                this,
                AnalyticsActivity::class.java
            )
            startActivity(intent)
        }

        btnAnalytics.setOnClickListener(openAnalyticsListener)
        cardTotalExpense.setOnClickListener(openAnalyticsListener)

        // Set Budget listener
        val openSetBudgetListener = View.OnClickListener { showSetBudgetDialog() }
        cardMonthlyBudget.setOnClickListener(openSetBudgetListener)
        btnSetBudget.setOnClickListener(openSetBudgetListener)

        // Scan Receipt listener
        val btnScanReceipt = findViewById<View>(R.id.btnScanReceipt)
        btnScanReceipt.setOnClickListener {
            val intent = Intent(
                this,
                ReceiptScannerActivity::class.java
            )
            startActivity(intent)
        }

        // Setup RecyclerView Adapter & LayoutManager
        setupRecyclerView()

        // Setup Search and Filters
        setupSearchAndFilterListeners()

        // Observe Live Expenses Flow from Room Database
        observeExpenses()

        // Observe Live Pending Transactions Flow from Room Database
        observePendingTransactions()

        // Add Expense Dialog trigger
        btnAddExpense.setOnClickListener {
            showAddExpenseDialog()
        }

        // Request SMS permission
        requestSmsPermission()

        // Request notification permission
        requestNotificationPermission()
    }

    override fun onResume() {
        super.onResume()

        if (lockManager.isLockEnabled() && !lockManager.isSessionUnlocked()) {
            val intent = Intent(this, AppLockActivity::class.java)
            startActivity(intent)
            return
        }

        updatePendingTransactionsState()
    }

    private fun setupSearchAndFilterListeners() {
        // Search text watcher
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim() ?: ""
                btnClearSearch.visibility = if (searchQuery.isNotEmpty()) View.VISIBLE else View.GONE
                applyFiltersAndSort()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnClearSearch.setOnClickListener {
            etSearch.setText("")
        }

        // Chip click listeners
        chipMonth.setOnClickListener {
            showMonthSelectionDialog()
        }

        chipCategory.setOnClickListener {
            showCategorySelectionDialog()
        }

        chipDate.setOnClickListener {
            showDatePickerFilterDialog()
        }

        chipSort.setOnClickListener {
            showSortSelectionDialog()
        }

        chipClear.setOnClickListener {
            clearAllFilters()
        }
    }

    private fun observeExpenses() {
        // Collect real-time Flow updates from Room Database
        lifecycleScope.launch {
            database
                .expenseDao()
                .getAllExpenses()
                .collect { expenses ->
                    rawExpenseList = expenses
                    applyFiltersAndSort()
                }
        }
    }

    private fun observePendingTransactions() {
        lifecycleScope.launch {
            database
                .pendingTransactionDao()
                .getAllPendingTransactionsFlow()
                .collect { pendingList ->
                    val count = pendingList.size
                    if (count > 0) {
                        cardPendingBanner.visibility = View.VISIBLE
                        tvPendingBannerTitle.text = if (count == 1) {
                            "1 Pending Transaction"
                        } else {
                            "$count Pending Transactions"
                        }
                        btnPendingTransactions.text = "Pending ($count)"
                    } else {
                        cardPendingBanner.visibility = View.GONE
                        btnPendingTransactions.text = "Pending (0)"
                    }
                }
        }
    }

    private fun applyFiltersAndSort() {
        var filteredList = rawExpenseList

        // 1. Search Query Filter (matches title or category)
        if (searchQuery.isNotBlank()) {
            val query = searchQuery.lowercase(Locale.getDefault())
            filteredList = filteredList.filter {
                it.title.lowercase(Locale.getDefault()).contains(query) ||
                        it.category.lowercase(Locale.getDefault()).contains(query)
            }
        }

        // 2. Category Filter
        if (selectedCategory != "All") {
            filteredList = filteredList.filter {
                it.category.equals(selectedCategory, ignoreCase = true)
            }
        }

        // 3. Month Filter
        if (selectedMonthKey != "All") {
            filteredList = filteredList.filter { expense ->
                getMonthYearKey(expense.date) == selectedMonthKey
            }
        }

        // 4. Date Filter
        if (selectedDate != "All") {
            filteredList = filteredList.filter {
                it.date == selectedDate
            }
        }

        // 5. Sort Order
        filteredList = when (currentSortOrder) {
            SortOrder.AMOUNT_HIGH_TO_LOW -> filteredList.sortedByDescending { it.amount }
            SortOrder.AMOUNT_LOW_TO_HIGH -> filteredList.sortedBy { it.amount }
            SortOrder.DATE_NEWEST -> filteredList.sortedByDescending { parseDate(it.date).time }
            SortOrder.DATE_OLDEST -> filteredList.sortedBy { parseDate(it.date).time }
            SortOrder.DEFAULT -> filteredList
        }

        // Submit to adapter
        expenseAdapter.submitList(filteredList)

        // Calculate Income, Expense & Net Balance
        val totalIncome = filteredList.filter { it.transactionType == "INCOME" }.sumOf { it.amount }
        val totalExpense = filteredList.filter { it.transactionType != "INCOME" }.sumOf { it.amount }
        val netBalance = totalIncome - totalExpense

        tvTotalIncomeAmount.text = String.format(Locale.getDefault(), "₹%.2f", totalIncome)
        tvTotalExpenseAmount.text = String.format(Locale.getDefault(), "₹%.2f", totalExpense)
        tvNetBalanceAmount.text = String.format(Locale.getDefault(), "₹%.2f", netBalance)

        // Update Overview Pill
        if (selectedMonthKey != "All") {
            tvOverviewPill.text = "$selectedMonthLabel Overview"
        } else if (selectedCategory != "All" || selectedDate != "All" || searchQuery.isNotBlank()) {
            tvOverviewPill.text = "Filtered Overview"
        } else {
            tvOverviewPill.text = "All Time Overview"
        }

        // Update Summary Text
        tvFilterSummary.text = "${filteredList.size} of ${rawExpenseList.size} items"

        // Toggle Empty State vs List
        if (filteredList.isEmpty()) {
            rvExpenses.visibility = View.GONE
            cardPlaceholder.visibility = View.VISIBLE
            if (rawExpenseList.isNotEmpty()) {
                tvPlaceholderTitle.text = "No matching expenses"
                tvPlaceholderSub.text = "Try adjusting your search query or clear filters"
            } else {
                tvPlaceholderTitle.text = getString(R.string.all_caught_up)
                tvPlaceholderSub.text = getString(R.string.placeholder_description)
            }
        } else {
            rvExpenses.visibility = View.VISIBLE
            cardPlaceholder.visibility = View.GONE
        }

        // Update Chip Appearance & Budget Progress based on exact expense amount in card
        updateChipAppearance()
        updateBudgetProgress(totalExpense)
    }

    private fun updateBudgetProgress(currentSpent: Double) {
        val targetBudget = budgetManager.getMonthlyBudget()

        if (targetBudget <= 0.0) {
            btnSetBudget.text = "Set Budget ✏️"
            tvBudgetSpentInfo.text = "Set a monthly budget to track spending progress"
            progressBudget.progress = 0
            layoutBudgetAlert.visibility = View.GONE
            return
        }

        btnSetBudget.text = "Edit Budget ✏️"
        val usagePercentage = (currentSpent / targetBudget * 100).toInt()
        progressBudget.progress = usagePercentage.coerceAtMost(100)

        tvBudgetSpentInfo.text = String.format(
            Locale.getDefault(),
            "Spent ₹%.2f of ₹%.2f (%d%%)",
            currentSpent,
            targetBudget,
            usagePercentage
        )

        when {
            usagePercentage >= 100 -> {
                layoutBudgetAlert.visibility = View.VISIBLE
                layoutBudgetAlert.setBackgroundResource(R.drawable.bg_red_badge)
                val excess = currentSpent - targetBudget
                tvBudgetAlertText.text = String.format(
                    Locale.getDefault(),
                    "🚨 Alert: Budget exceeded by ₹%.2f!",
                    excess
                )
                tvBudgetAlertText.setTextColor(ContextCompat.getColor(this, R.color.red_primary))
            }
            usagePercentage >= 80 -> {
                layoutBudgetAlert.visibility = View.VISIBLE
                layoutBudgetAlert.setBackgroundResource(R.drawable.bg_pending_badge)
                tvBudgetAlertText.text = "⚠️ Warning: You have used $usagePercentage% of monthly budget!"
                tvBudgetAlertText.setTextColor(ContextCompat.getColor(this, R.color.amber_primary))
            }
            else -> {
                layoutBudgetAlert.visibility = View.VISIBLE
                layoutBudgetAlert.setBackgroundResource(R.drawable.bg_chip_selected)
                val remaining = targetBudget - currentSpent
                tvBudgetAlertText.text = String.format(
                    Locale.getDefault(),
                    "Remaining: ₹%.2f for this period",
                    remaining
                )
                tvBudgetAlertText.setTextColor(ContextCompat.getColor(this, R.color.primary))
            }
        }

        // Check and notify budget threshold (80% / 100%+)
        budgetManager.checkAndNotifyBudgetThreshold(currentSpent)
    }

    private fun showSettingsDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_settings)

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        val rgThemeOptions = dialog.findViewById<RadioGroup>(R.id.rgThemeOptions)
        val rbLight = dialog.findViewById<RadioButton>(R.id.rbThemeLight)
        val rbDark = dialog.findViewById<RadioButton>(R.id.rbThemeDark)
        val rbSystem = dialog.findViewById<RadioButton>(R.id.rbThemeSystem)

        val tvLockStatus = dialog.findViewById<TextView>(R.id.tvLockStatus)
        val btnSetupPin = dialog.findViewById<Button>(R.id.btnSetupPin)
        val btnDisablePin = dialog.findViewById<Button>(R.id.btnDisablePin)
        val btnClose = dialog.findViewById<Button>(R.id.btnCloseSettings)

        when (themeManager.getThemeMode()) {
            ThemeManager.THEME_LIGHT -> rbLight.isChecked = true
            ThemeManager.THEME_DARK -> rbDark.isChecked = true
            else -> rbSystem.isChecked = true
        }

        rgThemeOptions.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.rbThemeLight -> ThemeManager.THEME_LIGHT
                R.id.rbThemeDark -> ThemeManager.THEME_DARK
                else -> ThemeManager.THEME_SYSTEM
            }
            themeManager.setThemeMode(mode)
        }

        fun updateLockUI() {
            if (lockManager.isLockEnabled()) {
                tvLockStatus.text = "🔒 PIN Lock is currently ENABLED"
                tvLockStatus.setTextColor(ContextCompat.getColor(this, R.color.primary))
                btnDisablePin.visibility = View.VISIBLE
                btnSetupPin.text = "Change PIN"
            } else {
                tvLockStatus.text = "🔓 PIN Lock is currently OFF"
                tvLockStatus.setTextColor(ContextCompat.getColor(this, R.color.text_muted))
                btnDisablePin.visibility = View.GONE
                btnSetupPin.text = "Setup PIN"
            }
        }

        updateLockUI()

        btnSetupPin.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, AppLockActivity::class.java).apply {
                putExtra("isSetupMode", true)
            }
            startActivity(intent)
        }

        btnDisablePin.setOnClickListener {
            lockManager.setLockEnabled(false)
            Toast.makeText(this, "PIN Lock disabled", Toast.LENGTH_SHORT).show()
            updateLockUI()
        }

        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun showSetBudgetDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_set_budget)

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        val etBudgetAmount = dialog.findViewById<EditText>(R.id.etBudgetAmount)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancelBudget)
        val btnSave = dialog.findViewById<Button>(R.id.btnSaveBudget)

        val currentBudget = budgetManager.getMonthlyBudget()
        if (currentBudget > 0.0) {
            etBudgetAmount.setText(String.format(Locale.getDefault(), "%.0f", currentBudget))
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val amountText = etBudgetAmount.text.toString().trim()
            val amount = amountText.toDoubleOrNull()

            if (amount == null || amount <= 0) {
                etBudgetAmount.error = "Enter a valid budget amount"
                return@setOnClickListener
            }

            budgetManager.setMonthlyBudget(amount)
            Toast.makeText(this, "Monthly budget updated!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            applyFiltersAndSort()
        }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun updateChipAppearance() {
        // Month chip
        if (selectedMonthKey != "All") {
            chipMonth.text = "📅 $selectedMonthLabel"
            chipMonth.setBackgroundResource(R.drawable.bg_chip_selected)
            chipMonth.setTextColor(ContextCompat.getColor(this, R.color.primary))
        } else {
            chipMonth.text = getString(R.string.filter_all_months)
            chipMonth.setBackgroundResource(R.drawable.bg_chip_normal)
            chipMonth.setTextColor(ContextCompat.getColor(this, R.color.text_dark))
        }

        // Category chip
        if (selectedCategory != "All") {
            chipCategory.text = "📂 $selectedCategory"
            chipCategory.setBackgroundResource(R.drawable.bg_chip_selected)
            chipCategory.setTextColor(ContextCompat.getColor(this, R.color.primary))
        } else {
            chipCategory.text = getString(R.string.filter_all_categories)
            chipCategory.setBackgroundResource(R.drawable.bg_chip_normal)
            chipCategory.setTextColor(ContextCompat.getColor(this, R.color.text_dark))
        }

        // Date chip
        if (selectedDate != "All") {
            chipDate.text = "📅 $selectedDate"
            chipDate.setBackgroundResource(R.drawable.bg_chip_selected)
            chipDate.setTextColor(ContextCompat.getColor(this, R.color.primary))
        } else {
            chipDate.text = getString(R.string.filter_all_dates)
            chipDate.setBackgroundResource(R.drawable.bg_chip_normal)
            chipDate.setTextColor(ContextCompat.getColor(this, R.color.text_dark))
        }

        // Sort chip
        if (currentSortOrder != SortOrder.DEFAULT) {
            chipSort.text = when (currentSortOrder) {
                SortOrder.AMOUNT_HIGH_TO_LOW -> "💰 Amount: High → Low"
                SortOrder.AMOUNT_LOW_TO_HIGH -> "💰 Amount: Low → High"
                SortOrder.DATE_NEWEST -> "📅 Date: Newest"
                SortOrder.DATE_OLDEST -> "📅 Date: Oldest"
                else -> getString(R.string.filter_sort)
            }
            chipSort.setBackgroundResource(R.drawable.bg_chip_selected)
            chipSort.setTextColor(ContextCompat.getColor(this, R.color.primary))
        } else {
            chipSort.text = getString(R.string.filter_sort)
            chipSort.setBackgroundResource(R.drawable.bg_chip_normal)
            chipSort.setTextColor(ContextCompat.getColor(this, R.color.text_dark))
        }

        // Clear button visibility
        val isAnyFilterActive = searchQuery.isNotEmpty() ||
                selectedCategory != "All" ||
                selectedMonthKey != "All" ||
                selectedDate != "All" ||
                currentSortOrder != SortOrder.DEFAULT

        chipClear.visibility = if (isAnyFilterActive) View.VISIBLE else View.GONE
    }

    private fun clearAllFilters() {
        searchQuery = ""
        etSearch.setText("")
        selectedCategory = "All"
        selectedMonthKey = "All"
        selectedMonthLabel = "All Months"
        selectedDate = "All"
        currentSortOrder = SortOrder.DEFAULT
        applyFiltersAndSort()
    }

    private fun showMonthSelectionDialog() {
        // Extract distinct month keys from rawExpenseList
        val monthKeys = rawExpenseList
            .map { getMonthYearKey(it.date) }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedByDescending { it }

        val labels = mutableListOf<String>()
        val keyList = mutableListOf<String>()

        labels.add("📅 All Months")
        keyList.add("All")

        for (key in monthKeys) {
            labels.add(getMonthYearLabel(key))
            keyList.add(key)
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Month")
        builder.setItems(labels.toTypedArray()) { _, which ->
            selectedMonthKey = keyList[which]
            selectedMonthLabel = labels[which].replace("📅 ", "")
            applyFiltersAndSort()
        }
        builder.show()
    }

    private fun showCategorySelectionDialog() {
        val categories = resources.getStringArray(R.array.expense_categories).toMutableList()
        categories.add(0, "All Categories")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Filter by Category")
        builder.setItems(categories.toTypedArray()) { _, which ->
            selectedCategory = if (which == 0) "All" else categories[which]
            applyFiltersAndSort()
        }
        builder.show()
    }

    private fun showDatePickerFilterDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCal = Calendar.getInstance()
                selectedCal.set(selectedYear, selectedMonth, selectedDay)
                val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                selectedDate = dateFormat.format(selectedCal.time)
                applyFiltersAndSort()
            },
            year,
            month,
            day
        ).apply {
            setButton(AlertDialog.BUTTON_NEUTRAL, "Clear Date Filter") { _, _ ->
                selectedDate = "All"
                applyFiltersAndSort()
            }
        }.show()
    }

    private fun showSortSelectionDialog() {
        val options = arrayOf(
            "Default (Newest First)",
            "Amount: High to Low",
            "Amount: Low to High",
            "Date: Newest First",
            "Date: Oldest First"
        )

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Sort Expenses")
        builder.setItems(options) { _, which ->
            currentSortOrder = when (which) {
                1 -> SortOrder.AMOUNT_HIGH_TO_LOW
                2 -> SortOrder.AMOUNT_LOW_TO_HIGH
                3 -> SortOrder.DATE_NEWEST
                4 -> SortOrder.DATE_OLDEST
                else -> SortOrder.DEFAULT
            }
            applyFiltersAndSort()
        }
        builder.show()
    }

    private fun getMonthYearKey(dateStr: String): String {
        return try {
            // dateStr format "dd-MM-yyyy" -> monthKey "MM-yyyy"
            val parts = dateStr.split("-")
            if (parts.size == 3) {
                "${parts[1]}-${parts[2]}"
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun getMonthYearLabel(monthKey: String): String {
        return try {
            val date = SimpleDateFormat("MM-yyyy", Locale.getDefault()).parse(monthKey)
            if (date != null) {
                SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date)
            } else {
                monthKey
            }
        } catch (e: Exception) {
            monthKey
        }
    }

    private fun parseDate(dateStr: String): Date {
        return try {
            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).parse(dateStr) ?: Date(0)
        } catch (e: Exception) {
            Date(0)
        }
    }

    private fun updatePendingTransactionsState() {
        lifecycleScope.launch {
            val pendingList = withContext(Dispatchers.IO) {
                database
                    .pendingTransactionDao()
                    .getAllPendingTransactions()
            }

            val count = pendingList.size
            if (count > 0) {
                cardPendingBanner.visibility = View.VISIBLE
                tvPendingBannerTitle.text = if (count == 1) {
                    "1 Pending Transaction"
                } else {
                    "$count Pending Transactions"
                }
                btnPendingTransactions.text = "Pending ($count)"
            } else {
                cardPendingBanner.visibility = View.GONE
                btnPendingTransactions.text = "Pending (0)"
            }
        }
    }

    private fun setupRecyclerView() {
        expenseAdapter = ExpenseAdapter(
            onItemClick = { expense ->
                val intent = Intent(this, ExpenseDetailActivity::class.java).apply {
                    putExtra("expenseId", expense.id)
                }
                startActivity(intent)
            },
            onDeleteClick = { expense ->
                deleteExpense(expense)
            }
        )

        rvExpenses.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = expenseAdapter
        }
    }

    private fun deleteExpense(expense: ExpenseEntity) {
        CoroutineScope(Dispatchers.IO).launch {
            database
                .expenseDao()
                .deleteExpense(expense)

            runOnUiThread {
                Toast.makeText(
                    this@MainActivity,
                    "Expense deleted",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun requestSmsPermission() {
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECEIVE_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.RECEIVE_SMS
                ),
                SMS_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun requestNotificationPermission() {
        if (
            android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.TIRAMISU
        ) {
            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.POST_NOTIFICATIONS
                    ),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show()
            }
        }

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAddExpenseDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_add_expenses)

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        val etExpenseName = dialog.findViewById<EditText>(R.id.etExpenseName)
        val etAmount = dialog.findViewById<EditText>(R.id.etAmount)
        val etCategory = dialog.findViewById<AutoCompleteTextView>(R.id.etCategory)
        val etDate = dialog.findViewById<EditText>(R.id.etDate)

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

        etDate.setText(dateFormat.format(calendar.time))
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
        etCategory.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) etCategory.showDropDown() }

        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialog.findViewById<Button>(R.id.btnSaveExpense)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val title = etExpenseName.text.toString().trim()
            val amountText = etAmount.text.toString().trim()
            val category = etCategory.text.toString().trim()
            val date = etDate.text.toString().trim()

            if (title.isEmpty()) {
                etExpenseName.error = "Enter expense name"
                return@setOnClickListener
            }

            if (amountText.isEmpty()) {
                etAmount.error = "Enter amount"
                return@setOnClickListener
            }

            val amount = amountText.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                etAmount.error = "Enter a valid amount"
                return@setOnClickListener
            }

            if (category.isEmpty()) {
                etCategory.error = "Select a category"
                return@setOnClickListener
            }

            if (date.isEmpty()) {
                etDate.error = "Enter date"
                return@setOnClickListener
            }

            val expense = ExpenseEntity(
                title = title,
                amount = amount,
                category = category,
                date = date
            )

            CoroutineScope(Dispatchers.IO).launch {
                database.expenseDao().insertExpense(expense)
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Expense saved successfully", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
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