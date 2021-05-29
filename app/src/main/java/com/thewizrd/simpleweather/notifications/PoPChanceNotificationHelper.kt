package com.thewizrd.simpleweather.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.icons.WeatherIconsManager
import com.thewizrd.shared_resources.icons.WeatherIconsProvider
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.SettingsManager
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.main.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

object PoPChanceNotificationHelper {
    // Sets an ID for the notification
    private const val NOT_CHANNEL_ID = "SimpleWeather.chancenotification"

    suspend fun postNotification(context: Context) {
        val settingsManager = SettingsManager(context.applicationContext)

        if (!settingsManager.isPoPChanceNotificationEnabled() || !settingsManager.isWeatherLoaded()) return

        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val lastPostTime = settingsManager.getLastPoPChanceNotificationTime()

        // We already posted today; post any chance tomorrow
        if (now.toLocalDate().isEqual(lastPostTime.withZoneSameInstant(ZoneOffset.UTC).toLocalDate())) return

        // Get the forecast for the next 12 hours
        val location: LocationData?
        val hrForecasts = withContext(Dispatchers.IO) {
            location = settingsManager.getHomeData()
            if (location == null) return@withContext null
            val nowHour = now.withZoneSameInstant(location.tzOffset).truncatedTo(ChronoUnit.HOURS)
            settingsManager.getHourlyForecastsByQueryOrderByDateByLimitFilterByDate(location.query, 12, nowHour)
        }

        if (hrForecasts.isNullOrEmpty() || location == null) return

        // Find the next hour with a 60% or higher chance of precipitation
        val hrf = hrForecasts.find { it.extras?.pop != null && it.extras.pop >= 60 }

        // Proceed if within the next 3hrs
        if (hrf == null || Duration.between(now.truncatedTo(ChronoUnit.HOURS), hrf.date).toHours() > 3) return

        createNotification(context, location, hrf, now.truncatedTo(ChronoUnit.HOURS))

        settingsManager.setLastPoPChanceNotificationTime(now)
    }

    private fun createNotification(context: Context, location: LocationData, forecast: HourlyForecast, now: ZonedDateTime) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            initChannel(context)
        }

        val wim = WeatherIconsManager.getInstance()

        // Should be within 0-3 hours
        val duration = Duration.between(now, forecast.date).toMinutes()
        val duraStr = if (duration <= 60) {
            context.getString(R.string.precipitation_nexthour_text_format, forecast.extras.pop)
        } else if (duration < 120) {
            context.getString(R.string.precipitation_text_format, forecast.extras.pop,
                    context.getString(R.string.refresh_30min).replace("30", duration.toString()))
        } else {
            context.getString(R.string.precipitation_text_format, forecast.extras.pop,
                    context.getString(R.string.refresh_12hrs).replace("12", (duration / 60).toString()))
        }

        val notifBuilder = NotificationCompat.Builder(context, NOT_CHANNEL_ID)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(getOnClickIntent(context, location))
                .setContentTitle(duraStr)
                .setContentText(location.name)

        if (wim.isFontIcon) {
            notifBuilder.setSmallIcon(wim.getWeatherIconResource(WeatherIcons.UMBRELLA))
        } else {
            // Use default icon pack here; animated icons are not supported here
            val wip = WeatherIconsManager.getProvider(WeatherIconsProvider.KEY)
            notifBuilder.setSmallIcon(wip.getWeatherIconResource(WeatherIcons.UMBRELLA))
        }

        val notifMgr = context.getSystemService<NotificationManager>()!!
        notifMgr.notify(NOT_CHANNEL_ID, location.hashCode(), notifBuilder.build())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initChannel(context: Context) {
        // Gets an instance of the NotificationManager service
        val mNotifyMgr = context.getSystemService(NotificationManager::class.java)
        var mChannel = mNotifyMgr.getNotificationChannel(NOT_CHANNEL_ID)
        val notChannelName = context.getString(R.string.not_channel_name_precipnotification)
        if (mChannel == null) {
            mChannel = NotificationChannel(NOT_CHANNEL_ID, notChannelName, NotificationManager.IMPORTANCE_DEFAULT)
        }

        // Configure the notification channel.
        mChannel.name = notChannelName
        mNotifyMgr.createNotificationChannel(mChannel)
    }

    private fun getOnClickIntent(context: Context, location: LocationData): PendingIntent {
        // When user clicks on widget, launch to WeatherNow page
        val onClickIntent = Intent(context.applicationContext, MainActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

        return PendingIntent.getActivity(context, location.hashCode(),
                onClickIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    }
}