package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.DailyStockRecord
import com.example.data.model.Product

@Composable
fun BreakageLogDialog(
    product: Product,
    record: DailyStockRecord,
    onDismiss: () -> Unit,
    onConfirmBreakage: (updatedRecord: DailyStockRecord) -> Unit
) {
    var breakageQtyText by remember { mutableStateOf(record.breakageQty.toString()) }
    var notes by remember { mutableStateOf(record.notes) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val handleDismiss = {
        keyboardController?.hide()
        focusManager.clearFocus()
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = handleDismiss,
        title = { Text("Log Stock Breakage / Discrepancy") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${product.brand} ${product.name} (${product.bottleSizeMl})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Current Available: ${record.totalAvailable} bottles | Rate: ₹${record.rate.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = breakageQtyText,
                    onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) breakageQtyText = it },
                    label = { Text("Breakage / Missing Bottle Count") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Reason / Discrepancy Remarks") },
                    placeholder = { Text("e.g. Broken in crate transit / audit discrepancy") },
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
                    val newBreakage = breakageQtyText.toIntOrNull() ?: 0
                    val updated = record.copy(
                        breakageQty = newBreakage,
                        notes = notes
                    )
                    onConfirmBreakage(updated)
                    onDismiss()
                }
            ) {
                Text("Save Adjustment")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = handleDismiss) {
                Text("Cancel")
            }
        }
    )
}

