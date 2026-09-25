package com.example.ui.dialogs

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.FurnitureOrder
import com.example.model.PaymentRecord
import com.example.ui.components.KhayyatonLogo
import com.example.ui.theme.Amber600
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Rose600
import com.example.util.InvoiceDocumentGenerator
import com.example.util.ExcelExportUtil
import com.example.util.PersianUtils

@Composable
fun InvoiceDialog(
    isOpen: Boolean,
    selectedOrder: FurnitureOrder?,
    orders: List<FurnitureOrder>,
    payments: List<PaymentRecord>,
    currencyUnit: String,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current
    val displayOrders = if (selectedOrder != null) listOf(selectedOrder) else orders
    val targetCustomer = selectedOrder?.customerName ?: ""
    val customerPayments = if (selectedOrder != null) {
        payments.filter { it.customerName == selectedOrder.customerName || it.relatedOrderId == selectedOrder.id }
    } else payments

    val totalWork = displayOrders.sumOf { it.calculatedTotal }
    val totalPaid = customerPayments.sumOf { it.amount }
    val balance = totalWork - totalPaid
    val todayDate = PersianUtils.getTodayJalaliString()
    val tableHorizontalScroll = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f)
                .padding(vertical = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Toolbar Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (selectedOrder != null) "فاکتور و تسویه حساب #${PersianUtils.toPersianDigits(selectedOrder.invoiceNumber)}" else "صورت حساب و کارکرد کلی",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (targetCustomer.isNotBlank()) "طرف حساب: $targetCustomer" else "نمایش جدول کارکرد و دریافتی ها",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Action Bar for PDF / HTML / Text Export
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // PDF Export Button
                    Button(
                        onClick = {
                            InvoiceDocumentGenerator.sharePdfInvoice(
                                context = context,
                                orders = displayOrders,
                                payments = customerPayments,
                                targetCustomer = targetCustomer,
                                currencyUnit = currencyUnit,
                                currentDate = todayDate
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Rose600),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Outlined.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("خروجی PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Excel Export Button
                    Button(
                        onClick = {
                            ExcelExportUtil.shareInvoiceExcel(
                                context = context,
                                orders = displayOrders,
                                payments = customerPayments,
                                currencyUnit = currencyUnit
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(imageVector = Icons.Outlined.TableView, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("خروجی Excel", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // HTML Export Button
                    Button(
                        onClick = {
                            InvoiceDocumentGenerator.shareHtmlInvoice(
                                context = context,
                                orders = displayOrders,
                                payments = customerPayments,
                                targetCustomer = targetCustomer,
                                currencyUnit = currencyUnit,
                                currentDate = todayDate
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Outlined.Html, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("خروجی HTML", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Scrollable Table and Statement Body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Workshop Branding Header
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            KhayyatonLogo(fontSize = 20.sp)
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "تاریخ صدور: ${PersianUtils.toPersianDigits(todayDate)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (targetCustomer.isNotBlank()) "پرداخت‌کننده: $targetCustomer" else "مجموع فاکتور های کارکرد و دریافتی",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Section Title 1: Orders Table
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "جدول کارکرد و دریافتی ها:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                            Text(
                                text = "${PersianUtils.toPersianDigits(displayOrders.size)} فاکتور",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Professional Lined Orders Table
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.horizontalScroll(tableHorizontalScroll)
                    ) {
                        Column(modifier = Modifier.width(760.dp)) {
                            // Table Header Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 8.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("ردیف / تاریخ / فاکتور", fontSize = 10.5.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                                Text("مدل مبل", fontSize = 10.5.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text("اجزا و واحد", fontSize = 10.5.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text("دستمزد کل ($currencyUnit)", fontSize = 10.5.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                            }

                            HorizontalDivider(thickness = 1.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                            if (displayOrders.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("فاکتوری برای نمایش وجود ندارد.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                displayOrders.forEachIndexed { idx, ord ->
                                    val orderColor = PersianUtils.parseColor(
                                        if (ord.colorCode.isNotBlank()) ord.colorCode else PersianUtils.getModelColor(ord.modelName)
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(orderColor.copy(alpha = 0.10f))
                                            .padding(horizontal = 8.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Left color indicator bar for row
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(28.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(orderColor)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))

                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${idx + 1}. ${PersianUtils.toPersianDigits(ord.dateJalali.takeLast(8))} #${PersianUtils.toPersianDigits(ord.invoiceNumber)}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.weight(1f),
                                                    textAlign = TextAlign.Start
                                                )
                                                Text(
                                                    text = ord.modelName,
                                                    fontSize = if (ord.modelName.length > 14) 9.5.sp else 11.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = orderColor,
                                                    modifier = Modifier.weight(1f),
                                                    textAlign = TextAlign.Center
                                                )
                                                Text(
                                                    text = "${PersianUtils.toPersianDigits(ord.countFormula)} (${PersianUtils.formatNumberWithCommas(ord.calculatedUnits)})",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.weight(1f),
                                                    textAlign = TextAlign.Center
                                                )
                                                Text(
                                                    text = PersianUtils.formatNumberWithCommas(ord.calculatedTotal),
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Emerald600,
                                                    modifier = Modifier.weight(1f),
                                                    textAlign = TextAlign.End
                                                )
                                            }

                                            // Full expandable descriptions row (Fabric & Notes)
                                            if (ord.fabricName.isNotBlank() || ord.notes.isNotBlank() || (targetCustomer.isBlank() && ord.customerName.isNotBlank())) {
                                                val infoList = mutableListOf<String>()
                                                if (targetCustomer.isBlank() && ord.customerName.isNotBlank()) infoList.add("مشتری: ${ord.customerName}")
                                                if (ord.fabricName.isNotBlank()) infoList.add("پارچه: ${ord.fabricName}")
                                                if (ord.notes.isNotBlank()) infoList.add("توضیحات: ${ord.notes}")
                                                val fullText = infoList.joinToString(" | ")
                                                val dynamicFontSize = if (fullText.length > 60) 8.5.sp else 9.5.sp
                                                Text(
                                                    text = fullText,
                                                    fontSize = dynamicFontSize,
                                                    lineHeight = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(top = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (idx < displayOrders.size - 1) {
                                        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                    }
                                }
                            }
                        }
                    }

                    // Section Title 2: Payments Table
                    if (customerPayments.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "جدول سوابق دریافتی‌ها و واریزی‌ها:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Badge(containerColor = Emerald600.copy(alpha = 0.2f)) {
                                Text(
                                    text = "${PersianUtils.toPersianDigits(customerPayments.size)} دریافتی",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald600
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Emerald600.copy(alpha = 0.4f)),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.horizontalScroll(tableHorizontalScroll)
                        ) {
                            Column(modifier = Modifier.width(760.dp)) {
                                // Payment Table Header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Emerald600.copy(alpha = 0.12f))
                                        .padding(horizontal = 8.dp, vertical = 9.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("ردیف / تاریخ", fontSize = 10.5.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                                    Text("پرداخت‌کننده / بانک", fontSize = 10.5.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                    Text("کارت / پیگیری", fontSize = 10.5.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                    Text("مبلغ ($currencyUnit)", fontSize = 10.5.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                }

                                HorizontalDivider(thickness = 1.5.dp, color = Emerald600.copy(alpha = 0.3f))

                                customerPayments.forEachIndexed { idx, pay ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (idx % 2 == 1) Emerald600.copy(alpha = 0.05f) else Color.Transparent)
                                            .padding(horizontal = 8.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${idx + 1}. ${PersianUtils.toPersianDigits(pay.dateJalali)}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = pay.customerName.ifBlank { "عمومی" },
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Center
                                            )
                                            val bankText = if (pay.bankName.isNotBlank()) pay.bankName else if (pay.description.isNotBlank()) pay.description else ""
                                            if (bankText.isNotBlank()) {
                                                val pDescSize = if (bankText.length > 20) 8.5.sp else 9.sp
                                                Text(
                                                    text = bankText,
                                                    fontSize = pDescSize,
                                                    color = Emerald600,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                        val cardOrRef = when {
                                            pay.cardNumber.isNotBlank() -> pay.cardNumber
                                            pay.referenceNo.isNotBlank() -> pay.referenceNo
                                            else -> "-"
                                        }
                                        Text(
                                            text = PersianUtils.toPersianDigits(cardOrRef),
                                            fontSize = 9.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.weight(1f),
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = PersianUtils.formatNumberWithCommas(pay.amount),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Emerald600,
                                            modifier = Modifier.weight(1f),
                                            textAlign = TextAlign.End
                                        )
                                    }
                                    if (idx < customerPayments.size - 1) {
                                        HorizontalDivider(thickness = 1.dp, color = Emerald600.copy(alpha = 0.15f))
                                    }
                                }
                            }
                        }
                    }

                    // Financial Summary Card
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "مجموع کل کارکرد و فاکتورها:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = PersianUtils.formatCurrency(totalWork, currencyUnit),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "مجموع کل دریافتی‌ها و واریزی‌ها:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = PersianUtils.formatCurrency(totalPaid, currencyUnit),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Emerald600
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val status = if (balance > 0) "باقی مانده حساب:" else if (balance == 0L) "باقی مانده حساب:" else "باقی مانده حساب (بدهکاری):"
                                val col = if (balance > 0) Rose600 else if (balance == 0L) Emerald600 else Rose600
                                Text(text = status, fontSize = 13.sp, fontWeight = FontWeight.Black, color = col)
                                Text(
                                    text = PersianUtils.formatRemainingBalanceText(balance, currencyUnit),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = col
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
