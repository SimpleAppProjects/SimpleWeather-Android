package com.thewizrd.weather_api.tomorrow

import android.annotation.SuppressLint
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.AirQualityUtils.AQICO
import com.thewizrd.shared_resources.utils.AirQualityUtils.AQINO2
import com.thewizrd.shared_resources.utils.AirQualityUtils.AQIO3
import com.thewizrd.shared_resources.utils.AirQualityUtils.AQIPM10
import com.thewizrd.shared_resources.utils.AirQualityUtils.AQIPM2_5
import com.thewizrd.shared_resources.utils.AirQualityUtils.AQISO2
import com.thewizrd.shared_resources.utils.AirQualityUtils.getIndexFromData
import com.thewizrd.shared_resources.utils.ConversionMethods
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.utils.getBeaufortScale
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.model.*
import com.thewizrd.weather_api.R
import com.thewizrd.weather_api.weatherModule
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
            when (timeline.timestep) {
                "1h" -> {
                    hrForecast = ArrayList(timeline.intervals.size)

                    for (interval in timeline.intervals) {
                        hrForecast.add(createHourlyForecast(interval))
                    }
                }
                "1d" -> {
                    forecast = ArrayList(timeline.intervals.size)
                    txtForecast = ArrayList(timeline.intervals.size)
                    aqiForecast = ArrayList(timeline.intervals.size)

                    for (interval in timeline.intervals) {
                        if (astronomy == null && updateTime.truncatedTo(ChronoUnit.DAYS).isEqual(
                                ZonedDateTime.parse(interval.startTime).truncatedTo(ChronoUnit.DAYS)
                            )
                        ) {
                            astronomy = createAstronomy(interval)
                        }

                        forecast.add(createForecast(interval))
                        txtForecast.add(createTextForecast(interval))

                        if (interval.values.epaIndex != null) {
                            aqiForecast.add(createAQIForecast(interval))
                        }
                    }
                }
                "current" -> {
                    condition = createCondition(timeline.intervals[0])
                    atmosphere = createAtmosphere(timeline.intervals[0])
                    precipitation = createPrecipitation(timeline.intervals[0])
                }
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

        if ((condition.highF == null || condition.highC == null || condition.highF == condition.lowF) && forecast.isNotEmpty()) {
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
        date = ZonedDateTime.parse(item.startTime).withZoneSameInstant(ZoneOffset.UTC)
            .toLocalDateTime()
        item.values.temperatureMax?.let {
            highF = ConversionMethods.CtoF(it)
            highC = it
        }
        item.values.temperatureMin?.let {
            lowF = ConversionMethods.CtoF(it)
            lowC = it
        }

        icon = (item.values.weatherCodeFullDay ?: item.values.weatherCode)?.toString()

        // Extras
        extras = ForecastExtras()

        item.values.temperatureApparent?.let {
            extras.feelslikeC = it
            extras.feelslikeF = ConversionMethods.CtoF(it)
        }

        extras.humidity = item.values.humidity?.roundToInt()

        item.values.dewPoint?.let {
            extras.dewpointC = it
            extras.dewpointF = ConversionMethods.CtoF(it).roundToInt().toFloat()
        }

        extras.pop = item.values.precipitationProbability?.roundToInt()
        extras.cloudiness = item.values.cloudCover?.roundToInt()

        item.values.precipitationIntensity?.let {
            extras.qpfRainMm = it
            extras.qpfRainIn = ConversionMethods.mmToIn(it)
        }

        item.values.snowAccumulation?.let {
            extras.qpfSnowCm = it / 10f
            extras.qpfSnowIn = ConversionMethods.mmToIn(it)
        }

        // 1hPA = 1mbar
        item.values.pressureSeaLevel?.let {
            extras.pressureMb = it
            extras.pressureIn = ConversionMethods.mbToInHg(it).roundToInt().toFloat()
        }

        extras.windDegrees = item.values.windDirection?.roundToInt()

        item.values.windSpeed?.let {
            extras.windMph = ConversionMethods.msecToMph(it)
            extras.windKph = ConversionMethods.msecToKph(it)
        }

        item.values.windGust?.let {
            extras.windGustMph = ConversionMethods.msecToMph(it)
            extras.windGustKph = ConversionMethods.msecToKph(it)
        }

        item.values.visibility?.let {
            extras.visibilityKm = it
            extras.visibilityMi = ConversionMethods.kmToMi(it)
        }
    }
}

fun createTextForecast(item: IntervalsItem): TextForecast {
    return TextForecast().apply {
        date = ZonedDateTime.parse(item.startTime).withZoneSameInstant(ZoneOffset.UTC)

        val fcastStr = StringBuilder().apply {
            val ctx = sharedDeps.context
            val provider =
                weatherModule.weatherProviderFactory.getWeatherProvider(WeatherAPI.TOMORROWIO)

            append(
                String.format(
                    "%s - %s",
                    ctx.getString(R.string.label_day),
                    provider.getWeatherCondition(item.values.weatherCodeDay?.toString())
                )
            )
            appendLine()
            append(
                String.format(
                    "%s - %s",
                    ctx.getString(R.string.label_night),
                    provider.getWeatherCondition(item.values.weatherCodeNight?.toString())
                )
            )
        }.toString()

        fcttext = fcastStr
        fcttextMetric = fcastStr
    }
}

fun createHourlyForecast(item: IntervalsItem): HourlyForecast {
    return HourlyForecast().apply {
        date = ZonedDateTime.parse(item.startTime).withZoneSameInstant(ZoneOffset.UTC)

        item.values.temperature?.let {
            highC = it
            highF = ConversionMethods.CtoF(it)
        }

        icon = item.values.weatherCode?.toString()

        // Extras
        extras = ForecastExtras()

        item.values.temperatureApparent?.let {
            extras.feelslikeC = it
            extras.feelslikeF = ConversionMethods.CtoF(it)
        }

        extras.humidity = item.values.humidity?.roundToInt()

        item.values.dewPoint?.let {
            extras.dewpointC = it
            extras.dewpointF = ConversionMethods.CtoF(it).roundToInt().toFloat()
        }

        extras.pop = item.values.precipitationProbability?.roundToInt()
        extras.cloudiness = item.values.cloudCover?.roundToInt()

        item.values.precipitationIntensity?.let {
            extras.qpfRainMm = it
            extras.qpfRainIn = ConversionMethods.mmToIn(it)
        }

        item.values.snowAccumulation?.let {
            extras.qpfSnowCm = it / 10f
            extras.qpfSnowIn = ConversionMethods.mmToIn(it)
        }

        // 1hPA = 1mbar
        item.values.pressureSeaLevel?.let {
            extras.pressureMb = it
            extras.pressureIn = ConversionMethods.mbToInHg(it).roundToInt().toFloat()
        }

        extras.windDegrees = item.values.windDirection?.roundToInt()?.also { windDegrees = it }

        item.values.windSpeed?.let { speed ->
            extras.windMph = ConversionMethods.msecToMph(speed).also { windMph = it }
            extras.windKph = ConversionMethods.msecToKph(speed).also { windKph = it }
        }

        item.values.windGust?.let {
            extras.windGustMph = ConversionMethods.msecToMph(it)
            extras.windGustKph = ConversionMethods.msecToKph(it)
        }

        item.values.visibility?.let {
            extras.visibilityKm = it
            extras.visibilityMi = ConversionMethods.kmToMi(it)
        }
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

        item.values.temperature?.let {
            tempC = it
            tempF = ConversionMethods.CtoF(it)
        }

        windDegrees = item.values.windDirection?.roundToInt()

        item.values.windSpeed?.let {
            windMph = ConversionMethods.msecToMph(it)
            windKph = ConversionMethods.msecToKph(it)
            beaufort = Beaufort(getBeaufortScale(it))
        }

        item.values.windGust?.let {
            windGustMph = ConversionMethods.msecToMph(it)
            windGustKph = ConversionMethods.msecToKph(it)
        }

        item.values.temperatureApparent?.let {
            feelslikeC = it
            feelslikeF = ConversionMethods.CtoF(it)
        }

        icon = item.values.weatherCode?.toString()

        item.values.temperatureMax?.let {
            highC = it
            highF = ConversionMethods.CtoF(it)
        }

        item.values.temperatureMin?.let {
            lowC = it
            lowF = ConversionMethods.CtoF(it)
        }

        airQuality = AirQuality().apply {
            index = item.values.epaIndex
            pm25 = item.values.particulateMatter25?.let { runCatching { AQIPM2_5(it) }.getOrNull() }
            pm10 = item.values.particulateMatter10?.let { runCatching { AQIPM10(it) }.getOrNull() }
            o3 = item.values.pollutantO3?.let { runCatching { AQIO3(it) }.getOrNull() }
            no2 = item.values.pollutantNO2?.let { runCatching { AQINO2(it) }.getOrNull() }
            co = item.values.pollutantCO?.let { runCatching { AQICO(it) }.getOrNull() }
            so2 = item.values.pollutantSO2?.let { runCatching { AQISO2(it) }.getOrNull() }
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
        humidity = item.values.humidity?.roundToInt()

        item.values.pressureSeaLevel?.let {
            pressureMb = it
            pressureIn = ConversionMethods.mbToInHg(it)
        }
        pressureTrend = ""

        item.values.visibility?.let {
            visibilityKm = it
            visibilityMi = ConversionMethods.kmToMi(it)
        }

        item.values.dewPoint?.let {
            dewpointC = it
            dewpointF = ConversionMethods.CtoF(it).roundToInt().toFloat()
        }
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
        pop = item.values.precipitationProbability?.roundToInt()
        cloudiness = item.values.cloudCover?.roundToInt()

        item.values.precipitationIntensity?.let {
            qpfRainMm = it
            qpfRainIn = ConversionMethods.mmToIn(it)
        }
    }
}

fun createAQIForecast(item: IntervalsItem): AirQuality {
    return AirQuality().apply {
        date = ZonedDateTime.parse(item.startTime).withZoneSameInstant(ZoneOffset.UTC).toLocalDate()
        pm25 = item.values.particulateMatter25?.let { runCatching { AQIPM2_5(it) }.getOrNull() }
        pm10 = item.values.particulateMatter10?.let { runCatching { AQIPM10(it) }.getOrNull() }
        o3 = item.values.pollutantO3?.let { runCatching { AQIO3(it) }.getOrNull() }
        no2 = item.values.pollutantNO2?.let { runCatching { AQINO2(it) }.getOrNull() }
        co = item.values.pollutantCO?.let { runCatching { AQICO(it) }.getOrNull() }
        so2 = item.values.pollutantSO2?.let { runCatching { AQISO2(it) }.getOrNull() }
        index = item.values.epaIndex ?: getIndexFromData()
    }
}