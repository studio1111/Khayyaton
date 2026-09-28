package com.example

import com.example.data.sync.SyncPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncPolicyTest {
    @Test
    fun `deleted id blocks incoming document`() {
        assertTrue(SyncPolicy.shouldIgnoreIncoming("orders", "abc", setOf("orders|abc")))
        assertFalse(SyncPolicy.shouldIgnoreIncoming("orders", "abc", emptySet()))
    }

    @Test
    fun `newer server timestamp wins over older local timestamp`() {
        assertEquals(SyncPolicy.REMOTE, SyncPolicy.chooseWinner(localUpdatedAt = 10L, remoteUpdatedAt = 20L))
        assertEquals(SyncPolicy.LOCAL, SyncPolicy.chooseWinner(localUpdatedAt = 30L, remoteUpdatedAt = 20L))
        assertEquals(SyncPolicy.REMOTE, SyncPolicy.chooseWinner(localUpdatedAt = 20L, remoteUpdatedAt = 20L))
    }
}
