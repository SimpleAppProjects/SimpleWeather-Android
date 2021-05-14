package com.thewizrd.shared_resources.weatherdata.meteofrance

import com.thewizrd.shared_resources.utils.ConversionMethods
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.utils.getFeelsLikeTemp
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.WeatherManager
import com.thewizrd.shared_resources.weatherdata.model.*
import com.thewizrd.shared_resources.weatherdata.model.Precipitation
import com.thewizrd.shared_resources.weatherdata.model.Weather
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

fun createWeatherData(currRoot: CurrentsResponse, foreRoot: ForecastResponse,
                      alertRoot: AlertsResponse? = null): Weather {
    return Weather().apply {
        location = createLocation(foreRoot)
        updateTime = ZonedDateTime.now(ZoneOffset.UTC)

        forecast = ArrayList(foreRoot.dailyForecast!!.size)
        hrForecast = ArrayList(foreRoot.forecast!!.size)

        // Forecast
        for (daily in foreRoot.dailyForecast!!) {
            forecast.add(createForecast(daily!!))
        }

        for (hourly in foreRoot.forecast!!) {
            hrForecast.add(createHourlyForecast(hourly!!, foreRoot.probabilityForecast))
        }

        condition = createCondition(currRoot)
        atmosphere = createAtmosphere(currRoot)

        ttl = 180

        // Observation only gives temp & wind
        if (hrForecast.isNotEmpty()) {
            val firstHr = hrForecast[0]
            atmosphere.humidity = firstHr.extras.humidity
            atmosphere.pressureMb = firstHr.extras.pressureMb
            atmosphere.pressureIn = firstHr.extras.pressureIn

            precipitation = Precipitation().apply {
                cloudiness = firstHr.extras.cloudiness
                pop = firstHr.extras.pop
                qpfRainIn = firstHr.extras.qpfRainIn
                qpfRainMm = firstHr.extras.qpfRainMm
                qpfSnowIn = firstHr.extras.qpfSnowIn
                qpfSnowCm = firstHr.extras.qpfSnowCm
            }
        }

        // Set feelslike temp
        if (condition.feelslikeF == null && condition.tempF != null && condition.windMph != null && atmosphere.humidity != null) {
            condition.feelslikeF = getFeelsLikeTemp(condition.tempF, condition.windMph, atmosphere.humidity)
            condition.feelslikeC = ConversionMethods.FtoC(condition.feelslikeF)
        }

        if ((condition.highF == null || condition.highC == null) && forecast.size > 0) {
            condition.highF = forecast[0].highF
            condition.highC = forecast[0].highC
            condition.lowF = forecast[0].lowF
            condition.lowC = forecast[0].lowC
        }

        weatherAlerts = createWeatherAlerts(alertRoot)

        source = WeatherAPI.METEOFRANCE
    }
}

fun createLocation(foreRoot: ForecastResponse): Location {
    return Location().apply {
        // Use location name from location provider
        name = null
        latitude = foreRoot.position!!.lat!!.toFloat()
        longitude = foreRoot.position!!.lon!!.toFloat()
        tzLong = foreRoot.position!!.timezone
    }
}

fun createForecast(day: DailyForecastItem): Forecast {
    return Forecast().apply {
        val provider = WeatherManager.getProvider(WeatherAPI.METEOFRANCE)
        val locale = LocaleUtils.getLocale()

        date = LocalDateTime.ofEpochSecond(day.dt!!, 0, ZoneOffset.UTC)
        highC = day.T!!.max
        highF = ConversionMethods.CtoF(highC)
        lowC = day.T!!.min
        lowF = ConversionMethods.CtoF(lowC)

        condition = if (locale.toString() == "en" || locale.toString().startsWith("en_") ||
                locale.toString() == "fr" || locale.toString().startsWith("fr_") ||
                locale == Locale.ROOT) {
            day.weather12H!!.desc
        } else {
            provider.getWeatherCondition(day.weather12H!!.icon)
        }
        icon = provider.getWeatherIcon(false, day.weather12H!!.icon)

        // Extras
        extras = ForecastExtras()
        if (day.humidity?.max != null && day.humidity?.min != null) {
            extras.humidity = ((day.humidity!!.min!! + day.humidity!!.max!!) / 2f).roundToInt()
        }
        if (day.T?.sea != null) {
            extras.pressureMb = day.T!!.sea
            extras.pressureIn = ConversionMethods.mbToInHg(day.T!!.sea!!)
        }
        if (day.precipitation?.jsonMember24h != null) {
            extras.qpfRainMm = day.precipitation!!.jsonMember24h!!.toFloat()
            extras.qpfRainIn = ConversionMethods.mmToIn(day.precipitation!!.jsonMember24h!!.toFloat())
        }
        if (day.uv != null) {
            extras.uvIndex = day.uv!!.toFloat()
        }
    }
}

fun createHourlyForecast(forecast: ForecastItem,
                         probabilityForecasts: List<ProbabilityForecastItem?>?): HourlyForecast {
    return HourlyForecast().apply {
        val provider = WeatherManager.getProvider(WeatherAPI.METEOFRANCE)
        val locale = LocaleUtils.getLocale()

        val date = Instant.ofEpochSecond(forecast.dt!!).atZone(ZoneOffset.UTC)
        setDate(date)

        highC = forecast.T!!.value
        highF = ConversionMethods.CtoF(forecast.T!!.value!!)

        condition = if (locale.toString() == "en" || locale.toString().startsWith("en_") ||
                locale.toString() == "fr" || locale.toString().startsWith("fr_") ||
                locale == Locale.ROOT) {
            forecast.weather!!.desc
        } else {
            provider.getWeatherCondition(forecast.weather!!.icon)
        }
        icon = forecast.weather!!.icon

        // Extras
        extras = ForecastExtras()

        if (forecast.T?.windchill != null) {
            extras.feelslikeC = forecast.T!!.windchill
            extras.feelslikeF = ConversionMethods.CtoF(forecast.T!!.windchill!!)
        }

        if (forecast.humidity != null) {
            extras.humidity = forecast.humidity
        }

        if (forecast.seaLevel != null) {
            extras.pressureMb = forecast.seaLevel
            extras.pressureIn = ConversionMethods.mbToInHg(forecast.seaLevel!!)
        }

        if (forecast.wind != null) {
            if (forecast.wind!!.speed != null && forecast.wind!!.direction != null) {
                windDegrees = forecast.wind!!.direction
                windKph = ConversionMethods.msecToKph(forecast.wind!!.speed!!.toFloat())
                windMph = ConversionMethods.msecToMph(forecast.wind!!.speed!!.toFloat())
                extras.windDegrees = windDegrees
                extras.windMph = windMph
                extras.windKph = windKph
            }
            if (forecast.wind!!.gust != null) {
                extras.windGustKph = ConversionMethods.msecToKph(forecast.wind!!.gust!!.toFloat())
                extras.windGustMph = ConversionMethods.msecToMph(forecast.wind!!.gust!!.toFloat())
            }
        }

        if (forecast.rain != null) {
            if (forecast.rain!!.jsonMember1h != null) {
                extras.qpfRainMm = forecast.rain!!.jsonMember1h
                extras.qpfRainIn = ConversionMethods.mmToIn(forecast.rain!!.jsonMember1h!!)
            } else if (forecast.rain!!.jsonMember3h != null) {
                extras.qpfRainMm = forecast.rain!!.jsonMember3h
                extras.qpfRainIn = ConversionMethods.mmToIn(forecast.rain!!.jsonMember3h!!)
            } else if (forecast.rain!!.jsonMember6h != null) {
                extras.qpfRainMm = forecast.rain!!.jsonMember6h
                extras.qpfRainIn = ConversionMethods.mmToIn(forecast.rain!!.jsonMember6h!!)
            }
        }

        if (forecast.snow != null) {
            if (forecast.snow!!.jsonMember1h != null) {
                extras.qpfSnowCm = forecast.snow!!.jsonMember1h!! / 10
                extras.qpfRainIn = ConversionMethods.mmToIn(forecast.snow!!.jsonMember1h!!)
            } else if (forecast.snow!!.jsonMember3h != null) {
                extras.qpfSnowCm = forecast.snow!!.jsonMember3h!! / 10
                extras.qpfRainIn = ConversionMethods.mmToIn(forecast.snow!!.jsonMember3h!!)
            } else if (forecast.snow!!.jsonMember6h != null) {
                extras.qpfSnowCm = forecast.snow!!.jsonMember6h!! / 10
                extras.qpfRainIn = ConversionMethods.mmToIn(forecast.snow!!.jsonMember6h!!)
            }
        }

        if (forecast.clouds != null) {
            extras.cloudiness = forecast.clouds
        }

        if (!probabilityForecasts.isNullOrEmpty()) {
            // Note: probability forecasts are given either every 3 or 6 hours
            // Rain/Snow object can contain forecast for either next 3 or 6 hrs, or both
            val dt = forecast.dt!! // Unix time in seconds
            val hrsInSec = TimeUnit.HOURS.toSeconds(1)
            var found3hrForecast = false
            var _3hrForecastNA = false

            for (prob in probabilityForecasts) {
                // Check if timestamp is within 3-hr forecast
                if (dt == prob!!.dt!! || dt == (prob.dt!! + hrsInSec) || dt == (prob.dt!! + hrsInSec * 2)) {
                    if (prob.rain?.jsonMember3h != null) {
                        extras.pop = prob.rain!!.jsonMember3h!!.toInt()
                        found3hrForecast = true
                        _3hrForecastNA = false
                    } else {
                        found3hrForecast = false
                        _3hrForecastNA = true
                    }
                    if (extras.pop == null && prob.rain?.jsonMember6h != null) {
                        extras.pop = prob.rain!!.jsonMember6h!!.toInt()
                        _3hrForecastNA = true
                        found3hrForecast = false
                    }
                    if (extras.pop == null) {
                        if (prob.snow?.jsonMember3h != null) {
                            extras.pop = prob.snow!!.jsonMember3h!!.toInt()
                        }
                        if (prob.snow?.jsonMember6h != null) {
                            extras.pop = prob.snow!!.jsonMember6h!!.toInt()
                        }
                    }
                }

                // Timestamp is not within 3-hr forecast; check 6-hr timeframe
                // Check if timestamp is within 6-hr forecast
                if (extras.pop == null && (dt == (prob.dt!! + hrsInSec * 3) || dt == (prob.dt!! + hrsInSec * 4) || dt == (prob.dt!! + hrsInSec * 5))) {
                    if (prob.rain!!.jsonMember6h != null) {
                        extras.pop = prob.rain!!.jsonMember6h!!.toInt()
                        _3hrForecastNA = true
                        found3hrForecast = false
                    }
                    if (extras.pop == null) {
                        if (prob.snow!!.jsonMember6h != null) {
                            extras.pop = prob.snow!!.jsonMember6h!!.toInt()
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
        val provider = WeatherManager.getProvider(WeatherAPI.METEOFRANCE)
        val locale = LocaleUtils.getLocale()

        tempC = currRoot.observation!!.T!!.toFloat()
        tempF = ConversionMethods.CtoF(tempC)

        weather = if (locale.toString() == "en" || locale.toString().startsWith("en_") ||
                locale.toString() == "fr" || locale.toString().startsWith("fr_") ||
                locale == Locale.ROOT) {
            currRoot.observation!!.weather!!.desc
        } else {
            provider.getWeatherCondition(currRoot.observation!!.weather!!.icon)
        }
        icon = currRoot.observation!!.weather!!.icon

        if (currRoot.observation!!.wind != null) {
            windDegrees = currRoot.observation!!.wind!!.direction
            windKph = ConversionMethods.msecToKph(currRoot.observation!!.wind!!.speed!!.toFloat())
            windMph = ConversionMethods.msecToMph(currRoot.observation!!.wind!!.speed!!.toFloat())
        }

        observationTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(currRoot.updatedOn!!), ZoneOffset.UTC)
    }
}

fun createAtmosphere(currRoot: CurrentsResponse): Atmosphere {
    return Atmosphere().apply {
        // no-op
    }
}