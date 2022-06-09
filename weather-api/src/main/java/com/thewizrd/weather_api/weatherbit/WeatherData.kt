package com.thewizrd.weather_api.weatherbit

import com.thewizrd.shared_resources.utils.ConversionMethods
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.utils.StringUtils.toUpperCase
import com.thewizrd.shared_resources.utils.ZoneIdCompat
import com.thewizrd.shared_resources.utils.getBeaufortScale
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.model.*
import com.thewizrd.weather_api.weatherModule
import java.text.DecimalFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.roundToInt

fun createWeatherData(currRoot: CurrentResponse, foreRoot: ForecastResponse): Weather {
    return Weather().apply {
        val currData = currRoot.data?.first()!!
        val tzLong = currData.timezone!!
        val zoneId = ZoneIdCompat.of(tzLong)

        location = createLocation(currData)
        updateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(currData.ts!!.toLong()), zoneId)

        // 16-day forecast
        forecast = ArrayList(16)

        for (day in foreRoot.data!!) {
            val fcast = createForecast(day!!)
            forecast.add(fcast)
        }

        currRoot.minutely?.let {
            minForecast = ArrayList(it.size)

            it.forEach { minute ->
                minForecast.add(createMinutelyForecast(minute!!).apply {
                    date = date.withZoneSameInstant(zoneId)
                })
            }
        }

        condition = createCondition(currData)
        atmosphere = createAtmosphere(currData)
        astronomy = createAstronomy(currData, foreRoot)
        precipitation = createPrecipitation(currData)
        ttl = 120

        val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
        df.applyPattern("#.####")
        query = String.format(
            Locale.ROOT,
            "lat=%s&lon=%s",
            df.format(location.latitude),
            location.longitude
        )

        if ((condition.highF == null || condition.highC == null) && forecast.size > 0) {
            condition.highF = forecast[0].highF
            condition.highC = forecast[0].highC
            condition.lowF = forecast[0].lowF
            condition.lowC = forecast[0].lowC
        }

        weatherAlerts = createWeatherAlerts(currRoot.alerts, tzLong)

        source = WeatherAPI.WEATHERBITIO
    }
}

fun createLocation(currData: CurrentDataItem): Location {
    return Location().apply {
        // Use location name from location provider
        name = null
        latitude = currData.lat
        longitude = currData.lon
        tzLong = currData.timezone
    }
}

fun createForecast(forecast: ForecastDataItem): Forecast {
    return Forecast().apply {
        date = LocalDate.parse(forecast.validDate, DateTimeFormatter.ISO_DATE).atStartOfDay()
        forecast.highTemp?.let {
            highC = it
            highF = ConversionMethods.CtoF(it)
        }
        forecast.lowTemp?.let {
            lowC = it
            lowF = ConversionMethods.CtoF(it)
        }
        condition = forecast.weather?.description?.toUpperCase()
        icon = weatherModule.weatherManager.getWeatherProvider(WeatherAPI.WEATHERBITIO)
            .getWeatherIcon(forecast.weather?.icon)

        // Extras
        extras = ForecastExtras()
        extras.humidity = forecast.rh
        extras.cloudiness = forecast.clouds
        // 1hPA = 1mbar
        forecast.slp?.let {
            extras.pressureMb = it
            extras.pressureIn = ConversionMethods.mbToInHg(it)
        }
        extras.windDegrees = forecast.windDir
        forecast.windSpd?.let {
            extras.windMph = ConversionMethods.msecToMph(it)
            extras.windKph = ConversionMethods.msecToKph(it)
        }
        forecast.dewpt?.let {
            extras.dewpointC = it
            extras.dewpointF = ConversionMethods.CtoF(it).roundToInt().toFloat()
        }
        forecast.appMaxTemp?.let {
            extras.feelslikeC = it
            extras.feelslikeF = ConversionMethods.CtoF(it)
        }
        extras.pop = forecast.pop
        forecast.vis?.let {
            extras.visibilityKm = it
            extras.visibilityMi = ConversionMethods.kmToMi(it)
        }
        forecast.windGustSpd?.let {
            extras.windGustMph = ConversionMethods.msecToMph(it)
            extras.windGustKph = ConversionMethods.msecToKph(it)
        }
        forecast.precip?.let {
            extras.qpfRainMm = it
            extras.qpfRainIn = ConversionMethods.mmToIn(it)
        }
        forecast.snow?.let {
            extras.qpfSnowCm = it / 10f
            extras.qpfSnowIn = ConversionMethods.mmToIn(it)
        }
        extras.uvIndex = forecast.uv
    }
}

fun createMinutelyForecast(item: MinutelyItem): MinutelyForecast {
    return MinutelyForecast().apply {
        date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(item.ts!!.toLong()), ZoneOffset.UTC)
        rainMm = item.precip
    }
}

fun createCondition(current: CurrentDataItem): Condition {
    return Condition().apply {
        weather = current.weather?.description?.toUpperCase()
        current.temp?.let {
            tempC = it
            tempF = ConversionMethods.CtoF(it)
        }
        windDegrees = current.windDir
        current.windSpd?.let {
            windKph = ConversionMethods.msecToKph(it)
            windMph = ConversionMethods.msecToMph(it)
            beaufort = Beaufort(getBeaufortScale(it))
        }
        current.appTemp?.let {
            feelslikeC = it
            feelslikeF = ConversionMethods.CtoF(it)
        }

        icon = weatherModule.weatherManager.getWeatherProvider(WeatherAPI.WEATHERBITIO)
            .getWeatherIcon(current.weather?.icon)

        current.uv?.let {
            uv = UV(it)
        }

        current.aqi?.let {
            airQuality = AirQuality().apply {
                index = it
            }
        }

        observationTime = ZonedDateTime.ofInstant(
            Instant.ofEpochSecond(current.ts!!.toLong()),
            ZoneIdCompat.of(current.timezone)
        )
    }
}

fun createAtmosphere(current: CurrentDataItem): Atmosphere {
    return Atmosphere().apply {
        humidity = current.rh?.roundToInt()
        current.slp?.let {
            pressureMb = it
            pressureIn = ConversionMethods.mbToInHg(it)
        }
        pressureTrend = ""
        current.vis?.let {
            visibilityKm = it
            visibilityMi = ConversionMethods.kmToMi(it)
        }
        current.dewpt?.let {
            dewpointC = it
            dewpointF = ConversionMethods.CtoF(it)
        }
    }
}

fun createAstronomy(current: CurrentDataItem, foreRoot: ForecastResponse): Astronomy {
    return Astronomy().apply {
        val zoneId = ZoneIdCompat.of(current.timezone)
        val obsTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(current.ts!!.toLong()), zoneId)
        val obsDateStr = obsTime.toLocalDate().toString()

        val currentDayFcast =
            foreRoot.data?.firstOrNull { it != null && it.validDate == obsDateStr }

        if (currentDayFcast != null) {
            runCatching {
                sunrise = ZonedDateTime.ofInstant(
                    Instant.ofEpochSecond(currentDayFcast.sunriseTs!!.toLong()),
                    zoneId
                ).toLocalDateTime()
            }
            runCatching {
                sunset = ZonedDateTime.ofInstant(
                    Instant.ofEpochSecond(currentDayFcast.sunsetTs!!.toLong()),
                    zoneId
                ).toLocalDateTime()
            }
            runCatching {
                moonrise = ZonedDateTime.ofInstant(
                    Instant.ofEpochSecond(currentDayFcast.moonriseTs!!.toLong()),
                    zoneId
                ).toLocalDateTime()
            }
            runCatching {
                moonset = ZonedDateTime.ofInstant(
                    Instant.ofEpochSecond(currentDayFcast.moonsetTs!!.toLong()),
                    zoneId
                ).toLocalDateTime()
            }

            currentDayFcast.moonPhaseLunation?.times(100)?.let {
                val moonPhaseType = if (it >= 2 && it < 23) {
                    MoonPhase.MoonPhaseType.WAXING_CRESCENT
                } else if (it >= 23 && it < 26) {
                    MoonPhase.MoonPhaseType.FIRST_QTR
                } else if (it >= 26 && it < 48) {
                    MoonPhase.MoonPhaseType.WAXING_GIBBOUS
                } else if (it >= 48 && it < 52) {
                    MoonPhase.MoonPhaseType.FULL_MOON
                } else if (it >= 52 && it < 73) {
                    MoonPhase.MoonPhaseType.WANING_GIBBOUS
                } else if (it >= 73 && it < 76) {
                    MoonPhase.MoonPhaseType.LAST_QTR
                } else if (it >= 76 && it < 98) {
                    MoonPhase.MoonPhaseType.WANING_CRESCENT
                } else { // 0, 1, 98, 99, 100
                    MoonPhase.MoonPhaseType.NEWMOON
                }

                moonPhase = MoonPhase(moonPhaseType)
            }
        }

        // If the sun won't set/rise, set time to the future
        if (sunrise == null) {
            sunrise = LocalDateTime.now().plusYears(1).minusNanos(1)
        }
        if (sunset == null) {
            sunset = LocalDateTime.now().plusYears(1).minusNanos(1)
        }
        if (moonrise == null) {
            moonrise = DateTimeUtils.LOCALDATETIME_MIN
        }
        if (moonset == null) {
            moonset = DateTimeUtils.LOCALDATETIME_MIN
        }
    }
}

fun createPrecipitation(current: CurrentDataItem): Precipitation {
    return Precipitation().apply {
        // Use cloudiness value here
        cloudiness = current.clouds
        current.precip?.let {
            qpfRainMm = it
            qpfRainIn = ConversionMethods.mmToIn(it)
        }
        current.snow?.let {
            qpfSnowCm = it / 10f
            qpfSnowIn = ConversionMethods.mmToIn(it)
        }
    }
}