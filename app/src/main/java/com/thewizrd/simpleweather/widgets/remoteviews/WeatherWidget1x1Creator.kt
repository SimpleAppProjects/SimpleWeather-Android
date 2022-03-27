package com.thewizrd.simpleweather.widgets.remoteviews

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.thewizrd.shared_resources.controls.WeatherNowViewModel
import com.thewizrd.shared_resources.helpers.ColorsUtils
import com.thewizrd.shared_resources.icons.WeatherIconsManager
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.utils.ImageUtils
import com.thewizrd.shared_resources.weatherdata.WeatherManager
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.widgets.WeatherWidgetProvider1x1
import com.thewizrd.simpleweather.widgets.WidgetProviderInfo
import com.thewizrd.simpleweather.widgets.WidgetUtils

class WeatherWidget1x1Creator(context: Context) : WidgetRemoteViewCreator(context) {
    private fun generateRemoteViews() = RemoteViews(context.packageName, info.widgetLayoutId)

    override val info: WidgetProviderInfo
        get() = WeatherWidgetProvider1x1.Info.getInstance()

    override suspend fun buildUpdate(
        appWidgetId: Int,
        weather: WeatherNowViewModel,
        location: LocationData,
        newOptions: Bundle
    ): RemoteViews {
        return buildLayout(appWidgetId, weather, location, newOptions)
    }

    private suspend fun buildLayout(
        appWidgetId: Int,
        weather: WeatherNowViewModel, location: LocationData,
        newOptions: Bundle
    ): RemoteViews {
        // Build an update that holds the updated widget contents
        val updateViews = generateRemoteViews()

        val backgroundColor = WidgetUtils.getBackgroundColor(appWidgetId)
        val textColor = WidgetUtils.getTextColor(appWidgetId)
        val viewCtx = context.getThemeContextOverride(
            ColorsUtils.isSuperLight(backgroundColor)
        )

        // WeatherIcon
        val wim = WeatherIconsManager.getInstance()
        val weatherIconResId = wim.getWeatherIconResource(weather.weatherIcon)

        val txtSizeMultiplier = WidgetUtils.getCustomTextSizeMultiplier(appWidgetId)
        // icon size: 36dp
        val icoSizeMultiplier = WidgetUtils.getCustomIconSizeMultiplier(appWidgetId)

        val maxIconSize = context.dpToPx(36f) * icoSizeMultiplier
        updateViews.setImageViewBitmap(
            R.id.weather_icon,
            ImageUtils.bitmapFromDrawable(
                viewCtx,
                weatherIconResId,
                maxIconSize,
                maxIconSize
            )
        )
        updateViews.setContentDescription(
            R.id.weather_icon,
            WeatherManager.instance.getWeatherCondition(weather.weatherIcon)
        )

        updateViews.setTextViewText(R.id.condition_temp, weather.curTemp)
        updateViews.setTextColor(R.id.condition_temp, textColor)

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
        updateViews.setTextColor(R.id.location_name, textColor)

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
        val updateViews = generateRemoteViews()

        updateViewSizes(updateViews, appWidgetId, newOptions)

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
        val cellHeight = WidgetUtils.getCellsForSize(minHeight)
        val cellWidth = WidgetUtils.getCellsForSize(minWidth)

        val txtSizeMultiplier = WidgetUtils.getCustomTextSizeMultiplier(appWidgetId)

        if (cellWidth > 1 && cellHeight > 1) {
            updateViews.setTextViewTextSize(
                R.id.location_name,
                TypedValue.COMPLEX_UNIT_SP,
                14f * txtSizeMultiplier
            )
        } else {
            updateViews.setTextViewTextSize(
                R.id.location_name,
                TypedValue.COMPLEX_UNIT_SP,
                12f * txtSizeMultiplier
            )
        }
        if (cellWidth > 2 && cellHeight > 2) {
            updateViews.setTextViewTextSize(
                R.id.condition_temp,
                TypedValue.COMPLEX_UNIT_SP,
                24f * txtSizeMultiplier
            )
        } else if (cellWidth > 1 && cellHeight > 1) {
            updateViews.setTextViewTextSize(
                R.id.condition_temp,
                TypedValue.COMPLEX_UNIT_SP,
                18f * txtSizeMultiplier
            )
        } else {
            updateViews.setTextViewTextSize(
                R.id.condition_temp,
                TypedValue.COMPLEX_UNIT_SP,
                16f * txtSizeMultiplier
            )
        }
    }
}