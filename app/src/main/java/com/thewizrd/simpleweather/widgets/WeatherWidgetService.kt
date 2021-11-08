package com.thewizrd.simpleweather.widgets

import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.simpleweather.services.ServiceNotificationHelper
import com.thewizrd.simpleweather.services.ServiceNotificationHelper.JOB_ID
import com.thewizrd.simpleweather.services.ServiceNotificationHelper.createForegroundNotification
import kotlinx.coroutines.*

/**
 * Foreground service to update appwidgets
 *
 * Should only be invoked in reaction to an allowed situation
 *
 * For example: on receiving broadcast: ACTION_BOOT_COMPLETED, ACTION_MY_PACKAGE_REPLACED, ACTION_TIMEZONE_CHANGED or ACTION_TIME_CHANGED
 *
 * https://developer.android.com/guide/components/foreground-services#background-start-restrictions
 */
class WeatherWidgetService : Service() {
    private lateinit var mAppWidgetManager: AppWidgetManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var stopServiceJob: Job? = null
    private val runningJobs = mutableListOf<Job>()

    companion object {
        private const val TAG = "WeatherWidgetService"

        // Widget Actions
        const val ACTION_UPDATECLOCK = "SimpleWeather.Droid.action.UPDATE_CLOCK"
        const val ACTION_UPDATEDATE = "SimpleWeather.Droid.action.UPDATE_DATE"

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
            ACTION_UPDATECLOCK -> {
                val appWidgetIds = intent.getIntArrayExtra(WeatherWidgetProvider.EXTRA_WIDGET_IDS)!!
                val widgetType = WidgetType.valueOf(intent.getIntExtra(WeatherWidgetProvider.EXTRA_WIDGET_TYPE, -1))

                scope.launch {
                    withContext(Dispatchers.Default) {
                        val info = WidgetUtils.getWidgetProviderInfoFromType(widgetType)
                            ?: return@withContext
                        WidgetUpdaterHelper.refreshClock(applicationContext, info, appWidgetIds)
                    }
                }.also { registerJob(it) }
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
                }.also { registerJob(it) }
            }
            else -> {
                postStopService()
            }
        }

        return START_STICKY
    }

    private fun registerJob(job: Job) {
        runningJobs += job
        job.invokeOnCompletion {
            runningJobs -= job
            postStopService()
        }
    }

    private fun postStopService() {
        stopServiceJob?.cancel()
        stopServiceJob = scope.launch {
            delay(1000)

            ensureActive()

            if (runningJobs.isEmpty()) {
                Logger.writeLine(Log.INFO, "${TAG}: stopping service...")
                stopSelf()
            }
        }
    }
}