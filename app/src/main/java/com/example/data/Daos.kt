package com.example.data

import androidx.room.*
import com.example.model.FurnitureOrder
import com.example.model.ModelPreset
import com.example.model.PaymentRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Query("SELECT * FROM furniture_orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<FurnitureOrder>>

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

    @Query("DELETE FROM furniture_orders")
    suspend fun clearAll()
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payment_records ORDER BY createdAt DESC")
    fun getAllPayments(): Flow<List<PaymentRecord>>

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

    @Query("DELETE FROM model_presets")
    suspend fun clearAll()
}

@Dao
interface UnitRuleDao {
    @Query("SELECT * FROM unit_conversion_rules ORDER BY pieceCount DESC")
    fun getAllRules(): Flow<List<com.example.model.UnitConversionRule>>

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
