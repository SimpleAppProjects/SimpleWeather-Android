package com.thewizrd.simpleweather.widgets

import android.app.IntentService
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.utils.Settings
import com.thewizrd.simpleweather.services.ServiceNotificationHelper
import com.thewizrd.simpleweather.services.ServiceNotificationHelper.JOB_ID
import com.thewizrd.simpleweather.services.ServiceNotificationHelper.createForegroundNotification
import kotlinx.coroutines.*

class WeatherWidgetService : IntentService("weather-widget-service") {
    private lateinit var mAppWidgetManager: AppWidgetManager

    companion object {
        private const val TAG = "WeatherWidgetService"

        // Widget Actions
        const val ACTION_REFRESHWIDGET = "SimpleWeather.Droid.action.REFRESH_WIDGET"
        const val ACTION_RESIZEWIDGET = "SimpleWeather.Droid.action.RESIZE_WIDGET"
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

    override fun onCreate() {
        super.onCreate()
        setIntentRedelivery(true)

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            stopForeground(true)

        super.onDestroy()
    }

    override fun onHandleIntent(intent: Intent?) {
        Logger.writeLine(Log.INFO, "%s: Intent Action = %s", TAG, intent?.action)

        when (intent?.action) {
            // Widget update actions
            // Note: should end service if fg option is disabled
            ACTION_REFRESHWIDGET -> {
                val appWidgetIds = intent.getIntArrayExtra(WeatherWidgetProvider.EXTRA_WIDGET_IDS)!!
                val widgetType = WidgetType.valueOf(intent.getIntExtra(WeatherWidgetProvider.EXTRA_WIDGET_TYPE, -1))

                runBlocking {
                    withContext(Dispatchers.Default) {
                        val info = WidgetUtils.getWidgetProviderInfoFromType(widgetType)
                                ?: return@withContext
                        WidgetUpdaterHelper.refreshWidget(applicationContext, info, mAppWidgetManager, appWidgetIds)
                    }
                }
            }
            ACTION_RESIZEWIDGET -> {
                val appWidgetId = intent.getIntExtra(WeatherWidgetProvider.EXTRA_WIDGET_ID, -1)
                val widgetType = WidgetType.valueOf(intent.getIntExtra(WeatherWidgetProvider.EXTRA_WIDGET_TYPE, -1))
                val newOptions = intent.getBundleExtra(WeatherWidgetProvider.EXTRA_WIDGET_OPTIONS)!!

                runBlocking {
                    withContext(Dispatchers.Default) {
                        if (Settings.isWeatherLoaded()) {
                            val info = WidgetUtils.getWidgetProviderInfoFromType(widgetType)
                                    ?: return@withContext
                            WidgetUpdaterHelper.rebuildWidget(applicationContext, info, mAppWidgetManager, appWidgetId, newOptions)
                        }
                    }
                    Logger.writeLine(Log.INFO, "${TAG}: resize completed...")
                }
            }
            ACTION_RESETGPSWIDGETS -> {
                runBlocking {
                    withContext(Dispatchers.Default) {
                        // GPS feature disabled; reset widget
                        WidgetUpdaterHelper.resetGPSWidgets(applicationContext);
                    }
                }
            }
            ACTION_REFRESHGPSWIDGETS -> {
                runBlocking {
                    withContext(Dispatchers.Default) {
                        WidgetUpdaterHelper.refreshWidgets(applicationContext, Constants.KEY_GPS);
                    }
                }
            }
            ACTION_REFRESHWIDGETS -> {
                val locationQuery = intent.getStringExtra(EXTRA_LOCATIONQUERY)
                runBlocking {
                    locationQuery?.let {
                        withContext(Dispatchers.Default) {
                            WidgetUpdaterHelper.refreshWidgets(applicationContext, it);
                        }
                    }
                }
            }
            ACTION_UPDATECLOCK -> {
                val appWidgetIds = intent.getIntArrayExtra(WeatherWidgetProvider.EXTRA_WIDGET_IDS)!!
                val widgetType = WidgetType.valueOf(intent.getIntExtra(WeatherWidgetProvider.EXTRA_WIDGET_TYPE, -1))

                runBlocking {
                    withContext(Dispatchers.Default) {
                        val info = WidgetUtils.getWidgetProviderInfoFromType(widgetType)
                                ?: return@withContext
                        WidgetUpdaterHelper.refreshClock(applicationContext, info, appWidgetIds)
                    }
                }
            }
            ACTION_UPDATEDATE -> {
                val appWidgetIds = intent.getIntArrayExtra(WeatherWidgetProvider.EXTRA_WIDGET_IDS)!!
                val widgetType = WidgetType.valueOf(intent.getIntExtra(WeatherWidgetProvider.EXTRA_WIDGET_TYPE, -1))

                runBlocking {
                    withContext(Dispatchers.Default) {
                        val info = WidgetUtils.getWidgetProviderInfoFromType(widgetType)
                                ?: return@withContext
                        WidgetUpdaterHelper.refreshDate(applicationContext, info, appWidgetIds)
                    }
                }
            }
        }
    }
}