package com.thewizrd.weather_api.brightsky

import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.ConversionMethods
import com.thewizrd.shared_resources.utils.getBeaufortScale
import com.thewizrd.shared_resources.utils.getFeelsLikeTemp
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.model.Atmosphere
import com.thewizrd.shared_resources.weatherdata.model.Beaufort
import com.thewizrd.shared_resources.weatherdata.model.Condition
import com.thewizrd.shared_resources.weatherdata.model.Forecast
import com.thewizrd.shared_resources.weatherdata.model.ForecastExtras
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.shared_resources.weatherdata.model.Location
import com.thewizrd.shared_resources.weatherdata.model.Precipitation
import com.thewizrd.shared_resources.weatherdata.model.Weather
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

fun createWeatherData(
    currRoot: CurrentResponse,
    foreRoot: ForecastResponse,
    locationData: LocationData
): Weather {
    return Weather().apply {
        val now = if (locationData.tzOffset != null) {
            ZonedDateTime.now(ZoneOffset.UTC).withZoneSameInstant(locationData.tzOffset)
        } else {
            ZonedDateTime.now(ZoneOffset.UTC)
        }

        location = createLocation().apply {
            name = locationData.name
            latitude = locationData.latitude.toFloat()
            longitude = locationData.longitude.toFloat()
            tzLong = locationData.tzLong
        }
        updateTime = now

        foreRoot.weather?.let { items ->
            forecast = ArrayList(5)
            hrForecast = ArrayList(foreRoot.weather?.size ?: 0)

            // Store potential min/max values
            var dayMax = Float.NaN
            var dayMin = Float.NaN

            val epochTime = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC)
            var currentDate = epochTime
            var fcast: Forecast? = null

            val iconCountMap = mutableMapOf<String, Int>()

            // No daily data for DWD
            for (i in items.indices) {
                val item = items[i]
                var date = ZonedDateTime.parse(item.timestamp)
                if (locationData.tzOffset != null) {
                    date = date.withZoneSameInstant(locationData.tzOffset)
                }

                if (currentDate.isEqual(epochTime)) {
                    currentDate = date
                }

                // Add a new hour
                if (!date.truncatedTo(ChronoUnit.HOURS).isBefore(now.truncatedTo(ChronoUnit.HOURS)))
                    hrForecast!!.add(createHourlyForecast(item))

                // Create new forecast
                if (i == 0 || !currentDate.toLocalDate().isEqual(date.toLocalDate())) {
                    // Last forecast for day; create forecast
                    if (fcast != null) {
                        // condition (set in provider GetWeather method)
                        // date
                        fcast.date = currentDate.toLocalDateTime()
                        // high
                        fcast.highF = ConversionMethods.CtoF(dayMax)
                        fcast.highC = dayMax
                        // low
                        fcast.lowF = ConversionMethods.CtoF(dayMin)
                        fcast.lowC = dayMin
                        // icon
                        fcast.icon = iconCountMap
                            .filterNot { it.key.endsWith("-night") }
                            .maxByOrNull { it.value }
                            ?.key
                            ?: fcast.icon

                        forecast!!.add(fcast)
                    }

                    currentDate = date
                    fcast = createForecast(item)
                    fcast.date = date.toLocalDateTime()

                    // Reset
                    dayMax = Float.NaN
                    dayMin = Float.NaN
                    iconCountMap.clear()
                }

                // Find max/min for each hour
                val temp = item.temperature ?: Float.NaN

                if (!temp.isNaN() && (dayMax.isNaN() || temp > dayMax)) {
                    dayMax = temp
                }
                if (!temp.isNaN() && (dayMin.isNaN() || temp < dayMin)) {
                    dayMin = temp
                }

                // Keep track of conditions
                item.icon?.let {
                    iconCountMap.put(it, iconCountMap.getOrDefault(it, 0) + 1)
                }
            }

            fcast = forecast!!.lastOrNull()
            if (fcast != null && fcast.condition == null && fcast.icon == null) {
                forecast!!.removeAt(forecast!!.size - 1)
            }

            val hrfcast = hrForecast!!.lastOrNull()
            if (hrfcast != null && hrfcast.condition == null && hrfcast.icon == null) {
                hrForecast!!.removeAt(hrForecast!!.size - 1)
            }
        }

        condition = createCondition(currRoot)
        atmosphere = createAtmosphere(currRoot)
        precipitation = createPrecipitation(currRoot)

        ttl = 120

        // Set feelslike temp
        if (condition?.feelslikeF == null && condition?.tempF != null && condition?.windMph != null && atmosphere?.humidity != null) {
            condition!!.feelslikeF =
                getFeelsLikeTemp(condition!!.tempF, condition!!.windMph, atmosphere!!.humidity)
            condition!!.feelslikeC = ConversionMethods.FtoC(condition!!.feelslikeF)
        }

        if ((condition?.highF == null || condition?.highC == null) && forecast!!.size > 0) {
            condition!!.highF = forecast!![0].highF
            condition!!.highC = forecast!![0].highC
            condition!!.lowF = forecast!![0].lowF
            condition!!.lowC = forecast!![0].lowC
        }

        source = WeatherAPI.DWD
    }
}

fun createLocation(): Location {
    return Location()
}

fun createCondition(currRoot: CurrentResponse): Condition {
    return Condition().apply {
        currRoot.weather?.temperature?.let {
            tempF = ConversionMethods.CtoF(it)
            tempC = it
        }

        windDegrees = currRoot.weather?.windDirection10

        currRoot.weather?.windSpeed10?.let {
            windMph = ConversionMethods.kphTomph(it)
            windKph = it
        }

        currRoot.weather?.windGustSpeed10?.let {
            windGustMph = ConversionMethods.kphTomph(it)
            windGustKph = it
        }

        icon = currRoot.weather?.icon

        windMph?.let {
            beaufort = Beaufort(getBeaufortScale(it.roundToInt()))
        }

        observationTime = ZonedDateTime.parse(currRoot.weather?.timestamp)
    }
}

fun createAtmosphere(currRoot: CurrentResponse): Atmosphere {
    return Atmosphere().apply {
        humidity = currRoot.weather?.relativeHumidity

        currRoot.weather?.pressureMsl?.let {
            pressureMb = it
            pressureIn = ConversionMethods.mbToInHg(it)
            pressureTrend = ""
        }

        currRoot.weather?.visibility?.let {
            visibilityMi = ConversionMethods.kmToMi(it / 1000f)
            visibilityKm = it / 1000f
        }

        currRoot.weather?.dewPoint?.let {
            dewpointF = ConversionMethods.CtoF(it)
            dewpointC = it
        }
    }
}

fun createPrecipitation(currRoot: CurrentResponse): Precipitation {
    return Precipitation().apply {
        // Use cloudiness value here
        cloudiness = currRoot.weather?.cloudCover?.roundToInt()
        // Precipitation
        (currRoot.weather?.precipitation10 ?: currRoot.weather?.precipitation30
        ?: currRoot.weather?.precipitation10)?.let {
            qpfRainMm = it
            qpfRainIn = ConversionMethods.mmToIn(it)
        }
        // The rest DNE
    }
}

fun createHourlyForecast(hr_forecast: WeatherItem): HourlyForecast {
    return HourlyForecast().apply {
        date = ZonedDateTime.parse(hr_forecast.timestamp)
        hr_forecast.temperature?.let {
            highF = ConversionMethods.CtoF(it)
            highC = it
        }

        icon = hr_forecast.icon

        // Extras
        val humidity = hr_forecast.relativeHumidity?.roundToInt()
        extras = ForecastExtras()
        extras.humidity = humidity
        hr_forecast.dewPoint?.let {
            extras.dewpointF = ConversionMethods.CtoF(it)
            extras.dewpointC = it
        }
        extras.cloudiness = hr_forecast.cloudCover?.roundToInt()
        // Precipitation
        extras.pop = hr_forecast.precipitationProbability ?: hr_forecast.precipitationProbability6h
        hr_forecast.pressureMsl?.let {
            extras.pressureIn = ConversionMethods.mbToInHg(it)
            extras.pressureMb = it
        }
        extras.windDegrees = hr_forecast.windDirection?.roundToInt()
        hr_forecast.windSpeed?.let {
            extras.windMph = ConversionMethods.kphTomph(it)
            extras.windKph = it
        }
        hr_forecast.windGustSpeed?.let {
            extras.windGustMph = ConversionMethods.kphTomph(it)
            extras.windGustKph = it
        }
        hr_forecast.visibility?.let {
            extras.visibilityMi = ConversionMethods.kmToMi(it / 1000f)
            extras.visibilityKm = it / 1000f
        }
        if (highF != null && extras.windMph != null && humidity != null) {
            val feelsLikeF = getFeelsLikeTemp(highF, extras.windMph, humidity)
            extras.feelslikeF = feelsLikeF
            extras.feelslikeC = ConversionMethods.FtoC(feelsLikeF)
        }
    }
}

fun createForecast(forecast: WeatherItem): Forecast {
    return Forecast().apply {
        date = LocalDateTime.ofInstant(
            Instant.from(
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(forecast.timestamp)
            ), ZoneOffset.UTC
        )
        icon = forecast.icon
        // Don't bother setting other values; they're not available yet
    }
}