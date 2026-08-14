package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun CloseDayDialog(
    date: String,
    expectedSalesRevenue: Double,
    totalExpenses: Double,
    onDismiss: () -> Unit,
    onConfirmCloseDay: (cash: Double, upi: Double, credit: Double, notes: String) -> Unit
) {
    var cashCollected by remember { mutableStateOf(expectedSalesRevenue.toInt().toString()) }
    var upiCollected by remember { mutableStateOf("0") }
    var creditSales by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val cashVal = cashCollected.toDoubleOrNull() ?: 0.0
    val upiVal = upiCollected.toDoubleOrNull() ?: 0.0
    val creditVal = creditSales.toDoubleOrNull() ?: 0.0

    val totalEnteredRevenue = cashVal + upiVal + creditVal
    val netCashInHand = cashVal - totalExpenses
    val variance = totalEnteredRevenue - expectedSalesRevenue

    val handleDismiss = {
        keyboardController?.hide()
        focusManager.clearFocus()
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = handleDismiss,
        title = { Text("Day Settlement & Closure ($date)") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Expected Sales Revenue: ₹${expectedSalesRevenue.toInt()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Logged Day Expenses: ₹${totalExpenses.toInt()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Text("Enter Collection Breakdown:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)

                OutlinedTextField(
                    value = cashCollected,
                    onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() || char == '.' }) cashCollected = it },
                    label = { Text("Cash Collection (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = upiCollected,
                    onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() || char == '.' }) upiCollected = it },
                    label = { Text("UPI / Digital Collection (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = creditSales,
                    onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() || char == '.' }) creditSales = it },
                    label = { Text("Credit Sales / Outstanding (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()

                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text("Total Accounted: ₹${totalEnteredRevenue.toInt()}", style = MaterialTheme.typography.bodyMedium)
                    Text("Net Cash In Hand (Cash - Expenses): ₹${netCashInHand.toInt()}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                    if (variance != 0.0) {
                        Text(
                            text = if (variance > 0) "Excess Revenue: +₹${variance.toInt()}" else "Shortage / Discrepancy: -₹${(-variance).toInt()}",
                            color = if (variance > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Closure Remarks / Notes") },
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
                    onConfirmCloseDay(cashVal, upiVal, creditVal, notes)
                    onDismiss()
                }
            ) {
                Text("Lock & Close Day")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = handleDismiss) {
                Text("Cancel")
            }
        }
    )
}

