package com.thewizrd.simpleweather.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.notifications.NotificationUtils

internal object ServiceNotificationHelper {
    internal const val JOB_ID = 1000
    internal const val NOT_CHANNEL_ID = "SimpleWeather.generalnotif"

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

    internal fun createForegroundNotification(context: Context): Notification {
        val notif = NotificationCompat.Builder(context, NOT_CHANNEL_ID).apply {
            setSmallIcon(R.drawable.wi_cloud_refresh)
            setSubText(context.getString(R.string.app_name))
            setContentTitle(context.getString(R.string.message_widgetservice_running))
            setOnlyAlertOnce(true)
            setNotificationSilent()
            setShowWhen(false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationUtils.getAppNotificationChannelSettingsActivityIntent(context, NOT_CHANNEL_ID).also {
                    if (it.resolveActivity(context.packageManager) != null) {
                        setContentIntent(it.toPendingActivity(context))
                    }
                }
            } else {
                NotificationUtils.getAppSettingsActivityIntent(context).also {
                    if (it.resolveActivity(context.packageManager) != null) {
                        setContentIntent(it.toPendingActivity(context))
                    }
                }
            }
            priority = NotificationCompat.PRIORITY_LOW
        }

        return notif.build()
    }

    internal fun Intent.toPendingActivity(context: Context): PendingIntent {
        return PendingIntent.getActivity(context, 0, this, PendingIntent.FLAG_UPDATE_CURRENT)
    }
}