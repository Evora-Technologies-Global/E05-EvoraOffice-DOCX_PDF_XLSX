package com.evora.technologies.office.core.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.evora.technologies.office.core.database.dao.TrashDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import java.io.File
import java.util.concurrent.TimeUnit

@HiltWorker
class TrashPurgeWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val trashDao: TrashDao,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val now = System.currentTimeMillis()
            val expired = trashDao.findExpired(now)
            expired.forEach { entry ->
                val file = File(entry.cachedFilePath)
                if (!file.exists() || file.delete()) {
                    trashDao.deleteById(entry.trashId)
                }
            }
            Result.success()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "evora_trash_auto_purge"

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<TrashPurgeWorker>(24, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
