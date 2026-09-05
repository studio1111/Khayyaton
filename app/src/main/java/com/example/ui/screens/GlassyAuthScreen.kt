package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.firebase.FirebaseService
import com.example.data.firebase.FirebaseUserDto
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonGreenGlow
import com.example.ui.theme.NeonYellow
import com.example.ui.theme.NeonYellowGlow
import com.example.ui.theme.Rose500
import com.example.ui.theme.Rose600
import kotlinx.coroutines.launch

enum class GlassAuthTab {
    SIGN_IN,
    SIGN_UP,
    FORGOT_PASSWORD
}

@Composable
fun GlassyAuthScreen(
    onAuthSuccess: (FirebaseUserDto, String, String) -> Unit,
    onSkip: (() -> Unit)? = null,
    isFirstLaunch: Boolean = false,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var activeTab by remember { mutableStateOf(if (isFirstLaunch) GlassAuthTab.SIGN_UP else GlassAuthTab.SIGN_IN) }
    var username by remember { mutableStateOf("") }
    var workshopName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF030712),
                        Color(0xFF0B192A),
                        Color(0xFF04131E),
                        Color(0xFF020617)
                    )
                )
            )
    ) {
        // Decorative ambient glow orbs behind glass
        Box(
            modifier = Modifier
                .offset(x = 120.dp, y = (-50).dp)
                .size(240.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(NeonGreen.copy(alpha = 0.18f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )
        Box(
            modifier = Modifier
                .offset(x = (-80).dp, y = 320.dp)
                .size(280.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF3B82F6).copy(alpha = 0.15f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Identity Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(bottom = 18.dp)
            ) {
                // Glass badge icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.15f),
                                    Color.White.copy(alpha = 0.05f)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            Brush.linearGradient(
                                colors = listOf(NeonGreenGlow.copy(alpha = 0.5f), Color.Transparent)
                            ),
                            RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.sheeton_logo_icon),
                        contentDescription = "لوگوی SheetOn",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "SheetOn",
                    color = NeonYellowGlow,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                Text(
                    text = if (isFirstLaunch) "خوش آمدید! ثبت نام و ورود اولیه" else "ورود به حساب ابری SheetOn",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "همگام‌سازی و بازیابی خودکار اطلاعات با فایربیس",
                    color = NeonGreenGlow.copy(alpha = 0.9f),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Glass Container Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.10f),
                                Color.White.copy(alpha = 0.04f)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.35f),
                                Color.White.copy(alpha = 0.08f)
                            )
                        ),
                        RoundedCornerShape(26.dp)
                    )
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(26.dp),
                        ambientColor = NeonGreen.copy(alpha = 0.2f),
                        spotColor = Color.Black
                    )
                    .padding(20.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Glass Segmented Tab Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        GlassTabButton(
                            title = "ثبت‌نام",
                            icon = Icons.Outlined.PersonAdd,
                            isSelected = activeTab == GlassAuthTab.SIGN_UP,
                            onClick = {
                                activeTab = GlassAuthTab.SIGN_UP
                                errorMessage = null
                                successMessage = null
                            },
                            modifier = Modifier.weight(1f)
                        )
                        GlassTabButton(
                            title = "ورود",
                            icon = Icons.AutoMirrored.Outlined.Login,
                            isSelected = activeTab == GlassAuthTab.SIGN_IN,
                            onClick = {
                                activeTab = GlassAuthTab.SIGN_IN
                                errorMessage = null
                                successMessage = null
                            },
                            modifier = Modifier.weight(1f)
                        )
                        GlassTabButton(
                            title = "بازیابی رمز",
                            icon = Icons.Outlined.Key,
                            isSelected = activeTab == GlassAuthTab.FORGOT_PASSWORD,
                            onClick = {
                                activeTab = GlassAuthTab.FORGOT_PASSWORD
                                errorMessage = null
                                successMessage = null
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Alerts
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn(tween(250)),
                        exit = fadeOut(tween(200))
                    ) {
                        errorMessage?.let { msg ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Rose600.copy(alpha = 0.18f))
                                    .border(1.dp, Rose500.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = Rose500, modifier = Modifier.size(20.dp))
                                Text(msg, color = Color(0xFFFFB4BA), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = successMessage != null,
                        enter = fadeIn(tween(250)),
                        exit = fadeOut(tween(200))
                    ) {
                        successMessage?.let { msg ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Emerald600.copy(alpha = 0.18f))
                                    .border(1.dp, Emerald500.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Emerald500, modifier = Modifier.size(20.dp))
                                Text(msg, color = Color(0xFFA7F3D0), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Username field (shown on Sign Up and Sign In to ensure username is known)
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it; errorMessage = null },
                        label = { Text("نام کاربری") },
                        placeholder = { Text("مثلاً: علی رضایی") },
                        leadingIcon = {
                            Icon(Icons.Outlined.AccountCircle, contentDescription = null, tint = NeonGreen)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = glassTextFieldColors(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Workshop name field (shown on Sign Up)
                    if (activeTab == GlassAuthTab.SIGN_UP) {
                        OutlinedTextField(
                            value = workshopName,
                            onValueChange = { workshopName = it; errorMessage = null },
                            label = { Text("نام کارگاه") },
                            placeholder = { Text("مثلاً: کارگاه مبل آریا") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Storefront, contentDescription = null, tint = NeonGreen)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = glassTextFieldColors(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; errorMessage = null },
                        label = { Text("آدرس ایمیل") },
                        placeholder = { Text("example@gmail.com") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Email, contentDescription = null, tint = NeonGreen)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = glassTextFieldColors(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = if (activeTab == GlassAuthTab.FORGOT_PASSWORD) ImeAction.Done else ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) },
                            onDone = { focusManager.clearFocus() }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Password Field
                    if (activeTab != GlassAuthTab.FORGOT_PASSWORD) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; errorMessage = null },
                            label = { Text("کلمه عبور") },
                            placeholder = { Text("حداقل ۶ کاراکتر") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Lock, contentDescription = null, tint = NeonGreen)
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (passwordVisible) "مخفی کردن" else "نمایش",
                                        tint = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = glassTextFieldColors(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = if (activeTab == GlassAuthTab.SIGN_UP) ImeAction.Next else ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                                onDone = { focusManager.clearFocus() }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Confirm Password Field (only on Sign Up)
                    if (activeTab == GlassAuthTab.SIGN_UP) {
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it; errorMessage = null },
                            label = { Text("تکرار کلمه عبور") },
                            placeholder = { Text("مجدداً کلمه عبور را وارد کنید") },
                            leadingIcon = {
                                Icon(Icons.Outlined.LockReset, contentDescription = null, tint = NeonGreen)
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = glassTextFieldColors(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Auto-sync reassurance notice (no manual button needed)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CloudDone,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "ذخیره‌سازی و بازیابی اطلاعات به صورت خودکار و نامحسوس پس از ورود با ایمیل انجام می‌شود.",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            lineHeight = 17.sp
                        )
                    }

                    // Action Button (ورود / ثبت‌نام / بازیابی)
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            errorMessage = null
                            successMessage = null

                            if (username.isBlank() && activeTab != GlassAuthTab.FORGOT_PASSWORD) {
                                errorMessage = "لطفاً نام کاربری خود را وارد نمایید."
                                return@Button
                            }

                            if (email.isBlank()) {
                                errorMessage = "لطفاً آدرس ایمیل خود را وارد نمایید."
                                return@Button
                            }

                            if (activeTab == GlassAuthTab.FORGOT_PASSWORD) {
                                isLoading = true
                                coroutineScope.launch {
                                    val res = FirebaseService.sendPasswordResetEmail(email)
                                    isLoading = false
                                    if (res.isSuccess) {
                                        successMessage = "لینک بازیابی رمز عبور به ایمیل شما ارسال گردید."
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

                            if (activeTab == GlassAuthTab.SIGN_UP && password != confirmPassword) {
                                errorMessage = "کلمه عبور و تکرار آن یکسان نیستند."
                                return@Button
                            }

                            isLoading = true
                            coroutineScope.launch {
                                val res = if (activeTab == GlassAuthTab.SIGN_IN) {
                                    FirebaseService.signInWithEmail(email, password)
                                } else {
                                    FirebaseService.registerWithEmail(email, password)
                                }
                                isLoading = false
                                if (res.isSuccess) {
                                    val user = res.getOrNull()!!
                                    val finalUser = if (username.isNotBlank() && user.displayName.isNullOrBlank()) {
                                        user.copy(displayName = username.trim())
                                    } else {
                                        user
                                    }
                                    successMessage = if (activeTab == GlassAuthTab.SIGN_IN) "با موفقیت وارد شدید." else "ثبت‌نام با موفقیت انجام شد."
                                    onAuthSuccess(finalUser, username.trim(), workshopName.trim())
                                } else {
                                    errorMessage = res.exceptionOrNull()?.message ?: "عملیات ناموفق بود."
                                }
                            }
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen,
                            contentColor = Color(0xFF021B0F),
                            disabledContainerColor = NeonGreen.copy(alpha = 0.4f),
                            disabledContentColor = Color.White.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color(0xFF021B0F),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = when (activeTab) {
                                        GlassAuthTab.SIGN_IN -> Icons.AutoMirrored.Outlined.Login
                                        GlassAuthTab.SIGN_UP -> Icons.Outlined.PersonAdd
                                        GlassAuthTab.FORGOT_PASSWORD -> Icons.Outlined.Send
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = when (activeTab) {
                                        GlassAuthTab.SIGN_IN -> "ورود به حساب و بازیابی خودکار"
                                        GlassAuthTab.SIGN_UP -> "ثبت‌نام، ذخیره و شروع به کار"
                                        GlassAuthTab.FORGOT_PASSWORD -> "ارسال لینک بازیابی کلمه عبور"
                                    },
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.5.sp
                                )
                            }
                        }
                    }

                    // ورود آفلاین برای بار اول حذف شد تا حتما با ثبت ایمیل و نام کاربری وارد شوند
                }
            }
        }
    }
}

@Composable
private fun GlassTabButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundModifier = if (isSelected) {
        Modifier.background(
            Brush.linearGradient(
                colors = listOf(NeonGreen.copy(alpha = 0.25f), NeonGreen.copy(alpha = 0.10f))
            )
        )
    } else {
        Modifier.background(Color.Transparent)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(backgroundModifier)
            .border(
                if (isSelected) 1.dp else 0.dp,
                if (isSelected) NeonGreen.copy(alpha = 0.6f) else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) NeonGreen else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun glassTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedContainerColor = Color.Black.copy(alpha = 0.35f),
    unfocusedContainerColor = Color.Black.copy(alpha = 0.25f),
    focusedBorderColor = NeonGreen,
    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
    focusedLabelColor = NeonGreen,
    unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
    focusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
    unfocusedPlaceholderColor = Color.White.copy(alpha = 0.3f),
    cursorColor = NeonGreen
)
