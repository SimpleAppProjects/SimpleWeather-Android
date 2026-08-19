package com.thewizrd.weather_api.openmeteo

import android.annotation.SuppressLint
import com.thewizrd.shared_resources.utils.ConversionMethods
import com.thewizrd.shared_resources.utils.getBeaufortScale
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.model.AirQuality
import com.thewizrd.shared_resources.weatherdata.model.Atmosphere
import com.thewizrd.shared_resources.weatherdata.model.Beaufort
import com.thewizrd.shared_resources.weatherdata.model.Condition
import com.thewizrd.shared_resources.weatherdata.model.Forecast
import com.thewizrd.shared_resources.weatherdata.model.ForecastExtras
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.shared_resources.weatherdata.model.Location
import com.thewizrd.shared_resources.weatherdata.model.MinutelyForecast
import com.thewizrd.shared_resources.weatherdata.model.Pollen
import com.thewizrd.shared_resources.weatherdata.model.Precipitation
import com.thewizrd.shared_resources.weatherdata.model.UV
import com.thewizrd.shared_resources.weatherdata.model.Weather
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.max
import kotlin.math.roundToInt

@SuppressLint("VisibleForTests")
fun createWeatherData(forecast: ForecastResponse, aqiResponse: AQIResponse): Weather {
    return Weather().apply {
        location = createLocation(forecast)
        updateTime = ZonedDateTime.now(ZoneOffset.UTC)

        this.forecast = forecast.daily?.let { createForecasts(it) }
        hrForecast = forecast.hourly?.let { createHourlyForecasts(it) }
        minForecast = forecast.minutely15?.let { createMinutelyForecasts(it) }

        condition = forecast.current?.let { createCondition(it, aqiResponse.current) }
        atmosphere = forecast.current?.let { createAtmosphere(it) }
        precipitation = forecast.current?.let { createPrecipitation(it) }

        if ((condition?.highF == null || condition?.highC == null) && !this.forecast.isNullOrEmpty()) {
            condition!!.highF = this.forecast!![0].highF
            condition!!.highC = this.forecast!![0].highC
            condition!!.lowF = this.forecast!![0].lowF
            condition!!.lowC = this.forecast!![0].lowC
        }

        ttl = 120
        source = WeatherAPI.OPENMETEO
    }
}

fun createLocation(forecast: ForecastResponse): Location {
    return Location().apply {
        // Use location name from location provider
        name = null
        latitude = forecast.latitude
        longitude = forecast.longitude
        tzLong = forecast.timezone
    }
}

fun createForecasts(daily: Daily): List<Forecast>? {
    return daily.time?.mapIndexed { index, ts ->
        Forecast().apply {
            date =
                ZonedDateTime.ofInstant(Instant.ofEpochSecond(ts), ZoneOffset.UTC).toLocalDateTime()

            daily.temperature2mMax?.getOrNull(index)?.let {
                highF = ConversionMethods.CtoF(it)
                highC = it
            }
            daily.temperature2mMin?.getOrNull(index)?.let {
                lowF = ConversionMethods.CtoF(it)
                lowC = it
            }

            icon = (daily.weatherCode?.getOrNull(index) ?: -1).toString()
            condition = icon

            // Extras
            extras = ForecastExtras()

            extras.uvIndex = daily.uvIndexMax?.getOrNull(index)
            extras.pop = daily.precipitationProbabilityMax?.getOrNull(index)

            daily.windSpeed10mMax?.getOrNull(index)?.let {
                extras.windKph = it
                extras.windMph = ConversionMethods.kphTomph(it)
            }

            daily.windGusts10mMax?.getOrNull(index)?.let {
                extras.windGustKph = it
                extras.windGustMph = ConversionMethods.kphTomph(it)
            }

            extras.windDegrees = daily.windDirection10mDominant?.getOrNull(index)

            extras.cloudiness = daily.cloudCoverMean?.getOrNull(index)

            daily.apparentTemperatureMean?.getOrNull(index)?.let {
                extras.feelslikeC = it
                extras.feelslikeF = ConversionMethods.CtoF(it)
            }

            daily.dewPoint2mMean?.getOrNull(index)?.let {
                extras.dewpointC = it
                extras.dewpointF = ConversionMethods.CtoF(it)
            }

            // 1hPA = 1mbar
            daily.pressureMslMean?.getOrNull(index)?.let {
                extras.pressureMb = it
                extras.pressureIn = ConversionMethods.mbToInHg(it)
            }

            extras.humidity = daily.relativeHumidity2mMean?.getOrNull(index)

            daily.visibilityMean?.getOrNull(index)?.let {
                extras.visibilityKm = it / 1000
                extras.visibilityMi = ConversionMethods.kmToMi(it / 1000)
            }
        }
    }
}

fun createHourlyForecasts(hourly: Hourly): List<HourlyForecast>? {
    return hourly.time?.mapIndexed { index, ts ->
        HourlyForecast().apply {
            date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(ts), ZoneOffset.UTC)

            hourly.temperature2m?.getOrNull(index)?.let {
                highC = it
                highF = ConversionMethods.CtoF(it)
            }

            val isDay = hourly.isDay?.getOrNull(index) ?: 1
            icon = hourly.weatherCode?.getOrNull(index)?.toString() + "_" + isDay
            condition = icon

            // Extras
            extras = ForecastExtras()

            extras.humidity = hourly.relativeHumidity2m?.getOrNull(index)

            hourly.dewPoint2m?.getOrNull(index)?.let {
                extras.dewpointC = it
                extras.dewpointF = ConversionMethods.CtoF(it)
            }

            hourly.apparentTemperature?.getOrNull(index)?.let {
                extras.feelslikeC = it
                extras.feelslikeF = ConversionMethods.CtoF(it)
            }

            extras.pop = hourly.precipitationProbability?.getOrNull(index)

            // 1hPA = 1mbar
            hourly.pressureMsl?.getOrNull(index)?.let {
                extras.pressureMb = it
                extras.pressureIn = ConversionMethods.mbToInHg(it)
            }

            extras.cloudiness = hourly.cloudCover?.getOrNull(index)

            hourly.visibility?.getOrNull(index)?.let {
                extras.visibilityKm = it / 1000
                extras.visibilityMi = ConversionMethods.kmToMi(it / 1000)
            }

            hourly.windSpeed10m?.getOrNull(index)?.let { speed ->
                extras.windKph = speed
                extras.windMph = ConversionMethods.kphTomph(speed)
            }

            hourly.windGusts10m?.getOrNull(index)?.let { speed ->
                extras.windGustKph = speed
                extras.windGustKph = ConversionMethods.kphTomph(speed)
            }

            extras.windDegrees = hourly.windDirection10m?.getOrNull(index)

            hourly.rain?.getOrNull(index)?.let {
                extras.qpfRainMm = it
                extras.qpfRainIn = ConversionMethods.mmToIn(it)
            }

            hourly.snowfall?.getOrNull(index)?.let {
                extras.qpfSnowCm = it
                extras.qpfSnowIn = ConversionMethods.mmToIn(it * 10)
            }

            extras.uvIndex = hourly.uvIndex?.getOrNull(index)
        }
    }
}

fun createMinutelyForecasts(minutely15: Minutely15): List<MinutelyForecast>? {
    return minutely15.time?.mapIndexed { index, ts ->
        MinutelyForecast().apply {
            date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(ts), ZoneOffset.UTC)
            rainMm = minutely15.rain?.getOrNull(index)
            snowMm = minutely15.snowfall?.getOrNull(index)?.let { it * 10 }
        }
    }
}

fun createCondition(current: Current, aqi: CurrentAQI?): Condition {
    return Condition().apply {
        observationTime =
            current.time?.let { ZonedDateTime.ofInstant(Instant.ofEpochSecond(it), ZoneOffset.UTC) }

        val isDay = current.isDay ?: 1
        icon = current.weatherCode?.toString() + "_" + isDay
        weather = icon

        current.temperature2m?.let {
            tempC = it
            tempF = ConversionMethods.CtoF(it)
        }

        current.apparentTemperature?.let {
            feelslikeC = it
            feelslikeF = ConversionMethods.CtoF(it)
        }

        current.windSpeed10m?.let {
            windKph = it
            windMph = ConversionMethods.kphTomph(it)
            beaufort = Beaufort(getBeaufortScale(mph = windMph.roundToInt()))
        }

        current.windGusts10m?.let {
            windGustKph = it
            windGustMph = ConversionMethods.kphTomph(it)
        }

        windDegrees = current.windDirection10m

        uv = current.uvIndex?.let { UV(it) }

        airQuality = AirQuality().apply {
            index = aqi?.usAqi
            pm25 = aqi?.usAqiPm25
            pm10 = aqi?.usAqiPm10
            o3 = aqi?.usAqiOzone
            no2 = aqi?.usAqiNitrogenDioxide
            co = aqi?.usAqiCarbonMonoxide
            so2 = aqi?.usAqiSulphurDioxide
        }

        val treePollenMeasure = if (aqi?.birchPollen != null && aqi.alderPollen != null) {
            max(aqi.birchPollen!!, aqi.alderPollen!!)
        } else {
            aqi?.birchPollen ?: aqi?.alderPollen
        }
        val grassPollenMeasure = aqi?.grassPollen
        val ragweedPollenMeasure = aqi?.ragweedPollen

        pollen = Pollen().apply {
            treePollenCount = when {
                treePollenMeasure == null -> Pollen.PollenCount.UNKNOWN
                treePollenMeasure < 15 -> Pollen.PollenCount.LOW
                treePollenMeasure < 90 -> Pollen.PollenCount.MODERATE
                treePollenMeasure < 1500 -> Pollen.PollenCount.HIGH
                treePollenMeasure >= 1500 -> Pollen.PollenCount.VERY_HIGH
                else -> Pollen.PollenCount.UNKNOWN
            }

            grassPollenCount = when {
                grassPollenMeasure == null -> Pollen.PollenCount.UNKNOWN
                grassPollenMeasure < 5 -> Pollen.PollenCount.LOW
                grassPollenMeasure < 20 -> Pollen.PollenCount.MODERATE
                grassPollenMeasure < 200 -> Pollen.PollenCount.HIGH
                grassPollenMeasure >= 200 -> Pollen.PollenCount.VERY_HIGH
                else -> Pollen.PollenCount.UNKNOWN
            }

            ragweedPollenCount = when {
                ragweedPollenMeasure == null -> Pollen.PollenCount.UNKNOWN
                ragweedPollenMeasure < 10 -> Pollen.PollenCount.LOW
                ragweedPollenMeasure < 50 -> Pollen.PollenCount.MODERATE
                ragweedPollenMeasure < 500 -> Pollen.PollenCount.HIGH
                ragweedPollenMeasure >= 500 -> Pollen.PollenCount.VERY_HIGH
                else -> Pollen.PollenCount.UNKNOWN
            }
        }
    }
}

fun createAtmosphere(current: Current): Atmosphere {
    return Atmosphere().apply {
        humidity = current.relativeHumidity2m

        // 1hPa = 1mb
        current.pressureMsl?.let {
            pressureMb = it
            pressureIn = ConversionMethods.mbToInHg(it)
        }
        pressureTrend = ""

        current.dewPoint2m?.let {
            dewpointC = it
            dewpointF = ConversionMethods.CtoF(it)
        }

        current.visibility?.let {
            visibilityKm = it / 1000
            visibilityMi = ConversionMethods.kmToMi(visibilityKm)
        }
    }
}

fun createPrecipitation(current: Current): Precipitation {
    return Precipitation().apply {
        pop = current.precipitationProbability
        cloudiness = current.cloudCover

        current.rain?.let {
            qpfRainMm = it
            qpfRainIn = ConversionMethods.mmToIn(it)
        }

        current.snowfall?.let {
            qpfSnowCm = it
            qpfSnowIn = ConversionMethods.mmToIn(it * 10)
        }
    }
}