package com.thewizrd.shared_resources.weatherdata.weatherunlocked

import android.util.Log
import com.ibm.icu.util.ULocale
import com.thewizrd.shared_resources.SimpleLibrary
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.keys.Keys
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.weatherapi.WeatherApiLocationProvider
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.await
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.getStream
import com.thewizrd.shared_resources.remoteconfig.RemoteConfig.getLocationProvider
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.weatherdata.Weather
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.WeatherProviderImpl
import com.thewizrd.shared_resources.weatherdata.smc.SunMoonCalcProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly
import java.io.IOException
import java.text.DecimalFormat
import java.time.*
import java.util.*
import java.util.concurrent.TimeUnit

class WeatherUnlockedProvider : WeatherProviderImpl() {
    companion object {
        private const val BASE_URL = "http://api.weatherunlocked.com/api/"
        private const val CURRENT_QUERY_URL = BASE_URL + "current/%s?app_id=%s&app_key=%s&lang=%s"
        private const val FORECAST_QUERY_URL = BASE_URL + "forecast/%s?app_id=%s&app_key=%s&lang=%s"
    }

    init {
        mLocationProvider = getLocationProvider(getWeatherAPI()) ?: WeatherApiLocationProvider()
    }

    override fun getWeatherAPI(): String {
        return WeatherAPI.WEATHERUNLOCKED
    }

    override fun supportsWeatherLocale(): Boolean {
        return true
    }

    override fun isKeyRequired(): Boolean {
        return false
    }

    override fun getHourlyForecastInterval(): Int {
        return 3
    }

    override suspend fun isKeyValid(key: String?): Boolean {
        return false
    }

    override fun getAPIKey(): String? {
        return null
    }

    private val appID: String
        get() = Keys.getWUnlockedAppID()
    private val appKey: String
        get() = Keys.getWUnlockedKey()

    @Throws(WeatherException::class)
    override suspend fun getWeather(location_query: String, country_code: String): Weather =
            withContext(Dispatchers.IO) {
                var weather: Weather?

                val uLocale = ULocale.forLocale(LocaleUtils.getLocale())
                val locale = localeToLangCode(uLocale.language, uLocale.toLanguageTag())

                val client = SimpleLibrary.getInstance().httpClient
                var currentResponse: Response? = null
                var forecastResponse: Response? = null
                var wEx: WeatherException? = null

                try {
                    val currentRequest = Request.Builder()
                            .cacheControl(CacheControl.Builder()
                                    .maxAge(1, TimeUnit.HOURS)
                                    .build())
                            .addHeader("Accept", "application/json")
                            .url(String.format(CURRENT_QUERY_URL, location_query, appID, appKey, locale))
                            .build()
                    val forecastRequest = Request.Builder()
                            .cacheControl(CacheControl.Builder()
                                    .maxAge(3, TimeUnit.HOURS)
                                    .build())
                            .addHeader("Accept", "application/json")
                            .url(String.format(FORECAST_QUERY_URL, location_query, appID, appKey, locale))
                            .build()

                    // Connect to webstream
                    currentResponse = client.newCall(currentRequest).await()
                    forecastResponse = client.newCall(forecastRequest).await()
                    val currentStream = currentResponse.getStream()
                    val forecastStream = forecastResponse.getStream()

                    // Load weather
                    val currRoot = JSONParser.deserializer<CurrentResponse>(currentStream, CurrentResponse::class.java)
                    val foreRoot = JSONParser.deserializer<ForecastResponse>(forecastStream, ForecastResponse::class.java)

                    // End Stream
                    currentStream.closeQuietly()
                    forecastStream.closeQuietly()

                    weather = createWeatherData(currRoot, foreRoot)
                } catch (ex: Exception) {
                    weather = null
                    if (ex is IOException) {
                        wEx = WeatherException(ErrorStatus.NETWORKERROR)
                    }
                    Logger.writeLine(Log.ERROR, ex, "WeatherUnlockedProvider: error getting weather data")
                } finally {
                    currentResponse?.closeQuietly()
                    forecastResponse?.closeQuietly()
                }
                if (wEx == null && weather?.isValid == false) {
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
        val offset = location.tzOffset
        weather.updateTime = weather.updateTime.withZoneSameInstant(offset)
        weather.condition.observationTime = weather.condition.observationTime.withZoneSameInstant(offset)

        weather.astronomy = SunMoonCalcProvider().getAstronomyData(location, weather.condition.observationTime)

        // Update icons
        val now = ZonedDateTime.now(ZoneOffset.UTC).withZoneSameInstant(offset).toLocalTime()
        val sunrise = weather.astronomy.sunrise.toLocalTime()
        val sunset = weather.astronomy.sunset.toLocalTime()

        weather.condition.icon = getWeatherIcon(now.isBefore(sunrise) || now.isAfter(sunset), weather.condition.icon)

        for (hr_forecast in weather.hrForecast) {
            val hrf_date = hr_forecast.date.withZoneSameInstant(offset)
            hr_forecast.date = hrf_date
            val hrf_localTime = hrf_date.toLocalTime()
            hr_forecast.icon = getWeatherIcon(hrf_localTime.isBefore(sunrise) || hrf_localTime.isAfter(sunset), hr_forecast.icon)
        }
    }

    override fun updateLocationQuery(weather: Weather): String {
        val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
        df.applyPattern("0.##")
        return String.format(Locale.ROOT, "%s,%s", df.format(weather.location.latitude), df.format(weather.location.longitude))
    }

    override fun updateLocationQuery(location: LocationData): String {
        val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
        df.applyPattern("0.##")
        return String.format(Locale.ROOT, "%s,%s", df.format(location.latitude), df.format(location.longitude))
    }

    override fun localeToLangCode(iso: String, name: String): String {
        var code = "en"
        code = when (iso) {
            // Danish
            "da",
                // French
            "fr",
                // Italian
            "it",
                // German
            "de",
                // Dutch
            "nl",
                // Spanish
            "es",
                // Norwegian
            "no",
                // Swedish
            "sv",
                // Turkish
            "tr",
                // Bulgarian
            "bg",
                // Czech
            "cs",
                // Hungarian
            "hu",
                // Polish
            "pl",
                // Russian
            "ru",
                // Slovak
            "sk" -> iso
            // Romanian
            "ro" -> "rm"
            // Default is English
            else -> "en"
        }
        return code
    }

    override fun getWeatherIcon(icon: String?): String {
        return getWeatherIcon(false, icon)
    }

    override fun getWeatherIcon(isNight: Boolean, icon: String?): String {
        var weatherIcon = ""

        try {
            val code = icon!!.toInt()

            weatherIcon = when (code) {
                0 /* Sunny skies/Clear skies */ -> {
                    if (isNight) WeatherIcons.NIGHT_CLEAR else WeatherIcons.DAY_SUNNY
                }
                1, // Partly cloudy skies
                3  /* Cloudy skies */ -> {
                    if (isNight) WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY else WeatherIcons.DAY_SUNNY_OVERCAST
                }
                2 /* Cloudy skies */ -> {
                    if (isNight) WeatherIcons.NIGHT_ALT_CLOUDY else WeatherIcons.DAY_CLOUDY
                }
                10, // Haze
                45, // Fog
                49  /* Freezing fog */ -> {
                    if (isNight) WeatherIcons.NIGHT_FOG else WeatherIcons.DAY_FOG
                }
                21, // Patchy rain possible
                50, // Patchy light drizzle
                51, // Light drizzle
                60, // Patchy light rain
                61  /* Light rain */ -> {
                    if (isNight) WeatherIcons.NIGHT_ALT_SPRINKLE else WeatherIcons.DAY_SPRINKLE
                }
                22, // Patchy snow possible
                70, // Patchy snow possible
                71, // Light snow
                72, // Patchy moderate snow
                73, // Moderate snow
                85, // Light snow showers
                86  /* Moderate or heavy snow showers */ -> {
                    if (isNight) WeatherIcons.NIGHT_ALT_SNOW else WeatherIcons.DAY_SNOW
                }
                23, // Patchy sleet possible
                24, // Patchy freezing drizzle possible
                56, // Freezing drizzle
                57, // Heavy freezing drizzle
                68, // Light sleet
                69, // Moderate or heavy sleet
                79, // Ice pellets
                83, // Light sleet showers
                84, // Moderate or heavy sleet showers
                87, // Light showers of ice pellets
                88  /* Moderate or heavy showers of ice pellets */ -> {
                    if (isNight) WeatherIcons.NIGHT_ALT_SLEET else WeatherIcons.DAY_SLEET
                }
                29  /* Thundery outbreaks possible */ -> {
                    if (isNight) WeatherIcons.NIGHT_ALT_LIGHTNING else WeatherIcons.DAY_LIGHTNING
                }
                38, // Blowing snow
                39, // Blizzard
                74, // Patchy heavy snow
                75  /* Heavy snow */ -> {
                    if (isNight) WeatherIcons.NIGHT_ALT_SNOW_WIND else WeatherIcons.DAY_SNOW_WIND
                }
                62, // Moderate rain at times
                63, // Moderate rain
                64, // Heavy rain at times
                65  /* Heavy rain */ -> {
                    if (isNight) WeatherIcons.NIGHT_ALT_RAIN else WeatherIcons.DAY_RAIN
                }
                66, // Light freezing rain
                67  /* Moderate or heavy freezing rain */ -> {
                    if (isNight) WeatherIcons.NIGHT_ALT_RAIN_MIX else WeatherIcons.DAY_RAIN_MIX
                }
                80, // Light rain shower
                81, // Moderate or heavy rain shower
                82  /* Torrential rain shower */ -> {
                    if (isNight) WeatherIcons.NIGHT_ALT_SHOWERS else WeatherIcons.DAY_SHOWERS
                }
                91, // Patchy light rain with thunder
                92  /* Moderate or heavy rain with thunder */ -> {
                    if (isNight) WeatherIcons.NIGHT_ALT_THUNDERSTORM else WeatherIcons.DAY_THUNDERSTORM
                }
                93, // Patchy light snow with thunder
                94  /* Moderate or heavy snow with thunder */ -> {
                    if (isNight) WeatherIcons.NIGHT_ALT_SNOW_THUNDERSTORM else WeatherIcons.DAY_SNOW_THUNDERSTORM
                }
                else -> WeatherIcons.NA
            }
        } catch (ex: NumberFormatException) {
            // DO nothing
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

        return isNight
    }
}