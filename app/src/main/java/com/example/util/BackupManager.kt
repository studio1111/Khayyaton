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
        presets: List<ModelPreset>
    ): String {
        val root = JSONObject()
        root.put("app", "SheetOn")
        root.put("version", 2)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("exportedDateJalali", PersianUtils.getTodayJalaliString())
        root.put("exportedDateGregorian", PersianUtils.getTodayGregorianString())

        // Orders
        val ordersArray = JSONArray()
        orders.forEach { ord ->
            val obj = JSONObject().apply {
                put("id", ord.id)
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
            }
            ordersArray.put(obj)
        }
        root.put("orders", ordersArray)

        // Payments
        val paymentsArray = JSONArray()
        payments.forEach { pay ->
            val obj = JSONObject().apply {
                put("id", pay.id)
                put("paymentNumber", pay.paymentNumber)
                put("amount", pay.amount)
                put("dateJalali", pay.dateJalali)
                put("dateGregorian", pay.dateGregorian)
                put("customerName", pay.customerName)
                put("description", pay.description)
                put("paymentType", pay.paymentType)
                put("referenceNo", pay.referenceNo)
                put("relatedOrderId", pay.relatedOrderId ?: JSONObject.NULL)
                put("createdAt", pay.createdAt)
            }
            paymentsArray.put(obj)
        }
        root.put("payments", paymentsArray)

        // Presets
        val presetsArray = JSONArray()
        presets.forEach { pre ->
            val obj = JSONObject().apply {
                put("id", pre.id)
                put("name", pre.name)
                put("defaultPricePerSet", pre.defaultPricePerSet)
                put("defaultUnitsPerSet", pre.defaultUnitsPerSet)
                put("colorCode", pre.colorCode)
                put("description", pre.description)
            }
            presetsArray.put(obj)
        }
        root.put("presets", presetsArray)

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
            val fileName = "SheetOn_Backup_$timeStamp.json"

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
        preferGoogleDrive: Boolean = false
    ) {
        try {
            val json = createBackupJson(orders, payments, presets)
            val cacheDir = File(context.cacheDir, "backups")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(cacheDir, "SheetOn_Backup_$timeStamp.json")
            file.writeText(json, Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "پشتیبان اطلاعات کارگاه SheetOn")
                putExtra(Intent.EXTRA_TEXT, "فایل پشتیبان کامل داده‌های کارگاه مبل SheetOn شامل ${orders.size} سفارش و ${payments.size} دریافتی.")
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
            Toast.makeText(context, "خطا در ایجاد فایل پشتیبان: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
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
        var restoredOrders = 0
        var restoredPayments = 0
        var restoredPresets = 0

        // Parse orders
        if (root.has("orders")) {
            val ordersArray = root.getJSONArray("orders")
            for (i in 0 until ordersArray.length()) {
                val obj = ordersArray.getJSONObject(i)
                val order = FurnitureOrder(
                    id = 0, // Reset ID for clean insertion
                    orderNumber = obj.optLong("orderNumber", (i + 1).toLong()),
                    invoiceNumber = obj.optString("invoiceNumber", (i + 1).toString()),
                    modelName = obj.optString("modelName", "مدل مبل"),
                    pricePerSet = obj.optLong("pricePerSet", 0L),
                    unitsPerSet = obj.optDouble("unitsPerSet", 8.0),
                    countFormula = obj.optString("countFormula", "3+3+1+1"),
                    calculatedUnits = obj.optDouble("calculatedUnits", 8.0),
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
                repository.saveOrder(order)
                restoredOrders++
            }
        }

        // Parse payments
        if (root.has("payments")) {
            val paymentsArray = root.getJSONArray("payments")
            for (i in 0 until paymentsArray.length()) {
                val obj = paymentsArray.getJSONObject(i)
                val payment = PaymentRecord(
                    id = 0,
                    paymentNumber = obj.optLong("paymentNumber", (i + 1).toLong()),
                    amount = obj.optLong("amount", 0L),
                    dateJalali = obj.optString("dateJalali", PersianUtils.getTodayJalaliString()),
                    dateGregorian = obj.optString("dateGregorian", PersianUtils.getTodayGregorianString()),
                    customerName = obj.optString("customerName", ""),
                    description = obj.optString("description", ""),
                    paymentType = obj.optString("paymentType", "transfer"),
                    referenceNo = obj.optString("referenceNo", ""),
                    relatedOrderId = if (obj.has("relatedOrderId") && !obj.isNull("relatedOrderId")) obj.optLong("relatedOrderId") else null,
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
                repository.savePayment(payment)
                restoredPayments++
            }
        }

        // Parse presets
        if (root.has("presets")) {
            val presetsArray = root.getJSONArray("presets")
            for (i in 0 until presetsArray.length()) {
                val obj = presetsArray.getJSONObject(i)
                val preset = ModelPreset(
                    id = 0,
                    name = obj.optString("name", "مدل"),
                    defaultPricePerSet = obj.optLong("defaultPricePerSet", 0L),
                    defaultUnitsPerSet = obj.optDouble("defaultUnitsPerSet", 8.0),
                    colorCode = obj.optString("colorCode", "#2563EB"),
                    description = obj.optString("description", "")
                )
                repository.savePreset(preset)
                restoredPresets++
            }
        }

        return Triple(restoredOrders, restoredPayments, restoredPresets)
    }
}
