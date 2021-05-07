package com.thewizrd.shared_resources.weatherdata.nws

import android.util.Log
import com.google.gson.JsonStreamParser
import com.thewizrd.shared_resources.R
import com.thewizrd.shared_resources.SimpleLibrary
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.weatherapi.WeatherApiLocationProvider
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.await
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.getStream
import com.thewizrd.shared_resources.remoteconfig.RemoteConfig.getLocationProvider
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.weatherdata.Weather
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.WeatherProviderImpl
import com.thewizrd.shared_resources.weatherdata.nws.hourly.HourlyForecastResponse
import com.thewizrd.shared_resources.weatherdata.nws.hourly.Location
import com.thewizrd.shared_resources.weatherdata.nws.hourly.PeriodsItem
import com.thewizrd.shared_resources.weatherdata.nws.observation.ForecastResponse
import com.thewizrd.shared_resources.weatherdata.smc.SunMoonCalcProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.text.DecimalFormat
import java.time.*
import java.util.*
import java.util.concurrent.TimeUnit

class NWSWeatherProvider : WeatherProviderImpl() {
    companion object {
        private const val FORECAST_QUERY_URL = "https://forecast.weather.gov/MapClick.php?%s&FcstType=json"
        private const val HRFORECAST_QUERY_URL = "https://forecast.weather.gov/MapClick.php?%s&FcstType=digitalJSON"
    }

    init {
        mLocationProvider = getLocationProvider(getWeatherAPI()) ?: WeatherApiLocationProvider()
    }

    override fun getWeatherAPI(): String {
        return WeatherAPI.NWS
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
                var weather: Weather?

                val client = SimpleLibrary.getInstance().httpClient
                var observationResponse: Response? = null
                var forecastResponse: Response? = null
                var wEx: WeatherException? = null

                try {
                    val context = SimpleLibrary.getInstance().app.appContext
                    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    val version = String.format("v%s", packageInfo.versionName)

                    val observationRequest = Request.Builder()
                            .cacheControl(CacheControl.Builder()
                                    .maxAge(1, TimeUnit.HOURS)
                                    .build())
                            .url(String.format(FORECAST_QUERY_URL, location_query))
                            .addHeader("Accept", "application/ld+json")
                            .addHeader("User-Agent", String.format("SimpleWeather (thewizrd.dev@gmail.com) %s", version))
                            .build()

                    // Connect to webstream
                    observationResponse = client.newCall(observationRequest).await()

                    // Check for errors
                    checkForErrors(observationResponse.code)

                    val observationStream = observationResponse.getStream()

                    // Load point json data
                    val observationData = JSONParser.deserializer<ForecastResponse>(observationStream, ForecastResponse::class.java)

                    // End Stream
                    observationStream.closeQuietly()

                    val hrForecastRequest = Request.Builder()
                            .url(String.format(HRFORECAST_QUERY_URL, location_query))
                            .cacheControl(CacheControl.Builder()
                                    .maxAge(3, TimeUnit.HOURS)
                                    .build())
                            .addHeader("Accept", "application/ld+json")
                            .addHeader("User-Agent", String.format("SimpleWeather (thewizrd.dev@gmail.com) %s", version))
                            .build()

                    // Connect to webstream
                    forecastResponse = client.newCall(hrForecastRequest).await()

                    // Check for errors
                    checkForErrors(forecastResponse.code)

                    val forecastStream = forecastResponse.getStream()

                    // Load point json data
                    val forecastData = createHourlyForecastResponse(forecastStream)

                    // End Stream
                    forecastStream.closeQuietly()

                    weather = Weather(observationData, forecastData)
                } catch (ex: Exception) {
                    weather = null
                    if (ex is IOException) {
                        wEx = WeatherException(ErrorStatus.NETWORKERROR)
                    }
                    Logger.writeLine(Log.ERROR, ex, "NWSWeatherProvider: error getting weather data")
                } finally {
                    observationResponse?.close()
                }

                if (wEx == null && weather?.isValid == false) {
                    wEx = WeatherException(ErrorStatus.NOWEATHER)
                } else if (weather != null) {
                    weather.query = location_query!!
                }

                if (wEx != null) throw wEx

                return@withContext weather!!
            }

    private fun createHourlyForecastResponse(forecastStream: InputStream): HourlyForecastResponse {
        val forecastData = HourlyForecastResponse()
        val forecastParser = JsonStreamParser(InputStreamReader(forecastStream))

        if (forecastParser.hasNext()) {
            val element = forecastParser.next()
            val fcastRoot = element.asJsonObject

            forecastData.creationDate = fcastRoot["creationDate"].asString
            forecastData.location = Location()

            val location = fcastRoot.getAsJsonObject("location")
            forecastData.location.latitude = location.getAsJsonPrimitive("latitude").asDouble
            forecastData.location.longitude = location.getAsJsonPrimitive("longitude").asDouble

            val periodNameList = fcastRoot.getAsJsonObject("PeriodNameList")
            val sortedKeys: SortedSet<String> = TreeSet(Comparator { o1, o2 ->
                val x = NumberUtils.tryParseInt(o1)
                val y = NumberUtils.tryParseInt(o2)
                if (x != null && y != null) {
                    Integer.compare(x, y)
                } else {
                    if (o1 == null) {
                        return@Comparator -1
                    }
                    if (o2 == null) {
                        1
                    } else o1.compareTo(o2)
                }
            })
            sortedKeys.addAll(periodNameList.keySet())

            forecastData.periodsItems = ArrayList(sortedKeys.size)

            for (periodNumber in sortedKeys) {
                val periodName = periodNameList.getAsJsonPrimitive(periodNumber).asString

                if (!fcastRoot.has(periodName)) continue

                val item = PeriodsItem()

                val periodObj = fcastRoot.getAsJsonObject(periodName)
                val timeArr = periodObj.getAsJsonArray("time")
                val unixTimeArr = periodObj.getAsJsonArray("unixtime")
                val windChillArr = periodObj.getAsJsonArray("windChill")
                val windSpeedArr = periodObj.getAsJsonArray("windSpeed")
                val cloudAmtArr = periodObj.getAsJsonArray("cloudAmount")
                val popArr = periodObj.getAsJsonArray("pop")
                val humidityArr = periodObj.getAsJsonArray("relativeHumidity")
                val windGustArr = periodObj.getAsJsonArray("windGust")
                val tempArr = periodObj.getAsJsonArray("temperature")
                val windDirArr = periodObj.getAsJsonArray("windDirection")
                val iconArr = periodObj.getAsJsonArray("iconLink")
                val conditionTxtArr = periodObj.getAsJsonArray("weather")

                item.periodName = periodObj.getAsJsonPrimitive("periodName").asString

                item.time = ArrayList(timeArr.size())
                for (jsonElement in timeArr) {
                    val time = jsonElement.asString
                    item.time.add(time)
                }

                item.unixtime = ArrayList(unixTimeArr.size())
                for (jsonElement in unixTimeArr) {
                    val time = jsonElement.asString
                    item.unixtime.add(time)
                }

                if (windChillArr != null) {
                    item.windChill = ArrayList(windChillArr.size())
                    for (jsonElement in windChillArr) {
                        val windChill = jsonElement.asString
                        item.windChill.add(windChill)
                    }
                } else {
                    item.windChill = Collections.nCopies<String?>(unixTimeArr.size(), null)
                }

                if (windSpeedArr != null) {
                    item.windSpeed = ArrayList(windSpeedArr.size())
                    for (jsonElement in windSpeedArr) {
                        val windSpeed = jsonElement.asString
                        item.windSpeed.add(windSpeed)
                    }
                } else {
                    item.windSpeed = Collections.nCopies<String?>(unixTimeArr.size(), null)
                }

                if (cloudAmtArr != null) {
                    item.cloudAmount = ArrayList(cloudAmtArr.size())
                    for (jsonElement in cloudAmtArr) {
                        val cloudAmt = jsonElement.asString
                        item.cloudAmount.add(cloudAmt)
                    }
                } else {
                    item.cloudAmount = Collections.nCopies<String?>(unixTimeArr.size(), null)
                }

                if (popArr != null) {
                    item.pop = ArrayList(popArr.size())
                    for (jsonElement in popArr) {
                        val pop = jsonElement.asString
                        item.pop.add(pop)
                    }
                } else {
                    item.pop = Collections.nCopies<String?>(unixTimeArr.size(), null)
                }

                if (humidityArr != null) {
                    item.relativeHumidity = ArrayList(humidityArr.size())
                    for (jsonElement in humidityArr) {
                        val humidity = jsonElement.asString
                        item.relativeHumidity.add(humidity)
                    }
                } else {
                    item.relativeHumidity = Collections.nCopies<String?>(unixTimeArr.size(), null)
                }

                if (windGustArr != null) {
                    item.windGust = ArrayList(windGustArr.size())
                    for (jsonElement in windGustArr) {
                        val windGust = jsonElement.asString
                        item.windGust.add(windGust)
                    }
                } else {
                    item.windGust = Collections.nCopies<String?>(unixTimeArr.size(), null)
                }

                if (tempArr != null) {
                    item.temperature = ArrayList(tempArr.size())
                    for (jsonElement in tempArr) {
                        val temp = jsonElement.asString
                        item.temperature.add(temp)
                    }
                } else {
                    item.temperature = Collections.nCopies<String?>(unixTimeArr.size(), null)
                }

                if (windDirArr != null) {
                    item.windDirection = ArrayList(windDirArr.size())
                    for (jsonElement in windDirArr) {
                        val windDir = jsonElement.asString
                        item.windDirection.add(windDir)
                    }
                } else {
                    item.windDirection = Collections.nCopies<String?>(unixTimeArr.size(), null)
                }

                if (iconArr != null) {
                    item.iconLink = ArrayList(iconArr.size())
                    for (jsonElement in iconArr) {
                        val icon = jsonElement.asString
                        item.iconLink.add(icon)
                    }
                } else {
                    item.iconLink = Collections.nCopies<String?>(unixTimeArr.size(), null)
                }

                if (conditionTxtArr != null) {
                    item.weather = ArrayList(conditionTxtArr.size())
                    for (jsonElement in conditionTxtArr) {
                        val condition = jsonElement.asString
                        item.weather.add(condition)
                    }
                } else {
                    item.weather = Collections.nCopies<String?>(unixTimeArr.size(), null)
                }

                forecastData.periodsItems.add(item)
            }
        }
        return forecastData
    }

    @Throws(WeatherException::class)
    private fun checkForErrors(responseCode: Int) {
        // Check for errors
        when (responseCode) {
            HttpURLConnection.HTTP_OK -> {
            }
            HttpURLConnection.HTTP_BAD_REQUEST -> {
                throw WeatherException(ErrorStatus.NOWEATHER)
            }
            HttpURLConnection.HTTP_NOT_FOUND -> {
                throw WeatherException(ErrorStatus.QUERYNOTFOUND)
            }
            HttpURLConnection.HTTP_INTERNAL_ERROR -> {
                throw WeatherException(ErrorStatus.UNKNOWN)
            }
            else -> {
                throw WeatherException(ErrorStatus.NOWEATHER)
            }
        }
    }

    @Throws(WeatherException::class)
    override suspend fun updateWeatherData(location: LocationData, weather: Weather) {
        val offset = location.tzOffset

        weather.updateTime = weather.updateTime.withZoneSameInstant(offset)
        weather.condition.observationTime = weather.condition.observationTime.withZoneSameInstant(offset)

        // NWS does not provide astrodata; calculate this ourselves (using their calculator)
        val solCalcData = SolCalcAstroProvider().getAstronomyData(location, weather.condition.observationTime)
        weather.astronomy = SunMoonCalcProvider().getAstronomyData(location, weather.condition.observationTime)
        weather.astronomy.sunrise = solCalcData.sunrise
        weather.astronomy.sunset = solCalcData.sunset

        // Update icons
        val now = ZonedDateTime.now(ZoneOffset.UTC).withZoneSameInstant(offset).toLocalTime()
        val sunrise = weather.astronomy.sunrise.toLocalTime()
        val sunset = weather.astronomy.sunset.toLocalTime()

        weather.condition.icon = getWeatherIcon(now.isBefore(sunrise) || now.isAfter(sunset), weather.condition.icon)

        for (hr_forecast in weather.hrForecast) {
            val hrf_date = hr_forecast.date.withZoneSameInstant(offset)
            hr_forecast.date = hrf_date
            val hrf_localTime = hrf_date.toLocalTime()
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
        return String.format(Locale.ROOT, "lat=%s&lon=%s", df.format(location.latitude), location.longitude)
    }

    override fun getWeatherIcon(icon: String?): String {
        return getWeatherIcon(false, icon)
    }

    override fun getWeatherIcon(isNight: Boolean, icon: String?): String {
        // Example: https://api.weather.gov/icons/land/day/tsra_hi,20?size=medium
        var weatherIcon = ""

        if (icon == null) return WeatherIcons.NA

        weatherIcon = if (icon.contains("fog") || icon == "fg.png" || icon == "nfg.png" || icon.contains("nfg") || icon.matches(".*([/]?)([n]?)fg([0-9]{0,3})((.png)?).*".toRegex())) {
            if (isNight) WeatherIcons.NIGHT_FOG else WeatherIcons.DAY_FOG
        } else if (icon.contains("blizzard")) {
            if (isNight) WeatherIcons.NIGHT_ALT_SNOW_WIND else WeatherIcons.DAY_SNOW_WIND
        } else if (icon.contains("cold")) {
            WeatherIcons.SNOWFLAKE_COLD
        } else if (icon.contains("hot")) {
            if (isNight) WeatherIcons.NIGHT_CLEAR else WeatherIcons.DAY_HOT
        } else if (icon.contains("haze")) {
            if (isNight) WeatherIcons.NIGHT_FOG else WeatherIcons.DAY_HAZE
        } else if (icon.contains("smoke") || icon == "fu.png" || icon == "nfu.png" || icon.contains("nfu") || icon.matches(".*([/]?)([n]?)fu([0-9]{0,3})((.png)?).*".toRegex())) {
            WeatherIcons.SMOKE
        } else if (icon.contains("dust") || icon == "du.png" || icon == "ndu.png" || icon.contains("ndu") || icon.matches(".*([/]?)([n]?)du([0-9]{0,3})((.png)?).*".toRegex())) {
            WeatherIcons.DUST
        } else if (icon.contains("tropical_storm") || icon.contains("hurricane") || icon.contains("hur_warn") || icon.contains("hur_watch") || icon.contains("ts_warn") || icon.contains("ts_watch") || icon.contains("ts_nowarn")) {
            WeatherIcons.HURRICANE
        } else if (icon.contains("tsra")) {
            if (isNight) WeatherIcons.NIGHT_ALT_THUNDERSTORM else WeatherIcons.DAY_THUNDERSTORM
        } else if (icon.contains("tornado") || icon.contains("tor") || icon == "tor.png" || icon == "fc.png" || icon == "nfc.png" || icon.contains("nfc") || icon.matches(".*([/]?)([n]?)fc([0-9]{0,3})((.png)?).*".toRegex())) {
            WeatherIcons.TORNADO
        } else if (icon.contains("rain_showers") || icon.contains("shra") || icon.contains("shwrs")) {
            if (isNight) WeatherIcons.NIGHT_ALT_SHOWERS else WeatherIcons.DAY_SHOWERS
        } else if (icon.contains("fzra") || icon.contains("rain_sleet") || icon.contains("rain_snow") || icon.contains("ra_sn")) {
            if (isNight) WeatherIcons.NIGHT_ALT_RAIN_MIX else WeatherIcons.DAY_RAIN_MIX
        } else if (icon.contains("sleet") || icon.contains("raip")) {
            if (isNight) WeatherIcons.NIGHT_ALT_SLEET else WeatherIcons.DAY_SLEET
        } else if (icon.contains("minus_ra")) {
            if (isNight) WeatherIcons.NIGHT_ALT_SPRINKLE else WeatherIcons.DAY_SPRINKLE
        } else if (icon.contains("rain") || icon == "ra.png" || icon == "nra.png" || icon.contains("nra") || icon.matches(".*([/]?)([n]?)ra([0-9]{0,3})((.png)?).*".toRegex())) {
            if (isNight) WeatherIcons.NIGHT_ALT_RAIN else WeatherIcons.DAY_RAIN
        } else if (icon.contains("snow") || icon == "sn.png" || icon == "nsn.png" || icon.contains("nsn") || icon.matches(".*([/]?)([n]?)sn([0-9]{0,3})((.png)?).*".toRegex())) {
            if (isNight) WeatherIcons.NIGHT_ALT_SNOW else WeatherIcons.DAY_SNOW
        } else if (icon.contains("snip") || icon == "ip.png" || icon == "nip.png" || icon.contains("nip") || icon.matches(".*([/]?)([n]?)ip([0-9]{0,3})((.png)?).*".toRegex())) {
            if (isNight) WeatherIcons.NIGHT_ALT_HAIL else WeatherIcons.DAY_HAIL
        } else if (icon.contains("wind_bkn") || icon.contains("wind_ovc") || icon.contains("wind_sct")) {
            if (isNight) WeatherIcons.NIGHT_ALT_CLOUDY_WINDY else WeatherIcons.DAY_CLOUDY_WINDY
        } else if (icon.contains("wind_skc") || icon.contains("wind_few") || icon.contains("wind")) {
            if (isNight) WeatherIcons.WINDY else WeatherIcons.DAY_WINDY
        } else if (icon.contains("ovc") || icon.contains("sct") || icon.contains("few")) {
            if (isNight) WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY else WeatherIcons.DAY_SUNNY_OVERCAST
        } else if (icon.contains("bkn")) {
            if (isNight) WeatherIcons.NIGHT_ALT_CLOUDY else WeatherIcons.DAY_CLOUDY
        } else if (icon.contains("skc")) {
            if (isNight) WeatherIcons.NIGHT_CLEAR else WeatherIcons.DAY_SUNNY
        } else {
            if (isNight) WeatherIcons.NIGHT_CLEAR else WeatherIcons.DAY_SUNNY
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

        return if (icon.contains("fog") || icon == "fg.png" || icon == "nfg.png" || icon.contains("nfg") || icon.matches(".*([/]?)([n]?)fg([0-9]{0,3})((.png)?).*".toRegex())) {
            context.getString(R.string.weather_fog)
        } else if (icon.contains("blizzard")) {
            context.getString(R.string.weather_blizzard)
        } else if (icon.contains("cold")) {
            context.getString(R.string.weather_cold)
        } else if (icon.contains("hot")) {
            context.getString(R.string.weather_hot)
        } else if (icon.contains("haze") || icon == "hz.png" || icon.matches(".*([/]?)hz([0-9]{0,3})((.png)?).*".toRegex())) {
            context.getString(R.string.weather_haze)
        } else if (icon.contains("smoke") || icon == "fu.png" || icon == "nfu.png" || icon.contains("nfu") || icon.matches(".*([/]?)([n]?)fu([0-9]{0,3})((.png)?).*".toRegex())) {
            context.getString(R.string.weather_smoky)
        } else if (icon.contains("dust") || icon == "du.png" || icon == "ndu.png" || icon.contains("ndu") || icon.matches(".*([/]?)([n]?)du([0-9]{0,3})((.png)?).*".toRegex())) {
            context.getString(R.string.weather_dust)
        } else if (icon.contains("tropical_storm") || icon.contains("ts_warn") || icon.contains("ts_watch") || icon.contains("ts_nowarn")) {
            context.getString(R.string.weather_tropicalstorm)
        } else if (icon.contains("tsra")) {
            context.getString(R.string.weather_tstorms)
        } else if (icon.contains("hurricane") || icon.contains("hur_warn") || icon.contains("hur_watch")) {
            context.getString(R.string.weather_hurricane)
        } else if (icon.contains("tornado") || icon.contains("tor") || icon == "tor.png" || icon == "fc.png" || icon == "nfc.png" || icon.contains("nfc") || icon.matches(".*([/]?)([n]?)fc([0-9]{0,3})((.png)?).*".toRegex())) {
            context.getString(R.string.weather_tornado)
        } else if (icon.contains("rain_showers") || icon.contains("shra") || icon.contains("shwrs")) {
            context.getString(R.string.weather_rainshowers)
        } else if (icon.contains("rain_sleet") || icon.contains("raip")) {
            context.getString(R.string.weather_rainandsleet)
        } else if (icon.contains("rain_snow") || icon.contains("ra_sn")) {
            context.getString(R.string.weather_rainandsnow)
        } else if (icon.contains("fzra")) {
            context.getString(R.string.weather_freezingrain)
        } else if (icon.contains("snow_sleet")) {
            context.getString(R.string.weather_snowandsleet)
        } else if (icon.contains("sleet")) {
            context.getString(R.string.weather_sleet)
        } else if (icon.contains("rain") || icon == "ra.png" || icon == "nra.png" || icon.contains("nra") || icon.matches(".*([/]?)([n]?)ra([0-9]{0,3})((.png)?).*".toRegex())) {
            context.getString(R.string.weather_rain)
        } else if (icon.contains("snow") || icon == "sn.png" || icon == "nsn.png" || icon.contains("nsn") || icon.matches(".*([/]?)([n]?)sn([0-9]{0,3})((.png)?).*".toRegex())) {
            context.getString(R.string.weather_snow)
        } else if (icon.contains("snip") || icon == "ip.png" || icon == "nip.png" || icon.contains("nip") || icon.matches(".*([/]?)([n]?)ip([0-9]{0,3})((.png)?).*".toRegex())) {
            context.getString(R.string.weather_hail)
        } else if (icon.contains("wind_bkn") || icon.contains("wind_ovc") || icon.contains("wind_sct") || icon.contains("wind")) {
            context.getString(R.string.weather_windy)
        } else if (icon.contains("ovc")) {
            context.getString(R.string.weather_overcast)
        } else if (icon.contains("sct") || icon.contains("few")) {
            context.getString(R.string.weather_partlycloudy)
        } else if (icon.contains("bkn")) {
            context.getString(R.string.weather_partlycloudy)
        } else if (icon.contains("skc")) {
            context.getString(R.string.weather_clearsky)
        } else {
            super.getWeatherCondition(icon)
        }
    }

    // Some conditions can be for any time of day
    // So use sunrise/set data as fallback
    override fun isNight(weather: Weather): Boolean {
        var isNight = super.isNight(weather)

        // The following cases can be present at any time of day
        if (WeatherIcons.SNOWFLAKE_COLD == weather.condition.icon) {
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
        }

        return isNight
    }
}