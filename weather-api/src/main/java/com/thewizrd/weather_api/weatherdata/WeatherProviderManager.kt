package com.thewizrd.weather_api.weatherdata

import android.location.Location
import androidx.annotation.WorkerThread
import com.thewizrd.shared_resources.appLib
import com.thewizrd.shared_resources.exceptions.WeatherException
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.LocationQuery
import com.thewizrd.shared_resources.locationdata.WeatherLocationProvider
import com.thewizrd.shared_resources.utils.Coordinate
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.WeatherProvider
import com.thewizrd.shared_resources.weatherdata.auth.AuthType
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlert
import com.thewizrd.weather_api.weatherModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeatherProviderManager internal constructor() : WeatherProvider {
    private var _weatherProvider: WeatherProvider? = null

    init {
        updateAPI()
    }

    fun updateAPI() {
        val settingsMgr = appLib.settingsManager
        val API = settingsMgr.getAPI()
        _weatherProvider = getWeatherProvider(API)
    }

    fun getWeatherProvider(@WeatherAPI.WeatherProviders API: String?): WeatherProvider {
        return weatherModule.weatherProviderFactory.getWeatherProvider(API)
    }

    fun isKeyRequired(API: String): Boolean {
        val provider = getWeatherProvider(API)
        return provider.isKeyRequired()
    }

    @Throws(WeatherException::class)
    suspend fun isKeyValid(key: String?, API: String): Boolean {
        val provider = getWeatherProvider(API)
        return withContext(Dispatchers.Default) {
            provider.isKeyValid(key)
        }
    }

    fun getAuthType(API: String): AuthType {
        val provider = getWeatherProvider(API)
        return provider.getAuthType()
    }

    /* WeatherProvider proxy methods */
    override fun getWeatherAPI(): String {
        return _weatherProvider!!.getWeatherAPI()
    }

    override fun isKeyRequired(): Boolean {
        return _weatherProvider!!.isKeyRequired()
    }

    override fun supportsWeatherLocale(): Boolean {
        return _weatherProvider!!.supportsWeatherLocale()
    }

    override fun supportsAlerts(): Boolean {
        return _weatherProvider!!.supportsAlerts()
    }

    override fun needsExternalAlertData(): Boolean {
        return _weatherProvider!!.needsExternalAlertData()
    }

    override fun isRegionSupported(countryCode: String?): Boolean {
        return _weatherProvider!!.isRegionSupported(countryCode)
    }

    override fun getHourlyForecastInterval(): Int {
        return _weatherProvider!!.getHourlyForecastInterval()
    }

    override suspend fun updateLocationData(location: LocationData) {
        _weatherProvider!!.updateLocationData(location)
    }

    override fun updateLocationQuery(weather: Weather): String {
        return _weatherProvider!!.updateLocationQuery(weather)
    }

    override fun updateLocationQuery(location: LocationData): String {
        return _weatherProvider!!.updateLocationQuery(location)
    }

    override fun getLocationProvider(): WeatherLocationProvider {
        return _weatherProvider!!.getLocationProvider()
    }

    @WorkerThread
    @Throws(WeatherException::class)
    override suspend fun getLocations(ac_query: String?): Collection<LocationQuery>? {
        return _weatherProvider!!.getLocations(ac_query)
    }

    @WorkerThread
    @Throws(WeatherException::class)
    override suspend fun getLocation(location: Location): LocationQuery? {
        return _weatherProvider!!.getLocation(Coordinate(location))
    }

    @WorkerThread
    @Throws(WeatherException::class)
    override suspend fun getLocation(coordinate: Coordinate): LocationQuery? {
        return _weatherProvider!!.getLocation(coordinate)
    }

    @WorkerThread
    @Throws(WeatherException::class)
    override suspend fun getWeather(location: LocationData?): Weather {
        return _weatherProvider!!.getWeather(location)
    }

    @WorkerThread
    override suspend fun getAlerts(location: LocationData): Collection<WeatherAlert>? {
        return _weatherProvider!!.getAlerts(location)
    }

    override fun localeToLangCode(iso: String, name: String): String {
        return _weatherProvider!!.localeToLangCode(iso, name)
    }

    override fun getWeatherIcon(icon: String?): String {
        return _weatherProvider!!.getWeatherIcon(icon)
    }

    override fun getWeatherIcon(isNight: Boolean, icon: String?): String {
        return _weatherProvider!!.getWeatherIcon(isNight, icon)
    }

    override fun getWeatherCondition(icon: String?): String {
        return _weatherProvider!!.getWeatherCondition(icon)
    }

    @Throws(WeatherException::class)
    override suspend fun isKeyValid(key: String?): Boolean {
        return _weatherProvider!!.isKeyValid(key)
    }

    override fun getAPIKey(): String? {
        return _weatherProvider!!.getAPIKey()
    }

    override fun isNight(weather: Weather): Boolean {
        return _weatherProvider!!.isNight(weather)
    }

    override fun getAuthType(): AuthType {
        return _weatherProvider!!.getAuthType()
    }
}
