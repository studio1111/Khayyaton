package com.example.data.sync

object AuthLossPolicy {
    fun shouldClearLocalData(hasPendingSyncWork: Boolean): Boolean =
        !hasPendingSyncWork
}
