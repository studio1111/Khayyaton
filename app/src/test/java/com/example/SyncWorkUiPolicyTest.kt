package com.example

import androidx.work.WorkInfo
import com.example.data.sync.SyncWorkUiPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncWorkUiPolicyTest {
    @Test
    fun queued_child_work_must_not_look_successful_just_because_head_succeeded() {
        val result = SyncWorkUiPolicy.resolve(
            listOf(
                SyncWorkUiPolicy.Snapshot(WorkInfo.State.SUCCEEDED),
                SyncWorkUiPolicy.Snapshot(WorkInfo.State.ENQUEUED)
            )
        )

        assertEquals(SyncWorkUiPolicy.State.QUEUED, result.state)
    }

    @Test
    fun failed_work_preserves_its_persian_error() {
        val result = SyncWorkUiPolicy.resolve(
            listOf(
                SyncWorkUiPolicy.Snapshot(
                    WorkInfo.State.FAILED,
                    "دسترسی لازم برای این عملیات وجود ندارد."
                )
            )
        )

        assertEquals(SyncWorkUiPolicy.State.FAILED, result.state)
        assertEquals(
            "دسترسی لازم برای این عملیات وجود ندارد.",
            result.errorMessage
        )
    }
}
