package com.thewizrd.simpleweather.wearable.tiles

import androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters
import androidx.wear.tiles.LayoutElementBuilders.LayoutElement
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.wearable.tiles.layouts.currentWeatherTileLayout

class CurrentWeatherTileProviderService : WeatherCoroutinesTileService() {
    override fun renderTile(
        weather: Weather?,
        deviceParameters: DeviceParameters
    ): LayoutElement {
        resources.clear()
        resources.add("$ID_WEATHER_ICON_PREFIX${weather?.condition?.icon ?: WeatherIcons.NA}")

        return currentWeatherTileLayout(weather, this, deviceParameters)
    }
}