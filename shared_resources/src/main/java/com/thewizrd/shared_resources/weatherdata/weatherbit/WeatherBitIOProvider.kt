package com.thewizrd.shared_resources.weatherdata.weatherbit

import android.util.Log
import com.ibm.icu.util.ULocale
import com.thewizrd.shared_resources.SimpleLibrary
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.locationiq.LocationIQProvider
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.await
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.getStream
import com.thewizrd.shared_resources.remoteconfig.RemoteConfig
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.utils.APIRequestUtils.checkForErrors
import com.thewizrd.shared_resources.utils.APIRequestUtils.checkRateLimit
import com.thewizrd.shared_resources.utils.APIRequestUtils.throwIfRateLimited
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.WeatherAlertProviderInterface
import com.thewizrd.shared_resources.weatherdata.WeatherProviderImpl
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlert
import com.thewizrd.shared_resources.weatherdata.model.isNullOrInvalid
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

class WeatherBitIOProvider : WeatherProviderImpl(), WeatherAlertProviderInterface {
    companion object {
        private const val BASE_URL = "https://api.weatherbit.io/v2.0/"
        private const val KEYCHECK_QUERY_URL =
            BASE_URL + "current?key=%s"
        private const val CURRENT_QUERY_URL =
            BASE_URL + "current?%s&lang=%s&units=M&include=minutely,alerts&key=%s"
        private const val FORECAST_QUERY_URL =
            BASE_URL + "forecast/daily?%s&lang=%s&units=M&key=%s"
        private const val ALERTS_QUERY_URL =
            BASE_URL + "alerts?%s&lang=%s&units=M&key=%s"
    }

    init {
        mLocationProvider = RemoteConfig.getLocationProvider(getWeatherAPI())
            ?: LocationIQProvider()
    }

    override fun getWeatherAPI(): String {
        return WeatherAPI.WEATHERBITIO
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
        return 0
    }

    @Throws(WeatherException::class)
    override suspend fun isKeyValid(key: String?): Boolean = withContext(Dispatchers.IO) {
        if (key.isNullOrBlank()) {
            throw WeatherException(ErrorStatus.INVALIDAPIKEY)
        }

        var isValid = false
        var wEx: WeatherException? = null

        val client = SimpleLibrary.instance.httpClient
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
                HttpURLConnection.HTTP_UNAUTHORIZED,
                HttpURLConnection.HTTP_FORBIDDEN -> {
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
        return null
    }

    @Throws(WeatherException::class)
    override suspend fun getWeather(location_query: String, country_code: String): Weather =
        withContext(Dispatchers.IO) {
            var weather: Weather?

            val uLocale = ULocale.forLocale(LocaleUtils.getLocale())
            val locale = localeToLangCode(uLocale.language, uLocale.toLanguageTag())

            val settingsMgr = SimpleLibrary.instance.app.settingsManager
            val key =
                if (settingsMgr.usePersonalKey()) settingsMgr.getAPIKey(getWeatherAPI()) else getAPIKey()

            val client = SimpleLibrary.instance.httpClient
            var currentResponse: Response? = null
            var forecastResponse: Response? = null
            var wEx: WeatherException? = null

            try {
                // If were under rate limit, deny request
                checkRateLimit()

                val currentRequest = Request.Builder()
                    .cacheControl(
                        CacheControl.Builder()
                            .maxAge(30, TimeUnit.MINUTES)
                            .build()
                    )
                    .url(String.format(CURRENT_QUERY_URL, location_query, locale, key))
                    .build()
                val forecastRequest = Request.Builder()
                    .cacheControl(
                        CacheControl.Builder()
                            .maxAge(1, TimeUnit.HOURS)
                            .build()
                    )
                    .url(String.format(FORECAST_QUERY_URL, location_query, locale, key))
                    .build()

                // Connect to webstream
                currentResponse = client.newCall(currentRequest).await()
                checkForErrors(currentResponse)

                forecastResponse = client.newCall(forecastRequest).await()
                checkForErrors(forecastResponse)

                val currentStream = currentResponse.getStream()
                val forecastStream = forecastResponse.getStream()

                // Load weather
                val currRoot = JSONParser.deserializer<CurrentResponse>(
                    currentStream,
                    CurrentResponse::class.java
                )
                val foreRoot = JSONParser.deserializer<ForecastResponse>(
                    forecastStream,
                    ForecastResponse::class.java
                )

                // End Stream
                currentStream.closeQuietly()
                forecastStream.closeQuietly()

                weather = createWeatherData(currRoot, foreRoot)
            } catch (ex: Exception) {
                weather = null
                if (ex is IOException) {
                    wEx = WeatherException(ErrorStatus.NETWORKERROR).apply {
                        initCause(ex)
                    }
                } else if (ex is WeatherException) {
                    wEx = ex
                }
                Logger.writeLine(Log.ERROR, ex, "WeatherBitIOProvider: error getting weather data")
            } finally {
                currentResponse?.closeQuietly()
                forecastResponse?.closeQuietly()
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
    override suspend fun getAlerts(location: LocationData): Collection<WeatherAlert> =
        withContext(Dispatchers.IO) {
            var alerts: Collection<WeatherAlert>? = null

            val settingsMgr = SimpleLibrary.instance.app.settingsManager
            val key =
                if (settingsMgr.usePersonalKey()) settingsMgr.getAPIKey(getWeatherAPI()) else getAPIKey()

            val client = SimpleLibrary.instance.httpClient
            var response: Response? = null

            try {
                // If were under rate limit, deny request
                checkRateLimit()

                val request = Request.Builder()
                    .cacheControl(
                        CacheControl.Builder()
                            .maxAge(6, TimeUnit.HOURS)
                            .build()
                    )
                    .url(
                        String.format(
                            ALERTS_QUERY_URL,
                            updateLocationQuery(location),
                            key
                        )
                    )
                    .build()

                // Connect to webstream
                response = client.newCall(request).await()
                checkForErrors(response)

                val stream = response.getStream()

                // Load weather
                val root = JSONParser.deserializer<AlertsResponse>(
                    stream,
                    AlertsResponse::class.java
                )

                // End Stream
                stream.closeQuietly()

                alerts = createWeatherAlerts(root.alerts, root.timezone!!)
            } catch (ex: Exception) {
                Logger.writeLine(
                    Log.ERROR,
                    ex,
                    "WeatherBitIOProvider: error getting weather alert data"
                )
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
        val code = when (iso) {
            "ar", // Arabic
            "az", // Azerbaijani
            "be", // Belarusian
            "bg", // Bulgarian
            "bs", // Bosnian
            "ca", // Catalan
            "da", // Danish
            "de", // German
            "fi", // Finnish
            "fr", // French
            "el", // Greek
            "es", // Spanish
            "et", // Estonian
            "ja", // Japanese
            "hr", // Croatian
            "hu", // Hungarian
            "id", // Indonesian
            "it", // Italian
            "is", // Icelandic
            "kw", // Cornish
            "lt", // Lithuanian
            "nb", // Norwegian Bokmål
            "nl", // Dutch
            "pl", // Polish
            "pt", // Portuguese
            "ro", // Romanian
            "ru", // Russian
            "sk", // Slovak
            "sl", // Slovenian
            "sr", // Serbian
            "sv", // Swedish
            "tr", // Turkish
            "uk", // Ukrainian
            -> iso
            // Chinese
            "zh" -> when (name) {
                "zh-Hant",
                "zh-HK",
                "zh-MO",
                "zh-TW" -> {
                    "zh-tw"
                }
                else -> {
                    "zh"
                }
            }
            "cs" -> "cz" // Czech
            "he" -> "iw" // Hebrew
            else ->
                // Default is English
                "en"
        }

        return code
    }

    override fun getWeatherIcon(icon: String?): String {
        var isNight = false

        if (icon == null) return WeatherIcons.NA

        if (icon.endsWith("n")) isNight = true

        return getWeatherIcon(isNight, icon)
    }

    override fun getWeatherIcon(isNight: Boolean, icon: String?): String {
        var weatherIcon = ""

        if (icon == null) return WeatherIcons.NA

        if (icon.startsWith("t01") || icon.startsWith("t02") || icon.startsWith("t03")) {
            // t01: 200	Thunderstorm with light rain
            // t02: 201	Thunderstorm with rain
            // t03: 202	Thunderstorm with heavy rain
            weatherIcon =
                if (isNight) WeatherIcons.NIGHT_ALT_STORM_SHOWERS else WeatherIcons.DAY_STORM_SHOWERS
        } else if (icon.startsWith("t04") || icon.startsWith("t05")) {
            // t04: 230	Thunderstorm with light drizzle | 231 Thunderstorm with drizzle | 232 Thunderstorm with heavy drizzle
            // t05: 233	Thunderstorm with Hail
            weatherIcon =
                if (isNight) WeatherIcons.NIGHT_ALT_THUNDERSTORM else WeatherIcons.DAY_THUNDERSTORM
        } else if (icon.startsWith("d0")) {
            // d01: 300	Light Drizzle
            // d02: 301	Drizzle
            // d03: 302	Heavy Drizzle
            weatherIcon =
                if (isNight) WeatherIcons.NIGHT_ALT_SPRINKLE else WeatherIcons.DAY_SPRINKLE
        } else if (icon.startsWith("r01") || icon.startsWith("r02") || icon.startsWith("u00")) {
            // r01: 500	Light Rain
            // r02: 501	Moderate Rain
            // u00: 900	Unknown Precipitation
            weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_RAIN else WeatherIcons.DAY_RAIN
        } else if (icon.startsWith("r03")) {
            // r03: 502	Heavy Rain
            weatherIcon =
                if (isNight) WeatherIcons.NIGHT_ALT_RAIN_WIND else WeatherIcons.DAY_RAIN_WIND
        } else if (icon.startsWith("f01") || icon.startsWith("s04")) {
            // f01: 511	Freezing rain
            // s04: 610	Mix snow/rain
            weatherIcon =
                if (isNight) WeatherIcons.NIGHT_ALT_RAIN_MIX else WeatherIcons.DAY_RAIN_MIX
        } else if (icon.startsWith("r04") || icon.startsWith("r05") || icon.startsWith("r06")) {
            // r04: 520	Light shower rain
            // r05: 521	Shower rain
            // r06: 522	Heavy shower rain
            weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_SHOWERS else WeatherIcons.DAY_SHOWERS
        } else if (icon.startsWith("s01") || icon.startsWith("s02") || icon.startsWith("s03") || icon.startsWith(
                "s06"
            )
        ) {
            // s01: 600	Light snow | 621 Snow shower
            // s02: 601	Snow | 622 Heavy snow shower
            // s03: 602	Heavy Snow
            // s06: 623	Flurries
            weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_SNOW else WeatherIcons.DAY_SNOW
        } else if (icon.startsWith("s05")) {
            // s05: 611	Sleet | 612	Heavy sleet
            weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_SLEET else WeatherIcons.DAY_SLEET
        } else if (icon.startsWith("a01") || icon.startsWith("a05") || icon.startsWith("a06")) {
            // a01: 700	Mist
            // a05: 741	Fog
            // a06: 751 Freezing Fog
            weatherIcon = if (isNight) WeatherIcons.NIGHT_FOG else WeatherIcons.DAY_FOG
        } else if (icon.startsWith("a02")) {
            // a02: 711	Smoke
            weatherIcon = WeatherIcons.SMOKE
        } else if (icon.startsWith("a03")) {
            // a03: 721	Haze
            weatherIcon = if (isNight) WeatherIcons.NIGHT_FOG else WeatherIcons.DAY_HAZE
        } else if (icon.startsWith("a04")) {
            // a04: 731	Sand/dust
            weatherIcon = WeatherIcons.DUST
        } else if (icon.startsWith("c01")) {
            // c01: 800	Clear sky
            weatherIcon = if (isNight) WeatherIcons.NIGHT_CLEAR else WeatherIcons.DAY_SUNNY
        } else if (icon.startsWith("c01") || icon.startsWith("c02")) {
            // c02: 801	Few clouds
            // c03: 802	Scattered clouds
            weatherIcon =
                if (isNight) WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY else WeatherIcons.DAY_PARTLY_CLOUDY
        } else if (icon.startsWith("c03")) {
            // c03: 803	Broken clouds
            weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_CLOUDY else WeatherIcons.DAY_CLOUDY
        } else if (icon.startsWith("c04")) {
            // c04: 804	Overcast clouds
            weatherIcon =
                if (isNight) WeatherIcons.NIGHT_OVERCAST else WeatherIcons.DAY_SUNNY_OVERCAST
        } else if (icon.startsWith("u00")) {
            // c04: 804	Overcast clouds
            weatherIcon =
                if (isNight) WeatherIcons.NIGHT_OVERCAST else WeatherIcons.DAY_SUNNY_OVERCAST
        } else {
            weatherIcon = if (isNight) WeatherIcons.NIGHT_CLEAR else WeatherIcons.DAY_SUNNY
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

        when (weather.condition.icon) {
            // The following cases can be present at any time of day
            WeatherIcons.SMOKE,
            WeatherIcons.DUST -> {
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
            }
        }

        return isNight
    }
}