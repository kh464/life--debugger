package com.lifedbg.mobile.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lifedbg.mobile.LifeDbgApp
import com.lifedbg.mobile.summarize.UsageRefreshRepository
import com.lifedbg.mobile.usage.UsageAccessChecker
import java.util.concurrent.TimeUnit

class UsageCollectWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        if (!UsageAccessChecker(applicationContext).hasUsageAccess()) {
            return Result.success()
        }
        return runCatching {
            val app = applicationContext as LifeDbgApp
            UsageRefreshRepository(applicationContext, app.database).refreshToday()
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() },
        )
    }

    companion object {
        private const val WORK_NAME = "usage_collect_worker"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<UsageCollectWorker>(30, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }
}
