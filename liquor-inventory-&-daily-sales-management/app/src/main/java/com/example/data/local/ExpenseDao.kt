package com.example.data.local

import androidx.room.*
import com.example.data.model.DailyExpense
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM daily_expenses ORDER BY createdAt DESC")
    fun getAllExpenses(): Flow<List<DailyExpense>>

    @Query("SELECT * FROM daily_expenses WHERE date = :date ORDER BY createdAt DESC")
    fun getExpensesForDate(date: String): Flow<List<DailyExpense>>

    @Query("SELECT SUM(amount) FROM daily_expenses WHERE date = :date")
    suspend fun getTotalExpensesForDate(date: String): Double?

    @Query("SELECT SUM(amount) FROM daily_expenses WHERE date = :date AND paymentMode = 'Cash'")
    suspend fun getCashExpensesForDate(date: String): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: DailyExpense)

    @Delete
    suspend fun deleteExpense(expense: DailyExpense)
}
