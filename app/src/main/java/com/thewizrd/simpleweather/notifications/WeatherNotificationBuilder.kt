package com.thewizrd.simpleweather.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.text.TextUtils
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.thewizrd.shared_resources.controls.DetailItemViewModel
import com.thewizrd.shared_resources.controls.WeatherDetailsType
import com.thewizrd.shared_resources.controls.WeatherNowViewModel
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.icons.WeatherIconsManager
import com.thewizrd.shared_resources.icons.WeatherIconsProvider
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.utils.ImageUtils.bitmapFromDrawable
import com.thewizrd.shared_resources.utils.ImageUtils.rotateBitmap
import com.thewizrd.shared_resources.utils.StringUtils.containsDigits
import com.thewizrd.shared_resources.utils.StringUtils.isNullOrWhitespace
import com.thewizrd.shared_resources.utils.StringUtils.removeNonDigitChars
import com.thewizrd.simpleweather.App
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.main.MainActivity

object WeatherNotificationBuilder {
    private const val TAG = "WeatherNotificationBuilder"

    fun updateNotification(notChannelID: String, viewModel: WeatherNowViewModel): Notification {
        val context = App.instance.appContext
        val settingsManager = App.instance.settingsManager
        val wim = WeatherIconsManager.getInstance()

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
                if (temp.isNotBlank()) temp else WeatherIcons.PLACEHOLDER,
                condition
            )
        )

        // Details
        updateViews.setTextViewText(
            R.id.condition_hilo,
            String.format(
                "%s / %s",
                if (hiTemp.containsDigits()) "$hiTemp°" else WeatherIcons.PLACEHOLDER,
                if (loTemp.containsDigits()) "$loTemp°" else WeatherIcons.PLACEHOLDER
            )
        )
        updateViews.setViewVisibility(
            R.id.condition_hilo_layout,
            if (viewModel.isShowHiLo) View.VISIBLE else View.GONE
        )

        // Get extras
        var chanceModel: DetailItemViewModel? = null
        var windModel: DetailItemViewModel? = null
        var windGustModel: DetailItemViewModel? = null
        var feelsLikeModel: DetailItemViewModel? = null
        var humidityModel: DetailItemViewModel? = null
        for (input in viewModel.getWeatherDetails()) {
            if (input.detailsType == WeatherDetailsType.POPCHANCE) {
                chanceModel = input
            } else if (chanceModel == null && input.detailsType == WeatherDetailsType.POPCLOUDINESS) {
                chanceModel = input
            } else if (input.detailsType == WeatherDetailsType.WINDSPEED) {
                windModel = input
            } else if (input.detailsType == WeatherDetailsType.WINDGUST) {
                windGustModel = input
            } else if (input.detailsType == WeatherDetailsType.FEELSLIKE) {
                feelsLikeModel = input
            } else if (input.detailsType == WeatherDetailsType.HUMIDITY) {
                humidityModel = input
            }

            if (chanceModel != null && windModel != null && windGustModel != null && feelsLikeModel != null && humidityModel != null) {
                break
            }
        }

        if (feelsLikeModel != null) {
            updateViews.setTextViewText(
                R.id.feelslike_temp,
                String.format(
                    "%s: %s",
                    context.getString(R.string.label_feelslike),
                    feelsLikeModel.value.toString()
                )
            )
            updateViews.setViewVisibility(R.id.feelslike_temp, View.VISIBLE)
        }

        // Extras
        val bigUpdateViews = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            RemoteViews(updateViews)
        } else {
            updateViews.clone()
        }
        if (windModel != null) {
            val windIconResId = wim.getWeatherIconResource(WeatherIcons.WIND_DIRECTION)
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
                wim.getWeatherIconResource(humidityModel.icon)
            )
            bigUpdateViews.setTextViewText(R.id.humidity, humidityModel.value)
            bigUpdateViews.setViewVisibility(R.id.humidity_layout, View.VISIBLE)
        }
        if (windGustModel != null) {
            bigUpdateViews.setImageViewResource(
                R.id.windgust_icon,
                wim.getWeatherIconResource(windGustModel.icon)
            )
            bigUpdateViews.setTextViewText(R.id.windgust, windGustModel.value)
            bigUpdateViews.setViewVisibility(R.id.windgust_layout, View.VISIBLE)
        }
        if (chanceModel != null) {
            bigUpdateViews.setImageViewResource(
                R.id.weather_popicon,
                wim.getWeatherIconResource(chanceModel.icon)
            )
            bigUpdateViews.setTextViewText(R.id.weather_pop, chanceModel.value)
            bigUpdateViews.setViewVisibility(R.id.weather_pop_layout, View.VISIBLE)
        } else {
            bigUpdateViews.setViewVisibility(R.id.weather_pop_layout, View.GONE)
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
                    val wip = WeatherIconsManager.getProvider(WeatherIconsProvider.KEY)
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
            val temp_float = temp.toFloatOrNull()
            if (temp_float != null) {
                val temp_f = if (viewModel.curTemp!!.endsWith(Units.FAHRENHEIT)) {
                    temp_float
                } else {
                    ConversionMethods.CtoF(temp_float)
                }
                getColorFromTempF(temp_f)
            } else {
                Colors.SIMPLEBLUE
            }
        } else {
            0 // Don't colorize; use Material You colors
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
}