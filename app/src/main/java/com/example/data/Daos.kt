package com.example.data

import androidx.room.*
import com.example.model.FurnitureOrder
import com.example.model.ModelPreset
import com.example.model.PaymentRecord
import com.example.model.Workshop
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkshopDao {
    @Query("SELECT * FROM workshops ORDER BY id ASC")
    fun getAllWorkshops(): Flow<List<Workshop>>

    @Query("SELECT * FROM workshops ORDER BY id ASC")
    suspend fun getAllWorkshopsSync(): List<Workshop>

    @Query("SELECT * FROM workshops WHERE id = :id LIMIT 1")
    suspend fun getWorkshopById(id: Long): Workshop?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkshop(workshop: Workshop): Long

    @Update
    suspend fun updateWorkshop(workshop: Workshop)

    @Delete
    suspend fun deleteWorkshop(workshop: Workshop)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(workshops: List<Workshop>)

    @Query("DELETE FROM workshops")
    suspend fun clearAll()

    @Query("DELETE FROM workshops WHERE id = :id")
    suspend fun deleteWorkshopById(id: Long)

    @Query("DELETE FROM furniture_orders WHERE workshopId = :workshopId")
    suspend fun deleteOrdersByWorkshop(workshopId: Long)

    @Query("DELETE FROM payment_records WHERE workshopId = :workshopId")
    suspend fun deletePaymentsByWorkshop(workshopId: Long)

    @Query("DELETE FROM model_presets WHERE workshopId = :workshopId")
    suspend fun deletePresetsByWorkshop(workshopId: Long)
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM furniture_orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<FurnitureOrder>>

    @Query("SELECT * FROM furniture_orders ORDER BY createdAt DESC")
    suspend fun getAllOrdersSync(): List<FurnitureOrder>

    @Query("SELECT * FROM furniture_orders WHERE workshopId = :workshopId ORDER BY createdAt DESC")
    fun getOrdersByWorkshop(workshopId: Long): Flow<List<FurnitureOrder>>

    @Query("SELECT * FROM furniture_orders WHERE workshopId = :workshopId ORDER BY createdAt DESC")
    suspend fun getOrdersByWorkshopSync(workshopId: Long): List<FurnitureOrder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: FurnitureOrder): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(orders: List<FurnitureOrder>)

    @Update
    suspend fun updateOrder(order: FurnitureOrder)

    @Delete
    suspend fun deleteOrder(order: FurnitureOrder)

    @Query("DELETE FROM furniture_orders WHERE id = :id")
    suspend fun deleteOrderById(id: Long)

    @Query("UPDATE furniture_orders SET colorCode = :newColor WHERE LOWER(TRIM(modelName)) = LOWER(TRIM(:modelName)) AND workshopId = :workshopId")
    suspend fun updateModelColor(modelName: String, newColor: String, workshopId: Long)

    @Query("DELETE FROM furniture_orders")
    suspend fun clearAll()
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payment_records ORDER BY createdAt DESC")
    fun getAllPayments(): Flow<List<PaymentRecord>>

    @Query("SELECT * FROM payment_records ORDER BY createdAt DESC")
    suspend fun getAllPaymentsSync(): List<PaymentRecord>

    @Query("SELECT * FROM payment_records WHERE workshopId = :workshopId ORDER BY createdAt DESC")
    fun getPaymentsByWorkshop(workshopId: Long): Flow<List<PaymentRecord>>

    @Query("SELECT * FROM payment_records WHERE workshopId = :workshopId ORDER BY createdAt DESC")
    suspend fun getPaymentsByWorkshopSync(workshopId: Long): List<PaymentRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(payments: List<PaymentRecord>)

    @Update
    suspend fun updatePayment(payment: PaymentRecord)

    @Delete
    suspend fun deletePayment(payment: PaymentRecord)

    @Query("DELETE FROM payment_records WHERE id = :id")
    suspend fun deletePaymentById(id: Long)

    @Query("DELETE FROM payment_records")
    suspend fun clearAll()
}

@Dao
interface ModelPresetDao {
    @Query("SELECT * FROM model_presets ORDER BY id ASC")
    fun getAllPresets(): Flow<List<ModelPreset>>

    @Query("SELECT * FROM model_presets ORDER BY id ASC")
    suspend fun getAllPresetsSync(): List<ModelPreset>

    @Query("SELECT * FROM model_presets WHERE workshopId = :workshopId ORDER BY id ASC")
    fun getPresetsByWorkshop(workshopId: Long): Flow<List<ModelPreset>>

    @Query("SELECT * FROM model_presets WHERE workshopId = :workshopId ORDER BY id ASC")
    suspend fun getPresetsByWorkshopSync(workshopId: Long): List<ModelPreset>

    @Query("SELECT * FROM model_presets WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun getPresetByName(name: String): ModelPreset?

    @Query("SELECT * FROM model_presets WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) AND workshopId = :workshopId LIMIT 1")
    suspend fun getPresetByNameAndWorkshop(name: String, workshopId: Long): ModelPreset?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: ModelPreset): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(presets: List<ModelPreset>)

    @Update
    suspend fun updatePreset(preset: ModelPreset)

    @Delete
    suspend fun deletePreset(preset: ModelPreset)

    @Query("DELETE FROM model_presets WHERE id = :id")
    suspend fun deletePresetById(id: Long)

    @Query("DELETE FROM model_presets WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name))")
    suspend fun deletePresetByName(name: String)

    @Query("DELETE FROM model_presets WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) AND workshopId = :workshopId")
    suspend fun deletePresetByNameAndWorkshop(name: String, workshopId: Long)

    @Query("UPDATE model_presets SET colorCode = :newColor WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) AND workshopId = :workshopId")
    suspend fun updatePresetColor(name: String, newColor: String, workshopId: Long)

    @Query("DELETE FROM model_presets")
    suspend fun clearAll()
}

@Dao
interface UnitRuleDao {
    @Query("SELECT * FROM unit_conversion_rules ORDER BY pieceCount DESC")
    fun getAllRules(): Flow<List<com.example.model.UnitConversionRule>>

    @Query("SELECT * FROM unit_conversion_rules ORDER BY pieceCount DESC")
    suspend fun getAllRulesSync(): List<com.example.model.UnitConversionRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: com.example.model.UnitConversionRule): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<com.example.model.UnitConversionRule>)

    @Update
    suspend fun updateRule(rule: com.example.model.UnitConversionRule)

    @Delete
    suspend fun deleteRule(rule: com.example.model.UnitConversionRule)

    @Query("DELETE FROM unit_conversion_rules WHERE id = :id")
    suspend fun deleteRuleById(id: Long)

    @Query("DELETE FROM unit_conversion_rules")
    suspend fun clearAll()
}
