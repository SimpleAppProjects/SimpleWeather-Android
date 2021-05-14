package com.thewizrd.shared_resources.weatherdata

import android.location.Location
import com.thewizrd.shared_resources.controls.LocationQueryViewModel
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.LocationProviderImpl
import com.thewizrd.shared_resources.utils.Coordinate
import com.thewizrd.shared_resources.utils.WeatherException
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlert

interface WeatherProviderImplInterface {
    @WeatherAPI.WeatherProviders
    fun getWeatherAPI(): String

    fun isKeyRequired(): Boolean

    fun supportsWeatherLocale(): Boolean

    fun supportsAlerts(): Boolean

    fun needsExternalAlertData(): Boolean

    fun isRegionSupported(countryCode: String?): Boolean

    fun getHourlyForecastInterval(): Int

    @Throws(WeatherException::class)
    suspend fun getLocations(ac_query: String?): Collection<LocationQueryViewModel>?

    @Throws(WeatherException::class)
    suspend fun getLocation(coordinate: Coordinate): LocationQueryViewModel?

    @Throws(WeatherException::class)
    suspend fun getLocation(location: Location): LocationQueryViewModel?

    @Throws(WeatherException::class)
    suspend fun getWeather(location_query: String, country_code: String): Weather

    @Throws(WeatherException::class)
    suspend fun getWeather(location: LocationData?): Weather

    suspend fun getAlerts(location: LocationData): Collection<WeatherAlert?>?

    fun getWeatherIcon(icon: String?): String

    fun getWeatherIcon(isNight: Boolean, icon: String?): String

    fun getWeatherCondition(icon: String?): String

    @Throws(WeatherException::class)
    suspend fun isKeyValid(key: String?): Boolean

    fun getAPIKey(): String?

    fun isNight(weather: Weather): Boolean

    fun localeToLangCode(iso: String, name: String): String

    suspend fun updateLocationData(location: LocationData)

    fun updateLocationQuery(weather: Weather): String

    fun updateLocationQuery(location: LocationData): String

    fun getLocationProvider(): LocationProviderImpl
}