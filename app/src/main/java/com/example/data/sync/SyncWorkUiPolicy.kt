package com.example.data.sync

import androidx.work.WorkInfo

object SyncWorkUiPolicy {
    enum class State {
        IDLE,
        RUNNING,
        QUEUED,
        SUCCESS,
        FAILED,
        CANCELLED
    }

    data class Snapshot(
        val state: WorkInfo.State,
        val errorMessage: String? = null
    )

    data class Result(
        val state: State,
        val errorMessage: String? = null
    )

    fun resolve(infos: List<Snapshot>): Result {
        if (infos.isEmpty()) return Result(State.IDLE)

        val running = infos.any { it.state == WorkInfo.State.RUNNING }
        if (running) return Result(State.RUNNING)

        val queued = infos.any {
            it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.BLOCKED
        }
        if (queued) return Result(State.QUEUED)

        val failed = infos.firstOrNull { it.state == WorkInfo.State.FAILED }
        if (failed != null) {
            return Result(
                State.FAILED,
                failed.errorMessage?.takeIf { it.isNotBlank() }
                    ?: "همگام‌سازی اطلاعات ناموفق بود."
            )
        }

        if (infos.any { it.state == WorkInfo.State.CANCELLED }) {
            return Result(State.CANCELLED)
        }

        return Result(State.SUCCESS)
    }
}
