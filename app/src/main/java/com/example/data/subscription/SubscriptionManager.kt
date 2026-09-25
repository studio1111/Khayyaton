package com.example.data.subscription

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.activity.ComponentActivity
import com.example.model.SubscriptionPlan
import com.example.model.SubscriptionStatus
import com.example.model.UserSubscription
import com.google.firebase.auth.FirebaseAuth
import ir.cafebazaar.poolakey.Connection
import ir.cafebazaar.poolakey.Payment
import ir.cafebazaar.poolakey.config.PaymentConfiguration
import ir.cafebazaar.poolakey.config.SecurityCheck
import ir.cafebazaar.poolakey.entity.PurchaseInfo
import ir.cafebazaar.poolakey.request.PurchaseRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * مدیریت اشتراک‌های کافه‌بازار و دوره آزمایشی ۳ روزه خیاطان
 *
 * کلید عمومی RSA بازار:
 * TODO: مقدار YOUR_BAZAAR_RSA_PUBLIC_KEY را با کلید RSA دریافت شده از پیشخوان توسعه‌دهندگان کافه‌بازار جایگزین کنید.
 */
object SubscriptionManager {
    private const val TAG = "SubscriptionManager"

    // TODO: کلید RSA اختصاصی برنامه در پیشخوان کافه‌بازار را در اینجا قرار دهید
    const val BAZAAR_RSA_PUBLIC_KEY = "MIHNMA0GCSqGSIb3DQEBAQUAA4G7ADCBtwKBrwCabwVc2p7UqBqZFyMleXiuGT8fHE/obwon3f859+kYRWU5kWqGadTCqEH5JOWALZ7XP0SzynhJ2We24MITaQy0ai6QPEihSgfjYgk5rtpce7ZuB3bwP+4iZcpNKo/HMS+CPRNOPGO87XbZZcDk4DQHgb8vL/PySfLkvu2T7GtPqc6Yicfk/ym2qzb/57ANFP76WiGQHTl/znFKhFj+BSc9wqnldfXMM3SwrNh+YSECAwEAAQ=="

    private const val PREFS_NAME = "khayyaton_prefs"
    private const val KEY_TRIAL_STARTED_AT = "sub_trial_started_at"
    private const val KEY_TRIAL_ENDS_AT = "sub_trial_ends_at"
    private const val KEY_TRIAL_USED = "sub_trial_used"
    private const val KEY_SUB_STATUS = "sub_status"
    private const val KEY_ACTIVE_PRODUCT_ID = "sub_active_product_id"
    private const val KEY_EXPIRES_AT = "sub_expires_at"
    private const val KEY_CACHED_UID = "sub_cached_uid"

    private val _subscriptionState = MutableStateFlow(UserSubscription())
    val subscriptionState: StateFlow<UserSubscription> = _subscriptionState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _operationMessage = MutableStateFlow<String?>(null)
    val operationMessage: StateFlow<String?> = _operationMessage.asStateFlow()

    private val _trialAvailable = MutableStateFlow(false)
    val trialAvailable: StateFlow<Boolean> = _trialAvailable.asStateFlow()

    private val _trialPeriodDays = MutableStateFlow(0)
    val trialPeriodDays: StateFlow<Int> = _trialPeriodDays.asStateFlow()

    private var payment: Payment? = null
    private var paymentConnection: Connection? = null
    private var prefs: SharedPreferences? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun initialize(context: Context) {
        val appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) loadCachedSubscription(uid) else clearCachedUserState()

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

    private fun connectPaymentService() {
        try {
            paymentConnection = payment?.connect {
                connectionSucceed {
                    Log.d(TAG, "Poolakey connected successfully to Cafe Bazaar")
                    checkBazaarTrialAvailability()
                    refreshSubscriptionFromBazaar()
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
     * بررسی دسترسی پرمیوم کاربر (اشتراک فعال یا نسخه آزمایشی فعال)
     */
    fun hasPremiumAccess(): Boolean {
        return _subscriptionState.value.hasAccess
    }

    /**
     * بارگذاری اطلاعات اشتراک ذخیره شده در حافظه محلی
     */
    private fun loadCachedSubscription(uid: String) {
        val sp = prefs ?: return
        val cachedUid = sp.getString(KEY_CACHED_UID, null)
        if (cachedUid != uid) {
            _subscriptionState.value = UserSubscription()
            return
        }

        val statusStr = sp.getString(KEY_SUB_STATUS, SubscriptionStatus.UNKNOWN.name)
        val status = runCatching {
            SubscriptionStatus.valueOf(statusStr ?: SubscriptionStatus.UNKNOWN.name)
        }.getOrDefault(SubscriptionStatus.UNKNOWN)

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
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        prefs?.edit()?.apply {
            putString(KEY_CACHED_UID, uid)
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

    fun clearCachedUserState() {
        prefs?.edit()?.apply {
            remove(KEY_CACHED_UID)
            remove(KEY_SUB_STATUS)
            remove(KEY_TRIAL_STARTED_AT)
            remove(KEY_TRIAL_ENDS_AT)
            remove(KEY_TRIAL_USED)
            remove(KEY_ACTIVE_PRODUCT_ID)
            remove(KEY_EXPIRES_AT)
            apply()
        }
        _subscriptionState.value = UserSubscription()
        _trialAvailable.value = false
        _trialPeriodDays.value = 0
    }

    /**
     * همگام‌سازی اطلاعات اشتراک و نسخه آزمایشی با فایربیس
     */
    fun syncSubscriptionWithFirebase(onComplete: ((Result<UserSubscription>) -> Unit)? = null) {
        scope.launch {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                clearCachedUserState()
                onComplete?.invoke(Result.success(_subscriptionState.value))
                return@launch
            }

            loadCachedSubscription(user.uid)
            _isLoading.value = true
            try {
                // Firestore is intentionally not an entitlement authority.
                // Premium access is based on locally verified Bazaar state.
                refreshSubscriptionFromBazaar()
                onComplete?.invoke(Result.success(_subscriptionState.value))
            } catch (_: Exception) {
                onComplete?.invoke(Result.success(_subscriptionState.value))
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
                    onResult(Result.failure(Exception("برای فعال‌سازی اشتراک ابتدا وارد حساب کاربری شوید.")))
                }
                return@launch
            }

            try {
                // Poolakey's current PurchaseInfo does not expose developerPayload.
                // The purchase is already validated by Poolakey's signed Bazaar flow.
                // Keep the entitlement bound to the currently authenticated app user.
                val now = System.currentTimeMillis()
                val durationMillis = plan.durationDays.toLong() * 24 * 60 * 60 * 1000L
                val purchaseExpiry = purchaseInfo.purchaseTime + durationMillis
                val currentExpiry = _subscriptionState.value.expiresAt ?: 0L
                val baseTime = if (currentExpiry > now) currentExpiry else now
                val newExpiry = if (extendExisting) {
                    baseTime + durationMillis
                } else {
                    maxOf(currentExpiry, purchaseExpiry)
                }

                val newSub = _subscriptionState.value.copy(
                    status = SubscriptionStatus.SUBSCRIBED,
                    activeProductId = plan.productId,
                    startedAt = purchaseInfo.purchaseTime.takeIf { it > 0L } ?: now,
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
                Log.e(TAG, "Error caching Bazaar subscription: " + e.message, e)
                _operationMessage.value = "خرید موفق بود اما ذخیره وضعیت اشتراک انجام نشد."
                withContext(Dispatchers.Main) {
                    onResult(Result.failure(e))
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * بررسی واجدشرایط بودن کاربر برای Trial واقعی کافه‌بازار.
     */
    fun checkBazaarTrialAvailability() {
        val p = payment ?: return
        try {
            p.checkTrialSubscription {
                checkTrialSubscriptionSucceed { info ->
                    _trialAvailable.value = info.isAvailable
                    _trialPeriodDays.value = info.trialPeriodDays
                }
                checkTrialSubscriptionFailed {
                    _trialAvailable.value = false
                    _trialPeriodDays.value = 0
                }
            }
        } catch (e: Exception) {
            _trialAvailable.value = false
            _trialPeriodDays.value = 0
            Log.w(TAG, "Error checking Bazaar trial: " + e.message)
        }
    }

    /**
     * وضعیت اشتراک از خود کافه‌بازار دوباره خوانده می‌شود.
     */
    fun refreshSubscriptionFromBazaar() {
        val p = payment ?: return
        try {
            p.getSubscribedProducts {
                querySucceed { purchases ->
                    val activePurchase = purchases
                        .filter { purchase -> SubscriptionPlan.PLANS.any { it.productId == purchase.productId } }
                        .maxByOrNull { it.purchaseTime }

                    if (activePurchase == null) {
                        val current = _subscriptionState.value
                        if (current.status == SubscriptionStatus.SUBSCRIBED &&
                            (current.expiresAt ?: 0L) <= System.currentTimeMillis()
                        ) {
                            val expired = current.copy(
                                status = SubscriptionStatus.EXPIRED,
                                activeProductId = null,
                                expiresAt = null,
                                updatedAt = System.currentTimeMillis()
                            )
                            _subscriptionState.value = expired
                            cacheSubscription(expired)
                        }
                        return@querySucceed
                    }

                    val plan = SubscriptionPlan.PLANS.firstOrNull { it.productId == activePurchase.productId }
                        ?: return@querySucceed

                    handleSuccessfulSubscription(
                        plan = plan,
                        purchaseInfo = activePurchase,
                        onResult = { result ->
                            if (result.isFailure) Log.w(TAG, "Could not cache Bazaar subscription")
                        },
                        extendExisting = false
                    )
                }
                queryFailed { throwable ->
                    Log.w(TAG, "Bazaar subscription refresh failed: " + throwable.message)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error refreshing Bazaar subscription: " + e.message)
        }
    }

    /**
     * بازیابی خریدهای فعال از کافه‌بازار (Restore Purchases)
     */
    fun restorePurchases(onResult: (Result<Int>) -> Unit) {
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
                        val latestPurchase = purchases
                            .filter { purchase -> SubscriptionPlan.PLANS.any { it.productId == purchase.productId } }
                            .maxByOrNull { it.purchaseTime }
                        if (latestPurchase == null) {
                            _isLoading.value = false
                            onResult(Result.success(0))
                            return@querySucceed
                        }
                        val matchedPlan = SubscriptionPlan.PLANS.first { it.productId == latestPurchase.productId }

                        handleSuccessfulSubscription(matchedPlan, latestPurchase, onResult = { result ->
                            if (result.isSuccess) {
                                _operationMessage.value = "اشتراک قبلی شما با موفقیت بازیابی شد."
                                onResult(Result.success(purchases.size))
                            } else {
                                onResult(Result.failure(result.exceptionOrNull() ?: Exception("خطا در بازیابی اشتراک")))
                            }
                        }, extendExisting = false)
                    }
                }
                queryFailed { throwable ->
                    _isLoading.value = false
                    val errorMsg = "بررسی اشتراک‌های کافه‌بازار انجام نشد. لطفاً اتصال اینترنت و وضعیت کافه‌بازار را بررسی کنید."
                    Log.e(TAG, errorMsg, throwable)
                    _operationMessage.value = errorMsg
                    onResult(Result.failure(throwable))
                }
            }
        } catch (e: Exception) {
            _isLoading.value = false
            val errorMsg = "بازیابی اشتراک انجام نشد. لطفاً دوباره تلاش کنید."
            Log.e(TAG, errorMsg, e)
            _operationMessage.value = errorMsg
            onResult(Result.failure(e))
        }
    }
}
