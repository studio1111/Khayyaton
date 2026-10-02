package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.WorkshopRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CloudSyncNoticeTest {

    @Test
    fun notice_is_shown_by_default_and_persisted_when_checked() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Clear test preferences
        context.getSharedPreferences("khayyaton_prefs", Context.MODE_PRIVATE).edit().clear().commit()

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

            // 1. By default, the notice MUST be shown on first entry
            assertTrue(repository.shouldShowCloudVpnWarning())

            // 2. If user dismisses without checking "don't show again", it still should be shown next time
            repository.setDontShowCloudVpnWarning(false)
            assertTrue(repository.shouldShowCloudVpnWarning())

            // 3. If user checks "don't show again" and confirms, it should NEVER be shown again
            repository.setDontShowCloudVpnWarning(true)
            assertFalse(repository.shouldShowCloudVpnWarning())
        } finally {
            db.close()
        }
    }
}
