package com.example.data.subscription

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.activity.ComponentActivity
import com.example.model.SubscriptionPlan
import com.example.model.SubscriptionStatus
import com.example.model.UserSubscription
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import ir.cafebazaar.poolakey.Connection
import ir.cafebazaar.poolakey.Payment
import ir.cafebazaar.poolakey.config.PaymentConfiguration
import ir.cafebazaar.poolakey.config.SecurityCheck
import ir.cafebazaar.poolakey.entity.PurchaseInfo
import ir.cafebazaar.poolakey.entity.PurchaseState
import ir.cafebazaar.poolakey.entity.SkuDetails
import ir.cafebazaar.poolakey.request.PurchaseRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * مدیریت اشتراک‌های کافه‌بازار خیاطان
 *
 * کلید عمومی RSA اختصاصی برنامه از پیشخوان توسعه‌دهندگان کافه‌بازار.
 */
object SubscriptionManager {
    private const val TAG = "SubscriptionManager"
    private const val APP_PACKAGE_NAME = "com.farsinnov.khayyaton"

    // کلید RSA اختصاصی برنامه در پیشخوان کافه‌بازار
    const val BAZAAR_RSA_PUBLIC_KEY = "MIHNMA0GCSqGSIb3DQEBAQUAA4G7ADCBtwKBrwCabwVc2p7UqBqZFyMleXiuGT8fHE/obwon3f859+kYRWU5kWqGadTCqEH5JOWALZ7XP0SzynhJ2We24MITaQy0ai6QPEihSgfjYgk5rtpce7ZuB3bwP+4iZcpNKo/HMS+CPRNOPGO87XbZZcDk4DQHgb8vL/PySfLkvu2T7GtPqc6Yicfk/ym2qzb/57ANFP76WiGQHTl/znFKhFj+BSc9wqnldfXMM3SwrNh+YSECAwEAAQ=="

    private const val PREFS_NAME = "khayyaton_prefs"
    private const val KEY_UID = "sub_uid"
    private const val KEY_SUB_STATUS = "sub_status"
    private const val KEY_ACTIVE_PRODUCT_ID = "sub_active_product_id"
    private const val KEY_EXPIRES_AT = "sub_expires_at"
    private const val KEY_STARTED_AT = "sub_started_at"
    private const val KEY_ORDER_ID = "sub_order_id"
    private const val KEY_AUTO_RENEWING = "sub_auto_renewing"

    private val _subscriptionState = MutableStateFlow(UserSubscription())
    val subscriptionState: StateFlow<UserSubscription> = _subscriptionState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _operationMessage = MutableStateFlow<String?>(null)
    val operationMessage: StateFlow<String?> = _operationMessage.asStateFlow()

    private val _skuDetails = MutableStateFlow<Map<String, SkuDetails>>(emptyMap())
    val skuDetails: StateFlow<Map<String, SkuDetails>> = _skuDetails.asStateFlow()

    private var payment: Payment? = null
    private var paymentConnection: Connection? = null
    private var prefs: SharedPreferences? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun initialize(context: Context) {
        val appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentUid = runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull()
        resetSubscriptionForUser(currentUid)


        // راه‌اندازی پل پرداخت پولکی کافه‌بازار
        try {
            val securityCheck = if (BAZAAR_RSA_PUBLIC_KEY.isNotBlank() && BAZAAR_RSA_PUBLIC_KEY != "YOUR_BAZAAR_RSA_PUBLIC_KEY") {
                SecurityCheck.Enable(rsaPublicKey = BAZAAR_RSA_PUBLIC_KEY)
            } else {
                Log.w(TAG, "Bazaar RSA Public Key is placeholder, using SecurityCheck.Disable for development mode.")
                SecurityCheck.Disable
            }

            val paymentConfig = PaymentConfiguration(
                localSecurityCheck = securityCheck,
                shouldSupportSubscription = true
            )
            payment = Payment(context = appContext, config = paymentConfig)
            connectPaymentService()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Poolakey: ${e.message}", e)
        }

        // بارگذاری اولیه اشتراک از فایربیس
        syncSubscriptionWithFirebase()
    }

    private fun loadSubscriptionProducts() {
        val p = payment ?: return
        try {
            p.getSubscriptionSkuDetails(SubscriptionPlan.PLANS.map { it.productId }) {
                getSkuDetailsSucceed { details: List<SkuDetails> ->
                    _skuDetails.value = details.associateBy { it.sku }
                    Log.d(TAG, "Loaded " + details.size + " Bazaar subscription products")
                }
                getSkuDetailsFailed { throwable ->
                    Log.w(TAG, "Could not load Bazaar subscription details: " + throwable.message)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error loading Bazaar subscription details: " + e.message)
        }
    }

    fun getBazaarPrice(productId: String): String? = _skuDetails.value[productId]?.price

    fun getBazaarTitle(productId: String): String? = _skuDetails.value[productId]?.title

    private fun connectPaymentService() {
        try {
            paymentConnection = payment?.connect {
                connectionSucceed {
                    Log.d(TAG, "Poolakey connected successfully to Cafe Bazaar")
                    loadSubscriptionProducts()
                }
                connectionFailed { throwable ->
                    Log.w(TAG, "Poolakey connection failed: ${throwable.message}")
                }
                disconnected {
                    Log.d(TAG, "Poolakey disconnected from Cafe Bazaar")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to Poolakey: ${e.message}", e)
        }
    }

    fun disconnect() {
        try {
            paymentConnection?.disconnect()
            paymentConnection = null
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting Poolakey: ${e.message}", e)
        }
    }

    fun clearMessage() {
        _operationMessage.value = null
    }

    /**
     * بررسی دسترسی پرمیوم کاربر. فقط اشتراک معتبر بازار دسترسی می‌دهد.
     */
    fun hasPremiumAccess(): Boolean {
        return _subscriptionState.value.hasAccess
    }

    /**
     * بارگذاری اطلاعات اشتراک ذخیره شده در حافظه محلی
     */
    private fun resetSubscriptionForUser(uid: String?) {
        val sp = prefs ?: return
        val cachedUid = sp.getString(KEY_UID, null)
        if (uid == null || cachedUid != uid) {
            _subscriptionState.value = UserSubscription()
            if (uid == null) sp.edit().clear().apply()
            return
        }
        val status = runCatching {
            SubscriptionStatus.valueOf(
                sp.getString(KEY_SUB_STATUS, SubscriptionStatus.UNKNOWN.name)
                    ?: SubscriptionStatus.UNKNOWN.name
            )
        }.getOrDefault(SubscriptionStatus.UNKNOWN)
        val expiresAt = if (sp.contains(KEY_EXPIRES_AT)) sp.getLong(KEY_EXPIRES_AT, 0L) else null
        val safeStatus = if (status == SubscriptionStatus.SUBSCRIBED && (expiresAt == null || expiresAt <= System.currentTimeMillis()))
            SubscriptionStatus.EXPIRED else status
        _subscriptionState.value = UserSubscription(
            status = safeStatus,
            activeProductId = sp.getString(KEY_ACTIVE_PRODUCT_ID, null),
            startedAt = sp.getLong(KEY_STARTED_AT, 0L).takeIf { it > 0 },
            expiresAt = expiresAt,
            orderId = sp.getString(KEY_ORDER_ID, null),
            updatedAt = System.currentTimeMillis(),
            autoRenewing = sp.getBoolean(KEY_AUTO_RENEWING, false)
        )
    }

    private fun cacheSubscription(sub: UserSubscription, uid: String) {
        prefs?.edit()?.apply {
            putString(KEY_UID, uid)
            putString(KEY_SUB_STATUS, sub.status.name)
            putString(KEY_ACTIVE_PRODUCT_ID, sub.activeProductId)
            if (sub.expiresAt != null) putLong(KEY_EXPIRES_AT, sub.expiresAt) else remove(KEY_EXPIRES_AT)
            if (sub.startedAt != null) putLong(KEY_STARTED_AT, sub.startedAt) else remove(KEY_STARTED_AT)
            if (sub.orderId != null) putString(KEY_ORDER_ID, sub.orderId) else remove(KEY_ORDER_ID)
            putBoolean(KEY_AUTO_RENEWING, sub.autoRenewing)
            apply()
        }
    }

    fun clearForSignedOutUser() {
        _subscriptionState.value = UserSubscription()
        _operationMessage.value = null
        prefs?.edit()?.clear()?.apply()
    }

        /**
     * همگام‌سازی وضعیت اشتراک با فایربیس
     */
    fun syncSubscriptionWithFirebase(onComplete: ((Result<UserSubscription>) -> Unit)? = null) {
        scope.launch {
            _isLoading.value = true
            try {
                val user = FirebaseAuth.getInstance().currentUser
                if (user == null) {
                    _subscriptionState.value = UserSubscription()
                    withContext(Dispatchers.Main) { onComplete?.invoke(Result.success(UserSubscription())) }
                    return@launch
                }
                resetSubscriptionForUser(user.uid)
                val snapshot = FirebaseFirestore.getInstance()
                    .collection("users").document(user.uid)
                    .collection("subscription").document("info")
                    .get().await()

                if (!snapshot.exists()) {
                    val empty = UserSubscription()
                    _subscriptionState.value = empty
                    cacheSubscription(empty, user.uid)
                    withContext(Dispatchers.Main) { onComplete?.invoke(Result.success(empty)) }
                    return@launch
                }

                val expiresAt = snapshot.getTimestamp("expiresAt")?.toDate()?.time
                    ?: snapshot.getLong("expiresAt")
                val startedAt = snapshot.getTimestamp("startedAt")?.toDate()?.time
                    ?: snapshot.getLong("startedAt")
                val statusFromServer = runCatching {
                    SubscriptionStatus.valueOf(
                        snapshot.getString("subscriptionStatus") ?: SubscriptionStatus.UNKNOWN.name
                    )
                }.getOrDefault(SubscriptionStatus.UNKNOWN)
                val status = if (statusFromServer == SubscriptionStatus.SUBSCRIBED &&
                    (expiresAt == null || expiresAt <= System.currentTimeMillis())
                ) SubscriptionStatus.EXPIRED else statusFromServer

                val sub = UserSubscription(
                    status = status,
                    activeProductId = snapshot.getString("activeProductId"),
                    startedAt = startedAt,
                    expiresAt = expiresAt,
                    orderId = snapshot.getString("orderId"),
                    updatedAt = System.currentTimeMillis(),
                    autoRenewing = snapshot.getBoolean("autoRenewing") ?: false
                )
                _subscriptionState.value = sub
                cacheSubscription(sub, user.uid)
                withContext(Dispatchers.Main) { onComplete?.invoke(Result.success(sub)) }
            } catch (e: Exception) {
                Log.e(TAG, "Subscription sync failed", e)
                withContext(Dispatchers.Main) { onComplete?.invoke(Result.failure(e)) }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun purchaseSubscription(
        activity: ComponentActivity,
        plan: SubscriptionPlan,
        onResult: (Result<Unit>) -> Unit
    ) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            val msg = "لطفاً ابتدا وارد حساب کاربری خود شوید."
            _operationMessage.value = msg
            onResult(Result.failure(Exception(msg)))
            return
        }

        val p = payment
        if (p == null) {
            val msg = "سرویس پرداخت کافه‌بازار در دسترس نیست. لطفاً نصب بودن کافه‌بازار را بررسی کنید."
            _operationMessage.value = msg
            onResult(Result.failure(Exception(msg)))
            return
        }

        _isLoading.value = true
        _operationMessage.value = "در حال اتصال به کافه‌بازار..."

        try {
            val request = PurchaseRequest(
                productId = plan.productId,
                payload = "user_${user.uid}"
            )

            p.subscribeProduct(
                registry = activity.activityResultRegistry,
                request = request
            ) {
                purchaseFlowBegan {
                    Log.d(TAG, "Purchase flow started for ${plan.productId}")
                    _operationMessage.value = "در حال باز کردن صفحه پرداخت کافه‌بازار..."
                }
                failedToBeginFlow { throwable ->
                    _isLoading.value = false
                    val errorMsg = "خطا در برقراری ارتباط با کافه‌بازار: ${throwable.message}"
                    Log.e(TAG, errorMsg, throwable)
                    _operationMessage.value = errorMsg
                    onResult(Result.failure(throwable))
                }
                purchaseSucceed { purchaseInfo: PurchaseInfo ->
                    Log.d(TAG, "Purchase succeeded: ${purchaseInfo.orderId}")
                    handleSuccessfulSubscription(plan, purchaseInfo, onResult)
                }
                purchaseCanceled {
                    _isLoading.value = false
                    val msg = "پرداخت توسط کاربر لغو شد."
                    Log.d(TAG, msg)
                    _operationMessage.value = msg
                    onResult(Result.failure(Exception(msg)))
                }
                purchaseFailed { throwable ->
                    _isLoading.value = false
                    val errorMsg = "پرداخت انجام نشد: ${throwable.message}"
                    Log.e(TAG, errorMsg, throwable)
                    _operationMessage.value = errorMsg
                    onResult(Result.failure(throwable))
                }
            }
        } catch (e: Exception) {
            _isLoading.value = false
            val errorMsg = "خطای پرداخت: ${e.message}"
            Log.e(TAG, errorMsg, e)
            _operationMessage.value = errorMsg
            onResult(Result.failure(e))
        }
    }

    /**
     * ثبت و اعمال اشتراک خریداری شده در فایربیس و حافظه محلی
     */
    private fun handleSuccessfulSubscription(
        plan: SubscriptionPlan,
        purchaseInfo: PurchaseInfo,
        onResult: (Result<Unit>) -> Unit
    ) {
        scope.launch {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                withContext(Dispatchers.Main) { onResult(Result.failure(Exception("لطفاً ابتدا وارد حساب کاربری خود شوید."))) }
                return@launch
            }
            try {
                if (purchaseInfo.packageName != APP_PACKAGE_NAME ||
                    purchaseInfo.productId != plan.productId ||
                    purchaseInfo.purchaseState != PurchaseState.PURCHASED ||
                    purchaseInfo.payload != "user_${user.uid}" ||
                    purchaseInfo.purchaseToken.isBlank()
                ) throw IllegalStateException("رسید خرید کافه‌بازار معتبر نیست.")

                val options = HttpsCallableOptions.Builder()
                    .setLimitedUseAppCheckTokens(true)
                    .build()
                val callable = FirebaseFunctions.getInstance("europe-west1")
                    .getHttpsCallable("verifyBazaarSubscription", options)

                val response = callable.call(
                    mapOf(
                        "packageName" to purchaseInfo.packageName,
                        "productId" to purchaseInfo.productId,
                        "purchaseToken" to purchaseInfo.purchaseToken,
                        "orderId" to purchaseInfo.orderId,
                        "purchaseTime" to purchaseInfo.purchaseTime,
                        "payload" to purchaseInfo.payload
                    )
                ).await()

                val data = response.data as? Map<*, *>
                    ?: throw IllegalStateException("پاسخ تأیید اشتراک نامعتبر است.")
                val expiresAt = (data["expiresAt"] as? Number)?.toLong()
                    ?: throw IllegalStateException("تاریخ انقضای تأییدشده دریافت نشد.")
                val startedAt = (data["startedAt"] as? Number)?.toLong()
                val status = SubscriptionStatus.SUBSCRIBED
                val sub = UserSubscription(
                    status = status,
                    activeProductId = data["activeProductId"]?.toString(),
                    startedAt = startedAt,
                    expiresAt = expiresAt,
                    orderId = data["orderId"]?.toString(),
                    updatedAt = System.currentTimeMillis(),
                    autoRenewing = data["autoRenewing"] as? Boolean ?: false
                )
                _subscriptionState.value = sub
                cacheSubscription(sub, user.uid)
                _operationMessage.value = "اشتراک با موفقیت تأیید و فعال شد."
                withContext(Dispatchers.Main) { onResult(Result.success(Unit)) }
            } catch (e: Exception) {
                Log.e(TAG, "Secure Bazaar verification failed", e)
                _operationMessage.value = "خرید انجام شد اما تأیید امن اشتراک ناموفق بود. لطفاً بازیابی اشتراک را امتحان کنید."
                withContext(Dispatchers.Main) { onResult(Result.failure(e)) }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * بازیابی خریدهای فعال از کافه‌بازار (Restore Purchases)
     */
    fun restorePurchases(onResult: (Result<Int>) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            val msg = "لطفاً ابتدا وارد حساب کاربری خود شوید."
            _operationMessage.value = msg
            onResult(Result.failure(Exception(msg)))
            return
        }

        val p = payment
        if (p == null) {
            val msg = "سرویس کافه‌بازار در دسترس نیست."
            _operationMessage.value = msg
            onResult(Result.failure(Exception(msg)))
            return
        }

        _isLoading.value = true
        _operationMessage.value = "در حال بررسی خریدهای قبلی در کافه‌بازار..."

        try {
            p.getSubscribedProducts {
                querySucceed { purchases: List<PurchaseInfo> ->
                    if (purchases.isEmpty()) {
                        _isLoading.value = false
                        val msg = "هیچ اشتراک فعالی در حساب کافه‌بازار شما یافت نشد."
                        _operationMessage.value = msg
                        onResult(Result.success(0))
                    } else {
                        val validPurchases = purchases
                            .filter { it.packageName == APP_PACKAGE_NAME }
                            .filter { it.purchaseState == PurchaseState.PURCHASED }
                            .filter { it.payload == "user_" + user.uid }
                            .mapNotNull { purchase ->
                                SubscriptionPlan.PLANS
                                    .find { it.productId == purchase.productId }
                                    ?.let { plan -> plan to purchase }
                            }

                        if (validPurchases.isEmpty()) {
                            _isLoading.value = false
                            val msg = "هیچ اشتراک فعال و معتبر کافه‌بازار برای این برنامه یافت نشد."
                            _operationMessage.value = msg
                            onResult(Result.success(0))
                            return@querySucceed
                        }

                        // بر اساس زمان خرید انتخاب می‌کنیم، نه ترتیب لیست برگشتی بازار.
                        val (matchedPlan, latestPurchase) = validPurchases.maxByOrNull { it.second.purchaseTime }!!

                        // Restore نباید هر بار اشتراک را دوباره تمدید کند.
                        handleSuccessfulSubscription(
                            plan = matchedPlan,
                            purchaseInfo = latestPurchase,
                            onResult = { result ->
                                if (result.isSuccess) {
                                    _operationMessage.value = "اشتراک قبلی شما با موفقیت بازیابی شد."
                                    onResult(Result.success(validPurchases.size))
                                } else {
                                    onResult(Result.failure(result.exceptionOrNull() ?: Exception("خطا در بازیابی اشتراک")))
                                }
                            },
                        )
                    }
                }
                queryFailed { throwable ->
                    _isLoading.value = false
                    val errorMsg = "خطا در بررسی اشتراک‌های کافه‌بازار: ${throwable.message}"
                    Log.e(TAG, errorMsg, throwable)
                    _operationMessage.value = errorMsg
                    onResult(Result.failure(throwable))
                }
            }
        } catch (e: Exception) {
            _isLoading.value = false
            val errorMsg = "خطای بازیابی اشتراک: ${e.message}"
            Log.e(TAG, errorMsg, e)
            _operationMessage.value = errorMsg
            onResult(Result.failure(e))
        }
    }
}
