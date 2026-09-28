package com.example.data.sync

import android.content.Context
import android.net.Uri
import com.example.data.AppDatabase
import com.example.data.firebase.FirebaseService
import com.google.firebase.firestore.FieldValue
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID
import kotlin.math.min

class StorageSyncManager(
    private val context: Context,
    private val database: AppDatabase
) {
    companion object {
        private const val MAX_RETRIES = 5
    }

    suspend fun enqueueUpload(
        collection: String,
        documentId: String,
        sourceFile: File
    ): String {
        val stableId = collection + "_" + documentId
        val safeDir = File(context.filesDir, "offline_uploads").apply { mkdirs() }
        val localFile = File(safeDir, stableId)
        sourceFile.copyTo(localFile, overwrite = true)
        database.uploadQueueDao().upsert(
            UploadQueueEntity(
                id = stableId,
                documentId = documentId,
                collection = collection,
                localFilePath = localFile.absolutePath,
                storagePath = "users/" + FirebaseService.currentUser()?.uid.orEmpty() + "/files/" + documentId,
                status = FileUploadStatus.PENDING,
                retryCount = 0
            )
        )
        return stableId
    }

    suspend fun enqueueDelete(
        collection: String,
        documentId: String,
        storagePath: String?
    ) {
        val id = UUID.randomUUID().toString()
        database.pendingDeleteDao().upsert(
            PendingDeleteEntity(
                id = id,
                collection = collection,
                documentId = documentId,
                storagePath = storagePath,
                status = PendingDeleteStatus.PENDING,
                retryCount = 0
            )
        )
        storagePath?.let { path ->
            val localName = path.substringAfterLast('/')
            File(context.filesDir, "offline_uploads/$localName").delete()
        }
    }

    suspend fun processQueues() {
        processUploads()
        processDeletes()
    }

    private suspend fun processUploads() {
        val storage = FirebaseService.storageInstance() ?: return
        val firestore = FirebaseService.firestoreInstance() ?: return
        val user = FirebaseService.currentUser() ?: return

        for (item in database.uploadQueueDao().pending()) {
            if (item.status == FileUploadStatus.UPLOADED) continue

            val localFile = File(item.localFilePath)
            if (!localFile.exists()) {
                database.uploadQueueDao().updateStatus(item.id, FileUploadStatus.FAILED, MAX_RETRIES)
                continue
            }

            var success = false
            var retries = item.retryCount
            while (retries < MAX_RETRIES && !success) {
                try {
                    database.uploadQueueDao().updateStatus(item.id, FileUploadStatus.UPLOADING, retries)
                    val ref = storage.reference.child(item.storagePath)
                    ref.putFile(Uri.fromFile(localFile)).await()
                    val url = ref.downloadUrl.await().toString()
                    firestore.collection("users").document(user.uid)
                        .collection(item.collection)
                        .document(item.documentId)
                        .set(
                            mapOf(
                                "fileUrl" to url,
                                "storagePath" to item.storagePath,
                                "updatedAt" to FieldValue.serverTimestamp()
                            ),
                            com.google.firebase.firestore.SetOptions.merge()
                        )
                        .await()
                    database.uploadQueueDao().updateStatus(item.id, FileUploadStatus.UPLOADED, retries)
                    localFile.delete()
                    database.uploadQueueDao().delete(item.id)
                    success = true
                } catch (_: Exception) {
                    retries += 1
                    if (retries >= MAX_RETRIES) {
                        database.uploadQueueDao().updateStatus(item.id, FileUploadStatus.FAILED, retries)
                    } else {
                        database.uploadQueueDao().updateStatus(item.id, FileUploadStatus.PENDING, retries)
                        delay(min(30_000L, 1_000L shl (retries - 1)))
                    }
                }
            }
        }
    }

    private suspend fun processDeletes() {
        val storage = FirebaseService.storageInstance() ?: return
        val firestore = FirebaseService.firestoreInstance() ?: return
        val user = FirebaseService.currentUser() ?: return

        for (item in database.pendingDeleteDao().pending()) {
            var retries = item.retryCount
            var success = false

            while (retries < MAX_RETRIES && !success) {
                try {
                    database.pendingDeleteDao().updateStatus(item.id, PendingDeleteStatus.DELETING, retries)
                    item.storagePath?.let { path ->
                        try {
                            storage.reference.child(path).delete().await()
                        } catch (e: StorageException) {
                            if (e.errorCode != StorageException.ERROR_OBJECT_NOT_FOUND) throw e
                        }
                    }

                    firestore.collection("users").document(user.uid)
                        .collection(item.collection)
                        .document(item.documentId)
                        .delete()
                        .await()

                    database.pendingDeleteDao().updateStatus(item.id, PendingDeleteStatus.DELETED, retries)
                    database.pendingDeleteDao().delete(item.id)
                    // Keep the tombstone durable. A later snapshot must never
                    // resurrect the document after remote cleanup.
                    database.deletedIdDao().markCloudSynced(item.collection, item.documentId)
                    success = true
                } catch (_: Exception) {
                    retries += 1
                    if (retries >= MAX_RETRIES) {
                        database.pendingDeleteDao().updateStatus(item.id, PendingDeleteStatus.FAILED, retries)
                    } else {
                        database.pendingDeleteDao().updateStatus(item.id, PendingDeleteStatus.PENDING, retries)
                        delay(min(30_000L, 1_000L shl (retries - 1)))
                    }
                }
            }
        }
    }
}
