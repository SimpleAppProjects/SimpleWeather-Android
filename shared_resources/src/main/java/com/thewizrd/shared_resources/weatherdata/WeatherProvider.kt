package com.thewizrd.shared_resources.weatherdata

import android.location.Location
import com.thewizrd.shared_resources.exceptions.WeatherException
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.LocationQuery
import com.thewizrd.shared_resources.locationdata.WeatherLocationProvider
import com.thewizrd.shared_resources.utils.Coordinate
import com.thewizrd.shared_resources.weatherdata.auth.AuthType
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlert

interface WeatherProvider {
    @WeatherAPI.WeatherProviders
    fun getWeatherAPI(): String

    fun isKeyRequired(): Boolean

    fun supportsWeatherLocale(): Boolean

    fun supportsAlerts(): Boolean

    fun needsExternalAlertData(): Boolean

    fun isRegionSupported(countryCode: String?): Boolean

    fun getHourlyForecastInterval(): Int

    @Throws(WeatherException::class)
    suspend fun getLocations(ac_query: String?): Collection<LocationQuery>?

    @Throws(WeatherException::class)
    suspend fun getLocation(coordinate: Coordinate): LocationQuery?

    @Throws(WeatherException::class)
    suspend fun getLocation(location: Location): LocationQuery?

    @Throws(WeatherException::class)
    suspend fun getWeather(location: LocationData?): Weather

    suspend fun getAlerts(location: LocationData): Collection<WeatherAlert>?

    fun getWeatherIcon(icon: String?): String

    fun getWeatherIcon(isNight: Boolean, icon: String?): String

    fun getWeatherCondition(icon: String?): String

    fun getAuthType(): AuthType

    @Throws(WeatherException::class)
    suspend fun isKeyValid(key: String?): Boolean

    fun getAPIKey(): String?

    fun isNight(weather: Weather): Boolean

    fun localeToLangCode(iso: String, name: String): String

    suspend fun updateLocationData(location: LocationData)

    fun updateLocationQuery(weather: Weather): String

    fun updateLocationQuery(location: LocationData): String

    fun getLocationProvider(): WeatherLocationProvider
}