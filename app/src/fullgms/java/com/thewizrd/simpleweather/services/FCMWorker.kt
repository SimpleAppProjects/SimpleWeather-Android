package com.thewizrd.simpleweather.services

import android.content.Context
import android.util.Log
import androidx.work.*
import com.thewizrd.shared_resources.preferences.FeatureSettings
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.simpleweather.images.ImageDataHelper
import com.thewizrd.simpleweather.images.ImageDatabase
import java.util.concurrent.TimeUnit

class FCMWorker(context: Context, workerParams: WorkerParameters) :
        CoroutineWorker(context, workerParams) {
    companion object {
        private const val TAG = "FCMWorker"

        const val ACTION_INVALIDATE = "SimpleWeather.Droid.action.INVALIDATE"

        @JvmStatic
        fun enqueueAction(context: Context, intentAction: String) {
            if (ACTION_INVALIDATE == intentAction) {
                startWork(context.applicationContext)
            }
        }

        private fun startWork(context: Context) {
            Logger.writeLine(Log.INFO, "%s: Requesting to start work", TAG)

            val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresCharging(true)
                    .build()

            val updateRequest = OneTimeWorkRequest.Builder(FCMWorker::class.java)
                    .setConstraints(constraints)
                    .setInitialDelay(1, TimeUnit.HOURS)
                    .build()

            WorkManager.getInstance(context.applicationContext).enqueue(updateRequest)

            Logger.writeLine(Log.INFO, "%s: One-time work enqueued", TAG)
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
                AnalyticsLogger.logEvent("$TAG: clearing image cache")

                // if so, invalidate
                ImageDataHelper.setImageDBUpdateTime(updateTime)
                ImageDataHelper.getImageDataHelper().clearCachedImageData()
                ImageDataHelper.invalidateCache(true)
            }
        }

        return Result.success()
    }
}