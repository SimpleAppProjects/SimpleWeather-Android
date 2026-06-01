package com.thewizrd.weather_api.eccc

import com.thewizrd.shared_resources.utils.ConversionMethods
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.utils.getBeaufortScale
import com.thewizrd.shared_resources.utils.getWindDirection
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.model.Astronomy
import com.thewizrd.shared_resources.weatherdata.model.Atmosphere
import com.thewizrd.shared_resources.weatherdata.model.Beaufort
import com.thewizrd.shared_resources.weatherdata.model.Condition
import com.thewizrd.shared_resources.weatherdata.model.Forecast
import com.thewizrd.shared_resources.weatherdata.model.ForecastExtras
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.shared_resources.weatherdata.model.Location
import com.thewizrd.shared_resources.weatherdata.model.TextForecast
import com.thewizrd.shared_resources.weatherdata.model.Weather
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.roundToInt

fun createWeatherData(root: LocationResponseItem): Weather {
    return Weather().apply {
        val now = root.lastUpdated?.let {
            ZonedDateTime.ofInstant(Instant.ofEpochSecond(it), ZoneOffset.UTC)
        } ?: ZonedDateTime.now()

        location = createLocation(root)
        updateTime = now

        var startDate = root.dailyFcst?.dailyIssuedTimeEpoch?.toLongOrNull()?.let {
            LocalDateTime.ofEpochSecond(it, 0, ZoneOffset.UTC)
        }

        if (startDate != null) {
            root.dailyFcst?.daily?.groupBy { it.date }?.let { entry ->
                val fcasts = ArrayList<Forecast>(entry.size)
                val txtFcasts = ArrayList<TextForecast>(entry.size)

                entry.forEach { kv ->
                    val values = kv.value.sortedBy { it.periodID }
                    val day = values.firstOrNull { it.temperature?.periodHigh != null }
                    val night = values.firstOrNull { it.temperature?.periodLow != null }

                    fcasts.add(createForecast(day, night).apply {
                        date = startDate
                    })
                    txtFcasts.add(createTextForecast(day, night).apply {
                        date = startDate!!.atZone(ZoneOffset.UTC)
                    })
                    startDate = startDate!!.plusDays(1)
                }

                forecast = fcasts
                txtForecast = txtFcasts
            }
        }

        hrForecast = root.hourlyFcst?.hourly?.map {
            createHourlyForecast(it)
        }

        root.observation?.let {
            condition = createCondition(it)
            atmosphere = createAtmosphere(it)
        }

        astronomy = createAstronomy(root.riseSet)
        weatherAlerts = createWeatherAlerts(root.alert)

        ttl = 120

        if ((condition?.highF == null || condition?.highC == null) && forecast!!.size > 0) {
            condition!!.highF = forecast!![0].highF
            condition!!.highC = forecast!![0].highC
            condition!!.lowF = forecast!![0].lowF
            condition!!.lowC = forecast!![0].lowC
        }

        source = WeatherAPI.ECCC
    }
}

fun createLocation(root: LocationResponseItem): Location {
    return Location().apply {
        name = if (root.displayName != null && root.observation?.provinceCode != null) {
            "${root.displayName}, ${root.observation!!.provinceCode}"
        } else {
            root.displayName
        }
    }
}

fun createCondition(observation: Observation): Condition {
    return Condition().apply {
        weather = observation.condition

        tempF = (observation.temperature?.imperialUnrounded
            ?: observation.temperature?.imperial)?.toFloatOrNull()
        tempC = (observation.temperature?.metricUnrounded
            ?: observation.temperature?.metric)?.toFloatOrNull()

        windDegrees = observation.windBearing?.toFloatOrNull()?.roundToInt()

        windMph = observation.windSpeed?.imperial?.toFloatOrNull()
        windKph = observation.windSpeed?.metric?.toFloatOrNull()

        windGustMph = observation.windGust?.imperial?.toFloatOrNull()
        windGustKph = observation.windGust?.metric?.toFloatOrNull()

        feelslikeF = observation.feelsLike?.imperial?.toFloatOrNull()
        feelslikeC = observation.feelsLike?.metric?.toFloatOrNull()

        icon = observation.iconCode

        windMph?.let {
            beaufort = Beaufort(getBeaufortScale(it.roundToInt()))
        }

        observationTime = observation.timeStamp?.let {
            Instant.parse(it).atZone(ZoneOffset.UTC)
        }
    }
}

fun createAtmosphere(observation: Observation): Atmosphere {
    return Atmosphere().apply {
        humidity = observation.humidity?.toIntOrNull()

        pressureMb = observation.pressure?.metric?.toFloatOrNull()?.let {
            ConversionMethods.paToMB(it * 1000f)
        }
        pressureIn = observation.pressure?.imperial?.toFloatOrNull()
        pressureTrend = observation.tendency?.let {
            when (it) {
                "falling" -> "-"
                "rising" -> "+"
                else -> ""
            }
        }

        visibilityMi = observation.visibility?.imperial?.toFloatOrNull()
        visibilityKm = observation.visibility?.metric?.toFloatOrNull()

        dewpointF = (observation.dewpoint?.imperialUnrounded
            ?: observation.dewpoint?.imperial)?.toFloatOrNull()
        dewpointC =
            (observation.dewpoint?.metricUnrounded ?: observation.dewpoint?.metric)?.toFloatOrNull()
    }
}

fun createAstronomy(riseSet: RiseSet?): Astronomy {
    return Astronomy().apply {
        riseSet?.rise?.let {
            sunrise = it.epochTimeRounded?.toLongOrNull()?.let { riseEpoch ->
                Instant.ofEpochSecond(riseEpoch).atZone(ZoneOffset.UTC).toLocalDateTime()
            }
        }
        riseSet?.set?.let {
            sunset = it.epochTimeRounded?.toLongOrNull()?.let { setEpoch ->
                Instant.ofEpochSecond(setEpoch).atZone(ZoneOffset.UTC).toLocalDateTime()
            }
        }

        if (sunrise == null) {
            sunrise = DateTimeUtils.LOCALDATETIME_MIN
        }
        if (sunset == null) {
            sunset = DateTimeUtils.LOCALDATETIME_MIN
        }
        if (moonrise == null) {
            moonrise = DateTimeUtils.LOCALDATETIME_MIN
        }
        if (moonset == null) {
            moonset = DateTimeUtils.LOCALDATETIME_MIN
        }
    }
}

fun createHourlyForecast(item: HourlyItem): HourlyForecast {
    return HourlyForecast().apply {
        date = item.epochTime?.let {
            Instant.ofEpochSecond(it).atZone(ZoneOffset.UTC)
        }

        highF = item.temperature?.imperial?.toFloatOrNull()
        highC = item.temperature?.metric?.toFloatOrNull()

        icon = item.iconCode
        condition = item.condition

        // Extras
        extras = ForecastExtras()
        extras.feelslikeF = item.feelsLike?.imperial?.toFloatOrNull()
        extras.feelslikeC = item.feelsLike?.metric?.toFloatOrNull()
        extras.pop = item.precip?.toIntOrNull()
        extras.windDegrees = item.windDir?.let {
            getWindDirection(it)
        }
        extras.windMph = item.windSpeed?.imperial?.toFloatOrNull()
        extras.windKph = item.windSpeed?.metric?.toFloatOrNull()
        extras.windGustMph = item.windGust?.imperial?.toFloatOrNull()
        extras.windGustKph = item.windGust?.metric?.toFloatOrNull()
        extras.uvIndex = item.uv?.index?.toFloatOrNull()
    }
}

fun createForecast(dayItem: DailyItem?, nightItem: DailyItem?): Forecast {
    return Forecast().apply {
        // date is not in standard format to parse; set it manually
        val highFStr = dayItem?.temperature?.imperial
        highF = highFStr?.toFloatOrNull()
        val highCStr = dayItem?.temperature?.metric
        highC = highCStr?.toFloatOrNull()

        val lowFStr = nightItem?.temperature?.imperial
        lowF = lowFStr?.toFloatOrNull()
        val lowCStr = nightItem?.temperature?.metric
        lowC = lowCStr?.toFloatOrNull()

        condition = dayItem?.text?.let { text ->
            val tempText = dayItem.temperatureText?.takeIf {
                highCStr != null && highFStr != null && it.isNotBlank()
            }?.replace(highCStr!!, "${highCStr}°C / ${highFStr}°F") // High 10°C / 50°F

            if (tempText != null) {
                text.replace(dayItem.temperatureText!!, tempText)
            } else {
                text
            }
        } ?: nightItem?.text?.let { text ->
            val tempText = nightItem.temperatureText?.takeIf {
                lowCStr != null && lowFStr != null && it.isNotBlank()
            }?.replace(lowCStr!!, "${lowCStr}°C / ${lowFStr}°F") // Low 10°C / 50°F

            if (tempText != null) {
                text.replace(nightItem.temperatureText!!, tempText)
            } else {
                text
            }
        }
        icon = dayItem?.iconCode ?: nightItem?.iconCode

        extras = ForecastExtras()
        extras.pop = dayItem?.precip?.toIntOrNull()

        // Humidex / Feels like
        dayItem?.text?.indexOf("Humidex ")?.takeIf { it >= 0 }?.let { humidexStart ->
            val substr = dayItem.text?.substring(humidexStart)
            if (substr != null) {
                val endStr = dayItem.text!!.substring(humidexStart)
                val endIndex = endStr.indexOf('.')
                if (endIndex >= 0) {
                    val humidexStr = dayItem.text?.substring(humidexStart, endIndex + humidexStart)
                    val humidexMetric = humidexStr?.removePrefix("Humidex ")?.toFloatOrNull()

                    if (humidexMetric != null) {
                        extras.feelslikeC = humidexMetric
                        extras.feelslikeF = ConversionMethods.CtoF(humidexMetric)
                    }
                }
            }
        }
    }
}

fun createTextForecast(dayItem: DailyItem?, nightItem: DailyItem?): TextForecast {
    return TextForecast().apply {
        // date is not in standard format to parse; set it manually
        val highF = dayItem?.temperature?.imperial
        val highC = dayItem?.temperature?.metric

        val lowF = nightItem?.temperature?.imperial
        val lowC = nightItem?.temperature?.metric

        fcttextMetric = buildString {
            if (dayItem != null) {
                append(dayItem.periodLabel)
                append(" : ")
                append(dayItem.text)
                appendLine()
            }

            if (nightItem != null) {
                append(nightItem.periodLabel)
                append(" : ")
                append(nightItem.text)
            }
        }

        fcttext = buildString {
            if (dayItem != null) {
                val tempText = dayItem.temperatureText?.takeIf {
                    highC != null && highF != null && it.isNotBlank()
                }?.replace(highC!!, "${highF}°F")

                append(dayItem.periodLabel)
                append(" : ")
                if (dayItem.text != null && tempText != null) {
                    append(dayItem.text!!.replace(dayItem.temperatureText!!, tempText))
                } else {
                    append(dayItem.text)
                }
                appendLine()
            }

            if (nightItem != null) {
                val tempText = nightItem.temperatureText?.takeIf {
                    lowC != null && lowF != null && it.isNotBlank()
                }?.replace(lowC!!, "${lowF}°F")

                append(nightItem.periodLabel)
                append(" : ")
                if (nightItem.text != null && tempText != null) {
                    append(nightItem.text!!.replace(nightItem.temperatureText!!, tempText))
                } else {
                    append(nightItem.text)
                }
            }
        }
    }
}