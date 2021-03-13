package com.thewizrd.simpleweather.services

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.utils.Settings
import com.thewizrd.simpleweather.notifications.WeatherNotificationBroadcastReceiver
import com.thewizrd.simpleweather.notifications.WeatherNotificationWorker
import com.thewizrd.simpleweather.shortcuts.ShortcutCreatorWorker
import com.thewizrd.simpleweather.utils.PowerUtils
import com.thewizrd.simpleweather.widgets.WeatherWidgetBroadcastReceiver
import com.thewizrd.simpleweather.widgets.WeatherWidgetService
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class WidgetUpdaterWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    private val mContext = context.applicationContext

    companion object {
        private const val TAG = "WidgetUpdaterWorker"

        const val ACTION_UPDATEWIDGETS = "SimpleWeather.Droid.action.UPDATE_WIDGETS"
        const val ACTION_ENQUEUEWORK = "SimpleWeather.Droid.action.START_ALARM"
        const val ACTION_CANCELWORK = "SimpleWeather.Droid.action.CANCEL_ALARM"
        const val ACTION_REQUEUEWORK = "SimpleWeather.Droid.action.UPDATE_ALARM"

        private const val JOB_ID = 1005

        @JvmStatic
        fun enqueueAction(context: Context, intentAction: String) {
            val context = context.applicationContext
            when (intentAction) {
                ACTION_REQUEUEWORK -> enqueueWork(context)
                ACTION_ENQUEUEWORK ->
                    if (!isWorkScheduled(context)) {
                        startWork(context)
                    }
                ACTION_UPDATEWIDGETS ->
                    // For immediate action
                    startWork(context)
                ACTION_CANCELWORK -> cancelWork(context)
            }
        }

        private fun startWork(context: Context) {
            val context = context.applicationContext
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
            val context = context.applicationContext
            Logger.writeLine(Log.INFO, "%s: Requesting work; workExists: %s", TAG, java.lang.Boolean.toString(isWorkScheduled(context)))
            val updateRequest = PeriodicWorkRequest.Builder(WidgetUpdaterWorker::class.java, 60, TimeUnit.MINUTES, 15, TimeUnit.MINUTES)
                    .setConstraints(Constraints.NONE)
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
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(TAG)
            Logger.writeLine(Log.INFO, "%s: Canceled work", TAG)
        }
    }

    override fun doWork(): Result {
        if (Settings.isWeatherLoaded()) {
            if (WeatherWidgetService.widgetsExist(mContext)) {
                mContext.sendBroadcast(Intent(mContext, WeatherWidgetBroadcastReceiver::class.java)
                        .setAction(WeatherWidgetService.ACTION_REFRESHWIDGET))
            }

            if (Settings.showOngoingNotification()) {
                mContext.sendBroadcast(Intent(mContext, WeatherNotificationBroadcastReceiver::class.java)
                        .setAction(WeatherNotificationWorker.ACTION_REFRESHNOTIFICATION))
            }

            ShortcutCreatorWorker.requestUpdateShortcuts(mContext)
        }

        return Result.success()
    }
}