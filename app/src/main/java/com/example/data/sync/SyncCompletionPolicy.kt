package com.example.data.sync

object SyncCompletionPolicy {
    enum class Decision {
        SUCCESS,
        RETRY
    }

    fun afterSync(hasPendingSyncWork: Boolean): Decision =
        if (hasPendingSyncWork) Decision.RETRY else Decision.SUCCESS
}
