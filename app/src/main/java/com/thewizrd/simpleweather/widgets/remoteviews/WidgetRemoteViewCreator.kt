package com.thewizrd.simpleweather.widgets.remoteviews

import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews
import com.thewizrd.shared_resources.controls.WeatherNowViewModel
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.widgets.WidgetUtils

abstract class WidgetRemoteViewCreator(context: Context) :
    AbstractWidgetRemoteViewCreator(context) {
    override suspend fun buildUpdate(appWidgetId: Int, newOptions: Bundle): RemoteViews? {
        val locData = getLocation(appWidgetId) ?: return null
        val weather = loadWeather(locData) ?: return null
        val viewModel = WeatherNowViewModel(weather)

        return buildUpdate(appWidgetId, viewModel, locData, newOptions).apply {
            buildExtras(appWidgetId, this, viewModel, locData, newOptions)
        }
    }

    abstract suspend fun buildUpdate(
        appWidgetId: Int,
        weather: WeatherNowViewModel, location: LocationData,
        newOptions: Bundle
    ): RemoteViews

    open suspend fun buildExtras(
        appWidgetId: Int,
        updateViews: RemoteViews,
        weather: WeatherNowViewModel,
        location: LocationData,
        newOptions: Bundle
    ) {
        if (WidgetUtils.isBackgroundCustomOnlyWidget(WidgetUtils.getWidgetTypeFromID(appWidgetId))) {
            val backgroundColor = WidgetUtils.getBackgroundColor(appWidgetId)

            if (backgroundColor == Colors.TRANSPARENT) {
                updateViews.setInt(R.id.widget, "setBackgroundColor", Colors.TRANSPARENT)
            } else {
                updateViews.setInt(R.id.widget, "setBackgroundColor", backgroundColor)
            }
        }
    }
}