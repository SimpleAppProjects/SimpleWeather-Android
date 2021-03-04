package com.thewizrd.simpleweather.services

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.utils.Settings
import com.thewizrd.simpleweather.BuildConfig
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.notifications.NotificationUtils
import java.time.Duration

class WeatherUpdaterService : Service() {
    private lateinit var mNotificationManager: NotificationManager
    private lateinit var mTickReceiver: TickReceiver

    private var mReceiverRegistered = false
    private var mLastWeatherUpdateTime: Long = -1
    private var mLastWidgetUpdateTime: Long = -1

    companion object {
        const val TAG = "WeatherUpdaterService"

        const val ACTION_STARTALARM = "SimpleWeather.Droid.action.START_ALARM"
        const val ACTION_CANCELALARM = "SimpleWeather.Droid.action.CANCEL_ALARM"
        const val ACTION_UPDATEALARM = "SimpleWeather.Droid.action.UPDATE_ALARM"

        private const val ACTION_REQUESTUPDATE = "SimpleWeather.Droid.action.REQUEST_UPDATE"

        internal const val NOT_CHANNEL_ID = "SimpleWeather.generalnotif"
        private const val JOB_ID = 1006

        @RequiresApi(Build.VERSION_CODES.O)
        internal fun initChannel(context: Context) {
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
                if (!mReceiverRegistered) {
                    registerReceiver(mTickReceiver, IntentFilter().apply {
                        addAction(Intent.ACTION_TIME_TICK)
                        addAction(Intent.ACTION_TIMEZONE_CHANGED)
                        addAction(Intent.ACTION_TIME_CHANGED)
                    })
                    mReceiverRegistered = true
                }
                doWork()
            }
            ACTION_UPDATEALARM -> {
                // Refresh interval was changed
                // Update alarm
                val nowMillis = System.currentTimeMillis()
                mLastWeatherUpdateTime = nowMillis
                mLastWidgetUpdateTime = nowMillis
                doWork()
            }
            ACTION_CANCELALARM -> {
                // Cancel clock alarm
                this.unregisterReceiver(mTickReceiver)
                mReceiverRegistered = false
                stopSelf()
            }
            ACTION_REQUESTUPDATE -> {
                WeatherUpdaterWorker.enqueueAction(this, WeatherUpdaterWorker.ACTION_UPDATEWEATHER)
            }
            WidgetUpdaterWorker.ACTION_UPDATEWIDGETS -> {
                WidgetUpdaterWorker.enqueueAction(this, WidgetUpdaterWorker.ACTION_UPDATEWIDGETS)
            }
            WeatherUpdaterWorker.ACTION_UPDATEWEATHER -> {
                WeatherUpdaterWorker.enqueueAction(this, WeatherUpdaterWorker.ACTION_UPDATEWEATHER)
            }
        }

        return START_STICKY
    }

    private fun doWork() {
        val nowMillis = System.currentTimeMillis()

        if (Duration.ofMillis(nowMillis - mLastWeatherUpdateTime).toMinutes() >= Settings.getRefreshInterval()) {
            Logger.writeLine(Log.INFO, "${TAG}: updating weather...")
            WeatherUpdaterWorker.enqueueAction(this, WeatherUpdaterWorker.ACTION_UPDATEWEATHER)
            mLastWeatherUpdateTime = nowMillis
            mLastWidgetUpdateTime = nowMillis
        } else if (Duration.ofMillis(nowMillis - mLastWidgetUpdateTime).toMinutes() >= 60) {
            Logger.writeLine(Log.INFO, "${TAG}: updating widgets...")
            WidgetUpdaterWorker.enqueueAction(this, WidgetUpdaterWorker.ACTION_UPDATEWIDGETS)
            mLastWidgetUpdateTime = nowMillis
        }
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