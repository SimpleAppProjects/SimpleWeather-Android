package com.thewizrd.simpleweather.services

import android.content.Context
import android.util.Log
import androidx.work.*
import com.thewizrd.shared_resources.preferences.FeatureSettings
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.shared_resources.utils.LiveDataUtils.awaitWithTimeout
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.simpleweather.images.ImageDataHelper
import com.thewizrd.simpleweather.images.ImageDatabase
import com.thewizrd.simpleweather.services.ImageDatabaseWorkerActions.ACTION_CANCELALARM
import com.thewizrd.simpleweather.services.ImageDatabaseWorkerActions.ACTION_CHECKUPDATETIME
import com.thewizrd.simpleweather.services.ImageDatabaseWorkerActions.ACTION_STARTALARM
import com.thewizrd.simpleweather.services.ImageDatabaseWorkerActions.ACTION_UPDATEALARM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class ImageDatabaseWorker(context: Context, workerParams: WorkerParameters) :
        CoroutineWorker(context, workerParams) {
    companion object {
        private const val TAG = "ImageDatabaseWorker"

        @JvmStatic
        @JvmOverloads
        fun enqueueAction(context: Context, intentAction: String, onBoot: Boolean = false) {
            // For immediate action
            when (intentAction) {
                ACTION_UPDATEALARM -> enqueueWork(context.applicationContext)
                ACTION_STARTALARM -> {
                    GlobalScope.launch(Dispatchers.Default) {
                        if (onBoot || !isWorkScheduled(context.applicationContext)) {
                            startWork(context.applicationContext)
                        }
                    }
                }
                ACTION_CHECKUPDATETIME -> {
                    // For immediate action
                    startWork(context.applicationContext)
                }
                ACTION_CANCELALARM -> cancelWork(context.applicationContext)
            }
        }

        private fun startWork(context: Context) {
            Logger.writeLine(Log.INFO, "%s: Requesting to start work", TAG)

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresCharging(false)
                .build()
            val updateRequest = OneTimeWorkRequest.Builder(ImageDatabaseWorker::class.java)
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueue(updateRequest)

            Logger.writeLine(Log.INFO, "%s: One-time work enqueued", TAG)

            // Enqueue periodic task as well
            enqueueWork(context)
        }

        private fun enqueueWork(context: Context) {
            Logger.writeLine(Log.INFO, "%s: Requesting work", TAG)

            val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresCharging(false)
                    .build()

            val updateRequest = PeriodicWorkRequest.Builder(ImageDatabaseWorker::class.java, 7, TimeUnit.DAYS)
                    .setConstraints(constraints)
                    .addTag(TAG)
                    .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.REPLACE, updateRequest)

            Logger.writeLine(Log.INFO, "%s: Work enqueued", TAG)
        }

        private suspend fun isWorkScheduled(context: Context): Boolean {
            val workMgr = WorkManager.getInstance(context)
            val statuses = workMgr.getWorkInfosForUniqueWorkLiveData(TAG).awaitWithTimeout(10000)
            if (statuses.isNullOrEmpty()) return false
            var running = false
            for (workStatus in statuses) {
                running = (workStatus.state == WorkInfo.State.RUNNING
                        || workStatus.state == WorkInfo.State.ENQUEUED)
            }
            return running
        }

        private fun cancelWork(context: Context) {
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
                ImageDatabase.getLastUpdateTime()
            } catch (e: Exception) {
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