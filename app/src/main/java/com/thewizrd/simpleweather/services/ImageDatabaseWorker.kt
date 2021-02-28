package com.thewizrd.simpleweather.services

import android.content.Context
import android.util.Log
import androidx.work.*
import com.thewizrd.shared_resources.preferences.FeatureSettings
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.weatherdata.images.ImageDataHelper
import com.thewizrd.shared_resources.weatherdata.images.ImageDatabase
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class ImageDatabaseWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    companion object {
        private const val TAG = "ImageDatabaseWorker"

        const val ACTION_CHECKUPDATETIME = "SimpleWeather.Droid.action.CHECK_UPDATE_TIME"
        const val ACTION_STARTALARM = "SimpleWeather.Droid.action.START_ALARM"
        const val ACTION_CANCELALARM = "SimpleWeather.Droid.action.CANCEL_ALARM"
        const val ACTION_UPDATEALARM = "SimpleWeather.Droid.action.UPDATE_ALARM"

        @JvmStatic
        fun enqueueAction(context: Context, intentAction: String) {
            val context = context.applicationContext

            if (ACTION_UPDATEALARM == intentAction) {
                enqueueWork(context)
            } else if (ACTION_CHECKUPDATETIME == intentAction || ACTION_STARTALARM == intentAction) {
                // For immediate action
                startWork(context)
            } else if (ACTION_CANCELALARM == intentAction) {
                cancelWork(context)
            }
        }

        private fun startWork(context: Context) {
            val context = context.applicationContext

            Logger.writeLine(Log.INFO, "%s: Requesting to start work", TAG)

            val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresCharging(false)
                    .build()
            val updateRequest = OneTimeWorkRequest.Builder(ImageDatabaseWorker::class.java)
                    .setConstraints(constraints)
                    .setInitialDelay(1, TimeUnit.MINUTES)
                    .build()
            WorkManager.getInstance(context)
                    .enqueueUniqueWork(TAG + "_onBoot", ExistingWorkPolicy.REPLACE, updateRequest)
            Logger.writeLine(Log.INFO, "%s: One-time work enqueued", TAG)

            // Enqueue periodic task as well
            enqueueWork(context)
        }

        private fun enqueueWork(context: Context) {
            val context = context.applicationContext

            Logger.writeLine(Log.INFO, "%s: Requesting work; workExists: %s", TAG, java.lang.Boolean.toString(isWorkScheduled(context)))

            val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresCharging(false)
                    .build()

            val updateRequest = PeriodicWorkRequest.Builder(ImageDatabaseWorker::class.java, 7, TimeUnit.DAYS)
                    .setConstraints(constraints)
                    .build()

            WorkManager.getInstance(context)
                    .enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.KEEP, updateRequest)

            Logger.writeLine(Log.INFO, "%s: Work enqueued", TAG)
        }

        private fun isWorkScheduled(context: Context): Boolean {
            val context = context.applicationContext
            val workMgr = WorkManager.getInstance(context)
            var statuses: List<WorkInfo>? = null
            try {
                statuses = workMgr.getWorkInfosForUniqueWork(TAG).get()
            } catch (ignored: ExecutionException) {
            } catch (ignored: InterruptedException) {
            }
            if (statuses == null || statuses.isEmpty()) return false
            var running = false
            for (workStatus in statuses) {
                running = (workStatus.state == WorkInfo.State.RUNNING
                        || workStatus.state == WorkInfo.State.ENQUEUED)
            }
            return running
        }

        private fun cancelWork(context: Context) {
            val context = context.applicationContext
            WorkManager.getInstance(context).cancelUniqueWork(TAG)
            Logger.writeLine(Log.INFO, "%s: Canceled work", TAG)
        }
    }

    override suspend fun doWork(): Result {
        Logger.writeLine(Log.INFO, "%s: Work started", TAG)

        // Check if cache is populated
        if (!ImageDataHelper.getImageDataHelper().isEmpty && !FeatureSettings.isUpdateAvailable()) {
            // If so, check if we need to invalidate
            val updateTime = try {
                ImageDatabase.getLastUpdateTime().await()
            } catch (e: ExecutionException) {
                Logger.writeLine(Log.ERROR, e)
                0L
            } catch (e: InterruptedException) {
                Logger.writeLine(Log.ERROR, e)
                0L
            }

            if (updateTime > ImageDataHelper.getImageDBUpdateTime()) {
                AnalyticsLogger.logEvent("ImgDBWorker: clearing image cache")

                // if so, invalidate
                ImageDataHelper.setImageDBUpdateTime(updateTime)
                ImageDataHelper.getImageDataHelper().clearCachedImageData()
                ImageDataHelper.invalidateCache(true)
            }
        }

        return Result.success()
    }
}