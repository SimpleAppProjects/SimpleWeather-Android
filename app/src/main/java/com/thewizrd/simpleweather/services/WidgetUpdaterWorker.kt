package com.thewizrd.simpleweather.services

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.*
import androidx.work.multiprocess.RemoteWorkManager
import com.thewizrd.common.utils.LiveDataUtils.awaitWithTimeout
import com.thewizrd.common.wearable.WearableSettings
import com.thewizrd.common.weatherdata.WeatherDataLoader
import com.thewizrd.common.weatherdata.WeatherRequest
import com.thewizrd.common.weatherdata.WeatherResult
import com.thewizrd.shared_resources.appLib
import com.thewizrd.shared_resources.di.localBroadcastManager
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.exceptions.ErrorStatus
import com.thewizrd.shared_resources.exceptions.WeatherException
import com.thewizrd.shared_resources.preferences.SettingsManager
import com.thewizrd.shared_resources.utils.CommonActions
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.simpleweather.notifications.PoPChanceNotificationHelper
import com.thewizrd.simpleweather.notifications.WeatherNotificationWorker
import com.thewizrd.simpleweather.shortcuts.ShortcutCreatorWorker
import com.thewizrd.simpleweather.utils.PowerUtils
import com.thewizrd.simpleweather.widgets.WidgetUpdaterHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        @JvmOverloads
        fun enqueueAction(context: Context, intentAction: String, onBoot: Boolean = false) {
            when (intentAction) {
                ACTION_REQUEUEWORK -> enqueueWork(context.applicationContext)
                ACTION_ENQUEUEWORK ->
                    appLib.appScope.launch(Dispatchers.Default) {
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

            RemoteWorkManager.getInstance(context).enqueue(updateRequest)

            Logger.writeLine(Log.INFO, "%s: One-time work enqueued", TAG)

            // Enqueue periodic task as well
            if (!PowerUtils.useForegroundService) {
                enqueueWork(context)
            }
        }

        private fun enqueueWork(context: Context) {
            Logger.writeLine(Log.INFO, "%s: Requesting work")

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

            RemoteWorkManager.getInstance(context)
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
            RemoteWorkManager.getInstance(context.applicationContext).cancelUniqueWork(TAG)
            Logger.writeLine(Log.INFO, "%s: Canceled work", TAG)
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ServiceNotificationHelper.initChannel(applicationContext)
        }

        return ForegroundInfo(
            ServiceNotificationHelper.JOB_ID,
            ServiceNotificationHelper.createForegroundNotification(applicationContext)
        )
    }

    override suspend fun doWork(): Result {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            runCatching {
                setForeground(getForegroundInfo())
            }
        }

        WidgetUpdaterWork.executeWork(applicationContext)
        return Result.success()
    }

    private object WidgetUpdaterWork {
        suspend fun executeWork(context: Context) {
            val settingsManager = SettingsManager(context.applicationContext)

            if (settingsManager.isWeatherLoaded()) {
                // If saved data DNE (for current location), refresh weather
                val result = loadWeather()
                if (result !is WeatherResult.Success && result !is WeatherResult.WeatherWithError) {
                    if (loadWeather(true).let { it is WeatherResult.Success && !it.isSavedData }) {
                        localBroadcastManager.sendBroadcast(
                            Intent(CommonActions.ACTION_WEATHER_SENDWEATHERUPDATE).apply {
                                putExtra(WearableSettings.KEY_PARTIAL_WEATHER_UPDATE, true)
                            }
                        )
                    }
                }

                if (WidgetUpdaterHelper.widgetsExist()) {
                    WidgetUpdaterHelper.refreshWidgets(context)
                }

                if (settingsManager.showOngoingNotification()) {
                    WeatherNotificationWorker.refreshNotification(context)
                }

                if (settingsManager.isPoPChanceNotificationEnabled()) {
                    PoPChanceNotificationHelper.postNotification(context)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                    ShortcutCreatorWorker.updateShortcuts(context)
                }
            }

            Timber.tag(TAG).i("Work completed successfully...")
        }

        private suspend fun loadWeather(forceRefresh: Boolean = false): WeatherResult =
            withContext(Dispatchers.IO) {
                Timber.tag(TAG).d("Getting weather data for home...")

                val locData =
                    settingsManager.getHomeData() ?: return@withContext WeatherResult.Error(
                        WeatherException(ErrorStatus.NOWEATHER)
                    )

                WeatherDataLoader(locData)
                    .loadWeatherResult(
                        WeatherRequest.Builder()
                            .run {
                                if (forceRefresh) {
                                    this.forceRefresh(false)
                                        .loadAlerts()
                                        .loadForecasts()
                                } else {
                                    this.forceLoadSavedData()
                                }
                            }
                            .build()
                    )
            }
    }
}