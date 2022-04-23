package com.thewizrd.weather_api.weatherapi.weather

import com.thewizrd.shared_resources.utils.*
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
import com.thewizrd.shared_resources.utils.AirQualityUtils.getIndexFromData
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.model.*
import com.thewizrd.shared_resources.weatherdata.model.AirQuality
import com.thewizrd.shared_resources.weatherdata.model.Condition
import com.thewizrd.shared_resources.weatherdata.model.Forecast
import com.thewizrd.shared_resources.weatherdata.model.Location
import com.thewizrd.weather_api.weatherModule
import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

fun createWeatherData(root: ForecastResponse): Weather {
    return Weather().apply {
        location = createLocation(root.location!!)
        updateTime = ZonedDateTime.of(
            LocalDateTime.parse(
                root.location!!.localtime,
                DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm")
            ),
            ZoneIdCompat.of(root.location!!.tzId)
        )

        forecast = ArrayList(root.forecast!!.forecastday!!.size)
        hrForecast = ArrayList<HourlyForecast>().apply {
            root.forecast!!.forecastday?.firstOrNull()?.hour?.size?.let {
                ensureCapacity(it)
            }
        }

        // Forecast
        for (day in root.forecast!!.forecastday!!) {
            val fcast = createForecast(day)

            day.hour?.forEach { hour ->
                val date = ZonedDateTime.ofInstant(
                    Instant.ofEpochSecond(hour.timeEpoch!!.toLong()),
                    ZoneOffset.UTC
                )

                if (!date.isBefore(updateTime)) {
                    hrForecast.add(createHourlyForecast(hour, root.location!!.tzId!!))
                }
            }

            forecast.add(fcast)
        }

        condition = createCondition(root.current!!, root.location!!.tzId!!)
        atmosphere = createAtmosphere(root.current!!)
        if (root.forecast!!.forecastday!![0].date == condition.observationTime.toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))) {
            astronomy = createAstronomy(root.forecast!!.forecastday!![0].astro!!)
        }
        precipitation = createPrecipitation(root.current!!)
        ttl = 180

        if ((condition.highF == null || condition.highC == null) && forecast.size > 0) {
            condition.highF = forecast[0].highF
            condition.highC = forecast[0].highC
            condition.lowF = forecast[0].lowF
            condition.lowC = forecast[0].lowC
        }

        weatherAlerts = createWeatherAlerts(root.alerts)

        source = WeatherAPI.WEATHERAPI
    }
}

fun createLocation(location: com.thewizrd.weather_api.weatherapi.weather.Location): Location {
    return Location().apply {
        /* Use name from location provider */
        //name = location.name
        latitude = location.lat!!.toFloat()
        longitude = location.lon!!.toFloat()
        tzLong = location.tzId!!
    }
}

fun createForecast(day: ForecastdayItem): Forecast {
    return Forecast().apply {
        date = LocalDate.parse(day.date!!).atStartOfDay()

        highF = day.day!!.maxtempF
        highC = day.day!!.maxtempC
        lowF = day.day!!.mintempF
        lowC = day.day!!.mintempC

        condition = day.day!!.condition!!.text
        icon = weatherModule.weatherManager.getWeatherProvider(WeatherAPI.WEATHERAPI)
            .getWeatherIcon(day.day!!.condition!!.code?.toString())

        extras = ForecastExtras()

        extras.feelslikeF = getFeelsLikeTemp(day.day!!.avgtempF!!, day.day!!.maxwindMph!!, day.day!!.avghumidity!!.roundToInt())
        extras.feelslikeC = ConversionMethods.FtoC(extras.feelslikeF)
        extras.humidity = day.day!!.avghumidity?.roundToInt()
        extras.dewpointC = calculateDewpointC(day.day!!.avgtempC!!, extras.humidity)
        extras.dewpointF = ConversionMethods.CtoF(extras.dewpointC)
        extras.uvIndex = day.day!!.uv
        extras.pop = day.day!!.dailyChanceOfRain?.toIntOrNull()
                ?: day.day!!.dailyChanceOfSnow?.toIntOrNull()
        extras.qpfRainIn = day.day!!.totalprecipIn
        extras.qpfRainMm = day.day!!.totalprecipMm
        extras.windMph = day.day!!.maxwindMph
        extras.windKph = day.day!!.maxwindKph
        extras.visibilityMi = day.day!!.avgvisMiles
        extras.visibilityKm = day.day!!.avgvisKm
    }
}

fun createHourlyForecast(hour: HourItem, tzId: String): HourlyForecast {
    return HourlyForecast().apply {
        date = ZonedDateTime.of(
                LocalDateTime.parse(hour.time, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
            ZoneIdCompat.of(tzId)
        )

        highF = hour.tempF
        highC = hour.tempC

        condition = hour.condition!!.text
        icon = weatherModule.weatherManager.getWeatherProvider(WeatherAPI.WEATHERAPI)
            .getWeatherIcon(hour.isDay == 0, hour.condition!!.code!!.toString())

        windMph = hour.windMph
        windKph = hour.windKph
        windDegrees = hour.windDegree

        extras = ForecastExtras()
        extras.feelslikeF = hour.feelslikeF
        extras.feelslikeC = hour.feelslikeC
        extras.humidity = hour.humidity
        extras.dewpointF = hour.dewpointF
        extras.dewpointC = hour.dewpointC
        extras.uvIndex = hour.uv
        extras.pop = hour.chanceOfRain?.toIntOrNull() ?: hour.chanceOfSnow?.toIntOrNull()
        extras.cloudiness = hour.cloud
        extras.qpfRainIn = hour.precipIn
        extras.qpfRainMm = hour.precipMm
        extras.pressureIn = hour.pressureIn
        extras.pressureMb = hour.pressureMb
        extras.windDegrees = windDegrees
        extras.windMph = windMph
        extras.windKph = windKph
        extras.visibilityMi = hour.visMiles
        extras.visibilityKm = hour.visKm
        extras.windGustMph = hour.gustMph
        extras.windGustKph = hour.gustKph
    }
}

fun createCondition(current: Current, tzId: String): Condition {
    return Condition().apply {
        weather = current.condition?.text

        tempF = current.tempF
        tempC = current.tempC

        windDegrees = current.windDegree
        windMph = current.windMph
        windKph = current.windKph

        windGustMph = current.gustMph
        windGustKph = current.gustKph

        feelslikeF = current.feelslikeF
        feelslikeC = current.feelslikeC

        icon = weatherModule.weatherManager.getWeatherProvider(WeatherAPI.WEATHERAPI)
            .getWeatherIcon(current.isDay == 0, current.condition!!.code!!.toString())

        beaufort = Beaufort(getBeaufortScale(windMph.toInt()))
        uv = UV(current.uv!!)

        airQuality = createAirQuality(current.airQuality)

        observationTime = ZonedDateTime.of(
            LocalDateTime.parse(
                current.lastUpdated,
                DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm")
            ),
            ZoneIdCompat.of(tzId)
        )
    }
}

fun createAtmosphere(current: Current): Atmosphere {
    return Atmosphere().apply {
        humidity = current.humidity

        pressureMb = current.pressureMb
        pressureIn = current.pressureIn
        pressureTrend = ""

        visibilityMi = current.visMiles
        visibilityKm = current.visKm
    }
}

fun createAstronomy(astro: Astro): Astronomy {
    return Astronomy().apply {
        val now = LocalDate.now()

        runCatching {
            sunrise = LocalTime.parse(astro.sunrise, DateTimeFormatter.ofPattern("hh:mm a")).atDate(now)
        }

        runCatching {
            sunset =
                LocalTime.parse(astro.sunset, DateTimeFormatter.ofPattern("hh:mm a")).atDate(now)
            if (sunrise != null && sunset.isBefore(sunrise)) {
                // Is next day
                sunset = LocalTime.parse(astro.sunset, DateTimeFormatter.ofPattern("hh:mm a"))
                    .atDate(now.plusDays(1))
            }
        }

        runCatching {
            moonrise = LocalTime.parse(astro.moonrise, DateTimeFormatter.ofPattern("hh:mm a")).atDate(now)
        }

        runCatching {
            moonset = LocalTime.parse(astro.moonset, DateTimeFormatter.ofPattern("hh:mm a")).atDate(now)
        }

        when (astro.moonPhase) {
            "New Moon" -> moonPhase = MoonPhase(MoonPhase.MoonPhaseType.NEWMOON)
            "Waxing Crescent" -> moonPhase = MoonPhase(MoonPhase.MoonPhaseType.WAXING_CRESCENT)
            "First Quarter" -> moonPhase = MoonPhase(MoonPhase.MoonPhaseType.FIRST_QTR)
            "Waxing Gibbous" -> moonPhase = MoonPhase(MoonPhase.MoonPhaseType.WAXING_GIBBOUS)
            "Full Moon" -> moonPhase = MoonPhase(MoonPhase.MoonPhaseType.FULL_MOON)
            "Waning Gibbous" -> moonPhase = MoonPhase(MoonPhase.MoonPhaseType.WANING_GIBBOUS)
            "Last Quarter" -> moonPhase = MoonPhase(MoonPhase.MoonPhaseType.LAST_QTR)
            "Waning Crescent" -> moonPhase = MoonPhase(MoonPhase.MoonPhaseType.WANING_CRESCENT)
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
        cloudiness = current.cloud

        qpfRainIn = current.precipIn
        qpfRainMm = current.precipMm
    }
}

fun createAirQuality(airQuality: com.thewizrd.weather_api.weatherapi.weather.AirQuality?): AirQuality? {
    if (airQuality == null) return null

    return AirQuality().apply {
        co = airQuality.co?.let { runCatching { AQICO(CO_ugm3_TO_ppm(it)) }.getOrNull() }
        no2 = airQuality.no2?.let { runCatching { AQINO2(NO2_ugm3_to_ppb(it)) }.getOrNull() }
        o3 = airQuality.o3?.let { runCatching { AQIO3(O3_ugm3_to_ppb(it)) }.getOrNull() }
        so2 = airQuality.so2?.let { runCatching { AQISO2(SO2_ugm3_to_ppb(it)) }.getOrNull() }
        pm25 = airQuality.pm25?.let { runCatching { AQIPM2_5(it) }.getOrNull() }
        pm10 = airQuality.pm10?.let { runCatching { AQIPM10(it) }.getOrNull() }

        index = getIndexFromData()
    }
}