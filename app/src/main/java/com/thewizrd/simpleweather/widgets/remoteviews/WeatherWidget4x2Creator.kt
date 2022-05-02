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
import com.thewizrd.common.controls.WeatherNowViewModel
import com.thewizrd.common.helpers.ColorsUtils
import com.thewizrd.common.utils.ImageUtils
import com.thewizrd.shared_resources.DateTimeConstants
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.widgets.WeatherWidgetProvider4x2
import com.thewizrd.simpleweather.widgets.WidgetProviderInfo
import com.thewizrd.simpleweather.widgets.WidgetUpdaterHelper.buildForecast
import com.thewizrd.simpleweather.widgets.WidgetUpdaterHelper.updateForecastSizes
import com.thewizrd.simpleweather.widgets.WidgetUtils
import com.thewizrd.simpleweather.widgets.preferences.*

class WeatherWidget4x2Creator(context: Context, loadBackground: Boolean = true) :
    CustomBackgroundWidgetRemoteViewCreator(context, loadBackground) {
    private fun generateRemoteViews(): RemoteViews {
        return RemoteViews(context.packageName, R.layout.app_widget_4x2)
    }

    override val info: WidgetProviderInfo
        get() = WeatherWidgetProvider4x2.Info.getInstance()

    override suspend fun buildUpdate(
        appWidgetId: Int,
        weather: WeatherNowViewModel,
        location: LocationData,
        newOptions: Bundle
    ): RemoteViews {
        return buildLayout(appWidgetId, weather, location, newOptions).apply {
            buildForecast(
                context,
                info,
                this,
                appWidgetId,
                location,
                weather.weatherData,
                newOptions
            )
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
        val textColor = if (background == WidgetUtils.WidgetBackground.CUSTOM) {
            newOptions.get(KEY_TXTCOLORCODE) as? Int ?: WidgetUtils.getTextColor(appWidgetId)
        } else {
            Colors.WHITE
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
            val backgroundColor =
                if (background == WidgetUtils.WidgetBackground.CUSTOM) {
                    newOptions.get(KEY_BGCOLORCODE) as? Int ?: WidgetUtils.getBackgroundColor(
                        appWidgetId
                    )
                } else {
                    Colors.BLACK
                }

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
        updateForecastSizes(context, info, appWidgetId, updateViews, newOptions)

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