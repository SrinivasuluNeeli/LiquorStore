package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_stock_records",
    indices = [Index(value = ["date", "productId"], unique = true)]
)
data class DailyStockRecord(
    @PrimaryKey val id: String, // e.g. "d_2026-08-13_p_1"
    val date: String, // "YYYY-MM-DD"
    val productId: String,
    val openingStock: Int,
    val receivedQty: Int = 0,
    val breakageQty: Int = 0,
    val closingStock: Int,
    val rate: Double,
    val costPrice: Double,
    val notes: String = ""
) {
    val totalAvailable: Int
        get() = (openingStock + receivedQty - breakageQty).coerceAtLeast(0)

    val salesQty: Int
        get() = (totalAvailable - closingStock).coerceAtLeast(0)

    val totalSalesValue: Double
        get() = salesQty * rate

    val grossProfit: Double
        get() = salesQty * (rate - costPrice)
}
