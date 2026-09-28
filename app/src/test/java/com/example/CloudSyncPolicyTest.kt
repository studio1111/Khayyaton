package com.example

import com.example.data.sync.CloudSyncPhase
import com.example.data.sync.CloudSyncPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class CloudSyncPolicyTest {
    @Test
    fun new_account_restores_before_upload() {
        assertEquals(
            CloudSyncPhase.RESTORE_THEN_UPLOAD,
            CloudSyncPolicy.phaseFor(cloudReady = false)
        )
    }

    @Test
    fun cloud_ready_account_uploads_before_reconcile() {
        assertEquals(
            CloudSyncPhase.UPLOAD_THEN_RECONCILE,
            CloudSyncPolicy.phaseFor(cloudReady = true)
        )
    }
}
