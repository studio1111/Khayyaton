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
import com.example.util.PersianUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SheetOnViewModel(val repository: WorkshopRepository) : ViewModel() {

    val orders: StateFlow<List<FurnitureOrder>> = repository.orders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payments: StateFlow<List<PaymentRecord>> = repository.payments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val modelPresets: StateFlow<List<ModelPreset>> = repository.modelPresets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
    val isDrawerOpen = MutableStateFlow(false)

    init {
        // App initialized clean without sample data; insert default unit rules if empty
        viewModelScope.launch {
            repository.insertDefaultUnitRulesIfEmpty()
        }
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
        }
    }

    fun duplicateOrder(order: FurnitureOrder) {
        viewModelScope.launch {
            val newNum = getNextOrderNumber()
            val duplicated = order.copy(
                id = 0L,
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
            repository.saveOrder(order)
            if (order.modelName.isNotBlank()) {
                val trimmedName = order.modelName.trim()
                val existingPreset = modelPresets.value.find { it.name.trim().equals(trimmedName, ignoreCase = true) }
                if (existingPreset != null) {
                    repository.savePreset(
                        existingPreset.copy(
                            defaultPricePerSet = order.pricePerSet,
                            defaultUnitsPerSet = order.unitsPerSet,
                            colorCode = order.colorCode.ifBlank { existingPreset.colorCode }
                        )
                    )
                } else {
                    repository.savePreset(
                        ModelPreset(
                            name = trimmedName,
                            defaultPricePerSet = order.pricePerSet,
                            defaultUnitsPerSet = order.unitsPerSet,
                            colorCode = order.colorCode,
                            description = if (order.fabricName.isNotBlank()) "پارچه ${order.fabricName}" else ""
                        )
                    )
                }
            }
            isOrderDialogOpen.value = false
            editingOrder.value = null
        }
    }

    fun savePayment(payment: PaymentRecord) {
        viewModelScope.launch {
            repository.savePayment(payment)
            isPaymentDialogOpen.value = false
            editingPayment.value = null
        }
    }

    val manuallyDeletedModelNames = MutableStateFlow<Set<String>>(emptySet())

    fun savePreset(preset: ModelPreset) {
        viewModelScope.launch {
            repository.savePreset(preset)
            manuallyDeletedModelNames.value = manuallyDeletedModelNames.value - preset.name.trim().lowercase()
        }
    }

    fun deletePreset(preset: ModelPreset) {
        viewModelScope.launch {
            repository.deletePreset(preset)
            manuallyDeletedModelNames.value = manuallyDeletedModelNames.value + preset.name.trim().lowercase()
        }
    }

    fun deletePresetByName(name: String) {
        viewModelScope.launch {
            repository.deletePresetByName(name)
            manuallyDeletedModelNames.value = manuallyDeletedModelNames.value + name.trim().lowercase()
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
        }
    }

    fun restoreDefaultUnitRules() {
        viewModelScope.launch {
            repository.insertDefaultUnitRulesIfEmpty()
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
}

class SheetOnViewModelFactory(private val repository: WorkshopRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SheetOnViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SheetOnViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
