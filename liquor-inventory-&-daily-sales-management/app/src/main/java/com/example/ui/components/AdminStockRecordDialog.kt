package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyStockRecord
import com.example.data.model.Product
import com.example.ui.theme.CategoryColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStockRecordDialog(
    record: DailyStockRecord,
    product: Product,
    onDismiss: () -> Unit,
    onSaveRecord: (DailyStockRecord, String) -> Unit
) {
    var openingInput by remember { mutableIntStateOf(record.openingStock) }
    var receivedInput by remember { mutableIntStateOf(record.receivedQty) }
    var breakageInput by remember { mutableIntStateOf(record.breakageQty) }
    var closingInput by remember { mutableIntStateOf(record.closingStock) }
    var rateInput by remember { mutableDoubleStateOf(record.rate) }
    var auditReason by remember { mutableStateOf("Admin audit adjustment") }

    val catTheme = remember(product.category) { CategoryColors.forCategory(product.category) }
    val (sizeTextColor, sizeBgColor) = remember(product.bottleSizeMl) { CategoryColors.forBottleSize(product.bottleSizeMl) }

    // Live calculated properties
    val available = (openingInput + receivedInput - breakageInput).coerceAtLeast(0)
    val sold = (available - closingInput).coerceAtLeast(0)
    val totalRevenue = sold * rateInput
    val estProfit = sold * (rateInput - product.costPrice)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = catTheme.container,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = product.category.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = catTheme.onContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Surface(
                            color = sizeBgColor,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = product.bottleSizeMl,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = sizeTextColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = record.date,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${product.brand} ${product.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Modify Past Day Stock & Sales Record (Admin Direct Mode)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Stepper fields for stock quantities
                QuantityStepperRow(
                    label = "Opening Stock",
                    value = openingInput,
                    accentColor = MaterialTheme.colorScheme.primary,
                    onValueChange = { openingInput = it }
                )

                QuantityStepperRow(
                    label = "Received (Deliveries)",
                    value = receivedInput,
                    accentColor = Color(0xFF00897B),
                    onValueChange = { receivedInput = it }
                )

                QuantityStepperRow(
                    label = "Breakage / Damage",
                    value = breakageInput,
                    accentColor = Color(0xFFD84315),
                    onValueChange = { breakageInput = it }
                )

                val maxAllowedStock = openingInput + receivedInput
                val isClosingOverStock = closingInput > maxAllowedStock

                QuantityStepperRow(
                    label = "Closing Stock",
                    value = closingInput,
                    accentColor = if (isClosingOverStock) MaterialTheme.colorScheme.error else catTheme.primary,
                    onValueChange = { closingInput = it }
                )

                if (isClosingOverStock) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Closing ($closingInput) exceeds Open+Recv ($maxAllowedStock)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Physical closing count cannot exceed total stock received. Sold count is clamped to 0.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // Selling Rate
                OutlinedTextField(
                    value = if (rateInput % 1.0 == 0.0) rateInput.toInt().toString() else rateInput.toString(),
                    onValueChange = { newVal ->
                        val parsed = newVal.toDoubleOrNull()
                        if (parsed != null && parsed >= 0) {
                            rateInput = parsed
                        }
                    },
                    label = { Text("Selling Rate (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Live Dynamic Calculation Summary Box
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(catTheme.container)
                        .border(1.dp, catTheme.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    color = catTheme.container
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "LIVE RECALCULATED FIGURES",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = catTheme.onContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Available: $available", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = catTheme.onContainer)
                            Text("Bottles Sold: $sold", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = catTheme.onContainer)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Est Revenue: ₹${totalRevenue.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = catTheme.onContainer)
                            Text("Gross Profit: ₹${estProfit.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = catTheme.onContainer)
                        }
                    }
                }

                // Audit Log Reason
                OutlinedTextField(
                    value = auditReason,
                    onValueChange = { auditReason = it },
                    label = { Text("Reason for Adjustment (Audit Log)") },
                    placeholder = { Text("e.g., Physical count recount, breakage correction") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = record.copy(
                        openingStock = openingInput,
                        receivedQty = receivedInput,
                        breakageQty = breakageInput,
                        closingStock = closingInput,
                        rate = rateInput
                    )
                    onSaveRecord(updated, auditReason)
                    onDismiss()
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_admin_stock_record")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save Changes")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun QuantityStepperRow(
    label: String,
    value: Int,
    accentColor: Color,
    onValueChange: (Int) -> Unit
) {
    var textValue by remember(value) { mutableStateOf(value.toString()) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.2f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilledIconButton(
                onClick = {
                    val next = (value - 1).coerceAtLeast(0)
                    onValueChange(next)
                },
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(8.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
            }

            OutlinedTextField(
                value = textValue,
                onValueChange = { newVal ->
                    if (newVal.isEmpty() || newVal.all { it.isDigit() }) {
                        textValue = newVal
                        val parsed = newVal.toIntOrNull() ?: 0
                        onValueChange(parsed)
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(62.dp),
                shape = RoundedCornerShape(8.dp)
            )

            FilledIconButton(
                onClick = {
                    val next = value + 1
                    onValueChange(next)
                },
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(8.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = accentColor.copy(alpha = 0.15f))
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase", tint = accentColor, modifier = Modifier.size(16.dp))
            }
        }
    }
}
