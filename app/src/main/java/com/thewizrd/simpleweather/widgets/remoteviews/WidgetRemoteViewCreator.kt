package com.thewizrd.simpleweather.widgets.remoteviews

import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews
import com.thewizrd.shared_resources.controls.WeatherNowViewModel
import com.thewizrd.shared_resources.locationdata.LocationData

abstract class WidgetRemoteViewCreator(context: Context) :
    AbstractWidgetRemoteViewCreator(context) {
    override suspend fun buildUpdate(appWidgetId: Int, newOptions: Bundle): RemoteViews? {
        val locData = getLocation(appWidgetId) ?: return null
        val weather = loadWeather(locData) ?: return null
        val viewModel = WeatherNowViewModel(weather)

        return buildUpdate(appWidgetId, viewModel, locData, newOptions)
    }

    abstract suspend fun buildUpdate(
        appWidgetId: Int,
        weather: WeatherNowViewModel, location: LocationData,
        newOptions: Bundle
    ): RemoteViews
}