package com.thewizrd.shared_resources.weatherdata.nws

import androidx.core.util.ObjectsCompat
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.WeatherManager
import com.thewizrd.shared_resources.weatherdata.model.*
import com.thewizrd.shared_resources.weatherdata.nws.hourly.HourlyForecastResponse
import com.thewizrd.shared_resources.weatherdata.nws.hourly.PeriodItem
import com.thewizrd.shared_resources.weatherdata.nws.observation.ForecastResponse
import com.thewizrd.shared_resources.weatherdata.nws.observation.PeriodsItem
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

fun createWeatherData(forecastResponse: ForecastResponse,
                      hourlyForecastResponse: HourlyForecastResponse): Weather {
    return Weather().apply {
        location = createLocation(forecastResponse)
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        updateTime = now

        // ~8-day forecast
        forecast = ArrayList(8)
        txtForecast = ArrayList(16)

        run {
            val periodsSize = forecastResponse.time.startValidTime.size
            var i = 0
            while (i < periodsSize) {
                val forecastItem = PeriodsItem(
                        forecastResponse.time.startPeriodName[i],
                        forecastResponse.time.startValidTime[i],
                        forecastResponse.time.tempLabel[i],
                        forecastResponse.data.temperature[i],
                        forecastResponse.data.pop[i],
                        forecastResponse.data.weather[i],
                        forecastResponse.data.iconLink[i],
                        forecastResponse.data.text[i]
                )

                if (forecast.isEmpty() && !forecastItem.isDaytime || forecast.size == periodsSize - 1 && forecastItem.isDaytime) {
                    forecast.add(createForecast(forecastItem))
                    txtForecast.add(createTextForecast(forecastItem))
                } else if (forecastItem.isDaytime && i + 1 < periodsSize) {
                    val nightForecastItem = PeriodsItem(
                            forecastResponse.time.startPeriodName[i + 1],
                            forecastResponse.time.startValidTime[i + 1],
                            forecastResponse.time.tempLabel[i + 1],
                            forecastResponse.data.temperature[i + 1],
                            forecastResponse.data.pop[i + 1],
                            forecastResponse.data.weather[i + 1],
                            forecastResponse.data.iconLink[i + 1],
                            forecastResponse.data.text[i + 1]
                    )

                    forecast.add(createForecast(forecastItem, nightForecastItem))
                    txtForecast.add(createTextForecast(forecastItem, nightForecastItem))

                    i++
                }

                i++
            }
        }

        run {
            var adjustDate = false
            val creationDate = ZonedDateTime.parse(hourlyForecastResponse.creationDate, DateTimeFormatter.ISO_ZONED_DATE_TIME)
            hrForecast = ArrayList(144)
            for (period in hourlyForecastResponse.periodsItems) {
                val periodsSize = period.unixtime.size
                for (i in 0 until periodsSize) {
                    var instant = Instant.ofEpochSecond(period.unixtime[i].toLong())

                    // BUG: NWS MapClick API
                    // The epoch time sometimes is a day ahead
                    // If this is the case, adjust all dates accordingly
                    if (i == 0 && period.periodName?.contains("night") == true && "6 pm" == period.time[i]) {
                        val hrDate = instant.atZone(creationDate.zone)
                        if (creationDate.plusDays(1).truncatedTo(ChronoUnit.DAYS).isEqual(hrDate.truncatedTo(ChronoUnit.DAYS))) {
                            adjustDate = true
                        }
                    }

                    if (adjustDate) {
                        instant = instant.minus(1, ChronoUnit.DAYS)
                    }

                    if (instant.atZone(ZoneOffset.UTC).isBefore(now.truncatedTo(ChronoUnit.HOURS)))
                        continue

                    val forecastItem = PeriodItem(
                            period.unixtime[i],
                            period.windChill[i],
                            period.windSpeed[i],
                            period.cloudAmount[i],
                            period.pop[i],
                            period.relativeHumidity[i],
                            period.windGust[i],
                            period.temperature[i],
                            period.windDirection[i],
                            period.iconLink[i],
                            period.weather[i]
                    )

                    hrForecast.add(createHourlyForecast(forecastItem, adjustDate))
                }
            }
        }

        condition = createCondition(forecastResponse)
        atmosphere = createAtmosphere(forecastResponse)
        //astronomy = new Astronomy(obsCurrentResponse);
        precipitation = createPrecipitation(forecastResponse)
        ttl = 180

        if (condition.highF == null && forecast.size > 0) {
            condition.highF = forecast[0].highF
            condition.highC = forecast[0].highC
        }
        if (condition.lowF == null && forecast.size > 0) {
            condition.lowF = forecast[0].lowF
            condition.lowC = forecast[0].lowC
        }

        source = WeatherAPI.NWS
    }
}

fun createLocation(forecastResponse: ForecastResponse): Location {
    return Location().apply {
        // Use location name from location provider
        name = null
        latitude = forecastResponse.location.latitude.toFloatOrNull()
        longitude = forecastResponse.location.longitude.toFloatOrNull()
        tzLong = null
    }
}

fun createForecast(forecastItem: PeriodsItem): Forecast {
    return Forecast().apply {
        val provider = WeatherManager.getProvider(WeatherAPI.NWS)
        val locale = LocaleUtils.getLocale()

        date = ZonedDateTime.parse(forecastItem.startTime, DateTimeFormatter.ISO_ZONED_DATE_TIME).toLocalDateTime()

        val temp = NumberUtils.tryParseFloat(forecastItem.temperature)
        if (forecastItem.isDaytime) {
            highF = temp
            highC = ConversionMethods.FtoC(highF)
        } else {
            lowF = temp
            lowC = ConversionMethods.FtoC(lowF)
        }

        condition = if (locale.toString() == "en" || locale.toString().startsWith("en_") ||
                locale == Locale.ROOT) {
            forecastItem.shortForecast
        } else {
            provider.getWeatherCondition(forecastItem.icon)
        }
        icon = provider.getWeatherIcon(!forecastItem.isDaytime, forecastItem.icon)

        extras = ForecastExtras()
        extras.pop = NumberUtils.tryParseInt(forecastItem.pop, 0)
    }
}

fun createTextForecast(forecastItem: PeriodsItem): TextForecast {
    return TextForecast().apply {
        date = ZonedDateTime.parse(forecastItem.startTime, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        fcttext = String.format(Locale.ROOT,
                "%s - %s", forecastItem.name, forecastItem.detailedForecast)
        fcttextMetric = fcttext
    }
}

fun createForecast(forecastItem: PeriodsItem, nightForecastItem: PeriodsItem): Forecast {
    return Forecast().apply {
        val provider = WeatherManager.getProvider(WeatherAPI.NWS)
        val locale = LocaleUtils.getLocale()

        date = ZonedDateTime.parse(forecastItem.startTime, DateTimeFormatter.ISO_ZONED_DATE_TIME).toLocalDateTime()
        highF = forecastItem.temperature.toFloat()
        highC = ConversionMethods.FtoC(highF)
        lowF = nightForecastItem.temperature.toFloat()
        lowC = ConversionMethods.FtoC(lowF)

        condition = if (locale.toString() == "en" || locale.toString().startsWith("en_") || locale == Locale.ROOT) {
            forecastItem.shortForecast
        } else {
            provider.getWeatherCondition(forecastItem.icon)
        }
        icon = provider.getWeatherIcon(false, forecastItem.icon)

        extras = ForecastExtras()
        extras.pop = NumberUtils.tryParseInt(forecastItem.pop, 0)
    }
}

fun createTextForecast(forecastItem: PeriodsItem, ntForecastItem: PeriodsItem): TextForecast {
    return TextForecast().apply {
        date = ZonedDateTime.parse(forecastItem.startTime, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        fcttext = String.format(Locale.ROOT,
                "%s - %s\n\n%s - %s",
                forecastItem.name, forecastItem.detailedForecast,
                ntForecastItem.name, ntForecastItem.detailedForecast)
        fcttextMetric = fcttext
    }
}

fun createHourlyForecast(forecastItem: PeriodItem, adjustDate: Boolean = false): HourlyForecast {
    return HourlyForecast().apply {
        val provider = WeatherManager.getProvider(WeatherAPI.NWS)
        val locale = LocaleUtils.getLocale()

        var date = Instant.ofEpochSecond(forecastItem.unixTime.toLong()).atZone(ZoneOffset.UTC)
        if (adjustDate) date = date.minusDays(1)
        setDate(date)

        val temp = NumberUtils.tryParseFloat(forecastItem.temperature)
        if (temp != null) {
            highF = temp
            highC = ConversionMethods.FtoC(temp)
        }

        condition = if (locale.toString() == "en" || locale.toString().startsWith("en_") || locale == Locale.ROOT) {
            forecastItem.weather
        } else {
            provider.getWeatherCondition(forecastItem.iconLink)
        }
        icon = forecastItem.iconLink

        // Extras
        extras = ForecastExtras()

        val windSpeed = NumberUtils.tryParseFloat(forecastItem.windSpeed)
        val windDirection = NumberUtils.tryParseInt(forecastItem.windDirection)
        if (windSpeed != null && windDirection != null) {
            windDegrees = windDirection
            windMph = windSpeed
            windKph = ConversionMethods.mphTokph(windMph)

            extras.windDegrees = windDegrees
            extras.windMph = windMph
            extras.windKph = windKph
        }

        val windChill = NumberUtils.tryParseFloat(forecastItem.windChill)
        if (windChill != null) {
            extras.feelslikeF = windChill
            extras.feelslikeC = ConversionMethods.FtoC(windChill)
        }

        val cloudiness = NumberUtils.tryParseInt(forecastItem.cloudAmount)
        if (cloudiness != null) {
            extras.cloudiness = cloudiness
        }

        val pop = NumberUtils.tryParseInt(forecastItem.pop)
        if (pop != null) {
            extras.pop = pop
        }

        val humidity = NumberUtils.tryParseInt(forecastItem.relativeHumidity)
        if (humidity != null) {
            extras.humidity = humidity
        }

        val windGust = NumberUtils.tryParseFloat(forecastItem.windGust)
        if (windGust != null) {
            extras.windGustMph = windGust
            extras.windGustKph = ConversionMethods.mphTokph(windGust)
        }
    }
}

fun createCondition(forecastResponse: ForecastResponse): Condition {
    return Condition().apply {
        val provider = WeatherManager.getProvider(WeatherAPI.NWS)
        val locale = LocaleUtils.getLocale()

        weather = if (locale.toString() == "en" || locale.toString().startsWith("en_") || locale == Locale.ROOT) {
            forecastResponse.currentobservation.weather
        } else {
            provider.getWeatherCondition(forecastResponse.currentobservation.weatherimage)
        }
        icon = forecastResponse.currentobservation.weatherimage

        val temp = NumberUtils.tryParseFloat(forecastResponse.currentobservation.temp)
        if (temp != null) {
            tempF = temp
            tempC = ConversionMethods.FtoC(temp)
        }

        val windDir = NumberUtils.tryParseInt(forecastResponse.currentobservation.windd)
        if (windDir != null) {
            windDegrees = windDir
        }

        val windSpeed = NumberUtils.tryParseFloat(forecastResponse.currentobservation.winds)
        if (windSpeed != null) {
            windMph = windSpeed
            windKph = ConversionMethods.mphTokph(windSpeed)
        }

        val windGust = NumberUtils.tryParseFloat(forecastResponse.currentobservation.gust)
        if (windGust != null) {
            windGustMph = windGust
            windGustKph = ConversionMethods.mphTokph(windGust)
        }

        val windChill = NumberUtils.tryParseFloat(forecastResponse.currentobservation.windChill)
        if (windChill != null) {
            feelslikeF = windChill
            feelslikeC = ConversionMethods.FtoC(windChill)
        } else if (tempF != null && !ObjectsCompat.equals(tempF, tempC) && windMph != null) {
            val humidity = NumberUtils.tryParseFloat(forecastResponse.currentobservation.relh, -1f)
            if (humidity >= 0) {
                feelslikeF = getFeelsLikeTemp(tempF, windMph, Math.round(humidity))
                feelslikeC = ConversionMethods.FtoC(feelslikeF)
            }
        }

        if (windMph != null) {
            beaufort = Beaufort(getBeaufortScale(Math.round(windMph)))
        }

        observationTime = ZonedDateTime.parse(forecastResponse.creationDate, DateTimeFormatter.ISO_ZONED_DATE_TIME)
    }
}

fun createAtmosphere(forecastResponse: ForecastResponse): Atmosphere {
    return Atmosphere().apply {
        val relh = NumberUtils.tryParseInt(forecastResponse.currentobservation.relh)
        if (relh != null) {
            humidity = relh
        }

        val pressure = NumberUtils.tryParseFloat(forecastResponse.currentobservation.slp)
        if (pressure != null) {
            pressureIn = pressure
            pressureMb = ConversionMethods.inHgToMB(pressure)
        }
        pressureTrend = ""

        val visibility = NumberUtils.tryParseFloat(forecastResponse.currentobservation.visibility)
        if (visibility != null) {
            visibilityMi = visibility
            visibilityKm = ConversionMethods.miToKm(visibility)
        }

        val dewp = NumberUtils.tryParseFloat(forecastResponse.currentobservation.dewp)
        if (dewp != null) {
            dewpointF = dewp
            dewpointC = ConversionMethods.FtoC(dewp)
        }
    }
}

fun createPrecipitation(forecastResponse: ForecastResponse): Precipitation {
    return Precipitation().apply {
        // The rest DNE
    }
}