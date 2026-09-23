package com.example.model

enum class SubscriptionStatus(val titleFa: String) {
    SUBSCRIBED("اشتراک ویژه فعال"),
    EXPIRED("اشتراک منقضی شده"),
    UNKNOWN("اشتراک فعال نیست")
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
            SubscriptionPlan("khayyaton_3_month", "اشتراک ۳ ماهه", 90, 55_000L, "۵۵,۰۰۰ تومان"),
            SubscriptionPlan("khayyaton_6_month", "اشتراک ۶ ماهه", 180, 90_000L, "۹۰,۰۰۰ تومان", "محبوب‌ترین"),
            SubscriptionPlan("khayyaton_1_year", "اشتراک ۱ ساله", 365, 150_000L, "۱۵۰,۰۰۰ تومان", "بیشترین صرفه")
        )
    }
}

data class UserSubscription(
    val status: SubscriptionStatus = SubscriptionStatus.UNKNOWN,
    val activeProductId: String? = null,
    val startedAt: Long? = null,
    val expiresAt: Long? = null,
    val orderId: String? = null,
    val updatedAt: Long = 0L,
    val autoRenewing: Boolean = false
) {
    val hasAccess: Boolean
        get() = status == SubscriptionStatus.SUBSCRIBED &&
            expiresAt != null &&
            expiresAt > System.currentTimeMillis()

    val remainingDays: Long
        get() {
            val target = expiresAt ?: return 0L
            val diff = target - System.currentTimeMillis()
            return if (diff > 0) diff / 86_400_000L else 0L
        }

    val remainingHours: Long
        get() {
            val target = expiresAt ?: return 0L
            val diff = target - System.currentTimeMillis()
            return if (diff > 0) (diff % 86_400_000L) / 3_600_000L else 0L
        }
}
