package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.ModelPreset
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Rose600
import com.example.util.COLOR_PALETTE
import com.example.util.PersianUtils

@Composable
fun ModelPresetsDialog(
    isOpen: Boolean,
    presets: List<ModelPreset>,
    currencyUnit: String,
    onDismiss: () -> Unit,
    onSavePreset: (ModelPreset) -> Unit,
    onDeletePreset: (ModelPreset) -> Unit
) {
    if (!isOpen) return

    var editingPreset by remember { mutableStateOf<ModelPreset?>(null) }
    var isAddingNew by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("2000000") }
    var unitsStr by remember { mutableStateOf("6") }
    var colorCode by remember { mutableStateOf("#2563EB") }

    fun startAdd() {
        editingPreset = null
        isAddingNew = true
        name = ""
        priceStr = "2000000"
        unitsStr = "6"
        colorCode = "#2563EB"
    }

    fun startEdit(preset: ModelPreset) {
        editingPreset = preset
        isAddingNew = true
        name = preset.name
        priceStr = preset.defaultPricePerSet.toString()
        unitsStr = if (preset.defaultUnitsPerSet % 1.0 == 0.0) preset.defaultUnitsPerSet.toInt().toString() else preset.defaultUnitsPerSet.toString()
        colorCode = preset.colorCode
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
                .fillMaxHeight(0.88f)
                .padding(vertical = 12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
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
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "مدیریت مدل‌های پیش‌فرض کارگاه",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isAddingNew) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = if (editingPreset != null) "ویرایش مشخصات مدل" else "افزودن مدل مبلمان جدید",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                OutlinedTextField(
                                    value = name,
                                    onValueChange = {
                                        name = it
                                        if (it.isNotBlank()) colorCode = PersianUtils.getModelColor(it)
                                    },
                                    label = { Text("نام مدل", fontSize = 11.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = priceStr,
                                        onValueChange = { priceStr = it },
                                        label = { Text("دستمزد پیش‌فرض دست", fontSize = 10.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1.3f)
                                    )

                                    OutlinedTextField(
                                        value = unitsStr,
                                        onValueChange = { unitsStr = it },
                                        label = { Text("تعداد واحد دست", fontSize = 10.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                 // 36 Curated Colors in 3 responsive rows (matching order dialog)
                                Text(text = "رنگ مدل:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
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

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { isAddingNew = false }) {
                                        Text("انصراف", fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Button(
                                        onClick = {
                                            if (name.isBlank()) return@Button
                                            val price = PersianUtils.toEnglishDigits(priceStr).toLongOrNull() ?: 2000000L
                                            val units = PersianUtils.toEnglishDigits(unitsStr).toDoubleOrNull() ?: 6.0
                                            val preset = ModelPreset(
                                                id = editingPreset?.id ?: 0L,
                                                name = name.trim(),
                                                defaultPricePerSet = price,
                                                defaultUnitsPerSet = units,
                                                colorCode = colorCode,
                                                description = ""
                                            )
                                            onSavePreset(preset)
                                            isAddingNew = false
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("ذخیره مدل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = { startAdd() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("افزودن مدل جدید به پیش‌فرض‌ها", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Presets List
                    if (presets.isEmpty()) {
                        Text(
                            text = "هنوز مدلی در لیست پیش‌فرض ثبت نشده است.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        presets.forEach { preset ->
                            val presetColor = PersianUtils.parseColor(preset.colorCode)
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
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
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(presetColor)
                                        )
                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = preset.name,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "${PersianUtils.toPersianDigits(preset.defaultUnitsPerSet)} واحدی",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = PersianUtils.formatCurrency(preset.defaultPricePerSet, currencyUnit),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Emerald600
                                            )
                                        }
                                    }

                                    Row {
                                        IconButton(onClick = { startEdit(preset) }, modifier = Modifier.size(30.dp)) {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = "ویرایش", modifier = Modifier.size(14.dp))
                                        }
                                        IconButton(onClick = { onDeletePreset(preset) }, modifier = Modifier.size(30.dp)) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = Rose600, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
