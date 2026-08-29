package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "furniture_orders")
data class FurnitureOrder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderNumber: Long,
    val invoiceNumber: String,
    val modelName: String,
    val pricePerSet: Long,
    val unitsPerSet: Double = 8.0,
    val countFormula: String,
    val calculatedUnits: Double,
    val calculatedTotal: Long,
    val dateJalali: String,
    val dateGregorian: String,
    val customerName: String,
    val phone: String = "",
    val fabricName: String = "",
    val workshopInvoiceNumber: String = "",
    val notes: String = "",
    val colorCode: String = "#2563EB",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "payment_records")
data class PaymentRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val paymentNumber: Long,
    val amount: Long,
    val dateJalali: String,
    val dateGregorian: String,
    val customerName: String,
    val description: String = "",
    val paymentType: String = "transfer", // "transfer", "cash", "pos", "cheque"
    val referenceNo: String = "",
    val bankName: String = "",
    val cardNumber: String = "",
    val relatedOrderId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "model_presets")
data class ModelPreset(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val defaultPricePerSet: Long,
    val defaultUnitsPerSet: Double = 8.0,
    val colorCode: String = "#2563EB",
    val description: String = ""
)

@Entity(tableName = "unit_conversion_rules")
data class UnitConversionRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pieceKey: String = "",
    val pieceCount: Double = 0.0,
    val calculatedUnits: Double = 0.0,
    val isEnabled: Boolean = true
)

enum class AppThemeMode(val titleFa: String) {
    NEON_GLASS("نئونی شیشه‌ای سبز و زرد (SheetOn Neon)"),
    LIGHT("روشن (استاندارد)"),
    DARK("تاریک (محیط شب)"),
    GLASS_BLUE("شیشه‌ای سرمه‌ای"),
    GLASS_PURPLE("شیشه‌ای بنفش")
}

enum class CalendarType(val titleFa: String) {
    JALALI("تقویم شمسی (جلالی)"),
    GREGORIAN("تقویم میلادی (گرگوری)")
}

enum class CardDisplayMode(val titleFa: String) {
    UNIFIED("نمایش یکپارچه کارت‌ها (پشت سر هم)"),
    SEPARATED("تفکیک کارت‌های دریافتی و کارکرد")
}

enum class CardSortOrder(val titleFa: String) {
    NEWEST_BOTTOM("کارت جدید در انتهای لیست (صعودی)"),
    NEWEST_TOP("کارت جدید در ابتدای لیست (نزولی)")
}

sealed interface FeedItem {
    val timestamp: Long
    val id: Long
    val displayIndex: Long

    data class OrderItem(val order: FurnitureOrder, val index: Long) : FeedItem {
        override val timestamp: Long get() = order.createdAt
        override val id: Long get() = order.id
        override val displayIndex: Long get() = index
    }

    data class PaymentItem(val payment: PaymentRecord, val index: Long) : FeedItem {
        override val timestamp: Long get() = payment.createdAt
        override val id: Long get() = payment.id
        override val displayIndex: Long get() = index
    }
}

data class DeleteTarget(
    val type: String, // "order" or "payment"
    val id: Long,
    val title: String,
    val message: String,
    val details: List<Pair<String, String>> = emptyList()
)

