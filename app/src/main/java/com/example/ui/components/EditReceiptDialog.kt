package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.data.model.StockReceipt
import com.example.data.model.StockReceiptItem
import com.example.ui.theme.CategoryColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReceiptDialog(
    receipt: StockReceipt,
    existingItems: List<StockReceiptItem>,
    products: List<Product>,
    onDismiss: () -> Unit,
    onDeleteReceipt: () -> Unit,
    onSaveReceipt: (
        supplierName: String,
        invoiceNo: String,
        invoiceDate: String,
        notes: String,
        items: List<Pair<Product, Int>>
    ) -> Unit
) {
    var supplierName by remember { mutableStateOf(receipt.supplierName) }
    var invoiceNo by remember { mutableStateOf(receipt.invoiceNo) }
    var invoiceDate by remember { mutableStateOf(receipt.invoiceDate) }
    var notes by remember { mutableStateOf(receipt.notes) }
    var showDatePicker by remember { mutableStateOf(false) }

    val productMap = remember(products) { products.associateBy { it.id } }

    val selectedItems = remember {
        mutableStateListOf<Pair<Product, Int>>().apply {
            addAll(
                existingItems.mapNotNull { item ->
                    productMap[item.productId]?.let { p -> p to item.quantity }
                }
            )
        }
    }

    var expandedProductDropdown by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedProductForAdd by remember { mutableStateOf<Product?>(null) }
    var addQtyText by remember { mutableStateOf("10") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val handleDismiss = {
        keyboardController?.hide()
        focusManager.clearFocus()
        onDismiss()
    }

    val filteredProducts = remember(products, searchQuery) {
        if (searchQuery.isBlank()) products else products.filter {
            it.brand.contains(searchQuery, ignoreCase = true) ||
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.bottleSizeMl.contains(searchQuery, ignoreCase = true)
        }
    }

    val totalCalculatedAmt = selectedItems.sumOf { it.first.costPrice * it.second }

    if (showDatePicker) {
        PastDatePickerDialog(
            initialDate = invoiceDate,
            onDismiss = { showDatePicker = false },
            onDateSelected = { newDate ->
                invoiceDate = newDate
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Delivery Invoice?") },
            text = { Text("Are you sure you want to delete invoice #${receipt.invoiceNo}? Stock received quantities for this delivery will be rolled back from the stock sheet.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteReceipt()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Delivery")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = handleDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Delivery Invoice")
                }
                IconButton(
                    onClick = { showDeleteConfirm = true }
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Delivery", tint = MaterialTheme.colorScheme.error)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = supplierName,
                    onValueChange = { supplierName = it },
                    label = { Text("Supplier Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = invoiceNo,
                        onValueChange = { invoiceNo = it },
                        label = { Text("Invoice No") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = invoiceDate,
                        onValueChange = { invoiceDate = it },
                        label = { Text("Invoice Date") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Text("Add / Modify Items in this Delivery:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                ExposedDropdownMenuBox(
                    expanded = expandedProductDropdown,
                    onExpandedChange = { expandedProductDropdown = !expandedProductDropdown },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedProductForAdd?.displayLabel ?: "Select Product SKU to Add",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Add More Products") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedProductDropdown) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedProductDropdown,
                        onDismissRequest = { expandedProductDropdown = false },
                        modifier = Modifier.heightIn(max = 250.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search brand / size...") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        )
                        filteredProducts.take(40).forEach { prod ->
                            val catTheme = CategoryColors.forCategory(prod.category)
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(color = catTheme.container, shape = RoundedCornerShape(4.dp)) {
                                            Text(prod.category, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = catTheme.onContainer, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(prod.displayLabel, fontSize = 13.sp)
                                    }
                                },
                                onClick = {
                                    selectedProductForAdd = prod
                                    expandedProductDropdown = false
                                }
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = addQtyText,
                        onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) addQtyText = it },
                        label = { Text("Received Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = {
                            val p = selectedProductForAdd
                            val qty = addQtyText.toIntOrNull() ?: 0
                            if (p != null && qty > 0) {
                                val existingIndex = selectedItems.indexOfFirst { it.first.id == p.id }
                                if (existingIndex >= 0) {
                                    selectedItems[existingIndex] = p to (selectedItems[existingIndex].second + qty)
                                } else {
                                    selectedItems.add(p to qty)
                                }
                                selectedProductForAdd = null
                                addQtyText = "10"
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        enabled = selectedProductForAdd != null && (addQtyText.toIntOrNull() ?: 0) > 0
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Item")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Delivery Contents (${selectedItems.size} items):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text("Total: ₹${totalCalculatedAmt.toInt()}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(selectedItems) { itemPair ->
                        val (product, qty) = itemPair
                        val catTheme = CategoryColors.forCategory(product.category)
                        val (sizeColor, sizeBg) = CategoryColors.forBottleSize(product.bottleSizeMl)

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(10.dp),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Surface(color = catTheme.container, shape = RoundedCornerShape(3.dp)) {
                                            Text(product.category, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = catTheme.onContainer, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                        }
                                        Surface(color = sizeBg, shape = RoundedCornerShape(3.dp)) {
                                            Text(product.bottleSizeMl, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = sizeColor, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                        }
                                        Text(
                                            text = "${product.brand} ${product.name}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "Qty: $qty bottles • Unit: ₹${product.costPrice.toInt()} • Subtotal: ₹${(product.costPrice * qty).toInt()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            val next = qty - 1
                                            val idx = selectedItems.indexOf(itemPair)
                                            if (idx >= 0) {
                                                if (next <= 0) selectedItems.removeAt(idx)
                                                else selectedItems[idx] = product to next
                                            }
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                                    }
                                    Text("$qty", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                                    IconButton(
                                        onClick = {
                                            val idx = selectedItems.indexOf(itemPair)
                                            if (idx >= 0) selectedItems[idx] = product to (qty + 1)
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { selectedItems.remove(itemPair) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Receipt Remarks / Notes") },
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    if (supplierName.isNotBlank() && invoiceNo.isNotBlank() && selectedItems.isNotEmpty()) {
                        onSaveReceipt(supplierName, invoiceNo, invoiceDate, notes, selectedItems.toList())
                        onDismiss()
                    }
                },
                shape = RoundedCornerShape(10.dp),
                enabled = supplierName.isNotBlank() && invoiceNo.isNotBlank() && selectedItems.isNotEmpty()
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = handleDismiss, shape = RoundedCornerShape(10.dp)) {
                Text("Cancel")
            }
        }
    )
}
