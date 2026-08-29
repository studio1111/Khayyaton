package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppThemeMode
import com.example.model.CardDisplayMode
import com.example.model.CardSortOrder
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SidebarDrawer(
    currentTheme: AppThemeMode,
    onSelectTheme: (AppThemeMode) -> Unit,
    currencyUnit: String,
    onSelectCurrency: (String) -> Unit,
    cardDisplayMode: CardDisplayMode,
    onSelectCardDisplayMode: (CardDisplayMode) -> Unit,
    cardSortOrder: CardSortOrder,
    onSelectCardSortOrder: (CardSortOrder) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenAnalysis: () -> Unit,
    onOpenModels: () -> Unit,
    onOpenUnitRules: () -> Unit,
    onOpenInvoice: () -> Unit,
    onOpenBackup: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isDisplayModeExpanded by remember { mutableStateOf(false) }
    var isSortOrderExpanded by remember { mutableStateOf(false) }
    var isCurrencyMenuExpanded by remember { mutableStateOf(false) }
    var isThemeMenuExpanded by remember { mutableStateOf(false) }

    val currencyList = listOf(
        "تومان",
        "ریال",
        "دلار ($)",
        "دینار (عراق)",
        "افغانی (؋)",
        "یورو (€)",
        "درهم (امارات)",
        "میلیون تومان"
    )

    ModalDrawerSheet(
        modifier = modifier.width(320.dp),
        drawerShape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 24.dp, bottomEnd = 24.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // Header with Large Distinctive App Name & Close Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SheetOnLogo(fontSize = 24.sp)
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "بستن منو",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Scrollable Content with all items organized consistently as elegant drawer cards
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Section 1: Five Primary Action Items (5 آیتم اول)
                DrawerItem(
                    icon = Icons.Outlined.Analytics,
                    title = "اطلاعات و آنالیز",
                    onClick = { onOpenAnalysis(); onClose() }
                )
                DrawerItem(
                    icon = Icons.Outlined.ReceiptLong,
                    title = "صورت حساب و کارکرد کلی",
                    onClick = { onOpenInvoice(); onClose() }
                )
                DrawerItem(
                    icon = Icons.Outlined.Search,
                    title = "جستجو و فیلتر پیشرفته",
                    onClick = { onOpenSearch(); onClose() }
                )
                DrawerItem(
                    icon = Icons.Outlined.Tune,
                    title = "مدیریت مدل‌های پیش‌فرض",
                    onClick = { onOpenModels(); onClose() }
                )
                DrawerItem(
                    icon = Icons.Outlined.Calculate,
                    title = "قوانین تبدیل واحد قطعات",
                    onClick = { onOpenUnitRules(); onClose() }
                )
                DrawerItem(
                    icon = Icons.Outlined.CloudSync,
                    title = "پشتیبان‌گیری و خروجی پیشرفته",
                    onClick = { onOpenBackup(); onClose() }
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Section 2: Expandable Settings Drawers (طراحی هماهنگ به صورت کشویی برای بقیه آیتم‌ها)
                
                // 1. نحوه نمایش کارت‌ها (Card Display Mode)
                ExpandableDrawerCard(
                    icon = Icons.Outlined.ViewStream,
                    title = "نحوه نمایش کارت‌ها",
                    badge = if (cardDisplayMode == CardDisplayMode.UNIFIED) "یکپارچه" else "تفکیک‌شده",
                    isExpanded = isDisplayModeExpanded,
                    onToggle = { isDisplayModeExpanded = !isDisplayModeExpanded }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OptionSelectButton(
                            title = "یکپارچه (پشت سر هم)",
                            isSelected = cardDisplayMode == CardDisplayMode.UNIFIED,
                            onClick = { onSelectCardDisplayMode(CardDisplayMode.UNIFIED) }
                        )
                        OptionSelectButton(
                            title = "تفکیک کارکرد و دریافتی",
                            isSelected = cardDisplayMode == CardDisplayMode.SEPARATED,
                            onClick = { onSelectCardDisplayMode(CardDisplayMode.SEPARATED) }
                        )
                    }
                }

                // 2. ترتیب چیدمان کارت جدید (Card Sorting Order)
                ExpandableDrawerCard(
                    icon = Icons.Outlined.Sort,
                    title = "ترتیب چیدمان کارت جدید",
                    badge = if (cardSortOrder == CardSortOrder.NEWEST_BOTTOM) "در انتها" else "در ابتدا",
                    isExpanded = isSortOrderExpanded,
                    onToggle = { isSortOrderExpanded = !isSortOrderExpanded }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OptionSelectButton(
                            title = "در انتهای لیست (صعودی)",
                            isSelected = cardSortOrder == CardSortOrder.NEWEST_BOTTOM,
                            onClick = { onSelectCardSortOrder(CardSortOrder.NEWEST_BOTTOM) }
                        )
                        OptionSelectButton(
                            title = "در ابتدای لیست (نزولی)",
                            isSelected = cardSortOrder == CardSortOrder.NEWEST_TOP,
                            onClick = { onSelectCardSortOrder(CardSortOrder.NEWEST_TOP) }
                        )
                    }
                }

                // 3. واحد پول (Currency Unit)
                ExpandableDrawerCard(
                    icon = Icons.Outlined.MonetizationOn,
                    title = "واحد پول",
                    badge = currencyUnit,
                    isExpanded = isCurrencyMenuExpanded,
                    onToggle = { isCurrencyMenuExpanded = !isCurrencyMenuExpanded }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        currencyList.forEach { unit ->
                            val isSelected = unit == currencyUnit
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectCurrency(unit)
                                        isCurrencyMenuExpanded = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = unit,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. پوسته و تم برنامه (Theme & Styling)
                ExpandableDrawerCard(
                    icon = Icons.Outlined.Palette,
                    title = "پوسته و تم برنامه",
                    badge = currentTheme.titleFa.substringBefore(" ("),
                    isExpanded = isThemeMenuExpanded,
                    onToggle = { isThemeMenuExpanded = !isThemeMenuExpanded }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AppThemeMode.entries.forEach { mode ->
                            val isSelected = mode == currentTheme
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectTheme(mode)
                                        isThemeMenuExpanded = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val dotColor = when (mode) {
                                            AppThemeMode.NEON_GLASS -> NeonGreen
                                            AppThemeMode.LIGHT -> Color(0xFFF4F6F9)
                                            AppThemeMode.DARK -> Slate950
                                            AppThemeMode.GLASS_PURPLE -> PurpleBg
                                            AppThemeMode.GLASS_BLUE -> NavyBg
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(13.dp)
                                                .clip(CircleShape)
                                                .background(dotColor)
                                                .border(1.dp, if (mode == AppThemeMode.NEON_GLASS) NeonYellow else Color.Gray.copy(alpha = 0.5f), CircleShape)
                                        )
                                        Text(
                                            text = mode.titleFa,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
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

/**
 * Standard Uniform Drawer Item for direct click actions (Matches First 5 Items)
 */
@Composable
private fun DrawerItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Expandable Drawer Card with matching appearance to DrawerItem,
 * featuring an expandable content drawer, status badge, and animated chevron.
 */
@Composable
private fun ExpandableDrawerCard(
    icon: ImageVector,
    title: String,
    badge: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        border = if (isExpanded) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "بستن" else "باز کردن",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                content()
            }
        }
    }
}

/**
 * Clean Option Toggle Button inside an expanded drawer
 */
@Composable
private fun OptionSelectButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.padding(vertical = 7.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                fontSize = 10.5.sp,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
