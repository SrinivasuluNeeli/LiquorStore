package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.Product
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.LiquorInventoryTheme
import com.example.ui.viewmodel.LiquorViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector, val tag: String) {
    object DailyStock : Screen("daily_stock", "Stock Sheet", Icons.Default.Inventory, "nav_stock")
    object Receipts : Screen("receipts", "Deliveries", Icons.Default.ReceiptLong, "nav_receipts")
    object Expenses : Screen("expenses", "Expenses", Icons.Default.Payments, "nav_expenses")
    object AuditLogs : Screen("audit_logs", "Audit Log", Icons.Default.History, "nav_audit_logs")
    object Reports : Screen("reports", "Reports", Icons.Default.BarChart, "nav_reports")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LiquorInventoryTheme {
                LiquorInventoryApp()
            }
        }
    }
}

@Composable
fun LiquorInventoryApp(
    viewModel: LiquorViewModel = viewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.DailyStock.route

    val snackbarHostState = remember { SnackbarHostState() }
    val toastMessage by viewModel.snackbarMessage.collectAsState()

    val selectedDate by viewModel.selectedDate.collectAsState()
    val currentRecords by viewModel.filteredDayRecords.collectAsState()
    val productsList by viewModel.products.collectAsState()
    val daySummary by viewModel.currentDaySummary.collectAsState()
    val totalExpenses by viewModel.currentDayExpenses.collectAsState()

    // Dialog state handlers
    var showAdminPinDialog by remember { mutableStateOf(false) }
    var showBatchEntryDialog by remember { mutableStateOf(false) }
    var showCloseDayDialog by remember { mutableStateOf(false) }
    var showAddReceiptDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddEditProductDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    val screens = listOf(
        Screen.DailyStock,
        Screen.Receipts,
        Screen.Expenses,
        Screen.AuditLogs,
        Screen.Reports
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                screens.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title, fontSize = 10.sp, maxLines = 1, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag(screen.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = Screen.DailyStock.route
            ) {
                composable(Screen.DailyStock.route) {
                    DailyStockScreen(
                        viewModel = viewModel,
                        onOpenBatchEntry = { showBatchEntryDialog = true },
                        onOpenCloseDayModal = { showCloseDayDialog = true },
                        onOpenAddReceiptModal = { showAddReceiptDialog = true }
                    )
                }

                composable(Screen.Receipts.route) {
                    StockReceiptsScreen(
                        viewModel = viewModel,
                        onOpenAddReceiptModal = { showAddReceiptDialog = true }
                    )
                }

                composable(Screen.Expenses.route) {
                    ExpensesScreen(
                        viewModel = viewModel,
                        onOpenAddExpenseModal = { showAddExpenseDialog = true }
                    )
                }

                composable(Screen.AuditLogs.route) {
                    AuditLogScreen(
                        viewModel = viewModel
                    )
                }

                composable(Screen.Reports.route) {
                    ReportsAuditScreen(
                        viewModel = viewModel,
                        onOpenAdminPinDialog = { showAdminPinDialog = true },
                        onOpenAddProductDialog = { prod ->
                            productToEdit = prod
                            showAddEditProductDialog = true
                        }
                    )
                }
            }
        }
    }

    // Dialog Rendering
    if (showAdminPinDialog) {
        AdminPinDialog(
            onDismiss = { showAdminPinDialog = false },
            onConfirmPin = { pin ->
                if (viewModel.verifyAdminPin(pin)) {
                    showAdminPinDialog = false
                }
            }
        )
    }

    if (showBatchEntryDialog) {
        BatchEntryDialog(
            items = currentRecords,
            onDismiss = { showBatchEntryDialog = false },
            onSaveBatch = { map ->
                viewModel.updateBatchClosingStock(map)
            }
        )
    }

    if (showCloseDayDialog) {
        val expectedRev = currentRecords.sumOf { it.record.totalSalesValue }
        val expTotal = totalExpenses.sumOf { it.amount }
        CloseDayDialog(
            date = selectedDate,
            expectedSalesRevenue = expectedRev,
            totalExpenses = expTotal,
            onDismiss = { showCloseDayDialog = false },
            onConfirmCloseDay = { cash, upi, credit, notes ->
                viewModel.closeDay(cash, upi, credit, notes)
            }
        )
    }

    if (showAddReceiptDialog) {
        AddReceiptDialog(
            currentDate = selectedDate,
            products = productsList,
            onDismiss = { showAddReceiptDialog = false },
            onSubmitReceipt = { supplier, invoiceNo, invoiceDate, notes, items ->
                viewModel.addStockDelivery(supplier, invoiceNo, invoiceDate, notes, items)
            }
        )
    }

    if (showAddExpenseDialog) {
        AddExpenseDialog(
            onDismiss = { showAddExpenseDialog = false },
            onSubmitExpense = { category, amount, paymentMode, remarks ->
                viewModel.addExpense(category, amount, paymentMode, remarks)
            }
        )
    }

    if (showAddEditProductDialog) {
        AddEditProductDialog(
            productToEdit = productToEdit,
            onDismiss = {
                showAddEditProductDialog = false
                productToEdit = null
            },
            onSaveProduct = { prod ->
                viewModel.saveProduct(prod)
            }
        )
    }
}
