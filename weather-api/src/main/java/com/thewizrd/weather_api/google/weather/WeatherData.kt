package com.thewizrd.weather_api.google.weather

import com.thewizrd.shared_resources.R
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.ConversionMethods
import com.thewizrd.shared_resources.utils.DateTimeUtils
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
import com.thewizrd.shared_resources.weatherdata.model.MinutelyForecast
import com.thewizrd.shared_resources.weatherdata.model.MoonPhase
import com.thewizrd.shared_resources.weatherdata.model.MoonPhase.MoonPhaseType
import com.thewizrd.shared_resources.weatherdata.model.TextForecast
import com.thewizrd.shared_resources.weatherdata.model.UV
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.weather_api.weatherModule
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

fun createWeatherData(
    current: CurrentResponse,
    daily: DailyResponse,
    hourly: HourlyResponse,
    minutely: MinutelyResponse? = null,
    alerts: AlertsResponse? = null
): Weather {
    return Weather().apply {

        val now = ZonedDateTime.parse(current.currentTime)
        var todaysForecast: Pair<ForecastDaysItem, Forecast>? = null
        var todaysTxtForecast: TextForecast? = null

        location = createLocation(current)
        updateTime = now

        // Forecast
        forecast = ArrayList<Forecast>().apply {
            daily.forecastDays?.size?.run {
                ensureCapacity(this)
            }
        }
        txtForecast = ArrayList<TextForecast>().apply {
            daily.forecastDays?.size?.run {
                ensureCapacity(this)
            }
        }

        daily.forecastDays?.forEach { day ->
            val dailyFcast = createForecast(day)
            val txtFcast = createTextForecast(day)

            if (todaysForecast == null && now.toLocalDate()
                    .isEqual(dailyFcast.date.toLocalDate())
            ) {
                todaysForecast = day to dailyFcast
                todaysTxtForecast = txtFcast
            }

            forecast!!.add(dailyFcast)
            txtForecast!!.add(txtFcast)
        }

        // Hourly Forecast
        hrForecast = hourly.forecastHours?.map {
            createHourlyForecast(it)
        }

        minForecast = minutely?.segments?.map {
            createMinutelyForecast(it)
        }

        condition = createCondition(current, todaysForecast?.second, todaysTxtForecast)
        atmosphere = createAtmosphere(current)
        astronomy = createAstronomy(todaysForecast?.first)
        precipitation = createPrecipitation(current)
        weatherAlerts = createWeatherAlerts(alerts)
        ttl = 60 // TODO: TBD

        if ((condition!!.highF == null || condition!!.highC == null) && forecast!!.isNotEmpty()) {
            condition!!.highF = forecast!![0].highF
            condition!!.highC = forecast!![0].highC
            condition!!.lowF = forecast!![0].lowF
            condition!!.lowC = forecast!![0].lowC
        }

        source = WeatherAPI.GOOGLE
    }
}

fun createLocation(current: CurrentResponse): Location {
    return Location().apply {
        /* Use name, latlng from location provider */
        name = null
        latitude = null
        longitude = null
        tzLong = current.timeZone?.id
    }
}

fun createCondition(
    current: CurrentResponse,
    todaysForecast: Forecast?,
    todaysTxtForecast: TextForecast?
): Condition {
    return Condition().apply {
        val wm = weatherModule.weatherManager.getWeatherProvider(WeatherAPI.GOOGLE)

        icon = wm.getWeatherIcon(current.isDaytime?.not() ?: false, current.weatherCondition?.type)
        weather = current.weatherCondition?.description?.text

        current.temperature?.degrees?.let {
            tempC = it
            tempF = ConversionMethods.CtoF(it)
        }

        windDegrees = current.wind?.direction?.degrees
        current.wind?.speed?.value?.let {
            windKph = it
            windMph = ConversionMethods.kphTomph(it)
        }

        current.wind?.gust?.value?.let {
            windGustMph = it
            windGustKph = ConversionMethods.kphTomph(it)
        }

        current.feelsLikeTemperature?.degrees?.let {
            feelslikeC = it
            feelslikeF = ConversionMethods.CtoF(it)
        }

        beaufort = Beaufort(getBeaufortScale(windMph.toInt()))
        current.uvIndex?.let { uv = UV(it) }
        observationTime = ZonedDateTime.parse(current.currentTime)

        current.currentConditionsHistory?.maxTemperature?.degrees?.let {
            highC = it
            highF = ConversionMethods.CtoF(it)
        } ?: run {
            highC = todaysForecast?.highC
            highF = todaysForecast?.highF
        }
        current.currentConditionsHistory?.minTemperature?.degrees?.let {
            lowC = it
            lowF = ConversionMethods.CtoF(it)
        } ?: run {
            lowC = todaysForecast?.lowC
            lowF = todaysForecast?.lowF
        }

        // fcttext & fcttextMetric are the same
        summary = todaysTxtForecast?.fcttext
    }
}

fun createForecast(day: ForecastDaysItem): Forecast {
    return Forecast().apply {
        date = ZonedDateTime.parse(day.interval?.startTime).toLocalDateTime()

        day.maxTemperature?.degrees?.let {
            highC = it
            highF = ConversionMethods.CtoF(it)
        }

        day.minTemperature?.degrees?.let {
            lowC = it
            lowF = ConversionMethods.CtoF(it)
        }

        val wm = weatherModule.weatherManager.getWeatherProvider(WeatherAPI.GOOGLE)

        icon = day.daytimeForecast?.weatherCondition?.type?.let {
            wm.getWeatherIcon(false, it)
        } ?: day.nighttimeForecast?.weatherCondition?.type?.let {
            wm.getWeatherIcon(true, it)
        }
        condition = day.daytimeForecast?.weatherCondition?.description?.text
            ?: day.nighttimeForecast?.weatherCondition?.description?.text

        extras = ForecastExtras()
        extras.humidity = day.daytimeForecast?.relativeHumidity
        extras.uvIndex = day.daytimeForecast?.uvIndex
        extras.pop = day.daytimeForecast?.precipitation?.probability?.percent
        day.daytimeForecast?.precipitation?.qpf?.quantity?.let {
            extras.qpfRainMm = it
            extras.qpfRainIn = ConversionMethods.mmToIn(it)
        }
        extras.windDegrees = day.daytimeForecast?.wind?.direction?.degrees
        day.daytimeForecast?.wind?.speed?.value?.let {
            extras.windKph = it
            extras.windMph = ConversionMethods.kphTomph(it)
        }
        day.daytimeForecast?.wind?.gust?.value?.let {
            extras.windGustKph = it
            extras.windGustMph = ConversionMethods.kphTomph(it)
        }
        extras.cloudiness = day.daytimeForecast?.cloudCover
        day.feelsLikeMaxTemperature?.degrees?.let {
            extras.feelslikeC = it
            extras.feelslikeF = ConversionMethods.CtoF(it)
        }
    }
}

fun createTextForecast(day: ForecastDaysItem): TextForecast {
    return TextForecast().apply {
        date = ZonedDateTime.parse(day.interval?.startTime)

        fcttext = StringBuilder().apply {
            val ctx = sharedDeps.context

            day.daytimeForecast?.let { dayCast ->
                append("${ctx.getString(R.string.label_day)}: ")
                dayCast.weatherCondition?.description?.text?.let {
                    append("$it. ")
                }
                dayCast.precipitation?.probability?.percent?.let {
                    append("${ctx.getString(R.string.label_chance)}: ${it}% ")
                }
                appendLine()
            }

            day.nighttimeForecast?.let { nightCast ->
                append("${ctx.getString(R.string.label_night)}: ")
                nightCast.weatherCondition?.description?.text?.let {
                    append("$it. ")
                }
                nightCast.precipitation?.probability?.percent?.let {
                    append("${ctx.getString(R.string.label_chance)}: ${it}% ")
                }
                appendLine()
            }
        }.toString()

        fcttextMetric = fcttext
    }
}

fun createHourlyForecast(hour: ForecastHoursItem): HourlyForecast {
    return HourlyForecast().apply {
        date = ZonedDateTime.parse(hour.interval?.startTime)

        hour.temperature?.degrees?.let {
            highC = it
            highF = ConversionMethods.CtoF(it)
        }

        val wm = weatherModule.weatherManager.getWeatherProvider(WeatherAPI.GOOGLE)

        icon = wm.getWeatherIcon(hour.isDaytime?.not() ?: false, hour.weatherCondition?.type)
        condition = hour.weatherCondition?.description?.text

        extras = ForecastExtras()
        hour.feelsLikeTemperature?.degrees?.let {
            extras.feelslikeC = it
            extras.feelslikeF = ConversionMethods.CtoF(it)
        }
        hour.dewPoint?.degrees?.let {
            extras.dewpointC = it
            extras.dewpointF = ConversionMethods.CtoF(it)
        }
        extras.humidity = hour.relativeHumidity
        extras.uvIndex = hour.uvIndex
        extras.pop = hour.precipitation?.probability?.percent
        hour.precipitation?.qpf?.quantity?.let {
            extras.qpfRainMm = it
            extras.qpfRainIn = ConversionMethods.mmToIn(it)
        }
        hour.airPressure?.meanSeaLevelMillibars?.let {
            extras.pressureMb = it
            extras.pressureIn = ConversionMethods.mbToInHg(it)
        }
        extras.windDegrees = hour.wind?.direction?.degrees
        hour.wind?.speed?.value?.let {
            extras.windKph = it
            extras.windMph = ConversionMethods.kphTomph(it)
        }
        hour.wind?.gust?.value?.let {
            extras.windGustKph = it
            extras.windGustMph = ConversionMethods.kphTomph(it)
        }
        hour.visibility?.distance?.let {
            extras.visibilityKm = it
            extras.visibilityMi = ConversionMethods.kmToMi(it)
        }
        extras.cloudiness = hour.cloudCover
    }
}

fun createMinutelyForecast(minute: SegmentsItem): MinutelyForecast {
    return MinutelyForecast().apply {
        date = ZonedDateTime.parse(minute.timeFrame?.startTime).withZoneSameInstant(ZoneOffset.UTC)
        rainMm = minute.qpf?.quantity
        snowMm = minute.snowfallAmount?.quantity
    }
}

fun createAtmosphere(current: CurrentResponse): Atmosphere {
    return Atmosphere().apply {
        current.dewPoint?.degrees?.let {
            dewpointC = it
            dewpointF = ConversionMethods.CtoF(it)
        }

        humidity = current.relativeHumidity

        current.airPressure?.meanSeaLevelMillibars?.let {
            pressureMb = it
            pressureIn = ConversionMethods.mbToInHg(it)
        }

        current.visibility?.distance?.let {
            visibilityKm = it
            visibilityMi = ConversionMethods.kmToMi(it)
        }
    }
}

fun createAstronomy(day: ForecastDaysItem?): Astronomy {
    return Astronomy().apply {
        runCatching {
            sunrise = day?.sunEvents?.sunriseTime?.let {
                ZonedDateTime.parse(it).toLocalDateTime()
            }
        }

        runCatching {
            sunset = day?.sunEvents?.sunsetTime?.let {
                ZonedDateTime.parse(it).toLocalDateTime()
            }
        }

        runCatching {
            moonrise = day?.moonEvents?.moonriseTimes?.firstOrNull()?.let {
                ZonedDateTime.parse(it).toLocalDateTime()
            }
        }

        runCatching {
            moonset = day?.moonEvents?.moonsetTimes?.firstOrNull()?.let {
                ZonedDateTime.parse(it).toLocalDateTime()
            }
        }

        moonPhase = when (day?.moonEvents?.moonPhase) {
            "NEW_MOON" -> MoonPhase(MoonPhaseType.NEWMOON)
            "WAXING_CRESCENT" -> MoonPhase(MoonPhaseType.WAXING_CRESCENT)
            "FIRST_QUARTER" -> MoonPhase(MoonPhaseType.FIRST_QTR)
            "WAXING_GIBBOUS" -> MoonPhase(MoonPhaseType.WAXING_GIBBOUS)
            "FULL_MOON" -> MoonPhase(MoonPhaseType.FULL_MOON)
            "WANING_GIBBOUS" -> MoonPhase(MoonPhaseType.WANING_GIBBOUS)
            "LAST_QUARTER" -> MoonPhase(MoonPhaseType.LAST_QTR)
            "WANING_CRESCENT" -> MoonPhase(MoonPhaseType.WANING_CRESCENT)
            else -> null
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

fun createPrecipitation(current: CurrentResponse): com.thewizrd.shared_resources.weatherdata.model.Precipitation {
    return com.thewizrd.shared_resources.weatherdata.model.Precipitation().apply {
        pop = current.precipitation?.probability?.percent

        current.precipitation?.qpf?.quantity?.let {
            qpfRainMm = it
            qpfRainIn = ConversionMethods.mmToIn(it)
        }

        cloudiness = current.cloudCover
    }
}