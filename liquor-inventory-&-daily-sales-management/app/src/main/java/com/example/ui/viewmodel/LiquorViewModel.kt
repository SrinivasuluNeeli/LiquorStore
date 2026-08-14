package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.LiquorRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class StockRecordItemUiState(
    val record: DailyStockRecord,
    val product: Product
)

data class DailySalesTrendPoint(
    val date: String,
    val formattedDate: String,
    val totalRevenue: Double,
    val totalBottles: Int,
    val grossProfit: Double
)

data class MonthlyExpenseCategoryItem(
    val category: String,
    val totalAmount: Double,
    val percentage: Float,
    val count: Int
)

data class CategoryStockMovementItem(
    val category: String,
    val openingStock: Int,
    val receivedQty: Int,
    val salesQty: Int,
    val breakageQty: Int,
    val closingStock: Int,
    val salesValue: Double
)

class LiquorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LiquorRepository

    val selectedDate = MutableStateFlow("2026-08-13")
    val selectedCategory = MutableStateFlow("All")
    val searchQuery = MutableStateFlow("")
    val stockAlertFilter = MutableStateFlow("All") // All, Low Stock, Out of Stock, Discrepancies
    val isGridView = MutableStateFlow(false)

    val currentUserRole = MutableStateFlow("Staff") // "Staff" or "Admin"
    val isAdminUnlocked = MutableStateFlow(false)
    val adminPin = MutableStateFlow("1234")

    val snackbarMessage = MutableStateFlow<String?>(null)

    val products: StateFlow<List<Product>>
    val allExpenses: StateFlow<List<DailyExpense>>
    val allReceipts: StateFlow<List<StockReceipt>>
    val auditLogs: StateFlow<List<AuditLog>>
    val allDailyRecords: StateFlow<List<DailyStockRecord>>

    init {
        val db = AppDatabase.getDatabase(application)
        repository = LiquorRepository(db)

        viewModelScope.launch {
            repository.seedDatabaseIfEmpty()
            repository.ensureRecordsForDate(selectedDate.value)
        }

        products = repository.allProducts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allExpenses = repository.allExpenses.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allReceipts = repository.allReceipts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        auditLogs = repository.allAuditLogs.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allDailyRecords = repository.allDailyStockRecords.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    val salesTrendData: StateFlow<List<DailySalesTrendPoint>> = allDailyRecords
        .map { records ->
            if (records.isEmpty()) return@map emptyList()
            val grouped = records.groupBy { it.date }
            val sortedDates = grouped.keys.sorted()
            sortedDates.map { dStr ->
                val dayRecords = grouped[dStr] ?: emptyList()
                val revenue = dayRecords.sumOf { it.totalSalesValue }
                val units = dayRecords.sumOf { it.salesQty }
                val profit = dayRecords.sumOf { it.grossProfit }
                
                // Formatted date label e.g. "07 Aug"
                val label = try {
                    val inFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    val outFormat = java.text.SimpleDateFormat("dd MMM", java.util.Locale.US)
                    inFormat.parse(dStr)?.let { outFormat.format(it) } ?: dStr
                } catch (e: Exception) {
                    dStr
                }

                DailySalesTrendPoint(
                    date = dStr,
                    formattedDate = label,
                    totalRevenue = revenue,
                    totalBottles = units,
                    grossProfit = profit
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val monthlyExpenseBreakdown: StateFlow<List<MonthlyExpenseCategoryItem>> = allExpenses
        .map { expenses ->
            if (expenses.isEmpty()) return@map emptyList()
            val total = expenses.sumOf { it.amount }
            val grouped = expenses.groupBy { it.category }
            grouped.map { (cat, list) ->
                val sum = list.sumOf { it.amount }
                val pct = if (total > 0) ((sum / total) * 100).toFloat() else 0f
                MonthlyExpenseCategoryItem(
                    category = cat,
                    totalAmount = sum,
                    percentage = pct,
                    count = list.size
                )
            }.sortedByDescending { it.totalAmount }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentDayRecords: StateFlow<List<StockRecordItemUiState>> = selectedDate
        .flatMapLatest { date ->
            repository.ensureRecordsForDate(date)
            combine(
                repository.getRecordsForDate(date),
                products
            ) { records, productList ->
                val productMap = productList.associateBy { it.id }
                records.mapNotNull { r ->
                    productMap[r.productId]?.let { p ->
                        StockRecordItemUiState(record = r, product = p)
                    }
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val categoryStockMovement: StateFlow<List<CategoryStockMovementItem>> = combine(
        currentDayRecords,
        products
    ) { currentRecords, _ ->
        if (currentRecords.isEmpty()) return@combine emptyList()
        val grouped = currentRecords.groupBy { it.product.category }
        grouped.map { (cat, items) ->
            CategoryStockMovementItem(
                category = cat,
                openingStock = items.sumOf { it.record.openingStock },
                receivedQty = items.sumOf { it.record.receivedQty },
                salesQty = items.sumOf { it.record.salesQty },
                breakageQty = items.sumOf { it.record.breakageQty },
                closingStock = items.sumOf { it.record.closingStock },
                salesValue = items.sumOf { it.record.totalSalesValue }
            )
        }.sortedByDescending { it.salesValue }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private fun parseBottleSizeMl(sizeStr: String): Int {
        val clean = sizeStr.trim().uppercase()
        if (clean.contains("L") && !clean.contains("ML")) {
            val num = clean.replace("L", "").trim().toDoubleOrNull() ?: 0.0
            return (num * 1000).toInt()
        }
        val digits = clean.filter { it.isDigit() }
        return digits.toIntOrNull() ?: 0
    }

    private val standardCategoryOrder = listOf("Whisky", "Beer", "Brandy", "Rum", "Vodka", "Wine")

    val filteredDayRecords: StateFlow<List<StockRecordItemUiState>> = combine(
        currentDayRecords,
        selectedCategory,
        searchQuery,
        stockAlertFilter
    ) { items, cat, query, alert ->
        items.filter { item ->
            val p = item.product
            val r = item.record

            val matchesCategory = (cat == "All" || p.category.equals(cat, ignoreCase = true))
            val matchesQuery = query.isBlank() ||
                    p.brand.contains(query, ignoreCase = true) ||
                    p.name.contains(query, ignoreCase = true) ||
                    p.bottleSizeMl.contains(query, ignoreCase = true)

            val matchesAlert = when (alert) {
                "Low Stock" -> r.closingStock > 0 && r.closingStock <= p.minStock
                "Out of Stock" -> r.closingStock == 0
                "Discrepancies" -> r.breakageQty > 0
                else -> true
            }

            matchesCategory && matchesQuery && matchesAlert
        }.sortedWith(
            compareBy<StockRecordItemUiState> { item ->
                val idx = standardCategoryOrder.indexOfFirst { it.equals(item.product.category, ignoreCase = true) }
                if (idx >= 0) idx else 999
            }
            .thenBy { it.product.brand.lowercase() }
            .thenBy { it.product.name.lowercase() }
            .thenByDescending { parseBottleSizeMl(it.product.bottleSizeMl) }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentDaySummary: StateFlow<DaySummary?> = selectedDate
        .flatMapLatest { date ->
            repository.getDaySummaryFlow(date)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentDayExpenses: StateFlow<List<DailyExpense>> = selectedDate
        .flatMapLatest { date ->
            repository.getExpensesForDate(date)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentDayReceipts: StateFlow<List<StockReceipt>> = selectedDate
        .flatMapLatest { date ->
            repository.getReceiptsForDate(date)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun changeDate(newDate: String) {
        selectedDate.value = newDate
    }

    fun verifyAdminPin(enteredPin: String): Boolean {
        if (enteredPin == adminPin.value) {
            isAdminUnlocked.value = true
            currentUserRole.value = "Admin"
            showToast("Admin access unlocked!")
            return true
        } else {
            showToast("Invalid PIN. Access denied.")
            return false
        }
    }

    fun switchRoleToStaff() {
        currentUserRole.value = "Staff"
        isAdminUnlocked.value = false
        showToast("Switched to Staff role.")
    }

    fun updateStockRecord(record: DailyStockRecord, auditNote: String? = null) {
        viewModelScope.launch {
            repository.updateStockRecord(record, currentUserRole.value, auditNote)
        }
    }

    fun updatePastStockRecord(record: DailyStockRecord, auditNote: String) {
        viewModelScope.launch {
            repository.updateStockRecord(record, currentUserRole.value, auditNote)
            showToast("Past stock updated for ${record.date}!")
        }
    }

    fun updateBatchClosingStock(closingMap: Map<String, Int>) {
        viewModelScope.launch {
            val date = selectedDate.value
            val currentList = currentDayRecords.value
            for (item in currentList) {
                val newClosing = closingMap[item.product.id]
                if (newClosing != null && newClosing != item.record.closingStock) {
                    val updated = item.record.copy(closingStock = newClosing)
                    repository.updateStockRecord(updated, currentUserRole.value)
                }
            }
            showToast("Batch closing stock updated!")
        }
    }

    fun addStockDelivery(
        supplierName: String,
        invoiceNo: String,
        invoiceDate: String,
        notes: String,
        items: List<Pair<Product, Int>> // Product & quantity
    ) {
        viewModelScope.launch {
            val receiptId = java.util.UUID.randomUUID().toString()
            val totalAmt = items.sumOf { it.first.costPrice * it.second }
            val receipt = StockReceipt(
                id = receiptId,
                receiptNo = "REC-${System.currentTimeMillis().toString().takeLast(6)}",
                supplierName = supplierName,
                invoiceNo = invoiceNo,
                invoiceDate = invoiceDate,
                totalAmount = totalAmt,
                notes = notes
            )
            val receiptItems = items.map { (prod, qty) ->
                StockReceiptItem(
                    id = java.util.UUID.randomUUID().toString(),
                    receiptId = receiptId,
                    productId = prod.id,
                    quantity = qty,
                    unitCost = prod.costPrice
                )
            }
            repository.addStockReceipt(receipt, receiptItems, currentUserRole.value)
            showToast("Stock receipt saved successfully!")
        }
    }

    fun addExpense(category: String, amount: Double, paymentMode: String, remarks: String) {
        viewModelScope.launch {
            val expense = DailyExpense(
                id = java.util.UUID.randomUUID().toString(),
                date = selectedDate.value,
                category = category,
                amount = amount,
                paymentMode = paymentMode,
                remarks = remarks
            )
            repository.addExpense(expense, currentUserRole.value)
            showToast("Expense logged: ₹${amount.toInt()} ($category)")
        }
    }

    fun updateExpense(oldExpense: DailyExpense, updatedExpense: DailyExpense) {
        viewModelScope.launch {
            repository.updateExpense(oldExpense, updatedExpense, currentUserRole.value)
            showToast("Expense updated: ₹${updatedExpense.amount.toInt()} (${updatedExpense.category})")
        }
    }

    fun deleteExpense(expense: DailyExpense) {
        viewModelScope.launch {
            repository.deleteExpense(expense, currentUserRole.value)
            showToast("Expense deleted.")
        }
    }

    fun closeDay(cash: Double, upi: Double, credit: Double, notes: String) {
        viewModelScope.launch {
            repository.closeDay(
                date = selectedDate.value,
                cashCollected = cash,
                upiCollected = upi,
                creditSales = credit,
                notes = notes,
                userRole = currentUserRole.value
            )
            showToast("Day ${selectedDate.value} closed and locked!")
        }
    }

    fun reopenDay() {
        if (currentUserRole.value != "Admin") {
            showToast("Only Admin can reopen a closed day.")
            return
        }
        viewModelScope.launch {
            repository.reopenDay(selectedDate.value, currentUserRole.value)
            showToast("Day ${selectedDate.value} unlocked for editing.")
        }
    }

    fun saveProduct(product: Product) {
        if (currentUserRole.value != "Admin") {
            showToast("Admin access required to modify catalog.")
            return
        }
        viewModelScope.launch {
            repository.saveProduct(product, currentUserRole.value)
            showToast("Product saved: ${product.brand} ${product.name}")
        }
    }

    fun deleteProduct(product: Product) {
        if (currentUserRole.value != "Admin") {
            showToast("Admin access required to delete items.")
            return
        }
        viewModelScope.launch {
            repository.deleteProduct(product, currentUserRole.value)
            showToast("Product deleted: ${product.brand} ${product.name}")
        }
    }

    fun reorderProductsInCategory(category: String, reorderedList: List<Product>) {
        viewModelScope.launch {
            val updated = reorderedList.mapIndexed { index, product ->
                product.copy(displayOrder = index)
            }
            repository.updateProductsOrder(updated)
            showToast("Catalog order updated")
        }
    }

    suspend fun getReceiptItemsForReceipt(receiptId: String): List<StockReceiptItem> {
        return repository.getReceiptItems(receiptId)
    }

    fun updateStockDelivery(
        oldReceipt: StockReceipt,
        supplierName: String,
        invoiceNo: String,
        invoiceDate: String,
        notes: String,
        items: List<Pair<Product, Int>>
    ) {
        viewModelScope.launch {
            val totalAmt = items.sumOf { it.first.costPrice * it.second }
            val updatedReceipt = oldReceipt.copy(
                supplierName = supplierName,
                invoiceNo = invoiceNo,
                invoiceDate = invoiceDate,
                totalAmount = totalAmt,
                notes = notes
            )
            val receiptItems = items.map { (prod, qty) ->
                StockReceiptItem(
                    id = java.util.UUID.randomUUID().toString(),
                    receiptId = oldReceipt.id,
                    productId = prod.id,
                    quantity = qty,
                    unitCost = prod.costPrice
                )
            }
            repository.updateStockReceipt(oldReceipt, updatedReceipt, receiptItems, currentUserRole.value)
            showToast("Delivery updated successfully!")
        }
    }

    fun deleteStockDelivery(receipt: StockReceipt) {
        viewModelScope.launch {
            repository.deleteStockReceipt(receipt, currentUserRole.value)
            showToast("Delivery invoice #${receipt.invoiceNo} deleted.")
        }
    }

    fun resetDatabase() {
        if (currentUserRole.value != "Admin") {
            showToast("Admin access required.")
            return
        }
        viewModelScope.launch {
            repository.resetDatabase()
            showToast("Database reset to initial 153 SKUs state.")
        }
    }

    val auditSearchQuery = MutableStateFlow("")
    val auditRoleFilter = MutableStateFlow("All") // "All", "Admin", "Staff", "System"
    val auditActionFilter = MutableStateFlow("All") // "All", "Stock Updates", "Deliveries", "Expenses", "Catalog & Price", "Day Close/Reopen"
    val auditDateFilter = MutableStateFlow("All") // "All", "Today", "Yesterday", "Last 7 Days"

    val filteredAuditLogs: StateFlow<List<AuditLog>> = combine(
        auditLogs,
        auditSearchQuery,
        auditRoleFilter,
        auditActionFilter,
        auditDateFilter
    ) { logs, query, role, action, dateFilter ->
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        logs.filter { log ->
            // Role filter
            val matchesRole = when (role) {
                "All" -> true
                else -> log.userRole.equals(role, ignoreCase = true)
            }

            // Action filter
            val matchesAction = when (action) {
                "All" -> true
                "Stock Updates" -> log.actionType.contains("STOCK", ignoreCase = true) || log.actionType.contains("BREAKAGE", ignoreCase = true)
                "Deliveries" -> log.actionType.contains("RECEIPT", ignoreCase = true)
                "Expenses" -> log.actionType.contains("EXPENSE", ignoreCase = true)
                "Catalog & Price" -> log.actionType.contains("PRODUCT", ignoreCase = true) || log.actionType.contains("PRICE", ignoreCase = true)
                "Day Close/Reopen" -> log.actionType.contains("DAY", ignoreCase = true)
                else -> log.actionType.equals(action, ignoreCase = true)
            }

            // Date filter (based on log timestamp)
            val matchesDate = when (dateFilter) {
                "Today" -> {
                    val cal = java.util.Calendar.getInstance().apply { timeInMillis = log.timestamp }
                    val logDate = sdf.format(cal.time)
                    logDate == selectedDate.value || logDate == "2026-08-13"
                }
                "Yesterday" -> {
                    val cal = java.util.Calendar.getInstance().apply { timeInMillis = log.timestamp }
                    val logDate = sdf.format(cal.time)
                    logDate == "2026-08-12"
                }
                "Last 7 Days" -> {
                    val sevenDaysAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000L
                    log.timestamp >= sevenDaysAgo
                }
                else -> true
            }

            // Search query filter
            val q = query.trim()
            val matchesQuery = if (q.isBlank()) {
                true
            } else {
                log.targetEntity.contains(q, ignoreCase = true) ||
                log.actionType.contains(q, ignoreCase = true) ||
                log.userRole.contains(q, ignoreCase = true) ||
                log.changedFields.contains(q, ignoreCase = true) ||
                log.oldValue.contains(q, ignoreCase = true) ||
                log.newValue.contains(q, ignoreCase = true)
            }

            matchesRole && matchesAction && matchesDate && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setAuditSearchQuery(query: String) {
        auditSearchQuery.value = query
    }

    fun setAuditRoleFilter(role: String) {
        auditRoleFilter.value = role
    }

    fun setAuditActionFilter(action: String) {
        auditActionFilter.value = action
    }

    fun setAuditDateFilter(filter: String) {
        auditDateFilter.value = filter
    }

    fun resetAuditFilters() {
        auditSearchQuery.value = ""
        auditRoleFilter.value = "All"
        auditActionFilter.value = "All"
        auditDateFilter.value = "All"
    }

    fun exportAuditTrailAsText(): String {
        val list = filteredAuditLogs.value
        val sb = StringBuilder()
        sb.appendLine("==================================================")
        sb.appendLine("LIQUOR INVENTORY & STORE AUDIT TRAIL LOG")
        sb.appendLine("Exported on: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}")
        sb.appendLine("Total Filtered Records: ${list.size}")
        sb.appendLine("==================================================")
        sb.appendLine()
        for ((idx, log) in list.withIndex()) {
            val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date(log.timestamp))
            sb.appendLine("[#${idx + 1}] $dateStr | User: ${log.userRole.uppercase()} | Action: ${log.actionType}")
            sb.appendLine("Target: ${log.targetEntity}")
            if (log.changedFields.isNotBlank()) sb.appendLine("Fields Changed: ${log.changedFields}")
            if (log.oldValue.isNotBlank()) sb.appendLine("Previous Value: ${log.oldValue}")
            if (log.newValue.isNotBlank()) sb.appendLine("New Value:      ${log.newValue}")
            sb.appendLine("--------------------------------------------------")
        }
        return sb.toString()
    }

    fun exportAuditTrailAsCsv(): String {
        val list = filteredAuditLogs.value
        val sb = StringBuilder()
        sb.appendLine("Log ID,Timestamp,Date Time,User Role,Action Type,Target Entity,Changed Fields,Previous Value,New Value")
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
        for (log in list) {
            val dateStr = sdf.format(java.util.Date(log.timestamp))
            fun escape(s: String) = "\"${s.replace("\"", "\"\"")}\""
            sb.appendLine("${escape(log.id)},${log.timestamp},${escape(dateStr)},${escape(log.userRole)},${escape(log.actionType)},${escape(log.targetEntity)},${escape(log.changedFields)},${escape(log.oldValue)},${escape(log.newValue)}")
        }
        return sb.toString()
    }

    fun getExportCSV(): String? {
        val records = currentDayRecords.value.map { it.record }
        val pMap = products.value.associateBy { it.id }
        return kotlinx.coroutines.runBlocking {
            repository.exportCSV(selectedDate.value, records, pMap)
        }
    }

    fun showToast(msg: String) {
        snackbarMessage.value = msg
    }

    fun clearToast() {
        snackbarMessage.value = null
    }
}
