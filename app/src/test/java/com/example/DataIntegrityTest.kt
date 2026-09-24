package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.WorkshopRepository
import com.example.model.FurnitureOrder
import com.example.model.ModelPreset
import com.example.model.PaymentRecord
import com.example.model.UnitConversionRule
import com.example.model.Workshop
import com.example.util.BackupManager
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.runBlocking

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DataIntegrityTest {

    private fun repository(context: Context): Pair<AppDatabase, WorkshopRepository> {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val repo = WorkshopRepository(
            context = context,
            database = db,
            orderDao = db.orderDao(),
            paymentDao = db.paymentDao(),
            modelPresetDao = db.modelPresetDao(),
            unitRuleDao = db.unitRuleDao(),
            workshopDao = db.workshopDao()
        )
        return db to repo
    }

    @Test
    fun \`replaceAllData preserves IDs and workshop relationships\`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val (db, repo) = repository(context)

        try {
            val workshop = Workshop(id = 42L, name = "کارگاه آزمایشی")
            val order = FurnitureOrder(
                id = 1001L,
                workshopId = 42L,
                orderNumber = 7L,
                invoiceNumber = "7",
                modelName = "مدل تست",
                pricePerSet = 1000L,
                countFormula = "2",
                calculatedUnits = 2.0,
                calculatedTotal = 2000L,
                dateJalali = "1405/07/02",
                dateGregorian = "2026-09-24",
                customerName = "مشتری تست"
            )
            val payment = PaymentRecord(
                id = 2001L,
                workshopId = 42L,
                paymentNumber = 3L,
                amount = 1500L,
                dateJalali = "1405/07/02",
                dateGregorian = "2026-09-24",
                customerName = "مشتری تست",
                relatedOrderId = 1001L
            )

            repo.replaceAllData(
                workshops = listOf(workshop),
                orders = listOf(order),
                payments = listOf(payment),
                presets = listOf(ModelPreset(id = 3001L, workshopId = 42L, name = "مدل تست")),
                unitRules = listOf(UnitConversionRule(id = 4001L, pieceKey = "2", pieceCount = 2.0, calculatedUnits = 1.5))
            )

            assertEquals(42L, repo.getAllWorkshopsSync().single().id)
            assertEquals(1001L, repo.getAllOrdersSync().single().id)
            assertEquals(42L, repo.getAllOrdersSync().single().workshopId)
            assertEquals(2001L, repo.getAllPaymentsSync().single().id)
            assertEquals(1001L, repo.getAllPaymentsSync().single().relatedOrderId)
            assertEquals(3001L, repo.getAllPresetsSync().single().id)
            assertEquals(4001L, repo.getAllUnitRulesSync().single().id)
        } finally {
            db.close()
        }
    }

    @Test
    fun \`cloud merge keeps separate workshops when local numeric IDs collide\`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val (db, repo) = repository(context)
        try {
            repo.replaceAllData(
                workshops = listOf(Workshop(id = 1L, name = "دستگاه A", syncId = "workshop-A")),
                orders = emptyList(),
                payments = emptyList(),
                presets = emptyList(),
                unitRules = emptyList()
            )
            repo.mergeCloudData(
                cloudWorkshops = listOf(Workshop(id = 1L, name = "دستگاه B", syncId = "workshop-B")),
                cloudOrders = listOf(
                    FurnitureOrder(
                        id = 1L,
                        syncId = "order-B",
                        workshopId = 1L,
                        workshopSyncId = "workshop-B",
                        orderNumber = 1L,
                        invoiceNumber = "B-1",
                        modelName = "مدل B",
                        pricePerSet = 100L,
                        countFormula = "1",
                        calculatedUnits = 1.0,
                        calculatedTotal = 100L,
                        dateJalali = "1405/07/02",
                        dateGregorian = "2026-09-24",
                        customerName = "مشتری B"
                    )
                ),
                cloudPayments = emptyList(),
                cloudPresets = emptyList(),
                cloudUnitRules = emptyList()
            )
            val workshops = repo.getAllWorkshopsSync()
            val orders = repo.getAllOrdersSync()
            assertEquals(2, workshops.size)
            assertEquals(1, orders.size)
            assertEquals("workshop-B", workshops.first { it.name == "دستگاه B" }.syncId)
            assertEquals(workshops.first { it.syncId == "workshop-B" }.id, orders.single().workshopId)
        } finally {
            db.close()
        }
    }

    @Test
    fun \`backup JSON contains full current data schema\`() {
        val order = FurnitureOrder(
            id = 1L,
            workshopId = 42L,
            orderNumber = 1L,
            invoiceNumber = "1",
            modelName = "مدل",
            pricePerSet = 10L,
            countFormula = "1",
            calculatedUnits = 1.0,
            calculatedTotal = 10L,
            dateJalali = "1405/07/02",
            dateGregorian = "2026-09-24",
            customerName = "مشتری"
        )
        val payment = PaymentRecord(
            id = 2L,
            workshopId = 42L,
            paymentNumber = 1L,
            amount = 5L,
            dateJalali = "1405/07/02",
            dateGregorian = "2026-09-24",
            customerName = "مشتری",
            relatedOrderId = 1L
        )

        val json = BackupManager.createBackupJson(
            orders = listOf(order),
            payments = listOf(payment),
            presets = listOf(ModelPreset(id = 3L, workshopId = 42L, name = "مدل")),
            workshops = listOf(Workshop(id = 42L, name = "کارگاه")),
            unitRules = listOf(UnitConversionRule(id = 4L, pieceKey = "1", pieceCount = 1.0, calculatedUnits = 1.0))
        )

        val root = JSONObject(json)
        assertEquals(4, root.getInt("version"))
        assertEquals(1, root.getJSONArray("workshops").length())
        assertEquals(1, root.getJSONArray("orders").length())
        assertEquals(1, root.getJSONArray("payments").length())
        assertEquals(1, root.getJSONArray("presets").length())
        assertEquals(1, root.getJSONArray("unitRules").length())
        assertTrue(root.getJSONArray("payments").getJSONObject(0).has("relatedOrderId"))
        assertTrue(root.getJSONArray("orders").getJSONObject(0).has("syncId"))
        assertTrue(root.getJSONArray("orders").getJSONObject(0).has("workshopSyncId"))
        assertTrue(root.getJSONArray("payments").getJSONObject(0).has("relatedOrderSyncId"))
    }
}
