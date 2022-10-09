package com.thewizrd.simpleweather.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.core.app.NotificationCompat
import com.thewizrd.common.controls.*
import com.thewizrd.common.utils.ImageUtils.bitmapFromDrawable
import com.thewizrd.common.utils.ImageUtils.rotateBitmap
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.icons.WeatherIconsEFProvider
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.preferences.SettingsManager
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ConversionMethods
import com.thewizrd.shared_resources.utils.StringUtils.containsDigits
import com.thewizrd.shared_resources.utils.StringUtils.isNullOrWhitespace
import com.thewizrd.shared_resources.utils.StringUtils.removeNonDigitChars
import com.thewizrd.shared_resources.utils.Units
import com.thewizrd.shared_resources.utils.getColorFromTempF
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.main.MainActivity
import com.thewizrd.weather_api.weatherModule
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.min

object WeatherNotificationBuilder {
    private const val TAG = "WeatherNotificationBuilder"
    private const val MAX_FORECASTS = 6

    suspend fun updateNotification(
        context: Context,
        notChannelID: String,
        location: LocationData,
        viewModel: WeatherUiModel
    ): Notification {
        val wim = sharedDeps.weatherIconsManager
        val wip = wim.getIconProvider(WeatherIconsEFProvider.KEY)

        // Build update
        val updateViews = RemoteViews(
            context.packageName, if (wim.isFontIcon) {
                R.layout.weather_notification_layout_tintable
            } else {
                R.layout.weather_notification_layout
            }
        )

        val condition = viewModel.curCondition
        val hiTemp = viewModel.hiTemp?.removeNonDigitChars()
        val loTemp = viewModel.loTemp?.removeNonDigitChars()
        val temp = viewModel.curTemp?.removeNonDigitChars() ?: WeatherIcons.PLACEHOLDER
        val weatherIconResId = wim.getWeatherIconResource(viewModel.weatherIcon)

        // Weather icon
        updateViews.setImageViewResource(R.id.weather_icon, weatherIconResId)

        // Location Name
        updateViews.setTextViewText(R.id.location_name, viewModel.location)

        // Condition text
        updateViews.setTextViewText(
            R.id.condition_weather,
            String.format(
                "%s° - %s",
                temp.ifBlank { WeatherIcons.PLACEHOLDER },
                condition
            )
        )

        // Details
        // Get extras
        val chanceModel = viewModel.weatherDetailsMap[WeatherDetailsType.POPCHANCE]
            ?: viewModel.weatherDetailsMap[WeatherDetailsType.POPCLOUDINESS]
        val windModel = viewModel.weatherDetailsMap[WeatherDetailsType.WINDSPEED]
        val windGustModel = viewModel.weatherDetailsMap[WeatherDetailsType.WINDGUST]
        val feelsLikeModel = viewModel.weatherDetailsMap[WeatherDetailsType.FEELSLIKE]
        val humidityModel = viewModel.weatherDetailsMap[WeatherDetailsType.HUMIDITY]

        val conditionTempBuilder = SpannableStringBuilder()

        if (viewModel.isShowHiLo) {
            conditionTempBuilder.append(
                String.format(
                    "%s / %s",
                    if (hiTemp.containsDigits()) "$hiTemp°" else WeatherIcons.PLACEHOLDER,
                    if (loTemp.containsDigits()) "$loTemp°" else WeatherIcons.PLACEHOLDER
                )
            )
        }

        if (feelsLikeModel != null) {
            if (conditionTempBuilder.isNotEmpty()) {
                conditionTempBuilder.append(" ${WeatherIcons.PLACEHOLDER} ")
            }

            conditionTempBuilder.append("${feelsLikeModel.label}: ${feelsLikeModel.value}")
        }

        if (conditionTempBuilder.isNotEmpty()) {
            updateViews.setTextViewText(R.id.condition_temp, conditionTempBuilder)
        } else {
            updateViews.setViewVisibility(R.id.condition_temp, View.GONE)
        }

        if (chanceModel != null) {
            updateViews.setImageViewResource(
                R.id.weather_popicon,
                wip.getWeatherIconResource(chanceModel.icon)
            )
            updateViews.setTextViewText(R.id.weather_pop, chanceModel.value)
            updateViews.setViewVisibility(R.id.weather_pop_layout, View.VISIBLE)
        } else {
            updateViews.setViewVisibility(R.id.weather_pop_layout, View.GONE)
        }

        // Extras
        val bigUpdateViews = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            RemoteViews(updateViews)
        } else {
            updateViews.clone()
        }

        if (windModel != null) {
            val windIconResId = wip.getWeatherIconResource(WeatherIcons.WIND_DIRECTION)
            if (windModel.iconRotation != 0) {
                bigUpdateViews.setImageViewBitmap(
                    R.id.weather_windicon,
                    rotateBitmap(
                        bitmapFromDrawable(context, windIconResId),
                        windModel.iconRotation.toFloat()
                    )
                )
            } else {
                bigUpdateViews.setImageViewResource(R.id.weather_windicon, windIconResId)
            }
            var speed = if (TextUtils.isEmpty(windModel.value)) "" else windModel.value.toString()
            speed = speed.split(",".toRegex()).first()
            bigUpdateViews.setTextViewText(R.id.weather_windspeed, speed)
            bigUpdateViews.setViewVisibility(R.id.weather_wind_layout, View.VISIBLE)
        } else {
            bigUpdateViews.setViewVisibility(R.id.weather_wind_layout, View.GONE)
        }

        if (humidityModel != null) {
            bigUpdateViews.setImageViewResource(
                R.id.humidity_icon,
                wip.getWeatherIconResource(humidityModel.icon)
            )
            bigUpdateViews.setTextViewText(R.id.humidity, humidityModel.value)
            bigUpdateViews.setViewVisibility(R.id.humidity_layout, View.VISIBLE)
        }

        if (windGustModel != null) {
            bigUpdateViews.setImageViewResource(
                R.id.windgust_icon,
                wip.getWeatherIconResource(windGustModel.icon)
            )
            bigUpdateViews.setTextViewText(R.id.windgust, windGustModel.value)
            bigUpdateViews.setViewVisibility(R.id.windgust_layout, View.VISIBLE)
        }

        bigUpdateViews.setViewVisibility(
            R.id.extra_layout,
            if (windModel != null || humidityModel != null || windGustModel != null) View.VISIBLE else View.GONE
        )

        val smallIconResId = when (settingsManager.getNotificationIcon()) {
            SettingsManager.TEMPERATURE_ICON -> {
                val tempLevel = temp.replace("°", "").toIntOrNull()
                if (tempLevel == null) {
                    R.drawable.notification_temp_unknown
                } else {
                    WeatherNotificationTemp.getTempDrawable(tempLevel)
                }
            }
            else /* SettingsManager.CONDITION_ICON */ -> {
                if (wim.isFontIcon) {
                    weatherIconResId
                } else {
                    // Use default icon pack here; animated icons are not supported here
                    wip.getWeatherIconResource(viewModel.weatherIcon)
                }
            }
        }

        val isTempIcon = settingsManager.getNotificationIcon() == SettingsManager.TEMPERATURE_ICON

        val contentIntent: PendingIntent = run {
            val onClickIntent = Intent(context, MainActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            var flags = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                flags = flags or PendingIntent.FLAG_IMMUTABLE
            PendingIntent.getActivity(context, 0, onClickIntent, flags)
        }

        // Builds the notification and issues it.
        val notifColor = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            val tempFloat = temp.toFloatOrNull()
            if (tempFloat != null) {
                val tempF = if (viewModel.curTemp!!.endsWith(Units.FAHRENHEIT)) {
                    tempFloat
                } else {
                    ConversionMethods.CtoF(tempFloat)
                }
                getColorFromTempF(tempF)
            } else {
                Colors.SIMPLEBLUE
            }
        } else {
            0 // Don't colorize; use Material You colors
        }

        val forecastType = settingsManager.getNotificationForecastType()
        val forecastItemLayoutId = if (wim.isFontIcon) {
            R.layout.weather_notification_forecast_item_tintable
        } else {
            R.layout.weather_notification_forecast_item
        }

        when (forecastType) {
            SettingsManager.NOTIF_FORECAST_NONE -> {
                bigUpdateViews.setViewVisibility(R.id.forecast_layout, View.GONE)
            }
            SettingsManager.NOTIF_FORECAST_DAILY -> {
                val forecasts = getForecasts(location, MAX_FORECASTS - 1)

                bigUpdateViews.removeAllViews(R.id.forecast_container)
                forecasts.forEach {
                    addForecastItem(
                        bigUpdateViews,
                        R.id.forecast_container,
                        forecastItemLayoutId,
                        it
                    )
                }

                bigUpdateViews.setViewVisibility(
                    R.id.forecast_layout,
                    if (forecasts.isNotEmpty()) View.VISIBLE else View.GONE
                )
            }
            SettingsManager.NOTIF_FORECAST_HOURLY -> {
                val hourlyForecasts = getHourlyForecasts(location, MAX_FORECASTS)

                bigUpdateViews.removeAllViews(R.id.forecast_container)
                hourlyForecasts.forEach {
                    addForecastItem(
                        bigUpdateViews,
                        R.id.forecast_container,
                        forecastItemLayoutId,
                        it
                    )
                }

                bigUpdateViews.setViewVisibility(
                    R.id.forecast_layout,
                    if (hourlyForecasts.isNotEmpty()) View.VISIBLE else View.GONE
                )
            }
        }

        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            NotificationCompat.Builder(context, notChannelID)
                .setCustomContentView(updateViews)
                .setCustomBigContentView(bigUpdateViews)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setShowWhen(false)
                .setSmallIcon(smallIconResId)
                .setColor(notifColor)
                .setContentIntent(contentIntent)
                .build()
        } else {
            // Android 12 limits customizable notification area
            val contentTitle = if (isTempIcon) {
                viewModel.location
            } else {
                String.format(
                    "%s° - %s",
                    if (!temp.isNullOrWhitespace()) temp else WeatherIcons.PLACEHOLDER,
                    viewModel.location
                )
            }
            val contentText = if (feelsLikeModel != null) {
                "$condition ${WeatherIcons.PLACEHOLDER} ${feelsLikeModel.label} ${feelsLikeModel.value}"
            } else {
                condition
            }

            Notification.Builder(context, notChannelID)
                .setCustomContentView(null)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setCustomBigContentView(bigUpdateViews)
                .setOngoing(true)
                .setShowWhen(true)
                .setSmallIcon(smallIconResId)
                .setLargeIcon(Icon.createWithResource(context, weatherIconResId))
                .setColor(notifColor)
                .setContentIntent(contentIntent)
                .build()
        }
    }

    private fun addForecastItem(
        remoteViews: RemoteViews,
        @IdRes containerId: Int,
        @LayoutRes layoutId: Int,
        forecast: BaseForecastItemViewModel
    ) {
        val forecastItem = RemoteViews(remoteViews.`package`, layoutId)

        forecastItem.setTextViewText(R.id.forecast_date, forecast.shortDate)
        forecastItem.setTextViewText(R.id.forecast_hi, forecast.hiTemp)
        if (forecast is ForecastItemViewModel) {
            forecastItem.setTextViewText(R.id.forecast_lo, forecast.loTemp)
        }

        // WeatherIcon
        val wim = sharedDeps.weatherIconsManager
        val weatherIconResId = wim.getWeatherIconResource(forecast.weatherIcon)
        forecastItem.setImageViewResource(R.id.forecast_icon, weatherIconResId)

        if (forecast is HourlyForecastItemViewModel) {
            forecastItem.setViewVisibility(R.id.forecast_divider, View.GONE)
            forecastItem.setViewVisibility(R.id.forecast_lo, View.GONE)
        }

        remoteViews.addView(containerId, forecastItem)
    }

    private suspend fun getForecasts(
        locData: LocationData,
        forecastLength: Int
    ): List<ForecastItemViewModel> {
        val fcastData = settingsManager.getWeatherForecastData(locData.query)
        val fcasts = fcastData?.forecast

        if (fcasts?.isNotEmpty() == true) {
            return ArrayList<ForecastItemViewModel>(forecastLength).also {
                for (i in 0 until min(forecastLength, fcasts.size)) {
                    it.add(ForecastItemViewModel(fcasts[i]))
                }
            }
        }

        return emptyList()
    }

    private suspend fun getHourlyForecasts(
        locData: LocationData,
        forecastLength: Int
    ): List<HourlyForecastItemViewModel> {
        val now = ZonedDateTime.now().withZoneSameInstant(locData.tzOffset)
        val hrInterval = weatherModule.weatherManager.getHourlyForecastInterval()
        val hrfcasts = settingsManager.getHourlyForecastsByQueryOrderByDateByLimitFilterByDate(
            locData.query,
            forecastLength,
            now.minusHours((hrInterval * 0.5).toLong()).truncatedTo(ChronoUnit.HOURS)
        )

        if (hrfcasts.isNotEmpty()) {
            return ArrayList<HourlyForecastItemViewModel>(forecastLength).apply {
                var count = 0
                for (fcast in hrfcasts) {
                    add(HourlyForecastItemViewModel(fcast))
                    count++

                    if (count >= forecastLength) break
                }
            }
        }

        return emptyList()
    }
}