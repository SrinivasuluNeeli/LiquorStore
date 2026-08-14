package com.example.ui.components

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
import com.example.ui.theme.CategoryColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReceiptDialog(
    currentDate: String,
    products: List<Product>,
    onDismiss: () -> Unit,
    onSubmitReceipt: (
        supplierName: String,
        invoiceNo: String,
        invoiceDate: String,
        notes: String,
        items: List<Pair<Product, Int>>
    ) -> Unit
) {
    var supplierName by remember { mutableStateOf("") }
    var invoiceNo by remember { mutableStateOf("") }
    var invoiceDate by remember { mutableStateOf(currentDate) }
    var notes by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    val selectedItems = remember { mutableStateListOf<Pair<Product, Int>>() }

    var expandedProductDropdown by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedProductForAdd by remember { mutableStateOf<Product?>(null) }
    var addQtyText by remember { mutableStateOf("10") }

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

    if (showDatePicker) {
        PastDatePickerDialog(
            initialDate = invoiceDate,
            onDismiss = { showDatePicker = false },
            onDateSelected = { newDate ->
                invoiceDate = newDate
            }
        )
    }

    val totalCalculatedAmt = selectedItems.sumOf { it.first.costPrice * it.second }

    AlertDialog(
        onDismissRequest = handleDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocalShipping, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Record Stock Delivery / Invoice")
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
                    label = { Text("Supplier / Distributor Name") },
                    placeholder = { Text("e.g., United Spirits Ltd, AB InBev") },
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
                        placeholder = { Text("INV-9821") },
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

                Text("Add Delivery SKUs:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                ExposedDropdownMenuBox(
                    expanded = expandedProductDropdown,
                    onExpandedChange = { expandedProductDropdown = !expandedProductDropdown },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedProductForAdd?.displayLabel ?: "Select Product SKU",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Product SKU") },
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
                                        Surface(
                                            color = catTheme.container,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
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
                                selectedItems.add(p to qty)
                                selectedProductForAdd = null
                                addQtyText = "10"
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        enabled = selectedProductForAdd != null && (addQtyText.toIntOrNull() ?: 0) > 0
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Item")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add SKU")
                    }
                }

                if (selectedItems.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Items in Delivery (${selectedItems.size}):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text("Total Cost: ₹${totalCalculatedAmt.toInt()}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(selectedItems) { (product, qty) ->
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
                                        text = "Qty: $qty bottles • Unit Cost: ₹${product.costPrice.toInt()} • Total: ₹${(product.costPrice * qty).toInt()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { selectedItems.remove(product to qty) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Receipt Remarks / Delivery Notes") },
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
                        onSubmitReceipt(supplierName, invoiceNo, invoiceDate, notes, selectedItems.toList())
                        onDismiss()
                    }
                },
                shape = RoundedCornerShape(10.dp),
                enabled = supplierName.isNotBlank() && invoiceNo.isNotBlank() && selectedItems.isNotEmpty()
            ) {
                Text("Save Receipt")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = handleDismiss, shape = RoundedCornerShape(10.dp)) {
                Text("Cancel")
            }
        }
    )
}
