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
import com.thewizrd.shared_resources.weatherdata.auth.AuthType
import com.thewizrd.shared_resources.weatherdata.model.Pollen
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.shared_resources.weatherdata.model.isNullOrInvalid
import com.thewizrd.weather_api.R
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

    override fun getAuthType(): AuthType {
        return AuthType.APIKEY
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
                wEx = WeatherException(ErrorStatus.NETWORKERROR, ex)
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

                    if (key.isNullOrBlank()) {
                        throw WeatherException(ErrorStatus.INVALIDAPIKEY)
                    }

                    val requestUri = Uri.parse(BASE_URL).buildUpon()
                        .appendQueryParameter("apikey", key)
                        .appendQueryParameter("location", location_query)
                        .appendQueryParameter(
                            "fields",
                            "temperature,temperatureApparent,temperatureMin,temperatureMax,dewPoint,humidity,windSpeed,windDirection,windGust,pressureSeaLevel,precipitationIntensity,precipitationProbability,snowAccumulation,sunriseTime,sunsetTime,visibility,cloudCover,moonPhase,weatherCode,weatherCodeFullDay,weatherCodeDay,weatherCodeNight,treeIndex,grassIndex,weedIndex,epaIndex,particulateMatter25,particulateMatter10,pollutantO3,pollutantNO2,pollutantCO,pollutantSO2"
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

                    requireNotNull(root)

                    var minutelyRoot: Rootobject? = null
                    var alertsRoot: AlertsRootobject? = null

                    runCatching {
                        minutelyResponse = client.newCall(minutelyRequest).await()
                        checkForErrors(minutelyResponse!!)
                        minutelyResponse!!.getStream().use {
                            minutelyRoot =
                                JSONParser.deserializer<Rootobject>(it, Rootobject::class.java)
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
                        wEx = WeatherException(ErrorStatus.NETWORKERROR, ex)
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

                requireNotNull(root)

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

        weather.condition.icon.let {
            weather.condition.icon =
                getWeatherIcon(now.isBefore(sunrise) || now.isAfter(sunset), it)
            weather.condition.weather = getWeatherCondition(it)
        }

        for (forecast in weather.forecast) {
            forecast.date = forecast.date.plusSeconds(offset.totalSeconds.toLong())

            forecast.icon.let {
                forecast.icon = getWeatherIcon(it)
                forecast.condition = getWeatherCondition(it)
            }
        }

        for (hr_forecast in weather.hrForecast) {
            val hrfDate = hr_forecast.date.withZoneSameInstant(offset)
            hr_forecast.date = hrfDate

            val hrfLocalTime = hrfDate.toLocalTime()

            hr_forecast.icon.let {
                hr_forecast.icon = getWeatherIcon(
                    hrfLocalTime.isBefore(sunrise) || hrfLocalTime.isAfter(sunset),
                    it
                )
                hr_forecast.condition = getWeatherCondition(it)
            }
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
             * 1001: Cloudy
             */
            1001 -> {
                weatherIcon = WeatherIcons.CLOUDY
            }

            /*
             * 1101: Partly cloudy
             * 1100: Mostly Clear
             * 1103
             * Mixed conditions:
             * Condition 1: Partly Cloudy
             * Condition 2: Mostly Clear
             */
            1100, 1101, 1103 -> {
                weatherIcon =
                    if (isNight) WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY else WeatherIcons.DAY_PARTLY_CLOUDY
            }

            /*
             * 1102: Mostly Cloudy
             */
            1102 -> {
                weatherIcon =
                    if (isNight) WeatherIcons.NIGHT_ALT_CLOUDY else WeatherIcons.DAY_CLOUDY
            }

            /*
             * 2000: Fog
             * 2100: Light fog
             */
            2000, 2100 -> {
                weatherIcon = WeatherIcons.FOG
            }

            /*
             * 3000: Light Wind
             */
            3000 -> {
                weatherIcon =
                    if (isNight) WeatherIcons.NIGHT_LIGHT_WIND else WeatherIcons.DAY_LIGHT_WIND
            }

            /*
             * 3001: Wind
             */
            3001 -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_WINDY else WeatherIcons.DAY_WINDY
            }

            /* Strong Wind */
            3002 -> {
                weatherIcon = WeatherIcons.STRONG_WIND
            }

            /*
             * 4000: Drizzle
             * 4200: Light rain
             */
            4000, 4200 -> {
                weatherIcon = WeatherIcons.SPRINKLE
            }

            /* Rain */
            4001 -> {
                weatherIcon = WeatherIcons.RAIN
            }

            /* Heavy rain */
            4201 -> {
                weatherIcon = WeatherIcons.RAIN_WIND
            }

            /*
             * 5001: Flurries
             * 5100: Light snow
             * 5000: Snow
             */
            5001, 5100, 5000 -> {
                weatherIcon = WeatherIcons.SNOW
            }

            /* Heavy snow */
            5101 -> {
                weatherIcon = WeatherIcons.SNOW_WIND
            }

            /*
             * 6000: Freezing Drizzle
             * 6200: Light Freezing Drizzle
             * 6001: Freezing Rain
             * 6201: Heavy Freezing Rain
             */
            6000, 6200, 6001, 6201 -> {
                weatherIcon = WeatherIcons.RAIN_MIX
            }

            /*
             * 7102: Light Ice Pellets
             * 7000: Ice Pellets
             * 7101: Heavy Ice Pellets
             */
            7102, 7000, 7101 -> {
                weatherIcon = WeatherIcons.HAIL
            }

            /* Thunderstorm */
            8000 -> {
                weatherIcon = WeatherIcons.THUNDERSTORM
            }

            /* Mixed Condition Codes */
            /* 10000, 10001 - Clear */
            10000 -> WeatherIcons.DAY_SUNNY
            10001 -> WeatherIcons.NIGHT_CLEAR

            /*
             * 11000, 11001 - Mostly Clear
             * 11010, 11011 - Partly Cloudy
             */
            11000, 11010 -> WeatherIcons.DAY_PARTLY_CLOUDY
            11001, 11011 -> WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY

            /* 11030: Partly Cloudy */
            11030 -> WeatherIcons.DAY_PARTLY_CLOUDY

            /* 11020, 11021 - Mostly Cloudy */
            11020 -> WeatherIcons.DAY_CLOUDY
            11021 -> WeatherIcons.NIGHT_ALT_CLOUDY

            /* 10010, 10011: Cloudy */
            10010, 10011 -> WeatherIcons.CLOUDY

            /* 11031: Mostly Clear */
            11031 -> WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY

            /*
             * 21000, 21001 -> Light Fog
             * 20000, 20001 -> Fog
             */
            21000, 21001, 20000, 20001 -> WeatherIcons.FOG

            /*
             * Full  D      Nt
             * 2101, 21010, 21011
             * 2102, 21020, 21021
             * 2103, 21030, 21031
             * 2106, 21060, 21061
             * 2107, 21070, 21071
             * 2108, 21080, 21081
             * Mixed conditions:
             * Condition 1: Mostly Clear / Partly Cloudy / Mostly Cloudy
             * Condition 2: Light Fog / Fog
             */
            2101, 2102, 2103, 2106, 2107, 2108 -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_FOG else WeatherIcons.DAY_FOG
            }
            21010, 21020, 21030, 21060, 21070, 21080 -> {
                weatherIcon = WeatherIcons.DAY_FOG
            }
            21011, 21021, 21031, 21061, 21071, 21081 -> {
                weatherIcon = WeatherIcons.NIGHT_FOG
            }

            /*
             * 40000, 40001: Drizzle
             * 42000, 42001: Light Rain
             */
            40000, 40001, 42000, 42001 -> WeatherIcons.SPRINKLE

            /*
             * Full  D      Nt
             * 4203, 42030, 42031
             * 4204, 42040, 42041
             * 4205, 42050, 42051
             * 4213, 42130, 42131
             * 4214, 42140, 42141
             * 4215, 42150, 42151
             * Mixed conditions:
             * Condition 1: Mostly Clear / Partly Cloudy / Mostly Cloudy
             * Condition 2: Drizzle / Light Rain
             */
            4203, 4204, 4205, 4213, 4214, 4215 -> {
                weatherIcon =
                    if (isNight) WeatherIcons.NIGHT_ALT_SPRINKLE else WeatherIcons.DAY_SPRINKLE
            }
            42030, 42040, 42050, 42130, 42140, 42150 -> {
                weatherIcon = WeatherIcons.DAY_SPRINKLE
            }
            42031, 42041, 42051, 42131, 42141, 42151 -> {
                weatherIcon = WeatherIcons.NIGHT_ALT_SPRINKLE
            }

            /*
             * 40010, 40011: Rain
             */
            40010, 40011 -> WeatherIcons.RAIN

            /*
             * Full  D      Nt
             * 4209, 42090, 42091
             * 4208, 42080, 42081
             * 4210, 42100, 42101
             * Mixed conditions:
             * Condition 1: Mostly Clear / Partly Cloudy / Mostly Cloudy
             * Condition 2: Rain
             */
            4209, 4208, 4210 -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_RAIN else WeatherIcons.DAY_RAIN
            }
            42090, 42080, 42100 -> {
                weatherIcon = WeatherIcons.DAY_RAIN
            }
            42091, 42081, 42101 -> {
                weatherIcon = WeatherIcons.NIGHT_ALT_RAIN
            }

            /*
             * 42010, 42011: Heavy Rain
             */
            42010, 42011 -> WeatherIcons.RAIN_WIND

            /*
             * Full  D      Nt
             * 4211, 42110, 42111
             * 4202, 42020, 42021
             * 4212, 42120, 42121
             * Mixed conditions:
             * Condition 1: Mostly Clear / Partly Cloudy / Mostly Cloudy
             * Condition 2: Heavy Rain
             */
            4211, 4202, 4212 -> {
                weatherIcon =
                    if (isNight) WeatherIcons.NIGHT_ALT_RAIN_WIND else WeatherIcons.DAY_RAIN_WIND
            }
            42110, 42020, 42120 -> {
                weatherIcon = WeatherIcons.DAY_RAIN_WIND
            }
            42111, 42021, 42121 -> {
                weatherIcon = WeatherIcons.NIGHT_ALT_RAIN_WIND
            }

            /*
             * 50010, 50011: Flurries
             * 51000, 51001: Light snow
             * 50000, 50001: Snow
             */
            50010, 50011,
            51000, 51001,
            50000, 50001 -> {
                weatherIcon = WeatherIcons.SNOW
            }

            /*
             * Full  D      Nt
             * 5115, 51150, 51151
             * 5116, 51160, 51161
             * 5117, 51170, 51171
             * 5102, 51020, 51021
             * 5103, 51030, 51031
             * 5104, 51040, 51041
             * 5105, 51050, 51051
             * 5106, 51060, 51061
             * 5107, 51070, 51071
             * Mixed conditions:
             * Condition 1: Mostly Clear / Partly Cloudy / Mostly Cloudy
             * Condition 2: Flurries / Light Snow / Snow
             */
            5115, 5116, 5117, 5102, 5103, 5104, 5105, 5106, 5107 -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_SNOW else WeatherIcons.DAY_SNOW
            }
            51150, 51160, 51170, 51020, 51030, 51040, 51050, 51060, 51070 -> {
                weatherIcon = WeatherIcons.DAY_SNOW
            }
            51151, 51161, 51171, 51021, 51031, 51041, 51051, 51061, 51071 -> {
                weatherIcon = WeatherIcons.NIGHT_ALT_SNOW
            }

            /*
             * 5122, 51220, 51221
             * 5110, 51100, 51101
             * 5108, 51080, 51081
             * 5114, 51140, 51141
             * 6204, 62040, 62041
             * 6206, 62060, 62061
             * 6212, 62120, 62121
             * 6220, 62200, 62201
             * 6222, 62220, 62221
             * Mixed conditions:
             * Condition 1: Drizzle / Rain / Light Rain
             * Condition 2: Light Snow / Snow / Freezing Rain / Freezing Drizzle
             *
             * 60000, 60001: Freezing Drizzle
             * 62000, 62001: Light Freezing Drizzle
             * 60010, 60011: Freezing Rain
             * 62010, 62011: Heavy Freezing Rain
             */
            5122, 51220, 51221,
            5110, 51100, 51101,
            5108, 51080, 51081,
            5114, 51140, 51141,
            60000, 60001,
            62000, 62001,
            60010, 60011,
            62010, 62011,
            6204, 62040, 62041,
            6206, 62060, 62061,
            6212, 62120, 62121,
            6220, 62200, 62201,
            6222, 62220, 62221 -> WeatherIcons.RAIN_MIX

            /*
             * Full  D      Nt
             * 6003, 60030, 60031
             * 6002, 60020, 60021
             * 6004, 60040, 60041
             * 6205, 62050, 62051
             * 6203, 62030, 62031
             * 6209, 62090, 62091
             * 6213, 62130, 62131
             * 6214, 62140, 62141
             * 6215, 62150, 62151
             * 6207, 62070, 62071
             * 6202, 62020, 62021
             * 6208, 62080, 62081
             * Mixed conditions:
             * Condition 1: Mostly Clear / Partly Cloudy / Mostly Cloudy
             * Condition 2: Freezing Drizzle / Light Freezing Rain / Freezing Rain / Heavy Freezing Rain
             */
            6003, 6002, 6004, 6205, 6203, 6209, 6213, 6214, 6215, 6207, 6202, 6208 -> {
                weatherIcon =
                    if (isNight) WeatherIcons.NIGHT_ALT_RAIN_MIX else WeatherIcons.DAY_RAIN_MIX
            }
            60030, 60020, 60040, 62050, 62030, 62090, 62130, 62140, 62150, 62070, 62020, 62080 -> {
                weatherIcon = WeatherIcons.DAY_RAIN_MIX
            }
            60031, 60021, 60041, 62051, 62031, 62091, 62131, 62141, 62151, 62071, 62021, 62081 -> {
                weatherIcon = WeatherIcons.NIGHT_ALT_RAIN_MIX
            }

            /*
             * 51010, 51011: Heavy snow
             */
            51010, 51011 -> {
                weatherIcon = WeatherIcons.SNOW_WIND
            }

            /*
             * Full  D      Nt
             * 5119, 51190, 51191
             * 5120, 51200, 51201
             * 5121, 51210, 51211
             * Mixed conditions:
             * Condition 1: Mostly Clear / Partly Cloudy / Mostly Cloudy
             * Condition 2: Heavy Snow
             */
            5119, 5120, 5121 -> {
                weatherIcon =
                    if (isNight) WeatherIcons.NIGHT_ALT_SNOW_WIND else WeatherIcons.DAY_SNOW_WIND
            }
            51190, 51200, 51210 -> {
                weatherIcon = WeatherIcons.DAY_SNOW_WIND
            }
            51191, 51201, 51211 -> {
                weatherIcon = WeatherIcons.NIGHT_ALT_SNOW_WIND
            }

            /*
             * 5112, 51120, 51121
             * Mixed conditions:
             * Condition 1: Snow
             * Condition 2: Ice Pellets
             *
             * 71020, 71021: Light Ice Pellets
             * 70000, 70001: Ice Pellets
             * 71010, 71011: Heavy Ice Pellets
             */
            5112, 51120, 51121,
            71020, 71021,
            70000, 70001,
            71010, 71011 -> WeatherIcons.HAIL

            /*
             * Full  D      Nt
             * 7110, 71100, 71101
             * 7111, 71110, 71111
             * 7112, 71120, 71121
             * 7108, 71080, 71081
             * 7107, 71070, 71071
             * 7109, 71090, 71091
             * 7113, 71130, 71131
             * 7114, 71140, 71141
             * 7116, 71160, 71161
             * Mixed conditions:
             * Condition 1: Mostly Clear / Partly Cloudy / Mostly Cloudy
             * Condition 2: Light Ice Pellets / Ice Pellets / Heavy Ice Pellets
             */
            7110, 7111, 7112, 7108, 7107, 7109, 7113, 7114, 7116 -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_HAIL else WeatherIcons.DAY_HAIL
            }
            71100, 71110, 71120, 71080, 71070, 71090, 71130, 71140, 71160 -> {
                weatherIcon = WeatherIcons.DAY_HAIL
            }
            71101, 71111, 71121, 71081, 71071, 71091, 71131, 71141, 71161 -> {
                weatherIcon = WeatherIcons.NIGHT_ALT_HAIL
            }

            /*
             * 7105, 71050, 71051
             * 7115, 71150, 71151
             * 7117, 71170, 71171
             * 7106, 71060, 71061
             * 7103, 71030, 71031
             * Mixed conditions:
             * Condition 1: Drizzle / Rain / Light Rain / Freezing Rain
             * Condition 2: Ice Pellets / Heavy Ice Pellets
             */
            7105, 71050, 71051,
            7115, 71150, 71151,
            7117, 71170, 71171,
            7106, 71060, 71061,
            7103, 71030, 71031 -> WeatherIcons.RAIN_MIX

            /*
             * 80000, 80001: Thunderstorm
             */
            80000, 80001 -> {
                weatherIcon = WeatherIcons.THUNDERSTORM
            }

            /*
             * Full  D      Nt
             * 8001, 80010, 80011
             * 8003, 80030, 80031
             * 8002, 80020, 80021
             * Mixed conditions:
             * Condition 1: Mostly Clear / Partly Cloudy / Mostly Cloudy
             * Condition 2: Thunderstorm
             */
            8001, 8003, 8002 -> {
                weatherIcon =
                    if (isNight) WeatherIcons.NIGHT_ALT_THUNDERSTORM else WeatherIcons.DAY_THUNDERSTORM
            }
            80010, 80030, 80020 -> {
                weatherIcon = WeatherIcons.DAY_THUNDERSTORM
            }
            80011, 80031, 80021 -> {
                weatherIcon = WeatherIcons.NIGHT_ALT_THUNDERSTORM
            }
        }

        if (weatherIcon.isBlank()) {
            // Not Available
            weatherIcon = WeatherIcons.NA
        }
        return weatherIcon
    }

    override fun getWeatherCondition(icon: String?): String {
        val conditionCode = icon?.toIntOrNull() ?: return super.getWeatherCondition(icon)

        return when (conditionCode) {
            /* Clear */
            1000, 10000, 10001 -> context.getString(R.string.weather_clear)
            /* Mostly Clear */
            1100, 11000, 11001 -> context.getString(R.string.weather_mostlyclear)
            /* Partly Cloudy */
            1101, 11010, 11011 -> context.getString(R.string.weather_partlycloudy)
            /* Mostly Cloudy */
            1102, 11020, 11021 -> context.getString(R.string.weather_mostlycloudy)
            /* Cloudy */
            1001, 10010, 10011 -> context.getString(R.string.weather_cloudy)
            /* Mostly Clear */
            1103, 11030, 11031 -> context.getString(R.string.weather_mostlyclear)
            /* Light Fog */
            2100, 21000, 21001,
            2101, 21010, 21011,
            2102, 21020, 21021,
            2103, 21030, 21031 -> context.getString(R.string.weather_lightfog)
            /* Fog */
            2000, 20000, 20001,
            2106, 21060, 21061,
            2107, 21070, 21071,
            2108, 21080, 21081 -> context.getString(R.string.weather_fog)
            /* Drizzle */
            4000, 40000, 40001,
            4203, 42030, 42031,
            4204, 42040, 42041,
            4205, 42050, 42051 -> context.getString(R.string.weather_drizzle)
            /* Light Rain */
            4200, 42000, 42001,
            4213, 42130, 42131,
            4214, 42140, 42141,
            4215, 42150, 42151 -> context.getString(R.string.weather_lightrain)
            /* Rain */
            4001, 40010, 40011,
            4209, 42090, 42091,
            4208, 42080, 42081,
            4210, 42100, 42101 -> context.getString(R.string.weather_rain)
            /* Heavy Rain */
            4201, 42010, 42011,
            4211, 42110, 42111 -> context.getString(R.string.weather_heavyrain)
            /* Flurries */
            5001, 50010, 50011,
            5115, 51150, 51151,
            5116, 51160, 51161,
            5117, 51170, 51171 -> context.getString(R.string.weather_snowflurries)
            /* Light Snow */
            5100, 51000, 51001,
            5102, 51020, 51021,
            5103, 51030, 51031,
            5104, 51040, 51041 -> context.getString(R.string.weather_lightsnowshowers)
            /* Snow */
            5000, 50000, 50001,
                /*
                 * Mixed conditions:
                 * Condition 1: Mostly Clear / Partly Cloudy / Mostly Cloudy
                 * Condition 2: Snow
                 */
            5105, 51050, 51051,
            5106, 51060, 51061,
            5107, 51070, 51071 -> context.getString(R.string.weather_snow)
            /* Heavy Snow */
            5101, 51010, 51011,
                /*
                 * Mixed conditions:
                 * Condition 1: Mostly Clear / Partly Cloudy / Mostly Cloudy
                 * Condition 2: Snow
                 */
            5119, 51190, 51191,
            5120, 51200, 51201,
            5121, 51210, 51211 -> context.getString(R.string.weather_heavysnow)
            /*
             * Mixed conditions:
             * Condition 1: Drizzle / Rain
             * Condition 2: Light Snow / Snow
             */
            5122, 51220, 51221,
            5110, 51100, 51101,
            5108, 51080, 51081 -> context.getString(R.string.weather_rainandsnow)
            /*
             * Mixed conditions:
             * Condition 1: Snow
             * Condition 2: Freezing Rain
             */
            5114, 51140, 51141,
                /* Freezing Drizzle / Light Freezing Drizzle / Freezing Rain / Heavy Freezing Rain */
            6000, 60000, 60001,
            6200, 62000, 62001,
            6001, 60010, 60011,
            6201, 62010, 62011,
                /*
                 * Mixed conditions:
                 * Condition 1: Mostly Clear / Partly Cloudy / Mostly Cloudy
                 * Condition 2: Freezing Drizzle / Light Freezing Rain / Freezing Rain
                 */
            6003, 60030, 60031,
            6002, 60020, 60021,
            6004, 60040, 60041,
            6204, 62040, 62041,
            6206, 62060, 62061,
            6205, 62050, 62051,
            6203, 62030, 62031,
            6209, 62090, 62091,
            6213, 62130, 62131,
            6214, 62140, 62141,
            6215, 62150, 62151,
                /*
                 * Mixed conditions:
                 * Condition 1: Drizzle / Light Rain / Rain
                 * Condition 2: Freezing Rain
                 */
            6212, 62120, 62121,
            6222, 62220, 62221,
                /*
                 * Mixed conditions:
                 * Condition 1: Mostly Clear / Partly Cloudy / Mostly Cloudy
                 * Condition 2: Heavy Freezing Rain
                 */
            6207, 62070, 62071,
            6202, 62020, 62021,
            6208, 62080, 62081 -> context.getString(R.string.weather_freezingrain)
            /*
             * Mixed conditions:
             * Condition 1: Snow
             * Condition 2: Ice Pellets
             */
            5112, 51120, 51121 -> context.getString(R.string.weather_snowandsleet)
            /* Light Ice Pellets / Ice Pellets / Heavy Ice Pellets / [Sleet] */
            7102, 71020, 71021,
            7000, 70000, 70001,
            7101, 71010, 71011,
                /*
                 * Mixed conditions:
                 * Condition 1: Mostly Clear / Partly Cloudy / Mostly Cloudy
                 * Condition 2: Light Ice Pellets / Ice Pellets / Heavy Ice Pellets
                 */
            7110, 71100, 71101,
            7111, 71110, 71111,
            7112, 71120, 71121,
            7108, 71080, 71081,
            7107, 71070, 71071,
            7109, 71090, 71091,
            7113, 71130, 71131,
            7114, 71140, 71141,
            7116, 71160, 71161 -> context.getString(R.string.weather_sleet)
            /*
             * Mixed conditions:
             * Condition 1: Drizzle / Light Rain / Rain / Freezing Rain
             * Condition 2: Light Ice Pellets / Ice Pellets / Heavy Ice Pellets
             */
            7105, 71050, 71051,
            7115, 71150, 71151,
            7117, 71170, 71171,
            7106, 71060, 71061,
            7103, 71030, 71031 -> context.getString(R.string.weather_rainandsleet)
            /* Thunderstorm */
            8000, 80000, 80001,
                /*
                 * Mixed conditions:
                 * Condition 1: Mostly Clear / Partly Cloudy / Mostly Cloudy
                 * Condition 2: Thunderstorm
                 */
            8001, 80010, 80011,
            8003, 80030, 80031,
            8002, 80020, 80021 -> context.getString(R.string.weather_tstorms)
            else -> context.getString(R.string.weather_notavailable)
        }
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