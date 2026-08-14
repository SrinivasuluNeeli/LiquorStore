package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.seed.ProductSeedData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class LiquorRepository(private val db: AppDatabase) {

    val allProducts: Flow<List<Product>> = db.productDao().getAllProducts()
    val activeProducts: Flow<List<Product>> = db.productDao().getActiveProducts()
    val allExpenses: Flow<List<DailyExpense>> = db.expenseDao().getAllExpenses()
    val allReceipts: Flow<List<StockReceipt>> = db.stockReceiptDao().getAllReceipts()
    val allAuditLogs: Flow<List<AuditLog>> = db.auditDao().getAllAuditLogs()
    val allDailyStockRecords: Flow<List<DailyStockRecord>> = db.dailyStockDao().getAllRecordsFlow()

    suspend fun seedDatabaseIfEmpty() = withContext(Dispatchers.IO) {
        val count = db.productDao().getProductCount()
        if (count == 0) {
            db.productDao().insertAll(ProductSeedData.initialProducts)
            // Seed sample audit history if empty
            val sampleAuditLogs = listOf(
                AuditLog(
                    id = "audit_seed_001",
                    timestamp = 1754092800000L, // 2026-08-01 00:00
                    userRole = "System",
                    actionType = "INITIAL_SEED",
                    targetEntity = "Master Product Catalog",
                    changedFields = "Catalog SKUs (50 Products)",
                    oldValue = "Catalog: 0 items",
                    newValue = "Initialized 50 Master SKUs across Brandy, Whisky, Beer, Rum, Vodka, Wine"
                ),
                AuditLog(
                    id = "audit_seed_002",
                    timestamp = 1754442000000L, // 2026-08-05 09:00
                    userRole = "Admin",
                    actionType = "PRICE_CHANGE",
                    targetEntity = "Morpheus XO Premium Brandy 750 ML",
                    changedFields = "defaultRate: ₹1,280 -> ₹1,350",
                    oldValue = "MRP: ₹1,280 (Cost: ₹1,020)",
                    newValue = "MRP: ₹1,350 (Cost: ₹1,020) [Excise revision]"
                ),
                AuditLog(
                    id = "audit_seed_003",
                    timestamp = 1754614800000L, // 2026-08-07 10:30
                    userRole = "Staff",
                    actionType = "STOCK_RECEIPT",
                    targetEntity = "Invoice USL-8821 (United Spirits Ltd)",
                    changedFields = "Inward Stock Delivery (3 SKUs)",
                    oldValue = "Pending Verification",
                    newValue = "Received 48 Bottles: McDowell's No.1, Royal Challenge, Antiquity Blue"
                ),
                AuditLog(
                    id = "audit_seed_004",
                    timestamp = 1754787600000L, // 2026-08-09 11:15
                    userRole = "Staff",
                    actionType = "CREATE_EXPENSE",
                    targetEntity = "Daily Store Expense (Transport / Freight)",
                    changedFields = "amount: ₹4,600, paymentMode: Cash",
                    oldValue = "No previous entry",
                    newValue = "₹4,600.00 via Cash (Depo Unloading & Truck Freight)"
                ),
                AuditLog(
                    id = "audit_seed_005",
                    timestamp = 1754960400000L, // 2026-08-11 12:45
                    userRole = "Staff",
                    actionType = "UPDATE_STOCK",
                    targetEntity = "M.H. Brandy 750 ML (Date: 2026-08-11)",
                    changedFields = "Closing: 14 -> 12, Sold: 0 -> 2",
                    oldValue = "Opening: 14, Inward: 0, Breakage: 0, Closing: 14 | Sold: 0 Btls",
                    newValue = "Opening: 14, Inward: 0, Breakage: 0, Closing: 12 | Sold: 2 Btls (₹1,780)"
                ),
                AuditLog(
                    id = "audit_seed_006",
                    timestamp = 1754982000000L, // 2026-08-11 18:45
                    userRole = "Staff",
                    actionType = "CLOSE_DAY",
                    targetEntity = "Day Summary (2026-08-11)",
                    changedFields = "isClosed: false -> true, Revenue: ₹68,400",
                    oldValue = "Day Open (Active Sales)",
                    newValue = "Day Closed | Cash: ₹38,200, UPI: ₹26,400, Credit: ₹3,800"
                ),
                AuditLog(
                    id = "audit_seed_007",
                    timestamp = 1755046800000L, // 2026-08-12 09:30
                    userRole = "Admin",
                    actionType = "REOPEN_DAY",
                    targetEntity = "Day Summary (2026-08-11)",
                    changedFields = "isClosed: true -> false",
                    oldValue = "Day Closed by Staff",
                    newValue = "Reopened for Evening Excise Breakage Inspection"
                ),
                AuditLog(
                    id = "audit_seed_008",
                    timestamp = 1755048600000L, // 2026-08-12 10:00
                    userRole = "Admin",
                    actionType = "BREAKAGE_ADJUSTMENT",
                    targetEntity = "Magic Moments Vodka 750 ML (Date: 2026-08-11)",
                    changedFields = "Breakage: 0 -> 1, Closing: 18 -> 17",
                    oldValue = "Opening: 18, Recv: 0, Breakage: 0, Closing: 18 | Sold: 0",
                    newValue = "[Transit Seal Leak] Opening: 18, Breakage: 1, Closing: 17 | Sold: 0"
                ),
                AuditLog(
                    id = "audit_seed_009",
                    timestamp = 1755050400000L, // 2026-08-12 10:30
                    userRole = "Admin",
                    actionType = "CLOSE_DAY",
                    targetEntity = "Day Summary (2026-08-11)",
                    changedFields = "isClosed: false -> true",
                    oldValue = "Reopened",
                    newValue = "Day Finalized & Re-Closed by Admin"
                ),
                AuditLog(
                    id = "audit_seed_010",
                    timestamp = 1755061200000L, // 2026-08-12 13:30
                    userRole = "Staff",
                    actionType = "STOCK_RECEIPT",
                    targetEntity = "Invoice CLB-4402 (Carlsberg Breweries)",
                    changedFields = "Inward Stock Delivery (2 SKUs)",
                    oldValue = "Pending Verification",
                    newValue = "Received 36 Bottles: Tuborg Green 650 ML, Carlsberg Elephant 650 ML"
                ),
                AuditLog(
                    id = "audit_seed_011",
                    timestamp = 1755082800000L, // 2026-08-12 19:30
                    userRole = "Staff",
                    actionType = "UPDATE_STOCK",
                    targetEntity = "Tuborg Green Beer 650 ML (Date: 2026-08-12)",
                    changedFields = "Closing: 36 -> 28, Sold: 0 -> 8",
                    oldValue = "Opening: 24, Inward: 12, Breakage: 0, Closing: 36 | Sold: 0",
                    newValue = "Opening: 24, Inward: 12, Breakage: 0, Closing: 28 | Sold: 8 Btls (₹1,520)"
                ),
                AuditLog(
                    id = "audit_seed_012",
                    timestamp = 1755090000000L, // 2026-08-12 21:30
                    userRole = "Staff",
                    actionType = "CLOSE_DAY",
                    targetEntity = "Day Summary (2026-08-12)",
                    changedFields = "isClosed: false -> true, Revenue: ₹84,200",
                    oldValue = "Day Open (Active Sales)",
                    newValue = "Day Closed | Cash: ₹49,600, UPI: ₹31,800, Credit: ₹2,800"
                ),
                AuditLog(
                    id = "audit_seed_013",
                    timestamp = 1755133200000L, // 2026-08-13 09:30
                    userRole = "Staff",
                    actionType = "CREATE_EXPENSE",
                    targetEntity = "Daily Store Expense (Miscellaneous)",
                    changedFields = "amount: ₹1,450, paymentMode: Cash",
                    oldValue = "No previous entry",
                    newValue = "₹1,450.00 via Cash (Store Refreshments & Cleaning Supplies)"
                ),
                AuditLog(
                    id = "audit_seed_014",
                    timestamp = 1755140400000L, // 2026-08-13 11:30
                    userRole = "Staff",
                    actionType = "UPDATE_STOCK",
                    targetEntity = "Royal Challenge Whisky 750 ML (Date: 2026-08-13)",
                    changedFields = "Closing: 18 -> 15, Sold: 0 -> 3",
                    oldValue = "Opening: 18, Inward: 0, Breakage: 0, Closing: 18 | Sold: 0",
                    newValue = "Opening: 18, Inward: 0, Breakage: 0, Closing: 15 | Sold: 3 Btls (₹2,640)"
                )
            )
            db.auditDao().insertAll(sampleAuditLogs)

            // Seed initial sample monthly expenses for analytics distribution
            val sampleExpenses = listOf(
                DailyExpense(
                    id = "exp_rent_1",
                    date = "2026-08-01",
                    category = "Rent",
                    amount = 45000.0,
                    paymentMode = "Bank Transfer",
                    remarks = "Shop Monthly Lease Advance",
                    createdAt = 1754092800000L
                ),
                DailyExpense(
                    id = "exp_salary_1",
                    date = "2026-08-05",
                    category = "Staff Salary",
                    amount = 32000.0,
                    paymentMode = "Bank Transfer",
                    remarks = "Counter & Floor Staff Wages (1st Fortnight)",
                    createdAt = 1754438400000L
                ),
                DailyExpense(
                    id = "exp_excise_1",
                    date = "2026-08-03",
                    category = "License / Regulatory Fee",
                    amount = 18500.0,
                    paymentMode = "Bank Transfer",
                    remarks = "State Excise Import Permit & Verification Fee",
                    createdAt = 1754265600000L
                ),
                DailyExpense(
                    id = "exp_elec_1",
                    date = "2026-08-07",
                    category = "Utility Bill",
                    amount = 8900.0,
                    paymentMode = "UPI / Digital",
                    remarks = "Commercial Chiller & HVAC Monthly Bill",
                    createdAt = 1754611200000L
                ),
                DailyExpense(
                    id = "exp_trans_1",
                    date = "2026-08-09",
                    category = "Transport / Freight",
                    amount = 4600.0,
                    paymentMode = "Cash",
                    remarks = "Warehouse Depo Unloading & Truck Freight",
                    createdAt = 1754784000000L
                ),
                DailyExpense(
                    id = "exp_ice_1",
                    date = "2026-08-11",
                    category = "Miscellaneous",
                    amount = 2800.0,
                    paymentMode = "Cash",
                    remarks = "Carry Bags, Bubble Wrap & Ice Blocks Supply",
                    createdAt = 1754956800000L
                ),
                DailyExpense(
                    id = "exp_maint_1",
                    date = "2026-08-12",
                    category = "Miscellaneous",
                    amount = 2100.0,
                    paymentMode = "Cash",
                    remarks = "POS Thermal Printer & Barcode Scanner Service",
                    createdAt = 1755043200000L
                ),
                DailyExpense(
                    id = "exp_misc_1",
                    date = "2026-08-13",
                    category = "Miscellaneous",
                    amount = 1450.0,
                    paymentMode = "Cash",
                    remarks = "Store Refreshments & Cleaning Supplies",
                    createdAt = 1755129600000L
                )
            )
            for (expense in sampleExpenses) {
                db.expenseDao().insertExpense(expense)
            }

            // Seed historical stock records for past dates (Aug 7 to Aug 12) so trends are populated
            val dates = listOf("2026-08-07", "2026-08-08", "2026-08-09", "2026-08-10", "2026-08-11", "2026-08-12")
            val pastRecords = mutableListOf<DailyStockRecord>()
            val prods = ProductSeedData.initialProducts
            dates.forEachIndexed { dIndex, dStr ->
                prods.forEachIndexed { pIndex, p ->
                    val opening = p.initialOpeningStock + (pIndex % 4) * 2
                    val received = if (dIndex % 3 == 0 && pIndex % 3 == 0) 12 else 0
                    val sales = ((pIndex + dIndex * 3) % 7) + 1
                    val breakage = if ((pIndex + dIndex) % 29 == 0) 1 else 0
                    val closing = (opening + received - sales - breakage).coerceAtLeast(0)
                    pastRecords.add(
                        DailyStockRecord(
                            id = "d_${dStr}_${p.id}",
                            date = dStr,
                            productId = p.id,
                            openingStock = opening,
                            receivedQty = received,
                            breakageQty = breakage,
                            closingStock = closing,
                            rate = p.defaultRate,
                            costPrice = p.costPrice
                        )
                    )
                }
            }
            db.dailyStockDao().insertOrUpdateAll(pastRecords)
        }
    }

    suspend fun ensureRecordsForDate(date: String) = withContext(Dispatchers.IO) {
        val activeProductsList = db.productDao().getActiveProducts().first()
        val existingRecords = db.dailyStockDao().getRecordsListForDate(date).associateBy { it.productId }
        val newRecords = mutableListOf<DailyStockRecord>()

        for (product in activeProductsList) {
            if (!existingRecords.containsKey(product.id)) {
                val opening = if (date == "2026-08-13") {
                    product.initialOpeningStock
                } else {
                    val prevRecord = db.dailyStockDao().getPreviousDayRecord(product.id, date)
                    prevRecord?.closingStock ?: product.initialOpeningStock
                }

                val recordId = "d_${date}_${product.id}"
                newRecords.add(
                    DailyStockRecord(
                        id = recordId,
                        date = date,
                        productId = product.id,
                        openingStock = opening,
                        receivedQty = 0,
                        breakageQty = 0,
                        closingStock = opening,
                        rate = product.defaultRate,
                        costPrice = product.costPrice
                    )
                )
            }
        }

        if (newRecords.isNotEmpty()) {
            db.dailyStockDao().insertOrUpdateAll(newRecords)
        }
    }

    fun getRecordsForDate(date: String): Flow<List<DailyStockRecord>> {
        return db.dailyStockDao().getRecordsForDate(date)
    }

    fun getExpensesForDate(date: String): Flow<List<DailyExpense>> {
        return db.expenseDao().getExpensesForDate(date)
    }

    fun getReceiptsForDate(date: String): Flow<List<StockReceipt>> {
        return db.stockReceiptDao().getReceiptsForDate(date)
    }

    fun getDaySummaryFlow(date: String): Flow<DaySummary?> {
        return db.daySummaryDao().getDaySummaryFlow(date)
    }

    suspend fun updateStockRecord(
        record: DailyStockRecord,
        userRole: String,
        auditNote: String? = null
    ) = withContext(Dispatchers.IO) {
        val oldRecord = db.dailyStockDao().getRecord(record.date, record.productId)
        db.dailyStockDao().insertOrUpdateRecord(record)

        val product = db.productDao().getProductById(record.productId)
        val prodLabel = if (product != null) "${product.brandName} ${product.name} (${product.unitSize})" else "SKU: ${record.productId}"

        val changes = mutableListOf<String>()
        if (oldRecord != null) {
            if (oldRecord.openingStock != record.openingStock) changes.add("Opening: ${oldRecord.openingStock} -> ${record.openingStock}")
            if (oldRecord.receivedQty != record.receivedQty) changes.add("Inward: ${oldRecord.receivedQty} -> ${record.receivedQty}")
            if (oldRecord.breakageQty != record.breakageQty) changes.add("Breakage: ${oldRecord.breakageQty} -> ${record.breakageQty}")
            if (oldRecord.closingStock != record.closingStock) changes.add("Closing: ${oldRecord.closingStock} -> ${record.closingStock}")
            if (oldRecord.rate != record.rate) changes.add("Rate: ₹${oldRecord.rate.toInt()} -> ₹${record.rate.toInt()}")
        } else {
            changes.add("Initial Entry: Closing ${record.closingStock}")
        }

        if (changes.isNotEmpty()) {
            val notePrefix = if (!auditNote.isNullOrBlank()) "[$auditNote] " else ""
            val oldSold = oldRecord?.salesQty ?: 0
            val oldValStr = if (oldRecord != null) {
                "Opening: ${oldRecord.openingStock}, Inward: ${oldRecord.receivedQty}, Breakage: ${oldRecord.breakageQty}, Closing: ${oldRecord.closingStock} | Sold: $oldSold Btls"
            } else {
                "No previous record"
            }
            val newValStr = "$notePrefix Opening: ${record.openingStock}, Inward: ${record.receivedQty}, Breakage: ${record.breakageQty}, Closing: ${record.closingStock} | Sold: ${record.salesQty} Btls (₹${record.totalSalesValue.toInt()})"

            db.auditDao().insertAuditLog(
                AuditLog(
                    id = UUID.randomUUID().toString(),
                    userRole = userRole,
                    actionType = if (record.date != "2026-08-13") "UPDATE_PAST_STOCK" else "UPDATE_STOCK",
                    targetEntity = "$prodLabel (Date: ${record.date})",
                    changedFields = changes.joinToString(", "),
                    oldValue = oldValStr,
                    newValue = newValStr
                )
            )
        }
    }

    suspend fun addStockReceipt(
        receipt: StockReceipt,
        items: List<StockReceiptItem>,
        userRole: String
    ) = withContext(Dispatchers.IO) {
        db.stockReceiptDao().insertReceipt(receipt)
        db.stockReceiptDao().insertReceiptItems(items)

        // Ensure records for receipt date exist
        ensureRecordsForDate(receipt.invoiceDate)

        // Update receivedQty in daily stock records for this date
        for (item in items) {
            val existing = db.dailyStockDao().getRecord(receipt.invoiceDate, item.productId)
            if (existing != null) {
                val newReceived = existing.receivedQty + item.quantity
                val closingAdjustment = if (existing.closingStock == existing.totalAvailable) {
                    existing.openingStock + newReceived - existing.breakageQty
                } else {
                    existing.closingStock
                }
                val updatedRecord = existing.copy(
                    receivedQty = newReceived,
                    closingStock = closingAdjustment
                )
                db.dailyStockDao().insertOrUpdateRecord(updatedRecord)
            }
        }

        db.auditDao().insertAuditLog(
            AuditLog(
                id = UUID.randomUUID().toString(),
                userRole = userRole,
                actionType = "STOCK_RECEIPT",
                targetEntity = "StockReceipt (${receipt.invoiceNo})",
                changedFields = "Received Items",
                oldValue = "",
                newValue = "${items.size} SKUs, Total: ₹${receipt.totalAmount}"
            )
        )
    }

    suspend fun addExpense(expense: DailyExpense, userRole: String) = withContext(Dispatchers.IO) {
        db.expenseDao().insertExpense(expense)
        db.auditDao().insertAuditLog(
            AuditLog(
                id = UUID.randomUUID().toString(),
                userRole = userRole,
                actionType = "CREATE_EXPENSE",
                targetEntity = "DailyExpense (${expense.category})",
                changedFields = "amount, paymentMode",
                oldValue = "",
                newValue = "₹${expense.amount} (${expense.paymentMode})"
            )
        )
    }

    suspend fun updateExpense(
        oldExpense: DailyExpense,
        updatedExpense: DailyExpense,
        userRole: String
    ) = withContext(Dispatchers.IO) {
        db.expenseDao().insertExpense(updatedExpense)
        db.auditDao().insertAuditLog(
            AuditLog(
                id = UUID.randomUUID().toString(),
                userRole = userRole,
                actionType = "UPDATE_EXPENSE",
                targetEntity = "DailyExpense (${updatedExpense.category})",
                changedFields = "Amount/Category/Mode/Remarks",
                oldValue = "₹${oldExpense.amount.toInt()} (${oldExpense.category} - ${oldExpense.paymentMode})",
                newValue = "₹${updatedExpense.amount.toInt()} (${updatedExpense.category} - ${updatedExpense.paymentMode})"
            )
        )
    }

    suspend fun deleteExpense(expense: DailyExpense, userRole: String) = withContext(Dispatchers.IO) {
        db.expenseDao().deleteExpense(expense)
        db.auditDao().insertAuditLog(
            AuditLog(
                id = UUID.randomUUID().toString(),
                userRole = userRole,
                actionType = "DELETE_EXPENSE",
                targetEntity = "DailyExpense (${expense.category})",
                changedFields = "deleted",
                oldValue = "₹${expense.amount}",
                newValue = "Deleted"
            )
        )
    }

    suspend fun closeDay(
        date: String,
        cashCollected: Double,
        upiCollected: Double,
        creditSales: Double,
        notes: String,
        userRole: String
    ) = withContext(Dispatchers.IO) {
        val totalExpenses = db.expenseDao().getTotalExpensesForDate(date) ?: 0.0
        val summary = DaySummary(
            date = date,
            isClosed = true,
            closedAt = System.currentTimeMillis(),
            closedBy = userRole,
            cashCollected = cashCollected,
            upiCollected = upiCollected,
            creditSales = creditSales,
            totalExpenses = totalExpenses,
            notes = notes
        )
        db.daySummaryDao().insertOrUpdateSummary(summary)

        db.auditDao().insertAuditLog(
            AuditLog(
                id = UUID.randomUUID().toString(),
                userRole = userRole,
                actionType = "CLOSE_DAY",
                targetEntity = "DaySummary ($date)",
                changedFields = "isClosed",
                oldValue = "Open",
                newValue = "Closed (Revenue: ₹${summary.totalSalesRevenue})"
            )
        )
    }

    suspend fun reopenDay(date: String, userRole: String) = withContext(Dispatchers.IO) {
        val existing = db.daySummaryDao().getDaySummary(date)
        if (existing != null) {
            val updated = existing.copy(
                isClosed = false,
                closedAt = null,
                closedBy = null
            )
            db.daySummaryDao().insertOrUpdateSummary(updated)

            db.auditDao().insertAuditLog(
                AuditLog(
                    id = UUID.randomUUID().toString(),
                    userRole = userRole,
                    actionType = "REOPEN_DAY",
                    targetEntity = "DaySummary ($date)",
                    changedFields = "isClosed",
                    oldValue = "Closed",
                    newValue = "Reopened"
                )
            )
        }
    }

    suspend fun saveProduct(product: Product, userRole: String) = withContext(Dispatchers.IO) {
        val oldProduct = db.productDao().getProductById(product.id)
        db.productDao().insertProduct(product)

        db.auditDao().insertAuditLog(
            AuditLog(
                id = UUID.randomUUID().toString(),
                userRole = userRole,
                actionType = if (oldProduct == null) "CREATE_PRODUCT" else "UPDATE_PRODUCT",
                targetEntity = "Product (${product.brand} ${product.name})",
                changedFields = "Details",
                oldValue = oldProduct?.defaultRate?.toString() ?: "",
                newValue = "Rate: ₹${product.defaultRate}, Cost: ₹${product.costPrice}"
            )
        )
    }

    suspend fun deleteProduct(product: Product, userRole: String) = withContext(Dispatchers.IO) {
        db.productDao().deleteProduct(product)
        db.auditDao().insertAuditLog(
            AuditLog(
                id = UUID.randomUUID().toString(),
                userRole = userRole,
                actionType = "DELETE_PRODUCT",
                targetEntity = "Product (${product.brand} ${product.name})",
                changedFields = "deleted",
                oldValue = "Rate: ₹${product.defaultRate}",
                newValue = "Deleted"
            )
        )
    }

    suspend fun updateProductsOrder(products: List<Product>) = withContext(Dispatchers.IO) {
        db.productDao().insertAll(products)
    }

    suspend fun getReceiptItems(receiptId: String): List<StockReceiptItem> = withContext(Dispatchers.IO) {
        db.stockReceiptDao().getItemsForReceipt(receiptId)
    }

    suspend fun updateStockReceipt(
        oldReceipt: StockReceipt,
        updatedReceipt: StockReceipt,
        newItems: List<StockReceiptItem>,
        userRole: String
    ) = withContext(Dispatchers.IO) {
        val oldItems = db.stockReceiptDao().getItemsForReceipt(oldReceipt.id)

        // 1. Rollback old items receivedQty in old invoiceDate
        for (item in oldItems) {
            val existing = db.dailyStockDao().getRecord(oldReceipt.invoiceDate, item.productId)
            if (existing != null) {
                val revertedReceived = (existing.receivedQty - item.quantity).coerceAtLeast(0)
                val updatedRecord = existing.copy(
                    receivedQty = revertedReceived,
                    closingStock = (existing.openingStock + revertedReceived - existing.breakageQty).coerceAtLeast(0)
                )
                db.dailyStockDao().insertOrUpdateRecord(updatedRecord)
            }
        }

        // 2. Delete old items and insert updated receipt & new items
        db.stockReceiptDao().deleteReceiptItems(oldReceipt.id)
        db.stockReceiptDao().insertReceipt(updatedReceipt)
        db.stockReceiptDao().insertReceiptItems(newItems)

        // 3. Ensure and apply new items receivedQty in new invoiceDate
        ensureRecordsForDate(updatedReceipt.invoiceDate)
        for (item in newItems) {
            val existing = db.dailyStockDao().getRecord(updatedReceipt.invoiceDate, item.productId)
            if (existing != null) {
                val newReceived = existing.receivedQty + item.quantity
                val updatedRecord = existing.copy(
                    receivedQty = newReceived,
                    closingStock = (existing.openingStock + newReceived - existing.breakageQty).coerceAtLeast(0)
                )
                db.dailyStockDao().insertOrUpdateRecord(updatedRecord)
            }
        }

        db.auditDao().insertAuditLog(
            AuditLog(
                id = UUID.randomUUID().toString(),
                userRole = userRole,
                actionType = "UPDATE_RECEIPT",
                targetEntity = "StockReceipt (${updatedReceipt.invoiceNo})",
                changedFields = "Items & Total",
                oldValue = "₹${oldReceipt.totalAmount}",
                newValue = "₹${updatedReceipt.totalAmount} (${newItems.size} items)"
            )
        )
    }

    suspend fun deleteStockReceipt(receipt: StockReceipt, userRole: String) = withContext(Dispatchers.IO) {
        val items = db.stockReceiptDao().getItemsForReceipt(receipt.id)
        for (item in items) {
            val existing = db.dailyStockDao().getRecord(receipt.invoiceDate, item.productId)
            if (existing != null) {
                val revertedReceived = (existing.receivedQty - item.quantity).coerceAtLeast(0)
                val updatedRecord = existing.copy(
                    receivedQty = revertedReceived,
                    closingStock = (existing.openingStock + revertedReceived - existing.breakageQty).coerceAtLeast(0)
                )
                db.dailyStockDao().insertOrUpdateRecord(updatedRecord)
            }
        }
        db.stockReceiptDao().deleteReceiptItems(receipt.id)
        db.stockReceiptDao().deleteReceipt(receipt.id)

        db.auditDao().insertAuditLog(
            AuditLog(
                id = UUID.randomUUID().toString(),
                userRole = userRole,
                actionType = "DELETE_RECEIPT",
                targetEntity = "StockReceipt (${receipt.invoiceNo})",
                changedFields = "deleted",
                oldValue = "₹${receipt.totalAmount}",
                newValue = "Deleted"
            )
        )
    }

    suspend fun exportCSV(
        date: String,
        records: List<DailyStockRecord>,
        productsMap: Map<String, Product>
    ): String = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        sb.append("Date,Category,Brand,Product Name,Bottle Size,Opening Stock,Received Qty,Breakage Qty,Total Available,Closing Stock,Sales Qty,Selling Rate (INR),Total Sales Value (INR),Cost Price (INR),Gross Profit (INR),Status\n")

        var totalRev = 0.0
        var totalProfit = 0.0
        var totalSold = 0

        for (r in records) {
            val p = productsMap[r.productId]
            val category = p?.category ?: "Unknown"
            val brand = p?.brand ?: "Unknown"
            val name = p?.name ?: "Unknown"
            val size = p?.bottleSizeMl ?: ""
            val status = if (r.closingStock == 0) "OUT_OF_STOCK" else if (r.closingStock <= (p?.minStock ?: 10)) "LOW_STOCK" else "OK"

            totalRev += r.totalSalesValue
            totalProfit += r.grossProfit
            totalSold += r.salesQty

            sb.append("\"$date\",")
            sb.append("\"$category\",")
            sb.append("\"$brand\",")
            sb.append("\"$name\",")
            sb.append("\"$size\",")
            sb.append("${r.openingStock},")
            sb.append("${r.receivedQty},")
            sb.append("${r.breakageQty},")
            sb.append("${r.totalAvailable},")
            sb.append("${r.closingStock},")
            sb.append("${r.salesQty},")
            sb.append("${r.rate},")
            sb.append("${r.totalSalesValue},")
            sb.append("${r.costPrice},")
            sb.append("${r.grossProfit},")
            sb.append("\"$status\"\n")
        }

        sb.append("\nSUMMARY\n")
        sb.append("Total Bottles Sold,$totalSold\n")
        sb.append("Total Sales Revenue (INR),$totalRev\n")
        sb.append("Total Estimated Gross Profit (INR),$totalProfit\n")

        sb.toString()
    }

    suspend fun exportJSONBackup(): String = withContext(Dispatchers.IO) {
        val products = db.productDao().getAllProducts().first()
        val json = JSONObject()

        val productsArray = JSONArray()
        for (p in products) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("brand", p.brand)
            obj.put("name", p.name)
            obj.put("category", p.category)
            obj.put("bottleSizeMl", p.bottleSizeMl)
            obj.put("minStock", p.minStock)
            obj.put("costPrice", p.costPrice)
            obj.put("defaultRate", p.defaultRate)
            obj.put("initialOpeningStock", p.initialOpeningStock)
            obj.put("isActive", p.isActive)
            productsArray.put(obj)
        }
        json.put("products", productsArray)
        json.toString(2)
    }

    suspend fun resetDatabase() = withContext(Dispatchers.IO) {
        db.clearAllTables()
        seedDatabaseIfEmpty()
    }
}
