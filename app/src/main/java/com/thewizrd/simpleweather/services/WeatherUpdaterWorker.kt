package com.thewizrd.simpleweather.services

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.multiprocess.RemoteWorkManager
import com.thewizrd.common.helpers.locationPermissionEnabled
import com.thewizrd.common.helpers.notificationPermissionEnabled
import com.thewizrd.common.location.LocationProvider
import com.thewizrd.common.location.LocationResult
import com.thewizrd.common.utils.ErrorMessage
import com.thewizrd.common.utils.LiveDataUtils.awaitWithTimeout
import com.thewizrd.common.weatherdata.WeatherDataLoader
import com.thewizrd.common.weatherdata.WeatherRequest
import com.thewizrd.common.weatherdata.WeatherResult
import com.thewizrd.shared_resources.R as sharedRes
import com.thewizrd.shared_resources.appLib
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.preferences.SettingsManager
import com.thewizrd.shared_resources.remoteconfig.remoteConfigService
import com.thewizrd.shared_resources.utils.CommonActions
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.notifications.PoPChanceNotificationHelper
import com.thewizrd.simpleweather.notifications.WeatherNotificationWorker
import com.thewizrd.simpleweather.services.ServiceNotificationHelper.NOT_CHANNEL_ID
import com.thewizrd.simpleweather.services.ServiceNotificationHelper.initChannel
import com.thewizrd.simpleweather.shortcuts.ShortcutCreatorWorker
import com.thewizrd.simpleweather.weatheralerts.WeatherAlertHandler
import com.thewizrd.simpleweather.widgets.WidgetUpdaterHelper
import com.thewizrd.simpleweather.widgets.WidgetUtils
import com.thewizrd.weather_api.weatherModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

class WeatherUpdaterWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    companion object {
        private const val TAG = "WeatherUpdaterWorker"

        const val ACTION_UPDATEWEATHER = "SimpleWeather.Droid.action.UPDATE_WEATHER"
        const val ACTION_ENQUEUEWORK = "SimpleWeather.Droid.action.START_ALARM"
        const val ACTION_CANCELWORK = "SimpleWeather.Droid.action.CANCEL_ALARM"
        const val ACTION_REQUEUEWORK = "SimpleWeather.Droid.action.UPDATE_ALARM"

        private const val JOB_ID = 1004

        suspend fun executeWork(context: Context) {
            WeatherUpdaterHelper.executeWork(context.applicationContext)
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
                ACTION_UPDATEWEATHER ->
                    // For immediate action
                    startWork(context.applicationContext)
                ACTION_CANCELWORK -> cancelWork(context.applicationContext)
            }
        }

        private fun startWork(context: Context) {
            Logger.writeLine(Log.INFO, "%s: Requesting to start work", TAG)

            val updateRequest = OneTimeWorkRequestBuilder<WeatherUpdaterWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            RemoteWorkManager.getInstance(context).enqueue(updateRequest)

            Logger.writeLine(Log.INFO, "%s: One-time work enqueued", TAG)

            // Enqueue periodic task as well
            enqueueWork(context.applicationContext)
        }

        private fun enqueueWork(context: Context) {
            val settingsManager = SettingsManager(context.applicationContext)
            Logger.writeLine(Log.INFO, "%s: Requesting work", TAG)

            val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresCharging(false)
                    .build()

            val updateRequest = PeriodicWorkRequest.Builder(
                WeatherUpdaterWorker::class.java,
                settingsManager.getRefreshInterval().toLong(),
                TimeUnit.MINUTES,
                5,
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .addTag(TAG)
                .build()

            RemoteWorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.UPDATE, updateRequest)

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
            RemoteWorkManager.getInstance(context).cancelUniqueWork(TAG)
            Logger.writeLine(Log.INFO, "%s: Canceled work", TAG)
        }

        private fun getForegroundNotification(context: Context): Notification {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                initChannel(context)
            }

            return NotificationCompat.Builder(context, NOT_CHANNEL_ID)
                .setSmallIcon(sharedRes.drawable.ic_launcher_monochrome)
                .setContentTitle(context.getString(sharedRes.string.not_title_weather_update))
                .setColor(ContextCompat.getColor(context, sharedRes.color.colorPrimary))
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(JOB_ID, getForegroundNotification(applicationContext))
    }

    override suspend fun doWork(): Result {
        Logger.writeLine(Log.INFO, "%s: Work started", TAG)
        return WeatherUpdaterHelper.executeWork(applicationContext)
    }

    private object WeatherUpdaterHelper {
        @SuppressLint("MissingPermission")
        suspend fun executeWork(context: Context): Result {
            var result = Result.success()

            val wm = weatherModule.weatherManager
            var locationChanged = false

            runCatching {
                // Update configuration
                remoteConfigService.checkConfigAsync()
            }

            if (settingsManager.isWeatherLoaded()) {
                if (settingsManager.useFollowGPS()) {
                    try {
                        val locationResult = updateLocation()

                        when (locationResult) {
                            is LocationResult.Changed -> {
                                locationChanged = true
                                settingsManager.saveLastGPSLocData(locationResult.data)
                            }
                            else -> {
                                // no-op
                            }
                        }

                        Timber.tag(TAG).i("locationChanged = $locationChanged...")
                    } catch (e: CancellationException) {
                        // ignore
                    } catch (e: Exception) {
                        Logger.writeLine(Log.ERROR, e, "Error updating location")
                    }
                }

                // Refresh weather data for widgets
                preloadWeather()

                val weatherResult = getWeather()

                result = when (weatherResult) {
                    is WeatherResult.Success -> Result.success()
                    is WeatherResult.NoWeather -> Result.failure()
                    is WeatherResult.Error,
                    is WeatherResult.WeatherWithError -> Result.retry()
                }

                val willRetry = result == Result.retry()

                if (WidgetUpdaterHelper.widgetsExist()) {
                    WidgetUpdaterHelper.refreshWidgets(context, resetIfUnavailable = !willRetry)
                }

                if (settingsManager.showOngoingNotification()) {
                    WeatherNotificationWorker.refreshNotification(
                        context,
                        resetIfUnavailable = !willRetry
                    )
                }

                if (settingsManager.isPoPChanceNotificationEnabled()) {
                    PoPChanceNotificationHelper.postNotification(context)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                    ShortcutCreatorWorker.updateShortcuts(context)
                }

                if (weatherResult is WeatherResult.Success || weatherResult is WeatherResult.WeatherWithError) {
                    if (settingsManager.useAlerts() && wm.supportsAlerts()) {
                        WeatherAlertHandler.postAlerts(
                            settingsManager.getHomeData()!!,
                            weatherResult.data?.weatherAlerts
                        )
                    }

                    // Update data for Wearables
                    if (locationChanged) {
                        LocalBroadcastManager.getInstance(context)
                            .sendBroadcast(
                                Intent(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE)
                                    .putExtra(CommonActions.EXTRA_FORCEUPDATE, false)
                            )
                    }
                    LocalBroadcastManager.getInstance(context)
                        .sendBroadcast(Intent(CommonActions.ACTION_WEATHER_SENDWEATHERUPDATE))
                }
            }

            when (result) {
                Result.success() -> {
                    Timber.tag(TAG).i("Work completed successfully...")
                }

                Result.retry() -> {
                    Timber.tag(TAG).w("Work failed. Will retry...")
                }

                Result.failure() -> {
                    Timber.tag(TAG).e("Work failed...")
                }
            }

            return result
        }

        // Re-schedule alarm at selected interval from now
        private suspend fun getWeather(): WeatherResult = withContext(Dispatchers.IO) {
            Timber.tag(TAG).d("Getting weather data for home...")

            val locData =
                settingsManager.getHomeData() ?: return@withContext WeatherResult.NoWeather()

            WeatherDataLoader(locData)
                .loadWeatherResult(
                    WeatherRequest.Builder()
                        .forceRefresh(false)
                        .loadAlerts()
                        .loadForecasts()
                        .build()
                )
        }

        private suspend fun preloadWeather() = withContext(Dispatchers.IO) {
            val results = mutableListOf<Boolean>()

            val locations = settingsManager.getFavorites() ?: emptyList()

            Timber.tag(TAG).d("Preloading weather data for favorites...")

            for (location in locations) {
                if (WidgetUtils.exists(location.query)) {
                    val result = WeatherDataLoader(location)
                        .loadWeatherResult(
                            WeatherRequest.Builder()
                                .forceRefresh(false)
                                .loadAlerts()
                                .build()
                        )

                    if (result is WeatherResult.Error) {
                        Logger.error(TAG, result.exception, "preloadWeather error")
                    } else if (result is WeatherResult.WeatherWithError) {
                        Logger.error(TAG, result.exception, "preloadWeather error")
                    }

                    results.add(result is WeatherResult.Success || result is WeatherResult.WeatherWithError)
                }
            }

            results.all { it }
        }

        private suspend fun updateLocation(): LocationResult {
            val context = appLib.context
            val locationProvider = LocationProvider(context)

            if (settingsManager.useFollowGPS()) {
                if (!context.locationPermissionEnabled()) {
                    return LocationResult.PermissionDenied()
                }

                val locMan = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
                if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
                    return LocationResult.Error(errorMessage = ErrorMessage.Resource(sharedRes.string.error_retrieve_location))
                }

                return locationProvider.getLatestLocationData(settingsManager.getLastGPSLocData())
            }

            return LocationResult.NotChanged(null)
        }
    }
}