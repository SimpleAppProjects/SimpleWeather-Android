package com.thewizrd.simpleweather.services

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.*
import androidx.work.multiprocess.RemoteWorkManager
import com.thewizrd.common.helpers.locationPermissionEnabled
import com.thewizrd.common.location.LocationProvider
import com.thewizrd.common.utils.LiveDataUtils.awaitWithTimeout
import com.thewizrd.common.weatherdata.WeatherDataLoader
import com.thewizrd.common.weatherdata.WeatherRequest
import com.thewizrd.shared_resources.appLib
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.exceptions.WeatherException
import com.thewizrd.shared_resources.locationdata.toLocationData
import com.thewizrd.shared_resources.remoteconfig.remoteConfigService
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.notifications.PoPChanceNotificationHelper
import com.thewizrd.simpleweather.notifications.WeatherNotificationWorker
import com.thewizrd.simpleweather.services.ServiceNotificationHelper.NOT_CHANNEL_ID
import com.thewizrd.simpleweather.services.ServiceNotificationHelper.initChannel
import com.thewizrd.simpleweather.shortcuts.ShortcutCreatorWorker
import com.thewizrd.simpleweather.utils.PowerUtils
import com.thewizrd.simpleweather.weatheralerts.WeatherAlertHandler
import com.thewizrd.simpleweather.widgets.WidgetUpdaterHelper
import com.thewizrd.simpleweather.widgets.WidgetUtils
import com.thewizrd.weather_api.weatherModule
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.absoluteValue

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

            if (!PowerUtils.useForegroundService) {
                // Enqueue periodic task as well
                enqueueWork(context.applicationContext)
            }
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
            RemoteWorkManager.getInstance(context).cancelUniqueWork(TAG)
            Logger.writeLine(Log.INFO, "%s: Canceled work", TAG)
        }

        private fun getForegroundNotification(context: Context): Notification {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                initChannel(context)
            }

            return NotificationCompat.Builder(context, NOT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_logo_stroke)
                .setContentTitle(context.getString(R.string.not_title_weather_update))
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .setOnlyAlertOnce(true)
                .setNotificationSilent()
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                JOB_ID, getForegroundNotification(applicationContext),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            ForegroundInfo(JOB_ID, getForegroundNotification(applicationContext))
        }
    }

    override suspend fun doWork(): Result {
        Logger.writeLine(Log.INFO, "%s: Work started", TAG)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            runCatching {
                setForeground(getForegroundInfo())
            }
        }

        if (!WeatherUpdaterHelper.executeWork(applicationContext))
            return Result.failure()

        return Result.success()
    }

    private object WeatherUpdaterHelper {
        suspend fun executeWork(context: Context): Boolean {
            val wm = weatherModule.weatherManager
            var locationChanged = false

            runCatching {
                // Update configuration
                remoteConfigService.checkConfigAsync()
            }

            if (settingsManager.isWeatherLoaded()) {
                if (settingsManager.useFollowGPS()) {
                    try {
                        locationChanged = updateLocation()
                        Timber.tag(TAG).i("locationChanged = $locationChanged...")
                    } catch (e: CancellationException) {
                        // ignore
                    } catch (e: Exception) {
                        Logger.writeLine(Log.ERROR, e, "Error updating location")
                    }
                }

                // Refresh weather data for widgets
                preloadWeather()

                val weather = getWeather()

                if (WidgetUpdaterHelper.widgetsExist()) {
                    WidgetUpdaterHelper.refreshWidgets(context)
                }

                if (weather != null) {
                    if (settingsManager.showOngoingNotification()) {
                        WeatherNotificationWorker.refreshNotification(context)
                    }

                    if (settingsManager.useAlerts() && wm.supportsAlerts()) {
                        WeatherAlertHandler.postAlerts(settingsManager.getHomeData()!!, weather.weatherAlerts)
                    }

                    if (settingsManager.isPoPChanceNotificationEnabled()) {
                        PoPChanceNotificationHelper.postNotification(context)
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                        ShortcutCreatorWorker.updateShortcuts(context)
                    }

                    // Update data for Wearables
                    if (locationChanged) {
                        LocalBroadcastManager.getInstance(context)
                                .sendBroadcast(Intent(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE)
                                        .putExtra(CommonActions.EXTRA_FORCEUPDATE, false))
                    }
                    LocalBroadcastManager.getInstance(context)
                            .sendBroadcast(Intent(CommonActions.ACTION_WEATHER_SENDWEATHERUPDATE))
                } else {
                    Timber.tag(TAG).i("Work failed...")
                    return false
                }
            }

            Timber.tag(TAG).i("Work completed successfully...")
            return true
        }

        // Re-schedule alarm at selected interval from now
        private suspend fun getWeather(): Weather? = withContext(Dispatchers.IO) {
            Timber.tag(TAG).d("Getting weather data for home...")

            val weather = try {
                val locData = settingsManager.getHomeData() ?: return@withContext null
                WeatherDataLoader(locData)
                    .loadWeatherData(
                        WeatherRequest.Builder()
                            .forceRefresh(false)
                            .loadAlerts()
                            .loadForecasts()
                            .build()
                    )
            } catch (ex: Exception) {
                Logger.writeLine(Log.ERROR, ex, "%s: getWeather error", TAG)
                null
            }
            weather
        }

        private suspend fun preloadWeather() = withContext(Dispatchers.IO) {
            val locations = settingsManager.getFavorites() ?: emptyList()

            Timber.tag(TAG).d("Preloading weather data for favorites...")

            for (location in locations) {
                if (WidgetUtils.exists(location.query)) {
                    try {
                        WeatherDataLoader(location)
                            .loadWeatherData(
                                WeatherRequest.Builder()
                                    .forceRefresh(false)
                                    .loadAlerts()
                                    .build()
                            )
                    } catch (ex: Exception) {
                        Logger.writeLine(Log.ERROR, ex, "%s: preloadWeather error", TAG)
                    }
                }
            }
        }

        @SuppressLint("MissingPermission")
        private suspend fun updateLocation(): Boolean = withContext(Dispatchers.Default) {
            val context = appLib.context
            val wm = weatherModule.weatherManager
            val locationProvider = LocationProvider(context)

            if (settingsManager.useFollowGPS()) {
                if (!context.locationPermissionEnabled()) {
                    return@withContext false
                }

                val locMan = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
                if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
                    return@withContext false
                }

                var location: Location? = try {
                    withTimeoutOrNull(10 * 1000) {
                        locationProvider.getLastLocation()
                    }
                } catch (e: Exception) {
                    Logger.writeLine(Log.ERROR, e)
                    null
                }

                if (location == null) {
                    location = withTimeoutOrNull(60000) {
                        locationProvider.getCurrentLocation()
                    }
                }

                if (location != null) {
                    val lastGPSLocData = settingsManager.getLastGPSLocData()

                    // Check previous location difference
                    if (lastGPSLocData?.isValid == true && ConversionMethods.calculateHaversine(
                            lastGPSLocData.latitude,
                            lastGPSLocData.longitude,
                            location.latitude,
                            location.longitude
                        ).absoluteValue < 1600
                    ) {
                        return@withContext false
                    }

                    val query_vm = try {
                        withContext(Dispatchers.IO) {
                            wm.getLocation(location)
                        }
                    } catch (e: WeatherException) {
                        Logger.writeLine(Log.ERROR, e)
                        return@withContext false
                    }

                    if (query_vm == null || query_vm.locationQuery.isNullOrBlank()) {
                        // Stop since there is no valid query
                        return@withContext false
                    } else if (query_vm.locationTZLong.isNullOrBlank() && query_vm.locationLat != 0.0 && query_vm.locationLong != 0.0) {
                        val tzId =
                            weatherModule.tzdbService.getTimeZone(
                                query_vm.locationLat,
                                query_vm.locationLong
                            )

                        if ("unknown" != tzId) {
                            query_vm.locationTZLong = tzId
                        }
                    }

                    // Save location as last known
                    settingsManager.saveLastGPSLocData(query_vm.toLocationData(location))
                    return@withContext true
                }
            }
            false
        }
    }
}