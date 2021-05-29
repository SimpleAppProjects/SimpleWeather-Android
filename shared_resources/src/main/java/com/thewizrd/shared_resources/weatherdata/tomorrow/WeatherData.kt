package com.thewizrd.shared_resources.weatherdata.tomorrow

import android.annotation.SuppressLint
import com.thewizrd.shared_resources.utils.ConversionMethods
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.utils.getBeaufortScale
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.model.*
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

@SuppressLint("VisibleForTests")
fun createWeatherData(root: Rootobject, minutelyRoot: Rootobject?, alertRoot: AlertsRootobject?): Weather {
    return Weather().apply {
        location = createLocation(root)
        updateTime = ZonedDateTime.now(ZoneOffset.UTC)

        for (timeline in root.data.timelines) {
            if (timeline.timestep == "1h") {
                hrForecast = ArrayList(timeline.intervals.size)

                for (interval in timeline.intervals) {
                    hrForecast.add(createHourlyForecast(interval))
                }
            } else if (timeline.timestep == "1d") {
                forecast = ArrayList(timeline.intervals.size)

                for (interval in timeline.intervals) {
                    if (astronomy == null && updateTime.truncatedTo(ChronoUnit.DAYS).isEqual(ZonedDateTime.parse(interval.startTime).truncatedTo(ChronoUnit.DAYS))) {
                        astronomy = createAstronomy(interval)
                    }

                    forecast.add(createForecast(interval))
                }
            } else if (timeline.timestep == "current") {
                condition = createCondition(timeline.intervals[0])
                atmosphere = createAtmosphere(timeline.intervals[0])
                precipitation = createPrecipitation(timeline.intervals[0])
            }
        }

        if (minutelyRoot != null) {
            for (timeline in minutelyRoot.data.timelines) {
                if (timeline.timestep == "1m") {
                    minForecast = ArrayList(timeline.intervals.size)

                    for (interval in timeline.intervals) {
                        minForecast.add(createMinutelyForecast(interval))
                    }
                }
            }
        }

        if ((condition.highF == null || condition.highC == null) && forecast.isNotEmpty()) {
            condition.highF = forecast[0].highF
            condition.highC = forecast[0].highC
            condition.lowF = forecast[0].lowF
            condition.lowC = forecast[0].lowC
        }

        weatherAlerts = createWeatherAlerts(alertRoot)

        ttl = 180
        source = WeatherAPI.TOMORROWIO
    }
}

fun createLocation(root: Rootobject): com.thewizrd.shared_resources.weatherdata.model.Location {
    return com.thewizrd.shared_resources.weatherdata.model.Location().apply {
        // Use location name from location provider
        name = null
        latitude = null
        longitude = null
        tzLong = null
    }
}

fun createForecast(item: IntervalsItem): Forecast {
    return Forecast().apply {
        date = ZonedDateTime.parse(item.startTime).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()
        highF = ConversionMethods.CtoF(item.values.temperatureMax)
        highC = item.values.temperatureMax
        lowF = ConversionMethods.CtoF(item.values.temperatureMin)
        lowC = item.values.temperatureMin

        icon = item.values.weatherCode?.toString()

        // Extras
        extras = ForecastExtras()
        extras.feelslikeF = ConversionMethods.CtoF(item.values.temperatureApparent)
        extras.feelslikeC = item.values.temperatureApparent
        extras.humidity = item.values.humidity.roundToInt()
        extras.dewpointF = ConversionMethods.CtoF(item.values.dewPoint).roundToInt().toFloat()
        extras.dewpointC = item.values.dewPoint
        extras.pop = item.values.precipitationProbability
        extras.cloudiness = item.values.cloudCover.roundToInt()
        extras.qpfRainMm = item.values.precipitationIntensity
        extras.qpfRainIn = ConversionMethods.mmToIn(item.values.precipitationIntensity)
        extras.qpfSnowCm = item.values.snowAccumulation / 10f
        extras.qpfSnowIn = ConversionMethods.mmToIn(item.values.snowAccumulation)
        // 1hPA = 1mbar
        extras.pressureMb = item.values.pressureSeaLevel
        extras.pressureIn = ConversionMethods.mbToInHg(item.values.pressureSeaLevel).roundToInt().toFloat()
        extras.windDegrees = item.values.windDirection?.roundToInt()
        extras.windMph = ConversionMethods.msecToMph(item.values.windSpeed)
        extras.windKph = ConversionMethods.msecToKph(item.values.windSpeed)
        extras.visibilityMi = ConversionMethods.kmToMi(item.values.visibility)
        extras.visibilityKm = item.values.visibility
        extras.windGustMph = ConversionMethods.msecToMph(item.values.windGust)
        extras.windGustKph = ConversionMethods.msecToKph(item.values.windGust)
    }
}

fun createHourlyForecast(item: IntervalsItem): HourlyForecast {
    return HourlyForecast().apply {
        date = ZonedDateTime.parse(item.startTime).withZoneSameInstant(ZoneOffset.UTC)
        highF = ConversionMethods.CtoF(item.values.temperature)
        highC = item.values.temperature

        icon = item.values.weatherCode?.toString()

        // Extras
        extras = ForecastExtras()
        extras.feelslikeF = ConversionMethods.CtoF(item.values.temperatureApparent)
        extras.feelslikeC = item.values.temperatureApparent
        extras.humidity = item.values.humidity.roundToInt()
        extras.dewpointF = ConversionMethods.CtoF(item.values.dewPoint).roundToInt().toFloat()
        extras.dewpointC = item.values.dewPoint
        extras.pop = item.values.precipitationProbability
        extras.cloudiness = item.values.cloudCover.roundToInt()
        extras.qpfRainMm = item.values.precipitationIntensity
        extras.qpfRainIn = ConversionMethods.mmToIn(item.values.precipitationIntensity)
        extras.qpfSnowCm = item.values.snowAccumulation / 10f
        extras.qpfSnowIn = ConversionMethods.mmToIn(item.values.snowAccumulation)
        // 1hPA = 1mbar
        extras.pressureMb = item.values.pressureSeaLevel
        extras.pressureIn = ConversionMethods.mbToInHg(item.values.pressureSeaLevel).roundToInt().toFloat()
        extras.windDegrees = item.values.windDirection?.roundToInt()?.also { windDegrees = it }
        extras.windMph = ConversionMethods.msecToMph(item.values.windSpeed).also { windMph = it }
        extras.windKph = ConversionMethods.msecToKph(item.values.windSpeed).also { windKph = it }
        extras.visibilityMi = ConversionMethods.kmToMi(item.values.visibility)
        extras.visibilityKm = item.values.visibility
        extras.windGustMph = ConversionMethods.msecToMph(item.values.windGust)
        extras.windGustKph = ConversionMethods.msecToKph(item.values.windGust)
    }
}

fun createMinutelyForecast(item: IntervalsItem): MinutelyForecast {
    return MinutelyForecast().apply {
        date = ZonedDateTime.parse(item.startTime).withZoneSameInstant(ZoneOffset.UTC)
        rainMm = item.values.precipitationIntensity
    }
}

fun createCondition(item: IntervalsItem): Condition {
    return Condition().apply {
        weather = null

        tempF = ConversionMethods.CtoF(item.values.temperature)
        tempC = item.values.temperature

        windDegrees = item.values.windDirection?.roundToInt()
        windMph = ConversionMethods.msecToMph(item.values.windSpeed)
        windKph = ConversionMethods.msecToKph(item.values.windSpeed)

        windGustMph = ConversionMethods.msecToMph(item.values.windGust)
        windGustKph = ConversionMethods.msecToKph(item.values.windGust)

        feelslikeF = ConversionMethods.CtoF(item.values.temperatureApparent)
        feelslikeC = item.values.temperatureApparent

        icon = item.values.weatherCode.toString()

        beaufort = Beaufort(getBeaufortScale(item.values.windSpeed))

        /*
        highF = ConversionMethods.CtoF(item.values.temperatureMax)
        highC = item.values.temperatureMax
        lowF = ConversionMethods.CtoF(item.values.temperatureMin)
        lowC = item.values.temperatureMin
         */

        airQuality = AirQuality().apply {
            index = item.values.epaIndex
        }

        pollen = Pollen().apply {
            treePollenCount = when (item.values.treeIndex) {
                1, 2 -> Pollen.PollenCount.LOW
                3 -> Pollen.PollenCount.MODERATE
                4 -> Pollen.PollenCount.HIGH
                5 -> Pollen.PollenCount.VERY_HIGH
                else -> Pollen.PollenCount.UNKNOWN
            }

            grassPollenCount = when (item.values.grassIndex) {
                1, 2 -> Pollen.PollenCount.LOW
                3 -> Pollen.PollenCount.MODERATE
                4 -> Pollen.PollenCount.HIGH
                5 -> Pollen.PollenCount.VERY_HIGH
                else -> Pollen.PollenCount.UNKNOWN
            }

            ragweedPollenCount = when (item.values.weedIndex) {
                1, 2 -> Pollen.PollenCount.LOW
                3 -> Pollen.PollenCount.MODERATE
                4 -> Pollen.PollenCount.HIGH
                5 -> Pollen.PollenCount.VERY_HIGH
                else -> Pollen.PollenCount.UNKNOWN
            }
        }

        observationTime = ZonedDateTime.parse(item.startTime)
    }
}

fun createAtmosphere(item: IntervalsItem): Atmosphere {
    return Atmosphere().apply {
        humidity = item.values.humidity.roundToInt()
        pressureMb = item.values.pressureSeaLevel
        pressureIn = ConversionMethods.mbToInHg(item.values.pressureSeaLevel)
        pressureTrend = ""
        visibilityMi = ConversionMethods.kmToMi(item.values.visibility)
        visibilityKm = item.values.visibility
        dewpointF = ConversionMethods.CtoF(item.values.dewPoint)
        dewpointC = item.values.dewPoint
    }
}

fun createAstronomy(item: IntervalsItem): Astronomy {
    return Astronomy().apply {
        runCatching {
            sunrise = ZonedDateTime.parse(item.values.sunriseTime).toLocalDateTime()
        }
        runCatching {
            sunset = ZonedDateTime.parse(item.values.sunsetTime).toLocalDateTime()
        }

        moonPhase = when (item.values.moonPhase) {
            0 -> MoonPhase(MoonPhase.MoonPhaseType.NEWMOON)
            1 -> MoonPhase(MoonPhase.MoonPhaseType.WAXING_CRESCENT)
            2 -> MoonPhase(MoonPhase.MoonPhaseType.FIRST_QTR)
            3 -> MoonPhase(MoonPhase.MoonPhaseType.WAXING_GIBBOUS)
            4 -> MoonPhase(MoonPhase.MoonPhaseType.FULL_MOON)
            5 -> MoonPhase(MoonPhase.MoonPhaseType.WANING_GIBBOUS)
            6 -> MoonPhase(MoonPhase.MoonPhaseType.LAST_QTR)
            7 -> MoonPhase(MoonPhase.MoonPhaseType.WANING_CRESCENT)
            else -> MoonPhase(MoonPhase.MoonPhaseType.NEWMOON)
        }

        // If the sun won't set/rise, set time to the future
        if (sunrise == null) {
            sunrise = LocalDateTime.now().plusYears(1).minusNanos(1)
        }
        if (sunset == null) {
            sunset = LocalDateTime.now().plusYears(1).minusNanos(1)
        }
        if (moonrise == null) {
            moonrise = DateTimeUtils.getLocalDateTimeMIN()
        }
        if (moonset == null) {
            moonset = DateTimeUtils.getLocalDateTimeMIN()
        }
    }
}

fun createPrecipitation(item: IntervalsItem): Precipitation {
    return Precipitation().apply {
        pop = item.values.precipitationProbability
        cloudiness = item.values.cloudCover.roundToInt()
        qpfRainIn = ConversionMethods.mmToIn(item.values.precipitationIntensity)
        qpfRainMm = item.values.precipitationIntensity
    }
}