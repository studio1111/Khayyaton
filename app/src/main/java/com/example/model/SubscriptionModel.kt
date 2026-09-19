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
            SubscriptionPlan("khayyaton_3_month", "اشتراک ۳ ماهه", 90, 50_000L, "۵۰,۰۰۰ تومان"),
            SubscriptionPlan("khayyaton_6_month", "اشتراک ۶ ماهه", 180, 90_000L, "۹۰,۰۰۰ تومان", "محبوب‌ترین (۱۰٪ تخفیف)"),
            SubscriptionPlan("khayyaton_1_year", "اشتراک ۱ ساله", 365, 150_000L, "۱۵۰,۰۰۰ تومان", "بیشترین صرفه (۲۵٪ تخفیف)")
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
                SubscriptionStatus.SUBSCRIBED -> expiresAt != null && expiresAt > now
                SubscriptionStatus.TRIAL_ACTIVE -> trialEndsAt > now
                SubscriptionStatus.TRIAL_EXPIRED,
                SubscriptionStatus.EXPIRED,
                SubscriptionStatus.UNKNOWN -> false
            }
        }

    val remainingDays: Long
        get() {
            val target = when (status) {
                SubscriptionStatus.SUBSCRIBED -> expiresAt ?: 0L
                SubscriptionStatus.TRIAL_ACTIVE -> trialEndsAt
                else -> 0L
            }
            val diff = target - System.currentTimeMillis()
            return if (diff > 0) diff / 86_400_000L else 0L
        }

    val remainingHours: Long
        get() {
            val target = when (status) {
                SubscriptionStatus.SUBSCRIBED -> expiresAt ?: 0L
                SubscriptionStatus.TRIAL_ACTIVE -> trialEndsAt
                else -> 0L
            }
            val diff = target - System.currentTimeMillis()
            return if (diff > 0) (diff % 86_400_000L) / 3_600_000L else 0L
        }
}
