package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.FurnitureOrder
import com.example.model.ModelPreset
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Rose600
import com.example.util.PersianUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFilterDialog(
    isOpen: Boolean,
    currentQuery: String,
    currentCustomer: String?,
    currentModel: String?,
    currentDate: String?,
    currentInvoice: String?,
    allOrders: List<FurnitureOrder>,
    modelPresets: List<ModelPreset>,
    customers: List<String>,
    currencyUnit: String,
    onDismiss: () -> Unit,
    onApplyFilters: (query: String, customer: String?, model: String?, date: String?, invoice: String?) -> Unit,
    onClearFilters: () -> Unit
) {
    if (!isOpen) return

    var query by remember(isOpen, currentQuery) { mutableStateOf(currentQuery) }
    var selectedCustomer by remember(isOpen, currentCustomer) { mutableStateOf(currentCustomer ?: "") }
    var selectedModel by remember(isOpen, currentModel) { mutableStateOf(currentModel ?: "") }
    var selectedDate by remember(isOpen, currentDate) { mutableStateOf(currentDate ?: "") }
    var selectedInvoice by remember(isOpen, currentInvoice) { mutableStateOf(currentInvoice ?: "") }

    var isDatePickerOpen by remember { mutableStateOf(false) }

    // Live preview of matched orders
    val matchedOrders = remember(query, selectedCustomer, selectedModel, selectedDate, selectedInvoice, allOrders) {
        val q = PersianUtils.toEnglishDigits(query.trim()).lowercase()
        val c = selectedCustomer.trim()
        val m = selectedModel.trim()
        val d = selectedDate.trim()
        val inv = PersianUtils.toEnglishDigits(selectedInvoice.trim())

        allOrders.filter { order ->
            val matchCustomer = c.isBlank() || order.customerName.trim().contains(c, ignoreCase = true)
            val matchModel = m.isBlank() || order.modelName.trim().contains(m, ignoreCase = true)
            val matchDate = d.isBlank() || order.dateJalali.trim() == d
            val matchInvoice = inv.isBlank() ||
                    PersianUtils.toEnglishDigits(order.invoiceNumber).contains(inv) ||
                    order.orderNumber.toString().contains(inv)

            val matchQuery = if (q.isBlank()) {
                true
            } else {
                val invNum = PersianUtils.toEnglishDigits(order.invoiceNumber)
                val ordNum = order.orderNumber.toString()
                val workshopInv = PersianUtils.toEnglishDigits(order.workshopInvoiceNumber).lowercase()
                val model = order.modelName.lowercase()
                val customer = order.customerName.lowercase()
                val fabric = order.fabricName.lowercase()
                val notes = order.notes.lowercase()
                val dateJ = PersianUtils.toEnglishDigits(order.dateJalali)

                invNum.contains(q) || ordNum.contains(q) || workshopInv.contains(q) || model.contains(q) ||
                        customer.contains(q) || fabric.contains(q) || notes.contains(q) || dateJ.contains(q)
            }

            matchCustomer && matchModel && matchDate && matchInvoice && matchQuery
        }
    }

    val totalCalculatedWage = remember(matchedOrders) {
        matchedOrders.sumOf { it.calculatedTotal }
    }

    val availableModelNames = remember(allOrders, modelPresets) {
        val names = linkedSetOf<String>()
        modelPresets.forEach { if (it.name.isNotBlank()) names.add(it.name.trim()) }
        allOrders.forEach { if (it.modelName.isNotBlank()) names.add(it.modelName.trim()) }
        names.toList()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.88f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "جستجو و فیلتر پیشرفته",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "فیلتر دقیق فاکتورهای کارکرد",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )

                // Scrollable Filters Form
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. مدل
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Chair,
                                contentDescription = null,
                                tint = Color(0xFF8B5CF6),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "مدل",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        OutlinedTextField(
                            value = selectedModel,
                            onValueChange = { selectedModel = it },
                            placeholder = { Text("نام مدل را بنویسید یا انتخاب کنید...", fontSize = 12.sp) },
                            singleLine = true,
                            trailingIcon = {
                                if (selectedModel.isNotBlank()) {
                                    IconButton(onClick = { selectedModel = "" }) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = "پاک کردن", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Quick Model Suggestion Chips
                        if (availableModelNames.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                availableModelNames.take(8).forEach { mName ->
                                    val isSelected = selectedModel == mName
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedModel = if (isSelected) "" else mName },
                                        label = { Text(mName, fontSize = 11.sp) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 2. نام مشتری یا نمایشگاه
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Storefront,
                                contentDescription = null,
                                tint = Emerald600,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "نام مشتری یا نمایشگاه",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        OutlinedTextField(
                            value = selectedCustomer,
                            onValueChange = { selectedCustomer = it },
                            placeholder = { Text("نام مشتری، همکار یا نمایشگاه...", fontSize = 12.sp) },
                            singleLine = true,
                            trailingIcon = {
                                if (selectedCustomer.isNotBlank()) {
                                    IconButton(onClick = { selectedCustomer = "" }) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = "پاک کردن", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Quick Customer Suggestion Chips
                        if (customers.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                customers.take(8).forEach { cName ->
                                    val isSelected = selectedCustomer == cName
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedCustomer = if (isSelected) "" else cName },
                                        label = { Text(cName, fontSize = 11.sp) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 3. شماره فاکتور
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Receipt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "شماره فاکتور",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        OutlinedTextField(
                            value = selectedInvoice,
                            onValueChange = { selectedInvoice = it },
                            placeholder = { Text("مثال: 12 یا 1403", fontSize = 12.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            trailingIcon = {
                                if (selectedInvoice.isNotBlank()) {
                                    IconButton(onClick = { selectedInvoice = "" }) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = "پاک کردن", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 4. تاریخ با آیکون تقویم و انتخاب از تقویم
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "تاریخ ثبت (شمسی)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        OutlinedTextField(
                            value = if (selectedDate.isNotBlank()) PersianUtils.toPersianDigits(selectedDate) else "",
                            onValueChange = { selectedDate = PersianUtils.toEnglishDigits(it) },
                            placeholder = { Text("مثال: 1404/01/15 یا از تقویم انتخاب کنید", fontSize = 12.sp) },
                            singleLine = true,
                            leadingIcon = {
                                IconButton(onClick = { isDatePickerOpen = true }) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = "انتخاب از تقویم",
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            trailingIcon = {
                                if (selectedDate.isNotBlank()) {
                                    IconButton(onClick = { selectedDate = "" }) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = "پاک کردن", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Quick Date Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val todayJalali = remember { PersianUtils.getTodayJalaliString() }
                            SuggestionChip(
                                onClick = { selectedDate = todayJalali },
                                label = { Text("امروز (${PersianUtils.toPersianDigits(todayJalali)})", fontSize = 10.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                            FilledTonalButton(
                                onClick = { isDatePickerOpen = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Text("باز کردن تقویم", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // 5. جستجوی متنی آزاد (پارچه، یادداشت و ...)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "سایر عبارات (نام پارچه، رنگ، یادداشت...)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text("جستجو در تمام فیلدها...", fontSize = 12.sp) },
                            singleLine = true,
                            trailingIcon = {
                                if (query.isNotBlank()) {
                                    IconButton(onClick = { query = "" }) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = "پاک کردن", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Live Search Result Banner
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "یافت‌شده: ${PersianUtils.toPersianDigits(matchedOrders.size)} فاکتور",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "مجموع: ${PersianUtils.formatCurrency(totalCalculatedWage, currencyUnit)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            query = ""
                            selectedCustomer = ""
                            selectedModel = ""
                            selectedDate = ""
                            selectedInvoice = ""
                            onClearFilters()
                            onDismiss()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Rose600),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("پاکسازی همه", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            onApplyFilters(
                                query,
                                selectedCustomer.ifBlank { null },
                                selectedModel.ifBlank { null },
                                selectedDate.ifBlank { null },
                                selectedInvoice.ifBlank { null }
                            )
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.4f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.FilterAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("اعمال فیلتر", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Secondary Jalali Date Picker Dialog
    JalaliCalendarDialog(
        isOpen = isDatePickerOpen,
        onDismiss = { isDatePickerOpen = false },
        onDateSelected = { pickedDate ->
            selectedDate = pickedDate
            isDatePickerOpen = false
        }
    )
}
