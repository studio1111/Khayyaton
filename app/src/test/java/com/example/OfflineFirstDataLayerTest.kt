package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.sync.FileUploadStatus
import com.example.data.sync.PendingDeleteStatus
import com.example.data.sync.DeletedIdEntity
import com.example.data.sync.UploadQueueEntity
import com.example.data.sync.PendingDeleteEntity
import com.example.model.Workshop
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OfflineFirstDataLayerTest {
    @Test
    fun `tombstone and queues survive Room persistence`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val documentId = UUID.randomUUID().toString()
            db.deletedIdDao().upsert(
                DeletedIdEntity(
                    collection = "orders",
                    documentId = documentId,
                    deletedAt = 100L
                )
            )
            db.uploadQueueDao().upsert(
                UploadQueueEntity(
                    id = UUID.randomUUID().toString(),
                    documentId = documentId,
                    localFilePath = "/data/user/0/com.example/files/$documentId",
                    storagePath = "users/u/orders/$documentId",
                    status = FileUploadStatus.PENDING,
                    retryCount = 0,
                    createdAt = 100L
                )
            )
            db.pendingDeleteDao().upsert(
                PendingDeleteEntity(
                    id = UUID.randomUUID().toString(),
                    collection = "orders",
                    documentId = documentId,
                    storagePath = "users/u/orders/$documentId",
                    status = PendingDeleteStatus.PENDING,
                    retryCount = 0,
                    createdAt = 100L
                )
            )

            assertTrue(db.deletedIdDao().contains("orders", documentId))
            assertEquals(1, db.uploadQueueDao().pending().size)
            assertEquals(1, db.pendingDeleteDao().pending().size)
        } finally {
            db.close()
        }
    }
    @Test
    fun `cloud upsert and tombstone prevent duplicates and resurrection`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repository = com.example.data.WorkshopRepository(
                context = context,
                database = db,
                orderDao = db.orderDao(),
                paymentDao = db.paymentDao(),
                modelPresetDao = db.modelPresetDao(),
                unitRuleDao = db.unitRuleDao(),
                workshopDao = db.workshopDao()
            )
            val syncId = UUID.randomUUID().toString()
            repository.mergeCloudData(
                cloudWorkshops = listOf(Workshop(id = 0L, syncId = syncId, name = "کارگاه یک")),
                cloudOrders = emptyList(),
                cloudPayments = emptyList(),
                cloudPresets = emptyList(),
                cloudUnitRules = emptyList()
            )
            repository.mergeCloudData(
                cloudWorkshops = listOf(Workshop(id = 0L, syncId = syncId, name = "کارگاه به‌روز")),
                cloudOrders = emptyList(),
                cloudPayments = emptyList(),
                cloudPresets = emptyList(),
                cloudUnitRules = emptyList()
            )
            assertEquals(1, db.workshopDao().getAllWorkshopsSync().size)
            assertEquals("کارگاه به‌روز", db.workshopDao().getAllWorkshopsSync().first().name)

            db.deletedIdDao().upsert(
                DeletedIdEntity("workshops", syncId, System.currentTimeMillis())
            )
            repository.mergeCloudData(
                cloudWorkshops = listOf(Workshop(id = 0L, syncId = syncId, name = "نباید برگردد")),
                cloudOrders = emptyList(),
                cloudPayments = emptyList(),
                cloudPresets = emptyList(),
                cloudUnitRules = emptyList()
            )
            assertEquals(1, db.workshopDao().getAllWorkshopsSync().size)
            assertEquals("کارگاه به‌روز", db.workshopDao().getAllWorkshopsSync().first().name)
        } finally {
            db.close()
        }
    }

}
