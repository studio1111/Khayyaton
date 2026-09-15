package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.model.FurnitureOrder
import com.example.model.PaymentRecord
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object CardImageSharer {

    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        maxWidth: Float,
        paint: Paint,
        lineHeight: Float = 34f,
        maxLines: Int = 3
    ) {
        if (text.isBlank()) return
        if (paint.measureText(text) <= maxWidth) {
            canvas.drawText(text, x, y, paint)
            return
        }

        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                }
                currentLine = word
            }
            if (lines.size == maxLines - 1) {
                break
            }
        }
        if (currentLine.isNotEmpty() && lines.size < maxLines) {
            lines.add(currentLine)
        }

        if (lines.isEmpty()) {
            canvas.drawText(text, x, y, paint)
            return
        }

        val totalHeight = (lines.size - 1) * lineHeight
        val startY = y - (totalHeight / 2f)

        lines.forEachIndexed { index, line ->
            canvas.drawText(line, x, startY + (index * lineHeight), paint)
        }
    }

    fun generateOrderCardBitmap(
        context: Context,
        order: FurnitureOrder,
        currencyUnit: String,
        totalWork: Long,
        totalReceived: Long,
        remainingBalance: Long,
        orderCount: Int,
        paymentCount: Int
    ): Bitmap {
        val width = 1080
        val height = 1520
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Overall Canvas Background (Dark neutral background with soft gradient)
        val bgPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#060F1A")
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Header Brand Banner (مدیریت کارگاه خیاطان)
        val headerPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#0B192A")
            isAntiAlias = true
        }
        val headerRect = RectF(40f, 40f, width - 40f, 130f)
        canvas.drawRoundRect(headerRect, 20f, 20f, headerPaint)

        val headerBorderPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#00E676")
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        canvas.drawRoundRect(headerRect, 20f, 20f, headerBorderPaint)

        val brandPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#00E676")
            textSize = 34f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("ساخته شده با برنامه خیاطان", width / 2f, 96f, brandPaint)

        // 3. Main Order Card Background (Color of the Model)
        val modelColorInt = PersianUtils.parseColor(
            if (order.colorCode.isNotBlank()) order.colorCode else PersianUtils.getModelColor(order.modelName)
        ).hashCode()

        val cardRect = RectF(40f, 150f, width - 40f, 1140f)

        // Card Fill
        val cardFillPaint = Paint().apply {
            color = modelColorInt
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, 40f, 40f, cardFillPaint)

        // Card Gold Border
        val cardBorderPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#D4A017")
            style = Paint.Style.STROKE
            strokeWidth = 6f
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, 40f, 40f, cardBorderPaint)

        // Inside Card - Top Row Number Badge
        val rowBadgeRect = RectF(70f, 175f, 380f, 235f)
        val rowBadgePaint = Paint().apply {
            color = android.graphics.Color.argb(160, 0, 0, 0)
            isAntiAlias = true
        }
        canvas.drawRoundRect(rowBadgeRect, 16f, 16f, rowBadgePaint)

        val rowTextPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 26f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("شماره ردیف: ${PersianUtils.toPersianDigits(order.orderNumber)}", rowBadgeRect.centerX(), rowBadgeRect.centerY() + 9f, rowTextPaint)

        // Cells Grid (5 Rows x 2 Columns)
        val cellPadding = 24f
        val cellStartX = 70f
        val cellEndX = width - 70f
        val cellTotalW = cellEndX - cellStartX
        val colW = (cellTotalW - cellPadding) / 2f
        val cellH = 145f
        val cellStartY = 260f
        val rowGap = 20f

        val cellBgPaint = Paint().apply {
            color = android.graphics.Color.argb(210, 15, 23, 42)
            isAntiAlias = true
        }
        val cellBorderPaint = Paint().apply {
            color = android.graphics.Color.argb(70, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            isAntiAlias = true
        }

        val labelPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#CBD5E1")
            textSize = 23f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val valuePaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 28f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        fun drawCell(r: RectF, label: String, value: String, valueColor: Int = android.graphics.Color.WHITE, textSize: Float = 28f, maxLines: Int = 2) {
            canvas.drawRoundRect(r, 22f, 22f, cellBgPaint)
            canvas.drawRoundRect(r, 22f, 22f, cellBorderPaint)

            canvas.drawText(label, r.centerX(), r.top + 42f, labelPaint)
            valuePaint.color = valueColor
            valuePaint.textSize = textSize
            drawWrappedText(canvas, value, r.centerX(), r.top + 98f, r.width() - 20f, valuePaint, lineHeight = 32f, maxLines = maxLines)
        }

        // ROW 1: Right [مدل مبل] | Left [دستمزد هر دست]
        var curY = cellStartY
        val r1Right = RectF(cellStartX + colW + cellPadding, curY, cellEndX, curY + cellH)
        val r1Left = RectF(cellStartX, curY, cellStartX + colW, curY + cellH)
        drawCell(r1Right, "مدل مبل", order.modelName)
        drawCell(r1Left, "دستمزد هر دست", "${PersianUtils.formatNumberWithCommas(order.pricePerSet)} $currencyUnit")

        // ROW 2: Right [تعداد واحد در یک دست] | Left [تعداد]
        curY += cellH + rowGap
        val r2Right = RectF(cellStartX + colW + cellPadding, curY, cellEndX, curY + cellH)
        val r2Left = RectF(cellStartX, curY, cellStartX + colW, curY + cellH)
        drawCell(r2Right, "تعداد واحد در یک دست", PersianUtils.toPersianDigits(order.unitsPerSet))
        drawCell(r2Left, "تعداد", PersianUtils.toPersianDigits(order.countFormula))

        // ROW 3: Right [مجموع واحد] | Left [دستمزد]
        curY += cellH + rowGap
        val r3Right = RectF(cellStartX + colW + cellPadding, curY, cellEndX, curY + cellH)
        val r3Left = RectF(cellStartX, curY, cellStartX + colW, curY + cellH)
        drawCell(r3Right, "مجموع واحد", "${PersianUtils.formatNumberWithCommas(order.calculatedUnits)} واحد")
        drawCell(r3Left, "دستمزد", "${PersianUtils.formatNumberWithCommas(order.calculatedTotal)} $currencyUnit", android.graphics.Color.parseColor("#38BDF8"))

        // ROW 4: Right [تاریخ] | Left [شماره فاکتور]
        curY += cellH + rowGap
        val r4Right = RectF(cellStartX + colW + cellPadding, curY, cellEndX, curY + cellH)
        val r4Left = RectF(cellStartX, curY, cellStartX + colW, curY + cellH)
        drawCell(r4Right, "تاریخ", PersianUtils.toPersianDigits(order.dateJalali))
        drawCell(r4Left, "شماره فاکتور", PersianUtils.toPersianDigits(order.invoiceNumber))

        // ROW 5: Right [مشتری / نمایشگاه] | Left [توضیحات]
        curY += cellH + rowGap
        val r5Right = RectF(cellStartX + colW + cellPadding, curY, cellEndX, curY + cellH)
        val r5Left = RectF(cellStartX, curY, cellStartX + colW, curY + cellH)
        drawCell(r5Right, "مشتری / نمایشگاه", order.customerName.ifBlank { "مشتری عمومی" })

        // مشخصات پارچه در ابتدا و سپس متن خود توضیحات
        val fullNotes = buildString {
            if (order.fabricName.isNotBlank()) {
                append("پارچه: ${order.fabricName}")
            }
            if (order.notes.isNotBlank()) {
                if (order.fabricName.isNotBlank()) append(" - ")
                append(order.notes)
            }
        }.ifBlank { "-" }
        drawCell(r5Left, "توضیحات", fullNotes, textSize = 23f, maxLines = 3)

        // 4. BOTTOM SUMMARY FOOTER (Matching app footer)
        val footerRect = RectF(40f, 1170f, width - 40f, 1460f)
        val footerBgPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#020617")
            isAntiAlias = true
        }
        val footerBorderPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#334155")
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        canvas.drawRoundRect(footerRect, 30f, 30f, footerBgPaint)
        canvas.drawRoundRect(footerRect, 30f, 30f, footerBorderPaint)

        // 3 Summary Columns in Footer
        val fColW = (footerRect.width() - 40f) / 3f
        val fStartY = footerRect.top + 35f

        // Column 1: مجموع کارکرد
        val fLabel1 = "مجموع کارکرد (${PersianUtils.toPersianDigits(orderCount)})"
        val fVal1 = PersianUtils.formatCurrency(totalWork, currencyUnit)
        val fX1 = footerRect.left + 20f + fColW * 2.5f

        val fTitlePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#94A3B8")
            textSize = 22f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val fValWorkPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 26f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(fLabel1, fX1, fStartY + 25f, fTitlePaint)
        canvas.drawText(fVal1, fX1, fStartY + 75f, fValWorkPaint)

        // Divider 1
        val divPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#1E293B")
            strokeWidth = 3f
        }
        canvas.drawLine(footerRect.left + 20f + fColW * 2f, footerRect.top + 25f, footerRect.left + 20f + fColW * 2f, footerRect.bottom - 25f, divPaint)

        // Column 2: کل دریافتی
        val fLabel2 = "کل دریافتی (${PersianUtils.toPersianDigits(paymentCount)})"
        val fVal2 = PersianUtils.formatCurrency(totalReceived, currencyUnit)
        val fX2 = footerRect.left + 20f + fColW * 1.5f

        val fValPayPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#34D399")
            textSize = 26f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(fLabel2, fX2, fStartY + 25f, fTitlePaint)
        canvas.drawText(fVal2, fX2, fStartY + 75f, fValPayPaint)

        // Divider 2
        canvas.drawLine(footerRect.left + 20f + fColW, footerRect.top + 25f, footerRect.left + 20f + fColW, footerRect.bottom - 25f, divPaint)

        // Column 3: باقی‌مانده (با قانون بدهکاری منفی)
        val fLabel3 = if (remainingBalance < 0) "باقی‌مانده (بدهکاری)" else if (remainingBalance == 0L) "تسویه کامل" else "باقی‌مانده"
        val fVal3 = PersianUtils.formatRemainingBalanceText(remainingBalance, currencyUnit)
        val fX3 = footerRect.left + 20f + fColW * 0.5f

        val fValBalPaint = Paint().apply {
            color = if (remainingBalance == 0L) android.graphics.Color.parseColor("#34D399")
            else android.graphics.Color.parseColor("#F87171")
            textSize = if (fVal3.length > 20) 20f else 24f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(fLabel3, fX3, fStartY + 25f, fTitlePaint)
        canvas.drawText(fVal3, fX3, fStartY + 75f, fValBalPaint)

        return bitmap
    }

    fun generatePaymentCardBitmap(
        context: Context,
        payment: PaymentRecord,
        currencyUnit: String,
        totalWork: Long,
        totalReceived: Long,
        remainingBalance: Long,
        orderCount: Int,
        paymentCount: Int
    ): Bitmap {
        val width = 1080
        val height = 1380
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Overall Canvas Background
        val bgPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#060F1A")
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Header Brand Banner (خیاطان)
        val headerPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#0B192A")
            isAntiAlias = true
        }
        val headerRect = RectF(40f, 40f, width - 40f, 130f)
        canvas.drawRoundRect(headerRect, 20f, 20f, headerPaint)

        val headerBorderPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#00E676")
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        canvas.drawRoundRect(headerRect, 20f, 20f, headerBorderPaint)

        val brandPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#00E676")
            textSize = 34f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("ساخته شده با برنامه خیاطان", width / 2f, 96f, brandPaint)

        // 3. Main Payment Card Background
        val cardRect = RectF(40f, 150f, width - 40f, 1000f)

        val cardFillPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#131826")
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, 40f, 40f, cardFillPaint)

        val cardBorderPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#D4A017")
            style = Paint.Style.STROKE
            strokeWidth = 6f
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, 40f, 40f, cardBorderPaint)

        // Top Row Number Badge
        val rowBadgeRect = RectF(70f, 175f, 380f, 235f)
        val rowBadgePaint = Paint().apply {
            color = android.graphics.Color.argb(160, 0, 0, 0)
            isAntiAlias = true
        }
        canvas.drawRoundRect(rowBadgeRect, 16f, 16f, rowBadgePaint)

        val rowTextPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 26f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("شماره ردیف: ${PersianUtils.toPersianDigits(payment.paymentNumber)}", rowBadgeRect.centerX(), rowBadgeRect.centerY() + 9f, rowTextPaint)

        // Payment status badge on right
        val payStatusRect = RectF(width - 270f, 175f, width - 70f, 235f)
        val payStatusPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#064E3B")
            isAntiAlias = true
        }
        canvas.drawRoundRect(payStatusRect, 16f, 16f, payStatusPaint)
        val payStatusTextPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#34D399")
            textSize = 24f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("دریافتی / واریز", payStatusRect.centerX(), payStatusRect.centerY() + 8f, payStatusTextPaint)

        // Cells Grid
        val cellPadding = 24f
        val cellStartX = 70f
        val cellEndX = width - 70f
        val cellTotalW = cellEndX - cellStartX
        val colW = (cellTotalW - cellPadding) / 2f
        val cellH = 140f
        val cellStartY = 260f
        val rowGap = 20f

        val cellBgPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#1B2134")
            isAntiAlias = true
        }
        val cellBorderPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#2B334D")
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            isAntiAlias = true
        }

        val labelPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#9AA4BF")
            textSize = 23f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val valuePaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 28f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        fun drawCell(r: RectF, label: String, value: String, valueColor: Int = android.graphics.Color.WHITE, textSize: Float = 28f, maxLines: Int = 2) {
            canvas.drawRoundRect(r, 22f, 22f, cellBgPaint)
            canvas.drawRoundRect(r, 22f, 22f, cellBorderPaint)

            canvas.drawText(label, r.centerX(), r.top + 42f, labelPaint)
            valuePaint.color = valueColor
            valuePaint.textSize = textSize
            drawWrappedText(canvas, value, r.centerX(), r.top + 98f, r.width() - 20f, valuePaint, lineHeight = 32f, maxLines = maxLines)
        }

        // ROW 1: Right [پرداخت‌کننده] | Left [مبلغ دریافتی]
        var curY = cellStartY
        val r1Right = RectF(cellStartX + colW + cellPadding, curY, cellEndX, curY + cellH)
        val r1Left = RectF(cellStartX, curY, cellStartX + colW, curY + cellH)
        drawCell(r1Right, "پرداخت‌کننده", payment.customerName.ifBlank { "عمومی" })
        drawCell(r1Left, "مبلغ دریافتی", "${PersianUtils.formatNumberWithCommas(payment.amount)} $currencyUnit", android.graphics.Color.parseColor("#34D399"))

        // ROW 2: Right [نوع پرداخت] | Left [کد پیگیری]
        curY += cellH + rowGap
        val methodTitle = when (payment.paymentType) {
            "transfer" -> "کارت به کارت / حواله"
            "cash" -> "نقدی"
            "pos" -> "کارتخوان POS"
            "cheque" -> "چک بانکی"
            else -> "واریزی"
        }
        val r2Right = RectF(cellStartX + colW + cellPadding, curY, cellEndX, curY + cellH)
        val r2Left = RectF(cellStartX, curY, cellStartX + colW, curY + cellH)
        drawCell(r2Right, "نوع پرداخت", methodTitle)
        drawCell(r2Left, "کد پیگیری", payment.referenceNo.ifBlank { "-" })

        // ROW 3: Right [تاریخ] | Left [شماره کارت یا حساب دریافتی]
        curY += cellH + rowGap
        val r3Right = RectF(cellStartX + colW + cellPadding, curY, cellEndX, curY + cellH)
        val r3Left = RectF(cellStartX, curY, cellStartX + colW, curY + cellH)
        drawCell(r3Right, "تاریخ", PersianUtils.toPersianDigits(payment.dateJalali))
        val cardOrAccount = when {
            payment.cardNumber.isNotBlank() -> payment.cardNumber
            payment.referenceNo.isNotBlank() -> payment.referenceNo
            else -> "-"
        }
        drawCell(r3Left, "شماره کارت یا حساب دریافتی", PersianUtils.toPersianDigits(cardOrAccount), textSize = 22f)

        // ROW 4: [توضیحات / بابت] Full Width
        curY += cellH + rowGap
        val r4Full = RectF(cellStartX, curY, cellEndX, curY + 120f)
        drawCell(r4Full, "توضیحات / بابت", payment.description.ifBlank { "بابت تسویه حساب سفارش" }, textSize = 23f, maxLines = 3)

        // 4. BOTTOM SUMMARY FOOTER
        val footerRect = RectF(40f, 1030f, width - 40f, 1320f)
        val footerBgPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#020617")
            isAntiAlias = true
        }
        val footerBorderPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#334155")
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        canvas.drawRoundRect(footerRect, 30f, 30f, footerBgPaint)
        canvas.drawRoundRect(footerRect, 30f, 30f, footerBorderPaint)

        val fColW = (footerRect.width() - 40f) / 3f
        val fStartY = footerRect.top + 35f

        val fTitlePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#94A3B8")
            textSize = 22f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val fValWorkPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 26f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        // Column 1: مجموع کارکرد
        val fLabel1 = "مجموع کارکرد (${PersianUtils.toPersianDigits(orderCount)})"
        val fVal1 = PersianUtils.formatCurrency(totalWork, currencyUnit)
        val fX1 = footerRect.left + 20f + fColW * 2.5f
        canvas.drawText(fLabel1, fX1, fStartY + 25f, fTitlePaint)
        canvas.drawText(fVal1, fX1, fStartY + 75f, fValWorkPaint)

        // Divider 1
        val divPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#1E293B")
            strokeWidth = 3f
        }
        canvas.drawLine(footerRect.left + 20f + fColW * 2f, footerRect.top + 25f, footerRect.left + 20f + fColW * 2f, footerRect.bottom - 25f, divPaint)

        // Column 2: کل دریافتی
        val fLabel2 = "کل دریافتی (${PersianUtils.toPersianDigits(paymentCount)})"
        val fVal2 = PersianUtils.formatCurrency(totalReceived, currencyUnit)
        val fX2 = footerRect.left + 20f + fColW * 1.5f
        val fValPayPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#34D399")
            textSize = 26f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(fLabel2, fX2, fStartY + 25f, fTitlePaint)
        canvas.drawText(fVal2, fX2, fStartY + 75f, fValPayPaint)

        // Divider 2
        canvas.drawLine(footerRect.left + 20f + fColW, footerRect.top + 25f, footerRect.left + 20f + fColW, footerRect.bottom - 25f, divPaint)

        // Column 3: باقی‌مانده (با قانون بدهکاری منفی)
        val fLabel3 = if (remainingBalance < 0) "باقی‌مانده (بدهکاری)" else if (remainingBalance == 0L) "تسویه کامل" else "باقی‌مانده"
        val fVal3 = PersianUtils.formatRemainingBalanceText(remainingBalance, currencyUnit)
        val fX3 = footerRect.left + 20f + fColW * 0.5f
        val fValBalPaint = Paint().apply {
            color = if (remainingBalance == 0L) android.graphics.Color.parseColor("#34D399")
            else android.graphics.Color.parseColor("#F87171")
            textSize = if (fVal3.length > 20) 20f else 24f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(fLabel3, fX3, fStartY + 25f, fTitlePaint)
        canvas.drawText(fVal3, fX3, fStartY + 75f, fValBalPaint)

        return bitmap
    }

    fun buildOrderCardText(order: FurnitureOrder): String {
        return buildString {
            appendLine("شماره ردیف: ${PersianUtils.toPersianDigits(order.orderNumber)}")
            appendLine("مدل مبل: ${order.modelName}")
            appendLine("تاریخ: ${PersianUtils.toPersianDigits(order.dateJalali)}")
            appendLine("نام مشتری: ${order.customerName.ifBlank { "عمومی" }}")
            append("شماره فاکتور: ${PersianUtils.toPersianDigits(order.invoiceNumber)}")
        }
    }

    fun buildPaymentCardText(payment: PaymentRecord, currencyUnit: String): String {
        return buildString {
            appendLine("شماره: ${PersianUtils.toPersianDigits(payment.paymentNumber)}")
            appendLine("مبلغ دریافتی: ${PersianUtils.formatCurrency(payment.amount, currencyUnit)}")
            appendLine("نام پرداخت کننده: ${payment.customerName.ifBlank { "عمومی" }}")
            append("تاریخ: ${PersianUtils.toPersianDigits(payment.dateJalali)}")
        }
    }

    fun buildOrderFileName(order: FurnitureOrder): String {
        val cleanModel = order.modelName.replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_").take(25)
        val cleanCustomer = order.customerName.replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_").take(25)
        val cleanDate = order.dateJalali.replace("/", "-")
        val cleanCount = order.countFormula.replace("+", "-").replace(Regex("[\\\\/:*?\"<>|\\s]+"), "")
        return "ردیف_${order.orderNumber}_مدل_${cleanModel}_قطعات_${cleanCount}_واحد_${order.calculatedUnits}_دستمزد_${order.calculatedTotal}_تاریخ_${cleanDate}_مشتری_${cleanCustomer}_فاکتور_${order.invoiceNumber}"
    }

    fun buildPaymentFileName(payment: PaymentRecord): String {
        val cleanCustomer = payment.customerName.replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_").take(25)
        val cleanDate = payment.dateJalali.replace("/", "-")
        return "ردیف_${payment.paymentNumber}_دریافتی_${payment.amount}_تاریخ_${cleanDate}_پرداخت_کننده_${cleanCustomer}"
    }

    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, fileNamePrefix: String): Boolean {
        return try {
            val sanitized = fileNamePrefix.replace(Regex("[\\\\/:*?\"<>|]+"), "_")
            val filename = "${sanitized}_${System.currentTimeMillis()}.png"
            var outputStream: OutputStream? = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/KhayyatOn")
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    outputStream = context.contentResolver.openOutputStream(uri)
                }
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val appDir = File(picturesDir, "KhayyatOn").apply { mkdirs() }
                val imageFile = File(appDir, filename)
                outputStream = FileOutputStream(imageFile)
            }

            outputStream?.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                Toast.makeText(context, "✅ تصویر کارت با موفقیت در گالری ذخیره شد", Toast.LENGTH_LONG).show()
                true
            } ?: run {
                Toast.makeText(context, "خطا در ذخیره تصویر", Toast.LENGTH_SHORT).show()
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "خطا در ذخیره تصویر: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun shareBitmap(
        context: Context,
        bitmap: Bitmap,
        subjectTitle: String,
        shareText: String = "",
        fileNamePrefix: String = "Khayyaton_Card"
    ) {
        try {
            val cachePath = File(context.cacheDir, "images").apply { mkdirs() }
            val sanitized = fileNamePrefix.replace(Regex("[\\\\/:*?\"<>|]+"), "_").take(60)
            val file = File(cachePath, "${sanitized}_${System.currentTimeMillis()}.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, subjectTitle)
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "اشتراک‌گذاری تصویر کارت")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "خطا در اشتراک‌گذاری تصویر: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
