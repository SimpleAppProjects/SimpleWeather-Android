package com.thewizrd.weather_api.here.weather

import android.annotation.SuppressLint
import com.thewizrd.shared_resources.R
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.utils.StringUtils.toPascalCase
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
        date = ZonedDateTime.parse(forecast.utcTime).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()
        val high_f = NumberUtils.tryParseFloat(forecast.highTemperature)
        if (high_f != null) {
            highF = high_f
            highC = ConversionMethods.FtoC(high_f)
        }
        val low_f = NumberUtils.tryParseFloat(forecast.lowTemperature)
        if (low_f != null) {
            lowF = low_f
            lowC = ConversionMethods.FtoC(low_f)
        }
        condition = forecast.description.toPascalCase()
        icon = weatherModule.weatherManager.getWeatherProvider(WeatherAPI.HERE)
            .getWeatherIcon(
                forecast.daylight == "N" || forecast.iconName.startsWith("night_"),
                forecast.iconName
            )

        // Extras
        extras = ForecastExtras()
        val comfortTempF = NumberUtils.tryParseFloat(forecast.comfort)
        if (comfortTempF != null) {
            extras.feelslikeF = comfortTempF
            extras.feelslikeC = ConversionMethods.FtoC(comfortTempF)
        }
        val humidity = NumberUtils.tryParseInt(forecast.humidity)
        if (humidity != null) {
            extras.humidity = humidity
        }
        val dewpointF = NumberUtils.tryParseFloat(forecast.dewPoint)
        if (dewpointF != null) {
            extras.dewpointF = dewpointF
            extras.dewpointC = ConversionMethods.FtoC(dewpointF)
        }
        val pop = NumberUtils.tryParseInt(forecast.precipitationProbability)
        if (pop != null) {
            extras.pop = pop
        }
        val rain_in = NumberUtils.tryParseFloat(forecast.rainFall)
        if (rain_in != null) {
            extras.qpfRainIn = rain_in
            extras.qpfRainMm = ConversionMethods.inToMM(rain_in)
        }
        val snow_in = NumberUtils.tryParseFloat(forecast.snowFall)
        if (snow_in != null) {
            extras.qpfSnowIn = snow_in
            extras.qpfSnowCm = ConversionMethods.inToMM(snow_in) / 10
        }
        val pressureIN = NumberUtils.tryParseFloat(forecast.barometerPressure)
        if (pressureIN != null) {
            extras.pressureIn = pressureIN
            extras.pressureMb = ConversionMethods.inHgToMB(pressureIN)
        }
        val windDegrees = NumberUtils.tryParseInt(forecast.windDirection)
        if (windDegrees != null) {
            extras.windDegrees = windDegrees
        }
        val windSpeed = NumberUtils.tryParseFloat(forecast.windSpeed)
        if (windSpeed != null) {
            extras.windMph = windSpeed
            extras.windKph = ConversionMethods.mphTokph(windSpeed)
        }
        val uv_index = NumberUtils.tryParseFloat(forecast.uvIndex)
        if (uv_index != null) {
            extras.uvIndex = uv_index
        }
    }
}

fun createTextForecast(forecast: ForecastItem): TextForecast {
    return TextForecast().apply {
        date = ZonedDateTime.parse(forecast.utcTime)
        fcttext = String.format(
            Locale.ROOT, "%s - %s %s",
            forecast.weekday,
            forecast.description.toPascalCase(),
            forecast.beaufortDescription.toPascalCase()
        )
        fcttextMetric = fcttext
    }
}

fun createHourlyForecast(hr_forecast: ForecastItem1): HourlyForecast {
    return HourlyForecast().apply {
        date = ZonedDateTime.parse(hr_forecast.utcTime)
        val high_f = NumberUtils.tryParseFloat(hr_forecast.temperature)
        if (high_f != null) {
            highF = high_f
            highC = ConversionMethods.FtoC(high_f)
        }
        condition = hr_forecast.description.toPascalCase()

        icon = weatherModule.weatherManager.getWeatherProvider(WeatherAPI.HERE)
            .getWeatherIcon(
                hr_forecast.daylight == "N" || hr_forecast.iconName.startsWith("night_"),
                hr_forecast.iconName
            )

        val windDeg = NumberUtils.tryParseInt(hr_forecast.windDirection)
        if (windDeg != null) {
            windDegrees = windDeg
        }
        val windSpeed = NumberUtils.tryParseFloat(hr_forecast.windSpeed)
        if (windSpeed != null) {
            windMph = windSpeed
            windKph = ConversionMethods.mphTokph(windSpeed)
        }

        // Extras
        extras = ForecastExtras()
        val comfortTempF = NumberUtils.tryParseFloat(hr_forecast.comfort)
        if (comfortTempF != null) {
            extras.feelslikeF = comfortTempF
            extras.feelslikeC = ConversionMethods.FtoC(comfortTempF)
        }
        val humidity = NumberUtils.tryParseInt(hr_forecast.humidity)
        if (humidity != null) {
            extras.humidity = humidity
        }
        val dewpointF = NumberUtils.tryParseFloat(hr_forecast.dewPoint)
        if (dewpointF != null) {
            extras.dewpointF = dewpointF
            extras.dewpointC = ConversionMethods.FtoC(dewpointF)
        }
        val visibilityMI = NumberUtils.tryParseFloat(hr_forecast.visibility)
        if (visibilityMI != null) {
            extras.visibilityMi = visibilityMI
            extras.visibilityKm = ConversionMethods.miToKm(visibilityMI)
        }
        extras.pop = NumberUtils.tryParseInt(hr_forecast.precipitationProbability)
        val rain_in = NumberUtils.tryParseFloat(hr_forecast.rainFall)
        if (rain_in != null) {
            extras.qpfRainIn = rain_in
            extras.qpfRainMm = ConversionMethods.inToMM(rain_in)
        }
        val snow_in = NumberUtils.tryParseFloat(hr_forecast.snowFall)
        if (snow_in != null) {
            extras.qpfSnowIn = snow_in
            extras.qpfSnowCm = ConversionMethods.inToMM(snow_in) / 10
        }
        val pressureIN = NumberUtils.tryParseFloat(hr_forecast.barometerPressure)
        if (pressureIN != null) {
            extras.pressureIn = pressureIN
            extras.pressureMb = ConversionMethods.inHgToMB(pressureIN)
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
        val temp_F = NumberUtils.tryParseFloat(observation.temperature)
        if (temp_F != null) {
            tempF = temp_F
            tempC = ConversionMethods.FtoC(temp_F)
        }

        val highTempF = NumberUtils.tryParseFloat(observation.highTemperature)
        val lowTempF = NumberUtils.tryParseFloat(observation.lowTemperature)
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

        val windDeg = NumberUtils.tryParseInt(observation.windDirection)
        if (windDeg != null) {
            windDegrees = observation.windDirection.toInt()
        }

        val windSpeed = NumberUtils.tryParseFloat(observation.windSpeed)
        if (windSpeed != null) {
            windMph = windSpeed
            windKph = ConversionMethods.mphTokph(windSpeed)
            beaufort = Beaufort(getBeaufortScale(windSpeed.roundToInt()))
        }

        val comfortTempF = NumberUtils.tryParseFloat(observation.comfort)
        if (comfortTempF != null) {
            feelslikeF = comfortTempF
            feelslikeC = ConversionMethods.FtoC(comfortTempF)
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

            val summaryStr = StringBuilder()
            summaryStr.append(todaysTxtForecast.fcttext) // fcttext & fcttextMetric are the same
            if (todaysForecast.extras?.pop != null) {
                summaryStr.append(" ${ctx.getString(R.string.label_chance)}: ${todaysForecast.extras.pop}%")
            }

            summary = summaryStr.toString()
        }
    }
}

fun createAtmosphere(observation: ObservationItem): Atmosphere {
    return Atmosphere().apply {
        val Humidity = NumberUtils.tryParseInt(observation.humidity)
        if (Humidity != null) {
            humidity = Humidity
        }

        val pressureIN = NumberUtils.tryParseFloat(observation.barometerPressure)
        if (pressureIN != null) {
            pressureIn = pressureIN
            pressureMb = ConversionMethods.inHgToMB(pressureIN)
        }
        pressureTrend = observation.barometerTrend

        val visibilityMI = NumberUtils.tryParseFloat(observation.visibility)
        if (visibilityMI != null) {
            visibilityMi = visibilityMI
            visibilityKm = ConversionMethods.miToKm(visibilityMI)
        }

        val dewpoint_f = NumberUtils.tryParseFloat(observation.dewPoint)
        if (dewpoint_f != null) {
            dewpointF = dewpoint_f
            dewpointC = ConversionMethods.FtoC(dewpoint_f)
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
            moonrise = DateTimeUtils.getLocalDateTimeMIN()
        }
        if (moonset == null) {
            moonset = DateTimeUtils.getLocalDateTimeMIN()
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