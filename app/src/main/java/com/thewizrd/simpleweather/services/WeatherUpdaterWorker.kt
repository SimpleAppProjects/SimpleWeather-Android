package com.thewizrd.simpleweather.services

import android.Manifest
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
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
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
import com.thewizrd.simpleweather.notifications.WeatherNotificationBroadcastReceiver
import com.thewizrd.simpleweather.notifications.WeatherNotificationWorker
import com.thewizrd.simpleweather.utils.PowerUtils
import com.thewizrd.simpleweather.weatheralerts.WeatherAlertHandler
import com.thewizrd.simpleweather.widgets.WeatherWidgetBroadcastReceiver
import com.thewizrd.simpleweather.widgets.WeatherWidgetService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.util.concurrent.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.absoluteValue

class WeatherUpdaterWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    private val wm = WeatherManager.getInstance()
    private var mFusedLocationClient: FusedLocationProviderClient? = null

    companion object {
        private const val TAG = "WeatherUpdaterWorker"

        const val ACTION_UPDATEWEATHER = "SimpleWeather.Droid.action.UPDATE_WEATHER"
        const val ACTION_ENQUEUEWORK = "SimpleWeather.Droid.action.START_ALARM"
        const val ACTION_CANCELWORK = "SimpleWeather.Droid.action.CANCEL_ALARM"
        const val ACTION_REQUEUEWORK = "SimpleWeather.Droid.action.UPDATE_ALARM"

        private const val JOB_ID = 1004

        @JvmStatic
        fun enqueueAction(context: Context, intentAction: String) {
            val context = context.applicationContext
            when (intentAction) {
                ACTION_REQUEUEWORK -> enqueueWork(context)
                ACTION_ENQUEUEWORK ->
                    if (!isWorkScheduled(context)) {
                        startWork(context)
                    }
                ACTION_UPDATEWEATHER ->
                    // For immediate action
                    startWork(context)
                ACTION_CANCELWORK -> cancelWork(context)
            }
        }

        private fun startWork(context: Context) {
            val context = context.applicationContext
            Logger.writeLine(Log.INFO, "%s: Requesting to start work", TAG)
            val updateRequest = OneTimeWorkRequest.Builder(WeatherUpdaterWorker::class.java).apply {
                if (App.getInstance().appState != AppState.FOREGROUND) {
                    setInitialDelay(60, TimeUnit.SECONDS)
                }
            }
            WorkManager.getInstance(context)
                    .enqueueUniqueWork(TAG + "_onBoot", ExistingWorkPolicy.REPLACE, updateRequest.build())
            Logger.writeLine(Log.INFO, "%s: One-time work enqueued", TAG)

            if (!PowerUtils.useForegroundService) {
                // Enqueue periodic task as well
                enqueueWork(context)
            }
        }

        private fun enqueueWork(context: Context) {
            val context = context.applicationContext
            Logger.writeLine(Log.INFO, "%s: Requesting work", TAG)
            val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresCharging(false)
                    .build()
            val updateRequest = PeriodicWorkRequest.Builder(WeatherUpdaterWorker::class.java, Settings.getRefreshInterval().toLong(), TimeUnit.MINUTES, 15, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .build()
            WorkManager.getInstance(context)
                    .enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.REPLACE, updateRequest)
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

        private fun cancelWork(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(TAG)
            Logger.writeLine(Log.INFO, "%s: Canceled work", TAG)
        }

        @TargetApi(Build.VERSION_CODES.O)
        private fun getForegroundNotification(context: Context): Notification {
            WeatherUpdaterService.initChannel(context)
            val mBuilder = NotificationCompat.Builder(context, WeatherUpdaterService.NOT_CHANNEL_ID)
                    .setSmallIcon(R.drawable.wi_day_cloudy)
                    .setContentTitle(context.getString(R.string.not_title_weather_update))
                    .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                    .setOnlyAlertOnce(true)
                    .setNotificationSilent()
                    .setPriority(NotificationCompat.PRIORITY_LOW)
            return mBuilder.build()
        }
    }

    init {
        if (WearableHelper.isGooglePlayServicesInstalled()) {
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
        }
    }

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            Logger.writeLine(Log.INFO, "%s: Work started", TAG)
            val context = applicationContext
            val hasBackgroundLocationAccess = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED

            // Request work to be in foreground (only for Oreo+)
            val appState = App.getInstance().appState
            if (!PowerUtils.useForegroundService && appState != AppState.FOREGROUND && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    var foregroundServiceTypeFlags = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    if (hasBackgroundLocationAccess) foregroundServiceTypeFlags = foregroundServiceTypeFlags or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                    setForeground(ForegroundInfo(JOB_ID, getForegroundNotification(context), foregroundServiceTypeFlags))
                } else {
                    setForeground(ForegroundInfo(JOB_ID, getForegroundNotification(context)))
                }
            }

            // Update configuration
            RemoteConfig.checkConfig()

            if (Settings.isWeatherLoaded()) {
                if (Settings.useFollowGPS()) {
                    try {
                        updateLocation()
                    } catch (e: ExecutionException) {
                        Logger.writeLine(Log.ERROR, e)
                        if (hasBackgroundLocationAccess) {
                            return@withContext Result.retry()
                        }
                    } catch (e: InterruptedException) {
                        Logger.writeLine(Log.ERROR, e)
                        if (hasBackgroundLocationAccess) {
                            return@withContext Result.retry()
                        }
                    }
                }

                // Update for home
                val weather = getWeather()

                if (WeatherWidgetService.widgetsExist(context)) {
                    context.sendBroadcast(Intent(context, WeatherWidgetBroadcastReceiver::class.java)
                            .setAction(WeatherWidgetService.ACTION_REFRESHWIDGET))
                }

                if (weather != null) {
                    if (Settings.showOngoingNotification()) {
                        context.sendBroadcast(Intent(context, WeatherNotificationBroadcastReceiver::class.java)
                                .setAction(WeatherNotificationWorker.ACTION_REFRESHNOTIFICATION))
                    }
                    if (Settings.useAlerts() && wm.supportsAlerts()) {
                        WeatherAlertHandler.postAlerts(Settings.getHomeData(), weather.weatherAlerts)
                    }

                    // Update weather data for Wearables
                    LocalBroadcastManager.getInstance(context)
                            .sendBroadcast(Intent(CommonActions.ACTION_WEATHER_SENDWEATHERUPDATE))
                } else {
                    return@withContext Result.retry()
                }
            }
            Result.success()
        }
    }

    // Re-schedule alarm at selected interval from now
    private suspend fun getWeather(): Weather? = withContext(Dispatchers.IO) {
        val weather = try {
            val wloader = WeatherDataLoader(Settings.getHomeData())
            wloader.loadWeatherData(WeatherRequest.Builder()
                    .forceRefresh(false)
                    .loadAlerts()
                    .loadForecasts()
                    .build()
            ).await()
        } catch (ex: Exception) {
            Logger.writeLine(Log.ERROR, ex, "%s: GetWeather error", TAG)
            null
        }
        weather
    }

    private suspend fun updateLocation(): Boolean = withContext(Dispatchers.IO) {
        val context = applicationContext

        if (Settings.useFollowGPS()) {
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
                    val mLocationRequest = LocationRequest.create().also {
                        it.numUpdates = 1
                        it.interval = 10000
                        it.fastestInterval = 1000
                        it.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                        it.setExpirationDuration(60000)
                    }

                    Looper.prepare()

                    Timber.tag(TAG).i("Fused: Requesting location updates...")

                    val locationResult = suspendCancellableCoroutine<LocationResult?> { continuation ->
                        mFusedLocationClient!!.requestLocationUpdates(mLocationRequest, object : LocationCallback() {
                            override fun onLocationResult(locationResult: LocationResult) {
                                super.onLocationResult(locationResult)
                                mFusedLocationClient!!.removeLocationUpdates(this)
                                Timber.tag(TAG).i("Fused: Location update received...")
                                continuation.resume(locationResult)
                            }

                            override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                                super.onLocationAvailability(locationAvailability)
                                if (!locationAvailability.isLocationAvailable) {
                                    mFusedLocationClient!!.removeLocationUpdates(this)
                                    continuation.resume(null)
                                }
                            }
                        }, Looper.myLooper())
                    }

                    if (locationResult != null) {
                        location = locationResult.lastLocation
                    }
                }
            } else {
                val isGPSEnabled = locMan.isProviderEnabled(LocationManager.GPS_PROVIDER)
                val isNetEnabled = locMan.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                if (isGPSEnabled || isNetEnabled) {
                    val locCriteria = Criteria().also {
                        it.accuracy = Criteria.ACCURACY_COARSE
                        it.isCostAllowed = false
                        it.powerRequirement = Criteria.POWER_LOW
                    }

                    val provider = locMan.getBestProvider(locCriteria, true)
                    location = locMan.getLastKnownLocation(provider)

                    if (location == null) {
                        Looper.prepare()

                        Timber.tag(TAG).i("LocMan: Requesting location update...")

                        location = suspendCancellableCoroutine { continuation ->
                            val locationListener = object : LocationListener {
                                override fun onLocationChanged(location: Location) {
                                    locMan.removeUpdates(this)

                                    Timber.tag(TAG).i("LocMan: Location update received...")

                                    continuation.resume(location)
                                }

                                override fun onStatusChanged(s: String, i: Int, bundle: Bundle) {}
                                override fun onProviderEnabled(s: String) {}
                                override fun onProviderDisabled(s: String) {}
                            }


                            try {
                                locMan.requestSingleUpdate(provider, locationListener, Looper.myLooper())
                            } catch (e: Exception) {
                                locMan.removeUpdates(locationListener)
                                continuation.resumeWithException(e)
                            }
                        }
                    }
                }
            }

            if (location != null) {
                val lastGPSLocData = Settings.getLastGPSLocData()

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
                Settings.saveLastGPSLocData(lastGPSLocData)
                return@withContext true
            }
        }
        false
    }
}