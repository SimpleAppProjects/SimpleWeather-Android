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
import com.thewizrd.simpleweather.notifications.DailyWeatherNotificationWorkerActions
import com.thewizrd.simpleweather.services.ServiceNotificationHelper.JOB_ID
import com.thewizrd.simpleweather.services.ServiceNotificationHelper.createForegroundNotification
import com.thewizrd.simpleweather.services.ServiceNotificationHelper.initChannel
import java.time.Duration
import java.util.*

class UpdaterTimerService : Service() {
    private lateinit var mNotificationManager: NotificationManager
    private lateinit var mTickReceiver: TickReceiver

    // Foreground service
    private var mReceiverRegistered = false
    private var mLastWeatherUpdateTime: Long = -1
    private var mLastWidgetUpdateTime: Long = -1
    private var mUpdateInterval: Int = SettingsManager.DEFAULTINTERVAL

    private var mTodayForecastTime: String? = SettingsManager.DEFAULT_DAILYNOTIFICATION_TIME
    private var mLastTodayForecastTime: Long = -1

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

        private fun isDailyForecastTime(forecastTime: String, lastUpdateTime: Long): Boolean {
            /*
            val defaultZone = ZoneId.systemDefault()
            val now = LocalDateTime.now(defaultZone)

            val configuredTime = LocalTime.parse(forecastTime, DateTimeFormatter.ofPattern("[H:mm][HH:mm][H:m][HH:m]"))
            val configuredDateTime = now.truncatedTo(ChronoUnit.DAYS).plusNanos(configuredTime.toNanoOfDay())

            return now >= configuredDateTime && configuredDateTime > LocalDateTime.ofInstant(Instant.ofEpochMilli(lastUpdateTime), defaultZone)
            */
            val now = System.currentTimeMillis()

            val timeSplit = forecastTime.split(":")
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, timeSplit[0].toInt())
            cal.set(Calendar.MINUTE, timeSplit[1].toInt())
            val configuredTime = cal.timeInMillis

            return now >= configuredTime && configuredTime > lastUpdateTime
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
            DailyWeatherNotificationWorkerActions.ACTION_UPDATENOTIFICATIONTIME -> {
                // Start alarm if it hasn't started already
                mUpdateInterval = intent.getIntExtra(EXTRA_INTERVAL, mUpdateInterval)
                checkReceiver()

                mTodayForecastTime = intent.getStringExtra(DailyWeatherNotificationWorkerActions.EXTRA_UPDATETIME)
                mLastTodayForecastTime = System.currentTimeMillis()
                doWork()
            }
            DailyWeatherNotificationWorkerActions.ACTION_CANCELNOTIFICATION -> {
                mTodayForecastTime = null
                mLastTodayForecastTime = System.currentTimeMillis()
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

        if (!mTodayForecastTime.isNullOrBlank() && isDailyForecastTime(mTodayForecastTime!!, mLastTodayForecastTime)) {
            Log.println(Log.INFO, TAG, "creating daily notification...")
            sendDailyNotification()
            mLastTodayForecastTime = nowMillis
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

    private fun sendDailyNotification() {
        WeatherUpdaterService.enqueueWork(this, Intent(this, WeatherUpdaterService::class.java)
                .setAction(DailyWeatherNotificationWorkerActions.ACTION_SENDNOTIFICATION))
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
                    mLastTodayForecastTime = nowMillis
                    doWork()
                }
            }
        }
    }
}