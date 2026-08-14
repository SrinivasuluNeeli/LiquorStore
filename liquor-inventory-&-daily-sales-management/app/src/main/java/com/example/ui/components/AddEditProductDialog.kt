package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductDialog(
    productToEdit: Product?,
    onDismiss: () -> Unit,
    onSaveProduct: (Product) -> Unit
) {
    val categories = listOf("Brandy", "Rum", "Vodka", "Wine", "Whisky", "Beer")
    val bottleSizes = listOf("1000 ML", "750 ML", "375 ML", "180 ML", "90 ML", "60 ML", "650 ML", "500 ML", "330 ML", "275 ML")

    var brandName by remember { mutableStateOf(productToEdit?.brandName ?: "") }
    var skuCode by remember { mutableStateOf(productToEdit?.sku ?: "") }
    var name by remember { mutableStateOf(productToEdit?.name ?: "") }
    var selectedCategory by remember { mutableStateOf(productToEdit?.category ?: categories[0]) }
    var selectedSize by remember { mutableStateOf(productToEdit?.unitSize ?: bottleSizes[1]) }
    var costPriceText by remember { mutableStateOf(productToEdit?.costPrice?.toInt()?.toString() ?: "500") }
    var rateText by remember { mutableStateOf(productToEdit?.defaultRate?.toInt()?.toString() ?: "650") }
    var reorderPointText by remember { mutableStateOf(productToEdit?.reorderPoint?.toString() ?: "10") }
    var currentStockText by remember { mutableStateOf(productToEdit?.currentStockLevel?.toString() ?: "20") }
    var isActive by remember { mutableStateOf(productToEdit?.isActive ?: true) }

    var catExpanded by remember { mutableStateOf(false) }
    var sizeExpanded by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val handleDismiss = {
        keyboardController?.hide()
        focusManager.clearFocus()
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = handleDismiss,
        title = { Text(if (productToEdit == null) "Add Master Liquor SKU" else "Edit SKU Details") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = brandName,
                    onValueChange = { brandName = it },
                    label = { Text("Brand Name (e.g. Royal Challenge)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product / Variant Name (e.g. Classic Select)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = skuCode,
                    onValueChange = { skuCode = it },
                    label = { Text("SKU / Barcode Code (Optional)") },
                    placeholder = { Text("e.g. SKU-WHI-750-001") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = catExpanded,
                    onExpandedChange = { catExpanded = !catExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = catExpanded,
                        onDismissRequest = { catExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCategory = cat
                                    catExpanded = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = sizeExpanded,
                    onExpandedChange = { sizeExpanded = !sizeExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedSize,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unit Size (Bottle Volume)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sizeExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = sizeExpanded,
                        onDismissRequest = { sizeExpanded = false }
                    ) {
                        bottleSizes.forEach { sz ->
                            DropdownMenuItem(
                                text = { Text(sz) },
                                onClick = {
                                    selectedSize = sz
                                    sizeExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = costPriceText,
                        onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() || c == '.' }) costPriceText = it },
                        label = { Text("Cost Price (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = rateText,
                        onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() || c == '.' }) rateText = it },
                        label = { Text("Selling Rate / MRP (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = reorderPointText,
                        onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) reorderPointText = it },
                        label = { Text("Reorder Point") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = currentStockText,
                        onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) currentStockText = it },
                        label = { Text("Current Stock Level") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("SKU Active Status")
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    if (brandName.isNotBlank() && name.isNotBlank()) {
                        val genId = productToEdit?.id ?: "p_${System.currentTimeMillis()}"
                        val finalSku = if (skuCode.isNotBlank()) skuCode.trim() else "SKU-${selectedCategory.take(3).uppercase()}-${selectedSize.replace(" ", "").replace("ML", "")}-${genId.takeLast(4)}"
                        val stockQty = currentStockText.toIntOrNull() ?: 0
                        val reorderQty = reorderPointText.toIntOrNull() ?: 10

                        val prod = Product(
                            id = genId,
                            sku = finalSku,
                            brandName = brandName.trim(),
                            name = name.trim(),
                            category = selectedCategory,
                            unitSize = selectedSize,
                            currentStockLevel = stockQty,
                            reorderPoint = reorderQty,
                            costPrice = costPriceText.toDoubleOrNull() ?: 0.0,
                            defaultRate = rateText.toDoubleOrNull() ?: 0.0,
                            initialOpeningStock = stockQty,
                            isActive = isActive
                        )
                        onSaveProduct(prod)
                        onDismiss()
                    }
                },
                enabled = brandName.isNotBlank() && name.isNotBlank()
            ) {
                Text("Save SKU")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = handleDismiss) {
                Text("Cancel")
            }
        }
    )
}
