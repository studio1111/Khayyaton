package com.example.data.subscription

import com.example.model.UserSubscription

/** Centralizes edition access without changing subscription data in the paid edition. */
object EditionAccessPolicy {
    fun hasAccess(isFreeEdition: Boolean, subscription: UserSubscription): Boolean =
        isFreeEdition || subscription.hasAccess
}
