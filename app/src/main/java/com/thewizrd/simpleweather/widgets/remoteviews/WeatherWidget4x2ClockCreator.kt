package com.thewizrd.simpleweather.widgets.remoteviews

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import android.text.SpannableString
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.thewizrd.shared_resources.DateTimeConstants
import com.thewizrd.shared_resources.controls.WeatherNowViewModel
import com.thewizrd.shared_resources.helpers.ColorsUtils
import com.thewizrd.shared_resources.icons.WeatherIconsManager
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.utils.ImageUtils
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.widgets.WeatherWidgetProvider4x2Clock
import com.thewizrd.simpleweather.widgets.WidgetProviderInfo
import com.thewizrd.simpleweather.widgets.WidgetUtils

class WeatherWidget4x2ClockCreator(context: Context) : WidgetRemoteViewCreator(context) {
    private fun generateRemoteViews() =
        RemoteViews(context.packageName, R.layout.app_widget_4x2_clock)

    override val info: WidgetProviderInfo
        get() = WeatherWidgetProvider4x2Clock.Info.getInstance()

    override suspend fun buildUpdate(
        appWidgetId: Int,
        weather: WeatherNowViewModel,
        location: LocationData,
        newOptions: Bundle
    ): RemoteViews {
        val views = buildLayout(appWidgetId, weather, location, newOptions)
        buildDate(location, views, appWidgetId, newOptions)
        buildClock(location, views, appWidgetId, newOptions)

        return views
    }

    private suspend fun buildLayout(
        appWidgetId: Int,
        weather: WeatherNowViewModel,
        location: LocationData,
        newOptions: Bundle
    ): RemoteViews {
        // Build an update that holds the updated widget contents
        val updateViews = generateRemoteViews()

        val backgroundColor = WidgetUtils.getBackgroundColor(appWidgetId)
        val textColor = WidgetUtils.getTextColor(appWidgetId)
        val viewCtx = context.getThemeContextOverride(
            ColorsUtils.isSuperLight(backgroundColor)
        )

        val txtSizeMultiplier = WidgetUtils.getCustomTextSizeMultiplier(appWidgetId)
        val icoSizeMultiplier = WidgetUtils.getCustomIconSizeMultiplier(appWidgetId)

        // WeatherIcon
        val wim = WeatherIconsManager.getInstance()
        val weatherIconResId = wim.getWeatherIconResource(weather.weatherIcon)

        val weatherIconSize = context.dpToPx(60f) * icoSizeMultiplier

        updateViews.setInt(R.id.weather_icon, "setMaxWidth", weatherIconSize.toInt())
        updateViews.setInt(R.id.weather_icon, "setMaxHeight", weatherIconSize.toInt())

        updateViews.setImageViewBitmap(
            R.id.weather_icon,
            ImageUtils.bitmapFromDrawable(
                viewCtx,
                weatherIconResId,
                weatherIconSize,
                weatherIconSize
            )
        )

        // Condition text
        updateViews.setTextViewText(R.id.condition_weather, weather.curCondition)

        updateViews.setTextViewText(R.id.condition_temp, weather.curTemp)

        // Open default clock/calendar app
        updateViews.setOnClickPendingIntent(R.id.date_panel, getCalendarAppIntent())

        // Open default clock/calendar app
        updateViews.setOnClickPendingIntent(R.id.clock_panel, getClockAppIntent())

        // Color/tint
        updateViews.setTextColor(R.id.clock_panel, textColor)
        updateViews.setTextColor(R.id.date_panel, textColor)
        updateViews.setTextColor(R.id.location_name, textColor)
        updateViews.setTextColor(R.id.condition_temp, textColor)
        updateViews.setTextColor(R.id.condition_weather, textColor)

        updateViews.setInt(R.id.refresh_button, "setColorFilter", textColor)
        updateViews.setInt(R.id.settings_button, "setColorFilter", textColor)

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
        val minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val cellWidth = WidgetUtils.getCellsForSize(minWidth)

        val txtSizeMultiplier = WidgetUtils.getCustomTextSizeMultiplier(appWidgetId)

        updateViews.setViewVisibility(
            R.id.spacer_left,
            if (cellWidth <= 3) View.GONE else View.VISIBLE
        )
        updateViews.setViewVisibility(
            R.id.spacer_right,
            if (cellWidth <= 3) View.GONE else View.VISIBLE
        )
        updateViews.setTextViewTextSize(
            R.id.condition_temp,
            TypedValue.COMPLEX_UNIT_SP,
            (if (cellWidth <= 3) 28f else 36f) * txtSizeMultiplier
        )

        updateViews.setTextViewTextSize(
            R.id.location_name,
            TypedValue.COMPLEX_UNIT_SP,
            12f * txtSizeMultiplier
        )
        updateViews.setTextViewTextSize(
            R.id.condition_weather,
            TypedValue.COMPLEX_UNIT_SP,
            14f * txtSizeMultiplier
        )
        updateViews.setTextViewTextSize(
            R.id.condition_weather,
            TypedValue.COMPLEX_UNIT_SP,
            14f * txtSizeMultiplier
        )
    }

    private fun updateClockSize(views: RemoteViews, appWidgetId: Int, newOptions: Bundle) {
        val minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        val maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        val maxCellHeight = WidgetUtils.getCellsForSize(maxHeight)
        val cellHeight = WidgetUtils.getCellsForSize(minHeight)
        val isSmallHeight = maxCellHeight.toFloat() / cellHeight <= 1.5f

        val txtSizeMultiplier = WidgetUtils.getCustomTextSizeMultiplier(appWidgetId)

        // Update clock widgets
        val timeStr12hr = SpannableString(context.getText(R.string.clock_12_hours_format))

        views.setCharSequence(
            R.id.clock_panel, "setFormat12Hour",
            timeStr12hr
        )
        views.setCharSequence(
            R.id.clock_panel, "setFormat24Hour",
            context.getText(R.string.clock_24_hours_format)
        )

        views.setTextViewTextSize(
            R.id.clock_panel,
            TypedValue.COMPLEX_UNIT_SP,
            (if (isSmallHeight && cellHeight <= 2) 60f else 66f) * txtSizeMultiplier
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

        val datePattern = if (cellWidth >= 4) {
            DateTimeUtils.getBestPatternForSkeleton(DateTimeConstants.SKELETON_LONG_DATE_FORMAT)
        } else {
            DateTimeUtils.getBestPatternForSkeleton(DateTimeConstants.SKELETON_SHORT_DATE_FORMAT)
        }
        views.setCharSequence(R.id.date_panel, "setFormat12Hour", datePattern)
        views.setCharSequence(R.id.date_panel, "setFormat24Hour", datePattern)
    }
}