package com.thewizrd.simpleweather.services

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.postDelayed
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.*
import com.google.android.gms.location.*
import com.thewizrd.shared_resources.AppState
import com.thewizrd.shared_resources.remoteconfig.RemoteConfig
import com.thewizrd.shared_resources.tzdb.TZDBCache
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.wearable.WearableHelper
import com.thewizrd.shared_resources.weatherdata.Weather
import com.thewizrd.shared_resources.weatherdata.WeatherDataLoader
import com.thewizrd.shared_resources.weatherdata.WeatherManager
import com.thewizrd.shared_resources.weatherdata.WeatherRequest
import com.thewizrd.simpleweather.App
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.notifications.WeatherNotificationWorker
import com.thewizrd.simpleweather.services.ServiceNotificationHelper.NOT_CHANNEL_ID
import com.thewizrd.simpleweather.services.ServiceNotificationHelper.initChannel
import com.thewizrd.simpleweather.shortcuts.ShortcutCreatorWorker
import com.thewizrd.simpleweather.utils.PowerUtils
import com.thewizrd.simpleweather.weatheralerts.WeatherAlertHandler
import com.thewizrd.simpleweather.widgets.WidgetUpdaterHelper
import com.thewizrd.simpleweather.widgets.WidgetUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.concurrent.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
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
                    if (onBoot || !isWorkScheduled(context.applicationContext)) {
                        startWork(context.applicationContext)
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
                if (App.instance.appState != AppState.FOREGROUND) {
                    setInitialDelay(60, TimeUnit.SECONDS)
                }
            }

            WorkManager.getInstance(context)
                    .enqueueUniqueWork(TAG + "_onBoot", ExistingWorkPolicy.APPEND_OR_REPLACE, updateRequest.build())

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

            val updateRequest = PeriodicWorkRequest.Builder(WeatherUpdaterWorker::class.java, settingsManager.getRefreshInterval().toLong(), TimeUnit.MINUTES, 15, TimeUnit.MINUTES)
                    .setConstraints(constraints)
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
            WorkManager.getInstance(context).cancelUniqueWork(TAG)
            Logger.writeLine(Log.INFO, "%s: Canceled work", TAG)
        }

        @TargetApi(Build.VERSION_CODES.O)
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
        return withContext(Dispatchers.IO) {
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
            val wm = WeatherManager.getInstance()
            val settingsManager = App.instance.settingsManager

            try {
                // Update configuration
                RemoteConfig.checkConfigAsync()
            } catch (e: Exception) {
                Timber.tag(TAG).e(e)
            }

            if (settingsManager.isWeatherLoaded()) {
                if (settingsManager.useFollowGPS()) {
                    try {
                        updateLocation()
                    } catch (e: Exception) {
                        Logger.writeLine(Log.ERROR, e)
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

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                        ShortcutCreatorWorker.updateShortcuts(context)
                    }

                    // Update data for Wearables
                    LocalBroadcastManager.getInstance(context)
                            .sendBroadcast(Intent(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE)
                                    .putExtra(CommonActions.EXTRA_FORCEUPDATE, false))
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
            val settingsManager = App.instance.settingsManager
            val weather = try {
                WeatherDataLoader(settingsManager.getHomeData()!!)
                        .loadWeatherData(WeatherRequest.Builder()
                                .forceRefresh(false)
                                .loadAlerts()
                                .loadForecasts()
                                .build()
                        ).await()
            } catch (ex: Exception) {
                Logger.writeLine(Log.ERROR, ex, "%s: getWeather error", TAG)
                null
            }
            weather
        }

        private suspend fun preloadWeather() = withContext(Dispatchers.IO) {
            val settingsManager = App.instance.settingsManager
            val locations = settingsManager.getFavorites() ?: emptyList()

            for (location in locations) {
                if (WidgetUtils.exists(location.query)) {
                    try {
                        WeatherDataLoader(location)
                                .loadWeatherData(WeatherRequest.Builder()
                                        .forceRefresh(false)
                                        .loadAlerts()
                                        .build()
                                ).await()
                    } catch (ex: Exception) {
                        Logger.writeLine(Log.ERROR, ex, "%s: preloadWeather error", TAG)
                    }
                }
            }
        }

        @SuppressLint("MissingPermission")
        private suspend fun updateLocation(): Boolean = withContext(Dispatchers.Default) {
            val context = App.instance.appContext
            val wm = WeatherManager.getInstance()
            val settingsManager = App.instance.settingsManager
            var mFusedLocationClient: FusedLocationProviderClient? = null

            if (WearableHelper.isGooglePlayServicesInstalled()) {
                mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            }

            if (settingsManager.useFollowGPS()) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return@withContext false
                }

                var location: Location? = null
                val locMan = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
                if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
                    return@withContext false
                }

                if (WearableHelper.isGooglePlayServicesInstalled()) {
                    location = try {
                        withTimeoutOrNull(10 * 1000) {
                            mFusedLocationClient?.lastLocation?.await()
                        }
                    } catch (e: Exception) {
                        Logger.writeLine(Log.ERROR, e)
                        null
                    }

                    if (location == null) {
                        val mLocationRequest = LocationRequest.create().apply {
                            numUpdates = 1
                            interval = 10000
                            fastestInterval = 1000
                            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                        }

                        if (!isActive) return@withContext false

                        val handlerThread = HandlerThread("location")
                        handlerThread.start()
                        // Handler for timeout callback
                        val handler = Handler(handlerThread.looper)

                        Timber.tag(TAG).i("Fused: Requesting location updates...")

                        val locationResult = suspendCancellableCoroutine<LocationResult?> { continuation ->
                            val locationCallback = object : LocationCallback() {
                                override fun onLocationResult(locationResult: LocationResult) {
                                    handler.removeCallbacksAndMessages(null)
                                    mFusedLocationClient!!.removeLocationUpdates(this)

                                    Timber.tag(TAG).i("Fused: Location update received...")
                                    if (continuation.isActive) {
                                        continuation.resume(locationResult)
                                    }
                                    handlerThread.quitSafely()
                                }

                                override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                                    if (!locationAvailability.isLocationAvailable) {
                                        handler.removeCallbacksAndMessages(null)
                                        mFusedLocationClient!!.removeLocationUpdates(this)

                                        Timber.tag(TAG).i("Fused: Location update unavailable...")
                                        if (continuation.isActive) {
                                            continuation.resume(null)
                                        }
                                        handlerThread.quitSafely()
                                    }
                                }
                            }

                            continuation.invokeOnCancellation {
                                mFusedLocationClient?.removeLocationUpdates(locationCallback)
                                handler.removeCallbacksAndMessages(null)
                                handlerThread.quitSafely()
                            }

                            mFusedLocationClient!!.requestLocationUpdates(mLocationRequest, locationCallback, handlerThread.looper)

                            // Timeout after 60s
                            handler.postDelayed(60000) {
                                mFusedLocationClient?.removeLocationUpdates(locationCallback)
                                Timber.tag(TAG).i("Fused: Location update timed out...")
                                if (continuation.isActive) {
                                    continuation.resume(null)
                                }
                                handlerThread.quitSafely()
                            }
                        }

                        location = locationResult?.lastLocation
                    }
                } else {
                    val isGPSEnabled = locMan.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    val isNetEnabled = locMan.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                    if (isGPSEnabled || isNetEnabled) {
                        val locCriteria = Criteria().apply {
                            accuracy = Criteria.ACCURACY_COARSE
                            isCostAllowed = false
                            powerRequirement = Criteria.POWER_LOW
                        }

                        val provider = locMan.getBestProvider(locCriteria, true)!!
                        location = locMan.getLastKnownLocation(provider)

                        if (location == null) {
                            if (!isActive) return@withContext false

                            val handlerThread = HandlerThread("location")
                            handlerThread.start()
                            // Handler for timeout callback
                            val handler = Handler(handlerThread.looper)

                            Timber.tag(TAG).i("LocMan: Requesting location update...")

                            location = suspendCancellableCoroutine { continuation ->
                                val locationListener = object : LocationListener {
                                    override fun onLocationChanged(location: Location) {
                                        handler.removeCallbacksAndMessages(null)
                                        locMan.removeUpdates(this)

                                        Timber.tag(TAG).i("LocMan: Location update received...")

                                        if (continuation.isActive) {
                                            continuation.resume(location)
                                        }
                                        handlerThread.quitSafely()
                                    }

                                    override fun onStatusChanged(s: String, i: Int, bundle: Bundle) {}
                                    override fun onProviderEnabled(s: String) {}
                                    override fun onProviderDisabled(s: String) {}
                                }

                                continuation.invokeOnCancellation {
                                    handler.removeCallbacksAndMessages(null)
                                    locMan.removeUpdates(locationListener)
                                    handlerThread.quitSafely()
                                }

                                try {
                                    locMan.requestSingleUpdate(provider, locationListener, handlerThread.looper)
                                } catch (e: Exception) {
                                    locMan.removeUpdates(locationListener)
                                    if (continuation.isActive) {
                                        continuation.resumeWithException(e)
                                    }
                                    handlerThread.quitSafely()
                                }

                                // Timeout after 60s
                                handler.postDelayed(60000) {
                                    locMan.removeUpdates(locationListener)
                                    Timber.tag(TAG).i("LocMan: Location update timed out...")
                                    if (continuation.isActive) {
                                        continuation.resume(null)
                                    }
                                    handlerThread.quitSafely()
                                }
                            }
                        }
                    }
                }

                if (location != null) {
                    val lastGPSLocData = settingsManager.getLastGPSLocData()

                    // Check previous location difference
                    if (lastGPSLocData?.query != null && ConversionMethods.calculateHaversine(lastGPSLocData.latitude, lastGPSLocData.longitude, location.latitude, location.longitude).absoluteValue < 1600) {
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
                    lastGPSLocData!!.setData(query_vm, location)
                    settingsManager.saveLastGPSLocData(lastGPSLocData)
                    return@withContext true
                }
            }
            false
        }
    }
}