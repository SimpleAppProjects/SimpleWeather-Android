package com.thewizrd.simpleweather.services

import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.thewizrd.shared_resources.utils.SettingsManager
import com.thewizrd.simpleweather.BuildConfig
import com.thewizrd.simpleweather.services.ServiceNotificationHelper.JOB_ID
import com.thewizrd.simpleweather.services.ServiceNotificationHelper.createForegroundNotification
import com.thewizrd.simpleweather.services.ServiceNotificationHelper.initChannel
import java.time.Duration

class UpdaterTimerService : Service() {
    private lateinit var mNotificationManager: NotificationManager
    private lateinit var mTickReceiver: TickReceiver

    // Foreground service
    private var mReceiverRegistered = false
    private var mLastWeatherUpdateTime: Long = -1
    private var mLastWidgetUpdateTime: Long = -1
    private var mUpdateInterval: Int = SettingsManager.DEFAULTINTERVAL

    companion object {
        private const val TAG = "UpdaterTimerService"

        // Actions
        const val ACTION_STARTALARM = "SimpleWeather.Droid.action.START_ALARM"
        const val ACTION_CANCELALARM = "SimpleWeather.Droid.action.CANCEL_ALARM"
        const val ACTION_UPDATEALARM = "SimpleWeather.Droid.action.UPDATE_ALARM"

        const val EXTRA_INTERVAL = "SimpleWeather.Droid.extra.UPDATE_INTERVAL"

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

        mTickReceiver = TickReceiver()
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            initChannel(this)
        }

        startForeground(JOB_ID, createForegroundNotification(applicationContext))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.println(Log.INFO, TAG, "Intent Action = ${intent?.action}")

        when (intent?.action) {
            ACTION_STARTALARM -> {
                // Start alarm if it hasn't started already
                mUpdateInterval = intent.getIntExtra(EXTRA_INTERVAL, SettingsManager.DEFAULTINTERVAL)
                checkReceiver()
                doWork()
            }
            ACTION_UPDATEALARM -> {
                // Refresh interval was changed
                mUpdateInterval = intent.getIntExtra(EXTRA_INTERVAL, SettingsManager.DEFAULTINTERVAL)
                // Update alarm
                checkReceiver()
                val nowMillis = System.currentTimeMillis()
                mLastWeatherUpdateTime = nowMillis
                mLastWidgetUpdateTime = nowMillis
                doWork()
            }
            ACTION_CANCELALARM -> {
                unregisterReceiver()
                stopSelf()
            }
        }

        return START_STICKY
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

    private fun unregisterReceiver() {
        if (mReceiverRegistered) {
            // Cancel clock alarm
            this.unregisterReceiver(mTickReceiver)
            mReceiverRegistered = false
        }
    }

    private fun doWork() {
        val nowMillis = System.currentTimeMillis()

        if (Duration.ofMillis(nowMillis - mLastWeatherUpdateTime).toMinutes() >= mUpdateInterval) {
            Log.println(Log.INFO, TAG, "updating weather...")
            updateWeather()
            mLastWeatherUpdateTime = nowMillis
            mLastWidgetUpdateTime = nowMillis
        } else if (Duration.ofMillis(nowMillis - mLastWidgetUpdateTime).toMinutes() >= 60) {
            Log.println(Log.INFO, TAG, "updating widgets...")
            updateWidgets()
            mLastWidgetUpdateTime = nowMillis
        }
    }

    private fun updateWeather() {
        WeatherUpdaterService.enqueueWork(this, Intent(this, WeatherUpdaterService::class.java)
                .setAction(WeatherUpdaterWorker.ACTION_UPDATEWEATHER))
    }

    private fun updateWidgets() {
        WeatherUpdaterService.enqueueWork(this, Intent(this, WeatherUpdaterService::class.java)
                .setAction(WidgetUpdaterWorker.ACTION_UPDATEWIDGETS))
    }

    override fun onDestroy() {
        stopForeground(true)
        super.onDestroy()
    }

    inner class TickReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_TIME_TICK -> {
                    if (BuildConfig.DEBUG) {
                        Log.println(Log.INFO, TAG, "TickReceiver: ${intent.action} received")
                    }
                    doWork()
                }
                Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> {
                    if (BuildConfig.DEBUG) {
                        Log.println(Log.INFO, TAG, "TickReceiver: ${intent.action} received")
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