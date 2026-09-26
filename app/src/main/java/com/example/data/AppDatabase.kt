package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.FurnitureOrder
import com.example.model.ModelPreset
import com.example.model.PaymentRecord
import com.example.model.UnitConversionRule
import com.example.model.Workshop
import com.example.model.CalendarType
import com.example.util.PersianUtils
import kotlinx.coroutines.flow.Flow
import androidx.room.withTransaction

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE workshops ADD COLUMN syncId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE furniture_orders ADD COLUMN workshopSyncId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE furniture_orders ADD COLUMN syncId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE payment_records ADD COLUMN workshopSyncId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE payment_records ADD COLUMN syncId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE payment_records ADD COLUMN relatedOrderSyncId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE model_presets ADD COLUMN workshopSyncId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE model_presets ADD COLUMN syncId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE unit_conversion_rules ADD COLUMN syncId TEXT NOT NULL DEFAULT ''")

        db.execSQL("UPDATE workshops SET syncId = 'wrk_' || id WHERE syncId = ''")
        db.execSQL("UPDATE furniture_orders SET syncId = 'ord_' || id WHERE syncId = ''")
        db.execSQL("UPDATE payment_records SET syncId = 'pay_' || id WHERE syncId = ''")
        db.execSQL("UPDATE model_presets SET syncId = 'pre_' || id WHERE syncId = ''")
        db.execSQL("UPDATE unit_conversion_rules SET syncId = 'rule_' || id WHERE syncId = ''")

        db.execSQL("""
            UPDATE furniture_orders
            SET workshopSyncId = (
                SELECT syncId FROM workshops WHERE workshops.id = furniture_orders.workshopId
            )
            WHERE workshopSyncId = ''
        """.trimIndent())
        db.execSQL("""
            UPDATE payment_records
            SET workshopSyncId = (
                SELECT syncId FROM workshops WHERE workshops.id = payment_records.workshopId
            )
            WHERE workshopSyncId = ''
        """.trimIndent())
        db.execSQL("""
            UPDATE model_presets
            SET workshopSyncId = (
                SELECT syncId FROM workshops WHERE workshops.id = model_presets.workshopId
            )
            WHERE workshopSyncId = ''
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_workshops_syncId ON workshops(syncId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_furniture_orders_syncId ON furniture_orders(syncId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_furniture_orders_workshopSyncId ON furniture_orders(workshopSyncId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_payment_records_syncId ON payment_records(syncId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_payment_records_workshopSyncId ON payment_records(workshopSyncId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_model_presets_syncId ON model_presets(syncId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_model_presets_workshopSyncId ON model_presets(workshopSyncId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_unit_conversion_rules_syncId ON unit_conversion_rules(syncId)")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS index_workshops_createdAt ON workshops(createdAt)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_furniture_orders_workshopId ON furniture_orders(workshopId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_furniture_orders_createdAt ON furniture_orders(createdAt)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_furniture_orders_invoiceNumber ON furniture_orders(invoiceNumber)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_payment_records_workshopId ON payment_records(workshopId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_payment_records_createdAt ON payment_records(createdAt)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_payment_records_relatedOrderId ON payment_records(relatedOrderId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_model_presets_workshopId ON model_presets(workshopId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_model_presets_name ON model_presets(name)")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS workshops (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, createdAt INTEGER NOT NULL)")
        db.execSQL("INSERT OR IGNORE INTO workshops (id, name, createdAt) VALUES (1, 'کارگاه اصلی', ${System.currentTimeMillis()})")
        try {
            db.execSQL("ALTER TABLE furniture_orders ADD COLUMN workshopId INTEGER NOT NULL DEFAULT 1")
        } catch (_: Exception) {}
        try {
            db.execSQL("ALTER TABLE payment_records ADD COLUMN workshopId INTEGER NOT NULL DEFAULT 1")
        } catch (_: Exception) {}
        try {
            db.execSQL("ALTER TABLE model_presets ADD COLUMN workshopId INTEGER NOT NULL DEFAULT 1")
        } catch (_: Exception) {}
    }
}

@Database(
    entities = [FurnitureOrder::class, PaymentRecord::class, ModelPreset::class, UnitConversionRule::class, Workshop::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao
    abstract fun paymentDao(): PaymentDao
    abstract fun modelPresetDao(): ModelPresetDao
    abstract fun unitRuleDao(): UnitRuleDao
    abstract fun workshopDao(): WorkshopDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "khayyaton_workshop.db"
                )
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class WorkshopRepository(
    private val context: android.content.Context? = null,
    private val database: AppDatabase,
    val orderDao: OrderDao,
    val paymentDao: PaymentDao,
    val modelPresetDao: ModelPresetDao,
    val unitRuleDao: UnitRuleDao,
    val workshopDao: WorkshopDao
) {
    private val prefs by lazy {
        context?.getSharedPreferences("khayyaton_prefs", android.content.Context.MODE_PRIVATE)
    }

    data class PendingCloudDeletion(
        val collection: String,
        val syncId: String
    )

    private fun pendingDeletionKey(): String {
        val uid = getLocalAccountUid().orEmpty()
        return "pending_cloud_deletions_$uid"
    }

    private fun deletionKey(collection: String, syncId: String): String =
        "$collection|$syncId"

    fun recordCloudDeletion(collection: String, syncId: String) {
        if (syncId.isBlank()) return
        val current = prefs?.getStringSet(pendingDeletionKey(), emptySet()).orEmpty().toMutableSet()
        current += deletionKey(collection, syncId)
        prefs?.edit()?.putStringSet(pendingDeletionKey(), current)?.apply()
    }

    fun getPendingCloudDeletions(): List<PendingCloudDeletion> {
        return prefs?.getStringSet(pendingDeletionKey(), emptySet()).orEmpty()
            .mapNotNull { raw ->
                val parts = raw.split("|", limit = 2)
                if (parts.size == 2 && parts[1].isNotBlank()) {
                    PendingCloudDeletion(parts[0], parts[1])
                } else null
            }
    }

    fun clearCloudDeletions(deletions: Collection<PendingCloudDeletion>) {
        if (deletions.isEmpty()) return
        val removeKeys = deletions.map { deletionKey(it.collection, it.syncId) }.toSet()
        val current = prefs?.getStringSet(pendingDeletionKey(), emptySet()).orEmpty()
        prefs?.edit()?.putStringSet(
            pendingDeletionKey(),
            current.filterNot { it in removeKeys }.toSet()
        )?.apply()
    }

    fun getApplicationContext(): android.content.Context? = context?.applicationContext

    fun getSavedActiveWorkshopId(): Long {
        return prefs?.getLong("active_workshop_id", -1L) ?: -1L
    }

    fun getLocalAccountUid(): String? =
        prefs?.getString("local_account_uid", null)

    fun saveLocalAccountUid(uid: String) {
        prefs?.edit()?.putString("local_account_uid", uid)?.apply()
    }

    fun clearLocalAccountUid() {
        prefs?.edit()?.remove("local_account_uid")?.apply()
    }

    fun saveActiveWorkshopId(id: Long) {
        prefs?.edit()?.putLong("active_workshop_id", id)?.apply()
    }

    fun getCustomUsername(): String {
        return prefs?.getString("custom_username", "") ?: ""
    }

    fun saveCustomUsername(username: String) {
        prefs?.edit()?.putString("custom_username", username.trim())?.apply()
    }

    fun getSavedThemeMode(): String? = prefs?.getString("theme_mode", null)
    fun saveThemeMode(mode: String) {
        prefs?.edit()?.putString("theme_mode", mode)?.apply()
    }

    fun getSavedCurrencyUnit(): String = prefs?.getString("currency_unit", "تومان") ?: "تومان"
    fun saveCurrencyUnit(value: String) { prefs?.edit()?.putString("currency_unit", value)?.apply() }

    fun getSavedCalendarType(): String = prefs?.getString("calendar_type", CalendarType.JALALI.name) ?: CalendarType.JALALI.name
    fun saveCalendarType(value: String) { prefs?.edit()?.putString("calendar_type", value)?.apply() }

    fun getSavedCardDisplayMode(): String =
        prefs?.getString("card_display_mode", com.example.model.CardDisplayMode.UNIFIED.name)
            ?: com.example.model.CardDisplayMode.UNIFIED.name
    fun saveCardDisplayMode(value: String) { prefs?.edit()?.putString("card_display_mode", value)?.apply() }

    fun getSavedCardSortOrder(): String =
        prefs?.getString("card_sort_order", com.example.model.CardSortOrder.NEWEST_BOTTOM.name)
            ?: com.example.model.CardSortOrder.NEWEST_BOTTOM.name
    fun saveCardSortOrder(value: String) { prefs?.edit()?.putString("card_sort_order", value)?.apply() }

    val orders: Flow<List<FurnitureOrder>> = orderDao.getAllOrders()
    val payments: Flow<List<PaymentRecord>> = paymentDao.getAllPayments()
    val modelPresets: Flow<List<ModelPreset>> = modelPresetDao.getAllPresets()
    val unitRules: Flow<List<UnitConversionRule>> = unitRuleDao.getAllRules()
    val workshops: Flow<List<Workshop>> = workshopDao.getAllWorkshops()

    fun getOrdersFlow(workshopId: Long): Flow<List<FurnitureOrder>> =
        orderDao.getOrdersByWorkshop(workshopId)

    fun getPaymentsFlow(workshopId: Long): Flow<List<PaymentRecord>> =
        paymentDao.getPaymentsByWorkshop(workshopId)

    fun getPresetsFlow(workshopId: Long): Flow<List<ModelPreset>> =
        modelPresetDao.getPresetsByWorkshop(workshopId)

    suspend fun getAllWorkshopsSync(): List<Workshop> =
        workshopDao.getAllWorkshopsSync()

    suspend fun getWorkshopById(id: Long): Workshop? =
        workshopDao.getWorkshopById(id)

    suspend fun ensureDefaultWorkshop(): Long {
        val existing = workshopDao.getAllWorkshopsSync()
        if (existing.isNotEmpty()) {
            return existing.first().id
        }
        val defaultWorkshop = Workshop(
            id = 1L,
            name = "کارگاه اصلی",
            createdAt = System.currentTimeMillis()
        )
        return workshopDao.insertWorkshop(defaultWorkshop)
    }

    suspend fun deduplicateWorkshops() {
        val all = workshopDao.getAllWorkshopsSync()
        if (all.size <= 1) return

        val seenNames = mutableMapOf<String, Long>()
        val defaultWorkshops = mutableListOf<Workshop>()
        val toDelete = mutableListOf<Workshop>()

        for (ws in all) {
            val normName = ws.name.trim().lowercase(java.util.Locale.ROOT)
            val isDefaultName = normName == "کارگاه اصلی" || normName == "کارگاه"

            if (isDefaultName) {
                defaultWorkshops.add(ws)
            } else {
                val existingMasterId = seenNames[normName]
                if (existingMasterId != null) {
                    toDelete.add(ws)
                } else {
                    seenNames[normName] = ws.id
                }
            }
        }

        // Ensure default workshops NEVER exist more than one:
        if (defaultWorkshops.size > 1) {
            for (i in 1 until defaultWorkshops.size) {
                toDelete.add(defaultWorkshops[i])
            }
        }

        for (dup in toDelete) {
            val normName = dup.name.trim().lowercase(java.util.Locale.ROOT)
            val targetId = if (normName in listOf("کارگاه اصلی", "کارگاه")) {
                defaultWorkshops.first().id
            } else {
                seenNames[normName] ?: all.first().id
            }

            if (dup.id != targetId) {
                val dupOrders = orderDao.getOrdersByWorkshopSync(dup.id)
                for (ord in dupOrders) {
                    orderDao.updateOrder(ord.copy(workshopId = targetId))
                }
                val dupPayments = paymentDao.getPaymentsByWorkshopSync(dup.id)
                for (pay in dupPayments) {
                    paymentDao.updatePayment(pay.copy(workshopId = targetId))
                }
                val dupPresets = modelPresetDao.getPresetsByWorkshopSync(dup.id)
                for (pre in dupPresets) {
                    modelPresetDao.updatePreset(pre.copy(workshopId = targetId))
                }

                if (getLocalAccountUid() != null) recordCloudDeletion("workshops", dup.syncId)
                workshopDao.deleteWorkshopById(dup.id)
            }
        }
    }

    suspend fun saveWorkshop(workshop: Workshop): Long {
        val trimmed = workshop.name.trim()
        val toSave = workshop.copy(name = if (trimmed.isBlank()) "کارگاه جدید" else trimmed)
        return if (toSave.id == 0L) {
            workshopDao.insertWorkshop(toSave)
        } else {
            workshopDao.updateWorkshop(toSave)
            toSave.id
        }
    }

    suspend fun deleteWorkshopAndAllData(workshopId: Long) {
        val workshop = workshopDao.getWorkshopById(workshopId)
        val orders = orderDao.getOrdersByWorkshopSync(workshopId)
        val payments = paymentDao.getPaymentsByWorkshopSync(workshopId)
        val presets = modelPresetDao.getPresetsByWorkshopSync(workshopId)

        database.withTransaction {
            workshopDao.deleteOrdersByWorkshop(workshopId)
            workshopDao.deletePaymentsByWorkshop(workshopId)
            workshopDao.deletePresetsByWorkshop(workshopId)
            workshopDao.deleteWorkshopById(workshopId)
        }

        workshop?.syncId?.let { recordCloudDeletion("workshops", it) }
        orders.forEach { recordCloudDeletion("orders", it.syncId) }
        payments.forEach { recordCloudDeletion("payments", it.syncId) }
        presets.forEach { recordCloudDeletion("presets", it.syncId) }
    }

    suspend fun clearAllDomainData() {
        database.withTransaction {
            orderDao.clearAll()
            paymentDao.clearAll()
            modelPresetDao.clearAll()
            unitRuleDao.clearAll()
            workshopDao.clearAll()
        }
        saveActiveWorkshopId(0L)
    }

    suspend fun applyCloudDeletions(deletions: List<PendingCloudDeletion>) {
        if (deletions.isEmpty()) return
        database.withTransaction {
            for (deletion in deletions) {
                when (deletion.collection) {
                    "orders" -> orderDao.deleteOrderBySyncId(deletion.syncId)
                    "payments" -> paymentDao.deletePaymentBySyncId(deletion.syncId)
                    "presets" -> modelPresetDao.deletePresetBySyncId(deletion.syncId)
                    "unitRules" -> unitRuleDao.getRuleBySyncId(deletion.syncId)?.let {
                        unitRuleDao.deleteRuleById(it.id)
                    }
                    "workshops" -> {
                        workshopDao.getWorkshopBySyncId(deletion.syncId)?.let { workshop ->
                            workshopDao.deleteOrdersByWorkshop(workshop.id)
                            workshopDao.deletePaymentsByWorkshop(workshop.id)
                            workshopDao.deletePresetsByWorkshop(workshop.id)
                            workshopDao.deleteWorkshopById(workshop.id)
                        }
                    }
                }
            }
        }
    }

    suspend fun mergeCloudData(
        cloudWorkshops: List<Workshop>,
        cloudOrders: List<FurnitureOrder>,
        cloudPayments: List<PaymentRecord>,
        cloudPresets: List<ModelPreset>,
        cloudUnitRules: List<UnitConversionRule>
    ) {
        database.withTransaction {
            val workshopMap = mutableMapOf<String, Long>()
            // Legacy cloud records may have only the old numeric workshopId.
            // Keep a second map so their child records are remapped to the
            // actual local Room workshop id after multi-device merge.
            val legacyWorkshopIdMap = mutableMapOf<Long, Long>()

            for (remote in cloudWorkshops) {
                val existing = workshopDao.getWorkshopBySyncId(remote.syncId)
                val localId = if (existing == null) {
                    workshopDao.insertWorkshop(remote.copy(id = 0L))
                } else {
                    workshopDao.updateWorkshop(remote.copy(id = existing.id))
                    existing.id
                }
                workshopMap[remote.syncId] = localId
                legacyWorkshopIdMap[remote.id] = localId
            }

            val orderMap = mutableMapOf<String, Long>()
            val legacyOrderIdMap = mutableMapOf<Long, Long>()
            for (remote in cloudOrders) {
                val localWorkshopId = workshopMap[remote.workshopSyncId]
                    ?: legacyWorkshopIdMap[remote.workshopId]
                    ?: remote.workshopId
                val existing = orderDao.getOrderBySyncId(remote.syncId)
                val value = remote.copy(
                    id = existing?.id ?: 0L,
                    workshopId = localWorkshopId
                )
                val localId = if (existing == null) {
                    orderDao.insertOrder(value)
                } else {
                    orderDao.updateOrder(value)
                    existing.id
                }
                orderMap[remote.syncId] = localId
                legacyOrderIdMap[remote.id] = localId
            }

            for (remote in cloudPayments) {
                val localWorkshopId = workshopMap[remote.workshopSyncId]
                    ?: legacyWorkshopIdMap[remote.workshopId]
                    ?: remote.workshopId
                val localRelatedOrderId =
                    remote.relatedOrderSyncId.takeIf { it.isNotBlank() }?.let { orderMap[it] }
                        ?: remote.relatedOrderId?.let { legacyOrderIdMap[it] }
                val existing = paymentDao.getPaymentBySyncId(remote.syncId)
                val value = remote.copy(
                    id = existing?.id ?: 0L,
                    workshopId = localWorkshopId,
                    relatedOrderId = localRelatedOrderId
                )
                if (existing == null) paymentDao.insertPayment(value)
                else paymentDao.updatePayment(value)
            }

            for (remote in cloudPresets) {
                val localWorkshopId = workshopMap[remote.workshopSyncId]
                    ?: legacyWorkshopIdMap[remote.workshopId]
                    ?: remote.workshopId
                val existing = modelPresetDao.getPresetBySyncId(remote.syncId)
                val value = remote.copy(
                    id = existing?.id ?: 0L,
                    workshopId = localWorkshopId
                )
                if (existing == null) modelPresetDao.insertPreset(value)
                else modelPresetDao.updatePreset(value)
            }

            for (remote in cloudUnitRules) {
                val existing = unitRuleDao.getRuleBySyncId(remote.syncId)
                val value = remote.copy(id = existing?.id ?: 0L)
                if (existing == null) unitRuleDao.insertRule(value)
                else unitRuleDao.updateRule(value)
            }
        }

        val savedId = getSavedActiveWorkshopId()
        if (savedId <= 0L) {
            saveActiveWorkshopId(workshopDao.getAllWorkshopsSync().firstOrNull()?.id ?: 0L)
        }
    }

    suspend fun replaceAllData(
        workshops: List<Workshop>,
        orders: List<FurnitureOrder>,
        payments: List<PaymentRecord>,
        presets: List<ModelPreset>,
        unitRules: List<UnitConversionRule>
    ) {
        database.withTransaction {
            orderDao.clearAll()
            paymentDao.clearAll()
            modelPresetDao.clearAll()
            unitRuleDao.clearAll()
            workshopDao.clearAll()

            if (workshops.isNotEmpty()) workshopDao.insertAll(workshops)
            if (orders.isNotEmpty()) orderDao.insertAll(orders)
            if (payments.isNotEmpty()) paymentDao.insertAll(payments)
            if (presets.isNotEmpty()) modelPresetDao.insertAll(presets)
            if (unitRules.isNotEmpty()) unitRuleDao.insertAll(unitRules)
        }
        val savedId = getSavedActiveWorkshopId()
        if (savedId <= 0L || workshops.none { it.id == savedId }) {
            saveActiveWorkshopId(workshops.firstOrNull()?.id ?: 0L)
        }
    }

    suspend fun updateOrdersColorForModel(modelName: String, newColor: String, workshopId: Long) {
        orderDao.updateModelColor(modelName, newColor, workshopId)
        modelPresetDao.updatePresetColor(modelName, newColor, workshopId)
    }

    suspend fun saveOrder(order: FurnitureOrder) {
        val workshopSyncId = order.workshopSyncId.ifBlank {
            workshopDao.getWorkshopById(order.workshopId)?.syncId.orEmpty()
        }
        val normalizedOrder = order.copy(workshopSyncId = workshopSyncId)
        if (normalizedOrder.id == 0L) {
            val existing = orderDao.getOrdersByWorkshopSync(normalizedOrder.workshopId)
            val normInv = com.example.util.PersianUtils.toEnglishDigits(normalizedOrder.invoiceNumber.trim()).lowercase(java.util.Locale.ROOT)
            val normCust = normalizedOrder.customerName.trim().lowercase(java.util.Locale.ROOT)
            val normModel = normalizedOrder.modelName.trim().lowercase(java.util.Locale.ROOT)
            val normDate = com.example.util.PersianUtils.toEnglishDigits(normalizedOrder.dateJalali.trim())

            val match = existing.find { ex ->
                val exInv = com.example.util.PersianUtils.toEnglishDigits(ex.invoiceNumber.trim()).lowercase(java.util.Locale.ROOT)
                val exCust = ex.customerName.trim().lowercase(java.util.Locale.ROOT)
                val exModel = ex.modelName.trim().lowercase(java.util.Locale.ROOT)
                val exDate = com.example.util.PersianUtils.toEnglishDigits(ex.dateJalali.trim())

                (normInv.isNotBlank() && normInv != "0" && exInv == normInv) ||
                (ex.orderNumber == normalizedOrder.orderNumber && (exCust == normCust || ex.createdAt == normalizedOrder.createdAt)) ||
                (normCust.isNotBlank() && exCust == normCust && exModel == normModel && ex.calculatedTotal == normalizedOrder.calculatedTotal && exDate == normDate)
            }

            if (match != null) {
                orderDao.updateOrder(normalizedOrder.copy(id = match.id, syncId = match.syncId))
            } else {
                orderDao.insertOrder(normalizedOrder)
            }
        } else {
            orderDao.updateOrder(normalizedOrder)
        }
    }

    suspend fun deleteOrder(order: FurnitureOrder) {
        orderDao.deleteOrder(order)
    }

    suspend fun deleteOrderById(id: Long) {
        val existing = orderDao.getOrderById(id)
        orderDao.deleteOrderById(id)
        existing?.syncId?.let { recordCloudDeletion("orders", it) }
    }

    suspend fun savePayment(payment: PaymentRecord) {
        val workshopSyncId = payment.workshopSyncId.ifBlank {
            workshopDao.getWorkshopById(payment.workshopId)?.syncId.orEmpty()
        }
        val relatedOrderSyncId = payment.relatedOrderSyncId.ifBlank {
            payment.relatedOrderId?.let { orderDao.getAllOrdersSync().firstOrNull { o -> o.id == it }?.syncId }.orEmpty()
        }
        val normalizedPayment = payment.copy(
            workshopSyncId = workshopSyncId,
            relatedOrderSyncId = relatedOrderSyncId
        )
        if (normalizedPayment.id == 0L) {
            val existing = paymentDao.getPaymentsByWorkshopSync(normalizedPayment.workshopId)
            val normRef = com.example.util.PersianUtils.toEnglishDigits(normalizedPayment.referenceNo.trim()).lowercase(java.util.Locale.ROOT)
            val normCust = normalizedPayment.customerName.trim().lowercase(java.util.Locale.ROOT)
            val normDate = com.example.util.PersianUtils.toEnglishDigits(normalizedPayment.dateJalali.trim())

            val match = existing.find { ex ->
                val exRef = com.example.util.PersianUtils.toEnglishDigits(ex.referenceNo.trim()).lowercase(java.util.Locale.ROOT)
                val exCust = ex.customerName.trim().lowercase(java.util.Locale.ROOT)
                val exDate = com.example.util.PersianUtils.toEnglishDigits(ex.dateJalali.trim())

                (normRef.isNotBlank() && exRef == normRef) ||
                (ex.paymentNumber == normalizedPayment.paymentNumber && (exCust == normCust || ex.createdAt == normalizedPayment.createdAt)) ||
                (normCust.isNotBlank() && exCust == normCust && ex.amount == normalizedPayment.amount && exDate == normDate)
            }

            if (match != null) {
                paymentDao.updatePayment(normalizedPayment.copy(id = match.id, syncId = match.syncId))
            } else {
                paymentDao.insertPayment(normalizedPayment)
            }
        } else {
            paymentDao.updatePayment(normalizedPayment)
        }
    }

    suspend fun deletePayment(payment: PaymentRecord) {
        paymentDao.deletePayment(payment)
    }

    suspend fun deletePaymentById(id: Long) {
        val existing = paymentDao.getPaymentById(id)
        paymentDao.deletePaymentById(id)
        existing?.syncId?.let { recordCloudDeletion("payments", it) }
    }

    suspend fun getAllOrdersSync(): List<FurnitureOrder> = orderDao.getAllOrdersSync()
    suspend fun getOrdersByWorkshopSync(workshopId: Long): List<FurnitureOrder> =
        orderDao.getOrdersByWorkshopSync(workshopId)

    suspend fun getAllPaymentsSync(): List<PaymentRecord> = paymentDao.getAllPaymentsSync()
    suspend fun getPaymentsByWorkshopSync(workshopId: Long): List<PaymentRecord> =
        paymentDao.getPaymentsByWorkshopSync(workshopId)

    suspend fun getAllPresetsSync(): List<ModelPreset> = modelPresetDao.getAllPresetsSync()
    suspend fun getAllUnitRulesSync(): List<UnitConversionRule> = unitRuleDao.getAllRulesSync()
    suspend fun getPresetsByWorkshopSync(workshopId: Long): List<ModelPreset> =
        modelPresetDao.getPresetsByWorkshopSync(workshopId)

    suspend fun getPresetByName(name: String): ModelPreset? = modelPresetDao.getPresetByName(name.trim())
    suspend fun getPresetByNameAndWorkshop(name: String, workshopId: Long): ModelPreset? =
        modelPresetDao.getPresetByNameAndWorkshop(name.trim(), workshopId)

    suspend fun deduplicatePresets() {
        val all = modelPresetDao.getAllPresetsSync()
        val seen = mutableSetOf<String>()
        for (preset in all) {
            val key = "${preset.workshopId}_${preset.name.trim().lowercase(java.util.Locale.ROOT)}"
            if (preset.name.trim().isBlank()) {
                if (getLocalAccountUid() != null) recordCloudDeletion("presets", preset.syncId)
                modelPresetDao.deletePreset(preset)
            } else if (key in seen) {
                if (getLocalAccountUid() != null) recordCloudDeletion("presets", preset.syncId)
                modelPresetDao.deletePreset(preset)
            } else {
                seen.add(key)
            }
        }
    }

    suspend fun deduplicateOrders() {
        val all = orderDao.getAllOrdersSync()
        val seenExact = mutableSetOf<String>()
        val seenRapidSubmits = mutableMapOf<String, Long>()
        val toDelete = mutableListOf<FurnitureOrder>()

        for (ord in all) {
            val wsId = ord.workshopId
            val normInv = com.example.util.PersianUtils.toEnglishDigits(ord.invoiceNumber.trim()).lowercase(java.util.Locale.ROOT)
            val normCust = ord.customerName.trim().lowercase(java.util.Locale.ROOT)
            val normModel = ord.modelName.trim().lowercase(java.util.Locale.ROOT)
            val normTotal = ord.calculatedTotal
            val normDate = com.example.util.PersianUtils.toEnglishDigits(ord.dateJalali.trim())

            // Exact identical identity key: same workshop, order number, and invoice number
            val exactKey = "${wsId}_${ord.orderNumber}_$normInv"
            val rapidKey = "${wsId}_${normCust}_${normModel}_${normTotal}_$normDate"

            val lastRapidTime = seenRapidSubmits[rapidKey]
            val isRapidDuplicate = lastRapidTime != null && Math.abs(ord.createdAt - lastRapidTime) < 10000L
            val isExactDuplicate = exactKey in seenExact

            if (isExactDuplicate || (normCust.isNotBlank() && isRapidDuplicate)) {
                toDelete.add(ord)
            } else {
                seenExact.add(exactKey)
                if (normCust.isNotBlank()) seenRapidSubmits[rapidKey] = ord.createdAt
            }
        }
        for (ord in toDelete) {
            if (getLocalAccountUid() != null) recordCloudDeletion("orders", ord.syncId)
            orderDao.deleteOrder(ord)
        }
    }

    suspend fun deduplicatePayments() {
        val all = paymentDao.getAllPaymentsSync()
        val seenExact = mutableSetOf<String>()
        val seenRapidSubmits = mutableMapOf<String, Long>()
        val toDelete = mutableListOf<PaymentRecord>()

        for (pay in all) {
            val wsId = pay.workshopId
            val normRef = com.example.util.PersianUtils.toEnglishDigits(pay.referenceNo.trim()).lowercase(java.util.Locale.ROOT)
            val normCust = pay.customerName.trim().lowercase(java.util.Locale.ROOT)
            val normAmt = pay.amount
            val normDate = com.example.util.PersianUtils.toEnglishDigits(pay.dateJalali.trim())

            val exactKey = "${wsId}_${pay.paymentNumber}_$normRef"
            val rapidKey = "${wsId}_${normCust}_${normAmt}_$normDate"

            val lastRapidTime = seenRapidSubmits[rapidKey]
            val isRapidDuplicate = lastRapidTime != null && Math.abs(pay.createdAt - lastRapidTime) < 10000L
            val isExactDuplicate = exactKey in seenExact

            if (isExactDuplicate || (normCust.isNotBlank() && isRapidDuplicate)) {
                toDelete.add(pay)
            } else {
                seenExact.add(exactKey)
                if (normCust.isNotBlank()) seenRapidSubmits[rapidKey] = pay.createdAt
            }
        }
        for (pay in toDelete) {
            if (getLocalAccountUid() != null) recordCloudDeletion("payments", pay.syncId)
            paymentDao.deletePayment(pay)
        }
    }

    suspend fun savePreset(preset: ModelPreset) {
        val workshopSyncId = preset.workshopSyncId.ifBlank {
            workshopDao.getWorkshopById(preset.workshopId)?.syncId.orEmpty()
        }
        val normalizedPreset = preset.copy(workshopSyncId = workshopSyncId)
        val trimmed = preset.name.trim()
        if (trimmed.isBlank()) return
        val existing = modelPresetDao.getPresetByNameAndWorkshop(trimmed, normalizedPreset.workshopId)
        if (existing != null) {
            modelPresetDao.updatePreset(
                normalizedPreset.copy(id = existing.id, name = trimmed)
            )
        } else {
            if (preset.id == 0L) {
                modelPresetDao.insertPreset(normalizedPreset.copy(name = trimmed))
            } else {
                modelPresetDao.updatePreset(normalizedPreset.copy(name = trimmed))
            }
        }
    }

    suspend fun deletePreset(preset: ModelPreset) {
        modelPresetDao.deletePreset(preset)
        recordCloudDeletion("presets", preset.syncId)
    }

    suspend fun deletePresetById(id: Long) {
        val existing = modelPresetDao.getPresetById(id)
        modelPresetDao.deletePresetById(id)
        existing?.syncId?.let { recordCloudDeletion("presets", it) }
    }

    suspend fun deletePresetByName(name: String) {
        modelPresetDao.deletePresetByName(name)
    }

    suspend fun deletePresetByNameAndWorkshop(name: String, workshopId: Long) {
        val existing = modelPresetDao.getPresetByNameAndWorkshop(name, workshopId)
        modelPresetDao.deletePresetByNameAndWorkshop(name, workshopId)
        existing?.syncId?.let { recordCloudDeletion("presets", it) }
    }

    suspend fun saveUnitRule(rule: UnitConversionRule) {
        val rawKey = rule.pieceKey.ifBlank {
            if (rule.pieceCount % 1.0 == 0.0) rule.pieceCount.toInt().toString() else rule.pieceCount.toString()
        }
        val norm = normalizeUnitKey(rawKey)
        val existing = unitRuleDao.getAllRulesSync()
        val duplicate = existing.find {
            it.id != rule.id && normalizeUnitKey(it.pieceKey.ifBlank { if (it.pieceCount % 1.0 == 0.0) it.pieceCount.toInt().toString() else it.pieceCount.toString() }) == norm
        }
        if (duplicate != null) {
            // Update the existing rule to maintain single rule per piece count/title
            unitRuleDao.updateRule(
                duplicate.copy(
                    pieceKey = rule.pieceKey,
                    pieceCount = rule.pieceCount,
                    calculatedUnits = rule.calculatedUnits,
                    isEnabled = rule.isEnabled
                )
            )
        } else {
            if (rule.id == 0L) {
                unitRuleDao.insertRule(rule)
            } else {
                unitRuleDao.updateRule(rule)
            }
        }
    }

    suspend fun deleteUnitRule(rule: UnitConversionRule) {
        markDefaultUnitRuleDeleted(rule)
        unitRuleDao.deleteRule(rule)
        recordCloudDeletion("unitRules", rule.syncId)
    }

    suspend fun deleteUnitRuleById(id: Long) {
        val existing = unitRuleDao.getAllRulesSync().firstOrNull { it.id == id }
        existing?.let { markDefaultUnitRuleDeleted(it) }
        unitRuleDao.deleteRuleById(id)
        existing?.syncId?.let { recordCloudDeletion("unitRules", it) }
    }

    private data class DefaultUnitRule(val key: String, val calculatedUnits: Double)

    private val defaultUnitRules = listOf(
        DefaultUnitRule("3", 2.0),
        DefaultUnitRule("2", 1.5),
        DefaultUnitRule("1", 1.0),
        DefaultUnitRule("0.5", 0.5)
    )

    private fun deletedDefaultUnitRules(): MutableSet<String> =
        prefs?.getStringSet("deleted_default_unit_rules", emptySet()).orEmpty().toMutableSet()

    private fun markDefaultUnitRuleDeleted(rule: UnitConversionRule) {
        val raw = rule.pieceKey.ifBlank {
            if (rule.pieceCount % 1.0 == 0.0) rule.pieceCount.toInt().toString() else rule.pieceCount.toString()
        }
        val key = normalizeUnitKey(raw)
        if (defaultUnitRules.any { it.key == key }) {
            val deleted = deletedDefaultUnitRules()
            deleted += key
            prefs?.edit()?.putStringSet("deleted_default_unit_rules", deleted)?.apply()
        }
    }

    suspend fun insertDefaultUnitRulesIfEmpty() {
        val existing = unitRuleDao.getAllRulesSync()
        val deletedDefaults = deletedDefaultUnitRules()

        for (defaultRule in defaultUnitRules) {
            if (defaultRule.key in deletedDefaults) continue
            val exists = existing.any { rule ->
                val raw = rule.pieceKey.ifBlank {
                    if (rule.pieceCount % 1.0 == 0.0) rule.pieceCount.toInt().toString() else rule.pieceCount.toString()
                }
                normalizeUnitKey(raw) == defaultRule.key
            }
            if (!exists) {
                unitRuleDao.insertRule(
                    UnitConversionRule(
                        pieceKey = defaultRule.key,
                        pieceCount = defaultRule.key.toDouble(),
                        calculatedUnits = defaultRule.calculatedUnits,
                        isEnabled = true
                    )
                )
            }
        }

        val all = unitRuleDao.getAllRulesSync()
        val seen = mutableSetOf<String>()
        for (r in all) {
            val rawKey = r.pieceKey.ifBlank {
                if (r.pieceCount % 1.0 == 0.0) r.pieceCount.toInt().toString() else r.pieceCount.toString()
            }
            val norm = normalizeUnitKey(rawKey)
            if (norm in seen) unitRuleDao.deleteRuleById(r.id) else seen.add(norm)
        }
    }

    suspend fun restoreDefaultUnitRules() {
        prefs?.edit()?.remove("deleted_default_unit_rules")?.apply()
        unitRuleDao.clearAll()
        unitRuleDao.insertAll(defaultUnitRules.map {
            UnitConversionRule(
                pieceKey = it.key,
                pieceCount = it.key.toDouble(),
                calculatedUnits = it.calculatedUnits,
                isEnabled = true
            )
        })
    }

    companion object {
        fun normalizeUnitKey(raw: String): String {
            val eng = PersianUtils.toEnglishDigits(raw.trim().lowercase(java.util.Locale.ROOT))
                .replace("/", ".")
                .replace(",", "")
            val num = eng.toDoubleOrNull()
            return if (num != null && num > 0.0) {
                if (num % 1.0 == 0.0) num.toLong().toString() else num.toString()
            } else {
                eng
            }
        }
    }

    suspend fun seedInitialDataIfEmpty() {
        // App starts clean with zero sample data as requested
    }

    suspend fun resetAllData() {
        clearAllDomainData()
    }
}
