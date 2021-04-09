package com.thewizrd.simpleweather.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.simpleweather.services.ServiceNotificationHelper.JOB_ID
import com.thewizrd.simpleweather.services.ServiceNotificationHelper.createForegroundNotification
import com.thewizrd.simpleweather.services.ServiceNotificationHelper.initChannel
import kotlinx.coroutines.*

class WeatherUpdaterService : Service() {
    private val scope = CoroutineScope(Job() + Dispatchers.Default)
    private var stopServiceJob: Job? = null

    companion object {
        private const val TAG = "WeatherUpdaterService"

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            initChannel(this)
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

        Logger.writeLine(Log.INFO, "%s: Intent Action = %s", TAG, intent?.action)

        stopServiceJob?.cancel()

        when (intent?.action) {
            WidgetUpdaterWorker.ACTION_UPDATEWIDGETS -> {
                scope.launch {
                    WidgetUpdaterWorker.executeWork(applicationContext)
                }.invokeOnCompletion(checkStopSelfCompletionHandler)
            }
            WeatherUpdaterWorker.ACTION_UPDATEWEATHER -> {
                scope.launch {
                    WeatherUpdaterWorker.executeWork(applicationContext)
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