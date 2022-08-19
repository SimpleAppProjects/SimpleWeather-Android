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
import androidx.core.app.ServiceCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.thewizrd.shared_resources.utils.SettingsManager
import com.thewizrd.simpleweather.BuildConfig
import com.thewizrd.simpleweather.notifications.DailyWeatherNotificationWorker
import com.thewizrd.simpleweather.notifications.DailyWeatherNotificationWorkerActions
import com.thewizrd.simpleweather.services.ServiceNotificationHelper.JOB_ID
import com.thewizrd.simpleweather.services.ServiceNotificationHelper.createForegroundNotification
import com.thewizrd.simpleweather.services.ServiceNotificationHelper.initChannel
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit

class UpdaterTimerService : Service() {
    private lateinit var mNotificationManager: NotificationManager
    private lateinit var mTickReceiver: TickReceiver

    private lateinit var mRemoteReceiver: BroadcastReceiver
    private lateinit var mLocalBroadcastMgr: LocalBroadcastManager

    // Foreground service
    private var mReceiverRegistered = false
    private var mLastWeatherUpdateTime: Long = -1
    private var mLastWidgetUpdateTime: Long = -1
    private var mUpdateInterval: Int = SettingsManager.DEFAULT_INTERVAL

    private var mTodayForecastTime: String? = SettingsManager.DEFAULT_DAILYNOTIFICATION_TIME
    private var mLastTodayForecastTime: Long = -1

    private var mFirstTime = true

    companion object {
        private const val TAG = "UpdaterTimerService"

        // Actions
        const val ACTION_STARTALARM = "SimpleWeather.Droid.action.START_ALARM"
        const val ACTION_CANCELALARM = "SimpleWeather.Droid.action.CANCEL_ALARM"
        const val ACTION_UPDATEALARM = "SimpleWeather.Droid.action.UPDATE_ALARM"

        const val EXTRA_INTERVAL = "SimpleWeather.Droid.extra.UPDATE_INTERVAL"

        private val HOUR_IN_MILLIS = TimeUnit.HOURS.toMillis(1)

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

        mLocalBroadcastMgr = LocalBroadcastManager.getInstance(applicationContext)
        mRemoteReceiver = RemoteReceiver()
        mLocalBroadcastMgr.registerReceiver(mRemoteReceiver, IntentFilter().apply {
            addAction(ACTION_STARTALARM)
            addAction(ACTION_UPDATEALARM)
            addAction(ACTION_CANCELALARM)
            addAction(DailyWeatherNotificationWorkerActions.ACTION_UPDATENOTIFICATIONTIME)
            addAction(DailyWeatherNotificationWorkerActions.ACTION_CANCELNOTIFICATION)
        })

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
                startAlarmIfNeeded(
                    intent.getIntExtra(
                        EXTRA_INTERVAL,
                        SettingsManager.DEFAULT_INTERVAL
                    )
                )
            }
            ACTION_UPDATEALARM -> {
                updateAlarm(intent.getIntExtra(EXTRA_INTERVAL, SettingsManager.DEFAULT_INTERVAL))
            }
            ACTION_CANCELALARM -> {
                cancelAlarm()
            }
            DailyWeatherNotificationWorkerActions.ACTION_UPDATENOTIFICATIONTIME -> {
                updateDailyNotificationTime(
                    intent.getIntExtra(EXTRA_INTERVAL, mUpdateInterval),
                    intent.getStringExtra(DailyWeatherNotificationWorkerActions.EXTRA_UPDATETIME)
                )
            }
            DailyWeatherNotificationWorkerActions.ACTION_CANCELNOTIFICATION -> {
                cancelDailyNotification()
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

    private fun startAlarmIfNeeded(interval: Int) {
        mUpdateInterval = interval
        checkReceiver()
        doWork()
    }

    private fun cancelAlarm() {
        unregisterReceiver()
        stopSelf()
    }

    private fun updateAlarm(interval: Int) {
        // Refresh interval was changed
        mUpdateInterval = interval
        // Update alarm
        checkReceiver()
        val nowMillis = System.currentTimeMillis()
        mLastWeatherUpdateTime = nowMillis - (nowMillis % HOUR_IN_MILLIS)
        mLastWidgetUpdateTime = nowMillis - (nowMillis % HOUR_IN_MILLIS)
        doWork()
    }

    private fun updateDailyNotificationTime(interval: Int?, forecastTime: String?) {
        // Start alarm if it hasn't started already
        mUpdateInterval = interval ?: mUpdateInterval
        checkReceiver()

        mTodayForecastTime = forecastTime
        mLastTodayForecastTime = System.currentTimeMillis()
        doWork()
    }

    private fun cancelDailyNotification() {
        mTodayForecastTime = null
        mLastTodayForecastTime = System.currentTimeMillis()
    }

    private fun doWork() {
        val nowMillis = System.currentTimeMillis()

        if (Duration.ofMillis(nowMillis - mLastWeatherUpdateTime).toMinutes() >= mUpdateInterval) {
            Log.println(Log.INFO, TAG, "updating weather...")
            updateWeather()
            mLastWeatherUpdateTime =
                nowMillis - (if (mFirstTime) (nowMillis % HOUR_IN_MILLIS) else 0)
            mLastWidgetUpdateTime =
                nowMillis - (if (mFirstTime) (nowMillis % HOUR_IN_MILLIS) else 0)
            mFirstTime = false
        } else if (Duration.ofMillis(nowMillis - mLastWidgetUpdateTime).toMinutes() >= 60) {
            Log.println(Log.INFO, TAG, "updating widgets...")
            updateWidgets()
            mLastWidgetUpdateTime =
                nowMillis - (if (mFirstTime) (nowMillis % HOUR_IN_MILLIS) else 0)
            mFirstTime = false
        }

        if (!mTodayForecastTime.isNullOrBlank() && isDailyForecastTime(
                mTodayForecastTime!!,
                mLastTodayForecastTime
            )
        ) {
            Log.println(Log.INFO, TAG, "creating daily notification...")
            sendDailyNotification()
            mLastTodayForecastTime = nowMillis
        }
    }

    private fun updateWeather() {
        WeatherUpdaterWorker.enqueueAction(this, WeatherUpdaterWorker.ACTION_UPDATEWEATHER)
    }

    private fun updateWidgets() {
        WidgetUpdaterWorker.enqueueAction(this, WidgetUpdaterWorker.ACTION_UPDATEWIDGETS)
    }

    private fun sendDailyNotification() {
        DailyWeatherNotificationWorker.sendDailyNotification(this)
    }

    override fun onDestroy() {
        mLocalBroadcastMgr.unregisterReceiver(mRemoteReceiver)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
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
                    mLastWeatherUpdateTime = nowMillis - (nowMillis % HOUR_IN_MILLIS)
                    mLastWidgetUpdateTime = nowMillis - (nowMillis % HOUR_IN_MILLIS)
                    mLastTodayForecastTime = nowMillis
                    doWork()
                }
            }
        }
    }

    inner class RemoteReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.println(Log.INFO, TAG, "RemoteReceiver: ${intent?.action} received")
            when (intent?.action) {
                ACTION_STARTALARM -> {
                    startAlarmIfNeeded(
                        intent.getIntExtra(
                            EXTRA_INTERVAL,
                            SettingsManager.DEFAULT_INTERVAL
                        )
                    )
                }
                ACTION_CANCELALARM -> {
                    cancelAlarm()
                }
                ACTION_UPDATEALARM -> {
                    updateAlarm(
                        intent.getIntExtra(
                            EXTRA_INTERVAL,
                            SettingsManager.DEFAULT_INTERVAL
                        )
                    )
                }
                DailyWeatherNotificationWorkerActions.ACTION_UPDATENOTIFICATIONTIME -> {
                    updateDailyNotificationTime(
                        intent.getIntExtra(EXTRA_INTERVAL, mUpdateInterval),
                        intent.getStringExtra(DailyWeatherNotificationWorkerActions.EXTRA_UPDATETIME)
                    )
                }
                DailyWeatherNotificationWorkerActions.ACTION_CANCELNOTIFICATION -> {
                    cancelDailyNotification()
                }
            }
        }
    }
}