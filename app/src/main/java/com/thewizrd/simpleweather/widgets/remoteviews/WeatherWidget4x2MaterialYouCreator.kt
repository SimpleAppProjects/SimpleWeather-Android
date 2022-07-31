package com.thewizrd.simpleweather.widgets.remoteviews

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import com.thewizrd.common.controls.WeatherUiModel
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.StringUtils.removeNonDigitChars
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.widgets.WeatherWidgetProvider4x2MaterialYou
import com.thewizrd.simpleweather.widgets.WidgetProviderInfo
import com.thewizrd.simpleweather.widgets.WidgetUpdaterHelper.buildForecast
import com.thewizrd.simpleweather.widgets.WidgetUpdaterHelper.updateForecastSizes
import com.thewizrd.simpleweather.widgets.WidgetUtils
import com.thewizrd.simpleweather.widgets.preferences.KEY_HIDELOCNAME
import com.thewizrd.simpleweather.widgets.preferences.KEY_HIDESETTINGSBTN

class WeatherWidget4x2MaterialYouCreator(context: Context) : WidgetRemoteViewCreator(context) {
    private fun generateRemoteViews(): RemoteViews {
        return RemoteViews(context.packageName, R.layout.app_widget_4x2_materialu)
    }

    override val info: WidgetProviderInfo
        get() = WeatherWidgetProvider4x2MaterialYou.Info.getInstance()

    override suspend fun buildUpdate(
        appWidgetId: Int,
        weather: WeatherUiModel,
        location: LocationData,
        newOptions: Bundle
    ): RemoteViews {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val views4x2 = buildNormalUpdate(appWidgetId, weather, location, newOptions)

            val views3x1 = WeatherWidget3x1MaterialYouCreator(context).run {
                buildUpdate(appWidgetId, weather, location, newOptions)
            }

            RemoteViews(
                mapOf(
                    SizeF(110f, 40f) to views3x1,
                    SizeF(180f, 110f) to views4x2
                )
            )
        } else {
            buildNormalUpdate(appWidgetId, weather, location, newOptions)
        }
    }

    internal suspend fun buildNormalUpdate(
        appWidgetId: Int,
        weather: WeatherUiModel,
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
        weather: WeatherUiModel, location: LocationData,
        newOptions: Bundle
    ): RemoteViews {
        val updateViews = generateRemoteViews()

        // WeatherIcon
        val wim = sharedDeps.weatherIconsManager
        val weatherIconResId = wim.getWeatherIconResource(weather.weatherIcon)

        updateViews.setImageViewResource(R.id.weather_icon, weatherIconResId)
        if (!wim.isFontIcon) {
            // Remove tint
            updateViews.setInt(R.id.weather_icon, "setColorFilter", 0x0)
        }

        updateViews.setTextViewText(
            R.id.condition_hi,
            if (weather.hiTemp?.isNotBlank() == true) weather.hiTemp else WeatherIcons.PLACEHOLDER
        )
        updateViews.setTextViewText(
            R.id.condition_lo,
            if (weather.loTemp?.isNotBlank() == true) weather.loTemp else WeatherIcons.PLACEHOLDER
        )

        updateViews.setTextViewText(R.id.condition_weather, weather.curCondition)

        val temp = weather.curTemp?.removeNonDigitChars()
        updateViews.setTextViewText(
            R.id.condition_temp,
            if (temp.isNullOrBlank()) {
                WeatherIcons.PLACEHOLDER
            } else {
                "$temp°"
            }
        )

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

        setOnClickIntent(location, updateViews)
        setOnSettingsClickIntent(updateViews, location, appWidgetId)

        updateViewSizes(updateViews, appWidgetId, newOptions)

        return updateViews
    }

    override fun resizeWidget(
        info: WidgetProviderInfo,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            val updateViews = generateRemoteViews()

            // Set sizes for views
            updateViewSizes(updateViews, appWidgetId, newOptions)
            updateForecastSizes(context, info, appWidgetId, updateViews, newOptions)

            appWidgetManager.partiallyUpdateAppWidget(appWidgetId, updateViews)
        }
    }

    private fun updateViewSizes(
        updateViews: RemoteViews,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        // Widget dimensions
        val minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val cellWidth = WidgetUtils.getCellsForSize(minWidth)

        updateViews.setViewVisibility(
            R.id.condition_weather,
            if (cellWidth <= 2) View.GONE else View.VISIBLE
        )
    }
}