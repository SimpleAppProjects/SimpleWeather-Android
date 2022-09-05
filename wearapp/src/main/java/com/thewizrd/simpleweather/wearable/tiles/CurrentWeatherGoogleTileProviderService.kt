package com.thewizrd.simpleweather.wearable.tiles

import androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters
import androidx.wear.tiles.LayoutElementBuilders
import androidx.wear.tiles.ResourceBuilders
import com.google.android.horologist.tiles.images.drawableResToImageResource
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.wearable.tiles.layouts.ID_WEATHER_HI_ICON
import com.thewizrd.simpleweather.wearable.tiles.layouts.ID_WEATHER_LO_ICON
import com.thewizrd.simpleweather.wearable.tiles.layouts.currentWeatherGoogleTileLayout

class CurrentWeatherGoogleTileProviderService : WeatherCoroutinesTileService() {
    override fun renderTile(
        weather: Weather?,
        deviceParameters: DeviceParameters
    ): LayoutElementBuilders.LayoutElement {
        resources.clear()
        resources.add("$ID_WEATHER_ICON_PREFIX${weather?.condition?.icon ?: WeatherIcons.NA}")
        resources.add(ID_WEATHER_HI_ICON)
        resources.add(ID_WEATHER_LO_ICON)

        return currentWeatherGoogleTileLayout(weather, this, deviceParameters)
    }

    override fun ResourceBuilders.Resources.Builder.produceRequestedResource(
        deviceParameters: DeviceParameters,
        id: String
    ) {
        when (id) {
            ID_WEATHER_HI_ICON -> {
                addIdToImageMapping(
                    ID_WEATHER_HI_ICON,
                    drawableResToImageResource(R.drawable.ic_arrow_upward_24dp)
                )
            }
            ID_WEATHER_LO_ICON -> {
                addIdToImageMapping(
                    ID_WEATHER_LO_ICON,
                    drawableResToImageResource(R.drawable.ic_arrow_downward_24dp)
                )
            }
        }
    }
}