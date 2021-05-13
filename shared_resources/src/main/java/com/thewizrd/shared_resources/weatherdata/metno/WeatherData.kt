package com.thewizrd.shared_resources.weatherdata.metno

import com.thewizrd.shared_resources.utils.ConversionMethods
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.utils.getBeaufortScale
import com.thewizrd.shared_resources.utils.getFeelsLikeTemp
import com.thewizrd.shared_resources.weatherdata.*
import com.thewizrd.shared_resources.weatherdata.Location
import com.thewizrd.shared_resources.weatherdata.MoonPhase.MoonPhaseType
import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.math.roundToInt

fun createWeatherData(foreRoot: Response, astroRoot: AstroResponse): Weather {
    return Weather().apply {
        val now = ZonedDateTime.now(ZoneOffset.UTC)

        location = createLocation(foreRoot)
        updateTime = now

        // 9-day forecast / hrly -> 6hrly forecast
        forecast = ArrayList(10)
        hrForecast = ArrayList(foreRoot.properties.timeseries.size)

        // Store potential min/max values
        var dayMax = Float.NaN
        var dayMin = Float.NaN

        var currentDate = LocalDateTime.MIN
        var fcast: Forecast? = null

        // Metno data is troublesome to parse thru
        for (i in foreRoot.properties.timeseries.indices) {
            val time = foreRoot.properties.timeseries[i]
            val date = LocalDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(time.time)), ZoneOffset.UTC)

            // Create condition for next 2hrs from data
            if (i == 0) {
                condition = createCondition(time)
                atmosphere = createAtmosphere(time)
                precipitation = createPrecipitation(time)
            }

            // Add a new hour
            if (!date.truncatedTo(ChronoUnit.HOURS).isBefore(now.toLocalDateTime().truncatedTo(ChronoUnit.HOURS)))
                hrForecast.add(createHourlyForecast(time))

            // Create new forecast
            if (!currentDate.toLocalDate().isEqual(date.toLocalDate()) &&
                    !date.isBefore(currentDate.plusDays(1))) {
                // Last forecast for day; create forecast
                if (fcast != null) {
                    // condition (set in provider GetWeather method)
                    // date
                    fcast.date = currentDate
                    // high
                    fcast.highF = ConversionMethods.CtoF(dayMax)
                    fcast.highC = dayMax.roundToInt().toFloat()
                    // low
                    fcast.lowF = ConversionMethods.CtoF(dayMin)
                    fcast.lowC = dayMin.roundToInt().toFloat()

                    forecast.add(fcast)
                }

                currentDate = date
                fcast = createForecast(time)
                fcast.date = date

                // Reset
                dayMax = Float.NaN
                dayMin = Float.NaN
            }

            // Find max/min for each hour
            val temp = if (time.data.instant.details.airTemperature != null) {
                time.data.instant.details.airTemperature
            } else {
                Float.NaN
            }

            if (!temp.isNaN() && (dayMax.isNaN() || temp > dayMax)) {
                dayMax = temp
            }
            if (!temp.isNaN() && (dayMin.isNaN() || temp < dayMin)) {
                dayMin = temp
            }
        }

        fcast = forecast.lastOrNull()
        if (fcast != null && fcast.condition == null && fcast.icon == null) {
            forecast.removeAt(forecast.size - 1)
        }

        val hrfcast = hrForecast.lastOrNull()
        if (hrfcast != null && hrfcast.condition == null && hrfcast.icon == null) {
            hrForecast.removeAt(hrForecast.size - 1)
        }

        astronomy = createAstronomy(astroRoot)
        ttl = 120

        val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
        df.applyPattern("#.####")
        query = String.format(Locale.ROOT, "lat=%s&lon=%s", df.format(location.latitude), location.longitude)

        if ((condition.highF == null || condition.highC == null) && forecast.size > 0) {
            condition.highF = forecast[0].highF
            condition.highC = forecast[0].highC
            condition.lowF = forecast[0].lowF
            condition.lowC = forecast[0].lowC
        }

        condition.observationTime = ZonedDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(foreRoot.properties.meta.updatedAt)), ZoneOffset.UTC)

        source = WeatherAPI.METNO
    }
}

fun createLocation(foreRoot: Response): Location {
    return Location().apply {
        // API doesn't provide location name (at all)
        name = null
        latitude = foreRoot.geometry.coordinates[1]
        longitude = foreRoot.geometry.coordinates[0]
        tzLong = null
    }
}

fun createCondition(time: TimeseriesItem): Condition {
    return Condition().apply {
        // weather
        tempF = ConversionMethods.CtoF(time.data.instant.details.airTemperature)
        tempC = time.data.instant.details.airTemperature
        windDegrees = Math.round(time.data.instant.details.windFromDirection)
        windMph = Math.round(ConversionMethods.msecToMph(time.data.instant.details.windSpeed)).toFloat()
        windKph = Math.round(ConversionMethods.msecToKph(time.data.instant.details.windSpeed)).toFloat()
        feelslikeF = getFeelsLikeTemp(tempF, windMph, Math.round(time.data.instant.details.relativeHumidity))
        feelslikeC = ConversionMethods.FtoC(feelslikeF)
        if (time.data.instant.details.windSpeedOfGust != null) {
            windGustMph = Math.round(ConversionMethods.msecToMph(time.data.instant.details.windSpeedOfGust)).toFloat()
            windGustKph = Math.round(ConversionMethods.msecToKph(time.data.instant.details.windSpeedOfGust)).toFloat()
        }

        if (time.data.next12Hours != null) {
            icon = time.data.next12Hours.summary.symbolCode
        } else if (time.data.next6Hours != null) {
            icon = time.data.next6Hours.summary.symbolCode
        } else if (time.data.next1Hours != null) {
            icon = time.data.next1Hours.summary.symbolCode
        }

        beaufort = Beaufort(getBeaufortScale(time.data.instant.details.windSpeed).value)
        if (time.data.instant.details.ultravioletIndexClearSky != null) {
            uv = UV(time.data.instant.details.ultravioletIndexClearSky)
        }
    }
}

fun createAtmosphere(time: TimeseriesItem): Atmosphere {
    return Atmosphere().apply {
        humidity = Math.round(time.data.instant.details.relativeHumidity)
        pressureMb = time.data.instant.details.airPressureAtSeaLevel
        pressureIn = ConversionMethods.mbToInHg(time.data.instant.details.airPressureAtSeaLevel)
        pressureTrend = ""

        if (time.data.instant.details.fogAreaFraction != null) {
            val visMi = 10.0f
            visibilityMi = visMi - (visMi * time.data.instant.details.fogAreaFraction / 100)
            visibilityKm = ConversionMethods.miToKm(visibilityMi)
        }

        if (time.data.instant.details.dewPointTemperature != null) {
            dewpointF = ConversionMethods.CtoF(time.data.instant.details.dewPointTemperature)
            dewpointC = time.data.instant.details.dewPointTemperature
        }
    }
}

fun createPrecipitation(time: TimeseriesItem): Precipitation {
    return Precipitation().apply {
        // Use cloudiness value here
        cloudiness = Math.round(time.data.instant.details.cloudAreaFraction)
        // Precipitation
        if (time.data?.instant?.details?.probabilityOfPrecipitation != null) {
            pop = Math.round(time.data.instant.details.probabilityOfPrecipitation)
        } else if (time.data?.next1Hours?.details?.probabilityOfPrecipitation != null) {
            pop = Math.round(time.data.next1Hours.details.probabilityOfPrecipitation)
        } else if (time.data?.next6Hours?.details?.probabilityOfPrecipitation != null) {
            pop = Math.round(time.data.next6Hours.details.probabilityOfPrecipitation)
        } else if (time.data?.next12Hours?.details?.probabilityOfPrecipitation != null) {
            pop = Math.round(time.data.next12Hours.details.probabilityOfPrecipitation)
        }
        // The rest DNE
    }
}

fun createHourlyForecast(hr_forecast: TimeseriesItem): HourlyForecast {
    return HourlyForecast().apply {
        // new DateTimeOffset(, TimeSpan.Zero);
        date = ZonedDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(hr_forecast.time)), ZoneOffset.UTC)
        highF = ConversionMethods.CtoF(hr_forecast.data.instant.details.airTemperature)
        highC = hr_forecast.data.instant.details.airTemperature
        windDegrees = Math.round(hr_forecast.data.instant.details.windFromDirection)
        windMph = ConversionMethods.msecToMph(hr_forecast.data.instant.details.windSpeed).roundToInt().toFloat()
        windKph = ConversionMethods.msecToKph(hr_forecast.data.instant.details.windSpeed).roundToInt().toFloat()

        if (hr_forecast.data.next12Hours != null) {
            icon = hr_forecast.data.next12Hours.summary.symbolCode
        } else if (hr_forecast.data.next6Hours != null) {
            icon = hr_forecast.data.next6Hours.summary.symbolCode
        } else if (hr_forecast.data.next1Hours != null) {
            icon = hr_forecast.data.next1Hours.summary.symbolCode
        }

        val humidity = hr_forecast.data.instant.details.relativeHumidity
        // Extras
        extras = ForecastExtras()
        extras.feelslikeF = getFeelsLikeTemp(highF, windMph, Math.round(humidity))
        extras.feelslikeC = ConversionMethods.FtoC(getFeelsLikeTemp(highF, windMph, Math.round(humidity)))
        extras.humidity = Math.round(humidity)
        extras.dewpointF = ConversionMethods.CtoF(hr_forecast.data.instant.details.dewPointTemperature)
        extras.dewpointC = hr_forecast.data.instant.details.dewPointTemperature
        if (hr_forecast.data.instant.details.cloudAreaFraction != null) {
            extras.cloudiness = Math.round(hr_forecast.data.instant.details.cloudAreaFraction)
        }
        // Precipitation
        if (hr_forecast.data.instant.details.probabilityOfPrecipitation != null) {
            extras.pop = Math.round(hr_forecast.data.instant.details.probabilityOfPrecipitation)
        } else if (hr_forecast.data?.next1Hours?.details?.probabilityOfPrecipitation != null) {
            extras.pop = Math.round(hr_forecast.data.next1Hours.details.probabilityOfPrecipitation)
        } else if (hr_forecast.data?.next6Hours?.details?.probabilityOfPrecipitation != null) {
            extras.pop = Math.round(hr_forecast.data.next6Hours.details.probabilityOfPrecipitation)
        } else if (hr_forecast.data?.next12Hours?.details?.probabilityOfPrecipitation != null) {
            extras.pop = Math.round(hr_forecast.data.next12Hours.details.probabilityOfPrecipitation)
        }
        extras.pressureIn = ConversionMethods.mbToInHg(hr_forecast.data.instant.details.airPressureAtSeaLevel)
        extras.pressureMb = hr_forecast.data.instant.details.airPressureAtSeaLevel
        extras.windDegrees = windDegrees
        extras.windMph = windMph
        extras.windKph = windKph
        if (hr_forecast.data.instant.details.windSpeedOfGust != null) {
            extras.windGustMph = ConversionMethods.msecToMph(hr_forecast.data.instant.details.windSpeedOfGust).roundToInt().toFloat()
            extras.windGustKph = ConversionMethods.msecToKph(hr_forecast.data.instant.details.windSpeedOfGust).roundToInt().toFloat()
        }
        if (hr_forecast.data.instant.details.fogAreaFraction != null) {
            val visMi = 10.0f
            extras.visibilityMi = visMi - visMi * hr_forecast.data.instant.details.fogAreaFraction / 100
            extras.visibilityKm = ConversionMethods.miToKm(extras.visibilityMi)
        }
        if (hr_forecast.data.instant.details.ultravioletIndexClearSky != null) {
            extras.uvIndex = hr_forecast.data.instant.details.ultravioletIndexClearSky
        }
    }
}

fun createForecast(time: TimeseriesItem): Forecast {
    return Forecast().apply {
        date = LocalDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(time.time)), ZoneOffset.UTC)

        if (time.data.next12Hours != null) {
            icon = time.data.next12Hours.summary.symbolCode
        } else if (time.data.next6Hours != null) {
            icon = time.data.next6Hours.summary.symbolCode
        } else if (time.data.next1Hours != null) {
            icon = time.data.next1Hours.summary.symbolCode
        }
        // Don't bother setting other values; they're not available yet
    }
}

fun createAstronomy(astroRoot: AstroResponse): Astronomy {
    return Astronomy().apply {
        var moonPhaseValue = -1

        for (time in astroRoot.location.time) {
            if (time.sunrise != null) {
                sunrise = ZonedDateTime.parse(astroRoot.location.time[0].sunrise.time,
                        DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime()
            }
            if (time.sunset != null) {
                sunset = ZonedDateTime.parse(astroRoot.location.time[0].sunset.time,
                        DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime()
            }

            if (time.moonrise != null) {
                moonrise = ZonedDateTime.parse(astroRoot.location.time[0].moonrise.time,
                        DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime()
            }
            if (time.moonset != null) {
                moonset = ZonedDateTime.parse(astroRoot.location.time[0].moonset.time,
                        DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime()
            }

            if (time.moonphase != null)
                moonPhaseValue = time.moonphase.value.toFloat().roundToInt()
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

        val moonPhaseType = if (moonPhaseValue >= 2 && moonPhaseValue < 23) {
            MoonPhaseType.WAXING_CRESCENT
        } else if (moonPhaseValue >= 23 && moonPhaseValue < 26) {
            MoonPhaseType.FIRST_QTR
        } else if (moonPhaseValue >= 26 && moonPhaseValue < 48) {
            MoonPhaseType.WAXING_GIBBOUS
        } else if (moonPhaseValue >= 48 && moonPhaseValue < 52) {
            MoonPhaseType.FULL_MOON
        } else if (moonPhaseValue >= 52 && moonPhaseValue < 73) {
            MoonPhaseType.WANING_GIBBOUS
        } else if (moonPhaseValue >= 73 && moonPhaseValue < 76) {
            MoonPhaseType.LAST_QTR
        } else if (moonPhaseValue >= 76 && moonPhaseValue < 98) {
            MoonPhaseType.WANING_CRESCENT
        } else { // 0, 1, 98, 99, 100
            MoonPhaseType.NEWMOON
        }

        moonPhase = MoonPhase(moonPhaseType)
    }
}