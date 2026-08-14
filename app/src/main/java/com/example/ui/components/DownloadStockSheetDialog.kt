package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyExpense
import com.example.data.model.DaySummary
import com.example.data.model.StockReceipt
import com.example.ui.viewmodel.CategoryStockMovementItem
import com.example.ui.viewmodel.LiquorViewModel
import com.example.ui.viewmodel.StockRecordItemUiState
import com.example.util.ExcelReportGenerator
import com.example.util.PdfReportGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadStockSheetDialog(
    initialDate: String,
    viewModel: LiquorViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var targetDate by remember { mutableStateOf(initialDate) }
    var showDatePicker by remember { mutableStateOf(false) }

    val currentSelectedDate by viewModel.selectedDate.collectAsState()
    val recordsList by viewModel.currentDayRecords.collectAsState()
    val categoryMovements by viewModel.categoryStockMovement.collectAsState()
    val allExpenses by viewModel.allExpenses.collectAsState()
    val allReceipts by viewModel.allReceipts.collectAsState()
    val daySummary by viewModel.currentDaySummary.collectAsState()
    val role by viewModel.currentUserRole.collectAsState()

    var isGeneratingPdf by remember { mutableStateOf(false) }
    var isGeneratingExcel by remember { mutableStateOf(false) }
    var generatedPdfFile by remember { mutableStateOf<File?>(null) }
    var generatedExcelFile by remember { mutableStateOf<File?>(null) }

    if (showDatePicker) {
        PastDatePickerDialog(
            initialDate = targetDate,
            onDismiss = { showDatePicker = false },
            onDateSelected = { newDate ->
                targetDate = newDate
                viewModel.changeDate(newDate)
                generatedPdfFile = null
                generatedExcelFile = null
            }
        )
    }

    val totalRevenue = recordsList.sumOf { it.record.totalSalesValue }
    val totalProfit = recordsList.sumOf { it.record.grossProfit }
    val totalSold = recordsList.sumOf { it.record.salesQty }
    val dayExpenses = allExpenses.filter { it.date == targetDate }.sumOf { it.amount }

    val handleExportPdf = {
        isGeneratingPdf = true
        coroutineScope.launch {
            val file = PdfReportGenerator.generateDailySummaryPdf(
                context = context,
                selectedDate = targetDate,
                daySummary = daySummary,
                recordsList = recordsList,
                categoryMovements = categoryMovements,
                dailyExpenses = allExpenses,
                stockReceipts = allReceipts,
                role = role
            )
            isGeneratingPdf = false
            generatedPdfFile = file
            if (file != null) {
                viewModel.showToast("Daily Accounting PDF Ready for $targetDate!")
            } else {
                viewModel.showToast("Failed to generate PDF.")
            }
        }
    }

    val handleExportExcel = {
        isGeneratingExcel = true
        coroutineScope.launch {
            val file = ExcelReportGenerator.generateDailyExcelReport(
                context = context,
                selectedDate = targetDate,
                daySummary = daySummary,
                recordsList = recordsList,
                categoryMovements = categoryMovements,
                dailyExpenses = allExpenses,
                stockReceipts = allReceipts,
                role = role
            )
            isGeneratingExcel = false
            generatedExcelFile = file
            if (file != null) {
                viewModel.showToast("Excel Stock Sheet created for $targetDate!")
            } else {
                viewModel.showToast("Failed to generate Excel report.")
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Download Daily Stock Sheet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "PDF or Excel sheet with detailed analysis",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Date Selector Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showDatePicker = true },
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Selected Report Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(targetDate, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Change Date", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Change Date", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Daily Metrics Snapshot
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Items in Register:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${recordsList.size} SKUs", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Gross Sales Revenue:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹ %,.2f".format(totalRevenue), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Est. Gross Margin:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹ %,.2f".format(totalProfit), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Units Dispatched:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$totalSold Bottles", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Option 1: Printable PDF
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("1. Printable PDF Document", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("2-page formatted register with executive summary & audit lines", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        if (generatedPdfFile == null) {
                            Button(
                                onClick = { handleExportPdf() },
                                modifier = Modifier.fillMaxWidth().testTag("generate_pdf_in_dialog_btn"),
                                enabled = !isGeneratingPdf,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (isGeneratingPdf) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Generating PDF...")
                                } else {
                                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Generate & Print PDF")
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = { PdfReportGenerator.printPdfFile(context, generatedPdfFile!!) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Print", fontSize = 12.sp)
                                }
                                FilledTonalButton(
                                    onClick = { PdfReportGenerator.openPdfFile(context, generatedPdfFile!!) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Open", fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = { PdfReportGenerator.sharePdfFile(context, generatedPdfFile!!) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Share", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Option 2: Excel Spreadsheet
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TableChart, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("2. Excel Spreadsheet (.CSV / .XLS)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Multi-section workbook with summary, category KPI, & SKU ledger", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        if (generatedExcelFile == null) {
                            FilledTonalButton(
                                onClick = { handleExportExcel() },
                                modifier = Modifier.fillMaxWidth().testTag("generate_excel_in_dialog_btn"),
                                enabled = !isGeneratingExcel,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (isGeneratingExcel) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Generating Excel...")
                                } else {
                                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Generate Excel Sheet")
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = { ExcelReportGenerator.openExcelFile(context, generatedExcelFile!!) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Open in Excel", fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = { ExcelReportGenerator.shareExcelFile(context, generatedExcelFile!!) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Share Excel", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_download_dialog_btn")
            ) {
                Text("Close")
            }
        }
    )
}
