package com.thewizrd.simpleweather.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
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
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.postDelayed
import androidx.work.*
import com.google.android.gms.location.*
import com.thewizrd.shared_resources.remoteconfig.RemoteConfig
import com.thewizrd.shared_resources.tzdb.TZDBCache
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.wearable.WearableDataSync
import com.thewizrd.shared_resources.wearable.WearableHelper
import com.thewizrd.shared_resources.weatherdata.Weather
import com.thewizrd.shared_resources.weatherdata.WeatherDataLoader
import com.thewizrd.shared_resources.weatherdata.WeatherManager
import com.thewizrd.shared_resources.weatherdata.WeatherRequest
import com.thewizrd.simpleweather.App
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.wearable.WearableWorker
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.concurrent.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.absoluteValue

class WeatherUpdaterWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private val wm = WeatherManager.getInstance()
    private val settingsMgr = App.instance.settingsManager

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

    init {
        if (WearableHelper.isGooglePlayServicesInstalled()) {
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
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

            if (settingsMgr.getDataSync() == WearableDataSync.OFF) {
                try {
                    // Update configuration
                    RemoteConfig.checkConfigAsync()
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e)
                }
            }

            if (settingsMgr.isWeatherLoaded()) {
                if (settingsMgr.getDataSync() == WearableDataSync.OFF && settingsMgr.useFollowGPS()) {
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
                    if (settingsMgr.getDataSync() != WearableDataSync.OFF) {
                        // Check if data has been updated
                        WearableWorker.enqueueAction(context, WearableWorker.ACTION_REQUESTWEATHERUPDATE)
                    }
                    return@withContext Result.retry()
                }
            }
            return@withContext Result.success()
        }
    }

    private suspend fun getWeather(): Weather? = withContext(Dispatchers.IO) {
        val weather = try {
            val wloader = WeatherDataLoader(settingsMgr.getHomeData()!!)
            val request = WeatherRequest.Builder()
            if (settingsMgr.getDataSync() == WearableDataSync.OFF) {
                request.forceRefresh(false).loadAlerts()
            } else {
                request.forceLoadSavedData()
            }
            wloader.loadWeatherData(request.build()).await()
        } catch (ex: Exception) {
            Logger.writeLine(Log.ERROR, ex, "%s: GetWeather error", TAG)
            null
        }
        weather
    }

    @SuppressLint("MissingPermission")
    private suspend fun updateLocation(): Boolean = withContext(Dispatchers.Default) {
        val context = applicationContext

        if (settingsMgr.useFollowGPS()) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return@withContext false
            }

            var location: Location? = null
            val locMan = context.getSystemService(LocationManager::class.java)
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

                    val handlerThread = HandlerThread("location")
                    handlerThread.start()

                    Timber.tag(TAG).i("Fused: Requesting location updates...")

                    val locationResult = suspendCancellableCoroutine<LocationResult?> { continuation ->
                        // Handler for timeout callback
                        val handler = Handler(handlerThread.looper)

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
                        val handlerThread = HandlerThread("location")
                        handlerThread.start()

                        Timber.tag(TAG).i("LocMan: Requesting location update...")

                        location = suspendCancellableCoroutine { continuation ->
                            // Handler for timeout callback
                            val handler = Handler(handlerThread.looper)

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
                val lastGPSLocData = settingsMgr.getLastGPSLocData()

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
                lastGPSLocData!!.setData(query_vm, location)
                settingsMgr.saveLastGPSLocData(lastGPSLocData)
                return@withContext true
            }
        }
        false
    }
}