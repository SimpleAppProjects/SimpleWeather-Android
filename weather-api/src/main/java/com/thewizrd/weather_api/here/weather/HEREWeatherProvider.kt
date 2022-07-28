package com.thewizrd.weather_api.here.weather

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
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.WeatherAlertProvider
import com.thewizrd.shared_resources.weatherdata.auth.AuthType
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlert
import com.thewizrd.shared_resources.weatherdata.model.isNullOrInvalid
import com.thewizrd.weather_api.extras.cacheRequestIfNeeded
import com.thewizrd.weather_api.google.location.getGoogleLocationProvider
import com.thewizrd.weather_api.here.auth.hereOAuthService
import com.thewizrd.weather_api.smc.SunMoonCalcProvider
import com.thewizrd.weather_api.utils.APIRequestUtils.checkForErrors
import com.thewizrd.weather_api.utils.APIRequestUtils.checkRateLimit
import com.thewizrd.weather_api.weatherModule
import com.thewizrd.weather_api.weatherdata.WeatherProviderImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly
import java.io.IOException
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.TimeUnit

class HEREWeatherProvider : WeatherProviderImpl(), WeatherAlertProvider {
    companion object {
        private const val WEATHER_GLOBAL_QUERY_URL =
            "https://weather.ls.hereapi.com/weather/1.0/report.json?" +
                    "product=alerts&product=forecast_7days_simple&product=forecast_hourly&product=forecast_astronomy&product=observation&oneobservation=true" +
                    "&%s&language=%s&metric=false"
        private const val WEATHER_US_CA_QUERY_URL =
            "https://weather.ls.hereapi.com/weather/1.0/report.json?" +
                    "product=nws_alerts&product=forecast_7days_simple&product=forecast_hourly&product=forecast_astronomy&product=observation&oneobservation=true" +
                    "&%s&language=%s&metric=false"
        private const val ALERT_GLOBAL_QUERY_URL =
            "https://weather.ls.hereapi.com/weather/1.0/report.json?product=alerts&%s&language=%s&metric=false"
        private const val ALERT_US_CA_QUERY_URL =
            "https://weather.ls.hereapi.com/weather/1.0/report.json?product=nws_alerts&%s&language=%s&metric=false"
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
        return WeatherAPI.HERE
    }

    override fun supportsWeatherLocale(): Boolean {
        return true
    }

    override fun isKeyRequired(): Boolean {
        return false
    }

    override fun needsExternalAlertData(): Boolean {
        return false
    }

    override suspend fun isKeyValid(key: String?): Boolean {
        return false
    }

    override fun getAPIKey(): String? {
        return null
    }

    override fun getAuthType(): AuthType {
        return AuthType.INTERNAL // or AppID/AppCode
    }

    @Throws(WeatherException::class)
    override suspend fun getWeather(location_query: String, country_code: String): Weather =
        withContext(Dispatchers.IO) {
            var weather: Weather?

            val uLocale = ULocale.forLocale(LocaleUtils.getLocale())
            val locale = localeToLangCode(uLocale.language, uLocale.toLanguageTag())

            val client = sharedDeps.httpClient
                var response: Response? = null
                var wEx: WeatherException? = null

                try {
                    // If were under rate limit, deny request
                    checkRateLimit()

                    val authorization = hereOAuthService.getBearerToken(false)

                    if (authorization.isNullOrBlank()) {
                        throw WeatherException(ErrorStatus.NETWORKERROR)
                    }

                    val url = if (LocationUtils.isUSorCanada(country_code)) {
                        String.format(WEATHER_US_CA_QUERY_URL, location_query, locale)
                    } else {
                        String.format(WEATHER_GLOBAL_QUERY_URL, location_query, locale)
                    }

                    val request = Request.Builder()
                        .cacheRequestIfNeeded(isKeyRequired(), 1, TimeUnit.HOURS)
                        .url(url)
                        .addHeader("Authorization", authorization)
                        .build()

                    // Connect to webstream
                    response = client.newCall(request).await()
                    checkForErrors(response)

                    val stream = response.getStream()

                    // Load weather
                    val root: Rootobject? = JSONParser.deserializer(stream, Rootobject::class.java)

                    // Check for errors
                    when (root?.type) {
                        "Invalid Request" -> throw WeatherException(ErrorStatus.QUERYNOTFOUND)
                        "Unauthorized" -> throw WeatherException(ErrorStatus.INVALIDAPIKEY)
                    }

                    // End Stream
                    stream.closeQuietly()

                    requireNotNull(root)

                    weather = createWeatherData(root)

                    // Add weather alerts if available
                    weather.weatherAlerts = createWeatherAlerts(
                        root,
                        weather.location.latitude, weather.location.longitude
                    )
                } catch (ex: Exception) {
                    weather = null
                    if (ex is IOException) {
                        wEx = WeatherException(ErrorStatus.NETWORKERROR)
                    } else if (ex is WeatherException) {
                        wEx = ex
                    }
                    Logger.writeLine(Log.ERROR, ex, "HEREWeatherProvider: error getting weather data")
                } finally {
                    response?.close()
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
        val offset = location.tzOffset

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

        // Update tz for weather properties
        weather.updateTime = weather.updateTime.withZoneSameInstant(location.tzOffset)
        weather.condition.observationTime = weather.condition.observationTime.withZoneSameInstant(location.tzOffset)

        val old = weather.astronomy
        if (old.moonset.isEqual(DateTimeUtils.LOCALDATETIME_MIN) || old.moonrise.isEqual(
                DateTimeUtils.LOCALDATETIME_MIN
            )
        ) {
            runCatching {
                val newAstro = SunMoonCalcProvider().getAstronomyData(
                    location,
                    weather.condition.observationTime
                )
                newAstro.sunrise = old.sunrise
                newAstro.sunset = old.sunset
                weather.astronomy = newAstro
            }.onFailure {
                Logger.writeLine(Log.ERROR, it)
            }
        }

        for (forecast in weather.forecast) {
            forecast.date = forecast.date.plusSeconds(offset.totalSeconds.toLong())
        }
    }

    override suspend fun getAlerts(location: LocationData): Collection<WeatherAlert> {
        var alerts: Collection<WeatherAlert>? = null

        val uLocale = ULocale.forLocale(LocaleUtils.getLocale())
        val locale = localeToLangCode(uLocale.language, uLocale.toLanguageTag())

        val client = sharedDeps.httpClient
        var response: Response? = null

        try {
            // If were under rate limit, deny request
            checkRateLimit()

            val authorization = hereOAuthService.getBearerToken(false)

            if (authorization.isNullOrBlank()) {
                throw WeatherException(ErrorStatus.NETWORKERROR)
            }

            val country_code = location.countryCode
            val url = if (LocationUtils.isUSorCanada(country_code)) {
                String.format(ALERT_US_CA_QUERY_URL, location.query, locale)
            } else {
                String.format(ALERT_GLOBAL_QUERY_URL, location.query, locale)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", authorization)
                .cacheRequestIfNeeded(isKeyRequired(), 1, TimeUnit.HOURS)
                .build()

            // Connect to webstream
            response = client.newCall(request).await()
            checkForErrors(response)

            val stream = response.getStream()

            // Load data
            val root = withContext(Dispatchers.Default) {
                JSONParser.deserializer<Rootobject>(stream, Rootobject::class.java)
            }

            // End Stream
            stream.closeQuietly()

            requireNotNull(root)

            // Add weather alerts if available
            alerts = createWeatherAlerts(
                root,
                location.latitude.toFloat(), location.longitude.toFloat()
            )
        } catch (ex: Exception) {
            Logger.writeLine(Log.ERROR, ex, "HEREWeatherProvider: error getting weather alert data")
        } finally {
            response?.close()
        }

        if (alerts == null) alerts = emptyList()

        return alerts
    }

    override fun updateLocationQuery(weather: Weather): String {
        val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
        df.applyPattern("0.####")
        return String.format(Locale.ROOT, "latitude=%s&longitude=%s", df.format(weather.location.latitude), df.format(weather.location.longitude))
    }

    override fun updateLocationQuery(location: LocationData): String {
        val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
        df.applyPattern("0.####")
        return String.format(Locale.ROOT, "latitude=%s&longitude=%s", df.format(location.latitude), df.format(location.longitude))
    }

    override fun localeToLangCode(iso: String, name: String): String {
        return name
    }

    override fun getWeatherIcon(icon: String?): String {
        var isNight = false

        if (icon == null)
            return WeatherIcons.NA

        if (icon.contains("night_"))
            isNight = true

        return getWeatherIcon(isNight, icon)
    }

    override fun getWeatherIcon(isNight: Boolean, icon: String?): String {
        if (icon == null) return WeatherIcons.NA

        val neutralIcon = if (icon.startsWith("night_")) {
            icon.replaceFirst("night_", "")
        } else {
            icon
        }

        var weatherIcon = when (neutralIcon) {
            "sunny", "clear" -> {
                if (isNight) {
                    WeatherIcons.NIGHT_CLEAR
                } else {
                    WeatherIcons.DAY_SUNNY
                }
            }
            "mostly_sunny", "passing_clounds", "passing_clouds", "more_sun_than_clouds",
            "mostly_clear", "scattered_clouds", "partly_cloudy", "decreasing_cloudiness",
            "clearing_skies" -> {
                if (isNight) {
                    WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY
                } else {
                    WeatherIcons.DAY_PARTLY_CLOUDY
                }
            }
            "a_mixture_of_sun_and_clouds", "increasing_cloudiness", "breaks_of_sun_late",
            "afternoon_clouds", "morning_clouds", "partly_sunny", "more_clouds_than_sun",
            "broken_clouds", "mostly_cloudy" -> {
                if (isNight) {
                    WeatherIcons.NIGHT_ALT_CLOUDY
                } else {
                    WeatherIcons.DAY_CLOUDY
                }
            }
            "high_level_clouds", "high_clouds" -> {
                if (isNight) {
                    WeatherIcons.NIGHT_ALT_CLOUDY_HIGH
                } else {
                    WeatherIcons.DAY_CLOUDY_HIGH
                }
            }
            "rain_early", "rain", "rain_late" -> {
                WeatherIcons.RAIN
            }
            "strong_thunderstorms", "severe_thunderstorms", "thunderstorms", "tstorms_early",
            "isolated_tstorms_late", "tstorms", "tstorms_late" -> {
                WeatherIcons.THUNDERSTORM
            }
            "widely_scattered_tstorms", "isolated_tstorms", "a_few_tstorms",
            "scattered_tstorms", "scattered_tstorms_late" -> {
                if (isNight) {
                    WeatherIcons.NIGHT_ALT_THUNDERSTORM
                } else {
                    WeatherIcons.DAY_THUNDERSTORM
                }
            }
            "thundershowers" -> {
                WeatherIcons.STORM_SHOWERS
            }
            "ice_fog" -> {
                WeatherIcons.FOG
            }
            "scattered_showers", "a_few_showers", "light_showers", "passing_showers", "rain_showers",
            "showers", "numerous_showers", "showery", "showers_early", "showers_late" -> {
                if (isNight) {
                    WeatherIcons.NIGHT_ALT_SHOWERS
                } else {
                    WeatherIcons.DAY_SHOWERS
                }
            }
            "hazy_sunshine", "haze", "low_level_haze" -> {
                if (isNight) {
                    WeatherIcons.NIGHT_HAZE
                } else {
                    WeatherIcons.DAY_HAZE
                }
            }
            "smoke" -> {
                WeatherIcons.SMOKE
            }
            "early_fog_followed_by_sunny_skies", "early_fog", "light_fog" -> {
                if (isNight) {
                    WeatherIcons.NIGHT_FOG
                } else {
                    WeatherIcons.DAY_FOG
                }
            }
            "fog", "dense_fog" -> {
                WeatherIcons.FOG
            }
            "cloudy", "low_clouds" -> {
                WeatherIcons.CLOUDY
            }
            "overcast" -> {
                WeatherIcons.OVERCAST
            }
            "hail" -> {
                WeatherIcons.HAIL
            }
            "sleet" -> {
                WeatherIcons.SLEET
            }
            "light_mixture_of_precip", "icy_mix", "mixture_of_precip", "heavy_mixture_of_precip",
            "snow_changing_to_rain", "snow_changing_to_an_icy_mix", "an_icy_mix_changing_to_snow",
            "an_icy_mix_changing_to_rain", "rain_changing_to_snow", "rain_changing_to_an_icy_mix",
            "light_icy_mix_early", "icy_mix_early", "light_icy_mix_late", "icy_mix_late",
            "snow_rain_mix", "light_freezing_rain", "freezing_rain" -> {
                WeatherIcons.RAIN_MIX
            }
            "scattered_flurries", "snow_flurries", "light_snow_showers", "snow_showers", "light_snow",
            "flurries_early", "snow_showers_early", "light_snow_early", "flurries_late", "snow_showers_late",
            "light_snow_late", "snow", "moderate_snow", "snow_early", "snow_late" -> {
                WeatherIcons.SNOW
            }
            "heavy_rain_early", "heavy_rain", "lots_of_rain", "tons_of_rain", "heavy_rain_late",
            "flash_floods", "flood" -> {
                WeatherIcons.RAIN_WIND
            }
            "drizzle", "light_rain", "sprinkles_early", "light_rain_early", "sprinkles_late",
            "light_rain_late", "sprinkles" -> {
                if (isNight) {
                    WeatherIcons.NIGHT_ALT_SPRINKLE
                } else {
                    WeatherIcons.DAY_SPRINKLE
                }
            }
            "heavy_snow", "heavy_snow_early", "heavy_snow_late", "snowstorm", "blizzard" -> {
                WeatherIcons.SNOW_WIND
            }
            "tornado" -> {
                WeatherIcons.TORNADO
            }
            "tropical_storm" -> {
                WeatherIcons.SHOWERS
            }
            "hurricane" -> {
                WeatherIcons.HURRICANE
            }
            "sandstorm" -> {
                WeatherIcons.SANDSTORM
            }
            "duststorm" -> {
                WeatherIcons.DUST
            }
            else -> ""
        }

        // Fallback
        if (weatherIcon.isBlank()) {
            weatherIcon = when {
                icon.contains("overcast") -> {
                    if (isNight) {
                        WeatherIcons.NIGHT_OVERCAST
                    } else {
                        WeatherIcons.DAY_SUNNY_OVERCAST
                    }
                }
                icon.contains("mostly_sunny") || icon.contains("mostly_clear") || icon.contains("partly_cloudy") || icon.contains(
                    "passing_clounds"
                ) || icon.contains("more_sun_than_clouds") || icon.contains("scattered_clouds") || icon.contains(
                    "decreasing_cloudiness"
                ) || icon.contains("clearing_skies") || icon.contains("low_clouds") || icon.contains(
                    "passing_clouds"
                ) -> {
                    if (isNight) {
                        WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY
                    } else {
                        WeatherIcons.DAY_PARTLY_CLOUDY
                    }
                }
                icon.contains("cloudy") || icon.contains("a_mixture_of_sun_and_clouds") || icon.contains(
                    "increasing_cloudiness"
                ) || icon.contains("breaks_of_sun_late") || icon.contains("afternoon_clouds") || icon.contains(
                    "morning_clouds"
                ) || icon.contains("partly_sunny") || icon.contains("more_clouds_than_sun") || icon.contains(
                    "broken_clouds"
                ) -> {
                    if (isNight) {
                        WeatherIcons.NIGHT_ALT_CLOUDY
                    } else {
                        WeatherIcons.DAY_CLOUDY
                    }
                }
                icon.contains("high_level_clouds") || icon.contains("high_clouds") -> {
                    if (isNight) {
                        WeatherIcons.NIGHT_ALT_CLOUDY_HIGH
                    } else {
                        WeatherIcons.DAY_CLOUDY_HIGH
                    }
                }
                icon.contains("snowstorm") || icon.contains("blizzard") -> {
                    WeatherIcons.SNOW_WIND
                }
                icon.contains("fog") -> {
                    WeatherIcons.FOG
                }
                icon.contains("hazy") || icon.contains("haze") -> {
                    if (isNight) {
                        WeatherIcons.NIGHT_HAZE
                    } else {
                        WeatherIcons.DAY_HAZE
                    }
                }
                icon.contains("sleet") || icon.contains("snow_changing_to_an_icy_mix") || icon.contains(
                    "an_icy_mix_changing_to_snow"
                ) || icon.contains("rain_changing_to_snow") -> {
                    WeatherIcons.SLEET
                }
                icon.contains("mixture_of_precip") || icon.contains("icy_mix") || icon.contains("snow_changing_to_rain") || icon.contains(
                    "snow_rain_mix"
                ) || icon.contains("freezing_rain") -> {
                    WeatherIcons.RAIN_MIX
                }
                icon.contains("hail") -> {
                    WeatherIcons.HAIL
                }
                icon.contains("flurries") || icon.contains("snow") -> {
                    WeatherIcons.SNOW
                }
                icon.contains("sprinkles") || icon.contains("drizzle") -> {
                    WeatherIcons.SPRINKLE
                }
                icon.contains("light_rain") || icon.contains("showers") -> {
                    WeatherIcons.SHOWERS
                }
                icon.contains("rain") || icon.contains("flood") -> {
                    WeatherIcons.RAIN
                }
                icon.contains("tstorms") || icon.contains("thunderstorms") || icon.contains("thundershowers") || icon.contains(
                    "tropical_storm"
                ) -> {
                    WeatherIcons.THUNDERSTORM
                }
                icon.contains("smoke") -> {
                    WeatherIcons.SMOKE
                }
                icon.contains("tornado") -> {
                    WeatherIcons.TORNADO
                }
                icon.contains("hurricane") -> {
                    WeatherIcons.HURRICANE
                }
                icon.contains("sandstorm") -> {
                    WeatherIcons.SANDSTORM
                }
                icon.contains("duststorm") -> {
                    WeatherIcons.DUST
                }
                icon.contains("clear") || icon.contains("sunny") -> {
                    if (isNight) {
                        WeatherIcons.NIGHT_CLEAR
                    } else {
                        WeatherIcons.DAY_SUNNY
                    }
                }
                icon.contains("cw_no_report_icon") -> {
                    if (isNight) {
                        WeatherIcons.NIGHT_CLEAR
                    } else {
                        WeatherIcons.DAY_SUNNY
                    }
                }
                else -> {
                    // Not Available
                    WeatherIcons.NA
                }
            }
        }

        return weatherIcon
    }
}