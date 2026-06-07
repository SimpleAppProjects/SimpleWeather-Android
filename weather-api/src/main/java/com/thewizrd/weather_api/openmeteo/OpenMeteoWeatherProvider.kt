package com.thewizrd.weather_api.openmeteo

import android.util.Log
import androidx.core.net.toUri
import com.ibm.icu.util.ULocale
import com.thewizrd.shared_resources.R
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
import com.thewizrd.weather_api.BuildConfig
import com.thewizrd.weather_api.extras.cacheRequestIfNeeded
import com.thewizrd.weather_api.keys.Keys
import com.thewizrd.weather_api.nws.SolCalcAstroProvider
import com.thewizrd.weather_api.smc.SunMoonCalcProvider
import com.thewizrd.weather_api.utils.APIRequestUtils.checkForErrors
import com.thewizrd.weather_api.utils.APIRequestUtils.checkRateLimit
import com.thewizrd.weather_api.utils.APIRequestUtils.throwIfRateLimited
import com.thewizrd.weather_api.utils.logMissingIcon
import com.thewizrd.weather_api.weatherModule
import com.thewizrd.weather_api.weatherapi.location.WeatherApiLocationProvider
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
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max

class OpenMeteoWeatherProvider : WeatherProviderImpl(), PollenProvider {
    companion object {
        private const val NON_COMMERCIAL_FORECAST_URL = "https://api.open-meteo.com/v1/forecast"
        private const val COMMERCIAL_FORECAST_URL =
            "https://customer-api.open-meteo.com/v1/forecast"

        private const val NON_COMMERCIAL_AQ_URL =
            "https://air-quality-api.open-meteo.com/v1/air-quality"
        private const val COMMERCIAL_AQ_URL =
            "https://customer-air-quality-api.open-meteo.com/v1/air-quality"
        private const val KEYCHECK_QUERY_URL = "$COMMERCIAL_FORECAST_URL?apikey=%s"
    }

    init {
        mLocationProvider = runCatching {
            weatherModule.locationProviderFactory.getLocationProvider(
                remoteConfigService.getLocationProvider(
                    getWeatherAPI()
                )
            )
        }.getOrElse {
            WeatherApiLocationProvider()
        }
    }

    override fun getWeatherAPI(): String {
        return WeatherAPI.OPENMETEO
    }

    override fun supportsWeatherLocale(): Boolean {
        return false
    }

    override fun isKeyRequired(): Boolean {
        return BuildConfig.IS_NONGMS
    }

    override fun getAuthType(): AuthType {
        return if (BuildConfig.IS_NONGMS) AuthType.APIKEY else AuthType.INTERNAL
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
        return Keys.getOpenMeteoKey()
    }

    @Throws(WeatherException::class)
    override suspend fun getWeatherData(location: LocationData): Weather =
        withContext(Dispatchers.IO) {
            var weather: Weather?

            val uLocale = ULocale.forLocale(LocaleUtils.getLocale())
            val locale = localeToLangCode(uLocale.language, uLocale.toLanguageTag())
            val query = updateLocationQuery(location)

            val key = getProviderKey()

            val client = sharedDeps.httpClient
            var response: Response? = null
            var aqiResponse: Response? = null
            var wEx: WeatherException? = null

            try {
                // If were under rate limit, deny request
                checkRateLimit()

                if ((isKeyRequired() || getAuthType() == AuthType.INTERNAL) && key.isNullOrBlank()) {
                    throw WeatherException(ErrorStatus.INVALIDAPIKEY)
                }

                val baseUrl = if (BuildConfig.IS_NONGMS) {
                    NON_COMMERCIAL_FORECAST_URL
                } else {
                    COMMERCIAL_FORECAST_URL
                }

                val aqiBaseUrl = if (BuildConfig.IS_NONGMS) {
                    NON_COMMERCIAL_AQ_URL
                } else {
                    COMMERCIAL_AQ_URL
                }

                val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
                df.applyPattern("0.####")

                val forecastRequestUri = baseUrl.toUri().buildUpon()
                    .appendQueryParameter("latitude", df.format(location.latitude))
                    .appendQueryParameter("longitude", df.format(location.longitude))
                    .appendQueryParameter(
                        "daily",
                        "weather_code,temperature_2m_max,temperature_2m_min,uv_index_max,precipitation_probability_max,wind_speed_10m_max,wind_gusts_10m_max,wind_direction_10m_dominant,cloud_cover_mean,apparent_temperature_mean,dew_point_2m_mean,pressure_msl_mean,relative_humidity_2m_mean,visibility_mean"
                    )
                    .appendQueryParameter(
                        "hourly",
                        "temperature_2m,relative_humidity_2m,dew_point_2m,apparent_temperature,precipitation_probability,weather_code,pressure_msl,cloud_cover,visibility,wind_speed_10m,wind_gusts_10m,wind_direction_10m,rain,snowfall,uv_index,is_day"
                    )
                    .appendQueryParameter(
                        "current",
                        "temperature_2m,relative_humidity_2m,dew_point_2m,apparent_temperature,precipitation_probability,weather_code,pressure_msl,cloud_cover,visibility,wind_speed_10m,wind_gusts_10m,wind_direction_10m,rain,snowfall,uv_index,is_day"
                    )
                    .appendQueryParameter("minutely_15", "rain,snowfall")
                    .appendQueryParameter("models", "best_match")
                    .appendQueryParameter("timeformat", "unixtime")
                    .appendQueryParameter("timezone", "auto")
                    .apply {
                        if (isKeyRequired() || getAuthType() == AuthType.INTERNAL) {
                            this.appendQueryParameter("apikey", key)
                        }
                    }
                    .build()

                val forecastRequest = Request.Builder()
                    .cacheRequestIfNeeded(
                        (isKeyRequired() || getAuthType() == AuthType.INTERNAL),
                        30,
                        TimeUnit.MINUTES
                    )
                    .url(forecastRequestUri.toString())
                    .header("Accept", "application/json")
                    .build()

                val aqiRequestUri = aqiBaseUrl.toUri().buildUpon()
                    .appendQueryParameter("latitude", df.format(location.latitude))
                    .appendQueryParameter("longitude", df.format(location.longitude))
                    .appendQueryParameter(
                        "current",
                        "us_aqi,ragweed_pollen,grass_pollen,birch_pollen,alder_pollen,pm10,pm2_5,carbon_monoxide,nitrogen_dioxide,sulphur_dioxide,ozone"
                    )
                    .appendQueryParameter("timeformat", "unixtime")
                    .appendQueryParameter("timezone", "auto")
                    .apply {
                        if (isKeyRequired() || getAuthType() == AuthType.INTERNAL) {
                            this.appendQueryParameter("apikey", key)
                        }
                    }
                    .build()

                val aqiRequest = Request.Builder()
                    .cacheRequestIfNeeded(
                        (isKeyRequired() || getAuthType() == AuthType.INTERNAL),
                        20,
                        TimeUnit.MINUTES
                    )
                    .url(aqiRequestUri.toString())
                    .header("Accept", "application/json")
                    .build()

                // Connect to webstream
                response = client.newCall(forecastRequest).await()
                checkForErrors(response)
                // Load weather
                val root = response.getStream().use {
                    JSONParser.deserializer<ForecastResponse>(it, ForecastResponse::class.java)
                }

                val aqiRoot = runCatching {
                    aqiResponse = client.newCall(aqiRequest).await()
                    checkForErrors(aqiResponse)
                    aqiResponse.getStream().use {
                        JSONParser.deserializer<AQIResponse>(
                            it,
                            AQIResponse::class.java
                        )
                    }
                }.getOrNull()

                requireNotNull(root)
                requireNotNull(aqiRoot)

                weather = createWeatherData(root, aqiRoot)
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
                    "OpenMeteoWeatherProvider: error getting weather data"
                )
            } finally {
                response?.closeQuietly()
                aqiResponse?.closeQuietly()
            }

            if (wEx == null && weather.isNullOrInvalid()) {
                wEx = WeatherException(ErrorStatus.NOWEATHER)
            } else if (weather != null) {
                if (supportsWeatherLocale()) weather.locale = locale

                weather.query = query
            }

            if (wEx != null) throw wEx

            return@withContext weather!!
        }

    override suspend fun getPollenData(location: LocationData): Pollen? =
        withContext(Dispatchers.IO) {
            var pollenData: Pollen? = null

            val key = getProviderKey()

            val client = sharedDeps.httpClient
            var response: Response? = null

            try {
                // If were under rate limit, deny request
                checkRateLimit()

                if ((isKeyRequired() || getAuthType() == AuthType.INTERNAL) && key.isNullOrBlank()) {
                    throw WeatherException(ErrorStatus.INVALIDAPIKEY)
                }

                val baseUrl = if (BuildConfig.IS_NONGMS) {
                    NON_COMMERCIAL_AQ_URL
                } else {
                    COMMERCIAL_AQ_URL
                }

                val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
                df.applyPattern("0.####")

                val requestUri = baseUrl.toUri().buildUpon()
                    .appendQueryParameter("latitude", df.format(location.latitude))
                    .appendQueryParameter("longitude", df.format(location.longitude))
                    .appendQueryParameter(
                        "current",
                        "ragweed_pollen,grass_pollen,birch_pollen,alder_pollen"
                    )
                    .appendQueryParameter("timeformat", "unixtime")
                    .appendQueryParameter("timezone", "auto")
                    .apply {
                        if (isKeyRequired() || getAuthType() == AuthType.INTERNAL) {
                            this.appendQueryParameter("apikey", key)
                        }
                    }
                    .build()

                val request = Request.Builder()
                    .cacheRequestIfNeeded(
                        isKeyRequired() || getAuthType() == AuthType.INTERNAL,
                        20,
                        TimeUnit.MINUTES
                    )
                    .url(requestUri.toString())
                    .header("Accept", "application/json")
                    .build()

                // Connect to webstream
                response = client.newCall(request).await()
                checkForErrors(response)
                // Load weather
                val root = response.getStream().use {
                    JSONParser.deserializer<AQIResponse>(it, AQIResponse::class.java)
                }

                requireNotNull(root)

                root.current?.let { current ->
                    val treePollenMeasure =
                        if (current.birchPollen != null && current.alderPollen != null) {
                            max(current.birchPollen!!, current.alderPollen!!)
                        } else {
                            current.birchPollen ?: current.alderPollen
                        }
                    val grassPollenMeasure = current.grassPollen
                    val ragweedPollenMeasure = current.ragweedPollen

                    pollenData = Pollen().apply {
                        treePollenCount = when {
                            treePollenMeasure == null -> Pollen.PollenCount.UNKNOWN
                            treePollenMeasure < 15 -> Pollen.PollenCount.LOW
                            treePollenMeasure < 90 -> Pollen.PollenCount.MODERATE
                            treePollenMeasure < 1500 -> Pollen.PollenCount.HIGH
                            treePollenMeasure >= 1500 -> Pollen.PollenCount.VERY_HIGH
                            else -> Pollen.PollenCount.UNKNOWN
                        }

                        grassPollenCount = when {
                            grassPollenMeasure == null -> Pollen.PollenCount.UNKNOWN
                            grassPollenMeasure < 5 -> Pollen.PollenCount.LOW
                            grassPollenMeasure < 20 -> Pollen.PollenCount.MODERATE
                            grassPollenMeasure < 200 -> Pollen.PollenCount.HIGH
                            grassPollenMeasure >= 200 -> Pollen.PollenCount.VERY_HIGH
                            else -> Pollen.PollenCount.UNKNOWN
                        }

                        ragweedPollenCount = when {
                            ragweedPollenMeasure == null -> Pollen.PollenCount.UNKNOWN
                            ragweedPollenMeasure < 10 -> Pollen.PollenCount.LOW
                            ragweedPollenMeasure < 50 -> Pollen.PollenCount.MODERATE
                            ragweedPollenMeasure < 500 -> Pollen.PollenCount.HIGH
                            ragweedPollenMeasure >= 500 -> Pollen.PollenCount.VERY_HIGH
                            else -> Pollen.PollenCount.UNKNOWN
                        }
                    }
                }
            } catch (ex: Exception) {
                pollenData = null
                Logger.writeLine(
                    Log.ERROR,
                    ex,
                    "OpenMeteoWeatherProvider: error getting pollen data"
                )
            } finally {
                response?.closeQuietly()
            }

            return@withContext pollenData
        }

    @Throws(WeatherException::class)
    override suspend fun updateWeatherData(location: LocationData, weather: Weather) {
        super.updateWeatherData(location, weather)

        val offset = location.tzOffset

        // Update tz for weather properties
        weather.updateTime = weather.updateTime!!.withZoneSameInstant(offset)
        weather.condition!!.observationTime =
            weather.condition!!.observationTime.withZoneSameInstant(offset)

        weather.astronomy = try {
            SunMoonCalcProvider().getAstronomyData(location, weather.condition!!.observationTime)
        } catch (e: WeatherException) {
            Logger.writeLine(Log.ERROR, e, "Error")
            SolCalcAstroProvider().getAstronomyData(location, weather.condition!!.observationTime)
        }

        weather.condition?.icon.let {
            weather.condition!!.icon = getWeatherIcon(it)
            weather.condition!!.weather = getWeatherCondition(it)
        }

        weather.forecast?.forEach { forecast ->
            forecast.date = forecast.date.plusSeconds(offset.totalSeconds.toLong())

            forecast.icon.let {
                forecast.icon = getWeatherIcon(it)
                forecast.condition = getWeatherCondition(it)
            }
        }

        weather.hrForecast?.forEach { hrf ->
            hrf.date = hrf.date.withZoneSameInstant(offset)

            hrf.icon?.let {
                hrf.icon = getWeatherIcon(it)
                hrf.condition = getWeatherCondition(it)
            }
        }

        weather.minForecast?.forEach { minutelyForecast ->
            minutelyForecast.date = minutelyForecast.date.withZoneSameInstant(offset)
        }
    }

    override suspend fun updateLocationQuery(weather: Weather): String {
        val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
        df.applyPattern("0.####")
        return String.format(
            Locale.ROOT,
            "latitude=%s&longitude=%s",
            df.format(weather.location!!.latitude),
            df.format(weather.location!!.longitude)
        )
    }

    override suspend fun updateLocationQuery(location: LocationData): String {
        val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
        df.applyPattern("0.####")
        return String.format(
            Locale.ROOT,
            "latitude=%s&longitude=%s",
            df.format(location.latitude),
            df.format(location.longitude)
        )
    }

    override fun getWeatherIcon(icon: String?): String {
        var isNight = false

        if (icon == null) return WeatherIcons.NA

        if (icon.endsWith("_0")) isNight = true

        return getWeatherIcon(isNight, icon)
    }

    override fun getWeatherIcon(isNight: Boolean, icon: String?): String {
        var weatherIcon = ""

        val conditionCode =
            icon?.split('_')?.getOrNull(0)?.toIntOrNull() ?: return WeatherIcons.NA

        when (conditionCode) {
            /*
             * 0: Clear sky
             * 1: Mainly clear
             */
            0, 1 -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_CLEAR else WeatherIcons.DAY_SUNNY
            }
            /*
             * 2: Partly cloudy
             */
            2 -> {
                weatherIcon =
                    if (isNight) WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY else WeatherIcons.DAY_PARTLY_CLOUDY
            }

            /*
             * 3: overcast
             */
            3 -> {
                weatherIcon =
                    if (isNight) WeatherIcons.NIGHT_OVERCAST else WeatherIcons.DAY_SUNNY_OVERCAST
            }

            /*
             * 45: Fog
             * 48: depositing rime fog
             */
            45, 48 -> {
                weatherIcon = WeatherIcons.FOG
            }

            /*
             * 51, 53, 55: Drizzle: Light, moderate, and dense intensity
             */
            51, 53, 55 -> {
                weatherIcon = WeatherIcons.SPRINKLE
            }

            /*
             * 56, 57: Freezing Drizzle: Light and dense intensity
             */
            56, 57 -> {
                weatherIcon = WeatherIcons.RAIN_MIX
            }

            /*
             * 61, 63, 65: Rain: Slight, moderate and heavy intensity
             */
            61, 63 -> {
                weatherIcon = WeatherIcons.RAIN
            }

            65 -> {
                weatherIcon = WeatherIcons.RAIN_WIND
            }

            /*
             * 66, 67: Freezing Rain: Light and heavy intensity
             */
            66, 67 -> {
                weatherIcon = WeatherIcons.RAIN_MIX
            }

            /*
             * 71, 73, 75: Snow fall: Slight, moderate, and heavy intensity
             * 77: Snow grains
             */
            71, 73, 77 -> {
                weatherIcon = WeatherIcons.SNOW
            }

            75 -> {
                weatherIcon = WeatherIcons.SNOW_WIND
            }

            /*
             * 80, 81, 82: Rain showers: Slight, moderate, and violent
             */
            80, 81 -> {
                weatherIcon = WeatherIcons.SHOWERS
            }

            82 -> {
                weatherIcon = WeatherIcons.STORM_SHOWERS
            }

            /*
             * 85, 86: Snow showers slight and heavy
             */
            85 -> {
                weatherIcon = WeatherIcons.SNOW
            }

            86 -> {
                weatherIcon = WeatherIcons.SNOW_WIND
            }

            /*
             * 95: Thunderstorm: Slight or moderate
             * 96, 99: Thunderstorm with slight and heavy hail
             */
            95 -> {
                weatherIcon = WeatherIcons.THUNDERSTORM
            }

            96, 99 -> {
                weatherIcon = WeatherIcons.SNOW_THUNDERSTORM
            }
        }

        if (weatherIcon.isBlank()) {
            // Not Available
            logMissingIcon(icon)
            weatherIcon = WeatherIcons.NA
        }
        return weatherIcon
    }

    override fun getWeatherCondition(icon: String?): String {
        val conditionCode = icon?.split('_')?.getOrNull(0)?.toIntOrNull()
            ?: return super.getWeatherCondition(icon)

        return when (conditionCode) {
            /* 0: Clear sky */
            0 -> context.getString(R.string.weather_clearsky)
            /* 1: Mainly clear */
            1 -> context.getString(R.string.weather_mostlyclear)
            /* 2: partly cloudy */
            2 -> context.getString(R.string.weather_partlycloudy)
            /* 3: overcast */
            3 -> context.getString(R.string.weather_overcast)
            /* 45, 48: Fog and depositing rime fog */
            45, 48 -> context.getString(R.string.weather_fog)
            /* 51, 53, 55: Drizzle: Light, moderate, and dense intensity */
            51, 53, 55 -> context.getString(R.string.weather_drizzle)
            /*
             * 56, 57: Freezing Drizzle: Light and dense intensity
             * 66, 67: Freezing Rain: Light and heavy intensity
             */
            56, 57, 66, 67 -> context.getString(R.string.weather_freezingrain)
            /* 61, 63, 65: Rain: Slight, moderate and heavy intensity */
            61 -> context.getString(R.string.weather_lightrain)
            63 -> context.getString(R.string.weather_rain)
            65 -> context.getString(R.string.weather_heavyrain)
            /*
             * 71, 73, 75: Snow fall: Slight, moderate, and heavy intensity
             * 77: Snow grains
             */
            71 -> context.getString(R.string.weather_lightsnowshowers)
            73, 77 -> context.getString(R.string.weather_snow)
            75 -> context.getString(R.string.weather_heavysnow)
            /* 80, 81, 82: Rain showers: Slight, moderate, and violent */
            80, 81, 82 -> context.getString(R.string.weather_rainshowers)
            /* 85, 86: Snow showers slight and heavy */
            85 -> context.getString(R.string.weather_lightsnowshowers)
            86 -> context.getString(R.string.weather_heavysnow)
            /*
             * 95: Thunderstorm: Slight or moderate
             * 96, 99: Thunderstorm with slight and heavy hail
             */
            95, 96, 99 -> context.getString(R.string.weather_tstorms)
            else -> {
                logMissingIcon(icon)
                context.getString(R.string.weather_notavailable)
            }
        }
    }

    // Some conditions can be for any time of day
    // So use sunrise/set data as fallback
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
            if (now.toNanoOfDay() < sunrise.toNanoOfDay() || now.toNanoOfDay() > sunset.toNanoOfDay())
                isNight = true
        }

        return isNight
    }
}