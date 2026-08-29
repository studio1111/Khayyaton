package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.CalendarType
import com.example.util.GREGORIAN_MONTH_NAMES
import com.example.util.JALALI_MONTH_NAMES
import com.example.util.PERSIAN_WEEKDAYS
import com.example.util.PersianUtils
import java.util.Calendar
import java.util.Locale

@Composable
fun JalaliCalendarDialog(
    isOpen: Boolean,
    initialCalendarType: CalendarType = CalendarType.JALALI,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    if (!isOpen) return

    var activeTab by remember { mutableStateOf(initialCalendarType) }

    val cal = Calendar.getInstance()
    val todayJalali = remember {
        PersianUtils.gregorianToJalali(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }
    val todayGregorianYear = remember { cal.get(Calendar.YEAR) }
    val todayGregorianMonth = remember { cal.get(Calendar.MONTH) + 1 }
    val todayGregorianDay = remember { cal.get(Calendar.DAY_OF_MONTH) }

    var jalaliYear by remember { mutableStateOf(todayJalali.year) }
    var jalaliMonth by remember { mutableStateOf(todayJalali.month) }

    var gregYear by remember { mutableStateOf(todayGregorianYear) }
    var gregMonth by remember { mutableStateOf(todayGregorianMonth) }

    fun handlePrevMonth() {
        if (activeTab == CalendarType.JALALI) {
            if (jalaliMonth > 1) jalaliMonth -= 1 else { jalaliMonth = 12; jalaliYear -= 1 }
        } else {
            if (gregMonth > 1) gregMonth -= 1 else { gregMonth = 12; gregYear -= 1 }
        }
    }

    fun handleNextMonth() {
        if (activeTab == CalendarType.JALALI) {
            if (jalaliMonth < 12) jalaliMonth += 1 else { jalaliMonth = 1; jalaliYear += 1 }
        } else {
            if (gregMonth < 12) gregMonth += 1 else { gregMonth = 1; gregYear += 1 }
        }
    }

    // Jalali offsets
    val gFirst = PersianUtils.jalaliToGregorian(jalaliYear, jalaliMonth, 1)
    val firstJalaliDateCal = Calendar.getInstance().apply {
        set(gFirst.year, gFirst.month - 1, gFirst.day)
    }
    val javaDayOfWeekJalali = firstJalaliDateCal.get(Calendar.DAY_OF_WEEK)
    val jalaliDayOffset = when (javaDayOfWeekJalali) {
        Calendar.SATURDAY -> 0
        Calendar.SUNDAY -> 1
        Calendar.MONDAY -> 2
        Calendar.TUESDAY -> 3
        Calendar.WEDNESDAY -> 4
        Calendar.THURSDAY -> 5
        Calendar.FRIDAY -> 6
        else -> 0
    }
    val jalaliTotalDays = PersianUtils.getDaysInJalaliMonth(jalaliYear, jalaliMonth)

    // Gregorian offsets
    val firstGregDateCal = Calendar.getInstance().apply {
        set(gregYear, gregMonth - 1, 1)
    }
    val javaDayOfWeekGreg = firstGregDateCal.get(Calendar.DAY_OF_WEEK)
    val gregDayOffset = when (javaDayOfWeekGreg) {
        Calendar.SATURDAY -> 0
        Calendar.SUNDAY -> 1
        Calendar.MONDAY -> 2
        Calendar.TUESDAY -> 3
        Calendar.WEDNESDAY -> 4
        Calendar.THURSDAY -> 5
        Calendar.FRIDAY -> 6
        else -> 0
    }
    val gregTotalDays = PersianUtils.getDaysInGregorianMonth(gregYear, gregMonth)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "تقویم هوشمند شمسی و میلادی",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                // Tabs: Jalali / Gregorian
                TabRow(
                    selectedTabIndex = if (activeTab == CalendarType.JALALI) 0 else 1,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                    divider = {}
                ) {
                    Tab(
                        selected = activeTab == CalendarType.JALALI,
                        onClick = { activeTab = CalendarType.JALALI },
                        text = {
                            Text(
                                "تقویم شمسی",
                                fontWeight = if (activeTab == CalendarType.JALALI) FontWeight.Black else FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    )
                    Tab(
                        selected = activeTab == CalendarType.GREGORIAN,
                        onClick = { activeTab = CalendarType.GREGORIAN },
                        text = {
                            Text(
                                "تقویم میلادی",
                                fontWeight = if (activeTab == CalendarType.GREGORIAN) FontWeight.Black else FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    )
                }

                // Month / Year Navigation
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { handlePrevMonth() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "ماه قبل")
                        }

                        val headerTitle = if (activeTab == CalendarType.JALALI) {
                            "${JALALI_MONTH_NAMES[jalaliMonth - 1]} ${PersianUtils.toPersianDigits(jalaliYear)}"
                        } else {
                            "${GREGORIAN_MONTH_NAMES[gregMonth - 1]} $gregYear"
                        }

                        Text(
                            text = headerTitle,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        IconButton(
                            onClick = { handleNextMonth() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "ماه بعد")
                        }
                    }
                }

                // Weekday headers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    PERSIAN_WEEKDAYS.forEach { dayName ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Month Grid (Rows of 7 days)
                val currentOffset = if (activeTab == CalendarType.JALALI) jalaliDayOffset else gregDayOffset
                val currentTotalDays = if (activeTab == CalendarType.JALALI) jalaliTotalDays else gregTotalDays
                val totalCells = currentOffset + currentTotalDays
                val rowCount = (totalCells + 6) / 7

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (row in 0 until rowCount) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (col in 0 until 7) {
                                val cellIndex = row * 7 + col
                                val dayNum = cellIndex - currentOffset + 1

                                if (dayNum in 1..currentTotalDays) {
                                    val isToday = if (activeTab == CalendarType.JALALI) {
                                        jalaliYear == todayJalali.year && jalaliMonth == todayJalali.month && dayNum == todayJalali.day
                                    } else {
                                        gregYear == todayGregorianYear && gregMonth == todayGregorianMonth && dayNum == todayGregorianDay
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                else Color.Transparent
                                            )
                                            .border(
                                                if (isToday) 1.dp else 0.dp,
                                                if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                if (activeTab == CalendarType.JALALI) {
                                                    val mStr = if (jalaliMonth < 10) "0$jalaliMonth" else jalaliMonth.toString()
                                                    val dStr = if (dayNum < 10) "0$dayNum" else dayNum.toString()
                                                    onDateSelected("$jalaliYear/$mStr/$dStr")
                                                } else {
                                                    val mStr = if (gregMonth < 10) "0$gregMonth" else gregMonth.toString()
                                                    val dStr = if (dayNum < 10) "0$dayNum" else dayNum.toString()
                                                    onDateSelected("$gregYear/$mStr/$dStr")
                                                }
                                                onDismiss()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (activeTab == CalendarType.JALALI) PersianUtils.toPersianDigits(dayNum) else dayNum.toString(),
                                            fontSize = 12.sp,
                                            fontWeight = if (isToday) FontWeight.Black else FontWeight.SemiBold,
                                            color = if (isToday) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Today Button
                Button(
                    onClick = {
                        if (activeTab == CalendarType.JALALI) {
                            onDateSelected(PersianUtils.getTodayJalaliString())
                        } else {
                            onDateSelected(PersianUtils.getTodayGregorianString())
                        }
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val label = if (activeTab == CalendarType.JALALI) {
                        "انتخاب تاریخ امروز شمسی (${PersianUtils.toPersianDigits(PersianUtils.getTodayJalaliString())})"
                    } else {
                        "انتخاب تاریخ امروز میلادی (${PersianUtils.getTodayGregorianString()})"
                    }
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
