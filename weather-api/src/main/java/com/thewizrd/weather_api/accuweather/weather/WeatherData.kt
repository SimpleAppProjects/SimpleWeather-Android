package com.thewizrd.weather_api.accuweather.weather

import android.annotation.SuppressLint
import com.thewizrd.shared_resources.R
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.ConversionMethods
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.utils.getBeaufortScale
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.model.*
import com.thewizrd.shared_resources.weatherdata.model.Precipitation
import com.thewizrd.weather_api.weatherModule
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*
import kotlin.math.roundToInt

@SuppressLint("VisibleForTests")
fun createWeatherData(dailyRoot: DailyResponse, hourlyRoot: HourlyResponse, currentRoot: CurrentsResponse): Weather {
    return Weather().apply {
        val currentItem = currentRoot.currentsResponse!![0]!!
        val observationTime = ZonedDateTime.parse(currentItem.localObservationDateTime)
        val now = ZonedDateTime.now().withZoneSameInstant(observationTime.offset)

        updateTime = now

        location = createLocation(currentRoot)

        forecast = ArrayList(dailyRoot.dailyForecasts!!.size)
        txtForecast = ArrayList(dailyRoot.dailyForecasts!!.size)
        var haveTodaysForecast = false
        for (fcast in dailyRoot.dailyForecasts!!) {

            if (!haveTodaysForecast && ZonedDateTime.parse(fcast!!.date).toLocalDate().isEqual(now.toLocalDate())) {
                astronomy = createAstronomy(fcast)
                condition = createCondition(currentItem, fcast)
                haveTodaysForecast = true
            }

            forecast.add(createForecast(fcast!!))
            txtForecast.add(createTextForecast(fcast))
        }

        hrForecast = ArrayList(hourlyRoot.hourlyResponse!!.size)
        for (fcast in hourlyRoot.hourlyResponse!!) {
            hrForecast.add(createHourlyForecast(fcast!!))
        }

        if (condition == null) {
            condition = createCondition(currentItem)
        }
        atmosphere = createAtmosphere(currentItem)
        precipitation = createPrecipitation(currentItem)

        // Weather summary
        if (dailyRoot.headline != null && dailyRoot.headline!!.effectiveEpochDate != null && dailyRoot.headline!!.endEpochDate != null) {
            val effectiveDate = ZonedDateTime.ofInstant(
                Instant.ofEpochSecond(dailyRoot.headline!!.effectiveEpochDate!!), ZoneOffset.UTC
            )
                .withZoneSameInstant(observationTime.offset)
            val endDate = ZonedDateTime.ofInstant(
                Instant.ofEpochSecond(dailyRoot.headline!!.endEpochDate!!), ZoneOffset.UTC
            )
                .withZoneSameInstant(observationTime.offset)

            if (!observationTime.isBefore(effectiveDate) && !observationTime.isAfter(endDate)) {
                condition.summary = dailyRoot.headline?.text
            }
        }

        ttl = 180
        source = WeatherAPI.ACCUWEATHER
    }
}

fun createLocation(current: CurrentsResponse): Location {
    return Location().apply {
        // Use location name from location provider
        name = null
        latitude = null
        longitude = null
        tzLong = null
    }
}

fun createForecast(daily: DailyForecastsItem): Forecast {
    return Forecast().apply {
        date = ZonedDateTime.parse(daily.date!!).toLocalDateTime()

        highC = daily.temperature!!.maximum!!.value
        highF = ConversionMethods.CtoF(daily.temperature!!.maximum!!.value!!)

        lowC = daily.temperature!!.minimum!!.value
        lowF = ConversionMethods.CtoF(daily.temperature!!.minimum!!.value!!)

        condition = daily.day?.iconPhrase ?: daily.night?.iconPhrase
        icon = weatherModule.weatherManager.getWeatherProvider(WeatherAPI.ACCUWEATHER)
            .getWeatherIcon(false, daily.day?.icon?.toString() ?: daily.night?.icon?.toString())

        extras = ForecastExtras()

        extras.feelslikeC = daily.realFeelTemperature?.maximum?.value
                ?: daily.realFeelTemperature?.minimum?.value
        if (extras.feelslikeC != null) extras.feelslikeF = ConversionMethods.CtoF(extras.feelslikeC)

        extras.uvIndex = daily.airAndPollen?.find { it?.name == "UVIndex" }?.value?.toFloat()
        extras.pop = daily.day?.precipitationProbability ?: daily.night?.precipitationProbability
        extras.cloudiness = daily.day?.cloudCover ?: daily.night?.cloudCover

        extras.qpfRainMm = daily.day?.rain?.value ?: daily.night?.rain?.value
        if (extras.qpfRainMm != null) extras.qpfRainIn = ConversionMethods.mmToIn(extras.qpfRainMm)
        extras.qpfSnowCm = daily.day?.snow?.value ?: daily.night?.snow?.value
        if (extras.qpfSnowCm != null) extras.qpfSnowIn = ConversionMethods.mmToIn(extras.qpfSnowCm * 10)

        if (daily.day?.wind != null) {
            extras.windDegrees = daily.day?.wind?.direction?.degrees?.roundToInt()
            extras.windKph = daily.day?.wind?.speed?.value
            if (extras.windKph != null) extras.windMph = ConversionMethods.kphTomph(extras.windKph)
        } else if (daily.night?.wind != null) {
            extras.windDegrees = daily.night?.wind?.direction?.degrees?.roundToInt()
            extras.windKph = daily.night?.wind?.speed?.value
            if (extras.windKph != null) extras.windMph = ConversionMethods.kphTomph(extras.windKph)
        }

        if (daily.day?.windGust != null) {
            extras.windGustKph = daily.day?.windGust?.speed?.value
            if (extras.windGustKph != null) extras.windGustMph = ConversionMethods.kphTomph(extras.windGustKph)
        } else if (daily.night?.windGust != null) {
            extras.windGustKph = daily.night?.windGust?.speed?.value
            if (extras.windGustKph != null) extras.windGustMph = ConversionMethods.kphTomph(extras.windGustKph)
        }
    }
}

fun createTextForecast(daily: DailyForecastsItem): TextForecast {
    return TextForecast().apply {
        val context = sharedDeps.context

        date = ZonedDateTime.parse(daily.date!!)

        val fctStr = StringBuilder()
        if (daily.day != null) {
            fctStr.append("${context.getString(R.string.label_day)} - ${daily.day!!.longPhrase}")
        }
        if (daily.night != null) {
            if (fctStr.isNotEmpty()) fctStr.appendLine()
            fctStr.append("${context.getString(R.string.label_night)} - ${daily.night!!.longPhrase}")
        }

        fcttext = fctStr.toString()
        fcttextMetric = fcttext
    }
}

fun createHourlyForecast(hourly: HourlyResponseItem): HourlyForecast {
    return HourlyForecast().apply {
        date = ZonedDateTime.parse(hourly.dateTime)

        highC = hourly.temperature?.value
        highF = hourly.temperature?.value?.let { ConversionMethods.CtoF(it) }

        condition = hourly.iconPhrase
        icon = weatherModule.weatherManager.getWeatherProvider(WeatherAPI.ACCUWEATHER)
            .getWeatherIcon(!hourly.isDaylight!!, hourly.weatherIcon?.toString())

        extras = ForecastExtras()
        hourly.realFeelTemperature?.value?.let {
            extras.feelslikeC = it
            extras.feelslikeF = ConversionMethods.CtoF(it)
        }
        extras.humidity = hourly.relativeHumidity
        hourly.dewPoint?.value?.let {
            extras.dewpointC = it
            extras.dewpointF = ConversionMethods.CtoF(it)
        }

        extras.uvIndex = hourly.uVIndex
        extras.pop = hourly.precipitationProbability
        extras.cloudiness = hourly.cloudCover

        hourly.rain?.value?.let {
            extras.qpfRainMm = it
            extras.qpfRainIn = ConversionMethods.mmToIn(it)
        }
        hourly.snow?.value?.let {
            extras.qpfSnowCm = it
            extras.qpfSnowIn = ConversionMethods.mmToIn(it * 10)
        }

        hourly.wind?.speed?.value?.let {
            extras.windKph = it
            windKph = it

            extras.windMph = ConversionMethods.kphTomph(it)
            windMph = extras.windMph
        }
        extras.windDegrees = hourly.wind?.direction?.degrees?.roundToInt()

        hourly.visibility?.value?.let {
            extras.visibilityKm = it
            extras.visibilityMi = ConversionMethods.kmToMi(it)
        }

        hourly.windGust?.speed?.value?.let {
            extras.windGustKph = it
            extras.windGustMph = ConversionMethods.kphTomph(it)
        }
    }
}

fun createAstronomy(daily: DailyForecastsItem): Astronomy {
    return Astronomy().apply {
        daily.sun?.rise?.let {
            sunrise = ZonedDateTime.parse(it).toLocalDateTime()
        }
        daily.sun?.set?.let {
            sunset = ZonedDateTime.parse(it).toLocalDateTime()
        }
        daily.moon?.rise?.let {
            moonrise = ZonedDateTime.parse(it).toLocalDateTime()
        }
        daily.moon?.set?.let {
            moonset = ZonedDateTime.parse(it).toLocalDateTime()
        }
        daily.moon?.phase?.let {
            moonPhase = MoonPhase(when (it.toLowerCase(Locale.ROOT)) {
                "new", "newmoon" -> MoonPhase.MoonPhaseType.NEWMOON
                "waxingcrescent" -> MoonPhase.MoonPhaseType.WAXING_CRESCENT
                "first", "firstquarter" -> MoonPhase.MoonPhaseType.FIRST_QTR
                "waxinggibbous" -> MoonPhase.MoonPhaseType.WAXING_GIBBOUS
                "full", "fullmoon" -> MoonPhase.MoonPhaseType.FULL_MOON
                "waninggibbous" -> MoonPhase.MoonPhaseType.WANING_GIBBOUS
                "third", "last" -> MoonPhase.MoonPhaseType.LAST_QTR
                "waningcrescent" -> MoonPhase.MoonPhaseType.WANING_CRESCENT
                else -> MoonPhase.MoonPhaseType.NEWMOON
            })
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

fun createCondition(current: CurrentsResponseItem, daily: DailyForecastsItem? = null): Condition {
    return Condition().apply {
        weather = current.weatherText
        icon = weatherModule.weatherManager.getWeatherProvider(WeatherAPI.ACCUWEATHER)
            .getWeatherIcon(!current.isDayTime!!, current.weatherIcon?.toString())

        tempF = current.temperature?.imperial?.value
        tempC = current.temperature?.metric?.value

        windDegrees = current.wind?.direction?.degrees
        windMph = current.wind?.speed?.imperial?.value
        windKph = current.wind?.speed?.metric?.value
        windGustMph = current.windGust?.speed?.imperial?.value
        windGustKph = current.windGust?.speed?.metric?.value

        feelslikeF = current.realFeelTemperature?.imperial?.value
        feelslikeC = current.realFeelTemperature?.metric?.value

        current.wind?.speed?.imperial?.value?.let {
            beaufort = Beaufort(getBeaufortScale(it.roundToInt()))
        }

        current.uVIndex?.let {
            uv = UV(it)
        }

        if (current.temperatureSummary?.past6HourRange?.maximum?.imperial?.value != null &&
                current.temperatureSummary?.past6HourRange?.maximum?.metric?.value != null) {
            highF = current.temperatureSummary?.past6HourRange?.maximum?.imperial?.value
            highC = current.temperatureSummary?.past6HourRange?.maximum?.metric?.value
        } else if (current.temperatureSummary?.past12HourRange?.maximum?.imperial?.value != null &&
                current.temperatureSummary?.past12HourRange?.maximum?.metric?.value != null) {
            highF = current.temperatureSummary?.past12HourRange?.maximum?.imperial?.value
            highC = current.temperatureSummary?.past12HourRange?.maximum?.metric?.value
        } else if (current.temperatureSummary?.past24HourRange?.maximum?.imperial?.value != null &&
                current.temperatureSummary?.past24HourRange?.maximum?.metric?.value != null) {
            highF = current.temperatureSummary?.past24HourRange?.maximum?.imperial?.value
            highC = current.temperatureSummary?.past24HourRange?.maximum?.metric?.value
        }

        if (current.temperatureSummary?.past6HourRange?.minimum?.imperial?.value != null &&
                current.temperatureSummary?.past6HourRange?.minimum?.metric?.value != null) {
            lowF = current.temperatureSummary?.past6HourRange?.minimum?.imperial?.value
            lowC = current.temperatureSummary?.past6HourRange?.minimum?.metric?.value
        } else if (current.temperatureSummary?.past12HourRange?.minimum?.imperial?.value != null &&
                current.temperatureSummary?.past12HourRange?.minimum?.metric?.value != null) {
            lowF = current.temperatureSummary?.past12HourRange?.minimum?.imperial?.value
            lowC = current.temperatureSummary?.past12HourRange?.minimum?.metric?.value
        } else if (current.temperatureSummary?.past24HourRange?.minimum?.imperial?.value != null &&
                current.temperatureSummary?.past24HourRange?.minimum?.metric?.value != null) {
            lowF = current.temperatureSummary?.past24HourRange?.minimum?.imperial?.value
            lowC = current.temperatureSummary?.past24HourRange?.minimum?.metric?.value
        }

        daily?.airAndPollen?.find { it?.name == "AirQuality" }?.value?.let {
            airQuality = AirQuality().apply {
                index = it
            }
        }

        if (!daily?.airAndPollen.isNullOrEmpty()) {
            var treePollenValue: Int? = null
            var grassPollenValue: Int? = null
            var ragweedPollenValue: Int? = null

            daily?.airAndPollen?.forEach {
                if (it?.name == "Grass") {
                    grassPollenValue = it.value
                }
                if (it?.name == "Tree") {
                    treePollenValue = it.value
                }
                if (it?.name == "Ragweed") {
                    ragweedPollenValue = it.value
                }
            }

            if (grassPollenValue != null || treePollenValue != null || ragweedPollenValue != null) {
                pollen = Pollen().apply {
                    treePollenCount = when {
                        treePollenValue != null && treePollenValue in 0..14 -> Pollen.PollenCount.LOW
                        treePollenValue != null && treePollenValue in 15..89 -> Pollen.PollenCount.MODERATE
                        treePollenValue != null && treePollenValue in 90..1499 -> Pollen.PollenCount.HIGH
                        treePollenValue != null && treePollenValue!! >= 1500 -> Pollen.PollenCount.VERY_HIGH
                        else -> Pollen.PollenCount.UNKNOWN
                    }

                    grassPollenCount = when {
                        grassPollenValue != null && grassPollenValue in 0..4 -> Pollen.PollenCount.LOW
                        grassPollenValue != null && grassPollenValue in 5..19 -> Pollen.PollenCount.MODERATE
                        grassPollenValue != null && grassPollenValue in 20..199 -> Pollen.PollenCount.HIGH
                        grassPollenValue != null && grassPollenValue!! >= 200 -> Pollen.PollenCount.VERY_HIGH
                        else -> Pollen.PollenCount.UNKNOWN
                    }

                    ragweedPollenCount = when {
                        ragweedPollenValue != null && ragweedPollenValue in 0..9 -> Pollen.PollenCount.LOW
                        ragweedPollenValue != null && ragweedPollenValue in 10..49 -> Pollen.PollenCount.MODERATE
                        ragweedPollenValue != null && ragweedPollenValue in 50..499 -> Pollen.PollenCount.HIGH
                        ragweedPollenValue != null && ragweedPollenValue!! >= 500 -> Pollen.PollenCount.VERY_HIGH
                        else -> Pollen.PollenCount.UNKNOWN
                    }
                }
            }
        }

        observationTime = ZonedDateTime.parse(current.localObservationDateTime)

        summary = daily?.let {
            val ctx = sharedDeps.context
            val labelDay = ctx.getString(R.string.label_day)
            val labelNite = ctx.getString(R.string.label_night)

            val strBuilder = StringBuilder()
            if (!it.day?.longPhrase.isNullOrBlank()) {
                strBuilder.append("$labelDay - ${it.day!!.longPhrase}")
            }
            if (!it.night?.longPhrase.isNullOrBlank()) {
                if (strBuilder.isNotEmpty()) strBuilder.appendLine()
                strBuilder.append("$labelNite - ${it.night!!.longPhrase}")
            }

            return@let strBuilder.toString()
        }
    }
}

fun createAtmosphere(current: CurrentsResponseItem): Atmosphere {
    return Atmosphere().apply {
        humidity = current.relativeHumidity

        pressureMb = current.pressure?.metric?.value
        pressureIn = current.pressure?.imperial?.value
        pressureTrend = when (current.pressureTendency?.code) {
            "F" -> "Falling"
            "R" -> "Rising"
            else -> ""
        }

        visibilityMi = current.visibility?.imperial?.value
        visibilityKm = current.visibility?.metric?.value

        dewpointF = current.dewPoint?.imperial?.value
        dewpointC = current.dewPoint?.metric?.value
    }
}

fun createPrecipitation(current: CurrentsResponseItem): Precipitation {
    return Precipitation().apply {
        cloudiness = current.cloudCover

        if (current.precipitationType == "Rain" || current.precipitationType == "Mixed") {
            qpfRainIn = current.precip1hr?.imperial?.value
                    ?: current.precipitationSummary?.pastHour?.imperial?.value

            if ((current.precip1hr?.metric?.unit
                            ?: current.precipitationSummary?.pastHour?.metric?.unit) == "mm") {
                qpfRainMm = current.precip1hr?.metric?.value
                        ?: current.precipitationSummary?.pastHour?.metric?.value
            } else if ((current.precip1hr?.metric?.unit
                            ?: current.precipitationSummary?.pastHour?.metric?.unit) == "cm") {
                qpfRainMm = current.precip1hr?.metric?.value?.times(10)
                        ?: current.precipitationSummary?.pastHour?.metric?.value?.times(10)
            }
        } else if (current.precipitationType == "Snow" || current.precipitationType == "Ice") {
            qpfSnowIn = current.precip1hr?.imperial?.value
                    ?: current.precipitationSummary?.pastHour?.imperial?.value

            if ((current.precip1hr?.metric?.unit
                            ?: current.precipitationSummary?.pastHour?.metric?.unit) == "mm") {
                qpfSnowCm = current.precip1hr?.metric?.value?.div(10)
                        ?: current.precipitationSummary?.pastHour?.metric?.value?.div(10)
            } else if ((current.precip1hr?.metric?.unit
                            ?: current.precipitationSummary?.pastHour?.metric?.unit) == "cm") {
                qpfSnowCm = current.precip1hr?.metric?.value
                        ?: current.precipitationSummary?.pastHour?.metric?.value
            }
        }
    }
}