package com.thewizrd.weather_api.google.location

import android.location.Address
import android.location.Geocoder
import android.util.Log
import com.thewizrd.shared_resources.exceptions.ErrorStatus
import com.thewizrd.shared_resources.exceptions.WeatherException
import com.thewizrd.shared_resources.locationdata.LocationQuery
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.WeatherAPI.LocationProviders
import com.thewizrd.weather_api.locationdata.WeatherLocationProviderImpl
import com.thewizrd.weather_api.weatherModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*

open class AndroidLocationProvider : WeatherLocationProviderImpl() {
    @LocationProviders
    override fun getLocationAPI(): String {
        return WeatherAPI.ANDROID
    }

    override fun isKeyRequired(): Boolean {
        return false
    }

    override fun supportsLocale(): Boolean {
        return true
    }

    override fun needsLocationFromID(): Boolean {
        return false
    }

    override fun needsLocationFromName(): Boolean {
        return true
    }

    @Throws(WeatherException::class)
    override suspend fun getLocations(
        ac_query: String?, weatherAPI: String?
    ): Collection<LocationQuery> = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) {
            throw WeatherException(ErrorStatus.UNKNOWN)
        }

        var locations: Collection<LocationQuery>? = null
        var wEx: WeatherException? = null

        try {
            val addresses = weatherModule.geocoder.getFromLocationNameAsync(ac_query!!, 10)

            locations = HashSet()
            for (result in addresses) {
                locations.add(createLocationModel(result, weatherAPI!!))
            }
        } catch (ex: Exception) {
            locations = null
            if (ex is IOException) {
                wEx = WeatherException(ErrorStatus.NETWORKERROR)
            } else if (ex is IllegalArgumentException) {
                wEx = WeatherException(ErrorStatus.QUERYNOTFOUND)
            }
            Logger.writeLine(Log.ERROR, ex, "GoogleLocationProvider: error getting location")
        }

        if (wEx != null) throw wEx

        if (locations.isNullOrEmpty()) {
            locations = listOf(LocationQuery())
        }

        return@withContext locations
    }

    @Throws(WeatherException::class)
    override suspend fun getLocationFromID(model: LocationQuery): LocationQuery? {
        return null
    }

    @Throws(WeatherException::class)
    override suspend fun getLocationFromName(
        model: LocationQuery
    ): LocationQuery = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) {
            throw WeatherException(ErrorStatus.NETWORKERROR)
        }

        val location: LocationQuery
        var result: Address?
        var wEx: WeatherException? = null

        try {
            val addresses = weatherModule.geocoder.getFromLocationNameAsync(model.locationName!!, 1)

            result = addresses[0]
        } catch (ex: Exception) {
            result = null
            if (ex is IOException) {
                wEx = WeatherException(ErrorStatus.NETWORKERROR)
            } else if (ex is IllegalArgumentException) {
                wEx = WeatherException(ErrorStatus.QUERYNOTFOUND)
            }
            Logger.writeLine(Log.ERROR, ex, "GoogleLocationProvider: error getting location")
        }

        if (wEx != null) throw wEx

        location = result?.let { createLocationModel(it, model.weatherSource) }
            ?: LocationQuery()

        return@withContext location
    }

    @Throws(WeatherException::class)
    override suspend fun getLocation(
        coordinate: Coordinate, weatherAPI: String?
    ): LocationQuery = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) {
            throw WeatherException(ErrorStatus.NETWORKERROR)
        }

        val location: LocationQuery
        var result: Address?
        var wEx: WeatherException? = null

        try {
            val addresses = weatherModule.geocoder.getFromLocationAsync(
                coordinate.latitude,
                coordinate.longitude,
                1
            )

            result = addresses[0]
        } catch (ex: Exception) {
            result = null
            if (ex is IOException) {
                wEx = WeatherException(ErrorStatus.NETWORKERROR)
            } else if (ex is IllegalArgumentException) {
                wEx = WeatherException(ErrorStatus.QUERYNOTFOUND)
            }
            Logger.writeLine(Log.ERROR, ex, "GoogleLocationProvider: error getting location")
        }

        if (wEx != null) throw wEx

        location = result?.let { createLocationModel(it, weatherAPI) }
            ?: LocationQuery()

        return@withContext location
    }

    override suspend fun isKeyValid(key: String?): Boolean {
        return false
    }

    override fun getAPIKey(): String? {
        return null
    }
}