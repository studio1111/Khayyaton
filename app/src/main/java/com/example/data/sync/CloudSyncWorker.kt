package com.example.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.data.WorkshopRepository
import com.example.data.firebase.FirebaseService

class CloudSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            FirebaseService.initialize(applicationContext)
            val user = FirebaseService.getCurrentUser() ?: return Result.success()

            val database = AppDatabase.getDatabase(applicationContext)
            val repository = WorkshopRepository(
                context = applicationContext,
                database = database,
                orderDao = database.orderDao(),
                paymentDao = database.paymentDao(),
                modelPresetDao = database.modelPresetDao(),
                unitRuleDao = database.unitRuleDao(),
                workshopDao = database.workshopDao()
            )

            // Background work can start during the login lifecycle. Restore
            // the authenticated account first so an empty/default local snapshot
            // can never overwrite or mask the cloud account.
            if (!repository.isCloudSyncReady(user.uid)) {
                val restore = FirebaseService.downloadFromCloud(repository)
                if (restore.isFailure) {
                    return Result.retry()
                }
                repository.markCloudSyncReady(user.uid)
            }

            val result = FirebaseService.uploadAllToCloud(
                orders = repository.getAllOrdersSync(),
                payments = repository.getAllPaymentsSync(),
                presets = repository.getAllPresetsSync(),
                unitRules = repository.getAllUnitRulesSync(),
                workshops = repository.getAllWorkshopsSync(),
                repository = repository
            )

            if (result.isSuccess) {
                Result.success()
            } else {
                val errorMsg = result.exceptionOrNull()?.message.orEmpty()
                if (errorMsg.contains("منقضی") || errorMsg.contains("دسترسی") || errorMsg.contains("وارد حساب")) {
                    Result.failure()
                } else {
                    Result.retry()
                }
            }
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
