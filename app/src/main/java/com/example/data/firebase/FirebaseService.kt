package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.WorkshopRepository
import com.example.model.FurnitureOrder
import com.example.model.ModelPreset
import com.example.model.PaymentRecord
import com.example.model.UnitConversionRule
import com.example.model.Workshop
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
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
                    Log.e(
                        TAG,
                        "Firebase default configuration is missing. " +
                            "Provide app/google-services.json for local/release builds."
                    )
                } else {
                    Log.d(TAG, "Firebase initialized with default configuration")
                }
            } else {
                Log.d(TAG, "Firebase already initialized with ${apps.size} app(s)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization failed: ${e.message}", e)
        }
    }

    private val auth: FirebaseAuth?
        get() = try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Auth not available: ${e.message}")
            null
        }

    private val firestore: FirebaseFirestore?
        get() = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Firestore not available: ${e.message}")
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

    suspend fun ensureUserProfile(user: FirebaseUserDto) {
        val db = firestore ?: return
        try {
            val ref = db.collection("users").document(user.uid)
            val snapshot = ref.get().await()
            if (!snapshot.exists()) {
                ref.set(
                    mapOf(
                        "username" to (user.displayName?.takeIf { it.isNotBlank() } ?: "کاربر"),
                        "email" to user.email
                    )
                ).await()
            } else {
                ref.set(mapOf("email" to user.email), SetOptions.merge()).await()
            }
        } catch (_: Exception) {
            Log.w(TAG, "Could not ensure user profile")
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<FirebaseUserDto> {
        val fbAuth = auth ?: return Result.failure(Exception("سرویس فایربیس راه‌اندازی نشده است. فایل google-services.json را بررسی کنید."))
        return try {
            val result = fbAuth.signInWithEmailAndPassword(email.trim(), pass).await()
            val user = result.user
            if (user != null) {
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
     * Check if a username is available (not already taken) in Firestore.
     * Usernames are stored (lowercased) as document IDs under "usernames".
     */
    suspend fun isUsernameAvailable(username: String): Boolean {
        val db = firestore ?: return false
        val normalized = username.trim().lowercase()
        return try {
            val doc = db.collection("usernames").document(normalized).get().await()
            !doc.exists()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking username: ${e.message}", e)
            false
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

        // Step 1: Check username availability before creating the account
        val available = try {
            isUsernameAvailable(normalizedUsername)
        } catch (e: Exception) {
            return Result.failure(Exception(parseCloudError(e)))
        }
        if (!available) {
            return Result.failure(Exception("این نام کاربری قبلاً استفاده شده است. لطفاً نام دیگری انتخاب کنید."))
        }

        // Step 2: Create the Firebase Auth account
        return try {
            val result = fbAuth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val user = result.user ?: return Result.failure(Exception("ثبت‌نام ناموفق بود."))

            // Step 3: Reserve the username, linked to this user's uid (best-effort)
            try {
                db.collection("usernames").document(normalizedUsername)
                    .set(mapOf("uid" to user.uid))
                    .await()

                db.collection("users").document(user.uid)
                    .set(
                        mapOf("username" to username.trim(), "email" to (user.email ?: email)),
                        SetOptions.merge()
                    )
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Error reserving username: ${e.message}", e)
                // Not fatal: the auth account was already created successfully
            }

            Result.success(FirebaseUserDto(user.uid, user.email ?: email, username.trim()))
        } catch (e: Exception) {
            Result.failure(Exception(parseCloudError(e)))
        }
    }

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
        workshops: List<Workshop> = emptyList()
    ): Result<CloudSyncResult> {
        val user = auth?.currentUser ?: return Result.failure(Exception("ابتدا باید وارد حساب کاربری خود شوید."))
        val db = firestore ?: return Result.failure(Exception("پایگاه داده ابری Firestore در دسترس نیست."))

        return try {
            val userDoc = db.collection("users").document(user.uid)

            // 0. Workshops upload
            val workshopsCol = userDoc.collection("workshops")
            for (workshop in workshops) {
                val workshopMap = mapOf(
                    "id" to workshop.id,
                    "name" to workshop.name,
                    "createdAt" to workshop.createdAt
                )
                workshopsCol.document("wrk_${workshop.id}").set(workshopMap, SetOptions.merge()).await()
            }

            // 1. Orders batch upload
            val ordersCol = userDoc.collection("orders")
            for (order in orders) {
                val orderMap = mapOf(
                    "id" to order.id,
                    "workshopId" to order.workshopId,
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
                ordersCol.document("ord_${order.id}").set(orderMap, SetOptions.merge()).await()
            }

            // 2. Payments upload
            val paymentsCol = userDoc.collection("payments")
            for (payment in payments) {
                val payMap = mapOf(
                    "id" to payment.id,
                    "workshopId" to payment.workshopId,
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
                    "createdAt" to payment.createdAt
                )
                paymentsCol.document("pay_${payment.id}").set(payMap, SetOptions.merge()).await()
            }

            // 3. Presets upload
            val presetsCol = userDoc.collection("presets")
            for (preset in presets) {
                val presetMap = mapOf(
                    "id" to preset.id,
                    "workshopId" to preset.workshopId,
                    "name" to preset.name,
                    "defaultPricePerSet" to preset.defaultPricePerSet,
                    "defaultUnitsPerSet" to preset.defaultUnitsPerSet,
                    "colorCode" to preset.colorCode,
                    "description" to preset.description
                )
                presetsCol.document("pre_${preset.id}").set(presetMap, SetOptions.merge()).await()
            }

            // 4. Unit Rules upload
            val rulesCol = userDoc.collection("unitRules")
            for (rule in unitRules) {
                val ruleMap = mapOf(
                    "id" to rule.id,
                    "pieceKey" to rule.pieceKey,
                    "pieceCount" to rule.pieceCount,
                    "calculatedUnits" to rule.calculatedUnits,
                    "isEnabled" to rule.isEnabled
                )
                rulesCol.document("rule_${rule.id}").set(ruleMap, SetOptions.merge()).await()
            }

            suspend fun deleteStale(collectionName: String, keepIds: Set<String>) {
                val snapshot = userDoc.collection(collectionName).get().await()
                for (doc in snapshot.documents) {
                    if (doc.id !in keepIds) doc.reference.delete().await()
                }
            }
            deleteStale("workshops", workshops.map { "wrk_" + it.id }.toSet())
            deleteStale("orders", orders.map { "ord_" + it.id }.toSet())
            deleteStale("payments", payments.map { "pay_" + it.id }.toSet())
            deleteStale("presets", presets.map { "pre_" + it.id }.toSet())
            deleteStale("unitRules", unitRules.map { "rule_" + it.id }.toSet())

            // Update user metadata
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
                    ordersCount = orders.size,
                    paymentsCount = payments.size,
                    presetsCount = presets.size,
                    unitRulesCount = unitRules.size
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
        val user = auth?.currentUser ?: return Result.failure(Exception("ابتدا باید وارد حساب کاربری خود شوید."))
        val db = firestore ?: return Result.failure(Exception("پایگاه داده ابری Firestore در دسترس نیست."))

        return try {
            val userDoc = db.collection("users").document(user.uid)

            // 0. Download Workshops
            val workshopsSnapshot = userDoc.collection("workshops").get().await()
            var workshopCount = 0
            val workshopIdMap = mutableMapOf<Long, Long>()
            for (doc in workshopsSnapshot.documents) {
                val data = doc.data ?: continue
                val cloudWorkshopId = (data["id"] as? Number)?.toLong()
                    ?: doc.id.removePrefix("wrk_").toLongOrNull()
                    ?: continue
                val localId = repository.saveWorkshop(
                    Workshop(
                        id = 0L,
                        name = data["name"] as? String ?: "",
                        createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                )
                workshopIdMap[cloudWorkshopId] = localId
                workshopCount++
            }

            // Keep records even if an older cloud backup has no matching workshop document.
            // They are restored into workshopId=0 rather than silently discarded.
            val fallbackWorkshopId = workshopIdMap.values.firstOrNull() ?: 0L
            // 1. Download Orders
            val ordersSnapshot = userDoc.collection("orders").get().await()
            var ordCount = 0
            for (doc in ordersSnapshot.documents) {
                val data = doc.data ?: continue
                val cloudWorkshopId = (data["workshopId"] as? Number)?.toLong() ?: 0L
                val workshopId = workshopIdMap[cloudWorkshopId] ?: fallbackWorkshopId
                val order = FurnitureOrder(
                    id = 0L, // fresh auto-generated id or merge
                    workshopId = workshopId,
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
                repository.saveOrder(order)
                ordCount++
            }

            // 2. Download Payments
            val paymentsSnapshot = userDoc.collection("payments").get().await()
            var payCount = 0
            for (doc in paymentsSnapshot.documents) {
                val data = doc.data ?: continue
                val cloudWorkshopId = (data["workshopId"] as? Number)?.toLong() ?: 0L
                val workshopId = workshopIdMap[cloudWorkshopId] ?: continue
                val payment = PaymentRecord(
                    id = 0L,
                    workshopId = workshopId,
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
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
                repository.savePayment(payment)
                payCount++
            }

            // 3. Download Presets
            val presetsSnapshot = userDoc.collection("presets").get().await()
            var preCount = 0
            for (doc in presetsSnapshot.documents) {
                val data = doc.data ?: continue
                val cloudWorkshopId = (data["workshopId"] as? Number)?.toLong() ?: 0L
                val workshopId = workshopIdMap[cloudWorkshopId] ?: continue
                val name = data["name"] as? String ?: continue
                val preset = ModelPreset(
                    id = 0L,
                    workshopId = workshopId,
                    name = name,
                    defaultPricePerSet = (data["defaultPricePerSet"] as? Number)?.toLong() ?: 2000000L,
                    defaultUnitsPerSet = (data["defaultUnitsPerSet"] as? Number)?.toDouble() ?: 6.0,
                    colorCode = data["colorCode"] as? String ?: "#2563EB",
                    description = data["description"] as? String ?: ""
                )
                repository.savePreset(preset)
                preCount++
            }

            // 4. Download Unit Rules
            val rulesSnapshot = userDoc.collection("unitRules").get().await()
            var ruleCount = 0
            for (doc in rulesSnapshot.documents) {
                val data = doc.data ?: continue
                val pieceKey = data["pieceKey"] as? String ?: ""
                val rule = UnitConversionRule(
                    id = 0L,
                    pieceKey = pieceKey,
                    pieceCount = (data["pieceCount"] as? Number)?.toDouble() ?: 0.0,
                    calculatedUnits = (data["calculatedUnits"] as? Number)?.toDouble() ?: 0.0,
                    isEnabled = data["isEnabled"] as? Boolean ?: true
                )
                repository.saveUnitRule(rule)
                ruleCount++
            }

            Result.success(
                CloudSyncResult(
                    success = true,
                    ordersCount = ordCount,
                    paymentsCount = payCount,
                    presetsCount = preCount,
                    unitRulesCount = ruleCount,
                    workshopsCount = workshopCount
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
        val msg = e.message ?: ""
        val lowerMsg = msg.lowercase()

        return when {
            // Google 403 block page (sanctions-related access block) — most common
            // cause of "JSON conversion failed" / "Error 403 (Forbidden)" errors
            lowerMsg.contains("403") ||
                lowerMsg.contains("forbidden") ||
                lowerMsg.contains("json conversion failed") ||
                lowerMsg.contains("failed to parse") ->
                "دسترسی به سرور گوگل برقرار نشد (خطای ۴۰۳). برای استفاده از ذخیره‌سازی و بازیابی خودکار، لطفاً یک فیلترشکن (VPN) معتبر روشن کنید و دوباره تلاش نمایید."

            lowerMsg.contains("api key not valid") || lowerMsg.contains("api key expired") ->
                "کلید ارتباطی برنامه با سرور نامعتبر است. لطفاً از آخرین نسخه برنامه استفاده کنید یا با پشتیبانی تماس بگیرید."

            lowerMsg.contains("the email address is badly formatted") ->
                "فرمت آدرس ایمیل وارد شده صحیح نمی‌باشد."

            lowerMsg.contains("the password is invalid") || lowerMsg.contains("password should be at least") ->
                "رمز عبور باید حداقل ۶ کاراکتر باشد."

            lowerMsg.contains("there is no user record") || lowerMsg.contains("user-not-found") ->
                "کاربری با این ایمیل یافت نشد."

            lowerMsg.contains("wrong-password") || lowerMsg.contains("invalid_login_credentials") ->
                "ایمیل یا کلمه عبور اشتباه است."

            lowerMsg.contains("email-already-in-use") ->
                "حسابی با این ایمیل قبلاً ثبت‌نام شده است."

            lowerMsg.contains("too-many-requests") ->
                "تعداد تلاش‌های شما زیاد بوده است. لطفاً چند دقیقه دیگر دوباره تلاش کنید."

            lowerMsg.contains("network-request-failed") ||
                lowerMsg.contains("unable to resolve host") ||
                lowerMsg.contains("timeout") ||
                lowerMsg.contains("failed to connect") ->
                "خطای اتصال به اینترنت. لطفاً اتصال اینترنت خود را بررسی کنید و در صورت نیاز فیلترشکن (VPN) را روشن نمایید."

            lowerMsg.contains("permission_denied") || lowerMsg.contains("permission denied") ->
                "دسترسی لازم برای این عملیات وجود ندارد. لطفاً دوباره وارد حساب کاربری خود شوید."

            else -> "خطایی در ارتباط با سرور فایربیس رخ داد. لطفاً از روشن بودن اینترنت و فیلترشکن (VPN) خود اطمینان حاصل کرده و دوباره تلاش کنید."
        }
    }
}
