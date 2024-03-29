package com.thewizrd.simpleweather.services

import android.content.Context
import android.content.pm.ServiceInfo
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.core.location.LocationManagerCompat
import androidx.work.*
import com.thewizrd.common.helpers.locationPermissionEnabled
import com.thewizrd.common.location.LocationProvider
import com.thewizrd.common.location.LocationResult
import com.thewizrd.common.utils.ErrorMessage
import com.thewizrd.common.utils.LiveDataUtils.awaitWithTimeout
import com.thewizrd.common.weatherdata.WeatherDataLoader
import com.thewizrd.common.weatherdata.WeatherRequest
import com.thewizrd.shared_resources.appLib
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.preferences.SettingsManager
import com.thewizrd.shared_resources.remoteconfig.remoteConfigService
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.wearable.WearableDataSync
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.services.ServiceNotificationHelper.JOB_ID
import com.thewizrd.simpleweather.services.ServiceNotificationHelper.getForegroundNotification
import com.thewizrd.simpleweather.services.ServiceNotificationHelper.initChannel
import com.thewizrd.simpleweather.wearable.WearableWorker
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

class WeatherUpdaterWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    companion object {
        private const val TAG = "WeatherUpdaterWorker"

        const val ACTION_UPDATEWEATHER = "SimpleWeather.Droid.Wear.action.UPDATE_WEATHER"
        const val ACTION_ENQUEUEWORK = "SimpleWeather.Droid.Wear.action.START_ALARM"
        const val ACTION_CANCELWORK = "SimpleWeather.Droid.Wear.action.CANCEL_ALARM"
        const val ACTION_REQUEUEWORK = "SimpleWeather.Droid.Wear.action.UPDATE_ALARM"

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

            val updateRequest = OneTimeWorkRequest.Builder(WeatherUpdaterWorker::class.java).apply {
                setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            }.build()

            WorkManager.getInstance(context.applicationContext).enqueue(updateRequest)

            Logger.writeLine(Log.INFO, "%s: One-time work enqueued", TAG)

            // Enqueue periodic task as well
            enqueueWork(context.applicationContext)
        }

        private fun enqueueWork(context: Context) {
            Logger.writeLine(Log.INFO, "%s: Requesting work", TAG)

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresCharging(false)
                .build()

            val updateRequest = PeriodicWorkRequest.Builder(
                WeatherUpdaterWorker::class.java,
                SettingsManager.DEFAULT_INTERVAL.toLong(),
                TimeUnit.MINUTES,
                5,
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
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

        private fun cancelWork(context: Context): Boolean {
            // Cancel alarm if dependent features are turned off
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(TAG)
            Logger.writeLine(Log.INFO, "%s: Canceled work", TAG)
            return true
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        initChannel(applicationContext)

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
            var locationChanged = false

            if (settingsManager.getDataSync() == WearableDataSync.OFF) {
                runCatching {
                    // Update configuration
                    remoteConfigService.checkConfigAsync()
                }
            }

            if (settingsManager.isWeatherLoaded()) {
                if (settingsManager.getDataSync() == WearableDataSync.OFF && settingsManager.useFollowGPS()) {
                    try {
                        val result = updateLocation()

                        when (result) {
                            is LocationResult.Changed -> {
                                locationChanged = true
                                settingsManager.saveLastGPSLocData(result.data)
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

                // Update for home
                val weather = getWeather()

                WidgetUpdaterWorker.requestWidgetUpdate(context)

                if (weather == null) {
                    if (settingsManager.getDataSync() != WearableDataSync.OFF) {
                        // Check if data has been updated
                        WearableWorker.enqueueAction(
                            context,
                            WearableWorker.ACTION_REQUESTWEATHERUPDATE
                        )
                    }
                    Timber.tag(TAG).i("Work failed...")
                    return false
                }
            }

            Timber.tag(TAG).i("Work completed successfully...")
            return true
        }

        private suspend fun getWeather(): Weather? = withContext(Dispatchers.IO) {
            Timber.tag(TAG).d("Getting weather data...")

            val weather = try {
                val locData = settingsManager.getHomeData() ?: return@withContext null
                WeatherDataLoader(locData)
                    .loadWeatherData(
                        WeatherRequest.Builder()
                            .run {
                                if (settingsManager.getDataSync() == WearableDataSync.OFF) {
                                    this.forceRefresh(false)
                                        .loadAlerts()
                                } else {
                                    this.forceLoadSavedData()
                                }
                            }
                            .build()
                    )
            } catch (ex: Exception) {
                Logger.writeLine(Log.ERROR, ex, "%s: getWeather error", TAG)
                null
            }
            weather
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
                    return LocationResult.Error(errorMessage = ErrorMessage.Resource(R.string.error_retrieve_location))
                }

                return locationProvider.getLatestLocationData(settingsManager.getLastGPSLocData())
            }

            return LocationResult.NotChanged(null)
        }
    }
}