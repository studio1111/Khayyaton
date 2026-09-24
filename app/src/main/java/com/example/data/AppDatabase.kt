        val uid = getLocalAccountUid().orEmpty()
        return "pending_cloud_deletions_$uid"
    }

    private fun deletionKey(collection: String, syncId: String): String =
        "$collection|$syncId"

    fun recordCloudDeletion(collection: String, syncId: String) {
        if (syncId.isBlank()) return
        val current = prefs?.getStringSet(pendingDeletionKey(), emptySet()).orEmpty().toMutableSet()
        current += deletionKey(collection, syncId)
        prefs?.edit()?.putStringSet(pendingDeletionKey(), current)?.apply()
    }

    fun getPendingCloudDeletions(): List<PendingCloudDeletion> {
        return prefs?.getStringSet(pendingDeletionKey(), emptySet()).orEmpty()
            .mapNotNull { raw ->
                val parts = raw.split("|", limit = 2)
                if (parts.size == 2 && parts[1].isNotBlank()) {
                    PendingCloudDeletion(parts[0], parts[1])
                } else null