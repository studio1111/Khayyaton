package com.example.ui.dialogs

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.luminance
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
import com.example.util.ExcelExportUtil

enum class AnalysisTab(val title: String, val subtitle: String) {
    TOTAL("آمار کل", "تمامی سوابق کارکرد و دریافتی‌ها"),
    YEARLY("آمار سالیانه", "تفکیک بر اساس ماه‌های سال"),
    MONTHLY("آمار ماهیانه", "تفکیک بر اساس هفته‌های ماه"),
    WEEKLY("آمار هفتگی", "تفکیک بر اساس روزهای هفته")
}

data class ModelStat(
    val name: String,
    val colorCode: String,
    val orderCount: Int,
    val avgUnitsPerSet: Double,
    val totalUnits: Double,
    val avgPricePerUnit: Long,
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

data class PeriodBreakdownItem(
    val label: String,
    val orderCount: Int,
    val workAmount: Long,
    val paymentCount: Int,
    val paidAmount: Long
) {
    val balance: Long get() = workAmount - paidAmount
}

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
    var selectedTab by remember { mutableStateOf(AnalysisTab.TOTAL) }

    // Contrast text colors for light vs dark theme
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val primaryTextColor = if (isLight) Color(0xFF000000) else Color.White
    val secondaryTextColor = if (isLight) Color(0xFF0F172A) else Color(0xFFCBD5E1)
    val captionTextColor = if (isLight) Color(0xFF334155) else Color(0xFF94A3B8)
    val tableHeaderBg = if (isLight) Color(0xFFE2E8F0) else MaterialTheme.colorScheme.surfaceVariant
    val tableAltRowBg = if (isLight) Color(0xFFF8FAFC) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    val cardBg = if (isLight) Color(0xFFFFFFFF) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    val borderColor = if (isLight) Color(0xFFCBD5E1) else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

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
    var selectedWeek by remember { mutableStateOf(1) } // 1..5

    // Filter orders and payments by selected tab and period
    val periodOrders = remember(orders, selectedTab, selectedYear, selectedMonth, selectedWeek) {
        when (selectedTab) {
            AnalysisTab.TOTAL -> orders
            AnalysisTab.YEARLY -> orders.filter { ord ->
                val p = PersianUtils.parseJalaliParts(ord.dateJalali)
                p != null && p.year == selectedYear
            }
            AnalysisTab.MONTHLY -> orders.filter { ord ->
                val p = PersianUtils.parseJalaliParts(ord.dateJalali)
                p != null && p.year == selectedYear && p.month == selectedMonth
            }
            AnalysisTab.WEEKLY -> orders.filter { ord ->
                val p = PersianUtils.parseJalaliParts(ord.dateJalali)
                if (p != null && p.year == selectedYear && p.month == selectedMonth) {
                    val weekIdx = ((p.day - 1) / 7) + 1
                    weekIdx == selectedWeek || (selectedWeek == 5 && weekIdx >= 5)
                } else false
            }
        }
    }

    val periodPayments = remember(payments, selectedTab, selectedYear, selectedMonth, selectedWeek) {
        when (selectedTab) {
            AnalysisTab.TOTAL -> payments
            AnalysisTab.YEARLY -> payments.filter { pay ->
                val p = PersianUtils.parseJalaliParts(pay.dateJalali)
                p != null && p.year == selectedYear
            }
            AnalysisTab.MONTHLY -> payments.filter { pay ->
                val p = PersianUtils.parseJalaliParts(pay.dateJalali)
                p != null && p.year == selectedYear && p.month == selectedMonth
            }
            AnalysisTab.WEEKLY -> payments.filter { pay ->
                val p = PersianUtils.parseJalaliParts(pay.dateJalali)
                if (p != null && p.year == selectedYear && p.month == selectedMonth) {
                    val weekIdx = ((p.day - 1) / 7) + 1
                    weekIdx == selectedWeek || (selectedWeek == 5 && weekIdx >= 5)
                } else false
            }
        }
    }

    // High-level financial totals for active tab
    val totalWork = remember(periodOrders) { periodOrders.sumOf { it.calculatedTotal } }
    val totalUnits = remember(periodOrders) { periodOrders.sumOf { it.calculatedUnits } }
    val totalPaid = remember(periodPayments) { periodPayments.sumOf { it.amount } }
    val remainingBalance = totalWork - totalPaid

    // Model aggregation for active tab
    val modelStats = remember(periodOrders, totalWork) {
        periodOrders.groupBy { it.modelName }
            .map { (modelName, list) ->
                val mUnits = list.sumOf { it.calculatedUnits }
                val mAmount = list.sumOf { it.calculatedTotal }
                val mColor = list.firstOrNull()?.colorCode?.takeIf { it.isNotBlank() } ?: PersianUtils.getModelColor(modelName)
                val pct = if (totalWork > 0) (mAmount.toDouble() / totalWork) * 100.0 else 0.0

                // Average units per set and average wage per unit
                val avgUnits = if (list.isNotEmpty()) {
                    list.map { it.unitsPerSet }.average().takeIf { !it.isNaN() } ?: 1.0
                } else 1.0
                val avgWagePerUnit = if (mUnits > 0) (mAmount / mUnits).toLong() else 0L

                ModelStat(
                    name = modelName,
                    colorCode = mColor,
                    orderCount = list.size,
                    avgUnitsPerSet = avgUnits,
                    totalUnits = mUnits,
                    avgPricePerUnit = avgWagePerUnit,
                    totalAmount = mAmount,
                    percentOfTotal = pct
                )
            }
            .sortedByDescending { it.totalAmount }
    }

    // Payer aggregation for active tab
    val payerStats = remember(periodPayments, totalPaid) {
        periodPayments.groupBy { it.customerName.trim().ifBlank { "واریزکننده عمومی" } }
            .map { (payer, list) ->
                val pPaid = list.sumOf { it.amount }
                val banks = list.mapNotNull { it.bankName.takeIf { b -> b.isNotBlank() } }.distinct().joinToString("، ")
                val pct = if (totalPaid > 0) (pPaid.toDouble() / totalPaid) * 100.0 else 0.0
                PayerStat(payer, list.size, pPaid, banks.ifBlank { "واریزی کارگاه" }, pct)
            }
            .sortedByDescending { it.totalPaid }
    }

    // Period-specific breakdown items (for Chart 1 and Table 1)
    val breakdownItems: List<PeriodBreakdownItem> = remember(
        selectedTab, selectedYear, selectedMonth, selectedWeek, orders, payments
    ) {
        when (selectedTab) {
            AnalysisTab.TOTAL -> {
                // Breakdown by all available years
                availableYears.sorted().map { yr ->
                    val yOrders = orders.filter { PersianUtils.parseJalaliParts(it.dateJalali)?.year == yr }
                    val yPayments = payments.filter { PersianUtils.parseJalaliParts(it.dateJalali)?.year == yr }
                    PeriodBreakdownItem(
                        label = "سال ${PersianUtils.toPersianDigits(yr)}",
                        orderCount = yOrders.size,
                        workAmount = yOrders.sumOf { it.calculatedTotal },
                        paymentCount = yPayments.size,
                        paidAmount = yPayments.sumOf { it.amount }
                    )
                }
            }
            AnalysisTab.YEARLY -> {
                // 12 months of selected year
                (1..12).map { m ->
                    val mOrders = orders.filter {
                        val p = PersianUtils.parseJalaliParts(it.dateJalali)
                        p != null && p.year == selectedYear && p.month == m
                    }
                    val mPayments = payments.filter {
                        val p = PersianUtils.parseJalaliParts(it.dateJalali)
                        p != null && p.year == selectedYear && p.month == m
                    }
                    PeriodBreakdownItem(
                        label = JALALI_MONTH_NAMES.getOrElse(m - 1) { "$m" },
                        orderCount = mOrders.size,
                        workAmount = mOrders.sumOf { it.calculatedTotal },
                        paymentCount = mPayments.size,
                        paidAmount = mPayments.sumOf { it.amount }
                    )
                }
            }
            AnalysisTab.MONTHLY -> {
                // 5 weeks of selected month
                (1..5).map { w ->
                    val wOrders = orders.filter {
                        val p = PersianUtils.parseJalaliParts(it.dateJalali)
                        if (p != null && p.year == selectedYear && p.month == selectedMonth) {
                            val weekIdx = ((p.day - 1) / 7) + 1
                            weekIdx == w || (w == 5 && weekIdx >= 5)
                        } else false
                    }
                    val wPayments = payments.filter {
                        val p = PersianUtils.parseJalaliParts(it.dateJalali)
                        if (p != null && p.year == selectedYear && p.month == selectedMonth) {
                            val weekIdx = ((p.day - 1) / 7) + 1
                            weekIdx == w || (w == 5 && weekIdx >= 5)
                        } else false
                    }
                    val weekLabel = when (w) {
                        1 -> "هفته اول (۱-۷)"
                        2 -> "هفته دوم (۸-۱۴)"
                        3 -> "هفته سوم (۱۵-۲۱)"
                        4 -> "هفته چهارم (۲۲-۲۸)"
                        else -> "هفته پنجم (۲۹-۳۱)"
                    }
                    PeriodBreakdownItem(
                        label = weekLabel,
                        orderCount = wOrders.size,
                        workAmount = wOrders.sumOf { it.calculatedTotal },
                        paymentCount = wPayments.size,
                        paidAmount = wPayments.sumOf { it.amount }
                    )
                }
            }
            AnalysisTab.WEEKLY -> {
                // 7 days of the week (شنبه تا جمعه)
                (0..6).map { dayIdx ->
                    val dOrders = periodOrders.filter { ord ->
                        PersianUtils.getJalaliDayOfWeek(ord.dateJalali) == dayIdx
                    }
                    val dPayments = periodPayments.filter { pay ->
                        PersianUtils.getJalaliDayOfWeek(pay.dateJalali) == dayIdx
                    }
                    PeriodBreakdownItem(
                        label = PersianUtils.PERSIAN_WEEKDAY_NAMES.getOrElse(dayIdx) { "$dayIdx" },
                        orderCount = dOrders.size,
                        workAmount = dOrders.sumOf { it.calculatedTotal },
                        paymentCount = dPayments.size,
                        paidAmount = dPayments.sumOf { it.amount }
                    )
                }
            }
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
                .fillMaxWidth(0.97f)
                .fillMaxHeight(0.95f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
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
                                color = primaryTextColor
                            )
                            Text(
                                text = "آمار کل، سالیانه، ماهیانه و هفتگی به همراه نمودارها و جدول‌ها",
                                fontSize = 10.5.sp,
                                color = secondaryTextColor
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                ExcelExportUtil.shareAnalysisExcel(
                                    context = context,
                                    orders = periodOrders,
                                    payments = periodPayments,
                                    currencyUnit = currencyUnit
                                )
                            }
                        ) {
                            Icon(imageVector = Icons.Outlined.TableView, contentDescription = "خروجی Excel", tint = primaryTextColor)
                        }
                        IconButton(
                            onClick = {
                                shareAnalysisReport(
                                    context = context,
                                    tabTitle = selectedTab.title,
                                    periodOrders = periodOrders,
                                    periodPayments = periodPayments,
                                    totalWork = totalWork,
                                    totalPaid = totalPaid,
                                    balance = remainingBalance,
                                    currencyUnit = currencyUnit,
                                    modelStats = modelStats,
                                    payerStats = payerStats
                                )
                            }
                        ) {
                            Icon(imageVector = Icons.Outlined.Share, contentDescription = "اشتراک گزارش", tint = primaryTextColor)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "بستن", tint = primaryTextColor)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = borderColor)

                // The 4 Tabs Bar (آمار کل | سالیانه | ماهیانه | هفتگی)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AnalysisTab.values().forEach { tab ->
                            val isSelected = selectedTab == tab
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedTab = tab }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tab.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else primaryTextColor
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Period Selectors (سال، ماه، هفته بر اساس تب فعال)
                AnimatedVisibility(visible = selectedTab != AnalysisTab.TOTAL) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = cardBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Row 1: Year Selector (for Yearly, Monthly, Weekly)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "انتخاب سال:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = primaryTextColor
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.horizontalScroll(rememberScrollState())
                                ) {
                                    availableYears.forEach { yr ->
                                        FilterChip(
                                            selected = selectedYear == yr,
                                            onClick = { selectedYear = yr },
                                            label = {
                                                Text(
                                                    text = PersianUtils.toPersianDigits(yr),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = primaryTextColor
                                                )
                                            }
                                        )
                                    }
                                }
                            }

                            // Row 2: Month Selector (for Monthly and Weekly)
                            if (selectedTab == AnalysisTab.MONTHLY || selectedTab == AnalysisTab.WEEKLY) {
                                HorizontalDivider(color = borderColor.copy(alpha = 0.5f))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "انتخاب ماه:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = primaryTextColor
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.horizontalScroll(rememberScrollState())
                                    ) {
                                        (1..12).forEach { m ->
                                            val mName = JALALI_MONTH_NAMES.getOrElse(m - 1) { "$m" }
                                            FilterChip(
                                                selected = selectedMonth == m,
                                                onClick = { selectedMonth = m },
                                                label = {
                                                    Text(
                                                        text = mName,
                                                        fontSize = 10.5.sp,
                                                        fontWeight = if (selectedMonth == m) FontWeight.Black else FontWeight.Normal,
                                                        color = primaryTextColor
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Row 3: Week Selector (for Weekly)
                            if (selectedTab == AnalysisTab.WEEKLY) {
                                HorizontalDivider(color = borderColor.copy(alpha = 0.5f))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "انتخاب هفته:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = primaryTextColor
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.horizontalScroll(rememberScrollState())
                                    ) {
                                        (1..5).forEach { w ->
                                            val wTitle = when (w) {
                                                1 -> "هفته ۱ (۱-۷)"
                                                2 -> "هفته ۲ (۸-۱۴)"
                                                3 -> "هفته ۳ (۱۵-۲۱)"
                                                4 -> "هفته ۴ (۲۲-۲۸)"
                                                else -> "هفته ۵ (۲۹-۳۱)"
                                            }
                                            FilterChip(
                                                selected = selectedWeek == w,
                                                onClick = { selectedWeek = w },
                                                label = {
                                                    Text(
                                                        text = wTitle,
                                                        fontSize = 10.5.sp,
                                                        fontWeight = if (selectedWeek == w) FontWeight.Black else FontWeight.Normal,
                                                        color = primaryTextColor
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Top Overview Metric Cards (کارکرد | دریافتی | مانده حساب)
                    FinancialOverviewCards(
                        totalWork = totalWork,
                        totalPaid = totalPaid,
                        remainingBalance = remainingBalance,
                        orderCount = periodOrders.size,
                        paymentCount = periodPayments.size,
                        totalUnits = totalUnits,
                        currencyUnit = currencyUnit,
                        primaryTextColor = primaryTextColor,
                        secondaryTextColor = secondaryTextColor,
                        cardBg = cardBg,
                        borderColor = borderColor
                    )

                    // -------------------------------------------------------------
                    // CHART 1: نمودار کارکرد و دریافتی
                    // -------------------------------------------------------------
                    WorkVsReceivedChartCard(
                        tab = selectedTab,
                        items = breakdownItems,
                        totalWork = totalWork,
                        totalPaid = totalPaid,
                        currencyUnit = currencyUnit,
                        primaryTextColor = primaryTextColor,
                        secondaryTextColor = secondaryTextColor,
                        cardBg = cardBg,
                        borderColor = borderColor
                    )

                    // -------------------------------------------------------------
                    // CHART 2: نمودار انواع مدل‌ها
                    // -------------------------------------------------------------
                    ModelsDistributionChartCard(
                        tab = selectedTab,
                        modelStats = modelStats,
                        totalWork = totalWork,
                        currencyUnit = currencyUnit,
                        primaryTextColor = primaryTextColor,
                        secondaryTextColor = secondaryTextColor,
                        cardBg = cardBg,
                        borderColor = borderColor
                    )

                    // -------------------------------------------------------------
                    // TABLE 1: جدول کارکرد، دریافتی‌ها و پرداخت‌کننده‌ها
                    // -------------------------------------------------------------
                    WorkAndPaymentsTableCard(
                        tab = selectedTab,
                        items = breakdownItems,
                        totalWork = totalWork,
                        totalPaid = totalPaid,
                        remainingBalance = remainingBalance,
                        orderCount = periodOrders.size,
                        paymentCount = periodPayments.size,
                        payerStats = payerStats,
                        currencyUnit = currencyUnit,
                        primaryTextColor = primaryTextColor,
                        secondaryTextColor = secondaryTextColor,
                        captionTextColor = captionTextColor,
                        tableHeaderBg = tableHeaderBg,
                        tableAltRowBg = tableAltRowBg,
                        borderColor = borderColor
                    )

                    // -------------------------------------------------------------
                    // TABLE 2: جدول تعداد، واحدها و دستمزد مدل‌ها بر اساس واحد
                    // -------------------------------------------------------------
                    ModelWageDetailsTableCard(
                        tab = selectedTab,
                        modelStats = modelStats,
                        totalWork = totalWork,
                        totalUnits = totalUnits,
                        currencyUnit = currencyUnit,
                        primaryTextColor = primaryTextColor,
                        secondaryTextColor = secondaryTextColor,
                        captionTextColor = captionTextColor,
                        tableHeaderBg = tableHeaderBg,
                        tableAltRowBg = tableAltRowBg,
                        borderColor = borderColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = borderColor)

                // Close Button
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

// =============================================================================
// SUB-COMPONENTS: Financial Metric Overview Cards
// =============================================================================
@Composable
private fun FinancialOverviewCards(
    totalWork: Long,
    totalPaid: Long,
    remainingBalance: Long,
    orderCount: Int,
    paymentCount: Int,
    totalUnits: Double,
    currencyUnit: String,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    cardBg: Color,
    borderColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Card 1: کارکرد کل
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = cardBg,
            border = androidx.compose.foundation.BorderStroke(1.2.dp, borderColor),
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF2563EB).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Outlined.ReceiptLong, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(15.dp))
                    }
                    Text("مجموع کارکرد", fontSize = 10.sp, fontWeight = FontWeight.Black, color = secondaryTextColor)
                }
                Text(
                    text = PersianUtils.formatCurrency(totalWork, currencyUnit),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF2563EB),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${PersianUtils.toPersianDigits(orderCount)} فاکتور • ${PersianUtils.formatNumberWithCommas(totalUnits)} واحد",
                    fontSize = 9.sp,
                    color = primaryTextColor
                )
            }
        }

        // Card 2: دریافتی کل
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = cardBg,
            border = androidx.compose.foundation.BorderStroke(1.2.dp, borderColor),
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF059669).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Outlined.Payments, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(15.dp))
                    }
                    Text("مجموع دریافتی", fontSize = 10.sp, fontWeight = FontWeight.Black, color = secondaryTextColor)
                }
                Text(
                    text = PersianUtils.formatCurrency(totalPaid, currencyUnit),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF059669),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${PersianUtils.toPersianDigits(paymentCount)} سند دریافتی ثبت شده",
                    fontSize = 9.sp,
                    color = primaryTextColor
                )
            }
        }

        // Card 3: مانده حساب (با قانون بدهکاری منفی)
        val isNegative = remainingBalance < 0
        val isZero = remainingBalance == 0L
        val balanceColor = when {
            isZero -> Color(0xFF059669)
            isNegative -> Color(0xFFDC2626)
            else -> Color(0xFF2563EB)
        }
        val balanceLabel = when {
            isZero -> "تسویه کامل"
            isNegative -> "بدهکاری کارگاه"
            else -> "طلبکار / مانده"
        }
        val balanceValue = when {
            isZero -> "تسویه کامل"
            isNegative -> "-${PersianUtils.formatCurrency(Math.abs(remainingBalance), currencyUnit)}"
            else -> PersianUtils.formatCurrency(remainingBalance, currencyUnit)
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = cardBg,
            border = androidx.compose.foundation.BorderStroke(1.2.dp, borderColor),
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(balanceColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Outlined.AccountBalanceWallet, contentDescription = null, tint = balanceColor, modifier = Modifier.size(15.dp))
                    }
                    Text(balanceLabel, fontSize = 10.sp, fontWeight = FontWeight.Black, color = secondaryTextColor)
                }
                Text(
                    text = balanceValue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = balanceColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isNegative) "دریافتی بیشتر از کارکرد" else if (isZero) "حساب‌ها تراز است" else "کارکرد تسویه نشده",
                    fontSize = 9.sp,
                    color = primaryTextColor
                )
            }
        }
    }
}

// =============================================================================
// CHART 1: نمودار کارکرد و دریافتی (پشتیبانی از ۴ تب)
// =============================================================================
@Composable
private fun WorkVsReceivedChartCard(
    tab: AnalysisTab,
    items: List<PeriodBreakdownItem>,
    totalWork: Long,
    totalPaid: Long,
    currencyUnit: String,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    cardBg: Color,
    borderColor: Color
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = androidx.compose.foundation.BorderStroke(1.2.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Chart Title and Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = Icons.Default.BarChart, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                    Text(
                        text = "نمودار مقایسه‌ای کارکرد و دریافتی (${tab.title})",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black,
                        color = primaryTextColor
                    )
                }
                // Legend
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF2563EB)))
                        Text("کارکرد", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = primaryTextColor)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF059669)))
                        Text("دریافتی", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = primaryTextColor)
                    }
                }
            }

            if (items.isEmpty() || (totalWork == 0L && totalPaid == 0L)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("اطلاعاتی برای رسم نمودار در این بازه ثبت نشده است", fontSize = 11.sp, color = secondaryTextColor)
                }
            } else {
                val maxAmount = remember(items, totalWork, totalPaid) {
                    val m1 = items.maxOfOrNull { it.workAmount } ?: 0L
                    val m2 = items.maxOfOrNull { it.paidAmount } ?: 0L
                    maxOf(m1, m2, 100000L).toFloat()
                }

                // Custom Bar Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val chartH = h - 10f

                    // Grid lines
                    for (step in 1..3) {
                        val lineY = chartH * (step / 4f)
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.35f),
                            start = Offset(0f, lineY),
                            end = Offset(w, lineY),
                            strokeWidth = 1f
                        )
                    }

                    val count = items.size
                    val slotW = w / count
                    val barW = (slotW * 0.34f).coerceAtMost(22f)

                    items.forEachIndexed { i, item ->
                        val slotCenter = (i + 0.5f) * slotW
                        val workH = if (maxAmount > 0) (item.workAmount / maxAmount) * (chartH * 0.88f) else 0f
                        val paidH = if (maxAmount > 0) (item.paidAmount / maxAmount) * (chartH * 0.88f) else 0f

                        // Draw Work bar (Blue)
                        if (item.workAmount > 0) {
                            drawRoundRect(
                                color = Color(0xFF2563EB),
                                topLeft = Offset(slotCenter - barW - 1f, chartH - workH),
                                size = Size(barW, workH),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                            )
                        }

                        // Draw Paid bar (Green)
                        if (item.paidAmount > 0) {
                            drawRoundRect(
                                color = Color(0xFF059669),
                                topLeft = Offset(slotCenter + 1f, chartH - paidH),
                                size = Size(barW, paidH),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                            )
                        }
                    }

                    // Baseline
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.5f),
                        start = Offset(0f, chartH),
                        end = Offset(w, chartH),
                        strokeWidth = 1.5f
                    )
                }

                // Labels below bars
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    items.forEach { item ->
                        Text(
                            text = item.label.take(6),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryTextColor,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// CHART 2: نمودار انواع مدل‌ها
// =============================================================================
@Composable
private fun ModelsDistributionChartCard(
    tab: AnalysisTab,
    modelStats: List<ModelStat>,
    totalWork: Long,
    currencyUnit: String,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    cardBg: Color,
    borderColor: Color
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = androidx.compose.foundation.BorderStroke(1.2.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = Icons.Default.PieChart, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                    Text(
                        text = "نمودار توزیع و سهم مدل‌های مبل",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black,
                        color = primaryTextColor
                    )
                }
                Text(
                    text = "${PersianUtils.toPersianDigits(modelStats.size)} مدل تولیدی",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor
                )
            }

            if (modelStats.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("مدلی برای نمایش در این بازه وجود ندارد", fontSize = 11.sp, color = secondaryTextColor)
                }
            } else {
                // Stacked Bar Representation
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = borderColor.copy(alpha = 0.3f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        modelStats.take(6).forEach { stat ->
                            val parsedCol = PersianUtils.parseColor(stat.colorCode)
                            val weightVal = stat.percentOfTotal.toFloat().coerceAtLeast(1f)
                            Box(
                                modifier = Modifier
                                    .weight(weightVal)
                                    .fillMaxHeight()
                                    .background(parsedCol)
                            )
                        }
                    }
                }

                // Model items distribution rows
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    modelStats.take(6).forEachIndexed { idx, stat ->
                        val parsedCol = PersianUtils.parseColor(stat.colorCode)
                        val modelDisplayName = if (tab == AnalysisTab.TOTAL || tab == AnalysisTab.YEARLY) {
                            stat.name
                        } else {
                            "${PersianUtils.toPersianDigits(idx + 1)}. ${stat.name}"
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1.5f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(parsedCol)
                                )
                                Text(
                                    text = modelDisplayName,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "${PersianUtils.toPersianDigits(stat.orderCount)} دست • ${PersianUtils.formatNumberWithCommas(stat.totalUnits)} واحد",
                                fontSize = 9.5.sp,
                                color = primaryTextColor,
                                modifier = Modifier.weight(1.2f),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "${PersianUtils.formatCurrency(stat.totalAmount, currencyUnit)} (${PersianUtils.toPersianDigits(String.format(java.util.Locale.US, "%.1f", stat.percentOfTotal))}%)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = primaryTextColor,
                                modifier = Modifier.weight(1.5f),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// TABLE 1: جدول کارکرد، دریافتی‌ها و پرداخت‌کننده‌ها
// =============================================================================
@Composable
private fun WorkAndPaymentsTableCard(
    tab: AnalysisTab,
    items: List<PeriodBreakdownItem>,
    totalWork: Long,
    totalPaid: Long,
    remainingBalance: Long,
    orderCount: Int,
    paymentCount: Int,
    payerStats: List<PayerStat>,
    currencyUnit: String,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    captionTextColor: Color,
    tableHeaderBg: Color,
    tableAltRowBg: Color,
    borderColor: Color
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor),
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
    ) {
        Column(modifier = Modifier.width(820.dp)) {
            // Table Header Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(tableHeaderBg)
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(imageVector = Icons.Default.Receipt, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                Text(
                    text = "جدول اول: کارکرد، دریافتی‌ها و پرداخت‌کننده‌ها (${tab.title})",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Black,
                    color = primaryTextColor
                )
            }

            HorizontalDivider(thickness = 1.2.dp, color = borderColor)

            // Section A: Breakdown Rows of the active Tab (Days, Weeks, Months, or Total Years)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(tableHeaderBg.copy(alpha = 0.6f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("بازه زمانی", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = primaryTextColor, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Start)
                Text("فاکتور / کارکرد", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = primaryTextColor, modifier = Modifier.weight(1.4f), textAlign = TextAlign.Center)
                Text("دریافتی / واریزی", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = primaryTextColor, modifier = Modifier.weight(1.4f), textAlign = TextAlign.Center)
                Text("تراز مانده ($currencyUnit)", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = primaryTextColor, modifier = Modifier.weight(1.3f), textAlign = TextAlign.End)
            }
            HorizontalDivider(thickness = 1.dp, color = borderColor)

            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(14.dp), contentAlignment = Alignment.Center) {
                    Text("رکوردی در این دوره یافت نشد", fontSize = 10.5.sp, color = secondaryTextColor)
                }
            } else {
                items.forEachIndexed { idx, item ->
                    val rowBg = if (idx % 2 == 1) tableAltRowBg else Color.Transparent
                    val bal = item.balance
                    val balColor = when {
                        bal == 0L -> Color(0xFF059669)
                        bal < 0 -> Color(0xFFDC2626)
                        else -> Color(0xFF2563EB)
                    }
                    val balText = when {
                        bal == 0L -> "تسویه"
                        bal < 0 -> "-${PersianUtils.formatNumberWithCommas(Math.abs(bal))}"
                        else -> PersianUtils.formatNumberWithCommas(bal)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(rowBg)
                            .padding(horizontal = 8.dp, vertical = 6.5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.label, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = primaryTextColor, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Start)
                        Text("${PersianUtils.toPersianDigits(item.orderCount)} فقره | ${PersianUtils.formatNumberWithCommas(item.workAmount)}", fontSize = 9.sp, color = primaryTextColor, modifier = Modifier.weight(1.4f), textAlign = TextAlign.Center)
                        Text("${PersianUtils.toPersianDigits(item.paymentCount)} سند | ${PersianUtils.formatNumberWithCommas(item.paidAmount)}", fontSize = 9.sp, color = primaryTextColor, modifier = Modifier.weight(1.4f), textAlign = TextAlign.Center)
                        Text(balText, fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = balColor, modifier = Modifier.weight(1.3f), textAlign = TextAlign.End)
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = borderColor.copy(alpha = 0.5f))
                }
            }

            // Summary Totals Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(tableHeaderBg.copy(alpha = 0.85f))
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("جمع کل دوره", fontSize = 10.sp, fontWeight = FontWeight.Black, color = primaryTextColor, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Start)
                Text("${PersianUtils.toPersianDigits(orderCount)} کارکرد | ${PersianUtils.formatNumberWithCommas(totalWork)}", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF2563EB), modifier = Modifier.weight(1.4f), textAlign = TextAlign.Center)
                Text("${PersianUtils.toPersianDigits(paymentCount)} دریافتی | ${PersianUtils.formatNumberWithCommas(totalPaid)}", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF059669), modifier = Modifier.weight(1.4f), textAlign = TextAlign.Center)
                val totalBalColor = if (remainingBalance < 0) Color(0xFFDC2626) else if (remainingBalance == 0L) Color(0xFF059669) else Color(0xFF2563EB)
                val totalBalText = if (remainingBalance < 0) "-${PersianUtils.formatNumberWithCommas(Math.abs(remainingBalance))} (بدهکاری)" else if (remainingBalance == 0L) "تسویه کامل" else PersianUtils.formatNumberWithCommas(remainingBalance)
                Text(totalBalText, fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = totalBalColor, modifier = Modifier.weight(1.3f), textAlign = TextAlign.End)
            }

            HorizontalDivider(thickness = 1.5.dp, color = borderColor)

            // Section B: Payer and Customer Breakdown List
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(tableHeaderBg.copy(alpha = 0.45f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("نام پرداخت‌کننده", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = primaryTextColor, modifier = Modifier.weight(1.5f), textAlign = TextAlign.Start)
                Text("تعداد اسناد", fontSize = 9.sp, fontWeight = FontWeight.Black, color = primaryTextColor, modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center)
                Text("بانک / درگاه", fontSize = 9.sp, fontWeight = FontWeight.Black, color = primaryTextColor, modifier = Modifier.weight(1.1f), textAlign = TextAlign.Center)
                Text("مجموع واریزی ($currencyUnit)", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = primaryTextColor, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
            }
            HorizontalDivider(thickness = 0.8.dp, color = borderColor)

            if (payerStats.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(10.dp), contentAlignment = Alignment.Center) {
                    Text("پرداخت‌کننده‌ای برای این دوره ثبت نشده است", fontSize = 10.sp, color = secondaryTextColor)
                }
            } else {
                payerStats.forEachIndexed { idx, pStat ->
                    val pRowBg = if (idx % 2 == 1) tableAltRowBg else Color.Transparent
                    val displayPayerName = if (tab == AnalysisTab.TOTAL || tab == AnalysisTab.YEARLY) {
                        pStat.payerName
                    } else {
                        "${PersianUtils.toPersianDigits(idx + 1)}. ${pStat.payerName}"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(pRowBg)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(displayPayerName, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = primaryTextColor, modifier = Modifier.weight(1.5f), textAlign = TextAlign.Start, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${PersianUtils.toPersianDigits(pStat.paymentCount)} سند", fontSize = 9.sp, color = primaryTextColor, modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center)
                        Text(pStat.banksUsed, fontSize = 8.5.sp, color = secondaryTextColor, modifier = Modifier.weight(1.1f), textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = "${PersianUtils.formatNumberWithCommas(pStat.totalPaid)} (${PersianUtils.toPersianDigits(String.format(java.util.Locale.US, "%.1f", pStat.percentOfTotalPaid))}%)",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF059669),
                            modifier = Modifier.weight(1.5f),
                            textAlign = TextAlign.End
                        )
                    }
                    if (idx < payerStats.size - 1) {
                        HorizontalDivider(thickness = 0.5.dp, color = borderColor.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
}

// =============================================================================
// TABLE 2: جدول تعداد، واحدها و دستمزد هر مدل بر اساس واحد
// =============================================================================
@Composable
private fun ModelWageDetailsTableCard(
    tab: AnalysisTab,
    modelStats: List<ModelStat>,
    totalWork: Long,
    totalUnits: Double,
    currencyUnit: String,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    captionTextColor: Color,
    tableHeaderBg: Color,
    tableAltRowBg: Color,
    borderColor: Color
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Table Header Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(tableHeaderBg)
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(imageVector = Icons.Default.Weekend, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                Text(
                    text = "جدول دوم: تعداد فاکتورها، مجموع واحدها و دستمزد مدل‌ها",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Black,
                    color = primaryTextColor
                )
            }

            HorizontalDivider(thickness = 1.2.dp, color = borderColor)

            // Table Column Headers: نام مدل، مجموع تعداد فاکتورها، مجموع تعداد واحدها، مجموع دستمزد هر مدل بر اساس واحد
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(tableHeaderBg.copy(alpha = 0.6f))
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (tab == AnalysisTab.TOTAL || tab == AnalysisTab.YEARLY) "نام مدل" else "ردیف و نام مدل", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = primaryTextColor, modifier = Modifier.weight(1.3f), textAlign = TextAlign.Start)
                Text("تعداد فاکتورها", fontSize = 9.sp, fontWeight = FontWeight.Black, color = primaryTextColor, modifier = Modifier.weight(1.0f), textAlign = TextAlign.Center)
                Text("مجموع واحدها", fontSize = 9.sp, fontWeight = FontWeight.Black, color = primaryTextColor, modifier = Modifier.weight(1.0f), textAlign = TextAlign.Center)
                Text("مجموع دستمزد ($currencyUnit)", fontSize = 9.sp, fontWeight = FontWeight.Black, color = primaryTextColor, modifier = Modifier.weight(1.4f), textAlign = TextAlign.End)
            }

            HorizontalDivider(thickness = 1.dp, color = borderColor)

            if (modelStats.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(14.dp), contentAlignment = Alignment.Center) {
                    Text("اطلاعات مدلی در این بازه ثبت نشده است", fontSize = 10.5.sp, color = secondaryTextColor)
                }
            } else {
                modelStats.forEachIndexed { idx, stat ->
                    val parsedCol = PersianUtils.parseColor(stat.colorCode)
                    val rowBg = if (idx % 2 == 1) tableAltRowBg else Color.Transparent
                    val displayModelName = if (tab == AnalysisTab.TOTAL || tab == AnalysisTab.YEARLY) {
                        stat.name
                    } else {
                        "${PersianUtils.toPersianDigits(idx + 1)}. ${stat.name}"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(rowBg)
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // ۱. نام مدل با نوار رنگی
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(20.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(parsedCol)
                            )
                            Text(
                                text = displayModelName,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // ۲. مجموع تعداد فاکتورها از هر مدل
                        Text(
                            text = "${PersianUtils.toPersianDigits(stat.orderCount)} فاکتور",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = primaryTextColor,
                            modifier = Modifier.weight(1.0f),
                            textAlign = TextAlign.Center
                        )

                        // ۳. مجموع تعداد واحدها در هر مدل
                        Text(
                            text = "${PersianUtils.formatNumberWithCommas(stat.totalUnits)} واحد",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryTextColor,
                            modifier = Modifier.weight(1.0f),
                            textAlign = TextAlign.Center
                        )

                        // ۴. مجموع دستمزد هر مدل بر اساس واحد
                        Column(
                            modifier = Modifier.weight(1.4f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = PersianUtils.formatNumberWithCommas(stat.totalAmount),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = primaryTextColor,
                                textAlign = TextAlign.End
                            )
                            Text(
                                text = "(${PersianUtils.toPersianDigits(String.format(java.util.Locale.US, "%.1f", stat.percentOfTotal))}٪)",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Normal,
                                color = secondaryTextColor,
                                textAlign = TextAlign.End
                            )
                        }
                    }

                    if (idx < modelStats.size - 1) {
                        HorizontalDivider(thickness = 0.5.dp, color = borderColor.copy(alpha = 0.4f))
                    }
                }

                // سطر جمع کل جدول دوم
                HorizontalDivider(thickness = 1.2.dp, color = borderColor)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(tableHeaderBg.copy(alpha = 0.85f))
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("جمع کل (${PersianUtils.toPersianDigits(modelStats.size)} مدل)", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = primaryTextColor, modifier = Modifier.weight(1.3f), textAlign = TextAlign.Start)
                    Text("${PersianUtils.toPersianDigits(modelStats.sumOf { it.orderCount })} فاکتور", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = primaryTextColor, modifier = Modifier.weight(1.0f), textAlign = TextAlign.Center)
                    Text("${PersianUtils.formatNumberWithCommas(totalUnits)} واحد", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = primaryTextColor, modifier = Modifier.weight(1.0f), textAlign = TextAlign.Center)
                    Text(PersianUtils.formatCurrency(totalWork, currencyUnit), fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF2563EB), modifier = Modifier.weight(1.4f), textAlign = TextAlign.End)
                }
            }
        }
    }
}

// =============================================================================
// HELPER: اشتراک‌گذاری متن کامل گزارش آنالیز هوشمند
// =============================================================================
private fun shareAnalysisReport(
    context: Context,
    tabTitle: String,
    periodOrders: List<FurnitureOrder>,
    periodPayments: List<PaymentRecord>,
    totalWork: Long,
    totalPaid: Long,
    balance: Long,
    currencyUnit: String,
    modelStats: List<ModelStat>,
    payerStats: List<PayerStat>
) {
    try {
        val sb = StringBuilder()
        sb.append("📊 گزارش جامع اطلاعات و آنالیز هوشمند خیاطان\n")
        sb.append("بخش: $tabTitle\n")
        sb.append("تاریخ گزارش: ${PersianUtils.toPersianDigits(PersianUtils.getTodayJalaliString())}\n")
        sb.append("==================================\n\n")

        sb.append("📈 خلاصه وضعیت مالی و کارکرد:\n")
        sb.append("• مجموع کارکرد فاکتورها: ${PersianUtils.formatCurrency(totalWork, currencyUnit)} (${PersianUtils.toPersianDigits(periodOrders.size)} فاکتور)\n")
        sb.append("• مجموع کل دریافتی‌ها: ${PersianUtils.formatCurrency(totalPaid, currencyUnit)} (${PersianUtils.toPersianDigits(periodPayments.size)} دریافتی)\n")
        val balText = PersianUtils.formatRemainingBalanceText(balance, currencyUnit)
        sb.append("• وضعیت مانده حساب: $balText\n\n")

        if (modelStats.isNotEmpty()) {
            sb.append("🛋️ برترین مدل‌های تولیدی بر اساس درآمد:\n")
            modelStats.take(5).forEachIndexed { i, m ->
                sb.append("${PersianUtils.toPersianDigits(i + 1)}. ${m.name}: ${PersianUtils.toPersianDigits(m.orderCount)} دست، ${PersianUtils.formatNumberWithCommas(m.totalUnits)} واحد، درآمد: ${PersianUtils.formatCurrency(m.totalAmount, currencyUnit)}\n")
            }
            sb.append("\n")
        }

        if (payerStats.isNotEmpty()) {
            sb.append("💳 پرداخت‌کنندگان و طرف‌حساب‌ها:\n")
            payerStats.take(5).forEachIndexed { i, p ->
                sb.append("${PersianUtils.toPersianDigits(i + 1)}. ${p.payerName}: ${PersianUtils.toPersianDigits(p.paymentCount)} واریزی، مبلغ: ${PersianUtils.formatCurrency(p.totalPaid, currencyUnit)}\n")
            }
        }

        sb.append("\n==================================\n")
        sb.append("نرم‌افزار مدیریت هوشمند کارکرد و حسابداری کارگاه خیاطان")

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "گزارش اطلاعات و آنالیز کارکرد")
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }
        context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری گزارش تحلیلی"))
    } catch (e: Exception) {
    }
}
