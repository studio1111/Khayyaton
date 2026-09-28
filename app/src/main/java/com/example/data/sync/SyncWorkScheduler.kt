package com.example.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

object SyncWorkScheduler {
    private const val WORK_PREFIX = "khayyaton_cloud_sync_"

    // The UID is part of the unique work name so an old account's KEEP policy
    // can never block a newly authenticated account.
    fun workName(uid: String): String = WORK_PREFIX + uid

    fun enqueue(context: Context, uid: String) {
        if (uid.isBlank()) return

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<CloudSyncWorker>()
            .setInputData(workDataOf("uid" to uid))
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30,
                TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            workName(uid),
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun cancel(context: Context, uid: String) {
        if (uid.isBlank()) return
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(workName(uid))
    }
}
