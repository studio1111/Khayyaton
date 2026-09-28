package com.example

import com.example.data.sync.AuthLossPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthLossPolicyTest {
    @Test
    fun local_state_is_preserved_when_unsynced_work_exists() {
        assertFalse(AuthLossPolicy.shouldClearLocalData(hasPendingSyncWork = true))
    }

    @Test
    fun clean_local_state_can_be_cleared_after_auth_loss() {
        assertTrue(AuthLossPolicy.shouldClearLocalData(hasPendingSyncWork = false))
    }
}
