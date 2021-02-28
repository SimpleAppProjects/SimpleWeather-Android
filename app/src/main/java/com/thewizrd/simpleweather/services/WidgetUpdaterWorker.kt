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
import com.thewizrd.simpleweather.wearable.WearableWorker
import com.thewizrd.simpleweather.widgets.WeatherWidgetBroadcastReceiver
import com.thewizrd.simpleweather.widgets.WeatherWidgetService
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class WidgetUpdaterWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    private val mContext = context.applicationContext

    companion object {
        private const val TAG = "WidgetUpdaterWorker"

        const val ACTION_UPDATEWIDGETS = "SimpleWeather.Droid.action.UPDATE_WIDGETS"
        const val ACTION_STARTALARM = "SimpleWeather.Droid.action.START_ALARM"
        const val ACTION_CANCELALARM = "SimpleWeather.Droid.action.CANCEL_ALARM"
        const val ACTION_UPDATEALARM = "SimpleWeather.Droid.action.UPDATE_ALARM"

        private const val JOB_ID = 1005

        @JvmStatic
        fun enqueueAction(context: Context, intentAction: String) {
            val context = context.applicationContext
            when (intentAction) {
                ACTION_UPDATEALARM -> enqueueWork(context)
                ACTION_STARTALARM, ACTION_UPDATEWIDGETS ->
                    // For immediate action
                    startWork(context)
                ACTION_CANCELALARM -> cancelWork(context)
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

            // Enqueue periodic task as well
            enqueueWork(context)
        }

        private fun enqueueWork(context: Context) {
            val context = context.applicationContext
            Logger.writeLine(Log.INFO, "%s: Requesting work; workExists: %s", TAG, java.lang.Boolean.toString(isWorkScheduled(context)))
            val updateRequest = PeriodicWorkRequest.Builder(WidgetUpdaterWorker::class.java, 60, TimeUnit.MINUTES, 15, TimeUnit.MINUTES)
                    .setConstraints(Constraints.NONE)
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

        private fun cancelWork(context: Context): Boolean {
            // Cancel alarm if dependent features are turned off
            val context = context.applicationContext
            if (!WeatherWidgetService.widgetsExist(context) && !Settings.showOngoingNotification()) {
                WorkManager.getInstance(context).cancelUniqueWork(TAG)
                Logger.writeLine(Log.INFO, "%s: Canceled work", TAG)
                return true
            }
            return false
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

            // Update weather data for Wearables
            WearableWorker.enqueueAction(mContext, WearableWorker.ACTION_SENDWEATHERUPDATE, false)
        }

        return Result.success()
    }
}