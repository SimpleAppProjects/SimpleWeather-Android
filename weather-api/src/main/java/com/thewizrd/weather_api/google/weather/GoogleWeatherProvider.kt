package com.thewizrd.weather_api.google.weather

import android.util.Log
import androidx.core.net.toUri
import com.ibm.icu.util.ULocale
import com.thewizrd.shared_resources.exceptions.ErrorStatus
import com.thewizrd.shared_resources.exceptions.WeatherException
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.await
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.getStream
import com.thewizrd.shared_resources.remoteconfig.remoteConfigService
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.auth.AuthType
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.shared_resources.weatherdata.model.isNullOrInvalid
import com.thewizrd.weather_api.extras.cacheRequestIfNeeded
import com.thewizrd.weather_api.google.location.getGoogleLocationProvider
import com.thewizrd.weather_api.google.utils.addGoogleAuth
import com.thewizrd.weather_api.keys.Keys
import com.thewizrd.weather_api.utils.APIRequestUtils.addUserAgent
import com.thewizrd.weather_api.utils.APIRequestUtils.checkForErrors
import com.thewizrd.weather_api.utils.APIRequestUtils.checkRateLimit
import com.thewizrd.weather_api.utils.APIRequestUtils.throwIfRateLimited
import com.thewizrd.weather_api.weatherModule
import com.thewizrd.weather_api.weatherdata.WeatherProviderImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly
import java.io.IOException
import java.net.HttpURLConnection
import java.text.DecimalFormat
import java.time.LocalTime
import java.util.Locale
import java.util.concurrent.TimeUnit

class GoogleWeatherProvider : WeatherProviderImpl() {
    companion object {
        private const val BASE_URL = "https://weather.googleapis.com/v1/"
        private const val CONDITIONS_PATH = "currentConditions:lookup"
        private const val FORECAST_PATH = "forecast/days:lookup"
        private const val HOURLY_FORECAST_PATH = "forecast/hours:lookup"
        private const val MINUTELY_FORECAST_PATH = "forecast/minutes:lookup"
        private const val PUBLIC_ALERTS_PATH = "publicAlerts:lookup"
    }

    init {
        mLocationProvider = runCatching {
            weatherModule.locationProviderFactory.getLocationProvider(
                remoteConfigService.getLocationProvider(
                    getWeatherAPI()
                )
            )
        }.getOrElse {
            getGoogleLocationProvider()
        }
    }

    override fun getWeatherAPI(): String {
        return WeatherAPI.GOOGLE
    }

    override fun isKeyRequired(): Boolean {
        return true
    }

    override fun supportsWeatherLocale(): Boolean {
        return true
    }

    override fun getHourlyForecastInterval(): Int {
        return 1
    }

    override fun getRetryTime(): Long {
        return 5000
    }

    override fun getAuthType(): AuthType {
        return AuthType.APIKEY
    }

    override fun needsExternalAlertData(): Boolean {
        return false
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

            val requestUri = BASE_URL.toUri().buildUpon()
                .appendEncodedPath(FORECAST_PATH)
                .appendQueryParameter("key", key)
                .build()

            val request = Request.Builder()
                .cacheControl(
                    CacheControl.Builder()
                        .maxAge(1, TimeUnit.DAYS)
                        .build()
                )
                .url(requestUri.toString())
                .addUserAgent(context)
                .addGoogleAuth(context)
                .build()

            // Connect to webstream
            response = client.newCall(request).await()
            throwIfRateLimited(response)

            when (response.code) {
                HttpURLConnection.HTTP_BAD_REQUEST -> isValid = true
                HttpURLConnection.HTTP_UNAUTHORIZED, HttpURLConnection.HTTP_FORBIDDEN -> {
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
        return Keys.getGWeatherKey()
    }

    override suspend fun getWeatherData(location: LocationData): Weather =
        withContext(Dispatchers.IO) {
            var weather: Weather?

            val uLocale = ULocale.forLocale(LocaleUtils.getLocale())
            val locale = localeToLangCode(uLocale.language, uLocale.toLanguageTag())
            val query = updateLocationQuery(location)

            val key = getProviderKey()

            val client = sharedDeps.httpClient
            var conditionResponse: Response? = null
            var wEx: WeatherException? = null

            try {
                // If were under rate limit, deny request
                checkRateLimit()

                if (key.isNullOrBlank()) {
                    throw WeatherException(ErrorStatus.INVALIDAPIKEY)
                }

                val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
                df.applyPattern("0.####")

                val conditionRequestUri = BASE_URL.toUri().buildUpon()
                    .appendEncodedPath(CONDITIONS_PATH)
                    .appendQueryParameter("location.latitude", df.format(location.latitude))
                    .appendQueryParameter("location.longitude", df.format(location.longitude))
                    .appendQueryParameter("units_system", "METRIC")
                    .appendQueryParameter("languageCode", locale)
                    .appendQueryParameter("key", key)
                    .build()

                val conditionRequest = Request.Builder()
                    .cacheRequestIfNeeded(isKeyRequired(), 15, TimeUnit.MINUTES)
                    .url(conditionRequestUri.toString())
                    .addUserAgent(context)
                    .addGoogleAuth(context)
                    .build()

                val dailyRequestUri = BASE_URL.toUri().buildUpon()
                    .appendEncodedPath(FORECAST_PATH)
                    .appendQueryParameter("location.latitude", df.format(location.latitude))
                    .appendQueryParameter("location.longitude", df.format(location.longitude))
                    .appendQueryParameter("units_system", "METRIC")
                    .appendQueryParameter("languageCode", locale)
                    .appendQueryParameter("key", key)
                    .build()

                val dailyRequest = Request.Builder()
                    .cacheRequestIfNeeded(isKeyRequired(), 1, TimeUnit.HOURS)
                    .url(dailyRequestUri.toString())
                    .addUserAgent(context)
                    .addGoogleAuth(context)
                    .build()

                val hourlyRequestUri = BASE_URL.toUri().buildUpon()
                    .appendEncodedPath(HOURLY_FORECAST_PATH)
                    .appendQueryParameter("location.latitude", df.format(location.latitude))
                    .appendQueryParameter("location.longitude", df.format(location.longitude))
                    .appendQueryParameter("units_system", "METRIC")
                    .appendQueryParameter("languageCode", locale)
                    .appendQueryParameter("key", key)
                    .build()

                val hourlyRequest = Request.Builder()
                    .cacheRequestIfNeeded(isKeyRequired(), 1, TimeUnit.HOURS)
                    .url(hourlyRequestUri.toString())
                    .addUserAgent(context)
                    .addGoogleAuth(context)
                    .build()

                val minutelyRequestUri = BASE_URL.toUri().buildUpon()
                    .appendEncodedPath(MINUTELY_FORECAST_PATH)
                    .appendQueryParameter("location.latitude", df.format(location.latitude))
                    .appendQueryParameter("location.longitude", df.format(location.longitude))
                    .appendQueryParameter("units_system", "METRIC")
                    .appendQueryParameter("key", key)
                    .build()

                val minutelyRequest = Request.Builder()
                    .cacheRequestIfNeeded(isKeyRequired(), 1, TimeUnit.HOURS)
                    .url(minutelyRequestUri.toString())
                    .addUserAgent(context)
                    .addGoogleAuth(context)
                    .build()

                val alertRequestUri = BASE_URL.toUri().buildUpon()
                    .appendEncodedPath(PUBLIC_ALERTS_PATH)
                    .appendQueryParameter("location.latitude", df.format(location.latitude))
                    .appendQueryParameter("location.longitude", df.format(location.longitude))
                    .appendQueryParameter("languageCode", locale)
                    .appendQueryParameter("key", key)
                    .build()

                val alertRequest = Request.Builder()
                    .cacheRequestIfNeeded(isKeyRequired(), 1, TimeUnit.HOURS)
                    .url(alertRequestUri.toString())
                    .addUserAgent(context)
                    .addGoogleAuth(context)
                    .build()

                // Connect to webstream
                conditionResponse = client.newCall(conditionRequest).await()
                checkForErrors(conditionResponse)
                val conditionData = conditionResponse.use { r ->
                    r.getStream().use { s ->
                        JSONParser.deserializer<CurrentResponse>(s, CurrentResponse::class.java)
                    }
                }
                checkNotNull(conditionData)

                val dailyData = getDailyForecasts(client, dailyRequest)
                val hourlyData = getHourlyForecasts(client, hourlyRequest)
                val minutelyData = getMinutelyForecasts(client, minutelyRequest)
                val alertData = getPublicAlerts(client, alertRequest)

                weather = createWeatherData(
                    conditionData,
                    dailyData,
                    hourlyData,
                    minutely = minutelyData,
                    alerts = alertData
                )
            } catch (ex: Exception) {
                weather = null
                if (ex is IOException) {
                    wEx = WeatherException(ErrorStatus.NETWORKERROR, ex)
                } else if (ex is WeatherException) {
                    wEx = ex
                }
                Logger.writeLine(Log.ERROR, ex, "GoogleWeatherProvider: error getting weather data")
            } finally {
                conditionResponse?.closeQuietly()
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

    private suspend fun getDailyForecasts(
        client: OkHttpClient,
        dailyRequest: Request
    ): DailyResponse {
        val dailyData = DailyResponse()
        val dailyForecasts = mutableListOf<ForecastDaysItem>()

        do {
            val request = if (dailyData.nextPageToken != null) {
                val newUrl = dailyRequest.url.newBuilder()
                    .setQueryParameter("pageToken", dailyData.nextPageToken)
                    .build()

                dailyRequest.newBuilder()
                    .url(newUrl)
                    .build()
            } else {
                dailyRequest
            }

            val dailyResponse = client.newCall(request).await()
            checkForErrors(dailyResponse)
            val dailyResponseData = dailyResponse.use { r ->
                r.getStream().use { s ->
                    JSONParser.deserializer<DailyResponse>(s, DailyResponse::class.java)
                }
            }

            dailyData.timeZone = dailyData.timeZone ?: dailyResponseData?.timeZone
            dailyData.nextPageToken = dailyResponseData?.nextPageToken

            dailyResponseData?.forecastDays?.run { dailyForecasts.addAll(this) }
        } while (!dailyData.nextPageToken.isNullOrEmpty())

        dailyData.forecastDays = dailyForecasts

        return dailyData
    }

    private suspend fun getHourlyForecasts(
        client: OkHttpClient,
        hourlyRequest: Request
    ): HourlyResponse {
        val hourlyData = HourlyResponse()
        val hourlyForecasts = mutableListOf<ForecastHoursItem>()

        do {
            val request = if (hourlyData.nextPageToken != null) {
                val newUrl = hourlyRequest.url.newBuilder()
                    .setQueryParameter("pageToken", hourlyData.nextPageToken)
                    .build()

                hourlyRequest.newBuilder()
                    .url(newUrl)
                    .build()
            } else {
                hourlyRequest
            }

            val hourlyResponse = client.newCall(request).await()
            checkForErrors(hourlyResponse)
            val hourlyResponseData = hourlyResponse.use { r ->
                r.getStream().use { s ->
                    JSONParser.deserializer<HourlyResponse>(s, HourlyResponse::class.java)
                }
            }

            hourlyData.timeZone = hourlyData.timeZone ?: hourlyResponseData?.timeZone
            hourlyData.nextPageToken = hourlyResponseData?.nextPageToken

            hourlyResponseData?.forecastHours?.run { hourlyForecasts.addAll(this) }
        } while (!hourlyData.nextPageToken.isNullOrEmpty())

        hourlyData.forecastHours = hourlyForecasts

        return hourlyData
    }

    private suspend fun getMinutelyForecasts(
        client: OkHttpClient,
        minutelyRequest: Request
    ): MinutelyResponse {
        val minutelyData = MinutelyResponse()
        val minutelySegments = mutableListOf<SegmentsItem>()

        do {
            val request = if (minutelyData.nextPageToken != null) {
                val newUrl = minutelyRequest.url.newBuilder()
                    .setQueryParameter("pageToken", minutelyData.nextPageToken)
                    .build()

                minutelyRequest.newBuilder()
                    .url(newUrl)
                    .build()
            } else {
                minutelyRequest
            }

            val minutelyResponse = client.newCall(request).await()
            checkForErrors(minutelyResponse)
            val minutelyResponseData = minutelyResponse.use { r ->
                r.getStream().use { s ->
                    JSONParser.deserializer<MinutelyResponse>(s, MinutelyResponse::class.java)
                }
            }

            minutelyData.overallPredictionTimeframe = minutelyData.overallPredictionTimeframe
                ?: minutelyResponseData?.overallPredictionTimeframe
            minutelyData.timeZone = minutelyData.timeZone ?: minutelyResponseData?.timeZone
            minutelyData.nextPageToken = minutelyResponseData?.nextPageToken

            minutelyResponseData?.segments?.run { minutelySegments.addAll(this) }
        } while (!minutelyData.nextPageToken.isNullOrEmpty())

        minutelyData.segments = minutelySegments

        return minutelyData
    }

    private suspend fun getPublicAlerts(
        client: OkHttpClient,
        alertsRequest: Request
    ): AlertsResponse {
        val alertsData = AlertsResponse()
        val weatherAlerts = mutableListOf<PublicAlerts>()

        do {
            val request = if (alertsData.nextPageToken != null) {
                val newUrl = alertsRequest.url.newBuilder()
                    .setQueryParameter("pageToken", alertsData.nextPageToken)
                    .build()

                alertsRequest.newBuilder()
                    .url(newUrl)
                    .build()
            } else {
                alertsRequest
            }

            val alertsResponse = client.newCall(request).await()
            checkForErrors(alertsResponse)
            val alertsResponseData = alertsResponse.use { r ->
                r.getStream().use { s ->
                    JSONParser.deserializer<AlertsResponse>(s, AlertsResponse::class.java)
                }
            }

            alertsData.regionCode = alertsData.regionCode ?: alertsResponseData?.regionCode
            alertsData.nextPageToken = alertsResponseData?.nextPageToken

            alertsResponseData?.weatherAlerts?.run { weatherAlerts.addAll(this) }
        } while (!alertsData.nextPageToken.isNullOrEmpty())

        alertsData.weatherAlerts = weatherAlerts

        return alertsData
    }

    override suspend fun updateWeatherData(location: LocationData, weather: Weather) {
        super.updateWeatherData(location, weather)

        // Update forecast, hourly, sunrise/sunset, moonrise/moonset
        val offset = location.tzOffset

        weather.updateTime = weather.updateTime!!.withZoneSameInstant(offset)
        weather.condition!!.observationTime =
            weather.condition!!.observationTime.withZoneSameInstant(offset)

        weather.forecast?.forEach { forecast ->
            forecast.date = forecast.date.plusSeconds(offset.totalSeconds.toLong())
        }
        weather.hrForecast?.forEach { hr_forecast ->
            hr_forecast.date = hr_forecast.date.withZoneSameInstant(offset)
        }
        weather.minForecast?.forEach { min_forecast ->
            min_forecast.date = min_forecast.date.withZoneSameInstant(offset)
        }
        weather.weatherAlerts?.forEach { alert ->
            alert.date = alert.date.withZoneSameInstant(offset)
            alert.expiresDate = alert.expiresDate.withZoneSameInstant(offset)
        }

        if (weather.astronomy != null) {
            // The time of day is set to max if the sun never sets/rises and
            // DateTime is set to min if not found
            // Don't change this if its set that way
            if (weather.astronomy!!.sunrise.isAfter(DateTimeUtils.LOCALDATETIME_MIN) &&
                weather.astronomy!!.sunrise.toLocalTime().isBefore(LocalTime.MAX)
            ) weather.astronomy!!.sunrise =
                weather.astronomy!!.sunrise.plusSeconds(offset.totalSeconds.toLong())
            if (weather.astronomy!!.sunset.isAfter(DateTimeUtils.LOCALDATETIME_MIN) &&
                weather.astronomy!!.sunset.toLocalTime().isBefore(LocalTime.MAX)
            ) weather.astronomy!!.sunset =
                weather.astronomy!!.sunset.plusSeconds(offset.totalSeconds.toLong())
            if (weather.astronomy!!.moonrise.isAfter(DateTimeUtils.LOCALDATETIME_MIN) &&
                weather.astronomy!!.moonrise.toLocalTime().isBefore(LocalTime.MAX)
            ) weather.astronomy!!.moonrise =
                weather.astronomy!!.moonrise.plusSeconds(offset.totalSeconds.toLong())
            if (weather.astronomy!!.moonset.isAfter(DateTimeUtils.LOCALDATETIME_MIN) &&
                weather.astronomy!!.moonset.toLocalTime().isBefore(LocalTime.MAX)
            ) weather.astronomy!!.moonset =
                weather.astronomy!!.moonset.plusSeconds(offset.totalSeconds.toLong())
        }
    }

    override suspend fun updateLocationQuery(weather: Weather): String {
        val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
        df.applyPattern("0.####")
        return String.format(
            Locale.ROOT,
            "location.latitude=%s&location.longitude=%s",
            df.format(weather.location!!.latitude),
            df.format(weather.location!!.longitude)
        )
    }

    override suspend fun updateLocationQuery(location: LocationData): String {
        val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
        df.applyPattern("0.####")
        return String.format(
            Locale.ROOT,
            "location.latitude=%s&location.longitude=%s",
            df.format(location.latitude),
            df.format(location.longitude)
        )
    }

    override fun localeToLangCode(iso: String, name: String): String {
        return name
    }

    override fun getWeatherIcon(icon: String?): String {
        return getWeatherIcon(false, icon)
    }

    override fun getWeatherIcon(isNight: Boolean, icon: String?): String {
        return when (icon) {
            "CLEAR" -> {
                if (isNight) WeatherIcons.NIGHT_CLEAR else WeatherIcons.DAY_SUNNY
            }

            "MOSTLY_CLEAR", "PARTLY_CLOUDY" -> {
                if (isNight) WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY else WeatherIcons.DAY_PARTLY_CLOUDY
            }

            "MOSTLY_CLOUDY" -> {
                if (isNight) WeatherIcons.NIGHT_ALT_CLOUDY else WeatherIcons.DAY_CLOUDY
            }

            "CLOUDY" -> {
                WeatherIcons.CLOUDY
            }

            "WINDY" -> {
                WeatherIcons.WINDY
            }

            "WIND_AND_RAIN", "HEAVY_RAIN_SHOWERS", "MODERATE_TO_HEAVY_RAIN", "HEAVY_RAIN", "RAIN_PERIODICALLY_HEAVY" -> {
                WeatherIcons.RAIN_WIND
            }

            "LIGHT_RAIN_SHOWERS", "CHANCE_OF_SHOWERS", "LIGHT_RAIN" -> {
                WeatherIcons.SPRINKLE
            }

            "SCATTERED_SHOWERS", "RAIN_SHOWERS", "LIGHT_TO_MODERATE_RAIN" -> {
                WeatherIcons.SHOWERS
            }

            "RAIN" -> {
                WeatherIcons.RAIN
            }

            "LIGHT_SNOW_SHOWERS", "CHANCE_OF_SNOW_SHOWERS", "SCATTERED_SNOW_SHOWERS", "SNOW_SHOWERS",
            "LIGHT_TO_MODERATE_SNOW", "MODERATE_TO_HEAVY_SNOW", "SNOW", "LIGHT_SNOW" -> {
                WeatherIcons.SNOW
            }

            "HEAVY_SNOW_SHOWERS", "HEAVY_SNOW", "SNOWSTORM", "SNOW_PERIODICALLY_HEAVY", "HEAVY_SNOW_STORM",
            "BLOWING_SNOW" -> {
                WeatherIcons.SNOW_WIND
            }

            "RAIN_AND_SNOW" -> {
                WeatherIcons.RAIN_MIX
            }

            "HAIL", "HAIL_SHOWERS" -> {
                WeatherIcons.HAIL
            }

            "THUNDERSTORM", "THUNDERSHOWER", "LIGHT_THUNDERSTORM_RAIN", "SCATTERED_THUNDERSTORMS", "HEAVY_THUNDERSTORM" -> {
                WeatherIcons.THUNDERSTORM
            }

            else -> {
                WeatherIcons.NA
            }
        }
    }
}