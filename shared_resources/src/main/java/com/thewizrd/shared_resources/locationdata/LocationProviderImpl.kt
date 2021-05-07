package com.thewizrd.shared_resources.locationdata

import android.location.Address
import android.location.Geocoder
import android.util.Log
import com.thewizrd.shared_resources.SimpleLibrary
import com.thewizrd.shared_resources.controls.LocationQueryViewModel
import com.thewizrd.shared_resources.tzdb.TZDBCache
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.weatherdata.WeatherAPI.LocationProviders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

abstract class LocationProviderImpl : LocationProviderImplInterface {
    // Variables
    @LocationProviders
    abstract override fun getLocationAPI(): String

    abstract override fun isKeyRequired(): Boolean

    abstract override fun supportsLocale(): Boolean

    override fun needsLocationFromID(): Boolean {
        return false
    }

    override fun needsLocationFromName(): Boolean {
        return false
    }

    override fun needsLocationFromGeocoder(): Boolean {
        return false
    }

    /**
     * Retrieve a list of locations from the location provider
     *
     * @param ac_query   The AutoComplete query used to search locations
     * @param weatherAPI The weather source to be assigned
     * @return A list of locations matching the query
     * @throws WeatherException Weather Exception
     */
    @Throws(WeatherException::class)
    abstract override suspend fun getLocations(ac_query: String?, weatherAPI: String?
    ): Collection<LocationQueryViewModel>

    /**
     * Retrieve a single (geo)location from the location provider
     *
     * @param coordinate The coordinate used to search the location data
     * @param weatherAPI The weather source to be assigned
     * @return A single location matching the provided coordinate
     * @throws WeatherException Weather Exception
     */
    @Throws(WeatherException::class)
    override suspend fun getLocation(coordinate: Coordinate, weatherAPI: String?
    ): LocationQueryViewModel? = withContext(Dispatchers.IO) {
        if (Geocoder.isPresent()) {
            val location: LocationQueryViewModel
            var result: Address? = null
            var wEx: WeatherException? = null

            try {
                val geocoder = Geocoder(SimpleLibrary.getInstance().appContext, LocaleUtils.getLocale())
                val addresses = geocoder.getFromLocation(coordinate.latitude, coordinate.longitude, 5)

                for (addr in addresses) {
                    if (Math.abs(ConversionMethods.calculateHaversine(coordinate.latitude, coordinate.longitude, addr.latitude, addr.longitude)) <= 100) {
                        result = addr
                        break
                    }
                }

                if (result == null) {
                    result = addresses[0]
                }
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

            location = result?.let { LocationQueryViewModel(it, weatherAPI) }
                       ?: LocationQueryViewModel()

            location.locationSource = getLocationAPI()

            return@withContext location
        }

        return@withContext null
    }

    /**
     * Retrieve a single location using the location id
     *
     * @param model The location model containing location data
     * @return A single location matching the provided location id
     * @throws WeatherException Weather Exception
     */
    @Throws(WeatherException::class)
    abstract override suspend fun getLocationFromID(model: LocationQueryViewModel
    ): LocationQueryViewModel?

    /**
     * Retrieve a single location using the location name
     *
     * @param model The location model containing location data
     * @return A single location matching the provided location id
     * @throws WeatherException Weather Exception
     */
    @Throws(WeatherException::class)
    override suspend fun getLocationFromName(model: LocationQueryViewModel
    ): LocationQueryViewModel? = withContext(Dispatchers.IO) {
        if (Geocoder.isPresent()) {
            val location: LocationQueryViewModel
            var result: Address?
            var wEx: WeatherException? = null

            try {
                val geocoder = Geocoder(SimpleLibrary.getInstance().appContext, LocaleUtils.getLocale())
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

            location = result?.let { LocationQueryViewModel(result, model.weatherSource) }
                       ?: LocationQueryViewModel()

            location.locationSource = getLocationAPI()

            return@withContext location
        }

        return@withContext null
    }

    /**
     * Query the location provider if the provided key is valid
     *
     * @param key Provider key to check
     * @return boolean Is valid or not
     * @throws WeatherException Weather Exception
     */
    @Throws(WeatherException::class)
    abstract override suspend fun isKeyValid(key: String?): Boolean

    abstract override fun getAPIKey(): String?

    /**
     * Refresh/update the location data from the supported location provider
     * and commit update to the database
     *
     * Uses coordinate [LocationData.getLatitude], [LocationData.getLongitude]
     * to query location provider for updated location data
     *
     * @param location Location data to update
     */
    override suspend fun updateLocationData(location: LocationData, weatherAPI: String) {
        var qview: LocationQueryViewModel? = null
        try {
            qview = getLocation(Coordinate(location), weatherAPI)
        } catch (e: WeatherException) {
            Logger.writeLine(Log.ERROR, e)
        }

        if (qview?.locationQuery?.isNotBlank() == true) {
            location.name = qview.locationName
            location.latitude = qview.locationLat
            location.longitude = qview.locationLong
            location.tzLong = qview.locationTZLong
            if (location.tzLong.isNullOrBlank() && location.latitude != 0.0 && location.longitude != 0.0) {
                val tzId = TZDBCache.getTimeZone(location.latitude, location.longitude)
                if ("unknown" != tzId) location.tzLong = tzId
            }
            location.locationSource = qview.locationSource

            // Update DB here or somewhere else
            val app = SimpleLibrary.getInstance().app
            val settingsMgr = app.settingsManager
            if (app.isPhone) {
                settingsMgr.updateLocation(location)
            } else {
                settingsMgr.saveHomeData(location)
            }
        }
    }

    /**
     * Returns the locale code supported by this location provider
     *
     * @param iso See [ULocale.getLanguage]
     * @param name See [ULocale.toLanguageTag]
     * @return The locale code supported by this provider
     */
    override fun localeToLangCode(iso: String, name: String): String {
        return "EN"
    }
}