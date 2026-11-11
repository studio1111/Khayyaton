package com.example

import com.example.data.subscription.EditionAccessPolicy
import com.example.model.SubscriptionStatus
import com.example.model.UserSubscription
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditionAccessPolicyTest {
    @Test
    fun freeEditionGrantsAccessWithoutTrialOrPurchase() {
        val expiredSubscription = UserSubscription(status = SubscriptionStatus.TRIAL_EXPIRED)

        assertTrue(EditionAccessPolicy.hasAccess(true, expiredSubscription))
    }

    @Test
    fun paidEditionStillRequiresAnActiveEntitlement() {
        val expiredSubscription = UserSubscription(status = SubscriptionStatus.EXPIRED)

        assertFalse(EditionAccessPolicy.hasAccess(false, expiredSubscription))
    }

    @Test
    fun paidEditionKeepsExistingPermanentAdminGrant() {
        val ownerSubscription = UserSubscription(status = SubscriptionStatus.ADMIN_GRANTED)

        assertTrue(EditionAccessPolicy.hasAccess(false, ownerSubscription))
    }
}
