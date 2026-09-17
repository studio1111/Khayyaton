package com.example.model

enum class SubscriptionStatus(val titleFa: String) {
    TRIAL_ACTIVE("نسخه آزمایشی فعال"),
    TRIAL_EXPIRED("پایان دوره آزمایشی"),
    SUBSCRIBED("اشتراک ویژه فعال"),
    EXPIRED("اشتراک منقضی شده"),
    UNKNOWN("در حال بررسی")
}

data class SubscriptionPlan(
    val productId: String,
    val titleFa: String,
    val durationDays: Int,
    val priceTomans: Long,
    val priceFormatted: String,
    val tagFa: String? = null
) {
    companion object {
        val PLANS = listOf(
            SubscriptionPlan(
                productId = "khayyaton_3_month",
                titleFa = "اشتراک ۳ ماهه",
                durationDays = 90,
                priceTomans = 50_000L,
                priceFormatted = "۵۰,۰۰۰ تومان",
                tagFa = null
            ),
            SubscriptionPlan(
                productId = "khayyaton_6_month",
                titleFa = "اشتراک ۶ ماهه",
                durationDays = 180,
                priceTomans = 90_000L,
                priceFormatted = "۹۰,۰۰۰ تومان",
                tagFa = "محبوب‌ترین (۱۰٪ تخفیف)"
            ),
            SubscriptionPlan(
                productId = "khayyaton_1_year",
                titleFa = "اشتراک ۱ ساله",
                durationDays = 365,
                priceTomans = 150_000L,
                priceFormatted = "۱۵۰,۰۰۰ تومان",
                tagFa = "بیشترین صرفه (۲۵٪ تخفیف)"
            )
        )
    }
}

data class UserSubscription(
    val status: SubscriptionStatus = SubscriptionStatus.UNKNOWN,
    val trialStartedAt: Long = 0L,
    val trialEndsAt: Long = 0L,
    val trialUsed: Boolean = false,
    val activeProductId: String? = null,
    val startedAt: Long? = null,
    val expiresAt: Long? = null,
    val purchaseToken: String? = null,
    val orderId: String? = null,
    val updatedAt: Long = 0L
) {
    val hasAccess: Boolean
        get() {
            val now = System.currentTimeMillis()
            return when (status) {
                SubscriptionStatus.SUBSCRIBED -> expiresAt == null || expiresAt > now
                SubscriptionStatus.TRIAL_ACTIVE -> trialEndsAt > now
                SubscriptionStatus.TRIAL_EXPIRED,
                SubscriptionStatus.EXPIRED -> false
                SubscriptionStatus.UNKNOWN -> true // Graceful access while fetching
            }
        }

    val remainingDays: Long
        get() {
            val now = System.currentTimeMillis()
            val target = when (status) {
                SubscriptionStatus.SUBSCRIBED -> expiresAt ?: 0L
                SubscriptionStatus.TRIAL_ACTIVE -> trialEndsAt
                else -> 0L
            }
            val diff = target - now
            return if (diff > 0) (diff / (24 * 60 * 60 * 1000L)) else 0L
        }

    val remainingHours: Long
        get() {
            val now = System.currentTimeMillis()
            val target = when (status) {
                SubscriptionStatus.SUBSCRIBED -> expiresAt ?: 0L
                SubscriptionStatus.TRIAL_ACTIVE -> trialEndsAt
                else -> 0L
            }
            val diff = target - now
            return if (diff > 0) ((diff % (24 * 60 * 60 * 1000L)) / (60 * 60 * 1000L)) else 0L
        }
}
