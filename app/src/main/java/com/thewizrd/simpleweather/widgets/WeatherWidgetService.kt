package com.thewizrd.simpleweather.widgets

import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.simpleweather.services.ServiceNotificationHelper
import com.thewizrd.simpleweather.services.ServiceNotificationHelper.JOB_ID
import com.thewizrd.simpleweather.services.ServiceNotificationHelper.createForegroundNotification
import kotlinx.coroutines.*

class WeatherWidgetService : Service() {
    private lateinit var mAppWidgetManager: AppWidgetManager

    private val scope = CoroutineScope(Job() + Dispatchers.Default)
    private var stopServiceJob: Job? = null

    companion object {
        private const val TAG = "WeatherWidgetService"

        // Widget Actions
        const val ACTION_REFRESHWIDGET = "SimpleWeather.Droid.action.REFRESH_WIDGET"
        const val ACTION_RESETGPSWIDGETS = "SimpleWeather.Droid.action.RESET_GPSWIDGETS"
        const val ACTION_REFRESHGPSWIDGETS = "SimpleWeather.Droid.action.REFRESH_GPSWIDGETS"
        const val ACTION_REFRESHWIDGETS = "SimpleWeather.Droid.action.REFRESH_WIDGETS"

        const val ACTION_UPDATECLOCK = "SimpleWeather.Droid.action.UPDATE_CLOCK"
        const val ACTION_UPDATEDATE = "SimpleWeather.Droid.action.UPDATE_DATE"

        // Extras
        const val EXTRA_LOCATIONNAME = "SimpleWeather.Droid.extra.LOCATION_NAME"
        const val EXTRA_LOCATIONQUERY = "SimpleWeather.Droid.extra.LOCATION_QUERY"

        @JvmStatic
        fun enqueueWork(context: Context, work: Intent) {
            ContextCompat.startForegroundService(context, work)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        mAppWidgetManager = AppWidgetManager.getInstance(applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ServiceNotificationHelper.initChannel(this)
        }

        startForegroundIfNeeded()
    }

    private fun startForegroundIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(JOB_ID, createForegroundNotification(applicationContext))
        }
    }

    override fun onDestroy() {
        Logger.writeLine(Log.INFO, "${TAG}: stopping service...")

        scope.cancel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            stopForeground(true)

        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundIfNeeded()
        stopServiceJob?.cancel()

        Logger.writeLine(Log.INFO, "%s: Intent Action = %s", TAG, intent?.action)

        when (intent?.action) {
            // Widget update actions
            // Note: should end service if fg option is disabled
            ACTION_REFRESHWIDGET -> {
                val appWidgetIds = intent.getIntArrayExtra(WeatherWidgetProvider.EXTRA_WIDGET_IDS)!!
                val widgetType = WidgetType.valueOf(intent.getIntExtra(WeatherWidgetProvider.EXTRA_WIDGET_TYPE, -1))

                scope.launch {
                    withContext(Dispatchers.Default) {
                        val info = WidgetUtils.getWidgetProviderInfoFromType(widgetType)
                                ?: return@withContext
                        WidgetUpdaterHelper.refreshWidget(applicationContext, info, mAppWidgetManager, appWidgetIds)
                    }
                }.invokeOnCompletion(checkStopSelfCompletionHandler)
            }
            ACTION_RESETGPSWIDGETS -> {
                scope.launch {
                    withContext(Dispatchers.Default) {
                        // GPS feature disabled; reset widget
                        WidgetUpdaterHelper.resetGPSWidgets(applicationContext);
                    }
                }.invokeOnCompletion(checkStopSelfCompletionHandler)
            }
            ACTION_REFRESHGPSWIDGETS -> {
                scope.launch {
                    withContext(Dispatchers.Default) {
                        WidgetUpdaterHelper.refreshWidgets(applicationContext, Constants.KEY_GPS);
                    }
                }.invokeOnCompletion(checkStopSelfCompletionHandler)
            }
            ACTION_REFRESHWIDGETS -> {
                val locationQuery = intent.getStringExtra(EXTRA_LOCATIONQUERY)
                scope.launch {
                    locationQuery?.let {
                        withContext(Dispatchers.Default) {
                            WidgetUpdaterHelper.refreshWidgets(applicationContext, it);
                        }
                    }
                }.invokeOnCompletion(checkStopSelfCompletionHandler)
            }
            ACTION_UPDATECLOCK -> {
                val appWidgetIds = intent.getIntArrayExtra(WeatherWidgetProvider.EXTRA_WIDGET_IDS)!!
                val widgetType = WidgetType.valueOf(intent.getIntExtra(WeatherWidgetProvider.EXTRA_WIDGET_TYPE, -1))

                scope.launch {
                    withContext(Dispatchers.Default) {
                        val info = WidgetUtils.getWidgetProviderInfoFromType(widgetType)
                                ?: return@withContext
                        WidgetUpdaterHelper.refreshClock(applicationContext, info, appWidgetIds)
                    }
                }.invokeOnCompletion(checkStopSelfCompletionHandler)
            }
            ACTION_UPDATEDATE -> {
                val appWidgetIds = intent.getIntArrayExtra(WeatherWidgetProvider.EXTRA_WIDGET_IDS)!!
                val widgetType = WidgetType.valueOf(intent.getIntExtra(WeatherWidgetProvider.EXTRA_WIDGET_TYPE, -1))

                scope.launch {
                    withContext(Dispatchers.Default) {
                        val info = WidgetUtils.getWidgetProviderInfoFromType(widgetType)
                                ?: return@withContext
                        WidgetUpdaterHelper.refreshDate(applicationContext, info, appWidgetIds)
                    }
                }.invokeOnCompletion(checkStopSelfCompletionHandler)
            }
            else -> {
                postStopService()
            }
        }

        return START_STICKY
    }

    private val checkStopSelfCompletionHandler = { _: Throwable? ->
        postStopService()
    }

    private fun postStopService() {
        stopServiceJob?.cancel()
        stopServiceJob = scope.launch {
            delay(1000)

            ensureActive()

            Logger.writeLine(Log.INFO, "${TAG}: stopping service...")
            stopSelf()
        }
    }
}