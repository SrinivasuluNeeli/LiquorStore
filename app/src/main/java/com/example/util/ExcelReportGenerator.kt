package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.DailyExpense
import com.example.data.model.DaySummary
import com.example.data.model.StockReceipt
import com.example.ui.viewmodel.CategoryStockMovementItem
import com.example.ui.viewmodel.StockRecordItemUiState
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelReportGenerator {

    private fun escapeCsv(value: Any?): String {
        if (value == null) return "\"\""
        val str = value.toString()
        return "\"${str.replace("\"", "\"\"")}\""
    }

    /**
     * Generates a comprehensive, formatted Excel/CSV spreadsheet report with
     * executive summary, cash reconciliation, category breakdown, itemized stock movements, and expense log.
     */
    fun generateDailyExcelReport(
        context: Context,
        selectedDate: String,
        daySummary: DaySummary?,
        recordsList: List<StockRecordItemUiState>,
        categoryMovements: List<CategoryStockMovementItem>,
        dailyExpenses: List<DailyExpense>,
        stockReceipts: List<StockReceipt>,
        role: String = "Admin"
    ): File? {
        val reportsDir = File(context.cacheDir, "reports").apply { if (!exists()) mkdirs() }
        val sanitizedDate = selectedDate.replace("-", "_")
        val outputFile = File(reportsDir, "Liquor_Daily_Stock_Sheet_$sanitizedDate.csv")

        val generatedAt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())

        val totalRevenue = recordsList.sumOf { it.record.totalSalesValue }
        val totalCost = recordsList.sumOf { it.record.salesQty * it.record.costPrice }
        val totalGrossProfit = totalRevenue - totalCost
        val overallMargin = if (totalRevenue > 0) (totalGrossProfit / totalRevenue * 100) else 0.0

        val totalOpen = recordsList.sumOf { it.record.openingStock }
        val totalRecv = recordsList.sumOf { it.record.receivedQty }
        val totalBreakage = recordsList.sumOf { it.record.breakageQty }
        val totalAvailable = recordsList.sumOf { it.record.totalAvailable }
        val totalClosing = recordsList.sumOf { it.record.closingStock }
        val totalSold = recordsList.sumOf { it.record.salesQty }

        val totalValuationMRP = recordsList.sumOf { it.record.closingStock * it.record.rate }
        val totalValuationCost = recordsList.sumOf { it.record.closingStock * it.record.costPrice }

        val dayExpensesList = dailyExpenses.filter { it.date == selectedDate }
        val totalExpenses = dayExpensesList.sumOf { it.amount }
        val netContribution = totalGrossProfit - totalExpenses

        val isClosed = daySummary?.isClosed == true
        val statusText = if (isClosed) "DAY CLOSED (AUDITED)" else "DAY OPEN (ACTIVE REGISTER)"

        return try {
            val fos = FileOutputStream(outputFile)
            // Write UTF-8 BOM so Excel opens Hindi/Special symbols correctly
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            val writer = OutputStreamWriter(fos, StandardCharsets.UTF_8)

            // 1. HEADER SECTION
            writer.write("${escapeCsv("LIQUOR INVENTORY & DAILY SALES MANAGEMENT")},,,,,,,,,,,,\n")
            writer.write("${escapeCsv("DAILY STOCK SHEET & AUDIT RECONCILIATION")},,,,,,,,,,,,\n")
            writer.write("${escapeCsv("Date: $selectedDate")},${escapeCsv("Status: $statusText")},${escapeCsv("Generated: $generatedAt")},${escapeCsv("User Role: $role")},,,,,,,,,\n")
            writer.write("\n")

            // 2. EXECUTIVE FINANCIAL & PERFORMANCE SUMMARY
            writer.write("${escapeCsv("=== 1. EXECUTIVE FINANCIAL SUMMARY ===")},,,,,,,,,,,,\n")
            writer.write("${escapeCsv("Metric")},${escapeCsv("Value")},${escapeCsv("Unit / Description")},,,,,,,,,,\n")
            writer.write("${escapeCsv("Gross Sales Turnover (MRP)")},${escapeCsv("%.2f".format(totalRevenue))},${escapeCsv("INR (₹)")},,,,,,,,,,\n")
            writer.write("${escapeCsv("Cost of Goods Sold (COGS)")},${escapeCsv("%.2f".format(totalCost))},${escapeCsv("INR (₹)")},,,,,,,,,,\n")
            writer.write("${escapeCsv("Estimated Gross Profit")},${escapeCsv("%.2f".format(totalGrossProfit))},${escapeCsv("INR (₹)")},,,,,,,,,,\n")
            writer.write("${escapeCsv("Gross Profit Margin")},${escapeCsv("%.1f%%".format(overallMargin))},${escapeCsv("Percentage of Sales")},,,,,,,,,,\n")
            writer.write("${escapeCsv("Total Physical Units Sold")},${escapeCsv(totalSold)},${escapeCsv("Bottles / Cans")},,,,,,,,,,\n")
            writer.write("${escapeCsv("Total Breakage / Transit Loss")},${escapeCsv(totalBreakage)},${escapeCsv("Bottles")},,,,,,,,,,\n")
            writer.write("${escapeCsv("Closing Stock Valuation (Retail MRP)")},${escapeCsv("%.2f".format(totalValuationMRP))},${escapeCsv("INR (₹)")},,,,,,,,,,\n")
            writer.write("${escapeCsv("Closing Stock Valuation (Cost Value)")},${escapeCsv("%.2f".format(totalValuationCost))},${escapeCsv("INR (₹)")},,,,,,,,,,\n")
            writer.write("${escapeCsv("Total Store Expenses Logged")},${escapeCsv("%.2f".format(totalExpenses))},${escapeCsv("INR (₹)")},,,,,,,,,,\n")
            writer.write("${escapeCsv("Net Operating Contribution")},${escapeCsv("%.2f".format(netContribution))},${escapeCsv("Gross Profit - Expenses")},,,,,,,,,,\n")
            writer.write("\n")

            // 3. CASH & SETTLEMENT RECONCILIATION
            writer.write("${escapeCsv("=== 2. DAILY CASH & SETTLEMENT RECONCILIATION ===")},,,,,,,,,,,,\n")
            writer.write("${escapeCsv("Settlement Channel")},${escapeCsv("Amount (INR)")},${escapeCsv("Notes")},,,,,,,,,,\n")
            val cashCol = daySummary?.cashCollected ?: 0.0
            val upiCol = daySummary?.upiCollected ?: 0.0
            val creditCol = daySummary?.creditSales ?: 0.0
            val netCash = cashCol - totalExpenses
            writer.write("${escapeCsv("Cash Collected at Register")},${escapeCsv("%.2f".format(cashCol))},${escapeCsv("Physical cash received")},,,,,,,,,,\n")
            writer.write("${escapeCsv("UPI / Digital Transactions")},${escapeCsv("%.2f".format(upiCol))},${escapeCsv("QR / POS machine receipts")},,,,,,,,,,\n")
            writer.write("${escapeCsv("Credit / Khata Sales")},${escapeCsv("%.2f".format(creditCol))},${escapeCsv("Customer book receivables")},,,,,,,,,,\n")
            writer.write("${escapeCsv("Total Sales Collected")},${escapeCsv("%.2f".format(cashCol + upiCol + creditCol))},${escapeCsv("Total recorded collection")},,,,,,,,,,\n")
            writer.write("${escapeCsv("Total Daily Expenses Deducted")},${escapeCsv("%.2f".format(totalExpenses))},${escapeCsv("Store petty cash / bills")},,,,,,,,,,\n")
            writer.write("${escapeCsv("Net Cash in Hand for Bank Deposit")},${escapeCsv("%.2f".format(netCash))},${escapeCsv("Cash Collected minus Expenses")},,,,,,,,,,\n")
            writer.write("\n")

            // 4. CATEGORY-WISE PERFORMANCE BREAKDOWN
            writer.write("${escapeCsv("=== 3. CATEGORY-WISE STOCK MOVEMENT & SALES ===")},,,,,,,,,,,,\n")
            writer.write("${escapeCsv("Category")},${escapeCsv("Opening Units")},${escapeCsv("Received")},${escapeCsv("Sold Units")},${escapeCsv("Breakage")},${escapeCsv("Closing Stock")},${escapeCsv("Sales Turnover (₹)")},${escapeCsv("Share %")},,,,,,\n")
            for (cm in categoryMovements) {
                val share = if (totalRevenue > 0) (cm.salesValue / totalRevenue * 100) else 0.0
                writer.write("${escapeCsv(cm.category)},${escapeCsv(cm.openingStock)},${escapeCsv(cm.receivedQty)},${escapeCsv(cm.salesQty)},${escapeCsv(cm.breakageQty)},${escapeCsv(cm.closingStock)},${escapeCsv("%.2f".format(cm.salesValue))},${escapeCsv("%.1f%%".format(share))},,,,,,\n")
            }
            writer.write("\n")

            // 5. DETAILED ITEM-WISE STOCK SHEET
            writer.write("${escapeCsv("=== 4. DETAILED ITEM-WISE STOCK MASTER REGISTER ===")},,,,,,,,,,,,,,,,,\n")
            writer.write("${escapeCsv("Sl No")},${escapeCsv("Category")},${escapeCsv("Brand")},${escapeCsv("Product Name")},${escapeCsv("Bottle Size")},${escapeCsv("Selling Rate (MRP)")},${escapeCsv("Cost Price")},${escapeCsv("Opening Stock")},${escapeCsv("Received Qty")},${escapeCsv("Breakage Qty")},${escapeCsv("Total Available")},${escapeCsv("Closing Stock")},${escapeCsv("Sales Qty")},${escapeCsv("Sales Turnover (₹)")},${escapeCsv("Gross Profit (₹)")},${escapeCsv("Closing Stock Value (₹)")},${escapeCsv("Reorder Point")},${escapeCsv("Inventory Status")}\n")

            // Sort grouped by category, SKU brand/name, and bottle size descending
            val categoryOrder = listOf("Whisky", "Beer", "Brandy", "Rum", "Vodka", "Wine")
            fun parseSize(s: String): Int = s.filter { it.isDigit() }.toIntOrNull() ?: 0

            val sortedRecords = recordsList.sortedWith(
                compareBy<StockRecordItemUiState> { item ->
                    val idx = categoryOrder.indexOfFirst { it.equals(item.product.category, ignoreCase = true) }
                    if (idx >= 0) idx else 999
                }
                .thenBy { it.product.brand.lowercase() }
                .thenBy { it.product.name.lowercase() }
                .thenByDescending { parseSize(it.product.bottleSizeMl) }
            )

            var index = 1
            for (item in sortedRecords) {
                val p = item.product
                val r = item.record
                val lineValuation = r.closingStock * r.rate
                val status = when {
                    r.closingStock == 0 -> "OUT OF STOCK"
                    r.closingStock <= p.minStock -> "LOW STOCK"
                    r.breakageQty > 0 -> "BREAKAGE REPORTED"
                    r.closingStock > (r.openingStock + r.receivedQty) -> "DISCREPANCY (OVERSTOCK)"
                    else -> "HEALTHY"
                }

                writer.write("${escapeCsv(index++)},")
                writer.write("${escapeCsv(p.category)},")
                writer.write("${escapeCsv(p.brand)},")
                writer.write("${escapeCsv(p.name)},")
                writer.write("${escapeCsv(p.bottleSizeMl)},")
                writer.write("${escapeCsv("%.2f".format(r.rate))},")
                writer.write("${escapeCsv("%.2f".format(r.costPrice))},")
                writer.write("${escapeCsv(r.openingStock)},")
                writer.write("${escapeCsv(r.receivedQty)},")
                writer.write("${escapeCsv(r.breakageQty)},")
                writer.write("${escapeCsv(r.totalAvailable)},")
                writer.write("${escapeCsv(r.closingStock)},")
                writer.write("${escapeCsv(r.salesQty)},")
                writer.write("${escapeCsv("%.2f".format(r.totalSalesValue))},")
                writer.write("${escapeCsv("%.2f".format(r.grossProfit))},")
                writer.write("${escapeCsv("%.2f".format(lineValuation))},")
                writer.write("${escapeCsv(p.minStock)},")
                writer.write("${escapeCsv(status)}\n")
            }

            writer.write("\n")

            // 6. DAILY EXPENSES LOG
            writer.write("${escapeCsv("=== 5. DAILY STORE EXPENSES LOG ===")},,,,\n")
            writer.write("${escapeCsv("Expense ID")},${escapeCsv("Category")},${escapeCsv("Amount (₹)")},${escapeCsv("Payment Mode")},${escapeCsv("Remarks / Description")}\n")
            if (dayExpensesList.isEmpty()) {
                writer.write("${escapeCsv("No expenses recorded for this date")},,,,\n")
            } else {
                for (exp in dayExpensesList) {
                    writer.write("${escapeCsv(exp.id)},${escapeCsv(exp.category)},${escapeCsv("%.2f".format(exp.amount))},${escapeCsv(exp.paymentMode)},${escapeCsv(exp.remarks)}\n")
                }
            }

            writer.flush()
            writer.close()
            fos.close()
            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun openExcelFile(context: Context, file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/comma-separated-values")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open Stock Sheet"))
        } catch (e: Exception) {
            Toast.makeText(context, "No app available to view CSV/Excel files: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun shareExcelFile(context: Context, file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/comma-separated-values"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Daily Liquor Stock Sheet - ${file.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Excel Stock Sheet"))
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to share file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
