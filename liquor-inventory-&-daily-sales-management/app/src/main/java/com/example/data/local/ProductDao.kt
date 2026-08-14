package com.example.data.local

import androidx.room.*
import com.example.data.model.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY category ASC, display_order ASC, brand_name ASC, name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE is_active = 1 ORDER BY category ASC, display_order ASC, brand_name ASC, name ASC")
    fun getActiveProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: String): Product?

    @Query("SELECT * FROM products WHERE sku = :sku LIMIT 1")
    suspend fun getProductBySku(sku: String): Product?

    @Query("SELECT * FROM products WHERE brand_name LIKE '%' || :brandName || '%' ORDER BY name ASC")
    fun getProductsByBrand(brandName: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE category = :category AND is_active = 1 ORDER BY display_order ASC, brand_name ASC")
    fun getProductsByCategory(category: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE current_stock_level <= reorder_point AND is_active = 1 ORDER BY current_stock_level ASC")
    fun getLowStockProducts(): Flow<List<Product>>

    @Query("SELECT COUNT(*) FROM products")
    suspend fun getProductCount(): Int

    @Query("UPDATE products SET current_stock_level = :newStockLevel WHERE id = :productId")
    suspend fun updateStockLevel(productId: String, newStockLevel: Int)

    @Query("UPDATE products SET reorder_point = :reorderPoint WHERE id = :productId")
    suspend fun updateReorderPoint(productId: String, reorderPoint: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<Product>)

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)
}
