package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.example.data.WorkshopRepository
import com.example.model.FurnitureOrder
import com.example.model.ModelPreset
import com.example.model.PaymentRecord
import com.example.model.UnitConversionRule
import com.example.model.Workshop
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

object FirebaseService {
    private const val TAG = "FirebaseService"

    fun initialize(context: Context) {
        try {
            val apps = FirebaseApp.getApps(context)
            if (apps.isEmpty()) {
                val initialized = FirebaseApp.initializeApp(context)
                if (initialized == null) {
                    // Fallback to explicit options from google-services.json
                    val options = FirebaseOptions.Builder()
                        .setApplicationId("1:15543905804:android:8fc6393c86598be4310829")
                        .setApiKey("AIzaSyAmLQ7SPiYxhMvquyV01xYD8MZZjezknoY")
                        .setProjectId("khayyaton-26abc")
                        .setStorageBucket("khayyaton-26abc.firebasestorage.app")
                        .setGcmSenderId("15543905804")
                        .build()
                    FirebaseApp.initializeApp(context, options)
                    if (BuildConfig.DEBUG) Log.d(TAG, "Firebase initialized with explicit options")
                } else {
                    if (BuildConfig.DEBUG) Log.d(TAG, "Firebase initialized with default app")
                }
            } else {
                if (BuildConfig.DEBUG) Log.d(TAG, "Firebase already initialized with ${apps.size} apps")
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Error initializing Firebase: ${e.message}", e)
            try {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:15543905804:android:8fc6393c86598be4310829")
                    .setApiKey("AIzaSyAmLQ7SPiYxhMvquyV01xYD8MZZjezknoY")
                    .setProjectId("khayyaton-26abc")
                    .setStorageBucket("khayyaton-26abc.firebasestorage.app")
                    .setGcmSenderId("15543905804")
                    .build()
                FirebaseApp.initializeApp(context, options)
                Log.d(TAG, "Firebase recovered with explicit options")
            } catch (ex: Exception) {
                if (BuildConfig.DEBUG) Log.e(TAG, "Firebase fallback initialization failed: ${ex.message}", ex)
            }
        }
    }

    private val auth: FirebaseAuth?
        get() = try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "Firebase Auth not available", e)
            null
        }

    private val firestore: FirebaseFirestore?
        get() = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "Firebase Firestore not available", e)
            null
        }

    fun getCurrentUser(): FirebaseUserDto? {
        val user = auth?.currentUser ?: return null
        return FirebaseUserDto(
            uid = user.uid,
            email = user.email ?: "",
            displayName = user.displayName
        )
    }

    suspend fun signInWithEmail(
        email: String,
        pass: String,
        username: String? = null
    ): Result<FirebaseUserDto> {
        val fbAuth = auth ?: return Result.failure(Exception("سرویس احراز هویت فایربیس راه‌اندازی نشده است."))
        return try {
            val result = fbAuth.signInWithEmailAndPassword(email.trim(), pass).await()
            val user = result.user
            if (user != null) {
                val enteredUsername = username?.trim().orEmpty()
                if (enteredUsername.isNotBlank()) {
                    val db = firestore
                    if (db != null) {
                        val stored = db.collection("users").document(user.uid).get().await()
                            .getString("username").orEmpty().trim()
                        if (stored.isNotBlank() && !stored.equals(enteredUsername, ignoreCase = true)) {
                            fbAuth.signOut()
                            return Result.failure(Exception("نام کاربری یا ایمیل اشتباه است."))
                        }
                    }
                }
                Result.success(FirebaseUserDto(user.uid, user.email ?: email, user.displayName))
            } else {
                Result.failure(Exception("ورود ناموفق بود."))
            }
        } catch (e: Exception) {
            Result.failure(Exception(parseCloudError(e)))
        }
    }


    suspend fun registerWithEmail(email: String, pass: String): Result<FirebaseUserDto> {
        val fbAuth = auth ?: return Result.failure(Exception("سرویس فایربیس راه‌اندازی نشده است. فایل google-services.json را بررسی کنید."))
        return try {
            val result = fbAuth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val user = result.user
            if (user != null) {
                Result.success(FirebaseUserDto(user.uid, user.email ?: email, user.displayName))
            } else {
                Result.failure(Exception("ثبت‌نام ناموفق بود."))
            }
        } catch (e: Exception) {
            Result.failure(Exception(parseCloudError(e)))
        }
    }

    /**
     * Register with email, password, and a unique username.
     * Checks username availability first, then creates the Firebase Auth account,
     * then reserves the username permanently linked to this user's uid.
     */
    suspend fun registerWithEmailAndUsername(
        username: String,
        email: String,
        pass: String
    ): Result<FirebaseUserDto> {
        val fbAuth = auth ?: return Result.failure(Exception("سرویس فایربیس راه‌اندازی نشده است. فایل google-services.json را بررسی کنید."))
        val db = firestore ?: return Result.failure(Exception("پایگاه داده ابری Firestore در دسترس نیست."))
        val normalizedUsername = username.trim().lowercase()

        if (normalizedUsername.isBlank()) {
            return Result.failure(Exception("لطفاً نام کاربری را وارد کنید."))
        }
        if (!normalizedUsername.matches(Regex("^[\\p{L}\\p{N}_.-]{3,32}$"))) {
            return Result.failure(Exception("نام کاربری باید ۳ تا ۳۲ نویسه و فقط شامل حروف، اعداد، نقطه، خط تیره یا زیرخط باشد."))
        }

        return try {
            val result = fbAuth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val user = result.user ?: return Result.failure(Exception("ثبت‌نام ناموفق بود."))
            val usernameRef = db.collection("usernames").document(normalizedUsername)
            val userRef = db.collection("users").document(user.uid)

            try {
                db.runTransaction { transaction ->
                    val current = transaction.get(usernameRef)
                    if (current.exists()) throw IllegalStateException("USERNAME_TAKEN")
                    transaction.set(usernameRef, mapOf("uid" to user.uid))
                    transaction.set(
                        userRef,
                        mapOf("username" to username.trim(), "email" to (user.email ?: email)),
                        SetOptions.merge()
                    )
                }.await()
            } catch (reservationError: Exception) {
                runCatching { user.delete().await() }
                if (reservationError.message == "USERNAME_TAKEN") {
                    return Result.failure(Exception("این نام کاربری قبلاً استفاده شده است. لطفاً نام دیگری انتخاب کنید."))
                }
                throw reservationError
            }

            Result.success(FirebaseUserDto(user.uid, user.email ?: email, username.trim()))
        } catch (e: Exception) {
            Result.failure(Exception(parseCloudError(e)))
        }    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        val fbAuth = auth ?: return Result.failure(Exception("سرویس فایربیس در دسترس نیست."))
        return try {
            fbAuth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(parseCloudError(e)))
        }
    }

    fun signOut() {
        auth?.signOut()
    }

    /**
     * Backup/Upload all local database records to Firebase Firestore under users/{uid}/
     */
    suspend fun uploadAllToCloud(
        orders: List<FurnitureOrder>,
        payments: List<PaymentRecord>,
        presets: List<ModelPreset>,
        unitRules: List<UnitConversionRule>,
        workshops: List<Workshop> = emptyList(),
        repository: WorkshopRepository? = null
    ): Result<CloudSyncResult> {
        val user = auth?.currentUser
            ?: return Result.failure(Exception("ابتدا باید وارد حساب کاربری خود شوید."))
        val db = firestore
            ?: return Result.failure(Exception("پایگاه داده ابری Firestore در دسترس نیست."))

        return try {
            val userDoc = db.collection("users").document(user.uid)
            val maxBatchSize = 400

            // Respect deletion tombstones from other devices so stale local
            // copies cannot resurrect records that were intentionally deleted.
            val cloudDeletionSnapshot = userDoc.collection("deletions").get().await()
            val blocked = cloudDeletionSnapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val collection = data["collection"] as? String ?: return@mapNotNull null
                val syncId = data["syncId"] as? String ?: return@mapNotNull null
                "$collection|$syncId"
            }.toSet()

            fun allowed(collection: String, syncId: String): Boolean =
                "$collection|$syncId" !in blocked

            suspend fun batchSet(
                collection: com.google.firebase.firestore.CollectionReference,
                documents: List<Pair<String, Map<String, Any?>>>
            ) {
                var batch = db.batch()
                var count = 0

                for ((documentId, data) in documents) {
                    batch.set(
                        collection.document(documentId),
                        data,
                        SetOptions.merge()
                    )
                    count++

                    if (count == maxBatchSize) {
                        batch.commit().await()
                        batch = db.batch()
                        count = 0
                    }
                }

                if (count > 0) batch.commit().await()
            }

            suspend fun deleteStale(
                collection: com.google.firebase.firestore.CollectionReference,
                keepIds: Set<String>
            ) {
                val snapshot = collection.get().await()
                var batch = db.batch()
                var count = 0

                for (doc in snapshot.documents) {
                    if (doc.id in keepIds) continue
                    batch.delete(doc.reference)
                    count++

                    if (count == maxBatchSize) {
                        batch.commit().await()
                        batch = db.batch()
                        count = 0
                    }
                }

                if (count > 0) batch.commit().await()
            }

            // Propagate local deletions with durable tombstones.
            val pendingDeletions = repository?.getPendingCloudDeletions().orEmpty()
            if (pendingDeletions.isNotEmpty()) {
                var deletionBatch = db.batch()
                var deletionCount = 0

                for (deletion in pendingDeletions) {
                    val collection = when (deletion.collection) {
                        "workshops" -> userDoc.collection("workshops")
                        "orders" -> userDoc.collection("orders")
                        "payments" -> userDoc.collection("payments")
                        "presets" -> userDoc.collection("presets")
                        "unitRules" -> userDoc.collection("unitRules")
                        else -> null
                    } ?: continue

                    deletionBatch.delete(collection.document(deletion.syncId))

                    val legacyId = when {
                        deletion.syncId.startsWith("wrk_") -> deletion.syncId.removePrefix("wrk_")
                        deletion.syncId.startsWith("ord_") -> deletion.syncId.removePrefix("ord_")
                        deletion.syncId.startsWith("pay_") -> deletion.syncId.removePrefix("pay_")
                        deletion.syncId.startsWith("pre_") -> deletion.syncId.removePrefix("pre_")
                        deletion.syncId.startsWith("rule_") -> deletion.syncId.removePrefix("rule_")
                        else -> null
                    }
                    if (!legacyId.isNullOrBlank()) {
                        deletionBatch.delete(collection.document(legacyId))
                    }

                    deletionBatch.set(
                        userDoc.collection("deletions").document("${deletion.collection}_${deletion.syncId}"),
                        mapOf(
                            "collection" to deletion.collection,
                            "syncId" to deletion.syncId,
                            "deletedAt" to System.currentTimeMillis()
                        )
                    )
                    deletionCount += 1

                    if (deletionCount == maxBatchSize / 3) {
                        deletionBatch.commit().await()
                        deletionBatch = db.batch()
                        deletionCount = 0
                    }
                }

                if (deletionCount > 0) deletionBatch.commit().await()
                repository?.clearCloudDeletions(pendingDeletions)
            }

            val workshopsCol = userDoc.collection("workshops")
            val activeWorkshops = workshops.filter { allowed("workshops", it.syncId) }
            val workshopDocs = activeWorkshops.map { workshop ->
                "${workshop.syncId}" to mapOf(
                    "id" to workshop.id,
                    "syncId" to workshop.syncId,
                    "name" to workshop.name,
                    "createdAt" to workshop.createdAt
                )
            }
            batchSet(workshopsCol, workshopDocs)

            val ordersCol = userDoc.collection("orders")
            val activeOrders = orders.filter { allowed("orders", it.syncId) }
            val orderDocs = activeOrders.map { order ->
                "${order.syncId}" to mapOf(
                    "id" to order.id,
                    "syncId" to order.syncId,
                    "workshopId" to order.workshopId,
                    "workshopSyncId" to order.workshopSyncId,
                    "orderNumber" to order.orderNumber,
                    "invoiceNumber" to order.invoiceNumber,
                    "modelName" to order.modelName,
                    "pricePerSet" to order.pricePerSet,
                    "unitsPerSet" to order.unitsPerSet,
                    "countFormula" to order.countFormula,
                    "calculatedUnits" to order.calculatedUnits,
                    "calculatedTotal" to order.calculatedTotal,
                    "dateJalali" to order.dateJalali,
                    "dateGregorian" to order.dateGregorian,
                    "customerName" to order.customerName,
                    "phone" to order.phone,
                    "fabricName" to order.fabricName,
                    "workshopInvoiceNumber" to order.workshopInvoiceNumber,
                    "notes" to order.notes,
                    "colorCode" to order.colorCode,
                    "createdAt" to order.createdAt
                )
            }
            batchSet(ordersCol, orderDocs)

            val paymentsCol = userDoc.collection("payments")
            val activePayments = payments.filter { allowed("payments", it.syncId) }
            val paymentDocs = activePayments.map { payment ->
                "${payment.syncId}" to mapOf(
                    "id" to payment.id,
                    "syncId" to payment.syncId,
                    "workshopId" to payment.workshopId,
                    "workshopSyncId" to payment.workshopSyncId,
                    "paymentNumber" to payment.paymentNumber,
                    "amount" to payment.amount,
                    "dateJalali" to payment.dateJalali,
                    "dateGregorian" to payment.dateGregorian,
                    "customerName" to payment.customerName,
                    "description" to payment.description,
                    "paymentType" to payment.paymentType,
                    "referenceNo" to payment.referenceNo,
                    "bankName" to payment.bankName,
                    "cardNumber" to payment.cardNumber,
                    "relatedOrderId" to payment.relatedOrderId,
                    "relatedOrderSyncId" to payment.relatedOrderSyncId,
                    "createdAt" to payment.createdAt
                )
            }
            batchSet(paymentsCol, paymentDocs)

            val presetsCol = userDoc.collection("presets")
            val activePresets = presets.filter { allowed("presets", it.syncId) }
            val presetDocs = activePresets.map { preset ->
                "${preset.syncId}" to mapOf(
                    "id" to preset.id,
                    "syncId" to preset.syncId,
                    "workshopId" to preset.workshopId,
                    "workshopSyncId" to preset.workshopSyncId,
                    "name" to preset.name,
                    "defaultPricePerSet" to preset.defaultPricePerSet,
                    "defaultUnitsPerSet" to preset.defaultUnitsPerSet,
                    "colorCode" to preset.colorCode,
                    "description" to preset.description
                )
            }
            batchSet(presetsCol, presetDocs)

            val rulesCol = userDoc.collection("unitRules")
            val activeUnitRules = unitRules.filter { allowed("unitRules", it.syncId) }
            val ruleDocs = activeUnitRules.map { rule ->
                "${rule.syncId}" to mapOf(
                    "id" to rule.id,
                    "syncId" to rule.syncId,
                    "pieceKey" to rule.pieceKey,
                    "pieceCount" to rule.pieceCount,
                    "calculatedUnits" to rule.calculatedUnits,
                    "isEnabled" to rule.isEnabled
                )
            }
            batchSet(rulesCol, ruleDocs)

            // Normal sync remains additive/update-only. Explicit user deletions are
            // handled separately through durable tombstones above.

            userDoc.set(
                mapOf(
                    "email" to (user.email ?: ""),
                    "lastSyncAt" to System.currentTimeMillis(),
                    "totalOrders" to orders.size,
                    "totalPayments" to payments.size
                ),
                SetOptions.merge()
            ).await()

            Result.success(
                CloudSyncResult(
                    success = true,
                    ordersCount = activeOrders.size,
                    paymentsCount = activePayments.size,
                    presetsCount = activePresets.size,
                    unitRulesCount = activeUnitRules.size
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading to Firestore", e)
            Result.failure(Exception(parseCloudError(e)))
        }
    }

    /**
     * Download and restore all records from Firebase Firestore to local Room Database
     */
    suspend fun downloadFromCloud(repository: WorkshopRepository): Result<CloudSyncResult> {
        val user = auth?.currentUser
            ?: return Result.failure(Exception("ابتدا باید وارد حساب کاربری خود شوید."))
        val db = firestore
            ?: return Result.failure(Exception("پایگاه داده ابری Firestore در دسترس نیست."))

        return try {
            val userDoc = db.collection("users").document(user.uid)

            val deletionSnapshot = userDoc.collection("deletions").get().await()
            val deletedRecords = deletionSnapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val collection = data["collection"] as? String ?: return@mapNotNull null
                val syncId = data["syncId"] as? String ?: return@mapNotNull null
                WorkshopRepository.PendingCloudDeletion(collection, syncId)
            }
            repository.applyCloudDeletions(deletedRecords)

            fun isDeleted(collection: String, syncId: String, documentId: String): Boolean {
                return deletedRecords.any { deletion ->
                    if (deletion.collection != collection) return@any false
                    deletion.syncId == syncId ||
                        when {
                            deletion.syncId.startsWith("wrk_") && collection == "workshops" ->
                                deletion.syncId.removePrefix("wrk_") == documentId
                            deletion.syncId.startsWith("ord_") && collection == "orders" ->
                                deletion.syncId.removePrefix("ord_") == documentId
                            deletion.syncId.startsWith("pay_") && collection == "payments" ->
                                deletion.syncId.removePrefix("pay_") == documentId
                            deletion.syncId.startsWith("pre_") && collection == "presets" ->
                                deletion.syncId.removePrefix("pre_") == documentId
                            deletion.syncId.startsWith("rule_") && collection == "unitRules" ->
                                deletion.syncId.removePrefix("rule_") == documentId
                            else -> false
                        }
                }
            }

            val workshopsSnapshot = userDoc.collection("workshops").get().await()
            val restoredWorkshops = workshopsSnapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val id = (data["id"] as? Number)?.toLong() ?: 0L
                val syncId = (data["syncId"] as? String).orEmpty().ifBlank { doc.id }
                if (isDeleted("workshops", syncId, doc.id)) return@mapNotNull null
                Workshop(
                    id = id,
                    syncId = syncId,
                    name = (data["name"] as? String).orEmpty().ifBlank { "کارگاه" },
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
            }
            val fallbackWorkshopId = restoredWorkshops.firstOrNull()?.id ?: 1L

            val ordersSnapshot = userDoc.collection("orders").get().await()
            val restoredOrders = ordersSnapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val id = (data["id"] as? Number)?.toLong() ?: 0L
                val syncId = (data["syncId"] as? String).orEmpty().ifBlank { doc.id }
                if (isDeleted("orders", syncId, doc.id)) return@mapNotNull null
                FurnitureOrder(
                    id = id,
                    syncId = syncId,
                    workshopId = (data["workshopId"] as? Number)?.toLong() ?: fallbackWorkshopId,
                    workshopSyncId = data["workshopSyncId"] as? String ?: "",
                    orderNumber = (data["orderNumber"] as? Number)?.toLong() ?: 1L,
                    invoiceNumber = data["invoiceNumber"] as? String ?: "",
                    modelName = data["modelName"] as? String ?: "",
                    pricePerSet = (data["pricePerSet"] as? Number)?.toLong() ?: 0L,
                    unitsPerSet = (data["unitsPerSet"] as? Number)?.toDouble() ?: 6.0,
                    countFormula = data["countFormula"] as? String ?: "",
                    calculatedUnits = (data["calculatedUnits"] as? Number)?.toDouble() ?: 0.0,
                    calculatedTotal = (data["calculatedTotal"] as? Number)?.toLong() ?: 0L,
                    dateJalali = data["dateJalali"] as? String ?: "",
                    dateGregorian = data["dateGregorian"] as? String ?: "",
                    customerName = data["customerName"] as? String ?: "",
                    phone = data["phone"] as? String ?: "",
                    fabricName = data["fabricName"] as? String ?: "",
                    workshopInvoiceNumber = data["workshopInvoiceNumber"] as? String ?: "",
                    notes = data["notes"] as? String ?: "",
                    colorCode = data["colorCode"] as? String ?: "#2563EB",
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
            }

            val paymentsSnapshot = userDoc.collection("payments").get().await()
            val restoredPayments = paymentsSnapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val id = (data["id"] as? Number)?.toLong() ?: 0L
                val syncId = (data["syncId"] as? String).orEmpty().ifBlank { doc.id }
                if (isDeleted("payments", syncId, doc.id)) return@mapNotNull null
                PaymentRecord(
                    id = id,
                    syncId = syncId,
                    workshopId = (data["workshopId"] as? Number)?.toLong() ?: fallbackWorkshopId,
                    workshopSyncId = data["workshopSyncId"] as? String ?: "",
                    paymentNumber = (data["paymentNumber"] as? Number)?.toLong() ?: 1L,
                    amount = (data["amount"] as? Number)?.toLong() ?: 0L,
                    dateJalali = data["dateJalali"] as? String ?: "",
                    dateGregorian = data["dateGregorian"] as? String ?: "",
                    customerName = data["customerName"] as? String ?: "",
                    description = data["description"] as? String ?: "",
                    paymentType = data["paymentType"] as? String ?: "transfer",
                    referenceNo = data["referenceNo"] as? String ?: "",
                    bankName = data["bankName"] as? String ?: "",
                    cardNumber = data["cardNumber"] as? String ?: "",
                    relatedOrderId = (data["relatedOrderId"] as? Number)?.toLong(),
                    relatedOrderSyncId = data["relatedOrderSyncId"] as? String ?: "",
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
            }

            val presetsSnapshot = userDoc.collection("presets").get().await()
            val restoredPresets = presetsSnapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val id = (data["id"] as? Number)?.toLong() ?: 0L
                val syncId = (data["syncId"] as? String).orEmpty().ifBlank { doc.id }
                if (isDeleted("presets", syncId, doc.id)) return@mapNotNull null
                val name = data["name"] as? String ?: return@mapNotNull null
                ModelPreset(
                    id = id,
                    syncId = syncId,
                    workshopId = (data["workshopId"] as? Number)?.toLong() ?: fallbackWorkshopId,
                    workshopSyncId = data["workshopSyncId"] as? String ?: "",
                    name = name,
                    defaultPricePerSet = (data["defaultPricePerSet"] as? Number)?.toLong() ?: 2000000L,
                    defaultUnitsPerSet = (data["defaultUnitsPerSet"] as? Number)?.toDouble() ?: 6.0,
                    colorCode = data["colorCode"] as? String ?: "#2563EB",
                    description = data["description"] as? String ?: ""
                )
            }

            val rulesSnapshot = userDoc.collection("unitRules").get().await()
            val restoredRules = rulesSnapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val id = (data["id"] as? Number)?.toLong() ?: 0L
                val syncId = (data["syncId"] as? String).orEmpty().ifBlank { doc.id }
                if (isDeleted("unitRules", syncId, doc.id)) return@mapNotNull null
                UnitConversionRule(
                    id = id,
                    syncId = syncId,
                    pieceKey = data["pieceKey"] as? String ?: "",
                    pieceCount = (data["pieceCount"] as? Number)?.toDouble() ?: 0.0,
                    calculatedUnits = (data["calculatedUnits"] as? Number)?.toDouble() ?: 0.0,
                    isEnabled = data["isEnabled"] as? Boolean ?: true
                )
            }

            // Older Firestore data may contain orders/payments/presets without
            // a corresponding workshops document. Without a local workshop those
            // records become invisible because the UI filters cards by activeWorkshopId.
            // Reconstruct the missing workshop identities from the legacy numeric IDs.
            val normalizedWorkshops = restoredWorkshops.toMutableList()
            val referencedWorkshopIds = buildSet {
                restoredOrders.mapTo(this) { it.workshopId }
                restoredPayments.mapTo(this) { it.workshopId }
                restoredPresets.mapTo(this) { it.workshopId }
            }.filter { it > 0L }

            val existingWorkshopIds = normalizedWorkshops.map { it.id }.toSet()
            for (legacyId in referencedWorkshopIds) {
                if (legacyId !in existingWorkshopIds) {
                    normalizedWorkshops += Workshop(
                        id = legacyId,
                        syncId = "legacy_workshop_$legacyId",
                        name = if (legacyId == 1L) "کارگاه اصلی" else "کارگاه $legacyId"
                    )
                }
            }

            // Restore as one atomic local transaction. This prevents half-restored
            // databases and keeps IDs stable so relations such as relatedOrderId work.
            repository.mergeCloudData(
                cloudWorkshops = normalizedWorkshops,
                cloudOrders = restoredOrders,
                cloudPayments = restoredPayments,
                cloudPresets = restoredPresets,
                cloudUnitRules = restoredRules
            )

            Result.success(
                CloudSyncResult(
                    success = true,
                    ordersCount = restoredOrders.size,
                    paymentsCount = restoredPayments.size,
                    presetsCount = restoredPresets.size,
                    unitRulesCount = restoredRules.size
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading from Firestore", e)
            Result.failure(Exception(parseCloudError(e)))
        }
    }

    /**
     * Translates any Firebase Auth / Firestore / network exception into a clear
     * Persian message for the user. Specifically detects the case where Google's
     * servers return an HTTP 403 (Forbidden) block page instead of JSON — this
     * happens for users in Iran connecting without (or with a blocked) VPN, since
     * Google blocks direct access from Iranian IP addresses due to sanctions.
     */
    private fun parseCloudError(e: Exception): String {
        val msg = e.message.orEmpty()
        val lowerMsg = msg.lowercase()

        // Authentication errors must be handled before generic HTTP/403 parsing.
        // Firebase can return a generic credentials error, and a generic 403 message
        // must never be shown as a VPN problem when the actual issue is the login data.
        if (e is FirebaseAuthException) {
            return when (e.errorCode.lowercase()) {
                "errorinvalidemail", "error_invalid_email", "invalid-email" ->
                    "فرمت آدرس ایمیل وارد شده صحیح نیست."

                "errorwrongpassword", "error_wrong_password", "errorinvalidcredential", "error_invalid_credential", "wrong-password", "invalid-credential", "invalid-credentials" ->
                    "ایمیل یا کلمه عبور اشتباه است."

                "errorusernotfound", "error_user_not_found", "user-not-found" ->
                    "حسابی با این ایمیل یافت نشد."

                "erroruserdisabled", "error_user_disabled", "user-disabled" ->
                    "این حساب کاربری غیرفعال شده است. با پشتیبانی تماس بگیرید."

                "erroremailalreadyinuse", "error_email_already_in_use", "email-already-in-use" ->
                    "این ایمیل قبلاً ثبت‌نام شده است."

                "errorweakpassword", "error_weak_password", "weak-password" ->
                    "کلمه عبور انتخابی ضعیف است. حداقل ۶ کاراکتر وارد کنید."

                "errortoomanyrequests", "error_too_many_requests", "too-many-requests" ->
                    "تعداد تلاش‌های ناموفق زیاد است. لطفاً چند دقیقه بعد دوباره تلاش کنید."

                "erroroperationnotallowed", "error_operation_not_allowed", "operation-not-allowed" ->
                    "ورود با ایمیل و کلمه عبور در سرویس احراز هویت فعال نیست."

                "errornetworkrequestfailed", "error_network_request_failed", "network-request-failed" ->
                    "ارتباط با سرویس احراز هویت برقرار نشد. اینترنت را بررسی کنید و دوباره تلاش کنید."

                else -> {
                    when {
                        lowerMsg.contains("badly formatted") ->
                            "فرمت آدرس ایمیل وارد شده صحیح نیست."
                        lowerMsg.contains("wrong password") ||
                            lowerMsg.contains("invalid credential") ||
                            lowerMsg.contains("invalid_login_credentials") ->
                            "ایمیل یا کلمه عبور اشتباه است."
                        else ->
                            "ورود به حساب انجام نشد. اطلاعات واردشده و اتصال اینترنت را بررسی کنید."
                    }
                }
            }
        }

        if (e is FirebaseTooManyRequestsException) {
            return "تعداد درخواست‌ها زیاد است. لطفاً چند دقیقه بعد دوباره تلاش کنید."
        }

        if (e is FirebaseNetworkException) {
            return "ارتباط با سرور برقرار نشد. اتصال اینترنت را بررسی کنید و دوباره تلاش نمایید."
        }

        return when {
            lowerMsg.contains("403") ||
                lowerMsg.contains("forbidden") ||
                lowerMsg.contains("json conversion failed") ||
                lowerMsg.contains("failed to parse") ->
                "دسترسی به سرویس گوگل برقرار نشد (خطای ۴۰۳). این خطا مربوط به دسترسی شبکه است، نه ایمیل یا رمز عبور. اتصال اینترنت را بررسی کنید و در صورت نیاز از مسیر شبکه مجاز خود استفاده نمایید."

            lowerMsg.contains("api key not valid") || lowerMsg.contains("api key expired") ->
                "کلید ارتباطی برنامه با سرویس نامعتبر است. لطفاً از آخرین نسخه برنامه استفاده کنید یا با پشتیبانی تماس بگیرید."

            lowerMsg.contains("permission_denied") || lowerMsg.contains("permission denied") ->
                "دسترسی لازم برای این عملیات وجود ندارد. لطفاً دوباره وارد حساب کاربری خود شوید."

            lowerMsg.contains("network-request-failed") ||
                lowerMsg.contains("unable to resolve host") ||
                lowerMsg.contains("timeout") ||
                lowerMsg.contains("failed to connect") ->
                "خطای اتصال به اینترنت. اتصال اینترنت را بررسی کنید و دوباره تلاش نمایید."

            else ->
                "خطایی در ارتباط با سرویس ابری رخ داد. لطفاً اتصال اینترنت را بررسی کرده و دوباره تلاش کنید."
        }
    }
}
