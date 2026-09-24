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

            val result = FirebaseService.uploadAllToCloud(
                orders = repository.getAllOrdersSync(),
                payments = repository.getAllPaymentsSync(),
                presets = repository.getAllPresetsSync(),
                unitRules = repository.getAllUnitRulesSync(),
                workshops = repository.getAllWorkshopsSync()
            )

            if (result.isSuccess) Result.success() else Result.retry()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
