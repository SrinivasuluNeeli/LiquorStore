package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_receipts")
data class StockReceipt(
    @PrimaryKey val id: String, // UUID string
    val receiptNo: String,
    val supplierName: String,
    val invoiceNo: String,
    val invoiceDate: String, // "YYYY-MM-DD"
    val totalAmount: Double,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "stock_receipt_items")
data class StockReceiptItem(
    @PrimaryKey val id: String, // UUID
    val receiptId: String,
    val productId: String,
    val quantity: Int,
    val unitCost: Double
)
