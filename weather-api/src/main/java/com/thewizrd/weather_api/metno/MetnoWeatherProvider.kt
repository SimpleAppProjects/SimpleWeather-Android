package com.thewizrd.weather_api.metno

import android.util.Log
import com.thewizrd.shared_resources.R
import com.thewizrd.shared_resources.exceptions.ErrorStatus
import com.thewizrd.shared_resources.exceptions.WeatherException
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.await
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.getStream
import com.thewizrd.shared_resources.remoteconfig.remoteConfigService
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.utils.ZoneIdCompat
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.shared_resources.weatherdata.model.isNullOrInvalid
import com.thewizrd.weather_api.extras.cacheRequestIfNeeded
import com.thewizrd.weather_api.locationiq.LocationIQProvider
import com.thewizrd.weather_api.utils.APIRequestUtils.checkForErrors
import com.thewizrd.weather_api.utils.APIRequestUtils.checkRateLimit
import com.thewizrd.weather_api.utils.logMissingIcon
import com.thewizrd.weather_api.weatherModule
import com.thewizrd.weather_api.weatherdata.WeatherProviderImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
            return icon_variant?.replace("_day", "")
                ?.replace("_night", "")
                ?.replace("_polartwilight", "")
                ?: ""
        }
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
    override suspend fun getWeatherData(location: LocationData): Weather =
        withContext(Dispatchers.IO) {
            var weather: Weather?

            var forecastResponse: okhttp3.Response? = null
            var sunriseResponse: okhttp3.Response? = null
            var wEx: WeatherException? = null

            val query = updateLocationQuery(location)

            try {
                // If were under rate limit, deny request
                checkRateLimit()

                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val version = String.format("v%s", packageInfo.versionName)

                val forecastRequest = Request.Builder()
                    .cacheRequestIfNeeded(isKeyRequired(), 15, TimeUnit.MINUTES)
                    .url(String.format(FORECAST_QUERY_URL, query))
                            .addHeader("Accept-Encoding", "gzip")
                            .addHeader("User-Agent", String.format("SimpleWeather (thewizrd.dev@gmail.com) %s", version))
                            .build()

                    val date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT))
                    val sunriseRequest = Request.Builder()
                        .cacheRequestIfNeeded(isKeyRequired(), 30, TimeUnit.MINUTES)
                        .url(String.format(SUNRISE_QUERY_URL, query, date))
                        .addHeader("Accept-Encoding", "gzip")
                        .addHeader(
                            "User-Agent",
                            String.format("SimpleWeather (thewizrd.dev@gmail.com) %s", version)
                        )
                        .build()

                    // Connect to webstream
                    forecastResponse = httpClient.newCall(forecastRequest).await()
                    checkForErrors(forecastResponse)

                    sunriseResponse = httpClient.newCall(sunriseRequest).await()
                    checkForErrors(sunriseResponse)

                    val forecastStream = forecastResponse.getStream()
                    val sunrisesetStream = sunriseResponse.getStream()

                    // Load weather
                    val foreRoot =
                        JSONParser.deserializer<Response>(forecastStream, Response::class.java)
                    val astroRoot = JSONParser.deserializer<AstroResponse>(
                        sunrisesetStream,
                        AstroResponse::class.java
                    )

                    // End Stream
                    forecastStream.closeQuietly()
                    sunrisesetStream.closeQuietly()

                    requireNotNull(foreRoot)
                    requireNotNull(astroRoot)

                    weather = createWeatherData(foreRoot, astroRoot)
                } catch (ex: Exception) {
                    weather = null
                    if (ex is IOException) {
                        wEx = WeatherException(ErrorStatus.NETWORKERROR, ex)
                    } else if (ex is WeatherException) {
                        wEx = ex
                    }
                    Logger.writeLine(Log.ERROR, ex, "MetnoWeatherProvider: error getting weather data")
                } finally {
                    forecastResponse?.closeQuietly()
                    sunriseResponse?.closeQuietly()
                }

                if (wEx == null && weather.isNullOrInvalid()) {
                    wEx = WeatherException(ErrorStatus.NOWEATHER)
                } else if (weather != null) {
                    weather.query = query
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
            val hrfDate = hr_forecast.date.withZoneSameInstant(offset)
            hr_forecast.date = hrfDate

            val hrfLocalTime = hrfDate.toLocalTime()
            hr_forecast.condition = getWeatherCondition(hr_forecast.icon)
            hr_forecast.icon = getWeatherIcon(
                hrfLocalTime.isBefore(sunrise) || hrfLocalTime.isAfter(sunset),
                hr_forecast.icon
            )
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

        //val isNeutral = icon.split("_").size == 1
        val isDay = icon.endsWith("day") && !isNight
        val isNight = icon.endsWith("night") && isNight
        //val isPolarTwilight = icon.endsWith("polartwilight")

        when (getNeutralIconName(icon)) {
            "clearsky" -> {
                weatherIcon = when {
                    isNight -> WeatherIcons.NIGHT_CLEAR
                    else -> WeatherIcons.DAY_SUNNY
                }
            }
            "cloudy" -> weatherIcon = WeatherIcons.CLOUDY
            "fair", "partlycloudy" -> {
                weatherIcon = when {
                    isNight -> WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY
                    else -> WeatherIcons.DAY_PARTLY_CLOUDY
                }
            }
            "fog" -> weatherIcon = WeatherIcons.FOG
            "heavyrain" -> weatherIcon = WeatherIcons.RAIN_WIND
            "heavyrainandthunder" -> weatherIcon = WeatherIcons.THUNDERSTORM
            "heavyrainshowers" -> {
                weatherIcon = when {
                    isDay -> WeatherIcons.DAY_RAIN_WIND
                    isNight -> WeatherIcons.NIGHT_ALT_RAIN_WIND
                    else -> WeatherIcons.RAIN_WIND
                }
            }
            "heavyrainshowersandthunder", "lightrainshowersandthunder" -> {
                weatherIcon = when {
                    isDay -> WeatherIcons.DAY_STORM_SHOWERS
                    isNight -> WeatherIcons.NIGHT_ALT_STORM_SHOWERS
                    else -> WeatherIcons.STORM_SHOWERS
                }
            }
            "heavysleet", "lightsleet", "sleet" -> weatherIcon = WeatherIcons.SLEET
            "heavysleetandthunder", "lightsleetandthunder", "sleetandthunder" -> {
                weatherIcon = when {
                    isNight -> WeatherIcons.NIGHT_ALT_SLEET_STORM
                    else -> WeatherIcons.DAY_SLEET_STORM
                }
            }
            "heavysleetshowersandthunder", "lightssleetshowersandthunder",
            "sleetshowersandthunder" -> {
                weatherIcon = when {
                    isDay -> WeatherIcons.DAY_SLEET_STORM
                    isNight -> WeatherIcons.NIGHT_ALT_SLEET_STORM
                    else -> WeatherIcons.SLEET_STORM
                }
            }
            "heavysleetshowers" -> {
                weatherIcon = when {
                    isDay -> WeatherIcons.DAY_SLEET
                    isNight -> WeatherIcons.NIGHT_ALT_SLEET
                    else -> WeatherIcons.SLEET
                }
            }
            "heavysnow" -> weatherIcon = WeatherIcons.SNOW_WIND
            "heavysnowandthunder", "heavysnowshowersandthunder", "lightsnowandthunder",
            "lightssnowshowersandthunder", "snowandthunder", "snowshowersandthunder" -> {
                weatherIcon = when {
                    isNight -> WeatherIcons.NIGHT_ALT_SNOW_THUNDERSTORM
                    else -> WeatherIcons.DAY_SNOW_THUNDERSTORM
                }
            }
            "heavysnowshowers" -> {
                weatherIcon = when {
                    isDay -> WeatherIcons.DAY_SNOW_WIND
                    isNight -> WeatherIcons.NIGHT_ALT_SNOW_WIND
                    else -> WeatherIcons.SNOW_WIND
                }
            }
            "lightrain" -> weatherIcon = WeatherIcons.SPRINKLE
            "lightrainandthunder", "rainandthunder" -> weatherIcon = WeatherIcons.STORM_SHOWERS
            "lightrainshowers", "rainshowers" -> {
                weatherIcon = when {
                    isDay -> WeatherIcons.DAY_SHOWERS
                    isNight -> WeatherIcons.NIGHT_ALT_SHOWERS
                    else -> WeatherIcons.SHOWERS
                }
            }
            "lightsleetshowers", "sleetshowers" -> {
                weatherIcon = when {
                    isDay -> WeatherIcons.DAY_SLEET
                    isNight -> WeatherIcons.NIGHT_ALT_SLEET
                    else -> WeatherIcons.SLEET
                }
            }
            "lightsnow", "snow" -> weatherIcon = WeatherIcons.SNOW
            "lightsnowshowers", "snowshowers" -> {
                weatherIcon = when {
                    isDay -> WeatherIcons.DAY_SNOW
                    isNight -> WeatherIcons.NIGHT_ALT_SNOW
                    else -> WeatherIcons.SNOW
                }
            }
            "rain" -> weatherIcon = WeatherIcons.RAIN
            "rainshowersandthunder" -> {
                weatherIcon = when {
                    isDay -> WeatherIcons.DAY_THUNDERSTORM
                    isNight -> WeatherIcons.NIGHT_ALT_THUNDERSTORM
                    else -> WeatherIcons.THUNDERSTORM
                }
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
        if (icon == null) return context.getString(R.string.weather_notavailable)

        return when (getNeutralIconName(icon)) {
            "clearsky" -> {
                context.getString(R.string.weather_clearsky)
            }
            "cloudy" -> {
                context.getString(R.string.weather_cloudy)
            }
            "fair" -> {
                context.getString(R.string.weather_fair)
            }
            "fog" -> {
                context.getString(R.string.weather_fog)
            }
            "heavyrain" -> {
                context.getString(R.string.weather_heavyrain)
            }
            "heavyrainandthunder", "heavyrainshowersandthunder", "lightrainandthunder",
            "lightrainshowersandthunder", "rainandthunder", "rainshowersandthunder" -> {
                context.getString(R.string.weather_tstorms)
            }
            "heavyrainshowers", "lightrainshowers", "rainshowers" -> {
                context.getString(R.string.weather_rainshowers)
            }
            "heavysleet", "heavysleetshowers" -> {
                context.getString(R.string.weather_sleet)
            }
            "heavysleetandthunder", "heavysleetshowersandthunder", "lightsleetandthunder",
            "lightssleetshowersandthunder", "sleetandthunder", "sleetshowersandthunder" -> {
                context.getString(R.string.weather_sleet_tstorms)
            }
            "heavysnow", "heavysnowshowers" -> {
                context.getString(R.string.weather_heavysnow)
            }
            "heavysnowandthunder", "heavysnowshowersandthunder", "lightsnowandthunder",
            "lightssnowshowersandthunder", "snowandthunder", "snowshowersandthunder" -> {
                context.getString(R.string.weather_snow_tstorms)
            }
            "lightrain" -> {
                context.getString(R.string.weather_lightrain)
            }
            "lightsleet", "lightsleetshowers", "sleet", "sleetshowers" -> {
                context.getString(R.string.weather_sleet)
            }
            "lightsnow", "lightsnowshowers" -> {
                context.getString(R.string.weather_lightsnowshowers)
            }
            "partlycloudy" -> {
                context.getString(R.string.weather_partlycloudy)
            }
            "rain" -> {
                context.getString(R.string.weather_rain)
            }
            "snow", "snowshowers" -> {
                context.getString(R.string.weather_snow)
            }
            else -> {
                super.getWeatherCondition(icon)
            }
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
            if (now.toNanoOfDay() < sunrise.toNanoOfDay() || now.toNanoOfDay() > sunset.toNanoOfDay()) isNight = true
        }

        return isNight
    }
}