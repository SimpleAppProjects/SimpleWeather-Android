package com.thewizrd.simpleweather.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.thewizrd.common.controls.WeatherAlertViewModel
import com.thewizrd.common.utils.ImageUtils
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.appLib
import com.thewizrd.shared_resources.helpers.toImmutableCompatFlag
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.getColorFromAlertSeverity
import com.thewizrd.shared_resources.utils.getDrawableFromAlertType
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlert
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.main.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.ZonedDateTime

object WeatherAlertNotificationBuilder {
    private const val LOG_TAG = "WAlertNotifBuilder"

    // Sets an ID for the notification
    private const val TAG = "SimpleWeather.WeatherAlerts"
    private const val NOT_CHANNEL_ID = "SimpleWeather.weatheralerts"
    private const val MIN_GROUPCOUNT = 3
    private const val SUMMARY_ID = -1

    suspend fun createNotifications(location: LocationData, alerts: Collection<WeatherAlert>) = withContext(Dispatchers.Default) {
        val context = appLib.context
        // Gets an instance of the NotificationManager service
        val mNotifyMgr = NotificationManagerCompat.from(context)
        initChannel()

        // Create click intent
        // Start WeatherNow Activity with weather data
        val intent = Intent(context, MainActivity::class.java)
            .setAction(WeatherAlertNotificationService.ACTION_SHOWALERTS)
            .putExtra(Constants.KEY_DATA, JSONParser.serializer(location, LocationData::class.java))
            .putExtra(WeatherAlertNotificationService.ACTION_SHOWALERTS, true)
            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val clickPendingIntent = PendingIntent.getActivity(
            context,
            location.hashCode(),
            intent,
            0.toImmutableCompatFlag()
        )

        // Build update
        for (alert in alerts) {
            if (alert.date.isAfter(ZonedDateTime.now()))
                continue

            val alertVM = WeatherAlertViewModel(alert)

            val title = alertVM.title
            val contentText = alertVM.expireDate
            val alertIconResId = alertVM.alertType.getDrawableFromAlertType()

            val notification = NotificationUtils.createNotificationBuilder(context, NOT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_error)
                .setStyle(Notification.BigTextStyle().bigText(alertVM.alertBodyMessage))
                .setContentTitle(title)
                .setContentText(contentText)
                .setSubText(location.name)
                .setContentIntent(clickPendingIntent)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .setColor(alert.severity.getColorFromAlertSeverity())
                .setPriority(Notification.PRIORITY_DEFAULT).apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        setLargeIcon(Icon.createWithResource(context, alertIconResId))
                    } else {
                        setLargeIcon(ImageUtils.bitmapFromDrawable(context, alertIconResId))
                    }

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                        // Tell service to remove stored notification
                        setDeleteIntent(getDeleteNotificationIntent(alertVM.alertType.value))
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                        || WeatherAlertNotificationService.getNotificationsCount() >= MIN_GROUPCOUNT
                    ) {
                        setGroup(TAG)
                    }
                }.build()

            // Builds the notification and issues it.
            // Tag: location.query; id: weather alert type
            val notId = (SystemClock.uptimeMillis() + alertVM.alertType.value).toInt()
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
                WeatherAlertNotificationService.addNotification(notId, title)

            mNotifyMgr.notify(TAG, notId, notification)
        }

        var buildSummary = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val mNotifyMgrV23 = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val statNotifs = mNotifyMgrV23.activeNotifications
                if (statNotifs != null && statNotifs.isNotEmpty()) {
                    var count = 0
                    for (not in statNotifs) {
                        if (TAG == not.tag) count++
                    }
                    buildSummary = count >= MIN_GROUPCOUNT
                }
            } catch (ex: Exception) {
                Timber.tag(LOG_TAG).d(ex, "error accessing notifications")
            }
        } else {
            buildSummary = WeatherAlertNotificationService.getNotificationsCount() >= MIN_GROUPCOUNT
        }

        if (buildSummary) {
            // Notification inboxStyle for grouped notifications
            val inboxStyle = NotificationCompat.InboxStyle()

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                // Add active notification titles to summary notification
                val notifsSet = WeatherAlertNotificationService.getNotifications()
                val iterator: Iterator<Map.Entry<Int, String>> = notifsSet.iterator()
                while (iterator.hasNext()) {
                    val notif = iterator.next()
                    mNotifyMgr.cancel(TAG, notif.key)
                    inboxStyle.addLine(notif.value)
                }

                inboxStyle.setBigContentTitle(context.getString(R.string.title_fragment_alerts))
                inboxStyle.setSummaryText(context.getString(R.string.app_name))
            } else {
                inboxStyle.setSummaryText(context.getString(R.string.title_fragment_alerts))
            }

            val mSummaryBuilder = NotificationCompat.Builder(context, NOT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_error)
                    .setContentTitle(context.getString(R.string.title_fragment_alerts))
                    .setContentText(context.getString(R.string.app_name))
                    .setStyle(inboxStyle)
                    .setGroup(TAG)
                    .setGroupSummary(true)
                    .setOnlyAlertOnce(true)
                    .setAutoCancel(true)
                    .setColor(Colors.SIMPLEBLUE)

            /*
             * NOTE
             * Compat issue: setAutoCancel does not work
             * If user clicks notification, delete intent is not called
             * (swipe to cancel works fine)
             *
             * Workaround: add content intent to do delete action and also start activity
             * by sending an extra to do so to the BroadcastReceiver
             */
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                mSummaryBuilder.setContentIntent(getDeleteAllNotificationsIntentJB())
                mSummaryBuilder.setDeleteIntent(getDeleteAllNotificationsIntent())
            } else {
                mSummaryBuilder.setContentIntent(clickPendingIntent)
            }

            // Builds the summary notification and issues it.
            mNotifyMgr.notify(TAG, SUMMARY_ID, mSummaryBuilder.build())
        }
    }

    private fun getDeleteNotificationIntent(notId: Int): PendingIntent {
        val context = appLib.context
        val intent = Intent(context, WeatherAlertNotificationBroadcastReceiver::class.java)
            .setAction(WeatherAlertNotificationService.ACTION_CANCELNOTIFICATION)
            .putExtra(WeatherAlertNotificationService.EXTRA_NOTIFICATIONID, notId)

        // Use notification id as unique request code
        return PendingIntent.getBroadcast(
            context,
            notId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT.toImmutableCompatFlag()
        )
    }

    private fun getDeleteAllNotificationsIntent(): PendingIntent {
        val context = appLib.context
        val intent = Intent(context, WeatherAlertNotificationBroadcastReceiver::class.java)
            .setAction(WeatherAlertNotificationService.ACTION_CANCELALLNOTIFICATIONS)
        return PendingIntent.getBroadcast(
            context,
            19,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT.toImmutableCompatFlag()
        )
    }

    private fun getDeleteAllNotificationsIntentJB(): PendingIntent {
        val context = appLib.context
        val intent = Intent(context, WeatherAlertNotificationBroadcastReceiver::class.java)
            .setAction(WeatherAlertNotificationService.ACTION_CANCELALLNOTIFICATIONS)
            .putExtra(WeatherAlertNotificationService.ACTION_SHOWALERTS, true)
        return PendingIntent.getBroadcast(
            context,
            16,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT.toImmutableCompatFlag()
        )
    }

    private fun initChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val context = appLib.context
            val mNotifyMgr =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            var mChannel = mNotifyMgr.getNotificationChannel(NOT_CHANNEL_ID)
            val notchannel_name = context.resources.getString(R.string.not_channel_name_alerts)
            val notchannel_desc = context.resources.getString(R.string.not_channel_desc_alerts)
            if (mChannel == null) {
                mChannel = NotificationChannel(NOT_CHANNEL_ID, notchannel_name, NotificationManager.IMPORTANCE_DEFAULT)
            }
            mChannel.name = notchannel_name
            mChannel.description = notchannel_desc
            // Configure the notification channel.
            mChannel.setShowBadge(true)
            mChannel.enableLights(true)
            mChannel.enableVibration(false)
            mNotifyMgr.createNotificationChannel(mChannel)
        }
    }
}