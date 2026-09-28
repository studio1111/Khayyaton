package com.example.data.sync

object AuthLossPolicy {
    /**
     * A passive Firebase Auth null event is not an explicit logout.
     * Local data is cleared only by the explicit, synchronized logout/account-switch path.
     */
    fun shouldClearLocalData(hasPendingSyncWork: Boolean): Boolean = false
}
