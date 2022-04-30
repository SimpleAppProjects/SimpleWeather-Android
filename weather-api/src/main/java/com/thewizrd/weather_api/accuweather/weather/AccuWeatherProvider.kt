package com.thewizrd.weather_api.accuweather.weather

import android.net.Uri
import android.util.Log
import com.google.gson.reflect.TypeToken
import com.ibm.icu.util.ULocale
import com.thewizrd.shared_resources.exceptions.ErrorStatus
import com.thewizrd.shared_resources.exceptions.WeatherException
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.await
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.getStream
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.Coordinate
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.shared_resources.weatherdata.model.isNullOrInvalid
import com.thewizrd.weather_api.accuweather.location.AccuWeatherLocationProvider
import com.thewizrd.weather_api.extras.cacheRequestIfNeeded
import com.thewizrd.weather_api.utils.APIRequestUtils.checkForErrors
import com.thewizrd.weather_api.utils.APIRequestUtils.checkRateLimit
import com.thewizrd.weather_api.utils.APIRequestUtils.throwIfRateLimited
import com.thewizrd.weather_api.weatherdata.WeatherProviderImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly
import java.io.IOException
import java.net.HttpURLConnection
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

    override fun getRetryTime(): Long {
        return 43200000L // 12 hrs
    }

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

            val requestUri = Uri.parse(CURRENT_CONDITIONS_URL).buildUpon()
                .appendQueryParameter("apikey", key)
                .build()

            val request = Request.Builder()
                .cacheControl(
                    CacheControl.Builder()
                        .maxAge(1, TimeUnit.DAYS)
                        .build()
                )
                .url(requestUri.toString())
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
                HttpURLConnection.HTTP_NOT_FOUND -> isValid = response.body?.contentLength() ?: 0 > 0
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

                val client = sharedDeps.httpClient
                var wEx: WeatherException? = null

                try {
                    // If were under rate limit, deny request
                    checkRateLimit()

                    val key =
                        if (settingsManager.usePersonalKey()) settingsManager.getAPIKey(
                            getWeatherAPI()
                        ) else getAPIKey()

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
                        .cacheRequestIfNeeded(isKeyRequired(), 3, TimeUnit.HOURS)
                            .url(request5dayUri.toString())
                            .build()

                    val requestHourlyUri = Uri.parse(HOURLY_12HR_FORECAST_URL).buildUpon()
                            .appendPath(location_query)
                            .appendQueryParameter("apikey", key)
                            .appendQueryParameter("language", locale)
                            .appendQueryParameter("details", "true")
                            .appendQueryParameter("metric", "true")

                    val hourlyRequest = Request.Builder()
                        .cacheRequestIfNeeded(isKeyRequired(), 3, TimeUnit.HOURS)
                            .url(requestHourlyUri.toString())
                            .build()

                    val requestCurrentUri = Uri.parse(CURRENT_CONDITIONS_URL).buildUpon()
                            .appendPath(location_query)
                            .appendQueryParameter("apikey", key)
                            .appendQueryParameter("language", locale)
                            .appendQueryParameter("details", "true")

                    val currentRequest = Request.Builder()
                        .cacheRequestIfNeeded(isKeyRequired(), 30, TimeUnit.MINUTES)
                        .url(requestCurrentUri.toString())
                        .build()

                    // Connect to webstream
                    val dailyResponse = client.newCall(request).await()
                    checkForErrors(dailyResponse)

                    val hourlyResponse = client.newCall(hourlyRequest).await()
                    checkForErrors(hourlyResponse)

                    val currentResponse = client.newCall(currentRequest).await()
                    checkForErrors(currentResponse)

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
                    } else if (ex is WeatherException) {
                        wEx = ex
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
        return locationModel!!.locationQuery!!
    }

    override fun updateLocationQuery(location: LocationData): String {
        // TODO: suspend?
        val locationModel = runBlocking {
            mLocationProvider.getLocation(
                Coordinate(location.latitude, location.longitude),
                getWeatherAPI()
            )
        }
        return locationModel!!.locationQuery!!
    }

    override fun localeToLangCode(iso: String, name: String): String {
        return name
    }

    override fun getWeatherIcon(icon: String?): String {
        return getWeatherIcon(false, icon)
    }

    override fun getWeatherIcon(isNight: Boolean, icon: String?): String {
        var weatherIcon = ""

        val conditionCode = icon?.toIntOrNull() ?: return WeatherIcons.NA

        when (conditionCode) {
            /* Sunny */
            1 -> WeatherIcons.DAY_SUNNY
            /*
             *  2: Mostly Sunny
             *  3: Partly Sunny
             *  4: Intermittent Clouds
             */
            2, 3, 4 -> weatherIcon = WeatherIcons.DAY_PARTLY_CLOUDY
            /* 5: Hazy Sunshine */
            5 -> weatherIcon = WeatherIcons.DAY_HAZE
            /* 6: Mostly Cloudy */
            6 -> weatherIcon = WeatherIcons.DAY_CLOUDY
            /* 7: Cloudy */
            7 -> weatherIcon = WeatherIcons.CLOUDY
            /* 8: Dreary (Overcast) */
            8 -> weatherIcon = if (isNight)
                WeatherIcons.NIGHT_OVERCAST
            else
                WeatherIcons.DAY_SUNNY_OVERCAST
            /* 11: Fog */
            11 -> weatherIcon = WeatherIcons.FOG
            /* 12: Showers */
            12 -> weatherIcon = WeatherIcons.SHOWERS
            /*
             * 13: Mostly Cloudy w/ Showers
             * 14: Partly Sunny w/ Showers
             */
            13, 14 -> weatherIcon = WeatherIcons.DAY_SHOWERS
            /* 15: T-Storms */
            15 -> weatherIcon = WeatherIcons.THUNDERSTORM
            /*
             * 16: Mostly Cloudy w/ T-Storms
             * 17: Partly Sunny w/ T-Storms
             */
            16, 17 -> weatherIcon = WeatherIcons.DAY_THUNDERSTORM
            /* 18: Rain */
            18 -> weatherIcon = WeatherIcons.RAIN
            /* 19: Flurries */
            19 -> weatherIcon = WeatherIcons.SNOW
            /*
             * 20: Mostly Cloudy w/ Flurries
             * 21: Partly Sunny w/ Flurries
             * 23: Mostly Cloudy w/ Snow
             */
            20, 21, 23 -> weatherIcon = WeatherIcons.DAY_SNOW
            /* 22: Snow */
            22 -> weatherIcon = WeatherIcons.SNOW
            /*
             * 24: Ice
             * 31: Cold
             */
            24, 31 -> weatherIcon = WeatherIcons.SNOWFLAKE_COLD
            /* 25: Sleet */
            25 -> weatherIcon = WeatherIcons.SLEET
            /*
             * 26: Freezing Rain
             * 29: Rain and Snow
             */
            26, 29 -> weatherIcon = WeatherIcons.RAIN_MIX
            /* 30: Hot */
            30 -> weatherIcon = if (isNight) WeatherIcons.NIGHT_CLEAR else WeatherIcons.DAY_HOT
            /* 32: Windy */
            32 -> weatherIcon = WeatherIcons.WINDY
            /* 33: Clear */
            33 -> weatherIcon = WeatherIcons.NIGHT_CLEAR
            /*
             * 34: Mostly Clear
             * 35: Partly Cloudy
             * 36: Intermittent Clouds
             */
            34, 35, 36 -> weatherIcon = WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY
            /* 37: Hazy Moonlight */
            37 -> weatherIcon = WeatherIcons.NIGHT_FOG
            /* 38: Mostly Cloudy */
            38 -> weatherIcon = WeatherIcons.NIGHT_ALT_CLOUDY
            /*
             * 39: Partly Cloudy w/ Showers
             * 40: Mostly Cloudy w/ Showers
             */
            39, 40 -> weatherIcon = WeatherIcons.NIGHT_ALT_SHOWERS
            /*
             * 41: Partly Cloudy w/ T-Storms
             * 42: Mostly Cloudy w/ T-Storms
             */
            41, 42 -> weatherIcon = WeatherIcons.NIGHT_ALT_THUNDERSTORM
            /*
             * 43: Mostly Cloudy w/ Flurries
             * 44: Mostly Cloudy w/ Snow
             */
            43, 44 -> weatherIcon = WeatherIcons.NIGHT_ALT_SNOW
        }

        if (weatherIcon.isBlank()) {
            // Not Available
            weatherIcon = WeatherIcons.NA
        }

        return weatherIcon
    }
}