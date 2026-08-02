package com.thewizrd.weather_api.brightsky

import android.util.Log
import androidx.core.net.toUri
import com.thewizrd.shared_resources.exceptions.ErrorStatus
import com.thewizrd.shared_resources.exceptions.WeatherException
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.LocationQuery
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.await
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.getStream
import com.thewizrd.shared_resources.remoteconfig.remoteConfigService
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.LocationUtils
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.utils.ZoneIdCompat
import com.thewizrd.shared_resources.utils.createUnsupportedLocationException
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.shared_resources.weatherdata.model.isNullOrInvalid
import com.thewizrd.weather_api.extras.cacheRequestIfNeeded
import com.thewizrd.weather_api.locationiq.LocationIQProvider
import com.thewizrd.weather_api.nws.SolCalcAstroProvider
import com.thewizrd.weather_api.smc.SunMoonCalcProvider
import com.thewizrd.weather_api.utils.APIRequestUtils.checkForErrors
import com.thewizrd.weather_api.utils.APIRequestUtils.checkRateLimit
import com.thewizrd.weather_api.utils.logMissingIcon
import com.thewizrd.weather_api.weatherModule
import com.thewizrd.weather_api.weatherdata.WeatherProviderImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly
import java.io.IOException
import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

class BrightSkyProvider : WeatherProviderImpl() {
    companion object {
        private const val BASE_URL = "https://api.brightsky.dev/"
        private const val CURRENT_QUERY_URL = BASE_URL + "current_weather?%s"
        private const val FORECAST_QUERY_URL = BASE_URL + "weather?%s"
        private const val ALERTS_QUERY_URL = BASE_URL + "alerts?%s"
    }

    init {
        mLocationProvider = runCatching {
            weatherModule.locationProviderFactory.getLocationProvider(
                remoteConfigService.getLocationProvider(
                    getWeatherAPI()
                )
            )
        }.getOrElse {
            LocationIQProvider()
        }
    }

    override fun getWeatherAPI(): String {
        return WeatherAPI.DWD
    }

    override fun supportsWeatherLocale(): Boolean {
        return false
    }

    override fun isKeyRequired(): Boolean {
        return false
    }

    override fun supportsAlerts(): Boolean {
        return true
    }

    override fun needsExternalAlertData(): Boolean {
        return false
    }

    override fun isRegionSupported(location: LocationData): Boolean {
        return LocationUtils.isGermany(location)
    }

    override fun isRegionSupported(location: LocationQuery): Boolean {
        return LocationUtils.isGermany(location)
    }

    override fun getHourlyForecastInterval(): Int {
        return 1
    }

    override suspend fun isKeyValid(key: String?): Boolean {
        return false
    }

    override fun getAPIKey(): String? {
        return null
    }

    @Throws(WeatherException::class)
    override suspend fun getWeatherData(location: LocationData): Weather =
        withContext(Dispatchers.IO) {
            var weather: Weather?

            // DWD is best in Germany
            if (!LocationUtils.isGermany(location)) {
                throw WeatherException(ErrorStatus.LOCATIONNOTSUPPORTED)
                    .initCause(createUnsupportedLocationException(getWeatherAPI(), location))
            }

            val query = updateLocationQuery(location)

            val client = sharedDeps.httpClient
            var currentResponse: Response? = null
            var forecastResponse: Response? = null
            var alertsResponse: Response? = null
            var wEx: WeatherException? = null

            try {
                // If were under rate limit, deny request
                checkRateLimit()

                val now = ZonedDateTime.now(ZoneOffset.UTC)
                val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT)

                val currentRequestUri = CURRENT_QUERY_URL.format(query).toUri().buildUpon()
                    .appendQueryParameter("tz", "Etc/UTC")
                    .appendQueryParameter("units", "dwd")
                    .build()
                    .toString()

                val currentRequest = Request.Builder()
                    .cacheRequestIfNeeded(isKeyRequired(), 15, TimeUnit.MINUTES)
                    .url(currentRequestUri)
                    .build()

                val forecastRequestUri = FORECAST_QUERY_URL.format(query).toUri().buildUpon()
                    .appendQueryParameter("date", now.format(dtf))
                    .appendQueryParameter("last_date", now.plusDays(5).format(dtf))
                    .appendQueryParameter("tz", "Etc/UTC")
                    .appendQueryParameter("units", "dwd")
                    .build()
                    .toString()

                val forecastRequest = Request.Builder()
                    .cacheRequestIfNeeded(isKeyRequired(), 1, TimeUnit.HOURS)
                    .url(forecastRequestUri)
                    .build()

                val alertsRequestUri = ALERTS_QUERY_URL.format(query).toUri().buildUpon()
                    .appendQueryParameter("tz", "Etc/UTC")
                    .build()
                    .toString()

                val alertsRequest = Request.Builder()
                    .cacheRequestIfNeeded(isKeyRequired(), 1, TimeUnit.HOURS)
                    .url(alertsRequestUri)
                    .build()

                // Connect to webstream
                currentResponse = client.newCall(currentRequest).await()
                checkForErrors(currentResponse)

                forecastResponse = client.newCall(forecastRequest).await()
                checkForErrors(forecastResponse)

                alertsResponse = client.newCall(alertsRequest).await()
                checkForErrors(alertsResponse)

                val currentStream = currentResponse.getStream()
                val forecastStream = forecastResponse.getStream()
                val alertStream = alertsResponse.getStream()

                // Load weather
                val currRoot = JSONParser.deserializer<CurrentResponse>(
                    currentStream,
                    CurrentResponse::class.java
                )
                val foreRoot = JSONParser.deserializer<ForecastResponse>(
                    forecastStream,
                    ForecastResponse::class.java
                )
                val alertsRoot = JSONParser.deserializer<AlertsResponse>(
                    alertStream,
                    AlertsResponse::class.java
                )

                // End Stream
                currentStream.closeQuietly()
                forecastStream.closeQuietly()
                alertStream.closeQuietly()

                requireNotNull(currRoot) { "currRoot is null" }
                requireNotNull(foreRoot) { "foreRoot is null" }

                weather = createWeatherData(currRoot, foreRoot, location)
                weather.weatherAlerts = createWeatherAlerts(alertsRoot)
            } catch (ex: Exception) {
                weather = null
                if (ex is IOException) {
                    wEx = WeatherException(ErrorStatus.NETWORKERROR, ex)
                } else if (ex is WeatherException) {
                    wEx = ex
                }
                Logger.writeLine(
                    Log.ERROR,
                    ex,
                    "BrightSkyProvider: error getting weather data"
                )
            } finally {
                currentResponse?.closeQuietly()
                forecastResponse?.closeQuietly()
                alertsResponse?.closeQuietly()
            }

            if (wEx == null && weather.isNullOrInvalid()) {
                wEx = WeatherException(ErrorStatus.NOWEATHER)
            } else if (weather != null) {
                weather.query = query
            }

            if (wEx != null) throw wEx

            return@withContext weather!!
        }

    @Throws(WeatherException::class)
    override suspend fun updateWeatherData(location: LocationData, weather: Weather) {
        super.updateWeatherData(location, weather)

        // DWD reports datetime in UTC; add location tz_offset
        val offset = location.tzOffset
        weather.updateTime = weather.updateTime!!.withZoneSameInstant(offset)
        weather.condition!!.observationTime =
            weather.condition!!.observationTime.withZoneSameInstant(offset)

        // Calculate astronomy
        weather.astronomy = try {
            SunMoonCalcProvider().getAstronomyData(location, weather.condition!!.observationTime)
        } catch (e: WeatherException) {
            SolCalcAstroProvider().getAstronomyData(location, weather.condition!!.observationTime)
        }

        // Update icons
        val now = ZonedDateTime.now(ZoneOffset.UTC).withZoneSameInstant(offset).toLocalTime()
        val sunrise = weather.astronomy!!.sunrise.toLocalTime()
        val sunset = weather.astronomy!!.sunset.toLocalTime()

        weather.condition!!.icon =
            getWeatherIcon(now.isBefore(sunrise) || now.isAfter(sunset), weather.condition!!.icon)

        if (weather.condition!!.weather.isNullOrBlank()) {
            weather.condition!!.weather = getWeatherCondition(weather.condition!!.icon)
        }

        for (forecast in weather.forecast!!) {
            forecast.icon.let {
                forecast.icon = getWeatherIcon(it)
                forecast.condition = getWeatherCondition(forecast.icon)
            }
        }

        for (hr_forecast in weather.hrForecast!!) {
            val hrfDate = hr_forecast.date.withZoneSameInstant(offset)
            hr_forecast.date = hrfDate

            val hrfLocalTime = hrfDate.toLocalTime()

            hr_forecast.icon.let {
                hr_forecast.icon = getWeatherIcon(
                    hrfLocalTime.isBefore(sunrise) || hrfLocalTime.isAfter(sunset),
                    it
                )
                hr_forecast.condition = getWeatherCondition(hr_forecast.icon)
            }
        }

        if (!weather.weatherAlerts.isNullOrEmpty()) {
            for (alert in weather.weatherAlerts) {
                alert.date = alert.date.withZoneSameInstant(offset)
                alert.expiresDate = alert.expiresDate.withZoneSameInstant(offset)
            }
        }
    }

    override suspend fun updateLocationQuery(weather: Weather): String {
        val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
        df.applyPattern("0.####")
        return String.format(
            Locale.ROOT,
            "lat=%s&lon=%s",
            df.format(weather.location!!.latitude),
            df.format(weather.location!!.longitude)
        )
    }

    override suspend fun updateLocationQuery(location: LocationData): String {
        val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
        df.applyPattern("0.####")
        return String.format(
            Locale.ROOT,
            "lat=%s&lon=%s",
            df.format(location.latitude),
            df.format(location.longitude)
        )
    }

    override fun localeToLangCode(iso: String, name: String): String {
        if (iso == "de")
            return iso

        return "en"
    }

    override fun getWeatherIcon(icon: String?): String {
        var isNight = false

        if (icon == null) return WeatherIcons.NA

        if (icon.endsWith("-night")) isNight = true

        return getWeatherIcon(isNight, icon)
    }

    override fun getWeatherIcon(isNight: Boolean, icon: String?): String {
        var weatherIcon = ""

        if (icon == null) return WeatherIcons.NA

        weatherIcon = when (icon) {
            "clear-day", "clear-night" -> {
                if (isNight) WeatherIcons.NIGHT_CLEAR else WeatherIcons.DAY_SUNNY
            }

            "partly-cloudy-day", "partly-cloudy-night" -> {
                if (isNight) WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY else WeatherIcons.DAY_PARTLY_CLOUDY
            }

            "cloudy" -> WeatherIcons.CLOUDY
            "fog" -> WeatherIcons.FOG
            "wind" -> WeatherIcons.WINDY
            "rain" -> WeatherIcons.RAIN
            "sleet" -> WeatherIcons.SLEET
            "snow" -> WeatherIcons.SNOW
            "hail" -> WeatherIcons.HAIL
            "thunderstorm" -> WeatherIcons.THUNDERSTORM
            else -> {
                logMissingIcon(icon)
                if (isNight) WeatherIcons.NIGHT_CLEAR else WeatherIcons.DAY_SUNNY
            }
        }

        if (weatherIcon.isBlank()) {
            // Not Available
            logMissingIcon(icon)
            weatherIcon = WeatherIcons.NA
        }

        return weatherIcon
    }

    override fun isNight(weather: Weather): Boolean {
        var isNight = super.isNight(weather)

        if (!isNight) {
            // Fallback to sunset/rise time just in case
            var tz: ZoneOffset? = null
            if (!weather.location!!.tzLong.isNullOrBlank()) {
                val id = ZoneIdCompat.of(weather.location!!.tzLong)
                tz = id.rules.getOffset(Instant.now())
            }
            if (tz == null) {
                tz = weather.location!!.tzOffset
            }

            val sunrise = weather.astronomy?.sunrise?.toLocalTime() ?: LocalTime.of(6, 0)
            val sunset = weather.astronomy?.sunset?.toLocalTime() ?: LocalTime.of(18, 0)

            val now = ZonedDateTime.now(tz).toLocalTime()

            // Determine whether its night using sunset/rise times
            if (now.toNanoOfDay() < sunrise.toNanoOfDay() || now.toNanoOfDay() > sunset.toNanoOfDay()) isNight =
                true
        }

        return isNight
    }
}