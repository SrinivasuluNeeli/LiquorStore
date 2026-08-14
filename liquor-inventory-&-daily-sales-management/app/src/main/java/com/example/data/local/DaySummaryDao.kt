package com.example.data.local

import androidx.room.*
import com.example.data.model.DaySummary
import kotlinx.coroutines.flow.Flow

@Dao
interface DaySummaryDao {
    @Query("SELECT * FROM day_summaries WHERE date = :date LIMIT 1")
    fun getDaySummaryFlow(date: String): Flow<DaySummary?>

    @Query("SELECT * FROM day_summaries WHERE date = :date LIMIT 1")
    suspend fun getDaySummary(date: String): DaySummary?

    @Query("SELECT * FROM day_summaries ORDER BY date DESC")
    fun getAllDaySummaries(): Flow<List<DaySummary>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSummary(summary: DaySummary)
}
