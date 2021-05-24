package com.thewizrd.simpleweather.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.thewizrd.shared_resources.controls.DetailItemViewModel
import com.thewizrd.shared_resources.controls.ForecastItemViewModel
import com.thewizrd.shared_resources.controls.WeatherDetailsType
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.icons.WeatherIconsManager
import com.thewizrd.shared_resources.icons.WeatherIconsProvider
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.ImageUtils
import com.thewizrd.shared_resources.weatherdata.model.Forecast
import com.thewizrd.simpleweather.main.MainActivity

object DailyWeatherNotificationBuilder {
    @JvmStatic
    fun createNotification(context: Context, notChannelID: String, location: LocationData,
                           forecast: Forecast): Notification {
        val wim = WeatherIconsManager.getInstance()

        val viewModel = ForecastItemViewModel(forecast)
        val hiTemp = viewModel.hiTemp ?: WeatherIcons.PLACEHOLDER
        val loTemp = viewModel.loTemp ?: WeatherIcons.PLACEHOLDER
        val condition = viewModel.condition ?: WeatherIcons.EM_DASH

        var chanceModel: DetailItemViewModel? = null
        var feelsLikeModel: DetailItemViewModel? = null
        for (model in viewModel.extras) {
            if (model.detailsType == WeatherDetailsType.POPCHANCE) {
                chanceModel = model
            } else if (model.detailsType == WeatherDetailsType.FEELSLIKE) {
                feelsLikeModel = model
            }

            if (chanceModel != null && feelsLikeModel != null)
                break
        }

        val contentText = StringBuilder().append(condition)
        var appendDiv = false
        var appendLine = true
        feelsLikeModel?.let {
            if (appendLine) {
                contentText.appendLine()
                appendLine = false
            }
            if (appendDiv) {
                contentText.append("; ")
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
                contentText.append("; ")
            }
            contentText.append("${chanceModel.label}: ${chanceModel.value}")
            appendDiv = true
        }

        val notifBuilder = NotificationCompat.Builder(context, notChannelID)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(getOnClickIntent(context, location))
                .setContentTitle("$hiTemp / $loTemp - ${location.name.split(",")[0]}")
                .setContentText(contentText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))

        if (wim.isFontIcon) {
            notifBuilder.setSmallIcon(wim.getWeatherIconResource(forecast.icon))
        } else {
            // Use default icon pack here; animated icons are not supported here
            val wip = WeatherIconsManager.getProvider(WeatherIconsProvider.KEY)
            notifBuilder.setSmallIcon(wip.getWeatherIconResource(forecast.icon))
        }

        notifBuilder.setLargeIcon(ImageUtils.bitmapFromDrawable(context, wim.getWeatherIconResource(forecast.icon)))

        return notifBuilder.build()
    }

    private fun getOnClickIntent(context: Context, location: LocationData): PendingIntent {
        // When user clicks on widget, launch to WeatherNow page
        val onClickIntent = Intent(context.applicationContext, MainActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

        return PendingIntent.getActivity(context, location.hashCode(),
                onClickIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    }
}