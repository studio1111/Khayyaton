package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.WorkshopRepository
import com.example.model.AppThemeMode
import com.example.model.CalendarType
import com.example.model.CardDisplayMode
import com.example.model.CardSortOrder
import com.example.model.DeleteTarget
import com.example.model.FeedItem
import com.example.model.FurnitureOrder
import com.example.model.ModelPreset
import com.example.model.PaymentRecord
import com.example.data.firebase.FirebaseService
import com.example.data.firebase.FirebaseUserDto
import com.example.data.subscription.SubscriptionManager
import com.example.model.UserSubscription
import com.example.util.PersianUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class KhayyatonViewModel(val repository: WorkshopRepository) : ViewModel() {

    val workshops: StateFlow<List<com.example.model.Workshop>> = repository.workshops
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeWorkshopId = MutableStateFlow(0L)

    val activeWorkshop: StateFlow<com.example.model.Workshop?> = combine(workshops, activeWorkshopId) { list, id ->
        list.find { it.id == id } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val orders: StateFlow<List<FurnitureOrder>> = combine(repository.orders, activeWorkshopId) { all, currentWsId ->
        all.filter { it.workshopId == currentWsId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payments: StateFlow<List<PaymentRecord>> = combine(repository.payments, activeWorkshopId) { all, currentWsId ->
        all.filter { it.workshopId == currentWsId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val modelPresets: StateFlow<List<ModelPreset>> = combine(repository.modelPresets, activeWorkshopId) { all, currentWsId ->
        all.filter { it.workshopId == currentWsId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unitRules: StateFlow<List<com.example.model.UnitConversionRule>> = repository.unitRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currencyUnit = MutableStateFlow("تومان")
    val themeMode = MutableStateFlow(AppThemeMode.NEON_GLASS)
    val calendarType = MutableStateFlow(CalendarType.JALALI)
    val cardDisplayMode = MutableStateFlow(CardDisplayMode.UNIFIED)
    val cardSortOrder = MutableStateFlow(CardSortOrder.NEWEST_BOTTOM)

    val todayDate: StateFlow<String> = calendarType.map { calType ->
        PersianUtils.getTodayDateByCalendar(calType)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PersianUtils.getTodayJalaliString())

    // Search & Filters
    val searchQuery = MutableStateFlow("")
    val selectedCustomerFilter = MutableStateFlow<String?>(null)
    val selectedModelFilter = MutableStateFlow<String?>(null)
    val selectedDateFilter = MutableStateFlow<String?>(null)
    val selectedInvoiceFilter = MutableStateFlow<String?>(null)

    // Dialog & UI Selection States
    val editingOrder = MutableStateFlow<FurnitureOrder?>(null)
    val editingPayment = MutableStateFlow<PaymentRecord?>(null)
    val selectedInvoiceOrder = MutableStateFlow<FurnitureOrder?>(null)
    val cardShareOrder = MutableStateFlow<FurnitureOrder?>(null)
    val cardSharePayment = MutableStateFlow<PaymentRecord?>(null)
    val deleteTarget = MutableStateFlow<DeleteTarget?>(null)

    val isOrderDialogOpen = MutableStateFlow(false)
    val isPaymentDialogOpen = MutableStateFlow(false)
    val isCalendarDialogOpen = MutableStateFlow(false)
    val isInvoiceDialogOpen = MutableStateFlow(false)
    val isAnalysisDialogOpen = MutableStateFlow(false)
    val isCardShareDialogOpen = MutableStateFlow(false)
    val isModelPresetsDialogOpen = MutableStateFlow(false)
    val isUnitRulesDialogOpen = MutableStateFlow(false)
    val isBackupDialogOpen = MutableStateFlow(false)
    val isSearchDialogOpen = MutableStateFlow(false)
    val isAuthDialogOpen = MutableStateFlow(false)
    val isWorkshopsDialogOpen = MutableStateFlow(false)
    val isSubscriptionDialogOpen = MutableStateFlow(false)
    val subscriptionState: StateFlow<UserSubscription> = SubscriptionManager.subscriptionState
    val currentUser = MutableStateFlow<FirebaseUserDto?>(null)
    val customUsername = MutableStateFlow<String>("")
    val isDrawerOpen = MutableStateFlow(false)
    val isAutoSyncing = MutableStateFlow(false)
    val autoSyncStatusMessage = MutableStateFlow<String?>(null)
    val isSessionReady = MutableStateFlow(false)
    private var sessionJob: kotlinx.coroutines.Job? = null

    fun hasPremiumAccess(): Boolean {
        return SubscriptionManager.hasPremiumAccess()
    }

    init {
        viewModelScope.launch {
            repository.getSavedThemeMode()?.let { saved ->
                runCatching { AppThemeMode.valueOf(saved) }.getOrNull()?.let { themeMode.value = it }
            }
            val user = FirebaseService.getCurrentUser()
            if (user == null) {
                repository.setSessionUid(null)
                repository.resetAllData()
                currentUser.value = null
                activeWorkshopId.value = 0L
                isSessionReady.value = false
            } else {
                switchUserSession(user, null, null)
            }

            launch {
                SubscriptionManager.subscriptionState.collect { sub ->
                    val userNow = currentUser.value
                    if (sub.hasAccess && userNow != null && isSessionReady.value && !repository.getCloudSyncInitialized()) {
                        performAutoSync(userNow, shouldDownload = true)
                    }
                }
            }
        }
    }

    private fun switchUserSession(user: FirebaseUserDto, preferredUsername: String?, workshopName: String?) {
        sessionJob?.cancel()
        sessionJob = viewModelScope.launch {
            isSessionReady.value = false
            val changed = repository.getSessionUid() != user.uid
            if (changed) {
                repository.resetAllData()
                repository.setSessionUid(user.uid)
                repository.saveCloudSyncInitialized(false)
                activeWorkshopId.value = 0L
            } else {
                repository.setSessionUid(user.uid)
            }
            currentUser.value = user
            FirebaseService.ensureUserProfile(user)
            // Wait for subscription state before reading protected cloud collections.
            runCatching {
                SubscriptionManager.syncSubscriptionWithFirebaseNow()
            }.onFailure {
                autoSyncStatusMessage.value = "وضعیت اشتراک از حساب ابری دریافت نشد."
            }
            val chosen = preferredUsername?.takeIf { it.isNotBlank() }
                ?: repository.getCustomUsername().takeIf { it.isNotBlank() }
                ?: user.displayName?.takeIf { it.isNotBlank() }
            if (!chosen.isNullOrBlank()) updateCustomUsername(chosen)

            if (workshopName?.trim()?.isNotBlank() == true && repository.getAllWorkshopsSync().isEmpty()) {
                val id = repository.saveWorkshop(com.example.model.Workshop(name = workshopName.trim()))
                activeWorkshopId.value = id
                repository.saveActiveWorkshopId(id)
            } else {
                val saved = repository.getSavedActiveWorkshopId()
                val all = repository.getAllWorkshopsSync()
                activeWorkshopId.value = if (saved > 0 && all.any { it.id == saved }) saved else all.firstOrNull()?.id ?: 0L
            }

            if (changed) {
                val restored = FirebaseService.downloadFromCloud(repository)
                if (restored.isSuccess) {
                    val restoredWorkshops = restored.getOrNull()?.workshopsCount ?: 0
                    val restoredAllWorkshops = repository.getAllWorkshopsSync()
                    val restoredSavedId = repository.getSavedActiveWorkshopId()
                    val restoredActiveId = if (
                        restoredSavedId > 0L && restoredAllWorkshops.any { it.id == restoredSavedId }
                    ) {
                        restoredSavedId
                    } else {
                        restoredAllWorkshops.firstOrNull()?.id ?: 0L
                    }
                    activeWorkshopId.value = restoredActiveId
                    if (restoredActiveId > 0L) {
                        repository.saveActiveWorkshopId(restoredActiveId)
                    }
                    if (restoredWorkshops > 0) {
                        repository.saveCloudSyncInitialized(true)
                    } else if (SubscriptionManager.hasPremiumAccess()) {
                        performAutoSync(user, shouldDownload = false)
                    }
                }
            } else if (!repository.getCloudSyncInitialized()) {
                performAutoSync(user, shouldDownload = repository.getAllWorkshopsSync().isEmpty())
            }
            isSessionReady.value = true
        }
    }

    fun updateCustomUsername(name: String) {
        val trimmed = name.trim()
        customUsername.value = trimmed
        repository.saveCustomUsername(trimmed)
    }

    // Filtered orders stream
    private data class FilterParams(
        val query: String,
        val customer: String?,
        val model: String?,
        val date: String?,
        val invoice: String?
    )

    private val filterParams = combine(
        searchQuery,
        selectedCustomerFilter,
        selectedModelFilter,
        selectedDateFilter,
        selectedInvoiceFilter
    ) { query, customer, model, date, invoice ->
        FilterParams(query, customer, model, date, invoice)
    }

    val filteredOrders: StateFlow<List<FurnitureOrder>> = combine(
        orders,
        filterParams,
        cardSortOrder
    ) { orderList, filters, sortOrder ->
        val q = PersianUtils.toEnglishDigits(filters.query.trim()).lowercase()
        val invFilterEng = filters.invoice?.let { PersianUtils.toEnglishDigits(it.trim()) }

        val filtered = orderList.filter { order ->
            val matchCustomer = filters.customer.isNullOrBlank() || order.customerName.trim() == filters.customer.trim()
            val matchModel = filters.model.isNullOrBlank() || order.modelName.trim() == filters.model.trim()
            val matchDate = filters.date.isNullOrBlank() || order.dateJalali.trim() == filters.date.trim()
            val matchInvoice = invFilterEng.isNullOrBlank() ||
                    PersianUtils.toEnglishDigits(order.invoiceNumber).contains(invFilterEng) ||
                    order.orderNumber.toString().contains(invFilterEng)

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

        if (sortOrder == CardSortOrder.NEWEST_BOTTOM) {
            filtered.sortedBy { it.createdAt }
        } else {
            filtered.sortedByDescending { it.createdAt }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredPayments: StateFlow<List<PaymentRecord>> = combine(
        payments,
        filterParams,
        cardSortOrder
    ) { paymentList, filters, sortOrder ->
        val q = PersianUtils.toEnglishDigits(filters.query.trim()).lowercase()

        val filtered = paymentList.filter { pay ->
            val matchCustomer = filters.customer.isNullOrBlank() || pay.customerName.trim() == filters.customer.trim()
            val matchDate = filters.date.isNullOrBlank() || pay.dateJalali.trim() == filters.date.trim()
            val matchQuery = if (q.isBlank()) {
                true
            } else {
                val cust = pay.customerName.lowercase()
                val desc = pay.description.lowercase()
                val ref = pay.referenceNo.lowercase()
                val dateJ = PersianUtils.toEnglishDigits(pay.dateJalali)
                val amt = pay.amount.toString()
                cust.contains(q) || desc.contains(q) || ref.contains(q) || dateJ.contains(q) || amt.contains(q)
            }
            matchCustomer && matchDate && matchQuery
        }

        if (sortOrder == CardSortOrder.NEWEST_BOTTOM) {
            filtered.sortedBy { it.createdAt }
        } else {
            filtered.sortedByDescending { it.createdAt }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Combined stream of feed items for unified feed display
    val feedItems: StateFlow<List<FeedItem>> = combine(
        filteredOrders,
        filteredPayments,
        cardDisplayMode,
        cardSortOrder
    ) { ords, pays, mode, sortOrder ->
        if (mode == CardDisplayMode.UNIFIED) {
            val list = mutableListOf<FeedItem>()
            ords.forEachIndexed { i, o -> list.add(FeedItem.OrderItem(o, (i + 1).toLong())) }
            pays.forEachIndexed { i, p -> list.add(FeedItem.PaymentItem(p, (i + 1).toLong())) }
            if (sortOrder == CardSortOrder.NEWEST_BOTTOM) {
                list.sortedBy { it.timestamp }
            } else {
                list.sortedByDescending { it.timestamp }
            }
        } else {
            val list = mutableListOf<FeedItem>()
            ords.forEachIndexed { i, o -> list.add(FeedItem.OrderItem(o, (i + 1).toLong())) }
            pays.forEachIndexed { i, p -> list.add(FeedItem.PaymentItem(p, (i + 1).toLong())) }
            list
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Unique customers list for autocomplete & filters
    val uniqueCustomers: StateFlow<List<String>> = combine(orders, payments) { ords, pays ->
        val set = linkedSetOf<String>()
        ords.forEach { if (it.customerName.isNotBlank()) set.add(it.customerName) }
        pays.forEach { if (it.customerName.isNotBlank()) set.add(it.customerName) }
        set.toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Financial totals based on current filters
    val totalWork: StateFlow<Long> = filteredOrders.map { list ->
        list.sumOf { it.calculatedTotal }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val totalReceived: StateFlow<Long> = combine(payments, selectedCustomerFilter) { payList, custFilter ->
        if (custFilter.isNullOrBlank()) {
            payList.sumOf { it.amount }
        } else {
            payList.filter { it.customerName == custFilter }.sumOf { it.amount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val remainingBalance: StateFlow<Long> = combine(totalWork, totalReceived) { work, received ->
        work - received
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun getNextOrderNumber(): Long {
        val current = orders.value
        return if (current.isEmpty()) 1L else (current.maxOf { it.orderNumber } + 1)
    }

    fun getNextPaymentNumber(): Long {
        val current = payments.value
        return if (current.isEmpty()) 1L else (current.maxOf { it.paymentNumber } + 1)
    }

    // Actions
    fun openNewOrder() {
        editingOrder.value = null
        isOrderDialogOpen.value = true
    }

    fun openEditOrder(order: FurnitureOrder) {
        editingOrder.value = order
        isOrderDialogOpen.value = true
    }

    fun openNewPayment() {
        editingPayment.value = null
        isPaymentDialogOpen.value = true
    }

    fun openEditPayment(payment: PaymentRecord) {
        editingPayment.value = payment
        isPaymentDialogOpen.value = true
    }

    fun openInvoice(order: FurnitureOrder?) {
        if (order != null) {
            openCardShareOrder(order)
        } else {
            selectedInvoiceOrder.value = null
            isInvoiceDialogOpen.value = true
        }
    }

    fun openCardShareOrder(order: FurnitureOrder) {
        cardShareOrder.value = order
        cardSharePayment.value = null
        isCardShareDialogOpen.value = true
    }

    fun openCardSharePayment(payment: PaymentRecord) {
        cardShareOrder.value = null
        cardSharePayment.value = payment
        isCardShareDialogOpen.value = true
    }

    fun requestDeleteOrder(order: FurnitureOrder) {
        deleteTarget.value = DeleteTarget(
            type = "order",
            id = order.id,
            title = "فاکتور #${PersianUtils.toPersianDigits(order.invoiceNumber)}",
            message = "آیا از حذف کامل این فاکتور کارکرد مطمئن هستید؟ این عملیات قابل بازگشت نیست.",
            details = listOf(
                "مدل مبل" to order.modelName,
                "مشتری" to order.customerName.ifBlank { "عمومی" },
                "مبلغ کل" to PersianUtils.formatCurrency(order.calculatedTotal, currencyUnit.value),
                "تاریخ ثبت" to PersianUtils.toPersianDigits(order.dateJalali)
            )
        )
    }

    fun requestDeletePayment(payment: PaymentRecord) {
        deleteTarget.value = DeleteTarget(
            type = "payment",
            id = payment.id,
            title = "سند دریافتی #${PersianUtils.toPersianDigits(payment.paymentNumber)}",
            message = "آیا از حذف این رکورد پرداخت اطمینان دارید؟",
            details = listOf(
                "مبلغ واریزی" to PersianUtils.formatCurrency(payment.amount, currencyUnit.value),
                "مشتری" to payment.customerName,
                "تاریخ" to PersianUtils.toPersianDigits(payment.dateJalali),
                "شرح" to payment.description.ifBlank { "واریزی وجه" }
            )
        )
    }

    fun confirmDelete() {
        val target = deleteTarget.value ?: return
        viewModelScope.launch {
            if (target.type == "order") {
                repository.deleteOrderById(target.id)
            } else if (target.type == "payment") {
                repository.deletePaymentById(target.id)
            }
            deleteTarget.value = null
            triggerAutoUpload()
        }
    }

    fun duplicateOrder(order: FurnitureOrder) {
        viewModelScope.launch {
            val newNum = getNextOrderNumber()
            val duplicated = order.copy(
                id = 0L,
                workshopId = activeWorkshopId.value,
                orderNumber = newNum,
                invoiceNumber = newNum.toString(),
                dateJalali = todayDate.value,
                dateGregorian = PersianUtils.getTodayGregorianString(),
                createdAt = System.currentTimeMillis()
            )
            repository.saveOrder(duplicated)
        }
    }

    fun saveOrder(order: FurnitureOrder, addToPresets: Boolean) {
        viewModelScope.launch {
            val wsId = activeWorkshopId.value
            val orderToSave = if (order.workshopId <= 0L) order.copy(workshopId = wsId) else order
            repository.saveOrder(orderToSave)
            if (addToPresets && orderToSave.modelName.isNotBlank()) {
                val trimmedName = orderToSave.modelName.trim()
                if (orderToSave.colorCode.isNotBlank()) {
                    repository.updateOrdersColorForModel(trimmedName, orderToSave.colorCode, wsId)
                }
                val existingPreset = modelPresets.value.find { it.name.trim().equals(trimmedName, ignoreCase = true) }
                if (existingPreset != null) {
                    repository.savePreset(
                        existingPreset.copy(
                            workshopId = wsId,
                            defaultPricePerSet = orderToSave.pricePerSet,
                            defaultUnitsPerSet = orderToSave.unitsPerSet,
                            colorCode = orderToSave.colorCode.ifBlank { existingPreset.colorCode }
                        )
                    )
                } else {
                    repository.savePreset(
                        ModelPreset(
                            workshopId = wsId,
                            name = trimmedName,
                            defaultPricePerSet = orderToSave.pricePerSet,
                            defaultUnitsPerSet = orderToSave.unitsPerSet,
                            colorCode = orderToSave.colorCode,
                            description = if (orderToSave.fabricName.isNotBlank()) "پارچه ${orderToSave.fabricName}" else ""
                        )
                    )
                }
            }
            isOrderDialogOpen.value = false
            editingOrder.value = null
            triggerAutoUpload()
        }
    }

    fun updateModelColor(modelName: String, newColor: String) {
        viewModelScope.launch {
            val wsId = activeWorkshopId.value
            repository.updateOrdersColorForModel(modelName, newColor, wsId)
            triggerAutoUpload()
        }
    }

    fun savePayment(payment: PaymentRecord) {
        viewModelScope.launch {
            val wsId = activeWorkshopId.value
            val paymentToSave = if (payment.workshopId <= 0L) payment.copy(workshopId = wsId) else payment
            repository.savePayment(paymentToSave)
            isPaymentDialogOpen.value = false
            editingPayment.value = null
            triggerAutoUpload()
        }
    }

    val manuallyDeletedModelNames = MutableStateFlow<Set<String>>(emptySet())

    fun savePreset(preset: ModelPreset) {
        viewModelScope.launch {
            val wsId = activeWorkshopId.value
            val presetToSave = if (preset.workshopId <= 0L) preset.copy(workshopId = wsId) else preset
            repository.savePreset(presetToSave)
            manuallyDeletedModelNames.value = manuallyDeletedModelNames.value - preset.name.trim().lowercase()
            // When updating a preset's color, also update existing orders for this model in this workshop
            if (preset.name.isNotBlank() && preset.colorCode.isNotBlank()) {
                repository.updateOrdersColorForModel(preset.name.trim(), preset.colorCode, wsId)
            }
            triggerAutoUpload()
        }
    }

    fun deletePreset(preset: ModelPreset) {
        viewModelScope.launch {
            repository.deletePreset(preset)
            manuallyDeletedModelNames.value = manuallyDeletedModelNames.value + preset.name.trim().lowercase()
            triggerAutoUpload()
        }
    }

    fun deletePresetByName(name: String) {
        viewModelScope.launch {
            val wsId = activeWorkshopId.value
            repository.deletePresetByNameAndWorkshop(name, wsId)
            manuallyDeletedModelNames.value = manuallyDeletedModelNames.value + name.trim().lowercase()
            triggerAutoUpload()
        }
    }

    fun selectWorkshop(id: Long) {
        activeWorkshopId.value = id
        repository.saveActiveWorkshopId(id)
        clearFilters()
    }

    fun createWorkshop(name: String) {
        viewModelScope.launch {
            val newId = repository.saveWorkshop(com.example.model.Workshop(name = name))
            activeWorkshopId.value = newId
            repository.saveActiveWorkshopId(newId)
            clearFilters()
            triggerAutoUpload()
        }
    }

    fun renameWorkshop(id: Long, newName: String) {
        viewModelScope.launch {
            val existing = repository.getWorkshopById(id) ?: return@launch
            repository.saveWorkshop(existing.copy(name = newName))
            triggerAutoUpload()
        }
    }

    fun deleteWorkshop(workshop: com.example.model.Workshop) {
        viewModelScope.launch {
            repository.deleteWorkshopAndAllData(workshop.id)
            val remaining = repository.getAllWorkshopsSync()
            val nextActive = remaining.firstOrNull()?.id ?: 0L
            activeWorkshopId.value = nextActive
            if (nextActive > 0L) {
                repository.saveActiveWorkshopId(nextActive)
            }
            clearFilters()
            triggerAutoUpload()
        }
    }

    fun saveUnitRule(rule: com.example.model.UnitConversionRule) {
        viewModelScope.launch {
            repository.saveUnitRule(rule)
        }
    }

    fun deleteUnitRule(rule: com.example.model.UnitConversionRule) {
        viewModelScope.launch {
            repository.deleteUnitRule(rule)
            triggerAutoUpload()
        }
    }

    fun restoreDefaultUnitRules() {
        viewModelScope.launch {
            repository.restoreDefaultUnitRules()
        }
    }

    fun changeCurrency(newUnit: String) {
        if (newUnit == currencyUnit.value) return
        currencyUnit.value = newUnit
    }

    fun clearFilters() {
        searchQuery.value = ""
        selectedCustomerFilter.value = null
        selectedModelFilter.value = null
        selectedDateFilter.value = null
        selectedInvoiceFilter.value = null
    }

    /**
     * Automatic sync and restore when a user enters email/signs in or registers.
     * Restores existing cloud data if available, then syncs local state to Firebase.
     */
    fun onUserLoggedIn(user: FirebaseUserDto, preferredUsername: String? = null, workshopName: String? = null) {
        switchUserSession(user, preferredUsername, workshopName)
    }

    fun onUserLoggedOut() {
        sessionJob?.cancel()
        isSessionReady.value = false
        FirebaseService.signOut()
        SubscriptionManager.clearForSignedOutUser()
        viewModelScope.launch {
            repository.resetAllData()
            repository.setSessionUid(null)
        }
        currentUser.value = null
        activeWorkshopId.value = 0L
        customUsername.value = ""
        autoSyncStatusMessage.value = null
    }

    private suspend fun performAutoSync(user: FirebaseUserDto, shouldDownload: Boolean = false) {
        isAutoSyncing.value = true
        try {
            if (shouldDownload) {
                val downloadRes = FirebaseService.downloadFromCloud(repository)
                if (downloadRes.isSuccess) {
                    autoSyncStatusMessage.value = "اطلاعات با حساب ابری همگام‌سازی و بازیابی شد."
                }
            }

            val currentOrders = repository.getAllOrdersSync()
            val currentPayments = repository.getAllPaymentsSync()
            val currentPresets = repository.getAllPresetsSync()
            val currentUnitRules = unitRules.value
            val currentWorkshops = repository.getAllWorkshopsSync()

            if (
                currentWorkshops.isNotEmpty() ||
                currentOrders.isNotEmpty() ||
                currentPayments.isNotEmpty() ||
                currentPresets.isNotEmpty() ||
                currentUnitRules.isNotEmpty()
            ) {
                FirebaseService.uploadAllToCloud(
                    orders = currentOrders,
                    payments = currentPayments,
                    presets = currentPresets,
                    unitRules = currentUnitRules,
                    workshops = currentWorkshops
                )
            }
            repository.saveCloudSyncInitialized(true)
            autoSyncStatusMessage.value = "اطلاعات با حساب ابری همگام‌سازی شد."
        } catch (e: Exception) {
            autoSyncStatusMessage.value = "همگام‌سازی ابری در دسترس نیست."
        } finally {
            isAutoSyncing.value = false
        }
    }

    fun triggerAutoUpload() {
        if (!isSessionReady.value || !hasPremiumAccess()) return
        val user = currentUser.value ?: return
        viewModelScope.launch {
            try {
                FirebaseService.uploadAllToCloud(
                    orders = repository.getAllOrdersSync(),
                    payments = repository.getAllPaymentsSync(),
                    presets = repository.getAllPresetsSync(),
                    unitRules = unitRules.value,
                    workshops = repository.getAllWorkshopsSync()
                )
            } catch (_: Exception) {}
        }
    }
}

class KhayyatonViewModelFactory(private val repository: WorkshopRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(KhayyatonViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return KhayyatonViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

typealias SheetOnViewModel = KhayyatonViewModel
typealias SheetOnViewModelFactory = KhayyatonViewModelFactory
