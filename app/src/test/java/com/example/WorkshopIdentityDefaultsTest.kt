package com.example

import com.example.model.FurnitureOrder
import com.example.model.ModelPreset
import com.example.model.PaymentRecord
import com.example.model.Workshop
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkshopIdentityDefaultsTest {
    @Test
    fun new_domain_models_do_not_assume_workshop_one() {
        val order = FurnitureOrder(
            orderNumber = 1L,
            invoiceNumber = "1",
            modelName = "مدل",
            pricePerSet = 1L,
            countFormula = "1",
            calculatedUnits = 1.0,
            calculatedTotal = 1L,
            dateJalali = "1405/01/01",
            dateGregorian = "2026-03-21",
            customerName = "مشتری"
        )
        val payment = PaymentRecord(
            paymentNumber = 1L,
            amount = 1L,
            dateJalali = "1405/01/01",
            dateGregorian = "2026-03-21",
            customerName = "مشتری"
        )
        val preset = ModelPreset(name = "مدل")

        assertEquals(0L, order.workshopId)
        assertEquals(0L, payment.workshopId)
        assertEquals(0L, preset.workshopId)
    }
    
    @Test
    fun same_name_workshops_keep_distinct_sync_identity() {
        val first = Workshop(name = "کارگاه مشترک", syncId = "workshop-a")
        val second = Workshop(name = "کارگاه مشترک", syncId = "workshop-b")

        assertEquals(first.name, second.name)
        assertEquals("workshop-a", first.syncId)
        assertEquals("workshop-b", second.syncId)
    }
}
