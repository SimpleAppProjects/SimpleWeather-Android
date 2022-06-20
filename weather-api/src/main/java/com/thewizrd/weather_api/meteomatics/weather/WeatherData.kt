package com.thewizrd.weather_api.meteomatics.weather

import android.annotation.SuppressLint
import com.thewizrd.shared_resources.utils.ConversionMethods
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.model.*
import com.thewizrd.weather_api.weatherModule
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.roundToInt

@SuppressLint("VisibleForTests")
fun createWeatherData(
    currRoot: WeatherResponse,
    foreRoot: WeatherResponse,
    hourlyRoot: WeatherResponse
): Weather {
    return Weather().apply {
        location = createLocation(currRoot)
        updateTime = ZonedDateTime.ofInstant(Instant.parse(currRoot.dateGenerated), ZoneOffset.UTC)

        forecast = createForecast(foreRoot)
        hrForecast = createHourlyForecast(hourlyRoot)

        condition = Condition()
        atmosphere = Atmosphere()
        astronomy = Astronomy()
        precipitation = Precipitation()

        updateWeather(currRoot)

        ttl = 120

        if ((condition.highF == null || condition.highC == null) && forecast.size > 0) {
            condition.highF = forecast[0].highF
            condition.highC = forecast[0].highC
            condition.lowF = forecast[0].lowF
            condition.lowC = forecast[0].lowC
        }

        condition.observationTime = updateTime

        source = WeatherAPI.METEOMATICS
    }
}

fun createLocation(currRoot: WeatherResponse): Location {
    return Location().apply {
        // Use location name from location provider
        name = null
        currRoot.data?.firstOrNull()?.coordinates?.firstOrNull()?.let {
            latitude = it.lat?.toFloat()
            longitude = it.lon?.toFloat()
        }
        tzLong = null
    }
}

private fun Weather.updateWeather(currRoot: WeatherResponse) {
    currRoot.data?.forEach {
        when (it?.parameter) {
            CURRENT_TEMP_C -> {
                it.coordinates?.firstOrNull()?.dates?.firstOrNull()?.value?.toFloatOrNull()
                    ?.let { value ->
                        condition.tempC = value
                        condition.tempF = ConversionMethods.CtoF(value)
                    }
            }
            MSL_PRESSURE_HPA -> {
                it.coordinates?.firstOrNull()?.dates?.firstOrNull()?.value?.toFloatOrNull()
                    ?.let { value ->
                        atmosphere.pressureMb = value
                        atmosphere.pressureIn = ConversionMethods.mbToInHg(value)
                    }
            }
            WIND_SPEED_MS -> {
                it.coordinates?.firstOrNull()?.dates?.firstOrNull()?.value?.toFloatOrNull()
                    ?.let { value ->
                        condition.windKph = ConversionMethods.msecToKph(value)
                        condition.windMph = ConversionMethods.msecToMph(value)
                    }
            }
            WIND_DIR -> {
                it.coordinates?.firstOrNull()?.dates?.firstOrNull()?.value?.toFloatOrNull()
                    ?.roundToInt()?.let { value ->
                    condition.windDegrees = value
                }
            }
            WIND_GUSTS_1H_MS -> {
                it.coordinates?.firstOrNull()?.dates?.firstOrNull()?.value?.toFloatOrNull()
                    ?.let { value ->
                        condition.windGustKph = ConversionMethods.msecToKph(value)
                        condition.windGustMph = ConversionMethods.msecToMph(value)
                    }
            }
            PRECIP_1H_MM -> {
                it.coordinates?.firstOrNull()?.dates?.firstOrNull()?.value?.toFloatOrNull()
                    ?.let { value ->
                        precipitation.qpfRainMm = value
                        precipitation.qpfRainIn = ConversionMethods.mmToIn(value)
                    }
            }
            SUNRISE -> {
                it.coordinates?.firstOrNull()?.dates?.firstOrNull()?.value?.let { value ->
                    astronomy.sunrise = runCatching {
                        LocalDateTime.ofInstant(Instant.parse(value), ZoneOffset.UTC)
                    }.getOrDefault(LocalDateTime.now().plusYears(1).minusNanos(1))
                }
            }
            SUNSET -> {
                it.coordinates?.firstOrNull()?.dates?.firstOrNull()?.value?.let { value ->
                    astronomy.sunset = runCatching {
                        LocalDateTime.ofInstant(Instant.parse(value), ZoneOffset.UTC)
                    }.getOrDefault(LocalDateTime.now().plusYears(1).minusNanos(1))
                }
            }
            UV_IDX -> {
                it.coordinates?.firstOrNull()?.dates?.firstOrNull()?.value?.toFloatOrNull()
                    ?.let { value ->
                        condition.uv = UV(value)
                    }
            }
            WEATHER_SYMBOL_1H -> {
                it.coordinates?.firstOrNull()?.dates?.firstOrNull()?.value?.toIntOrNull()
                    ?.let { value ->
                        val provider =
                            weatherModule.weatherProviderFactory.getWeatherProvider(WeatherAPI.METEOMATICS)
                        condition.icon = provider.getWeatherIcon(false, value.toString())
                        condition.weather = provider.getWeatherCondition(value.toString())
                    }
            }
        }
    }
}

fun createForecast(foreRoot: WeatherResponse): List<Forecast> {
    val forecastMap = mutableMapOf<String?, Forecast>()
    val provider = weatherModule.weatherProviderFactory.getWeatherProvider(WeatherAPI.METEOMATICS)

    foreRoot.data?.forEach {
        when (it?.parameter) {
            MAX_TEMP_24H_C -> {
                it.coordinates?.firstOrNull()?.dates?.forEach { datesItem ->
                    val dateStr = datesItem?.date

                    val forecast = forecastMap.getOrDefault(dateStr, Forecast())

                    datesItem?.value?.toFloatOrNull()?.let { value ->
                        forecast.highC = value
                        forecast.highF = ConversionMethods.CtoF(value)
                    }

                    forecastMap[dateStr] = forecast
                }
            }
            MIN_TEMP_24H_C -> {
                it.coordinates?.firstOrNull()?.dates?.forEach { datesItem ->
                    val dateStr = datesItem?.date

                    val forecast = forecastMap.getOrDefault(dateStr, Forecast())

                    datesItem?.value?.toFloatOrNull()?.let { value ->
                        forecast.lowC = value
                        forecast.lowF = ConversionMethods.CtoF(value)
                    }

                    forecastMap[dateStr] = forecast
                }
            }
            MSL_PRESSURE_HPA -> {
                it.coordinates?.firstOrNull()?.dates?.forEach { datesItem ->
                    val dateStr = datesItem?.date

                    val forecast = forecastMap.getOrDefault(dateStr, Forecast())

                    datesItem?.value?.toFloatOrNull()?.let { value ->
                        forecast.extras = forecast.extras ?: ForecastExtras()

                        forecast.extras.pressureMb = value
                        forecast.extras.pressureIn = ConversionMethods.mbToInHg(value)
                    }

                    forecastMap[dateStr] = forecast
                }
            }
            WIND_GUSTS_24H_MS -> {
                it.coordinates?.firstOrNull()?.dates?.forEach { datesItem ->
                    val dateStr = datesItem?.date

                    val forecast = forecastMap.getOrDefault(dateStr, Forecast())

                    datesItem?.value?.toFloatOrNull()?.let { value ->
                        forecast.extras = forecast.extras ?: ForecastExtras()

                        forecast.extras.windGustKph = ConversionMethods.msecToKph(value)
                        forecast.extras.windGustMph = ConversionMethods.msecToMph(value)
                    }

                    forecastMap[dateStr] = forecast
                }
            }
            PRECIP_24H_MM -> {
                it.coordinates?.firstOrNull()?.dates?.forEach { datesItem ->
                    val dateStr = datesItem?.date

                    val forecast = forecastMap.getOrDefault(dateStr, Forecast())

                    datesItem?.value?.toFloatOrNull()?.let { value ->
                        forecast.extras = forecast.extras ?: ForecastExtras()

                        forecast.extras.qpfRainMm = value
                        forecast.extras.qpfRainIn = ConversionMethods.mmToIn(value)
                    }

                    forecastMap[dateStr] = forecast
                }
            }
            WEATHER_SYMBOL_24H -> {
                it.coordinates?.firstOrNull()?.dates?.forEach { datesItem ->
                    val dateStr = datesItem?.date

                    val forecast = forecastMap.getOrDefault(dateStr, Forecast())

                    datesItem?.value?.toIntOrNull()?.let { value ->
                        forecast.icon = provider.getWeatherIcon(false, value.toString())
                        forecast.condition = provider.getWeatherCondition(value.toString())
                    }

                    forecastMap[dateStr] = forecast
                }
            }
        }
    }

    return forecastMap.map {
        it.value.apply {
            date = LocalDateTime.ofInstant(Instant.parse(it.key), ZoneOffset.UTC)
        }
    }
}

fun createHourlyForecast(hourlyRoot: WeatherResponse): List<HourlyForecast> {
    val forecastMap = mutableMapOf<String?, Pair<HourlyForecast, Boolean>>()
    val provider = weatherModule.weatherProviderFactory.getWeatherProvider(WeatherAPI.METEOMATICS)

    hourlyRoot.data?.forEach {
        when (it?.parameter) {
            CURRENT_TEMP_C -> {
                it.coordinates?.firstOrNull()?.dates?.forEach { datesItem ->
                    val dateStr = datesItem?.date

                    val forecast = forecastMap.getOrDefault(dateStr, Pair(HourlyForecast(), false))

                    datesItem?.value?.toFloatOrNull()?.let { value ->
                        forecast.first.highC = value
                        forecast.first.highF = ConversionMethods.CtoF(value)
                    }

                    forecastMap[dateStr] = forecast
                }
            }
            MSL_PRESSURE_HPA -> {
                it.coordinates?.firstOrNull()?.dates?.forEach { datesItem ->
                    val dateStr = datesItem?.date

                    val forecast = forecastMap.getOrDefault(dateStr, Pair(HourlyForecast(), false))

                    datesItem?.value?.toFloatOrNull()?.let { value ->
                        forecast.first.extras = forecast.first.extras ?: ForecastExtras()

                        forecast.first.extras.pressureMb = value
                        forecast.first.extras.pressureIn = ConversionMethods.mbToInHg(value)
                    }

                    forecastMap[dateStr] = forecast
                }
            }
            WIND_SPEED_MS -> {
                it.coordinates?.firstOrNull()?.dates?.forEach { datesItem ->
                    val dateStr = datesItem?.date

                    val forecast = forecastMap.getOrDefault(dateStr, Pair(HourlyForecast(), false))

                    datesItem?.value?.toFloatOrNull()?.let { value ->
                        forecast.first.extras = forecast.first.extras ?: ForecastExtras()

                        forecast.first.extras.windKph = ConversionMethods.msecToKph(value)
                        forecast.first.extras.windMph = ConversionMethods.msecToMph(value)
                    }

                    forecastMap[dateStr] = forecast
                }
            }
            WIND_DIR -> {
                it.coordinates?.firstOrNull()?.dates?.forEach { datesItem ->
                    val dateStr = datesItem?.date

                    val forecast = forecastMap.getOrDefault(dateStr, Pair(HourlyForecast(), false))

                    datesItem?.value?.toFloatOrNull()?.roundToInt()?.let { value ->
                        forecast.first.extras = forecast.first.extras ?: ForecastExtras()

                        forecast.first.extras.windDegrees = value
                    }

                    forecastMap[dateStr] = forecast
                }
            }
            WIND_GUSTS_1H_MS -> {
                it.coordinates?.firstOrNull()?.dates?.forEach { datesItem ->
                    val dateStr = datesItem?.date

                    val forecast = forecastMap.getOrDefault(dateStr, Pair(HourlyForecast(), false))

                    datesItem?.value?.toFloatOrNull()?.let { value ->
                        forecast.first.extras = forecast.first.extras ?: ForecastExtras()

                        forecast.first.extras.windGustKph = ConversionMethods.msecToKph(value)
                        forecast.first.extras.windGustMph = ConversionMethods.msecToMph(value)
                    }

                    forecastMap[dateStr] = forecast
                }
            }
            PRECIP_1H_MM -> {
                it.coordinates?.firstOrNull()?.dates?.forEach { datesItem ->
                    val dateStr = datesItem?.date

                    val forecast = forecastMap.getOrDefault(dateStr, Pair(HourlyForecast(), false))

                    datesItem?.value?.toFloatOrNull()?.let { value ->
                        forecast.first.extras = forecast.first.extras ?: ForecastExtras()

                        forecast.first.extras.qpfRainMm = value
                        forecast.first.extras.qpfRainIn = ConversionMethods.mmToIn(value)
                    }

                    forecastMap[dateStr] = forecast
                }
            }
            UV_IDX -> {
                it.coordinates?.firstOrNull()?.dates?.forEach { datesItem ->
                    val dateStr = datesItem?.date

                    val forecast = forecastMap.getOrDefault(dateStr, Pair(HourlyForecast(), false))

                    datesItem?.value?.toFloatOrNull()?.let { value ->
                        forecast.first.extras = forecast.first.extras ?: ForecastExtras()

                        forecast.first.extras.uvIndex = value
                    }

                    forecastMap[dateStr] = forecast
                }
            }
            WEATHER_SYMBOL_1H -> {
                it.coordinates?.firstOrNull()?.dates?.forEach { datesItem ->
                    val dateStr = datesItem?.date

                    val forecast = forecastMap.getOrDefault(dateStr, Pair(HourlyForecast(), false))

                    datesItem?.value?.toIntOrNull()?.let { value ->
                        forecast.first.icon =
                            provider.getWeatherIcon(forecast.second, value.toString())
                        forecast.first.condition = provider.getWeatherCondition(value.toString())
                    }

                    forecastMap[dateStr] = forecast
                }
            }
        }
    }

    return forecastMap.map {
        it.value.first.apply {
            date = ZonedDateTime.ofInstant(Instant.parse(it.key), ZoneOffset.UTC)
        }
    }
}