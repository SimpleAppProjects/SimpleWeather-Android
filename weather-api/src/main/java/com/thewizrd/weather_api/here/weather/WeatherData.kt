package com.thewizrd.weather_api.here.weather

import android.annotation.SuppressLint
import com.thewizrd.shared_resources.R
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.ConversionMethods
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.utils.StringUtils.toPascalCase
import com.thewizrd.shared_resources.utils.getBeaufortScale
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.model.*
import com.thewizrd.shared_resources.weatherdata.model.Astronomy
import com.thewizrd.weather_api.weatherModule
import java.text.DecimalFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.math.roundToInt

@SuppressLint("VisibleForTests")
fun createWeatherData(root: Rootobject): Weather {
    return Weather().apply {
        val now = ZonedDateTime.parse(root.feedCreation)
        var todaysForecast: Forecast? = null
        var todaysTxtForecast: TextForecast? = null

        location = createLocationData(root.observations.location[0])
        updateTime = now
        forecast = ArrayList(root.dailyForecasts.forecastLocation.forecast.size)
        txtForecast = ArrayList(root.dailyForecasts.forecastLocation.forecast.size)
        for (fcast in root.dailyForecasts.forecastLocation.forecast) {
            val dailyFcast = createForecast(fcast)
            val txtFcast = createTextForecast(fcast)

            forecast.add(dailyFcast)
            txtForecast.add(txtFcast)

            if (todaysForecast == null && dailyFcast.date.toLocalDate()
                    .isEqual(now.toLocalDate())
            ) {
                todaysForecast = dailyFcast
                todaysTxtForecast = txtFcast
            }
        }
        hrForecast = ArrayList(root.hourlyForecasts.forecastLocation.forecast.size)
        for (forecast1 in root.hourlyForecasts.forecastLocation.forecast) {
            if (ZonedDateTime.parse(forecast1.utcTime).truncatedTo(ChronoUnit.HOURS).isBefore(now.truncatedTo(ChronoUnit.HOURS)))
                continue

            hrForecast.add(createHourlyForecast(forecast1))
        }

        val observation = root.observations.location[0].observation[0]

        condition = createCondition(observation, todaysForecast, todaysTxtForecast)
        atmosphere = createAtmosphere(observation)
        astronomy = createAstronomy(root.astronomy.astronomy)
        precipitation = createPrecipitation(observation, todaysForecast)
        ttl = 180

        source = WeatherAPI.HERE
    }
}

fun createLocationData(location: LocationItem): Location {
    return Location().apply {
        // Use location name from location provider
        name = null
        latitude = location.latitude
        longitude = location.longitude
        tzLong = null
    }
}

fun createForecast(forecast: ForecastItem): Forecast {
    return Forecast().apply {
        date = ZonedDateTime.parse(forecast.utcTime).withZoneSameInstant(ZoneOffset.UTC)
            .toLocalDateTime()
        forecast.highTemperature?.toFloatOrNull()?.let {
            highF = it
            highC = ConversionMethods.FtoC(it)
        }
        forecast.lowTemperature?.toFloatOrNull()?.let {
            lowF = it
            lowC = ConversionMethods.FtoC(it)
        }
        condition = StringBuilder(forecast.description.toPascalCase()).apply {
            if (forecast.airDescription.isNotBlank() && !forecast.airDescription.equals("*")) {
                if (!endsWith('.')) {
                    append('.')
                }
                append(" ${forecast.airDescription}.")
            }
        }.toString()
        icon = weatherModule.weatherManager.getWeatherProvider(WeatherAPI.HERE)
            .getWeatherIcon(
                forecast.daylight == "N" || forecast.iconName.startsWith("night_"),
                forecast.iconName
            )

        // Extras
        extras = ForecastExtras()
        forecast.comfort?.toFloatOrNull()?.let {
            extras.feelslikeF = it
            extras.feelslikeC = ConversionMethods.FtoC(it)
        }
        extras.humidity = forecast.humidity?.toIntOrNull()
        forecast.dewPoint?.toFloatOrNull()?.let {
            extras.dewpointF = it
            extras.dewpointC = ConversionMethods.FtoC(it)
        }
        extras.pop = forecast.precipitationProbability?.toIntOrNull()
        forecast.rainFall?.toFloatOrNull()?.let {
            extras.qpfRainIn = it
            extras.qpfRainMm = ConversionMethods.inToMM(it)
        }
        forecast.snowFall?.toFloatOrNull()?.let {
            extras.qpfSnowIn = it
            extras.qpfSnowCm = ConversionMethods.inToMM(it) / 10
        }
        forecast.barometerPressure?.toFloatOrNull()?.let {
            extras.pressureIn = it
            extras.pressureMb = ConversionMethods.inHgToMB(it)
        }
        extras.windDegrees = forecast.windDirection?.toIntOrNull()
        forecast.windSpeed?.toFloatOrNull()?.let {
            extras.windMph = it
            extras.windKph = ConversionMethods.mphTokph(it)
        }
        extras.uvIndex = forecast.uvIndex?.toFloatOrNull()
    }
}

fun createTextForecast(forecast: ForecastItem): TextForecast {
    return TextForecast().apply {
        date = ZonedDateTime.parse(forecast.utcTime)
        fcttext = StringBuilder(
            String.format(
                Locale.ROOT, "%s - %s",
                forecast.weekday,
                forecast.description.toPascalCase()
            )
        ).apply {
            if (forecast.beaufortDescription.isNotBlank() && !forecast.beaufortDescription.equals("*")) {
                if (!this.endsWith('.')) {
                    append('.')
                }
                append(" ${forecast.beaufortDescription}.")
            }
            if (forecast.airDescription.isNotBlank() && !forecast.airDescription.equals("*")) {
                if (!this.endsWith('.')) {
                    append('.')
                }
                append(" ${forecast.airDescription}.")
            }
        }.toString()
        fcttextMetric = fcttext
    }
}

fun createHourlyForecast(hr_forecast: ForecastItem1): HourlyForecast {
    return HourlyForecast().apply {
        date = ZonedDateTime.parse(hr_forecast.utcTime)
        hr_forecast.temperature?.toFloatOrNull()?.let {
            highF = it
            highC = ConversionMethods.FtoC(it)
        }
        condition = StringBuilder(hr_forecast.description.toPascalCase()).apply {
            if (hr_forecast.airDescription.isNotBlank() && !hr_forecast.airDescription.equals("*")) {
                if (!endsWith('.')) {
                    append('.')
                }
                append(" ${hr_forecast.airDescription}.")
            }
        }.toString()

        icon = weatherModule.weatherManager.getWeatherProvider(WeatherAPI.HERE)
            .getWeatherIcon(
                hr_forecast.daylight == "N" || hr_forecast.iconName.startsWith("night_"),
                hr_forecast.iconName
            )

        windDegrees = hr_forecast.windDirection?.toIntOrNull()
        hr_forecast.windSpeed?.toFloatOrNull()?.let {
            windMph = it
            windKph = ConversionMethods.mphTokph(it)
        }

        // Extras
        extras = ForecastExtras()
        hr_forecast.comfort?.toFloatOrNull()?.let {
            extras.feelslikeF = it
            extras.feelslikeC = ConversionMethods.FtoC(it)
        }
        hr_forecast.humidity?.toIntOrNull()?.let {
            extras.humidity = it
        }
        hr_forecast.dewPoint?.toFloatOrNull()?.let {
            extras.dewpointF = it
            extras.dewpointC = ConversionMethods.FtoC(it)
        }
        hr_forecast.visibility?.toFloatOrNull()?.let {
            extras.visibilityMi = it
            extras.visibilityKm = ConversionMethods.miToKm(it)
        }
        extras.pop = hr_forecast.precipitationProbability?.toIntOrNull()
        hr_forecast.rainFall?.toFloatOrNull()?.let {
            extras.qpfRainIn = it
            extras.qpfRainMm = ConversionMethods.inToMM(it)
        }
        hr_forecast.snowFall?.toFloatOrNull()?.let {
            extras.qpfSnowIn = it
            extras.qpfSnowCm = ConversionMethods.inToMM(it) / 10
        }
        hr_forecast.barometerPressure?.toFloatOrNull()?.let {
            extras.pressureIn = it
            extras.pressureMb = ConversionMethods.inHgToMB(it)
        }
        extras.windDegrees = windDegrees
        extras.windMph = windMph
        extras.windKph = windKph
    }
}

fun createCondition(
    observation: ObservationItem,
    todaysForecast: Forecast? = null,
    todaysTxtForecast: TextForecast? = null
): Condition {
    return Condition().apply {
        weather = observation.description.toPascalCase()
        observation.temperature?.toFloatOrNull()?.let {
            tempF = it
            tempC = ConversionMethods.FtoC(it)
        }

        val highTempF = observation.highTemperature?.toFloatOrNull()
        val lowTempF = observation.lowTemperature?.toFloatOrNull()
        if (highTempF != null && lowTempF != null) {
            highF = highTempF
            highC = ConversionMethods.FtoC(highTempF)
            lowF = lowTempF
            lowC = ConversionMethods.FtoC(lowTempF)
        } else {
            highF = todaysForecast?.highF
            highC = todaysForecast?.highC
            lowF = todaysForecast?.lowF
            lowC = todaysForecast?.lowC
        }

        windDegrees = observation.windDirection?.toIntOrNull()

        observation.windSpeed?.toFloatOrNull()?.let {
            windMph = it
            windKph = ConversionMethods.mphTokph(it)
            beaufort = Beaufort(getBeaufortScale(it.roundToInt()))
        }

        observation.comfort?.toFloatOrNull()?.let {
            feelslikeF = it
            feelslikeC = ConversionMethods.FtoC(it)
        }

        icon = weatherModule.weatherManager.getWeatherProvider(WeatherAPI.HERE)
            .getWeatherIcon(
                observation.daylight == "N" || observation.iconName.startsWith("night_"),
                observation.iconName
            )

        if (todaysForecast?.extras?.uvIndex != null) {
            uv = UV(todaysForecast.extras.uvIndex)
        }

        observationTime = ZonedDateTime.parse(observation.utcTime)

        if (todaysForecast != null && todaysTxtForecast != null) {
            val locale = LocaleUtils.getLocale()
            val ctx = sharedDeps.context
            val df = DecimalFormat.getInstance(locale) as DecimalFormat
            df.applyPattern("#.##")

            // fcttext & fcttextMetric are the same
            val summaryStr = StringBuilder(todaysTxtForecast.fcttext).apply {
                if (todaysForecast.extras?.pop != null) {
                    if (!endsWith('.')) {
                        append('.')
                    }
                    append(" ${ctx.getString(R.string.label_chance)}: ${todaysForecast.extras.pop}%")
                }
            }

            summary = summaryStr.toString()
        }
    }
}

fun createAtmosphere(observation: ObservationItem): Atmosphere {
    return Atmosphere().apply {
        humidity = observation.humidity?.toIntOrNull()

        observation.barometerPressure?.toFloatOrNull()?.let {
            pressureIn = it
            pressureMb = ConversionMethods.inHgToMB(it)
        }
        pressureTrend = observation.barometerTrend

        observation.visibility?.toFloatOrNull()?.let {
            visibilityMi = it
            visibilityKm = ConversionMethods.miToKm(it)
        }

        observation.dewPoint?.toFloatOrNull()?.let {
            dewpointF = it
            dewpointC = ConversionMethods.FtoC(it)
        }
    }
}

fun createAstronomy(astronomy: List<AstronomyItem>): Astronomy {
    return Astronomy().apply {
        val astroData = astronomy[0]

        val now = LocalDate.now()

        runCatching {
            sunrise = LocalTime.parse(astroData.sunrise, DateTimeFormatter.ofPattern("h:mma", Locale.ROOT)).atDate(now)
        }
        runCatching {
            sunset =
                LocalTime.parse(astroData.sunset, DateTimeFormatter.ofPattern("h:mma", Locale.ROOT))
                    .atDate(now)
            if (sunrise != null && sunset.isBefore(sunrise)) {
                // Is next day
                sunset = sunset.plusDays(1)
            }
        }
        runCatching {
            moonrise = LocalTime.parse(astroData.moonrise, DateTimeFormatter.ofPattern("h:mma", Locale.ROOT)).atDate(now)
        }
        runCatching {
            moonset = LocalTime.parse(astroData.moonset, DateTimeFormatter.ofPattern("h:mma", Locale.ROOT)).atDate(now)
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

        moonPhase = when (astroData.iconName) {
            "cw_new_moon" -> MoonPhase(MoonPhase.MoonPhaseType.NEWMOON)
            "cw_waxing_crescent" -> MoonPhase(MoonPhase.MoonPhaseType.WAXING_CRESCENT)
            "cw_first_qtr" -> MoonPhase(MoonPhase.MoonPhaseType.FIRST_QTR)
            "cw_waxing_gibbous" -> MoonPhase(MoonPhase.MoonPhaseType.WAXING_GIBBOUS)
            "cw_full_moon" -> MoonPhase(MoonPhase.MoonPhaseType.FULL_MOON)
            "cw_waning_gibbous" -> MoonPhase(MoonPhase.MoonPhaseType.WANING_GIBBOUS)
            "cw_last_quarter" -> MoonPhase(MoonPhase.MoonPhaseType.LAST_QTR)
            "cw_waning_crescent" -> MoonPhase(MoonPhase.MoonPhaseType.WANING_CRESCENT)
            else -> MoonPhase(MoonPhase.MoonPhaseType.NEWMOON)
        }
    }
}

fun createPrecipitation(
    observation: ObservationItem,
    todaysForecast: Forecast? = null
): Precipitation {
    return Precipitation().apply {
        pop = todaysForecast?.extras?.pop

        observation.precipitation1H?.toFloatOrNull()?.let {
            qpfRainIn = it
            qpfRainMm = ConversionMethods.inToMM(it)
        } ?: observation.precipitation3H?.toFloatOrNull()?.let {
            qpfRainIn = it
            qpfRainMm = ConversionMethods.inToMM(it)
        } ?: observation.precipitation6H?.toFloatOrNull()?.let {
            qpfRainIn = it
            qpfRainMm = ConversionMethods.inToMM(it)
        } ?: observation.precipitation12H?.toFloatOrNull()?.let {
            qpfRainIn = it
            qpfRainMm = ConversionMethods.inToMM(it)
        } ?: observation.precipitation24H?.toFloatOrNull()?.let {
            qpfRainIn = it
            qpfRainMm = ConversionMethods.inToMM(it)
        } ?: todaysForecast?.extras?.let {
            qpfRainIn = it.qpfRainIn
            qpfRainMm = it.qpfRainMm
        }

        observation.snowCover?.toFloatOrNull()?.let {
            qpfSnowIn = it
            qpfSnowCm = ConversionMethods.inToMM(it) / 10
        } ?: todaysForecast?.extras?.let {
            qpfSnowIn = it.qpfSnowIn
            qpfSnowCm = it.qpfSnowCm
        }
    }
}