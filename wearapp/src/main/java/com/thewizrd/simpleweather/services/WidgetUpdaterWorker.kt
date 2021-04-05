package com.thewizrd.simpleweather.services

import android.content.Context
import android.util.Log
import androidx.work.*
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.simpleweather.App
import com.thewizrd.simpleweather.wearable.WeatherComplicationHelper
import com.thewizrd.simpleweather.wearable.WeatherTileHelper
import timber.log.Timber
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class WidgetUpdaterWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    companion object {
        private const val TAG = "WidgetUpdaterWorker"

        const val ACTION_STARTALARM = "SimpleWeather.Droid.action.START_ALARM"
        const val ACTION_CANCELALARM = "SimpleWeather.Droid.action.CANCEL_ALARM"
        const val ACTION_UPDATEALARM = "SimpleWeather.Droid.action.UPDATE_ALARM"

        @JvmStatic
        fun requestWidgetUpdate(context: Context) {
            if (App.instance.settingsManager.isWeatherLoaded()) {
                Timber.tag(TAG).i("Requesting widget update...")

                // Update complications
                WeatherComplicationHelper.requestComplicationUpdateAll(context.applicationContext)

                // Update tiles
                WeatherTileHelper.requestTileUpdateAll(context.applicationContext)
            }
        }

        @JvmStatic
        fun enqueueAction(context: Context, intentAction: String) {
            when (intentAction) {
                ACTION_UPDATEALARM -> enqueueWork(context.applicationContext)
                ACTION_STARTALARM ->
                    // For immediate action
                    startWork(context)
                ACTION_CANCELALARM -> cancelWork(context.applicationContext)
            }
        }

        private fun startWork(context: Context) {
            Logger.writeLine(Log.INFO, "%s: Requesting to start work", TAG)

            val updateRequest = OneTimeWorkRequest.Builder(WidgetUpdaterWorker::class.java)
                    .build()

            WorkManager.getInstance(context.applicationContext)
                    .enqueueUniqueWork(TAG + "_onBoot", ExistingWorkPolicy.APPEND_OR_REPLACE, updateRequest)

            Logger.writeLine(Log.INFO, "%s: One-time work enqueued", TAG)

            // Enqueue periodic task as well
            enqueueWork(context.applicationContext)
        }

        private fun enqueueWork(context: Context) {
            Logger.writeLine(Log.INFO, "%s: Requesting work; workExists: %s", TAG, java.lang.Boolean.toString(isWorkScheduled(context)))

            val updateRequest = PeriodicWorkRequest.Builder(WidgetUpdaterWorker::class.java, 60, TimeUnit.MINUTES, 30, TimeUnit.MINUTES)
                    .setConstraints(Constraints.NONE)
                    .addTag(TAG)
                    .build()

            WorkManager.getInstance(context.applicationContext)
                    .enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.REPLACE, updateRequest)

            Logger.writeLine(Log.INFO, "%s: Work enqueued", TAG)
        }

        private fun isWorkScheduled(context: Context): Boolean {
            val workMgr = WorkManager.getInstance(context.applicationContext)
            var statuses: List<WorkInfo>? = null
            try {
                statuses = workMgr.getWorkInfosForUniqueWork(TAG).get()
            } catch (ignored: ExecutionException) {
            } catch (ignored: InterruptedException) {
            }
            if (statuses?.isNullOrEmpty() == true) return false
            var running = false
            for (workStatus in statuses) {
                running = (workStatus.state == WorkInfo.State.RUNNING
                        || workStatus.state == WorkInfo.State.ENQUEUED)
            }
            return running
        }

        private fun cancelWork(context: Context): Boolean {
            // Cancel alarm if dependent features are turned off
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(TAG)
            Logger.writeLine(Log.INFO, "%s: Canceled work", TAG)
            return true
        }
    }

    override fun doWork(): Result {
        requestWidgetUpdate(applicationContext)
        return Result.success()
    }
}