package com.thewizrd.simpleweather.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.thewizrd.shared_resources.utils.Settings
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.notifications.NotificationUtils
import java.time.Duration

class WeatherUpdaterService : Service() {
    private lateinit var mNotificationManager: NotificationManager
    private lateinit var mAlarmMgr: AlarmManager

    private var mAlarmStarted = false
    private var mLastWeatherUpdateTime: Long = -1

    companion object {
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

        mAlarmMgr = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            initChannel(this)
        }

        startForeground(JOB_ID, createForegroundNotification())
    }

    private fun createForegroundNotification(): Notification {
        val notif = NotificationCompat.Builder(this, NOT_CHANNEL_ID).apply {
            setSmallIcon(R.drawable.ic_wi_cloud_refresh)
            setContentTitle(getString(R.string.app_name))
            setContentText(getString(R.string.message_widgetservice_running))
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
        when (intent?.action) {
            ACTION_STARTALARM -> {
                // Start alarm if it hasn't started already
                startAlarm()
                requestUpdate()
            }
            ACTION_UPDATEALARM -> {
                // Refresh interval was changed
                // Update alarm
                updateAlarm()
                requestUpdate()
            }
            ACTION_CANCELALARM -> {
                // Cancel clock alarm
                cancelAlarm()
            }
            ACTION_REQUESTUPDATE -> {
                requestUpdate()
            }
            WidgetUpdaterWorker.ACTION_UPDATEWIDGETS -> {
                requestWidgetUpdate()
            }
            WeatherUpdaterWorker.ACTION_UPDATEWEATHER -> {
                requestWeatherUpdate()
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun requestUpdate() {
        val nowMillis = System.currentTimeMillis()

        if (Duration.ofMillis(nowMillis - mLastWeatherUpdateTime).toMinutes() >= Settings.getRefreshInterval()) {
            requestWeatherUpdate()
        } else {
            requestWidgetUpdate()
        }
    }

    private fun requestWeatherUpdate() {
        WeatherUpdaterWorker.enqueueAction(this, WeatherUpdaterWorker.ACTION_UPDATEWEATHER)
    }

    private fun requestWidgetUpdate() {
        WidgetUpdaterWorker.enqueueAction(this, WidgetUpdaterWorker.ACTION_UPDATEWIDGETS)
    }

    private fun startAlarm() {
        if (!mAlarmStarted) {
            updateAlarm()
            mAlarmStarted = true
        }
    }

    private fun updateAlarm() {
        val nowMillis = System.currentTimeMillis()
        val nextAlarmTime = nowMillis + Duration.ofHours(1).toMillis()

        mAlarmMgr.cancel(getAlarmIntent())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mAlarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAlarmTime, getAlarmIntent())
        } else {
            mAlarmMgr.setExact(AlarmManager.RTC_WAKEUP, nextAlarmTime, getAlarmIntent())
        }

        mAlarmStarted = true
    }

    private fun cancelAlarm() {
        mAlarmMgr.cancel(getAlarmIntent())
        mAlarmStarted = false
        stopSelf()
    }

    private fun getAlarmIntent(): PendingIntent {
        val intent = Intent(this, this::class.java).setAction(ACTION_REQUESTUPDATE)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(this, intent.filterHashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT)
        } else {
            PendingIntent.getService(this, intent.filterHashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    override fun onDestroy() {
        stopForeground(true)
        super.onDestroy()
    }
}