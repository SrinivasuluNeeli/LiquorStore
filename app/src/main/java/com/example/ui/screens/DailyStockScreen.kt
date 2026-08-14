package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyStockRecord
import com.example.ui.components.AdminStockRecordDialog
import com.example.ui.components.DownloadStockSheetDialog
import com.example.ui.components.PastDatePickerDialog
import com.example.ui.theme.CategoryColors
import com.example.ui.viewmodel.LiquorViewModel
import com.example.ui.viewmodel.StockRecordItemUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyStockScreen(
    viewModel: LiquorViewModel,
    onOpenBatchEntry: () -> Unit,
    onOpenCloseDayModal: () -> Unit,
    onOpenAddReceiptModal: () -> Unit
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val recordsList by viewModel.filteredDayRecords.collectAsState()
    val daySummary by viewModel.currentDaySummary.collectAsState()
    val role by viewModel.currentUserRole.collectAsState()

    val categories = listOf("All", "Whisky", "Beer", "Brandy", "Rum", "Vodka", "Wine")

    // Calculated totals for bottom summary bar
    val totalOpenBottles = recordsList.sumOf { it.record.openingStock }
    val totalRecvBottles = recordsList.sumOf { it.record.receivedQty }
    val totalClosingBottles = recordsList.sumOf { it.record.closingStock }
    val totalBottlesSold = recordsList.sumOf { it.record.salesQty }
    val totalDaySales = recordsList.sumOf { it.record.totalSalesValue }

    val isClosed = daySummary?.isClosed == true
    val isPastDate = selectedDate != "2026-08-13"
    var showDatePicker by remember { mutableStateOf(false) }
    var showDownloadDialog by remember { mutableStateOf(false) }

    // Admin Stock Record Editor Dialog State
    var selectedRecordForAdminEdit by remember { mutableStateOf<StockRecordItemUiState?>(null) }

    if (showDatePicker) {
        PastDatePickerDialog(
            initialDate = selectedDate,
            onDismiss = { showDatePicker = false },
            onDateSelected = { newDate ->
                viewModel.changeDate(newDate)
            }
        )
    }

    if (showDownloadDialog) {
        DownloadStockSheetDialog(
            initialDate = selectedDate,
            viewModel = viewModel,
            onDismiss = { showDownloadDialog = false }
        )
    }

    if (selectedRecordForAdminEdit != null) {
        AdminStockRecordDialog(
            record = selectedRecordForAdminEdit!!.record,
            product = selectedRecordForAdminEdit!!.product,
            onDismiss = { selectedRecordForAdminEdit = null },
            onSaveRecord = { updatedRecord, auditNote ->
                viewModel.updatePastStockRecord(updatedRecord, auditNote)
                selectedRecordForAdminEdit = null
            }
        )
    }

    val groupedByCategory = remember(recordsList) {
        recordsList.groupBy { it.product.category }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Top Date Header & Fluid Past Day Selector
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .clickable { showDatePicker = true }
                            .padding(vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Daily Stock Sheet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Change Date", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Date: $selectedDate",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isPastDate) Color(0xFFE65100) else MaterialTheme.colorScheme.primary
                            )
                            if (isPastDate) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    color = Color(0xFFFFE0B2),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text("PAST", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100), modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = { showDownloadDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f))
                                .testTag("download_sheet_header_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Download Sheet (PDF/Excel)", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }

                        IconButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
                        ) {
                            Icon(Icons.Default.Event, contentDescription = "Pick Date", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }

                        if (isClosed) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = "Closed", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("CLOSED", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        } else {
                            Button(
                                onClick = onOpenCloseDayModal,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("close_day_button")
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Close Day", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Past Day Banner with Quick Admin Action
            if (isPastDate) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFFF3E0),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (role == "Admin") "Admin Past Day Edit Mode Active" else "Viewing Historical Stock ($selectedDate)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (role == "Admin" && isClosed) {
                                TextButton(
                                    onClick = { viewModel.reopenDay() },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Reopen Day", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                }
                            }
                            TextButton(
                                onClick = { viewModel.changeDate("2026-08-13") },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Go to Today", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            // Colorful Category Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = cat.equals(selectedCategory, ignoreCase = true)
                    val catTheme = CategoryColors.forCategory(cat)

                    Surface(
                        onClick = { viewModel.selectedCategory.value = cat },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) catTheme.primary else catTheme.container.copy(alpha = 0.6f),
                        border = if (isSelected) null else CardDefaults.outlinedCardBorder(),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val iconVector = when (cat.lowercase()) {
                                "whisky" -> Icons.Default.WineBar
                                "beer" -> Icons.Default.SportsBar
                                "brandy" -> Icons.Default.LocalBar
                                "rum" -> Icons.Default.Liquor
                                "vodka" -> Icons.Default.WaterDrop
                                "wine" -> Icons.Default.WineBar
                                else -> Icons.Default.AllInclusive
                            }
                            Icon(
                                imageVector = iconVector,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isSelected) Color.White else catTheme.onContainer
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = cat,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else catTheme.onContainer
                            )
                        }
                    }
                }
            }

            // Search Bar & View Toggle & Fast Entry
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search brand or SKU...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("search_sku_input")
                )

                IconButton(
                    onClick = onOpenBatchEntry,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f))
                        .testTag("fast_batch_entry_button")
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = "Fast Batch Entry", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }

                IconButton(
                    onClick = { viewModel.isGridView.value = !isGridView },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                ) {
                    Icon(
                        imageVector = if (isGridView) Icons.Default.TableChart else Icons.Default.GridView,
                        contentDescription = "Toggle Grid/Table"
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Main Data Grid Container
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 14.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ) {
                Column {
                    // Data Grid Table Header (If Table View)
                    if (!isGridView) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("BRAND / SKU", modifier = Modifier.weight(2.2f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("OPEN", modifier = Modifier.weight(0.7f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("RECV", modifier = Modifier.weight(0.7f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("CLOSING", modifier = Modifier.weight(1.3f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("SOLD", modifier = Modifier.weight(0.9f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (role == "Admin") {
                                Text("EDIT", modifier = Modifier.weight(0.5f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }

                    if (recordsList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No liquor SKUs match the selected criteria.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else if (isGridView) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (selectedCategory == "All") {
                                groupedByCategory.forEach { (cat, itemsInCat) ->
                                    val catTheme = CategoryColors.forCategory(cat)
                                    val catSold = itemsInCat.sumOf { it.record.salesQty }
                                    val catRev = itemsInCat.sumOf { it.record.totalSalesValue }

                                    item(span = { GridItemSpan(2) }, key = "grid_header_$cat") {
                                        Surface(
                                            color = catTheme.primary.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = catTheme.primary
                                                    ) {
                                                        Text(
                                                            text = cat.uppercase(),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "${itemsInCat.size} SKUs",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = catTheme.primary
                                                    )
                                                }
                                                Text(
                                                    text = "Sold: $catSold • ₹${catRev.toInt()}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = catTheme.primary
                                                )
                                            }
                                        }
                                    }

                                    items(itemsInCat, key = { it.product.id }) { item ->
                                        StockGridCardItem(
                                            item = item,
                                            isClosed = isClosed,
                                            role = role,
                                            onOpenAdminEdit = { selectedRecordForAdminEdit = item },
                                            onUpdateRecord = { updated -> viewModel.updateStockRecord(updated) }
                                        )
                                    }
                                }
                            } else {
                                items(recordsList, key = { it.product.id }) { item ->
                                    StockGridCardItem(
                                        item = item,
                                        isClosed = isClosed,
                                        role = role,
                                        onOpenAdminEdit = { selectedRecordForAdminEdit = item },
                                        onUpdateRecord = { updated -> viewModel.updateStockRecord(updated) }
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (selectedCategory == "All") {
                                groupedByCategory.forEach { (cat, itemsInCat) ->
                                    val catTheme = CategoryColors.forCategory(cat)
                                    val catSold = itemsInCat.sumOf { it.record.salesQty }
                                    val catRev = itemsInCat.sumOf { it.record.totalSalesValue }

                                    item(key = "header_$cat") {
                                        Surface(
                                            color = catTheme.primary.copy(alpha = 0.10f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = catTheme.primary
                                                    ) {
                                                        Text(
                                                            text = cat.uppercase(),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "${itemsInCat.size} SKUs",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = catTheme.primary
                                                    )
                                                }
                                                Text(
                                                    text = "Sold: $catSold Btls • ₹${catRev.toInt()}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = catTheme.primary
                                                )
                                            }
                                        }
                                    }

                                    items(itemsInCat, key = { it.product.id }) { item ->
                                        StockRowItem(
                                            item = item,
                                            isClosed = isClosed,
                                            role = role,
                                            onOpenAdminEdit = { selectedRecordForAdminEdit = item },
                                            onUpdateRecord = { updated -> viewModel.updateStockRecord(updated) }
                                        )
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    }
                                }
                            } else {
                                items(recordsList, key = { it.product.id }) { item ->
                                    StockRowItem(
                                        item = item,
                                        isClosed = isClosed,
                                        role = role,
                                        onOpenAdminEdit = { selectedRecordForAdminEdit = item },
                                        onUpdateRecord = { updated -> viewModel.updateStockRecord(updated) }
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                }
                            }
                        }
                    }
                }
            }

            // Summary Footer Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("TOTAL ITEMS: ${recordsList.size} SKUs", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Open: $totalOpenBottles • Recv: +$totalRecvBottles • Close: $totalClosingBottles", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("SOLD: $totalBottlesSold Bottles", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("₹${totalDaySales.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun StockRowItem(
    item: StockRecordItemUiState,
    isClosed: Boolean,
    role: String,
    onOpenAdminEdit: () -> Unit,
    onUpdateRecord: (DailyStockRecord) -> Unit
) {
    val product = item.product
    val record = item.record

    var closingInput by remember(record.closingStock) { mutableStateOf(record.closingStock.toString()) }
    val catTheme = remember(product.category) { CategoryColors.forCategory(product.category) }
    val (sizeTextColor, sizeBgColor) = remember(product.bottleSizeMl) { CategoryColors.forBottleSize(product.bottleSizeMl) }

    val parsedClosing = closingInput.toIntOrNull() ?: 0
    val maxAvailable = record.openingStock + record.receivedQty
    val isClosingExceeding = parsedClosing > maxAvailable

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = role == "Admin") { onOpenAdminEdit() }
            .background(
                if (isClosingExceeding) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Category color bar (or red error alert bar if exceeding)
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (isClosingExceeding) MaterialTheme.colorScheme.error else catTheme.primary)
        )
        Spacer(modifier = Modifier.width(6.dp))

        // Brand & Size
        Column(modifier = Modifier.weight(2.2f)) {
            Text(
                text = "${product.brand} ${product.name}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    color = sizeBgColor,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = product.bottleSizeMl,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = sizeTextColor,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
                Text(
                    text = "₹${record.rate.toInt()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isClosingExceeding) {
                    Surface(
                        color = MaterialTheme.colorScheme.error,
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Text(
                            text = "MAX: $maxAvailable",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }

        // Opening
        Text(
            text = "${record.openingStock}",
            modifier = Modifier.weight(0.7f),
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Received
        Text(
            text = if (record.receivedQty > 0) "+${record.receivedQty}" else "0",
            modifier = Modifier.weight(0.7f),
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (record.receivedQty > 0) FontWeight.Bold else FontWeight.Normal,
            color = if (record.receivedQty > 0) Color(0xFF00897B) else MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Closing Stock editable input (editable by staff when open, or admin anytime)
        Box(modifier = Modifier.weight(1.3f)) {
            if (isClosed && role != "Admin") {
                Column {
                    Text(
                        text = "${record.closingStock}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (isClosingExceeding) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                    if (isClosingExceeding) {
                        Text(
                            text = "> Open+Recv",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.Start) {
                    OutlinedTextField(
                        value = closingInput,
                        onValueChange = { newVal ->
                            if (newVal.isEmpty() || newVal.all { it.isDigit() }) {
                                closingInput = newVal
                                val parsed = newVal.toIntOrNull() ?: 0
                                if (parsed != record.closingStock) {
                                    onUpdateRecord(record.copy(closingStock = parsed))
                                }
                            }
                        },
                        singleLine = true,
                        isError = isClosingExceeding,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier
                            .width(72.dp)
                            .testTag("closing_input_${product.id}")
                    )
                    if (isClosingExceeding) {
                        Text(
                            text = "⚠️ > $maxAvailable",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Sold
        Column(modifier = Modifier.weight(0.9f), horizontalAlignment = Alignment.End) {
            Text(
                text = "${record.salesQty}",
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (isClosingExceeding) MaterialTheme.colorScheme.error
                    else if (record.salesQty > 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
            )
            if (record.salesQty > 0) {
                Text(
                    text = "₹${record.totalSalesValue.toInt()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (role == "Admin") {
            IconButton(
                onClick = onOpenAdminEdit,
                modifier = Modifier
                    .weight(0.5f)
                    .size(28.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Admin Edit Past Stock",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun StockGridCardItem(
    item: StockRecordItemUiState,
    isClosed: Boolean,
    role: String,
    onOpenAdminEdit: () -> Unit,
    onUpdateRecord: (DailyStockRecord) -> Unit
) {
    val product = item.product
    val record = item.record

    var closingInput by remember(record.closingStock) { mutableStateOf(record.closingStock.toString()) }
    val catTheme = remember(product.category) { CategoryColors.forCategory(product.category) }
    val (sizeTextColor, sizeBgColor) = remember(product.bottleSizeMl) { CategoryColors.forBottleSize(product.bottleSizeMl) }

    val parsedClosing = closingInput.toIntOrNull() ?: 0
    val maxAvailable = record.openingStock + record.receivedQty
    val isClosingExceeding = parsedClosing > maxAvailable

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = role == "Admin") { onOpenAdminEdit() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isClosingExceeding) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        border = if (isClosingExceeding) BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
        else CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (isClosingExceeding) MaterialTheme.colorScheme.error else catTheme.container,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = product.category,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isClosingExceeding) MaterialTheme.colorScheme.onError else catTheme.onContainer,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }

                Surface(
                    color = sizeBgColor,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = product.bottleSizeMl,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = sizeTextColor,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${product.brand} ${product.name}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Open: ${record.openingStock}", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                Text("Recv: +${record.receivedQty}", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = Color(0xFF00897B))
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Closing", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (isClosed && role != "Admin") {
                        Text(
                            "${record.closingStock}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isClosingExceeding) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        OutlinedTextField(
                            value = closingInput,
                            onValueChange = { newVal ->
                                if (newVal.isEmpty() || newVal.all { it.isDigit() }) {
                                    closingInput = newVal
                                    val parsed = newVal.toIntOrNull() ?: 0
                                    if (parsed != record.closingStock) {
                                        onUpdateRecord(record.copy(closingStock = parsed))
                                    }
                                }
                            },
                            singleLine = true,
                            isError = isClosingExceeding,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(64.dp)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Sold", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${record.salesQty}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isClosingExceeding) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (isClosingExceeding) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Closing exceeds Open+Recv ($maxAvailable)",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
