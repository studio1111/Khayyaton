package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.WorkshopRepository
import com.example.data.sync.FileUploadStatus
import com.example.data.sync.UploadQueueEntity
import com.example.data.sync.PendingDeleteEntity
import com.example.data.sync.PendingDeleteStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SyncPendingStateTest {
    @Test
    fun legacy_file_queues_do_not_count_as_active_sync_work() = runBlocking {
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

            db.uploadQueueDao().upsert(
                UploadQueueEntity(
                    id = UUID.randomUUID().toString(),
                    documentId = "legacy-doc",
                    localFilePath = "legacy",
                    storagePath = "legacy",
                    status = FileUploadStatus.PENDING,
                    retryCount = 0,
                    createdAt = 1L
                )
            )
            db.pendingDeleteDao().upsert(
                PendingDeleteEntity(
                    id = UUID.randomUUID().toString(),
                    collection = "orders",
                    documentId = "legacy-doc",
                    storagePath = "legacy",
                    status = PendingDeleteStatus.PENDING,
                    retryCount = 0,
                    createdAt = 1L
                )
            )

            assertFalse(repository.hasPendingSyncWork())
            assertTrue(db.uploadQueueDao().pending().isNotEmpty())
            assertTrue(db.pendingDeleteDao().pending().isNotEmpty())
        } finally {
            db.close()
        }
    }
}
