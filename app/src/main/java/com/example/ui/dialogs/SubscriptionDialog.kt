package com.example.ui.dialogs

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.subscription.SubscriptionManager
import com.example.model.SubscriptionPlan
import com.example.model.SubscriptionStatus
import com.example.ui.theme.Amber600
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Rose600
import com.example.util.PersianUtils

@Composable
fun SubscriptionDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val subscription by SubscriptionManager.subscriptionState.collectAsStateWithLifecycle()
    val isLoading by SubscriptionManager.isLoading.collectAsStateWithLifecycle()
    val operationMessage by SubscriptionManager.operationMessage.collectAsStateWithLifecycle()

    LaunchedEffect(operationMessage) {
        operationMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Emerald600, Color(0xFF10B981))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WorkspacePremium,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "اشتراک برنامه و نسخه ویژه",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "دسترسی نامحدود به تمامی بخش‌ها و کارگاه‌ها",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Status Card
                    SubscriptionStatusBanner(subscription = subscription)

                    // نمایش وضعیت Trial واقعی حساب کاربری
                    if (subscription.status == SubscriptionStatus.TRIAL_ACTIVE) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Amber600.copy(alpha = 0.10f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                Amber600.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Outlined.Timer, contentDescription = null, tint = Amber600)
                                Text(
                                    text = "نسخه آزمایشی ۷ روزه خیاطان",
                                    fontSize = 11.sp,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    // Description text
                    Text(
                        text = "اشتراک‌های برنامه (پرداخت امن درون‌برنامه‌ای):",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Subscription Plans
                    SubscriptionPlan.PLANS.forEach { plan ->
                        PlanCard(
                            plan = plan,
                            isCurrentPlan = subscription.activeProductId == plan.productId && subscription.status == SubscriptionStatus.SUBSCRIBED,
                            isLoading = isLoading,
                            onPurchase = {
                                if (activity != null) {
                                    SubscriptionManager.purchaseSubscription(activity, plan) { result ->
                                        if (result.isSuccess) {
                                            Toast.makeText(context, "اشتراک با موفقیت فعال شد!", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "امکان ارتباط با صفحه پرداخت وجود ندارد.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Restore Purchases Button
                    OutlinedButton(
                        onClick = {
                            SubscriptionManager.restorePurchases { result ->
                                if (result.isSuccess) {
                                    val count = result.getOrDefault(0)
                                    if (count > 0) {
                                        Toast.makeText(context, "اشتراک شما بازیابی شد.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(imageVector = Icons.Outlined.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = "بازیابی خریدهای قبلی از کافه‌بازار",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Security & Support Note
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "کلیه پرداخت‌ها به صورت کاملاً امن از طریق درگاه کافه‌بازار انجام می‌شوند و اشتراک شما به حساب کاربری ابری شما متصل خواهد ماند.",
                                fontSize = 11.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubscriptionStatusBanner(subscription: com.example.model.UserSubscription) {
    val (bgColor, borderColor, icon, title, subtitle) = when (subscription.status) {
        SubscriptionStatus.ADMIN_GRANTED -> {
            Tuple5(
                Emerald600.copy(alpha = 0.12f),
                Emerald600.copy(alpha = 0.45f),
                Icons.Default.VerifiedUser,
                "دسترسی مالک فعال است",
                "این حساب با مجوز مالک Firebase بدون محدودیت اشتراک فعال است"
            )
        }
        SubscriptionStatus.SUBSCRIBED -> {
            Tuple5(
                Emerald600.copy(alpha = 0.12f),
                Emerald600.copy(alpha = 0.45f),
                Icons.Default.CheckCircle,
                "اشتراک ویژه فعال است",
                "دسترسی کامل به برنامه فعال است (${PersianUtils.toPersianDigits(subscription.remainingDays)} روز باقی مانده)"
            )
        }
        SubscriptionStatus.TRIAL_ACTIVE -> {
            Tuple5(
                Amber600.copy(alpha = 0.12f),
                Amber600.copy(alpha = 0.45f),
                Icons.Outlined.Timer,
                "نسخه آزمایشی ۷ روزه خیاطان",
                "دوره آزمایشی ۷ روزه از زمان ساخت حساب Firebase محاسبه می‌شود؛ ${PersianUtils.toPersianDigits(subscription.remainingDays)} روز و ${PersianUtils.toPersianDigits(subscription.remainingHours)} ساعت باقی مانده است"
            )
        }
        SubscriptionStatus.TRIAL_EXPIRED -> {
            Tuple5(
                Rose600.copy(alpha = 0.12f),
                Rose600.copy(alpha = 0.45f),
                Icons.Outlined.Warning,
                "نسخه آزمایشی ۷ روزه خیاطان به پایان رسیده است",
                "برای ثبت فاکتور و ادامه استفاده از برنامه، لطفاً یکی از بسته‌های اشتراک را فعال کنید."
            )
        }
        SubscriptionStatus.EXPIRED -> {
            Tuple5(
                Rose600.copy(alpha = 0.12f),
                Rose600.copy(alpha = 0.45f),
                Icons.Outlined.ErrorOutline,
                "اشتراک ویژه شما منقضی شده است",
                "جهت تمدید دسترسی، لطفاً یکی از اشتراک‌های زیر را تمدید فرمایید."
            )
        }
        SubscriptionStatus.UNKNOWN -> {
            Tuple5(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                Icons.Outlined.Info,
                "بررسی وضعیت اشتراک",
                "در حال همگام‌سازی وضعیت اشتراک با سرور..."
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (subscription.status == SubscriptionStatus.SUBSCRIBED) Emerald600 else if (subscription.status == SubscriptionStatus.TRIAL_ACTIVE) Amber600 else Rose600,
                modifier = Modifier.size(28.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PlanCard(
    plan: SubscriptionPlan,
    isCurrentPlan: Boolean,
    isLoading: Boolean,
    onPurchase: () -> Unit
) {
    val isHighlighted = plan.tagFa != null

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isHighlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    Text(
                        text = plan.titleFa,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (plan.tagFa != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Emerald600,
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Text(
                                text = plan.tagFa,
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = PersianUtils.toPersianDigits(plan.priceFormatted),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Button and duration info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "دسترسی نامحدود برای ${PersianUtils.toPersianDigits(plan.durationDays)} روز",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isCurrentPlan) {
                    FilledTonalButton(
                        onClick = {},
                        enabled = false,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("طرح فعال شما", fontSize = 11.sp)
                    }
                } else {
                    Button(
                        onClick = onPurchase,
                        enabled = !isLoading,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isHighlighted) Emerald600 else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = "خرید و فعال‌سازی",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private data class Tuple5<A, B, C, D, E>(
    val a: A,
    val b: B,
    val c: C,
    val d: D,
    val e: E
)
