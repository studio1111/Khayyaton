package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.FurnitureOrder
import com.example.model.ModelPreset
import com.example.model.PaymentRecord
import com.example.model.UnitConversionRule
import kotlinx.coroutines.flow.Flow

@Database(
    entities = [FurnitureOrder::class, PaymentRecord::class, ModelPreset::class, UnitConversionRule::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao
    abstract fun paymentDao(): PaymentDao
    abstract fun modelPresetDao(): ModelPresetDao
    abstract fun unitRuleDao(): UnitRuleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sheeton_workshop.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class WorkshopRepository(
    private val orderDao: OrderDao,
    private val paymentDao: PaymentDao,
    private val modelPresetDao: ModelPresetDao,
    private val unitRuleDao: UnitRuleDao
) {
    val orders: Flow<List<FurnitureOrder>> = orderDao.getAllOrders()
    val payments: Flow<List<PaymentRecord>> = paymentDao.getAllPayments()
    val modelPresets: Flow<List<ModelPreset>> = modelPresetDao.getAllPresets()
    val unitRules: Flow<List<UnitConversionRule>> = unitRuleDao.getAllRules()

    suspend fun saveOrder(order: FurnitureOrder) {
        if (order.id == 0L) {
            orderDao.insertOrder(order)
        } else {
            orderDao.updateOrder(order)
        }
    }

    suspend fun deleteOrder(order: FurnitureOrder) {
        orderDao.deleteOrder(order)
    }

    suspend fun deleteOrderById(id: Long) {
        orderDao.deleteOrderById(id)
    }

    suspend fun savePayment(payment: PaymentRecord) {
        if (payment.id == 0L) {
            paymentDao.insertPayment(payment)
        } else {
            paymentDao.updatePayment(payment)
        }
    }

    suspend fun deletePayment(payment: PaymentRecord) {
        paymentDao.deletePayment(payment)
    }

    suspend fun deletePaymentById(id: Long) {
        paymentDao.deletePaymentById(id)
    }

    suspend fun savePreset(preset: ModelPreset) {
        if (preset.id == 0L) {
            modelPresetDao.insertPreset(preset)
        } else {
            modelPresetDao.updatePreset(preset)
        }
    }

    suspend fun deletePreset(preset: ModelPreset) {
        modelPresetDao.deletePreset(preset)
    }

    suspend fun deletePresetById(id: Long) {
        modelPresetDao.deletePresetById(id)
    }

    suspend fun deletePresetByName(name: String) {
        modelPresetDao.deletePresetByName(name)
    }

    suspend fun saveUnitRule(rule: UnitConversionRule) {
        if (rule.id == 0L) {
            unitRuleDao.insertRule(rule)
        } else {
            unitRuleDao.updateRule(rule)
        }
    }

    suspend fun deleteUnitRule(rule: UnitConversionRule) {
        unitRuleDao.deleteRule(rule)
    }

    suspend fun deleteUnitRuleById(id: Long) {
        unitRuleDao.deleteRuleById(id)
    }

    suspend fun insertDefaultUnitRulesIfEmpty() {
        val defaultRules = listOf(
            UnitConversionRule(pieceKey = "3", pieceCount = 3.0, calculatedUnits = 2.0, isEnabled = true),
            UnitConversionRule(pieceKey = "2", pieceCount = 2.0, calculatedUnits = 1.5, isEnabled = true),
            UnitConversionRule(pieceKey = "1", pieceCount = 1.0, calculatedUnits = 1.0, isEnabled = true),
            UnitConversionRule(pieceKey = "0.5", pieceCount = 0.5, calculatedUnits = 0.5, isEnabled = true)
        )
        unitRuleDao.insertAll(defaultRules)
    }

    suspend fun seedInitialDataIfEmpty() {
        // App starts clean with zero sample data as requested
    }

    suspend fun resetAllData() {
        orderDao.clearAll()
        paymentDao.clearAll()
        modelPresetDao.clearAll()
        unitRuleDao.clearAll()
    }
}
