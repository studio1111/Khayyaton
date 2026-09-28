package com.example.data.sync

import android.content.Context
import com.example.data.AppDatabase
import com.example.data.WorkshopRepository
import com.example.data.firebase.FirebaseService
import com.example.model.FurnitureOrder
import com.example.model.ModelPreset
import com.example.model.PaymentRecord
import com.example.model.UnitConversionRule
import com.example.model.Workshop
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.combine
import kotlinx.coroutines.debounce
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.CopyOnWriteArrayList

class SyncManager(
    context: Context,
    private val repository: WorkshopRepository,
    private val database: AppDatabase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val connectivity = ConnectivityMonitor(context)
    private val storageSync = StorageSyncManager(context, database)
    private val listeners = CopyOnWriteArrayList<com.google.firebase.firestore.ListenerRegistration>()
    private var authListener: FirebaseAuth.AuthStateListener? = null
    private var observeJob: Job? = null
    private var syncJob: Job? = null

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    fun start() {
        connectivity.start()
        observePendingCount()
        observeLocalChanges()

        scope.launch {
            connectivity.isOnline.collect { online ->
                if (online) syncNow()
            }
        }

        val auth = FirebaseService.authInstance()
        authListener = FirebaseAuth.AuthStateListener { user ->
            if (user == null) {
                detachListeners()
            } else {
                restartListeners()
                syncNow()
            }
        }
        auth.addAuthStateListener(authListener!!)
        if (auth.currentUser != null) {
            restartListeners()
            syncNow()
        }
    }

    fun stop() {
        authListener?.let { FirebaseService.authInstance().removeAuthStateListener(it) }
        authListener = null
        detachListeners()
        observeJob?.cancel()
        syncJob?.cancel()
        connectivity.stop()
        scope.cancel()
    }

    fun syncNow() {
        if (!connectivity.isOnline.value) return
        syncJob?.cancel()
        syncJob = scope.launch {
            storageSync.processQueues()
            val result = FirebaseService.uploadAllToCloud(
                orders = repository.getAllOrdersSync(),
                payments = repository.getAllPaymentsSync(),
                presets = repository.getAllPresetsSync(),
                unitRules = repository.getAllUnitRulesSync(),
                workshops = repository.getAllWorkshopsSync(),
                repository = repository
            )
            if (result.isSuccess) {
                attachListenersIfNeeded()
            }
        }
    }

    private fun observePendingCount() {
        scope.launch {
            combine(
                database.uploadQueueDao().pendingCount(),
                database.pendingDeleteDao().pendingCount(),
                database.documentCacheDao().pendingWritesCount(),
                database.deletedIdDao().countFlow()
            ) { uploads, deletes, pendingWrites, tombstones ->
                uploads + deletes + pendingWrites + tombstones
            }.collect { _pendingCount.value = it }
        }
    }

    private fun observeLocalChanges() {
        observeJob = scope.launch {
            combine(
                repository.workshops,
                repository.orders,
                repository.payments,
                repository.modelPresets,
                repository.unitRules
            ) { workshops, orders, payments, presets, rules ->
                LocalSnapshot(workshops, orders, payments, presets, rules)
            }.debounce(350).collect { snapshot ->
                val hasPending = snapshot.workshops.any { it.syncStatus == RecordSyncStatus.PENDING || it.syncStatus == RecordSyncStatus.FAILED } ||
                    snapshot.orders.any { it.syncStatus == RecordSyncStatus.PENDING || it.syncStatus == RecordSyncStatus.FAILED } ||
                    snapshot.payments.any { it.syncStatus == RecordSyncStatus.PENDING || it.syncStatus == RecordSyncStatus.FAILED } ||
                    snapshot.presets.any { it.syncStatus == RecordSyncStatus.PENDING || it.syncStatus == RecordSyncStatus.FAILED } ||
                    snapshot.rules.any { it.syncStatus == RecordSyncStatus.PENDING || it.syncStatus == RecordSyncStatus.FAILED }
                if (hasPending && connectivity.isOnline.value) syncNow()
            }
        }
    }

    private fun restartListeners() {
        detachListeners()
        attachListenersIfNeeded()
    }

    private fun attachListenersIfNeeded() {
        val db = FirebaseService.firestoreInstance() ?: return
        val user = FirebaseService.currentUser() ?: return
        if (listeners.isNotEmpty()) return

        val userDoc = db.collection("users").document(user.uid)
        listOf("workshops", "orders", "payments", "presets", "unitRules").forEach { collection ->
            val registration = userDoc.collection(collection)
                .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    scope.launch { handleSnapshot(collection, snapshot) }
                }
            listeners += registration
        }
    }

    private suspend fun handleSnapshot(collection: String, snapshot: QuerySnapshot) {
        for (change in snapshot.documentChanges) {
            val doc = change.document
            val syncId = (doc.getString("syncId") ?: doc.id).ifBlank { doc.id }
            val pending = doc.metadata.hasPendingWrites()
            val fromCache = snapshot.metadata.isFromCache

            database.documentCacheDao().upsert(
                DocumentCacheEntity(
                    collection = collection,
                    documentId = syncId,
                    updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time
                        ?: doc.getLong("updatedAt")
                        ?: System.currentTimeMillis(),
                    fromCache = fromCache,
                    hasPendingWrites = pending
                )
            )

            if (pending) continue

            if (change.type == DocumentChange.Type.REMOVED) {
                repository.applyCloudDeletions(
                    listOf(WorkshopRepository.PendingCloudDeletion(collection, syncId))
                )
                database.deletedIdDao().delete(collection, syncId)
                continue
            }

            if (database.deletedIdDao().contains(collection, syncId)) continue

            val parsed = parseDocument(collection, doc)
            if (parsed != null) {
                when (parsed) {
                    is Workshop -> repository.mergeCloudData(parsed, emptyList(), emptyList(), emptyList(), emptyList())
                    is FurnitureOrder -> repository.mergeCloudData(emptyList(), listOf(parsed), emptyList(), emptyList(), emptyList())
                    is PaymentRecord -> repository.mergeCloudData(emptyList(), emptyList(), listOf(parsed), emptyList(), emptyList())
                    is ModelPreset -> repository.mergeCloudData(emptyList(), emptyList(), emptyList(), listOf(parsed), emptyList())
                    is UnitConversionRule -> repository.mergeCloudData(emptyList(), emptyList(), emptyList(), emptyList(), listOf(parsed))
                }
            }
        }
    }

    private fun parseDocument(collection: String, doc: com.google.firebase.firestore.DocumentSnapshot): Any? {
        val data = doc.data ?: return null
        val updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time
            ?: doc.getLong("updatedAt")
            ?: System.currentTimeMillis()
        val status = if (doc.metadata.hasPendingWrites()) RecordSyncStatus.PENDING else RecordSyncStatus.SYNCED

        return when (collection) {
            "workshops" -> Workshop(
                id = data["id"].numberLong(),
                syncId = data["syncId"] as? String ?: doc.id,
                name = data["name"] as? String ?: "کارگاه",
                createdAt = data["createdAt"].numberLong(System.currentTimeMillis()),
                updatedAt = updatedAt,
                syncStatus = status,
                fileUrl = data["fileUrl"] as? String,
                storagePath = data["storagePath"] as? String
            )
            "orders" -> FurnitureOrder(
                id = data["id"].numberLong(),
                workshopId = data["workshopId"].numberLong(1L),
                workshopSyncId = data["workshopSyncId"] as? String ?: "",
                syncId = data["syncId"] as? String ?: doc.id,
                orderNumber = data["orderNumber"].numberLong(1L),
                invoiceNumber = data["invoiceNumber"] as? String ?: "",
                modelName = data["modelName"] as? String ?: "",
                pricePerSet = data["pricePerSet"].numberLong(),
                unitsPerSet = data["unitsPerSet"].numberDouble(6.0),
                countFormula = data["countFormula"] as? String ?: "",
                calculatedUnits = data["calculatedUnits"].numberDouble(),
                calculatedTotal = data["calculatedTotal"].numberLong(),
                dateJalali = data["dateJalali"] as? String ?: "",
                dateGregorian = data["dateGregorian"] as? String ?: "",
                customerName = data["customerName"] as? String ?: "",
                phone = data["phone"] as? String ?: "",
                fabricName = data["fabricName"] as? String ?: "",
                workshopInvoiceNumber = data["workshopInvoiceNumber"] as? String ?: "",
                notes = data["notes"] as? String ?: "",
                colorCode = data["colorCode"] as? String ?: "#2563EB",
                createdAt = data["createdAt"].numberLong(System.currentTimeMillis()),
                updatedAt = updatedAt,
                syncStatus = status,
                fileUrl = data["fileUrl"] as? String,
                storagePath = data["storagePath"] as? String
            )
            "payments" -> PaymentRecord(
                id = data["id"].numberLong(),
                workshopId = data["workshopId"].numberLong(1L),
                workshopSyncId = data["workshopSyncId"] as? String ?: "",
                syncId = data["syncId"] as? String ?: doc.id,
                paymentNumber = data["paymentNumber"].numberLong(1L),
                amount = data["amount"].numberLong(),
                dateJalali = data["dateJalali"] as? String ?: "",
                dateGregorian = data["dateGregorian"] as? String ?: "",
                customerName = data["customerName"] as? String ?: "",
                description = data["description"] as? String ?: "",
                paymentType = data["paymentType"] as? String ?: "transfer",
                referenceNo = data["referenceNo"] as? String ?: "",
                bankName = data["bankName"] as? String ?: "",
                cardNumber = data["cardNumber"] as? String ?: "",
                relatedOrderId = data["relatedOrderId"].numberLongOrNull(),
                relatedOrderSyncId = data["relatedOrderSyncId"] as? String ?: "",
                createdAt = data["createdAt"].numberLong(System.currentTimeMillis()),
                updatedAt = updatedAt,
                syncStatus = status,
                fileUrl = data["fileUrl"] as? String,
                storagePath = data["storagePath"] as? String
            )
            "presets" -> ModelPreset(
                id = data["id"].numberLong(),
                workshopId = data["workshopId"].numberLong(1L),
                workshopSyncId = data["workshopSyncId"] as? String ?: "",
                syncId = data["syncId"] as? String ?: doc.id,
                name = data["name"] as? String ?: "",
                defaultPricePerSet = data["defaultPricePerSet"].numberLong(2000000L),
                defaultUnitsPerSet = data["defaultUnitsPerSet"].numberDouble(6.0),
                colorCode = data["colorCode"] as? String ?: "#2563EB",
                description = data["description"] as? String ?: "",
                updatedAt = updatedAt,
                syncStatus = status,
                fileUrl = data["fileUrl"] as? String,
                storagePath = data["storagePath"] as? String
            )
            "unitRules" -> UnitConversionRule(
                id = data["id"].numberLong(),
                syncId = data["syncId"] as? String ?: doc.id,
                pieceKey = data["pieceKey"] as? String ?: "",
                pieceCount = data["pieceCount"].numberDouble(),
                calculatedUnits = data["calculatedUnits"].numberDouble(),
                isEnabled = data["isEnabled"] as? Boolean ?: true,
                updatedAt = updatedAt,
                syncStatus = status,
                fileUrl = data["fileUrl"] as? String,
                storagePath = data["storagePath"] as? String
            )
            else -> null
        }
    }

    private data class LocalSnapshot(
        val workshops: List<Workshop>,
        val orders: List<FurnitureOrder>,
        val payments: List<PaymentRecord>,
        val presets: List<ModelPreset>,
        val rules: List<UnitConversionRule>
    )

    private fun Any?.numberLong(default: Long = 0L): Long = when (this) {
        is Number -> this.toLong()
        is String -> this.toLongOrNull() ?: default
        else -> default
    }

    private fun Any?.numberDouble(default: Double = 0.0): Double = when (this) {
        is Number -> this.toDouble()
        is String -> this.toDoubleOrNull() ?: default
        else -> default
    }

    private fun Any?.numberLongOrNull(): Long? = when (this) {
        is Number -> this.toLong()
        is String -> this.toLongOrNull()
        else -> null
    }
}
