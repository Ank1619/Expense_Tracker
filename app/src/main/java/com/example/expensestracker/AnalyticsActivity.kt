package com.example.expensestracker

import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.expensestracker.data.ReportExportManager
import com.example.expensestracker.data.local.BudgetManager
import com.example.expensestracker.data.local.ExpenseDatabase
import com.example.expensestracker.data.local.ExpenseEntity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AnalyticsActivity : AppCompatActivity() {

    private lateinit var database: ExpenseDatabase
    private lateinit var budgetManager: BudgetManager

    private lateinit var tvAnalyticsTotalIncome: TextView
    private lateinit var tvAnalyticsTotal: TextView
    private lateinit var tvAnalyticsTopCategory: TextView
    private lateinit var tvAnalyticsAvgExpense: TextView
    private lateinit var tvSmartInsightsText: TextView

    private lateinit var chipAnalyticsMonth: TextView
    private lateinit var chipAnalyticsCategory: TextView
    private lateinit var chipAnalyticsClear: TextView

    private lateinit var btnAnalyticsSetBudget: TextView
    private lateinit var tvAnalyticsBudgetSpent: TextView
    private lateinit var progressAnalyticsBudget: ProgressBar
    private lateinit var layoutAnalyticsBudgetAlert: View
    private lateinit var tvAnalyticsBudgetAlert: TextView

    private lateinit var tvTrendTitle: TextView
    private lateinit var tvTrendSubtitle: TextView

    private lateinit var pieChartCategory: PieChart
    private lateinit var barChartTrend: BarChart

    private var rawExpenses: List<ExpenseEntity> = emptyList()

    private var selectedMonthKey: String = "All" // "MM-yyyy" or "All"
    private var selectedMonthLabel: String = "All Months"
    private var selectedCategory: String = "All"

    private val chartColors = listOf(
        Color.parseColor("#4F46E5"), // Indigo
        Color.parseColor("#10B981"), // Emerald
        Color.parseColor("#F59E0B"), // Amber
        Color.parseColor("#F43F5E"), // Rose
        Color.parseColor("#06B6D4"), // Cyan
        Color.parseColor("#8B5CF6"), // Purple
        Color.parseColor("#3B82F6"), // Blue
        Color.parseColor("#EC4899")  // Pink
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics)

        database = ExpenseDatabase.getDatabase(this)
        budgetManager = BudgetManager(this)

        val btnBack = findViewById<ImageView>(R.id.btnBackAnalytics)
        btnBack.setOnClickListener { finish() }

        val btnExportReport = findViewById<Button>(R.id.btnExportReport)
        btnExportReport.setOnClickListener { showExportDialog() }

        tvAnalyticsTotalIncome = findViewById(R.id.tvAnalyticsTotalIncome)
        tvAnalyticsTotal = findViewById(R.id.tvAnalyticsTotal)
        tvAnalyticsTopCategory = findViewById(R.id.tvAnalyticsTopCategory)
        tvAnalyticsAvgExpense = findViewById(R.id.tvAnalyticsAvgExpense)
        tvSmartInsightsText = findViewById(R.id.tvSmartInsightsText)

        chipAnalyticsMonth = findViewById(R.id.chipAnalyticsMonth)
        chipAnalyticsCategory = findViewById(R.id.chipAnalyticsCategory)
        chipAnalyticsClear = findViewById(R.id.chipAnalyticsClear)

        btnAnalyticsSetBudget = findViewById(R.id.btnAnalyticsSetBudget)
        tvAnalyticsBudgetSpent = findViewById(R.id.tvAnalyticsBudgetSpent)
        progressAnalyticsBudget = findViewById(R.id.progressAnalyticsBudget)
        layoutAnalyticsBudgetAlert = findViewById(R.id.layoutAnalyticsBudgetAlert)
        tvAnalyticsBudgetAlert = findViewById(R.id.tvAnalyticsBudgetAlert)

        tvTrendTitle = findViewById(R.id.tvTrendTitle)
        tvTrendSubtitle = findViewById(R.id.tvTrendSubtitle)

        pieChartCategory = findViewById(R.id.pieChartCategory)
        barChartTrend = findViewById(R.id.barChartTrend)

        setupChartConfigurations()

        chipAnalyticsMonth.setOnClickListener { showMonthSelectionDialog() }
        chipAnalyticsCategory.setOnClickListener { showCategorySelectionDialog() }
        chipAnalyticsClear.setOnClickListener { clearAnalyticsFilters() }
        btnAnalyticsSetBudget.setOnClickListener { showSetBudgetDialog() }

        loadAnalyticsData()
    }

    private fun setupChartConfigurations() {
        // Pie Chart Config with Outside Labels
        pieChartCategory.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            setTransparentCircleAlpha(0)
            holeRadius = 48f
            setCenterTextSize(11f)
            setCenterTextColor(ContextCompat.getColor(this@AnalyticsActivity, R.color.text_dark))
            setExtraOffsets(28f, 12f, 28f, 12f)
            setEntryLabelColor(ContextCompat.getColor(this@AnalyticsActivity, R.color.text_dark))
            setEntryLabelTextSize(11f)

            legend.apply {
                isEnabled = true
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                isWordWrapEnabled = true
                textColor = ContextCompat.getColor(this@AnalyticsActivity, R.color.text_dark)
                textSize = 10f
            }
        }

        // Bar Chart Config
        barChartTrend.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)
            setPinchZoom(false)
            setScaleEnabled(false)
            legend.isEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = ContextCompat.getColor(this@AnalyticsActivity, R.color.text_muted)
                textSize = 10f
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = ContextCompat.getColor(this@AnalyticsActivity, R.color.border_color)
                axisMinimum = 0f
                textColor = ContextCompat.getColor(this@AnalyticsActivity, R.color.text_muted)
                textSize = 10f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return if (value >= 1000) {
                            "₹${(value / 1000).toInt()}k"
                        } else {
                            "₹${value.toInt()}"
                        }
                    }
                }
            }

            axisRight.isEnabled = false
        }
    }

    private fun loadAnalyticsData() {
        lifecycleScope.launch {
            database.expenseDao().getAllExpenses().collect { expenses ->
                rawExpenses = expenses
                updateAnalyticsUI()
            }
        }
    }

    private fun updateAnalyticsUI() {
        var filteredList = rawExpenses

        if (selectedMonthKey != "All") {
            filteredList = filteredList.filter { getMonthYearKey(it.date) == selectedMonthKey }
        }

        if (selectedCategory != "All") {
            filteredList = filteredList.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }

        val expenseList = filteredList.filter { it.transactionType != "INCOME" }
        val incomeList = filteredList.filter { it.transactionType == "INCOME" }

        val totalSpending = expenseList.sumOf { it.amount }
        val totalIncome = incomeList.sumOf { it.amount }
        val count = expenseList.size
        val avgExpense = if (count > 0) totalSpending / count else 0.0

        val categoryTotals = expenseList
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val topCategoryEntry = categoryTotals.maxByOrNull { it.value }
        val topCategoryText = topCategoryEntry?.key ?: "No Data"

        // Update Overview Cards
        tvAnalyticsTotalIncome.text = String.format(Locale.getDefault(), "₹%.2f", totalIncome)
        tvAnalyticsTotal.text = String.format(Locale.getDefault(), "₹%.2f", totalSpending)
        tvAnalyticsTopCategory.text = topCategoryText
        tvAnalyticsAvgExpense.text = String.format(Locale.getDefault(), "₹%.2f", avgExpense)

        // Smart Insights Text
        val insights = StringBuilder()
        insights.append("• Daily Average Spending: ₹").append(String.format(Locale.getDefault(), "%.1f", totalSpending / 30.0)).append("/day\n")
        if (topCategoryEntry != null) {
            val percent = if (totalSpending > 0) (topCategoryEntry.value / totalSpending * 100).toInt() else 0
            insights.append("• Highest Category: ").append(topCategoryEntry.key).append(" (").append(percent).append("% of expenses)\n")
        }
        if (totalIncome > 0) {
            val savingsRate = (((totalIncome - totalSpending) / totalIncome) * 100).toInt()
            insights.append("• Net Savings Rate: ").append(savingsRate).append("% of income saved")
        } else {
            insights.append("• Total Expenses Tracked: ₹").append(String.format(Locale.getDefault(), "%.2f", totalSpending))
        }
        tvSmartInsightsText.text = insights.toString()

        // Update Budget Card in Analytics
        updateAnalyticsBudgetProgress(totalSpending)

        // Render Charts
        renderPieChart(categoryTotals, totalSpending)
        renderTrendBarChart(expenseList)

        // Update Chips Appearance
        updateChipStyles()
    }

    private fun updateAnalyticsBudgetProgress(currentSpent: Double) {
        val targetBudget = budgetManager.getMonthlyBudget()

        if (targetBudget <= 0.0) {
            btnAnalyticsSetBudget.text = "Set Budget ✏️"
            tvAnalyticsBudgetSpent.text = "Set a monthly budget to track spending"
            progressAnalyticsBudget.progress = 0
            layoutAnalyticsBudgetAlert.visibility = View.GONE
            return
        }

        btnAnalyticsSetBudget.text = "Edit Budget ✏️"
        val usagePercentage = (currentSpent / targetBudget * 100).toInt()
        progressAnalyticsBudget.progress = usagePercentage.coerceAtMost(100)

        tvAnalyticsBudgetSpent.text = String.format(
            Locale.getDefault(),
            "Spent ₹%.2f of ₹%.2f (%d%%)",
            currentSpent,
            targetBudget,
            usagePercentage
        )

        when {
            usagePercentage >= 100 -> {
                layoutAnalyticsBudgetAlert.visibility = View.VISIBLE
                layoutAnalyticsBudgetAlert.setBackgroundResource(R.drawable.bg_red_badge)
                val excess = currentSpent - targetBudget
                tvAnalyticsBudgetAlert.text = String.format(
                    Locale.getDefault(),
                    "🚨 Alert: Budget exceeded by ₹%.2f!",
                    excess
                )
                tvAnalyticsBudgetAlert.setTextColor(ContextCompat.getColor(this, R.color.red_primary))
            }
            usagePercentage >= 80 -> {
                layoutAnalyticsBudgetAlert.visibility = View.VISIBLE
                layoutAnalyticsBudgetAlert.setBackgroundResource(R.drawable.bg_pending_badge)
                tvAnalyticsBudgetAlert.text = "⚠️ Warning: You have used $usagePercentage% of monthly budget!"
                tvAnalyticsBudgetAlert.setTextColor(ContextCompat.getColor(this, R.color.amber_primary))
            }
            else -> {
                layoutAnalyticsBudgetAlert.visibility = View.VISIBLE
                layoutAnalyticsBudgetAlert.setBackgroundResource(R.drawable.bg_chip_selected)
                val remaining = targetBudget - currentSpent
                tvAnalyticsBudgetAlert.text = String.format(
                    Locale.getDefault(),
                    "Remaining: ₹%.2f for this period",
                    remaining
                )
                tvAnalyticsBudgetAlert.setTextColor(ContextCompat.getColor(this, R.color.primary))
            }
        }
    }

    private fun renderPieChart(categoryTotals: Map<String, Double>, totalSpending: Double) {
        if (categoryTotals.isEmpty()) {
            pieChartCategory.clear()
            pieChartCategory.centerText = "No Expenses\nFound"
            return
        }

        val entries = mutableListOf<PieEntry>()
        for ((category, amount) in categoryTotals) {
            entries.add(PieEntry(amount.toFloat(), category))
        }

        val dataSet = PieDataSet(entries, "").apply {
            colors = chartColors
            sliceSpace = 4f

            // Position Value Labels OUTSIDE Pie Chart Slices
            xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            valueLinePart1OffsetPercentage = 80f
            valueLinePart1Length = 0.35f
            valueLinePart2Length = 0.45f
            valueLineColor = ContextCompat.getColor(this@AnalyticsActivity, R.color.text_dark)

            valueTextSize = 11f
            valueTypeface = Typeface.DEFAULT_BOLD
            valueTextColor = ContextCompat.getColor(this@AnalyticsActivity, R.color.text_dark)
            valueFormatter = PercentFormatter(pieChartCategory)
        }

        val data = PieData(dataSet)
        pieChartCategory.data = data
        pieChartCategory.centerText = String.format(Locale.getDefault(), "Total\n₹%.0f", totalSpending)
        pieChartCategory.animateY(800)
        pieChartCategory.invalidate()
    }

    private fun renderTrendBarChart(filteredList: List<ExpenseEntity>) {
        if (filteredList.isEmpty()) {
            barChartTrend.clear()
            tvTrendTitle.text = "Spending Trend"
            tvTrendSubtitle.text = "No data available for selected filters"
            return
        }

        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()

        if (selectedMonthKey != "All") {
            tvTrendTitle.text = "Daily Trend ($selectedMonthLabel)"
            tvTrendSubtitle.text = "Daily spending breakdown for the month"

            val dailyGroup = filteredList
                .groupBy { it.date }
                .mapValues { entry -> entry.value.sumOf { it.amount } }

            val sortedDates = dailyGroup.keys.sortedBy { parseDate(it).time }

            sortedDates.forEachIndexed { index, dateStr ->
                val sum = dailyGroup[dateStr] ?: 0.0
                entries.add(BarEntry(index.toFloat(), sum.toFloat()))
                labels.add(formatDayMonthLabel(dateStr))
            }
        } else {
            tvTrendTitle.text = "Monthly Spending Trend"
            tvTrendSubtitle.text = "Month-over-month total expenses"

            val monthlyGroup = filteredList
                .groupBy { getMonthYearKey(it.date) }
                .mapValues { entry -> entry.value.sumOf { it.amount } }

            val sortedMonthKeys = monthlyGroup.keys.filter { it.isNotBlank() }.sortedBy {
                parseMonthYearKey(it).time
            }

            sortedMonthKeys.forEachIndexed { index, monthKey ->
                val sum = monthlyGroup[monthKey] ?: 0.0
                entries.add(BarEntry(index.toFloat(), sum.toFloat()))
                labels.add(getMonthYearLabel(monthKey))
            }
        }

        val dataSet = BarDataSet(entries, "Spending").apply {
            color = ContextCompat.getColor(this@AnalyticsActivity, R.color.primary)
            valueTextColor = ContextCompat.getColor(this@AnalyticsActivity, R.color.text_dark)
            valueTextSize = 10f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (value > 0) "₹${value.toInt()}" else ""
                }
            }
        }

        barChartTrend.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val idx = value.toInt()
                return if (idx >= 0 && idx < labels.size) labels[idx] else ""
            }
        }

        barChartTrend.data = BarData(dataSet).apply {
            barWidth = 0.5f
        }
        barChartTrend.animateY(800)
        barChartTrend.invalidate()
    }

    private fun showExportDialog() {
        val options = arrayOf("📄 Export CSV Spreadsheet", "📊 Export PDF Report")
        AlertDialog.Builder(this)
            .setTitle("Export Financial Report")
            .setItems(options) { _, which ->
                val exportManager = ReportExportManager(this)
                if (which == 0) {
                    exportManager.exportCSV(rawExpenses)
                } else {
                    exportManager.exportPDF(rawExpenses)
                }
            }
            .show()
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
            updateAnalyticsUI()
        }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun updateChipStyles() {
        if (selectedMonthKey != "All") {
            chipAnalyticsMonth.text = "📅 $selectedMonthLabel"
            chipAnalyticsMonth.setBackgroundResource(R.drawable.bg_chip_selected)
            chipAnalyticsMonth.setTextColor(ContextCompat.getColor(this, R.color.primary))
        } else {
            chipAnalyticsMonth.text = getString(R.string.filter_all_months)
            chipAnalyticsMonth.setBackgroundResource(R.drawable.bg_chip_normal)
            chipAnalyticsMonth.setTextColor(ContextCompat.getColor(this, R.color.text_dark))
        }

        if (selectedCategory != "All") {
            chipAnalyticsCategory.text = "📂 $selectedCategory"
            chipAnalyticsCategory.setBackgroundResource(R.drawable.bg_chip_selected)
            chipAnalyticsCategory.setTextColor(ContextCompat.getColor(this, R.color.primary))
        } else {
            chipAnalyticsCategory.text = getString(R.string.filter_all_categories)
            chipAnalyticsCategory.setBackgroundResource(R.drawable.bg_chip_normal)
            chipAnalyticsCategory.setTextColor(ContextCompat.getColor(this, R.color.text_dark))
        }

        val isFiltered = selectedMonthKey != "All" || selectedCategory != "All"
        chipAnalyticsClear.visibility = if (isFiltered) View.VISIBLE else View.GONE
    }

    private fun clearAnalyticsFilters() {
        selectedMonthKey = "All"
        selectedMonthLabel = "All Months"
        selectedCategory = "All"
        updateAnalyticsUI()
    }

    private fun showMonthSelectionDialog() {
        val monthKeys = rawExpenses
            .map { getMonthYearKey(it.date) }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedByDescending { parseMonthYearKey(it).time }

        val labels = mutableListOf<String>()
        val keyList = mutableListOf<String>()

        labels.add("📅 All Months")
        keyList.add("All")

        for (key in monthKeys) {
            labels.add(getMonthYearLabel(key))
            keyList.add(key)
        }

        AlertDialog.Builder(this)
            .setTitle("Select Month for Analytics")
            .setItems(labels.toTypedArray()) { _, which ->
                selectedMonthKey = keyList[which]
                selectedMonthLabel = labels[which].replace("📅 ", "")
                updateAnalyticsUI()
            }
            .show()
    }

    private fun showCategorySelectionDialog() {
        val categories = resources.getStringArray(R.array.expense_categories).toMutableList()
        categories.add(0, "All Categories")

        AlertDialog.Builder(this)
            .setTitle("Filter Analytics by Category")
            .setItems(categories.toTypedArray()) { _, which ->
                selectedCategory = if (which == 0) "All" else categories[which]
                updateAnalyticsUI()
            }
            .show()
    }

    private fun getMonthYearKey(dateStr: String): String {
        return try {
            val parts = dateStr.split("-")
            if (parts.size == 3) "${parts[1]}-${parts[2]}" else ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun getMonthYearLabel(monthKey: String): String {
        return try {
            val date = SimpleDateFormat("MM-yyyy", Locale.getDefault()).parse(monthKey)
            if (date != null) SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date) else monthKey
        } catch (e: Exception) {
            monthKey
        }
    }

    private fun formatDayMonthLabel(dateStr: String): String {
        return try {
            val date = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).parse(dateStr)
            if (date != null) SimpleDateFormat("dd MMM", Locale.getDefault()).format(date) else dateStr
        } catch (e: Exception) {
            dateStr
        }
    }

    private fun parseDate(dateStr: String): Date {
        return try {
            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).parse(dateStr) ?: Date(0)
        } catch (e: Exception) {
            Date(0)
        }
    }

    private fun parseMonthYearKey(monthKey: String): Date {
        return try {
            SimpleDateFormat("MM-yyyy", Locale.getDefault()).parse(monthKey) ?: Date(0)
        } catch (e: Exception) {
            Date(0)
        }
    }
}