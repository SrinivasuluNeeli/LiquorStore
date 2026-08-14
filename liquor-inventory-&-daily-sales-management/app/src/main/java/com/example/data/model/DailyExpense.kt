package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_expenses")
data class DailyExpense(
    @PrimaryKey val id: String, // UUID
    val date: String, // "YYYY-MM-DD"
    val category: String, // "Supplier Payment", "License / Regulatory Fee", "Utility Bill", "Transport / Freight", "Staff Salary", "Miscellaneous"
    val amount: Double,
    val paymentMode: String, // "Cash", "UPI / Digital", "Bank Transfer"
    val remarks: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
