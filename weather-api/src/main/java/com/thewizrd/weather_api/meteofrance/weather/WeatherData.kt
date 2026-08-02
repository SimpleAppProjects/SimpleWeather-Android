package com.thewizrd.weather_api.meteofrance.weather

import android.annotation.SuppressLint
import com.thewizrd.shared_resources.utils.ConversionMethods
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.utils.calculateDewpointC
import com.thewizrd.shared_resources.utils.calculateDewpointF
import com.thewizrd.shared_resources.utils.getFeelsLikeTemp
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.model.Atmosphere
import com.thewizrd.shared_resources.weatherdata.model.Condition
import com.thewizrd.shared_resources.weatherdata.model.Forecast
import com.thewizrd.shared_resources.weatherdata.model.ForecastExtras
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.shared_resources.weatherdata.model.Location
import com.thewizrd.shared_resources.weatherdata.model.Precipitation
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.weather_api.weatherModule
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

@SuppressLint("VisibleForTests")
fun createWeatherData(currRoot: CurrentsResponse, foreRoot: ForecastResponse,
                      alertRoot: AlertsResponse? = null): Weather {
    return Weather().apply {
        location = createLocation(foreRoot)
        updateTime = ZonedDateTime.now(ZoneOffset.UTC)

        // Forecast
        forecast = foreRoot.properties?.dailyForecast?.map { daily ->
            createForecast(daily)
        }
        hrForecast = foreRoot.properties?.forecast?.map { hourly ->
            createHourlyForecast(hourly, foreRoot.properties.probabilityForecast)
        }

        condition = createCondition(currRoot)
        atmosphere = createAtmosphere(currRoot)
        precipitation = createPrecipitation(currRoot)

        ttl = 180

        // Observation only gives temp & wind
        if (!hrForecast.isNullOrEmpty()) {
            val firstHr = hrForecast!![0]
            atmosphere?.run {
                humidity = firstHr.extras?.humidity
                pressureMb = firstHr.extras?.pressureMb
                pressureIn = firstHr.extras?.pressureIn
                dewpointC = firstHr.extras?.dewpointC
                dewpointF = firstHr.extras?.dewpointF
                visibilityKm = firstHr.extras?.visibilityKm
                visibilityMi = firstHr.extras?.visibilityMi
            }

            precipitation?.run {
                cloudiness = firstHr.extras?.cloudiness
                pop = firstHr.extras?.pop
                qpfRainIn = firstHr.extras?.qpfRainIn
                qpfRainMm = firstHr.extras?.qpfRainMm
                qpfSnowIn = firstHr.extras?.qpfSnowIn
                qpfSnowCm = firstHr.extras?.qpfSnowCm
            }
        }

        // Set feelslike temp
        if (condition?.feelslikeF == null && condition?.tempF != null && condition?.windMph != null && atmosphere?.humidity != null) {
            condition!!.feelslikeF =
                getFeelsLikeTemp(condition!!.tempF, condition!!.windMph, atmosphere!!.humidity)
            condition!!.feelslikeC = ConversionMethods.FtoC(condition!!.feelslikeF)
        }

        if ((condition?.highF == null || condition?.highC == null) && forecast!!.isNotEmpty()) {
            condition!!.highF = forecast!![0].highF
            condition!!.highC = forecast!![0].highC
            condition!!.lowF = forecast!![0].lowF
            condition!!.lowC = forecast!![0].lowC
        }

        weatherAlerts = createWeatherAlerts(alertRoot)

        source = WeatherAPI.METEOFRANCE
    }
}

fun createLocation(foreRoot: ForecastResponse): Location {
    return Location().apply {
        // Use location name from location provider
        name = null
        latitude = foreRoot.geometry?.coordinates?.getOrNull(1)
        longitude = foreRoot.geometry?.coordinates?.getOrNull(0)
        tzLong = foreRoot.properties?.timezone
    }
}

fun createForecast(day: DailyForecastItem): Forecast {
    return Forecast().apply {
        val provider = weatherModule.weatherManager.getWeatherProvider(WeatherAPI.METEOFRANCE)
        val locale = LocaleUtils.getLocale()

        date = ZonedDateTime.parse(day.time).toLocalDateTime()

        day.tMax?.let {
            highC = it
            highF = ConversionMethods.CtoF(it)
        }

        day.tMin?.let {
            lowC = it
            lowF = ConversionMethods.CtoF(it)
        }

        condition =
            if (!day.dailyWeatherDescription.isNullOrBlank() && locale.toLanguageTag() == "en" || locale.toLanguageTag()
                    .startsWith("en_") ||
                locale.toLanguageTag() == "fr" || locale.toLanguageTag().startsWith("fr_") ||
                locale == Locale.ROOT
            ) {
                day.dailyWeatherDescription
            } else {
                provider.getWeatherCondition(provider.getWeatherIcon(false, day.dailyWeatherIcon))
            }
        icon = day.dailyWeatherIcon

        // Extras
        extras = ForecastExtras()
        if (day.relativeHumidityMax != null && day.relativeHumidityMin != null) {
            extras.humidity =
                ((day.relativeHumidityMin + day.relativeHumidityMax) / 2f).roundToInt()
        }
        day.tSea?.let {
            extras.pressureMb = it
            extras.pressureIn = ConversionMethods.mbToInHg(it)
        }
        day.totalPrecipitation24h?.let {
            extras.qpfRainMm = it
            extras.qpfRainIn = ConversionMethods.mmToIn(it)
        }
        extras.uvIndex = day.uvIndex
    }
}

fun createHourlyForecast(forecast: ForecastItem,
                         probabilityForecasts: List<ProbabilityForecastItem>?
): HourlyForecast {
    return HourlyForecast().apply {
        val provider = weatherModule.weatherManager.getWeatherProvider(WeatherAPI.METEOFRANCE)
        val locale = LocaleUtils.getLocale()

        date = ZonedDateTime.parse(forecast.time)

        forecast.t?.let {
            highC = it
            highF = ConversionMethods.CtoF(it)
        }

        condition =
            if (!forecast.weatherDescription.isNullOrBlank() && locale.toLanguageTag() == "en" || locale.toLanguageTag()
                    .startsWith("en_") ||
                locale.toLanguageTag() == "fr" || locale.toLanguageTag().startsWith("fr_") ||
                locale == Locale.ROOT
            ) {
                forecast.weatherDescription
            } else {
                provider.getWeatherCondition(provider.getWeatherIcon(forecast.weatherIcon))
            }
        icon = forecast.weatherIcon

        // Extras
        extras = ForecastExtras()

        forecast.tWindchill?.let {
            extras.feelslikeC = it
            extras.feelslikeF = ConversionMethods.CtoF(it)
        }

        extras.humidity = forecast.relativeHumidity

        forecast.pSea?.let {
            extras.pressureMb = it
            extras.pressureIn = ConversionMethods.mbToInHg(it)
        }

        forecast.windSpeed?.let {
            extras.windMph = ConversionMethods.msecToMph(it)
            extras.windKph = ConversionMethods.msecToKph(it)
            extras.windDegrees = forecast.windDirection
        }
        forecast.windSpeedGust?.let {
            extras.windGustKph = ConversionMethods.msecToKph(it)
            extras.windGustMph = ConversionMethods.msecToMph(it)
        }

        if (forecast.rain1h != null) {
            extras.qpfRainMm = forecast.rain1h
            extras.qpfRainIn = ConversionMethods.mmToIn(forecast.rain1h)
        } else if (forecast.rain3h != null) {
            extras.qpfRainMm = forecast.rain3h
            extras.qpfRainIn = ConversionMethods.mmToIn(forecast.rain3h)
        } else if (forecast.rain6h != null) {
            extras.qpfRainMm = forecast.rain6h
            extras.qpfRainIn = ConversionMethods.mmToIn(forecast.rain6h)
        }

        if (forecast.snow1h != null) {
            extras.qpfSnowCm = forecast.snow1h / 10
            extras.qpfRainIn = ConversionMethods.mmToIn(forecast.snow1h)
        } else if (forecast.snow3h != null) {
            extras.qpfSnowCm = forecast.snow3h / 10
            extras.qpfRainIn = ConversionMethods.mmToIn(forecast.snow3h)
        } else if (forecast.snow6h != null) {
            extras.qpfSnowCm = forecast.snow6h / 10
            extras.qpfRainIn = ConversionMethods.mmToIn(forecast.snow6h)
        }

        extras.cloudiness = forecast.totalCloudCover

        if (highC != null && extras.humidity != null) {
            extras.dewpointC = calculateDewpointC(highC, extras.humidity)
        }
        if (highF != null && extras.humidity != null) {
            extras.dewpointF = calculateDewpointF(highF, extras.humidity)
        }

        if (!probabilityForecasts.isNullOrEmpty()) {
            // Note: probability forecasts are given either every 3 or 6 hours
            // Rain/Snow object can contain forecast for either next 3 or 6 hrs, or both
            val dt = date.truncatedTo(ChronoUnit.HOURS)
            var found3hrForecast = false
            var _3hrForecastNA = false

            for (prob in probabilityForecasts) {
                val probDt = ZonedDateTime.parse(prob.time).truncatedTo(ChronoUnit.HOURS)

                // Check if timestamp is within 3-hr forecast
                if (dt.isEqual(probDt) || dt.isEqual(probDt.plusHours(1)) || dt.isEqual(
                        probDt.plusHours(
                            2
                        )
                    )
                ) {
                    if (prob.rainHazard3h != null) {
                        extras.pop = prob.rainHazard3h.toInt()
                        found3hrForecast = true
                        _3hrForecastNA = false
                    } else {
                        found3hrForecast = false
                        _3hrForecastNA = true
                    }
                    if (extras.pop == null && prob.rainHazard3h != null) {
                        extras.pop = prob.rainHazard3h.toInt()
                        _3hrForecastNA = true
                        found3hrForecast = false
                    }
                    if (extras.pop == null) {
                        if (prob.snowHazard3h != null) {
                            extras.pop = prob.snowHazard3h.toInt()
                        }
                        if (prob.snowHazard6h != null) {
                            extras.pop = prob.snowHazard6h.toInt()
                        }
                    }
                }

                // Timestamp is not within 3-hr forecast; check 6-hr timeframe
                // Check if timestamp is within 6-hr forecast
                if (extras.pop == null && (dt.isEqual(probDt.plusHours(3)) || dt.isEqual(
                        probDt.plusHours(
                            4
                        )
                    ) || dt.isEqual(probDt.plusHours(5)))
                ) {
                    if (prob.rainHazard6h != null) {
                        extras.pop = prob.rainHazard6h.toInt()
                        _3hrForecastNA = true
                        found3hrForecast = false
                    }
                    if (extras.pop == null) {
                        if (prob.snowHazard6h != null) {
                            extras.pop = prob.snowHazard6h.toInt()
                        }
                    }
                }

                if (extras.pop != null && (found3hrForecast || _3hrForecastNA)) break
            }
        }
    }
}

fun createCondition(currRoot: CurrentsResponse): Condition {
    return Condition().apply {
        val provider = weatherModule.weatherManager.getWeatherProvider(WeatherAPI.METEOFRANCE)
        val locale = LocaleUtils.getLocale()

        currRoot.properties?.gridded?.t?.let {
            tempC = it
            tempF = ConversionMethods.CtoF(it)
        }

        weather =
            if (!currRoot.properties?.gridded?.weatherDescription.isNullOrBlank() && locale.toLanguageTag() == "en" || locale.toLanguageTag()
                    .startsWith("en_") ||
                locale.toLanguageTag() == "fr" || locale.toLanguageTag().startsWith("fr_") ||
                locale == Locale.ROOT
            ) {
                currRoot.properties?.gridded?.weatherDescription
            } else {
                provider.getWeatherCondition(provider.getWeatherIcon(currRoot.properties?.gridded?.weatherIcon))
            }
        icon = currRoot.properties?.gridded?.weatherIcon

        windDegrees = currRoot.properties?.gridded?.windDirection
        currRoot.properties?.gridded?.windSpeed?.let {
            windKph = ConversionMethods.msecToKph(it)
            windMph = ConversionMethods.msecToMph(it)
        }
        currRoot.properties?.gridded?.windSpeedGust?.let {
            windGustKph = ConversionMethods.msecToKph(it)
            windGustMph = ConversionMethods.msecToMph(it)
        }

        observationTime =
            ZonedDateTime.parse(currRoot.properties?.gridded?.time ?: currRoot.updateTime)
    }
}

fun createAtmosphere(currRoot: CurrentsResponse): Atmosphere {
    return Atmosphere().apply {
        // no-op
    }
}

fun createPrecipitation(currRoot: CurrentsResponse): Precipitation {
    return Precipitation().apply {
        // no-op
    }
}