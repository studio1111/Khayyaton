package com.example.data.sync

import androidx.room.Entity
import androidx.room.Index
import androidx.room.TypeConverter

enum class FileUploadStatus { PENDING, UPLOADING, UPLOADED, FAILED }
enum class PendingDeleteStatus { PENDING, DELETING, DELETED, FAILED }
enum class RecordSyncStatus { SYNCED, PENDING, FAILED, DELETED }

class SyncStatusConverters {
    @TypeConverter
    fun uploadStatusToString(value: FileUploadStatus): String = value.name

    @TypeConverter
    fun stringToUploadStatus(value: String): FileUploadStatus =
        runCatching { FileUploadStatus.valueOf(value) }.getOrDefault(FileUploadStatus.PENDING)

    @TypeConverter
    fun deleteStatusToString(value: PendingDeleteStatus): String = value.name

    @TypeConverter
    fun stringToDeleteStatus(value: String): PendingDeleteStatus =
        runCatching { PendingDeleteStatus.valueOf(value) }.getOrDefault(PendingDeleteStatus.PENDING)

    @TypeConverter
    fun recordStatusToString(value: RecordSyncStatus): String = value.name

    @TypeConverter
    fun stringToRecordStatus(value: String): RecordSyncStatus =
        runCatching { RecordSyncStatus.valueOf(value) }.getOrDefault(RecordSyncStatus.SYNCED)
}

@Entity(
    tableName = "documents_cache",
    primaryKeys = ["collection", "documentId"],
    indices = [Index(value = ["documentId"]), Index(value = ["updatedAt"])]
)
data class DocumentCacheEntity(
    val collection: String,
    val documentId: String,
    val updatedAt: Long = 0L,
    val fromCache: Boolean = false,
    val hasPendingWrites: Boolean = false
)

@Entity(
    tableName = "deleted_ids",
    primaryKeys = ["collection", "documentId"],
    indices = [Index(value = ["deletedAt"])]
)
data class DeletedIdEntity(
    val collection: String,
    val documentId: String,
    val deletedAt: Long,
    val cloudSynced: Boolean = false
)

@Entity(
    tableName = "upload_queue",
    indices = [Index(value = ["status"]), Index(value = ["createdAt"])]
)
data class UploadQueueEntity(
    @androidx.room.PrimaryKey
    val id: String,
    val documentId: String,
    val collection: String = "",
    val localFilePath: String,
    val storagePath: String,
    val status: FileUploadStatus = FileUploadStatus.PENDING,
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "pending_deletes",
    indices = [Index(value = ["status"]), Index(value = ["createdAt"])]
)
data class PendingDeleteEntity(
    @androidx.room.PrimaryKey
    val id: String,
    val collection: String,
    val documentId: String,
    val storagePath: String? = null,
    val status: PendingDeleteStatus = PendingDeleteStatus.PENDING,
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
