package com.thewizrd.simpleweather.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.thewizrd.simpleweather.R

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
            mChannel = NotificationChannel(
                NOT_CHANNEL_ID,
                notchannel_name,
                NotificationManager.IMPORTANCE_LOW
            )
        }

        // Configure the notification channel.
        mChannel.name = notchannel_name
        mChannel.setShowBadge(false)
        mChannel.enableLights(false)
        mChannel.enableVibration(false)
        mNotifyMgr.createNotificationChannel(mChannel)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    internal fun getForegroundNotification(context: Context): Notification {
        return NotificationCompat.Builder(context, NOT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logo_stroke)
            .setContentTitle(context.getString(R.string.not_title_weather_update))
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setOnlyAlertOnce(true)
            .setNotificationSilent()
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}