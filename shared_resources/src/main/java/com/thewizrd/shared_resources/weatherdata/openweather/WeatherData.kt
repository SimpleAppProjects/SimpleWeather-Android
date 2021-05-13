package com.thewizrd.shared_resources.weatherdata.openweather

import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.weatherdata.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*
import kotlin.math.roundToInt

fun createWeatherData(currRoot: CurrentRootobject, foreRoot: ForecastRootobject): Weather {
    return Weather().apply {
        location = createLocation(foreRoot)
        updateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(currRoot.dt), ZoneOffset.UTC)

        // 5-day forecast / 3-hr forecast
        // 24hr / 3hr = 8items for each day
        forecast = ArrayList(5)
        hrForecast = ArrayList(foreRoot.list.size)

        // Store potential min/max values
        var dayMax = Float.NaN
        var dayMin = Float.NaN
        var lastDay = -1

        for (i in foreRoot.list.indices) {
            hrForecast.add(createHourlyForecast(foreRoot.list[i]))

            val max = foreRoot.list[i].main.tempMax
            if (!max.isNaN() && (dayMax.isNaN() || max > dayMax)) {
                dayMax = max
            }

            val min = foreRoot.list[i].main.tempMin
            if (!min.isNaN() && (dayMin.isNaN() || min < dayMin)) {
                dayMin = min
            }

            // Add mid-day forecast
            val currHour = hrForecast[i].date.plusSeconds(currRoot.timezone.toLong()).hour
            if (currHour >= 11 && currHour <= 13) {
                forecast.add(createForecast(foreRoot.list[i]))
                lastDay = forecast.size - 1
            }

            // This is possibly the last forecast for the day (3-hrly forecast)
            // Set the min / max temp here and reset
            if (currHour >= 21) {
                if (lastDay >= 0) {
                    if (!dayMax.isNaN()) {
                        forecast[lastDay].highF = ConversionMethods.KtoF(dayMax)
                        forecast[lastDay].highC = ConversionMethods.KtoC(dayMax)
                    }
                    if (!dayMin.isNaN()) {
                        forecast[lastDay].lowF = ConversionMethods.KtoF(dayMin)
                        forecast[lastDay].lowC = ConversionMethods.KtoC(dayMin)
                    }
                }
                dayMax = Float.NaN
                dayMin = Float.NaN
            }
        }

        condition = createCondition(currRoot)
        atmosphere = createAtmosphere(currRoot)
        astronomy = createAstronomy(currRoot)
        precipitation = createPrecipitation(currRoot)
        ttl = 180

        query = currRoot.id.toString()

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

        if (atmosphere.dewpointC == null && condition.tempC != null && atmosphere.humidity != null && condition.tempC > 0 && condition.tempC < 60 && atmosphere.humidity > 1) {
            atmosphere.dewpointC = calculateDewpointC(condition.tempC, atmosphere.humidity).roundToInt().toFloat()
            atmosphere.dewpointF = ConversionMethods.CtoF(atmosphere.dewpointC).roundToInt().toFloat()
        }

        source = WeatherAPI.OPENWEATHERMAP
    }
}

fun createLocation(root: ForecastRootobject): Location {
    return Location().apply {
        // Use location name from location provider
        name = null
        latitude = root.city.coord.lat
        longitude = root.city.coord.lon
        tzLong = null
    }
}

fun createHourlyForecast(hr_forecast: ListItem): HourlyForecast {
    return HourlyForecast().apply {
        date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(hr_forecast.dt), ZoneOffset.UTC)
        highF = ConversionMethods.KtoF(hr_forecast.main.temp)
        highC = ConversionMethods.KtoC(hr_forecast.main.temp)
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

        windDegrees = hr_forecast.wind.deg.roundToInt()
        windMph = ConversionMethods.msecToMph(hr_forecast.wind.speed).roundToInt().toFloat()
        windKph = ConversionMethods.msecToKph(hr_forecast.wind.speed).roundToInt().toFloat()

        // Extras
        extras = ForecastExtras()
        extras.humidity = hr_forecast.main.humidity
        extras.cloudiness = hr_forecast.clouds.all
        // 1hPA = 1mbar
        extras.pressureMb = hr_forecast.main.pressure
        extras.pressureIn = ConversionMethods.mbToInHg(hr_forecast.main.pressure)
        extras.windDegrees = windDegrees
        extras.windMph = windMph
        extras.windKph = windKph
        if (highC > 0 && highC < 60 && hr_forecast.main.humidity > 1) {
            extras.dewpointC = calculateDewpointC(highC, hr_forecast.main.humidity).roundToInt().toFloat()
            extras.dewpointF = ConversionMethods.CtoF(extras.dewpointC).roundToInt().toFloat()
        }
        if (hr_forecast.main.feelsLike != null) {
            extras.feelslikeF = ConversionMethods.KtoF(hr_forecast.main.feelsLike)
            extras.feelslikeC = ConversionMethods.KtoC(hr_forecast.main.feelsLike)
        }
        if (hr_forecast.pop != null) {
            extras.pop = (hr_forecast.pop * 100).roundToInt()
        }
        if (hr_forecast.wind.gust != null) {
            extras.windGustMph = ConversionMethods.msecToMph(hr_forecast.wind.gust).roundToInt().toFloat()
            extras.windGustKph = ConversionMethods.msecToKph(hr_forecast.wind.gust).roundToInt().toFloat()
        }
        if (hr_forecast.visibility != null) {
            extras.visibilityKm = hr_forecast.visibility.toFloat() / 1000
            extras.visibilityMi = ConversionMethods.kmToMi(extras.visibilityKm)
        }
        if (hr_forecast.rain != null && hr_forecast.rain._3h != null) {
            extras.qpfRainMm = hr_forecast.rain._3h
            extras.qpfRainIn = ConversionMethods.mmToIn(hr_forecast.rain._3h)
        }
        if (hr_forecast.snow != null && hr_forecast.snow._3h != null) {
            extras.qpfSnowCm = hr_forecast.snow._3h / 10
            extras.qpfSnowIn = ConversionMethods.mmToIn(hr_forecast.snow._3h)
        }
    }
}

fun createForecast(forecast: ListItem): Forecast {
    return Forecast().apply {
        date = LocalDateTime.ofEpochSecond(forecast.dt, 0, ZoneOffset.UTC)
        highF = ConversionMethods.KtoF(forecast.main.tempMax)
        highC = ConversionMethods.KtoC(forecast.main.tempMax)
        lowF = ConversionMethods.KtoF(forecast.main.tempMin)
        lowC = ConversionMethods.KtoC(forecast.main.tempMin)
        condition = StringUtils.toUpperCase(forecast.weather[0].description)
        icon = WeatherManager.getProvider(WeatherAPI.OPENWEATHERMAP)
                .getWeatherIcon(forecast.weather[0].id.toString())

        // Extras
        extras = ForecastExtras()
        extras.humidity = forecast.main.humidity
        extras.cloudiness = forecast.clouds.all
        // 1hPA = 1mbar
        extras.pressureMb = forecast.main.pressure
        extras.pressureIn = ConversionMethods.mbToInHg(forecast.main.pressure)
        extras.windDegrees = forecast.wind.deg.roundToInt()
        extras.windMph = ConversionMethods.msecToMph(forecast.wind.speed).roundToInt().toFloat()
        extras.windKph = ConversionMethods.msecToKph(forecast.wind.speed).roundToInt().toFloat()
        val temp_c = ConversionMethods.KtoC(forecast.main.temp)
        if (temp_c > 0 && temp_c < 60 && forecast.main.humidity > 1) {
            extras.dewpointC = calculateDewpointC(temp_c, forecast.main.humidity).roundToInt().toFloat()
            extras.dewpointF = ConversionMethods.CtoF(extras.dewpointC).roundToInt().toFloat()
        }
        if (forecast.main.feelsLike != null) {
            extras.feelslikeF = ConversionMethods.KtoF(forecast.main.feelsLike)
            extras.feelslikeC = ConversionMethods.KtoC(forecast.main.feelsLike)
        }
        if (forecast.pop != null) {
            extras.pop = (forecast.pop * 100).roundToInt()
        }
        if (forecast.visibility != null) {
            extras.visibilityKm = forecast.visibility.toFloat() / 1000
            extras.visibilityMi = ConversionMethods.kmToMi(extras.visibilityKm)
        }
        if (forecast.wind.gust != null) {
            extras.windGustMph = ConversionMethods.msecToMph(forecast.wind.gust).roundToInt().toFloat()
            extras.windGustKph = ConversionMethods.msecToKph(forecast.wind.gust).roundToInt().toFloat()
        }
        if (forecast.rain != null && forecast.rain._3h != null) {
            extras.qpfRainMm = forecast.rain._3h
            extras.qpfRainIn = ConversionMethods.mmToIn(forecast.rain._3h)
        }
        if (forecast.snow != null && forecast.snow._3h != null) {
            extras.qpfSnowCm = forecast.snow._3h / 10
            extras.qpfSnowIn = ConversionMethods.mmToIn(forecast.snow._3h)
        }
    }
}

fun createCondition(current: CurrentRootobject): Condition {
    return Condition().apply {
        weather = StringUtils.toUpperCase(current.weather[0].description)
        tempF = ConversionMethods.KtoF(current.main.temp)
        tempC = ConversionMethods.KtoC(current.main.temp)
        highF = ConversionMethods.KtoF(current.main.tempMax)
        highC = ConversionMethods.KtoC(current.main.tempMax)
        lowF = ConversionMethods.KtoF(current.main.tempMin)
        lowC = ConversionMethods.KtoC(current.main.tempMin)
        windDegrees = current.wind.deg.toInt()
        windMph = ConversionMethods.msecToMph(current.wind.speed)
        windKph = ConversionMethods.msecToKph(current.wind.speed)
        if (current.main.feelsLike != null) {
            feelslikeF = ConversionMethods.KtoF(current.main.feelsLike)
            feelslikeC = ConversionMethods.KtoC(current.main.feelsLike)
        }
        if (current.wind.gust != null) {
            windGustMph = ConversionMethods.msecToMph(current.wind.gust)
            windGustKph = ConversionMethods.msecToKph(current.wind.gust)
        }

        val ico = current.weather[0].icon
        var dn = ico[if (ico.isEmpty()) 0 else ico.length - 1].toString()

        try {
            val x = dn.toInt()
            dn = ""
        } catch (ex: java.lang.NumberFormatException) {
            // DO nothing
        }

        icon = WeatherManager.getProvider(WeatherAPI.OPENWEATHERMAP)
                .getWeatherIcon(current.weather[0].id.toString() + dn)

        beaufort = Beaufort(getBeaufortScale(current.wind.speed).value)

        observationTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(current.dt), ZoneOffset.UTC)
    }
}

fun createAtmosphere(root: CurrentRootobject): Atmosphere {
    return Atmosphere().apply {
        humidity = root.main.humidity
        // 1hPa = 1mbar
        pressureMb = root.main.pressure
        pressureIn = ConversionMethods.mbToInHg(root.main.pressure)
        pressureTrend = ""
        visibilityKm = root.visibility / 1000f
        visibilityMi = ConversionMethods.kmToMi(visibilityKm)
    }
}

fun createAstronomy(root: CurrentRootobject): Astronomy {
    return Astronomy().apply {
        runCatching {
            sunrise = LocalDateTime.ofEpochSecond(root.sys.sunrise, 0, ZoneOffset.UTC)
        }
        runCatching {
            sunset = LocalDateTime.ofEpochSecond(root.sys.sunset, 0, ZoneOffset.UTC)
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

fun createPrecipitation(root: CurrentRootobject): Precipitation {
    return Precipitation().apply {
        // Use cloudiness value here
        cloudiness = root.clouds.all
        if (root.rain != null) {
            if (root.rain._1h != null) {
                qpfRainIn = ConversionMethods.mmToIn(root.rain._1h)
                qpfRainMm = root.rain._1h
            } else if (root.rain._3h != null) {
                qpfRainIn = ConversionMethods.mmToIn(root.rain._3h)
                qpfRainMm = root.rain._3h
            }
        }
        if (root.snow != null) {
            if (root.snow._1h != null) {
                qpfSnowIn = ConversionMethods.mmToIn(root.snow._1h)
                qpfSnowCm = root.snow._1h / 10
            } else if (root.snow._3h != null) {
                qpfSnowIn = ConversionMethods.mmToIn(root.snow._3h)
                qpfSnowCm = root.snow._3h / 10
            }
        }
    }
}