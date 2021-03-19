package com.thewizrd.simpleweather.services

import android.content.Context
import android.util.Log
import androidx.work.*
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.utils.Settings
import com.thewizrd.simpleweather.notifications.WeatherNotificationWorker
import com.thewizrd.simpleweather.shortcuts.ShortcutCreatorWorker
import com.thewizrd.simpleweather.utils.PowerUtils
import com.thewizrd.simpleweather.widgets.WidgetUpdaterHelper
import timber.log.Timber
import java.util.concurrent.TimeUnit

class WidgetUpdaterWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    companion object {
        private const val TAG = "WidgetUpdaterWorker"

        const val ACTION_UPDATEWIDGETS = "SimpleWeather.Droid.action.UPDATE_WIDGETS"
        const val ACTION_ENQUEUEWORK = "SimpleWeather.Droid.action.START_ALARM"
        const val ACTION_CANCELWORK = "SimpleWeather.Droid.action.CANCEL_ALARM"
        const val ACTION_REQUEUEWORK = "SimpleWeather.Droid.action.UPDATE_ALARM"

        suspend fun executeWork(context: Context) {
            WidgetUpdaterWork.executeWork(context.applicationContext)
        }

        @JvmStatic
        fun enqueueAction(context: Context, intentAction: String) {
            when (intentAction) {
                ACTION_REQUEUEWORK -> enqueueWork(context.applicationContext)
                ACTION_ENQUEUEWORK ->
                    if (!isWorkScheduled(context.applicationContext)) {
                        startWork(context.applicationContext)
                    }
                ACTION_UPDATEWIDGETS ->
                    // For immediate action
                    startWork(context.applicationContext)
                ACTION_CANCELWORK -> cancelWork(context.applicationContext)
            }
        }

        private fun startWork(context: Context) {
            Logger.writeLine(Log.INFO, "%s: Requesting to start work", TAG)

            val updateRequest = OneTimeWorkRequest.Builder(WidgetUpdaterWorker::class.java)
                    .build()

            WorkManager.getInstance(context)
                    .enqueueUniqueWork(TAG + "_onBoot", ExistingWorkPolicy.APPEND_OR_REPLACE, updateRequest)

            Logger.writeLine(Log.INFO, "%s: One-time work enqueued", TAG)

            if (!PowerUtils.useForegroundService) {
                // Enqueue periodic task as well
                enqueueWork(context)
            }
        }

        private fun enqueueWork(context: Context) {
            Logger.writeLine(Log.INFO, "%s: Requesting work; workExists: %s", TAG, java.lang.Boolean.toString(isWorkScheduled(context)))

            val updateRequest = PeriodicWorkRequest.Builder(WidgetUpdaterWorker::class.java, 60, TimeUnit.MINUTES, 15, TimeUnit.MINUTES)
                    .setConstraints(Constraints.NONE)
                    .addTag(TAG)
                    .build()

            WorkManager.getInstance(context)
                    .enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.REPLACE, updateRequest)

            Logger.writeLine(Log.INFO, "%s: Work enqueued", TAG)
        }

        private fun isWorkScheduled(context: Context): Boolean {
            val workMgr = WorkManager.getInstance(context.applicationContext)
            var statuses: List<WorkInfo>? = null
            try {
                statuses = workMgr.getWorkInfosForUniqueWork(TAG).get()
            } catch (ignored: Exception) {
            }
            if (statuses?.isNullOrEmpty() == true) return false
            var running = false
            for (workStatus in statuses) {
                running = (workStatus.state == WorkInfo.State.RUNNING
                        || workStatus.state == WorkInfo.State.ENQUEUED)
            }
            return running
        }

        private fun cancelWork(context: Context) {
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(TAG)
            Logger.writeLine(Log.INFO, "%s: Canceled work", TAG)
        }
    }

    override suspend fun doWork(): Result {
        WidgetUpdaterWork.executeWork(applicationContext)
        return Result.success()
    }

    private object WidgetUpdaterWork {
        suspend fun executeWork(context: Context) {
            if (Settings.isWeatherLoaded()) {
                if (WidgetUpdaterHelper.widgetsExist()) {
                    WidgetUpdaterHelper.refreshWidgets(context)
                }

                if (Settings.showOngoingNotification()) {
                    WeatherNotificationWorker.refreshNotification(context)
                }

                ShortcutCreatorWorker.updateShortcuts(context)
            }

            Timber.tag(TAG).i("Work completed successfully...")
        }
    }
}