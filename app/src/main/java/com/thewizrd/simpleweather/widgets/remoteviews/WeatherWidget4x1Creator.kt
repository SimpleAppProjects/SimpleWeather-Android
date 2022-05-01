package com.thewizrd.simpleweather.widgets.remoteviews

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.thewizrd.common.controls.WeatherNowViewModel
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.widgets.WeatherWidgetProvider4x1
import com.thewizrd.simpleweather.widgets.WidgetProviderInfo
import com.thewizrd.simpleweather.widgets.WidgetUpdaterHelper.buildForecast
import com.thewizrd.simpleweather.widgets.WidgetUpdaterHelper.updateForecastSizes
import com.thewizrd.simpleweather.widgets.WidgetUtils
import com.thewizrd.simpleweather.widgets.preferences.KEY_TEXTSIZE
import com.thewizrd.simpleweather.widgets.preferences.KEY_TXTCOLORCODE

class WeatherWidget4x1Creator(context: Context) : WidgetRemoteViewCreator(context) {
    override val info: WidgetProviderInfo
        get() = WeatherWidgetProvider4x1.Info.getInstance()

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
        weather: WeatherNowViewModel, location: LocationData,
        newOptions: Bundle
    ): RemoteViews {
        // Build an update that holds the updated widget contents
        val hideLocationName = WidgetUtils.isLocationNameHidden(appWidgetId)

        val updateViews = if (hideLocationName) {
            RemoteViews(context.packageName, R.layout.app_widget_4x1_nolocation)
        } else {
            RemoteViews(context.packageName, R.layout.app_widget_4x1)
        }

        val textColor =
            newOptions.get(KEY_TXTCOLORCODE) as? Int ?: WidgetUtils.getTextColor(appWidgetId)

        if (!hideLocationName) {
            updateViews.setTextViewText(R.id.location_name, weather.location)
            updateViews.setTextColor(R.id.location_name, textColor)
        }

        updateViews.setInt(R.id.refresh_button, "setColorFilter", textColor)
        updateViews.setInt(R.id.settings_button, "setColorFilter", textColor)

        val txtSizeMultiplier =
            newOptions.get(KEY_TEXTSIZE) as? Float ?: WidgetUtils.getCustomTextSizeMultiplier(
                appWidgetId
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

        if (!hideLocationName) {
            updateViews.setViewVisibility(
                R.id.location_name,
                if (WidgetUtils.isLocationNameHidden(appWidgetId)) View.GONE else View.VISIBLE
            )
        }
        updateViews.setViewVisibility(
            R.id.settings_button,
            if (WidgetUtils.isSettingsButtonHidden(appWidgetId)) View.GONE else View.VISIBLE
        )
        updateViews.setViewVisibility(
            R.id.refresh_button,
            if (WidgetUtils.isRefreshButtonHidden(appWidgetId)) View.GONE else View.VISIBLE
        )

        // Resizing
        updateViewSizes(updateViews, appWidgetId, newOptions)

        setOnClickIntent(location, updateViews)
        setOnSettingsClickIntent(updateViews, location, appWidgetId)
        setOnRefreshClickIntent(updateViews, appWidgetId)

        return updateViews
    }

    override fun resizeWidget(
        info: WidgetProviderInfo,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        val updateViews = if (WidgetUtils.isLocationNameHidden(appWidgetId)) {
            RemoteViews(context.packageName, R.layout.app_widget_4x1_nolocation)
        } else {
            RemoteViews(context.packageName, R.layout.app_widget_4x1)
        }

        updateViewSizes(updateViews, appWidgetId, newOptions)
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
        val maxWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
        val maxCellHeight = WidgetUtils.getCellsForSize(maxHeight)
        val maxCellWidth = WidgetUtils.getCellsForSize(maxWidth)
        val cellHeight = WidgetUtils.getCellsForSize(minHeight)
        val cellWidth = WidgetUtils.getCellsForSize(minWidth)
        val isSmallHeight = maxCellHeight.toFloat() / cellHeight <= 1.5f
        val isSmallWidth = maxCellWidth.toFloat() / cellWidth <= 1.5f

        val txtSizeMultiplier =
            newOptions.get(KEY_TEXTSIZE) as? Float ?: WidgetUtils.getCustomTextSizeMultiplier(
                appWidgetId
            )

        if (!WidgetUtils.isLocationNameHidden(appWidgetId)) {
            var locTextSize = 12f
            if (cellHeight > 1 && (!isSmallWidth || cellWidth > 4)) locTextSize = 14f

            updateViews.setTextViewTextSize(
                R.id.location_name,
                TypedValue.COMPLEX_UNIT_SP,
                locTextSize * txtSizeMultiplier
            )
        }

        if (isSmallHeight && cellHeight == 1) {
            val padding = context.dpToPx(0f).toInt()
            updateViews.setViewPadding(
                R.id.layout_container,
                padding,
                padding,
                padding,
                padding
            )
        } else {
            val padding = context.dpToPx(8f).toInt()
            updateViews.setViewPadding(
                R.id.layout_container,
                padding,
                padding,
                padding,
                padding
            )
        }
    }
}