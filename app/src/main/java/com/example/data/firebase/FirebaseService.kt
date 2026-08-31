package com.example.data.firebase

import android.util.Log
import com.example.data.WorkshopRepository
import com.example.model.FurnitureOrder
import com.example.model.ModelPreset
import com.example.model.PaymentRecord
import com.example.model.UnitConversionRule
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

object FirebaseService {
    private const val TAG = "FirebaseService"

    private val isFirebaseInitialized: Boolean
        get() = try {
            FirebaseApp.getApps(FirebaseApp.getInstance().applicationContext).isNotEmpty()
        } catch (e: Exception) {
            false
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
            Result.failure(Exception(parseFirebaseError(e)))
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
            Result.failure(Exception(parseFirebaseError(e)))
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        val fbAuth = auth ?: return Result.failure(Exception("سرویس فایربیس در دسترس نیست."))
        return try {
            fbAuth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(parseFirebaseError(e)))
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
        unitRules: List<UnitConversionRule>
    ): Result<CloudSyncResult> {
        val user = auth?.currentUser ?: return Result.failure(Exception("ابتدا باید وارد حساب کاربری خود شوید."))
        val db = firestore ?: return Result.failure(Exception("پایگاه داده ابری Firestore در دسترس نیست."))

        return try {
            val userDoc = db.collection("users").document(user.uid)
            
            // 1. Orders batch upload
            val ordersCol = userDoc.collection("orders")
            for (order in orders) {
                val orderMap = mapOf(
                    "id" to order.id,
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
            Result.failure(Exception("خطا در همگام‌سازی با فضای ابری: ${e.localizedMessage}"))
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

            // 1. Download Orders
            val ordersSnapshot = userDoc.collection("orders").get().await()
            var ordCount = 0
            for (doc in ordersSnapshot.documents) {
                val data = doc.data ?: continue
                val order = FurnitureOrder(
                    id = 0L, // fresh auto-generated id or merge
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
            Result.failure(Exception("خطا در بازیابی اطلاعات از فایربیس: ${e.localizedMessage}"))
        }
    }

    private fun parseFirebaseError(e: Exception): String {
        val msg = e.message ?: ""
        return when {
            msg.contains("The email address is badly formatted", ignoreCase = true) ->
                "فرمت آدرس ایمیل وارد شده صحیح نمی‌باشد."
            msg.contains("The password is invalid", ignoreCase = true) || msg.contains("Password should be at least", ignoreCase = true) ->
                "رمز عبور باید حداقل ۶ کاراکتر باشد."
            msg.contains("There is no user record", ignoreCase = true) || msg.contains("user-not-found", ignoreCase = true) ->
                "کاربری با این ایمیل یافت نشد."
            msg.contains("wrong-password", ignoreCase = true) || msg.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ->
                "ایمیل یا کلمه عبور اشتباه است."
            msg.contains("email-already-in-use", ignoreCase = true) ->
                "حسابی با این ایمیل قبلاً ثبت‌نام شده است."
            msg.contains("network-request-failed", ignoreCase = true) || msg.contains("Unable to resolve host", ignoreCase = true) ->
                "خطای اتصال به اینترنت. لطفاً اینترنت یا VPN خود را بررسی فرمایید."
            else -> e.localizedMessage ?: "خطای ناشناخته در احراز هویت رخ داد."
        }
    }
}
