package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey val id: String, // UUID
    val timestamp: Long = System.currentTimeMillis(),
    val userRole: String, // "Staff", "Admin"
    val actionType: String, // "CREATE", "UPDATE", "DELETE", "STOCK_RECEIPT", "CLOSE_DAY", "REOPEN_DAY", "AUDIT_ADJUSTMENT", "PRICE_CHANGE"
    val targetEntity: String,
    val changedFields: String = "",
    val oldValue: String = "",
    val newValue: String = ""
)
