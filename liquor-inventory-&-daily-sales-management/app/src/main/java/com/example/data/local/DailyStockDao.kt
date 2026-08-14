package com.example.data.local

import androidx.room.*
import com.example.data.model.DailyStockRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyStockDao {
    @Query("SELECT * FROM daily_stock_records WHERE date = :date")
    fun getRecordsForDate(date: String): Flow<List<DailyStockRecord>>

    @Query("SELECT * FROM daily_stock_records ORDER BY date ASC")
    fun getAllRecordsFlow(): Flow<List<DailyStockRecord>>

    @Query("SELECT * FROM daily_stock_records WHERE date = :date")
    suspend fun getRecordsListForDate(date: String): List<DailyStockRecord>

    @Query("SELECT * FROM daily_stock_records WHERE date = :date AND productId = :productId LIMIT 1")
    suspend fun getRecord(date: String, productId: String): DailyStockRecord?

    @Query("SELECT * FROM daily_stock_records WHERE productId = :productId AND date < :date ORDER BY date DESC LIMIT 1")
    suspend fun getPreviousDayRecord(productId: String, date: String): DailyStockRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRecord(record: DailyStockRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(records: List<DailyStockRecord>)

    @Query("DELETE FROM daily_stock_records WHERE date = :date")
    suspend fun deleteRecordsForDate(date: String)
}
