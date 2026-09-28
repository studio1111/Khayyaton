package com.example.data.sync

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DeletedIdDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DeletedIdEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM deleted_ids WHERE collection = :collection AND documentId = :documentId)")
    suspend fun contains(collection: String, documentId: String): Boolean

    @Query("SELECT * FROM deleted_ids")
    suspend fun getAll(): List<DeletedIdEntity>

    @Query("SELECT * FROM deleted_ids WHERE cloudSynced = 0")
    suspend fun getPending(): List<DeletedIdEntity>

    @Query("UPDATE deleted_ids SET cloudSynced = 1 WHERE collection = :collection AND documentId = :documentId")
    suspend fun markCloudSynced(collection: String, documentId: String)

    @Query("DELETE FROM deleted_ids WHERE collection = :collection AND documentId = :documentId")
    suspend fun delete(collection: String, documentId: String)

    @Query("DELETE FROM deleted_ids")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM deleted_ids WHERE cloudSynced = 0")
    fun countPendingFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM deleted_ids")
    fun countFlow(): Flow<Int>
}

@Dao
interface UploadQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UploadQueueEntity)

    @Query("SELECT * FROM upload_queue WHERE status IN ('PENDING', 'FAILED') ORDER BY createdAt ASC")
    suspend fun pending(): List<UploadQueueEntity>

    @Query("DELETE FROM upload_queue")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM upload_queue WHERE status IN ('PENDING', 'UPLOADING', 'FAILED')")
    fun pendingCount(): Flow<Int>

    @Query("UPDATE upload_queue SET status = :status, retryCount = :retryCount WHERE id = :id")
    suspend fun updateStatus(id: String, status: FileUploadStatus, retryCount: Int)

    @Query("DELETE FROM upload_queue WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM upload_queue WHERE id = :id LIMIT 1")
    suspend fun find(id: String): UploadQueueEntity?
}

@Dao
interface PendingDeleteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PendingDeleteEntity)

    @Query("SELECT * FROM pending_deletes WHERE status IN ('PENDING', 'FAILED') ORDER BY createdAt ASC")
    suspend fun pending(): List<PendingDeleteEntity>

    @Query("DELETE FROM pending_deletes")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM pending_deletes WHERE status IN ('PENDING', 'DELETING', 'FAILED')")
    fun pendingCount(): Flow<Int>

    @Query("UPDATE pending_deletes SET status = :status, retryCount = :retryCount WHERE id = :id")
    suspend fun updateStatus(id: String, status: PendingDeleteStatus, retryCount: Int)

    @Query("DELETE FROM pending_deletes WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM pending_deletes WHERE id = :id LIMIT 1")
    suspend fun find(id: String): PendingDeleteEntity?
}

@Dao
interface DocumentCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DocumentCacheEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM documents_cache WHERE collection = :collection AND documentId = :documentId)")
    suspend fun exists(collection: String, documentId: String): Boolean

    @Query("DELETE FROM documents_cache WHERE collection = :collection AND documentId = :documentId")
    suspend fun delete(collection: String, documentId: String)

    @Query("SELECT * FROM documents_cache WHERE hasPendingWrites = 1")
    fun pendingWrites(): Flow<List<DocumentCacheEntity>>

    @Query("DELETE FROM documents_cache")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM documents_cache WHERE hasPendingWrites = 1")
    fun pendingWritesCount(): Flow<Int>
}
