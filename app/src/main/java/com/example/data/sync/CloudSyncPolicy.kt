package com.example.data.sync

enum class CloudSyncPhase {
    RESTORE_THEN_UPLOAD,
    UPLOAD_THEN_RECONCILE
}

object CloudSyncPolicy {
    fun phaseFor(cloudReady: Boolean): CloudSyncPhase =
        if (cloudReady) {
            CloudSyncPhase.UPLOAD_THEN_RECONCILE
        } else {
            CloudSyncPhase.RESTORE_THEN_UPLOAD
        }
}
