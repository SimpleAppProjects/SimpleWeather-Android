package com.thewizrd.weather_api.meteomatics.weather

import android.annotation.SuppressLint
import com.thewizrd.shared_resources.utils.ConversionMethods
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.model.*
import com.thewizrd.weather_api.weatherModule
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.roundToInt

@SuppressLint("VisibleForTests")
fun createWeatherData(
    currRoot: WeatherResponse,
    foreRoot: WeatherResponse,
    hourlyRoot: WeatherResponse,
    minutelyRoot: WeatherResponse
): Weather {
    return Weather().apply {
        location = createLocation(currRoot)
        updateTime = ZonedDateTime.ofInstant(Instant.parse(currRoot.dateGenerated), ZoneOffset.UTC)

        forecast = createForecast(foreRoot)
        hrForecast = createHourlyForecast(hourlyRoot)
        minForecast = createMinutelyForecast(minutelyRoot)

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
    var isNight = false

    currRoot.data?.forEach {
        when (it?.parameter) {
            CURRENT_TEMP_C -> {
                it.coordinates?.firstOrNull()?.dates?.firstOrNull()?.value?.toFloatOrNull()
                    ?.let { value ->
                        condition.tempC = value
                        condition.tempF = ConversionMethods.CtoF(value)
                    }
            }
            MAX_TEMP_1H_C -> {
                it.coordinates?.firstOrNull()?.dates?.firstOrNull()?.value?.toFloatOrNull()
                    ?.let { value ->
                        condition.highC = value
                        condition.highF = ConversionMethods.CtoF(value)
                    }
            }
            MIN_TEMP_1H_C -> {
                it.coordinates?.firstOrNull()?.dates?.firstOrNull()?.value?.toFloatOrNull()
                    ?.let { value ->
                        condition.lowC = value
                        condition.lowF = ConversionMethods.CtoF(value)
                    }
            }
            APPARENT_TEMP_C -> {
                it.coordinates?.firstOrNull()?.dates?.firstOrNull()?.value?.toFloatOrNull()
                    ?.let { value ->
                        condition.feelslikeC = value
                        condition.feelslikeF = ConversionMethods.CtoF(value)
                    }
            }
            RELATIVE_HUMIDITY -> {
                it.coordinates?.firstOrNull()?.dates?.firstOrNull()?.value?.toFloatOrNull()
                    ?.roundToInt()?.let { value ->
                    atmosphere.humidity = value
                }
            }
            DEW_POINT_C -> {
                it.coordinates?.firstOrNull()?.dates?.firstOrNull()?.value?.toFloatOrNull()
                    ?.let { value ->
                        atmosphere.dewpointC = value
                        atmosphere.dewpointF = ConversionMethods.CtoF(value)
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
            TOTAL_CLOUD_COVER -> {
                it.coordinates?.firstOrNull()?.dates?.firstOrNull()?.value?.toFloatOrNull()
                    ?.roundToInt()?.let { value ->
                    precipitation.cloudiness = value
                }
            }
            PROB_PRECIP_1H -> {
                it.coordinates?.firstOrNull()?.dates?.firstOrNull()?.value?.toFloatOrNull()
                    ?.roundToInt()?.let { value ->
                    precipitation.pop = value
                }
            }
            PRECIP_1H_MM -> {
                it.coordinates?.firstOrNull()?.dates?.firstOrNull()?.value?.toFloatOrNull()
                    ?.let { value ->
                        precipitation.qpfRainMm = value
                        precipitation.qpfRainIn = ConversionMethods.mmToIn(value)
                    }
            }
            PRECIP_TYPE_1H -> {
                it.coordinates?.firstOrNull()?.dates?.firstOrNull()?.value?.toIntOrNull()
                    ?.let { value ->
                        /*
                        Index	Precipitation Type
                            0	None
                            1	Rain
                            2	Rain and snow mixed
                            3	Snow
                            4	Sleet
                            5	Freezing rain
                            6	Hail
                         */
                        if ((value == 3 || value == 6) && precipitation.qpfRainMm != null) {
                            precipitation.qpfSnowCm = precipitation.qpfRainMm / 10
                            precipitation.qpfSnowIn = precipitation.qpfRainIn

                            precipitation.qpfRainMm = null
                            precipitation.qpfRainIn = null
                        }
                    }
            }
            VISIBILITY_KM -> {
                it.coordinates?.firstOrNull()?.dates?.firstOrNull()?.value?.toFloatOrNull()
                    ?.let { value ->
                        atmosphere.visibilityKm = value
                        atmosphere.visibilityMi = ConversionMethods.kmToMi(value)
                    }
            }
            BIRCH_POLLEN_GM3 -> {
                it.coordinates?.firstOrNull()?.dates?.firstOrNull()?.value?.toFloatOrNull()
                    ?.roundToInt()?.let { value ->
                    condition.pollen = condition.pollen ?: Pollen()
                    condition.pollen.treePollenCount = when {
                        value in 1..14 -> Pollen.PollenCount.LOW
                        value in 15..89 -> Pollen.PollenCount.MODERATE
                        value in 90..1499 -> Pollen.PollenCount.HIGH
                        value >= 1500 -> Pollen.PollenCount.VERY_HIGH
                        else -> Pollen.PollenCount.UNKNOWN
                    }
                }
            }
            GRASS_POLLEN_GM3 -> {
                it.coordinates?.firstOrNull()?.dates?.firstOrNull()?.value?.toFloatOrNull()
                    ?.roundToInt()?.let { value ->
                    condition.pollen = condition.pollen ?: Pollen()
                    condition.pollen.grassPollenCount = when {
                        value in 1..4 -> Pollen.PollenCount.LOW
                        value in 5..19 -> Pollen.PollenCount.MODERATE
                        value in 20..199 -> Pollen.PollenCount.HIGH
                        value >= 200 -> Pollen.PollenCount.VERY_HIGH
                        else -> Pollen.PollenCount.UNKNOWN
                    }
                }
            }
            RAGWEED_POLLEN_GM3 -> {
                it.coordinates?.firstOrNull()?.dates?.firstOrNull()?.value?.toFloatOrNull()
                    ?.roundToInt()?.let { value ->
                    condition.pollen = condition.pollen ?: Pollen()
                    condition.pollen.ragweedPollenCount = when {
                        value in 1..9 -> Pollen.PollenCount.LOW
                        value in 10..49 -> Pollen.PollenCount.MODERATE
                        value in 50..499 -> Pollen.PollenCount.HIGH
                        value >= 500 -> Pollen.PollenCount.VERY_HIGH
                        else -> Pollen.PollenCount.UNKNOWN
                    }
                }
            }
            MOON_PHASE -> {
                it.coordinates?.firstOrNull()?.dates?.firstOrNull()?.value?.toBigDecimalOrNull()
                    ?.setScale(1, RoundingMode.HALF_EVEN)?.toFloat()?.let { value ->
                    astronomy.moonPhase = when {
                        value == -1f -> MoonPhase(MoonPhase.MoonPhaseType.FULL_MOON)
                        value > -1f && value < -0.5f -> MoonPhase(MoonPhase.MoonPhaseType.WANING_GIBBOUS)
                        value == -0.5f -> MoonPhase(MoonPhase.MoonPhaseType.LAST_QTR)
                        value > -0.5f && value < 0f -> MoonPhase(MoonPhase.MoonPhaseType.WANING_CRESCENT)
                        value == 0f -> MoonPhase(MoonPhase.MoonPhaseType.NEWMOON)
                        value > 0f && value < 0.5f -> MoonPhase(MoonPhase.MoonPhaseType.WAXING_CRESCENT)
                        value == 0.5f -> MoonPhase(MoonPhase.MoonPhaseType.FIRST_QTR)
                        value > 0.5f && value < 1f -> MoonPhase(MoonPhase.MoonPhaseType.WAXING_GIBBOUS)
                        value == 1f -> MoonPhase(MoonPhase.MoonPhaseType.FULL_MOON)
                        else -> MoonPhase(MoonPhase.MoonPhaseType.FULL_MOON)
                    }
                }
            }
            MOONRISE -> {
                it.coordinates?.firstOrNull()?.dates?.firstOrNull()?.value?.let { value ->
                    astronomy.moonrise = runCatching {
                        LocalDateTime.ofInstant(Instant.parse(value), ZoneOffset.UTC)
                    }.getOrDefault(DateTimeUtils.getLocalDateTimeMIN())
                }
            }
            MOONSET -> {
                it.coordinates?.firstOrNull()?.dates?.firstOrNull()?.value?.let { value ->
                    astronomy.moonset = runCatching {
                        LocalDateTime.ofInstant(Instant.parse(value), ZoneOffset.UTC)
                    }.getOrDefault(DateTimeUtils.getLocalDateTimeMIN())
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
            IS_NIGHT -> {
                it.coordinates?.firstOrNull()?.dates?.firstOrNull()?.value?.toIntOrNull()
                    ?.let { value ->
                        isNight = value == 1
                    }
            }
            WEATHER_SYMBOL_1H -> {
                it.coordinates?.firstOrNull()?.dates?.firstOrNull()?.value?.toIntOrNull()
                    ?.let { value ->
                        val provider =
                            weatherModule.weatherProviderFactory.getWeatherProvider(WeatherAPI.METEOMATICS)
                        condition.icon = provider.getWeatherIcon(isNight, value.toString())
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
            MAX_TEMP_12H_C -> {
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
            MIN_TEMP_12H_C -> {
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
            APPARENT_TEMP_C -> {
                it.coordinates?.firstOrNull()?.dates?.forEach { datesItem ->
                    val dateStr = datesItem?.date

                    val forecast = forecastMap.getOrDefault(dateStr, Forecast())

                    datesItem?.value?.toFloatOrNull()?.let { value ->
                        forecast.extras = forecast.extras ?: ForecastExtras()

                        forecast.extras.feelslikeC = value
                        forecast.extras.feelslikeF = ConversionMethods.CtoF(value)
                    }

                    forecastMap[dateStr] = forecast
                }
            }
            MEAN_RELATIVE_HUMIDITY_12H -> {
                it.coordinates?.firstOrNull()?.dates?.forEach { datesItem ->
                    val dateStr = datesItem?.date

                    val forecast = forecastMap.getOrDefault(dateStr, Forecast())

                    datesItem?.value?.toFloatOrNull()?.roundToInt()?.let { value ->
                        forecast.extras = forecast.extras ?: ForecastExtras()

                        forecast.extras.humidity = value
                    }

                    forecastMap[dateStr] = forecast
                }
            }
            MEAN_DEW_POINT_12H_C -> {
                it.coordinates?.firstOrNull()?.dates?.forEach { datesItem ->
                    val dateStr = datesItem?.date

                    val forecast = forecastMap.getOrDefault(dateStr, Forecast())

                    datesItem?.value?.toFloatOrNull()?.let { value ->
                        forecast.extras = forecast.extras ?: ForecastExtras()

                        forecast.extras.dewpointC = value
                        forecast.extras.dewpointF = ConversionMethods.CtoF(value)
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
            MEAN_WIND_SPEED_12H_MS -> {
                it.coordinates?.firstOrNull()?.dates?.forEach { datesItem ->
                    val dateStr = datesItem?.date

                    val forecast = forecastMap.getOrDefault(dateStr, Forecast())

                    datesItem?.value?.toFloatOrNull()?.let { value ->
                        forecast.extras = forecast.extras ?: ForecastExtras()

                        forecast.extras.windKph = ConversionMethods.msecToKph(value)
                        forecast.extras.windMph = ConversionMethods.msecToMph(value)
                    }

                    forecastMap[dateStr] = forecast
                }
            }
            MEAN_WIND_DIR_12H -> {
                it.coordinates?.firstOrNull()?.dates?.forEach { datesItem ->
                    val dateStr = datesItem?.date

                    val forecast = forecastMap.getOrDefault(dateStr, Forecast())

                    datesItem?.value?.toFloatOrNull()?.roundToInt()?.let { value ->
                        forecast.extras = forecast.extras ?: ForecastExtras()

                        forecast.extras.windDegrees = value
                    }

                    forecastMap[dateStr] = forecast
                }
            }
            MEAN_WIND_GUSTS_12H_MS -> {
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
            TOTAL_CLOUD_COVER -> {
                it.coordinates?.firstOrNull()?.dates?.forEach { datesItem ->
                    val dateStr = datesItem?.date

                    val forecast = forecastMap.getOrDefault(dateStr, Forecast())

                    datesItem?.value?.toFloatOrNull()?.roundToInt()?.let { value ->
                        forecast.extras = forecast.extras ?: ForecastExtras()

                        forecast.extras.cloudiness = value
                    }

                    forecastMap[dateStr] = forecast
                }
            }
            PROB_PRECIP_12H -> {
                it.coordinates?.firstOrNull()?.dates?.forEach { datesItem ->
                    val dateStr = datesItem?.date

                    val forecast = forecastMap.getOrDefault(dateStr, Forecast())

                    datesItem?.value?.toFloatOrNull()?.roundToInt()?.let { value ->
                        forecast.extras = forecast.extras ?: ForecastExtras()

                        forecast.extras.pop = value
                    }

                    forecastMap[dateStr] = forecast
                }
            }
            PRECIP_12H_MM -> {
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
            PRECIP_TYPE_12H -> {
                it.coordinates?.firstOrNull()?.dates?.forEach { datesItem ->
                    val dateStr = datesItem?.date

                    val forecast = forecastMap.getOrDefault(dateStr, Forecast())

                    datesItem?.value?.toIntOrNull()?.let { value ->
                        forecast.extras = forecast.extras ?: ForecastExtras()

                        /*
                        Index	Precipitation Type
                            0	None
                            1	Rain
                            2	Rain and snow mixed
                            3	Snow
                            4	Sleet
                            5	Freezing rain
                            6	Hail
                         */
                        if ((value == 3 || value == 6) && forecast.extras.qpfRainMm != null) {
                            forecast.extras.qpfSnowCm = forecast.extras.qpfRainMm / 10
                            forecast.extras.qpfSnowIn = forecast.extras.qpfRainIn

                            forecast.extras.qpfRainMm = null
                            forecast.extras.qpfRainIn = null
                        }
                    }

                    forecastMap[dateStr] = forecast
                }
            }
            MAX_UV_12H -> {
                it.coordinates?.firstOrNull()?.dates?.forEach { datesItem ->
                    val dateStr = datesItem?.date

                    val forecast = forecastMap.getOrDefault(dateStr, Forecast())

                    datesItem?.value?.toFloatOrNull()?.let { value ->
                        forecast.extras = forecast.extras ?: ForecastExtras()

                        forecast.extras.uvIndex = value
                    }

                    forecastMap[dateStr] = forecast
                }
            }
            WEATHER_SYMBOL_12H -> {
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
            APPARENT_TEMP_C -> {
                it.coordinates?.firstOrNull()?.dates?.forEach { datesItem ->
                    val dateStr = datesItem?.date

                    val forecast = forecastMap.getOrDefault(dateStr, Pair(HourlyForecast(), false))

                    datesItem?.value?.toFloatOrNull()?.let { value ->
                        forecast.first.extras = forecast.first.extras ?: ForecastExtras()

                        forecast.first.extras.feelslikeC = value
                        forecast.first.extras.feelslikeF = ConversionMethods.CtoF(value)
                    }

                    forecastMap[dateStr] = forecast
                }
            }
            MEAN_RELATIVE_HUMIDITY_1H -> {
                it.coordinates?.firstOrNull()?.dates?.forEach { datesItem ->
                    val dateStr = datesItem?.date

                    val forecast = forecastMap.getOrDefault(dateStr, Pair(HourlyForecast(), false))

                    datesItem?.value?.toFloatOrNull()?.roundToInt()?.let { value ->
                        forecast.first.extras = forecast.first.extras ?: ForecastExtras()

                        forecast.first.extras.humidity = value
                    }

                    forecastMap[dateStr] = forecast
                }
            }
            MEAN_DEW_POINT_1H_C -> {
                it.coordinates?.firstOrNull()?.dates?.forEach { datesItem ->
                    val dateStr = datesItem?.date

                    val forecast = forecastMap.getOrDefault(dateStr, Pair(HourlyForecast(), false))

                    datesItem?.value?.toFloatOrNull()?.let { value ->
                        forecast.first.extras = forecast.first.extras ?: ForecastExtras()

                        forecast.first.extras.dewpointC = value
                        forecast.first.extras.dewpointF = ConversionMethods.CtoF(value)
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
            MEAN_WIND_SPEED_1H_MS -> {
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
            MEAN_WIND_DIR_1H -> {
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
            MEAN_WIND_GUSTS_1H_MS -> {
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
            TOTAL_CLOUD_COVER -> {
                it.coordinates?.firstOrNull()?.dates?.forEach { datesItem ->
                    val dateStr = datesItem?.date

                    val forecast = forecastMap.getOrDefault(dateStr, Pair(HourlyForecast(), false))

                    datesItem?.value?.toFloatOrNull()?.roundToInt()?.let { value ->
                        forecast.first.extras = forecast.first.extras ?: ForecastExtras()

                        forecast.first.extras.cloudiness = value
                    }

                    forecastMap[dateStr] = forecast
                }
            }
            PROB_PRECIP_1H -> {
                it.coordinates?.firstOrNull()?.dates?.forEach { datesItem ->
                    val dateStr = datesItem?.date

                    val forecast = forecastMap.getOrDefault(dateStr, Pair(HourlyForecast(), false))

                    datesItem?.value?.toFloatOrNull()?.roundToInt()?.let { value ->
                        forecast.first.extras = forecast.first.extras ?: ForecastExtras()

                        forecast.first.extras.pop = value
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
            PRECIP_TYPE_1H -> {
                it.coordinates?.firstOrNull()?.dates?.forEach { datesItem ->
                    val dateStr = datesItem?.date

                    val forecast = forecastMap.getOrDefault(dateStr, Pair(HourlyForecast(), false))

                    datesItem?.value?.toIntOrNull()?.let { value ->
                        forecast.first.extras = forecast.first.extras ?: ForecastExtras()

                        /*
                        Index	Precipitation Type
                            0	None
                            1	Rain
                            2	Rain and snow mixed
                            3	Snow
                            4	Sleet
                            5	Freezing rain
                            6	Hail
                         */
                        if ((value == 3 || value == 6) && forecast.first.extras.qpfRainMm != null) {
                            forecast.first.extras.qpfSnowCm = forecast.first.extras.qpfRainMm / 10
                            forecast.first.extras.qpfSnowIn = forecast.first.extras.qpfRainIn

                            forecast.first.extras.qpfRainMm = null
                            forecast.first.extras.qpfRainIn = null
                        }
                    }

                    forecastMap[dateStr] = forecast
                }
            }
            MAX_UV_1H -> {
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
            IS_NIGHT -> {
                it.coordinates?.firstOrNull()?.dates?.forEach { datesItem ->
                    val dateStr = datesItem?.date

                    var forecast = forecastMap.getOrDefault(dateStr, Pair(HourlyForecast(), false))

                    datesItem?.value?.toIntOrNull()?.let { value ->
                        forecast = Pair(forecast.first, value == 1)
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

fun createMinutelyForecast(minutelyRoot: WeatherResponse): List<MinutelyForecast> {
    val forecastMap = mutableMapOf<String?, MinutelyForecast>()

    minutelyRoot.data?.forEach {
        when (it?.parameter) {
            PRECIP_5MIN_MM -> {
                it.coordinates?.firstOrNull()?.dates?.forEach { datesItem ->
                    val dateStr = datesItem?.date

                    val forecast = forecastMap.getOrDefault(dateStr, MinutelyForecast())

                    datesItem?.value?.toFloatOrNull()?.let { value ->
                        forecast.rainMm = value
                    }

                    forecastMap[dateStr] = forecast
                }
            }
        }
    }

    return forecastMap.map {
        it.value.apply {
            date = ZonedDateTime.ofInstant(Instant.parse(it.key), ZoneOffset.UTC)
        }
    }
}