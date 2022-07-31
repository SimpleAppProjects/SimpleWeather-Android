package com.thewizrd.weather_api.nws

import android.annotation.SuppressLint
import androidx.core.util.ObjectsCompat
import com.thewizrd.shared_resources.utils.ConversionMethods
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.utils.NumberUtils.tryParseFloat
import com.thewizrd.shared_resources.utils.getBeaufortScale
import com.thewizrd.shared_resources.utils.getFeelsLikeTemp
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.model.*
import com.thewizrd.weather_api.nws.hourly.HourlyForecastResponse
import com.thewizrd.weather_api.nws.hourly.PeriodItem
import com.thewizrd.weather_api.nws.observation.ForecastResponse
import com.thewizrd.weather_api.nws.observation.PeriodsItem
import com.thewizrd.weather_api.weatherModule
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.math.roundToInt

@SuppressLint("VisibleForTests")
fun createWeatherData(
    forecastResponse: ForecastResponse,
    hourlyForecastResponse: HourlyForecastResponse
): Weather {
    return Weather().apply {
        location = createLocation(forecastResponse)
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        updateTime = now

        condition = createCondition(forecastResponse)

        // ~8-day forecast
        forecast = ArrayList(8)
        txtForecast = ArrayList(16)

        run {
            val periodsSize = forecastResponse.time.startValidTime.size
            var i = 0
            while (i < periodsSize) {
                val forecastItem =
                    PeriodsItem(
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
                    txtForecast.add(createTextForecast(forecastItem).also {
                        if (condition.summary == null && !condition.observationTime.toLocalDate()
                                .isBefore(it.date.toLocalDate())
                        ) {
                            condition.summary = String.format(
                                Locale.ROOT,
                                "%s - %s", forecastItem.name, forecastItem.detailedForecast
                            )
                        }
                    })
                } else if (forecastItem.isDaytime && i + 1 < periodsSize) {
                    val nightForecastItem =
                        PeriodsItem(
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
                    txtForecast.add(createTextForecast(forecastItem, nightForecastItem).also {
                        if (condition.summary == null && !condition.observationTime.toLocalDate()
                                .isBefore(it.date.toLocalDate())
                        ) {
                            condition.summary = String.format(
                                Locale.ROOT,
                                "%s - %s\n%s - %s",
                                forecastItem.name, forecastItem.detailedForecast,
                                nightForecastItem.name, nightForecastItem.detailedForecast
                            )
                        }
                    })

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
        val provider = weatherModule.weatherManager.getWeatherProvider(WeatherAPI.NWS)
        val locale = LocaleUtils.getLocale()

        date = ZonedDateTime.parse(forecastItem.startTime, DateTimeFormatter.ISO_ZONED_DATE_TIME)
            .toLocalDateTime()

        forecastItem.temperature?.toFloatOrNull()?.let {
            if (forecastItem.isDaytime) {
                highF = it
                highC = ConversionMethods.FtoC(it)
            } else {
                lowF = it
                lowC = ConversionMethods.FtoC(it)
            }
        }

        condition = if (locale.toString() == "en" || locale.toString().startsWith("en_") ||
            locale == Locale.ROOT
        ) {
            forecastItem.shortForecast
        } else {
            provider.getWeatherCondition(forecastItem.icon)
        }
        icon = provider.getWeatherIcon(!forecastItem.isDaytime, forecastItem.icon)

        extras = ForecastExtras()
        extras.pop = forecastItem.pop?.toIntOrNull()
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
        val provider = weatherModule.weatherManager.getWeatherProvider(WeatherAPI.NWS)
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
        extras.pop = forecastItem.pop?.toIntOrNull()
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
        val provider = weatherModule.weatherManager.getWeatherProvider(WeatherAPI.NWS)
        val locale = LocaleUtils.getLocale()

        var date = Instant.ofEpochSecond(forecastItem.unixTime.toLong()).atZone(ZoneOffset.UTC)
        if (adjustDate) date = date.minusDays(1)
        setDate(date)

        forecastItem.temperature?.toFloatOrNull()?.let {
            highF = it
            highC = ConversionMethods.FtoC(it)
        }

        condition = if (locale.toString() == "en" || locale.toString()
                .startsWith("en_") || locale == Locale.ROOT
        ) {
            forecastItem.weather
        } else {
            provider.getWeatherCondition(forecastItem.iconLink)
        }
        icon = forecastItem.iconLink

        // Extras
        extras = ForecastExtras()

        val windSpeed = forecastItem.windSpeed?.toFloatOrNull()
        val windDirection = forecastItem.windDirection?.toIntOrNull()
        if (windSpeed != null && windDirection != null) {
            windDegrees = windDirection
            windMph = windSpeed
            windKph = ConversionMethods.mphTokph(windMph)

            extras.windDegrees = windDegrees
            extras.windMph = windMph
            extras.windKph = windKph
        }

        forecastItem.windChill?.toFloatOrNull()?.let {
            extras.feelslikeF = it
            extras.feelslikeC = ConversionMethods.FtoC(it)
        }

        extras.cloudiness = forecastItem.cloudAmount?.toIntOrNull()
        extras.pop = forecastItem.pop?.toIntOrNull()
        extras.humidity = forecastItem.relativeHumidity?.toIntOrNull()

        forecastItem.windGust?.toFloatOrNull()?.let {
            extras.windGustMph = it
            extras.windGustKph = ConversionMethods.mphTokph(it)
        }
    }
}

fun createCondition(forecastResponse: ForecastResponse): Condition {
    return Condition().apply {
        val provider = weatherModule.weatherManager.getWeatherProvider(WeatherAPI.NWS)
        val locale = LocaleUtils.getLocale()

        weather = if (locale.toString() == "en" || locale.toString()
                .startsWith("en_") || locale == Locale.ROOT
        ) {
            forecastResponse.currentobservation.weather
        } else {
            provider.getWeatherCondition(forecastResponse.currentobservation.weatherimage)
        }
        icon = forecastResponse.currentobservation.weatherimage

        forecastResponse.currentobservation.temp?.toFloatOrNull()?.let {
            tempF = it
            tempC = ConversionMethods.FtoC(it)
        }

        windDegrees = forecastResponse.currentobservation.windd?.toIntOrNull()

        forecastResponse.currentobservation.winds?.toFloatOrNull()?.let {
            windMph = it
            windKph = ConversionMethods.mphTokph(it)
        }

        forecastResponse.currentobservation.gust?.toFloatOrNull()?.let {
            windGustMph = it
            windGustKph = ConversionMethods.mphTokph(it)
        }

        val windChill = forecastResponse.currentobservation.windChill?.toFloatOrNull()
        if (windChill != null) {
            feelslikeF = windChill
            feelslikeC = ConversionMethods.FtoC(windChill)
        } else if (tempF != null && !ObjectsCompat.equals(tempF, tempC) && windMph != null) {
            val humidity = forecastResponse.currentobservation.relh.tryParseFloat(-1f)
            if (humidity >= 0) {
                feelslikeF = getFeelsLikeTemp(tempF, windMph, humidity.roundToInt())
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
        humidity = forecastResponse.currentobservation.relh?.toIntOrNull()

        forecastResponse.currentobservation.slp?.toFloatOrNull()?.let {
            pressureIn = it
            pressureMb = ConversionMethods.inHgToMB(it)
        }
        pressureTrend = ""

        forecastResponse.currentobservation.visibility?.toFloatOrNull()?.let {
            visibilityMi = it
            visibilityKm = ConversionMethods.miToKm(it)
        }

        forecastResponse.currentobservation.dewp?.toFloatOrNull()?.let {
            dewpointF = it
            dewpointC = ConversionMethods.FtoC(it)
        }
    }
}

fun createPrecipitation(forecastResponse: ForecastResponse): Precipitation {
    return Precipitation().apply {
        // The rest DNE
    }
}