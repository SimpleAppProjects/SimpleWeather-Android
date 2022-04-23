package com.thewizrd.simpleweather.services

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.work.*
import com.thewizrd.common.utils.LiveDataUtils.awaitWithTimeout
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.simpleweather.services.ServiceNotificationHelper.JOB_ID
import com.thewizrd.simpleweather.services.ServiceNotificationHelper.getForegroundNotification
import com.thewizrd.simpleweather.services.ServiceNotificationHelper.initChannel
import com.thewizrd.simpleweather.wearable.WeatherComplicationHelper
import com.thewizrd.simpleweather.wearable.WeatherTileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit

class WidgetUpdaterWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    companion object {
        private const val TAG = "WidgetUpdaterWorker"

        const val ACTION_UPDATEWIDGETS = "SimpleWeather.Droid.action.UPDATE_WIDGETS"
        const val ACTION_ENQUEUEWORK = "SimpleWeather.Droid.action.START_ALARM"
        const val ACTION_CANCELWORK = "SimpleWeather.Droid.action.CANCEL_ALARM"
        const val ACTION_REQUEUEWORK = "SimpleWeather.Droid.action.UPDATE_ALARM"

        suspend fun requestWidgetUpdate(context: Context) {
            if (settingsManager.isWeatherLoaded()) {
                Timber.tag(TAG).i("Requesting widget update...")

                // Update complications
                WeatherComplicationHelper.requestComplicationUpdateAll(context.applicationContext)

                // Update tiles
                WeatherTileHelper.requestTileUpdateAll(context.applicationContext)
            }
        }

        @JvmStatic
        @JvmOverloads
        fun enqueueAction(context: Context, intentAction: String, onBoot: Boolean = false) {
            when (intentAction) {
                ACTION_REQUEUEWORK -> enqueueWork(context.applicationContext)
                ACTION_ENQUEUEWORK ->
                    GlobalScope.launch(Dispatchers.Default) {
                        if (onBoot || !isWorkScheduled(context.applicationContext)) {
                            startWork(context.applicationContext)
                        }
                    }
                ACTION_UPDATEWIDGETS ->
                    // For immediate action
                    startWork(context.applicationContext)
                ACTION_CANCELWORK -> cancelWork(context.applicationContext)
            }
        }

        private fun startWork(context: Context) {
            Logger.writeLine(Log.INFO, "%s: Requesting to start work", TAG)

            val updateRequest = OneTimeWorkRequestBuilder<WidgetUpdaterWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            WorkManager.getInstance(context.applicationContext).enqueue(updateRequest)

            Logger.writeLine(Log.INFO, "%s: One-time work enqueued", TAG)

            // Enqueue periodic task as well
            enqueueWork(context.applicationContext)
        }

        private fun enqueueWork(context: Context) {
            Logger.writeLine(Log.INFO, "%s: Requesting work", TAG)

            val updateRequest = PeriodicWorkRequest.Builder(
                WidgetUpdaterWorker::class.java,
                60,
                TimeUnit.MINUTES,
                5,
                TimeUnit.MINUTES
            )
                .setConstraints(Constraints.NONE)
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context.applicationContext)
                    .enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.REPLACE, updateRequest)

            Logger.writeLine(Log.INFO, "%s: Work enqueued", TAG)
        }

        private suspend fun isWorkScheduled(context: Context): Boolean {
            val workMgr = WorkManager.getInstance(context.applicationContext)
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
            // Cancel alarm if dependent features are turned off
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(TAG)
            Logger.writeLine(Log.INFO, "%s: Canceled work", TAG)
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        initChannel(applicationContext)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                JOB_ID,
                getForegroundNotification(applicationContext),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            ForegroundInfo(
                JOB_ID,
                getForegroundNotification(applicationContext)
            )
        }
    }

    override suspend fun doWork(): Result {
        requestWidgetUpdate(applicationContext)
        return Result.success()
    }
}