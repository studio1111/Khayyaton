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

            val expectedUid = inputData.getString("uid").orEmpty()
            val user = FirebaseService.currentUser()
            if (user == null) return Result.success()
            if (expectedUid.isNotBlank() && user.uid != expectedUid) {
                return Result.success()
            }

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

            val result = FirebaseService.syncAccount(repository)
            if (result.isSuccess) {
                when (SyncCompletionPolicy.afterSync(repository.hasPendingSyncWork())) {
                    SyncCompletionPolicy.Decision.RETRY -> Result.retry()
                    SyncCompletionPolicy.Decision.SUCCESS -> Result.success()
                }
            } else {
                val errorMsg = result.exceptionOrNull()?.message.orEmpty()
                if (
                    errorMsg.contains("منقضی") ||
                    errorMsg.contains("دسترسی لازم") ||
                    errorMsg.contains("وارد حساب") ||
                    errorMsg.contains("احراز هویت") ||
                    errorMsg.contains("تأیید امنیتی برنامه") ||
                    errorMsg.contains("کلید ارتباطی برنامه") ||
                    errorMsg.contains("شناسه همگام‌سازی")
                ) {
                    Result.failure(
                        androidx.work.workDataOf(
                            "errorMessage" to errorMsg
                        )
                    )
                } else {
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            android.util.Log.w(
                "CloudSyncWorker",
                e.message ?: "خطای نامشخص هنگام همگام‌سازی اطلاعات.",
                e
            )
            Result.retry()
        }
    }
}
