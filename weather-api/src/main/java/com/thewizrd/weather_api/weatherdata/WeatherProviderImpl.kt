package com.thewizrd.weather_api.weatherdata

import android.location.Location
import android.util.Log
import com.thewizrd.shared_resources.BuildConfig
import com.thewizrd.shared_resources.R
import com.thewizrd.shared_resources.appLib
import com.thewizrd.shared_resources.exceptions.ErrorStatus
import com.thewizrd.shared_resources.exceptions.WeatherException
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.LocationQuery
import com.thewizrd.shared_resources.locationdata.WeatherLocationProvider
import com.thewizrd.shared_resources.preferences.DevSettingsEnabler
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.Coordinate
import com.thewizrd.shared_resources.utils.LocationUtils
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.weatherdata.AirQualityProvider
import com.thewizrd.shared_resources.weatherdata.WeatherProvider
import com.thewizrd.shared_resources.weatherdata.auth.AuthType
import com.thewizrd.shared_resources.weatherdata.model.*
import com.thewizrd.weather_api.aqicn.AQICNData
import com.thewizrd.weather_api.aqicn.AQICNProvider
import com.thewizrd.weather_api.nws.alerts.NWSAlertProvider
import com.thewizrd.weather_api.tomorrow.TomorrowIOWeatherProvider
import com.thewizrd.weather_api.utils.RateLimitedRequest
import com.thewizrd.weather_api.weatherModule
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

abstract class WeatherProviderImpl : WeatherProvider, RateLimitedRequest {
    protected lateinit var mLocationProvider: WeatherLocationProvider

    protected val context
        get() = appLib.context
    protected val settingsManager
        get() = appLib.settingsManager
    protected val httpClient
        get() = sharedDeps.httpClient

    // Variables
    abstract override fun getWeatherAPI(): String

    abstract override fun isKeyRequired(): Boolean

    abstract override fun supportsWeatherLocale(): Boolean

    override fun supportsAlerts(): Boolean {
        return true
    }

    override fun needsExternalAlertData(): Boolean {
        return true
    }

    override fun isRegionSupported(countryCode: String?): Boolean {
        return true
    }

    override fun getHourlyForecastInterval(): Int {
        return 1
    }

    override fun getRetryTime(): Long {
        return 5000
    }

    override fun getAuthType(): AuthType {
        return AuthType.NONE
    }

    /**
     * Retrieve a list of locations from the location provider
     *
     * @param ac_query The AutoComplete query used to search locations
     * @return A list of locations matching the query
     * @throws WeatherException Weather Exception
     */
    @Throws(WeatherException::class)
    override suspend fun getLocations(ac_query: String?): Collection<LocationQuery> {
        return mLocationProvider.getLocations(ac_query, getWeatherAPI())
    }

    /**
     * Retrieve a single location from the location provider
     *
     * @param coordinate The coordinate used to search the location data
     * @return A single location matching the provided coordinate
     * @throws WeatherException Weather Exception
     */
    @Throws(WeatherException::class)
    override suspend fun getLocation(coordinate: Coordinate): LocationQuery? {
        return mLocationProvider.getLocation(coordinate, getWeatherAPI())
    }

    /**
     * Retrieve a single location from the location provider
     *
     * @param location The coordinate used to search the location data
     * @return A single location matching the provided coordinate
     * @throws WeatherException Weather Exception
     */
    @Throws(WeatherException::class)
    override suspend fun getLocation(location: Location): LocationQuery? {
        return mLocationProvider.getLocation(Coordinate(location), getWeatherAPI())
    }

    /**
     * Retrieve weather data from the weather provider
     *
     * @param location_query Location query to retrieve weather data;
     * Query string is defined in [LocationQuery.updateLocationQuery]
     * @param country_code   Country code for the location defined by location_query parameter
     * @return Weather data object
     * @throws WeatherException Weather Exception
     */
    @Throws(WeatherException::class)
    abstract override suspend fun getWeather(location_query: String, country_code: String): Weather

    /**
     * This method is used to update the weather data retrieved with the query
     * (see [WeatherProviderImpl.getWeather])
     *
     *
     * Mostly used to update Weather data with time zone info from [LocationData] or
     * to update [LocationData] if itself is missing TZ data
     *
     * @param location Location Data object
     * @return updated Weather data object
     * @throws WeatherException Weather Exception
     */
    @Throws(WeatherException::class)
    override suspend fun getWeather(location: LocationData?): Weather {
        if (location?.query == null)
            throw WeatherException(ErrorStatus.UNKNOWN)

        val weather = getWeather(location.query, location.countryCode)

        if (location.tzLong.isNullOrBlank()) {
            if (!weather.location.tzLong.isNullOrBlank()) {
                location.tzLong = weather.location.tzLong
            } else if (location.latitude != 0.0 && location.longitude != 0.0) {
                val tzId =
                    weatherModule.tzdbService.getTimeZone(location.latitude, location.longitude)
                if ("unknown" != tzId)
                    location.tzLong = tzId
            }

            // Update DB here or somewhere else
            if (appLib.isPhone) {
                settingsManager.updateLocation(location)
            } else {
                settingsManager.saveHomeData(location)
            }
        }

        if (weather.location.tzLong.isNullOrBlank())
            weather.location.tzLong = location.tzLong

        if (weather.location.name.isNullOrBlank())
            weather.location.name = location.name

        weather.location.latitude = location.latitude.toFloat()
        weather.location.longitude = location.longitude.toFloat()

        // Provider-specifc updates/fixes
        updateWeatherData(location, weather)

        // Additional external data
        if (weather.condition.airQuality == null && weather.aqiForecast == null) {
            if (!BuildConfig.IS_NONGMS) {
                updateAQIData(location, weather)
            } else if (this is AirQualityProvider) {
                val aqiData = this.getAirQualityData(location)
                updateAQIData(location, weather, aqiData)
            }
        }

        if (weather.condition.pollen == null) {
            if (DevSettingsEnabler.isDevSettingsEnabled(context)) {
                weather.condition.pollen = TomorrowIOWeatherProvider().getPollenData(location)
            }
        }

        return weather
    }

    /**
     * Providers weather provider specific updates to the weather object; For example,
     * location tz offset fixes, etc.
     *
     * @param location Location data object
     * @param weather  The weather data to update
     */
    @Throws(WeatherException::class)
    protected abstract suspend fun updateWeatherData(location: LocationData, weather: Weather)

    private suspend fun updateAQIData(location: LocationData, weather: Weather) {
        val aqicnData = AQICNProvider().getAirQualityData(location)
        updateAQIData(location, weather, aqicnData)
    }

    private fun updateAQIData(location: LocationData, weather: Weather, aqiData: AirQualityData?) {
        weather.condition.airQuality = aqiData?.current

        if (aqiData is AQICNData) {
            try {
                if (!aqiData.uviForecast.isNullOrEmpty()) {
                    for (i in aqiData.uviForecast.indices) {
                        val uviData = aqiData.uviForecast[i]
                        val date = LocalDate.parse(
                            uviData.day,
                            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT)
                        )

                        if (i == 0 && weather.condition.uv == null && date.isEqual(weather.condition.observationTime.toLocalDate())) {
                            if (weather.astronomy.sunrise != null && weather.astronomy.sunset != null) {
                                val obsLocalTime = weather.condition.observationTime.toLocalTime()
                                // if before sunrise or after sunset, uv min
                                if (obsLocalTime.isBefore(weather.astronomy.sunrise.toLocalTime()) || obsLocalTime.isAfter(
                                        weather.astronomy.sunset.toLocalTime()
                                    )
                                ) {
                                    weather.condition.uv = UV(uviData.min.toFloat())
                                } else {
                                    val totalSunlightTime =
                                        weather.astronomy.sunset.toEpochSecond(location.tzOffset) - weather.astronomy.sunrise.toEpochSecond(
                                            location.tzOffset
                                        )
                                    val solarNoon =
                                        weather.astronomy.sunrise.plusSeconds(totalSunlightTime)

                                    // If +/- 2hrs within solar noon, UV max
                                    if (Duration.between(solarNoon.toLocalTime(), obsLocalTime)
                                            .abs().toHours() <= 2
                                    ) {
                                        weather.condition.uv = UV(uviData.max.toFloat())
                                    } else { // else uv avg
                                        weather.condition.uv = UV(uviData.avg.toFloat())
                                    }
                                }
                            }
                        }

                        val forecastObj =
                            weather.forecast.find { it.date.toLocalDate().isEqual(date) }
                        if (forecastObj != null && forecastObj.extras?.uvIndex == null) {
                            if (forecastObj.extras == null) {
                                forecastObj.extras = ForecastExtras()
                            }
                            forecastObj.extras.uvIndex = uviData.max.toFloat()
                        }
                    }
                }
            } catch (e: Exception) {
                Logger.writeLine(Log.ERROR, e, "Error parsing AQI data")
            }

            weather.condition.airQuality?.attribution = context.getString(R.string.api_waqi)
        }

        weather.aqiForecast = aqiData?.aqiForecast
    }

    /**
     * Query the alert provider for current available weather alerts (currently US-only supported)
     *
     * @param location The location data used to search for weather alerts
     * @return A collection of weather alerts currently available
     */
    override suspend fun getAlerts(location: LocationData): Collection<WeatherAlert>? {
        return if (LocationUtils.isUS(location.countryCode)) {
            NWSAlertProvider().getAlerts(location)
        } else {
            //WeatherApiProvider().getAlerts(location)
            null
        }
    }

    /**
     * Query the weather provider if the provided key is valid
     *
     * @param key Provider key to check
     * @return boolean Is valid or not
     * @throws WeatherException Weather Exception
     */
    @Throws(WeatherException::class)
    abstract override suspend fun isKeyValid(key: String?): Boolean

    abstract override fun getAPIKey(): String?

    // Utils Methods
    /**
     * Refresh/update the location data from the supported location provider
     * and commit update to the database
     *
     * Uses coordinate [LocationData.getLatitude], [LocationData.getLongitude]
     * to query location provider for updated location data
     *
     * @param location Location data to update
     */
    override suspend fun updateLocationData(location: LocationData) {
        mLocationProvider.updateLocationData(location, getWeatherAPI())
    }

    /**
     * Returns an location query supported by this weather provider
     *
     * @param weather Weather data used to retrieve updated query
     * @return Returns location query supported by this weather provider
     */
    abstract override fun updateLocationQuery(weather: Weather): String

    /**
     * Returns an location query supported by this weather provider
     *
     * @param location Location data used to retrieve updated query
     * @return Returns location query supported by this weather provider
     */
    abstract override fun updateLocationQuery(location: LocationData): String

    /**
     * Returns the locale code supported by this weather provider
     *
     * @param iso See [com.ibm.icu.util.ULocale.getLanguage]
     * @param name See [com.ibm.icu.util.ULocale.toLanguageTag]
     * @return The locale code supported by this provider
     */
    override fun localeToLangCode(iso: String, name: String): String {
        return "EN"
    }

    abstract override fun getWeatherIcon(icon: String?): String

    // Used in some providers for hourly forecast
    override fun getWeatherIcon(isNight: Boolean, icon: String?): String {
        return getWeatherIcon(icon)
    }

    /**
     * Map the passed icon string to a localized weather condition string (if available)
     *
     * @param icon The [WeatherIcons] to map
     * @return A localized weather condition string (if available)
     */
    override fun getWeatherCondition(icon: String?): String {
        return when (icon) {
            WeatherIcons.DAY_SUNNY -> {
                context.getString(R.string.weather_sunny)
            }
            WeatherIcons.NIGHT_CLEAR -> {
                context.getString(R.string.weather_clear)
            }
            WeatherIcons.DAY_SUNNY_OVERCAST,
            WeatherIcons.NIGHT_OVERCAST,
            WeatherIcons.OVERCAST -> {
                context.getString(R.string.weather_overcast)
            }
            WeatherIcons.DAY_PARTLY_CLOUDY,
            WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY -> {
                context.getString(R.string.weather_partlycloudy)
            }
            WeatherIcons.DAY_CLOUDY,
            WeatherIcons.NIGHT_ALT_CLOUDY,
            WeatherIcons.CLOUDY,
            WeatherIcons.NIGHT_ALT_CLOUDY_HIGH,
            WeatherIcons.DAY_CLOUDY_HIGH -> {
                context.getString(R.string.weather_cloudy)
            }
            WeatherIcons.DAY_SPRINKLE,
            WeatherIcons.NIGHT_ALT_SPRINKLE,
            WeatherIcons.SPRINKLE,
            WeatherIcons.DAY_SHOWERS,
            WeatherIcons.NIGHT_ALT_SHOWERS,
            WeatherIcons.SHOWERS -> {
                context.getString(R.string.weather_rainshowers)
            }
            WeatherIcons.DAY_THUNDERSTORM,
            WeatherIcons.NIGHT_ALT_THUNDERSTORM,
            WeatherIcons.THUNDERSTORM,
            WeatherIcons.DAY_STORM_SHOWERS,
            WeatherIcons.NIGHT_ALT_STORM_SHOWERS -> {
                context.getString(R.string.weather_tstorms)
            }
            WeatherIcons.DAY_SLEET,
            WeatherIcons.NIGHT_ALT_SLEET,
            WeatherIcons.SLEET -> {
                context.getString(R.string.weather_sleet)
            }
            WeatherIcons.DAY_SNOW,
            WeatherIcons.NIGHT_ALT_SNOW,
            WeatherIcons.SNOW -> {
                context.getString(R.string.weather_snow)
            }
            WeatherIcons.DAY_SNOW_WIND,
            WeatherIcons.NIGHT_ALT_SNOW_WIND,
            WeatherIcons.SNOW_WIND -> {
                context.getString(R.string.weather_heavysnow)
            }
            WeatherIcons.DAY_SNOW_THUNDERSTORM,
            WeatherIcons.NIGHT_ALT_SNOW_THUNDERSTORM,
            WeatherIcons.SNOW_THUNDERSTORM -> {
                context.getString(R.string.weather_snow_tstorms)
            }
            WeatherIcons.HAIL,
            WeatherIcons.DAY_HAIL,
            WeatherIcons.NIGHT_ALT_HAIL -> {
                context.getString(R.string.weather_hail)
            }
            WeatherIcons.DAY_RAIN,
            WeatherIcons.NIGHT_ALT_RAIN,
            WeatherIcons.RAIN -> {
                context.getString(R.string.weather_rain)
            }
            WeatherIcons.DAY_FOG,
            WeatherIcons.NIGHT_FOG,
            WeatherIcons.FOG -> {
                context.getString(R.string.weather_fog)
            }
            WeatherIcons.DAY_SLEET_STORM,
            WeatherIcons.NIGHT_ALT_SLEET_STORM,
            WeatherIcons.SLEET_STORM -> {
                context.getString(R.string.weather_sleet_tstorms)
            }
            WeatherIcons.SNOWFLAKE_COLD -> context.getString(R.string.weather_cold)
            WeatherIcons.DAY_HOT,
            WeatherIcons.NIGHT_HOT,
            WeatherIcons.HOT -> {
                context.getString(R.string.weather_hot)
            }
            WeatherIcons.DAY_HAZE,
            WeatherIcons.NIGHT_HAZE,
            WeatherIcons.HAZE -> {
                context.getString(R.string.weather_haze)
            }
            WeatherIcons.SMOKE -> context.getString(R.string.weather_smoky)
            WeatherIcons.SANDSTORM, WeatherIcons.DUST -> context.getString(R.string.weather_dust)
            WeatherIcons.TORNADO -> context.getString(R.string.weather_tornado)
            WeatherIcons.DAY_RAIN_MIX,
            WeatherIcons.NIGHT_ALT_RAIN_MIX,
            WeatherIcons.RAIN_MIX -> {
                context.getString(R.string.weather_rainandsnow)
            }
            WeatherIcons.DAY_WINDY,
            WeatherIcons.NIGHT_WINDY,
            WeatherIcons.WINDY,
            WeatherIcons.DAY_CLOUDY_WINDY,
            WeatherIcons.NIGHT_ALT_CLOUDY_WINDY,
            WeatherIcons.CLOUDY_WINDY,
            WeatherIcons.DAY_CLOUDY_GUSTS,
            WeatherIcons.NIGHT_ALT_CLOUDY_GUSTS,
            WeatherIcons.CLOUDY_GUSTS,
            WeatherIcons.STRONG_WIND -> {
                context.getString(R.string.weather_windy)
            }
            WeatherIcons.HURRICANE -> context.getString(R.string.weather_tropicalstorm)
            else -> context.getString(R.string.weather_notavailable)
        }
    }

    override fun isNight(weather: Weather): Boolean {
        var isNight = false

        when (weather.condition.icon) {
            WeatherIcons.NIGHT_CLEAR,
            WeatherIcons.NIGHT_ALT_CLOUDY,
            WeatherIcons.NIGHT_ALT_CLOUDY_GUSTS,
            WeatherIcons.NIGHT_ALT_CLOUDY_WINDY,
            WeatherIcons.NIGHT_FOG,
            WeatherIcons.NIGHT_ALT_HAIL,
            WeatherIcons.NIGHT_HAZE,
            WeatherIcons.NIGHT_ALT_LIGHTNING,
            WeatherIcons.NIGHT_ALT_RAIN,
            WeatherIcons.NIGHT_ALT_RAIN_MIX,
            WeatherIcons.NIGHT_ALT_RAIN_WIND,
            WeatherIcons.NIGHT_ALT_SHOWERS,
            WeatherIcons.NIGHT_ALT_SLEET,
            WeatherIcons.NIGHT_ALT_SLEET_STORM,
            WeatherIcons.NIGHT_ALT_SNOW,
            WeatherIcons.NIGHT_ALT_SNOW_THUNDERSTORM,
            WeatherIcons.NIGHT_ALT_SNOW_WIND,
            WeatherIcons.NIGHT_ALT_SPRINKLE,
            WeatherIcons.NIGHT_ALT_STORM_SHOWERS,
            WeatherIcons.NIGHT_OVERCAST,
            WeatherIcons.NIGHT_ALT_THUNDERSTORM,
            WeatherIcons.NIGHT_WINDY,
            WeatherIcons.NIGHT_HOT,
            WeatherIcons.NIGHT_ALT_CLOUDY_HIGH,
            WeatherIcons.NIGHT_LIGHT_WIND,
            WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY -> isNight = true
        }

        return isNight
    }

    override fun getLocationProvider(): WeatherLocationProvider {
        return mLocationProvider
    }
}