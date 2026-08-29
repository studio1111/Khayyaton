package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PaymentRecord
import com.example.ui.theme.Emerald600
import com.example.util.PersianUtils

private val CardOuterDarkBg = Color(0xFF131826)
private val CardCellDarkBg = Color(0xFF1B2134)
private val CardCellBorder = Color(0xFF2B334D)
private val CardGoldBorder = Color(0xFFD4A017)
private val LabelGrayText = Color(0xFF9AA4BF)
private val ValueWhiteText = Color(0xFFF1F5F9)
private val EditButtonGreen = Color(0xFF107C41)
private val DeleteButtonRed = Color(0xFFA82828)
private val ShareButtonBlue = Color(0xFF1D4ED8)

@Composable
fun RecentPayments(
    payments: List<PaymentRecord>,
    currencyUnit: String,
    onOpenNewPayment: () -> Unit,
    onEditPayment: (PaymentRecord) -> Unit,
    onDeletePayment: (PaymentRecord) -> Unit,
    onSharePayment: (PaymentRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header Row for Recent Payments Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Emerald600.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = Emerald600,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "دریافتی‌های اخیر",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Emerald600.copy(alpha = 0.1f))
                        .border(1.dp, Emerald600.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "${PersianUtils.toPersianDigits(payments.size)} فقره",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Emerald600
                    )
                }
            }

            TextButton(
                onClick = onOpenNewPayment,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = Emerald600)
            ) {
                Text(
                    text = "ثبت دریافتی جدید",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Empty state or list of payment cards
        if (payments.isEmpty()) {
            Surface(
                color = CardOuterDarkBg,
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, CardGoldBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "هنوز دریافتی ثبت نشده است.",
                        fontSize = 12.sp,
                        color = LabelGrayText
                    )
                }
            }
        } else {
            payments.forEachIndexed { idx, payment ->
                PaymentCardItem(
                    index = idx + 1,
                    payment = payment,
                    currencyUnit = currencyUnit,
                    onEdit = { onEditPayment(payment) },
                    onDelete = { onDeletePayment(payment) },
                    onShare = { onSharePayment(payment) }
                )
            }
        }
    }
}

@Composable
fun PaymentCardItem(
    index: Int,
    payment: PaymentRecord,
    currencyUnit: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val methodIcon = when (payment.paymentType) {
        "transfer" -> Icons.Default.PhoneAndroid
        "cash" -> Icons.Default.Payments
        "pos" -> Icons.Default.CreditCard
        "cheque" -> Icons.Default.Description
        else -> Icons.Default.Payment
    }
    val methodTitle = when (payment.paymentType) {
        "transfer" -> "کارت به کارت / حواله"
        "cash" -> "نقدی"
        "pos" -> "کارتخوان POS"
        "cheque" -> "چک بانکی"
        else -> "واریزی"
    }

    Surface(
        color = CardOuterDarkBg,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(2.5.dp, CardGoldBorder),
        shadowElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // TOP HEADER: Spacing + Row Number ("شماره ردیف")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Row Number Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "شماره ردیف: ${PersianUtils.toPersianDigits(if (payment.paymentNumber > 0) payment.paymentNumber else index.toLong())}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Status Pill
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

            // ROW 1: [پرداخت‌کننده] (Right) | [مبلغ دریافتی] (Left)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Right: پرداخت‌کننده
                PaymentCell(
                    label = "پرداخت‌کننده",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = LabelGrayText,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = payment.customerName.ifBlank { "عمومی" },
                            color = ValueWhiteText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Left: مبلغ دریافتی
                PaymentCell(
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
                        Text(
                            text = currencyUnit,
                            color = LabelGrayText,
                            fontSize = 10.sp
                        )
                        Text(
                            text = PersianUtils.formatNumberWithCommas(payment.amount),
                            color = Emerald600,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // ROW 2: [نوع پرداخت] (Right) | [کد پیگیری / سند] (Left)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Right: نوع پرداخت / بانک
                val bankDetail = if (payment.bankName.isNotBlank()) " (${payment.bankName})" else ""
                PaymentCell(
                    label = if (payment.bankName.isNotBlank()) "نوع پرداخت و بانک" else "نوع پرداخت",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = methodIcon,
                            contentDescription = null,
                            tint = Emerald600,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$methodTitle$bankDetail",
                            color = ValueWhiteText,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Left: کد پیگیری
                PaymentCell(
                    label = "کد پیگیری",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Text(
                        text = if (payment.referenceNo.isNotBlank()) PersianUtils.toPersianDigits(payment.referenceNo) else "-",
                        color = ValueWhiteText,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ROW 3: [تاریخ] (Right) | [شماره کارت یا حساب دریافتی] (Left)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Right: تاریخ
                PaymentCell(
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

                // Left: شماره کارت یا حساب دریافتی
                val cardOrAccount = when {
                    payment.cardNumber.isNotBlank() -> payment.cardNumber
                    payment.referenceNo.isNotBlank() -> payment.referenceNo
                    else -> "-"
                }
                PaymentCell(
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ROW 4: [توضیحات] (Full width cell)
            PaymentCell(
                label = "توضیحات / بابت",
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = payment.description.ifBlank { "بابت تسویه حساب سفارش" },
                    color = ValueWhiteText,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // BOTTOM ACTION BUTTONS: [حذف] (Red) & [ویرایش] (Green) & [اشتراک گذاری] (Blue)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // حذف Button (Red)
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DeleteButtonRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = "حذف",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // ویرایش Button (Green)
                    Button(
                        onClick = onEdit,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EditButtonGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = "ویرایش",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // اشتراک گذاری Button (Blue)
                    Button(
                        onClick = onShare,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ShareButtonBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "اشتراک گذاری",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "اشتراک گذاری",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentCell(
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
