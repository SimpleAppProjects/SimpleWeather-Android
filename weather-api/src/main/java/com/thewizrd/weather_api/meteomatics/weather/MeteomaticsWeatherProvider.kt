package com.thewizrd.weather_api.meteomatics.weather

import android.net.Uri
import android.util.Log
import com.thewizrd.shared_resources.exceptions.ErrorStatus
import com.thewizrd.shared_resources.exceptions.WeatherException
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.await
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.getStream
import com.thewizrd.shared_resources.remoteconfig.remoteConfigService
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.utils.ZoneIdCompat
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.auth.AuthType
import com.thewizrd.shared_resources.weatherdata.auth.BasicAuthProviderKey
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.shared_resources.weatherdata.model.isNullOrInvalid
import com.thewizrd.weather_api.R
import com.thewizrd.weather_api.extras.cacheRequestIfNeeded
import com.thewizrd.weather_api.locationiq.LocationIQProvider
import com.thewizrd.weather_api.utils.APIRequestUtils.checkForErrors
import com.thewizrd.weather_api.utils.APIRequestUtils.checkRateLimit
import com.thewizrd.weather_api.utils.APIRequestUtils.throwIfRateLimited
import com.thewizrd.weather_api.weatherModule
import com.thewizrd.weather_api.weatherdata.WeatherProviderImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.internal.closeQuietly
import java.io.IOException
import java.net.HttpURLConnection
import java.text.DecimalFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit

class MeteomaticsWeatherProvider : WeatherProviderImpl() {
    companion object {
        private const val BASE_QUERY_URL = "https://api.meteomatics.com/"
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
        return WeatherAPI.METEOMATICS
    }

    override fun supportsWeatherLocale(): Boolean {
        return false
    }

    override fun isKeyRequired(): Boolean {
        return true
    }

    override suspend fun isKeyValid(key: String?): Boolean = withContext(Dispatchers.IO) {
        val providerKey = BasicAuthProviderKey().apply {
            fromString(key ?: "")
        }

        if (providerKey.username.isBlank() || providerKey.password.isBlank()) {
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
                .url(String.format(BASE_QUERY_URL, key))
                .post("".toRequestBody("application/json".toMediaTypeOrNull()))
                .header("Authorization", key!!)
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

    override fun getAuthType(): AuthType {
        return AuthType.BASIC
    }

    override suspend fun getWeather(location_query: String, country_code: String): Weather =
        withContext(Dispatchers.IO) {
            var weather: Weather?

            val key =
                if (settingsManager.usePersonalKey()) settingsManager.getAPIKey(getWeatherAPI()) else getAPIKey()

            val providerKey = BasicAuthProviderKey().apply {
                fromString(key ?: "")
            }

            if (providerKey.username.isBlank() || providerKey.password.isBlank()) {
                throw WeatherException(ErrorStatus.INVALIDAPIKEY)
            }

            val client = sharedDeps.httpClient
            var wEx: WeatherException? = null

            try {
                // If were under rate limit, deny request
                checkRateLimit()

                val currentRequest = Request.Builder()
                    .cacheRequestIfNeeded(isKeyRequired(), 20, TimeUnit.MINUTES)
                    .url(createCurrentRequestUri(location_query).toString())
                    .header("Authorization", key!!)
                    .build()
                val forecastRequest = Request.Builder()
                    .cacheRequestIfNeeded(isKeyRequired(), 1, TimeUnit.HOURS)
                    .url(createDailyRequestUri(location_query).toString())
                    .header("Authorization", key)
                    .build()
                val hourlyRequest = Request.Builder()
                    .cacheRequestIfNeeded(isKeyRequired(), 3, TimeUnit.HOURS)
                    .url(createHourlyRequestUri(ZonedDateTime.now(), location_query).toString())
                    .header("Authorization", key)
                    .build()
                val minutelyRequest = Request.Builder()
                    .cacheRequestIfNeeded(isKeyRequired(), 30, TimeUnit.MINUTES)
                    .url(createMinutelyRequestUri(ZonedDateTime.now(), location_query).toString())
                    .header("Authorization", key)
                    .build()

                // Connect to webstream
                val currentResponse = client.newCall(currentRequest).await()
                checkForErrors(currentResponse)

                val forecastResponse = client.newCall(forecastRequest).await()
                checkForErrors(forecastResponse)

                val hourlyResponse = client.newCall(hourlyRequest).await()
                checkForErrors(hourlyResponse)

                val minutelyResponse = client.newCall(minutelyRequest).await()
                checkForErrors(minutelyResponse)

                val currentRoot = currentResponse.use { r ->
                    r.getStream().use { s ->
                        JSONParser.deserializer<WeatherResponse>(s, WeatherResponse::class.java)
                    }
                }
                val forecastRoot = forecastResponse.use { r ->
                    r.getStream().use { s ->
                        JSONParser.deserializer<WeatherResponse>(s, WeatherResponse::class.java)
                    }
                }
                val hourlyRoot = hourlyResponse.use { r ->
                    r.getStream().use { s ->
                        JSONParser.deserializer<WeatherResponse>(s, WeatherResponse::class.java)
                    }
                }
                val minutelyRoot = minutelyResponse.use { r ->
                    r.getStream().use { s ->
                        JSONParser.deserializer<WeatherResponse>(s, WeatherResponse::class.java)
                    }
                }

                weather = createWeatherData(currentRoot, forecastRoot, hourlyRoot, minutelyRoot)
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
                    "MeteomaticsWeatherProvider: error getting weather data"
                )
            }

            if (wEx == null && weather.isNullOrInvalid()) {
                wEx = WeatherException(ErrorStatus.NOWEATHER)
            } else if (weather != null) {
                weather.query = location_query
            }

            if (wEx != null) throw wEx

            return@withContext weather!!
        }

    // NOTE: Uri format
    // https://api.meteomatics.com/validdatetime/parameters/locations/format?optionals
    private fun createCurrentRequestUri(location_query: String): Uri {
        return Uri.parse(BASE_QUERY_URL).buildUpon()
            .appendPath("now") // time interval
            .appendPath(
                listOf(
                    "t_2m:C",
                    "t_max_2m_1h:C",
                    "t_min_2m_1h:C",
                    "t_apparent:C",
                    "relative_humidity_2m:p",
                    "dew_point_2m:C",
                    "msl_pressure:hPa",
                    "wind_speed_2m:ms",
                    "wind_dir_2m:d",
                    "wind_gusts_2m_1h:ms",
                    "total_cloud_cover:p",
                    "prob_precip_1h:p",
                    "precip_1h:mm",
                    "precip_type_1h:idx",
                    "visibility:km",
                    "birch_pollen:grainsm3",
                    "grass_pollen:grainsm3",
                    "ragweed_pollen:grainsm3",
                    "moon_phase:idx",
                    "moonrise:sql",
                    "moonset:sql",
                    "sunrise:sql",
                    "sunset:sql",
                    "uv:idx",
                    "is_night:idx",
                    "weather_symbol_1h:idx",
                    "weather_code_1h:idx"
                ).joinToString(separator = ",")
            ) // parameters
            .appendPath(location_query) // location
            .appendPath("json") // format
            .build()
    }

    private fun createDailyRequestUri(location_query: String): Uri {
        return Uri.parse(BASE_QUERY_URL).buildUpon()
            .appendPath("todayT00:00:00ZP10D:P1D") // time interval
            .appendPath(
                listOf(
                    "t_max_2m_24h:C",
                    "t_min_2m_24h:C",
                    "t_apparent:C",
                    "relative_humidity_mean_2m_24h:p",
                    "dew_point_mean_2m_24h:C",
                    "msl_pressure:hPa",
                    "wind_speed_mean_10m_24h:ms",
                    "wind_dir_mean_10m_24h:d",
                    "wind_gusts_10m_24h:ms",
                    "total_cloud_cover:p",
                    "prob_precip_24h:p",
                    "precip_24h:mm",
                    "precip_type_24h:idx",
                    "uv_max_24h:idx",
                    "is_night:idx",
                    "weather_symbol_24h:idx",
                    "weather_code_24h:idx"
                ).joinToString(separator = ",")
            ) // parameters
            .appendPath(location_query) // location
            .appendPath("json") // format
            .build()
    }

    private fun createHourlyRequestUri(date: ZonedDateTime, location_query: String): Uri {
        val requestDate = date.truncatedTo(ChronoUnit.HOURS).format(DateTimeFormatter.ISO_INSTANT)

        return Uri.parse(BASE_QUERY_URL).buildUpon()
            .appendPath("${requestDate}P2D:PT1H") // time interval
            .appendPath(
                listOf(
                    "t_2m:C",
                    "t_apparent:C",
                    "relative_humidity_mean_2m_1h:p",
                    "dew_point_mean_2m_1h:C",
                    "msl_pressure:hPa",
                    "wind_speed_mean_10m_1h:ms",
                    "wind_dir_mean_10m_1h:d",
                    "wind_gusts_10m_1h:ms",
                    "total_cloud_cover:p",
                    "prob_precip_1h:p",
                    "precip_1h:mm",
                    "precip_type_1h:idx",
                    "uv_max_1h:idx",
                    "is_night:idx",
                    "weather_symbol_1h:idx",
                    "weather_code_1h:idx"
                ).joinToString(separator = ",")
            ) // parameters
            .appendPath(location_query) // location
            .appendPath("json") // format
            .build()
    }

    private fun createMinutelyRequestUri(date: ZonedDateTime, location_query: String): Uri {
        val requestDate = date.truncatedTo(ChronoUnit.HOURS).format(DateTimeFormatter.ISO_INSTANT)

        return Uri.parse(BASE_QUERY_URL).buildUpon()
            .appendPath("${requestDate}PT12H:PT5M") // time interval
            .appendPath("precip_5min:mm") // parameters
            .appendPath(location_query) // location
            .appendPath("json") // format
            .build()
    }

    override suspend fun updateWeatherData(location: LocationData, weather: Weather) {
        val offset = location.tzOffset
        weather.updateTime = weather.updateTime.withZoneSameInstant(offset)
        weather.condition.observationTime =
            weather.condition.observationTime.withZoneSameInstant(offset)

        // The time of day is set to max if the sun never sets/rises and
        // DateTime is set to min if not found
        // Don't change this if its set that way
        if (weather.astronomy.sunrise.isAfter(LocalDateTime.MIN) &&
            weather.astronomy.sunrise.toLocalTime().isBefore(LocalTime.MAX)
        ) weather.astronomy.sunrise =
            weather.astronomy.sunrise.plusSeconds(offset.totalSeconds.toLong())
        if (weather.astronomy.sunset.isAfter(LocalDateTime.MIN) &&
            weather.astronomy.sunset.toLocalTime().isBefore(LocalTime.MAX)
        ) weather.astronomy.sunset =
            weather.astronomy.sunset.plusSeconds(offset.totalSeconds.toLong())
        if (weather.astronomy.moonrise.isAfter(LocalDateTime.MIN) &&
            weather.astronomy.moonrise.toLocalTime().isBefore(LocalTime.MAX)
        ) weather.astronomy.moonrise =
            weather.astronomy.moonrise.plusSeconds(offset.totalSeconds.toLong())
        if (weather.astronomy.moonset.isAfter(LocalDateTime.MIN) &&
            weather.astronomy.moonset.toLocalTime().isBefore(LocalTime.MAX)
        ) weather.astronomy.moonset =
            weather.astronomy.moonset.plusSeconds(offset.totalSeconds.toLong())

        for (forecast in weather.forecast) {
            forecast.date = forecast.date.plusSeconds(offset.totalSeconds.toLong())
        }

        for (hr_forecast in weather.hrForecast) {
            hr_forecast.date = hr_forecast.date.withZoneSameInstant(offset)
        }

        if (!weather.minForecast.isNullOrEmpty()) {
            for (min_forecast in weather.minForecast) {
                min_forecast.date = min_forecast.date.withZoneSameInstant(offset)
            }
        }
    }

    override fun updateLocationQuery(weather: Weather): String {
        val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
        df.applyPattern("0.####")
        return String.format(
            Locale.ROOT,
            "%s,%s",
            df.format(weather.location.latitude),
            df.format(weather.location.longitude)
        )
    }

    override fun updateLocationQuery(location: LocationData): String {
        val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
        df.applyPattern("0.####")
        return String.format(
            Locale.ROOT,
            "%s,%s",
            df.format(location.latitude),
            df.format(location.longitude)
        )
    }

    override fun getWeatherIcon(icon: String?): String {
        return getWeatherIcon(false, icon)
    }

    override fun getWeatherIcon(isNight: Boolean, icon: String?): String {
        val symbolId = icon?.toIntOrNull() ?: return WeatherIcons.NA

        return when (symbolId) {
            1 -> WeatherIcons.DAY_SUNNY
            101 -> WeatherIcons.NIGHT_CLEAR
            2, 3 -> WeatherIcons.DAY_PARTLY_CLOUDY
            102, 103 -> WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY
            4, 104 -> WeatherIcons.CLOUDY
            5, 105 -> WeatherIcons.RAIN
            6, 106 -> WeatherIcons.SLEET
            7, 107 -> WeatherIcons.SNOW
            8 -> WeatherIcons.DAY_SHOWERS
            108 -> WeatherIcons.NIGHT_ALT_SHOWERS
            9 -> WeatherIcons.DAY_SNOW
            109 -> WeatherIcons.NIGHT_ALT_SNOW
            10 -> WeatherIcons.DAY_SLEET
            110 -> WeatherIcons.NIGHT_ALT_SLEET
            11 -> WeatherIcons.DAY_FOG
            111 -> WeatherIcons.NIGHT_FOG
            12, 112 -> WeatherIcons.FOG
            13, 113 -> WeatherIcons.RAIN_MIX
            14, 114 -> WeatherIcons.THUNDERSTORM
            15, 115 -> WeatherIcons.SPRINKLE
            16, 116 -> WeatherIcons.SANDSTORM
            else -> WeatherIcons.NA
        }
    }

    override fun getWeatherCondition(icon: String?): String {
        val symbolId = icon?.toIntOrNull() ?: return super.getWeatherCondition(icon)

        return when (symbolId) {
            1, 101 -> context.getString(R.string.weather_clearsky)
            2, 102, 3, 103 -> context.getString(R.string.weather_partlycloudy)
            4, 104 -> context.getString(R.string.weather_cloudy)
            5, 105 -> context.getString(R.string.weather_rain)
            6, 106 -> context.getString(R.string.weather_rainandsnow)
            7, 107 -> context.getString(R.string.weather_snow)
            8, 108 -> context.getString(R.string.weather_rainshowers)
            9, 109 -> context.getString(R.string.weather_lightsnowshowers)
            10, 110 -> context.getString(R.string.weather_sleet)
            11, 111 -> context.getString(R.string.weather_lightfog)
            12, 112 -> context.getString(R.string.weather_foggy)
            13, 113 -> context.getString(R.string.weather_freezingrain)
            14, 114 -> context.getString(R.string.weather_tstorms)
            15, 115 -> context.getString(R.string.weather_drizzle)
            16, 116 -> context.getString(R.string.weather_dust)
            else -> context.getString(R.string.weather_notavailable)
        }
    }

    override fun isNight(weather: Weather): Boolean {
        var isNight = super.isNight(weather)

        when (weather.condition.icon) {
            // The following cases can be present at any time of day
            WeatherIcons.CLOUDY,
            WeatherIcons.RAIN,
            WeatherIcons.SLEET,
            WeatherIcons.SNOW,
            WeatherIcons.FOG,
            WeatherIcons.RAIN_MIX,
            WeatherIcons.THUNDERSTORM,
            WeatherIcons.SPRINKLE,
            WeatherIcons.SANDSTORM -> {
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