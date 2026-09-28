package com.example

import com.example.data.sync.AuthLossPolicy
import org.junit.Assert.assertFalse
import org.junit.Test

class AuthLossPolicyTest {
    @Test
    fun local_state_is_preserved_when_unsynced_work_exists() {
        assertFalse(AuthLossPolicy.shouldClearLocalData(hasPendingSyncWork = true))
    }

    @Test
    fun passive_auth_loss_never_clears_local_state() {
        assertFalse(AuthLossPolicy.shouldClearLocalData(hasPendingSyncWork = false))
    }
}
