package com.thewizrd.simpleweather.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.postDelayed
import androidx.work.*
import com.google.android.gms.location.*
import com.thewizrd.shared_resources.location.LocationProvider
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.remoteconfig.RemoteConfig
import com.thewizrd.shared_resources.tzdb.TZDBCache
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.wearable.WearableDataSync
import com.thewizrd.shared_resources.weatherdata.WeatherDataLoader
import com.thewizrd.shared_resources.weatherdata.WeatherManager
import com.thewizrd.shared_resources.weatherdata.WeatherRequest
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.App
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.wearable.WearableWorker
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.concurrent.*
import kotlin.coroutines.resume
import kotlin.math.absoluteValue

class WeatherUpdaterWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    companion object {
        private const val TAG = "WeatherUpdaterWorker"

        const val ACTION_UPDATEWEATHER = "SimpleWeather.Droid.Wear.action.UPDATE_WEATHER"
        const val ACTION_STARTALARM = "SimpleWeather.Droid.Wear.action.START_ALARM"
        const val ACTION_CANCELALARM = "SimpleWeather.Droid.Wear.action.CANCEL_ALARM"
        const val ACTION_UPDATEALARM = "SimpleWeather.Droid.Wear.action.UPDATE_ALARM"

        private const val JOB_ID = 1000
        private const val NOT_CHANNEL_ID = "SimpleWeather.generalnotif"

        @JvmStatic
        fun enqueueAction(context: Context, intentAction: String) {
            when (intentAction) {
                ACTION_UPDATEALARM -> enqueueWork(context.applicationContext)
                ACTION_UPDATEWEATHER, ACTION_STARTALARM ->
                    // For immediate action
                    startWork(context.applicationContext)
                ACTION_CANCELALARM -> cancelWork(context.applicationContext)
            }
        }

        private fun startWork(context: Context) {
            Logger.writeLine(Log.INFO, "%s: Requesting to start work", TAG)
            val updateRequest = OneTimeWorkRequest.Builder(WeatherUpdaterWorker::class.java)
                    .setInitialDelay(60, TimeUnit.SECONDS)
                    .build()
            WorkManager.getInstance(context.applicationContext)
                    .enqueueUniqueWork(TAG + "_onBoot", ExistingWorkPolicy.APPEND_OR_REPLACE, updateRequest)
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
            val updateRequest = PeriodicWorkRequest.Builder(WeatherUpdaterWorker::class.java, SettingsManager.DEFAULTINTERVAL.toLong(), TimeUnit.MINUTES, 30, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .addTag(TAG)
                    .build()
            WorkManager.getInstance(context.applicationContext)
                    .enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.REPLACE, updateRequest)
            Logger.writeLine(Log.INFO, "%s: Work enqueued", TAG)
        }

        private fun cancelWork(context: Context): Boolean {
            // Cancel alarm if dependent features are turned off
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(TAG)
            Logger.writeLine(Log.INFO, "%s: Canceled work", TAG)
            return true
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun initChannel(context: Context) {
            // Gets an instance of the NotificationManager service
            val mNotifyMgr = context.getSystemService(NotificationManager::class.java)
            var mChannel = mNotifyMgr.getNotificationChannel(NOT_CHANNEL_ID)
            val notchannel_name = context.resources.getString(R.string.not_channel_name_general)
            if (mChannel == null) {
                mChannel = NotificationChannel(NOT_CHANNEL_ID, notchannel_name, NotificationManager.IMPORTANCE_LOW)
            }

            // Configure the notification channel.
            mChannel.name = notchannel_name
            mChannel.setShowBadge(false)
            mChannel.enableLights(false)
            mChannel.enableVibration(false)
            mNotifyMgr.createNotificationChannel(mChannel)
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun getForegroundNotification(context: Context): Notification {
            initChannel(context)
            val mBuilder = NotificationCompat.Builder(context, NOT_CHANNEL_ID)
                    .setSmallIcon(R.drawable.wi_day_cloudy)
                    .setContentTitle(context.getString(R.string.not_title_weather_update))
                    .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                    .setOnlyAlertOnce(true)
                    .setNotificationSilent()
                    .setPriority(NotificationCompat.PRIORITY_LOW)
            return mBuilder.build()
        }
    }

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.Default) {
            Logger.writeLine(Log.INFO, "%s: Work started", TAG)
            val context = applicationContext

            // Request work to be in foreground (only for Oreo+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setForeground(ForegroundInfo(JOB_ID, getForegroundNotification(context),
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION))
                } else {
                    setForeground(ForegroundInfo(JOB_ID, getForegroundNotification(context)))
                }
            }

            if (!WeatherUpdaterHelper.executeWork(context))
                return@withContext Result.failure()

            Result.success()
        }
    }

    private object WeatherUpdaterHelper {
        suspend fun executeWork(context: Context): Boolean {
            val wm = WeatherManager.instance
            val settingsManager = App.instance.settingsManager

            if (settingsManager.getDataSync() == WearableDataSync.OFF) {
                runCatching {
                    // Update configuration
                    RemoteConfig.checkConfigAsync()
                }
            }

            if (settingsManager.isWeatherLoaded()) {
                if (settingsManager.getDataSync() == WearableDataSync.OFF && settingsManager.useFollowGPS()) {
                    try {
                        updateLocation()
                    } catch (e: Exception) {
                        Logger.writeLine(Log.ERROR, e)
                    }
                }

                // Update for home
                val weather = getWeather()

                if (weather != null) {
                    WidgetUpdaterWorker.requestWidgetUpdate(context)
                } else {
                    if (settingsManager.getDataSync() != WearableDataSync.OFF) {
                        // Check if data has been updated
                        WearableWorker.enqueueAction(context, WearableWorker.ACTION_REQUESTWEATHERUPDATE)
                    }
                    Timber.tag(TAG).i("Work failed...")
                    return false
                }
            }

            Timber.tag(TAG).i("Work completed successfully...")
            return true
        }

        private suspend fun getWeather(): Weather? = withContext(Dispatchers.IO) {
            val settingsManager = App.instance.settingsManager
            val weather = try {
                val locData = settingsManager.getHomeData() ?: return@withContext null
                val wloader = WeatherDataLoader(locData)
                val request = WeatherRequest.Builder()
                if (settingsManager.getDataSync() == WearableDataSync.OFF) {
                    request.forceRefresh(false).loadAlerts()
                } else {
                    request.forceLoadSavedData()
                }
                wloader.loadWeatherData(request.build())
            } catch (ex: Exception) {
                Logger.writeLine(Log.ERROR, ex, "%s: GetWeather error", TAG)
                null
            }
            weather
        }

        @SuppressLint("MissingPermission")
        private suspend fun updateLocation(): Boolean = withContext(Dispatchers.Default) {
            val context = App.instance.appContext
            val wm = WeatherManager.instance
            val settingsManager = App.instance.settingsManager
            val locationProvider = LocationProvider(context)

            if (settingsManager.useFollowGPS()) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return@withContext false
                }

                val locMan = context.getSystemService(LocationManager::class.java)
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
                    if (!isActive) return@withContext false

                    val handlerThread = HandlerThread("location")
                    handlerThread.start()
                    // Handler for timeout callback
                    val handler = Handler(handlerThread.looper)

                    Timber.tag(TAG).i("Requesting location updates...")

                    location = suspendCancellableCoroutine { continuation ->
                        val locationCallback = object : LocationProvider.Callback {
                            override fun onLocationChanged(location: Location?) {
                                handler.removeCallbacksAndMessages(null)
                                locationProvider.stopLocationUpdates()

                                Timber.tag(TAG).i("Location update received...")
                                if (continuation.isActive) {
                                    continuation.resume(location)
                                }
                                handlerThread.quitSafely()
                            }

                            override fun onRequestTimedOut() {
                                Timber.tag(TAG).i("Location update timed out...")
                                continuation.cancel()
                            }
                        }

                        continuation.invokeOnCancellation {
                            locationProvider.stopLocationUpdates()
                            handler.removeCallbacksAndMessages(null)
                            handlerThread.quitSafely()
                        }

                        // Timeout after 60s
                        locationProvider.requestSingleUpdate(
                            locationCallback,
                            handlerThread.looper,
                            60000
                        )
                    }
                }

                if (location != null) {
                    val lastGPSLocData = settingsManager.getLastGPSLocData()

                    // Check previous location difference
                    if (lastGPSLocData?.query != null && ConversionMethods.calculateHaversine(
                                    lastGPSLocData.latitude, lastGPSLocData.longitude, location.latitude, location.longitude).absoluteValue < 1600) {
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

                    if (query_vm == null || StringUtils.isNullOrWhitespace(query_vm.locationQuery)) {
                        // Stop since there is no valid query
                        return@withContext false
                    } else if (query_vm.locationTZLong?.isNotBlank() == true && query_vm.locationLat != 0.0 && query_vm.locationLong != 0.0) {
                        val tzId = TZDBCache.getTimeZone(query_vm.locationLat, query_vm.locationLong)

                        if ("unknown" != tzId) {
                            query_vm.locationTZLong = tzId
                        }
                    }

                    // Save location as last known
                    settingsManager.saveLastGPSLocData(LocationData(query_vm, location))
                    return@withContext true
                }
            }
            false
        }
    }
}