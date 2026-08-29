package com.example.ui.dialogs

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.util.JALALI_MONTH_NAMES
import com.example.util.PersianUtils

enum class AnalysisPeriod(val title: String) {
    ALL_TIME("همه زمان‌ها"),
    YEARLY("سالانه"),
    MONTHLY("ماهیانه"),
    WEEKLY("هفتگی")
}

data class ModelStat(
    val name: String,
    val colorCode: String,
    val orderCount: Int,
    val totalUnits: Double,
    val totalAmount: Long,
    val percentOfTotal: Double
)

data class PayerStat(
    val payerName: String,
    val paymentCount: Int,
    val totalPaid: Long,
    val banksUsed: String,
    val percentOfTotalPaid: Double
)

data class MonthTrend(
    val monthIdx: Int,
    val monthName: String,
    val workAmount: Long,
    val paidAmount: Long
)

data class MethodStat(
    val typeKey: String,
    val titleFa: String,
    val count: Int,
    val amount: Long,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisDialog(
    isOpen: Boolean,
    orders: List<FurnitureOrder>,
    payments: List<PaymentRecord>,
    currencyUnit: String,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current
    var selectedPeriod by remember { mutableStateOf(AnalysisPeriod.ALL_TIME) }
    
    // Extract available years from orders and payments
    val availableYears = remember(orders, payments) {
        val years = mutableSetOf<Int>()
        orders.forEach { ord ->
            PersianUtils.parseJalaliParts(ord.dateJalali)?.year?.let { years.add(it) }
        }
        payments.forEach { pay ->
            PersianUtils.parseJalaliParts(pay.dateJalali)?.year?.let { years.add(it) }
        }
        if (years.isEmpty()) {
            val currentJYear = PersianUtils.parseJalaliParts(PersianUtils.getTodayJalaliString())?.year ?: 1403
            years.add(currentJYear)
        }
        years.sortedDescending()
    }

    var selectedYear by remember(availableYears) { mutableStateOf(availableYears.firstOrNull() ?: 1403) }
    var selectedMonth by remember { 
        val currentJMonth = PersianUtils.parseJalaliParts(PersianUtils.getTodayJalaliString())?.month ?: 1
        mutableStateOf(currentJMonth) 
    }
    var selectedWeek by remember { mutableStateOf(1) } // 1..4

    // Filter orders and payments by selected period
    val periodOrders = remember(orders, selectedPeriod, selectedYear, selectedMonth, selectedWeek) {
        when (selectedPeriod) {
            AnalysisPeriod.ALL_TIME -> orders
            AnalysisPeriod.YEARLY -> orders.filter { ord ->
                val p = PersianUtils.parseJalaliParts(ord.dateJalali)
                p != null && p.year == selectedYear
            }
            AnalysisPeriod.MONTHLY -> orders.filter { ord ->
                val p = PersianUtils.parseJalaliParts(ord.dateJalali)
                p != null && p.year == selectedYear && p.month == selectedMonth
            }
            AnalysisPeriod.WEEKLY -> orders.filter { ord ->
                val p = PersianUtils.parseJalaliParts(ord.dateJalali)
                if (p != null && p.year == selectedYear && p.month == selectedMonth) {
                    val weekIdx = ((p.day - 1) / 7) + 1
                    weekIdx == selectedWeek || (selectedWeek == 4 && weekIdx >= 4)
                } else false
            }
        }
    }

    val periodPayments = remember(payments, selectedPeriod, selectedYear, selectedMonth, selectedWeek) {
        when (selectedPeriod) {
            AnalysisPeriod.ALL_TIME -> payments
            AnalysisPeriod.YEARLY -> payments.filter { pay ->
                val p = PersianUtils.parseJalaliParts(pay.dateJalali)
                p != null && p.year == selectedYear
            }
            AnalysisPeriod.MONTHLY -> payments.filter { pay ->
                val p = PersianUtils.parseJalaliParts(pay.dateJalali)
                p != null && p.year == selectedYear && p.month == selectedMonth
            }
            AnalysisPeriod.WEEKLY -> payments.filter { pay ->
                val p = PersianUtils.parseJalaliParts(pay.dateJalali)
                if (p != null && p.year == selectedYear && p.month == selectedMonth) {
                    val weekIdx = ((p.day - 1) / 7) + 1
                    weekIdx == selectedWeek || (selectedWeek == 4 && weekIdx >= 4)
                } else false
            }
        }
    }

    // Calculations
    val totalWork = remember(periodOrders) { periodOrders.sumOf { it.calculatedTotal } }
    val totalUnits = remember(periodOrders) { periodOrders.sumOf { it.calculatedUnits } }
    val totalPaid = remember(periodPayments) { periodPayments.sumOf { it.amount } }
    val remainingBalance = totalWork - totalPaid

    val avgWorkPerOrder = if (periodOrders.isNotEmpty()) totalWork / periodOrders.size else 0L
    val avgWorkPerUnit = if (totalUnits > 0) (totalWork / totalUnits).toLong() else 0L

    // Min & Max Highlights
    val maxWorkOrder = remember(periodOrders) { periodOrders.maxByOrNull { it.calculatedTotal } }
    val minWorkOrder = remember(periodOrders) { periodOrders.minByOrNull { it.calculatedTotal } }
    val maxPayment = remember(periodPayments) { periodPayments.maxByOrNull { it.amount } }
    val minPayment = remember(periodPayments) { periodPayments.minByOrNull { it.amount } }

    // Top Models Aggregation
    val modelStats = remember(periodOrders, totalWork) {
        periodOrders.groupBy { it.modelName }
            .map { (modelName, list) ->
                val mUnits = list.sumOf { it.calculatedUnits }
                val mAmount = list.sumOf { it.calculatedTotal }
                val mColor = list.firstOrNull()?.colorCode?.takeIf { it.isNotBlank() } ?: PersianUtils.getModelColor(modelName)
                val pct = if (totalWork > 0) (mAmount.toDouble() / totalWork) * 100.0 else 0.0
                ModelStat(modelName, mColor, list.size, mUnits, mAmount, pct)
            }
            .sortedByDescending { it.totalAmount }
    }
    val topModel = modelStats.firstOrNull()

    // Top Payers Aggregation (From Payment Records)
    val payerStats = remember(periodPayments, totalPaid) {
        periodPayments.groupBy { it.customerName.trim().ifBlank { "واریزکننده عمومی" } }
            .map { (payer, list) ->
                val pPaid = list.sumOf { it.amount }
                val banks = list.mapNotNull { it.bankName.takeIf { b -> b.isNotBlank() } }.distinct().joinToString("، ")
                val pct = if (totalPaid > 0) (pPaid.toDouble() / totalPaid) * 100.0 else 0.0
                PayerStat(payer, list.size, pPaid, banks.ifBlank { "عمومی" }, pct)
            }
            .sortedByDescending { it.totalPaid }
    }
    val topPayer = payerStats.firstOrNull()

    // Monthly Trend Data for Chart (12 Months of selected Year)
    val monthlyTrends = remember(orders, payments, selectedYear) {
        (1..12).map { m ->
            val mOrders = orders.filter {
                val p = PersianUtils.parseJalaliParts(it.dateJalali)
                p != null && p.year == selectedYear && p.month == m
            }
            val mPayments = payments.filter {
                val p = PersianUtils.parseJalaliParts(it.dateJalali)
                p != null && p.year == selectedYear && p.month == m
            }
            MonthTrend(
                monthIdx = m,
                monthName = JALALI_MONTH_NAMES.getOrElse(m - 1) { "$m" },
                workAmount = mOrders.sumOf { it.calculatedTotal },
                paidAmount = mPayments.sumOf { it.amount }
            )
        }
    }

    // Payment Methods Breakdown
    val methodStats = remember(periodPayments) {
        val groups = periodPayments.groupBy { it.paymentType }
        listOf(
            MethodStat("transfer", "کارت به کارت", groups["transfer"]?.size ?: 0, groups["transfer"]?.sumOf { it.amount } ?: 0L, Color(0xFF2563EB)),
            MethodStat("pos", "کارتخوان", groups["pos"]?.size ?: 0, groups["pos"]?.sumOf { it.amount } ?: 0L, Color(0xFF059669)),
            MethodStat("cash", "نقدی", groups["cash"]?.size ?: 0, groups["cash"]?.sumOf { it.amount } ?: 0L, Color(0xFFD97706)),
            MethodStat("cheque", "چک صیادی", groups["cheque"]?.size ?: 0, groups["cheque"]?.sumOf { it.amount } ?: 0L, Color(0xFF9333EA))
        ).filter { it.amount > 0 || it.count > 0 }
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
                .fillMaxHeight(0.94f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "اطلاعات و آنالیز هوشمند کارگاه",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "تجزیه و تحلیل آماری کارکردها، دریافتی‌ها، نمودارها و رکوردها",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                shareAnalysisReport(
                                    context = context,
                                    periodTitle = selectedPeriod.title,
                                    periodOrders = periodOrders,
                                    periodPayments = periodPayments,
                                    totalWork = totalWork,
                                    totalPaid = totalPaid,
                                    balance = remainingBalance,
                                    currencyUnit = currencyUnit,
                                    maxWorkOrder = maxWorkOrder,
                                    minWorkOrder = minWorkOrder,
                                    maxPayment = maxPayment,
                                    minPayment = minPayment,
                                    topModel = topModel,
                                    topPayer = topPayer
                                )
                            }
                        ) {
                            Icon(imageVector = Icons.Outlined.Share, contentDescription = "اشتراک گزارش")
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "بستن")
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Period Selector Tabs
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AnalysisPeriod.values().forEach { period ->
                                val isSelected = selectedPeriod == period
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedPeriod = period }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = period.title,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Secondary Sub-filter Pickers (Year / Month / Week)
                    AnimatedVisibility(visible = selectedPeriod != AnalysisPeriod.ALL_TIME) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Year Selector
                                Text("سال:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                availableYears.forEach { yr ->
                                    FilterChip(
                                        selected = selectedYear == yr,
                                        onClick = { selectedYear = yr },
                                        label = { Text(PersianUtils.toPersianDigits(yr), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    )
                                }

                                if (selectedPeriod == AnalysisPeriod.MONTHLY || selectedPeriod == AnalysisPeriod.WEEKLY) {
                                    VerticalDivider(modifier = Modifier.height(24.dp))
                                    Text("ماه:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    ScrollableTabRow(
                                        selectedTabIndex = selectedMonth - 1,
                                        edgePadding = 4.dp,
                                        modifier = Modifier.weight(1f),
                                        divider = {}
                                    ) {
                                        (1..12).forEach { m ->
                                            Tab(
                                                selected = selectedMonth == m,
                                                onClick = { selectedMonth = m },
                                                text = {
                                                    Text(
                                                        text = JALALI_MONTH_NAMES.getOrElse(m - 1) { "$m" },
                                                        fontSize = 10.5.sp,
                                                        fontWeight = if (selectedMonth == m) FontWeight.Black else FontWeight.Normal
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }

                                if (selectedPeriod == AnalysisPeriod.WEEKLY) {
                                    VerticalDivider(modifier = Modifier.height(24.dp))
                                    Text("هفته:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    (1..4).forEach { w ->
                                        FilterChip(
                                            selected = selectedWeek == w,
                                            onClick = { selectedWeek = w },
                                            label = { Text("هفته ${PersianUtils.toPersianDigits(w)}", fontSize = 10.5.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section 1: Financial Overview Metric Cards (4 Cards)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Total Work Card
                            MetricSummaryCard(
                                title = "مجموع کارکرد و فاکتورها",
                                mainValue = PersianUtils.formatCurrency(totalWork, currencyUnit),
                                subValue = "${PersianUtils.toPersianDigits(periodOrders.size)} فاکتور (${PersianUtils.formatNumberWithCommas(totalUnits)} واحد)",
                                icon = Icons.Outlined.ReceiptLong,
                                accentColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            // Total Payments Card
                            MetricSummaryCard(
                                title = "مجموع کل دریافتی‌ها",
                                mainValue = PersianUtils.formatCurrency(totalPaid, currencyUnit),
                                subValue = "${PersianUtils.toPersianDigits(periodPayments.size)} دریافتی ثبت شده",
                                icon = Icons.Outlined.Payments,
                                accentColor = Emerald600,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Remaining Balance Card
                            val balCol = if (remainingBalance > 0) Rose600 else if (remainingBalance == 0L) Emerald600 else Amber600
                            val balStatus = if (remainingBalance > 0) "بدهکار به کارگاه" else if (remainingBalance == 0L) "تسویه کامل" else "بستانکار"
                            MetricSummaryCard(
                                title = "باقی مانده حساب ($balStatus)",
                                mainValue = if (remainingBalance == 0L) "تسویه کامل" else PersianUtils.formatCurrency(Math.abs(remainingBalance), currencyUnit),
                                subValue = "تراز مالی کارگاه در این دوره",
                                icon = Icons.Outlined.AccountBalanceWallet,
                                accentColor = balCol,
                                modifier = Modifier.weight(1f)
                            )
                            // Average Work per Order Card
                            MetricSummaryCard(
                                title = "میانگین دستمزد هر فاکتور",
                                mainValue = PersianUtils.formatCurrency(avgWorkPerOrder, currencyUnit),
                                subValue = "میانگین هر واحد: ${PersianUtils.formatCurrency(avgWorkPerUnit, currencyUnit)}",
                                icon = Icons.Outlined.Speed,
                                accentColor = Color(0xFF9333EA),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Section 2: Max & Min Records and Highlights (بیشترین‌ها و کمترین‌ها)
                    Text(
                        text = "🏆 رکوردها، بیشترین‌ها و کمترین‌ها:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Max Work Order
                            RecordHighlightCard(
                                title = "بیشترین کارکرد (فاکتور رکورد)",
                                mainText = if (maxWorkOrder != null) "${maxWorkOrder.modelName} • #${PersianUtils.toPersianDigits(maxWorkOrder.invoiceNumber)}" else "ثبت نشده",
                                subText = if (maxWorkOrder != null) "مشتری: ${maxWorkOrder.customerName.ifBlank { "عمومی" }} | تاریخ: ${PersianUtils.toPersianDigits(maxWorkOrder.dateJalali)}" else "-",
                                amountText = if (maxWorkOrder != null) PersianUtils.formatCurrency(maxWorkOrder.calculatedTotal, currencyUnit) else "-",
                                isPositive = true,
                                icon = Icons.Filled.TrendingUp,
                                accentColor = Emerald600,
                                modifier = Modifier.weight(1f)
                            )
                            // Min Work Order
                            RecordHighlightCard(
                                title = "کمترین کارکرد (کوچک‌ترین فاکتور)",
                                mainText = if (minWorkOrder != null) "${minWorkOrder.modelName} • #${PersianUtils.toPersianDigits(minWorkOrder.invoiceNumber)}" else "ثبت نشده",
                                subText = if (minWorkOrder != null) "اجزا: ${PersianUtils.toPersianDigits(minWorkOrder.countFormula)} (${PersianUtils.formatNumberWithCommas(minWorkOrder.calculatedUnits)}و)" else "-",
                                amountText = if (minWorkOrder != null) PersianUtils.formatCurrency(minWorkOrder.calculatedTotal, currencyUnit) else "-",
                                isPositive = false,
                                icon = Icons.Filled.TrendingDown,
                                accentColor = Amber600,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Max Payment
                            RecordHighlightCard(
                                title = "بیشترین دریافتی (بالاترین واریزی)",
                                mainText = if (maxPayment != null) "${maxPayment.customerName.ifBlank { "عمومی" }} (${formatPayMethod(maxPayment.paymentType)})" else "ثبت نشده",
                                subText = if (maxPayment != null) "تاریخ: ${PersianUtils.toPersianDigits(maxPayment.dateJalali)} | کد: ${PersianUtils.toPersianDigits(maxPayment.referenceNo.ifBlank { "-" })}" else "-",
                                amountText = if (maxPayment != null) PersianUtils.formatCurrency(maxPayment.amount, currencyUnit) else "-",
                                isPositive = true,
                                icon = Icons.Filled.ArrowUpward,
                                accentColor = Color(0xFF0284C7),
                                modifier = Modifier.weight(1f)
                            )
                            // Min Payment
                            RecordHighlightCard(
                                title = "کمترین دریافتی (پایین‌ترین سند)",
                                mainText = if (minPayment != null) "${minPayment.customerName.ifBlank { "عمومی" }} (${formatPayMethod(minPayment.paymentType)})" else "ثبت نشده",
                                subText = if (minPayment != null) "تاریخ: ${PersianUtils.toPersianDigits(minPayment.dateJalali)}" else "-",
                                amountText = if (minPayment != null) PersianUtils.formatCurrency(minPayment.amount, currencyUnit) else "-",
                                isPositive = false,
                                icon = Icons.Filled.ArrowDownward,
                                accentColor = Color(0xFF64748B),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Top Model
                            RecordHighlightCard(
                                title = "پرتقاضاترین مدل مبل",
                                mainText = topModel?.name ?: "ثبت نشده",
                                subText = if (topModel != null) "${PersianUtils.toPersianDigits(topModel.orderCount)} فاکتور • ${PersianUtils.formatNumberWithCommas(topModel.totalUnits)} واحد تولیدی" else "-",
                                amountText = if (topModel != null) "${PersianUtils.formatCurrency(topModel.totalAmount, currencyUnit)} (${PersianUtils.toPersianDigits(String.format(java.util.Locale.US, "%.1f", topModel.percentOfTotal))}%)" else "-",
                                isPositive = true,
                                icon = Icons.Default.Chair,
                                accentColor = PersianUtils.parseColor(topModel?.colorCode ?: "#2563EB"),
                                modifier = Modifier.weight(1f)
                            )
                            // Top Payer
                            RecordHighlightCard(
                                title = "بیشترین پرداخت‌کننده",
                                mainText = topPayer?.payerName ?: "ثبت نشده",
                                subText = if (topPayer != null) "${PersianUtils.toPersianDigits(topPayer.paymentCount)} سند دریافتی (${topPayer.banksUsed})" else "-",
                                amountText = if (topPayer != null) "کل دریافتی: ${PersianUtils.formatCurrency(topPayer.totalPaid, currencyUnit)}" else "-",
                                isPositive = true,
                                icon = Icons.Default.Person,
                                accentColor = Color(0xFF8B5CF6),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Section 3: Visual Charts (نمودارهای گرافیکی تحلیلی)
                    Text(
                        text = "📊 نمودارهای تحلیلی و مقایسه‌ای:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Chart 1: Work vs Received Monthly Bar Chart
                    MonthlyBarChartCard(
                        trends = monthlyTrends,
                        currencyUnit = currencyUnit,
                        year = selectedYear
                    )

                    // Chart 2: Model Distribution & Payment Methods
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ModelDistributionCard(
                            modelStats = modelStats.take(5),
                            currencyUnit = currencyUnit,
                            modifier = Modifier.weight(1f)
                        )
                        PaymentMethodsCard(
                            methods = methodStats,
                            currencyUnit = currencyUnit,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Section 4: Detailed Breakdown Tables (جدول‌های خط‌کشی‌شده تحلیلی)
                    Text(
                        text = "📋 جدول‌های تفکیکی و آماری:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Table 1: Model Analysis Table
                    ModelAnalysisTable(
                        modelStats = modelStats,
                        currencyUnit = currencyUnit
                    )

                    // Table 2: Payers Breakdown Table
                    PayerAnalysisTable(
                        payerStats = payerStats,
                        currencyUnit = currencyUnit
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Bottom Action Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("بستن بخش آنالیز", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Component: Metric Summary Card
// -------------------------------------------------------------
@Composable
private fun MetricSummaryCard(
    title: String,
    mainValue: String,
    subValue: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, accentColor.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                }
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = mainValue,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Black,
                color = accentColor
            )
            Text(
                text = subValue,
                fontSize = 9.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// -------------------------------------------------------------
// Component: Record Highlight Card (Min / Max)
// -------------------------------------------------------------
@Composable
private fun RecordHighlightCard(
    title: String,
    mainText: String,
    subText: String,
    amountText: String,
    isPositive: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = accentColor.copy(alpha = 0.06f),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, accentColor.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = mainText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = amountText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = accentColor
                )
                Text(
                    text = subText,
                    fontSize = 8.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Component: Monthly Bar Chart (Work vs Received)
// -------------------------------------------------------------
@Composable
private fun MonthlyBarChartCard(
    trends: List<MonthTrend>,
    currencyUnit: String,
    year: Int
) {
    val maxAmount = remember(trends) {
        val m1 = trends.maxOfOrNull { it.workAmount } ?: 0L
        val m2 = trends.maxOfOrNull { it.paidAmount } ?: 0L
        maxOf(m1, m2, 1000000L).toFloat()
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = Icons.Default.BarChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text(
                        text = "روند مقایسه‌ای کارکرد در برابر دریافتی (سال ${PersianUtils.toPersianDigits(year)})",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                // Legend
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF2563EB)))
                        Text("کارکرد", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF059669)))
                        Text("دریافتی", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Custom Canvas Bar Chart
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                val w = size.width
                val h = size.height
                val chartH = h - 10f

                // Draw background horizontal grid lines
                for (step in 1..3) {
                    val lineY = chartH * (step / 4f)
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.25f),
                        start = Offset(0f, lineY),
                        end = Offset(w, lineY),
                        strokeWidth = 1f
                    )
                }

                val numBars = 12
                val slotWidth = w / numBars
                val barW = slotWidth * 0.34f

                trends.forEachIndexed { i, trend ->
                    val slotCenter = (i + 0.5f) * slotWidth
                    val workH = if (maxAmount > 0) (trend.workAmount / maxAmount) * (chartH * 0.88f) else 0f
                    val paidH = if (maxAmount > 0) (trend.paidAmount / maxAmount) * (chartH * 0.88f) else 0f

                    // Draw Work Bar (Blue)
                    if (trend.workAmount > 0) {
                        drawRect(
                            color = Color(0xFF2563EB),
                            topLeft = Offset(slotCenter - barW - 1f, chartH - workH),
                            size = Size(barW, workH)
                        )
                    }

                    // Draw Paid Bar (Emerald)
                    if (trend.paidAmount > 0) {
                        drawRect(
                            color = Color(0xFF059669),
                            topLeft = Offset(slotCenter + 1f, chartH - paidH),
                            size = Size(barW, paidH)
                        )
                    }
                }

                // Draw baseline
                drawLine(
                    color = Color.Gray.copy(alpha = 0.5f),
                    start = Offset(0f, chartH),
                    end = Offset(w, chartH),
                    strokeWidth = 1.5f
                )
            }

            // Month Labels Row below canvas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                JALALI_MONTH_NAMES.forEach { mName ->
                    Text(
                        text = mName.take(3),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Component: Model Share Breakdown
// -------------------------------------------------------------
@Composable
private fun ModelDistributionCard(
    modelStats: List<ModelStat>,
    currencyUnit: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(imageVector = Icons.Default.PieChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Text("سهم مدل‌های مبل", fontSize = 11.5.sp, fontWeight = FontWeight.Black)
            }

            if (modelStats.isEmpty()) {
                Text("داده‌ای ثبت نشده", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    modelStats.take(4).forEach { stat ->
                        val parsedCol = PersianUtils.parseColor(stat.colorCode)

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(stat.name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = parsedCol, maxLines = 1)
                                Text("${PersianUtils.toPersianDigits(String.format(java.util.Locale.US, "%.1f", stat.percentOfTotal))}%", fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                            LinearProgressIndicator(
                                progress = { (stat.percentOfTotal / 100.0).toFloat().coerceIn(0.02f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = parsedCol,
                                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Component: Payment Methods Breakdown
// -------------------------------------------------------------
@Composable
private fun PaymentMethodsCard(
    methods: List<MethodStat>,
    currencyUnit: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, Emerald600.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(imageVector = Icons.Default.AccountBalance, contentDescription = null, tint = Emerald600, modifier = Modifier.size(16.dp))
                Text("روش‌های دریافتی", fontSize = 11.5.sp, fontWeight = FontWeight.Black)
            }

            if (methods.isEmpty()) {
                Text("دریافتی ثبت نشده", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    methods.forEach { m ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(m.color))
                                Text(m.titleFa, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text("(${PersianUtils.toPersianDigits(m.count)})", fontSize = 8.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(PersianUtils.formatCurrency(m.amount, currencyUnit), fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = m.color)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Component: Model Analysis Lined Table
// -------------------------------------------------------------
@Composable
private fun ModelAnalysisTable(
    modelStats: List<ModelStat>,
    currencyUnit: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ردیف / مدل مبل", fontSize = 10.5.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                Text("تعداد فاکتور", fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("واحدهای تولید", fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("درآمد کل ($currencyUnit)", fontSize = 10.5.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }

            HorizontalDivider(thickness = 1.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

            if (modelStats.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("مدلی در این دوره یافت نشد", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                modelStats.forEachIndexed { idx, stat ->
                    val parsedCol = PersianUtils.parseColor(stat.colorCode)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(parsedCol.copy(alpha = 0.08f))
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Color stripe
                        Box(modifier = Modifier.width(3.5.dp).height(20.dp).clip(RoundedCornerShape(2.dp)).background(parsedCol))
                        Spacer(modifier = Modifier.width(6.dp))

                        Text("${PersianUtils.toPersianDigits(idx + 1)}. ${stat.name}", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = parsedCol, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                        Text("${PersianUtils.toPersianDigits(stat.orderCount)} فاکتور", fontSize = 10.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text("${PersianUtils.formatNumberWithCommas(stat.totalUnits)} واحد", fontSize = 10.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text(PersianUtils.formatNumberWithCommas(stat.totalAmount), fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    }
                    if (idx < modelStats.size - 1) {
                        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Component: Payer Analysis Lined Table
// -------------------------------------------------------------
@Composable
private fun PayerAnalysisTable(
    payerStats: List<PayerStat>,
    currencyUnit: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("پرداخت‌کننده", fontSize = 10.5.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                Text("تعداد دریافتی", fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("بانک / حساب", fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("مجموع دریافتی ($currencyUnit)", fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }

            HorizontalDivider(thickness = 1.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

            if (payerStats.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("پرداخت‌کننده‌ای در این دوره یافت نشد", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                payerStats.forEachIndexed { idx, pStat ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (idx % 2 == 1) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f) else Color.Transparent)
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${PersianUtils.toPersianDigits(idx + 1)}. ${pStat.payerName}", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                        Text("${PersianUtils.toPersianDigits(pStat.paymentCount)} سند", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text(pStat.banksUsed, fontSize = 9.5.sp, color = Emerald600, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = PersianUtils.formatNumberWithCommas(pStat.totalPaid),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Emerald600,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End
                        )
                    }
                    if (idx < payerStats.size - 1) {
                        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Helper: Share full text analysis report
// -------------------------------------------------------------
private fun shareAnalysisReport(
    context: Context,
    periodTitle: String,
    periodOrders: List<FurnitureOrder>,
    periodPayments: List<PaymentRecord>,
    totalWork: Long,
    totalPaid: Long,
    balance: Long,
    currencyUnit: String,
    maxWorkOrder: FurnitureOrder?,
    minWorkOrder: FurnitureOrder?,
    maxPayment: PaymentRecord?,
    minPayment: PaymentRecord?,
    topModel: ModelStat?,
    topPayer: PayerStat?
) {
    try {
        val sb = StringBuilder()
        sb.append("📊 گزارش جامع اطلاعات و آنالیز SheetOn\n")
        sb.append("بازه زمانی: $periodTitle\n")
        sb.append("تاریخ گزارش: ${PersianUtils.toPersianDigits(PersianUtils.getTodayJalaliString())}\n")
        sb.append("==================================\n\n")

        sb.append("📈 خلاصه شاخص‌های مالی و کارکرد:\n")
        sb.append("• مجموع کارکرد فاکتورها: ${PersianUtils.formatCurrency(totalWork, currencyUnit)} (${PersianUtils.toPersianDigits(periodOrders.size)} فاکتور)\n")
        sb.append("• مجموع کل دریافتی‌ها: ${PersianUtils.formatCurrency(totalPaid, currencyUnit)} (${PersianUtils.toPersianDigits(periodPayments.size)} دریافتی)\n")
        val balStatus = if (balance > 0) "مانده حساب" else if (balance == 0L) "تسویه کامل" else "بستانکار"
        sb.append("• باقی مانده حساب ($balStatus): ${if (balance == 0L) "تسویه کامل" else PersianUtils.formatCurrency(Math.abs(balance), currencyUnit)}\n\n")

        sb.append("🏆 رکوردها، بیشترین‌ها و کمترین‌ها:\n")
        if (maxWorkOrder != null) {
            sb.append("• بیشترین کارکرد: ${maxWorkOrder.modelName} (فاکتور #${PersianUtils.toPersianDigits(maxWorkOrder.invoiceNumber)}) به مبلغ ${PersianUtils.formatCurrency(maxWorkOrder.calculatedTotal, currencyUnit)}\n")
        }
        if (minWorkOrder != null) {
            sb.append("• کمترین کارکرد: ${minWorkOrder.modelName} (فاکتور #${PersianUtils.toPersianDigits(minWorkOrder.invoiceNumber)}) به مبلغ ${PersianUtils.formatCurrency(minWorkOrder.calculatedTotal, currencyUnit)}\n")
        }
        if (maxPayment != null) {
            sb.append("• بیشترین دریافتی: ${maxPayment.customerName.ifBlank { "عمومی" }} به مبلغ ${PersianUtils.formatCurrency(maxPayment.amount, currencyUnit)}\n")
        }
        if (topModel != null) {
            sb.append("• پرتقاضاترین مدل: ${topModel.name} (${PersianUtils.toPersianDigits(topModel.orderCount)} فاکتور - مجموع درآمد: ${PersianUtils.formatCurrency(topModel.totalAmount, currencyUnit)})\n")
        }
        if (topPayer != null) {
            sb.append("• بیشترین پرداخت‌کننده: ${topPayer.payerName} (${PersianUtils.toPersianDigits(topPayer.paymentCount)} سند دریافتی - مجموع دریافتی: ${PersianUtils.formatCurrency(topPayer.totalPaid, currencyUnit)})\n")
        }

        sb.append("\n==================================\n")
        sb.append("نرم‌افزار مدیریت هوشمند کارکرد و حسابداری SheetOn")

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "گزارش اطلاعات و آنالیز کارکرد")
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }
        context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری گزارش تحلیلی"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun formatPayMethod(method: String): String {
    return when (method) {
        "transfer" -> "کارت به کارت"
        "cash" -> "نقدی"
        "pos" -> "کارتخوان"
        "cheque" -> "چک"
        else -> "واریزی"
    }
}
