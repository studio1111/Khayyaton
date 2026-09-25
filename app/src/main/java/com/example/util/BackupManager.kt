package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.WorkshopRepository
import com.example.model.FurnitureOrder
import com.example.model.ModelPreset
import com.example.model.PaymentRecord
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {

    /**
     * Serializes all application data into a formatted JSON string
     */
    fun createBackupJson(
        orders: List<FurnitureOrder>,
        payments: List<PaymentRecord>,
        presets: List<ModelPreset>,
        workshops: List<com.example.model.Workshop> = emptyList(),
        unitRules: List<com.example.model.UnitConversionRule> = emptyList()
    ): String {
        val root = JSONObject().apply {
            put("app", "Khayyaton")
            put("version", 4)
            put("exportedAt", System.currentTimeMillis())
            put("exportedDateJalali", PersianUtils.getTodayJalaliString())
            put("exportedDateGregorian", PersianUtils.getTodayGregorianString())
        }

        val workshopsArray = JSONArray()
        workshops.forEach { ws ->
            workshopsArray.put(JSONObject().apply {
                put("id", ws.id)
                put("syncId", ws.syncId)
                put("name", ws.name)
                put("createdAt", ws.createdAt)
            })
        }
        root.put("workshops", workshopsArray)

        val ordersArray = JSONArray()
        orders.forEach { ord ->
            ordersArray.put(JSONObject().apply {
                put("id", ord.id)
                put("syncId", ord.syncId)
                put("workshopId", ord.workshopId)
                put("workshopSyncId", ord.workshopSyncId)
                put("orderNumber", ord.orderNumber)
                put("invoiceNumber", ord.invoiceNumber)
                put("modelName", ord.modelName)
                put("pricePerSet", ord.pricePerSet)
                put("unitsPerSet", ord.unitsPerSet)
                put("countFormula", ord.countFormula)
                put("calculatedUnits", ord.calculatedUnits)
                put("calculatedTotal", ord.calculatedTotal)
                put("dateJalali", ord.dateJalali)
                put("dateGregorian", ord.dateGregorian)
                put("customerName", ord.customerName)
                put("phone", ord.phone)
                put("fabricName", ord.fabricName)
                put("workshopInvoiceNumber", ord.workshopInvoiceNumber)
                put("notes", ord.notes)
                put("colorCode", ord.colorCode)
                put("createdAt", ord.createdAt)
            })
        }
        root.put("orders", ordersArray)

        val paymentsArray = JSONArray()
        payments.forEach { pay ->
            paymentsArray.put(JSONObject().apply {
                put("id", pay.id)
                put("syncId", pay.syncId)
                put("workshopId", pay.workshopId)
                put("workshopSyncId", pay.workshopSyncId)
                put("paymentNumber", pay.paymentNumber)
                put("amount", pay.amount)
                put("dateJalali", pay.dateJalali)
                put("dateGregorian", pay.dateGregorian)
                put("customerName", pay.customerName)
                put("description", pay.description)
                put("paymentType", pay.paymentType)
                put("referenceNo", pay.referenceNo)
                put("bankName", pay.bankName)
                put("cardNumber", pay.cardNumber)
                put("relatedOrderId", pay.relatedOrderId ?: JSONObject.NULL)
                put("relatedOrderSyncId", pay.relatedOrderSyncId)
                put("createdAt", pay.createdAt)
            })
        }
        root.put("payments", paymentsArray)

        val presetsArray = JSONArray()
        presets.forEach { pre ->
            presetsArray.put(JSONObject().apply {
                put("id", pre.id)
                put("syncId", pre.syncId)
                put("workshopId", pre.workshopId)
                put("workshopSyncId", pre.workshopSyncId)
                put("name", pre.name)
                put("defaultPricePerSet", pre.defaultPricePerSet)
                put("defaultUnitsPerSet", pre.defaultUnitsPerSet)
                put("colorCode", pre.colorCode)
                put("description", pre.description)
            })
        }
        root.put("presets", presetsArray)

        val rulesArray = JSONArray()
        unitRules.forEach { rule ->
            rulesArray.put(JSONObject().apply {
                put("id", rule.id)
                put("syncId", rule.syncId)
                put("pieceKey", rule.pieceKey)
                put("pieceCount", rule.pieceCount)
                put("calculatedUnits", rule.calculatedUnits)
                put("isEnabled", rule.isEnabled)
            })
        }
        root.put("unitRules", rulesArray)

        return root.toString(2)
    }

    /**
     * Saves JSON backup to internal/external storage and returns the file
     */
    fun saveBackupToStorage(
        context: Context,
        jsonContent: String
    ): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "Khayyaton_Backup_$timeStamp.json"

            val targetDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            if (!targetDir.exists()) targetDir.mkdirs()

            val file = File(targetDir, fileName)
            file.writeText(jsonContent, Charsets.UTF_8)
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Shares JSON backup file via Android Share Chooser (can choose Google Drive, WhatsApp, Telegram, etc.)
     */
    fun shareBackupJson(
        context: Context,
        orders: List<FurnitureOrder>,
        payments: List<PaymentRecord>,
        presets: List<ModelPreset>,
        workshops: List<com.example.model.Workshop> = emptyList(),
        unitRules: List<com.example.model.UnitConversionRule> = emptyList(),
        preferGoogleDrive: Boolean = false
    ) {
        try {
            val json = createBackupJson(orders, payments, presets, workshops, unitRules)
            val cacheDir = File(context.cacheDir, "backups")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(cacheDir, "Khayyaton_Backup_$timeStamp.json")
            file.writeText(json, Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "پشتیبان اطلاعات کارگاه خیاطان")
                putExtra(Intent.EXTRA_TEXT, "فایل پشتیبان کامل داده‌های کارگاه خیاطان شامل ${orders.size} سفارش و ${payments.size} دریافتی.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (preferGoogleDrive) {
                    setPackage("com.google.android.apps.docs")
                }
            }

            val chooserTitle = if (preferGoogleDrive) "ذخیره در گوگل درایو (Google Drive)" else "اشتراک‌گذاری و ذخیره فایل پشتیبان JSON"
            try {
                context.startActivity(Intent.createChooser(intent, chooserTitle))
            } catch (_: Exception) {
                // If package not found, open general chooser
                intent.setPackage(null)
                context.startActivity(Intent.createChooser(intent, chooserTitle))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "ایجاد فایل پشتیبان انجام نشد. لطفاً فضای ذخیره‌سازی و دسترسی برنامه را بررسی کنید.", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Restores database records from JSON content string
     */
    suspend fun restoreFromJson(
        jsonString: String,
        repository: WorkshopRepository
    ): Triple<Int, Int, Int> {
        val root = JSONObject(jsonString)
        val version = root.optInt("version", 1)

        val workshops = mutableListOf<com.example.model.Workshop>()
        if (root.has("workshops")) {
            val array = root.getJSONArray("workshops")
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optLong("id", 0L)
                if (id > 0L) {
                    workshops += com.example.model.Workshop(
                        id = id,
                        syncId = obj.optString("syncId", if (id > 0) "wrk_" + id else java.util.UUID.randomUUID().toString()),
                        name = obj.optString("name", "کارگاه بازیابی‌شده").ifBlank { "کارگاه بازیابی‌شده" },
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                }
            }
        }

        val fallbackWorkshopId = workshops.firstOrNull()?.id
            ?: repository.getAllWorkshopsSync().firstOrNull()?.id
            ?: 1L

        val orders = mutableListOf<FurnitureOrder>()
        if (root.has("orders")) {
            val array = root.getJSONArray("orders")
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optLong("id", 0L)
                orders += FurnitureOrder(
                    id = id,
                    syncId = obj.optString("syncId", if (id > 0) "ord_" + id else java.util.UUID.randomUUID().toString()),
                    workshopId = obj.optLong("workshopId", fallbackWorkshopId),
                    workshopSyncId = obj.optString("workshopSyncId", ""),
                    orderNumber = obj.optLong("orderNumber", (i + 1).toLong()),
                    invoiceNumber = obj.optString("invoiceNumber", (i + 1).toString()),
                    modelName = obj.optString("modelName", "مدل مبل"),
                    pricePerSet = obj.optLong("pricePerSet", 0L),
                    unitsPerSet = obj.optDouble("unitsPerSet", 6.0),
                    countFormula = obj.optString("countFormula", ""),
                    calculatedUnits = obj.optDouble("calculatedUnits", 0.0),
                    calculatedTotal = obj.optLong("calculatedTotal", 0L),
                    dateJalali = obj.optString("dateJalali", PersianUtils.getTodayJalaliString()),
                    dateGregorian = obj.optString("dateGregorian", PersianUtils.getTodayGregorianString()),
                    customerName = obj.optString("customerName", ""),
                    phone = obj.optString("phone", ""),
                    fabricName = obj.optString("fabricName", ""),
                    workshopInvoiceNumber = obj.optString("workshopInvoiceNumber", ""),
                    notes = obj.optString("notes", ""),
                    colorCode = obj.optString("colorCode", "#2563EB"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            }
        }

        val payments = mutableListOf<PaymentRecord>()
        if (root.has("payments")) {
            val array = root.getJSONArray("payments")
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val relatedOrderId =
                    if (obj.has("relatedOrderId") && !obj.isNull("relatedOrderId")) obj.optLong("relatedOrderId") else null
                payments += PaymentRecord(
                    id = obj.optLong("id", 0L),
                    syncId = obj.optString("syncId", "pay_" + obj.optLong("id", 0L)),
                    workshopId = obj.optLong("workshopId", fallbackWorkshopId),
                    workshopSyncId = obj.optString("workshopSyncId", ""),
                    paymentNumber = obj.optLong("paymentNumber", (i + 1).toLong()),
                    amount = obj.optLong("amount", 0L),
                    dateJalali = obj.optString("dateJalali", PersianUtils.getTodayJalaliString()),
                    dateGregorian = obj.optString("dateGregorian", PersianUtils.getTodayGregorianString()),
                    customerName = obj.optString("customerName", ""),
                    description = obj.optString("description", ""),
                    paymentType = obj.optString("paymentType", "transfer"),
                    referenceNo = obj.optString("referenceNo", ""),
                    bankName = obj.optString("bankName", ""),
                    cardNumber = obj.optString("cardNumber", ""),
                    relatedOrderId = relatedOrderId,
                    relatedOrderSyncId = obj.optString("relatedOrderSyncId", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            }
        }

        val presets = mutableListOf<ModelPreset>()
        if (root.has("presets")) {
            val array = root.getJSONArray("presets")
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                presets += ModelPreset(
                    id = obj.optLong("id", 0L),
                    syncId = obj.optString("syncId", "pre_" + obj.optLong("id", 0L)),
                    workshopId = obj.optLong("workshopId", fallbackWorkshopId),
                    workshopSyncId = obj.optString("workshopSyncId", ""),
                    name = obj.optString("name", "مدل").ifBlank { "مدل" },
                    defaultPricePerSet = obj.optLong("defaultPricePerSet", 0L),
                    defaultUnitsPerSet = obj.optDouble("defaultUnitsPerSet", 6.0),
                    colorCode = obj.optString("colorCode", "#2563EB"),
                    description = obj.optString("description", "")
                )
            }
        }

        val rules = mutableListOf<com.example.model.UnitConversionRule>()
        if (root.has("unitRules")) {
            val array = root.getJSONArray("unitRules")
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                rules += com.example.model.UnitConversionRule(
                    id = obj.optLong("id", 0L),
                    syncId = obj.optString("syncId", "rule_" + obj.optLong("id", 0L)),
                    pieceKey = obj.optString("pieceKey", ""),
                    pieceCount = obj.optDouble("pieceCount", 0.0),
                    calculatedUnits = obj.optDouble("calculatedUnits", 0.0),
                    isEnabled = obj.optBoolean("isEnabled", true)
                )
            }
        }

        // Legacy v1/v2 backups did not contain workshops. Create one only as part
        // of an explicit user restore operation, never during normal app startup.
        val finalWorkshops = if (workshops.isEmpty() && (orders.isNotEmpty() || payments.isNotEmpty() || presets.isNotEmpty())) {
            listOf(
                com.example.model.Workshop(
                    id = fallbackWorkshopId,
                    name = "کارگاه بازیابی‌شده"
                )
            )
        } else workshops

        repository.replaceAllData(
            workshops = finalWorkshops,
            orders = orders,
            payments = payments,
            presets = presets,
            unitRules = rules
        )

        return Triple(orders.size, payments.size, presets.size)
    }

}
