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

    val activeWorkshopId = MutableStateFlow(1L)

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

    val searchQuery = MutableStateFlow("")
    val selectedCustomerFilter = MutableStateFlow<String?>(null)
    val selectedModelFilter = MutableStateFlow<String?>(null)
    val selectedDateFilter = MutableStateFlow<String?>(null)
    val selectedInvoiceFilter = MutableStateFlow<Boolean?>(null)

    val filteredOrders: StateFlow<List<FurnitureOrder>> = combine(
        orders, searchQuery, selectedCustomerFilter, selectedModelFilter, selectedDateFilter, selectedInvoiceFilter
    ) { currentOrders, query, customer, model, date, invoice ->
        currentOrders.filter { order ->
            (query.isBlank() || order.customerName.contains(query, ignoreCase = true) || order.modelName.contains(query, ignoreCase = true) || order.orderNumber.toString().contains(query)) &&
            (customer == null || order.customerName == customer) &&
            (model == null || order.modelName == model) &&
            (date == null || order.date == date) &&
            (invoice == null || order.invoiceIssued == invoice)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredPayments: StateFlow<List<PaymentRecord>> = combine(
        payments, searchQuery, selectedCustomerFilter, selectedDateFilter
    ) { currentPayments, query, customer, date ->
        currentPayments.filter { payment ->
            (query.isBlank() || payment.customerName.contains(query, ignoreCase = true) || payment.paymentNumber.toString().contains(query)) &&
            (customer == null || payment.customerName == customer) &&
            (date == null || payment.date == date)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val feedItems: StateFlow<List<FeedItem>> = combine(filteredOrders, filteredPayments, cardSortOrder) { currentOrders, currentPayments, sortOrder ->
        val combined = buildList {
            addAll(currentOrders.map { FeedItem.OrderItem(it) })
            addAll(currentPayments.mapIndexed { index, payment -> FeedItem.PaymentItem(payment, index + 1) })
        }
        when (sortOrder) {
            CardSortOrder.NEWEST_TOP -> combined.sortedByDescending { item ->
                when (item) {
                    is FeedItem.OrderItem -> item.order.createdAt
                    is FeedItem.PaymentItem -> item.payment.createdAt
                }
            }
            CardSortOrder.NEWEST_BOTTOM -> combined.sortedBy { item ->
                when (item) {
                    is FeedItem.OrderItem -> item.order.createdAt
                    is FeedItem.PaymentItem -> item.payment.createdAt
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uniqueCustomers: StateFlow<List<String>> = orders.map { list ->
        list.map { it.customerName }.filter { it.isNotBlank() }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalWork: StateFlow<Long> = filteredOrders.map { list -> list.sumOf { it.totalPrice } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    val totalReceived: StateFlow<Long> = filteredPayments.map { list -> list.sumOf { it.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    val remainingBalance: StateFlow<Long> = combine(totalWork, totalReceived) { work, received -> work - received }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

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
    val subscriptionLoading: StateFlow<Boolean> = SubscriptionManager.isLoading
    val subscriptionMessage: StateFlow<String?> = SubscriptionManager.operationMessage

    val activeWorkshopState = activeWorkshop
    val currentUser = MutableStateFlow<FirebaseUserDto?>(null)
    val customUsername = MutableStateFlow("")
    val autoSyncStatusMessage = MutableStateFlow<String?>(null)
    val isAutoSyncing = MutableStateFlow(false)
    val manuallyDeletedModelNames = MutableStateFlow<Set<String>>(emptySet())
    val editingOrder = MutableStateFlow<FurnitureOrder?>(null)
    val editingPayment = MutableStateFlow<PaymentRecord?>(null)
    val selectedInvoiceOrder = MutableStateFlow<FurnitureOrder?>(null)
    val cardShareOrder = MutableStateFlow<FurnitureOrder?>(null)
    val cardSharePayment = MutableStateFlow<PaymentRecord?>(null)
    val deleteTarget = MutableStateFlow<DeleteTarget?>(null)

    fun hasPremiumAccess(): Boolean = SubscriptionManager.hasPremiumAccess()

    fun openSubscriptionDialog() { isSubscriptionDialogOpen.value = true }

    fun purchaseSubscription(activity: androidx.activity.ComponentActivity, plan: com.example.model.SubscriptionPlan) {
        SubscriptionManager.purchaseSubscription(activity, plan) { result ->
            if (result.isSuccess) isSubscriptionDialogOpen.value = false
        }
    }

    fun refreshSubscription() {
        SubscriptionManager.syncSubscriptionWithFirebase()
    }

    fun clearSubscriptionMessage() { SubscriptionManager.clearMessage() }

    fun getNextOrderNumber(): Int = (orders.value.maxOfOrNull { it.orderNumber } ?: 0) + 1
    fun getNextPaymentNumber(): Int = (payments.value.maxOfOrNull { it.paymentNumber } ?: 0) + 1

    fun openNewOrder() { editingOrder.value = null; isOrderDialogOpen.value = true }
    fun openEditOrder(order: FurnitureOrder) { editingOrder.value = order; isOrderDialogOpen.value = true }
    fun openNewPayment() { editingPayment.value = null; isPaymentDialogOpen.value = true }
    fun openEditPayment(payment: PaymentRecord) { editingPayment.value = payment; isPaymentDialogOpen.value = true }
    fun openInvoice(order: FurnitureOrder?) { selectedInvoiceOrder.value = order; isInvoiceDialogOpen.value = true }
    fun openCardShareOrder(order: FurnitureOrder) { cardShareOrder.value = order; cardSharePayment.value = null; isCardShareDialogOpen.value = true }
    fun openCardSharePayment(payment: PaymentRecord) { cardSharePayment.value = payment; cardShareOrder.value = null; isCardShareDialogOpen.value = true }

    fun saveOrder(order: FurnitureOrder, addToPresets: Boolean) {
        viewModelScope.launch {
            repository.saveOrder(order.copy(workshopId = activeWorkshopId.value))
            if (addToPresets) repository.savePreset(ModelPreset(name = order.modelName, workshopId = activeWorkshopId.value))
            triggerAutoUpload()
            isOrderDialogOpen.value = false
        }
    }

    fun duplicateOrder(order: FurnitureOrder) {
        viewModelScope.launch {
            repository.saveOrder(order.copy(id = 0L, orderNumber = getNextOrderNumber(), createdAt = System.currentTimeMillis()))
            triggerAutoUpload()
        }
    }

    fun savePayment(payment: PaymentRecord) {
        viewModelScope.launch {
            repository.savePayment(payment.copy(workshopId = activeWorkshopId.value))
            triggerAutoUpload()
            isPaymentDialogOpen.value = false
        }
    }

    fun requestDeleteOrder(order: FurnitureOrder) { deleteTarget.value = DeleteTarget.Order(order) }
    fun requestDeletePayment(payment: PaymentRecord) { deleteTarget.value = DeleteTarget.Payment(payment) }

    fun deleteTarget() {
        viewModelScope.launch {
            when (val target = deleteTarget.value) {
                is DeleteTarget.Order -> repository.deleteOrder(target.order)
                is DeleteTarget.Payment -> repository.deletePayment(target.payment)
                null -> return@launch
            }
            deleteTarget.value = null
            triggerAutoUpload()
        }
    }

    fun deletePresetByName(name: String) {
        manuallyDeletedModelNames.value = manuallyDeletedModelNames.value + name
        viewModelScope.launch { repository.deletePresetByName(name); triggerAutoUpload() }
    }

    fun updateModelColor(modelName: String, newColor: Long) {
        viewModelScope.launch { repository.updateModelColor(modelName, newColor); triggerAutoUpload() }
    }

    fun savePreset(preset: ModelPreset) { viewModelScope.launch { repository.savePreset(preset.copy(workshopId = activeWorkshopId.value)); triggerAutoUpload() } }
    fun deletePreset(preset: ModelPreset) { viewModelScope.launch { repository.deletePreset(preset); triggerAutoUpload() } }

    fun saveUnitRule(rule: com.example.model.UnitConversionRule) { viewModelScope.launch { repository.saveUnitRule(rule) } }
    fun deleteUnitRule(rule: com.example.model.UnitConversionRule) { viewModelScope.launch { repository.deleteUnitRule(rule) } }
    fun restoreDefaultUnitRules() { viewModelScope.launch { repository.restoreDefaultUnitRules() } }
    fun changeCurrency(newUnit: String) { if (newUnit != currencyUnit.value) currencyUnit.value = newUnit }

    fun clearFilters() {
        searchQuery.value = ""
        selectedCustomerFilter.value = null
        selectedModelFilter.value = null
        selectedDateFilter.value = null
        selectedInvoiceFilter.value = null
    }

    fun onUserLoggedIn(user: FirebaseUserDto, preferredUsername: String? = null, workshopName: String? = null) {
        currentUser.value = user
        SubscriptionManager.syncSubscriptionWithFirebase()
        val chosenName = preferredUsername?.takeIf { it.isNotBlank() }
            ?: customUsername.value.takeIf { it.isNotBlank() }
            ?: user.displayName?.takeIf { it.isNotBlank() }
        if (!chosenName.isNullOrBlank()) updateCustomUsername(chosenName)
        viewModelScope.launch {
            if (!workshopName.isNullOrBlank()) {
                val currentWs = activeWorkshop.value
                if (currentWs != null) renameWorkshop(currentWs.id, workshopName.trim()) else createWorkshop(workshopName.trim())
            }
            val localOrders = repository.getAllOrdersSync()
            val localPayments = repository.getAllPaymentsSync()
            performAutoSync(user, shouldDownload = localOrders.isEmpty() && localPayments.isEmpty())
        }
    }

    fun onUserLoggedOut() {
        FirebaseService.signOut()
        currentUser.value = null
        autoSyncStatusMessage.value = null
    }

    private suspend fun performAutoSync(user: FirebaseUserDto, shouldDownload: Boolean = false) {
        isAutoSyncing.value = true
        try {
            if (shouldDownload) FirebaseService.downloadFromCloud(repository)
            val currentOrders = repository.getAllOrdersSync()
            val currentPayments = repository.getAllPaymentsSync()
            val currentPresets = repository.getAllPresetsSync()
            if (currentOrders.isNotEmpty() || currentPayments.isNotEmpty() || currentPresets.isNotEmpty()) {
                FirebaseService.uploadAllToCloud(
                    orders = currentOrders,
                    payments = currentPayments,
                    presets = currentPresets,
                    unitRules = unitRules.value,
                    workshops = repository.getAllWorkshopsSync()
                )
            }
            autoSyncStatusMessage.value = "اطلاعات با حساب ابری همگام‌سازی شد."
        } catch (_: Exception) {
            autoSyncStatusMessage.value = "همگام‌سازی ابری در دسترس نیست."
        } finally {
            isAutoSyncing.value = false
        }
    }

    fun triggerAutoUpload() {
        if (currentUser.value == null) return
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

    fun updateCustomUsername(value: String) { customUsername.value = value }
    fun renameWorkshop(id: Long, newName: String) { viewModelScope.launch { repository.getWorkshopById(id)?.let { repository.saveWorkshop(it.copy(name = newName)); triggerAutoUpload() } } }
    fun createWorkshop(name: String) { viewModelScope.launch { repository.saveWorkshop(com.example.model.Workshop(name = name)); triggerAutoUpload() } }
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
