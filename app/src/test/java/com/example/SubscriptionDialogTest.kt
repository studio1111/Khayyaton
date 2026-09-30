package com.example

import com.example.model.SubscriptionStatus
import com.example.model.UserSubscription
import com.example.ui.dialogs.trialStatusTitle
import org.junit.Assert.assertEquals
import org.junit.Test

class SubscriptionDialogTest {
    @Test
    fun trialStatusTitleContainsSingleStatusLineAndRemainingTime() {
        val subscription = UserSubscription(
            status = SubscriptionStatus.TRIAL_ACTIVE,
            trialEndsAt = System.currentTimeMillis() + 2L * 24L * 60L * 60L * 1000L + 3L * 60L * 60L * 1000L
        )

        val title = trialStatusTitle(subscription)

        assertEquals(1, "نسخه آزمایشی ۷ روزه خیاطان".toRegex().findAll(title).count())
        assertEquals(false, title.contains("\n"))
        assertEquals(true, title.contains("روز"))
        assertEquals(true, title.contains("ساعت"))
    }
}
