package com.example.ui.dialogs

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.FurnitureOrder
import com.example.model.PaymentRecord
import com.example.ui.theme.*
import com.example.util.CardImageSharer
import com.example.util.PersianUtils

private val CardCellDarkBg = Color(0xFF0F172A).copy(alpha = 0.82f)
private val CardCellBorder = Color.White.copy(alpha = 0.15f)
private val CardGoldBorder = Color(0xFFD4A017)
private val LabelGrayText = Color(0xFFCBD5E1)
private val ValueWhiteText = Color(0xFFFFFFFF)
private val PaymentCardPreviewBg = Color(0xFF10271D)
private val PaymentNeonGreenPreview = Color(0xFF39FF88)

@Composable
fun CardShareDialog(
    isOpen: Boolean,
    order: FurnitureOrder?,
    payment: PaymentRecord?,
    currencyUnit: String,
    totalWork: Long,
    totalReceived: Long,
    remainingBalance: Long,
    orderCount: Int,
    paymentCount: Int,
    onDismiss: () -> Unit
) {
    if (!isOpen || (order == null && payment == null)) return

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val title = if (order != null) "فاکتور_${order.modelName}_${order.invoiceNumber}" else "دریافتی_${payment?.customerName ?: "سند"}"

    // Generate bitmap lazily on share or save
    fun getGeneratedBitmap(): Bitmap {
        return if (order != null) {
            CardImageSharer.generateOrderCardBitmap(
                context = context,
                order = order,
                currencyUnit = currencyUnit,
                totalWork = totalWork,
                totalReceived = totalReceived,
                remainingBalance = remainingBalance,
                orderCount = orderCount,
                paymentCount = paymentCount
            )
        } else {
            CardImageSharer.generatePaymentCardBitmap(
                context = context,
                payment = payment!!,
                currencyUnit = currencyUnit,
                totalWork = totalWork,
                totalReceived = totalReceived,
                remainingBalance = remainingBalance,
                orderCount = orderCount,
                paymentCount = paymentCount
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = if (order != null) "تصویر کارت سفارش #${PersianUtils.toPersianDigits(order.invoiceNumber)}" else "تصویر سند دریافتی",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Scrollable Content - Card snapshot + Footer
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Top App Branding Header matching image
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "ساخته شده با برنامه خیاطان",
                                color = Color(0xFFF8FAFC),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // 1. Rendered Card (Without bottom action buttons)
                    if (order != null) {
                        val modelColor = PersianUtils.parseColor(
                            if (order.colorCode.isNotBlank()) order.colorCode else PersianUtils.getModelColor(order.modelName)
                        )

                        Surface(
                            color = modelColor,
                            shape = RoundedCornerShape(24.dp),
                            border = androidx.compose.foundation.BorderStroke(2.5.dp, CardGoldBorder),
                            shadowElevation = 4.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Top Row Number Badge
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black.copy(alpha = 0.45f))
                                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "شماره ردیف: ${PersianUtils.toPersianDigits(order.orderNumber)}",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Row 1: مدل مبل | دستمزد هر دست
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PreviewCell(
                                        label = "مدل مبل",
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    ) {
                                        Text(
                                            text = order.modelName,
                                            color = ValueWhiteText,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    PreviewCell(
                                        label = "دستمزد هر دست",
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 4.dp)
                                        ) {
                                            Text(text = currencyUnit, color = LabelGrayText, fontSize = 10.sp)
                                            Text(
                                                text = PersianUtils.formatNumberWithCommas(order.pricePerSet),
                                                color = ValueWhiteText,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                }

                                // Row 2: تعداد واحد در یک دست | تعداد
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PreviewCell(
                                        label = "تعداد واحد در یک دست",
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    ) {
                                        Text(
                                            text = PersianUtils.toPersianDigits(order.unitsPerSet),
                                            color = ValueWhiteText,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    PreviewCell(
                                        label = "تعداد",
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    ) {
                                        Text(
                                            text = PersianUtils.toPersianDigits(order.countFormula),
                                            color = ValueWhiteText,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                // Row 3: مجموع واحد | دستمزد
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PreviewCell(
                                        label = "مجموع واحد",
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    ) {
                                        Text(
                                            text = PersianUtils.formatNumberWithCommas(order.calculatedUnits),
                                            color = ValueWhiteText,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    PreviewCell(
                                        label = "دستمزد",
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 4.dp)
                                        ) {
                                            Text(text = currencyUnit, color = LabelGrayText, fontSize = 10.sp)
                                            Text(
                                                text = PersianUtils.formatNumberWithCommas(order.calculatedTotal),
                                                color = ValueWhiteText,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                }

                                // Row 4: تاریخ | شماره فاکتور
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PreviewCell(
                                        label = "تاریخ",
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    ) {
                                        Text(
                                            text = PersianUtils.toPersianDigits(order.dateJalali),
                                            color = ValueWhiteText,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    PreviewCell(
                                        label = "شماره فاکتور",
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    ) {
                                        Text(
                                            text = PersianUtils.toPersianDigits(order.invoiceNumber),
                                            color = ValueWhiteText,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                // Row 5: مشتری / نمایشگاه | توضیحات
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PreviewCell(
                                        label = "مشتری / نمایشگاه",
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    ) {
                                        Text(
                                            text = order.customerName.ifBlank { "مشتری عمومی" },
                                            color = ValueWhiteText,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    PreviewCell(
                                        label = "توضیحات",
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    ) {
                                        val fullNotes = buildString {
                                            if (order.fabricName.isNotBlank()) {
                                                append("پارچه: ${order.fabricName}")
                                            }
                                            if (order.notes.isNotBlank()) {
                                                if (order.fabricName.isNotBlank()) append(" - ")
                                                append(order.notes)
                                            }
                                        }.ifBlank { "-" }
                                        Text(
                                            text = fullNotes,
                                            color = ValueWhiteText,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    } else if (payment != null) {
                        // Payment Card Preview
                        Surface(
                            color = PaymentCardPreviewBg,
                            shape = RoundedCornerShape(24.dp),
                            border = androidx.compose.foundation.BorderStroke(2.5.dp, CardGoldBorder),
                            shadowElevation = 4.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Top Row Number Badge
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black.copy(alpha = 0.45f))
                                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "شماره ردیف: ${PersianUtils.toPersianDigits(payment.paymentNumber)}",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Emerald600.copy(alpha = 0.2f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "دریافتی",
                                            color = Emerald600,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Row 1: پرداخت‌کننده | مبلغ دریافتی
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PreviewCell(
                                        label = "پرداخت‌کننده",
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    ) {
                                        Text(
                                            text = payment.customerName.ifBlank { "عمومی" },
                                            color = ValueWhiteText,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    PreviewCell(
                                        label = "مبلغ دریافتی",
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 4.dp)
                                        ) {
                                            Text(text = currencyUnit, color = LabelGrayText, fontSize = 10.sp)
                                            Text(
                                                text = PersianUtils.formatNumberWithCommas(payment.amount),
                                                color = Emerald600,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                }

                                // Row 2: نوع پرداخت | کد پیگیری
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val methodTitle = when (payment.paymentType) {
                                        "transfer" -> "کارت به کارت / حواله"
                                        "cash" -> "نقدی"
                                        "pos" -> "کارتخوان POS"
                                        "cheque" -> "چک بانکی"
                                        else -> "واریزی"
                                    }
                                    PreviewCell(
                                        label = "نوع پرداخت",
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    ) {
                                        Text(
                                            text = methodTitle,
                                            color = ValueWhiteText,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    PreviewCell(
                                        label = "کد پیگیری",
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    ) {
                                        Text(
                                            text = if (payment.referenceNo.isNotBlank()) PersianUtils.toPersianDigits(payment.referenceNo) else "-",
                                            color = ValueWhiteText,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                // Row 3: تاریخ | شماره کارت یا حساب دریافتی
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PreviewCell(
                                        label = "تاریخ",
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    ) {
                                        Text(
                                            text = PersianUtils.toPersianDigits(payment.dateJalali),
                                            color = ValueWhiteText,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    val cardOrAccount = when {
                                        payment.cardNumber.isNotBlank() -> payment.cardNumber
                                        payment.referenceNo.isNotBlank() -> payment.referenceNo
                                        else -> "-"
                                    }
                                    PreviewCell(
                                        label = "شماره کارت یا حساب دریافتی",
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    ) {
                                        Text(
                                            text = PersianUtils.toPersianDigits(cardOrAccount),
                                            color = ValueWhiteText,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                // Row 4: توضیحات / بابت
                                PreviewCell(label = "توضیحات / بابت", modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = payment.description.ifBlank { "بابت تسویه حساب سفارش" },
                                        color = ValueWhiteText,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // 2. Summary Footer Bar below card (Matching main screen bottom)
                    Surface(
                        color = Slate950,
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Slate800),
                        shadowElevation = 6.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // مجموع کارکرد
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "مجموع کارکرد (${PersianUtils.toPersianDigits(orderCount)})",
                                    fontSize = 10.sp,
                                    color = Slate400,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = PersianUtils.formatCurrency(totalWork, currencyUnit),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }

                            Box(modifier = Modifier.width(1.dp).height(28.dp).background(Slate800))

                            // کل دریافتی
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "کل دریافتی (${PersianUtils.toPersianDigits(paymentCount)})",
                                    fontSize = 10.sp,
                                    color = Slate400,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = PersianUtils.formatCurrency(totalReceived, currencyUnit),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Emerald400
                                )
                            }

                            Box(modifier = Modifier.width(1.dp).height(28.dp).background(Slate800))

                            // باقی‌مانده (بدون کلمه بدهکار)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                val balanceColor = when {
                                    remainingBalance > 0 -> Rose400
                                    remainingBalance == 0L -> Emerald400
                                    else -> Amber500
                                }
                                Text(
                                    text = if (remainingBalance == 0L) "تسویه کامل" else "باقی‌مانده",
                                    fontSize = 10.sp,
                                    color = Slate400,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = PersianUtils.formatCurrency(Math.abs(remainingBalance), currencyUnit),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = balanceColor
                                )
                            }
                        }
                    }
                }

                // Bottom Action Buttons: [ذخیره در گالری] & [اشتراک‌گذاری تصویر]
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val shareCaption = if (order != null) {
                            "نوع کارت: کارکرد\n" + CardImageSharer.buildOrderCardText(order)
                        } else if (payment != null) {
                            "نوع کارت: دریافتی\n" + CardImageSharer.buildPaymentCardText(payment, currencyUnit)
                        } else ""

                        val fileNamePrefix = if (order != null) {
                            CardImageSharer.buildOrderFileName(order)
                        } else if (payment != null) {
                            CardImageSharer.buildPaymentFileName(payment)
                        } else title

                        // Save to Gallery Button
                        Button(
                            onClick = {
                                val bmp = getGeneratedBitmap()
                                CardImageSharer.saveBitmapToGallery(context, bmp, fileNamePrefix)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SaveAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "ذخیره در گالری",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Share Image Button
                        Button(
                            onClick = {
                                val bmp = getGeneratedBitmap()
                                CardImageSharer.shareBitmap(
                                    context = context,
                                    bitmap = bmp,
                                    subjectTitle = title,
                                    shareText = shareCaption,
                                    fileNamePrefix = fileNamePrefix
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1D4ED8),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(44.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "اشتراک‌گذاری تصویر",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewCell(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = CardCellDarkBg,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardCellBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = LabelGrayText,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center
            )
            content()
        }
    }
}
