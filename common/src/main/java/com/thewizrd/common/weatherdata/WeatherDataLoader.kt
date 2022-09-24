package com.thewizrd.common.weatherdata

import android.content.Intent
import android.util.Log
import androidx.core.util.ObjectsCompat
import com.ibm.icu.util.ULocale
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.R
import com.thewizrd.shared_resources.appLib
import com.thewizrd.shared_resources.di.localBroadcastManager
import com.thewizrd.shared_resources.exceptions.ErrorStatus
import com.thewizrd.shared_resources.exceptions.WeatherException
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.utils.NumberUtils.getValueOrDefault
import com.thewizrd.shared_resources.weatherdata.model.*
import com.thewizrd.weather_api.weatherModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.coroutines.coroutineContext
import kotlin.math.max

class WeatherDataLoader {
    companion object {
        private const val TAG = "WeatherDataLoader"
    }

    private var location: LocationData

    constructor() {
        location = LocationData()
    }

    constructor(location: LocationData) : this() {
        updateLocation(location)
    }

    fun updateLocation(locationData: LocationData) {
        if (!locationData.isValid) {
            Logger.writeLine(
                Log.WARN,
                "Location: %s",
                JSONParser.serializer(location, LocationData::class.java)
            )
            throw IllegalArgumentException("location")
        }

        this.location = locationData
    }

    private val wm = weatherModule.weatherManager
    private val settingsMgr = appLib.settingsManager

    suspend fun loadWeatherData(request: WeatherRequest): Weather? {
        val result = getWeatherResult(request)

        return when (result) {
            is WeatherResult.Success -> result.data
            is WeatherResult.NoWeather -> null
            is WeatherResult.Error -> throw result.exception
            is WeatherResult.WeatherWithError -> throw result.exception
        }
    }

    suspend fun loadWeatherResult(request: WeatherRequest): WeatherResult {
        return getWeatherResult(request)
    }

    private suspend fun getWeatherResult(request: WeatherRequest): WeatherResult {
        val result: WeatherResult

        try {
            result = if (request.isForceLoadSavedData) {
                loadSavedWeatherData(request, true)
            } else {
                if (request.isForceRefresh) {
                    getWeatherData(request)
                } else {
                    loadWeatherDataInternal(request)
                }
            }

            result.data?.checkForOutdatedObservation(request)
        } catch (wEx: WeatherException) {
            return WeatherResult.Error(wEx)
        }

        Logger.writeLine(
            Log.DEBUG, "%s: Weather data for %s is valid = %s", TAG,
            location.toString(), result.data?.isValid ?: "null"
        )

        return result
    }

    suspend fun loadWeatherAlerts(loadSavedData: Boolean): Collection<WeatherAlert>? {
        var weatherAlerts: Collection<WeatherAlert>? = null

        if (wm.supportsAlerts()) {
            if (wm.needsExternalAlertData()) {
                if (!loadSavedData) {
                    weatherAlerts = wm.getAlerts(location)
                }
            }

            if (weatherAlerts == null) {
                weatherAlerts = settingsMgr.getWeatherAlertData(location.query)
            }

            if (!loadSavedData) {
                saveWeatherAlerts(weatherAlerts)
            }
        }

        return weatherAlerts
    }

    @Throws(WeatherException::class)
    private suspend fun getWeatherData(request: WeatherRequest): WeatherResult {
        var wEx: WeatherException? = null
        var loadedSavedAlertData = false
        var weather: Weather? = null

        // Try to get weather from provider API
        try {
            coroutineContext.ensureActive()

            // Is the timezone valid? If not try to fetch a valid zone id
            if (!wm.isRegionSupported(location.countryCode) && (location.tzLong == "unknown" || location.tzLong == "UTC")) {
                if (location.latitude != 0.0 && location.longitude != 0.0) {
                    val tzId =
                        weatherModule.tzdbService.getTimeZone(location.latitude, location.longitude)
                    if ("unknown" != tzId) {
                        location.tzLong = tzId
                        // Update DB here or somewhere else
                        settingsMgr.updateLocation(location)
                    }
                }
            }

            if (!wm.isRegionSupported(location.countryCode)) {
                // If location data hasn't been updated, try loading weather from the previous provider
                if (!location.weatherSource.isNullOrBlank()) {
                    val provider =
                        weatherModule.weatherManager.getWeatherProvider(location.weatherSource)
                    if (provider.isRegionSupported(location.countryCode)) {
                        weather = provider.getWeather(location)
                    }
                }

                // Nothing to fallback on; error out
                if (weather == null) {
                    Logger.writeLine(
                        Log.WARN,
                        "Location: %s",
                        JSONParser.serializer(location, LocationData::class.java)
                    )
                    throw WeatherException(ErrorStatus.QUERYNOTFOUND).initCause(CustomException(R.string.error_message_weather_region_unsupported))
                }
            } else {
                // Load weather from provider
                weather = wm.getWeather(location)
            }
        } catch (weatherEx: WeatherException) {
            wEx = weatherEx
            weather = null
        } catch (ex: Exception) {
            Logger.writeLine(Log.ERROR, ex, "WeatherDataLoader: error getting weather data")
            weather = null
        }

        if (request.isLoadAlerts && weather != null && wm.supportsAlerts()) {
            if (wm.needsExternalAlertData()) {
                weather.weatherAlerts = wm.getAlerts(location)
            }

            if (weather.weatherAlerts == null) {
                weather.weatherAlerts = settingsMgr.getWeatherAlertData(location.query)
                loadedSavedAlertData = true
            }
        }

        if (request.isShouldSaveData) {
            if (weather != null) {
                // Handle upgrades
                if (location.name.isNullOrBlank() || location.tzLong.isNullOrBlank()) {
                    location.name = weather.location.name
                    location.tzLong = weather.location.tzLong

                    settingsMgr.updateLocation(location)
                }

                if (location.latitude == 0.0 && location.longitude == 0.0 && weather.location?.latitude.getValueOrDefault(
                        0f
                    ) != 0f && weather.location?.longitude.getValueOrDefault(0f) != 0f
                ) {
                    location.latitude = weather.location.latitude.toDouble()
                    location.longitude = weather.location.longitude.toDouble()

                    settingsMgr.updateLocation(location)
                }

                if (location.locationSource.isNullOrBlank()) {
                    location.locationSource = wm.getLocationProvider().getLocationAPI()

                    settingsMgr.updateLocation(location)
                }

                saveWeatherData(weather)
                saveWeatherForecasts(weather)

                if ((request.isLoadAlerts || weather.weatherAlerts != null) && wm.supportsAlerts()) {
                    if (!loadedSavedAlertData) {
                        saveWeatherAlerts(weather.weatherAlerts)
                    }
                }
            } else {
                // Load old data if available and we can't get new data
                val result = loadSavedWeatherData(request, true)

                if (wEx != null) {
                    return result.data?.let {
                        WeatherResult.WeatherWithError(it, true, wEx)
                    } ?: WeatherResult.Error(wEx)
                }
            }
        }

        // Throw exception if we're unable to get any weather data
        if (wEx != null) {
            throw wEx
        } else if (weather == null) {
            throw WeatherException(ErrorStatus.NOWEATHER)
        }

        return weather.toWeatherResult(false)
    }

    private suspend fun loadWeatherDataInternal(request: WeatherRequest): WeatherResult {
        /*
         * If unable to retrieve saved data, data is old, or units don't match
         * Refresh weather data
         */

        Logger.writeLine(Log.DEBUG, "%s: Loading weather data for %s", TAG, location.toString())

        val result = loadSavedWeatherData(request)

        return when (result) {
            is WeatherResult.NoWeather -> {
                if (request.isShouldSaveData) {
                    Logger.writeLine(
                        Log.DEBUG,
                        "%s: Saved weather data invalid for %s",
                        TAG,
                        location.toString()
                    )
                    Logger.writeLine(Log.DEBUG, "%s: Retrieving data from weather provider", TAG)
                    val weather = result.data

                    if (weather != null && weather.source != settingsMgr.getAPI() || location.weatherSource != settingsMgr.getAPI()) {
                        // Only update location data if location region is supported by new API
                        // If not don't update so we can use fallback (previously used API)
                        if (wm.isRegionSupported(location.countryCode)) {
                            // Update location query and source for new API
                            val oldKey = location.query ?: null

                            if (location.latitude != 0.0 && location.longitude != 0.0) {
                                location.query = wm.updateLocationQuery(location)
                            } else if (weather != null) {
                                if (weather.location?.latitude.getValueOrDefault(0f) == 0f || weather.location?.longitude.getValueOrDefault(
                                        0f
                                    ) == 0f
                                ) {
                                    throw WeatherException(ErrorStatus.UNKNOWN)
                                }

                                location.query = wm.updateLocationQuery(weather)
                            } else {
                                throw WeatherException(ErrorStatus.UNKNOWN)
                            }

                            location.weatherSource = settingsMgr.getAPI()

                            // Update database as well
                            if (appLib.isPhone) {
                                if (location.locationType == LocationType.GPS) {
                                    settingsMgr.saveLastGPSLocData(location)
                                    localBroadcastManager.sendBroadcast(Intent(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE))
                                } else {
                                    settingsMgr.updateLocationWithKey(location, oldKey)
                                    localBroadcastManager.sendBroadcast(
                                        Intent(CommonActions.ACTION_WEATHER_UPDATEWIDGETLOCATION)
                                            .putExtra(Constants.WIDGETKEY_OLDKEY, oldKey)
                                            .putExtra(
                                                Constants.WIDGETKEY_LOCATION,
                                                JSONParser.serializer(
                                                    location,
                                                    LocationData::class.java
                                                )
                                            )
                                    )
                                }
                            } else {
                                settingsMgr.updateLocation(location)
                            }
                        }
                    }
                }

                getWeatherData(request)
            }
            else -> result
        }
    }

    private suspend fun loadSavedWeatherData(
        request: WeatherRequest,
        _override: Boolean = false
    ): WeatherResult {
        var weather: Weather?

        // Load weather data
        try {
            coroutineContext.ensureActive()

            weather = settingsMgr.getWeatherData(location.query)

            if (request.isLoadAlerts && weather != null && wm.supportsAlerts()) {
                weather.weatherAlerts = settingsMgr.getWeatherAlertData(location.query)
            }

            coroutineContext.ensureActive()

            if (request.isLoadForecasts && weather != null) {
                val forecasts = settingsMgr.getWeatherForecastData(location.query)
                val hrForecasts = settingsMgr.getHourlyWeatherForecastData(location.query)

                if (forecasts != null) {
                    weather.forecast = forecasts.forecast
                    weather.txtForecast = forecasts.txtForecast
                }
                weather.hrForecast = hrForecasts
            }

            coroutineContext.ensureActive()

            if (_override && weather == null) {
                // If weather is still unavailable try manually searching for it
                weather = settingsMgr.getWeatherDataByCoordinate(location)

                coroutineContext.ensureActive()

                if (request.isLoadAlerts && weather != null && wm.supportsAlerts()) {
                    weather.weatherAlerts = settingsMgr.getWeatherAlertData(location.query)
                }

                coroutineContext.ensureActive()

                if (request.isLoadForecasts && weather != null) {
                    val forecasts = settingsMgr.getWeatherForecastData(location.query)
                    val hrForecasts = settingsMgr.getHourlyWeatherForecastData(location.query)

                    if (forecasts != null) {
                        weather.forecast = forecasts.forecast
                        weather.txtForecast = forecasts.txtForecast
                    }
                    weather.hrForecast = hrForecasts
                }
            }
        } catch (ex: Exception) {
            Logger.writeLine(Log.ERROR, ex, "WeatherDataLoader: error loading saved weather data")
            return WeatherResult.Error(WeatherException(ErrorStatus.NOWEATHER).apply {
                initCause(ex)
            })
        }

        return if (weather.isDataValid(_override)) {
            WeatherResult.Success(weather!!, true)
        } else {
            WeatherResult.NoWeather(weather, true)
        }
    }

    private suspend fun Weather.checkForOutdatedObservation(request: WeatherRequest) {
        val weather = this
        val location = this@WeatherDataLoader.location

        // Check for outdated observation
        val now = ZonedDateTime.now().withZoneSameInstant(location.tzOffset)
        val duraMins =
            if (weather.condition?.observationTime == null) {
                61
            } else {
                Duration.between(weather.condition.observationTime, now).toMinutes()
            }
        if (duraMins > 90) {
            val interval =
                weatherModule.weatherManager.getWeatherProvider(weather.source)
                    .getHourlyForecastInterval()

            val nowHour = now.truncatedTo(ChronoUnit.HOURS)
            var hrf = settingsMgr.getFirstHourlyForecastDataByDate(location.query, nowHour)
            if (hrf == null || Duration.between(now, hrf.date).toHours() > interval * 0.5) {
                val prevHrf = settingsMgr.getFirstHourlyForecastDataByDate(
                    location.query,
                    nowHour.minusHours(interval.toLong())
                )
                if (prevHrf != null) hrf = prevHrf
            }

            if (hrf != null) {
                weather.condition.weather = hrf.condition
                weather.condition.icon = hrf.icon

                weather.condition.tempF = hrf.highF
                weather.condition.tempC = hrf.highC

                weather.condition.windMph = hrf.windMph
                weather.condition.windKph = hrf.windKph
                weather.condition.windDegrees = hrf.windDegrees

                if (hrf.windMph != null) {
                    weather.condition.beaufort = Beaufort(getBeaufortScale(Math.round(hrf.windMph)))
                }
                weather.condition.feelslikeF = hrf.extras?.feelslikeF
                weather.condition.feelslikeC = hrf.extras?.feelslikeC
                weather.condition.uv =
                    if ((hrf.extras?.uvIndex ?: -1f) >= 0) UV(hrf.extras.uvIndex) else null

                weather.condition.observationTime = hrf.date

                if (duraMins > 60 * 6 || weather.condition?.highF == null || weather.condition.highF == weather.condition.lowF) {
                    val fcasts = settingsMgr.getWeatherForecastData(location.query)
                    val fcast = fcasts?.forecast?.find { input ->
                        input != null && input.date.toLocalDate().isEqual(now.toLocalDate())
                    }

                    if (fcast != null) {
                        weather.condition.highF = fcast.highF
                        weather.condition.highC = fcast.highC
                        weather.condition.lowF = fcast.lowF
                        weather.condition.lowC = fcast.lowC
                    } else {
                        weather.condition.highF = 0f
                        weather.condition.highC = 0f
                        weather.condition.lowF = 0f
                        weather.condition.lowC = 0f
                    }
                }

                weather.atmosphere.dewpointF = hrf.extras?.dewpointF
                weather.atmosphere.dewpointC = hrf.extras?.dewpointC
                weather.atmosphere.humidity = hrf.extras?.humidity
                weather.atmosphere.pressureTrend = null
                weather.atmosphere.pressureIn = hrf.extras?.pressureIn
                weather.atmosphere.pressureMb = hrf.extras?.pressureMb
                weather.atmosphere.visibilityMi = hrf.extras?.visibilityMi
                weather.atmosphere.visibilityKm = hrf.extras?.visibilityKm

                if (weather.precipitation != null) {
                    weather.precipitation.pop = hrf.extras?.pop
                    weather.precipitation.cloudiness = hrf.extras?.cloudiness
                    weather.precipitation.qpfRainIn =
                        if ((hrf.extras?.qpfRainIn ?: -1f) >= 0) hrf.extras.qpfRainIn else 0.0f
                    weather.precipitation.qpfRainMm =
                        if ((hrf.extras?.qpfRainMm ?: -1f) >= 0) hrf.extras.qpfRainMm else 0.0f
                    weather.precipitation.qpfSnowIn =
                        if ((hrf.extras?.qpfSnowIn ?: -1f) >= 0) hrf.extras.qpfSnowIn else 0.0f
                    weather.precipitation.qpfSnowCm =
                        if ((hrf.extras?.qpfSnowCm ?: -1f) >= 0) hrf.extras.qpfSnowCm else 0.0f
                }

                if (request.isShouldSaveData) {
                    saveWeatherData(weather)
                }
            }
        }

        // Check for outdated forecasts
        if (!weather.forecast.isNullOrEmpty()) {
            weather.forecast.removeIf { input ->
                input == null || input.date.truncatedTo(ChronoUnit.DAYS)
                    .isBefore(now.toLocalDateTime().truncatedTo(ChronoUnit.DAYS))
            }
        }

        if (!weather.hrForecast.isNullOrEmpty()) {
            weather.hrForecast.removeIf { input ->
                input == null || input.date.truncatedTo(
                    ChronoUnit.HOURS
                ).isBefore(now.truncatedTo(ChronoUnit.HOURS))
            }
        }
    }

    private fun Weather?.isDataValid(overrideTtl: Boolean = false): Boolean {
        val currentLocale = ULocale.forLocale(LocaleUtils.getLocale())
        val locale = wm.localeToLangCode(currentLocale.language, currentLocale.toLanguageTag())

        if (this == null) return false

        var isInvalid = !this.isValid
        val API = settingsMgr.getAPI()

        if (!isInvalid && !ObjectsCompat.equals(this.source, API)) {
            isInvalid = true
        }

        if (!isInvalid && wm.supportsWeatherLocale()) {
            isInvalid = this.locale != locale
        }

        if (overrideTtl || isInvalid) return !isInvalid

        val ttl = max(settingsMgr.getRefreshInterval(), 30)

        // Check file age
        val span = Duration.between(ZonedDateTime.now(), updateTime).abs()
        return span.toMinutes() < ttl
    }

    private suspend fun saveWeatherData(weather: Weather) = withContext(Dispatchers.IO) {
        // Save location query
        weather.query = location.query

        settingsMgr.saveWeatherData(weather)

        if (!appLib.isPhone) {
            settingsMgr.setUpdateTime(
                weather.updateTime.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()
            )
        }
    }

    private suspend fun saveWeatherAlerts(weatherAlerts: Collection<WeatherAlert>) =
        withContext(Dispatchers.IO) {
            // Check for previously saved alerts
            val previousAlerts = settingsMgr.getWeatherAlertData(location.query)

            if (previousAlerts.isNotEmpty()) {
                // If any previous alerts were flagged before as notified
                // make sure to set them here as such
                // bc notified flag gets reset when retrieving weatherdata
                for (alert in weatherAlerts) {
                    for (prevAlert in previousAlerts) {
                        if (prevAlert == alert && prevAlert.isNotified) {
                            alert.isNotified = prevAlert.isNotified
                            break
                        }
                    }
                }
            }

            settingsMgr.saveWeatherAlerts(location, weatherAlerts)
        }

    private suspend fun saveWeatherForecasts(weather: Weather) = withContext(Dispatchers.IO) {
        val forecasts = Forecasts(weather)
        settingsMgr.saveWeatherForecasts(forecasts)

        val hrForecasts = weather.hrForecast?.let { hrfcasts ->
            MutableList(hrfcasts.size) {
                HourlyForecasts(weather.query, hrfcasts[it])
            }
        }

        settingsMgr.saveWeatherForecasts(location.query, hrForecasts)
    }
}