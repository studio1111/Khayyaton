package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.Workshop
import com.example.util.PersianUtils

@Composable
fun WorkshopsDialog(
    isOpen: Boolean,
    workshops: List<Workshop>,
    activeWorkshop: Workshop?,
    onDismiss: () -> Unit,
    onSelectWorkshop: (Long) -> Unit,
    onCreateWorkshop: (String) -> Unit,
    onRenameWorkshop: (Long, String) -> Unit,
    onDeleteWorkshop: (Workshop) -> Unit
) {
    if (!isOpen) return

    var newWorkshopName by remember { mutableStateOf("") }
    var editingWorkshop by remember { mutableStateOf<Workshop?>(null) }
    var editNameText by remember { mutableStateOf("") }
    var workshopWarningTarget by remember { mutableStateOf<Workshop?>(null) }
    var workshopToDelete by remember { mutableStateOf<Workshop?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .padding(vertical = 12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Storefront,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "مدیریت کارگاه‌ها",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "تغییر و جابجایی بین کارگاه‌ها با محاسبات کاملاً مستقل و مجزا",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_workshops_dialog")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Notice Banner
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "هر کارگاه دارای فاکتورها، کارکردها، اسناد پرداختی، مانده حساب و مدل‌های کاملاً مجزا و مستقل از سایر کارگاه‌ها می‌باشد.",
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Add New Workshop Card
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddBusiness,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "افزودن کارگاه جدید",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = newWorkshopName,
                                        onValueChange = { newWorkshopName = it },
                                        placeholder = { Text("نام کارگاه جدید (مثلاً: تولیدی دوم)", fontSize = 11.5.sp) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("input_new_workshop_name"),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )

                                    Button(
                                        onClick = {
                                            if (newWorkshopName.trim().isNotBlank()) {
                                                onCreateWorkshop(newWorkshopName.trim())
                                                newWorkshopName = ""
                                                onDismiss()
                                            }
                                        },
                                        enabled = newWorkshopName.trim().isNotBlank(),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.testTag("btn_create_workshop")
                                    ) {
                                        Text("ایجاد و ورود", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Section Title: Available Workshops
                    item {
                        Text(
                            text = "کارگاه‌های موجود (${PersianUtils.toPersianDigits(workshops.size)} کارگاه):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Workshop items list
                    items(workshops, key = { it.id }) { ws ->
                        val isActive = ws.id == (activeWorkshop?.id ?: 1L)

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isActive) 2.dp else 1.dp,
                                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isActive) Icons.Default.Check else Icons.Outlined.Store,
                                                contentDescription = null,
                                                tint = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = ws.name,
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (isActive) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = MaterialTheme.colorScheme.primary,
                                                        contentColor = Color.White
                                                    ) {
                                                        Text(
                                                            text = "کارگاه فعال",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Action buttons (Edit, Delete)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                editingWorkshop = ws
                                                editNameText = ws.name
                                            },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Edit,
                                                contentDescription = "ویرایش نام",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }

                                        if (true) {
                                            IconButton(
                                                onClick = { workshopWarningTarget = ws },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Delete,
                                                    contentDescription = "حذف کارگاه",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (!isActive) {
                                    Button(
                                        onClick = {
                                            onSelectWorkshop(ws.id)
                                            onDismiss()
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SwapHoriz,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("تغییر و جابجایی به این کارگاه", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("بستن", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Sub-dialog: Rename Workshop
    // -------------------------------------------------------------------------
    if (editingWorkshop != null) {
        val ws = editingWorkshop!!
        AlertDialog(
            onDismissRequest = { editingWorkshop = null },
            title = { Text("ویرایش نام کارگاه", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("نام جدید کارگاه را وارد نمایید:", fontSize = 11.5.sp)
                    OutlinedTextField(
                        value = editNameText,
                        onValueChange = { editNameText = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editNameText.trim().isNotBlank()) {
                            onRenameWorkshop(ws.id, editNameText.trim())
                            editingWorkshop = null
                        }
                    },
                    enabled = editNameText.trim().isNotBlank()
                ) {
                    Text("ذخیره تغییرات")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingWorkshop = null }) {
                    Text("انصراف")
                }
            }
        )
    }

    // -------------------------------------------------------------------------
    // Sub-dialog 1: Initial Warning Dialog
    // (Explicitly warns user that the data is non-recoverable)
    // -------------------------------------------------------------------------
    if (workshopWarningTarget != null) {
        val targetWs = workshopWarningTarget!!
        AlertDialog(
            onDismissRequest = { workshopWarningTarget = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(34.dp)
                )
            },
            title = {
                Text(
                    text = "هشدار حذف کارگاه «${targetWs.name}»",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF8B0000),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (workshops.size == 1) "این تنها کارگاه موجود در برنامه است. حذف آن باعث می‌شود هیچ اطلاعاتی در برنامه باقی نماند و این عملیات قابل بازگشت نیست." else "توجه: با حذف کارگاه «${targetWs.name}»، کلیه فاکتورها، دریافتی‌ها، تراز مالی و مدل‌های ثبت‌شده برای همیشه حذف خواهند شد.",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "آیا برای ادامه فرآیند حذف این کارگاه مطمئن هستید؟",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        workshopToDelete = targetWs
                        workshopWarningTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("btn_proceed_delete_workshop")
                ) {
                    Text("ادامه جهت تایید و تایپ نام کارگاه", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { workshopWarningTarget = null }) {
                    Text("انصراف")
                }
            }
        )
    }

    // -------------------------------------------------------------------------
    // Sub-dialog 2: Strict Delete Workshop Confirmation
    // (Requires user to type the exact workshop name to delete)
    // -------------------------------------------------------------------------
    if (workshopToDelete != null) {
        val targetWs = workshopToDelete!!
        var typedConfirmation by remember { mutableStateOf("") }
        val isConfirmed = typedConfirmation.trim() == targetWs.name.trim()

        AlertDialog(
            onDismissRequest = { workshopToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "تایید نهایی با تایپ نام کارگاه",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "اطلاعات این کارگاه دیگر قابل بازگرداندن نیست. جهت تایید قطعی حذف، نام کارگاه یعنی «${targetWs.name}» را در کادر زیر تایپ کنید:",
                        fontSize = 11.5.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = typedConfirmation,
                        onValueChange = { typedConfirmation = it },
                        placeholder = { Text(targetWs.name, fontSize = 11.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_confirm_delete_workshop"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isConfirmed) {
                            onDeleteWorkshop(targetWs)
                            workshopToDelete = null
                        }
                    },
                    enabled = isConfirmed,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("btn_confirm_delete_workshop")
                ) {
                    Text("حذف قطعی کارگاه و اطلاعات")
                }
            },
            dismissButton = {
                TextButton(onClick = { workshopToDelete = null }) {
                    Text("انصراف")
                }
            }
        )
    }
}
