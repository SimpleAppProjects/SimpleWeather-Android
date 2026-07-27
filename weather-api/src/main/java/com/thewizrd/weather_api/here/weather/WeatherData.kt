package com.thewizrd.weather_api.here.weather

import android.annotation.SuppressLint
import com.thewizrd.shared_resources.R
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.ConversionMethods
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.utils.NumberUtils.tryParseFloat
import com.thewizrd.shared_resources.utils.StringUtils.toPascalCase
import com.thewizrd.shared_resources.utils.getBeaufortScale
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.model.Astronomy
import com.thewizrd.shared_resources.weatherdata.model.Atmosphere
import com.thewizrd.shared_resources.weatherdata.model.Beaufort
import com.thewizrd.shared_resources.weatherdata.model.Condition
import com.thewizrd.shared_resources.weatherdata.model.Forecast
import com.thewizrd.shared_resources.weatherdata.model.ForecastExtras
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.shared_resources.weatherdata.model.Location
import com.thewizrd.shared_resources.weatherdata.model.MoonPhase
import com.thewizrd.shared_resources.weatherdata.model.Precipitation
import com.thewizrd.shared_resources.weatherdata.model.TextForecast
import com.thewizrd.shared_resources.weatherdata.model.UV
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.weather_api.weatherModule
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

@SuppressLint("VisibleForTests")
fun createWeatherData(root: PlacesItem): Weather {
    return Weather().apply {
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        var todaysForecast: Forecast? = null
        var todaysTxtForecast: TextForecast? = null

        location = createLocationData(root.observations!![0].place!!)
        updateTime = now
        forecast = ArrayList(root.dailyForecasts!![0].forecasts!!.size)
        txtForecast = ArrayList(root.dailyForecasts!![0].forecasts!!.size)
        for (fcast in root.dailyForecasts!![0].forecasts!!) {
            val dailyFcast = createForecast(fcast)
            val txtFcast = createTextForecast(fcast)
            val offset = txtFcast.date.offset

            forecast!!.add(dailyFcast)
            txtForecast!!.add(txtFcast)

            if (todaysForecast == null && dailyFcast.date.toLocalDate()
                    .isEqual(now.withZoneSameInstant(offset).toLocalDate())
            ) {
                todaysForecast = dailyFcast
                todaysTxtForecast = txtFcast
            }
        }
        hrForecast = ArrayList(root.hourlyForecasts!![0].forecasts!!.size)
        for (forecast1 in root.hourlyForecasts!![0].forecasts!!) {
            if (ZonedDateTime.parse(forecast1.time).truncatedTo(ChronoUnit.HOURS)
                    .isBefore(now.truncatedTo(ChronoUnit.HOURS))
            )
                continue

            hrForecast!!.add(createHourlyForecast(forecast1))
        }

        val observation = root.observations!![0]

        condition = createCondition(observation, todaysForecast, todaysTxtForecast)
        atmosphere = createAtmosphere(observation)
        astronomy = createAstronomy(root.astronomyForecasts!![0].forecasts!!)
        precipitation = createPrecipitation(observation, todaysForecast)
        ttl = 180

        source = WeatherAPI.HERE
    }
}

fun createLocationData(place: Place): Location {
    return Location().apply {
        // Use location name from location provider
        name = null
        latitude = place.location?.lat
        longitude = place.location?.lng
        tzLong = null
    }
}

fun createForecast(forecast: ForecastsItem): Forecast {
    return Forecast().apply {
        date = ZonedDateTime.parse(forecast.time).withZoneSameInstant(ZoneOffset.UTC)
            .toLocalDateTime()
        forecast.highTemperature?.toFloatOrNull()?.let {
            highF = it
            highC = ConversionMethods.FtoC(it)
        }
        forecast.lowTemperature?.toFloatOrNull()?.let {
            lowF = it
            lowC = ConversionMethods.FtoC(it)
        }
        condition = forecast.description?.let {
            StringBuilder(it.toPascalCase()).apply {
                if (!forecast.airDesc.isNullOrBlank() && !forecast.airDesc.equals("*")) {
                    if (!endsWith('.')) {
                        append('.')
                    }
                    append(" ${forecast.airDesc}.")
                }
            }.toString()
        }
        icon = weatherModule.weatherManager.getWeatherProvider(WeatherAPI.HERE)
            .getWeatherIcon(
                forecast.daylight == "night" || forecast.iconName?.startsWith("night") == true,
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
        forecast.rainFall.tryParseFloat(0f).let {
            extras.qpfRainIn = it
            extras.qpfRainMm = ConversionMethods.inToMM(it)
        }
        forecast.snowFall.tryParseFloat(0f).let {
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

fun createTextForecast(forecast: ForecastsItem): TextForecast {
    return TextForecast().apply {
        date = ZonedDateTime.parse(forecast.time)
        fcttext = forecast.description?.let {
            StringBuilder(it.toPascalCase()).apply {
                if (!forecast.beaufortDesc.isNullOrBlank() && !forecast.beaufortDesc.equals("*")) {
                    if (!this.endsWith('.')) {
                        append('.')
                    }
                    append(" ${forecast.beaufortDesc}.")
                }
                if (!forecast.airDesc.isNullOrBlank() && !forecast.airDesc.equals("*")) {
                    if (!this.endsWith('.')) {
                        append('.')
                    }
                    append(" ${forecast.airDesc}.")
                }
            }.toString()
        }
        fcttextMetric = fcttext
    }
}

fun createHourlyForecast(hr_forecast: ForecastsItem): HourlyForecast {
    return HourlyForecast().apply {
        date = ZonedDateTime.parse(hr_forecast.time)
        hr_forecast.temperature?.toFloatOrNull()?.let {
            highF = it
            highC = ConversionMethods.FtoC(it)
        }
        condition = hr_forecast.description?.let {
            StringBuilder(it.toPascalCase()).apply {
                if (!hr_forecast.airDesc.isNullOrBlank() && !hr_forecast.airDesc.equals("*")) {
                    if (!endsWith('.')) {
                        append('.')
                    }
                    append(" ${hr_forecast.airDesc}.")
                }
            }.toString()
        }

        icon = weatherModule.weatherManager.getWeatherProvider(WeatherAPI.HERE)
            .getWeatherIcon(
                hr_forecast.daylight == "night" || hr_forecast.iconName?.startsWith("night") == true,
                hr_forecast.iconName
            )

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
        hr_forecast.rainFall.tryParseFloat(0f).let {
            extras.qpfRainIn = it
            extras.qpfRainMm = ConversionMethods.inToMM(it)
        }
        hr_forecast.snowFall.tryParseFloat(0f).let {
            extras.qpfSnowIn = it
            extras.qpfSnowCm = ConversionMethods.inToMM(it) / 10
        }
        hr_forecast.barometerPressure?.toFloatOrNull()?.let {
            extras.pressureIn = it
            extras.pressureMb = ConversionMethods.inHgToMB(it)
        }
        extras.windDegrees = hr_forecast.windDirection?.toIntOrNull()
        hr_forecast.windSpeed?.toFloatOrNull()?.let {
            extras.windMph = it
            extras.windKph = ConversionMethods.mphTokph(it)
        }
    }
}

fun createCondition(
    observation: ObservationsItem,
    todaysForecast: Forecast? = null,
    todaysTxtForecast: TextForecast? = null
): Condition {
    return Condition().apply {
        weather = observation.description?.toPascalCase()
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
                observation.daylight == "night" || observation.iconName?.startsWith("night") == true,
                observation.iconName
            )

        if (todaysForecast?.extras?.uvIndex != null) {
            uv = UV(todaysForecast.extras.uvIndex)
        }

        observationTime = ZonedDateTime.parse(observation.time)

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

fun createAtmosphere(observation: ObservationsItem): Atmosphere {
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
            sunrise = LocalTime.parse(
                astroData.sunRise,
                DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ROOT)
            ).atDate(now)
        }
        runCatching {
            sunset =
                LocalTime.parse(
                    astroData.sunSet,
                    DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ROOT)
                )
                    .atDate(now)
            if (sunrise != null && sunset.isBefore(sunrise)) {
                // Is next day
                sunset = sunset.plusDays(1)
            }
        }
        runCatching {
            moonrise = LocalTime.parse(
                astroData.moonRise,
                DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ROOT)
            ).atDate(now)
        }
        runCatching {
            moonset = LocalTime.parse(
                astroData.moonSet,
                DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ROOT)
            ).atDate(now)
            if (moonrise != null && moonset.isBefore(moonrise)) {
                // Is next day
                moonset = moonset.plusDays(1)
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
    observation: ObservationsItem,
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