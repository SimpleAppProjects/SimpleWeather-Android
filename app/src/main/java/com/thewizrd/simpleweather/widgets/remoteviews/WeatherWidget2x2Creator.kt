package com.thewizrd.simpleweather.widgets.remoteviews

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.thewizrd.shared_resources.DateTimeConstants
import com.thewizrd.shared_resources.controls.WeatherDetailsType
import com.thewizrd.shared_resources.controls.WeatherNowViewModel
import com.thewizrd.shared_resources.helpers.ColorsUtils
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.icons.WeatherIconsManager
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.utils.ImageUtils
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.widgets.WeatherWidgetProvider2x2
import com.thewizrd.simpleweather.widgets.WidgetProviderInfo
import com.thewizrd.simpleweather.widgets.WidgetUtils
import java.util.*

class WeatherWidget2x2Creator(context: Context, loadBackground: Boolean = true) :
    CustomBackgroundWidgetRemoteViewCreator(context, loadBackground) {
    private fun generateRemoteViews() = RemoteViews(context.packageName, R.layout.app_widget_2x2)

    override val info: WidgetProviderInfo
        get() = WeatherWidgetProvider2x2.Info.getInstance()

    override suspend fun buildUpdate(
        appWidgetId: Int,
        weather: WeatherNowViewModel,
        location: LocationData,
        newOptions: Bundle
    ): RemoteViews {
        return buildLayout(appWidgetId, weather, location, newOptions).apply {
            buildDate(location, this, appWidgetId, newOptions)
            buildClock(location, this, appWidgetId, newOptions)
        }
    }

    private suspend fun buildLayout(
        appWidgetId: Int,
        weather: WeatherNowViewModel,
        location: LocationData,
        newOptions: Bundle
    ): RemoteViews {
        // Build an update that holds the updated widget contents
        val updateViews = generateRemoteViews()

        val txtSizeMultiplier = WidgetUtils.getCustomTextSizeMultiplier(appWidgetId)
        val icoSizeMultiplier = WidgetUtils.getCustomIconSizeMultiplier(appWidgetId)

        // Background
        val background = WidgetUtils.getWidgetBackground(appWidgetId)
        var style: WidgetUtils.WidgetBackgroundStyle? = null

        if (background == WidgetUtils.WidgetBackground.CURRENT_CONDITIONS) {
            style = WidgetUtils.getBackgroundStyle(appWidgetId)
        }

        setWidgetBackground(
            info,
            appWidgetId,
            updateViews,
            background,
            style,
            newOptions,
            weather
        )

        // Add notification layout
        updateViews.removeAllViews(R.id.weather_notif_layout)

        if (style != WidgetUtils.WidgetBackgroundStyle.PANDA) {
            updateViews.addView(
                R.id.weather_notif_layout,
                RemoteViews(context.packageName, R.layout.app_widget_2x2_notif_layout)
            )
        } else {
            updateViews.addView(
                R.id.weather_notif_layout,
                RemoteViews(context.packageName, R.layout.app_widget_2x2_notif_layout_themed)
            )
        }

        // WeatherIcon
        val wim = WeatherIconsManager.getInstance()
        val weatherIconResId = wim.getWeatherIconResource(weather.weatherIcon)

        val weatherIconSize =
            context.resources.getDimension(R.dimen.not_weather_icon_size) * icoSizeMultiplier

        updateViews.setInt(R.id.weather_icon, "setMaxWidth", weatherIconSize.toInt())
        updateViews.setInt(R.id.weather_icon, "setMaxHeight", weatherIconSize.toInt())

        if (style == WidgetUtils.WidgetBackgroundStyle.PANDA) {
            updateViews.setImageViewResource(R.id.weather_icon, weatherIconResId)
        } else {
            val panelBackgroundColor =
                if (background == WidgetUtils.WidgetBackground.CUSTOM) {
                    WidgetUtils.getBackgroundColor(appWidgetId)
                } else if (style == WidgetUtils.WidgetBackgroundStyle.LIGHT) {
                    Colors.WHITE
                } else {
                    Colors.BLACK
                }

            updateViews.setImageViewBitmap(
                R.id.weather_icon,
                ImageUtils.bitmapFromDrawable(
                    context.getThemeContextOverride(
                        ColorsUtils.isSuperLight(panelBackgroundColor)
                    ),
                    weatherIconResId,
                    weatherIconSize,
                    weatherIconSize
                )
            )
        }

        val textColor = WidgetUtils.getTextColor(appWidgetId, background)
        val panelTextColor = WidgetUtils.getPanelTextColor(appWidgetId, background, style)

        if (style != WidgetUtils.WidgetBackgroundStyle.PANDA) {
            updateViews.setTextColor(
                R.id.location_name,
                panelTextColor
            )
            updateViews.setTextColor(
                R.id.condition_weather,
                panelTextColor
            )
        }

        val tempArrowIconSize = context.dpToPx(16f) * txtSizeMultiplier

        // Extra layout
        if (style != WidgetUtils.WidgetBackgroundStyle.PANDA) {
            updateViews.setImageViewBitmap(
                R.id.hi_icon,
                ImageUtils.tintedBitmapFromDrawable(
                    context,
                    R.drawable.wi_direction_up,
                    Colors.WHITE,
                    tempArrowIconSize,
                    tempArrowIconSize
                )
            )
            updateViews.setImageViewBitmap(
                R.id.lo_icon,
                ImageUtils.tintedBitmapFromDrawable(
                    context,
                    R.drawable.wi_direction_down,
                    Colors.WHITE,
                    tempArrowIconSize,
                    tempArrowIconSize
                )
            )

            updateViews.setTextColor(R.id.condition_hi, panelTextColor)
            updateViews.setTextColor(R.id.divider, panelTextColor)
            updateViews.setTextColor(R.id.condition_lo, panelTextColor)
            updateViews.setTextColor(R.id.weather_pop, panelTextColor)
            updateViews.setTextColor(R.id.weather_windspeed, panelTextColor)

            val tint = if (wim.isFontIcon) panelTextColor else Colors.TRANSPARENT
            updateViews.setInt(R.id.hi_icon, "setColorFilter", tint)
            updateViews.setInt(R.id.lo_icon, "setColorFilter", tint)
            updateViews.setInt(R.id.weather_popicon, "setColorFilter", tint)
            updateViews.setInt(R.id.weather_windicon, "setColorFilter", tint)
        } else {
            updateViews.setInt(R.id.hi_icon, "setMaxWidth", tempArrowIconSize.toInt())
            updateViews.setInt(R.id.lo_icon, "setMaxWidth", tempArrowIconSize.toInt())

            updateViews.setImageViewResource(R.id.hi_icon, R.drawable.wi_direction_up)
            updateViews.setImageViewResource(R.id.lo_icon, R.drawable.wi_direction_down)
        }

        val chanceModel = weather.weatherDetailsMap[WeatherDetailsType.POPCHANCE]
            ?: weather.weatherDetailsMap[WeatherDetailsType.POPCLOUDINESS]
        val windModel = weather.weatherDetailsMap[WeatherDetailsType.WINDSPEED]
        val extraIconSize = context.dpToPx(24f) * txtSizeMultiplier

        if (chanceModel != null) {
            updateViews.setInt(R.id.weather_popicon, "setMaxWidth", extraIconSize.toInt())
            updateViews.setInt(R.id.weather_popicon, "setMaxHeight", extraIconSize.toInt())

            if (!WidgetUtils.isBackgroundOptionalWidget(info.widgetType) || background == WidgetUtils.WidgetBackground.TRANSPARENT) {
                updateViews.setImageViewBitmap(
                    R.id.weather_popicon,
                    ImageUtils.bitmapFromDrawable(
                        context.getThemeContextOverride(false),
                        wim.getWeatherIconResource(chanceModel.icon),
                        extraIconSize,
                        extraIconSize
                    )
                )
            } else if (background == WidgetUtils.WidgetBackground.CURRENT_CONDITIONS && style == WidgetUtils.WidgetBackgroundStyle.PANDA) {
                updateViews.setImageViewResource(
                    R.id.weather_popicon,
                    wim.getWeatherIconResource(chanceModel.icon)
                )
            } else {
                // Custom background color
                val backgroundColor = if (background == WidgetUtils.WidgetBackground.CUSTOM) {
                    WidgetUtils.getBackgroundColor(appWidgetId)
                } else {
                    if (style == WidgetUtils.WidgetBackgroundStyle.LIGHT) Colors.WHITE else Colors.BLACK
                }

                updateViews.setImageViewBitmap(
                    R.id.weather_popicon,
                    ImageUtils.bitmapFromDrawable(
                        context.getThemeContextOverride(ColorsUtils.isSuperLight(backgroundColor)),
                        wim.getWeatherIconResource(chanceModel.icon),
                        extraIconSize,
                        extraIconSize
                    )
                )
            }
        }

        if (windModel != null) {
            updateViews.setInt(R.id.weather_windicon, "setMaxWidth", extraIconSize.toInt())
            updateViews.setInt(R.id.weather_windicon, "setMaxHeight", extraIconSize.toInt())

            if (!WidgetUtils.isBackgroundOptionalWidget(info.widgetType) || background == WidgetUtils.WidgetBackground.TRANSPARENT) {
                updateViews.setImageViewBitmap(
                    R.id.weather_windicon,
                    ImageUtils.rotateBitmap(
                        ImageUtils.bitmapFromDrawable(
                            context.getThemeContextOverride(false),
                            wim.getWeatherIconResource(windModel.icon),
                            extraIconSize,
                            extraIconSize
                        ),
                        windModel.iconRotation.toFloat()
                    )
                )
            } else if (background == WidgetUtils.WidgetBackground.CURRENT_CONDITIONS && style == WidgetUtils.WidgetBackgroundStyle.PANDA) {
                updateViews.setImageViewBitmap(
                    R.id.weather_windicon,
                    ImageUtils.rotateBitmap(
                        ImageUtils.bitmapFromDrawable(
                            context,
                            wim.getWeatherIconResource(windModel.icon),
                            extraIconSize,
                            extraIconSize
                        ),
                        windModel.iconRotation
                            .toFloat()
                    )
                )
            } else {
                // Custom background color
                val backgroundColor = if (background == WidgetUtils.WidgetBackground.CUSTOM) {
                    WidgetUtils.getBackgroundColor(appWidgetId)
                } else {
                    Colors.BLACK
                }

                updateViews.setImageViewBitmap(
                    R.id.weather_windicon,
                    ImageUtils.rotateBitmap(
                        ImageUtils.bitmapFromDrawable(
                            context.getThemeContextOverride(
                                ColorsUtils.isSuperLight(
                                    backgroundColor
                                )
                            ),
                            wim.getWeatherIconResource(windModel.icon),
                            extraIconSize,
                            extraIconSize
                        ),
                        windModel.iconRotation.toFloat()
                    )
                )
            }
        }

        updateViews.setInt(R.id.settings_button, "setColorFilter", textColor)

        // Condition text
        updateViews.setTextViewText(
            R.id.condition_weather, String.format(
                Locale.ROOT, "%s - %s",
                if (weather.curTemp.isNullOrBlank()) WeatherIcons.PLACEHOLDER else weather.curTemp,
                weather.curCondition
            )
        )
        updateViews.setTextViewText(
            R.id.condition_hi,
            if (weather.hiTemp.isNullOrBlank()) WeatherIcons.PLACEHOLDER else weather.hiTemp
        )
        updateViews.setTextViewText(
            R.id.condition_lo,
            if (weather.loTemp.isNullOrBlank()) WeatherIcons.PLACEHOLDER else weather.loTemp
        )
        updateViews.setViewVisibility(
            R.id.condition_hilo_layout,
            if (weather.isShowHiLo) View.VISIBLE else View.GONE
        )

        if (chanceModel != null) {
            updateViews.setTextViewText(R.id.weather_pop, chanceModel.value)
            updateViews.setViewVisibility(R.id.weather_pop_layout, View.VISIBLE)
        } else {
            updateViews.setViewVisibility(R.id.weather_pop_layout, View.GONE)
        }

        if (windModel != null) {
            var speed = if (windModel.value.isNullOrEmpty()) "" else windModel.value.toString()
            speed = speed.split(",").toTypedArray()[0]
            updateViews.setTextViewText(R.id.weather_windspeed, speed)
            updateViews.setViewVisibility(R.id.weather_wind_layout, View.VISIBLE)
        } else {
            updateViews.setViewVisibility(R.id.weather_wind_layout, View.GONE)
        }

        updateViews.setViewVisibility(
            R.id.extra_layout,
            if (chanceModel != null || windModel != null) View.VISIBLE else View.GONE
        )

        // Open default clock/calendar app
        updateViews.setOnClickPendingIntent(R.id.date_panel, getCalendarAppIntent())

        // Open default clock/calendar app
        updateViews.setOnClickPendingIntent(R.id.clock_panel, getClockAppIntent())

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
            if (WidgetUtils.isLocationNameHidden(appWidgetId)) View.GONE else View.VISIBLE
        )
        updateViews.setViewVisibility(
            R.id.settings_button,
            if (WidgetUtils.isSettingsButtonHidden(appWidgetId)) View.GONE else View.VISIBLE
        )
        updateViews.setViewVisibility(
            R.id.refresh_button,
            if (WidgetUtils.isRefreshButtonHidden(appWidgetId)) View.GONE else View.VISIBLE
        )

        setOnClickIntent(location, updateViews)
        setOnSettingsClickIntent(updateViews, location, appWidgetId)
        setOnRefreshClickIntent(updateViews, appWidgetId)

        updateViewSizes(updateViews, appWidgetId, newOptions)

        return updateViews
    }

    private fun buildClock(
        location: LocationData?,
        updateViews: RemoteViews,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        if (!settingsManager.useFollowGPS() && WidgetUtils.isGPS(appWidgetId)) return

        updateClockSize(updateViews, appWidgetId, newOptions)

        if (location != null && WidgetUtils.useTimeZone(appWidgetId)) {
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
        if (!settingsManager.useFollowGPS() && WidgetUtils.isGPS(appWidgetId)) return

        updateDateSize(updateViews, appWidgetId, newOptions)

        if (location != null && WidgetUtils.useTimeZone(appWidgetId)) {
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
        val cellHeight = WidgetUtils.getCellsForSize(minHeight)
        val cellWidth = WidgetUtils.getCellsForSize(minWidth)

        val txtSizeMultiplier = WidgetUtils.getCustomTextSizeMultiplier(appWidgetId)

        val largeText = cellHeight > 2

        updateViews.setTextViewTextSize(
            R.id.location_name,
            TypedValue.COMPLEX_UNIT_SP,
            (if (largeText) 16f else 14f) * txtSizeMultiplier
        )
        updateViews.setTextViewTextSize(
            R.id.condition_weather,
            TypedValue.COMPLEX_UNIT_SP,
            (if (largeText) 15f else 13f) * txtSizeMultiplier
        )
        updateViews.setTextViewTextSize(
            R.id.condition_hi,
            TypedValue.COMPLEX_UNIT_SP,
            (if (largeText) 14f else 12f) * txtSizeMultiplier
        )
        updateViews.setTextViewTextSize(
            R.id.divider,
            TypedValue.COMPLEX_UNIT_SP,
            (if (largeText) 14f else 12f) * txtSizeMultiplier
        )
        updateViews.setTextViewTextSize(
            R.id.condition_lo,
            TypedValue.COMPLEX_UNIT_SP,
            (if (largeText) 14f else 12f) * txtSizeMultiplier
        )
        updateViews.setTextViewTextSize(
            R.id.weather_pop,
            TypedValue.COMPLEX_UNIT_SP,
            (if (largeText) 14f else 12f) * txtSizeMultiplier
        )
        updateViews.setTextViewTextSize(
            R.id.weather_windspeed,
            TypedValue.COMPLEX_UNIT_SP,
            (if (largeText) 14f else 12f) * txtSizeMultiplier
        )
        updateViews.setViewVisibility(
            R.id.extra_layout,
            if (cellWidth <= 3) View.GONE else View.VISIBLE
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

        val txtSizeMultiplier = WidgetUtils.getCustomTextSizeMultiplier(appWidgetId)

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

        var clockTextSize =
            context.resources.getDimensionPixelSize(R.dimen.clock_text_size).toFloat() // 36sp
        if (isSmallHeight && cellHeight <= 2 || cellWidth < 4) {
            clockTextSize *= 8f / 9 // 32sp
        }
        views.setTextViewTextSize(
            R.id.clock_panel,
            TypedValue.COMPLEX_UNIT_PX,
            clockTextSize * txtSizeMultiplier
        )
    }

    private fun updateDateSize(views: RemoteViews, appWidgetId: Int, newOptions: Bundle) {
        val minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        val minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        val maxCellHeight = WidgetUtils.getCellsForSize(maxHeight)
        val cellHeight = WidgetUtils.getCellsForSize(minHeight)
        val cellWidth = WidgetUtils.getCellsForSize(minWidth)
        val isSmallHeight = maxCellHeight.toFloat() / cellHeight <= 1.5f

        val txtSizeMultiplier = WidgetUtils.getCustomTextSizeMultiplier(appWidgetId)

        var dateTextSize =
            context.resources.getDimensionPixelSize(R.dimen.date_text_size).toFloat() // 16sp
        if (isSmallHeight && cellHeight <= 2 || cellWidth < 4) dateTextSize *= 0.875f // 14sp

        views.setTextViewTextSize(
            R.id.date_panel,
            TypedValue.COMPLEX_UNIT_PX,
            dateTextSize * txtSizeMultiplier
        )

        val datePattern = if (cellWidth >= 3) {
            DateTimeUtils.getBestPatternForSkeleton(DateTimeConstants.SKELETON_LONG_DATE_FORMAT)
        } else {
            DateTimeUtils.getBestPatternForSkeleton(DateTimeConstants.SKELETON_SHORT_DATE_FORMAT)
        }
        views.setCharSequence(R.id.date_panel, "setFormat12Hour", datePattern)
        views.setCharSequence(R.id.date_panel, "setFormat24Hour", datePattern)
    }
}