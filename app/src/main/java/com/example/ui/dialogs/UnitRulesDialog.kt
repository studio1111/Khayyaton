package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.UnitConversionRule
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.util.PersianUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitRulesDialog(
    rules: List<UnitConversionRule>,
    onSaveRule: (UnitConversionRule) -> Unit,
    onDeleteRule: (UnitConversionRule) -> Unit,
    onRestoreDefaults: () -> Unit,
    onDismiss: () -> Unit
) {
    var pieceCountInput by remember { mutableStateOf("") }
    var calculatedUnitsInput by remember { mutableStateOf("") }
    var editingRuleId by remember { mutableStateOf<Long?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Test preview calculation
    var testFormula by remember { mutableStateOf("3+3+2+1+1") }
    val testEvaluated = remember(testFormula, rules) {
        PersianUtils.evaluateCountFormula(testFormula, rules)
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Emerald600.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Calculate,
                                contentDescription = null,
                                tint = Emerald600,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "قوانین تبدیل واحد",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "تبدیل تعداد قطعات به واحد مشخص قبل از جمع",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Interactive Test Banner
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💡 تست زنده تبدیل و محاسبه:",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "مجموع واحد: ${PersianUtils.formatNumberWithCommas(testEvaluated)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Emerald600
                            )
                        }

                        OutlinedTextField(
                            value = testFormula,
                            onValueChange = { testFormula = it },
                            placeholder = { Text("مثلاً 3+3+2+1") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Input Box for Adding / Editing Rules
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (editingRuleId != null) "ویرایش قانون تبدیل" else "افزودن قانون جدید تبدیل",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = pieceCountInput,
                                onValueChange = { pieceCountInput = it; errorMessage = null },
                                label = { Text("تعداد قطعه یا عنوان (مثلاً کنج یا ۳)", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )

                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "تبدیل به",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )

                            OutlinedTextField(
                                value = calculatedUnitsInput,
                                onValueChange = { calculatedUnitsInput = it; errorMessage = null },
                                label = { Text("واحد محاسبه (مثلا ۱.۵ یا ۲)", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }

                        if (errorMessage != null) {
                            Text(
                                text = errorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (editingRuleId != null) {
                                TextButton(
                                    onClick = {
                                        editingRuleId = null
                                        pieceCountInput = ""
                                        calculatedUnitsInput = ""
                                        errorMessage = null
                                    }
                                ) {
                                    Text("انصراف", fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            Button(
                                onClick = {
                                    val keyTrimmed = pieceCountInput.trim()
                                    if (keyTrimmed.isBlank()) {
                                        errorMessage = "لطفاً تعداد قطعه یا عنوان (مثلاً کنج یا ۳) را وارد کنید"
                                        return@Button
                                    }
                                    val unitVal = PersianUtils.toEnglishDigits(calculatedUnitsInput).replace("/", ".").toDoubleOrNull()
                                    if (unitVal == null || unitVal < 0) {
                                        errorMessage = "لطفاً واحد محاسبه‌شده را به درستی وارد کنید (مثلاً ۱.۵)"
                                        return@Button
                                    }
                                    val numVal = PersianUtils.toEnglishDigits(keyTrimmed).replace("/", ".").toDoubleOrNull() ?: 0.0

                                    onSaveRule(
                                        UnitConversionRule(
                                            id = editingRuleId ?: 0L,
                                            pieceKey = keyTrimmed,
                                            pieceCount = numVal,
                                            calculatedUnits = unitVal,
                                            isEnabled = true
                                        )
                                    )
                                    editingRuleId = null
                                    pieceCountInput = ""
                                    calculatedUnitsInput = ""
                                    errorMessage = null
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                            ) {
                                Icon(
                                    imageVector = if (editingRuleId != null) Icons.Default.Check else Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (editingRuleId != null) "بروزرسانی قانون" else "افزودن قانون",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // List of Defined Rules
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "قوانین تعریف‌شده (${PersianUtils.toPersianDigits(rules.size)} مورد):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    TextButton(
                        onClick = onRestoreDefaults,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("بازیابی قوانین پیش‌فرض", fontSize = 10.5.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (rules.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "قانونی ثبت نشده است. جهت ایجاد، مقادیر بالا را وارد کنید.",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(rules, key = { it.id }) { rule ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (rule.isEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (rule.isEnabled) Emerald600.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Switch(
                                            checked = rule.isEnabled,
                                            onCheckedChange = { isChecked ->
                                                onSaveRule(rule.copy(isEnabled = isChecked))
                                            },
                                            modifier = Modifier.size(24.dp)
                                        )

                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                val pieceLabel = rule.pieceKey.ifBlank {
                                                    if (rule.pieceCount % 1.0 == 0.0) rule.pieceCount.toInt().toString() else rule.pieceCount.toString()
                                                }
                                                Text(
                                                    text = "قطعه $pieceLabel",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = if (rule.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "➔",
                                                    fontSize = 12.sp,
                                                    color = Emerald600
                                                )
                                                Text(
                                                    text = "${PersianUtils.formatNumberWithCommas(rule.calculatedUnits)} واحد",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Emerald600
                                                )
                                            }
                                            Text(
                                                text = if (rule.isEnabled) "فعال در محاسبات و افزودن سریع" else "غیرفعال",
                                                fontSize = 9.5.sp,
                                                color = if (rule.isEnabled) Emerald600 else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                editingRuleId = rule.id
                                                pieceCountInput = rule.pieceKey.ifBlank {
                                                    if (rule.pieceCount % 1.0 == 0.0) rule.pieceCount.toInt().toString() else rule.pieceCount.toString()
                                                }
                                                calculatedUnitsInput = if (rule.calculatedUnits % 1.0 == 0.0) rule.calculatedUnits.toInt().toString() else rule.calculatedUnits.toString()
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "ویرایش",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { onDeleteRule(rule) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "حذف",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
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
