package com.example.ui.dialogs

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.WorkshopRepository
import com.example.data.firebase.CloudSyncResult
import com.example.data.firebase.FirebaseService
import com.example.data.firebase.FirebaseUserDto
import com.example.model.FurnitureOrder
import com.example.model.ModelPreset
import com.example.model.PaymentRecord
import com.example.model.UnitConversionRule
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Rose600
import com.example.util.PersianUtils
import kotlinx.coroutines.launch

enum class AuthScreenMode {
    SIGN_IN,
    SIGN_UP,
    FORGOT_PASSWORD
}

@Composable
fun AuthAndCloudSyncDialog(
    isOpen: Boolean,
    currentUser: FirebaseUserDto?,
    orders: List<FurnitureOrder>,
    payments: List<PaymentRecord>,
    presets: List<ModelPreset>,
    unitRules: List<UnitConversionRule>,
    repository: WorkshopRepository,
    onUserChanged: (FirebaseUserDto?) -> Unit,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var mode by remember { mutableStateOf(AuthScreenMode.SIGN_IN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    var showCloudRestoreConfirm by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
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
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (currentUser != null) Icons.Default.CloudSync else Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (currentUser != null) "حساب ابری فایربیس (Firebase)" else "ورود و ثبت‌نام با ایمیل",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (currentUser != null) currentUser.email else "همگام‌سازی و ذخیره‌سازی ابری اطلاعات",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Body content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Success or Error banner
                    if (errorMessage != null) {
                        Surface(
                            color = Rose600.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Rose600.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, tint = Rose600)
                                Text(
                                    text = errorMessage!!,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Rose600,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    if (successMessage != null) {
                        Surface(
                            color = Emerald600.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Emerald600.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Emerald600)
                                Text(
                                    text = successMessage!!,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald600,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Logged in UI vs Not logged in UI
                    if (currentUser != null) {
                        // User Profile & Cloud Actions
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = currentUser.email.take(1).uppercase(),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }
                                    Column {
                                        Text("حساب کاربری فایربیس", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = currentUser.email,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("شناسه کاربری (UID):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = currentUser.uid.take(12) + "...",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Text(
                            text = "عملیات همگام‌سازی ابری (Firebase Firestore):",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // 1. Upload to Cloud
                        OutlinedCard(
                            onClick = {
                                if (isSyncing) return@OutlinedCard
                                errorMessage = null
                                successMessage = null
                                isSyncing = true
                                coroutineScope.launch {
                                    val result = FirebaseService.uploadAllToCloud(orders, payments, presets, unitRules)
                                    isSyncing = false
                                    if (result.isSuccess) {
                                        val data = result.getOrNull()!!
                                        successMessage = "همگام‌سازی با موفقیت انجام شد: ${PersianUtils.toPersianDigits(data.ordersCount)} فاکتور و ${PersianUtils.toPersianDigits(data.paymentsCount)} دریافتی در فایربیس ذخیره شدند."
                                    } else {
                                        errorMessage = result.exceptionOrNull()?.message ?: "خطا در آپلود ابری"
                                    }
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Emerald600.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CloudUpload,
                                        contentDescription = null,
                                        tint = Emerald600,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("پشتیبان‌گیری در فضای ابری (Upload to Cloud)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("ذخیره امن تمامی فاکتورها، دریافتی‌ها و مدل‌ها در دیتابیس ابری فایربیس", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (isSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        // 2. Download / Restore from Cloud
                        OutlinedCard(
                            onClick = {
                                if (isSyncing) return@OutlinedCard
                                showCloudRestoreConfirm = true
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF2563EB).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CloudDownload,
                                        contentDescription = null,
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("بازیابی از فضای ابری (Restore from Cloud)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("دریافت اطلاعات ثبت‌شده قبلی از حساب ابری فایربیس و ذخیره در برنامه", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Sign Out Button
                        Button(
                            onClick = {
                                FirebaseService.signOut()
                                onUserChanged(null)
                                successMessage = "با موفقیت از حساب خارج شدید."
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Rose600.copy(alpha = 0.15f),
                                contentColor = Rose600
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(imageVector = Icons.Outlined.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("خروج از حساب کاربری", fontWeight = FontWeight.Bold)
                            }
                        }

                    } else {
                        // Form tabs: Sign In, Sign Up, Forgot Password
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TabButton(
                                    title = "ورود",
                                    isSelected = mode == AuthScreenMode.SIGN_IN,
                                    onClick = {
                                        mode = AuthScreenMode.SIGN_IN
                                        errorMessage = null
                                        successMessage = null
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                TabButton(
                                    title = "ثبت‌نام جدید",
                                    isSelected = mode == AuthScreenMode.SIGN_UP,
                                    onClick = {
                                        mode = AuthScreenMode.SIGN_UP
                                        errorMessage = null
                                        successMessage = null
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                TabButton(
                                    title = "بازیابی رمز",
                                    isSelected = mode == AuthScreenMode.FORGOT_PASSWORD,
                                    onClick = {
                                        mode = AuthScreenMode.FORGOT_PASSWORD
                                        errorMessage = null
                                        successMessage = null
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Email field
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it; errorMessage = null },
                            label = { Text("آدرس ایمیل") },
                            placeholder = { Text("example@gmail.com") },
                            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = if (mode == AuthScreenMode.FORGOT_PASSWORD) ImeAction.Done else ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                                onDone = { focusManager.clearFocus() }
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Password field (only for Sign In / Sign Up)
                        if (mode != AuthScreenMode.FORGOT_PASSWORD) {
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it; errorMessage = null },
                                label = { Text("کلمه عبور") },
                                placeholder = { Text("حداقل ۶ کاراکتر") },
                                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (passwordVisible) "مخفی کردن" else "نمایش"
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = if (mode == AuthScreenMode.SIGN_UP) ImeAction.Next else ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                                    onDone = { focusManager.clearFocus() }
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Confirm Password (only for Sign Up)
                        if (mode == AuthScreenMode.SIGN_UP) {
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it; errorMessage = null },
                                label = { Text("تکرار کلمه عبور") },
                                leadingIcon = { Icon(Icons.Outlined.LockClock, contentDescription = null) },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { focusManager.clearFocus() }
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Submit Button
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                errorMessage = null
                                successMessage = null

                                if (email.isBlank()) {
                                    errorMessage = "لطفاً آدرس ایمیل خود را وارد نمایید."
                                    return@Button
                                }

                                if (mode == AuthScreenMode.FORGOT_PASSWORD) {
                                    isLoading = true
                                    coroutineScope.launch {
                                        val res = FirebaseService.sendPasswordResetEmail(email)
                                        isLoading = false
                                        if (res.isSuccess) {
                                            successMessage = "لینک بازیابی رمز عبور به ایمیل شما ارسال شد."
                                        } else {
                                            errorMessage = res.exceptionOrNull()?.message ?: "خطا در ارسال ایمیل بازیابی"
                                        }
                                    }
                                    return@Button
                                }

                                if (password.length < 6) {
                                    errorMessage = "رمز عبور باید حداقل ۶ کاراکتر باشد."
                                    return@Button
                                }

                                if (mode == AuthScreenMode.SIGN_UP && password != confirmPassword) {
                                    errorMessage = "کلمه عبور و تکرار آن یکسان نیستند."
                                    return@Button
                                }

                                isLoading = true
                                coroutineScope.launch {
                                    val res = if (mode == AuthScreenMode.SIGN_IN) {
                                        FirebaseService.signInWithEmail(email, password)
                                    } else {
                                        FirebaseService.registerWithEmail(email, password)
                                    }
                                    isLoading = false
                                    if (res.isSuccess) {
                                        val user = res.getOrNull()
                                        onUserChanged(user)
                                        successMessage = if (mode == AuthScreenMode.SIGN_IN) "با موفقیت وارد شدید." else "ثبت‌نام با موفقیت انجام شد و وارد شدید."
                                    } else {
                                        errorMessage = res.exceptionOrNull()?.message ?: "عملیات ناموفق بود."
                                    }
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = when (mode) {
                                            AuthScreenMode.SIGN_IN -> Icons.Outlined.Login
                                            AuthScreenMode.SIGN_UP -> Icons.Outlined.PersonAdd
                                            AuthScreenMode.FORGOT_PASSWORD -> Icons.Outlined.Send
                                        },
                                        contentDescription = null
                                    )
                                    Text(
                                        text = when (mode) {
                                            AuthScreenMode.SIGN_IN -> "ورود به حساب"
                                            AuthScreenMode.SIGN_UP -> "ایجاد حساب کاربری جدید"
                                            AuthScreenMode.FORGOT_PASSWORD -> "ارسال لینک بازیابی رمز"
                                        },
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        // Info hint
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "با ایجاد حساب و ورود، اطلاعات فاکتورها و دریافتی‌های شما در بستر امن ابری گوگل فایربیس ذخیره شده و از هر دستگاهی در دسترس خواهد بود.",
                                    fontSize = 10.5.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Cloud Restore Confirmation Dialog
    if (showCloudRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showCloudRestoreConfirm = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, tint = Color(0xFF2563EB))
                    Text("بازیابی اطلاعات از فایربیس", fontWeight = FontWeight.Black, fontSize = 15.sp)
                }
            },
            text = {
                Text(
                    text = "آیا مایلید تمام فاکتورها، دریافتی‌ها و مدل‌های ثبت‌شده در فضای ابری حساب کاربری شما دانلود و به برنامه اضافه شوند؟",
                    fontSize = 13.sp,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCloudRestoreConfirm = false
                        isSyncing = true
                        errorMessage = null
                        successMessage = null
                        coroutineScope.launch {
                            val result = FirebaseService.downloadFromCloud(repository)
                            isSyncing = false
                            if (result.isSuccess) {
                                val data = result.getOrNull()!!
                                successMessage = "بازیابی ابری با موفقیت انجام شد: ${PersianUtils.toPersianDigits(data.ordersCount)} فاکتور و ${PersianUtils.toPersianDigits(data.paymentsCount)} دریافتی دریافت شدند."
                            } else {
                                errorMessage = result.exceptionOrNull()?.message ?: "خطا در بازیابی ابری"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                ) {
                    Text("تأیید و بازیابی ابری", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloudRestoreConfirm = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}

@Composable
private fun TabButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
