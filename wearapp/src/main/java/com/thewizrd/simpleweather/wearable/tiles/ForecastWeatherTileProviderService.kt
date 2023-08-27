package com.thewizrd.simpleweather.wearable.tiles

import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ResourceBuilders
import com.google.android.horologist.tiles.images.drawableResToImageResource
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.wearable.tiles.layouts.ID_WEATHER_CHANCE_ICON
import com.thewizrd.simpleweather.wearable.tiles.layouts.ID_WEATHER_CLOUDINESS_ICON
import com.thewizrd.simpleweather.wearable.tiles.layouts.ID_WEATHER_WINDSPEED_ICON
import com.thewizrd.simpleweather.wearable.tiles.layouts.forecastWeatherTileLayout

class ForecastWeatherTileProviderService : WeatherCoroutinesTileService() {
    companion object {
        private const val TAG = "ForecastWeatherTileProviderService"
        private const val FORECAST_LENGTH = 4
    }

    override fun renderTile(
        weather: Weather?,
        deviceParameters: DeviceParameters
    ): LayoutElementBuilders.LayoutElement {
        resources.clear()
        resources.add("${ID_WEATHER_ICON_PREFIX}${weather?.condition?.icon ?: WeatherIcons.NA}")
        resources.add(ID_WEATHER_CHANCE_ICON)
        resources.add(ID_WEATHER_CLOUDINESS_ICON)
        resources.add(ID_WEATHER_WINDSPEED_ICON)

        // Add forecast icons to resources
        weather?.forecast?.forEachIndexed { index, forecast ->
            resources.add("${ID_FORECAST_ICON_PREFIX}idx=${index}:${forecast.icon ?: WeatherIcons.NA}")
        }

        return forecastWeatherTileLayout(weather, this, deviceParameters)
    }

    override fun ResourceBuilders.Resources.Builder.produceRequestedResource(
        deviceParameters: DeviceParameters,
        id: String
    ) {
        when (id) {
            ID_WEATHER_CHANCE_ICON -> {
                addIdToImageMapping(
                    ID_WEATHER_CHANCE_ICON,
                    drawableResToImageResource(R.drawable.wi_umbrella)
                )
            }

            ID_WEATHER_CLOUDINESS_ICON -> {
                addIdToImageMapping(
                    ID_WEATHER_CLOUDINESS_ICON,
                    drawableResToImageResource(R.drawable.wi_cloudy)
                )
            }

            ID_WEATHER_WINDSPEED_ICON -> {
                addIdToImageMapping(
                    ID_WEATHER_WINDSPEED_ICON,
                    drawableResToImageResource(R.drawable.wi_strong_wind)
                )
            }
        }
    }

    override suspend fun getWeather(): Weather? {
        val weather = super.getWeather()

        if (weather != null && weather.forecast.isNullOrEmpty()) {
            val locationData = settingsManager.getHomeData()

            if (locationData?.isValid == true) {
                val forecasts = settingsManager.getWeatherForecastData(locationData.query)
                weather.forecast = forecasts?.forecast?.take(FORECAST_LENGTH)
            }
        }

        return weather
    }
}