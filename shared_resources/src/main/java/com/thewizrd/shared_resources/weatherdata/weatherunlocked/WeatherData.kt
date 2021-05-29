package com.thewizrd.shared_resources.weatherdata.weatherunlocked

import android.annotation.SuppressLint
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.WeatherManager
import com.thewizrd.shared_resources.weatherdata.model.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.roundToInt

@SuppressLint("VisibleForTests")
fun createWeatherData(currRoot: CurrentResponse, foreRoot: ForecastResponse): Weather {
    return Weather().apply {
        location = createLocation(currRoot)
        updateTime = ZonedDateTime.now(ZoneOffset.UTC)

        // 8-day forecast / 3-hr forecast
        // 24hr / 3hr = 8items for each day
        forecast = ArrayList(8)
        hrForecast = ArrayList(64)

        // Forecast
        for (day in foreRoot.days) {
            val fcast = createForecast(day)

            val midDayIdx = day.timeframes.size / 2
            for (i in day.timeframes.indices) {
                val hrfcast = createHourlyForecast(day.timeframes[i])

                if (i == midDayIdx) {
                    fcast.icon = WeatherManager.getProvider(WeatherAPI.WEATHERUNLOCKED)
                            .getWeatherIcon(hrfcast.icon)
                    fcast.condition = hrfcast.condition
                }

                hrForecast.add(hrfcast)
            }

            forecast.add(fcast)
        }

        condition = createCondition(currRoot)
        atmosphere = createAtmosphere(currRoot)
        //astronomy = createAstronomy(currRoot)
        //precipitation = createPrecipitation(currRoot)
        ttl = 180

        // Set feelslike temp
        if (condition.feelslikeF == null && condition.tempF != null && condition.windMph != null && atmosphere.humidity != null) {
            condition.feelslikeF = getFeelsLikeTemp(condition.tempF, condition.windMph, atmosphere.humidity)
            condition.feelslikeC = ConversionMethods.FtoC(condition.feelslikeF)
        }

        if ((condition.highF == null || condition.highC == null) && forecast.size > 0) {
            condition.highF = forecast[0].highF
            condition.highC = forecast[0].highC
            condition.lowF = forecast[0].lowF
            condition.lowC = forecast[0].lowC
        }

        condition.observationTime = updateTime

        source = WeatherAPI.WEATHERUNLOCKED
    }
}

fun createLocation(currRoot: CurrentResponse): Location {
    return Location().apply {
        // Use location name from location provider
        name = null
        latitude = currRoot.lat
        longitude = currRoot.lon
        tzLong = null
    }
}

fun createForecast(day: DaysItem): Forecast {
    return Forecast().apply {
        date = LocalDate.parse(day.date, DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT)).atStartOfDay()
        highF = day.tempMaxF
        highC = day.tempMaxC
        lowF = day.tempMinF
        lowC = day.tempMinC

        //condition = null;
        //icon = null;

        // Extras
        extras = ForecastExtras()
        extras.humidity = ((day.humidMinPct + day.humidMaxPct) / 2).roundToInt()
        extras.pressureMb = ((day.slpMinMb + day.slpMaxMb) / 2).roundToInt().toFloat()
        extras.pressureIn = ((day.slpMinIn + day.slpMaxIn) / 2).roundToInt().toFloat()
        if (day.windspdMaxMph > 0 && day.humidMaxPct > 0) {
            extras.feelslikeF = getFeelsLikeTemp(highF, day.windspdMaxMph, day.humidMaxPct.roundToInt())
            extras.feelslikeC = ConversionMethods.FtoC(extras.feelslikeF)
        }
        if (highC > 0 && highC < 60 && day.humidMaxPct > 1) {
            extras.dewpointC = calculateDewpointC(highC, day.humidMaxPct.roundToInt()).roundToInt().toFloat()
            extras.dewpointF = ConversionMethods.CtoF(extras.dewpointC).roundToInt().toFloat()
        }
        extras.windMph = day.windspdMaxMph.roundToInt().toFloat()
        extras.windKph = day.windspdMaxKmh.roundToInt().toFloat()
        extras.pop = day.probPrecipPct.roundToInt()
        extras.windGustMph = day.windgstMaxMph.roundToInt().toFloat()
        extras.windGustKph = day.windgstMaxKmh.roundToInt().toFloat()
        extras.qpfRainMm = day.rainTotalMm
        extras.qpfRainIn = day.rainTotalIn
        extras.qpfSnowCm = day.snowTotalMm / 10f
        extras.qpfSnowIn = day.snowTotalIn
    }
}

fun createHourlyForecast(timeframe: TimeframesItem): HourlyForecast {
    return HourlyForecast().apply {
        val date = timeframe.utcdate
        val time = timeframe.utctime
        val dateObj = LocalDate.parse(date, DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT))
        val timeObj = if (time == 0) {
            LocalTime.MIDNIGHT
        } else {
            LocalTime.parse(time.toString(), DateTimeFormatter.ofPattern("Hmm", Locale.ROOT))
        }
        setDate(ZonedDateTime.of(dateObj, timeObj, ZoneOffset.UTC))

        highF = timeframe.tempF
        highC = timeframe.tempC
        condition = timeframe.wxDesc
        icon = timeframe.wxCode.toString()

        windDegrees = timeframe.winddirDeg.roundToInt()
        windMph = timeframe.windspdMph.roundToInt().toFloat()
        windKph = timeframe.windspdKmh.roundToInt().toFloat()

        // Extras
        extras = ForecastExtras()
        extras.humidity = timeframe.humidPct.roundToInt()
        extras.cloudiness = timeframe.cloudtotalPct.roundToInt()
        extras.pressureMb = timeframe.slpMb
        extras.pressureIn = timeframe.slpIn
        extras.windDegrees = windDegrees
        extras.windMph = windMph
        extras.windKph = windKph
        extras.dewpointF = timeframe.dewpointF.roundToInt().toFloat()
        extras.dewpointC = timeframe.dewpointC.roundToInt().toFloat()
        extras.feelslikeF = timeframe.feelslikeF.roundToInt().toFloat()
        extras.feelslikeC = timeframe.feelslikeC.roundToInt().toFloat()
        extras.pop = NumberUtils.tryParseInt(timeframe.probPrecipPct, 0)
        extras.windGustMph = timeframe.windgstMph.roundToInt().toFloat()
        extras.windGustKph = timeframe.windgstKmh.roundToInt().toFloat()
        extras.visibilityMi = timeframe.visMi
        extras.visibilityKm = timeframe.visKm
        extras.qpfRainMm = timeframe.rainMm
        extras.qpfRainIn = timeframe.rainIn
        extras.qpfSnowCm = timeframe.snowMm / 10
        extras.qpfSnowIn = timeframe.snowIn
    }
}

fun createCondition(currRoot: CurrentResponse): Condition {
    return Condition().apply {
        tempF = currRoot.tempF
        tempC = currRoot.tempC

        weather = currRoot.wxDesc
        icon = currRoot.wxCode.toString()

        windDegrees = currRoot.winddirDeg.roundToInt()
        windMph = currRoot.windspdMph
        windKph = currRoot.windspdKmh
        feelslikeF = currRoot.feelslikeF
        feelslikeC = currRoot.feelslikeC

        beaufort = Beaufort(getBeaufortScale(currRoot.windspdMs))
    }
}

fun createAtmosphere(currRoot: CurrentResponse): Atmosphere {
    return Atmosphere().apply {
        humidity = currRoot.humidPct.roundToInt()
        pressureIn = currRoot.slpIn
        pressureMb = currRoot.slpMb
        pressureTrend = ""
        visibilityMi = currRoot.visMi
        visibilityKm = currRoot.visKm
    }
}