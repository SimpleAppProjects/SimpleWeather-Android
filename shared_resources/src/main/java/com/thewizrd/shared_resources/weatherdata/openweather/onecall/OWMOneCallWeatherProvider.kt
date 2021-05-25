package com.thewizrd.shared_resources.weatherdata.openweather.onecall

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
import com.thewizrd.shared_resources.utils.ExceptionUtils.copyStackTrace
import com.thewizrd.shared_resources.weatherdata.AirQualityProviderInterface
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.WeatherProviderImpl
import com.thewizrd.shared_resources.weatherdata.model.AirQuality
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.shared_resources.weatherdata.model.isNullOrInvalid
import com.thewizrd.shared_resources.weatherdata.smc.SunMoonCalcProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly
import java.io.IOException
import java.net.HttpURLConnection
import java.text.DecimalFormat
import java.time.*
import java.util.*
import java.util.concurrent.TimeUnit
import com.thewizrd.shared_resources.weatherdata.openweather.onecall.Rootobject as OneCallRootobject
import com.thewizrd.shared_resources.weatherdata.openweather.onecall.createWeatherData as createOneCallWeatherData

class OWMOneCallWeatherProvider : WeatherProviderImpl(), AirQualityProviderInterface {
    companion object {
        private const val BASE_URL = "https://api.openweathermap.org/data/2.5/"
        private const val KEYCHECK_QUERY_URL = BASE_URL + "forecast?appid=%s"
        private const val WEATHER_QUERY_URL = BASE_URL + "onecall?%s&appid=%s&lang=%s"
        private const val AQI_QUERY_URL = BASE_URL + "air_pollution?lat=%s&lon=%s&appid=%s"
    }

    init {
        mLocationProvider = RemoteConfig.getLocationProvider(getWeatherAPI())
                ?: LocationIQProvider()
    }

    override fun getWeatherAPI(): String {
        return WeatherAPI.OPENWEATHERMAP
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

        val client = SimpleLibrary.instance.httpClient
        var response: Response? = null

        try {
            val request = Request.Builder()
                    .cacheControl(CacheControl.Builder()
                            .maxAge(1, TimeUnit.DAYS)
                            .build())
                    .url(String.format(KEYCHECK_QUERY_URL, key))
                    .build()

            // Connect to webstream
            response = client.newCall(request).await()

            when (response.code) {
                HttpURLConnection.HTTP_BAD_REQUEST -> isValid = true
                HttpURLConnection.HTTP_UNAUTHORIZED -> {
                    wEx = WeatherException(ErrorStatus.INVALIDAPIKEY)
                    isValid = false
                }
            }
        } catch (ex: Exception) {
            if (ex is IOException) {
                wEx = WeatherException(ErrorStatus.NETWORKERROR).copyStackTrace(ex)
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

                val query = try {
                    String.format(Locale.ROOT, "id=%d", location_query.toInt())
                } catch (ex: NumberFormatException) {
                    location_query
                }

                val settingsMgr = SimpleLibrary.instance.app.settingsManager
                val key = if (settingsMgr.usePersonalKey()) settingsMgr.getAPIKEY() else getAPIKey()

                val client = SimpleLibrary.instance.httpClient
                var response: Response? = null
                var wEx: WeatherException? = null

                try {
                    val request = Request.Builder()
                            .cacheControl(CacheControl.Builder()
                                    .maxAge(1, TimeUnit.HOURS)
                                    .build())
                            .url(String.format(WEATHER_QUERY_URL, query, key, locale))
                            .build()

                    // Connect to webstream
                    response = client.newCall(request).await()
                    val stream = response.getStream()

                    // Load weather
                    val root = JSONParser.deserializer<OneCallRootobject>(stream, OneCallRootobject::class.java)

                    // End Stream
                    stream.closeQuietly()

                    weather = createOneCallWeatherData(root)
                } catch (ex: Exception) {
                    weather = null
                    if (ex is IOException) {
                        wEx = WeatherException(ErrorStatus.NETWORKERROR)
                    }
                    Logger.writeLine(Log.ERROR, ex, "OpenWeatherMapProvider: error getting weather data")
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
    override suspend fun updateWeatherData(location: LocationData, weather: Weather) {
        // OWM reports datetime in UTC; add location tz_offset
        val offset = location.tzOffset
        weather.updateTime = weather.updateTime.withZoneSameInstant(offset)
        weather.condition.observationTime = weather.condition.observationTime.withZoneSameInstant(offset)
        for (hr_forecast in weather.hrForecast) {
            hr_forecast.date = hr_forecast.date.withZoneSameInstant(offset)
        }
        for (forecast in weather.forecast) {
            forecast.date = forecast.date.plusSeconds(offset.totalSeconds.toLong())
        }
        if (!weather.minForecast.isNullOrEmpty()) {
            for (min_forecast in weather.minForecast) {
                min_forecast.date = min_forecast.date.withZoneSameInstant(offset)
            }
        }

        weather.astronomy.sunrise = weather.astronomy.sunrise.plusSeconds(offset.totalSeconds.toLong())
        weather.astronomy.sunset = weather.astronomy.sunset.plusSeconds(offset.totalSeconds.toLong())

        if (weather.astronomy.moonrise.isEqual(DateTimeUtils.getLocalDateTimeMIN()) || weather.astronomy.moonset.isEqual(DateTimeUtils.getLocalDateTimeMIN())) {
            val old = weather.astronomy
            val newAstro = SunMoonCalcProvider().getAstronomyData(location, weather.condition.observationTime)
            newAstro.sunrise = old.sunrise
            newAstro.sunset = old.sunset
            weather.astronomy = newAstro
        } else {
            weather.astronomy.moonrise = weather.astronomy.moonrise.plusSeconds(offset.totalSeconds.toLong())
            weather.astronomy.moonset = weather.astronomy.moonset.plusSeconds(offset.totalSeconds.toLong())
        }

        if (weather.weatherAlerts?.isNotEmpty() == true) {
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

    override suspend fun getAirQualityData(location: LocationData): AirQuality? =
            withContext(Dispatchers.IO) {
                var aqiData: AirQuality? = null

                val settingsMgr = SimpleLibrary.instance.app.settingsManager
                val key = if (settingsMgr.usePersonalKey()) settingsMgr.getAPIKEY() else getAPIKey()

                val client = SimpleLibrary.instance.httpClient
                var response: Response? = null

                try {
                    val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
                    df.applyPattern("0.####")

                    val request = Request.Builder()
                            .cacheControl(CacheControl.Builder()
                                    .maxAge(12, TimeUnit.HOURS)
                                    .build())
                            .url(String.format(AQI_QUERY_URL, df.format(location.latitude), df.format(location.latitude), key))
                            .build()

                    // Connect to webstream
                    response = client.newCall(request).await()
                    val stream = response.getStream()

                    // Load weather
                    val root = JSONParser.deserializer<AirPollutionResponse>(stream, AirPollutionResponse::class.java)

                    // End Stream
                    stream.closeQuietly()

                    aqiData = createAirQuality(root)
                } catch (ex: Exception) {
                    aqiData = null
                    Logger.writeLine(Log.ERROR, ex, "OpenWeatherMapProvider: error getting aqi data")
                } finally {
                    response?.closeQuietly()
                }

                return@withContext aqiData
            }

    override fun updateLocationQuery(weather: Weather): String {
        val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
        df.applyPattern("0.####")
        return String.format(Locale.ROOT, "lat=%s&lon=%s", df.format(weather.location.latitude), df.format(weather.location.longitude))
    }

    override fun updateLocationQuery(location: LocationData): String {
        val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
        df.applyPattern("0.####")
        return String.format(Locale.ROOT, "lat=%s&lon=%s", df.format(location.latitude), df.format(location.longitude))
    }

    override fun localeToLangCode(iso: String, name: String): String {
        var code = "en"

        code = when (iso) {
            // Arabic
            "ar",
                // Bulgarian
            "bg",
                // Catalan
            "ca",
                // Croatian
            "hr",
                // Dutch
            "nl",
                // Farsi / Persian
            "fa",
                // Finnish
            "fi",
                // French
            "fr",
                // Galician
            "gl",
                // German
            "de",
                // Greek
            "el",
                // Hungarian
            "hu",
                // Italian
            "it",
                // Japanese
            "ja",
                // Lithuanian
            "lt",
                // Macedonian
            "mk",
                // Polish
            "pl",
                // Portuguese
            "pt",
                // Romanian
            "ro",
                // Russian
            "ru",
                // Slovak
            "sk",
                // Slovenian
            "sl",
                // Spanish
            "es",
                // Turkish
            "tr",
                // Vietnamese
            "vi" -> iso
            // Chinese
            "zh" -> when (name) {
                "zh-Hant",
                "zh-HK",
                "zh-MO",
                "zh-TW" -> {
                    "zh_tw"
                }
                else -> {
                    "zh_cn"
                }
            }
            // Czech
            "cs" -> "cz"
            // Korean
            "ko" -> "kr"
            // Latvian
            "lv" -> "la"
            // Swedish
            "sv" -> "se"
            // Ukrainian
            "uk" -> "ua"
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

        when (icon.substring(0, 3)) {
            "200", // thunderstorm w/ light rain
            "201", // thunderstorm w/ rain
            "210", // light thunderstorm
            "230", // thunderstorm w/ light drizzle
            "231", // thunderstorm w/ drizzle
            "531" /* ragged shower rain */ -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_STORM_SHOWERS else WeatherIcons.DAY_STORM_SHOWERS
            }

            "211", // thunderstorm
            "212", // heavy thunderstorm
            "221", // ragged thunderstorm
            "202", // thunderstorm w/ heavy rain
            "232" /* thunderstorm w/ heavy drizzle */ -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_THUNDERSTORM else WeatherIcons.DAY_THUNDERSTORM
            }

            "300", // light intensity drizzle
            "301", // drizzle
            "321", // shower drizzle
            "500" /* light rain */ -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_SPRINKLE else WeatherIcons.DAY_SPRINKLE
            }

            "302", // heavy intensity drizzle
            "311", // drizzle rain
            "312", // heavy intensity drizzle rain
            "314", // heavy shower rain and drizzle
            "501", // moderate rain
            "502", // heavy intensity rain
            "503", // very heavy rain
            "504" /* extreme rain */ -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_RAIN else WeatherIcons.DAY_RAIN
            }

            "310", // light intensity drizzle rain
            "511", // freezing rain
            "611", // sleet
            "612", // shower sleet
            "615", // light rain and snow
            "616", // rain and snow
            "620" /* light shower snow */ -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_RAIN_MIX else WeatherIcons.DAY_RAIN_MIX
            }

            "313", // shower rain and drizzle
            "520", // light intensity shower rain
            "521", // shower rain
            "522", // heavy intensity shower rain
            "701" /* mist */ -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_SHOWERS else WeatherIcons.DAY_SHOWERS
            }

            "600", // light snow
            "601", // snow
            "621", // shower snow
            "622" /* heavy shower snow */ -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_SNOW else WeatherIcons.DAY_SNOW
            }

            // heavy snow
            "602" -> weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_SNOW_WIND else WeatherIcons.DAY_SNOW_WIND

            // smoke
            "711" -> weatherIcon = WeatherIcons.SMOKE

            // haze
            "721" -> weatherIcon = if (isNight) WeatherIcons.WINDY else WeatherIcons.DAY_HAZE

            // dust
            "731",
            "761",
            "762" -> {
                weatherIcon = WeatherIcons.DUST
            }

            // fog
            "741" -> weatherIcon = if (isNight) WeatherIcons.NIGHT_FOG else WeatherIcons.DAY_FOG

            // cloudy-gusts
            // squalls
            "771" -> weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_CLOUDY_GUSTS else WeatherIcons.DAY_CLOUDY_GUSTS

            // tornado
            "781",
            "900" -> {
                weatherIcon = WeatherIcons.TORNADO
            }

            // day-sunny
            // clear sky
            "800" -> weatherIcon = if (isNight) WeatherIcons.NIGHT_CLEAR else WeatherIcons.DAY_SUNNY

            "801", // few clouds
            "802" /* scattered clouds */ -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY else WeatherIcons.DAY_SUNNY_OVERCAST
            }

            "803", // broken clouds
            "804" /* overcast clouds */ -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_CLOUDY else WeatherIcons.DAY_CLOUDY
            }

            "901", // tropical storm
            "902" /* hurricane */ -> {
                weatherIcon = WeatherIcons.HURRICANE
            }

            // cold
            "903" -> weatherIcon = WeatherIcons.SNOWFLAKE_COLD

            // hot
            "904" -> weatherIcon = if (isNight) WeatherIcons.NIGHT_CLEAR else WeatherIcons.DAY_HOT

            // windy
            "905" -> weatherIcon = WeatherIcons.WINDY

            // hail
            "906" -> weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_HAIL else WeatherIcons.DAY_HAIL

            // strong wind
            "957" -> weatherIcon = WeatherIcons.STRONG_WIND
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
            WeatherIcons.WINDY,
            WeatherIcons.DUST,
            WeatherIcons.TORNADO,
            WeatherIcons.HURRICANE,
            WeatherIcons.SNOWFLAKE_COLD,
            WeatherIcons.HAIL,
            WeatherIcons.STRONG_WIND -> {
                if (!isNight) {
                    // Fallback to sunset/rise time just in case
                    var tz: ZoneOffset? = null
                    if (!weather.location.tzLong.isNullOrBlank()) {
                        val id = ZoneId.of(weather.location.tzLong)
                        tz = id.rules.getOffset(Instant.now())
                    }
                    if (tz == null) {
                        tz = weather.location.tzOffset
                    }

                    val sunrise = weather.astronomy?.sunrise?.toLocalTime() ?: LocalTime.of(6, 0)
                    val sunset = weather.astronomy?.sunset?.toLocalTime() ?: LocalTime.of(18, 0)

                    val now = ZonedDateTime.now(tz).toLocalTime()

                    // Determine whether its night using sunset/rise times
                    if (now.toNanoOfDay() < sunrise.toNanoOfDay() || now.toNanoOfDay() > sunset.toNanoOfDay()) isNight = true
                }
            }
        }

        return isNight
    }
}