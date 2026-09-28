package com.example.data.sync

object SyncPolicy {
    const val LOCAL = "LOCAL"
    const val REMOTE = "REMOTE"

    fun shouldIgnoreIncoming(
        collection: String,
        documentId: String,
        deletedKeys: Set<String>
    ): Boolean = "$collection|$documentId" in deletedKeys

    fun chooseWinner(localUpdatedAt: Long, remoteUpdatedAt: Long): String =
        if (remoteUpdatedAt >= localUpdatedAt) REMOTE else LOCAL
}
