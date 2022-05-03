package com.thewizrd.weather_api.tomorrow

import android.net.Uri
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
import com.thewizrd.shared_resources.weatherdata.PollenProvider
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.model.Pollen
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.shared_resources.weatherdata.model.isNullOrInvalid
import com.thewizrd.weather_api.extras.cacheRequestIfNeeded
import com.thewizrd.weather_api.keys.Keys
import com.thewizrd.weather_api.locationiq.LocationIQProvider
import com.thewizrd.weather_api.nws.SolCalcAstroProvider
import com.thewizrd.weather_api.smc.SunMoonCalcProvider
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

class TomorrowIOWeatherProvider : WeatherProviderImpl(), PollenProvider {
    companion object {
        private const val BASE_URL = "https://api.tomorrow.io/v4/timelines"
        private const val EVENTS_BASE_URL = "https://api.tomorrow.io/v4/events"
        private const val KEYCHECK_QUERY_URL = "$BASE_URL?apikey=%s"
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
        return WeatherAPI.TOMORROWIO
    }

    override fun supportsWeatherLocale(): Boolean {
        return false
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

    override fun getRetryTime(): Long {
        return 5000
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
        return Keys.getTomorrowIoKey()
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
                var minutelyResponse: Response? = null
                var alertsResponse: Response? = null
                var wEx: WeatherException? = null

                try {
                    // If were under rate limit, deny request
                    checkRateLimit()

                    val requestUri = Uri.parse(BASE_URL).buildUpon()
                        .appendQueryParameter("apikey", key)
                        .appendQueryParameter("location", location_query)
                        .appendQueryParameter(
                            "fields",
                            "temperature,temperatureApparent,temperatureMin,temperatureMax,dewPoint,humidity,windSpeed,windDirection,windGust,pressureSeaLevel,precipitationIntensity,precipitationProbability,snowAccumulation,sunriseTime,sunsetTime,visibility,cloudCover,moonPhase,weatherCode,treeIndex,grassIndex,weedIndex,epaIndex"
                        )
                        .appendQueryParameter("timesteps", "current,1h,1d")
                        .appendQueryParameter("units", "metric")
                        .appendQueryParameter("timezone", "UTC")
                        .build()

                    val request = Request.Builder()
                        .cacheRequestIfNeeded(isKeyRequired(), 20, TimeUnit.MINUTES)
                        .url(requestUri.toString())
                        .header("Accept", "application/json")
                        .build()

                    val minutelyRequestUri = Uri.parse(BASE_URL).buildUpon()
                            .appendQueryParameter("apikey", key)
                            .appendQueryParameter("location", location_query)
                            .appendQueryParameter("fields", "precipitationIntensity,precipitationProbability")
                            .appendQueryParameter("timesteps", "1m")
                            .appendQueryParameter("units", "metric")
                            .appendQueryParameter("timezone", "UTC")
                            .build()

                    val minutelyRequest = Request.Builder()
                        .cacheRequestIfNeeded(isKeyRequired(), 30, TimeUnit.MINUTES)
                        .url(minutelyRequestUri.toString())
                        .header("Accept", "application/json")
                        .build()

                    val alertsRequestUri = Uri.parse(EVENTS_BASE_URL).buildUpon()
                        .appendQueryParameter("apikey", key)
                        .appendQueryParameter("location", location_query)
                        .appendQueryParameter("insights", "air")
                        .appendQueryParameter("insights", "fires")
                        .appendQueryParameter("insights", "wind")
                        .appendQueryParameter("insights", "winter")
                        .appendQueryParameter("insights", "thunderstorms")
                        .appendQueryParameter("insights", "floods")
                        .appendQueryParameter("insights", "temperature")
                        .appendQueryParameter("insights", "tropical")
                        .appendQueryParameter("insights", "marine")
                        .appendQueryParameter("insights", "fog")
                        .appendQueryParameter("insights", "tornado")
                            .appendQueryParameter("buffer", "20")
                            .build()

                    val alertsRequest = Request.Builder()
                        .cacheRequestIfNeeded(isKeyRequired(), 30, TimeUnit.MINUTES)
                        .url(alertsRequestUri.toString())
                        .header("Accept", "application/json")
                        .build()

                    // Connect to webstream
                    response = client.newCall(request).await()
                    checkForErrors(response)
                    // Load weather
                    val root = response.getStream().use {
                        JSONParser.deserializer<Rootobject>(it, Rootobject::class.java)
                    }

                    var minutelyRoot: Rootobject? = null
                    var alertsRoot: AlertsRootobject? = null

                    runCatching {
                        minutelyResponse = client.newCall(minutelyRequest).await()
                        checkForErrors(minutelyResponse!!)
                        minutelyResponse!!.getStream().use {
                            minutelyRoot = JSONParser.deserializer<Rootobject>(it, Rootobject::class.java)
                        }
                    }

                    runCatching {
                        alertsResponse = client.newCall(alertsRequest).await()
                        checkForErrors(alertsResponse!!)
                        alertsResponse!!.getStream().use {
                            alertsRoot = JSONParser.deserializer<AlertsRootobject>(it, AlertsRootobject::class.java)
                        }
                    }

                    weather = createWeatherData(root, minutelyRoot, alertsRoot)
                } catch (ex: Exception) {
                    weather = null
                    if (ex is IOException) {
                        wEx = WeatherException(ErrorStatus.NETWORKERROR)
                    } else if (ex is WeatherException) {
                        wEx = ex
                    }
                    Logger.writeLine(Log.ERROR, ex, "TomorrowIOWeatherProvider: error getting weather data")
                } finally {
                    response?.closeQuietly()
                    minutelyResponse?.closeQuietly()
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

    override suspend fun getPollenData(location: LocationData): Pollen? =
        withContext(Dispatchers.IO) {
            var pollenData: Pollen? = null

            val key =
                if (settingsManager.usePersonalKey()) settingsManager.getAPIKey(getWeatherAPI()) else getAPIKey()

            val client = sharedDeps.httpClient
            var response: Response? = null

            try {
                // If were under rate limit, deny request
                checkRateLimit()

                val requestUri = Uri.parse(BASE_URL).buildUpon()
                    .appendQueryParameter("apikey", key)
                    .appendQueryParameter("location", updateLocationQuery(location))
                    .appendQueryParameter(
                        "fields",
                        "treeIndex,grassIndex,weedIndex"
                    )
                    .appendQueryParameter("timesteps", "current")
                    .appendQueryParameter("units", "metric")
                    .appendQueryParameter("timezone", "UTC")
                    .build()

                val request = Request.Builder()
                    .cacheRequestIfNeeded(isKeyRequired(), 30, TimeUnit.MINUTES)
                    .url(requestUri.toString())
                    .header("Accept", "application/json")
                    .build()

                // Connect to webstream
                response = client.newCall(request).await()
                checkForErrors(response)
                // Load weather
                val root = response.getStream().use {
                    JSONParser.deserializer<Rootobject>(it, Rootobject::class.java)
                }

                root.data.timelines.firstOrNull()?.intervals?.firstOrNull()?.let { item ->
                    pollenData = Pollen().apply {
                        treePollenCount = when (item.values.treeIndex) {
                            1, 2 -> Pollen.PollenCount.LOW
                            3 -> Pollen.PollenCount.MODERATE
                            4 -> Pollen.PollenCount.HIGH
                            5 -> Pollen.PollenCount.VERY_HIGH
                            else -> Pollen.PollenCount.UNKNOWN
                        }

                        grassPollenCount = when (item.values.grassIndex) {
                            1, 2 -> Pollen.PollenCount.LOW
                            3 -> Pollen.PollenCount.MODERATE
                            4 -> Pollen.PollenCount.HIGH
                            5 -> Pollen.PollenCount.VERY_HIGH
                            else -> Pollen.PollenCount.UNKNOWN
                        }

                        ragweedPollenCount = when (item.values.weedIndex) {
                            1, 2 -> Pollen.PollenCount.LOW
                            3 -> Pollen.PollenCount.MODERATE
                            4 -> Pollen.PollenCount.HIGH
                            5 -> Pollen.PollenCount.VERY_HIGH
                            else -> Pollen.PollenCount.UNKNOWN
                        }
                    }
                }
            } catch (ex: Exception) {
                pollenData = null
                Logger.writeLine(
                    Log.ERROR,
                    ex,
                    "TomorrowIOWeatherProvider: error getting pollen data"
                )
            } finally {
                response?.closeQuietly()
            }

            return@withContext pollenData
        }

    @Throws(WeatherException::class)
    override suspend fun updateWeatherData(location: LocationData, weather: Weather) {
        val offset = location.tzOffset

        // Update tz for weather properties
        weather.updateTime = weather.updateTime.withZoneSameInstant(offset)
        weather.condition.observationTime =
            weather.condition.observationTime.withZoneSameInstant(offset)

        val newAstro = try {
            SunMoonCalcProvider().getAstronomyData(location, weather.condition.observationTime)
        } catch (e: WeatherException) {
            Logger.writeLine(Log.ERROR, e)
            SolCalcAstroProvider().getAstronomyData(location, weather.condition.observationTime)
        }

        if (weather.astronomy != null) {
            weather.astronomy.sunrise =
                weather.astronomy.sunrise.plusSeconds(offset.totalSeconds.toLong())
            weather.astronomy.sunset =
                weather.astronomy.sunset.plusSeconds(offset.totalSeconds.toLong())
            weather.astronomy.moonrise = newAstro.moonrise
            weather.astronomy.moonset = newAstro.moonset
        } else {
            weather.astronomy = newAstro
        }

        // Update icons
        val now = ZonedDateTime.now(ZoneOffset.UTC).withZoneSameInstant(offset).toLocalTime()
        val sunrise = weather.astronomy.sunrise.toLocalTime()
        val sunset = weather.astronomy.sunset.toLocalTime()

        weather.condition.icon =
            getWeatherIcon(now.isBefore(sunrise) || now.isAfter(sunset), weather.condition.icon)
        weather.condition.weather = getWeatherCondition(weather.condition.icon)

        for (forecast in weather.forecast) {
            forecast.date = forecast.date.plusSeconds(offset.totalSeconds.toLong())
            forecast.icon = getWeatherIcon(forecast.icon)
            forecast.condition = getWeatherCondition(forecast.icon)
        }

        for (hr_forecast in weather.hrForecast) {
            val hrf_date = hr_forecast.date.withZoneSameInstant(offset)
            hr_forecast.date = hrf_date

            val hrf_localTime = hrf_date.toLocalTime()
            hr_forecast.icon = getWeatherIcon(
                hrf_localTime.isBefore(sunrise) || hrf_localTime.isAfter(sunset),
                hr_forecast.icon
            )
            hr_forecast.condition = getWeatherCondition(hr_forecast.icon)
        }

        if (!weather.minForecast.isNullOrEmpty()) {
            for (min_forecast in weather.minForecast) {
                min_forecast.date = min_forecast.date.withZoneSameInstant(offset)
            }
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
        return String.format(Locale.ROOT, "%s,%s", df.format(weather.location.latitude), df.format(weather.location.longitude))
    }

    override fun updateLocationQuery(location: LocationData): String {
        val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
        df.applyPattern("0.####")
        return String.format(Locale.ROOT, "%s,%s", df.format(location.latitude), df.format(location.longitude))
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

            /*
             * 1101: Partly cloudy
             * 1100: Mostly Clear
             */
            1100, 1101 -> {
                weatherIcon =
                    if (isNight) WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY else WeatherIcons.DAY_PARTLY_CLOUDY
            }

            /*
             * 1102: Mostly Cloudy
             * 1001: Cloudy
             */
            1102, 1001 -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_CLOUDY else WeatherIcons.DAY_CLOUDY
            }

            /*
             * 2000: Fog
             * 2100: Light fog
             */
            2000, 2100 -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_FOG else WeatherIcons.DAY_FOG
            }

            /* Thunderstorm */
            8000 -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_THUNDERSTORM else WeatherIcons.DAY_THUNDERSTORM
            }

            /*
             * 5001: Flurries
             * 5100: Patchy light snow
             * 5000: Light snow
             */
            5001, 5100, 5000 -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_SNOW else WeatherIcons.DAY_SNOW
            }

            /* Heavy snow */
            5101 -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_SNOW_WIND else WeatherIcons.DAY_SNOW_WIND
            }

            /*
             * 7102: Light Ice pellets
             * 7000: Ice pellets
             * 7101: Heavy Ice pellets
             */
            7102, 7000, 7101 -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_HAIL else WeatherIcons.DAY_HAIL
            }

            /*
             * 4000: Drizzle
             * 4200: Light rain
             */
            4000, 4200 -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_SPRINKLE else WeatherIcons.DAY_SPRINKLE
            }

            /*
             * 6000: Freezing drizzle
             * 6200: Light freezing rain
             * 6001: Freezing rain
             * 6201: Heavy freezing drizzle
             */
            6000, 6200, 6001, 6201 -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_RAIN_MIX else WeatherIcons.DAY_RAIN_MIX
            }

            /* Rain */
            4001 -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_RAIN else WeatherIcons.DAY_RAIN
            }

            /* Heavy rain */
            4201 -> {
                weatherIcon =
                    if (isNight) WeatherIcons.NIGHT_ALT_RAIN_WIND else WeatherIcons.DAY_RAIN_WIND
            }

            /*
             * 3000: Light Wind
             * 3001: Wind
             */
            3000, 3001 -> {
                weatherIcon =
                    if (isNight) WeatherIcons.NIGHT_ALT_CLOUDY_WINDY else WeatherIcons.DAY_CLOUDY_WINDY
            }

            /* Strong Wind */
            3002 -> {
                weatherIcon = WeatherIcons.STRONG_WIND
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