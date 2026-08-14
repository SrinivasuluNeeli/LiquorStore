package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "day_summaries")
data class DaySummary(
    @PrimaryKey val date: String, // "YYYY-MM-DD"
    val isClosed: Boolean = false,
    val closedAt: Long? = null,
    val closedBy: String? = null, // "Staff", "Admin"
    val cashCollected: Double = 0.0,
    val upiCollected: Double = 0.0,
    val creditSales: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val notes: String = ""
) {
    val totalSalesRevenue: Double
        get() = cashCollected + upiCollected + creditSales

    val netCashInHand: Double
        get() = cashCollected - totalExpenses
}
