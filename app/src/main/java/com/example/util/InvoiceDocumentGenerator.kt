package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.model.FurnitureOrder
import com.example.model.PaymentRecord
import java.io.File
import java.io.FileOutputStream

object InvoiceDocumentGenerator {

    /**
     * Generates a modern, clean, print-ready Persian RTL HTML invoice
     */
    fun generateInvoiceHtml(
        orders: List<FurnitureOrder>,
        payments: List<PaymentRecord>,
        targetCustomer: String,
        currencyUnit: String,
        currentDate: String
    ): String {
        val totalWork = orders.sumOf { it.calculatedTotal }
        val totalPaid = payments.sumOf { it.amount }
        val balance = totalWork - totalPaid

        val statusText = if (balance > 0) "مانده حساب" else if (balance == 0L) "تسویه حساب کامل" else "بدهکاری"
        val statusColor = if (balance > 0) "#DC2626" else if (balance == 0L) "#059669" else "#DC2626"
        val formattedBal = PersianUtils.formatRemainingBalanceText(balance, currencyUnit)

        val sb = StringBuilder()
        sb.append("""
<!DOCTYPE html>
<html lang="fa" dir="rtl">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>صورتحساب کارکرد و دریافتی ها - خیاطان</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Vazirmatn', 'Tahoma', 'Segoe UI', sans-serif; }
        body { background-color: #f8fafc; color: #0f172a; padding: 24px; direction: rtl; }
        .invoice-card { max-width: 960px; margin: 0 auto; background: #ffffff; border-radius: 16px; box-shadow: 0 4px 20px rgba(0,0,0,0.08); border: 1.5px solid #cbd5e1; overflow: hidden; padding: 28px; }
        .header { display: flex; justify-content: space-between; align-items: center; border-bottom: 3px solid #2563eb; padding-bottom: 16px; margin-bottom: 20px; }
        .logo-title h1 { font-size: 22px; color: #1e3a8a; font-weight: 900; margin-bottom: 4px; }
        .logo-title p { font-size: 12px; color: #64748b; font-weight: 600; }
        .meta-info { text-align: left; font-size: 12px; line-height: 1.8; color: #334155; }
        .customer-pill { background: #eff6ff; color: #1d4ed8; font-weight: bold; padding: 4px 12px; border-radius: 8px; display: inline-block; border: 1px solid #bfdbfe; }
        
        .section-title { font-size: 15px; font-weight: 900; color: #1e293b; margin: 20px 0 10px 0; display: flex; align-items: center; gap: 8px; border-right: 4px solid #2563eb; padding-right: 10px; }
        
        /* Clear Bordered Table with Expandable Cells */
        table.grid-table { width: 100%; border-collapse: collapse; margin-bottom: 20px; font-size: 12px; border: 1.5px solid #94a3b8; }
        table.grid-table th { background-color: #1e293b; color: #ffffff; padding: 10px 8px; text-align: center; font-weight: 900; border: 1.5px solid #64748b; font-size: 12px; }
        table.grid-table td { padding: 9px 8px; text-align: center; border: 1px solid #cbd5e1; color: #0f172a; vertical-align: middle; word-break: break-word; overflow-wrap: anywhere; }
        
        .badge-model { font-weight: 900; color: #0f172a; display: inline-block; padding: 2px 6px; border-radius: 6px; }
        .currency-val { font-weight: 900; color: #0f172a; }
        
        .summary-box { background: #f8fafc; border-radius: 12px; padding: 18px; margin-top: 20px; border: 1.5px solid #cbd5e1; }
        .summary-row { display: flex; justify-content: space-between; padding: 8px 0; font-size: 13px; border-bottom: 1px dashed #cbd5e1; }
        .summary-row:last-child { border-bottom: none; }
        .summary-row.total-balance { font-size: 16px; font-weight: 900; margin-top: 6px; padding-top: 10px; border-top: 2px solid #94a3b8; }
        
        .footer { text-align: center; margin-top: 24px; font-size: 11px; color: #94a3b8; border-top: 1px solid #e2e8f0; padding-top: 12px; }

        @media print {
            body { background: #ffffff; padding: 0; }
            .invoice-card { box-shadow: none; border: 1px solid #000; padding: 10px; }
            table.grid-table th { background-color: #334155 !important; color: #fff !important; -webkit-print-color-adjust: exact; }
            table.grid-table td { -webkit-print-color-adjust: exact; }
            .no-print { display: none; }
        }
    </style>
</head>
<body>
    <div class="invoice-card">
        <!-- Header -->
        <div class="header">
            <div class="logo-title">
                <h1>خیاطان</h1>
                <p>صورتحساب کارکرد و دریافتی ها</p>
            </div>
            <div class="meta-info">
                <div>تاریخ صدور: <strong>${PersianUtils.toPersianDigits(currentDate)}</strong></div>
                ${if (targetCustomer.isNotBlank()) "<div>طرف حساب: <span class=\"customer-pill\">$targetCustomer</span></div>" else "<div>طرف حساب: <strong>مجموع فاکتور های کارکرد و دریافتی</strong></div>"}
            </div>
        </div>

        <!-- Orders Table -->
        <div class="section-title">جدول کارکرد و دریافتی ها (${PersianUtils.toPersianDigits(orders.size)} فاکتور)</div>
        <table class="grid-table">
            <thead>
                <tr>
                    <th style="width: 35px;">ردیف</th>
                    <th style="width: 75px;">تاریخ</th>
                    <th style="width: 70px;">شماره فاکتور</th>
                    <th style="width: 110px;">مدل</th>
                    <th style="width: 130px;">فرمول اجزا و تعداد</th>
                    <th style="width: 90px;">دستمزد هر دست</th>
                    <th style="width: 105px;">مبلغ کل ($currencyUnit)</th>
                    <th>مشخصات و توضیحات</th>
                </tr>
            </thead>
            <tbody>
        """.trimIndent())

        if (orders.isEmpty()) {
            sb.append("<tr><td colspan=\"8\" style=\"padding: 20px; color: #64748b;\">هیچ فاکتوری ثبت نشده است.</td></tr>")
        } else {
            orders.forEachIndexed { i, ord ->
                val details = listOfNotNull(
                    if (ord.fabricName.isNotBlank()) "پارچه: ${ord.fabricName}" else null,
                    if (ord.notes.isNotBlank()) "توضیحات: ${ord.notes}" else null,
                    if (ord.customerName.isNotBlank() && targetCustomer.isBlank()) "طرف حساب: ${ord.customerName}" else null
                ).joinToString(" | ")

                val modelHex = if (ord.colorCode.isNotBlank()) ord.colorCode else PersianUtils.getModelColor(ord.modelName)
                val cleanHex = modelHex.removePrefix("#")
                val rowBgStyle = "background-color: #${cleanHex}18; border-right: 4px solid #$cleanHex;"
                val detailFontSize = if (details.length > 50) "10px" else "11px"

                sb.append("""
                <tr style="$rowBgStyle">
                    <td><strong>${PersianUtils.toPersianDigits(i + 1)}</strong></td>
                    <td>${PersianUtils.toPersianDigits(ord.dateJalali)}</td>
                    <td><strong>#${PersianUtils.toPersianDigits(ord.invoiceNumber)}</strong></td>
                    <td><span class="badge-model" style="color: #$cleanHex; background: #${cleanHex}22;">${ord.modelName}</span></td>
                    <td>${PersianUtils.toPersianDigits(ord.countFormula)} (${PersianUtils.formatNumberWithCommas(ord.calculatedUnits)} واحد)</td>
                    <td>${PersianUtils.formatNumberWithCommas(ord.pricePerSet)}</td>
                    <td class="currency-val">${PersianUtils.formatNumberWithCommas(ord.calculatedTotal)}</td>
                    <td style="font-size: $detailFontSize; text-align: right; line-height: 1.4;">${if (details.isNotBlank()) details else "-"}</td>
                </tr>
                """.trimIndent())
            }
        }

        sb.append("""
            </tbody>
        </table>
        """.trimIndent())

        // Payments Table
        if (payments.isNotEmpty()) {
            sb.append("""
            <div class="section-title" style="border-right-color: #059669;">جدول دریافتی‌ها و پرداختی‌ها (${PersianUtils.toPersianDigits(payments.size)} تراکنش)</div>
            <table class="grid-table">
                <thead>
                    <tr>
                        <th style="width: 40px;">ردیف</th>
                        <th style="width: 90px;">تاریخ</th>
                        <th style="width: 120px;">پرداخت‌کننده</th>
                        <th style="width: 100px;">روش پرداخت</th>
                        <th style="width: 100px;">کد پیگیری</th>
                        <th>بابت / شرح</th>
                        <th style="width: 120px;">مبلغ پرداختی ($currencyUnit)</th>
                    </tr>
                </thead>
                <tbody>
            """.trimIndent())

            payments.forEachIndexed { i, pay ->
                val methodFa = when (pay.paymentType) {
                    "transfer" -> "کارت به کارت / پایا"
                    "cash" -> "نقدی"
                    "pos" -> "کارتخوان POS"
                    "cheque" -> "چک بانکی"
                    else -> "واریزی"
                }

                val descText = pay.description.ifBlank { "تسویه حساب فاکتور" }
                val descFontSize = if (descText.length > 40) "10px" else "11px"

                sb.append("""
                <tr style="background-color: ${if (i % 2 == 1) "#ecfdf5" else "#f0fdf4"}; border-right: 4px solid #059669;">
                    <td><strong>${PersianUtils.toPersianDigits(i + 1)}</strong></td>
                    <td>${PersianUtils.toPersianDigits(pay.dateJalali)}</td>
                    <td><strong>${pay.customerName.ifBlank { "عمومی" }}</strong></td>
                    <td>$methodFa</td>
                    <td>${if (pay.referenceNo.isNotBlank()) PersianUtils.toPersianDigits(pay.referenceNo) else "-"}</td>
                    <td style="font-size: $descFontSize; text-align: right;">$descText</td>
                    <td class="currency-val" style="color: #059669;">${PersianUtils.formatNumberWithCommas(pay.amount)}</td>
                </tr>
                """.trimIndent())
            }

            sb.append("""
                </tbody>
            </table>
            """.trimIndent())
        }

        // Summary Box
        sb.append("""
        <div class="summary-box">
            <div class="summary-row">
                <span>مجموع کارکرد و فاکتورها:</span>
                <strong>${PersianUtils.formatCurrency(totalWork, currencyUnit)}</strong>
            </div>
            <div class="summary-row">
                <span>مجموع دریافتی‌ها و پیش‌پرداخت‌ها:</span>
                <strong style="color: #059669;">${PersianUtils.formatCurrency(totalPaid, currencyUnit)}</strong>
            </div>
            <div class="summary-row total-balance" style="color: $statusColor;">
                <span>باقی مانده حساب ($statusText):</span>
                <span>$formattedBal</span>
            </div>
        </div>

        <div class="footer">
            فایل PDF صورتحساب کارکرد و دریافتی ها ساخته شده با خیاطان
        </div>
    </div>
</body>
</html>
        """.trimIndent())

        return sb.toString()
    }

    /**
     * Exports HTML file and launches share sheet
     */
    fun shareHtmlInvoice(
        context: Context,
        orders: List<FurnitureOrder>,
        payments: List<PaymentRecord>,
        targetCustomer: String,
        currencyUnit: String,
        currentDate: String
    ) {
        try {
            val htmlContent = generateInvoiceHtml(orders, payments, targetCustomer, currencyUnit, currentDate)
            val cacheDir = File(context.cacheDir, "invoices")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val sanitizedCustomer = if (targetCustomer.isNotBlank()) "_${targetCustomer.replace(" ", "_")}" else ""
            val file = File(cacheDir, "Khayyaton_Invoice${sanitizedCustomer}_${System.currentTimeMillis()}.html")
            file.writeText(htmlContent, Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/html"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "فایل PDF صورتحساب کارکرد و دریافتی ها ساخته شده با خیاطان")
                putExtra(Intent.EXTRA_TEXT, "فایل PDF صورتحساب کارکرد و دریافتی ها ساخته شده با خیاطان")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری فاکتور HTML"))
        } catch (e: Exception) {
        }
    }

    /**
     * Generates a multi-column PDF Invoice document with Persian text, tables, dates, and financial totals
     */
    fun sharePdfInvoice(
        context: Context,
        orders: List<FurnitureOrder>,
        payments: List<PaymentRecord>,
        targetCustomer: String,
        currencyUnit: String,
        currentDate: String
    ) {
        try {
            val totalWork = orders.sumOf { it.calculatedTotal }
            val totalPaid = payments.sumOf { it.amount }
            val balance = totalWork - totalPaid

            val doc = PdfDocument()
            val pageWidth = 595 // A4 standard width (pt)
            val pageHeight = 842 // A4 standard height (pt)

            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas

            // Paints
            val textPaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 9f
                isAntiAlias = true
            }

            val boldPaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
                isAntiAlias = true
            }

            val titlePaint = Paint().apply {
                color = Color.rgb(30, 58, 138)
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                isAntiAlias = true
            }

            val headerBgPaint = Paint().apply {
                color = Color.rgb(30, 41, 59)
                style = Paint.Style.FILL
            }

            val tableHeaderPaint = Paint().apply {
                color = Color.WHITE
                textSize = 8.5f
                typeface = Typeface.DEFAULT_BOLD
                isAntiAlias = true
            }

            val borderPaint = Paint().apply {
                color = Color.rgb(203, 213, 225)
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }

            val greenPaint = Paint().apply {
                color = Color.rgb(5, 150, 105)
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
                isAntiAlias = true
            }

            val redPaint = Paint().apply {
                color = Color.rgb(220, 38, 38)
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
                isAntiAlias = true
            }

            // Top Header Banner
            canvas.drawRect(20f, 20f, (pageWidth - 20).toFloat(), 75f, Paint().apply {
                color = Color.rgb(241, 245, 249)
                style = Paint.Style.FILL
            })
            canvas.drawRect(20f, 20f, (pageWidth - 20).toFloat(), 75f, borderPaint)

            canvas.drawText("خیاطان - صورتحساب کارکرد و دریافتی ها", 35f, 44f, titlePaint)
            textPaint.textSize = 8.5f
            canvas.drawText("نمایش جدول کارکرد و دریافتی ها با تاریخ فاکتور", 35f, 60f, textPaint)

            val dateStr = "تاریخ صدور: ${PersianUtils.toPersianDigits(currentDate)}"
            val custStr = if (targetCustomer.isNotBlank()) "طرف حساب: $targetCustomer" else "مجموع فاکتور های کارکرد و دریافتی"
            canvas.drawText(dateStr, (pageWidth - 170).toFloat(), 44f, boldPaint)
            canvas.drawText(custStr, (pageWidth - 170).toFloat(), 60f, boldPaint)

            var currentY = 95f

            // Section 1: Orders Table Header with Date Column
            boldPaint.textSize = 10.5f
            canvas.drawText("جدول کارکرد و دریافتی ها (${PersianUtils.toPersianDigits(orders.size)} فاکتور)", 20f, currentY, boldPaint)
            currentY += 8f

            // Table Header Bar (Grid Lined with Date)
            canvas.drawRect(20f, currentY, (pageWidth - 20).toFloat(), currentY + 20f, headerBgPaint)
            canvas.drawRect(20f, currentY, (pageWidth - 20).toFloat(), currentY + 20f, borderPaint)
            
            // Vertical header divider lines: col positions including Date
            val colPositions = floatArrayOf(20f, 45f, 85f, 138f, 225f, 305f, 385f, 465f, (pageWidth - 20).toFloat())
            for (colX in colPositions) {
                canvas.drawLine(colX, currentY, colX, currentY + 20f, borderPaint)
            }

            canvas.drawText("ردیف", 23f, currentY + 14f, tableHeaderPaint)
            canvas.drawText("تاریخ", 48f, currentY + 14f, tableHeaderPaint)
            canvas.drawText("فاکتور", 88f, currentY + 14f, tableHeaderPaint)
            canvas.drawText("مدل", 142f, currentY + 14f, tableHeaderPaint)
            canvas.drawText("اجزا / واحد", 228f, currentY + 14f, tableHeaderPaint)
            canvas.drawText("دستمزد دست", 308f, currentY + 14f, tableHeaderPaint)
            canvas.drawText("مبلغ کل ($currencyUnit)", 388f, currentY + 14f, tableHeaderPaint)
            canvas.drawText("مشخصات / توضیحات", 468f, currentY + 14f, tableHeaderPaint)
            currentY += 20f

            // Orders Table Rows
            orders.take(15).forEachIndexed { i, ord ->
                val rowH = 19f
                val modelColorHex = if (ord.colorCode.isNotBlank()) ord.colorCode else PersianUtils.getModelColor(ord.modelName)
                val rowColorInt = try {
                    val parsed = PersianUtils.parseColor(modelColorHex)
                    Color.argb(35, (parsed.red * 255).toInt(), (parsed.green * 255).toInt(), (parsed.blue * 255).toInt())
                } catch (_: Exception) {
                    if (i % 2 == 0) Color.rgb(248, 250, 252) else Color.WHITE
                }

                // Row background tinted with invoice color
                val customRowBgPaint = Paint().apply {
                    color = rowColorInt
                    style = Paint.Style.FILL
                }
                canvas.drawRect(20f, currentY, (pageWidth - 20).toFloat(), currentY + rowH, customRowBgPaint)

                // Accent colored border on right
                val accentPaint = Paint().apply {
                    color = try {
                        val parsed = PersianUtils.parseColor(modelColorHex)
                        Color.rgb((parsed.red * 255).toInt(), (parsed.green * 255).toInt(), (parsed.blue * 255).toInt())
                    } catch (_: Exception) { Color.rgb(37, 99, 235) }
                    style = Paint.Style.FILL
                }
                canvas.drawRect(20f, currentY, 23f, currentY + rowH, accentPaint)

                // Outer border & vertical grid lines
                canvas.drawRect(20f, currentY, (pageWidth - 20).toFloat(), currentY + rowH, borderPaint)
                for (colX in colPositions) {
                    canvas.drawLine(colX, currentY, colX, currentY + rowH, borderPaint)
                }

                textPaint.textSize = 8f
                canvas.drawText(PersianUtils.toPersianDigits(i + 1), 25f, currentY + 13f, textPaint)
                canvas.drawText(PersianUtils.toPersianDigits(ord.dateJalali.takeLast(8)), 48f, currentY + 13f, textPaint)
                canvas.drawText("#${PersianUtils.toPersianDigits(ord.invoiceNumber)}", 88f, currentY + 13f, textPaint)
                
                // Model Name with dynamic size if long
                boldPaint.textSize = if (ord.modelName.length > 12) 7f else 8f
                val modelDisplay = if (ord.modelName.length > 15) ord.modelName.take(15) + ".." else ord.modelName
                canvas.drawText(modelDisplay, 142f, currentY + 13f, boldPaint)
                
                textPaint.textSize = 7.5f
                val formulaDisplay = "${PersianUtils.toPersianDigits(ord.countFormula)} (${PersianUtils.formatNumberWithCommas(ord.calculatedUnits)}و)"
                canvas.drawText(formulaDisplay, 228f, currentY + 13f, textPaint)
                canvas.drawText(PersianUtils.formatNumberWithCommas(ord.pricePerSet), 308f, currentY + 13f, textPaint)

                boldPaint.textSize = 8f
                canvas.drawText(PersianUtils.formatNumberWithCommas(ord.calculatedTotal), 388f, currentY + 13f, boldPaint)
                
                val detailStr = listOfNotNull(
                    if (ord.fabricName.isNotBlank()) "پارچه: ${ord.fabricName}" else null,
                    if (ord.notes.isNotBlank()) ord.notes else null
                ).joinToString(" - ")
                textPaint.textSize = if (detailStr.length > 15) 6.5f else 7.5f
                val finalDetail = if (detailStr.length > 18) detailStr.take(18) + ".." else detailStr
                canvas.drawText(if (finalDetail.isNotBlank()) finalDetail else "-", 468f, currentY + 13f, textPaint)

                currentY += rowH
            }

            currentY += 14f

            // Section 2: Payments (if any)
            if (payments.isNotEmpty()) {
                boldPaint.textSize = 10.5f
                canvas.drawText("سوابق دریافتی‌ها و پرداختی‌ها (${PersianUtils.toPersianDigits(payments.size)} دریافتی)", 20f, currentY, boldPaint)
                currentY += 8f

                val payColPositions = floatArrayOf(20f, 50f, 110f, 210f, 290f, 380f, 470f, (pageWidth - 20).toFloat())

                canvas.drawRect(20f, currentY, (pageWidth - 20).toFloat(), currentY + 20f, headerBgPaint)
                canvas.drawRect(20f, currentY, (pageWidth - 20).toFloat(), currentY + 20f, borderPaint)
                for (colX in payColPositions) {
                    canvas.drawLine(colX, currentY, colX, currentY + 20f, borderPaint)
                }

                canvas.drawText("ردیف", 25f, currentY + 14f, tableHeaderPaint)
                canvas.drawText("تاریخ", 55f, currentY + 14f, tableHeaderPaint)
                canvas.drawText("پرداخت‌کننده", 115f, currentY + 14f, tableHeaderPaint)
                canvas.drawText("روش پرداخت", 215f, currentY + 14f, tableHeaderPaint)
                canvas.drawText("کد پیگیری", 295f, currentY + 14f, tableHeaderPaint)
                canvas.drawText("مبلغ ($currencyUnit)", 385f, currentY + 14f, tableHeaderPaint)
                canvas.drawText("بابت / توضیحات", 475f, currentY + 14f, tableHeaderPaint)
                currentY += 20f

                val payRowBg = Paint().apply {
                    color = Color.rgb(236, 253, 245)
                    style = Paint.Style.FILL
                }
                val payAccent = Paint().apply {
                    color = Color.rgb(5, 150, 105)
                    style = Paint.Style.FILL
                }

                payments.take(10).forEachIndexed { i, pay ->
                    val rowH = 19f
                    if (i % 2 == 1) {
                        canvas.drawRect(20f, currentY, (pageWidth - 20).toFloat(), currentY + rowH, payRowBg)
                    }
                    canvas.drawRect(20f, currentY, 23f, currentY + rowH, payAccent)
                    canvas.drawRect(20f, currentY, (pageWidth - 20).toFloat(), currentY + rowH, borderPaint)
                    for (colX in payColPositions) {
                        canvas.drawLine(colX, currentY, colX, currentY + rowH, borderPaint)
                    }

                    val methodFa = when (pay.paymentType) {
                        "transfer" -> "کارت به کارت"
                        "cash" -> "نقدی"
                        "pos" -> "کارتخوان"
                        "cheque" -> "چک"
                        else -> "واریزی"
                    }

                    textPaint.textSize = 8f
                    canvas.drawText(PersianUtils.toPersianDigits(i + 1), 28f, currentY + 13f, textPaint)
                    canvas.drawText(PersianUtils.toPersianDigits(pay.dateJalali), 55f, currentY + 13f, textPaint)
                    canvas.drawText(pay.customerName.ifBlank { "عمومی" }.take(14), 115f, currentY + 13f, textPaint)
                    canvas.drawText(methodFa, 215f, currentY + 13f, textPaint)
                    canvas.drawText(if (pay.referenceNo.isNotBlank()) PersianUtils.toPersianDigits(pay.referenceNo) else "-", 295f, currentY + 13f, textPaint)
                    
                    boldPaint.textSize = 8.5f
                    canvas.drawText(PersianUtils.formatNumberWithCommas(pay.amount), 385f, currentY + 13f, boldPaint)
                    val pDesc = pay.description.ifBlank { "تسویه حساب" }
                    textPaint.textSize = if (pDesc.length > 18) 7f else 8f
                    canvas.drawText(pDesc.take(20), 475f, currentY + 13f, textPaint)

                    currentY += rowH
                }
                currentY += 14f
            }

            // Financial Summary Block
            val summaryTop = currentY
            canvas.drawRoundRect(20f, summaryTop, (pageWidth - 20).toFloat(), summaryTop + 65f, 8f, 8f, Paint().apply {
                color = Color.rgb(241, 245, 249)
                style = Paint.Style.FILL
            })
            canvas.drawRoundRect(20f, summaryTop, (pageWidth - 20).toFloat(), summaryTop + 65f, 8f, 8f, borderPaint)

            boldPaint.textSize = 9.5f
            canvas.drawText("مجموع کل کارکرد و فاکتورها: ${PersianUtils.formatCurrency(totalWork, currencyUnit)}", 35f, summaryTop + 22f, boldPaint)
            canvas.drawText("مجموع کل دریافتی‌ها: ${PersianUtils.formatCurrency(totalPaid, currencyUnit)}", 35f, summaryTop + 42f, boldPaint)

            val status = if (balance > 0) "باقی مانده حساب: ${PersianUtils.formatCurrency(balance, currencyUnit)}"
                         else if (balance == 0L) "باقی مانده حساب: تسویه کامل"
                         else "باقی مانده حساب (بدهکاری): -${PersianUtils.formatCurrency(Math.abs(balance), currencyUnit)} (بدهکاری)"
            
            val statPaint = if (balance == 0L) greenPaint else redPaint
            statPaint.textSize = 10f
            canvas.drawText(status, (pageWidth - 275).toFloat(), summaryTop + 34f, statPaint)

            // Footer
            textPaint.textSize = 7.5f
            textPaint.color = Color.rgb(148, 163, 184)
            canvas.drawText("فایل PDF صورتحساب کارکرد و دریافتی ها ساخته شده با خیاطان", (pageWidth / 2 - 120).toFloat(), (pageHeight - 20).toFloat(), textPaint)

            doc.finishPage(page)

            // Save PDF to cache
            val cacheDir = File(context.cacheDir, "pdf_invoices")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val sanitizedCustomer = if (targetCustomer.isNotBlank()) "_${targetCustomer.replace(" ", "_")}" else ""
            val pdfFile = File(cacheDir, "Khayyaton_Invoice${sanitizedCustomer}_${System.currentTimeMillis()}.pdf")
            val fos = FileOutputStream(pdfFile)
            doc.writeTo(fos)
            fos.close()
            doc.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "فایل PDF صورتحساب کارکرد و دریافتی ها ساخته شده با خیاطان")
                putExtra(Intent.EXTRA_TEXT, "فایل PDF صورتحساب کارکرد و دریافتی ها ساخته شده با خیاطان")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری صورت‌حساب PDF"))
        } catch (e: Exception) {
        }
    }
}
