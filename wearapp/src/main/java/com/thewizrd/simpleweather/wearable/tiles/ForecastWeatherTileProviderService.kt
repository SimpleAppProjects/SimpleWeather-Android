package com.thewizrd.simpleweather.wearable.tiles

import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.tiles.RequestBuilders
import com.google.android.horologist.tiles.images.drawableResToImageResource
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.wearable.tiles.layouts.ID_WEATHER_CHANCE_ICON
import com.thewizrd.simpleweather.wearable.tiles.layouts.ID_WEATHER_CLOUDINESS_ICON
import com.thewizrd.simpleweather.wearable.tiles.layouts.ID_WEATHER_WINDSPEED_ICON
import com.thewizrd.simpleweather.wearable.tiles.layouts.forecastWeatherTileLayout
import com.thewizrd.simpleweather.wearable.tiles.layouts.hourlyForecastWeatherTileLayout
import com.thewizrd.weather_api.weatherModule
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class ForecastWeatherTileProviderService : WeatherCoroutinesTileService() {
    companion object {
        private const val TAG = "ForecastWeatherTileProviderService"
        private const val FORECAST_LENGTH = 4
    }

    override fun renderTile(
        weather: Weather?,
        requestParams: RequestBuilders.TileRequest
    ): LayoutElementBuilders.LayoutElement {
        resources.clear()
        resources.add("${ID_WEATHER_ICON_PREFIX}${weather?.condition?.icon ?: WeatherIcons.NA}")
        resources.add(ID_WEATHER_CHANCE_ICON)
        resources.add(ID_WEATHER_CLOUDINESS_ICON)
        resources.add(ID_WEATHER_WINDSPEED_ICON)

        // Add forecast icons to resources
        weather?.forecast?.take(FORECAST_LENGTH)?.forEach { forecast ->
            resources.add("${ID_WEATHER_ICON_PREFIX}${forecast.icon ?: WeatherIcons.NA}")
        }

        // Add forecast icons to resources
        weather?.hrForecast?.take(FORECAST_LENGTH)?.forEach { forecast ->
            resources.add("${ID_WEATHER_ICON_PREFIX}${forecast.icon ?: WeatherIcons.NA}")
        }

        return if (requestParams.currentState.lastClickableId == ID_HR_FORECAST_ICON_PREFIX && !weather?.hrForecast.isNullOrEmpty()) {
            hourlyForecastWeatherTileLayout(weather, this, requestParams.deviceConfiguration)
        } else {
            forecastWeatherTileLayout(weather, this, requestParams.deviceConfiguration)
        }
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

        if (weather != null && (weather.forecast.isNullOrEmpty() || weather.hrForecast.isNullOrEmpty())) {
            val locationData = settingsManager.getHomeData()

            if (locationData?.isValid == true) {
                if (weather.forecast.isNullOrEmpty()) {
                    val forecasts = settingsManager.getWeatherForecastData(locationData.query)
                    weather.forecast = forecasts?.forecast?.take(FORECAST_LENGTH)
                }

                if (weather.hrForecast.isNullOrEmpty()) {
                    val now = ZonedDateTime.now().withZoneSameInstant(locationData.tzOffset)
                    val hrInterval = weatherModule.weatherManager.getHourlyForecastInterval()

                    val hrforecasts =
                        settingsManager.getHourlyForecastsByQueryOrderByDateByLimitFilterByDate(
                            locationData.query,
                            FORECAST_LENGTH,
                            now.minusHours((hrInterval * 0.5).toLong())
                                .truncatedTo(ChronoUnit.HOURS)
                        )

                    weather.hrForecast = hrforecasts.take(FORECAST_LENGTH)
                }
            }
        }

        return weather
    }
}