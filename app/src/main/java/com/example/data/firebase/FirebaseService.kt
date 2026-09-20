package com.example.data.firebase

import android.content.Context
import android.util.Log
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
                    Log.d(TAG, "Firebase initialized with explicit options")
                } else {
                    Log.d(TAG, "Firebase initialized with default app")
                }
            } else {
                Log.d(TAG, "Firebase already initialized with ${apps.size} apps")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase: ${e.message}", e)
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
                Log.e(TAG, "Firebase fallback initialization failed: ${ex.message}", ex)
            }
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
                    .set(mapOf("uid" to user.uid, "email" to (user.email ?: email)))
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
            for (doc in workshopsSnapshot.documents) {
                val data = doc.data ?: continue
                val workshop = Workshop(
                    id = 0L,
                    name = data["name"] as? String ?: "",
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
                repository.saveWorkshop(workshop)
                workshopCount++
            }

            // 1. Download Orders
            val ordersSnapshot = userDoc.collection("orders").get().await()
            var ordCount = 0
            for (doc in ordersSnapshot.documents) {
                val data = doc.data ?: continue
                val order = FurnitureOrder(
                    id = 0L, // fresh auto-generated id or merge
                    workshopId = (data["workshopId"] as? Number)?.toLong() ?: 1L,
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
                val payment = PaymentRecord(
                    id = 0L,
                    workshopId = (data["workshopId"] as? Number)?.toLong() ?: 1L,
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
                val name = data["name"] as? String ?: continue
                val preset = ModelPreset(
                    id = 0L,
                    workshopId = (data["workshopId"] as? Number)?.toLong() ?: 1L,
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
                    unitRulesCount = ruleCount
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
                "errorinvalidemail", "invalid-email" ->
                    "فرمت آدرس ایمیل وارد شده صحیح نیست."

                "errorwrongpassword", "wrong-password", "invalid-credential", "invalid-credentials" ->
                    "ایمیل یا کلمه عبور اشتباه است."

                "errorusernotfound", "user-not-found" ->
                    "حسابی با این ایمیل یافت نشد."

                "erroruserdisabled", "user-disabled" ->
                    "این حساب کاربری غیرفعال شده است. با پشتیبانی تماس بگیرید."

                "erroremailalreadyinuse", "email-already-in-use" ->
                    "این ایمیل قبلاً ثبت‌نام شده است."

                "errorweakpassword", "weak-password" ->
                    "کلمه عبور انتخابی ضعیف است. حداقل ۶ کاراکتر وارد کنید."

                "errortoomanyrequests", "too-many-requests" ->
                    "تعداد تلاش‌های ناموفق زیاد است. لطفاً چند دقیقه بعد دوباره تلاش کنید."

                "erroroperationnotallowed", "operation-not-allowed" ->
                    "ورود با ایمیل و کلمه عبور در سرویس احراز هویت فعال نیست."

                "errornetworkrequestfailed", "network-request-failed" ->
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
