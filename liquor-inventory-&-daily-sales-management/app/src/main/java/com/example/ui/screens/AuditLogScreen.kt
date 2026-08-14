package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuditLog
import com.example.ui.viewmodel.LiquorViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditLogScreen(
    viewModel: LiquorViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val allLogs by viewModel.auditLogs.collectAsState()
    val filteredLogs by viewModel.filteredAuditLogs.collectAsState()
    val currentRole by viewModel.currentUserRole.collectAsState()
    val isAdminUnlocked by viewModel.isAdminUnlocked.collectAsState()

    val searchQuery by viewModel.auditSearchQuery.collectAsState()
    val roleFilter by viewModel.auditRoleFilter.collectAsState()
    val actionFilter by viewModel.auditActionFilter.collectAsState()
    val dateFilter by viewModel.auditDateFilter.collectAsState()

    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    var showExportDialog by remember { mutableStateOf(false) }
    var selectedLogForDetail by remember { mutableStateOf<AuditLog?>(null) }

    val adminCount = remember(allLogs) { allLogs.count { it.userRole.equals("Admin", ignoreCase = true) } }
    val staffCount = remember(allLogs) { allLogs.count { it.userRole.equals("Staff", ignoreCase = true) } }
    val stockChangesCount = remember(allLogs) {
        allLogs.count { it.actionType.contains("STOCK", ignoreCase = true) || it.actionType.contains("BREAKAGE", ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Audit Trail & Activity Log",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (currentRole == "Admin") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (currentRole == "Admin") Icons.Default.Shield else Icons.Default.Badge,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = if (currentRole == "Admin") MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = currentRole.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (currentRole == "Admin") MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Immutable chronological feed of all admin & staff changes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("audit_back_button")) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.testTag("audit_export_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Export Audit Log")
                    }
                    if (currentRole == "Staff") {
                        IconButton(
                            onClick = {
                                pinInput = ""
                                pinError = false
                                showPinDialog = true
                            },
                            modifier = Modifier.testTag("audit_admin_lock_button")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = "Unlock Admin")
                        }
                    } else {
                        IconButton(
                            onClick = { viewModel.switchRoleToStaff() },
                            modifier = Modifier.testTag("audit_role_switch_button")
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = "Switch to Staff", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Metrics Summary Ribbon
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AuditMetricItem(
                        label = "Total Logs",
                        value = allLogs.size.toString(),
                        icon = Icons.Default.History,
                        color = MaterialTheme.colorScheme.primary
                    )
                    AuditMetricDivider()
                    AuditMetricItem(
                        label = "Admin Changes",
                        value = adminCount.toString(),
                        icon = Icons.Default.AdminPanelSettings,
                        color = Color(0xFFD32F2F)
                    )
                    AuditMetricDivider()
                    AuditMetricItem(
                        label = "Staff Changes",
                        value = staffCount.toString(),
                        icon = Icons.Default.Person,
                        color = Color(0xFF1976D2)
                    )
                    AuditMetricDivider()
                    AuditMetricItem(
                        label = "Stock Edits",
                        value = stockChangesCount.toString(),
                        icon = Icons.Default.Inventory2,
                        color = Color(0xFFE65100)
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setAuditSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("audit_search_input"),
                placeholder = { Text("Search SKU, role, action, date, or values...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setAuditSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Filter Chips
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                // Role & Period Filter Row
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Role Filter Chips
                    item {
                        FilterChip(
                            selected = roleFilter == "All",
                            onClick = { viewModel.setAuditRoleFilter("All") },
                            label = { Text("All Roles (${allLogs.size})") },
                            modifier = Modifier.testTag("filter_role_all")
                        )
                    }
                    item {
                        FilterChip(
                            selected = roleFilter == "Admin",
                            onClick = { viewModel.setAuditRoleFilter("Admin") },
                            label = { Text("Admin ($adminCount)") },
                            leadingIcon = {
                                Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.testTag("filter_role_admin")
                        )
                    }
                    item {
                        FilterChip(
                            selected = roleFilter == "Staff",
                            onClick = { viewModel.setAuditRoleFilter("Staff") },
                            label = { Text("Staff ($staffCount)") },
                            leadingIcon = {
                                Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.testTag("filter_role_staff")
                        )
                    }
                    item {
                        FilterChip(
                            selected = roleFilter == "System",
                            onClick = { viewModel.setAuditRoleFilter("System") },
                            label = { Text("System") },
                            leadingIcon = {
                                Icon(Icons.Default.SettingsSuggest, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.testTag("filter_role_system")
                        )
                    }

                    item {
                        VerticalDivider(modifier = Modifier.height(28.dp).padding(vertical = 2.dp))
                    }

                    // Date Filters
                    item {
                        FilterChip(
                            selected = dateFilter == "All",
                            onClick = { viewModel.setAuditDateFilter("All") },
                            label = { Text("All Time") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = dateFilter == "Today",
                            onClick = { viewModel.setAuditDateFilter("Today") },
                            label = { Text("Today (13 Aug)") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = dateFilter == "Yesterday",
                            onClick = { viewModel.setAuditDateFilter("Yesterday") },
                            label = { Text("Yesterday (12 Aug)") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = dateFilter == "Last 7 Days",
                            onClick = { viewModel.setAuditDateFilter("Last 7 Days") },
                            label = { Text("Last 7 Days") }
                        )
                    }
                }

                // Action Category Filter Row
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val actions = listOf("All", "Stock Updates", "Deliveries", "Expenses", "Catalog & Price", "Day Close/Reopen")
                    items(actions) { act ->
                        FilterChip(
                            selected = actionFilter == act,
                            onClick = { viewModel.setAuditActionFilter(act) },
                            label = { Text(act) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }

                    if (searchQuery.isNotEmpty() || roleFilter != "All" || actionFilter != "All" || dateFilter != "All") {
                        item {
                            AssistChip(
                                onClick = { viewModel.resetAuditFilters() },
                                label = { Text("Reset Filters") },
                                leadingIcon = {
                                    Icon(Icons.Default.FilterAltOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }
                }
            }

            // Results count label
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Showing ${filteredLogs.size} audit events",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Sorted chronologically (Newest first)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Audit Feed List
            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No audit log entries found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try clearing filters or searching with a different keyword",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.resetAuditFilters() },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reset All Filters")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("audit_logs_list"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredLogs, key = { it.id }) { log ->
                        AuditLogItemCard(
                            log = log,
                            onClick = { selectedLogForDetail = log }
                        )
                    }
                }
            }
        }
    }

    // Detail BottomSheet/Dialog
    if (selectedLogForDetail != null) {
        val log = selectedLogForDetail!!
        AlertDialog(
            onDismissRequest = { selectedLogForDetail = null },
            icon = {
                Icon(
                    imageVector = getActionIcon(log.actionType),
                    contentDescription = null,
                    tint = getActionColor(log.actionType)
                )
            },
            title = {
                Text(
                    text = "Audit Log Record Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DetailRow(label = "Action Type", value = log.actionType)
                    DetailRow(label = "User Role", value = log.userRole)
                    DetailRow(label = "Target Entity", value = log.targetEntity)
                    val fullDate = SimpleDateFormat("EEEE, dd MMM yyyy HH:mm:ss", Locale.US).format(Date(log.timestamp))
                    DetailRow(label = "Timestamp", value = fullDate)
                    if (log.changedFields.isNotBlank()) {
                        DetailRow(label = "Changed Fields", value = log.changedFields)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Value Transition Diff:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Previous value card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFEBEE),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCDD2))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("PREVIOUS VALUE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (log.oldValue.isNotBlank()) log.oldValue else "(None / Blank)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFB71C1C),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // New value card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE8F5E9),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC8E6C9))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("NEW VALUE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (log.newValue.isNotBlank()) log.newValue else "(None / Blank)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF1B5E20),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    DetailRow(label = "Audit UUID", value = log.id)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedLogForDetail = null }) {
                    Text("Close")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Audit Log JSON", "ID: ${log.id}\nAction: ${log.actionType}\nUser: ${log.userRole}\nTarget: ${log.targetEntity}\nOld: ${log.oldValue}\nNew: ${log.newValue}")
                        clipboard.setPrimaryClip(clip)
                        viewModel.showToast("Audit log entry copied to clipboard!")
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy")
                }
            }
        )
    }

    // Export & Share Dialog
    if (showExportDialog) {
        val fullText = remember { viewModel.exportAuditTrailAsText() }
        val csvText = remember { viewModel.exportAuditTrailAsCsv() }

        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            icon = { Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = {
                Text("Export Audit Trail", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Export ${filteredLogs.size} filtered audit logs for compliance, accounting, and security inspection.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 140.dp)
                    ) {
                        Text(
                            text = fullText.take(400) + if (fullText.length > 400) "\n... [Full log ready for export]" else "",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Audit Trail Text", fullText)
                                clipboard.setPrimaryClip(clip)
                                viewModel.showToast("Formatted text audit log copied!")
                                showExportDialog = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy Text")
                        }

                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Audit Trail CSV", csvText)
                                clipboard.setPrimaryClip(clip)
                                viewModel.showToast("Audit Trail CSV copied!")
                                showExportDialog = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy CSV")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Liquor Store Audit Trail Export")
                            putExtra(Intent.EXTRA_TEXT, fullText)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Audit Trail"))
                        showExportDialog = false
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share Via...")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Admin PIN dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Admin PIN Required", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter Master Admin PIN (Default: 1234) to unlock administrator audit privileges:")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            pinInput = it
                            pinError = false
                        },
                        label = { Text("4-Digit PIN") },
                        isError = pinError,
                        supportingText = if (pinError) {
                            { Text("Invalid PIN. Please try again.") }
                        } else null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (viewModel.verifyAdminPin(pinInput)) {
                            showPinDialog = false
                        } else {
                            pinError = true
                        }
                    }
                ) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AuditLogItemCard(
    log: AuditLog,
    onClick: () -> Unit
) {
    val roleColor = when (log.userRole.lowercase()) {
        "admin" -> Color(0xFFD32F2F)
        "staff" -> Color(0xFF1976D2)
        else -> Color(0xFF616161)
    }

    val actionColor = getActionColor(log.actionType)
    val actionIcon = getActionIcon(log.actionType)

    val relativeTime = formatRelativeTime(log.timestamp)
    val exactTime = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(log.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("audit_item_${log.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: User Role Pill, Action Badge, and Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Role Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = roleColor.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, roleColor.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (log.userRole.equals("Admin", ignoreCase = true)) Icons.Default.Shield else if (log.userRole.equals("Staff", ignoreCase = true)) Icons.Default.Person else Icons.Default.SettingsSuggest,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = roleColor
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = log.userRole.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = roleColor
                            )
                        }
                    }

                    // Action Type Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = actionColor.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = actionIcon,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = actionColor
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formatActionName(log.actionType),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = actionColor
                            )
                        }
                    }
                }

                // Time Info
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = relativeTime,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = exactTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Target Entity Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = getEntityIcon(log.targetEntity),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = log.targetEntity,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Changed Fields chips if available
            if (log.changedFields.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Text(
                        text = "Changes: ${log.changedFields}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Previous vs New Values Diff Block
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Before / Old Value Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFFFEBEE),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.RemoveCircleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = Color(0xFFD32F2F)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "BEFORE",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD32F2F)
                            )
                        }
                    }
                    Text(
                        text = if (log.oldValue.isNotBlank()) log.oldValue else "(None)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFC62828),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                // After / New Value Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFE8F5E9),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = Color(0xFF2E7D32)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "AFTER",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                    Text(
                        text = if (log.newValue.isNotBlank()) log.newValue else "(None)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1B5E20),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AuditMetricItem(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = color
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AuditMetricDivider() {
    Box(
        modifier = Modifier
            .height(28.dp)
            .width(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        diff < 60000 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days == 1L -> "Yesterday"
        days in 2..6 -> "${days}d ago"
        else -> SimpleDateFormat("dd MMM", Locale.US).format(Date(timestamp))
    }
}

private fun formatActionName(action: String): String {
    return when (action) {
        "UPDATE_STOCK" -> "Stock Adjustment"
        "UPDATE_PAST_STOCK" -> "Past Stock Edit"
        "STOCK_RECEIPT" -> "Inward Delivery"
        "UPDATE_RECEIPT" -> "Delivery Edit"
        "DELETE_RECEIPT" -> "Delivery Deleted"
        "CREATE_EXPENSE" -> "Expense Added"
        "DELETE_EXPENSE" -> "Expense Deleted"
        "CLOSE_DAY" -> "Day Closed"
        "REOPEN_DAY" -> "Day Reopened"
        "CREATE_PRODUCT" -> "Catalog Added"
        "UPDATE_PRODUCT" -> "Catalog Updated"
        "DELETE_PRODUCT" -> "SKU Deleted"
        "PRICE_CHANGE" -> "Price Revision"
        "BREAKAGE_ADJUSTMENT" -> "Breakage Audit"
        "INITIAL_SEED" -> "Catalog Seed"
        else -> action.replace("_", " ")
    }
}

private fun getActionColor(action: String): Color {
    return when {
        action.contains("DELETE", ignoreCase = true) -> Color(0xFFD32F2F)
        action.contains("RECEIPT", ignoreCase = true) -> Color(0xFF00796B)
        action.contains("EXPENSE", ignoreCase = true) -> Color(0xFFE65100)
        action.contains("CLOSE", ignoreCase = true) -> Color(0xFF2E7D32)
        action.contains("REOPEN", ignoreCase = true) -> Color(0xFFC2185B)
        action.contains("PRICE", ignoreCase = true) -> Color(0xFF7B1FA2)
        action.contains("BREAKAGE", ignoreCase = true) -> Color(0xFFD84315)
        action.contains("PRODUCT", ignoreCase = true) -> Color(0xFF303F9F)
        action.contains("SEED", ignoreCase = true) -> Color(0xFF455A64)
        else -> Color(0xFF1976D2)
    }
}

private fun getActionIcon(action: String): ImageVector {
    return when {
        action.contains("DELETE", ignoreCase = true) -> Icons.Default.Delete
        action.contains("RECEIPT", ignoreCase = true) -> Icons.Default.ReceiptLong
        action.contains("EXPENSE", ignoreCase = true) -> Icons.Default.Payments
        action.contains("CLOSE", ignoreCase = true) -> Icons.Default.Lock
        action.contains("REOPEN", ignoreCase = true) -> Icons.Default.LockOpen
        action.contains("PRICE", ignoreCase = true) -> Icons.Default.Sell
        action.contains("BREAKAGE", ignoreCase = true) -> Icons.Default.ReportProblem
        action.contains("PRODUCT", ignoreCase = true) -> Icons.Default.LocalOffer
        action.contains("SEED", ignoreCase = true) -> Icons.Default.SettingsSuggest
        else -> Icons.Default.EditNote
    }
}

private fun getEntityIcon(entity: String): ImageVector {
    return when {
        entity.contains("Invoice", ignoreCase = true) || entity.contains("Receipt", ignoreCase = true) -> Icons.Default.ReceiptLong
        entity.contains("Expense", ignoreCase = true) -> Icons.Default.Payments
        entity.contains("Summary", ignoreCase = true) || entity.contains("Day", ignoreCase = true) -> Icons.Default.CalendarToday
        entity.contains("Catalog", ignoreCase = true) || entity.contains("SKU", ignoreCase = true) -> Icons.Default.Inventory2
        else -> Icons.Default.Liquor
    }
}
