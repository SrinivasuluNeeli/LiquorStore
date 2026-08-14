package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Database Entity representing a Liquor Inventory Item / Master SKU.
 *
 * Core fields:
 * - brandName: Brand Name (e.g., "M.H. Brandy", "Royal Challenge", "Old Monk")
 * - sku: Stock Keeping Unit / Barcode (e.g., "SKU-BRN-750-001")
 * - unitSize: Bottle volume / Packaging unit size (e.g., "1000 ML", "750 ML", "375 ML", "180 ML")
 * - currentStockLevel: Current live inventory stock count on hand
 * - reorderPoint: Minimum stock safety threshold triggering reorder alerts
 */
@Entity(
    tableName = "products",
    indices = [
        Index(value = ["sku"], unique = true),
        Index(value = ["category"]),
        Index(value = ["brand_name"])
    ]
)
data class Product(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String, // e.g. "p_1" to "p_153"

    @ColumnInfo(name = "sku")
    val sku: String = id,

    @ColumnInfo(name = "brand_name")
    val brandName: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "category")
    val category: String, // Brandy, Rum, Vodka, Wine, Whisky, Beer

    @ColumnInfo(name = "unit_size")
    val unitSize: String, // e.g. "1000 ML", "750 ML", "375 ML", "180 ML", "90 ML", "60 ML", "650 ML", "500 ML", "330 ML", "275 ML"

    @ColumnInfo(name = "current_stock_level")
    val currentStockLevel: Int = 0,

    @ColumnInfo(name = "reorder_point")
    val reorderPoint: Int = 10,

    @ColumnInfo(name = "cost_price")
    val costPrice: Double = 0.0,

    @ColumnInfo(name = "default_rate")
    val defaultRate: Double = 0.0,

    @ColumnInfo(name = "initial_opening_stock")
    val initialOpeningStock: Int = 0,

    @ColumnInfo(name = "display_order")
    val displayOrder: Int = 0,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true
) {
    // Convenient aliases & helper properties for backward compatibility
    val brand: String get() = brandName
    val bottleSizeMl: String get() = unitSize
    val minStock: Int get() = reorderPoint

    val isLowStock: Boolean
        get() = currentStockLevel <= reorderPoint

    val displayLabel: String
        get() = "$brandName $name - $unitSize - ₹${defaultRate.toInt()}"
}
