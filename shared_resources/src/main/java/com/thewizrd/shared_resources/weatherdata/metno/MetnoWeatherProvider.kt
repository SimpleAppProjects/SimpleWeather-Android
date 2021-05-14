package com.thewizrd.shared_resources.weatherdata.metno

import android.util.Log
import com.thewizrd.shared_resources.R
import com.thewizrd.shared_resources.SimpleLibrary
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.locationiq.LocationIQProvider
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.await
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.getStream
import com.thewizrd.shared_resources.remoteconfig.RemoteConfig
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.WeatherProviderImpl
import com.thewizrd.shared_resources.weatherdata.model.Weather
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.Request
import okhttp3.internal.closeQuietly
import java.io.IOException
import java.text.DecimalFormat
import java.time.*
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit

class MetnoWeatherProvider : WeatherProviderImpl() {
    companion object {
        private const val FORECAST_QUERY_URL = "https://api.met.no/weatherapi/locationforecast/2.0/complete.json?%s"
        private const val SUNRISE_QUERY_URL = "https://api.met.no/weatherapi/sunrise/2.0/.json?%s&date=%s&offset=+00:00"

        private fun getNeutralIconName(icon_variant: String?): String {
            return icon_variant?.replace("_day", "")?.replace("_night", "")?.replace("_polartwilight", "")
                   ?: ""
        }
    }

    init {
        mLocationProvider = RemoteConfig.getLocationProvider(getWeatherAPI())
                            ?: LocationIQProvider()
    }

    override fun getWeatherAPI(): String {
        return WeatherAPI.METNO
    }

    override fun supportsWeatherLocale(): Boolean {
        return false
    }

    override fun isKeyRequired(): Boolean {
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
                var weather: Weather? = null

                val client = SimpleLibrary.getInstance().httpClient
                var forecastResponse: okhttp3.Response? = null
                var sunriseResponse: okhttp3.Response? = null
                var wEx: WeatherException? = null

                try {
                    val context = SimpleLibrary.getInstance().app.appContext
                    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    val version = String.format("v%s", packageInfo.versionName)

                    val forecastRequest = Request.Builder()
                            .cacheControl(CacheControl.Builder()
                                    .maxAge(1, TimeUnit.HOURS)
                                    .build())
                            .url(String.format(FORECAST_QUERY_URL, location_query))
                            .addHeader("Accept-Encoding", "gzip")
                            .addHeader("User-Agent", String.format("SimpleWeather (thewizrd.dev@gmail.com) %s", version))
                            .build()

                    val date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT))
                    val sunriseRequest = Request.Builder()
                            .cacheControl(CacheControl.Builder()
                                    .maxAge(3, TimeUnit.HOURS)
                                    .build())
                            .url(String.format(SUNRISE_QUERY_URL, location_query, date))
                            .addHeader("Accept-Encoding", "gzip")
                            .addHeader("User-Agent", String.format("SimpleWeather (thewizrd.dev@gmail.com) %s", version))
                            .build()

                    // Connect to webstream
                    forecastResponse = client.newCall(forecastRequest).await()
                    val forecastStream = forecastResponse.getStream()
                    sunriseResponse = client.newCall(sunriseRequest).await()
                    val sunrisesetStream = sunriseResponse.getStream()

                    // Load weather
                    val foreRoot = JSONParser.deserializer<Response>(forecastStream, Response::class.java)
                    val astroRoot = JSONParser.deserializer<AstroResponse>(sunrisesetStream, AstroResponse::class.java)

                    // End Stream
                    forecastStream.closeQuietly()
                    sunrisesetStream.closeQuietly()

                    weather = createWeatherData(foreRoot, astroRoot)
                } catch (ex: Exception) {
                    weather = null
                    if (ex is IOException) {
                        wEx = WeatherException(ErrorStatus.NETWORKERROR)
                    }
                    Logger.writeLine(Log.ERROR, ex, "MetnoWeatherProvider: error getting weather data")
                } finally {
                    forecastResponse?.closeQuietly()
                    sunriseResponse?.closeQuietly()
                }
                if (wEx == null && weather?.isValid == false) {
                    wEx = WeatherException(ErrorStatus.NOWEATHER)
                } else if (weather != null) {
                    weather.query = location_query
                }

                if (wEx != null) throw wEx

                return@withContext weather!!
            }

    override suspend fun updateWeatherData(location: LocationData, weather: Weather) {
        // OWM reports datetime in UTC; add location tz_offset
        val offset = location.tzOffset
        weather.updateTime = weather.updateTime.withZoneSameInstant(offset)

        // The time of day is set to max if the sun never sets/rises and
        // DateTime is set to min if not found
        // Don't change this if its set that way
        if (weather.astronomy.sunrise.isAfter(LocalDateTime.MIN) &&
            weather.astronomy.sunrise.toLocalTime().isBefore(LocalTime.MAX)) weather.astronomy.sunrise = weather.astronomy.sunrise.plusSeconds(offset.totalSeconds.toLong())
        if (weather.astronomy.sunset.isAfter(LocalDateTime.MIN) &&
            weather.astronomy.sunset.toLocalTime().isBefore(LocalTime.MAX)) weather.astronomy.sunset = weather.astronomy.sunset.plusSeconds(offset.totalSeconds.toLong())
        if (weather.astronomy.moonrise.isAfter(LocalDateTime.MIN) &&
            weather.astronomy.moonrise.toLocalTime().isBefore(LocalTime.MAX)) weather.astronomy.moonrise = weather.astronomy.moonrise.plusSeconds(offset.totalSeconds.toLong())
        if (weather.astronomy.moonset.isAfter(LocalDateTime.MIN) &&
            weather.astronomy.moonset.toLocalTime().isBefore(LocalTime.MAX)) weather.astronomy.moonset = weather.astronomy.moonset.plusSeconds(offset.totalSeconds.toLong())

        // Set condition here
        val now = ZonedDateTime.now(ZoneOffset.UTC).withZoneSameInstant(offset).toLocalTime()
        val sunrise = weather.astronomy.sunrise.toLocalTime()
        val sunset = weather.astronomy.sunset.toLocalTime()

        weather.condition.weather = getWeatherCondition(weather.condition.icon)
        weather.condition.icon = getWeatherIcon(now.isBefore(sunrise) || now.isAfter(sunset), weather.condition.icon)
        weather.condition.observationTime = weather.condition.observationTime.withZoneSameInstant(offset)

        for (forecast in weather.forecast) {
            forecast.date = forecast.date.plusSeconds(offset.totalSeconds.toLong())
            forecast.condition = getWeatherCondition(forecast.icon)
            forecast.icon = getWeatherIcon(forecast.icon)
        }

        for (hr_forecast in weather.hrForecast) {
            val hrf_date = hr_forecast.date.withZoneSameInstant(offset)
            hr_forecast.date = hrf_date

            val hrf_localTime = hrf_date.toLocalTime()
            hr_forecast.condition = getWeatherCondition(hr_forecast.icon)
            hr_forecast.icon = getWeatherIcon(hrf_localTime.isBefore(sunrise) || hrf_localTime.isAfter(sunset), hr_forecast.icon)
        }
    }

    override fun updateLocationQuery(weather: Weather): String {
        val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
        df.applyPattern("0.####")
        return String.format(Locale.ROOT, "lat=%s&lon=%s", df.format(weather.location.latitude), df.format(weather.location.longitude))
    }

    override fun updateLocationQuery(location: LocationData): String {
        val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
        df.applyPattern("0.####")
        return String.format(Locale.ROOT, "lat=%s&lon=%s", df.format(location.latitude), df.format(location.longitude))
    }

    override fun getWeatherIcon(icon: String?): String {
        return getWeatherIcon(false, icon)
    }

    // Needed b/c icons don't show whether night or not
    override fun getWeatherIcon(isNight: Boolean, icon: String?): String {
        var weatherIcon = ""

        if (icon == null) return WeatherIcons.NA

        val icon = getNeutralIconName(icon)

        when (icon) {
            "clearsky" -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_CLEAR else WeatherIcons.DAY_SUNNY
            }
            "fair", "partlycloudy" -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY else WeatherIcons.DAY_SUNNY_OVERCAST
            }
            "cloudy" -> weatherIcon = WeatherIcons.CLOUDY
            "rainshowers" -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_SPRINKLE else WeatherIcons.DAY_SPRINKLE
            }
            "rainshowersandthunder" -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_THUNDERSTORM else WeatherIcons.DAY_THUNDERSTORM
            }
            "sleetshowers",
            "lightsleetshowers",
            "heavysleetshowers" -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_SLEET else WeatherIcons.DAY_SLEET
            }
            "snowshowers",
            "lightsnowshowers",
            "heavysnowshowers" -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_SNOW else WeatherIcons.DAY_SNOW
            }
            "rain", "lightrain" -> weatherIcon = WeatherIcons.SPRINKLE
            "heavyrain" -> weatherIcon = WeatherIcons.RAIN
            "heavyrainandthunder" -> weatherIcon = WeatherIcons.THUNDERSTORM
            "sleet",
            "lightsleet",
            "heavysleet" -> weatherIcon = WeatherIcons.SLEET
            "snow", "lightsnow" -> weatherIcon = WeatherIcons.SNOW
            "snowandthunder",
            "snowshowersandthunder",
            "lightssnowshowersandthunder",
            "heavysnowshowersandthunder",
            "lightsnowandthunder",
            "heavysnowandthunder" -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_SNOW_THUNDERSTORM else WeatherIcons.DAY_SNOW_THUNDERSTORM
            }
            "fog" -> weatherIcon = WeatherIcons.FOG
            "sleetshowersandthunder",
            "sleetandthunder",
            "lightssleetshowersandthunder",
            "heavysleetshowersandthunder",
            "lightsleetandthunder",
            "heavysleetandthunder" -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_SLEET_STORM else WeatherIcons.DAY_SLEET_STORM
            }
            "rainandthunder",
            "lightrainandthunder",
            "lightrainshowersandthunder",
            "heavyrainshowersandthunder" -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_STORM_SHOWERS else WeatherIcons.DAY_STORM_SHOWERS
            }
            "lightrainshowers",
            "heavyrainshowers" -> {
                weatherIcon = if (isNight) WeatherIcons.NIGHT_ALT_RAIN else WeatherIcons.DAY_RAIN
            }
            "heavysnow" -> weatherIcon = WeatherIcons.SNOW_WIND
        }

        if (weatherIcon.isBlank()) {
            // Not Available
            weatherIcon = WeatherIcons.NA
        }

        return weatherIcon
    }

    override fun getWeatherCondition(icon: String?): String {
        val context = SimpleLibrary.getInstance().appContext

        if (icon == null) return context.getString(R.string.weather_notavailable)

        val icon = getNeutralIconName(icon)

        return when (icon) {
            "clearsky" -> context.getString(R.string.weather_clearsky)
            "fair" -> context.getString(R.string.weather_fair)
            "partlycloudy" -> context.getString(R.string.weather_partlycloudy)
            "cloudy" -> context.getString(R.string.weather_cloudy)
            "rainshowers" -> context.getString(R.string.weather_rainshowers)
            "rainshowersandthunder" -> context.getString(R.string.weather_tstorms)
            "sleetshowers", "lightsleetshowers", "sleet", "lightsleet", "heavysleet", "heavysleetshowers" -> context.getString(R.string.weather_sleet)
            "snow", "snowshowers" -> context.getString(R.string.weather_snow)
            "lightsnowshowers", "lightsnow" -> context.getString(R.string.weather_lightsnowshowers)
            "heavysnowshowers", "heavysnow" -> context.getString(R.string.weather_heavysnow)
            "rain" -> context.getString(R.string.weather_rain)
            "lightrain" -> context.getString(R.string.weather_lightrain)
            "heavyrain" -> context.getString(R.string.weather_heavyrain)
            "rainandthunder", "lightrainandthunder", "lightrainshowersandthunder", "heavyrainshowersandthunder", "heavyrainandthunder" -> context.getString(R.string.weather_tstorms)
            "snowandthunder", "snowshowersandthunder", "lightssnowshowersandthunder", "heavysnowshowersandthunder", "lightsnowandthunder", "heavysnowandthunder" -> context.getString(R.string.weather_snow_tstorms)
            "fog" -> context.getString(R.string.weather_fog)
            "sleetshowersandthunder", "sleetandthunder", "lightssleetshowersandthunder", "heavysleetshowersandthunder", "lightsleetandthunder", "heavysleetandthunder" -> context.getString(R.string.weather_sleet_tstorms)
            "lightrainshowers", "heavyrainshowers" -> context.getString(R.string.weather_rainshowers)
            else -> super.getWeatherCondition(icon)
        }
    }

    // Met.no conditions can be for any time of day
    // So use sunrise/set data as fallback
    override fun isNight(weather: Weather): Boolean {
        var isNight = super.isNight(weather)

        if (!isNight) {
            // Fallback to sunset/rise time just in case
            var tz: ZoneOffset? = null
            if (!weather.location.tzLong.isNullOrBlank()) {
                val id = ZoneId.of(weather.location.tzLong)
                tz = id.rules.getOffset(Instant.now())
            }
            if (tz == null) {
                tz = weather.location.tzOffset
            }

            val sunrise = weather.astronomy?.sunrise?.toLocalTime() ?: LocalTime.of(6, 0)
            val sunset = weather.astronomy?.sunset?.toLocalTime() ?: LocalTime.of(18, 0)

            val now = ZonedDateTime.now(tz).toLocalTime()

            // Determine whether its night using sunset/rise times
            if (now.toNanoOfDay() < sunrise.toNanoOfDay() || now.toNanoOfDay() > sunset.toNanoOfDay()) isNight = true
        }

        return isNight
    }
}