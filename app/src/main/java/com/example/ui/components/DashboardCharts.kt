package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CategoryColors
import com.example.ui.viewmodel.CategoryStockMovementItem
import com.example.ui.viewmodel.DailySalesTrendPoint
import com.example.ui.viewmodel.MonthlyExpenseCategoryItem
import kotlin.math.cos
import kotlin.math.sin

/**
 * Interactive Daily Sales Trend Chart (Area + Bezier Curve + Bar Mode)
 * Recharts-style interactive charting with touch inspection tooltips.
 */
@Composable
fun DailySalesTrendCard(
    dataPoints: List<DailySalesTrendPoint>,
    modifier: Modifier = Modifier
) {
    var selectedMetric by remember { mutableStateOf("Revenue") } // Revenue, Bottles, Profit
    var selectedRange by remember { mutableStateOf("Last 7 Days") } // Last 7 Days, Last 14 Days, All
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

    val filteredPoints = remember(dataPoints, selectedRange) {
        when (selectedRange) {
            "Last 7 Days" -> dataPoints.takeLast(7)
            "Last 14 Days" -> dataPoints.takeLast(14)
            else -> dataPoints
        }
    }

    val totalPeriodRevenue = remember(filteredPoints) { filteredPoints.sumOf { it.totalRevenue } }
    val totalPeriodBottles = remember(filteredPoints) { filteredPoints.sumOf { it.totalBottles } }
    val totalPeriodProfit = remember(filteredPoints) { filteredPoints.sumOf { it.grossProfit } }
    val avgDailyRevenue = remember(filteredPoints) {
        if (filteredPoints.isNotEmpty()) totalPeriodRevenue / filteredPoints.size else 0.0
    }
    val peakDay = remember(filteredPoints) {
        filteredPoints.maxByOrNull { it.totalRevenue }
    }

    Card(
        modifier = modifier.fillMaxWidth().testTag("daily_sales_trend_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header & Metric Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Daily Sales Trends",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Historical turnover & revenue trajectory",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Range selector
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(2.dp)
                ) {
                    listOf("7D" to "Last 7 Days", "14D" to "Last 14 Days", "All" to "All").forEach { (short, full) ->
                        val isSelected = selectedRange == full
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable {
                                    selectedRange = full
                                    selectedPointIndex = null
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = short,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metric Toggle Chips (Revenue vs Bottles vs Profit)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Triple("Revenue", "₹${totalPeriodRevenue.toInt()}", MaterialTheme.colorScheme.primary),
                    Triple("Bottles", "$totalPeriodBottles Units", Color(0xFF00897B)),
                    Triple("Profit", "₹${totalPeriodProfit.toInt()}", Color(0xFFFB8C00))
                ).forEach { (metric, statVal, tagColor) ->
                    val active = selectedMetric == metric
                    FilterChip(
                        selected = active,
                        onClick = {
                            selectedMetric = metric
                            selectedPointIndex = null
                        },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(tagColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "$metric: $statVal",
                                    fontSize = 11.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Canvas Line / Area Chart
            if (filteredPoints.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No sales data available for this range", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            } else {
                val primaryColor = when (selectedMetric) {
                    "Revenue" -> MaterialTheme.colorScheme.primary
                    "Bottles" -> Color(0xFF00897B)
                    else -> Color(0xFFFB8C00)
                }
                val gradientStart = primaryColor.copy(alpha = 0.45f)
                val gradientEnd = primaryColor.copy(alpha = 0.02f)
                val onSurfaceColor = MaterialTheme.colorScheme.onSurfaceVariant

                val maxVal = remember(filteredPoints, selectedMetric) {
                    val peak = when (selectedMetric) {
                        "Revenue" -> filteredPoints.maxOfOrNull { it.totalRevenue } ?: 1.0
                        "Bottles" -> filteredPoints.maxOfOrNull { it.totalBottles.toDouble() } ?: 1.0
                        else -> filteredPoints.maxOfOrNull { it.grossProfit } ?: 1.0
                    }
                    if (peak <= 0.0) 1.0 else peak * 1.15 // Add 15% top headroom
                }

                val minVal = 0.0

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .pointerInput(filteredPoints) {
                            detectTapGestures { offset ->
                                val stepX = size.width / (filteredPoints.size - 1).coerceAtLeast(1)
                                val index = (offset.x / stepX).toInt().coerceIn(0, filteredPoints.size - 1)
                                selectedPointIndex = if (selectedPointIndex == index) null else index
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 20.dp)) {
                        val width = size.width
                        val height = size.height
                        val n = filteredPoints.size
                        val stepX = if (n > 1) width / (n - 1) else width

                        // Horizontal guide lines
                        val gridLines = 3
                        for (i in 0..gridLines) {
                            val y = height * (i.toFloat() / gridLines)
                            drawLine(
                                color = onSurfaceColor.copy(alpha = 0.15f),
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Build smooth Path
                        val points = filteredPoints.mapIndexed { idx, pt ->
                            val value = when (selectedMetric) {
                                "Revenue" -> pt.totalRevenue
                                "Bottles" -> pt.totalBottles.toDouble()
                                else -> pt.grossProfit
                            }
                            val x = if (n > 1) idx * stepX else width / 2f
                            val normalizedY = ((value - minVal) / (maxVal - minVal)).toFloat().coerceIn(0f, 1f)
                            val y = height - (normalizedY * height)
                            Offset(x, y)
                        }

                        if (points.isNotEmpty()) {
                            val linePath = Path().apply {
                                moveTo(points[0].x, points[0].y)
                                for (i in 0 until points.size - 1) {
                                    val p0 = points[i]
                                    val p1 = points[i + 1]
                                    val cx = (p0.x + p1.x) / 2f
                                    cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                                }
                            }

                            // Fill Area under curve
                            val areaPath = Path().apply {
                                addPath(linePath)
                                lineTo(points.last().x, height)
                                lineTo(points.first().x, height)
                                close()
                            }

                            drawPath(
                                path = areaPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(gradientStart, gradientEnd),
                                    startY = 0f,
                                    endY = height
                                )
                            )

                            // Draw Line Stroke
                            drawPath(
                                path = linePath,
                                color = primaryColor,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Draw Dots
                            points.forEachIndexed { idx, offset ->
                                val isSelected = selectedPointIndex == idx
                                drawCircle(
                                    color = if (isSelected) primaryColor else Color.White,
                                    radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                                    center = offset
                                )
                                drawCircle(
                                    color = primaryColor,
                                    radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                                    center = offset,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        }
                    }
                }

                // X-Axis Labels Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    filteredPoints.forEachIndexed { idx, pt ->
                        val isSelected = selectedPointIndex == idx
                        Text(
                            text = pt.formattedDate,
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Interactive Scrubber / Tooltip Callout
                selectedPointIndex?.let { idx ->
                    if (idx in filteredPoints.indices) {
                        val pt = filteredPoints[idx]
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${pt.date} (${pt.formattedDate})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Revenue", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                                        Text("₹${pt.totalRevenue.toInt()}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Bottles", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                                        Text("${pt.totalBottles} units", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Profit", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                                        Text("₹${pt.grossProfit.toInt()}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Stats Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Daily Avg Sales", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${avgDailyRevenue.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Divider(modifier = Modifier.height(24.dp).width(1.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Peak Day Record", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (peakDay != null) "${peakDay.formattedDate} (₹${peakDay.totalRevenue.toInt()})" else "N/A",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Monthly Expense Distribution Chart (Interactive Donut / Pie + Legend List)
 */
@Composable
fun MonthlyExpenseDistributionCard(
    expenseItems: List<MonthlyExpenseCategoryItem>,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val totalExpenseSum = remember(expenseItems) { expenseItems.sumOf { it.totalAmount } }

    val categoryColorMap = remember {
        mapOf(
            "Rent" to Color(0xFFD32F2F),
            "Staff Salary" to Color(0xFF1976D2),
            "Licensing & Excise" to Color(0xFF7B1FA2),
            "Electricity" to Color(0xFFFBC02D),
            "Transport & Logistics" to Color(0xFF00796B),
            "Ice & Packaging" to Color(0xFF0288D1),
            "Maintenance & Repairs" to Color(0xFFE64A19),
            "Miscellaneous" to Color(0xFF5D4037)
        )
    }

    Card(
        modifier = modifier.fillMaxWidth().testTag("monthly_expense_distribution_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.PieChart,
                            contentDescription = null,
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Monthly Expense Distribution",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Cost allocations & operational outflow",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "Total: ₹${totalExpenseSum.toInt()}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFFD32F2F)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (expenseItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No expense entries logged for this period", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Donut Canvas
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            var startAngle = -90f
                            val strokeWidth = 26.dp.toPx()

                            expenseItems.forEach { item ->
                                val sweepAngle = (item.percentage / 100f) * 360f
                                val color = categoryColorMap[item.category] ?: Color(0xFF757575)
                                val isSelected = selectedCategory == item.category

                                drawArc(
                                    color = color,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle - 2f, // Subtle gap between slices
                                    useCenter = false,
                                    style = Stroke(
                                        width = if (isSelected) strokeWidth + 6.dp.toPx() else strokeWidth,
                                        cap = StrokeCap.Round
                                    )
                                )
                                startAngle += sweepAngle
                            }
                        }

                        // Center Cutout Text
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = selectedCategory ?: "Total Outflow",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            val displayAmount = if (selectedCategory != null) {
                                expenseItems.find { it.category == selectedCategory }?.totalAmount ?: totalExpenseSum
                            } else {
                                totalExpenseSum
                            }
                            Text(
                                text = "₹${displayAmount.toInt()}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Legend & Percentage Bars
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        expenseItems.take(5).forEach { item ->
                            val color = categoryColorMap[item.category] ?: Color(0xFF757575)
                            val isSelected = selectedCategory == item.category

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
                                    .clickable {
                                        selectedCategory = if (selectedCategory == item.category) null else item.category
                                    }
                                    .padding(vertical = 3.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(color, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = item.category,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "₹${item.totalAmount.toInt()}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "(${String.format("%.0f", item.percentage)}%)",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (expenseItems.size > 5) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "+ ${expenseItems.size - 5} other expense types logged",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

/**
 * Category-Wise Stock Movement Chart (Recharts-style Grouped Bar Visualizer)
 */
@Composable
fun CategoryStockMovementCard(
    movementItems: List<CategoryStockMovementItem>,
    modifier: Modifier = Modifier
) {
    var viewMode by remember { mutableStateOf("Units") } // Units vs Revenue

    val maxBottlesSold = remember(movementItems) {
        (movementItems.maxOfOrNull { it.salesQty } ?: 1).coerceAtLeast(1)
    }

    val maxSalesValue = remember(movementItems) {
        (movementItems.maxOfOrNull { it.salesValue } ?: 1.0).coerceAtLeast(1.0)
    }

    Card(
        modifier = modifier.fillMaxWidth().testTag("category_stock_movement_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.BarChart,
                            contentDescription = null,
                            tint = Color(0xFF00897B),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Category Stock Movement",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Turnover, stock flow & velocity by category",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Units vs Revenue Toggle
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(2.dp)
                ) {
                    listOf("Units" to "Units", "Value" to "Revenue").forEach { (short, full) ->
                        val isSelected = viewMode == full
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { viewMode = full }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = short,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (movementItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No category movement data available", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    movementItems.forEach { item ->
                        val catColor = CategoryColors.forCategory(item.category).primary
                        val totalInStock = item.openingStock + item.receivedQty
                        val sellThroughPct = if (totalInStock > 0) ((item.salesQty.toFloat() / totalInStock) * 100).toInt() else 0

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                                .padding(10.dp)
                        ) {
                            // Category Row Top
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(catColor, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = item.category,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = catColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "$sellThroughPct% Sell-Through",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = catColor,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = if (viewMode == "Revenue") "₹${item.salesValue.toInt()}" else "${item.salesQty} Sold / ${item.closingStock} Rem",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (viewMode == "Revenue") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Horizontal Bar Visualization (Opening vs Sales vs Closing)
                            val barRatio = if (viewMode == "Revenue") {
                                (item.salesValue / maxSalesValue).toFloat().coerceIn(0.05f, 1f)
                            } else {
                                (item.salesQty.toFloat() / maxBottlesSold).coerceIn(0.05f, 1f)
                            }

                            val animatedRatio by animateFloatAsState(targetValue = barRatio, animationSpec = tween(500), label = "barAnim")

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(animatedRatio)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(catColor.copy(alpha = 0.8f), catColor)
                                            )
                                        )
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Granular Stock Flow Chips (Opening -> In -> Sold -> Breakage -> Close)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Open: ${item.openingStock}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("+Recv: ${item.receivedQty}", fontSize = 10.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold)
                                Text("-Sold: ${item.salesQty}", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                if (item.breakageQty > 0) {
                                    Text("Dmg: ${item.breakageQty}", fontSize = 10.sp, color = Color(0xFFD32F2F), fontWeight = FontWeight.SemiBold)
                                }
                                Text("Close: ${item.closingStock}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
