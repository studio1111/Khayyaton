package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.WorkshopRepository
import com.example.model.Workshop
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AccountIsolationTest {
    @Test
    fun switching_account_clears_previous_local_state_before_new_context() = runBlocking {
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
            repository.saveLocalAccountUid("user-a")
            repository.markCloudSyncReady("user-a")
            assertTrue(repository.isCloudSyncReady("user-a"))

            repository.replaceAllData(
                workshops = listOf(Workshop(id = 1L, syncId = "a-ws", name = "کارگاه کاربر اول")),
                orders = emptyList(),
                payments = emptyList(),
                presets = emptyList(),
                unitRules = emptyList()
            )

            db.workshopDao().setSyncStatus(
                "a-ws",
                com.example.data.sync.RecordSyncStatus.SYNCED
            )

            val previousUid = repository.ensureLocalAccount("user-b")

            assertEquals("user-a", previousUid)
            assertEquals("user-b", repository.getLocalAccountUid())
            assertEquals(false, repository.isCloudSyncReady("user-a"))
            assertEquals(false, repository.isCloudSyncReady("user-b"))
            assertTrue(repository.getAllWorkshopsSync().isEmpty())
            assertEquals(0L, repository.getSavedActiveWorkshopId())
        } finally {
            db.close()
        }
    }
    
    @Test
    fun switching_account_is_blocked_when_pending_local_changes_exist() = runBlocking {
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

            repository.saveLocalAccountUid("user-a")
            repository.saveWorkshop(Workshop(name = "کارگاه کاربر اول"))

            val result = runCatching {
                repository.ensureLocalAccount("user-b")
            }

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is com.example.data.sync.PendingAccountSwitchException)
            assertEquals("user-a", repository.getLocalAccountUid())
            assertEquals(1, repository.getAllWorkshopsSync().size)
        } finally {
            db.close()
        }
    }
}
