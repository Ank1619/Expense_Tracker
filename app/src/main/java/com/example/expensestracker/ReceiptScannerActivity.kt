package com.example.expensestracker

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.expensestracker.data.local.BudgetManager
import com.example.expensestracker.data.local.ExpenseDatabase
import com.example.expensestracker.data.local.ExpenseEntity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReceiptScannerActivity : AppCompatActivity() {

    private lateinit var database: ExpenseDatabase
    private lateinit var budgetManager: BudgetManager

    private lateinit var ivReceiptImage: ImageView
    private lateinit var tvScanInstruction: TextView
    private lateinit var etReceiptMerchant: EditText
    private lateinit var etReceiptAmount: EditText
    private lateinit var etReceiptDate: EditText
    private lateinit var etReceiptCategory: AutoCompleteTextView

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            ivReceiptImage.setImageBitmap(bitmap)
            tvScanInstruction.visibility = View.GONE
            processReceiptBitmap(bitmap)
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(contentResolver, uri)
                }
                ivReceiptImage.setImageBitmap(bitmap)
                tvScanInstruction.visibility = View.GONE
                processReceiptBitmap(bitmap)
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receipt_scanner)

        database = ExpenseDatabase.getDatabase(this)
        budgetManager = BudgetManager(this)

        val btnBack = findViewById<ImageView>(R.id.btnBackReceipt)
        btnBack.setOnClickListener { finish() }

        ivReceiptImage = findViewById(R.id.ivReceiptImage)
        tvScanInstruction = findViewById(R.id.tvScanInstruction)
        etReceiptMerchant = findViewById(R.id.etReceiptMerchant)
        etReceiptAmount = findViewById(R.id.etReceiptAmount)
        etReceiptDate = findViewById(R.id.etReceiptDate)
        etReceiptCategory = findViewById(R.id.etReceiptCategory)

        val btnTakePhoto = findViewById<Button>(R.id.btnTakePhoto)
        val btnPickGallery = findViewById<Button>(R.id.btnPickGallery)
        val btnSave = findViewById<Button>(R.id.btnSaveReceiptExpense)

        btnTakePhoto.setOnClickListener { cameraLauncher.launch(null) }
        btnPickGallery.setOnClickListener { galleryLauncher.launch("image/*") }

        // Setup Category dropdown
        val categories = resources.getStringArray(R.array.expense_categories)
        val categoryAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            categories
        )
        etReceiptCategory.setAdapter(categoryAdapter)
        etReceiptCategory.setOnClickListener { etReceiptCategory.showDropDown() }

        // Pre-fill today's date
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        etReceiptDate.setText(dateFormat.format(Date()))

        etReceiptDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    val selectedCal = Calendar.getInstance()
                    selectedCal.set(year, month, day)
                    etReceiptDate.setText(dateFormat.format(selectedCal.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnSave.setOnClickListener { saveReceiptExpense() }
    }

    private fun processReceiptBitmap(bitmap: Bitmap) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap, 0)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                extractReceiptData(visionText.text)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Could not extract text from receipt", Toast.LENGTH_SHORT).show()
            }
    }

    private fun extractReceiptData(text: String) {
        if (text.isBlank()) return

        val lines = text.split("\n").map { it.trim() }.filter { it.isNotBlank() }

        // Extract Merchant Name (first non-numeric line)
        val merchantCandidate = lines.firstOrNull { line ->
            !line.contains(Regex("""\d{4,}""")) && line.length in 3..30
        }
        if (!merchantCandidate.isNullOrBlank()) {
            etReceiptMerchant.setText(merchantCandidate)
        }

        // Extract Amount (find highest matching monetary figure)
        val amountRegex = Regex("""(?:TOTAL|AMOUNT|BAL|NET|DUE|PAID)?\s*[₹Rs.]*\s*([0-9,]+(?:\.[0-9]{2})?)""", RegexOption.IGNORE_CASE)
        val amounts = mutableListOf<Double>()

        for (line in lines) {
            val match = amountRegex.find(line)
            if (match != null) {
                val valueStr = match.groupValues.getOrNull(1)?.replace(",", "")
                val value = valueStr?.toDoubleOrNull()
                if (value != null && value in 1.0..500000.0) {
                    amounts.add(value)
                }
            }
        }

        val detectedAmount = amounts.maxOrNull()
        if (detectedAmount != null) {
            etReceiptAmount.setText(String.format(Locale.getDefault(), "%.2f", detectedAmount))
        }

        // Extract Date
        val dateRegex = Regex("""\b([0-3]?[0-9][/\-.][0-1]?[0-9][/\-.][1-2][0-9]{3})\b""")
        for (line in lines) {
            val match = dateRegex.find(line)
            if (match != null) {
                val rawDate = match.groupValues[1].replace("/", "-").replace(".", "-")
                etReceiptDate.setText(rawDate)
                break
            }
        }

        Toast.makeText(this, "Receipt text extracted! Please verify.", Toast.LENGTH_SHORT).show()
    }

    private fun saveReceiptExpense() {
        val title = etReceiptMerchant.text.toString().trim()
        val amountText = etReceiptAmount.text.toString().trim()
        val category = etReceiptCategory.text.toString().trim()
        val date = etReceiptDate.text.toString().trim()

        if (title.isEmpty()) {
            etReceiptMerchant.error = "Enter merchant name"
            return
        }

        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            etReceiptAmount.error = "Enter valid amount"
            return
        }

        if (category.isEmpty()) {
            etReceiptCategory.error = "Select category"
            return
        }

        val expense = ExpenseEntity(
            title = title,
            amount = amount,
            category = category,
            date = date,
            paymentSource = "Receipt Scan",
            transactionType = "EXPENSE"
        )

        lifecycleScope.launch(Dispatchers.IO) {
            database.expenseDao().insertExpense(expense)

            val currentMonthKey = SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date())
            val allExpenses = database.expenseDao().getAllExpensesOnce()
            val currentMonthSpent = allExpenses
                .filter { exp ->
                    exp.transactionType == "EXPENSE" && getMonthYearKey(exp.date) == currentMonthKey
                }
                .sumOf { it.amount }

            budgetManager.checkAndNotifyBudgetThreshold(currentMonthSpent)

            withContext(Dispatchers.Main) {
                Toast.makeText(this@ReceiptScannerActivity, "Receipt Expense Saved!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun getMonthYearKey(dateStr: String): String {
        return try {
            val parts = dateStr.split("-")
            if (parts.size == 3) "${parts[1]}-${parts[2]}" else ""
        } catch (e: Exception) {
            ""
        }
    }
}