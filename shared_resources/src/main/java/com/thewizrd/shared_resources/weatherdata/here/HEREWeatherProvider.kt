package com.thewizrd.shared_resources.weatherdata.here

import android.util.Log
import com.ibm.icu.util.ULocale
import com.thewizrd.shared_resources.SimpleLibrary
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.google.getGoogleLocationProvider
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.await
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.getStream
import com.thewizrd.shared_resources.remoteconfig.RemoteConfig
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.utils.here.HEREOAuthUtils
import com.thewizrd.shared_resources.weatherdata.*
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlert
import com.thewizrd.shared_resources.weatherdata.model.isNullOrInvalid
import com.thewizrd.shared_resources.weatherdata.smc.SunMoonCalcProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly
import java.io.IOException
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.TimeUnit

class HEREWeatherProvider : WeatherProviderImpl(), WeatherAlertProviderInterface {
    companion object {
        private const val WEATHER_GLOBAL_QUERY_URL = "https://weather.ls.hereapi.com/weather/1.0/report.json?" +
                                                     "product=alerts&product=forecast_7days_simple&product=forecast_hourly&product=forecast_astronomy&product=observation&oneobservation=true" +
                                                     "&%s&language=%s&metric=false"
        private const val WEATHER_US_CA_QUERY_URL = "https://weather.ls.hereapi.com/weather/1.0/report.json?" +
                                                    "product=nws_alerts&product=forecast_7days_simple&product=forecast_hourly&product=forecast_astronomy&product=observation&oneobservation=true" +
                                                    "&%s&language=%s&metric=false"
        private const val ALERT_GLOBAL_QUERY_URL = "https://weather.ls.hereapi.com/weather/1.0/report.json?product=alerts&%s&language=%s&metric=false"
        private const val ALERT_US_CA_QUERY_URL = "https://weather.ls.hereapi.com/weather/1.0/report.json?product=nws_alerts&%s&language=%s&metric=false"
    }

    init {
        mLocationProvider = RemoteConfig.getLocationProvider(getWeatherAPI())
                ?: getGoogleLocationProvider()
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

    @Throws(WeatherException::class)
    override suspend fun getWeather(location_query: String, country_code: String): Weather =
            withContext(Dispatchers.IO) {
                var weather: Weather?

                val uLocale = ULocale.forLocale(LocaleUtils.getLocale())
                val locale = localeToLangCode(uLocale.language, uLocale.toLanguageTag())

                val client = SimpleLibrary.instance.httpClient
                var response: Response? = null
                var wEx: WeatherException? = null

                try {
                    val authorization = HEREOAuthUtils.getBearerToken(false)

                    if (authorization.isNullOrBlank()) {
                        throw WeatherException(ErrorStatus.NETWORKERROR)
                    }

                    val url = if (LocationUtils.isUSorCanada(country_code)) {
                        String.format(WEATHER_US_CA_QUERY_URL, location_query, locale)
                    } else {
                        String.format(WEATHER_GLOBAL_QUERY_URL, location_query, locale)
                    }

                    val request = Request.Builder()
                            .cacheControl(CacheControl.Builder()
                                    .maxAge(1, TimeUnit.HOURS)
                                    .build())
                            .url(url)
                            .addHeader("Authorization", authorization)
                            .build()

                    // Connect to webstream
                    response = client.newCall(request).await()
                    val stream = response.getStream()

                    // Load weather
                    val root = JSONParser.deserializer<Rootobject>(stream, Rootobject::class.java)

                    // Check for errors
                    when (root?.type) {
                        "Invalid Request" -> wEx = WeatherException(ErrorStatus.QUERYNOTFOUND)
                        "Unauthorized" -> wEx = WeatherException(ErrorStatus.INVALIDAPIKEY)
                    }

                    // End Stream
                    stream.closeQuietly()

                    weather = createWeatherData(root)

                    // Add weather alerts if available
                    weather.weatherAlerts = createWeatherAlerts(root,
                            weather.location.latitude, weather.location.longitude)
                } catch (ex: Exception) {
                    weather = null
                    if (ex is IOException) {
                        wEx = WeatherException(ErrorStatus.NETWORKERROR)
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
        if (old.moonset.isEqual(DateTimeUtils.getLocalDateTimeMIN()) || old.moonrise.isEqual(DateTimeUtils.getLocalDateTimeMIN())) {
            val newAstro = SunMoonCalcProvider().getAstronomyData(location, weather.condition.observationTime)
            newAstro.sunrise = old.sunrise
            newAstro.sunset = old.sunset
            weather.astronomy = newAstro
        }

        for (forecast in weather.forecast) {
            forecast.date = forecast.date.plusSeconds(offset.totalSeconds.toLong())
        }
    }

    override suspend fun getAlerts(location: LocationData): Collection<WeatherAlert> {
        var alerts: Collection<WeatherAlert>? = null

        val uLocale = ULocale.forLocale(LocaleUtils.getLocale())
        val locale = localeToLangCode(uLocale.language, uLocale.toLanguageTag())

        val client = SimpleLibrary.instance.httpClient
        var response: Response? = null

        try {
            val authorization = HEREOAuthUtils.getBearerToken(false)

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
                    .cacheControl(CacheControl.Builder() // Updates 4x per day
                            .maxAge(6, TimeUnit.HOURS)
                            .build()
                    ).build()

            // Connect to webstream
            response = client.newCall(request).await()
            val stream = response.getStream()

            // Load data
            val root = withContext(Dispatchers.Default) {
                JSONParser.deserializer<Rootobject>(stream, Rootobject::class.java)
            }

            // End Stream
            stream.closeQuietly()

            // Add weather alerts if available
            alerts = createWeatherAlerts(root,
                    location.latitude.toFloat(), location.longitude.toFloat())
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

        if (icon.startsWith("N_") || icon.contains("night_"))
            isNight = true

        return getWeatherIcon(isNight, icon)
    }

    override fun getWeatherIcon(isNight: Boolean, icon: String?): String {
        var weatherIcon = ""

        if (icon == null) return WeatherIcons.NA

        if (icon.contains("mostly_sunny") || icon.contains("mostly_clear") || icon.contains("partly_cloudy")
            || icon.contains("passing_clounds") || icon.contains("more_sun_than_clouds") || icon.contains("scattered_clouds")
            || icon.contains("decreasing_cloudiness") || icon.contains("clearing_skies") || icon.contains("overcast")
            || icon.contains("low_clouds") || icon.contains("passing_clouds")) {
            weatherIcon = if (isNight)
                WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY
            else
                WeatherIcons.DAY_SUNNY_OVERCAST
        } else if (icon.contains("cloudy") || icon.contains("a_mixture_of_sun_and_clouds") || icon.contains("increasing_cloudiness")
                   || icon.contains("breaks_of_sun_late") || icon.contains("afternoon_clouds") || icon.contains("morning_clouds")
                   || icon.contains("partly_sunny") || icon.contains("more_clouds_than_sun") || icon.contains("broken_clouds")) {
            weatherIcon = if (isNight)
                WeatherIcons.NIGHT_ALT_CLOUDY
            else
                WeatherIcons.DAY_CLOUDY
        } else if (icon.contains("high_level_clouds") || icon.contains("high_clouds")) weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_CLOUDY_HIGH else WeatherIcons.DAY_CLOUDY_HIGH else if (icon.contains("flurries") || icon.contains("snowstorm") || icon.contains("blizzard")) weatherIcon = WeatherIcons.SNOW_WIND else if (icon.contains("fog")) weatherIcon = WeatherIcons.FOG else if (icon.contains("hazy") || icon.contains("haze")) weatherIcon = if (isNight) WeatherIcons.WINDY else WeatherIcons.DAY_HAZE else if (icon.contains("sleet") || icon.contains("snow_changing_to_an_icy_mix") || icon.contains("an_icy_mix_changing_to_snow")
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    || icon.contains("rain_changing_to_snow")) weatherIcon = WeatherIcons.SLEET else if (icon.contains("mixture_of_precip") || icon.contains("icy_mix") || icon.contains("snow_changing_to_rain")
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         || icon.contains("snow_rain_mix") || icon.contains("freezing_rain")) weatherIcon = WeatherIcons.RAIN_MIX else if (icon.contains("hail")) weatherIcon = WeatherIcons.HAIL else if (icon.contains("snow")) weatherIcon = WeatherIcons.SNOW else if (icon.contains("sprinkles") || icon.contains("drizzle")) weatherIcon = WeatherIcons.SPRINKLE else if (icon.contains("light_rain") || icon.contains("showers")) weatherIcon = WeatherIcons.SHOWERS else if (icon.contains("rain") || icon.contains("flood")) weatherIcon = WeatherIcons.RAIN else if (icon.contains("tstorms") || icon.contains("thunderstorms") || icon.contains("thundershowers")
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               || icon.contains("tropical_storm")) weatherIcon = WeatherIcons.THUNDERSTORM else if (icon.contains("smoke")) weatherIcon = WeatherIcons.SMOKE else if (icon.contains("tornado")) weatherIcon = WeatherIcons.TORNADO else if (icon.contains("hurricane")) weatherIcon = WeatherIcons.HURRICANE else if (icon.contains("sandstorm")) weatherIcon = WeatherIcons.SANDSTORM else if (icon.contains("duststorm")) weatherIcon = WeatherIcons.DUST else if (icon.contains("clear") || icon.contains("sunny")) weatherIcon = if (isNight) WeatherIcons.NIGHT_CLEAR else WeatherIcons.DAY_SUNNY else if (icon.contains("cw_no_report_icon") || icon.startsWith("night_")) {
            weatherIcon = if (isNight)
                WeatherIcons.NIGHT_CLEAR
            else
                WeatherIcons.DAY_SUNNY
        }

        if (weatherIcon.isBlank()) {
            // Not Available
            weatherIcon = WeatherIcons.NA
        }

        return weatherIcon
    }
}