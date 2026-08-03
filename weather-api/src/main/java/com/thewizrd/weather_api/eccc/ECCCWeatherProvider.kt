package com.thewizrd.weather_api.eccc

import android.util.Log
import com.ibm.icu.util.ULocale
import com.thewizrd.shared_resources.R
import com.thewizrd.shared_resources.exceptions.ErrorStatus
import com.thewizrd.shared_resources.exceptions.WeatherException
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.json.listType
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.LocationQuery
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.await
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.getStream
import com.thewizrd.shared_resources.remoteconfig.remoteConfigService
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.utils.LocationUtils
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.utils.ZoneIdCompat
import com.thewizrd.shared_resources.utils.createUnsupportedLocationException
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.shared_resources.weatherdata.model.isNullOrInvalid
import com.thewizrd.weather_api.extras.cacheRequestIfNeeded
import com.thewizrd.weather_api.locationiq.LocationIQProvider
import com.thewizrd.weather_api.nws.SolCalcAstroProvider
import com.thewizrd.weather_api.smc.SunMoonCalcProvider
import com.thewizrd.weather_api.utils.APIRequestUtils.checkForErrors
import com.thewizrd.weather_api.utils.APIRequestUtils.checkRateLimit
import com.thewizrd.weather_api.utils.logMissingIcon
import com.thewizrd.weather_api.weatherModule
import com.thewizrd.weather_api.weatherdata.WeatherProviderImpl
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
import java.util.Locale
import java.util.concurrent.TimeUnit

class ECCCWeatherProvider : WeatherProviderImpl() {
    companion object {
        private const val BASE_URL = "https://app.weather.gc.ca/v2/%s/Location/%s"
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
        return WeatherAPI.ECCC
    }

    override fun supportsWeatherLocale(): Boolean {
        return true
    }

    override fun isKeyRequired(): Boolean {
        return false
    }

    override fun supportsAlerts(): Boolean {
        return true
    }

    override fun needsExternalAlertData(): Boolean {
        return false
    }

    override fun isRegionSupported(location: LocationData): Boolean {
        return LocationUtils.isCanada(location)
    }

    override fun isRegionSupported(location: LocationQuery): Boolean {
        return LocationUtils.isCanada(location)
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
    override suspend fun getWeatherData(location: LocationData): Weather =
        withContext(Dispatchers.IO) {
            var weather: Weather?

            // ECCC
            if (!LocationUtils.isCanada(location)) {
                throw WeatherException(ErrorStatus.LOCATIONNOTSUPPORTED)
                    .initCause(createUnsupportedLocationException(getWeatherAPI(), location))
            }

            val uLocale = ULocale.forLocale(LocaleUtils.getLocale())
            val locale = localeToLangCode(uLocale.language, uLocale.toLanguageTag())
            val query = updateLocationQuery(location)

            val client = sharedDeps.httpClient
            var forecastResponse: Response? = null
            var wEx: WeatherException? = null

            try {
                // If were under rate limit, deny request
                checkRateLimit()

                val forecastRequest = Request.Builder()
                    .cacheRequestIfNeeded(isKeyRequired(), 1, TimeUnit.HOURS)
                    .url(BASE_URL.format(locale, query))
                    .build()

                // Connect to webstream
                forecastResponse = client.newCall(forecastRequest).await()
                checkForErrors(forecastResponse)

                val forecastStream = forecastResponse.getStream()

                // Load weather
                val root = JSONParser.deserializer<List<LocationResponseItem>>(
                    forecastStream,
                    listType<LocationResponseItem>()
                )

                // End Stream
                forecastStream.closeQuietly()

                val foreRoot =
                    requireNotNull(root?.firstOrNull()) { "List<LocationResponseItem> is null" }

                weather = createWeatherData(foreRoot)
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
                    "ECCCWeatherProvider: error getting weather data"
                )
            } finally {
                forecastResponse?.closeQuietly()
            }

            if (wEx == null && weather.isNullOrInvalid()) {
                wEx = WeatherException(ErrorStatus.NOWEATHER)
            } else if (weather != null) {
                if (supportsWeatherLocale())
                    weather.locale = locale

                weather.query = query
            }

            if (wEx != null) throw wEx

            return@withContext weather!!
        }

    @Throws(WeatherException::class)
    override suspend fun updateWeatherData(location: LocationData, weather: Weather) {
        super.updateWeatherData(location, weather)

        val offset = location.tzOffset
        weather.updateTime = weather.updateTime!!.withZoneSameInstant(offset)
        weather.condition!!.observationTime =
            weather.condition!!.observationTime.withZoneSameInstant(offset)

        // Calculate astronomy
        val newAstro = try {
            SunMoonCalcProvider().getAstronomyData(location, weather.condition!!.observationTime)
        } catch (e: WeatherException) {
            Logger.writeLine(Log.ERROR, e)
            SolCalcAstroProvider().getAstronomyData(location, weather.condition!!.observationTime)
        }

        if (weather.astronomy != null) {
            weather.astronomy!!.moonrise = newAstro.moonrise
            weather.astronomy!!.moonset = newAstro.moonset
        } else {
            weather.astronomy = newAstro
        }

        // Update icons
        val now = ZonedDateTime.now(ZoneOffset.UTC).withZoneSameInstant(offset).toLocalTime()
        val sunrise = weather.astronomy!!.sunrise.toLocalTime()
        val sunset = weather.astronomy!!.sunset.toLocalTime()

        if (weather.condition!!.weather.isNullOrBlank()) {
            weather.condition!!.weather = getWeatherCondition(weather.condition!!.icon)
        }
        weather.condition!!.icon =
            getWeatherIcon(/*now.isBefore(sunrise) || now.isAfter(sunset), */weather.condition!!.icon)

        for (forecast in weather.forecast!!) {
            forecast.icon.let {
                forecast.icon = getWeatherIcon(it)
                forecast.condition = getWeatherCondition(it)
            }
        }

        for (hrForecast in weather.hrForecast!!) {
            val hrfDate = hrForecast.date.withZoneSameInstant(offset)
            hrForecast.date = hrfDate

            val hrfLocalTime = hrfDate.toLocalTime()

            hrForecast.icon.let {
                hrForecast.icon = getWeatherIcon(
                    hrfLocalTime.isBefore(sunrise) || hrfLocalTime.isAfter(sunset),
                    it
                )
                hrForecast.condition = getWeatherCondition(it)
            }
        }

        if (!weather.weatherAlerts.isNullOrEmpty()) {
            for (alert in weather.weatherAlerts) {
                alert.date = alert.date.withZoneSameInstant(offset)
                alert.expiresDate = alert.expiresDate.withZoneSameInstant(offset)
            }
        }
    }

    override suspend fun updateLocationQuery(weather: Weather): String {
        val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
        df.applyPattern("0.####")
        return String.format(
            Locale.ROOT,
            "%s,%s",
            df.format(weather.location!!.latitude),
            df.format(weather.location!!.longitude)
        )
    }

    override suspend fun updateLocationQuery(location: LocationData): String {
        val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
        df.applyPattern("0.####")
        return String.format(
            Locale.ROOT,
            "%s,%s",
            df.format(location.latitude),
            df.format(location.longitude)
        )
    }

    override fun localeToLangCode(iso: String, name: String): String {
        if (iso == "fr")
            return iso

        return "en"
    }

    override fun getWeatherIcon(icon: String?): String {
        var isNight = false

        if (icon == null) return WeatherIcons.NA

        val iconCode = icon.toIntOrNull()

        if (iconCode != null) {
            isNight = iconCode in 30..39
        }

        return getWeatherIcon(isNight, icon)
    }

    // https://eccc-msc.github.io/open-data/msc-data/citypage-weather/readme_citypageweather-datamart_en/#icons-of-the-xml-product
    override fun getWeatherIcon(isNight: Boolean, icon: String?): String {
        var weatherIcon = ""

        val iconCode = icon?.toIntOrNull() ?: return WeatherIcons.NA

        when (iconCode) {
            0, 1, 30, 31 -> weatherIcon =
                if (isNight) WeatherIcons.NIGHT_CLEAR else WeatherIcons.DAY_SUNNY

            2, 32 -> weatherIcon =
                if (isNight) WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY else WeatherIcons.DAY_PARTLY_CLOUDY

            3, 4, 5, 22, 33, 34, 35 -> weatherIcon =
                if (isNight) WeatherIcons.NIGHT_ALT_CLOUDY else WeatherIcons.DAY_CLOUDY

            6, 36 -> weatherIcon =
                if (isNight) WeatherIcons.NIGHT_ALT_SPRINKLE else WeatherIcons.DAY_SPRINKLE

            7, 37 -> weatherIcon =
                if (isNight) WeatherIcons.NIGHT_ALT_RAIN_MIX else WeatherIcons.DAY_RAIN_MIX

            8, 38 -> weatherIcon =
                if (isNight) WeatherIcons.NIGHT_ALT_SNOW else WeatherIcons.DAY_SNOW

            9, 39 -> weatherIcon =
                if (isNight) WeatherIcons.NIGHT_ALT_THUNDERSTORM else WeatherIcons.DAY_THUNDERSTORM

            10 -> weatherIcon =
                if (isNight) WeatherIcons.NIGHT_OVERCAST else WeatherIcons.DAY_SUNNY_OVERCAST

            11 -> weatherIcon = WeatherIcons.RAIN
            12 -> weatherIcon = WeatherIcons.SHOWERS
            13 -> weatherIcon = WeatherIcons.RAIN_WIND
            14, 15, 28 -> weatherIcon = WeatherIcons.RAIN_MIX
            16 -> weatherIcon = WeatherIcons.SNOW
            17, 18, 25, 40 -> weatherIcon = WeatherIcons.SNOW_WIND
            19 -> weatherIcon = WeatherIcons.STORM_SHOWERS
            23 -> weatherIcon = WeatherIcons.HAZE
            24 -> weatherIcon = WeatherIcons.FOG
            26, 27 -> weatherIcon = WeatherIcons.HAIL
            29 -> weatherIcon = WeatherIcons.NA
            41, 42, 48 -> weatherIcon = WeatherIcons.TORNADO
            43 -> weatherIcon = WeatherIcons.WINDY
            44 -> weatherIcon = WeatherIcons.SMOKE
            45, 47 -> weatherIcon = WeatherIcons.DUST
            46 -> weatherIcon = WeatherIcons.SNOW_THUNDERSTORM
        }

        if (weatherIcon.isBlank()) {
            // Not Available
            logMissingIcon(icon)
            weatherIcon = WeatherIcons.NA
        }

        return weatherIcon
    }

    override fun getWeatherCondition(icon: String?): String {
        val iconCode = icon?.toIntOrNull() ?: return super.getWeatherCondition(icon)

        return when (iconCode) {
            0, 1 -> context.getString(R.string.weather_sunny)
            2, 32 -> context.getString(R.string.weather_partlycloudy)
            3, 4, 5, 22, 33, 34, 35 -> context.getString(R.string.weather_mostlycloudy)
            6, 36 -> context.getString(R.string.weather_drizzle)
            7, 15, 28, 37 -> context.getString(R.string.weather_rainandsnow)
            8, 38 -> context.getString(R.string.weather_snowflurries)
            9, 19, 39 -> context.getString(R.string.weather_tstorms)
            10 -> context.getString(R.string.weather_cloudy)
            11 -> context.getString(R.string.weather_rain)
            12, 13 -> context.getString(R.string.weather_rainshowers)
            14, 26 -> context.getString(R.string.weather_freezingrain)
            16, 17 -> context.getString(R.string.weather_snow)
            18 -> context.getString(R.string.weather_heavysnow)
            23 -> context.getString(R.string.weather_haze)
            24 -> context.getString(R.string.weather_fog)
            25, 40 -> context.getString(R.string.weather_blowingsnow)
            27, 46 -> context.getString(R.string.weather_hail)
            29 -> context.getString(R.string.weather_notavailable)
            30, 31 -> context.getString(R.string.weather_clear)
            41, 42, 48 -> context.getString(R.string.weather_tornado)
            43 -> context.getString(R.string.weather_windy)
            44 -> context.getString(R.string.weather_smoky)
            45, 47 -> context.getString(R.string.weather_dust)
            else -> super.getWeatherCondition(icon)
        }
    }

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
            if (now.toNanoOfDay() < sunrise.toNanoOfDay() || now.toNanoOfDay() > sunset.toNanoOfDay()) isNight =
                true
        }

        return isNight
    }
}