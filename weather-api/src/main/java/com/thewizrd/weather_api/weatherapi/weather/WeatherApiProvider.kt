package com.thewizrd.weather_api.weatherapi.weather

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
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.utils.ZoneIdCompat
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.WeatherAlertProvider
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlert
import com.thewizrd.shared_resources.weatherdata.model.isNullOrInvalid
import com.thewizrd.weather_api.extras.cacheRequestIfNeeded
import com.thewizrd.weather_api.keys.Keys
import com.thewizrd.weather_api.locationiq.LocationIQProvider
import com.thewizrd.weather_api.utils.APIRequestUtils.checkForErrors
import com.thewizrd.weather_api.utils.APIRequestUtils.checkRateLimit
import com.thewizrd.weather_api.utils.APIRequestUtils.throwIfRateLimited
import com.thewizrd.weather_api.weatherModule
import com.thewizrd.weather_api.weatherdata.WeatherProviderImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly
import java.io.IOException
import java.net.HttpURLConnection
import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.TimeUnit

class WeatherApiProvider : WeatherProviderImpl(), WeatherAlertProvider {
    companion object {
        private const val BASE_URL = "https://api.weatherapi.com/v1/"
        private const val KEYCHECK_QUERY_URL = BASE_URL + "forecast.json?key=%s"
        private const val WEATHER_QUERY_URL =
            BASE_URL + "forecast.json?q=%s&days=10&aqi=yes&alerts=yes&lang=%s&key=%s"
        private const val ALERTS_QUERY_URL =
            BASE_URL + "forecast.json?q=%s&days=1&hour=6&aqi=no&alerts=yes&lang=%s&key=%s"
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
        return WeatherAPI.WEATHERAPI
    }

    override fun supportsWeatherLocale(): Boolean {
        return true
    }

    override fun supportsAlerts(): Boolean {
        return true
    }

    override fun needsExternalAlertData(): Boolean {
        return false
    }

    override fun isKeyRequired(): Boolean {
        return true
    }

    override fun getHourlyForecastInterval(): Int {
        return 1
    }

    @Throws(WeatherException::class)
    override suspend fun isKeyValid(key: String?): Boolean = withContext(Dispatchers.IO) {
        if (key.isNullOrBlank()) {
            throw WeatherException(ErrorStatus.INVALIDAPIKEY)
        }

        var isValid = false
        var wEx: WeatherException? = null

        val client = sharedDeps.httpClient
        var response: Response? = null

        try {
            // If were under rate limit, deny request
            checkRateLimit()

            val request = Request.Builder()
                .cacheControl(
                    CacheControl.Builder()
                        .maxAge(1, TimeUnit.DAYS)
                        .build()
                )
                .url(String.format(KEYCHECK_QUERY_URL, key))
                .build()

            // Connect to webstream
            response = client.newCall(request).await()
            throwIfRateLimited(response)

            when (response.code) {
                HttpURLConnection.HTTP_BAD_REQUEST -> isValid = true
                HttpURLConnection.HTTP_UNAUTHORIZED -> {
                    wEx = WeatherException(ErrorStatus.INVALIDAPIKEY)
                    isValid = false
                }
            }
        } catch (ex: Exception) {
            if (ex is IOException) {
                wEx = WeatherException(ErrorStatus.NETWORKERROR).apply {
                    initCause(ex)
                }
            } else if (ex is WeatherException) {
                wEx = ex
            }

            isValid = false
        } finally {
            response?.closeQuietly()
        }

        if (wEx != null && wEx.errorStatus != ErrorStatus.INVALIDAPIKEY) {
            throw wEx
        }

        return@withContext isValid
    }

    override fun getAPIKey(): String? {
        return Keys.getWeatherApiKey()
    }

    @Throws(WeatherException::class)
    override suspend fun getWeather(location_query: String, country_code: String): Weather =
            withContext(Dispatchers.IO) {
                var weather: Weather?

                val uLocale = ULocale.forLocale(LocaleUtils.getLocale())
                val locale = localeToLangCode(uLocale.language, uLocale.toLanguageTag())

                val key =
                    if (settingsManager.usePersonalKey()) settingsManager.getAPIKey(getWeatherAPI()) else getAPIKey()

                val client = sharedDeps.httpClient
                var response: Response? = null
                var wEx: WeatherException? = null

                try {
                    // If were under rate limit, deny request
                    checkRateLimit()

                    val request = Request.Builder()
                        .cacheRequestIfNeeded(isKeyRequired(), 20, TimeUnit.MINUTES)
                        .url(String.format(WEATHER_QUERY_URL, location_query, locale, key))
                        .build()

                    // Connect to webstream
                    response = client.newCall(request).await()
                    checkForErrors(response)

                    val stream = response.getStream()

                    // Load weather
                    val root = JSONParser.deserializer<ForecastResponse>(
                        stream,
                        ForecastResponse::class.java
                    )

                    // End Stream
                    stream.closeQuietly()

                    weather = createWeatherData(root)
                } catch (ex: Exception) {
                    weather = null
                    if (ex is IOException) {
                        wEx = WeatherException(ErrorStatus.NETWORKERROR)
                    } else if (ex is WeatherException) {
                        wEx = ex
                    }
                    Logger.writeLine(Log.ERROR, ex, "WeatherApiProvider: error getting weather data")
                } finally {
                    response?.closeQuietly()
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
    override suspend fun getAlerts(location: LocationData): Collection<WeatherAlert>? =
        withContext(Dispatchers.IO) {
            var alerts: Collection<WeatherAlert>? = null

            val uLocale = ULocale.forLocale(LocaleUtils.getLocale())
            val locale = localeToLangCode(uLocale.language, uLocale.toLanguageTag())

            val key =
                if (settingsManager.usePersonalKey()) settingsManager.getAPIKey(getWeatherAPI()) else getAPIKey()

            val client = sharedDeps.httpClient
            var response: Response? = null

                try {
                    // If were under rate limit, deny request
                    checkRateLimit()

                    val request = Request.Builder()
                        .cacheRequestIfNeeded(isKeyRequired(), 30, TimeUnit.MINUTES)
                        .url(
                            String.format(
                                ALERTS_QUERY_URL,
                                updateLocationQuery(location),
                                locale,
                                key
                            )
                        )
                        .build()

                    // Connect to webstream
                    response = client.newCall(request).await()
                    checkForErrors(response)

                    val stream = response.getStream()

                    // Load weather
                    val root = JSONParser.deserializer<ForecastResponse>(
                        stream,
                        ForecastResponse::class.java
                    )

                    // End Stream
                    stream.closeQuietly()

                    alerts = createWeatherAlerts(root.alerts)
                } catch (ex: Exception) {
                    Logger.writeLine(Log.ERROR, ex, "WeatherApiProvider: error getting weather alert data")
                } finally {
                    response?.closeQuietly()
                }

                if (alerts == null) {
                    alerts = emptyList()
                }

                return@withContext alerts
            }

    @Throws(WeatherException::class)
    override suspend fun updateWeatherData(location: LocationData, weather: Weather) {
        // no-op
    }

    override fun updateLocationQuery(weather: Weather): String {
        val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
        df.applyPattern("0.####")
        return String.format(Locale.ROOT, "%s,%s", df.format(weather.location.latitude), df.format(weather.location.longitude))
    }

    override fun updateLocationQuery(location: LocationData): String {
        val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
        df.applyPattern("0.####")
        return String.format(Locale.ROOT, "%s,%s", df.format(location.latitude), df.format(location.longitude))
    }

    override fun localeToLangCode(iso: String, name: String): String {
        val code = when (iso) {
            // Chinese
            "zh" -> when (name) {
                // Chinese - Traditional
                "zh-Hant", "zh-HK", "zh-MO", "zh-TW" -> "zh_tw"
                // Mandarin
                "zh-cmn" -> "zh_cmn"
                // Wu
                "zh-wuu" -> "zh_wuu"
                // Xiang
                "zh-hsn" -> "zh_hsn"
                // Cantonese
                "zh-yue" -> "zh_yue"
                // Chinese - Simplified
                else -> "zh"
            }
            else -> iso
        }

        return code
    }

    override fun getWeatherIcon(icon: String?): String {
        return getWeatherIcon(false, icon)
    }

    override fun getWeatherIcon(isNight: Boolean, icon: String?): String {
        var weatherIcon = ""

        val conditionCode = icon?.toIntOrNull() ?: return WeatherIcons.NA

        when (conditionCode) {
            /* Sunny / Clear */
            1000 -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_CLEAR else WeatherIcons.DAY_SUNNY
            }

            /* Partly cloudy */
            1003 -> {
                weatherIcon =
                    if (isNight) WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY else WeatherIcons.DAY_PARTLY_CLOUDY
            }

            /* Overcast */
            1009 -> {
                weatherIcon =
                    if (isNight) WeatherIcons.NIGHT_OVERCAST else WeatherIcons.DAY_SUNNY_OVERCAST
            }

            /* Cloudy */
            1006 -> {
                weatherIcon =
                    if (isNight) WeatherIcons.NIGHT_ALT_CLOUDY else WeatherIcons.DAY_CLOUDY
            }

            /*
             * 1030: Mist
             * 1135: Fog
             * 1147: Freezing fog
             */
            1030, 1135, 1147 -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_FOG else WeatherIcons.DAY_FOG
            }

            /*
             * 1063: Patchy rain possible
             * 1186: Moderate rain at times
             * 1189: Moderate rain
             */
            1063, 1186, 1189 -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_RAIN else WeatherIcons.DAY_RAIN
            }

            /*
             * 1066: Patchy snow possible
             * 1210: Patchy light snow
             * 1213: Light snow
             * 1216: Patchy moderate snow
             * 1219: Moderate snow
             * 1255: Light snow showers
             * 1258: Moderate or heavy snow showers
             */
            1066, 1210, 1213, 1216, 1219, 1255, 1258 -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_SNOW else WeatherIcons.DAY_SNOW
            }

            /*
             * 1069: Patchy sleet possible
             * 1204: Light sleet
             * 1207: Moderate or heavy sleet
             * 1249: Light sleet showers
             * 1252: Moderate or heavy sleet showers
             */
            1069, 1204, 1207, 1249, 1252 -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_SLEET else WeatherIcons.DAY_SLEET
            }

            /*
             * 1072: Patchy freezing drizzle possible
             * 1168: Freezing drizzle
             * 1171: Heavy freezing drizzle
             * 1198: Light freezing rain
             * 1201: Moderate or heavy freezing rain
             */
            1072, 1168, 1171, 1198, 1201 -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_RAIN_MIX else WeatherIcons.DAY_RAIN_MIX
            }

            /* Thundery outbreaks possible */
            1087 -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_LIGHTNING else WeatherIcons.DAY_LIGHTNING
            }

            /*
             * 1114: Blowing snow
             * 1117: Blizzard
             * 1222: Patchy heavy snow
             * 1225: Heavy snow
             */
            1114, 1117, 1222, 1225 -> {
                weatherIcon =
                    if (isNight) WeatherIcons.NIGHT_ALT_SNOW_WIND else WeatherIcons.DAY_SNOW_WIND
            }

            /*
             * 1150: Patchy light drizzle
             * 1153: Light drizzle
             * 1180: Patchy light rain
             * 1183: Light rain
             */
            1150, 1153, 1180, 1183 -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_SPRINKLE else WeatherIcons.DAY_SPRINKLE
            }

            /*
             * 1192: Heavy rain at times
             * 1195: Heavy rain
             * 1243: Moderate or heavy rain shower
             * 1246: Torrential rain shower
             */
            1192, 1195, 1243, 1246 -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_RAIN_WIND else WeatherIcons.DAY_RAIN_WIND
            }

            /*
             * 1237: Ice pellets
             * 1261: Light showers of ice pellets
             * 1264: Moderate or heavy showers of ice pellets
             */
            1237, 1261, 1264 -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_HAIL else WeatherIcons.DAY_HAIL
            }

            /* 1240: Light rain shower */
            1240 -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_SHOWERS else WeatherIcons.DAY_SHOWERS
            }

            /* Patchy light rain with thunder */
            1273 -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_STORM_SHOWERS else WeatherIcons.DAY_STORM_SHOWERS
            }

            /* Moderate or heavy rain with thunder */
            1276 -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_THUNDERSTORM else WeatherIcons.DAY_THUNDERSTORM
            }

            /*
             * 1279: Patchy light snow with thunder
             * 1282: Moderate or heavy snow with thunder
             */
            1279, 1282 -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_SNOW_THUNDERSTORM else WeatherIcons.DAY_SNOW_THUNDERSTORM
            }
        }

        if (weatherIcon.isBlank()) {
            // Not Available
            weatherIcon = WeatherIcons.NA
        }
        return weatherIcon
    }

    // Some conditions can be for any time of day
    // So use sunrise/set data as fallback
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
            if (now.toNanoOfDay() < sunrise.toNanoOfDay() || now.toNanoOfDay() > sunset.toNanoOfDay())
                isNight = true
        }

        return isNight
    }
}