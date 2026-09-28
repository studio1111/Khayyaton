package com.example

import com.example.model.FurnitureOrder
import com.example.model.ModelPreset
import com.example.model.PaymentRecord
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
}
