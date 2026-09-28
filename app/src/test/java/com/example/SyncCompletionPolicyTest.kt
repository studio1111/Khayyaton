package com.example

import com.example.data.sync.SyncCompletionPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncCompletionPolicyTest {
    @Test
    fun pending_work_after_upload_requires_retry() {
        assertEquals(
            SyncCompletionPolicy.Decision.RETRY,
            SyncCompletionPolicy.afterSync(hasPendingSyncWork = true)
        )
    }

    @Test
    fun clean_database_allows_success() {
        assertEquals(
            SyncCompletionPolicy.Decision.SUCCESS,
            SyncCompletionPolicy.afterSync(hasPendingSyncWork = false)
        )
    }
}
