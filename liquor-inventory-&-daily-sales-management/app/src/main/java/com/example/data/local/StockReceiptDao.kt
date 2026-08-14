package com.example.data.local

import androidx.room.*
import com.example.data.model.StockReceipt
import com.example.data.model.StockReceiptItem
import kotlinx.coroutines.flow.Flow

@Dao
interface StockReceiptDao {
    @Query("SELECT * FROM stock_receipts ORDER BY createdAt DESC")
    fun getAllReceipts(): Flow<List<StockReceipt>>

    @Query("SELECT * FROM stock_receipts WHERE invoiceDate = :date ORDER BY createdAt DESC")
    fun getReceiptsForDate(date: String): Flow<List<StockReceipt>>

    @Query("SELECT * FROM stock_receipt_items WHERE receiptId = :receiptId")
    suspend fun getItemsForReceipt(receiptId: String): List<StockReceiptItem>

    @Query("SELECT * FROM stock_receipt_items WHERE receiptId IN (SELECT id FROM stock_receipts WHERE invoiceDate = :date)")
    suspend fun getReceiptItemsForDate(date: String): List<StockReceiptItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipt(receipt: StockReceipt)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceiptItems(items: List<StockReceiptItem>)

    @Query("DELETE FROM stock_receipts WHERE id = :receiptId")
    suspend fun deleteReceipt(receiptId: String)

    @Query("DELETE FROM stock_receipt_items WHERE receiptId = :receiptId")
    suspend fun deleteReceiptItems(receiptId: String)
}
