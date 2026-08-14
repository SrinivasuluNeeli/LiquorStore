package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.*

@Database(
    entities = [
        Product::class,
        DailyStockRecord::class,
        StockReceipt::class,
        StockReceiptItem::class,
        DailyExpense::class,
        DaySummary::class,
        AuditLog::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun dailyStockDao(): DailyStockDao
    abstract fun stockReceiptDao(): StockReceiptDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun daySummaryDao(): DaySummaryDao
    abstract fun auditDao(): AuditDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "liquor_inventory_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
