package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.PersianUtils

@Composable
fun FooterSummary(
    totalWork: Long,
    totalReceived: Long,
    remainingBalance: Long,
    currencyUnit: String,
    orderCount: Int,
    paymentCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Slate950,
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Total Work ("مجموع کارکرد (X فاکتور)")
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

            // Vertical divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(28.dp)
                    .background(Slate800)
            )

            // 2. Total Received ("کل دریافتی (X فقره)")
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

            // Vertical divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(28.dp)
                    .background(Slate800)
            )

            // 3. Remaining Balance (باقی‌مانده با قانون بدهکاری منفی)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                val balanceColor = when {
                    remainingBalance > 0 -> Rose400
                    remainingBalance == 0L -> Emerald400
                    else -> Rose400
                }

                Text(
                    text = if (remainingBalance < 0) "باقی‌مانده (بدهکاری)" else if (remainingBalance == 0L) "تسویه کامل" else "باقی‌مانده",
                    fontSize = 10.sp,
                    color = Slate400,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = PersianUtils.formatRemainingBalanceText(remainingBalance, currencyUnit),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Black,
                    color = balanceColor
                )
            }
        }
    }
}
