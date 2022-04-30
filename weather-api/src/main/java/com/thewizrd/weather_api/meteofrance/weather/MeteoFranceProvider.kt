package com.thewizrd.weather_api.meteofrance.weather

import android.util.Log
import com.ibm.icu.util.ULocale
import com.thewizrd.shared_resources.exceptions.ErrorStatus
import com.thewizrd.shared_resources.exceptions.WeatherException
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.await
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.getStream
import com.thewizrd.shared_resources.remoteconfig.remoteConfigService
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.shared_resources.weatherdata.model.isNullOrInvalid
import com.thewizrd.weather_api.extras.cacheRequestIfNeeded
import com.thewizrd.weather_api.keys.Keys
import com.thewizrd.weather_api.locationiq.LocationIQProvider
import com.thewizrd.weather_api.nws.SolCalcAstroProvider
import com.thewizrd.weather_api.smc.SunMoonCalcProvider
import com.thewizrd.weather_api.utils.APIRequestUtils.checkForErrors
import com.thewizrd.weather_api.utils.APIRequestUtils.checkRateLimit
import com.thewizrd.weather_api.weatherModule
import com.thewizrd.weather_api.weatherdata.WeatherProviderImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly
import java.io.IOException
import java.io.InputStream
import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.TimeUnit

class MeteoFranceProvider : WeatherProviderImpl() {
    companion object {
        private const val BASE_URL = "https://webservice.meteofrance.com/"
        private const val CURRENT_QUERY_URL = BASE_URL + "observation/gridded?%s&lang=%s&token=%s"
        private const val FORECAST_QUERY_URL = BASE_URL + "forecast?%s&lang=%s&token=%s"
        private const val ALERTS_QUERY_URL = BASE_URL + "warning/full?domain=%s&token=%s"
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
        return WeatherAPI.METEOFRANCE
    }

    override fun supportsWeatherLocale(): Boolean {
        return true
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

    override fun isRegionSupported(countryCode: String?): Boolean {
        return LocationUtils.isFrance(countryCode)
    }

    override fun getHourlyForecastInterval(): Int {
        return 1
    }

    override suspend fun isKeyValid(key: String?): Boolean {
        return false
    }

    override fun getAPIKey(): String? {
        return Keys.getMeteoFranceKey()
    }

    override fun getRetryTime(): Long {
        return 60000
    }

    @Throws(WeatherException::class)
    override suspend fun getWeather(location_query: String, country_code: String): Weather =
        withContext(Dispatchers.IO) {
            var weather: Weather?

            // MeteoFrance only supports locations in France
            if (!LocationUtils.isFrance(country_code)) {
                throw WeatherException(ErrorStatus.QUERYNOTFOUND)
            }

            val uLocale = ULocale.forLocale(LocaleUtils.getLocale())
            val locale = localeToLangCode(uLocale.language, uLocale.toLanguageTag())

            val key = getAPIKey()

            val client = sharedDeps.httpClient
            var currentResponse: Response? = null
            var forecastResponse: Response? = null
            var alertsResponse: Response? = null
            var wEx: WeatherException? = null

            try {
                // If were under rate limit, deny request
                checkRateLimit()

                val currentRequest = Request.Builder()
                    .cacheRequestIfNeeded(isKeyRequired(), 15, TimeUnit.MINUTES)
                    .url(String.format(CURRENT_QUERY_URL, location_query, locale, key))
                    .build()
                val forecastRequest = Request.Builder()
                    .cacheRequestIfNeeded(isKeyRequired(), 1, TimeUnit.HOURS)
                    .url(String.format(FORECAST_QUERY_URL, location_query, locale, key))
                    .build()

                // Connect to webstream
                currentResponse = client.newCall(currentRequest).await()
                checkForErrors(currentResponse)

                forecastResponse = client.newCall(forecastRequest).await()
                checkForErrors(forecastResponse)

                val currentStream = currentResponse.getStream()
                val forecastStream = forecastResponse.getStream()
                var alertStream: InputStream? = null

                // Load weather
                val currRoot = JSONParser.deserializer<CurrentsResponse>(
                    currentStream,
                    CurrentsResponse::class.java
                )
                val foreRoot = JSONParser.deserializer<ForecastResponse>(
                    forecastStream,
                    ForecastResponse::class.java
                )
                var alertsRoot: AlertsResponse? = null

                if (foreRoot.position?.dept != null) {
                    val alertsRequest = Request.Builder()
                        .cacheRequestIfNeeded(isKeyRequired(), 1, TimeUnit.HOURS)
                        .url(String.format(ALERTS_QUERY_URL, foreRoot.position!!.dept, key))
                        .build()

                    alertsResponse = client.newCall(alertsRequest).await()
                    alertStream = alertsResponse.getStream()
                    alertsRoot = JSONParser.deserializer<AlertsResponse>(
                        alertStream,
                        AlertsResponse::class.java
                    )
                }

                // End Stream
                currentStream.closeQuietly()
                forecastStream.closeQuietly()
                alertStream?.closeQuietly()

                weather = createWeatherData(currRoot, foreRoot, alertsRoot)
            } catch (ex: Exception) {
                weather = null
                if (ex is IOException) {
                    wEx = WeatherException(ErrorStatus.NETWORKERROR)
                } else if (ex is WeatherException) {
                    wEx = ex
                }
                Logger.writeLine(
                    Log.ERROR,
                    ex,
                    "MeteoFranceProvider: error getting weather data"
                )
            } finally {
                currentResponse?.closeQuietly()
                forecastResponse?.closeQuietly()
                alertsResponse?.closeQuietly()
            }

            if (wEx == null && weather.isNullOrInvalid()) {
                wEx = WeatherException(ErrorStatus.NOWEATHER)
            } else if (weather != null) {
                if (supportsWeatherLocale()) weather.locale = locale

                weather.query = location_query
            }

            if (wEx != null) throw wEx

            return@withContext weather!!
        }

    @Throws(WeatherException::class)
    override suspend fun updateWeatherData(location: LocationData, weather: Weather) {
        // MeteoFrance reports datetime in UTC; add location tz_offset
        val offset = location.tzOffset
        weather.updateTime = weather.updateTime.withZoneSameInstant(offset)
        weather.condition.observationTime =
            weather.condition.observationTime.withZoneSameInstant(offset)

        // Calculate astronomy
        weather.astronomy = try {
            SunMoonCalcProvider().getAstronomyData(location, weather.condition.observationTime)
        } catch (e: WeatherException) {
            Logger.writeLine(Log.ERROR, e)
            SolCalcAstroProvider().getAstronomyData(location, weather.condition.observationTime)
        }

        // Update icons
        val now = ZonedDateTime.now(ZoneOffset.UTC).withZoneSameInstant(offset).toLocalTime()
        val sunrise = weather.astronomy.sunrise.toLocalTime()
        val sunset = weather.astronomy.sunset.toLocalTime()

        weather.condition.icon =
            getWeatherIcon(now.isBefore(sunrise) || now.isAfter(sunset), weather.condition.icon)

        for (forecast in weather.forecast) {
            forecast.date = forecast.date.plusSeconds(offset.totalSeconds.toLong())
        }

        for (hr_forecast in weather.hrForecast) {
            val hrf_date = hr_forecast.date.withZoneSameInstant(offset)
            hr_forecast.date = hrf_date
            val hrf_localTime = hrf_date.toLocalTime()
            hr_forecast.icon = getWeatherIcon(
                hrf_localTime.isBefore(sunrise) || hrf_localTime.isAfter(sunset),
                hr_forecast.icon
            )
        }

        if (!weather.weatherAlerts.isNullOrEmpty()) {
            for (alert in weather.weatherAlerts) {
                if (alert.date.offset != offset) {
                    alert.date = alert.date.withZoneSameLocal(offset)
                }

                if (alert.expiresDate.offset != offset) {
                    alert.expiresDate = alert.expiresDate.withZoneSameLocal(offset)
                }
            }
        }
    }

    override fun updateLocationQuery(weather: Weather): String {
        val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
        df.applyPattern("0.####")
        return String.format(
            Locale.ROOT,
            "lat=%s&lon=%s",
            df.format(weather.location.latitude),
            df.format(weather.location.longitude)
        )
    }

    override fun updateLocationQuery(location: LocationData): String {
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
        if (iso == "fr")
            return iso

        return "en"
    }

    override fun getWeatherIcon(icon: String?): String {
        var isNight = false

        if (icon == null) return WeatherIcons.NA

        if (icon.endsWith("n")) isNight = true

        return getWeatherIcon(isNight, icon)
    }

    // Icon reference
    // https://meteofrance.com/modules/custom/mf_tools_common_theme_public/svg/weather/pxx.svg
    override fun getWeatherIcon(isNight: Boolean, icon: String?): String {
        var weatherIcon = ""

        if (icon == null) return WeatherIcons.NA

        if (icon.startsWith("p10bis")) {
            // Sprinkle
            weatherIcon =
                if (isNight) WeatherIcons.NIGHT_ALT_SPRINKLE else WeatherIcons.DAY_SPRINKLE
        } else if (icon.startsWith("p10")) {
            // Light rain?
            weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_SHOWERS else WeatherIcons.DAY_SHOWERS
        } else if (icon.startsWith("p11")) {
            // Light rain?
            weatherIcon = WeatherIcons.SHOWERS
        } else if (icon.startsWith("p12")) {
            // Rain
            weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_RAIN else WeatherIcons.DAY_RAIN
        } else if (icon.startsWith("p13") || icon.startsWith("p14")) {
            // Rain
            weatherIcon = WeatherIcons.RAIN
        } else if (icon.startsWith("p15")) {
            // Heavy Rain
            weatherIcon = WeatherIcons.RAIN_WIND
        } else if (icon.startsWith("p16") || icon.startsWith("p25")) {
            // Storm Showers
            weatherIcon = WeatherIcons.STORM_SHOWERS
        } else if (icon.startsWith("p17") || icon.startsWith("p21")) {
            // Flurries?
            weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_SNOW else WeatherIcons.DAY_SNOW
        } else if (icon.startsWith("p18") || icon.startsWith("p22")) {
            // Flurries?
            weatherIcon = WeatherIcons.SNOW
        } else if (icon.startsWith("p19")) {
            // Sleet / Rain mix
            weatherIcon =
                if (isNight) WeatherIcons.NIGHT_ALT_RAIN_MIX else WeatherIcons.DAY_RAIN_MIX
        } else if (icon.startsWith("p20")) {
            // Sleet / Rain mix
            weatherIcon = WeatherIcons.RAIN_MIX
        } else if (icon.startsWith("p21") || icon.startsWith("p22") || icon.startsWith("p23")) {
            // Snow
            weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_SNOW else WeatherIcons.DAY_SNOW
        } else if (icon.startsWith("p23")) {
            // Heavy Snow
            weatherIcon = WeatherIcons.SNOW_WIND
        } else if (icon.startsWith("p24") || icon.startsWith("p25")) {
            // Thundershowers / T-storms
            weatherIcon =
                if (isNight) WeatherIcons.NIGHT_ALT_STORM_SHOWERS else WeatherIcons.DAY_STORM_SHOWERS
        } else if (icon.startsWith("p26") || icon.startsWith("p28")) {
            // Thunder
            weatherIcon =
                if (isNight) WeatherIcons.NIGHT_ALT_LIGHTNING else WeatherIcons.DAY_LIGHTNING
        } else if (icon.startsWith("p27") || icon.startsWith("p29")) {
            // Thunder
            weatherIcon = WeatherIcons.LIGHTNING
        } else if (icon.startsWith("p30")) {
            // Snow/Thunder
            weatherIcon =
                if (isNight) WeatherIcons.NIGHT_ALT_SNOW_THUNDERSTORM else WeatherIcons.DAY_SNOW_THUNDERSTORM
        } else if (icon.startsWith("p32") || icon.startsWith("p33")) {
            // Tornado
            weatherIcon = WeatherIcons.TORNADO
        } else if (icon.startsWith("p34")) {
            // Hurricane
            weatherIcon = WeatherIcons.HURRICANE
        } else if (icon == "p1" || icon == "p1j" || icon == "p1n") {
            // Clear
            weatherIcon = if (isNight) WeatherIcons.NIGHT_CLEAR else WeatherIcons.DAY_SUNNY
        } else if (icon.startsWith("p1bis") || icon.startsWith("p2")) {
            // Partly Cloudy
            weatherIcon =
                if (isNight) WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY else WeatherIcons.DAY_PARTLY_CLOUDY
        } else if (icon.startsWith("p2")) {
            // Mostly Cloudy
            weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_CLOUDY else WeatherIcons.DAY_CLOUDY
        } else if (icon.startsWith("p3")) {
            // Mostly Cloudy
            weatherIcon = WeatherIcons.CLOUDY
        } else if (icon.startsWith("p4")) {
            // Haze
            weatherIcon = if (isNight) WeatherIcons.NIGHT_FOG else WeatherIcons.DAY_HAZE
        } else if (icon.startsWith("p5")) {
            // Fog
            weatherIcon = if (isNight) WeatherIcons.NIGHT_FOG else WeatherIcons.DAY_FOG
        } else if (icon.startsWith("p6") || icon.startsWith("p7") || icon.startsWith("p8")) {
            // Fog / Heavy Fog
            weatherIcon = WeatherIcons.FOG
        } else if (icon.startsWith("p9")) {
            // Sprinkle
            weatherIcon = WeatherIcons.SPRINKLE
        } else {
            weatherIcon = if (isNight) WeatherIcons.NIGHT_CLEAR else WeatherIcons.DAY_SUNNY
        }

        if (weatherIcon.isBlank()) {
            // Not Available
            weatherIcon = WeatherIcons.NA
        }

        return weatherIcon
    }

    override fun isNight(weather: Weather): Boolean {
        var isNight = super.isNight(weather)

        if (!isNight) {
            // Fallback to sunset/rise time just in case
            var tz: ZoneOffset? = null
            if (!weather.location.tzLong.isNullOrBlank()) {
                val id = ZoneIdCompat.of(weather.location.tzLong)
                tz = id.rules.getOffset(Instant.now())
            }
            if (tz == null) {
                tz = weather.location.tzOffset
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