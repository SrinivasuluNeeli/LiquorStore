package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.StockRecordItemUiState

@Composable
fun BatchEntryDialog(
    items: List<StockRecordItemUiState>,
    onDismiss: () -> Unit,
    onSaveBatch: (Map<String, Int>) -> Unit
) {
    val closingMap = remember {
        mutableStateMapOf<String, String>().apply {
            items.forEach {
                put(it.product.id, it.record.closingStock.toString())
            }
        }
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val handleDismiss = {
        keyboardController?.hide()
        focusManager.clearFocus()
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = handleDismiss,
        title = { Text("Fast Batch Closing Stock Entry") },
        text = {
            Column(modifier = Modifier.fillMaxHeight(0.7f)) {
                Text(
                    "Quickly review and enter physical closing bottle count for items in view:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items, key = { it.product.id }) { item ->
                        val product = item.product
                        val record = item.record

                        val currentClosing = closingMap[product.id]?.toIntOrNull() ?: 0
                        val maxAvailable = record.openingStock + record.receivedQty
                        val isExceeding = currentClosing > maxAvailable

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isExceeding) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            border = if (isExceeding) BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
                            else CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${product.brand} ${product.name}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${product.bottleSizeMl} • Open: ${record.openingStock} + Recv: ${record.receivedQty} (Max: $maxAvailable)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isExceeding) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    OutlinedTextField(
                                        value = closingMap[product.id] ?: "",
                                        onValueChange = { newVal ->
                                            if (newVal.isEmpty() || newVal.all { it.isDigit() }) {
                                                closingMap[product.id] = newVal
                                            }
                                        },
                                        label = { Text("Closing") },
                                        singleLine = true,
                                        isError = isExceeding,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                        modifier = Modifier.width(90.dp)
                                    )
                                }

                                if (isExceeding) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Closing ($currentClosing) exceeds Open+Recv ($maxAvailable)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    val resultMap = closingMap.mapValues { (_, v) -> v.toIntOrNull() ?: 0 }
                    onSaveBatch(resultMap)
                    onDismiss()
                }
            ) {
                Text("Save All Counts")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = handleDismiss) {
                Text("Cancel")
            }
        }
    )
}

