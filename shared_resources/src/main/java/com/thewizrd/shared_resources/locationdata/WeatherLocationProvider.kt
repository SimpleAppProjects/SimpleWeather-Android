package com.thewizrd.shared_resources.locationdata

import com.thewizrd.shared_resources.exceptions.WeatherException
import com.thewizrd.shared_resources.utils.Coordinate
import com.thewizrd.shared_resources.weatherdata.WeatherAPI.LocationProviders

interface WeatherLocationProvider {
    @LocationProviders
    fun getLocationAPI(): String

    fun isKeyRequired(): Boolean

    fun supportsLocale(): Boolean

    fun needsLocationFromID(): Boolean

    fun needsLocationFromName(): Boolean

    fun needsLocationFromGeocoder(): Boolean

    @Throws(WeatherException::class)
    suspend fun getLocations(
        ac_query: String?, weatherAPI: String?
    ): Collection<LocationQuery>

    @Throws(WeatherException::class)
    suspend fun getLocation(coordinate: Coordinate, weatherAPI: String?): LocationQuery?

    @Throws(WeatherException::class)
    suspend fun getLocationFromID(model: LocationQuery): LocationQuery?

    @Throws(WeatherException::class)
    suspend fun getLocationFromName(model: LocationQuery): LocationQuery?

    @Throws(WeatherException::class)
    suspend fun isKeyValid(key: String?): Boolean

    fun getAPIKey(): String?

    fun localeToLangCode(iso: String, name: String): String

    suspend fun updateLocationData(location: LocationData, weatherAPI: String)
}