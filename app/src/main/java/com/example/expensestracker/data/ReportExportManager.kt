package com.example.expensestracker.data

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.expensestracker.data.local.ExpenseEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportExportManager(private val context: Context) {

    fun exportCSV(expenses: List<ExpenseEntity>) {
        try {
            val fileName = "Expense_Report_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)

            val csvHeader = "ID,Title,Amount,Category,Date,Type,PaymentSource,Notes\n"
            outputStream.write(csvHeader.toByteArray())

            for (exp in expenses) {
                val line = "${exp.id},\"${exp.title.replace("\"", "\"\"")}\",${exp.amount},\"${exp.category}\",\"${exp.date}\",\"${exp.transactionType}\",\"${exp.paymentSource}\",\"${exp.note.replace("\"", "\"\"")}\"\n"
                outputStream.write(line.toByteArray())
            }

            outputStream.close()
            shareFile(file, "text/csv", "Share Expense CSV Report")

        } catch (e: Exception) {
            Toast.makeText(context, "Failed to export CSV: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun exportPDF(expenses: List<ExpenseEntity>) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint()
            val titlePaint = Paint().apply {
                color = Color.parseColor("#4F46E5")
                textSize = 20f
                isFakeBoldText = true
            }

            val textPaint = Paint().apply {
                color = Color.parseColor("#0F172A")
                textSize = 12f
            }

            val headerPaint = Paint().apply {
                color = Color.parseColor("#64748B")
                textSize = 11f
                isFakeBoldText = true
            }

            // Draw Header
            canvas.drawText("Expense Tracker - Financial Report", 36f, 40f, titlePaint)
            val dateStr = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()).format(Date())
            canvas.drawText("Generated on: $dateStr", 36f, 60f, headerPaint)

            // Draw Summary Box
            val totalExpense = expenses.filter { it.transactionType != "INCOME" }.sumOf { it.amount }
            val totalIncome = expenses.filter { it.transactionType == "INCOME" }.sumOf { it.amount }

            paint.color = Color.parseColor("#EEF2FF")
            canvas.drawRect(36f, 75f, 559f, 125f, paint)

            textPaint.isFakeBoldText = true
            canvas.drawText(String.format(Locale.getDefault(), "Total Expenses: ₹%.2f   |   Total Income: ₹%.2f   |   Net: ₹%.2f", totalExpense, totalIncome, totalIncome - totalExpense), 50f, 105f, textPaint)
            textPaint.isFakeBoldText = false

            // Draw Table Headers
            var yY = 150f
            paint.color = Color.parseColor("#F1F5F9")
            canvas.drawRect(36f, yY - 15f, 559f, yY + 10f, paint)

            canvas.drawText("Date", 42f, yY, headerPaint)
            canvas.drawText("Merchant / Title", 120f, yY, headerPaint)
            canvas.drawText("Category", 280f, yY, headerPaint)
            canvas.drawText("Source", 400f, yY, headerPaint)
            canvas.drawText("Amount", 480f, yY, headerPaint)

            yY += 25f

            for (exp in expenses.take(25)) { // First 25 items on page
                val amountStr = if (exp.transactionType == "INCOME") "+₹${exp.amount}" else "₹${exp.amount}"
                canvas.drawText(exp.date, 42f, yY, textPaint)

                val shortTitle = if (exp.title.length > 20) exp.title.substring(0, 18) + ".." else exp.title
                canvas.drawText(shortTitle, 120f, yY, textPaint)
                canvas.drawText(exp.category, 280f, yY, textPaint)
                canvas.drawText(exp.paymentSource, 400f, yY, textPaint)
                canvas.drawText(amountStr, 480f, yY, textPaint)

                yY += 22f
            }

            pdfDocument.finishPage(page)

            val fileName = "Expense_Report_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf"
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)

            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()

            shareFile(file, "application/pdf", "Share Expense PDF Report")

        } catch (e: Exception) {
            Toast.makeText(context, "Failed to export PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareFile(file: File, mimeType: String, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }
}