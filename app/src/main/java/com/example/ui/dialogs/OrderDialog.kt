package com.example.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.TipsAndUpdates
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
import androidx.compose.ui.window.DialogProperties
import com.example.model.FurnitureOrder
import com.example.model.ModelPreset
import com.example.ui.theme.Emerald600
import com.example.util.COLOR_PALETTE
import com.example.util.PersianUtils

data class ModelSuggestion(
    val name: String,
    val pricePerSet: Long,
    val unitsPerSet: Double,
    val colorCode: String,
    val usageCount: Int = 1
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDialog(
    isOpen: Boolean,
    initialOrder: FurnitureOrder?,
    modelPresets: List<ModelPreset>,
    existingOrders: List<FurnitureOrder>,
    deletedModelNames: Set<String> = emptySet(),
    unitRules: List<com.example.model.UnitConversionRule> = emptyList(),
    customers: List<String>,
    currencyUnit: String,
    nextOrderNumber: Long,
    onDismiss: () -> Unit,
    onDeleteModel: (String) -> Unit = {},
    onSave: (FurnitureOrder, Boolean) -> Unit
) {
    if (!isOpen) return

    var invoiceNumber by remember {
        mutableStateOf(initialOrder?.invoiceNumber ?: nextOrderNumber.toString())
    }
    var modelName by remember {
        mutableStateOf(initialOrder?.modelName ?: "")
    }
    var pricePerSet by remember {
        mutableStateOf((initialOrder?.pricePerSet ?: 2000000L).toString())
    }
    var unitsPerSet by remember {
        mutableStateOf((initialOrder?.unitsPerSet ?: 6.0).let {
            if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
        })
    }
    var countFormula by remember {
        mutableStateOf(initialOrder?.countFormula ?: "3+3+1+1")
    }
    var dateJalali by remember {
        mutableStateOf(initialOrder?.dateJalali ?: PersianUtils.getTodayJalaliString())
    }
    var customerName by remember {
        mutableStateOf(initialOrder?.customerName ?: "")
    }
    var fabricName by remember {
        mutableStateOf(initialOrder?.fabricName ?: "")
    }
    var workshopInvoiceNumber by remember {
        mutableStateOf(initialOrder?.workshopInvoiceNumber ?: "")
    }
    var notes by remember {
        mutableStateOf(initialOrder?.notes ?: "")
    }
    var colorCode by remember {
        mutableStateOf(initialOrder?.colorCode ?: "#2563EB")
    }
    var addToPresets by remember { mutableStateOf(false) }

    // Dialog state for calendar picker
    var isDatePickerOpen by remember { mutableStateOf(false) }
    // Dialog state for custom color creator
    var isCustomColorDialogOpen by remember { mutableStateOf(false) }
    var customHexInput by remember { mutableStateOf(colorCode) }

    // Dropdown state for model suggestions
    var isModelDropdownExpanded by remember { mutableStateOf(false) }

    // Aggregate all known models from presets and existing orders, excluding deleted ones
    val knownModels = remember(existingOrders, modelPresets, deletedModelNames) {
        val map = linkedMapOf<String, ModelSuggestion>()
        
        // 1. Load from Presets
        modelPresets.forEach { p ->
            val trimmed = p.name.trim()
            val key = trimmed.lowercase()
            if (key.isNotBlank() && !deletedModelNames.contains(key)) {
                map[key] = ModelSuggestion(
                    name = trimmed,
                    pricePerSet = p.defaultPricePerSet,
                    unitsPerSet = p.defaultUnitsPerSet,
                    colorCode = p.colorCode
                )
            }
        }
        
        // 2. Load and override with latest from existing orders
        existingOrders.sortedBy { it.createdAt }.forEach { ord ->
            val trimmed = ord.modelName.trim()
            val key = trimmed.lowercase()
            if (key.isNotBlank() && !deletedModelNames.contains(key)) {
                val existing = map[key]
                map[key] = ModelSuggestion(
                    name = trimmed,
                    pricePerSet = ord.pricePerSet,
                    unitsPerSet = ord.unitsPerSet,
                    colorCode = ord.colorCode.ifBlank { existing?.colorCode ?: "#2563EB" },
                    usageCount = (existing?.usageCount ?: 0) + 1
                )
            }
        }
        
        map.values.sortedByDescending { it.usageCount }
    }

    // Filter matching models based on input query
    val matchingSuggestions = remember(modelName, knownModels) {
        val q = modelName.trim().lowercase()
        if (q.isBlank()) {
            knownModels
        } else {
            knownModels.filter { it.name.lowercase().contains(q) }
        }
    }

    fun applyModel(suggestion: ModelSuggestion) {
        modelName = suggestion.name
        pricePerSet = suggestion.pricePerSet.toString()
        unitsPerSet = if (suggestion.unitsPerSet % 1.0 == 0.0) {
            suggestion.unitsPerSet.toInt().toString()
        } else {
            suggestion.unitsPerSet.toString()
        }
        colorCode = suggestion.colorCode
        isModelDropdownExpanded = false
    }

    // Recalculate units and total
    val calculatedUnits = remember(countFormula, unitRules) {
        PersianUtils.evaluateCountFormula(countFormula, unitRules)
    }
    val calculatedTotal = remember(calculatedUnits, pricePerSet, unitsPerSet) {
        val price = PersianUtils.toEnglishDigits(pricePerSet).toLongOrNull() ?: 0L
        val units = PersianUtils.toEnglishDigits(unitsPerSet).toDoubleOrNull() ?: 8.0
        if (units > 0) {
            ((calculatedUnits / units) * price).toLong()
        } else 0L
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
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
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = if (initialOrder != null) "ویرایش فاکتور" else "ثبت فاکتور جدید",
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

                // Body Form
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Invoice Number & Jalali Date Picker
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = invoiceNumber,
                            onValueChange = { invoiceNumber = it },
                            label = { Text("شماره فاکتور", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )

                        // Jalali Date Field with Calendar Picker trigger
                        Box(
                            modifier = Modifier
                                .weight(1.3f)
                                .clickable { isDatePickerOpen = true }
                        ) {
                            OutlinedTextField(
                                value = PersianUtils.toPersianDigits(dateJalali),
                                onValueChange = { dateJalali = PersianUtils.toEnglishDigits(it) },
                                label = { Text("تاریخ فاکتور (انتخاب تقویم)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = { isDatePickerOpen = true }) {
                                        Icon(
                                            imageVector = Icons.Outlined.CalendarMonth,
                                            contentDescription = "انتخاب تاریخ",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Model Name Input ("مدل مبل") with Autocomplete Suggestions Dropdown
                    ExposedDropdownMenuBox(
                        expanded = isModelDropdownExpanded && matchingSuggestions.isNotEmpty(),
                        onExpandedChange = { isModelDropdownExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = modelName,
                            onValueChange = { input ->
                                modelName = input
                                val trimmed = input.trim()
                                if (trimmed.isNotBlank()) {
                                    val exact = knownModels.find { it.name.equals(trimmed, ignoreCase = true) }
                                    if (exact != null) {
                                        colorCode = exact.colorCode
                                    } else {
                                        colorCode = PersianUtils.getModelColor(input)
                                    }
                                }
                                isModelDropdownExpanded = true
                            },
                            label = { Text("مدل مبل (اجباری)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            placeholder = { Text("مثلاً چستر کلاسیک، ال راحتی، کوئین، صدفی...", fontSize = 11.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(PersianUtils.parseColor(colorCode))
                                        .border(1.dp, Color.White, CircleShape)
                                )
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isModelDropdownExpanded && matchingSuggestions.isNotEmpty())
                            },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = isModelDropdownExpanded && matchingSuggestions.isNotEmpty(),
                            onDismissRequest = { isModelDropdownExpanded = false },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface)
                                .heightIn(max = 240.dp)
                        ) {
                            matchingSuggestions.forEach { suggestion ->
                                DropdownMenuItem(
                                    text = {
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
                                                        .size(14.dp)
                                                        .clip(CircleShape)
                                                        .background(PersianUtils.parseColor(suggestion.colorCode))
                                                )
                                                Text(
                                                    text = suggestion.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "${PersianUtils.toPersianDigits(suggestion.unitsPerSet)} واحدی",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = PersianUtils.formatCurrency(suggestion.pricePerSet, currencyUnit),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { onDeleteModel(suggestion.name) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteOutline,
                                                        contentDescription = "حذف مدل",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    onClick = { applyModel(suggestion) }
                                )
                            }
                        }
                    }

                    // Quick Model Suggestion Chips (if known models exist) with delete capability
                    if (knownModels.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.TipsAndUpdates,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "مدل‌های ثبت‌شده کارگاه (کلیک جهت انتخاب، ضربدر جهت حذف):",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                knownModels.forEach { item ->
                                    val isCurrent = modelName.trim().equals(item.name.trim(), ignoreCase = true)
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier.clickable { applyModel(item) }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                                            modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(PersianUtils.parseColor(item.colorCode))
                                            )
                                            Text(
                                                text = item.name,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "(${PersianUtils.toPersianDigits(item.unitsPerSet)}و)",
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            // Delete icon for model
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .clickable { onDeleteModel(item.name) }
                                                    .padding(2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "حذف مدل",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Color Palette Selector & Custom Color Creator
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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
                                        imageVector = Icons.Outlined.Palette,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "انتخاب یا ساخت رنگ اختصاصی:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // Custom Color Creator Trigger
                                TextButton(
                                    onClick = {
                                        customHexInput = colorCode
                                        isCustomColorDialogOpen = true
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddCircleOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "ساخت رنگ دلخواه",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            // 36 Curated Colors in 3 responsive rows
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                COLOR_PALETTE.chunked(12).forEach { rowColors ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        rowColors.forEach { c ->
                                            val isSelected = colorCode.equals(c.hex, ignoreCase = true)
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(c.color)
                                                    .border(
                                                        if (isSelected) 2.dp else 0.5.dp,
                                                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.2f),
                                                        CircleShape
                                                    )
                                                    .clickable { colorCode = c.hex },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Wage per Set ("دستمزد هر دست") & Units per Set (Editable)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = pricePerSet,
                            onValueChange = { pricePerSet = it },
                            label = { Text("دستمزد هر دست ($currencyUnit)", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.3f)
                        )

                        OutlinedTextField(
                            value = unitsPerSet,
                            onValueChange = { unitsPerSet = it },
                            label = { Text("تعداد واحد در ۱ دست", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Count Formula & Quick Piece Buttons ("تعداد قطعات مبل")
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "تعداد قطعات مبل (فرمول شمارش):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            OutlinedTextField(
                                value = countFormula,
                                onValueChange = { countFormula = it },
                                placeholder = { Text("مانند 3+3+2+1+1 یا 8", fontSize = 11.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Quick piece adders (dynamically generated from conversion rules)
                            val enabledRules = remember(unitRules) {
                                unitRules.filter { it.isEnabled }
                            }
                            val quickPieceItems = remember(enabledRules) {
                                if (enabledRules.isNotEmpty()) {
                                    enabledRules.map { r ->
                                        val key = r.pieceKey.ifBlank {
                                            if (r.pieceCount % 1.0 == 0.0) r.pieceCount.toInt().toString() else r.pieceCount.toString()
                                        }
                                        val label = "+$key"
                                        key to label
                                    }
                                } else {
                                    listOf("3" to "+۳ نفره", "2" to "+۲ نفره", "1" to "+۱ نفره", "0.5" to "+۰.۵ واحد")
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "افزودن سریع:", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                quickPieceItems.forEach { (pVal, pLabel) ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                            .clickable {
                                                countFormula = if (countFormula.isBlank()) pVal else "$countFormula+$pVal"
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = pLabel,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Live Calculation Preview Banner
                    Surface(
                        color = Emerald600.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Emerald600.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "مجموع واحد محاسبه‌شده:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald600
                                )
                                Text(
                                    text = "${PersianUtils.formatNumberWithCommas(calculatedUnits)} واحد",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Emerald600
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "مبلغ کل فاکتور:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald600
                                )
                                Text(
                                    text = PersianUtils.formatCurrency(calculatedTotal, currencyUnit),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Emerald600
                                )
                            }
                        }
                    }

                    // Customer Name
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("نام مشتری / سفارش دهنده", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Fabric Name
                    OutlinedTextField(
                        value = fabricName,
                        onValueChange = { fabricName = it },
                        label = { Text("نام و مشخصات پارچه", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        placeholder = { Text("مثلاً مازراتی کد ۳۴ طوسی، مخمل جاسمین...", fontSize = 11.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Notes ("توضیحات")
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("توضیحات", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        placeholder = { Text("توضیحات تکمیلی، جزئیات دوخت، موعد تحویل و...", fontSize = 11.sp) },
                        maxLines = 3,
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

                    // "ثبت نهایی فاکتور" Button
                    Button(
                        onClick = {
                            if (modelName.isBlank()) return@Button
                            val price = PersianUtils.toEnglishDigits(pricePerSet).toLongOrNull() ?: 2000000L
                            val units = PersianUtils.toEnglishDigits(unitsPerSet).toDoubleOrNull() ?: 6.0
                            val order = FurnitureOrder(
                                id = initialOrder?.id ?: 0L,
                                orderNumber = initialOrder?.orderNumber ?: nextOrderNumber,
                                invoiceNumber = invoiceNumber.ifBlank { nextOrderNumber.toString() },
                                modelName = modelName.trim(),
                                pricePerSet = price,
                                unitsPerSet = if (units <= 0) 6.0 else units,
                                countFormula = countFormula.ifBlank { "6" },
                                calculatedUnits = calculatedUnits,
                                calculatedTotal = calculatedTotal,
                                dateJalali = dateJalali,
                                dateGregorian = initialOrder?.dateGregorian ?: PersianUtils.getTodayGregorianString(),
                                customerName = customerName.trim(),
                                fabricName = fabricName.trim(),
                                workshopInvoiceNumber = workshopInvoiceNumber.trim(),
                                notes = notes.trim(),
                                colorCode = colorCode,
                                createdAt = initialOrder?.createdAt ?: System.currentTimeMillis()
                            )
                            onSave(order, addToPresets)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = if (initialOrder != null) "بروزرسانی فاکتور" else "ثبت نهایی فاکتور",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Jalali Date Picker Dialog
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

    // Custom Color Creator Dialog
    if (isCustomColorDialogOpen) {
        Dialog(
            onDismissRequest = { isCustomColorDialogOpen = false }
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "ساخت و انتخاب رنگ دلخواه کارگاه",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(PersianUtils.parseColor(customHexInput))
                                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        )

                        OutlinedTextField(
                            value = customHexInput,
                            onValueChange = { customHexInput = it },
                            label = { Text("کد هگز رنگ (HEX)", fontSize = 11.sp) },
                            placeholder = { Text("#3B82F6 یا #A855F7", fontSize = 11.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Popular tone presets for quick custom generator
                    Text(
                        text = "طیف‌های پیشنهادی سریع:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val customSuggestions = listOf(
                        "#1E293B", "#334155", "#475569", "#64748B",
                        "#0369A1", "#0284C7", "#0EA5E9", "#38BDF8",
                        "#047857", "#059669", "#10B981", "#34D399",
                        "#B45309", "#D97706", "#F59E0B", "#FBBF24",
                        "#BE123C", "#E11D48", "#F43F5E", "#FB7185",
                        "#6D28D9", "#7C3AED", "#8B5CF6", "#A78BFA"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        customSuggestions.chunked(8).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                row.forEach { hex ->
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(PersianUtils.parseColor(hex))
                                            .border(1.dp, Color.White, CircleShape)
                                            .clickable { customHexInput = hex }
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { isCustomColorDialogOpen = false }) {
                            Text("انصراف", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                colorCode = customHexInput.trim()
                                isCustomColorDialogOpen = false
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("اعمال رنگ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
