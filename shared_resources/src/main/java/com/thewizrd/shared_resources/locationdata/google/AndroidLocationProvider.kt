package com.thewizrd.shared_resources.locationdata.google

import android.location.Address
import android.location.Geocoder
import android.util.Log
import com.thewizrd.shared_resources.SimpleLibrary
import com.thewizrd.shared_resources.controls.LocationQueryViewModel
import com.thewizrd.shared_resources.locationdata.LocationProviderImpl
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.WeatherAPI.LocationProviders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*
import kotlin.collections.HashSet

open class AndroidLocationProvider : LocationProviderImpl() {
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
    override suspend fun getLocations(ac_query: String?, weatherAPI: String?
    ): Collection<LocationQueryViewModel> = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) {
            throw WeatherException(ErrorStatus.UNKNOWN)
        }

        var locations: Collection<LocationQueryViewModel>? = null
        var wEx: WeatherException? = null

        try {
            val geocoder = Geocoder(SimpleLibrary.instance.appContext, LocaleUtils.getLocale())
            val addresses = geocoder.getFromLocationName(ac_query, 10)

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
            locations = listOf(LocationQueryViewModel())
        }

        return@withContext locations
    }

    @Throws(WeatherException::class)
    override suspend fun getLocationFromID(model: LocationQueryViewModel): LocationQueryViewModel? {
        return null
    }

    @Throws(WeatherException::class)
    override suspend fun getLocationFromName(model: LocationQueryViewModel
    ): LocationQueryViewModel = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) {
            throw WeatherException(ErrorStatus.NETWORKERROR)
        }

        val location: LocationQueryViewModel
        var result: Address?
        var wEx: WeatherException? = null

        try {
            val geocoder = Geocoder(SimpleLibrary.instance.appContext, LocaleUtils.getLocale())
            val addresses = geocoder.getFromLocationName(model.locationName, 1)

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
                ?: LocationQueryViewModel()

        return@withContext location
    }

    @Throws(WeatherException::class)
    override suspend fun getLocation(coordinate: Coordinate, weatherAPI: String?
    ): LocationQueryViewModel = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) {
            throw WeatherException(ErrorStatus.NETWORKERROR)
        }

        val location: LocationQueryViewModel
        var result: Address?
        var wEx: WeatherException? = null

        try {
            val geocoder = Geocoder(SimpleLibrary.instance.appContext, LocaleUtils.getLocale())
            val addresses = geocoder.getFromLocation(coordinate.latitude, coordinate.longitude, 1)

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

        location = result?.let { createLocationModel(it, weatherAPI!!) }
                ?: LocationQueryViewModel()

        return@withContext location
    }

    override suspend fun isKeyValid(key: String?): Boolean {
        return false
    }

    override fun getAPIKey(): String? {
        return null
    }
}