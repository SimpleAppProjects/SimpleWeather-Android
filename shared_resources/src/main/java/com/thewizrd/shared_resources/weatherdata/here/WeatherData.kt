package com.thewizrd.shared_resources.weatherdata.here

import android.annotation.SuppressLint
import com.thewizrd.shared_resources.utils.ConversionMethods
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.utils.NumberUtils
import com.thewizrd.shared_resources.utils.StringUtils
import com.thewizrd.shared_resources.weatherdata.*
import com.thewizrd.shared_resources.weatherdata.Astronomy
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

@SuppressLint("VisibleForTests")
fun createWeatherData(root: Rootobject): Weather {
    return Weather().apply {
        val now = ZonedDateTime.parse(root.feedCreation)

        location = createLocationData(root.observations.location[0])
        updateTime = now
        forecast = ArrayList(root.dailyForecasts.forecastLocation.forecast.size)
        txtForecast = ArrayList(root.dailyForecasts.forecastLocation.forecast.size)
        for (fcast in root.dailyForecasts.forecastLocation.forecast) {
            forecast.add(createForecast(fcast))
            txtForecast.add(createTextForecast(fcast))
        }
        hrForecast = ArrayList(root.hourlyForecasts.forecastLocation.forecast.size)
        for (forecast1 in root.hourlyForecasts.forecastLocation.forecast) {
            if (ZonedDateTime.parse(forecast1.utcTime).truncatedTo(ChronoUnit.HOURS).isBefore(now.truncatedTo(ChronoUnit.HOURS)))
                continue

            hrForecast.add(createHourlyForecast(forecast1))
        }

        val observation = root.observations.location[0].observation[0]
        val todaysForecast = root.dailyForecasts.forecastLocation.forecast[0]

        condition = createCondition(observation, todaysForecast)
        atmosphere = createAtmosphere(observation)
        astronomy = createAstronomy(root.astronomy.astronomy)
        precipitation = createPrecipitation(todaysForecast)
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
        condition = StringUtils.toPascalCase(forecast.description)
        icon = WeatherManager.getProvider(WeatherAPI.HERE)
                .getWeatherIcon(String.format("%s_%s", forecast.daylight, forecast.iconName))

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
                StringUtils.toPascalCase(forecast.description),
                StringUtils.toPascalCase(forecast.beaufortDescription)
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
        condition = StringUtils.toPascalCase(hr_forecast.description)

        icon = WeatherManager.getProvider(WeatherAPI.HERE)
                .getWeatherIcon(String.format("%s_%s", hr_forecast.daylight, hr_forecast.iconName))

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

fun createCondition(observation: ObservationItem, forecastItem: ForecastItem): Condition {
    return Condition().apply {
        weather = StringUtils.toPascalCase(observation.description)
        val temp_F = NumberUtils.tryParseFloat(observation.temperature)
        if (temp_F != null) {
            tempF = temp_F
            tempC = ConversionMethods.FtoC(temp_F)
        }

        var highTempF = NumberUtils.tryParseFloat(observation.highTemperature)
        var lowTempF = NumberUtils.tryParseFloat(observation.lowTemperature)
        if (highTempF != null && lowTempF != null) {
            highF = highTempF
            highC = ConversionMethods.FtoC(highTempF)
            lowF = lowTempF
            lowC = ConversionMethods.FtoC(lowTempF)
        } else {
            highTempF = NumberUtils.tryParseFloat(forecastItem.highTemperature)
            lowTempF = NumberUtils.tryParseFloat(forecastItem.lowTemperature)

            if (highTempF != null && lowTempF != null) {
                highF = highTempF
                highC = ConversionMethods.FtoC(highTempF)
                lowF = lowTempF
                lowC = ConversionMethods.FtoC(lowTempF)
            } else {
                highF = 0.00f
                highC = 0.00f
                lowF = 0.00f
                lowC = 0.00f
            }
        }

        val windDeg = NumberUtils.tryParseInt(observation.windDirection)
        if (windDeg != null) {
            windDegrees = observation.windDirection.toInt()
        }

        val windSpeed = NumberUtils.tryParseFloat(observation.windSpeed)
        if (windSpeed != null) {
            windMph = windSpeed
            windKph = ConversionMethods.mphTokph(windSpeed)
        }

        val comfortTempF = NumberUtils.tryParseFloat(observation.comfort)
        if (comfortTempF != null) {
            feelslikeF = comfortTempF
            feelslikeC = ConversionMethods.FtoC(comfortTempF)
        }

        icon = WeatherManager.getProvider(WeatherAPI.HERE)
                .getWeatherIcon(String.format("%s_%s", observation.daylight, observation.iconName))

        val scale = NumberUtils.tryParseInt(forecastItem.beaufortScale)
        if (scale != null) {
            beaufort = Beaufort(scale)
        }

        val index = NumberUtils.tryParseFloat(forecastItem.uvIndex)
        if (index != null) {
            uv = UV(index)
        }

        observationTime = ZonedDateTime.parse(observation.utcTime)
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
            sunset = LocalTime.parse(astroData.sunset, DateTimeFormatter.ofPattern("h:mma", Locale.ROOT)).atDate(now)
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

fun createPrecipitation(forecast: ForecastItem): Precipitation {
    return Precipitation().apply {
        val POP = NumberUtils.tryParseInt(forecast.precipitationProbability)
        if (POP != null) {
            pop = POP
        }

        val rain_in = NumberUtils.tryParseFloat(forecast.rainFall)
        if (rain_in != null) {
            qpfRainIn = rain_in
            qpfRainMm = ConversionMethods.inToMM(rain_in)
        }

        val snow_in = NumberUtils.tryParseFloat(forecast.rainFall)
        if (snow_in != null) {
            qpfSnowIn = snow_in
            qpfSnowCm = ConversionMethods.inToMM(snow_in) / 10
        }
    }
}