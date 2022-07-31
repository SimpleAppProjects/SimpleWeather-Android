package com.thewizrd.simpleweather.widgets.remoteviews

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.format.DateFormat
import android.text.style.RelativeSizeSpan
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.thewizrd.common.controls.WeatherDetailsType
import com.thewizrd.common.controls.WeatherUiModel
import com.thewizrd.common.helpers.ColorsUtils
import com.thewizrd.common.utils.ImageUtils
import com.thewizrd.shared_resources.DateTimeConstants
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.shared_resources.weatherdata.model.MinutelyForecast
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.widgets.WeatherWidgetProvider4x2Tomorrow
import com.thewizrd.simpleweather.widgets.WidgetProviderInfo
import com.thewizrd.simpleweather.widgets.WidgetUtils
import com.thewizrd.simpleweather.widgets.preferences.*
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class WeatherWidget4x2TomorrowCreator(context: Context, loadBackground: Boolean = true) :
    CustomBackgroundWidgetRemoteViewCreator(context, loadBackground) {
    private fun generateRemoteViews(): RemoteViews {
        return RemoteViews(context.packageName, R.layout.app_widget_4x2_tomorrow)
    }

    override val info: WidgetProviderInfo
        get() = WeatherWidgetProvider4x2Tomorrow.Info.getInstance()

    override suspend fun buildUpdate(
        appWidgetId: Int,
        weather: WeatherUiModel,
        location: LocationData,
        newOptions: Bundle
    ): RemoteViews {
        return buildLayout(appWidgetId, weather, location, newOptions)
    }

    private suspend fun buildLayout(
        appWidgetId: Int,
        weather: WeatherUiModel,
        location: LocationData,
        newOptions: Bundle
    ): RemoteViews {
        // Build an update that holds the updated widget contents
        val updateViews = generateRemoteViews()

        val txtSizeMultiplier =
            newOptions.get(KEY_TEXTSIZE) as? Float ?: WidgetUtils.getCustomTextSizeMultiplier(
                appWidgetId
            )
        val icoSizeMultiplier =
            newOptions.get(KEY_ICONSIZE) as? Float ?: WidgetUtils.getCustomIconSizeMultiplier(
                appWidgetId
            )

        // Background
        val background = newOptions.getSerializable(KEY_BGCOLOR) as? WidgetUtils.WidgetBackground
            ?: WidgetUtils.getWidgetBackground(appWidgetId)
        var style: WidgetUtils.WidgetBackgroundStyle? = null

        if (background == WidgetUtils.WidgetBackground.CURRENT_CONDITIONS) {
            style = (newOptions.getSerializable(KEY_BGSTYLE) as? WidgetUtils.WidgetBackgroundStyle)
                ?: WidgetUtils.getBackgroundStyle(appWidgetId)
        }

        val textColor = if (background == WidgetUtils.WidgetBackground.CUSTOM) {
            newOptions.get(KEY_TXTCOLORCODE) as? Int ?: WidgetUtils.getTextColor(appWidgetId)
        } else {
            Colors.WHITE
        }

        val panelTextColor = when {
            background == WidgetUtils.WidgetBackground.CUSTOM -> {
                textColor
            }
            style == WidgetUtils.WidgetBackgroundStyle.LIGHT -> {
                Colors.BLACK
            }
            else -> {
                Colors.WHITE
            }
        }

        val backgroundColor =
            if (background == WidgetUtils.WidgetBackground.CUSTOM) {
                newOptions.get(KEY_BGCOLORCODE) as? Int ?: WidgetUtils.getBackgroundColor(
                    appWidgetId
                )
            } else {
                Colors.BLACK
            }


        // WeatherIcon
        val wim = sharedDeps.weatherIconsManager
        val weatherIconResId = wim.getWeatherIconResource(weather.weatherIcon)

        val weatherIconSize = context.dpToPx(60f) * icoSizeMultiplier

        updateViews.setInt(R.id.weather_icon, "setMaxWidth", weatherIconSize.toInt())
        updateViews.setInt(R.id.weather_icon, "setMaxHeight", weatherIconSize.toInt())

        if (wim.isFontIcon) {
            updateViews.setImageViewBitmap(
                R.id.weather_icon,
                ImageUtils.tintedBitmapFromDrawable(
                    context, weatherIconResId,
                    textColor,
                    weatherIconSize,
                    weatherIconSize
                )
            )
        } else {
            updateViews.setImageViewBitmap(
                R.id.weather_icon,
                ImageUtils.bitmapFromDrawable(
                    context.getThemeContextOverride(
                        ColorsUtils.isSuperLight(backgroundColor)
                    ),
                    weatherIconResId,
                    weatherIconSize,
                    weatherIconSize
                )
            )
        }

        updateViews.setTextColor(R.id.condition_temp, textColor)

        updateViews.setTextColor(
            R.id.location_name,
            textColor
        )
        updateViews.setTextColor(
            R.id.condition_weather,
            textColor
        )

        updateViews.setTextColor(R.id.date_panel, textColor)
        updateViews.setTextColor(R.id.clock_panel, textColor)
        updateViews.setInt(R.id.refresh_button, "setColorFilter", textColor)
        updateViews.setInt(R.id.settings_button, "setColorFilter", textColor)

        // Condition text
        updateViews.setTextViewText(R.id.condition_weather, weather.curCondition)

        updateViews.setTextViewText(R.id.condition_temp, weather.curTemp)

        buildDate(location, updateViews, appWidgetId, newOptions)
        // Open default clock/calendar app
        updateViews.setOnClickPendingIntent(
            R.id.date_panel,
            getCalendarAppIntent()
        )

        buildClock(location, updateViews, appWidgetId, newOptions)
        // Open default clock/calendar app
        updateViews.setOnClickPendingIntent(
            R.id.clock_panel,
            getClockAppIntent()
        )

        // Build extra panel
        updateViews.removeAllViews(R.id.extra_container)
        if (style != WidgetUtils.WidgetBackgroundStyle.PANDA) {
            updateViews.addView(
                R.id.extra_container,
                RemoteViews(context.packageName, R.layout.app_widget_4x2_tomorrow_extra_layout)
            )
        } else {
            updateViews.addView(
                R.id.extra_container,
                RemoteViews(
                    context.packageName,
                    R.layout.app_widget_4x2_tomorrow_extra_layout_themed
                )
            )
        }

        buildPrecipitationForecast(
            updateViews,
            location,
            weather,
            background,
            style,
            txtSizeMultiplier,
            backgroundColor,
            panelTextColor
        )

        weather.airQuality?.let {
            val dotSize = (context.dpToPx(24f) * txtSizeMultiplier).toInt()

            updateViews.setInt(R.id.aqi_dot_icon, "setMaxWidth", dotSize)
            updateViews.setInt(R.id.aqi_dot_icon, "setMaxHeight", dotSize)

            updateViews.setImageViewBitmap(
                R.id.aqi_dot_icon,
                ImageUtils.tintedBitmapFromDrawable(
                    context,
                    R.drawable.dot,
                    Colors.WHITE,
                    dotSize.toFloat(),
                    dotSize.toFloat()
                )
            )
            updateViews.setInt(R.id.aqi_dot_icon, "setColorFilter", it.progressColor)

            updateViews.setTextViewText(R.id.aqi_level, "${it.index} - ${it.level}")
            updateViews.setTextViewTextSize(
                R.id.aqi_level,
                TypedValue.COMPLEX_UNIT_SP,
                14f * txtSizeMultiplier
            )

            if (style != WidgetUtils.WidgetBackgroundStyle.PANDA) {
                updateViews.setTextColor(R.id.aqi_label, panelTextColor)
                updateViews.setTextColor(R.id.aqi_level, panelTextColor)
            }

            updateViews.setViewVisibility(R.id.aqi_panel, View.VISIBLE)
        } ?: run {
            updateViews.setViewVisibility(R.id.aqi_panel, View.GONE)
        }

        val now = LocalDateTime.now()
        val dateStr = if (DateFormat.is24HourFormat(context)) {
            now.format(
                DateTimeUtils.ofPatternForUserLocale(
                    DateTimeUtils.getBestPatternForSkeleton(
                        DateTimeConstants.SKELETON_DAYOFWEEK_AND_24HR
                    )
                )
            )
        } else {
            now.format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.ABBREV_DAYOFWEEK_AND_12HR_MIN_AMPM))
        }
        updateViews.setTextViewText(
            R.id.label_updatetime, "${context.getString(R.string.update_prefix)} $dateStr"
        )
        updateViews.setTextViewTextSize(
            R.id.label_updatetime,
            TypedValue.COMPLEX_UNIT_SP,
            11f * txtSizeMultiplier
        )

        if (style != WidgetUtils.WidgetBackgroundStyle.PANDA) {
            updateViews.setTextColor(R.id.label_updatetime, panelTextColor)
        }

        // original icon size: 24dp
        val scaledIconSize = (context.dpToPx(16f) * txtSizeMultiplier).toInt()

        // Refresh icon
        updateViews.setImageViewBitmap(R.id.refresh_button, null)

        updateViews.setInt(R.id.refresh_button, "setMaxWidth", scaledIconSize)
        updateViews.setInt(R.id.refresh_button, "setMaxHeight", scaledIconSize)

        updateViews.setImageViewResource(R.id.refresh_button, R.drawable.ic_refresh)

        // Setting icon
        updateViews.setImageViewBitmap(R.id.settings_button, null)

        updateViews.setInt(R.id.settings_button, "setMaxWidth", scaledIconSize)
        updateViews.setInt(R.id.settings_button, "setMaxHeight", scaledIconSize)

        updateViews.setImageViewResource(R.id.settings_button, R.drawable.ic_outline_settings_24)

        // Location Name
        updateViews.setTextViewText(R.id.location_name, weather.location)

        updateViews.setViewVisibility(
            R.id.location_name,
            if (newOptions.get(KEY_HIDELOCNAME) as? Boolean ?: WidgetUtils.isLocationNameHidden(
                    appWidgetId
                )
            ) View.GONE else View.VISIBLE
        )
        updateViews.setViewVisibility(
            R.id.settings_button,
            if (newOptions.get(KEY_HIDESETTINGSBTN) as? Boolean
                    ?: WidgetUtils.isSettingsButtonHidden(appWidgetId)
            ) View.GONE else View.VISIBLE
        )
        updateViews.setViewVisibility(
            R.id.refresh_button,
            if (newOptions.get(KEY_HIDEREFRESHBTN) as? Boolean ?: WidgetUtils.isRefreshButtonHidden(
                    appWidgetId
                )
            ) View.GONE else View.VISIBLE
        )

        setOnClickIntent(location, updateViews)
        setOnSettingsClickIntent(updateViews, location, appWidgetId)
        setOnRefreshClickIntent(updateViews, appWidgetId)

        updateViewSizes(updateViews, appWidgetId, newOptions)

        return updateViews
    }

    private suspend fun buildPrecipitationForecast(
        updateViews: RemoteViews,
        location: LocationData,
        weather: WeatherUiModel,
        background: WidgetUtils.WidgetBackground,
        style: WidgetUtils.WidgetBackgroundStyle?,
        txtSizeMultiplier: Float,
        backgroundColor: Int,
        textColor: Int
    ) {
        val now = ZonedDateTime.now(location.tzOffset ?: ZoneOffset.UTC)
        val minForecasts =
            settingsManager.getWeatherForecastData(location.query)?.minForecast?.filter {
                !it.date.isBefore(now.withZoneSameInstant(location.tzOffset))
            }

        // Create minutely precipitation text if possible
        if (!buildMinutelyForecast(updateViews, minForecasts, now)) {
            // If not fallback to PoP% text
            val nowHour = now.withZoneSameInstant(location.tzOffset).truncatedTo(ChronoUnit.HOURS)
            val hrForecasts =
                settingsManager.getHourlyForecastsByQueryOrderByDateByLimitFilterByDate(
                    location.query,
                    12,
                    nowHour
                )

            if (!buildPoPForecast(updateViews, hrForecasts, now)) {
                updateViews.setTextViewText(
                    R.id.precipitation_text,
                    weather.weatherDetailsMap[WeatherDetailsType.POPCHANCE]?.value ?: "0%"
                )
            }
        }

        updateViews.setTextViewTextSize(
            R.id.precipitation_text,
            TypedValue.COMPLEX_UNIT_SP,
            14f * txtSizeMultiplier
        )

        if (style != WidgetUtils.WidgetBackgroundStyle.PANDA) {
            updateViews.setTextColor(R.id.precipitation_text, textColor)
        }

        // Set precipitation icon
        val wim = sharedDeps.weatherIconsManager

        val iconSize = (context.dpToPx(24f) * txtSizeMultiplier).toInt()

        updateViews.setInt(R.id.precipitation_icon, "setMaxWidth", iconSize)
        updateViews.setInt(R.id.precipitation_icon, "setMaxHeight", iconSize)

        val iconResId = wim.getWeatherIconResource(WeatherIcons.UMBRELLA)

        if (style == WidgetUtils.WidgetBackgroundStyle.PANDA) {
            updateViews.setImageViewResource(R.id.precipitation_icon, iconResId)
            updateViews.setInt(R.id.precipitation_icon, "setColorFilter", 0)
        } else {
            val panelBackgroundColor = when {
                background == WidgetUtils.WidgetBackground.CUSTOM -> {
                    backgroundColor
                }
                style == WidgetUtils.WidgetBackgroundStyle.LIGHT -> {
                    Colors.WHITE
                }
                else -> {
                    Colors.BLACK
                }
            }

            updateViews.setImageViewBitmap(
                R.id.precipitation_icon,
                ImageUtils.bitmapFromDrawable(
                    context.getThemeContextOverride(
                        ColorsUtils.isSuperLight(panelBackgroundColor)
                    ),
                    iconResId,
                    iconSize.toFloat(),
                    iconSize.toFloat()
                )
            )
            if (wim.isFontIcon) {
                updateViews.setInt(R.id.precipitation_icon, "setColorFilter", textColor)
            } else {
                updateViews.setInt(R.id.precipitation_icon, "setColorFilter", 0)
            }
        }
    }

    private fun buildMinutelyForecast(
        updateViews: RemoteViews,
        minForecasts: List<MinutelyForecast>?,
        now: ZonedDateTime
    ): Boolean {
        if (minForecasts.isNullOrEmpty()) return false

        val isRainingMinute = minForecasts.firstOrNull {
            Duration.between(
                now.truncatedTo(ChronoUnit.MINUTES),
                it.date.truncatedTo(ChronoUnit.MINUTES)
            ).abs().toMinutes() <= 5 && it.rainMm > 0
        }

        val minute = if (isRainingMinute != null) {
            // Find minute where rain stops
            minForecasts.firstOrNull {
                it.date.truncatedTo(ChronoUnit.MINUTES)
                    .isAfter(isRainingMinute.date) && it.rainMm <= 0
            }
        } else {
            // Find minute where rain starts
            minForecasts.firstOrNull {
                it.date.truncatedTo(ChronoUnit.MINUTES)
                    .isAfter(now.truncatedTo(ChronoUnit.MINUTES)) && it.rainMm > 0
            }
        } ?: return false

        val formatStrResId = if (isRainingMinute != null) {
            R.string.precipitation_minutely_stopping_text_format
        } else {
            R.string.precipitation_minutely_starting_text_format
        }
        val duration = Duration.between(now, minute.date).toMinutes()
        val duraStr = when {
            duration < 120 -> {
                context.getString(
                    formatStrResId,
                    context.getString(R.string.refresh_30min).replace("30", duration.toString())
                )
            }
            else -> {
                context.getString(
                    formatStrResId,
                    context.getString(R.string.refresh_12hrs)
                        .replace("12", (duration / 60).toString())
                )
            }
        }

        updateViews.setTextViewText(R.id.precipitation_text, duraStr)
        return true
    }

    private fun buildPoPForecast(
        updateViews: RemoteViews,
        hrForecasts: List<HourlyForecast>?,
        now: ZonedDateTime
    ): Boolean {
        if (hrForecasts.isNullOrEmpty()) return false

        // Find the next hour with a 60% or higher chance of precipitation
        val forecast = hrForecasts.find { it.extras?.pop != null && it.extras.pop >= 60 }

        // Proceed if within the next 3hrs
        if (forecast == null || Duration.between(now.truncatedTo(ChronoUnit.HOURS), forecast.date)
                .toHours() > 3
        ) return false

        // Should be within 0-3 hours
        val duration = Duration.between(now, forecast.date).toMinutes()
        val duraStr = if (duration <= 60) {
            context.getString(R.string.precipitation_nexthour_text_format, forecast.extras.pop)
        } else if (duration < 120) {
            context.getString(
                R.string.precipitation_text_format, forecast.extras.pop,
                context.getString(R.string.refresh_30min).replace("30", duration.toString())
            )
        } else {
            context.getString(
                R.string.precipitation_text_format, forecast.extras.pop,
                context.getString(R.string.refresh_12hrs).replace("12", (duration / 60).toString())
            )
        }

        updateViews.setTextViewText(R.id.precipitation_text, duraStr)
        return true
    }

    private fun buildClock(
        location: LocationData?,
        updateViews: RemoteViews,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        updateClockSize(updateViews, appWidgetId, newOptions)

        if (location != null && (newOptions.get(KEY_USETIMEZONE) as? Boolean
                ?: WidgetUtils.useTimeZone(appWidgetId))
        ) {
            updateViews.setString(R.id.clock_panel, "setTimeZone", location.tzLong)
        } else {
            updateViews.setString(R.id.clock_panel, "setTimeZone", null)
        }
    }

    private fun buildDate(
        location: LocationData?,
        updateViews: RemoteViews,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        updateDateSize(updateViews, appWidgetId, newOptions)

        if (location != null && (newOptions.get(KEY_USETIMEZONE) as? Boolean
                ?: WidgetUtils.useTimeZone(appWidgetId))
        ) {
            updateViews.setString(R.id.date_panel, "setTimeZone", location.tzLong)
        } else {
            updateViews.setString(R.id.date_panel, "setTimeZone", null)
        }
    }

    override fun resizeWidget(
        info: WidgetProviderInfo,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        val updateViews = generateRemoteViews()

        updateViewSizes(updateViews, appWidgetId, newOptions)

        updateDateSize(updateViews, appWidgetId, newOptions)
        updateClockSize(updateViews, appWidgetId, newOptions)

        resizeWidgetBackground(info, appWidgetId, updateViews, newOptions)

        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, updateViews)
    }

    private fun updateViewSizes(
        updateViews: RemoteViews,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        // Widget dimensions
        val minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        val minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        val maxCellHeight = WidgetUtils.getCellsForSize(maxHeight)
        val cellHeight = WidgetUtils.getCellsForSize(minHeight)
        val cellWidth = WidgetUtils.getCellsForSize(minWidth)
        val forceSmallHeight = cellHeight == maxCellHeight
        val isSmallHeight = maxCellHeight.toFloat() / cellHeight <= 1.5f

        val txtSizeMultiplier =
            newOptions.get(KEY_TEXTSIZE) as? Float ?: WidgetUtils.getCustomTextSizeMultiplier(
                appWidgetId
            )
        val icoSizeMultiplier =
            newOptions.get(KEY_ICONSIZE) as? Float ?: WidgetUtils.getCustomIconSizeMultiplier(
                appWidgetId
            )

        val maxHeightSize = context.dpToPx(60f).toInt()

        if (isSmallHeight && cellHeight <= 2 || cellWidth < 4) {
            val iconWidth = context.dpToPx(45f).toInt()
            updateViews.setInt(
                R.id.weather_icon, "setMaxWidth",
                (iconWidth * icoSizeMultiplier).toInt()
            )
            updateViews.setInt(
                R.id.weather_icon, "setMaxHeight",
                (maxHeightSize * icoSizeMultiplier).toInt()
            )
        } else {
            val iconWidth = context.dpToPx(55f).toInt()
            updateViews.setInt(
                R.id.weather_icon, "setMaxWidth",
                (iconWidth * icoSizeMultiplier).toInt()
            )
            updateViews.setInt(
                R.id.weather_icon,
                "setMaxHeight",
                (maxHeightSize * 7f / 6 * icoSizeMultiplier).toInt()
            ) // 70dp
        }

        updateViews.setTextViewTextSize(
            R.id.condition_temp,
            TypedValue.COMPLEX_UNIT_SP,
            28f * txtSizeMultiplier
        )
        updateViews.setViewVisibility(
            R.id.condition_weather,
            if (forceSmallHeight && cellHeight <= 2) View.GONE else View.VISIBLE
        )

        updateViews.setTextViewTextSize(
            R.id.location_name,
            TypedValue.COMPLEX_UNIT_SP,
            14f * txtSizeMultiplier
        )
        updateViews.setTextViewTextSize(
            R.id.condition_weather,
            TypedValue.COMPLEX_UNIT_SP,
            12f * txtSizeMultiplier
        )
    }

    private fun updateClockSize(views: RemoteViews, appWidgetId: Int, newOptions: Bundle) {
        val minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        val minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        val maxCellHeight = WidgetUtils.getCellsForSize(maxHeight)
        val cellHeight = WidgetUtils.getCellsForSize(minHeight)
        val cellWidth = WidgetUtils.getCellsForSize(minWidth)
        val isSmallHeight = maxCellHeight.toFloat() / cellHeight <= 1.5f

        val txtSizeMultiplier =
            newOptions.get(KEY_TEXTSIZE) as? Float ?: WidgetUtils.getCustomTextSizeMultiplier(
                appWidgetId
            )

        // Update clock widgets
        val timeStr12hr = SpannableString(context.getText(R.string.clock_12_hours_ampm_format))
        val start12hr = timeStr12hr.length - 2
        timeStr12hr.setSpan(
            RelativeSizeSpan(0.875f),
            start12hr,
            timeStr12hr.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        views.setCharSequence(
            R.id.clock_panel, "setFormat12Hour",
            timeStr12hr
        )
        views.setCharSequence(
            R.id.clock_panel, "setFormat24Hour",
            context.getText(R.string.clock_24_hours_format)
        )

        var clockTextSize = 32f
        if (cellWidth < 4) {
            clockTextSize = 28f
        }
        views.setTextViewTextSize(
            R.id.clock_panel,
            TypedValue.COMPLEX_UNIT_SP,
            clockTextSize * txtSizeMultiplier
        )
    }

    private fun updateDateSize(views: RemoteViews, appWidgetId: Int, newOptions: Bundle) {
        val minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val cellWidth = WidgetUtils.getCellsForSize(minWidth)

        val txtSizeMultiplier =
            newOptions.get(KEY_TEXTSIZE) as? Float ?: WidgetUtils.getCustomTextSizeMultiplier(
                appWidgetId
            )

        views.setTextViewTextSize(
            R.id.date_panel,
            TypedValue.COMPLEX_UNIT_SP,
            12f * txtSizeMultiplier
        )

        val datePattern = DateTimeUtils.getBestPatternForSkeleton(
            if (cellWidth > 4) {
                DateTimeConstants.SKELETON_ABBR_WDAY_MONTH_FORMAT
            } else {
                DateTimeConstants.SKELETON_SHORT_DATE_FORMAT
            }
        )
        views.setCharSequence(R.id.date_panel, "setFormat12Hour", datePattern)
        views.setCharSequence(R.id.date_panel, "setFormat24Hour", datePattern)
    }
}