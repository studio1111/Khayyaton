package com.example.ui

import com.example.BuildConfig
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.WorkManager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.WorkshopRepository
import com.example.data.sync.SyncWorkScheduler
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
        list.find { it.id == id }
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
    val isCloudVpnNoticeOpen = MutableStateFlow(repository.shouldShowCloudVpnWarning())
    val subscriptionState: StateFlow<UserSubscription> = SubscriptionManager.subscriptionState
    val currentUser = MutableStateFlow<FirebaseUserDto?>(null)
    val customUsername = MutableStateFlow<String>("")
    val isDrawerOpen = MutableStateFlow(false)
    val isAutoSyncing = MutableStateFlow(false)
    val autoSyncStatusMessage = MutableStateFlow<String?>(null)
    private var autoSyncStateJob: kotlinx.coroutines.Job? = null

    fun hasPremiumAccess(): Boolean {
        return SubscriptionManager.hasPremiumAccess()
    }

    init {
        // Room is the UI source of truth. Restored workshops automatically
        // select a valid active workshop without creating placeholders.
        viewModelScope.launch {
            workshops.collect { list ->
                val current = activeWorkshopId.value
                if (list.isEmpty()) {
                    if (current != 0L) {
                        activeWorkshopId.value = 0L
                        repository.saveActiveWorkshopId(0L)
                    }
                } else if (current <= 0L || list.none { it.id == current }) {
                    val saved = repository.getSavedActiveWorkshopId()
                    val next = if (saved > 0L && list.any { it.id == saved }) saved else list.first().id
                    activeWorkshopId.value = next
                    repository.saveActiveWorkshopId(next)
                }
            }
        }

        viewModelScope.launch {
            try {
                val user = FirebaseService.getCurrentUser()
                var switchedAccount = false

            if (user != null) {
                val context = repository.getApplicationContext()
                val previousUid = try {
                    repository.ensureLocalAccount(user.uid)
                } catch (e: com.example.data.sync.PendingAccountSwitchException) {
                    FirebaseService.signOut()
                    currentUser.value = null
                    autoSyncStatusMessage.value = e.message
                    return@launch
                }
                if (previousUid != null) {
                    switchedAccount = true
                    if (context != null) SyncWorkScheduler.cancel(context, previousUid)
                    customUsername.value = ""
                    activeWorkshopId.value = 0L
                    repository.saveActiveWorkshopId(0L)
                    clearFilters()
                }
            }

            if (!switchedAccount) {
                val savedUser = repository.getCustomUsername()
                if (savedUser.isNotBlank()) customUsername.value = savedUser
            }

            val savedWsId = repository.getSavedActiveWorkshopId()
            val localWorkshops = repository.getAllWorkshopsSync()

            activeWorkshopId.value =
                if (savedWsId > 0L && localWorkshops.any { it.id == savedWsId }) {
                    savedWsId
                } else {
                    localWorkshops.firstOrNull()?.id ?: 0L
                }
            repository.saveActiveWorkshopId(activeWorkshopId.value)

            repository.insertDefaultUnitRulesIfEmpty()

            repository.getSavedThemeMode()?.let {
                runCatching { AppThemeMode.valueOf(it) }.getOrNull()?.let { mode -> themeMode.value = mode }
            }
            runCatching { CardDisplayMode.valueOf(repository.getSavedCardDisplayMode()) }
                .getOrNull()?.let { cardDisplayMode.value = it }
            runCatching { CardSortOrder.valueOf(repository.getSavedCardSortOrder()) }
                .getOrNull()?.let { cardSortOrder.value = it }
            runCatching { CalendarType.valueOf(repository.getSavedCalendarType()) }
                .getOrNull()?.let { calendarType.value = it }
            currencyUnit.value = repository.getSavedCurrencyUnit()

            currentUser.value = user
            SubscriptionManager.syncSubscriptionWithFirebase()

            if (user != null) {
                if (customUsername.value.isBlank() && !user.displayName.isNullOrBlank()) {
                    customUsername.value = user.displayName
                    repository.saveCustomUsername(user.displayName)
                }

                performAutoSync(user)
            }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    android.util.Log.e("KhayyatonViewModel", "Startup initialization failed", e)
                }
                if (currentUser.value != null) {
                    autoSyncStatusMessage.value =
                        e.message?.takeIf { it.isNotBlank() }
                            ?: "اطلاعات محلی برنامه کامل بارگذاری نشد."
                }
            }
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
                "پرداخت‌کننده" to payment.customerName,
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
                syncId = java.util.UUID.randomUUID().toString(),
                workshopId = activeWorkshopId.value,
                orderNumber = newNum,
                invoiceNumber = newNum.toString(),
                dateJalali = todayDate.value,
                dateGregorian = PersianUtils.getTodayGregorianString(),
                createdAt = System.currentTimeMillis()
            )
            repository.saveOrder(duplicated)
            triggerAutoUpload()
        }
    }

    fun saveOrder(order: FurnitureOrder, addToPresets: Boolean) {
        viewModelScope.launch {
            val wsId = activeWorkshopId.value
            if (wsId <= 0L) return@launch

            // New records always belong to the currently selected workshop.
            // Existing records retain their own workshop unless it was invalid.
            val orderToSave = when {
                order.id == 0L -> order.copy(workshopId = wsId)
                order.workshopId <= 0L -> order.copy(workshopId = wsId)
                else -> order
            }
            repository.saveOrder(orderToSave)
            val trimmedName = orderToSave.modelName.trim()
            if (trimmedName.isNotBlank()) {
                if (orderToSave.colorCode.isNotBlank()) {
                    repository.updateOrdersColorForModel(trimmedName, orderToSave.colorCode, wsId)
                }
                val existingPreset = modelPresets.value.find {
                    it.workshopId == wsId &&
                        it.name.trim().equals(trimmedName, ignoreCase = true)
                }
                if (existingPreset != null) {
                    if (addToPresets) {
                        repository.savePreset(
                            existingPreset.copy(
                                workshopId = wsId,
                                defaultPricePerSet = orderToSave.pricePerSet,
                                defaultUnitsPerSet = orderToSave.unitsPerSet,
                                colorCode = orderToSave.colorCode.ifBlank { existingPreset.colorCode }
                            )
                        )
                    }
                } else {
                    // New model! Always add to default model presets
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
                    manuallyDeletedModelNames.value = manuallyDeletedModelNames.value - trimmedName.lowercase()
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
            if (wsId <= 0L) return@launch
            val paymentToSave = if (payment.id == 0L || payment.workshopId <= 0L) payment.copy(workshopId = wsId) else payment
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
            val presetToSave = PresetWorkshopPolicy.forSave(preset, wsId)
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
        if (id <= 0L || workshops.value.none { it.id == id }) return
        activeWorkshopId.value = id
        repository.saveActiveWorkshopId(id)
        clearFilters()
    }

    fun createWorkshop(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            val existing = repository.getAllWorkshopsSync()
            val found = existing.firstOrNull { it.name.trim().equals(trimmed, ignoreCase = true) }
            if (found != null) {
                activeWorkshopId.value = found.id
                repository.saveActiveWorkshopId(found.id)
                clearFilters()
                return@launch
            }
            val newId = repository.saveWorkshop(com.example.model.Workshop(name = trimmed))
            activeWorkshopId.value = newId
            repository.saveActiveWorkshopId(newId)
            clearFilters()
            triggerAutoUpload()
        }
    }

    fun renameWorkshop(id: Long, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            val existing = repository.getAllWorkshopsSync()
            val isDuplicate = existing.any { it.id != id && it.name.trim().equals(trimmed, ignoreCase = true) }
            if (isDuplicate) return@launch
            val existingWs = repository.getWorkshopById(id) ?: return@launch
            repository.saveWorkshop(existingWs.copy(name = trimmed))
            triggerAutoUpload()
        }
    }

    fun deleteWorkshop(workshop: com.example.model.Workshop) {
        viewModelScope.launch {
            repository.deleteWorkshopAndAllData(workshop.id)
            repository.deduplicateWorkshops()
            val remaining = repository.getAllWorkshopsSync()
            val nextActive = remaining.firstOrNull()?.id ?: 0L

            // Deleting the final/main workshop is intentional. Never silently
            // recreate a workshop after deletion.
            activeWorkshopId.value = nextActive
            repository.saveActiveWorkshopId(nextActive)
            clearFilters()
            triggerAutoUpload()
        }
    }

    fun saveUnitRule(rule: com.example.model.UnitConversionRule) {
        viewModelScope.launch {
            repository.saveUnitRule(rule)
            triggerAutoUpload()
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
            triggerAutoUpload()
        }
    }

    fun dismissCloudVpnNotice(dontShowAgain: Boolean) {
        isCloudVpnNoticeOpen.value = false
        if (dontShowAgain) {
            repository.setDontShowCloudVpnWarning(true)
        }
    }

    fun changeTheme(newMode: AppThemeMode) {
        themeMode.value = newMode
        repository.saveThemeMode(newMode.name)
    }

    fun changeCurrency(newUnit: String) {
        if (newUnit == currencyUnit.value) return
        currencyUnit.value = newUnit
        repository.saveCurrencyUnit(newUnit)
    }

    fun changeCalendarType(newType: CalendarType) {
        calendarType.value = newType
        repository.saveCalendarType(newType.name)
    }

    fun changeCardDisplayMode(mode: CardDisplayMode) {
        cardDisplayMode.value = mode
        repository.saveCardDisplayMode(mode.name)
    }

    fun changeCardSortOrder(order: CardSortOrder) {
        cardSortOrder.value = order
        repository.saveCardSortOrder(order.name)
    }

    fun clearFilters() {
        searchQuery.value = ""
        selectedCustomerFilter.value = null
        selectedModelFilter.value = null
        selectedDateFilter.value = null
        selectedInvoiceFilter.value = null
    }

    fun refreshAfterLocalRestore() {
        viewModelScope.launch {
            val restored = repository.getAllWorkshopsSync()
            val saved = repository.getSavedActiveWorkshopId()
            val next = when {
                saved > 0L && restored.any { it.id == saved } -> saved
                restored.isNotEmpty() -> restored.first().id
                else -> 0L
            }
            activeWorkshopId.value = next
            repository.saveActiveWorkshopId(next)
            repository.insertDefaultUnitRulesIfEmpty()
            clearFilters()
        }
    }

    /**
     * Ensures the currently authenticated account has no pending local changes
     * before FirebaseAuth is allowed to replace it with another account.
     * This closes the gap where direct sign-in could otherwise switch Firebase
     * users before the previous account's local changes were synchronized.
     */
    suspend fun prepareForAccountSwitch(): Result<Unit> {
        val context = repository.getApplicationContext()
            ?: return Result.failure(Exception("محیط برنامه برای همگام‌سازی آماده نیست."))
        val currentUid = FirebaseService.currentUser()?.uid ?: return Result.success(Unit)

        if (!repository.hasPendingSyncWork()) return Result.success(Unit)

        if (!hasUsableNetwork(context)) {
            return Result.failure(
                Exception("اطلاعات حساب فعلی هنوز همگام نشده است. لطفاً ابتدا اینترنت را وصل کنید و دوباره تلاش کنید.")
            )
        }

        autoSyncStatusMessage.value = "در حال ذخیره و همگام‌سازی اطلاعات حساب فعلی..." 
        val result = FirebaseService.syncAccount(repository)
        return if (result.isSuccess && !repository.hasPendingSyncWork()) {
            autoSyncStatusMessage.value = null
            Result.success(Unit)
        } else {
            Result.failure(
                result.exceptionOrNull()
                    ?: Exception("همگام‌سازی اطلاعات حساب فعلی کامل نشد. برای جلوگیری از از دست رفتن اطلاعات، ورود به حساب دیگر انجام نشد.")
            )
        }
    }

    /**
     * Account login starts exactly one account-scoped WorkManager sync.
     * Room remains the only source used by the UI.
     */
    fun onUserLoggedIn(user: FirebaseUserDto, preferredUsername: String? = null, workshopName: String? = null) {
        // Publish the authenticated Firebase user immediately so the UI leaves
        // the auth screen before account-scoped background work starts.
        currentUser.value = user
        // syncSubscriptionWithFirebase() already refreshes Bazaar purchases after
        // owner/trial resolution. Calling the Bazaar refresh a second time here
        // creates duplicate callbacks and unnecessary concurrent state updates.
        SubscriptionManager.syncSubscriptionWithFirebase()

        viewModelScope.launch {
            try {
                val context = repository.getApplicationContext()
                    ?: throw IllegalStateException("محیط برنامه برای ورود آماده نیست.")

                val previousUid = try {
                    repository.ensureLocalAccount(user.uid)
                } catch (e: com.example.data.sync.PendingAccountSwitchException) {
                    FirebaseService.signOut()
                    currentUser.value = null
                    autoSyncStatusMessage.value = e.message
                    return@launch
                }

                if (previousUid != null) {
                    SyncWorkScheduler.cancel(context, previousUid)
                    repository.saveActiveWorkshopId(0L)
                    activeWorkshopId.value = 0L
                    customUsername.value = ""
                    clearFilters()
                }

                // Every Firebase account gets the four mandatory base unit rules.
                repository.insertDefaultUnitRulesIfEmpty()

                val chosenName = preferredUsername?.takeIf { it.isNotBlank() }
                    ?: customUsername.value.takeIf { it.isNotBlank() }
                    ?: user.displayName?.takeIf { it.isNotBlank() }

                if (!chosenName.isNullOrBlank()) {
                    updateCustomUsername(chosenName)
                }

                // A workshop is created only when a name was explicitly supplied.
                if (!workshopName.isNullOrBlank()) {
                    val trimmed = workshopName.trim()
                    val existing = repository.getAllWorkshopsSync()
                    val found = existing.firstOrNull { it.name.trim().equals(trimmed, ignoreCase = true) }
                    if (found != null) {
                        activeWorkshopId.value = found.id
                        repository.saveActiveWorkshopId(found.id)
                    } else if (existing.isEmpty()) {
                        createWorkshop(trimmed)
                    } else if (
                        existing.size == 1 &&
                        existing.first().name.trim() in setOf("کارگاه اصلی", "کارگاه") &&
                        repository.getAllOrdersSync().isEmpty() &&
                        repository.getAllPaymentsSync().isEmpty()
                    ) {
                        renameWorkshop(existing.first().id, trimmed)
                    }
                }

                autoSyncStatusMessage.value = null
                performAutoSync(user)
            } catch (e: Exception) {
                // Login itself succeeded. A Room/Sync failure must not crash the
                // app or log the user out. Surface a Persian status instead.
                if (currentUser.value?.uid == user.uid) {
                    autoSyncStatusMessage.value =
                        e.message?.takeIf { it.isNotBlank() }
                            ?: "ورود انجام شد، اما آماده‌سازی اطلاعات حساب کامل نشد."
                }
                if (com.example.BuildConfig.DEBUG) {
                    android.util.Log.e("KhayyatonViewModel", "Post-login initialization failed", e)
                }
            }
        }
    }

    fun onUserLoggedOut() {
        val context = repository.getApplicationContext() ?: return
        val uid = FirebaseService.currentUser()?.uid ?: repository.getLocalAccountUid()

        viewModelScope.launch {
            // Never discard unsynced local changes on logout.
            if (repository.hasPendingSyncWork()) {
                if (!hasUsableNetwork(context)) {
                    autoSyncStatusMessage.value =
                        "برای خروج از حساب، ابتدا اینترنت را وصل کنید تا اطلاعات ذخیره و همگام شود."
                    return@launch
                }

                autoSyncStatusMessage.value = "در حال ذخیره و همگام‌سازی اطلاعات قبل از خروج..."
                val syncResult = FirebaseService.syncAccount(repository)
                if (syncResult.isFailure || repository.hasPendingSyncWork()) {
                    autoSyncStatusMessage.value =
                        syncResult.exceptionOrNull()?.message
                            ?: "همگام‌سازی کامل نشد. خروج لغو شد تا اطلاعات شما از بین نرود."
                    return@launch
                }
            }

            uid?.let { SyncWorkScheduler.cancel(context, it) }
            FirebaseService.signOut()
            currentUser.value = null
            autoSyncStatusMessage.value = null
            SubscriptionManager.clearCachedUserState()

            repository.clearAccountLocalState()
            repository.saveActiveWorkshopId(0L)
            activeWorkshopId.value = 0L
            customUsername.value = ""
            clearFilters()
        }
    }

    private fun hasUsableNetwork(context: Context): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private suspend fun performAutoSync(user: FirebaseUserDto) {
        if (FirebaseService.currentUser()?.uid != user.uid) return
        val context = repository.getApplicationContext() ?: return

        autoSyncStateJob?.cancel()
        autoSyncStateJob = viewModelScope.launch {
            SyncWorkScheduler.enqueue(context, user.uid)
            val workManager = WorkManager.getInstance(context.applicationContext)
            workManager.getWorkInfosForUniqueWorkFlow(SyncWorkScheduler.workName(user.uid))
                .collect { infos ->
                    val result = com.example.data.sync.SyncWorkUiPolicy.resolve(
                        infos.map {
                            com.example.data.sync.SyncWorkUiPolicy.Snapshot(
                                state = it.state,
                                errorMessage = it.outputData.getString("errorMessage")
                            )
                        }
                    )

                    when (result.state) {
                        com.example.data.sync.SyncWorkUiPolicy.State.RUNNING -> {
                            isAutoSyncing.value = true
                            autoSyncStatusMessage.value = "همگام‌سازی اطلاعات در حال انجام است..."
                        }
                        com.example.data.sync.SyncWorkUiPolicy.State.QUEUED -> {
                            isAutoSyncing.value = true
                            autoSyncStatusMessage.value = "همگام‌سازی اطلاعات در صف اجرا قرار گرفت..."
                        }
                        com.example.data.sync.SyncWorkUiPolicy.State.SUCCESS -> {
                            isAutoSyncing.value = false
                            autoSyncStatusMessage.value = "همگام‌سازی اطلاعات انجام شد."
                        }
                        com.example.data.sync.SyncWorkUiPolicy.State.FAILED -> {
                            isAutoSyncing.value = false
                            autoSyncStatusMessage.value =
                                result.errorMessage ?: "همگام‌سازی اطلاعات ناموفق بود."
                        }
                        com.example.data.sync.SyncWorkUiPolicy.State.CANCELLED -> {
                            isAutoSyncing.value = false
                            autoSyncStatusMessage.value = "همگام‌سازی اطلاعات لغو شد."
                        }
                        com.example.data.sync.SyncWorkUiPolicy.State.IDLE -> Unit
                    }
                }
        }
    }
    fun triggerAutoUpload() {
        val uid = FirebaseService.currentUser()?.uid ?: currentUser.value?.uid ?: return
        val context = repository.getApplicationContext() ?: return
        SyncWorkScheduler.enqueue(context, uid)
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
