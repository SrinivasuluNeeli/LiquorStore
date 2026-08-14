package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.DailyExpense
import com.example.data.model.DailyStockRecord
import com.example.data.model.DaySummary
import com.example.data.model.Product
import com.example.data.model.StockReceipt
import com.example.data.model.StockReceiptItem
import com.example.data.repository.LiquorRepository
import com.example.data.seed.ProductSeedData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LiquorInventoryValidationTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: LiquorRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LiquorRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `test DailyStockRecord calculation formulas`() {
        val record = DailyStockRecord(
            id = "d_2026-08-13_p1",
            date = "2026-08-13",
            productId = "p1",
            openingStock = 20,
            receivedQty = 10,
            breakageQty = 2,
            closingStock = 18,
            rate = 500.0,
            costPrice = 380.0
        )

        // totalAvailable = openingStock + receivedQty - breakageQty = 20 + 10 - 2 = 28
        assertEquals(28, record.totalAvailable)
        // salesQty = totalAvailable - closingStock = 28 - 18 = 10
        assertEquals(10, record.salesQty)
        // totalSalesValue = salesQty * rate = 10 * 500.0 = 5000.0
        assertEquals(5000.0, record.totalSalesValue, 0.001)
        // grossProfit = salesQty * (rate - costPrice) = 10 * (500 - 380) = 1200.0
        assertEquals(1200.0, record.grossProfit, 0.001)
    }

    @Test
    fun `test DailyStockRecord edge case when closing stock exceeds available`() {
        val record = DailyStockRecord(
            id = "d_2026-08-13_p2",
            date = "2026-08-13",
            productId = "p2",
            openingStock = 5,
            receivedQty = 0,
            breakageQty = 1,
            closingStock = 10, // Higher than available (4)
            rate = 200.0,
            costPrice = 150.0
        )

        assertEquals(4, record.totalAvailable)
        // salesQty is coercedAtLeast(0)
        assertEquals(0, record.salesQty)
        assertEquals(0.0, record.totalSalesValue, 0.001)
        assertEquals(0.0, record.grossProfit, 0.001)
    }

    @Test
    fun `test DaySummary revenue calculation and balance check`() {
        val daySummary = DaySummary(
            date = "2026-08-13",
            isClosed = true,
            cashCollected = 50000.0,
            upiCollected = 30000.0,
            creditSales = 5000.0,
            totalExpenses = 8500.0,
            notes = "Standard closing"
        )

        // totalSalesRevenue = 50000 + 30000 + 5000 = 85000.0
        assertEquals(85000.0, daySummary.totalSalesRevenue, 0.001)
        // netCashInHand = 50000 - 8500 = 41500.0
        assertEquals(41500.0, daySummary.netCashInHand, 0.001)
    }

    @Test
    fun `test Product master catalog properties`() {
        val product = Product(
            id = "p_test_1",
            brand = "Royal Challenge",
            name = "Select Premium",
            category = "Whisky",
            bottleSizeMl = "750 ML",
            minStock = 10,
            costPrice = 650.0,
            defaultRate = 880.0,
            initialOpeningStock = 24,
            isActive = true
        )

        assertEquals("Royal Challenge Select Premium (750 ML)", product.displayLabel)
        assertEquals("Royal Challenge", product.brandName)
        assertEquals("750 ML", product.unitSize)
        assertEquals(24, product.currentStockLevel)
        assertEquals(10, product.reorderPoint)
    }

    @Test
    fun `test seed database and automatic records generation`() = runBlocking {
        repository.seedDatabaseIfEmpty()
        val productList = repository.allProducts.first()
        assertTrue(productList.size >= 50)

        // Ensure records for a specific date
        val testDate = "2026-08-14"
        repository.ensureRecordsForDate(testDate)

        val records = repository.getRecordsForDate(testDate).first()
        assertEquals(productList.size, records.size)
    }

    @Test
    fun `test stock update and audit logging`() = runBlocking {
        repository.seedDatabaseIfEmpty()
        val testDate = "2026-08-13"
        repository.ensureRecordsForDate(testDate)

        val records = repository.getRecordsForDate(testDate).first()
        val firstRecord = records.first()

        val updatedRecord = firstRecord.copy(
            closingStock = (firstRecord.openingStock - 5).coerceAtLeast(0)
        )

        repository.updateStockRecord(updatedRecord, "Staff", "Physical verification count")

        // Verify updated record in DB
        val fetched = db.dailyStockDao().getRecord(testDate, firstRecord.productId)
        assertNotNull(fetched)
        assertEquals(updatedRecord.closingStock, fetched!!.closingStock)
        assertEquals(5, fetched.salesQty)

        // Verify Audit Log generated
        val logs = repository.allAuditLogs.first()
        val latestLog = logs.firstOrNull { it.targetEntity.contains(testDate) }
        assertNotNull(latestLog)
        assertEquals("Staff", latestLog?.userRole)
    }

    @Test
    fun `test delivery receipt entry and stock inward adjustment`() = runBlocking {
        repository.seedDatabaseIfEmpty()
        val testDate = "2026-08-13"
        repository.ensureRecordsForDate(testDate)

        val products = repository.allProducts.first()
        val sampleProd = products.first()

        val initialRecord = db.dailyStockDao().getRecord(testDate, sampleProd.id)
        val initialReceived = initialRecord?.receivedQty ?: 0

        val receiptId = UUID.randomUUID().toString()
        val receipt = StockReceipt(
            id = receiptId,
            receiptNo = "REC-TEST-01",
            supplierName = "Test Distributor Ltd",
            invoiceNo = "INV-TEST-999",
            invoiceDate = testDate,
            totalAmount = sampleProd.costPrice * 24,
            notes = "Test Delivery"
        )
        val receiptItems = listOf(
            StockReceiptItem(
                id = UUID.randomUUID().toString(),
                receiptId = receiptId,
                productId = sampleProd.id,
                quantity = 24,
                unitCost = sampleProd.costPrice
            )
        )

        repository.addStockReceipt(receipt, receiptItems, "Staff")

        // Check stock record updated with +24 inward
        val updatedRecord = db.dailyStockDao().getRecord(testDate, sampleProd.id)
        assertNotNull(updatedRecord)
        assertEquals(initialReceived + 24, updatedRecord!!.receivedQty)

        // Check receipts in DB
        val receipts = repository.getReceiptsForDate(testDate).first()
        assertTrue(receipts.any { it.invoiceNo == "INV-TEST-999" })
    }

    @Test
    fun `test expense logging and day closure`() = runBlocking {
        repository.seedDatabaseIfEmpty()
        val testDate = "2026-08-13"
        repository.ensureRecordsForDate(testDate)

        val expense = DailyExpense(
            id = "exp_test_101",
            date = testDate,
            category = "Utility Bill",
            amount = 3500.0,
            paymentMode = "UPI / Digital",
            remarks = "Generator Diesel"
        )
        repository.addExpense(expense, "Staff")

        val dayExpenses = repository.getExpensesForDate(testDate).first()
        assertTrue(dayExpenses.any { it.id == "exp_test_101" })

        // Close Day
        repository.closeDay(
            date = testDate,
            cashCollected = 45000.0,
            upiCollected = 25000.0,
            creditSales = 2000.0,
            notes = "Day closed successfully",
            userRole = "Staff"
        )

        val daySummary = db.daySummaryDao().getDaySummary(testDate)
        assertNotNull(daySummary)
        assertTrue(daySummary!!.isClosed)
        assertEquals("Staff", daySummary.closedBy)
        assertEquals(72000.0, daySummary.totalSalesRevenue, 0.001)

        // Reopen Day as Admin
        repository.reopenDay(testDate, "Admin")
        val reopenedSummary = db.daySummaryDao().getDaySummary(testDate)
        assertNotNull(reopenedSummary)
        assertFalse(reopenedSummary!!.isClosed)
    }

    @Test
    fun `test CSV export formatting`() = runBlocking {
        repository.seedDatabaseIfEmpty()
        val testDate = "2026-08-13"
        repository.ensureRecordsForDate(testDate)

        val records = repository.getRecordsForDate(testDate).first()
        val productMap = repository.allProducts.first().associateBy { it.id }

        val csv = repository.exportCSV(testDate, records, productMap)
        assertTrue(csv.contains("Date,Category,Brand,Product Name"))
        assertTrue(csv.contains("SUMMARY"))
        assertTrue(csv.contains("Total Bottles Sold"))
    }
}
