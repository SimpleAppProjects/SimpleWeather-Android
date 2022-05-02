package com.thewizrd.simpleweather.widgets.remoteviews

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.thewizrd.common.controls.WeatherNowViewModel
import com.thewizrd.common.helpers.ColorsUtils
import com.thewizrd.common.utils.ImageUtils
import com.thewizrd.shared_resources.DateTimeConstants
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.widgets.WeatherWidgetProvider4x1Google
import com.thewizrd.simpleweather.widgets.WidgetProviderInfo
import com.thewizrd.simpleweather.widgets.WidgetUtils
import com.thewizrd.simpleweather.widgets.preferences.KEY_BGCOLORCODE
import com.thewizrd.simpleweather.widgets.preferences.KEY_ICONSIZE
import com.thewizrd.simpleweather.widgets.preferences.KEY_TEXTSIZE
import com.thewizrd.simpleweather.widgets.preferences.KEY_TXTCOLORCODE

class WeatherWidget4x1GoogleCreator(context: Context) : WidgetRemoteViewCreator(context) {
    override val info: WidgetProviderInfo
        get() = WeatherWidgetProvider4x1Google.Info.getInstance()

    private fun generateRemoteViews(): RemoteViews {
        return RemoteViews(context.packageName, R.layout.app_widget_4x1_google)
    }

    override suspend fun buildUpdate(
        appWidgetId: Int,
        weather: WeatherNowViewModel,
        location: LocationData,
        newOptions: Bundle
    ): RemoteViews {
        val views = buildLayout(appWidgetId, weather, location, newOptions)
        buildDate(location, views, appWidgetId, newOptions)

        return views
    }

    private suspend fun buildLayout(
        appWidgetId: Int,
        weather: WeatherNowViewModel, location: LocationData,
        newOptions: Bundle
    ): RemoteViews {
        val updateViews = generateRemoteViews()

        val backgroundColor =
            newOptions.get(KEY_BGCOLORCODE) as? Int ?: WidgetUtils.getBackgroundColor(appWidgetId)
        val textColor =
            newOptions.get(KEY_TXTCOLORCODE) as? Int ?: WidgetUtils.getTextColor(appWidgetId)
        val viewCtx = context.getThemeContextOverride(
            ColorsUtils.isSuperLight(backgroundColor)
        )

        val txtSizeMultiplier =
            newOptions.get(KEY_TEXTSIZE) as? Float ?: WidgetUtils.getCustomTextSizeMultiplier(
                appWidgetId
            )
        val icoSizeMultiplier =
            newOptions.get(KEY_ICONSIZE) as? Float ?: WidgetUtils.getCustomIconSizeMultiplier(
                appWidgetId
            )

        // WeatherIcon
        val wim = sharedDeps.weatherIconsManager
        val weatherIconResId = wim.getWeatherIconResource(weather.weatherIcon)

        val weatherIconSize = context.dpToPx(36f) * icoSizeMultiplier
        val weatherIconScaledSize = context.dpToPx(54f) // original: 36dp * 1.5 = 54dp

        updateViews.setImageViewBitmap(R.id.weather_icon, null)

        updateViews.setInt(R.id.weather_icon, "setMaxWidth", weatherIconSize.toInt())
        updateViews.setInt(R.id.weather_icon, "setMaxHeight", weatherIconSize.toInt())

        updateViews.setImageViewBitmap(
            R.id.weather_icon,
            ImageUtils.bitmapFromDrawable(
                viewCtx,
                weatherIconResId,
                weatherIconScaledSize,
                weatherIconScaledSize
            )
        )
        if (wim.isFontIcon) {
            updateViews.setInt(R.id.weather_icon, "setColorFilter", textColor)
        } else {
            updateViews.setInt(R.id.weather_icon, "setColorFilter", 0)
        }

        updateViews.setTextViewText(R.id.condition_temp, weather.curTemp)

        updateViews.setTextColor(R.id.condition_temp, textColor)
        updateViews.setTextColor(R.id.date_panel, textColor)
        updateViews.setTextColor(R.id.location_name, textColor)

        // Open default clock/calendar app
        updateViews.setOnClickPendingIntent(
            R.id.date_panel,
            getCalendarAppIntent()
        )

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
        val updateViews = RemoteViews(context.packageName, info.widgetLayoutId)

        // Set sizes for views
        updateViewSizes(updateViews, appWidgetId, newOptions)
        updateDateSize(updateViews, appWidgetId, newOptions)

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
        val isSmallHeight = maxCellHeight.toFloat() / cellHeight <= 1.5f

        val txtSizeMultiplier =
            newOptions.get(KEY_TEXTSIZE) as? Float ?: WidgetUtils.getCustomTextSizeMultiplier(
                appWidgetId
            )

        var forceSmall = false
        var textSize: Float =
            context.resources.getDimensionPixelSize(R.dimen.widget4x1G_text_size).toFloat() // 24sp
        if (cellWidth <= 3) {
            textSize *= (2f / 3) // 16sp
        } else if (isSmallHeight && cellHeight == 1) {
            textSize *= (5 / 6f) // 20sp
            forceSmall = true
        } else if (cellWidth == 4) {
            textSize *= 0.75f // 18sp
        }

        val layoutPadding = context.dpToPx(if (forceSmall) 0f else 12f).toInt()
        updateViews.setViewPadding(
            R.id.layout_container,
            layoutPadding,
            layoutPadding,
            layoutPadding,
            layoutPadding
        )

        updateViews.setTextViewTextSize(
            R.id.date_panel,
            TypedValue.COMPLEX_UNIT_PX,
            textSize * txtSizeMultiplier
        )
        updateViews.setTextViewTextSize(
            R.id.condition_temp,
            TypedValue.COMPLEX_UNIT_PX,
            textSize * txtSizeMultiplier
        )
        updateViews.setTextViewTextSize(
            R.id.location_name,
            TypedValue.COMPLEX_UNIT_SP,
            (if (forceSmall) 12f else 14f) * txtSizeMultiplier
        )
    }

    private fun updateDateSize(views: RemoteViews, appWidgetId: Int, newOptions: Bundle) {
        val minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        val maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        val maxCellHeight = WidgetUtils.getCellsForSize(maxHeight)
        val cellHeight = WidgetUtils.getCellsForSize(minHeight)
        val isSmallHeight = maxCellHeight.toFloat() / cellHeight <= 1.5f

        val txtSizeMultiplier =
            newOptions.get(KEY_TEXTSIZE) as? Float ?: WidgetUtils.getCustomTextSizeMultiplier(
                appWidgetId
            )

        var dateTextSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            24f,
            context.resources.displayMetrics
        )
        if (isSmallHeight && cellHeight <= 2) {
            dateTextSize *= 5 / 6f // 20sp
        }
        views.setTextViewTextSize(
            R.id.date_panel,
            TypedValue.COMPLEX_UNIT_PX,
            dateTextSize * txtSizeMultiplier
        )

        val datePattern =
            DateTimeUtils.getBestPatternForSkeleton(DateTimeConstants.SKELETON_WDAY_ABBR_MONTH_FORMAT)
        views.setCharSequence(R.id.date_panel, "setFormat12Hour", datePattern)
        views.setCharSequence(R.id.date_panel, "setFormat24Hour", datePattern)
    }
}