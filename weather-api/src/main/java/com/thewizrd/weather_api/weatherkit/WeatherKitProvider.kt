package com.thewizrd.weather_api.weatherkit

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
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.utils.ZoneIdCompat
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.auth.AuthType
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.shared_resources.weatherdata.model.isNullOrInvalid
import com.thewizrd.weather_api.R
import com.thewizrd.weather_api.extras.cacheRequestIfNeeded
import com.thewizrd.weather_api.google.location.getGoogleLocationProvider
import com.thewizrd.weather_api.nws.SolCalcAstroProvider
import com.thewizrd.weather_api.smc.SunMoonCalcProvider
import com.thewizrd.weather_api.utils.APIRequestUtils.checkForErrors
import com.thewizrd.weather_api.utils.APIRequestUtils.checkRateLimit
import com.thewizrd.weather_api.utils.logMissingIcon
import com.thewizrd.weather_api.weatherModule
import com.thewizrd.weather_api.weatherdata.WeatherProviderImpl
import com.thewizrd.weather_api.weatherkit.auth.weatherKitJwtService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly
import java.io.IOException
import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.TimeUnit

class WeatherKitProvider : WeatherProviderImpl() {
    companion object {
        private const val BASE_URL = "https://weatherkit.apple.com/api/v1/"
        private const val AVAILABILITY_QUERY_URL = BASE_URL + "availability/%s?country=%s"
        private const val WEATHER_QUERY_URL =
            BASE_URL + "weather/%s/%s?countryCode=%s&dataSets=currentWeather,forecastDaily,forecastHourly,forecastNextHour,weatherAlerts&timezone=%s"
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
        return WeatherAPI.APPLE
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
        return false
    }

    override fun getAuthType(): AuthType {
        return AuthType.INTERNAL
    }

    override fun getHourlyForecastInterval(): Int {
        return 1
    }

    override suspend fun isKeyValid(key: String?): Boolean {
        return false
    }

    override fun getAPIKey(): String? {
        return null
    }

    @Throws(WeatherException::class)
    override suspend fun getWeather(location_query: String, country_code: String): Weather =
        withContext(Dispatchers.IO) {
            var weather: Weather?
            var wEx: WeatherException? = null

            val uLocale = ULocale.forLocale(LocaleUtils.getLocale())
            val locale = localeToLangCode(uLocale.language, uLocale.toLanguageTag())

            val client = sharedDeps.httpClient
            var response: Response? = null

            try {
                // If were under rate limit, deny request
                checkRateLimit()

                val authorization = weatherKitJwtService.getBearerToken(false)

                if (authorization.isNullOrBlank()) {
                    throw WeatherException(ErrorStatus.NETWORKERROR).apply {
                        initCause(Exception("Invalid bearer token: $authorization"))
                    }
                }

                val requestUri = Uri.parse(BASE_URL).buildUpon()
                    .appendPath("weather")
                    .appendPath(locale)
                    .appendPath(location_query)
                    .appendQueryParameter("countryCode", country_code)
                    .appendQueryParameter(
                        "dataSets",
                        "currentWeather,forecastDaily,forecastHourly,forecastNextHour,weatherAlerts"
                    )
                    .appendQueryParameter("timezone", "UTC")
                    .build()

                val request = Request.Builder()
                    .cacheRequestIfNeeded(isKeyRequired(), 60, TimeUnit.MINUTES)
                    .url(requestUri.toString())
                    .addHeader("Authorization", "Bearer $authorization")
                    .build()

                // Connect to webstream
                response = client.newCall(request).await()
                checkForErrors(response)

                val stream = response.getStream()

                // Load weather
                val root = JSONParser.deserializer<com.thewizrd.weather_api.weatherkit.Weather>(
                    stream,
                    com.thewizrd.weather_api.weatherkit.Weather::class.java
                )

                // End Stream
                stream.closeQuietly()

                requireNotNull(root)

                weather = createWeatherData(root)
            } catch (ex: Exception) {
                weather = null
                if (ex is IOException) {
                    wEx = WeatherException(ErrorStatus.NETWORKERROR, ex)
                } else if (ex is WeatherException) {
                    wEx = ex
                }
                Logger.writeLine(Log.ERROR, ex, "WeatherKitProvider: error getting weather data")
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
        val offset = location.tzOffset

        // Update tz for weather properties
        weather.updateTime = weather.updateTime.withZoneSameInstant(offset)
        weather.condition.observationTime =
            weather.condition.observationTime.withZoneSameInstant(offset)

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
        if (!weather.weatherAlerts.isNullOrEmpty()) {
            for (alert in weather.weatherAlerts) {
                alert.date = alert.date.withZoneSameInstant(offset)
                alert.expiresDate = alert.expiresDate.withZoneSameInstant(offset)
            }
        }

        if (weather.astronomy != null) {
            // The time of day is set to max if the sun never sets/rises and
            // DateTime is set to min if not found
            // Don't change this if its set that way
            if (weather.astronomy.sunrise.isAfter(DateTimeUtils.LOCALDATETIME_MIN) &&
                weather.astronomy.sunrise.toLocalTime().isBefore(LocalTime.MAX)
            ) weather.astronomy.sunrise =
                weather.astronomy.sunrise.plusSeconds(offset.totalSeconds.toLong())
            if (weather.astronomy.sunset.isAfter(DateTimeUtils.LOCALDATETIME_MIN) &&
                weather.astronomy.sunset.toLocalTime().isBefore(LocalTime.MAX)
            ) weather.astronomy.sunset =
                weather.astronomy.sunset.plusSeconds(offset.totalSeconds.toLong())
            if (weather.astronomy.moonrise.isAfter(DateTimeUtils.LOCALDATETIME_MIN) &&
                weather.astronomy.moonrise.toLocalTime().isBefore(LocalTime.MAX)
            ) weather.astronomy.moonrise =
                weather.astronomy.moonrise.plusSeconds(offset.totalSeconds.toLong())
            if (weather.astronomy.moonset.isAfter(DateTimeUtils.LOCALDATETIME_MIN) &&
                weather.astronomy.moonset.toLocalTime().isBefore(LocalTime.MAX)
            ) weather.astronomy.moonset =
                weather.astronomy.moonset.plusSeconds(offset.totalSeconds.toLong())
        } else {
            weather.astronomy = try {
                SunMoonCalcProvider().getAstronomyData(location, weather.condition.observationTime)
            } catch (e: WeatherException) {
                Logger.writeLine(Log.ERROR, e)
                SolCalcAstroProvider().getAstronomyData(location, weather.condition.observationTime)
            }
        }
    }

    override fun updateLocationQuery(weather: Weather): String {
        val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
        df.applyPattern("0.####")
        return String.format(
            Locale.ROOT,
            "%s/%s",
            df.format(weather.location.latitude),
            df.format(weather.location.longitude)
        )
    }

    override fun updateLocationQuery(location: LocationData): String {
        val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
        df.applyPattern("0.####")
        return String.format(
            Locale.ROOT,
            "%s/%s",
            df.format(location.latitude),
            df.format(location.longitude)
        )
    }

    override fun localeToLangCode(iso: String, name: String): String {
        val code = when (iso) {
            // Chinese
            "zh" -> when (name) {
                // Chinese - Traditional
                "zh-TW" -> "zh_TW"
                "zh-Hant", "zh-HK", "zh-MO" -> "zh_HK"
                // Mandarin
                "zh-cmn" -> "zh_cmn"
                // Wu
                "zh-wuu" -> "zh_wuu"
                // Xiang
                "zh-hsn" -> "zh_hsn"
                // Cantonese
                "zh-yue" -> "zh_yue"
                // Chinese - Simplified
                else -> "zh"
            }

            else -> name
        }

        return code
    }

    override fun getWeatherIcon(icon: String?): String {
        return getWeatherIcon(false, icon)
    }

    override fun getWeatherIcon(isNight: Boolean, icon: String?): String {
        var weatherIcon: String = when (icon) {
            "BlowingDust" -> WeatherIcons.DUST
            "Clear" -> if (isNight) WeatherIcons.NIGHT_CLEAR else WeatherIcons.DAY_SUNNY
            "Cloudy" -> WeatherIcons.CLOUDY
            "Foggy" -> WeatherIcons.FOG
            "Haze" -> WeatherIcons.HAZE
            "MostlyClear", "PartlyCloudy" -> {
                if (isNight) WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY else WeatherIcons.DAY_PARTLY_CLOUDY
            }

            "MostlyCloudy" -> {
                if (isNight) WeatherIcons.NIGHT_ALT_CLOUDY else WeatherIcons.DAY_CLOUDY
            }

            "Smoky" -> WeatherIcons.SMOKE

            "Breezy" -> if (isNight) WeatherIcons.NIGHT_LIGHT_WIND else WeatherIcons.DAY_LIGHT_WIND
            "Windy" -> if (isNight) WeatherIcons.NIGHT_WINDY else WeatherIcons.DAY_WINDY

            "Drizzle" -> WeatherIcons.SPRINKLE
            "HeavyRain" -> WeatherIcons.RAIN_WIND
            "IsolatedThunderstorms", "ScatteredThunderstorms", "Thunderstorms" -> WeatherIcons.THUNDERSTORM
            "Rain" -> WeatherIcons.RAIN
            "SunShowers" -> if (isNight) WeatherIcons.NIGHT_ALT_RAIN else WeatherIcons.DAY_RAIN
            "StrongStorms" -> WeatherIcons.STORM_SHOWERS

            "Frigid" -> WeatherIcons.SNOWFLAKE_COLD
            "Hail" -> WeatherIcons.HAIL
            "Hot" -> WeatherIcons.HOT

            "Flurries", "Snow" -> WeatherIcons.SNOW
            "Sleet" -> WeatherIcons.SLEET
            "SunFlurries" -> if (isNight) WeatherIcons.NIGHT_ALT_SNOW else WeatherIcons.DAY_SNOW
            "WintryMix" -> WeatherIcons.RAIN_MIX

            "Blizzard", "BlowingSnow", "HeavySnow" -> WeatherIcons.SNOW_WIND
            "FreezingDrizzle", "FreezingRain" -> WeatherIcons.RAIN_MIX

            "Hurricane", "TropicalStorm" -> WeatherIcons.HURRICANE

            else -> ""
        }

        if (weatherIcon.isBlank()) {
            // Not Available
            logMissingIcon(icon)
            weatherIcon = WeatherIcons.NA
        }
        return weatherIcon
    }

    override fun getWeatherCondition(icon: String?): String {
        return when (icon) {
            "BlowingDust" -> context.getString(R.string.weather_dust)
            "Clear" -> context.getString(R.string.weather_clear)
            "Cloudy" -> context.getString(R.string.weather_cloudy)
            "Foggy" -> context.getString(R.string.weather_foggy)
            "Haze" -> context.getString(R.string.weather_haze)
            "MostlyClear" -> context.getString(R.string.weather_mostlyclear)
            "MostlyCloudy" -> context.getString(R.string.weather_mostlycloudy)
            "PartlyCloudy" -> context.getString(R.string.weather_partlycloudy)
            "Smoky" -> context.getString(R.string.weather_smoky)

            "Breezy" -> context.getString(R.string.weather_lightwind)
            "Windy" -> context.getString(R.string.weather_windy)

            "Drizzle" -> context.getString(R.string.weather_drizzle)
            "HeavyRain" -> context.getString(R.string.weather_heavyrain)
            "IsolatedThunderstorms" -> context.getString(R.string.weather_isotstorms)
            "Rain" -> context.getString(R.string.weather_rain)
            "SunShowers" -> context.getString(R.string.weather_rainshowers)
            "ScatteredThunderstorms" -> context.getString(R.string.weather_scatteredtstorms)
            "StrongStorms" -> context.getString(R.string.weather_severetstorms)
            "Thunderstorms" -> context.getString(R.string.weather_tstorms)

            "Frigid" -> context.getString(R.string.weather_cold)
            "Hail" -> context.getString(R.string.weather_hail)
            "Hot" -> context.getString(R.string.weather_hot)

            "Flurries", "SunFlurries" -> context.getString(R.string.weather_snowflurries)
            "Sleet" -> context.getString(R.string.weather_sleet)
            "Snow" -> context.getString(R.string.weather_snow)
            "WintryMix" -> context.getString(R.string.weather_rainandsnow)

            "Blizzard" -> context.getString(R.string.weather_blizzard)
            "BlowingSnow" -> context.getString(R.string.weather_blowingsnow)
            "FreezingDrizzle", "FreezingRain" -> context.getString(R.string.weather_freezingrain)
            "HeavySnow" -> context.getString(R.string.weather_heavysnow)

            "Hurricane" -> context.getString(R.string.weather_hurricane)
            "TropicalStorm" -> context.getString(R.string.weather_tropicalstorm)

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