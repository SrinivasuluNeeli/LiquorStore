package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuditLog
import com.example.data.model.Product
import com.example.ui.components.CategoryStockMovementCard
import com.example.ui.components.DailySalesTrendCard
import com.example.ui.components.MonthlyExpenseDistributionCard
import com.example.ui.components.PdfExportPreviewDialog
import com.example.ui.viewmodel.LiquorViewModel
import com.example.ui.viewmodel.StockRecordItemUiState
import com.example.util.PdfReportGenerator
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsAuditScreen(
    viewModel: LiquorViewModel,
    onOpenAdminPinDialog: () -> Unit,
    onOpenAddProductDialog: (Product?) -> Unit
) {
    val context = LocalContext.current
    val role by viewModel.currentUserRole.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val daySummary by viewModel.currentDaySummary.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()
    val recordsList by viewModel.filteredDayRecords.collectAsState()
    val dayRecords by viewModel.currentDayRecords.collectAsState()
    val productsList by viewModel.products.collectAsState()
    val categoryMovements by viewModel.categoryStockMovement.collectAsState()
    val allExpenses by viewModel.allExpenses.collectAsState()
    val allReceipts by viewModel.allReceipts.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Dashboard", "Financials", "Master Catalog", "Audit Trail", "Export / Data")

    var generatedPdfFile by remember { mutableStateOf<File?>(null) }
    var showPdfPreviewDialog by remember { mutableStateOf(false) }

    val totalEstRevenue = recordsList.sumOf { it.record.totalSalesValue }
    val totalEstProfit = recordsList.sumOf { it.record.grossProfit }
    val totalBreakageQty = recordsList.sumOf { it.record.breakageQty }
    val totalUnitsSold = recordsList.sumOf { it.record.salesQty }

    val handleGeneratePdf = {
        val effectiveRecords = if (dayRecords.isNotEmpty()) dayRecords else recordsList
        val pdfFile = PdfReportGenerator.generateDailySummaryPdf(
            context = context,
            selectedDate = selectedDate,
            daySummary = daySummary,
            recordsList = effectiveRecords,
            categoryMovements = categoryMovements,
            dailyExpenses = allExpenses,
            stockReceipts = allReceipts,
            role = role
        )
        if (pdfFile != null) {
            generatedPdfFile = pdfFile
            showPdfPreviewDialog = true
            viewModel.showToast("Daily Accounting PDF generated successfully!")
        } else {
            viewModel.showToast("Failed to generate PDF document.")
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = handleGeneratePdf,
                icon = {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "PDF Report",
                        modifier = Modifier.size(20.dp)
                    )
                },
                text = {
                    Text(
                        text = "Print Daily PDF",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(bottom = 72.dp, end = 4.dp)
                    .testTag("generate_pdf_fab")
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top Header
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Store Analytics & Reports",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Active Role: $role ${if (role == "Admin") "(Full Access)" else "(View Mode)"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (role != "Admin") {
                        Button(
                            onClick = onOpenAdminPinDialog,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("admin_login_button")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Admin PIN", fontSize = 12.sp)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { viewModel.switchRoleToStaff() },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Lock Admin", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Tab Selector
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                edgePadding = 14.dp,
                modifier = Modifier.padding(horizontal = 14.dp).clip(RoundedCornerShape(12.dp))
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
                when (selectedTab) {
                    0 -> DashboardOverviewTabContent(
                        viewModel = viewModel,
                        selectedDate = selectedDate,
                        totalEstRevenue = totalEstRevenue,
                        totalEstProfit = totalEstProfit,
                        recordsList = recordsList
                    )
                    1 -> FinancialsAndItemizedTabContent(
                        selectedDate = selectedDate,
                        totalEstRevenue = totalEstRevenue,
                        totalEstProfit = totalEstProfit,
                        totalBreakageQty = totalBreakageQty,
                        daySummary = daySummary,
                        recordsList = recordsList,
                        role = role,
                        onReopenDay = { viewModel.reopenDay() }
                    )
                    2 -> MasterCatalogReorderTabContent(
                        productsList = productsList,
                        role = role,
                        onOpenAddProductDialog = onOpenAddProductDialog,
                        onDeleteProduct = { viewModel.deleteProduct(it) },
                        onReorderCategory = { cat, list -> viewModel.reorderProductsInCategory(cat, list) }
                    )
                    3 -> AuditTrailTabContent(auditLogs = auditLogs)
                    4 -> ExportDataTabContent(
                        viewModel = viewModel,
                        context = context,
                        role = role,
                        onGeneratePdf = handleGeneratePdf
                    )
                }
            }
        }
    }

    if (showPdfPreviewDialog && generatedPdfFile != null) {
        PdfExportPreviewDialog(
            pdfFile = generatedPdfFile!!,
            selectedDate = selectedDate,
            totalRevenue = totalEstRevenue,
            totalProfit = totalEstProfit,
            totalBottlesSold = totalUnitsSold,
            context = context,
            onDismiss = { showPdfPreviewDialog = false }
        )
    }
}

@Composable
fun DashboardOverviewTabContent(
    viewModel: LiquorViewModel,
    selectedDate: String,
    totalEstRevenue: Double,
    totalEstProfit: Double,
    recordsList: List<StockRecordItemUiState>
) {
    val salesTrends by viewModel.salesTrendData.collectAsState()
    val expenseBreakdown by viewModel.monthlyExpenseBreakdown.collectAsState()
    val categoryMovement by viewModel.categoryStockMovement.collectAsState()
    val allExpenses by viewModel.allExpenses.collectAsState()
    val allReceipts by viewModel.allReceipts.collectAsState()

    val totalMonthlyExpenses = remember(allExpenses) { allExpenses.sumOf { it.amount } }
    val totalStockValuationMRP = remember(recordsList) { recordsList.sumOf { it.record.closingStock * it.record.rate } }
    val totalStockValuationCost = remember(recordsList) { recordsList.sumOf { it.record.closingStock * it.record.costPrice } }
    val totalUnitsSold = remember(recordsList) { recordsList.sumOf { it.record.salesQty } }
    val profitMarginPct = remember(totalEstRevenue, totalEstProfit) {
        if (totalEstRevenue > 0) ((totalEstProfit / totalEstRevenue) * 100).toInt() else 0
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Executive Scorecards Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 1: Today's Revenue & Margin
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("SALES ($selectedDate)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${totalEstRevenue.toInt()}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$totalUnitsSold Bottles | $profitMarginPct% Gross Margin",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                // Card 2: Inventory Value & Monthly Outflow
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("STOCK VALUATION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${totalStockValuationMRP.toInt()}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Cost: ₹${totalStockValuationCost.toInt()} | Exp: ₹${totalMonthlyExpenses.toInt()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // 1. Daily Sales Trends (Line / Area / Bezier Curve with scrubber tooltips)
        item {
            DailySalesTrendCard(dataPoints = salesTrends)
        }

        // 2. Monthly Expense Distribution (Donut / Pie Chart & Breakdown)
        item {
            MonthlyExpenseDistributionCard(expenseItems = expenseBreakdown)
        }

        // 3. Category-Wise Stock Movement (Grouped / Stacked Bar Visualizer)
        item {
            CategoryStockMovementCard(movementItems = categoryMovement)
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun FinancialsAndItemizedTabContent(
    selectedDate: String,
    totalEstRevenue: Double,
    totalEstProfit: Double,
    totalBreakageQty: Int,
    daySummary: com.example.data.model.DaySummary?,
    recordsList: List<StockRecordItemUiState>,
    role: String,
    onReopenDay: () -> Unit
) {
    val soldItems = remember(recordsList) {
        recordsList.filter { it.record.salesQty > 0 }.sortedByDescending { it.record.totalSalesValue }
    }

    val categorySalesMap = remember(soldItems) {
        soldItems.groupBy { it.product.category }.mapValues { (_, items) ->
            Pair(items.sumOf { it.record.salesQty }, items.sumOf { it.record.totalSalesValue })
        }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // High level metrics
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("FINANCIAL OVERVIEW ($selectedDate)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Total Sales Revenue", style = MaterialTheme.typography.bodySmall)
                            Text("₹${totalEstRevenue.toInt()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Estimated Gross Profit", style = MaterialTheme.typography.bodySmall)
                            Text("₹${totalEstProfit.toInt()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        // Category Breakdown Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("CATEGORY-WISE SALES SUMMARY", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (categorySalesMap.isEmpty()) {
                        Text("No sales recorded for any category today.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        categorySalesMap.forEach { (category, pair) ->
                            val (qty, value) = pair
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(category, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "$qty bottles • ₹${value.toInt()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }

        // Itemized Sales Summary Table
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ITEMIZED SALES SUMMARY", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text("${soldItems.size} SKUs Sold", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (soldItems.isEmpty()) {
                        Text("No individual item sales logged for $selectedDate yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("ITEM / SKU", modifier = Modifier.weight(2.2f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text("SOLD", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text("RATE", modifier = Modifier.weight(0.9f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text("TOTAL", modifier = Modifier.weight(1.1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        soldItems.forEach { item ->
                            val p = item.product
                            val r = item.record
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(2.2f)) {
                                    Text("${p.brand} ${p.name}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text("${p.bottleSizeMl} • ${p.category}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("${r.salesQty}", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                Text("₹${r.rate.toInt()}", modifier = Modifier.weight(0.9f), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                                Text(
                                    "₹${r.totalSalesValue.toInt()}",
                                    modifier = Modifier.weight(1.1f),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }

        // Daily settlement card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("SETTLEMENT & AUDIT STATUS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    if (daySummary == null || !daySummary.isClosed) {
                        Text("Status: OPEN (Day in progress)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Staff can record stock closing balances, deliveries, and expenses.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text("Status: CLOSED & LOCKED", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Text("Closed By: ${daySummary.closedBy ?: "Staff"}", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Cash Coll: ₹${daySummary.cashCollected.toInt()}")
                            Text("UPI Coll: ₹${daySummary.upiCollected.toInt()}")
                            Text("Credit: ₹${daySummary.creditSales.toInt()}")
                        }
                        Text("Logged Expenses: ₹${daySummary.totalExpenses.toInt()}", style = MaterialTheme.typography.bodySmall)
                        Text("Net Cash In Hand: ₹${daySummary.netCashInHand.toInt()}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                        if (role == "Admin") {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onReopenDay,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reopen Closed Day (Admin)")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MasterCatalogReorderTabContent(
    productsList: List<Product>,
    role: String,
    onOpenAddProductDialog: (Product?) -> Unit,
    onDeleteProduct: (Product) -> Unit,
    onReorderCategory: (String, List<Product>) -> Unit
) {
    val categories = listOf("Whisky", "Beer", "Brandy", "Rum", "Vodka", "Wine")
    var selectedCat by remember { mutableStateOf("Whisky") }
    var productToDelete by remember { mutableStateOf<Product?>(null) }

    val categoryProducts = remember(productsList, selectedCat) {
        productsList.filter { it.category.equals(selectedCat, ignoreCase = true) }
    }

    var mutableList by remember(categoryProducts) {
        mutableStateOf(categoryProducts)
    }

    if (productToDelete != null) {
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Remove Product from Master Catalog?") },
            text = { Text("Are you sure you want to remove '${productToDelete!!.brand} ${productToDelete!!.name}' (${productToDelete!!.bottleSizeMl}) from the master catalog?") },
            confirmButton = {
                Button(
                    onClick = {
                        val toDel = productToDelete!!
                        productToDelete = null
                        onDeleteProduct(toDel)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete SKU")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { productToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Master SKU Catalog", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Grouped by category • Long press to drag & rearrange", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (role == "Admin") {
                Button(
                    onClick = { onOpenAddProductDialog(null) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("add_new_sku_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add SKU")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Category Filter Chips for Grouping
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(categories) { cat ->
                val isSelected = cat.equals(selectedCat, ignoreCase = true)
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCat = cat },
                    label = { Text(cat, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (mutableList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No SKUs found in $selectedCat category.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(mutableList, key = { _, item -> item.id }) { index, p ->
                    ReorderableProductCard(
                        product = p,
                        index = index,
                        role = role,
                        canMoveUp = index > 0,
                        canMoveDown = index < mutableList.size - 1,
                        onMoveUp = {
                            if (index > 0) {
                                val current = mutableList.toMutableList()
                                val temp = current[index]
                                current[index] = current[index - 1]
                                current[index - 1] = temp
                                mutableList = current
                                onReorderCategory(selectedCat, current)
                            }
                        },
                        onMoveDown = {
                            if (index < mutableList.size - 1) {
                                val current = mutableList.toMutableList()
                                val temp = current[index]
                                current[index] = current[index + 1]
                                current[index + 1] = temp
                                mutableList = current
                                onReorderCategory(selectedCat, current)
                            }
                        },
                        onEdit = { onOpenAddProductDialog(p) },
                        onDelete = { productToDelete = p }
                    )
                }
            }
        }
    }
}

@Composable
fun ReorderableProductCard(
    product: Product,
    index: Int,
    role: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { },
                    onDragEnd = { },
                    onDragCancel = { },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (dragAmount.y < -20 && canMoveUp) {
                            onMoveUp()
                        } else if (dragAmount.y > 20 && canMoveDown) {
                            onMoveDown()
                        }
                    }
                )
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag handle & order indicator
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    Icons.Default.DragHandle,
                    contentDescription = "Drag to reorder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${product.brandName} ${product.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = product.sku,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                fontSize = 10.sp
                            )
                        }
                    }
                    Text(
                        text = "${product.unitSize} • Stock: ${product.currentStockLevel} • Reorder: ${product.reorderPoint} • Rate: ₹${product.defaultRate.toInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Move actions & edit / delete
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (role == "Admin") {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = canMoveUp,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = onMoveDown,
                        enabled = canMoveDown,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit SKU", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove SKU", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AuditTrailTabContent(auditLogs: List<AuditLog>) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }

    if (auditLogs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No audit log records found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(auditLogs, key = { it.id }) { log ->
                val roleColor = when (log.userRole.lowercase()) {
                    "admin" -> Color(0xFFD32F2F)
                    "staff" -> Color(0xFF1976D2)
                    else -> Color(0xFF616161)
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = roleColor.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = log.userRole.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = roleColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = log.actionType.replace("_", " "),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = dateFormat.format(Date(log.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        Text(
                            text = log.targetEntity,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (log.changedFields.isNotBlank()) {
                            Text(
                                text = "Changes: ${log.changedFields}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Previous vs New Values Diff block
                        if (log.oldValue.isNotBlank() || log.newValue.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (log.oldValue.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("BEFORE: ", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                                            Text(log.oldValue, style = MaterialTheme.typography.bodySmall, color = Color(0xFFC62828))
                                        }
                                    }
                                    if (log.newValue.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("AFTER:  ", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                            Text(log.newValue, style = MaterialTheme.typography.bodySmall, color = Color(0xFF1B5E20), fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExportDataTabContent(
    viewModel: LiquorViewModel,
    context: Context,
    role: String,
    onGeneratePdf: () -> Unit
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val dayRecords by viewModel.currentDayRecords.collectAsState()
    val categoryMovements by viewModel.categoryStockMovement.collectAsState()
    val allExpenses by viewModel.allExpenses.collectAsState()
    val allReceipts by viewModel.allReceipts.collectAsState()
    val daySummary by viewModel.currentDaySummary.collectAsState()

    var showDownloadDialog by remember { mutableStateOf(false) }
    var generatedExcelFile by remember { mutableStateOf<java.io.File?>(null) }

    if (showDownloadDialog) {
        com.example.ui.components.DownloadStockSheetDialog(
            initialDate = selectedDate,
            viewModel = viewModel,
            onDismiss = { showDownloadDialog = false }
        )
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Universal Download Dialog trigger card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "DAILY STOCK SHEET DOWNLOADER",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "Select ANY date to download printable PDF or Excel sheet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showDownloadDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        modifier = Modifier.fillMaxWidth().testTag("open_download_any_day_dialog_btn")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download Any Day's Stock Sheet (PDF / Excel)")
                    }
                }
            }
        }

        // PDF Export Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PRINTABLE DAILY AUDIT PDF",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Generates a clean, 2-page native PDF document for $selectedDate containing executive accounting summary, category movement register, daily expenses log, and official audit sign-off lines.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onGeneratePdf,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("export_pdf_button")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate & Print PDF for $selectedDate")
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("EXCEL / CSV DETAILED WORKSHEET ($selectedDate)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Generates formatted spreadsheet with executive financial KPIs, cash & expense settlement, category movement matrix, and complete SKU inventory register with sorting by category & bottle size descending.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val file = com.example.util.ExcelReportGenerator.generateDailyExcelReport(
                                    context = context,
                                    selectedDate = selectedDate,
                                    daySummary = daySummary,
                                    recordsList = dayRecords,
                                    categoryMovements = categoryMovements,
                                    dailyExpenses = allExpenses,
                                    stockReceipts = allReceipts,
                                    role = role
                                )
                                generatedExcelFile = file
                                if (file != null) {
                                    com.example.util.ExcelReportGenerator.openExcelFile(context, file)
                                } else {
                                    viewModel.showToast("Failed to generate Excel sheet.")
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("export_excel_button")
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open in Excel")
                        }

                        if (generatedExcelFile != null) {
                            OutlinedButton(
                                onClick = {
                                    com.example.util.ExcelReportGenerator.shareExcelFile(context, generatedExcelFile!!)
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share Excel")
                            }
                        }
                    }
                }
            }
        }

        if (role == "Admin") {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("DATABASE BACKUP & RESET (ADMIN)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.resetDatabase()
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset to 153 Seed SKUs")
                            }
                        }
                    }
                }
            }
        }
    }
}
