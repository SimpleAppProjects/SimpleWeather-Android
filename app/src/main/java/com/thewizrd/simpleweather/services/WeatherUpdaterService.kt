package com.thewizrd.simpleweather.services

import android.app.*
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.utils.Settings
import com.thewizrd.simpleweather.BuildConfig
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.notifications.NotificationUtils
import com.thewizrd.simpleweather.services.ServiceNotificationHelper.NOT_CHANNEL_ID
import com.thewizrd.simpleweather.services.ServiceNotificationHelper.initChannel
import com.thewizrd.simpleweather.utils.PowerUtils
import com.thewizrd.simpleweather.widgets.WeatherWidgetProvider
import com.thewizrd.simpleweather.widgets.WidgetType
import com.thewizrd.simpleweather.widgets.WidgetUpdaterHelper
import com.thewizrd.simpleweather.widgets.WidgetUtils
import kotlinx.coroutines.*
import java.time.Duration

class WeatherUpdaterService : Service() {
    private lateinit var mNotificationManager: NotificationManager
    private lateinit var mTickReceiver: TickReceiver

    private lateinit var mAppWidgetManager: AppWidgetManager

    // Foreground service
    private var mReceiverRegistered = false
    private var mLastWeatherUpdateTime: Long = -1
    private var mLastWidgetUpdateTime: Long = -1

    private val scope = CoroutineScope(Job() + Dispatchers.Default)

    companion object {
        const val TAG = "WeatherUpdaterService"

        // Actions
        const val ACTION_STARTALARM = "SimpleWeather.Droid.action.START_ALARM"
        const val ACTION_CANCELALARM = "SimpleWeather.Droid.action.CANCEL_ALARM"
        const val ACTION_UPDATEALARM = "SimpleWeather.Droid.action.UPDATE_ALARM"

        private const val ACTION_REQUESTUPDATE = "SimpleWeather.Droid.action.REQUEST_UPDATE"

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

        private const val JOB_ID = 1000

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

        mTickReceiver = TickReceiver()
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            initChannel(this)
        }

        startForeground(JOB_ID, createForegroundNotification())
    }

    private fun createForegroundNotification(): Notification {
        val notif = NotificationCompat.Builder(this, NOT_CHANNEL_ID).apply {
            setSmallIcon(R.drawable.ic_wi_cloud_refresh)
            setSubText(getString(R.string.app_name))
            setContentTitle(getString(R.string.message_widgetservice_running))
            setOnlyAlertOnce(true)
            setNotificationSilent()
            setShowWhen(false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationUtils.getAppNotificationChannelSettingsActivityIntent(applicationContext, NOT_CHANNEL_ID).also {
                    if (it.resolveActivity(applicationContext.packageManager) != null) {
                        setContentIntent(it.toPendingActivity())
                    }
                }
            } else {
                NotificationUtils.getAppSettingsActivityIntent(applicationContext).also {
                    if (it.resolveActivity(applicationContext.packageManager) != null) {
                        setContentIntent(it.toPendingActivity())
                    }
                }
            }
            priority = NotificationCompat.PRIORITY_LOW
        }

        return notif.build()
    }

    private fun Intent.toPendingActivity(): PendingIntent {
        return PendingIntent.getActivity(applicationContext, 0, this, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.writeLine(Log.INFO, "%s: Intent Action = %s", TAG, intent?.action)

        when (intent?.action) {
            ACTION_STARTALARM -> {
                // Start alarm if it hasn't started already
                checkReceiver()
                doWork()
            }
            ACTION_UPDATEALARM -> {
                // Refresh interval was changed
                // Update alarm
                checkReceiver()
                val nowMillis = System.currentTimeMillis()
                mLastWeatherUpdateTime = nowMillis
                mLastWidgetUpdateTime = nowMillis
                doWork()
            }
            ACTION_CANCELALARM -> {
                if (mReceiverRegistered) {
                    // Cancel clock alarm
                    this.unregisterReceiver(mTickReceiver)
                    mReceiverRegistered = false
                }
                stopSelf()
            }
            ACTION_REQUESTUPDATE -> {
                updateWeather()
            }
            WidgetUpdaterWorker.ACTION_UPDATEWIDGETS -> {
                updateWidgets()
            }
            WeatherUpdaterWorker.ACTION_UPDATEWEATHER -> {
                updateWeather()
            }
            // Widget update actions
            // Note: should end service if fg option is disabled
            ACTION_REFRESHWIDGET -> {
                val appWidgetIds = intent.getIntArrayExtra(WeatherWidgetProvider.EXTRA_WIDGET_IDS)!!
                val widgetType = WidgetType.valueOf(intent.getIntExtra(WeatherWidgetProvider.EXTRA_WIDGET_TYPE, -1))

                scope.launch(Dispatchers.Default) {
                    withContext(Dispatchers.Default) {
                        val info = WidgetUtils.getWidgetProviderInfoFromType(widgetType)
                                ?: return@withContext
                        WidgetUpdaterHelper.refreshWidget(applicationContext, info, mAppWidgetManager, appWidgetIds)
                    }
                }.invokeOnCompletion(checkStopSelfCompletionHandler)
            }
            ACTION_RESIZEWIDGET -> {
                val appWidgetId = intent.getIntExtra(WeatherWidgetProvider.EXTRA_WIDGET_ID, -1)
                val widgetType = WidgetType.valueOf(intent.getIntExtra(WeatherWidgetProvider.EXTRA_WIDGET_TYPE, -1))
                val newOptions = intent.getBundleExtra(WeatherWidgetProvider.EXTRA_WIDGET_OPTIONS)!!

                scope.launch(Dispatchers.Default) {
                    withContext(Dispatchers.Default) {
                        if (Settings.isWeatherLoaded()) {
                            val info = WidgetUtils.getWidgetProviderInfoFromType(widgetType)
                                    ?: return@withContext
                            WidgetUpdaterHelper.rebuildWidget(applicationContext, info, mAppWidgetManager, appWidgetId, newOptions)
                        }
                    }
                }.invokeOnCompletion(checkStopSelfCompletionHandler)
            }
            ACTION_RESETGPSWIDGETS -> {
                scope.launch(Dispatchers.Default) {
                    withContext(Dispatchers.Default) {
                        // GPS feature disabled; reset widget
                        WidgetUpdaterHelper.resetGPSWidgets(applicationContext);
                    }
                }.invokeOnCompletion(checkStopSelfCompletionHandler)
            }
            ACTION_REFRESHGPSWIDGETS -> {
                scope.launch(Dispatchers.Default) {
                    withContext(Dispatchers.Default) {
                        WidgetUpdaterHelper.refreshWidgets(applicationContext, Constants.KEY_GPS);
                    }
                }.invokeOnCompletion(checkStopSelfCompletionHandler)
            }
            ACTION_REFRESHWIDGETS -> {
                val locationQuery = intent.getStringExtra(EXTRA_LOCATIONQUERY)
                scope.launch(Dispatchers.Default) {
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

                scope.launch(Dispatchers.Default) {
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

                scope.launch(Dispatchers.Default) {
                    withContext(Dispatchers.Default) {
                        val info = WidgetUtils.getWidgetProviderInfoFromType(widgetType)
                                ?: return@withContext
                        WidgetUpdaterHelper.refreshDate(applicationContext, info, appWidgetIds)
                    }
                }.invokeOnCompletion(checkStopSelfCompletionHandler)
            }
        }

        return if (PowerUtils.useForegroundService) START_STICKY else START_NOT_STICKY
    }

    private val checkStopSelfCompletionHandler = { _: Throwable? ->
        if (!PowerUtils.useForegroundService) {
            stopSelf()
        }
    }

    private fun checkReceiver() {
        if (!mReceiverRegistered) {
            registerReceiver(mTickReceiver, IntentFilter().apply {
                addAction(Intent.ACTION_TIME_TICK)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
                addAction(Intent.ACTION_TIME_CHANGED)
            })
            mReceiverRegistered = true
        }
    }

    private fun doWork() {
        val nowMillis = System.currentTimeMillis()

        if (Duration.ofMillis(nowMillis - mLastWeatherUpdateTime).toMinutes() >= Settings.getRefreshInterval()) {
            Logger.writeLine(Log.INFO, "${TAG}: updating weather...")
            updateWeather()
            mLastWeatherUpdateTime = nowMillis
            mLastWidgetUpdateTime = nowMillis
        } else if (Duration.ofMillis(nowMillis - mLastWidgetUpdateTime).toMinutes() >= 60) {
            Logger.writeLine(Log.INFO, "${TAG}: updating widgets...")
            updateWidgets()
            mLastWidgetUpdateTime = nowMillis
        }
    }

    private fun updateWeather() {
        scope.launch(Dispatchers.Default) {
            WeatherUpdaterWorker.executeWork(applicationContext)
        }
    }

    private fun updateWidgets() {
        scope.launch(Dispatchers.Default) {
            WidgetUpdaterWorker.executeWork(applicationContext)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        stopForeground(true)
        super.onDestroy()
    }

    inner class TickReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_TIME_TICK -> {
                    if (BuildConfig.DEBUG) {
                        Logger.writeLine(Log.INFO, "${TAG}.TickReceiver: ${intent.action} received")
                    }
                    doWork()
                }
                Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> {
                    if (BuildConfig.DEBUG) {
                        Logger.writeLine(Log.INFO, "${TAG}.TickReceiver: ${intent.action} received")
                    }
                    val nowMillis = System.currentTimeMillis()
                    mLastWeatherUpdateTime = nowMillis
                    mLastWidgetUpdateTime = nowMillis
                    doWork()
                }
            }
        }
    }
}