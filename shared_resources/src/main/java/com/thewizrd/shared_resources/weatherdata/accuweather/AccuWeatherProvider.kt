package com.thewizrd.shared_resources.weatherdata.accuweather

import android.net.Uri
import android.util.Log
import com.google.gson.reflect.TypeToken
import com.ibm.icu.util.ULocale
import com.thewizrd.shared_resources.SimpleLibrary
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.accuweather.AccuWeatherLocationProvider
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.await
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.getStream
import com.thewizrd.shared_resources.preferences.DevSettingsEnabler
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.utils.ExceptionUtils.copyStackTrace
import com.thewizrd.shared_resources.weatherdata.*
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.shared_resources.weatherdata.model.isNullOrInvalid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly
import java.io.IOException
import java.net.HttpURLConnection
import java.util.*
import java.util.concurrent.TimeUnit

class AccuWeatherProvider : WeatherProviderImpl() {
    companion object {
        private const val DAILY_5DAY_FORECAST_URL = "https://dataservice.accuweather.com/forecasts/v1/daily/5day"
        private const val HOURLY_12HR_FORECAST_URL = "https://dataservice.accuweather.com/forecasts/v1/hourly/12hour"
        private const val CURRENT_CONDITIONS_URL = "https://dataservice.accuweather.com/currentconditions/v1"
    }

    init {
        mLocationProvider = AccuWeatherLocationProvider()
    }

    override fun getWeatherAPI(): String {
        return WeatherAPI.ACCUWEATHER
    }

    override fun supportsWeatherLocale(): Boolean {
        return true
    }

    override fun isKeyRequired(): Boolean {
        return true
    }

    override fun needsExternalAlertData(): Boolean {
        return true
    }

    override suspend fun isKeyValid(key: String?): Boolean = withContext(Dispatchers.IO) {
        if (key.isNullOrBlank()) {
            throw WeatherException(ErrorStatus.INVALIDAPIKEY)
        }

        var isValid = false
        var wEx: WeatherException? = null

        val client = SimpleLibrary.instance.httpClient
        var response: Response? = null

        try {
            val requestUri = Uri.parse(CURRENT_CONDITIONS_URL).buildUpon()
                    .appendQueryParameter("apikey", key)
                    .build()

            val request = Request.Builder()
                    .cacheControl(CacheControl.Builder()
                            .maxAge(1, TimeUnit.DAYS)
                            .build())
                    .url(requestUri.toString())
                    .build()

            // Connect to webstream
            response = client.newCall(request).await()

            when (response.code) {
                HttpURLConnection.HTTP_BAD_REQUEST -> isValid = true
                HttpURLConnection.HTTP_UNAUTHORIZED -> {
                    wEx = WeatherException(ErrorStatus.INVALIDAPIKEY)
                    isValid = false
                }
                HttpURLConnection.HTTP_NOT_FOUND -> isValid = response.body?.contentLength() ?: 0 > 0
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

                val client = SimpleLibrary.instance.httpClient
                var wEx: WeatherException? = null

                try {
                    val settingsMgr = SimpleLibrary.instance.app.settingsManager
                    val key = (if (settingsMgr.usePersonalKey()) settingsMgr.getAPIKEY() else getAPIKey())
                            ?: DevSettingsEnabler.getAPIKey(SimpleLibrary.instance.appContext, WeatherAPI.ACCUWEATHER)

                    if (key.isNullOrBlank()) {
                        throw WeatherException(ErrorStatus.INVALIDAPIKEY)
                    }

                    val request5dayUri = Uri.parse(DAILY_5DAY_FORECAST_URL).buildUpon()
                            .appendPath(location_query)
                            .appendQueryParameter("apikey", key)
                            .appendQueryParameter("language", locale)
                            .appendQueryParameter("details", "true")
                            .appendQueryParameter("metric", "true")

                    val request = Request.Builder()
                            .cacheControl(CacheControl.Builder()
                                    .maxAge(3, TimeUnit.HOURS)
                                    .build())
                            .url(request5dayUri.toString())
                            .build()

                    val requestHourlyUri = Uri.parse(HOURLY_12HR_FORECAST_URL).buildUpon()
                            .appendPath(location_query)
                            .appendQueryParameter("apikey", key)
                            .appendQueryParameter("language", locale)
                            .appendQueryParameter("details", "true")
                            .appendQueryParameter("metric", "true")

                    val hourlyRequest = Request.Builder()
                            .cacheControl(CacheControl.Builder()
                                    .maxAge(3, TimeUnit.HOURS)
                                    .build())
                            .url(requestHourlyUri.toString())
                            .build()

                    val requestCurrentUri = Uri.parse(CURRENT_CONDITIONS_URL).buildUpon()
                            .appendPath(location_query)
                            .appendQueryParameter("apikey", key)
                            .appendQueryParameter("language", locale)
                            .appendQueryParameter("details", "true")

                    val currentRequest = Request.Builder()
                            .cacheControl(CacheControl.Builder()
                                    .maxAge(1, TimeUnit.HOURS)
                                    .build())
                            .url(requestCurrentUri.toString())
                            .build()

                    // Connect to webstream
                    val dailyResponse = client.newCall(request).await()
                    val hourlyResponse = client.newCall(hourlyRequest).await()
                    val currentResponse = client.newCall(currentRequest).await()

                    val dailyRoot = dailyResponse.use { r ->
                        r.getStream().use { s ->
                            JSONParser.deserializer<DailyResponse>(s, DailyResponse::class.java)
                        }
                    }
                    val hourlyRoot = hourlyResponse.use { r ->
                        r.getStream().use { s ->
                            JSONParser.deserializer<List<HourlyResponseItem>>(s, object : TypeToken<List<HourlyResponseItem>>() {}.type)
                        }
                    }.let {
                        HourlyResponse(it)
                    }
                    val currentRoot = currentResponse.use { r ->
                        r.getStream().use { s ->
                            JSONParser.deserializer<List<CurrentsResponseItem>>(s, object : TypeToken<List<CurrentsResponseItem>>() {}.type)
                        }
                    }.let {
                        CurrentsResponse(it)
                    }

                    weather = createWeatherData(dailyRoot, hourlyRoot, currentRoot)
                } catch (ex: Exception) {
                    weather = null
                    if (ex is IOException) {
                        wEx = WeatherException(ErrorStatus.NETWORKERROR)
                    }
                    Logger.writeLine(Log.ERROR, ex, "AccuWeatherProvider: error getting weather data")
                }

                if (wEx == null && weather.isNullOrInvalid()) {
                    wEx = WeatherException(ErrorStatus.NOWEATHER)
                } else if (weather != null) {
                    if (supportsWeatherLocale())
                        weather.locale = locale

                    weather.query = location_query
                }

                if (wEx != null) throw wEx

                return@withContext weather!!
            }

    @Throws(WeatherException::class)
    override suspend fun updateWeatherData(location: LocationData, weather: Weather) {
        // no-op
    }

    override fun updateLocationQuery(weather: Weather): String {
        // TODO: suspend?
        val locationModel = runBlocking {
            mLocationProvider.getLocation(
                Coordinate(
                    weather.location.latitude.toDouble(),
                    weather.location.longitude.toDouble()
                ), getWeatherAPI()
            )
        }
        return locationModel!!.locationQuery
    }

    override fun updateLocationQuery(location: LocationData): String {
        // TODO: suspend?
        val locationModel = runBlocking {
            mLocationProvider.getLocation(
                Coordinate(location.latitude, location.longitude),
                getWeatherAPI()
            )
        }
        return locationModel!!.locationQuery
    }

    override fun localeToLangCode(iso: String, name: String): String {
        return name
    }

    override fun getWeatherIcon(icon: String?): String {
        return getWeatherIcon(false, icon)
    }

    override fun getWeatherIcon(isNight: Boolean, icon: String?): String {
        var weatherIcon = ""

        if (icon == null) return WeatherIcons.NA

        when (icon.toIntOrNull()) {
            /*
             *  1: Sunny
             * 33: Clear
             */
            1, 33 -> weatherIcon = if (isNight)
                WeatherIcons.NIGHT_CLEAR
            else
                WeatherIcons.DAY_SUNNY
            /*
             *  2: Mostly Sunny
             *  3: Partly Sunny
             * 34: Mostly Clear
             * 35: Partly Cloudy
             */
            2, 3, 34, 35 -> weatherIcon = if (isNight)
                WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY
            else
                WeatherIcons.DAY_SUNNY_OVERCAST
            /*
             *  4: Intermittent Clouds
             *  6: Mostly Cloudy
             *  7: Cloudy
             *  8: Dreary (Overcast)
             * 36: Intermittent Clouds
             * 38: Mostly Cloudy
             */
            4, 6, 7, 8, 36, 38 -> weatherIcon = if (isNight)
                WeatherIcons.NIGHT_ALT_CLOUDY
            else
                WeatherIcons.DAY_CLOUDY
            /*
             *  5: Hazy Sunshine
             * 37: Hazy Moonlight
             */
            5, 37 -> weatherIcon = if (isNight)
                WeatherIcons.NIGHT_FOG
            else
                WeatherIcons.DAY_HAZE
            /* 11: Fog */
            11 -> weatherIcon = if (isNight)
                WeatherIcons.NIGHT_FOG
            else
                WeatherIcons.DAY_FOG
            /*
             * 12: Showers
             * 13: Mostly Cloudy w/ Showers
             * 14: Partly Sunny w/ Showers
             * 39: Partly Cloudy w/ Showers
             * 40: Mostly Cloudy w/ Showers
             */
            12, 13, 14, 39, 40 -> weatherIcon = if (isNight)
                WeatherIcons.NIGHT_ALT_SHOWERS
            else
                WeatherIcons.DAY_SHOWERS
            /*
             * 15: T-Storms
             * 16: Mostly Cloudy w/ T-Storms
             * 17: Partly Sunny w/ T-Storms
             * 41: Partly Cloudy w/ T-Storms
             * 42: Mostly Cloudy w/ T-Storms
             */
            15, 16, 17, 41, 42 -> weatherIcon = if (isNight)
                WeatherIcons.NIGHT_ALT_THUNDERSTORM
            else
                WeatherIcons.DAY_THUNDERSTORM
            /* 18: Rain */
            18 -> weatherIcon = if (isNight)
                WeatherIcons.NIGHT_ALT_RAIN
            else
                WeatherIcons.DAY_RAIN
            /*
             * 19: Flurries
             * 20: Mostly Cloudy w/ Flurries
             * 21: Partly Sunny w/ Flurries
             * 22: Snow
             * 23: Mostly Cloudy w/ Snow
             * 43: Mostly Cloudy w/ Flurries
             * 44: Mostly Cloudy w/ Snow
             */
            19, 20, 21, 22, 23, 43, 44 -> weatherIcon = if (isNight)
                WeatherIcons.NIGHT_ALT_SNOW
            else
                WeatherIcons.DAY_SNOW
            /*
             * 24: Ice
             * 31: Cold
             */
            24, 31 -> weatherIcon = WeatherIcons.SNOWFLAKE_COLD
            /* 25: Sleet */
            25 -> weatherIcon = if (isNight)
                WeatherIcons.NIGHT_ALT_SLEET
            else
                WeatherIcons.DAY_SLEET
            /*
             * 26: Freezing Rain
             * 29: Rain and Snow
             */
            26, 29 -> weatherIcon = if (isNight)
                WeatherIcons.NIGHT_ALT_RAIN_MIX
            else
                WeatherIcons.DAY_RAIN_MIX
            /* 30: Hot */
            30 -> weatherIcon = if (isNight) WeatherIcons.NIGHT_CLEAR else WeatherIcons.DAY_HOT
            /* 32: Windy */
            32 -> weatherIcon = if (isNight) WeatherIcons.WINDY else WeatherIcons.DAY_WINDY
        }

        if (weatherIcon.isBlank()) {
            // Not Available
            weatherIcon = WeatherIcons.NA
        }

        return weatherIcon
    }
}