package com.thewizrd.weather_api.weatherkit

import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.ConversionMethods
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.utils.getBeaufortScale
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.model.*
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.shared_resources.weatherdata.model.MoonPhase.MoonPhaseType
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.weather_api.R
import com.thewizrd.weather_api.weatherModule
import java.time.LocalDateTime
import java.time.ZonedDateTime
import kotlin.math.roundToInt

fun createWeatherData(root: com.thewizrd.weather_api.weatherkit.Weather): Weather {
    return Weather().apply {

        val now = ZonedDateTime.parse(root.currentWeather!!.metadata.readTime)
        var todaysForecast: Forecast? = null
        var todaysTxtForecast: TextForecast? = null

        location = createLocation(root.currentWeather!!)
        updateTime = now

        forecast = ArrayList<Forecast>().apply {
            root.forecastDaily?.days?.size?.run {
                ensureCapacity(this)
            }
        }
        txtForecast = ArrayList<TextForecast>().apply {
            root.forecastDaily?.days?.size?.run {
                ensureCapacity(this)
            }
        }
        hrForecast = ArrayList<HourlyForecast>().apply {
            root.forecastHourly?.hours?.size?.run {
                ensureCapacity(this)
            }
        }
        minForecast = ArrayList<MinutelyForecast>().apply {
            root.forecastNextHour?.minutes?.size?.run {
                ensureCapacity(this)
            }
        }

        // Forecast
        root.forecastDaily?.days?.forEach { day ->
            val dailyFcast = createForecast(day)
            val txtFcast = createTextForecast(day)

            forecast.add(dailyFcast)
            txtForecast.add(txtFcast)

            if (todaysForecast == null && dailyFcast.date.toLocalDate()
                    .isEqual(now.toLocalDate())
            ) {
                todaysForecast = dailyFcast
                todaysTxtForecast = txtFcast
            }
        }

        // Hourly Forecast
        root.forecastHourly?.hours?.forEach { hour ->
            hrForecast.add(createHourlyForecast(hour))
        }

        // Minutely Forecast
        root.forecastNextHour?.minutes?.forEach { minute ->
            minForecast.add(createMinutelyForecast(minute))
        }

        condition = createCondition(root.currentWeather!!, todaysForecast, todaysTxtForecast)
        atmosphere = createAtmosphere(root.currentWeather!!)

        root.forecastDaily?.days?.firstOrNull {
            ZonedDateTime.parse(it.forecastStart).toLocalDate()
                .isEqual(condition.observationTime.toLocalDate())
        }?.let {
            astronomy = createAstronomy(it)
        }
        precipitation = createPrecipitation(root.currentWeather!!)
        ttl = 180

        if ((condition.highF == null || condition.highC == null) && forecast.size > 0) {
            condition.highF = forecast[0].highF
            condition.highC = forecast[0].highC
            condition.lowF = forecast[0].lowF
            condition.lowC = forecast[0].lowC
        }

        weatherAlerts = createWeatherAlerts(root.weatherAlerts)

        source = WeatherAPI.APPLE
    }
}

fun createLocation(current: CurrentWeather): Location {
    return Location().apply {
        /* Use name from location provider */
        name = null
        latitude = current.metadata.latitude
        longitude = current.metadata.longitude
        tzLong = null
    }
}

fun createForecast(day: DayWeatherConditions): Forecast {
    return Forecast().apply {
        date = ZonedDateTime.parse(day.forecastStart).toLocalDateTime()

        highC = day.temperatureMax
        lowC = day.temperatureMin
        highF = ConversionMethods.CtoF(day.temperatureMax)
        lowF = ConversionMethods.CtoF(day.temperatureMin)

        icon = weatherModule.weatherManager.getWeatherProvider(WeatherAPI.APPLE)
            .getWeatherIcon(day.conditionCode)
        condition = weatherModule.weatherManager.getWeatherProvider(WeatherAPI.APPLE)
            .getWeatherCondition(day.conditionCode)

        extras = ForecastExtras()

        extras.humidity = day.daytimeForecast?.humidity?.times(100)?.roundToInt()
        extras.uvIndex = day.maxUvIndex.toFloat()
        extras.pop = day.precipitationChance.times(100).roundToInt()
        extras.qpfRainMm = day.precipitationAmount
        extras.qpfRainIn = ConversionMethods.mmToIn(day.precipitationAmount)
        extras.windKph = day.daytimeForecast?.windSpeed
        extras.windMph = day.daytimeForecast?.windSpeed?.let { ConversionMethods.kphTomph(it) }
        extras.windDegrees = day.daytimeForecast?.windDirection
        extras.cloudiness = day.daytimeForecast?.cloudCover?.times(100)?.roundToInt()
    }
}

fun createHourlyForecast(hour: HourWeatherConditions): HourlyForecast {
    return HourlyForecast().apply {
        date = ZonedDateTime.parse(hour.forecastStart)

        highC = hour.temperature
        highF = ConversionMethods.CtoF(hour.temperature)

        icon = weatherModule.weatherManager.getWeatherProvider(WeatherAPI.APPLE)
            .getWeatherIcon(hour.daylight?.not() ?: false, hour.conditionCode)
        condition = weatherModule.weatherManager.getWeatherProvider(WeatherAPI.APPLE)
            .getWeatherCondition(hour.conditionCode)

        windKph = hour.windSpeed
        windMph = ConversionMethods.kphTomph(hour.windSpeed)
        windDegrees = hour.windDirection

        extras = ForecastExtras().apply {
            feelslikeC = hour.temperatureApparent
            feelslikeF = ConversionMethods.CtoF(hour.temperatureApparent)
            humidity = hour.humidity.times(100).roundToInt()
            dewpointC = hour.temperatureDewPoint
            dewpointF = hour.temperatureDewPoint?.let { ConversionMethods.CtoF(it) }
            uvIndex = hour.uvIndex.toFloat()
            pop = hour.precipitationChance.times(100).roundToInt()
            cloudiness = hour.cloudCover.times(100).roundToInt()
            qpfRainMm =
                if (hour.precipitationType != PrecipitationType.SNOW) hour.precipitationAmount else null
            qpfRainIn =
                if (hour.precipitationType != PrecipitationType.SNOW) hour.precipitationAmount?.let {
                    ConversionMethods.mmToIn(it)
                } else null
            qpfSnowCm =
                if (hour.precipitationType == PrecipitationType.SNOW) hour.precipitationAmount?.times(
                    10
                ) else null
            qpfSnowIn =
                if (hour.precipitationType == PrecipitationType.SNOW) hour.precipitationAmount?.let {
                    ConversionMethods.mmToIn(it)
                } else null
            pressureMb = hour.pressure
            pressureIn = ConversionMethods.mbToInHg(hour.pressure)
            windDegrees = hour.windDirection
            windKph = hour.windSpeed
            windMph = ConversionMethods.kphToMsec(hour.windSpeed)
            visibilityKm = hour.visibility / 1000
            visibilityMi = ConversionMethods.kmToMi(hour.visibility / 1000)
            windGustKph = hour.windGust
            windGustMph = hour.windGust?.let { ConversionMethods.kphTomph(it) }
        }
    }
}

fun createMinutelyForecast(minute: ForecastMinute): MinutelyForecast {
    return MinutelyForecast().apply {
        date = ZonedDateTime.parse(minute.startTime)
        rainMm = minute.precipitationIntensity
    }
}

fun createTextForecast(day: DayWeatherConditions): TextForecast {
    return TextForecast().apply {
        date = ZonedDateTime.parse(day.forecastStart)

        fcttext = StringBuilder().apply {
            val ctx = sharedDeps.context

            day.daytimeForecast?.let {
                val dayConditionText =
                    weatherModule.weatherManager.getWeatherProvider(WeatherAPI.APPLE)
                        .getWeatherCondition(it.conditionCode)
                append("${ctx.getString(R.string.label_day)}: $dayConditionText;")
                appendLine(
                    " ${ctx.getString(R.string.label_chance)}: ${
                        it.precipitationChance.times(
                            100
                        ).roundToInt()
                    }%"
                )
            }

            day.overnightForecast?.let {
                val ntConditionText =
                    weatherModule.weatherManager.getWeatherProvider(WeatherAPI.APPLE)
                        .getWeatherCondition(it.conditionCode)
                append("${ctx.getString(R.string.label_night)}: $ntConditionText;")
                appendLine(
                    " ${ctx.getString(R.string.label_chance)}: ${
                        it.precipitationChance.times(
                            100
                        ).roundToInt()
                    }%"
                )
            }
        }.toString()

        fcttextMetric = fcttext
    }
}

fun createCondition(
    current: CurrentWeather,
    todaysForecast: Forecast?,
    todaysTxtForecast: TextForecast?
): Condition {
    return Condition().apply {
        weather = weatherModule.weatherManager.getWeatherProvider(WeatherAPI.APPLE)
            .getWeatherCondition(current.conditionCode)

        tempC = current.temperature
        tempF = ConversionMethods.CtoF(current.temperature)

        windDegrees = current.windDirection
        windKph = current.windSpeed
        windMph = ConversionMethods.kphTomph(current.windSpeed)

        windGustMph = current.windGust
        windGustKph = current.windGust?.let { ConversionMethods.kphTomph(it) }

        feelslikeC = current.temperatureApparent
        feelslikeF = ConversionMethods.CtoF(current.temperatureApparent)

        icon = weatherModule.weatherManager.getWeatherProvider(WeatherAPI.APPLE)
            .getWeatherIcon(current.daylight?.not() ?: false, current.conditionCode)

        beaufort = Beaufort(getBeaufortScale(windMph.toInt()))
        uv = UV(current.uvIndex.toFloat())
        observationTime = ZonedDateTime.parse(current.asOf)

        // fcttext & fcttextMetric are the same
        summary = todaysTxtForecast?.fcttext

        highC = todaysForecast?.highC
        highF = todaysForecast?.highF
        lowC = todaysForecast?.lowC
        lowF = todaysForecast?.lowF
    }
}

fun createAtmosphere(current: CurrentWeather): Atmosphere {
    return Atmosphere().apply {
        humidity = current.humidity.times(100).roundToInt()

        pressureMb = current.pressure
        pressureIn = ConversionMethods.mbToInHg(current.pressure)
        pressureTrend = when (current.pressureTrend) {
            PressureTrend.RISING -> "+"
            PressureTrend.FALLING -> "-"
            else -> ""
        }

        visibilityKm = current.visibility / 1000
        visibilityMi = ConversionMethods.kmToMi(current.visibility / 1000)

        dewpointC = current.temperatureDewPoint
        dewpointF = ConversionMethods.CtoF(current.temperatureDewPoint)
    }
}

fun createAstronomy(day: DayWeatherConditions): Astronomy {
    return Astronomy().apply {
        runCatching {
            sunrise = ZonedDateTime.parse(day.sunrise).toLocalDateTime()
        }

        runCatching {
            sunset = ZonedDateTime.parse(day.sunset).toLocalDateTime()
        }

        runCatching {
            moonrise = ZonedDateTime.parse(day.moonrise).toLocalDateTime()
        }

        runCatching {
            moonset = ZonedDateTime.parse(day.moonset).toLocalDateTime()
        }

        moonPhase = when (day.moonPhase) {
            MoonPhase.NEW -> MoonPhase(MoonPhaseType.NEWMOON)
            MoonPhase.WAXING_CRESCENT -> MoonPhase(MoonPhaseType.WAXING_CRESCENT)
            MoonPhase.FIRST_QUARTER -> MoonPhase(MoonPhaseType.FIRST_QTR)
            MoonPhase.WAXING_GIBBOUS -> MoonPhase(MoonPhaseType.WAXING_GIBBOUS)
            MoonPhase.FULL -> MoonPhase(MoonPhaseType.FULL_MOON)
            MoonPhase.WANING_GIBBOUS -> MoonPhase(MoonPhaseType.WANING_GIBBOUS)
            MoonPhase.THIRD_QUARTER -> MoonPhase(MoonPhaseType.LAST_QTR)
            MoonPhase.WANING_CRESCENT -> MoonPhase(MoonPhaseType.WANING_CRESCENT)
            else -> MoonPhase(MoonPhaseType.NEWMOON)
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
    }
}

fun createPrecipitation(current: CurrentWeather): Precipitation {
    return Precipitation().apply {
        cloudiness = current.cloudCover?.times(100)?.roundToInt()

        qpfRainIn = current.precipitationIntensity
        qpfRainMm = ConversionMethods.mmToIn(current.precipitationIntensity)
    }
}