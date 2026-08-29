package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Emerald600
import com.example.util.PersianUtils

@Composable
fun QuickFilterBar(
    searchQuery: String,
    selectedCustomer: String?,
    selectedModel: String?,
    selectedDate: String?,
    selectedInvoice: String?,
    onClearSearch: () -> Unit,
    onClearCustomer: () -> Unit,
    onClearModel: () -> Unit,
    onClearDate: () -> Unit,
    onClearInvoice: () -> Unit,
    onClearAll: () -> Unit,
    onOpenFilterDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasFilter = searchQuery.isNotBlank() || selectedCustomer != null || selectedModel != null || selectedDate != null || selectedInvoice != null
    if (!hasFilter) return

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(
                onClick = onOpenFilterDialog,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "تنظیم فیلترها",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (searchQuery.isNotBlank()) {
                    FilterChipItem(
                        label = "جستجو: $searchQuery",
                        color = MaterialTheme.colorScheme.primary,
                        onClear = onClearSearch
                    )
                }
                selectedCustomer?.let { cust ->
                    FilterChipItem(
                        label = "مشتری: $cust",
                        color = Emerald600,
                        onClear = onClearCustomer
                    )
                }
                selectedModel?.let { model ->
                    FilterChipItem(
                        label = "مدل: $model",
                        color = Color(0xFF8B5CF6),
                        onClear = onClearModel
                    )
                }
                selectedInvoice?.let { inv ->
                    FilterChipItem(
                        label = "فاکتور: #${PersianUtils.toPersianDigits(inv)}",
                        color = MaterialTheme.colorScheme.primary,
                        onClear = onClearInvoice
                    )
                }
                selectedDate?.let { date ->
                    FilterChipItem(
                        label = "تاریخ: ${PersianUtils.toPersianDigits(date)}",
                        color = Color(0xFFF59E0B),
                        onClear = onClearDate
                    )
                }
            }

            TextButton(
                onClick = onClearAll,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text(
                    text = "حذف همه",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    color: Color,
    onClear: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "حذف",
                tint = color,
                modifier = Modifier
                    .size(12.dp)
                    .clickable { onClear() }
            )
        }
    }
}

