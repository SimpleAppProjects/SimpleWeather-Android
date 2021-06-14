package com.thewizrd.shared_resources.weatherdata.openweather.onecall

import com.thewizrd.shared_resources.R
import com.thewizrd.shared_resources.SimpleLibrary
import com.thewizrd.shared_resources.utils.AirQualityUtils.AQICO
import com.thewizrd.shared_resources.utils.AirQualityUtils.AQINO2
import com.thewizrd.shared_resources.utils.AirQualityUtils.AQIO3
import com.thewizrd.shared_resources.utils.AirQualityUtils.AQIPM10
import com.thewizrd.shared_resources.utils.AirQualityUtils.AQIPM2_5
import com.thewizrd.shared_resources.utils.AirQualityUtils.AQISO2
import com.thewizrd.shared_resources.utils.AirQualityUtils.CO_ugm3_TO_ppm
import com.thewizrd.shared_resources.utils.AirQualityUtils.NO2_ugm3_to_ppb
import com.thewizrd.shared_resources.utils.AirQualityUtils.O3_ugm3_to_ppb
import com.thewizrd.shared_resources.utils.AirQualityUtils.SO2_ugm3_to_ppb
import com.thewizrd.shared_resources.utils.ConversionMethods
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.utils.StringUtils
import com.thewizrd.shared_resources.utils.getBeaufortScale
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.WeatherManager
import com.thewizrd.shared_resources.weatherdata.model.*
import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

fun createWeatherData(root: Rootobject): Weather {
    return Weather().apply {
        location = createLocation(root)
        updateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(root.current.dt), ZoneOffset.UTC)

        forecast = ArrayList(root.daily.size)
        txtForecast = ArrayList(root.daily.size)
        for (daily in root.daily) {
            forecast.add(createForecast(daily))
            txtForecast.add(createTextForecast(daily))
        }
        hrForecast = ArrayList(root.hourly.size)
        for (hourly in root.hourly) {
            hrForecast.add(createHourlyForecast(hourly))
        }
        if (!root.minutely.isNullOrEmpty()) {
            minForecast = ArrayList(root.minutely.size)
            for (min in root.minutely) {
                minForecast.add(createMinutelyForecast(min))
            }
        }

        condition = createCondition(root.current)
        atmosphere = createAtmosphere(root.current)
        precipitation = createPrecipitation(root.current)
        ttl = 180

        val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
        df.applyPattern("#.####")
        query = String.format(Locale.ROOT, "lat=%s&lon=%s", df.format(location.latitude), location.longitude)

        if ((condition.highF == null || condition.highC == null) && forecast.size > 0) {
            condition.highF = forecast[0].highF
            condition.highC = forecast[0].highC
            condition.lowF = forecast[0].lowF
            condition.lowC = forecast[0].lowC
        }

        condition.observationTime = updateTime

        val firstDate = LocalDateTime.ofEpochSecond(root.daily[0].dt, 0, ZoneOffset.UTC)
        astronomy = if (firstDate.toLocalDate().isEqual(condition.observationTime.toLocalDate())) {
            createAstronomy(root.daily[0])
        } else {
            createAstronomy(root.current)
        }

        weatherAlerts = createWeatherAlerts(root.alerts)

        source = WeatherAPI.OPENWEATHERMAP
    }
}

fun createLocation(root: Rootobject): Location {
    return Location().apply {
        // Use location name from location provider
        name = null
        latitude = root.lat
        longitude = root.lon
        tzLong = root.timezone
    }
}

fun createHourlyForecast(hr_forecast: HourlyItem): HourlyForecast {
    return HourlyForecast().apply {
        date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(hr_forecast.dt), ZoneOffset.UTC)
        highF = ConversionMethods.KtoF(hr_forecast.temp)
        highC = ConversionMethods.KtoC(hr_forecast.temp)
        condition = StringUtils.toUpperCase(hr_forecast.weather[0].description)

        // Use icon to determine if day or night
        val ico = hr_forecast.weather[0].icon
        var dn = ico[if (ico.isEmpty()) 0 else ico.length - 1].toString()

        try {
            val x = dn.toInt()
            dn = ""
        } catch (ex: NumberFormatException) {
            // Do nothing
        }

        icon = WeatherManager.getProvider(WeatherAPI.OPENWEATHERMAP)
                .getWeatherIcon(hr_forecast.weather[0].id.toString() + dn)

        windDegrees = hr_forecast.windDeg
        windMph = ConversionMethods.msecToMph(hr_forecast.windSpeed).roundToInt().toFloat()
        windKph = ConversionMethods.msecToKph(hr_forecast.windSpeed).roundToInt().toFloat()

        // Extras
        extras = ForecastExtras()
        extras.feelslikeF = ConversionMethods.KtoF(hr_forecast.feelsLike)
        extras.feelslikeC = ConversionMethods.KtoC(hr_forecast.feelsLike)
        extras.dewpointF = ConversionMethods.KtoF(hr_forecast.dewPoint)
        extras.dewpointC = ConversionMethods.KtoC(hr_forecast.dewPoint)
        extras.humidity = hr_forecast.humidity
        extras.cloudiness = hr_forecast.clouds
        if (hr_forecast.pop != null) {
            extras.pop = (hr_forecast.pop * 100).roundToInt()
        }
        // 1hPA = 1mbar
        extras.pressureMb = hr_forecast.pressure
        extras.pressureIn = ConversionMethods.mbToInHg(hr_forecast.pressure)
        extras.windDegrees = windDegrees
        extras.windMph = windMph
        extras.windKph = windKph
        if (hr_forecast.windGust != null) {
            extras.windGustMph = ConversionMethods.msecToMph(hr_forecast.windGust).roundToInt().toFloat()
            extras.windGustKph = ConversionMethods.msecToKph(hr_forecast.windGust).roundToInt().toFloat()
        }
        if (hr_forecast.visibility != null) {
            extras.visibilityKm = hr_forecast.visibility.toFloat() / 1000
            extras.visibilityMi = ConversionMethods.kmToMi(extras.visibilityKm)
        }
        if (hr_forecast.rain != null) {
            extras.qpfRainMm = hr_forecast.rain._1h
            extras.qpfRainIn = ConversionMethods.mmToIn(hr_forecast.rain._1h)
        }
        if (hr_forecast.snow != null) {
            extras.qpfSnowCm = hr_forecast.snow._1h / 10
            extras.qpfSnowIn = ConversionMethods.mmToIn(hr_forecast.snow._1h)
        }
        if (hr_forecast.uvi != null) {
            extras.uvIndex = hr_forecast.uvi
        }
    }
}

fun createForecast(forecast: DailyItem): Forecast {
    return Forecast().apply {
        date = LocalDateTime.ofEpochSecond(forecast.dt, 0, ZoneOffset.UTC)
        highF = ConversionMethods.KtoF(forecast.temp.max)
        highC = ConversionMethods.KtoC(forecast.temp.max)
        lowF = ConversionMethods.KtoF(forecast.temp.min)
        lowC = ConversionMethods.KtoC(forecast.temp.min)
        condition = StringUtils.toUpperCase(forecast.weather[0].description)
        icon = WeatherManager.getProvider(WeatherAPI.OPENWEATHERMAP)
                .getWeatherIcon(forecast.weather[0].id.toString())

        // Extras
        extras = ForecastExtras()
        extras.dewpointF = ConversionMethods.KtoF(forecast.dewPoint)
        extras.dewpointC = ConversionMethods.KtoC(forecast.dewPoint)
        extras.humidity = forecast.humidity
        if (forecast.pop != null) {
            extras.pop = (forecast.pop * 100).roundToInt()
        }
        extras.cloudiness = forecast.clouds
        // 1hPA = 1mbar
        extras.pressureMb = forecast.pressure
        extras.pressureIn = ConversionMethods.mbToInHg(forecast.pressure)
        extras.windDegrees = forecast.windDeg
        extras.windMph = ConversionMethods.msecToMph(forecast.windSpeed).roundToInt().toFloat()
        extras.windKph = ConversionMethods.msecToKph(forecast.windSpeed).roundToInt().toFloat()
        extras.uvIndex = forecast.uvi
        if (forecast.visibility != null) {
            extras.visibilityKm = forecast.visibility.toFloat() / 1000
            extras.visibilityMi = ConversionMethods.kmToMi(extras.visibilityKm)
        }
        if (forecast.windGust != null) {
            extras.windGustMph = ConversionMethods.msecToMph(forecast.windGust).roundToInt().toFloat()
            extras.windGustKph = ConversionMethods.msecToKph(forecast.windGust).roundToInt().toFloat()
        }
        if (forecast.rain != null) {
            extras.qpfRainMm = forecast.rain
            extras.qpfRainIn = ConversionMethods.mmToIn(forecast.rain)
        }
        if (forecast.snow != null) {
            extras.qpfSnowCm = forecast.snow / 10
            extras.qpfSnowIn = ConversionMethods.mmToIn(forecast.snow)
        }
    }
}

fun createTextForecast(forecast: DailyItem): TextForecast {
    return TextForecast().apply {
        val context = SimpleLibrary.instance.app.appContext

        date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(forecast.dt), ZoneOffset.UTC)

        val sb = StringBuilder()
        sb.append(String.format(Locale.ROOT,
                "%s - %s: %s°; %s: %s°", context.getString(R.string.label_morning),
                context.getString(R.string.label_temp),
                ConversionMethods.KtoF(forecast.temp.morn).roundToInt(),
                context.getString(R.string.label_feelslike),
                ConversionMethods.KtoF(forecast.feelsLike.morn).roundToInt()))
        sb.append(StringUtils.lineSeparator())
        sb.append(String.format(Locale.ROOT,
                "%s - %s: %s°; %s: %s°", context.getString(R.string.label_afternoon),
                context.getString(R.string.label_temp),
                ConversionMethods.KtoF(forecast.temp.day).roundToInt(),
                context.getString(R.string.label_feelslike),
                ConversionMethods.KtoF(forecast.feelsLike.day).roundToInt()))
        sb.append(StringUtils.lineSeparator())
        sb.append(String.format(Locale.ROOT,
                "%s - %s: %s°; %s: %s°", context.getString(R.string.label_eve),
                context.getString(R.string.label_temp),
                ConversionMethods.KtoF(forecast.temp.eve).roundToInt(),
                context.getString(R.string.label_feelslike),
                ConversionMethods.KtoF(forecast.feelsLike.eve).roundToInt()))
        sb.append(StringUtils.lineSeparator())
        sb.append(String.format(Locale.ROOT,
                "%s - %s: %s°; %s: %s°", context.getString(R.string.label_night),
                context.getString(R.string.label_temp),
                ConversionMethods.KtoF(forecast.temp.night).roundToInt(),
                context.getString(R.string.label_feelslike),
                ConversionMethods.KtoF(forecast.feelsLike.night).roundToInt()))

        fcttext = sb.toString()

        val sb_metric = StringBuilder()
        sb_metric.append(String.format(Locale.ROOT,
                "%s - %s: %s°; %s: %s°", context.getString(R.string.label_morning),
                context.getString(R.string.label_temp),
                ConversionMethods.KtoC(forecast.temp.morn).roundToInt(),
                context.getString(R.string.label_feelslike),
                ConversionMethods.KtoC(forecast.feelsLike.morn).roundToInt()))
        sb_metric.append(StringUtils.lineSeparator())
        sb_metric.append(String.format(Locale.ROOT,
                "%s - %s: %s°; %s: %s°", context.getString(R.string.label_afternoon),
                context.getString(R.string.label_temp),
                ConversionMethods.KtoC(forecast.temp.day).roundToInt(),
                context.getString(R.string.label_feelslike),
                ConversionMethods.KtoC(forecast.feelsLike.day).roundToInt()))
        sb_metric.append(StringUtils.lineSeparator())
        sb_metric.append(String.format(Locale.ROOT,
                "%s - %s: %s°; %s: %s°", context.getString(R.string.label_eve),
                context.getString(R.string.label_temp),
                ConversionMethods.KtoC(forecast.temp.eve).roundToInt(),
                context.getString(R.string.label_feelslike),
                ConversionMethods.KtoC(forecast.feelsLike.eve).roundToInt()))
        sb_metric.append(StringUtils.lineSeparator())
        sb_metric.append(String.format(Locale.ROOT,
                "%s - %s: %s°; %s: %s°", context.getString(R.string.label_night),
                context.getString(R.string.label_temp),
                ConversionMethods.KtoC(forecast.temp.night).roundToInt(),
                context.getString(R.string.label_feelslike),
                ConversionMethods.KtoC(forecast.feelsLike.night).roundToInt()))

        fcttextMetric = sb_metric.toString()
    }
}

fun createMinutelyForecast(minutelyItem: MinutelyItem): MinutelyForecast {
    return MinutelyForecast().apply {
        date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(minutelyItem.dt), ZoneOffset.UTC)
        rainMm = minutelyItem.precipitation
    }
}

fun createCondition(current: Current): Condition {
    return Condition().apply {
        weather = StringUtils.toUpperCase(current.weather[0].description)
        tempF = ConversionMethods.KtoF(current.temp)
        tempC = ConversionMethods.KtoC(current.temp)
        windDegrees = current.windDeg
        windMph = ConversionMethods.msecToMph(current.windSpeed)
        windKph = ConversionMethods.msecToKph(current.windSpeed)
        feelslikeF = ConversionMethods.KtoF(current.feelsLike)
        feelslikeC = ConversionMethods.KtoC(current.feelsLike)
        if (current.windGust != null) {
            windGustMph = ConversionMethods.msecToMph(current.windGust)
            windGustKph = ConversionMethods.msecToKph(current.windGust)
        }

        val ico = current.weather[0].icon
        var dn = ico[if (ico.isEmpty()) 0 else ico.length - 1].toString()

        try {
            val x = dn.toInt()
            dn = ""
        } catch (ex: NumberFormatException) {
            // DO nothing
        }

        icon = WeatherManager.getProvider(WeatherAPI.OPENWEATHERMAP)
                .getWeatherIcon(current.weather[0].id.toString() + dn)

        uv = UV(current.uvi)
        beaufort = Beaufort(getBeaufortScale(current.windSpeed))

        observationTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(current.dt), ZoneOffset.UTC)
    }
}

fun createAtmosphere(current: Current): Atmosphere {
    return Atmosphere().apply {
        humidity = current.humidity
        // 1hPa = 1mbar
        pressureMb = current.pressure
        pressureIn = ConversionMethods.mbToInHg(pressureMb)
        pressureTrend = ""
        visibilityKm = current.visibility / 1000f
        visibilityMi = ConversionMethods.kmToMi(visibilityKm)
        dewpointF = ConversionMethods.KtoF(current.dewPoint)
        dewpointC = ConversionMethods.KtoC(current.dewPoint)
    }
}

fun createAstronomy(current: Current): Astronomy {
    return Astronomy().apply {
        runCatching {
            sunrise = LocalDateTime.ofEpochSecond(current.sunrise, 0, ZoneOffset.UTC)
        }
        runCatching {
            sunset = LocalDateTime.ofEpochSecond(current.sunset, 0, ZoneOffset.UTC)
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

fun createAstronomy(day: DailyItem): Astronomy {
    return Astronomy().apply {
        runCatching {
            sunrise = LocalDateTime.ofEpochSecond(day.sunrise, 0, ZoneOffset.UTC)
        }
        runCatching {
            sunset = LocalDateTime.ofEpochSecond(day.sunset, 0, ZoneOffset.UTC)
        }
        runCatching {
            moonrise = LocalDateTime.ofEpochSecond(day.sunset, 0, ZoneOffset.UTC)
        }
        runCatching {
            moonset = LocalDateTime.ofEpochSecond(day.sunset, 0, ZoneOffset.UTC)
        }
        runCatching {
            val moonPhaseType = when {
                day.moon_phase == 0f || day.moon_phase == 1f -> MoonPhase.MoonPhaseType.NEWMOON
                day.moon_phase == 0.25f -> MoonPhase.MoonPhaseType.FIRST_QTR
                day.moon_phase == 0.5f -> MoonPhase.MoonPhaseType.FULL_MOON
                day.moon_phase == 0.75f -> MoonPhase.MoonPhaseType.LAST_QTR
                day.moon_phase > 0f && day.moon_phase < 0.25f -> MoonPhase.MoonPhaseType.WAXING_CRESCENT
                day.moon_phase > 0.25f && day.moon_phase < 0.5f -> MoonPhase.MoonPhaseType.WAXING_GIBBOUS
                day.moon_phase > 0.5f && day.moon_phase < 0.75f -> MoonPhase.MoonPhaseType.WANING_GIBBOUS
                day.moon_phase > 0.75f && day.moon_phase < 1f -> MoonPhase.MoonPhaseType.WANING_CRESCENT
                else -> MoonPhase.MoonPhaseType.NEWMOON
            }
            moonPhase = MoonPhase(moonPhaseType)
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

fun createPrecipitation(current: Current): Precipitation {
    return Precipitation().apply {
        // Use cloudiness value here
        cloudiness = current.clouds
        if (current.rain != null) {
            qpfRainIn = ConversionMethods.mmToIn(current.rain._1h)
            qpfRainMm = current.rain._1h
        }
        if (current.snow != null) {
            qpfSnowIn = ConversionMethods.mmToIn(current.snow._1h)
            qpfSnowCm = current.snow._1h / 10
        }
    }
}

fun createAirQuality(response: AirPollutionResponse): AirQuality? {
    val data = response.list[0]

    // Convert
    val idx = maxOf(
        data.components.co?.let { runCatching { AQICO(CO_ugm3_TO_ppm(it)) }.getOrNull() } ?: -1,
        data.components.no2?.let { runCatching { AQINO2(NO2_ugm3_to_ppb(it)) }.getOrNull() } ?: -1,
        data.components.o3?.let { runCatching { AQIO3(O3_ugm3_to_ppb(it)) }.getOrNull() } ?: -1,
        data.components.so2?.let { runCatching { AQISO2(SO2_ugm3_to_ppb(it)) }.getOrNull() } ?: -1,
        data.components.pm25?.let { runCatching { AQIPM2_5(it) }.getOrNull() } ?: -1,
        data.components.pm10?.let { runCatching { AQIPM10(it) }.getOrNull() } ?: -1,
    )

    if (idx >= 0)
        return AirQuality().apply {
            index = idx
        }

    return null
}