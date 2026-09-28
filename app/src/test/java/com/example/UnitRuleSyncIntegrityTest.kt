package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.WorkshopRepository
import com.example.model.UnitConversionRule
import com.example.data.sync.RecordSyncStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class UnitRuleSyncIntegrityTest {
    @Test
    fun cloud_rule_reuses_existing_mandatory_local_rule_by_piece_key() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repository = WorkshopRepository(
                context = context,
                database = db,
                orderDao = db.orderDao(),
                paymentDao = db.paymentDao(),
                modelPresetDao = db.modelPresetDao(),
                unitRuleDao = db.unitRuleDao(),
                workshopDao = db.workshopDao()
            )

            repository.insertDefaultUnitRulesIfEmpty()

            repository.mergeCloudData(
                cloudWorkshops = emptyList(),
                cloudOrders = emptyList(),
                cloudPayments = emptyList(),
                cloudPresets = emptyList(),
                cloudUnitRules = listOf(
                    UnitConversionRule(
                        id = 99L,
                        syncId = "remote-rule-3",
                        pieceKey = "3",
                        pieceCount = 3.0,
                        calculatedUnits = 2.0,
                        isEnabled = true,
                        updatedAt = 1000L,
                        syncStatus = RecordSyncStatus.SYNCED
                    )
                )
            )

            val rules = db.unitRuleDao().getAllRulesSync()
                .filter { WorkshopRepository.normalizeUnitKey(it.pieceKey.ifBlank { it.pieceCount.toString() }) == "3" }

            assertEquals(1, rules.size)
            assertEquals("remote-rule-3", rules.first().syncId)
        } finally {
            db.close()
        }
    }
}
