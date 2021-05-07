package com.thewizrd.shared_resources.weatherdata

import android.location.Location
import androidx.annotation.WorkerThread
import com.thewizrd.shared_resources.BuildConfig
import com.thewizrd.shared_resources.SimpleLibrary
import com.thewizrd.shared_resources.controls.LocationQueryViewModel
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.LocationProviderImpl
import com.thewizrd.shared_resources.utils.Coordinate
import com.thewizrd.shared_resources.utils.WeatherException
import com.thewizrd.shared_resources.weatherdata.here.HEREWeatherProvider
import com.thewizrd.shared_resources.weatherdata.metno.MetnoWeatherProvider
import com.thewizrd.shared_resources.weatherdata.nws.NWSWeatherProvider
import com.thewizrd.shared_resources.weatherdata.openweather.OpenWeatherMapProvider
import com.thewizrd.shared_resources.weatherdata.weatherunlocked.WeatherUnlockedProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeatherManager private constructor() : WeatherProviderImplInterface {
    companion object {
        private var sInstance: WeatherManager? = null
        private var sWeatherProvider: WeatherProviderImpl? = null

        @JvmStatic
        val instance: WeatherManager
            get() {
                if (sInstance == null) {
                    sInstance = WeatherManager()
                }

                return sInstance!!
            }

        @JvmStatic
        fun getProvider(API: String?): WeatherProviderImpl {
            var providerImpl: WeatherProviderImpl? = null

            when (API) {
                WeatherAPI.HERE -> providerImpl = HEREWeatherProvider()
                WeatherAPI.OPENWEATHERMAP -> providerImpl = OpenWeatherMapProvider()
                WeatherAPI.METNO -> providerImpl = MetnoWeatherProvider()
                WeatherAPI.NWS -> providerImpl = NWSWeatherProvider()
                WeatherAPI.WEATHERUNLOCKED -> providerImpl = WeatherUnlockedProvider()
                else -> {
                    if (!BuildConfig.DEBUG) {
                        providerImpl = WeatherUnlockedProvider()
                    }
                }
            }

            requireNotNull(providerImpl) { "Argument API: Invalid API name! This API is not supported" }

            return providerImpl
        }

        @JvmStatic
        fun isKeyRequired(API: String): Boolean {
            val provider = getProvider(API)
            return provider.isKeyRequired()
        }

        @Throws(WeatherException::class)
        @JvmStatic
        suspend fun isKeyValid(key: String?, API: String): Boolean {
            val provider = getProvider(API)
            return withContext(Dispatchers.Default) {
                provider.isKeyValid(key)
            }
        }
    }

    init {
        updateAPI()
    }

    fun updateAPI() {
        val settingsMgr = SimpleLibrary.getInstance().app.settingsManager
        val API = settingsMgr.getAPI()
        sWeatherProvider = getProvider(API)
    }

    // Provider dependent methods
    override fun getWeatherAPI(): String {
        return sWeatherProvider!!.getWeatherAPI()
    }

    override fun isKeyRequired(): Boolean {
        return sWeatherProvider!!.isKeyRequired()
    }

    override fun supportsWeatherLocale(): Boolean {
        return sWeatherProvider!!.supportsWeatherLocale()
    }

    override fun supportsAlerts(): Boolean {
        return sWeatherProvider!!.supportsAlerts()
    }

    override fun needsExternalAlertData(): Boolean {
        return sWeatherProvider!!.needsExternalAlertData()
    }

    override fun getHourlyForecastInterval(): Int {
        return sWeatherProvider!!.getHourlyForecastInterval()
    }

    override suspend fun updateLocationData(location: LocationData) {
        sWeatherProvider!!.updateLocationData(location)
    }

    override fun updateLocationQuery(weather: Weather): String {
        return sWeatherProvider!!.updateLocationQuery(weather)
    }

    override fun updateLocationQuery(location: LocationData): String {
        return sWeatherProvider!!.updateLocationQuery(location)
    }

    override fun getLocationProvider(): LocationProviderImpl {
        return sWeatherProvider!!.getLocationProvider()
    }

    @WorkerThread
    @Throws(WeatherException::class)
    override suspend fun getLocations(ac_query: String?): Collection<LocationQueryViewModel> {
        return sWeatherProvider!!.getLocations(ac_query)
    }

    @WorkerThread
    @Throws(WeatherException::class)
    override suspend fun getLocation(location: Location): LocationQueryViewModel? {
        return sWeatherProvider!!.getLocation(Coordinate(location))
    }

    @WorkerThread
    @Throws(WeatherException::class)
    override suspend fun getLocation(coordinate: Coordinate): LocationQueryViewModel? {
        return sWeatherProvider!!.getLocation(coordinate)
    }

    @WorkerThread
    @Throws(WeatherException::class)
    override suspend fun getWeather(location_query: String, country_code: String): Weather {
        return sWeatherProvider!!.getWeather(location_query, country_code)
    }

    @WorkerThread
    @Throws(WeatherException::class)
    override suspend fun getWeather(location: LocationData?): Weather {
        return sWeatherProvider!!.getWeather(location)
    }

    @WorkerThread
    override suspend fun getAlerts(location: LocationData): Collection<WeatherAlert>? {
        return sWeatherProvider!!.getAlerts(location)
    }

    override fun localeToLangCode(iso: String, name: String): String {
        return sWeatherProvider!!.localeToLangCode(iso, name)
    }

    override fun getWeatherIcon(icon: String?): String {
        return sWeatherProvider!!.getWeatherIcon(icon)
    }

    override fun getWeatherIcon(isNight: Boolean, icon: String?): String {
        return sWeatherProvider!!.getWeatherIcon(isNight, icon)
    }

    override fun getWeatherCondition(icon: String?): String {
        return sWeatherProvider!!.getWeatherCondition(icon)
    }

    @Throws(WeatherException::class)
    override suspend fun isKeyValid(key: String?): Boolean {
        return sWeatherProvider!!.isKeyValid(key)
    }

    override fun getAPIKey(): String? {
        return sWeatherProvider!!.getAPIKey()
    }

    override fun isNight(weather: Weather): Boolean {
        return sWeatherProvider!!.isNight(weather)
    }
}
