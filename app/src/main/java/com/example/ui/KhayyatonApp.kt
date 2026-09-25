package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.CardDisplayMode
import com.example.model.FeedItem
import com.example.ui.components.*
import com.example.ui.dialogs.*
import com.example.ui.screens.GlassyAuthScreen
import com.example.ui.theme.KhayyatonTheme
import kotlinx.coroutines.launch

@Composable
fun KhayyatonApp(viewModel: KhayyatonViewModel) {
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val filteredOrders by viewModel.filteredOrders.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val filteredPayments by viewModel.filteredPayments.collectAsStateWithLifecycle()
    val feedItems by viewModel.feedItems.collectAsStateWithLifecycle()
    val modelPresets by viewModel.modelPresets.collectAsStateWithLifecycle()
    val uniqueCustomers by viewModel.uniqueCustomers.collectAsStateWithLifecycle()

    val currencyUnit by viewModel.currencyUnit.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val calendarType by viewModel.calendarType.collectAsStateWithLifecycle()
    val cardDisplayMode by viewModel.cardDisplayMode.collectAsStateWithLifecycle()
    val cardSortOrder by viewModel.cardSortOrder.collectAsStateWithLifecycle()
    val todayDate by viewModel.todayDate.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCustomerFilter by viewModel.selectedCustomerFilter.collectAsStateWithLifecycle()
    val selectedModelFilter by viewModel.selectedModelFilter.collectAsStateWithLifecycle()
    val selectedDateFilter by viewModel.selectedDateFilter.collectAsStateWithLifecycle()
    val selectedInvoiceFilter by viewModel.selectedInvoiceFilter.collectAsStateWithLifecycle()

    val totalWork by viewModel.totalWork.collectAsStateWithLifecycle()
    val totalReceived by viewModel.totalReceived.collectAsStateWithLifecycle()
    val remainingBalance by viewModel.remainingBalance.collectAsStateWithLifecycle()

    // Dialog states
    val isOrderDialogOpen by viewModel.isOrderDialogOpen.collectAsStateWithLifecycle()
    val isPaymentDialogOpen by viewModel.isPaymentDialogOpen.collectAsStateWithLifecycle()
    val isCalendarDialogOpen by viewModel.isCalendarDialogOpen.collectAsStateWithLifecycle()
    val isInvoiceDialogOpen by viewModel.isInvoiceDialogOpen.collectAsStateWithLifecycle()
    val isAnalysisDialogOpen by viewModel.isAnalysisDialogOpen.collectAsStateWithLifecycle()
    val isCardShareDialogOpen by viewModel.isCardShareDialogOpen.collectAsStateWithLifecycle()
    val isModelPresetsDialogOpen by viewModel.isModelPresetsDialogOpen.collectAsStateWithLifecycle()
    val isUnitRulesDialogOpen by viewModel.isUnitRulesDialogOpen.collectAsStateWithLifecycle()
    val isBackupDialogOpen by viewModel.isBackupDialogOpen.collectAsStateWithLifecycle()
    val isSearchDialogOpen by viewModel.isSearchDialogOpen.collectAsStateWithLifecycle()
    val isAuthDialogOpen by viewModel.isAuthDialogOpen.collectAsStateWithLifecycle()
    val isWorkshopsDialogOpen by viewModel.isWorkshopsDialogOpen.collectAsStateWithLifecycle()
    val isSubscriptionDialogOpen by viewModel.isSubscriptionDialogOpen.collectAsStateWithLifecycle()
    val subscriptionState by viewModel.subscriptionState.collectAsStateWithLifecycle()
    val activeWorkshop by viewModel.activeWorkshop.collectAsStateWithLifecycle()
    val workshops by viewModel.workshops.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val customUsername by viewModel.customUsername.collectAsStateWithLifecycle()
    val deleteTarget by viewModel.deleteTarget.collectAsStateWithLifecycle()

    val unitRules by viewModel.unitRules.collectAsStateWithLifecycle()
    val editingOrder by viewModel.editingOrder.collectAsStateWithLifecycle()
    val editingPayment by viewModel.editingPayment.collectAsStateWithLifecycle()
    val selectedInvoiceOrder by viewModel.selectedInvoiceOrder.collectAsStateWithLifecycle()
    val cardShareOrder by viewModel.cardShareOrder.collectAsStateWithLifecycle()
    val cardSharePayment by viewModel.cardSharePayment.collectAsStateWithLifecycle()
    val deletedModelNames by viewModel.manuallyDeletedModelNames.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val context = androidx.compose.ui.platform.LocalContext.current

    // احراز هویت واقعی Firebase مرجع وضعیت ورود است؛ پرچم محلی نباید ورود را دور بزند.
    if (currentUser == null) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            KhayyatonTheme(themeMode = themeMode) {
                GlassyAuthScreen(
                    isFirstLaunch = true,
                    onAuthSuccess = { user, username, workshopName ->
                        viewModel.onUserLoggedIn(user, username, workshopName)
                    }
                )
            }
        }
        return
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        KhayyatonTheme(themeMode = themeMode) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    SidebarDrawer(
                        currentTheme = themeMode,
                        onSelectTheme = {
                            viewModel.themeMode.value = it
                            viewModel.repository.saveThemeMode(it.name)
                        },
                        currencyUnit = currencyUnit,
                        onSelectCurrency = { viewModel.changeCurrency(it) },
                        cardDisplayMode = cardDisplayMode,
                        onSelectCardDisplayMode = { viewModel.cardDisplayMode.value = it },
                        cardSortOrder = cardSortOrder,
                        onSelectCardSortOrder = { viewModel.cardSortOrder.value = it },
                        currentUser = currentUser,
                        customUsername = customUsername,
                        onUpdateCustomUsername = { viewModel.updateCustomUsername(it) },
                        activeWorkshop = activeWorkshop,
                        onOpenWorkshops = { viewModel.isWorkshopsDialogOpen.value = true },
                        onOpenAuth = { viewModel.isAuthDialogOpen.value = true },
                        onOpenSearch = { viewModel.isSearchDialogOpen.value = true },
                        onOpenAnalysis = { viewModel.isAnalysisDialogOpen.value = true },
                        onOpenModels = { viewModel.isModelPresetsDialogOpen.value = true },
                        onOpenUnitRules = { viewModel.isUnitRulesDialogOpen.value = true },
                        onOpenInvoice = { viewModel.openInvoice(null) },
                        onOpenBackup = { viewModel.isBackupDialogOpen.value = true },
                        subscriptionState = subscriptionState,
                        onOpenSubscription = { viewModel.isSubscriptionDialogOpen.value = true },
                        onClose = { coroutineScope.launch { drawerState.close() } }
                    )
                }
            ) {
                fun checkAccessAndExecute(action: () -> Unit) {
                    if (viewModel.hasPremiumAccess()) {
                        action()
                    } else {
                        val accessMessage = when (subscriptionState.status) {
                            com.example.model.SubscriptionStatus.TRIAL_EXPIRED ->
                                "دوره آزمایشی ۷ روزه خیاطان شما به پایان رسیده است. لطفاً برای ادامه استفاده از امکانات، اشتراک تهیه فرمایید."
                            com.example.model.SubscriptionStatus.EXPIRED ->
                                "اشتراک شما منقضی شده است. لطفاً برای ادامه استفاده از امکانات، اشتراک خود را تمدید یا اشتراک جدید تهیه فرمایید."
                            else ->
                                "برای استفاده از این بخش، حساب کاربری شما اشتراک فعال ندارد. لطفاً وضعیت اشتراک را بررسی کنید."
                        }
                        android.widget.Toast.makeText(
                            context,
                            accessMessage,
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                        viewModel.isSubscriptionDialogOpen.value = true
                    }
                }

                Scaffold(
                    topBar = {
                        TopNavBar(
                            todayDate = todayDate,
                            onOpenNewOrder = { checkAccessAndExecute { viewModel.openNewOrder() } },
                            onOpenNewPayment = { checkAccessAndExecute { viewModel.openNewPayment() } },
                            onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                            onOpenSearch = { viewModel.isSearchDialogOpen.value = true }
                        )
                    },
                    bottomBar = {
                        FooterSummary(
                            totalWork = totalWork,
                            totalReceived = totalReceived,
                            remainingBalance = remainingBalance,
                            currencyUnit = currencyUnit,
                            orderCount = filteredOrders.size,
                            paymentCount = payments.size
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Quick filter bar if filters active
                        QuickFilterBar(
                            searchQuery = searchQuery,
                            selectedCustomer = selectedCustomerFilter,
                            selectedModel = selectedModelFilter,
                            selectedDate = selectedDateFilter,
                            selectedInvoice = selectedInvoiceFilter,
                            onClearSearch = { viewModel.searchQuery.value = "" },
                            onClearCustomer = { viewModel.selectedCustomerFilter.value = null },
                            onClearModel = { viewModel.selectedModelFilter.value = null },
                            onClearDate = { viewModel.selectedDateFilter.value = null },
                            onClearInvoice = { viewModel.selectedInvoiceFilter.value = null },
                            onClearAll = { viewModel.clearFilters() },
                            onOpenFilterDialog = { viewModel.isSearchDialogOpen.value = true }
                        )

                        // Main Scrollable Feed List
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 6.dp, bottom = 24.dp)
                        ) {
                            if (cardDisplayMode == CardDisplayMode.UNIFIED) {
                                // UNIFIED MODE: All cards (orders and payments) displayed chronologically in one feed
                                if (feedItems.isEmpty()) {
                                    item {
                                        EmptyFeedState(onNewOrder = { checkAccessAndExecute { viewModel.openNewOrder() } })
                                    }
                                } else {
                                    items(
                                        items = feedItems,
                                        key = { item ->
                                            when (item) {
                                                is FeedItem.OrderItem -> "order_${item.order.id}"
                                                is FeedItem.PaymentItem -> "pay_${item.payment.id}"
                                            }
                                        }
                                    ) { item ->
                                        when (item) {
                                            is FeedItem.OrderItem -> {
                                                OrderCard(
                                                    order = item.order,
                                                    currencyUnit = currencyUnit,
                                                    onEdit = { checkAccessAndExecute { viewModel.openEditOrder(it) } },
                                                    onDuplicate = { checkAccessAndExecute { viewModel.duplicateOrder(it) } },
                                                    onDelete = { checkAccessAndExecute { viewModel.requestDeleteOrder(it) } },
                                                    onViewInvoice = { checkAccessAndExecute { viewModel.openCardShareOrder(it) } },
                                                    onCustomerClick = { viewModel.selectedCustomerFilter.value = it },
                                                    onModelClick = { viewModel.selectedModelFilter.value = it }
                                                )
                                            }
                                            is FeedItem.PaymentItem -> {
                                                Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)) {
                                                    PaymentCardItem(
                                                        index = item.displayIndex.toInt(),
                                                        payment = item.payment,
                                                        currencyUnit = currencyUnit,
                                                        onEdit = { checkAccessAndExecute { viewModel.openEditPayment(item.payment) } },
                                                        onDelete = { checkAccessAndExecute { viewModel.requestDeletePayment(item.payment) } },
                                                        onShare = { checkAccessAndExecute { viewModel.openCardSharePayment(item.payment) } }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // SEPARATED MODE: Work orders first, followed by Recent Payments section
                                if (filteredOrders.isEmpty()) {
                                    item {
                                        EmptyFeedState(onNewOrder = { checkAccessAndExecute { viewModel.openNewOrder() } })
                                    }
                                } else {
                                    items(
                                        items = filteredOrders,
                                        key = { it.id }
                                    ) { order ->
                                        OrderCard(
                                            order = order,
                                            currencyUnit = currencyUnit,
                                            onEdit = { checkAccessAndExecute { viewModel.openEditOrder(it) } },
                                            onDuplicate = { checkAccessAndExecute { viewModel.duplicateOrder(it) } },
                                            onDelete = { checkAccessAndExecute { viewModel.requestDeleteOrder(it) } },
                                            onViewInvoice = { checkAccessAndExecute { viewModel.openCardShareOrder(it) } },
                                            onCustomerClick = { viewModel.selectedCustomerFilter.value = it },
                                            onModelClick = { viewModel.selectedModelFilter.value = it }
                                        )
                                    }
                                }

                                // Recent Payments Section
                                item {
                                    RecentPayments(
                                        payments = filteredPayments,
                                        currencyUnit = currencyUnit,
                                        onOpenNewPayment = { checkAccessAndExecute { viewModel.openNewPayment() } },
                                        onEditPayment = { checkAccessAndExecute { viewModel.openEditPayment(it) } },
                                        onDeletePayment = { checkAccessAndExecute { viewModel.requestDeletePayment(it) } },
                                        onSharePayment = { checkAccessAndExecute { viewModel.openCardSharePayment(it) } }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Dialogs
            OrderDialog(
                isOpen = isOrderDialogOpen,
                initialOrder = editingOrder,
                modelPresets = modelPresets,
                existingOrders = orders,
                deletedModelNames = deletedModelNames,
                unitRules = unitRules,
                customers = uniqueCustomers,
                currencyUnit = currencyUnit,
                nextOrderNumber = viewModel.getNextOrderNumber(),
                onDismiss = { viewModel.isOrderDialogOpen.value = false },
                onDeleteModel = { viewModel.deletePresetByName(it) },
                onUpdateModelColor = { modelName, newColor -> viewModel.updateModelColor(modelName, newColor) },
                onSave = { order, addToPresets ->
                    viewModel.saveOrder(order, addToPresets)
                }
            )

            PaymentDialog(
                isOpen = isPaymentDialogOpen,
                initialPayment = editingPayment,
                currencyUnit = currencyUnit,
                customers = uniqueCustomers,
                nextPaymentNumber = viewModel.getNextPaymentNumber(),
                remainingBalance = remainingBalance,
                existingPayments = payments,
                onDismiss = { viewModel.isPaymentDialogOpen.value = false },
                onSave = { viewModel.savePayment(it) }
            )

            JalaliCalendarDialog(
                isOpen = isCalendarDialogOpen,
                initialCalendarType = calendarType,
                onDismiss = { viewModel.isCalendarDialogOpen.value = false },
                onDateSelected = { selectedDate ->
                    viewModel.selectedDateFilter.value = selectedDate
                }
            )

            InvoiceDialog(
                isOpen = isInvoiceDialogOpen,
                selectedOrder = selectedInvoiceOrder,
                orders = filteredOrders,
                payments = payments,
                currencyUnit = currencyUnit,
                onDismiss = { viewModel.isInvoiceDialogOpen.value = false }
            )

            AnalysisDialog(
                isOpen = isAnalysisDialogOpen,
                orders = orders,
                payments = payments,
                currencyUnit = currencyUnit,
                onDismiss = { viewModel.isAnalysisDialogOpen.value = false }
            )

            CardShareDialog(
                isOpen = isCardShareDialogOpen,
                order = cardShareOrder,
                payment = cardSharePayment,
                currencyUnit = currencyUnit,
                totalWork = totalWork,
                totalReceived = totalReceived,
                remainingBalance = remainingBalance,
                orderCount = filteredOrders.size,
                paymentCount = payments.size,
                onDismiss = { viewModel.isCardShareDialogOpen.value = false }
            )

            ModelPresetsDialog(
                isOpen = isModelPresetsDialogOpen,
                presets = modelPresets,
                currencyUnit = currencyUnit,
                onDismiss = { viewModel.isModelPresetsDialogOpen.value = false },
                onSavePreset = { viewModel.savePreset(it) },
                onDeletePreset = { viewModel.deletePreset(it) }
            )

            if (isUnitRulesDialogOpen) {
                com.example.ui.dialogs.UnitRulesDialog(
                    rules = unitRules,
                    onSaveRule = { viewModel.saveUnitRule(it) },
                    onDeleteRule = { viewModel.deleteUnitRule(it) },
                    onRestoreDefaults = { viewModel.restoreDefaultUnitRules() },
                    onDismiss = { viewModel.isUnitRulesDialogOpen.value = false }
                )
            }

            BackupDialog(
                isOpen = isBackupDialogOpen,
                orders = orders,
                payments = payments,
                presets = modelPresets,
                currencyUnit = currencyUnit,
                repository = viewModel.repository,
                onOpenFirebaseAuth = { viewModel.isAuthDialogOpen.value = true },
                onDismiss = { viewModel.isBackupDialogOpen.value = false }
            )

            AuthAndCloudSyncDialog(
                isOpen = isAuthDialogOpen,
                currentUser = currentUser,
                customUsername = customUsername,
                onUpdateCustomUsername = { viewModel.updateCustomUsername(it) },
                activeWorkshop = activeWorkshop,
                onUpdateWorkshopName = { name ->
                    val cur = activeWorkshop
                    if (cur != null) {
                        viewModel.renameWorkshop(cur.id, name)
                    } else {
                        viewModel.createWorkshop(name)
                    }
                },
                orders = orders,
                payments = payments,
                presets = modelPresets,
                unitRules = unitRules,
                repository = viewModel.repository,
                onUserChanged = { user ->
                    if (user != null) {
                        viewModel.onUserLoggedIn(user)
                    } else {
                        viewModel.onUserLoggedOut()
                    }
                },
                onDismiss = { viewModel.isAuthDialogOpen.value = false }
            )

            SearchFilterDialog(
                isOpen = isSearchDialogOpen,
                currentQuery = searchQuery,
                currentCustomer = selectedCustomerFilter,
                currentModel = selectedModelFilter,
                currentDate = selectedDateFilter,
                currentInvoice = selectedInvoiceFilter,
                allOrders = orders,
                modelPresets = modelPresets,
                customers = uniqueCustomers,
                currencyUnit = currencyUnit,
                onDismiss = { viewModel.isSearchDialogOpen.value = false },
                onApplyFilters = { query, customer, model, date, invoice ->
                    viewModel.searchQuery.value = query
                    viewModel.selectedCustomerFilter.value = customer
                    viewModel.selectedModelFilter.value = model
                    viewModel.selectedDateFilter.value = date
                    viewModel.selectedInvoiceFilter.value = invoice
                },
                onClearFilters = {
                    viewModel.clearFilters()
                }
            )

            DeleteConfirmDialog(
                deleteTarget = deleteTarget,
                onDismiss = { viewModel.deleteTarget.value = null },
                onConfirm = { viewModel.confirmDelete() }
            )

            WorkshopsDialog(
                isOpen = isWorkshopsDialogOpen,
                workshops = workshops,
                activeWorkshop = activeWorkshop,
                onDismiss = { viewModel.isWorkshopsDialogOpen.value = false },
                onSelectWorkshop = { wsId -> viewModel.selectWorkshop(wsId) },
                onCreateWorkshop = { name -> viewModel.createWorkshop(name) },
                onRenameWorkshop = { id, name -> viewModel.renameWorkshop(id, name) },
                onDeleteWorkshop = { ws -> viewModel.deleteWorkshop(ws) }
            )

            SubscriptionDialog(
                isOpen = isSubscriptionDialogOpen,
                onDismiss = { viewModel.isSubscriptionDialogOpen.value = false }
            )
        }
    }
}

@Composable
private fun EmptyFeedState(onNewOrder: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Chair,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = "هیچ موردی برای نمایش یافت نشد",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "برای شروع، فاکتور کارکرد یا سند دریافتی جدیدی ثبت کنید.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onNewOrder,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("ثبت اولین فاکتور", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SheetOnApp(viewModel: KhayyatonViewModel) {
    KhayyatonApp(viewModel = viewModel)
}
