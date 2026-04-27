package com.example.baseandroidmodulehybrid.updater

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * UpdateWorker — фонова перевірка оновлень через WorkManager.
 *
 * ⚠️ ЗМІНИТИ:
 *  - REPEAT_INTERVAL_HOURS: як часто перевіряти (мінімум 15 хвилин)
 *  - Constraints: можна додати requiresCharging() або requiresBatteryNotLow()
 *  - Логіку після успішного оновлення (наприклад, показати сповіщення)
 */
@HiltWorker
class UpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val updaterRepository: UpdaterRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): androidx.work.ListenableWorker.Result {
        return try {
            val versionResult = updaterRepository.fetchVersionInfo()
            val versionInfo = versionResult.getOrElse { return androidx.work.ListenableWorker.Result.retry() }

            if (!updaterRepository.isUpdateRequired(versionInfo.version)) {
                return androidx.work.ListenableWorker.Result.success()
            }

            val downloadResult = updaterRepository.downloadBundle(versionInfo)
            val zipFile = downloadResult.getOrElse { return androidx.work.ListenableWorker.Result.retry() }

            val extractResult = updaterRepository.verifyAndExtract(zipFile, versionInfo.sha256)
            if (extractResult.isFailure) return androidx.work.ListenableWorker.Result.failure()

            updaterRepository.saveCurrentVersion(versionInfo.version)

            // ⚠️ ЗМІНИТИ: надіслати broadcast/notification про нове оновлення
            // NotificationHelper.showUpdateReady(applicationContext, versionInfo.version)

            androidx.work.ListenableWorker.Result.success()
        } catch (e: Exception) {
            // ⚠️ Якщо хочеш логувати помилки — додай тут Timber або Firebase Crashlytics
            androidx.work.ListenableWorker.Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "periodic_update_check"

        // ⚠️ ЗМІНИТИ: інтервал перевірки (мінімум 15 для WorkManager)
        private const val REPEAT_INTERVAL_HOURS = 6L

        /**
         * Реєструє або оновлює PeriodicWorkRequest.
         * Виклик з Application.onCreate() або MainActivity.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED) // Тільки з мережею
                // ⚠️ ОПЦІОНАЛЬНО: .setRequiresCharging(true)
                .build()

            val request = PeriodicWorkRequestBuilder<UpdateWorker>(
                REPEAT_INTERVAL_HOURS, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // KEEP = не перезапускати якщо вже є в черзі
                request
            )
        }
    }
}
