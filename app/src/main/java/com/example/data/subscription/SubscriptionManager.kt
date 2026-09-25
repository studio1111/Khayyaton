package com.example.data.subscription

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.activity.ComponentActivity
import com.example.model.SubscriptionPlan
import com.example.model.SubscriptionStatus
import com.example.model.UserSubscription
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
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
    private const val KEY_TRIAL_STARTED_AT = "sub_trial_started_at"
    private const val KEY_TRIAL_ENDS_AT = "sub_trial_ends_at"
    private const val KEY_TRIAL_USED = "sub_trial_used"
    private const val KEY_SUB_STATUS = "sub_status"
    private const val KEY_ACTIVE_PRODUCT_ID = "sub_active_product_id"
    private const val KEY_EXPIRES_AT = "sub_expires_at"

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
        loadCachedSubscription()

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
    private fun loadCachedSubscription() {
        val sp = prefs ?: return
        val statusStr = sp.getString(KEY_SUB_STATUS, SubscriptionStatus.UNKNOWN.name)
        val loadedStatus = try {
            SubscriptionStatus.valueOf(statusStr ?: SubscriptionStatus.UNKNOWN.name)
        } catch (_: Exception) {
            SubscriptionStatus.UNKNOWN
        }
        // Trial محلی نسخه‌های قدیمی دیگر entitlement معتبر نیست.
        val status = loadedStatus

        val trialStartedAt = sp.getLong(KEY_TRIAL_STARTED_AT, 0L)
        val trialEndsAt = sp.getLong(KEY_TRIAL_ENDS_AT, 0L)
        val trialUsed = sp.getBoolean(KEY_TRIAL_USED, false)
        val activeProductId = sp.getString(KEY_ACTIVE_PRODUCT_ID, null)
        val expiresAt = if (sp.contains(KEY_EXPIRES_AT)) sp.getLong(KEY_EXPIRES_AT, 0L) else null

        _subscriptionState.value = UserSubscription(
            status = status,
            trialStartedAt = trialStartedAt,
            trialEndsAt = trialEndsAt,
            trialUsed = trialUsed,
            activeProductId = activeProductId,
            expiresAt = expiresAt
        )
    }

    private fun cacheSubscription(sub: UserSubscription) {
        prefs?.edit()?.apply {
            putString(KEY_SUB_STATUS, sub.status.name)
            putLong(KEY_TRIAL_STARTED_AT, sub.trialStartedAt)
            putLong(KEY_TRIAL_ENDS_AT, sub.trialEndsAt)
            putBoolean(KEY_TRIAL_USED, sub.trialUsed)
            putString(KEY_ACTIVE_PRODUCT_ID, sub.activeProductId)
            if (sub.expiresAt != null) {
                putLong(KEY_EXPIRES_AT, sub.expiresAt)
            } else {
                remove(KEY_EXPIRES_AT)
            }
            apply()
        }
    }

    /**
     * همگام‌سازی وضعیت اشتراک با فایربیس
     */
    fun syncSubscriptionWithFirebase(onComplete: ((Result<UserSubscription>) -> Unit)? = null) {
        scope.launch {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                // کاربر هنوز وارد نشده است
                onComplete?.invoke(Result.success(_subscriptionState.value))
                return@launch
            }

            try {
                _isLoading.value = true
                val db = FirebaseFirestore.getInstance()
                val subDocRef = db.collection("users").document(user.uid)
                    .collection("subscription").document("info")

                val snapshot = subDocRef.get().await()
                val now = System.currentTimeMillis()

                if (!snapshot.exists()) {
                    val trialStarted = now
                    val trialEnds = now + 3L * 86_400_000L
                    val newSub = UserSubscription(
                        status = SubscriptionStatus.TRIAL_ACTIVE,
                        trialStartedAt = trialStarted,
                        trialEndsAt = trialEnds,
                        trialUsed = true,
                        activeProductId = null,
                        startedAt = null,
                        expiresAt = null,
                        updatedAt = now
                    )

                    val data = hashMapOf(
                        "subscriptionStatus" to SubscriptionStatus.TRIAL_ACTIVE.name,
                        "trialStartedAt" to trialStarted,
                        "trialEndsAt" to trialEnds,
                        "trialUsed" to true,
                        "activeProductId" to null,
                        "startedAt" to null,
                        "expiresAt" to null,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )

                    subDocRef.set(data, SetOptions.merge()).await()
                    _subscriptionState.value = newSub
                    cacheSubscription(newSub)
                    onComplete?.invoke(Result.success(newSub))
                } else {
                    // اشتراک قبلاً ثبت شده است
                    val trialStartedAt = snapshot.getLong("trialStartedAt") ?: 0L
                    val trialEndsAt = snapshot.getLong("trialEndsAt") ?: 0L
                    val trialUsed = snapshot.getBoolean("trialUsed") ?: false
                    val statusStr = snapshot.getString("subscriptionStatus") ?: SubscriptionStatus.UNKNOWN.name
                    val activeProductId = snapshot.getString("activeProductId")
                    val startedAt = snapshot.getLong("startedAt")
                    val expiresAt = snapshot.getLong("expiresAt")
                    val purchaseToken = snapshot.getString("purchaseToken")
                    val orderId = snapshot.getString("orderId")

                    var currentStatus = try {
                        SubscriptionStatus.valueOf(statusStr)
                    } catch (_: Exception) {
                        SubscriptionStatus.UNKNOWN
                    }

                    // دوره آزمایشی محلی نسخه‌های قبلی دیگر entitlement معتبر نیست.
                    if (currentStatus == SubscriptionStatus.TRIAL_ACTIVE) {
                        currentStatus = SubscriptionStatus.TRIAL_EXPIRED
                    }

                    if (currentStatus == SubscriptionStatus.TRIAL_ACTIVE && trialEndsAt > 0L && now >= trialEndsAt) {
                        currentStatus = SubscriptionStatus.TRIAL_EXPIRED
                    }
                    var needUpdateFirestore = currentStatus == SubscriptionStatus.TRIAL_EXPIRED &&
                        statusStr == SubscriptionStatus.TRIAL_ACTIVE.name
                    if (currentStatus == SubscriptionStatus.SUBSCRIBED && expiresAt != null && now > expiresAt) {
                        currentStatus = SubscriptionStatus.EXPIRED
                        needUpdateFirestore = true
                    }

                    if (needUpdateFirestore) {
                        subDocRef.set(
                            mapOf(
                                "subscriptionStatus" to currentStatus.name,
                                "updatedAt" to FieldValue.serverTimestamp()
                            ),
                            SetOptions.merge()
                        ).await()
                    }

                    val updatedSub = UserSubscription(
                        status = currentStatus,
                        trialStartedAt = trialStartedAt,
                        trialEndsAt = trialEndsAt,
                        trialUsed = trialUsed,
                        activeProductId = activeProductId,
                        startedAt = startedAt,
                        expiresAt = expiresAt,
                        purchaseToken = purchaseToken,
                        orderId = orderId,
                        updatedAt = now
                    )

                    _subscriptionState.value = updatedSub
                    cacheSubscription(updatedSub)
                    onComplete?.invoke(Result.success(updatedSub))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing subscription: ${e.message}", e)
                onComplete?.invoke(Result.failure(e))
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * خرید اشتراک توسط درگاه کافه‌بازار (Poolakey)
     */
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
                    val errorMsg = "ارتباط با کافه‌بازار برقرار نشد. لطفاً از نصب بودن و به‌روز بودن کافه‌بازار و اتصال اینترنت اطمینان حاصل کنید."
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
                    val errorMsg = "پرداخت انجام نشد. لطفاً اتصال اینترنت و وضعیت کافه‌بازار را بررسی کنید."
                    Log.e(TAG, errorMsg, throwable)
                    _operationMessage.value = errorMsg
                    onResult(Result.failure(throwable))
                }
            }
        } catch (e: Exception) {
            _isLoading.value = false
            val errorMsg = "خطایی در فرآیند پرداخت رخ داد. لطفاً دوباره تلاش کنید."
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
        onResult: (Result<Unit>) -> Unit,
        extendExisting: Boolean = true
    ) {
        scope.launch {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                withContext(Dispatchers.Main) {
                    onResult(Result.failure(Exception("لطفاً ابتدا وارد حساب کاربری خود شوید.")))
                }
                _isLoading.value = false
                return@launch
            }
            try {
                val now = System.currentTimeMillis()

                if (purchaseInfo.packageName != APP_PACKAGE_NAME ||
                    purchaseInfo.productId != plan.productId ||
                    purchaseInfo.purchaseState != PurchaseState.PURCHASED ||
                    purchaseInfo.payload != "user_" + user.uid
                ) {
                    throw IllegalStateException("اطلاعات خرید کافه‌بازار معتبر نیست.")
                }

                val db = FirebaseFirestore.getInstance()
                val subDocRef = db.collection("users").document(user.uid)
                    .collection("subscription").document("info")
                val existing = subDocRef.get().await()
                val existingToken = existing.getString("purchaseToken")
                val existingOrderId = existing.getString("orderId")

                if (existingToken == purchaseInfo.purchaseToken || existingOrderId == purchaseInfo.orderId) {
                    syncSubscriptionWithFirebase { result ->
                        scope.launch(Dispatchers.Main) {
                            onResult(result.map { Unit })
                        }
                    }
                    _isLoading.value = false
                    return@launch
                }

                val currentExpiry = _subscriptionState.value.expiresAt ?: 0L
                val purchaseTime = purchaseInfo.purchaseTime.takeIf { it > 0L } ?: now
                val durationMillis = plan.durationDays.toLong() * 24 * 60 * 60 * 1000L
                val newExpiry = if (extendExisting && currentExpiry > now) {
                    currentExpiry + durationMillis
                } else {
                    maxOf(currentExpiry, purchaseTime + durationMillis)
                }

                val subData = hashMapOf(
                    "subscriptionStatus" to SubscriptionStatus.SUBSCRIBED.name,
                    "activeProductId" to plan.productId,
                    "startedAt" to purchaseTime,
                    "expiresAt" to newExpiry,
                    "purchaseToken" to purchaseInfo.purchaseToken,
                    "orderId" to purchaseInfo.orderId,
                    "purchaseTime" to purchaseInfo.purchaseTime,
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                subDocRef
                    .set(subData, SetOptions.merge())
                    .await()

                val newSub = _subscriptionState.value.copy(
                    status = SubscriptionStatus.SUBSCRIBED,
                    activeProductId = plan.productId,
                    startedAt = purchaseTime,
                    expiresAt = newExpiry,
                    purchaseToken = purchaseInfo.purchaseToken,
                    orderId = purchaseInfo.orderId,
                    updatedAt = now
                )

                _subscriptionState.value = newSub
                cacheSubscription(newSub)
                _operationMessage.value = "اشتراک با موفقیت فعال شد!"
                withContext(Dispatchers.Main) {
                    onResult(Result.success(Unit))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error recording subscription in Firebase: ${e.message}", e)
                _operationMessage.value = "پرداخت موفق بود اما در ثبت ابری خطایی رخ داد: ${e.message}"
                withContext(Dispatchers.Main) {
                    onResult(Result.failure(e))
                }
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
                            extendExisting = false
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
