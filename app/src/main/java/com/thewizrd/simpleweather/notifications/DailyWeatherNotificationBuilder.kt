package com.thewizrd.simpleweather.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import com.thewizrd.common.controls.ForecastItemViewModel
import com.thewizrd.common.controls.WeatherDetailsType
import com.thewizrd.common.utils.ImageUtils
import com.thewizrd.shared_resources.helpers.toImmutableCompatFlag
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.icons.WeatherIconsEFProvider
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.weatherdata.model.Forecast
import com.thewizrd.simpleweather.main.MainActivity

object DailyWeatherNotificationBuilder {
    @JvmStatic
    fun createNotification(context: Context, notChannelID: String, location: LocationData,
                           forecast: Forecast): Notification {
        val wim = sharedDeps.weatherIconsManager

        val viewModel = ForecastItemViewModel(forecast)
        val hiTemp = viewModel.hiTemp ?: WeatherIcons.PLACEHOLDER
        val loTemp = viewModel.loTemp ?: WeatherIcons.PLACEHOLDER
        val condition = viewModel.condition ?: WeatherIcons.EM_DASH

        val chanceModel = viewModel.extras[WeatherDetailsType.POPCHANCE]
        val feelsLikeModel = viewModel.extras[WeatherDetailsType.FEELSLIKE]

        val contentText = StringBuilder().append(condition)
        var appendDiv = false
        var appendLine = true
        feelsLikeModel?.let {
            if (appendLine) {
                contentText.appendLine()
                appendLine = false
            }
            if (appendDiv) {
                contentText.append(" ${WeatherIcons.PLACEHOLDER} ")
            }
            contentText.append("${feelsLikeModel.label}: ${feelsLikeModel.value}")
            appendDiv = true
        }
        chanceModel?.let {
            if (appendLine) {
                contentText.appendLine()
                appendLine = false
            }
            if (appendDiv) {
                contentText.append(" ${WeatherIcons.PLACEHOLDER} ")
            }
            contentText.append("${chanceModel.label}: ${chanceModel.value}")
            appendDiv = true
        }

        val contentIntent = getOnClickIntent(context, location)
        val contentTitle = "$hiTemp / $loTemp - ${location.name.split(",")[0]}"
        val weatherIconResId = wim.getWeatherIconResource(forecast.icon)

        val notifBuilder = NotificationUtils.createNotificationBuilder(context, notChannelID)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setPriority(Notification.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setStyle(Notification.BigTextStyle().bigText(contentText))

        if (wim.isFontIcon) {
            notifBuilder.setSmallIcon(weatherIconResId)
        } else {
            // Use default icon pack here; animated icons are not supported here
            val wip = wim.getIconProvider(WeatherIconsEFProvider.KEY)
            notifBuilder.setSmallIcon(wip.getWeatherIconResource(forecast.icon))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notifBuilder.setLargeIcon(Icon.createWithResource(context, weatherIconResId))
        } else {
            notifBuilder.setLargeIcon(ImageUtils.bitmapFromDrawable(context, weatherIconResId))
        }

        return notifBuilder.build()
    }

    private fun getOnClickIntent(context: Context, location: LocationData): PendingIntent {
        // When user clicks on widget, launch to WeatherNow page
        val onClickIntent = Intent(context.applicationContext, MainActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

        return PendingIntent.getActivity(
            context, location.hashCode(),
            onClickIntent, PendingIntent.FLAG_UPDATE_CURRENT.toImmutableCompatFlag()
        )
    }
}