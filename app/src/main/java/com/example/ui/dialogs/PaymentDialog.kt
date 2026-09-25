package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.PaymentRecord
import com.example.ui.theme.Emerald600
import com.example.util.PersianUtils

data class PaymentTypeItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun PaymentDialog(
    isOpen: Boolean,
    initialPayment: PaymentRecord?,
    currencyUnit: String,
    customers: List<String>,
    nextPaymentNumber: Long,
    remainingBalance: Long,
    existingPayments: List<PaymentRecord> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (PaymentRecord) -> Unit
) {
    if (!isOpen) return

    var amountStr by remember {
        mutableStateOf((initialPayment?.amount ?: 5000000L).toString())
    }
    val lastPayerSuggestion = remember(existingPayments) {
        existingPayments.asSequence()
            .sortedByDescending { it.createdAt }
            .map { it.customerName.trim() }
            .firstOrNull { it.isNotBlank() } ?: ""
    }

    // پرداخت‌کننده کاملاً مستقل از مشتری فاکتور است.
    var customerName by remember(initialPayment, lastPayerSuggestion) {
        mutableStateOf(initialPayment?.customerName ?: lastPayerSuggestion)
    }
    var bankName by remember {
        mutableStateOf(initialPayment?.bankName ?: "")
    }
    var cardNumber by remember {
        mutableStateOf(initialPayment?.cardNumber ?: "")
    }
    var description by remember {
        mutableStateOf(initialPayment?.description ?: "")
    }
    var paymentType by remember {
        mutableStateOf(initialPayment?.paymentType ?: "transfer")
    }
    var referenceNo by remember {
        mutableStateOf(initialPayment?.referenceNo ?: "")
    }
    var dateJalali by remember {
        mutableStateOf(initialPayment?.dateJalali ?: PersianUtils.getTodayJalaliString())
    }
    var isDatePickerOpen by remember { mutableStateOf(false) }

    val currentAmount = remember(amountStr) {
        PersianUtils.toEnglishDigits(amountStr).toLongOrNull() ?: 0L
    }

    val bankSuggestions = remember(existingPayments) {
        val standardBanks = listOf("بانک ملی", "بانک ملت", "بانک صادرات", "بانک تجارت", "بانک سپه", "بانک سامان", "بانک پاسارگاد", "بانک رسالت", "بلوبانک", "بانک مهر ایران", "بانک کشاورزی")
        val fromHistory = existingPayments.map { it.bankName.trim() }.filter { it.isNotBlank() }
        (fromHistory + standardBanks).distinct()
    }

    val recentCards = remember(existingPayments) {
        existingPayments
            .map { it.cardNumber.trim() to it.bankName.trim() }
            .filter { it.first.isNotBlank() }
            .distinctBy { it.first }
    }

    val paymentOptions = listOf(
        PaymentTypeItem("transfer", "کارت به کارت / حواله", "انتقال بین‌بانکی یا ساتنا/پایا", Icons.Default.PhoneAndroid, Color(0xFF2563EB)),
        PaymentTypeItem("cash", "پرداخت نقدی", "دریافت وجه نقد در کارگاه", Icons.Default.Payments, Emerald600),
        PaymentTypeItem("pos", "کارتخوان کارگاه (POS)", "تراکنش دستگاه کارتخوان", Icons.Default.CreditCard, Color(0xFF9333EA)),
        PaymentTypeItem("cheque", "چک بانکی صیادی", "ثبت شماره و موعد چک", Icons.Default.Description, Color(0xFFD97706))
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Emerald600.copy(alpha = 0.1f))
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
                                .background(Emerald600),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = if (initialPayment != null) "ویرایش سند دریافتی" else "ثبت دریافتی",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Form Body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Amount Input & Quick Shortcuts
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مبلغ دریافتی ($currencyUnit) *",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = PersianUtils.formatCurrency(currentAmount, currencyUnit),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Emerald600
                            )
                        }

                        OutlinedTextField(
                            value = amountStr,
                            onValueChange = { amountStr = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Quick Amount Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val shortcuts = listOf(5000000L to "۵ میلیون", 10000000L to "۱۰ میلیون", 20000000L to "۲۰ میلیون")
                            shortcuts.forEach { (amt, label) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { amountStr = amt.toString() }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            if (remainingBalance > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Emerald600.copy(alpha = 0.15f))
                                        .border(1.dp, Emerald600.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .clickable { amountStr = remainingBalance.toString() }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "تسویه مانده (${PersianUtils.formatCurrency(remainingBalance, currencyUnit)})",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Emerald600
                                    )
                                }
                            }
                        }
                    }

                    // Customer Name & Date
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            label = { Text("پرداخت‌کننده", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                            placeholder = {
                                Text(
                                    if (lastPayerSuggestion.isNotBlank()) "پیشنهاد: $lastPayerSuggestion" else "نام پرداخت‌کننده",
                                    fontSize = 10.sp
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.2f)
                        )

                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .clickable { isDatePickerOpen = true }
                        ) {
                            OutlinedTextField(
                                value = PersianUtils.toPersianDigits(dateJalali),
                                onValueChange = { dateJalali = PersianUtils.toEnglishDigits(it) },
                                label = { Text("تاریخ (انتخاب)", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = { isDatePickerOpen = true }) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = "انتخاب تاریخ",
                                            tint = Emerald600
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Payment Method Selector (4 Tiles)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "نحوه دریافت و روش پرداخت:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        paymentOptions.forEach { opt ->
                            val isSelected = paymentType == opt.id
                            Surface(
                                color = if (isSelected) opt.color.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) opt.color else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { paymentType = opt.id }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(opt.color.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = opt.icon,
                                                contentDescription = null,
                                                tint = opt.color,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = opt.title,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = opt.subtitle,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = opt.color,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bank Name & Card/Account Number Section
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "مشخصات حساب و واریز (بانک و کارت):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Bank Name Input
                        OutlinedTextField(
                            value = bankName,
                            onValueChange = { bankName = it },
                            label = { Text("نام بانک (مثلاً بانک ملی)", fontSize = 10.5.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Bank Suggestions Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "بانک‌ها:",
                                fontSize = 9.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            bankSuggestions.forEach { b ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (bankName == b) Emerald600.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface)
                                        .border(
                                            1.dp,
                                            if (bankName == b) Emerald600 else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable { bankName = b }
                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = b,
                                        fontSize = 10.sp,
                                        fontWeight = if (bankName == b) FontWeight.Bold else FontWeight.Normal,
                                        color = if (bankName == b) Emerald600 else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // Card / Account Number Input
                        OutlinedTextField(
                            value = cardNumber,
                            onValueChange = { cardNumber = it },
                            label = { Text("شماره کارت / شماره حساب دریافتی", fontSize = 10.5.sp) },
                            placeholder = { Text("مثلاً ۶۰۳۷... یا ۱۲۳۴...", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Recent Card Numbers Suggestions
                        if (recentCards.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "کارت‌های قبلی:",
                                    fontSize = 9.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                recentCards.forEach { (card, bName) ->
                                    val label = if (bName.isNotBlank()) "$card ($bName)" else card
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (cardNumber == card) Color(0xFF2563EB).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                                            .border(
                                                1.dp,
                                                if (cardNumber == card) Color(0xFF2563EB) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .clickable {
                                                cardNumber = card
                                                if (bName.isNotBlank() && bankName.isBlank()) {
                                                    bankName = bName
                                                }
                                            }
                                            .padding(horizontal = 7.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = PersianUtils.toPersianDigits(label),
                                            fontSize = 9.5.sp,
                                            fontWeight = if (cardNumber == card) FontWeight.Bold else FontWeight.Normal,
                                            color = if (cardNumber == card) Color(0xFF2563EB) else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Reference / Tracking Number
                    OutlinedTextField(
                        value = referenceNo,
                        onValueChange = { referenceNo = it },
                        label = { Text("کد پیگیری تراکنش / شماره چک صیادی", fontSize = 11.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Description
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("شرح و بابت واریز", fontSize = 11.sp) },
                        placeholder = { Text("مثلاً بیعانه ساخت، تسویه فاکتور و...", fontSize = 11.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Footer Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = "انصراف", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (currentAmount <= 0) return@Button
                            val pay = PaymentRecord(
                                id = initialPayment?.id ?: 0L,
                                paymentNumber = initialPayment?.paymentNumber ?: nextPaymentNumber,
                                amount = currentAmount,
                                dateJalali = dateJalali,
                                dateGregorian = initialPayment?.dateGregorian ?: PersianUtils.getTodayGregorianString(),
                                customerName = customerName.trim().ifBlank { "پرداخت‌کننده عمومی" },
                                description = description.trim().ifBlank { "واریزی وجه" },
                                paymentType = paymentType,
                                referenceNo = referenceNo.trim(),
                                bankName = bankName.trim(),
                                cardNumber = cardNumber.trim(),
                                createdAt = initialPayment?.createdAt ?: System.currentTimeMillis()
                            )
                            onSave(pay)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Emerald600,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = if (initialPayment != null) "بروزرسانی سند" else "ثبت نهایی دریافتی",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (isDatePickerOpen) {
        JalaliCalendarDialog(
            isOpen = true,
            onDismiss = { isDatePickerOpen = false },
            onDateSelected = { pickedDate ->
                dateJalali = pickedDate
                isDatePickerOpen = false
            }
        )
    }
}
