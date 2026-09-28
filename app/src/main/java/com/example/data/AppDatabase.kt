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
import androidx.room.TypeConverters
import com.example.data.sync.DeletedIdDao
import com.example.data.sync.DocumentCacheDao
import com.example.data.sync.SyncStatusConverters

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Existing releases marked legacy local rows as SYNCED even when they
        // had never been uploaded. Re-queue them once so the new LWW engine can
        // reconcile them safely with the cloud instead of silently skipping them.
        val tables = listOf(
            "workshops",
            "furniture_orders",
            "payment_records",
            "model_presets",
            "unit_conversion_rules"
        )
        for (table in tables) {
            db.execSQL("UPDATE " + table + " SET syncStatus = 'PENDING' WHERE syncId IS NOT NULL AND syncId != ''")
            db.execSQL("UPDATE " + table + " SET updatedAt = createdAt WHERE updatedAt <= 0")
        }
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE deleted_ids ADD COLUMN cloudSynced INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE upload_queue ADD COLUMN collection TEXT NOT NULL DEFAULT ''")
    }
}
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val tables = listOf("workshops", "furniture_orders", "payment_records", "model_presets", "unit_conversion_rules")
        for (table in tables) {
            db.execSQL("ALTER TABLE $table ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE $table ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
            db.execSQL("ALTER TABLE $table ADD COLUMN fileUrl TEXT")
            db.execSQL("ALTER TABLE $table ADD COLUMN storagePath TEXT")
        }
    }
}
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS documents_cache (collection TEXT NOT NULL, documentId TEXT NOT NULL, updatedAt INTEGER NOT NULL DEFAULT 0, fromCache INTEGER NOT NULL DEFAULT 0, hasPendingWrites INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(collection, documentId))")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_documents_cache_documentId ON documents_cache(documentId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_documents_cache_updatedAt ON documents_cache(updatedAt)")
        db.execSQL("CREATE TABLE IF NOT EXISTS deleted_ids (collection TEXT NOT NULL, documentId TEXT NOT NULL, deletedAt INTEGER NOT NULL, PRIMARY KEY(collection, documentId))")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_deleted_ids_deletedAt ON deleted_ids(deletedAt)")
        db.execSQL("CREATE TABLE IF NOT EXISTS upload_queue (id TEXT NOT NULL PRIMARY KEY, documentId TEXT NOT NULL, localFilePath TEXT NOT NULL, storagePath TEXT NOT NULL, status TEXT NOT NULL, retryCount INTEGER NOT NULL, createdAt INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_upload_queue_status ON upload_queue(status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_upload_queue_createdAt ON upload_queue(createdAt)")
        db.execSQL("CREATE TABLE IF NOT EXISTS pending_deletes (id TEXT NOT NULL PRIMARY KEY, collection TEXT NOT NULL, documentId TEXT NOT NULL, storagePath TEXT, status TEXT NOT NULL, retryCount INTEGER NOT NULL, createdAt INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_pending_deletes_status ON pending_deletes(status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_pending_deletes_createdAt ON pending_deletes(createdAt)")
    }
}

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
    entities = [FurnitureOrder::class, PaymentRecord::class, ModelPreset::class, UnitConversionRule::class, Workshop::class, com.example.data.sync.DocumentCacheEntity::class, com.example.data.sync.DeletedIdEntity::class, com.example.data.sync.UploadQueueEntity::class, com.example.data.sync.PendingDeleteEntity::class],
    version = 12,
    exportSchema = false
)
@TypeConverters(SyncStatusConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao
    abstract fun paymentDao(): PaymentDao
    abstract fun modelPresetDao(): ModelPresetDao
    abstract fun unitRuleDao(): UnitRuleDao
    abstract fun workshopDao(): WorkshopDao
    abstract fun deletedIdDao(): DeletedIdDao
    abstract fun documentCacheDao(): DocumentCacheDao

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
                     .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
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

    private suspend fun migrateLegacyDeletionQueue() {
        val current = prefs?.getStringSet(pendingDeletionKey(), emptySet()).orEmpty()
        if (current.isEmpty()) return
        val now = System.currentTimeMillis()
        for (raw in current) {
            val parts = raw.split("|", limit = 2)
            if (parts.size == 2 && parts[1].isNotBlank()) {
                database.deletedIdDao().upsert(
                    com.example.data.sync.DeletedIdEntity(
                        collection = parts[0],
                        documentId = parts[1],
                        deletedAt = now,
                        cloudSynced = false
                    )
                )
            }
        }
        prefs?.edit()?.remove(pendingDeletionKey())?.apply()
    }

    suspend fun recordCloudDeletion(collection: String, syncId: String) {
        if (syncId.isBlank()) return
        database.deletedIdDao().upsert(
            com.example.data.sync.DeletedIdEntity(
                collection = collection,
                documentId = syncId,
                deletedAt = System.currentTimeMillis(),
                cloudSynced = false
            )
        )
    }

    suspend fun markCloudDeletionConfirmed(collection: String, syncId: String) {
        if (syncId.isBlank()) return
        database.deletedIdDao().markCloudSynced(collection, syncId)
    }

    suspend fun getPendingCloudDeletions(): List<PendingCloudDeletion> {
        migrateLegacyDeletionQueue()
        return database.deletedIdDao().getPending().map {
            PendingCloudDeletion(it.collection, it.documentId)
        }
    }

    suspend fun clearCloudDeletions(deletions: Collection<PendingCloudDeletion>) {
        if (deletions.isEmpty()) return
        for (deletion in deletions) {
            database.deletedIdDao().markCloudSynced(deletion.collection, deletion.syncId)
        }
        prefs?.edit()?.remove(pendingDeletionKey())?.apply()
    }

    suspend fun clearSyncState() {
        database.withTransaction {
            database.deletedIdDao().clearAll()
            database.uploadQueueDao().clearAll()
            database.pendingDeleteDao().clearAll()
            database.documentCacheDao().clearAll()
        }
        prefs?.edit()
            ?.remove(pendingDeletionKey())
            ?.remove("cloud_sync_ready_uid")
            ?.apply()
    }

    suspend fun hasPendingSyncWork(): Boolean {
        val pendingRecords =
            workshopDao.getPendingSync().isNotEmpty() ||
                orderDao.getPendingSync().isNotEmpty() ||
                paymentDao.getPendingSync().isNotEmpty() ||
                modelPresetDao.getPendingSync().isNotEmpty() ||
                unitRuleDao.getPendingSync().isNotEmpty()
        return pendingRecords ||
            database.deletedIdDao().getPending().isNotEmpty() ||

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
        if (trimmed.isBlank()) return workshop.id

        val syncId = workshop.syncId.ifBlank { java.util.UUID.randomUUID().toString() }
        val now = System.currentTimeMillis()
        val toSave = workshop.copy(
            name = trimmed,
            syncId = syncId,
            updatedAt = now,
            syncStatus = com.example.data.sync.RecordSyncStatus.PENDING
        )

        val existingBySync = workshopDao.getWorkshopBySyncId(syncId)
        return when {
            existingBySync != null -> {
                workshopDao.updateWorkshop(toSave.copy(id = existingBySync.id))
                existingBySync.id
            }
            toSave.id == 0L -> workshopDao.insertWorkshop(toSave)
            else -> {
                workshopDao.updateWorkshop(toSave)
                toSave.id
            }
        }
    }

    suspend fun deleteWorkshopAndAllData(workshopId: Long) {
        database.withTransaction {
            val workshop = workshopDao.getWorkshopById(workshopId)
            val orders = orderDao.getOrdersByWorkshopSync(workshopId)
            val payments = paymentDao.getPaymentsByWorkshopSync(workshopId)
            val presets = modelPresetDao.getPresetsByWorkshopSync(workshopId)

            workshopDao.deleteOrdersByWorkshop(workshopId)
            workshopDao.deletePaymentsByWorkshop(workshopId)
            workshopDao.deletePresetsByWorkshop(workshopId)
            workshopDao.deleteWorkshopById(workshopId)

            workshop?.syncId?.let { recordCloudDeletion("workshops", it) }
            orders.forEach { recordCloudDeletion("orders", it.syncId) }
            payments.forEach { recordCloudDeletion("payments", it.syncId) }
            presets.forEach { recordCloudDeletion("presets", it.syncId) }
        }
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

    suspend fun clearAccountLocalState() {
        database.withTransaction {
            orderDao.clearAll()
            paymentDao.clearAll()
            modelPresetDao.clearAll()
            unitRuleDao.clearAll()
            workshopDao.clearAll()
            database.deletedIdDao().clearAll()
            database.uploadQueueDao().clearAll()
            database.pendingDeleteDao().clearAll()
            database.documentCacheDao().clearAll()
        }
        prefs?.edit()
            ?.remove(pendingDeletionKey())
            ?.remove("local_account_uid")
            ?.remove("cloud_sync_ready_uid")
            ?.putLong("active_workshop_id", 0L)
            ?.apply()
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

                // A remote delete is itself a durable fact. Keep the tombstone so
                // a later stale snapshot or legacy document can never resurrect it.
                database.deletedIdDao().upsert(
                    com.example.data.sync.DeletedIdEntity(
                        collection = deletion.collection,
                        documentId = deletion.syncId,
                        deletedAt = System.currentTimeMillis(),
                        cloudSynced = true
                    )
                )
            }
        }
    }

    private fun shouldApplyRemote(localUpdatedAt: Long, localStatus: com.example.data.sync.RecordSyncStatus, remoteUpdatedAt: Long): Boolean {
        if (localStatus == com.example.data.sync.RecordSyncStatus.PENDING ||
            localStatus == com.example.data.sync.RecordSyncStatus.FAILED
        ) {
            return remoteUpdatedAt > localUpdatedAt
        }
        return remoteUpdatedAt >= localUpdatedAt
    }

    private fun normalizedRemoteUpdatedAt(value: Long, createdAt: Long): Long =
        if (value > 0L) value else createdAt

    suspend fun mergeCloudData(
        cloudWorkshops: List<Workshop>,
        cloudOrders: List<FurnitureOrder>,
        cloudPayments: List<PaymentRecord>,
        cloudPresets: List<ModelPreset>,
        cloudUnitRules: List<UnitConversionRule>
    ) {
        val deleted = database.deletedIdDao().getAll()
            .map { deletionKey(it.collection, it.documentId) }
            .toSet()

        val safeWorkshops = cloudWorkshops.filterNot { deletionKey("workshops", it.syncId) in deleted }
        val safeOrders = cloudOrders.filterNot { deletionKey("orders", it.syncId) in deleted }
        val safePayments = cloudPayments.filterNot { deletionKey("payments", it.syncId) in deleted }
        val safePresets = cloudPresets.filterNot { deletionKey("presets", it.syncId) in deleted }
        val safeUnitRules = cloudUnitRules.filterNot { deletionKey("unitRules", it.syncId) in deleted }

        database.withTransaction {
            val workshopMap = mutableMapOf<String, Long>()
            val legacyWorkshopIdMap = mutableMapOf<Long, Long>()

            for (remoteRaw in safeWorkshops) {
                val remote = remoteRaw.copy(
                    updatedAt = normalizedRemoteUpdatedAt(remoteRaw.updatedAt, remoteRaw.createdAt),
                    syncStatus = com.example.data.sync.RecordSyncStatus.SYNCED
                )
                val existing = workshopDao.getWorkshopBySyncId(remote.syncId)

                val localId = if (existing == null) {
                    workshopDao.insertWorkshop(remote.copy(id = 0L))
                } else {
                    if (shouldApplyRemote(existing.updatedAt, existing.syncStatus, remote.updatedAt)) {
                        workshopDao.updateWorkshop(remote.copy(id = existing.id))
                    }
                    existing.id
                }

                workshopMap[remote.syncId] = localId
                legacyWorkshopIdMap[remote.id] = localId
            }

            val orderMap = mutableMapOf<String, Long>()
            val legacyOrderIdMap = mutableMapOf<Long, Long>()

            for (remoteRaw in safeOrders) {
                val remote = remoteRaw.copy(
                    updatedAt = normalizedRemoteUpdatedAt(remoteRaw.updatedAt, remoteRaw.createdAt),
                    syncStatus = com.example.data.sync.RecordSyncStatus.SYNCED
                )
                val existingWorkshopBySync = remote.workshopSyncId
                    .takeIf { it.isNotBlank() }
                    ?.let { workshopDao.getWorkshopBySyncId(it)?.id }

                val localWorkshopId = workshopMap[remote.workshopSyncId]
                    ?: existingWorkshopBySync
                    ?: legacyWorkshopIdMap[remote.workshopId]
                    ?: remote.workshopId

                val existing = orderDao.getOrderBySyncId(remote.syncId)
                val localId = if (existing == null) {
                    orderDao.insertOrder(remote.copy(id = 0L, workshopId = localWorkshopId))
                } else {
                    if (shouldApplyRemote(existing.updatedAt, existing.syncStatus, remote.updatedAt)) {
                        orderDao.updateOrder(
                            remote.copy(id = existing.id, workshopId = localWorkshopId)
                        )
                    }
                    existing.id
                }

                orderMap[remote.syncId] = localId
                legacyOrderIdMap[remote.id] = localId
            }

            for (remoteRaw in safePayments) {
                val remote = remoteRaw.copy(
                    updatedAt = normalizedRemoteUpdatedAt(remoteRaw.updatedAt, remoteRaw.createdAt),
                    syncStatus = com.example.data.sync.RecordSyncStatus.SYNCED
                )
                val existingWorkshopBySync = remote.workshopSyncId
                    .takeIf { it.isNotBlank() }
                    ?.let { workshopDao.getWorkshopBySyncId(it)?.id }

                val localWorkshopId = workshopMap[remote.workshopSyncId]
                    ?: existingWorkshopBySync
                    ?: legacyWorkshopIdMap[remote.workshopId]
                    ?: remote.workshopId
                val existingOrderBySync = remote.relatedOrderSyncId
                    .takeIf { it.isNotBlank() }
                    ?.let { orderDao.getOrderBySyncId(it)?.id }

                val localRelatedOrderId =
                    remote.relatedOrderSyncId.takeIf { it.isNotBlank() }?.let { orderMap[it] }
                        ?: existingOrderBySync
                        ?: remote.relatedOrderId?.let { legacyOrderIdMap[it] }

                val existing = paymentDao.getPaymentBySyncId(remote.syncId)
                if (existing == null) {
                    paymentDao.insertPayment(
                        remote.copy(
                            id = 0L,
                            workshopId = localWorkshopId,
                            relatedOrderId = localRelatedOrderId
                        )
                    )
                } else if (shouldApplyRemote(existing.updatedAt, existing.syncStatus, remote.updatedAt)) {
                    paymentDao.updatePayment(
                        remote.copy(
                            id = existing.id,
                            workshopId = localWorkshopId,
                            relatedOrderId = localRelatedOrderId
                        )
                    )
                }
            }

            for (remoteRaw in safePresets) {
                val remote = remoteRaw.copy(
                    updatedAt = normalizedRemoteUpdatedAt(remoteRaw.updatedAt, System.currentTimeMillis()),
                    syncStatus = com.example.data.sync.RecordSyncStatus.SYNCED
                )
                val existingWorkshopBySync = remote.workshopSyncId
                    .takeIf { it.isNotBlank() }
                    ?.let { workshopDao.getWorkshopBySyncId(it)?.id }

                val localWorkshopId = workshopMap[remote.workshopSyncId]
                    ?: existingWorkshopBySync
                    ?: legacyWorkshopIdMap[remote.workshopId]
                    ?: remote.workshopId

                val existing = modelPresetDao.getPresetBySyncId(remote.syncId)
                if (existing == null) {
                    modelPresetDao.insertPreset(remote.copy(id = 0L, workshopId = localWorkshopId))
                } else if (shouldApplyRemote(existing.updatedAt, existing.syncStatus, remote.updatedAt)) {
                    modelPresetDao.updatePreset(
                        remote.copy(id = existing.id, workshopId = localWorkshopId)
                    )
                }
            }

            for (remoteRaw in safeUnitRules) {
                val remote = remoteRaw.copy(
                    updatedAt = normalizedRemoteUpdatedAt(remoteRaw.updatedAt, System.currentTimeMillis()),
                    syncStatus = com.example.data.sync.RecordSyncStatus.SYNCED
                )
                val existing = unitRuleDao.getRuleBySyncId(remote.syncId)
                if (existing == null) {
                    unitRuleDao.insertRule(remote.copy(id = 0L))
                } else if (shouldApplyRemote(existing.updatedAt, existing.syncStatus, remote.updatedAt)) {
                    unitRuleDao.updateRule(remote.copy(id = existing.id))
                }
            }
        }

        val savedId = getSavedActiveWorkshopId()
        val workshopsNow = workshopDao.getAllWorkshopsSync()
        if (savedId <= 0L || workshopsNow.none { it.id == savedId }) {
            saveActiveWorkshopId(workshopsNow.firstOrNull()?.id ?: 0L)
        }
    }

    suspend fun replaceAllData(
        workshops: List<Workshop>,
        orders: List<FurnitureOrder>,
        payments: List<PaymentRecord>,
        presets: List<ModelPreset>,
        unitRules: List<UnitConversionRule>
    ) {
        // A manual backup restore is a local user action. Restored records must
        // enter the new sync pipeline as pending changes, otherwise they would
        // look already synced and never reach Firestore.
        val normalizedWorkshops = workshops.map {
            it.copy(
                syncId = it.syncId.ifBlank { java.util.UUID.randomUUID().toString() },
                syncStatus = com.example.data.sync.RecordSyncStatus.PENDING,
                updatedAt = if (it.updatedAt > 0L) it.updatedAt else it.createdAt
            )
        }
        val workshopSyncById = normalizedWorkshops.associateBy { it.id }.mapValues { it.value.syncId }

        val normalizedOrders = orders.map {
            it.copy(
                syncId = it.syncId.ifBlank { java.util.UUID.randomUUID().toString() },
                workshopSyncId = it.workshopSyncId.ifBlank { workshopSyncById[it.workshopId].orEmpty() },
                syncStatus = com.example.data.sync.RecordSyncStatus.PENDING,
                updatedAt = if (it.updatedAt > 0L) it.updatedAt else it.createdAt
            )
        }
        val normalizedPayments = payments.map {
            it.copy(
                syncId = it.syncId.ifBlank { java.util.UUID.randomUUID().toString() },
                workshopSyncId = it.workshopSyncId.ifBlank { workshopSyncById[it.workshopId].orEmpty() },
                syncStatus = com.example.data.sync.RecordSyncStatus.PENDING,
                updatedAt = if (it.updatedAt > 0L) it.updatedAt else it.createdAt
            )
        }
        val normalizedPresets = presets.map {
            it.copy(
                syncId = it.syncId.ifBlank { java.util.UUID.randomUUID().toString() },
                workshopSyncId = it.workshopSyncId.ifBlank { workshopSyncById[it.workshopId].orEmpty() },
                syncStatus = com.example.data.sync.RecordSyncStatus.PENDING,
                updatedAt = if (it.updatedAt > 0L) it.updatedAt else System.currentTimeMillis()
            )
        }
        val normalizedRules = unitRules.map {
            it.copy(
                syncId = it.syncId.ifBlank { java.util.UUID.randomUUID().toString() },
                syncStatus = com.example.data.sync.RecordSyncStatus.PENDING,
                updatedAt = if (it.updatedAt > 0L) it.updatedAt else System.currentTimeMillis()
            )
        }

        database.withTransaction {
            orderDao.clearAll()
            paymentDao.clearAll()
            modelPresetDao.clearAll()
            unitRuleDao.clearAll()
            workshopDao.clearAll()

            if (normalizedWorkshops.isNotEmpty()) workshopDao.insertAll(normalizedWorkshops)
            if (normalizedOrders.isNotEmpty()) orderDao.insertAll(normalizedOrders)
            if (normalizedPayments.isNotEmpty()) paymentDao.insertAll(normalizedPayments)
            if (normalizedPresets.isNotEmpty()) modelPresetDao.insertAll(normalizedPresets)
            if (normalizedRules.isNotEmpty()) unitRuleDao.insertAll(normalizedRules)
        }

        val savedId = getSavedActiveWorkshopId()
        if (savedId <= 0L || normalizedWorkshops.none { it.id == savedId }) {
            saveActiveWorkshopId(normalizedWorkshops.firstOrNull()?.id ?: 0L)
        }
    }

    suspend fun updateOrdersColorForModel(modelName: String, newColor: String, workshopId: Long) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            orderDao.updateModelColor(modelName, newColor, workshopId, now)
            modelPresetDao.updatePresetColor(modelName, newColor, workshopId, now)
        }
    }

    suspend fun saveOrder(order: FurnitureOrder) {
        val workshopSyncId = order.workshopSyncId.ifBlank {
            workshopDao.getWorkshopById(order.workshopId)?.syncId.orEmpty()
        }
        val syncId = order.syncId.ifBlank { java.util.UUID.randomUUID().toString() }
        val normalizedOrder = order.copy(
            syncId = syncId,
            workshopSyncId = workshopSyncId,
            updatedAt = System.currentTimeMillis(),
            syncStatus = com.example.data.sync.RecordSyncStatus.PENDING
        )
        val existing = orderDao.getOrderBySyncId(syncId)
        if (existing == null) {
            orderDao.insertOrder(normalizedOrder)
        } else {
            orderDao.updateOrder(normalizedOrder.copy(id = existing.id))
        }
    }

    suspend fun deleteOrder(order: FurnitureOrder) {
        database.withTransaction {
            orderDao.deleteOrder(order)
            recordCloudDeletion("orders", order.syncId)
        }
    }

    suspend fun deleteOrderById(id: Long) {
        database.withTransaction {
            val existing = orderDao.getOrderById(id)
            orderDao.deleteOrderById(id)
            existing?.syncId?.let { recordCloudDeletion("orders", it) }
        }
    }

    suspend fun savePayment(payment: PaymentRecord) {
        val workshopSyncId = payment.workshopSyncId.ifBlank {
            workshopDao.getWorkshopById(payment.workshopId)?.syncId.orEmpty()
        }
        val relatedOrderSyncId = payment.relatedOrderSyncId.ifBlank {
            payment.relatedOrderId?.let { orderDao.getOrderById(it)?.syncId }.orEmpty()
        }
        val syncId = payment.syncId.ifBlank { java.util.UUID.randomUUID().toString() }
        val normalizedPayment = payment.copy(
            syncId = syncId,
            workshopSyncId = workshopSyncId,
            relatedOrderSyncId = relatedOrderSyncId,
            updatedAt = System.currentTimeMillis(),
            syncStatus = com.example.data.sync.RecordSyncStatus.PENDING
        )
        val existing = paymentDao.getPaymentBySyncId(syncId)
        if (existing == null) {
            paymentDao.insertPayment(normalizedPayment)
        } else {
            paymentDao.updatePayment(normalizedPayment.copy(id = existing.id))
        }
    }

    suspend fun deletePayment(payment: PaymentRecord) {
        database.withTransaction {
            paymentDao.deletePayment(payment)
            recordCloudDeletion("payments", payment.syncId)
        }
    }

    suspend fun deletePaymentById(id: Long) {
        database.withTransaction {
            val existing = paymentDao.getPaymentById(id)
            paymentDao.deletePaymentById(id)
            existing?.syncId?.let { recordCloudDeletion("payments", it) }
        }
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
        val syncId = preset.syncId.ifBlank { java.util.UUID.randomUUID().toString() }
        val normalizedPreset = preset.copy(
            syncId = syncId,
            workshopSyncId = workshopSyncId,
            updatedAt = System.currentTimeMillis(),
            syncStatus = com.example.data.sync.RecordSyncStatus.PENDING
        )
        val trimmed = preset.name.trim()
        if (trimmed.isBlank()) return

        val existingBySync = modelPresetDao.getPresetBySyncId(syncId)
        if (existingBySync != null) {
            modelPresetDao.updatePreset(
                normalizedPreset.copy(id = existingBySync.id, name = trimmed)
            )
            return
        }

        // Name uniqueness is a UI/domain rule. Only a new preset can resolve
        // to an existing name. Editing always follows syncId identity.
        val existingByName = if (preset.id == 0L) {
            modelPresetDao.getPresetByNameAndWorkshop(trimmed, normalizedPreset.workshopId)
        } else {
            null
        }

        when {
            existingByName != null -> {
                modelPresetDao.updatePreset(
                    normalizedPreset.copy(id = existingByName.id, syncId = existingByName.syncId, name = trimmed)
                )
            }
            preset.id == 0L -> modelPresetDao.insertPreset(normalizedPreset.copy(name = trimmed))
            else -> modelPresetDao.updatePreset(normalizedPreset.copy(name = trimmed))
        }
    }

    suspend fun deletePreset(preset: ModelPreset) {
        database.withTransaction {
            modelPresetDao.deletePreset(preset)
            recordCloudDeletion("presets", preset.syncId)
        }
    }

    suspend fun deletePresetById(id: Long) {
        database.withTransaction {
            val existing = modelPresetDao.getPresetById(id)
            modelPresetDao.deletePresetById(id)
            existing?.syncId?.let { recordCloudDeletion("presets", it) }
        }
    }

    suspend fun deletePresetByName(name: String) {
        database.withTransaction {
            modelPresetDao.getPresetByName(name)?.let { recordCloudDeletion("presets", it.syncId) }
            modelPresetDao.deletePresetByName(name)
        }
    }

    suspend fun deletePresetByNameAndWorkshop(name: String, workshopId: Long) {
        database.withTransaction {
            val existing = modelPresetDao.getPresetByNameAndWorkshop(name, workshopId)
            modelPresetDao.deletePresetByNameAndWorkshop(name, workshopId)
            existing?.syncId?.let { recordCloudDeletion("presets", it) }
        }
    }

    suspend fun saveUnitRule(rule: UnitConversionRule) {
        val syncId = rule.syncId.ifBlank { java.util.UUID.randomUUID().toString() }
        val pendingRule = rule.copy(
            syncId = syncId,
            updatedAt = System.currentTimeMillis(),
            syncStatus = com.example.data.sync.RecordSyncStatus.PENDING
        )
        val rawKey = rule.pieceKey.ifBlank {
            if (rule.pieceCount % 1.0 == 0.0) rule.pieceCount.toInt().toString() else rule.pieceCount.toString()
        }
        val norm = normalizeUnitKey(rawKey)

        val existingBySync = unitRuleDao.getRuleBySyncId(syncId)
        if (existingBySync != null) {
            unitRuleDao.updateRule(pendingRule.copy(id = existingBySync.id))
            return
        }

        val duplicate = if (rule.id == 0L) {
            unitRuleDao.getAllRulesSync().firstOrNull {
                normalizeUnitKey(
                    it.pieceKey.ifBlank {
                        if (it.pieceCount % 1.0 == 0.0) it.pieceCount.toInt().toString() else it.pieceCount.toString()
                    }
                ) == norm
            }
        } else {
            null
        }

        if (duplicate != null) {
            // A new local rule reuses the existing canonical identity instead
            // of creating a second cloud document for the same logical rule.
            unitRuleDao.updateRule(
                duplicate.copy(
                    pieceKey = pendingRule.pieceKey,
                    pieceCount = pendingRule.pieceCount,
                    calculatedUnits = pendingRule.calculatedUnits,
                    isEnabled = pendingRule.isEnabled,
                    updatedAt = pendingRule.updatedAt,
                    syncStatus = pendingRule.syncStatus
                )
            )
        } else if (rule.id == 0L) {
            unitRuleDao.insertRule(pendingRule)
        } else {
            unitRuleDao.updateRule(pendingRule)
        }
    }

    suspend fun deleteUnitRule(rule: UnitConversionRule) {
        // The four base conversion rules are permanent defaults for every account.
        if (isMandatoryDefaultUnitRule(rule)) return

        database.withTransaction {
            unitRuleDao.deleteRule(rule)
            recordCloudDeletion("unitRules", rule.syncId)
        }
    }

    suspend fun deleteUnitRuleById(id: Long) {
        database.withTransaction {
            val existing = unitRuleDao.getAllRulesSync().firstOrNull { it.id == id }
            if (existing != null && isMandatoryDefaultUnitRule(existing)) return@withTransaction

            unitRuleDao.deleteRuleById(id)
            existing?.syncId?.let { recordCloudDeletion("unitRules", it) }
        }
    }

    private data class DefaultUnitRule(val key: String, val calculatedUnits: Double)

    private val defaultUnitRules = listOf(
        DefaultUnitRule("3", 2.0),
        DefaultUnitRule("2", 1.5),
        DefaultUnitRule("1", 1.0),
        DefaultUnitRule("0.5", 0.5)
    )

    private fun deletedDefaultUnitRulesKey(): String {
        val uid = getLocalAccountUid().orEmpty()
        return if (uid.isBlank()) "deleted_default_unit_rules_unscoped"
        else "deleted_default_unit_rules_$uid"
    }

    private fun deletedDefaultUnitRules(): MutableSet<String> =
        prefs?.getStringSet(deletedDefaultUnitRulesKey(), emptySet()).orEmpty().toMutableSet()

    private fun isMandatoryDefaultUnitRule(rule: UnitConversionRule): Boolean {
        val raw = rule.pieceKey.ifBlank {
            if (rule.pieceCount % 1.0 == 0.0) rule.pieceCount.toInt().toString() else rule.pieceCount.toString()
        }
        val key = normalizeUnitKey(raw)
        return defaultUnitRules.any { it.key == key }
    }

    private fun markDefaultUnitRuleDeleted(rule: UnitConversionRule) {
        val raw = rule.pieceKey.ifBlank {
            if (rule.pieceCount % 1.0 == 0.0) rule.pieceCount.toInt().toString() else rule.pieceCount.toString()
        }
        val key = normalizeUnitKey(raw)
        if (defaultUnitRules.any { it.key == key }) {
            val deleted = deletedDefaultUnitRules()
            deleted += key
            prefs?.edit()?.putStringSet(deletedDefaultUnitRulesKey(), deleted)?.apply()
        }
    }

    suspend fun insertDefaultUnitRulesIfEmpty() {
        // These four base conversion rules are mandatory defaults for every account.
        // They must survive logout/account switching and must be restored if missing.
        // User-created extra rules remain untouched.
        val existing = unitRuleDao.getAllRulesSync()

        for (defaultRule in defaultUnitRules) {
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
        prefs?.edit()?.remove(deletedDefaultUnitRulesKey())?.apply()
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
