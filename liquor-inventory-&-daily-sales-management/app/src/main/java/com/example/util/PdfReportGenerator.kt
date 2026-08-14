package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.DailyExpense
import com.example.data.model.DaySummary
import com.example.data.model.StockReceipt
import com.example.ui.viewmodel.CategoryStockMovementItem
import com.example.ui.viewmodel.StockRecordItemUiState
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    private const val PAGE_WIDTH = 595 // Standard A4 width in points (72 dpi)
    private const val PAGE_HEIGHT = 842 // Standard A4 height in points (72 dpi)
    private const val MARGIN = 36f // 0.5 inch margin

    /**
     * Generates a native printable PDF report for daily accounting and stock audit records.
     */
    fun generateDailySummaryPdf(
        context: Context,
        selectedDate: String,
        daySummary: DaySummary?,
        recordsList: List<StockRecordItemUiState>,
        categoryMovements: List<CategoryStockMovementItem>,
        dailyExpenses: List<DailyExpense>,
        stockReceipts: List<StockReceipt>,
        role: String = "Admin"
    ): File? {
        val pdfDocument = PdfDocument()
        val generatedAt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())

        val totalEstRevenue = recordsList.sumOf { it.record.totalSalesValue }
        val totalEstCost = recordsList.sumOf { it.record.salesQty * it.record.costPrice }
        val totalEstProfit = totalEstRevenue - totalEstCost
        val totalUnitsSold = recordsList.sumOf { it.record.salesQty }
        val totalOpeningUnits = recordsList.sumOf { it.record.openingStock }
        val totalReceivedUnits = recordsList.sumOf { it.record.receivedQty }
        val totalClosingUnits = recordsList.sumOf { it.record.closingStock }
        val totalBreakageUnits = recordsList.sumOf { it.record.breakageQty }
        val totalStockValuationMRP = recordsList.sumOf { it.record.closingStock * it.record.rate }
        val totalStockValuationCost = recordsList.sumOf { it.record.closingStock * it.record.costPrice }

        val dayExpensesList = dailyExpenses.filter { it.date == selectedDate }
        val totalDailyExpenses = dayExpensesList.sumOf { it.amount }
        val netContribution = totalEstProfit - totalDailyExpenses
        val profitMarginPct = if (totalEstRevenue > 0) ((totalEstProfit / totalEstRevenue) * 100).toInt() else 0

        // ================= PAGE 1: EXECUTIVE ACCOUNTING SUMMARY & CATEGORY BREAKDOWN =================
        val pageInfo1 = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page1 = pdfDocument.startPage(pageInfo1)
        val canvas1 = page1.canvas

        drawPage1(
            canvas = canvas1,
            selectedDate = selectedDate,
            generatedAt = generatedAt,
            daySummary = daySummary,
            totalEstRevenue = totalEstRevenue,
            totalEstCost = totalEstCost,
            totalEstProfit = totalEstProfit,
            profitMarginPct = profitMarginPct,
            totalUnitsSold = totalUnitsSold,
            totalOpeningUnits = totalOpeningUnits,
            totalReceivedUnits = totalReceivedUnits,
            totalClosingUnits = totalClosingUnits,
            totalBreakageUnits = totalBreakageUnits,
            totalStockValuationMRP = totalStockValuationMRP,
            totalStockValuationCost = totalStockValuationCost,
            totalDailyExpenses = totalDailyExpenses,
            netContribution = netContribution,
            categoryMovements = categoryMovements,
            recordsList = recordsList,
            dayExpensesList = dayExpensesList,
            role = role
        )
        pdfDocument.finishPage(page1)

        // ================= PAGE 2: DETAILED ITEM-WISE STOCK & AUDIT LEDGER =================
        val pageInfo2 = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create()
        val page2 = pdfDocument.startPage(pageInfo2)
        val canvas2 = page2.canvas

        drawPage2(
            canvas = canvas2,
            selectedDate = selectedDate,
            recordsList = recordsList,
            totalEstRevenue = totalEstRevenue,
            totalEstProfit = totalEstProfit,
            totalUnitsSold = totalUnitsSold,
            role = role
        )
        pdfDocument.finishPage(page2)

        // Save PDF to App Cache / Files Directory
        val reportsDir = File(context.cacheDir, "reports").apply { if (!exists()) mkdirs() }
        val sanitizedDate = selectedDate.replace("-", "_")
        val outputFile = File(reportsDir, "Liquor_Daily_Audit_Report_$sanitizedDate.pdf")

        return try {
            val fos = FileOutputStream(outputFile)
            pdfDocument.writeTo(fos)
            fos.flush()
            fos.close()
            pdfDocument.close()
            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    private fun drawPage1(
        canvas: Canvas,
        selectedDate: String,
        generatedAt: String,
        daySummary: DaySummary?,
        totalEstRevenue: Double,
        totalEstCost: Double,
        totalEstProfit: Double,
        profitMarginPct: Int,
        totalUnitsSold: Int,
        totalOpeningUnits: Int,
        totalReceivedUnits: Int,
        totalClosingUnits: Int,
        totalBreakageUnits: Int,
        totalStockValuationMRP: Double,
        totalStockValuationCost: Double,
        totalDailyExpenses: Double,
        netContribution: Double,
        categoryMovements: List<CategoryStockMovementItem>,
        recordsList: List<StockRecordItemUiState>,
        dayExpensesList: List<DailyExpense>,
        role: String
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        var y = MARGIN

        // 1. Top Header Banner
        paint.color = Color.rgb(24, 30, 44) // Deep Navy
        canvas.drawRoundRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 68f), 8f, 8f, paint)

        // Title text
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 15f
        canvas.drawText("LIQUOR INVENTORY & DAILY SALES MANAGEMENT", MARGIN + 14f, y + 24f, paint)

        paint.color = Color.rgb(212, 175, 55) // Gold Accent
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DAILY ACCOUNTING & AUDIT SUMMARY STATEMENT", MARGIN + 14f, y + 42f, paint)

        paint.color = Color.rgb(200, 210, 225)
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Date: $selectedDate  |  Generated: $generatedAt  |  Audited By: $role", MARGIN + 14f, y + 58f, paint)

        // Status Badge in Banner
        val isClosed = daySummary?.isClosed == true
        val statusText = if (isClosed) "DAY CLOSED" else "DAY OPEN (ACTIVE)"
        paint.color = if (isClosed) Color.rgb(46, 125, 50) else Color.rgb(230, 81, 0)
        val badgeRect = RectF(PAGE_WIDTH - MARGIN - 120f, y + 20f, PAGE_WIDTH - MARGIN - 14f, y + 48f)
        canvas.drawRoundRect(badgeRect, 6f, 6f, paint)

        paint.color = Color.WHITE
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(statusText, badgeRect.centerX(), badgeRect.centerY() + 3f, paint)
        paint.textAlign = Paint.Align.LEFT

        y += 82f

        // 2. Financial KPI Metric Cards (2x2 Grid)
        val cardWidth = (PAGE_WIDTH - (MARGIN * 2) - 10f) / 2f
        val cardHeight = 44f

        // Card 1: Gross Sales Turnover
        drawMetricCard(
            canvas = canvas,
            x = MARGIN,
            y = y,
            width = cardWidth,
            height = cardHeight,
            label = "GROSS SALES REVENUE (MRP)",
            value = "₹ %,.2f".format(totalEstRevenue),
            subtext = "$totalUnitsSold Bottles Sold  •  Margin: $profitMarginPct%",
            accentColor = Color.rgb(41, 98, 255),
            bgAlpha = 0.08f
        )

        // Card 2: Estimated Gross Profit
        drawMetricCard(
            canvas = canvas,
            x = MARGIN + cardWidth + 10f,
            y = y,
            width = cardWidth,
            height = cardHeight,
            label = "ESTIMATED GROSS PROFIT",
            value = "₹ %,.2f".format(totalEstProfit),
            subtext = "COGS: ₹ %,.2f".format(totalEstCost),
            accentColor = Color.rgb(46, 125, 50),
            bgAlpha = 0.08f
        )

        y += cardHeight + 8f

        // Card 3: Stock Valuation & Physical Balance
        drawMetricCard(
            canvas = canvas,
            x = MARGIN,
            y = y,
            width = cardWidth,
            height = cardHeight,
            label = "CLOSING STOCK VALUATION (RETAIL MRP)",
            value = "₹ %,.2f".format(totalStockValuationMRP),
            subtext = "Cost: ₹ %,.2f  •  $totalClosingUnits Bottles on Shelf".format(totalStockValuationCost),
            accentColor = Color.rgb(106, 27, 154),
            bgAlpha = 0.08f
        )

        // Card 4: Expenses & Net Margin
        drawMetricCard(
            canvas = canvas,
            x = MARGIN + cardWidth + 10f,
            y = y,
            width = cardWidth,
            height = cardHeight,
            label = "DAILY EXPENSES & NET CONTRIBUTION",
            value = "₹ %,.2f".format(netContribution),
            subtext = "Expenses: ₹ %,.2f  •  Breakages: $totalBreakageUnits Units".format(totalDailyExpenses),
            accentColor = if (netContribution >= 0) Color.rgb(0, 137, 123) else Color.rgb(198, 40, 40),
            bgAlpha = 0.08f
        )

        y += cardHeight + 16f

        // 3. Section Title: Category-Wise Movement
        paint.color = Color.rgb(30, 40, 60)
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DEPARTMENT / CATEGORY-WISE INVENTORY & REVENUE BREAKDOWN", MARGIN, y, paint)
        y += 6f

        // Category Table Header
        val colWidths = floatArrayOf(80f, 48f, 48f, 48f, 50f, 50f, 95f, 95f)
        val colHeaders = arrayOf("Category", "Opening", "Inward", "Sold", "Breakage", "Closing", "Revenue (₹)", "Est. Profit (₹)")

        val tableHeaderY = y + 4f
        paint.color = Color.rgb(235, 240, 248)
        canvas.drawRoundRect(RectF(MARGIN, tableHeaderY, PAGE_WIDTH - MARGIN, tableHeaderY + 18f), 4f, 4f, paint)

        paint.color = Color.rgb(20, 30, 50)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        var curX = MARGIN + 6f
        for (i in colHeaders.indices) {
            val alignRight = i >= 1
            if (alignRight) {
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText(colHeaders[i], curX + colWidths[i] - 6f, tableHeaderY + 12f, paint)
            } else {
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText(colHeaders[i], curX, tableHeaderY + 12f, paint)
            }
            curX += colWidths[i]
        }
        paint.textAlign = Paint.Align.LEFT

        y = tableHeaderY + 22f

        // Compute category profits map
        val categoryProfits = recordsList.groupBy { it.product.category }
            .mapValues { (_, items) -> items.sumOf { it.record.grossProfit } }

        // Category Table Rows
        categoryMovements.forEachIndexed { index, item ->
            val rowBg = if (index % 2 == 0) Color.WHITE else Color.rgb(248, 250, 252)
            paint.color = rowBg
            canvas.drawRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 16f), paint)

            paint.color = Color.rgb(40, 50, 60)
            paint.textSize = 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            curX = MARGIN + 6f
            // 0: Category
            paint.textAlign = Paint.Align.LEFT
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(item.category, curX, y + 11f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            curX += colWidths[0]

            // 1: Opening
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("${item.openingStock}", curX + colWidths[1] - 6f, y + 11f, paint)
            curX += colWidths[1]

            // 2: Inward
            canvas.drawText("+${item.receivedQty}", curX + colWidths[2] - 6f, y + 11f, paint)
            curX += colWidths[2]

            // 3: Sold
            paint.color = Color.rgb(20, 100, 20)
            canvas.drawText("${item.salesQty}", curX + colWidths[3] - 6f, y + 11f, paint)
            paint.color = Color.rgb(40, 50, 60)
            curX += colWidths[3]

            // 4: Breakage
            canvas.drawText("${item.breakageQty}", curX + colWidths[4] - 6f, y + 11f, paint)
            curX += colWidths[4]

            // 5: Closing
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("${item.closingStock}", curX + colWidths[5] - 6f, y + 11f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            curX += colWidths[5]

            // 6: Revenue
            canvas.drawText("₹ %,.2f".format(item.salesValue), curX + colWidths[6] - 6f, y + 11f, paint)
            curX += colWidths[6]

            // 7: Profit
            val catProfit = categoryProfits[item.category] ?: 0.0
            paint.color = Color.rgb(30, 120, 40)
            canvas.drawText("₹ %,.2f".format(catProfit), curX + colWidths[7] - 6f, y + 11f, paint)
            paint.color = Color.rgb(40, 50, 60)

            y += 16f
        }

        // Category Totals Row
        paint.color = Color.rgb(228, 235, 245)
        canvas.drawRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 18f), paint)

        paint.color = Color.rgb(15, 25, 45)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        curX = MARGIN + 6f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("TOTAL", curX, y + 12f, paint)
        curX += colWidths[0]

        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("$totalOpeningUnits", curX + colWidths[1] - 6f, y + 12f, paint)
        curX += colWidths[1]

        canvas.drawText("+$totalReceivedUnits", curX + colWidths[2] - 6f, y + 12f, paint)
        curX += colWidths[2]

        canvas.drawText("$totalUnitsSold", curX + colWidths[3] - 6f, y + 12f, paint)
        curX += colWidths[3]

        canvas.drawText("$totalBreakageUnits", curX + colWidths[4] - 6f, y + 12f, paint)
        curX += colWidths[4]

        canvas.drawText("$totalClosingUnits", curX + colWidths[5] - 6f, y + 12f, paint)
        curX += colWidths[5]

        canvas.drawText("₹ %,.2f".format(totalEstRevenue), curX + colWidths[6] - 6f, y + 12f, paint)
        curX += colWidths[6]

        canvas.drawText("₹ %,.2f".format(totalEstProfit), curX + colWidths[7] - 6f, y + 12f, paint)
        paint.textAlign = Paint.Align.LEFT

        y += 28f

        // 4. Daily Operating Expenses Section
        paint.color = Color.rgb(30, 40, 60)
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DAILY STORE EXPENSES & CASH DISBURSEMENTS ($selectedDate)", MARGIN, y, paint)
        y += 6f

        val expHeaderY = y + 4f
        paint.color = Color.rgb(240, 240, 245)
        canvas.drawRoundRect(RectF(MARGIN, expHeaderY, PAGE_WIDTH - MARGIN, expHeaderY + 16f), 4f, 4f, paint)

        paint.color = Color.rgb(30, 40, 50)
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        canvas.drawText("Expense Category / Head", MARGIN + 6f, expHeaderY + 11f, paint)
        canvas.drawText("Payment Mode", MARGIN + 180f, expHeaderY + 11f, paint)
        canvas.drawText("Remarks / Purpose", MARGIN + 280f, expHeaderY + 11f, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Amount (₹)", PAGE_WIDTH - MARGIN - 6f, expHeaderY + 11f, paint)
        paint.textAlign = Paint.Align.LEFT

        y = expHeaderY + 18f

        if (dayExpensesList.isEmpty()) {
            paint.color = Color.GRAY
            paint.textSize = 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas.drawText("No specific daily expenses recorded for $selectedDate.", MARGIN + 6f, y + 12f, paint)
            y += 20f
        } else {
            dayExpensesList.take(4).forEachIndexed { i, exp ->
                val bg = if (i % 2 == 0) Color.WHITE else Color.rgb(250, 250, 252)
                paint.color = bg
                canvas.drawRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 14f), paint)

                paint.color = Color.rgb(40, 50, 60)
                paint.textSize = 7.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

                canvas.drawText(exp.category, MARGIN + 6f, y + 10f, paint)
                canvas.drawText(exp.paymentMode, MARGIN + 180f, y + 10f, paint)
                canvas.drawText(exp.remarks.take(30), MARGIN + 280f, y + 10f, paint)
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText("₹ %,.2f".format(exp.amount), PAGE_WIDTH - MARGIN - 6f, y + 10f, paint)
                paint.textAlign = Paint.Align.LEFT

                y += 14f
            }
        }

        y += 18f

        // 5. Verification & Audit Sign-Off Block
        val signBoxTop = PAGE_HEIGHT - MARGIN - 80f
        paint.color = Color.rgb(245, 247, 250)
        canvas.drawRoundRect(RectF(MARGIN, signBoxTop, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - MARGIN - 14f), 6f, 6f, paint)

        paint.color = Color.rgb(210, 215, 225)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(RectF(MARGIN, signBoxTop, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - MARGIN - 14f), 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.rgb(20, 30, 50)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        val col1X = MARGIN + 14f
        val col2X = MARGIN + 180f
        val col3X = MARGIN + 350f

        canvas.drawText("PREPARED BY (COUNTER IN-CHARGE)", col1X, signBoxTop + 16f, paint)
        canvas.drawText("VERIFIED BY (STORE MANAGER)", col2X, signBoxTop + 16f, paint)
        canvas.drawText("EXCISE / AUDIT STAMP", col3X, signBoxTop + 16f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.GRAY
        paint.textSize = 8f
        canvas.drawText("Signature: ______________________", col1X, signBoxTop + 40f, paint)
        canvas.drawText("Signature: ______________________", col2X, signBoxTop + 40f, paint)
        canvas.drawText("Clearance Stamp / Seal:", col3X, signBoxTop + 40f, paint)

        canvas.drawText("Date: $selectedDate", col1X, signBoxTop + 54f, paint)
        canvas.drawText("Date: $selectedDate", col2X, signBoxTop + 54f, paint)

        // Footer Note
        paint.textSize = 7.5f
        paint.color = Color.GRAY
        canvas.drawText("Page 1 of 2  •  Official Electronic Daily Stock & Sales Register for Excise Audit Compliance", MARGIN, PAGE_HEIGHT - MARGIN + 4f, paint)
    }

    private fun drawPage2(
        canvas: Canvas,
        selectedDate: String,
        recordsList: List<StockRecordItemUiState>,
        totalEstRevenue: Double,
        totalEstProfit: Double,
        totalUnitsSold: Int,
        role: String
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        var y = MARGIN

        // Header Strip
        paint.color = Color.rgb(24, 30, 44)
        canvas.drawRoundRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 36f), 6f, 6f, paint)

        paint.color = Color.WHITE
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("ITEMIZED STOCK MOVEMENT & SALES AUDIT REGISTER", MARGIN + 12f, y + 18f, paint)

        paint.color = Color.rgb(212, 175, 55)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Daily Ledger for: $selectedDate  |  Total Products Audited: ${recordsList.size}", MARGIN + 12f, y + 30f, paint)

        y += 44f

        // Table Column Specifications
        val colWidths = floatArrayOf(130f, 60f, 40f, 32f, 32f, 32f, 32f, 45f, 60f, 60f)
        val colHeaders = arrayOf("Product & Variant", "SKU Code", "Size", "Opn", "Rec", "Sold", "Cls", "Rate (₹)", "Sales (₹)", "Profit (₹)")

        // Header Background
        paint.color = Color.rgb(230, 236, 245)
        canvas.drawRoundRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 16f), 4f, 4f, paint)

        paint.color = Color.rgb(20, 30, 50)
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        var curX = MARGIN + 4f
        for (i in colHeaders.indices) {
            val alignRight = i >= 3
            if (alignRight) {
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText(colHeaders[i], curX + colWidths[i] - 4f, y + 11f, paint)
            } else {
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText(colHeaders[i], curX, y + 11f, paint)
            }
            curX += colWidths[i]
        }
        paint.textAlign = Paint.Align.LEFT

        y += 18f

        // Sort items by Sales quantity desc, then category
        val sortedList = recordsList.sortedWith(
            compareByDescending<StockRecordItemUiState> { it.record.salesQty }
                .thenBy { it.product.category }
                .thenBy { it.product.brandName }
        )

        // Show items that fit on Page 2 (approx 34 rows max to keep clean layout)
        val rowsToShow = sortedList.take(33)

        rowsToShow.forEachIndexed { idx, item ->
            val isEven = idx % 2 == 0
            paint.color = if (isEven) Color.WHITE else Color.rgb(248, 250, 252)
            canvas.drawRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 14f), paint)

            paint.textSize = 7f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = Color.rgb(30, 40, 50)

            curX = MARGIN + 4f

            // 0: Name
            paint.textAlign = Paint.Align.LEFT
            val fullName = "${item.product.brandName} ${item.product.name}".take(22)
            canvas.drawText(fullName, curX, y + 10f, paint)
            curX += colWidths[0]

            // 1: SKU
            paint.color = Color.rgb(100, 110, 125)
            canvas.drawText(item.product.sku.take(12), curX, y + 10f, paint)
            paint.color = Color.rgb(30, 40, 50)
            curX += colWidths[1]

            // 2: Size
            canvas.drawText(item.product.unitSize.replace(" ", ""), curX, y + 10f, paint)
            curX += colWidths[2]

            // 3: Opening
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("${item.record.openingStock}", curX + colWidths[3] - 4f, y + 10f, paint)
            curX += colWidths[3]

            // 4: Received
            canvas.drawText("${item.record.receivedQty}", curX + colWidths[4] - 4f, y + 10f, paint)
            curX += colWidths[4]

            // 5: Sold
            if (item.record.salesQty > 0) {
                paint.color = Color.rgb(20, 120, 30)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            } else {
                paint.color = Color.rgb(120, 130, 140)
            }
            canvas.drawText("${item.record.salesQty}", curX + colWidths[5] - 4f, y + 10f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = Color.rgb(30, 40, 50)
            curX += colWidths[5]

            // 6: Closing
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("${item.record.closingStock}", curX + colWidths[6] - 4f, y + 10f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            curX += colWidths[6]

            // 7: Rate
            canvas.drawText("₹${item.record.rate.toInt()}", curX + colWidths[7] - 4f, y + 10f, paint)
            curX += colWidths[7]

            // 8: Sales Value
            paint.color = Color.rgb(15, 25, 45)
            canvas.drawText("₹${item.record.totalSalesValue.toInt()}", curX + colWidths[8] - 4f, y + 10f, paint)
            curX += colWidths[8]

            // 9: Profit
            paint.color = Color.rgb(25, 110, 40)
            canvas.drawText("₹${item.record.grossProfit.toInt()}", curX + colWidths[9] - 4f, y + 10f, paint)
            paint.color = Color.rgb(30, 40, 50)

            y += 14f
        }

        // Grand Totals Summary Row at Bottom of Page 2
        y += 4f
        paint.color = Color.rgb(220, 230, 245)
        canvas.drawRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 18f), paint)

        paint.color = Color.rgb(15, 25, 45)
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        curX = MARGIN + 4f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("REGISTER TOTALS (${recordsList.size} ITEMS AUDITED)", curX, y + 12f, paint)

        paint.textAlign = Paint.Align.RIGHT
        // Total sold units
        val soldX = MARGIN + 4f + colWidths[0] + colWidths[1] + colWidths[2] + colWidths[3] + colWidths[4]
        canvas.drawText("$totalUnitsSold Btls", soldX + colWidths[5] - 4f, y + 12f, paint)

        // Total sales revenue
        val revX = soldX + colWidths[5] + colWidths[6] + colWidths[7]
        canvas.drawText("₹ %,.2f".format(totalEstRevenue), revX + colWidths[8] - 4f, y + 12f, paint)

        // Total profit
        val profX = revX + colWidths[8]
        canvas.drawText("₹ %,.2f".format(totalEstProfit), profX + colWidths[9] - 4f, y + 12f, paint)
        paint.textAlign = Paint.Align.LEFT

        // Page 2 Footer
        paint.textSize = 7.5f
        paint.color = Color.GRAY
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Page 2 of 2  •  End of Daily Stock Ledger  •  Liquor Inventory System", MARGIN, PAGE_HEIGHT - MARGIN + 4f, paint)
    }

    private fun drawMetricCard(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        label: String,
        value: String,
        subtext: String,
        accentColor: Int,
        bgAlpha: Float
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Background
        paint.color = accentColor
        paint.alpha = (bgAlpha * 255).toInt()
        canvas.drawRoundRect(RectF(x, y, x + width, y + height), 6f, 6f, paint)

        // Accent Left Border Bar
        paint.color = accentColor
        paint.alpha = 255
        canvas.drawRoundRect(RectF(x, y, x + 4f, y + height), 2f, 2f, paint)

        // Label
        paint.color = Color.rgb(80, 90, 105)
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(label, x + 10f, y + 12f, paint)

        // Value
        paint.color = Color.rgb(20, 25, 35)
        paint.textSize = 12.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(value, x + 10f, y + 27f, paint)

        // Subtext
        paint.color = Color.rgb(100, 110, 125)
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(subtext, x + 10f, y + 39f, paint)
    }

    /**
     * Launch native viewer or share intent for the generated PDF document.
     */
    fun openPdfFile(context: Context, file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open Daily Accounting PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "No PDF viewer app found on device.", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Share PDF file via WhatsApp, Gmail, Bluetooth, Drive, etc.
     */
    fun sharePdfFile(context: Context, file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Liquor Daily Accounting Report - ${file.nameWithoutExtension}")
                putExtra(Intent.EXTRA_TEXT, "Attached is the Liquor Store Daily Accounting & Stock Audit Report.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Daily Accounting Report PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to share PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Direct Native Print via Android PrintManager.
     */
    fun printPdfFile(context: Context, file: File) {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager != null) {
                val printAdapter: PrintDocumentAdapter = object : PrintDocumentAdapter() {
                    override fun onLayout(
                        oldAttributes: PrintAttributes?,
                        newAttributes: PrintAttributes?,
                        cancellationSignal: android.os.CancellationSignal?,
                        callback: LayoutResultCallback?,
                        extras: android.os.Bundle?
                    ) {
                        if (cancellationSignal?.isCanceled == true) {
                            callback?.onLayoutCancelled()
                            return
                        }
                        val info = android.print.PrintDocumentInfo.Builder(file.name)
                            .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .setPageCount(2)
                            .build()
                        callback?.onLayoutFinished(info, true)
                    }

                    override fun onWrite(
                        pages: Array<out android.print.PageRange>?,
                        destination: android.os.ParcelFileDescriptor?,
                        cancellationSignal: android.os.CancellationSignal?,
                        callback: WriteResultCallback?
                    ) {
                        try {
                            val input = java.io.FileInputStream(file)
                            val output = java.io.FileOutputStream(destination?.fileDescriptor)
                            val buf = ByteArray(1024)
                            var bytesRead: Int
                            while (input.read(buf).also { bytesRead = it } > 0) {
                                output.write(buf, 0, bytesRead)
                            }
                            callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                            input.close()
                            output.close()
                        } catch (e: Exception) {
                            callback?.onWriteFailed(e.message)
                        }
                    }
                }
                val jobName = "Liquor_Daily_Summary_${file.nameWithoutExtension}"
                printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
            } else {
                openPdfFile(context, file)
            }
        } catch (e: Exception) {
            openPdfFile(context, file)
        }
    }
}
