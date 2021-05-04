package com.thewizrd.shared_resources.locationdata.google

import android.location.Address
import android.location.Geocoder
import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FetchPlaceResponse
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.thewizrd.shared_resources.SimpleLibrary
import com.thewizrd.shared_resources.controls.LocationQueryViewModel
import com.thewizrd.shared_resources.keys.Keys
import com.thewizrd.shared_resources.locationdata.LocationProviderImpl
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.utils.WeatherException
import com.thewizrd.shared_resources.utils.WeatherUtils
import com.thewizrd.shared_resources.utils.WeatherUtils.Coordinate
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.WeatherAPI.LocationAPIs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*
import java.util.concurrent.ExecutionException

class GoogleLocationProvider : LocationProviderImpl() {
    companion object {
        private val BASIC_PLACE_FIELDS = listOf(Place.Field.ADDRESS_COMPONENTS, Place.Field.ADDRESS, Place.Field.LAT_LNG, Place.Field.NAME, Place.Field.TYPES)
    }

    private var autocompleteToken: AutocompleteSessionToken? = null

    @LocationAPIs
    override fun getLocationAPI(): String {
        return WeatherAPI.GOOGLE
    }

    override fun isKeyRequired(): Boolean {
        return false
    }

    override fun supportsLocale(): Boolean {
        return true
    }

    override fun needsLocationFromID(): Boolean {
        return true
    }

    override fun needsLocationFromName(): Boolean {
        return true
    }

    private fun refreshToken() {
        if (autocompleteToken == null) {
            autocompleteToken = AutocompleteSessionToken.newInstance()
        }
    }

    private val placesClient: PlacesClient
        get() {
            val ctx = SimpleLibrary.getInstance().appContext
            Places.initialize(ctx, getAPIKey()!!, LocaleUtils.getLocale())
            return Places.createClient(ctx)
        }

    @Throws(WeatherException::class)
    override suspend fun getLocations(ac_query: String?, weatherAPI: String?
    ): Collection<LocationQueryViewModel> = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) {
            throw WeatherException(WeatherUtils.ErrorStatus.UNKNOWN)
        }

        var locations: Collection<LocationQueryViewModel>? = null
        var wEx: WeatherException? = null

        try {
            // Create a new token for the autocomplete session. Pass this to FindAutocompletePredictionsRequest,
            // and once again when the user makes a selection (for example when calling fetchPlace()).
            refreshToken()

            // Use the builder to create a FindAutocompletePredictionsRequest.
            val request = FindAutocompletePredictionsRequest.builder()
                    .setTypeFilter(TypeFilter.CITIES)
                    .setSessionToken(autocompleteToken)
                    .setQuery(ac_query)
                    .build()

            val response = placesClient.findAutocompletePredictions(request).await()
            locations = HashSet()
            for (result in response.autocompletePredictions) {
                locations.add(LocationQueryViewModel(result, weatherAPI))
            }
        } catch (e: Throwable) {
            var ex = e

            if (ex is ExecutionException && ex.cause is Throwable) {
                ex = ex.cause!!
            }

            if (ex is IOException) {
                wEx = WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR)
            } else if (ex is IllegalArgumentException) {
                wEx = WeatherException(WeatherUtils.ErrorStatus.QUERYNOTFOUND)
            } else if (ex is ApiException) {
                when (ex.statusCode) {
                    CommonStatusCodes.NETWORK_ERROR,
                    CommonStatusCodes.RECONNECTION_TIMED_OUT,
                    CommonStatusCodes.RECONNECTION_TIMED_OUT_DURING_UPDATE,
                    CommonStatusCodes.API_NOT_CONNECTED -> {
                        wEx = WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR)
                    }
                    CommonStatusCodes.ERROR, CommonStatusCodes.INTERNAL_ERROR -> {
                        wEx = WeatherException(WeatherUtils.ErrorStatus.UNKNOWN)
                    }
                }
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
    override suspend fun getLocationFromID(model: LocationQueryViewModel): LocationQueryViewModel =
            withContext(Dispatchers.IO) {
                if (!Geocoder.isPresent()) {
                    throw WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR)
                }

                val location: LocationQueryViewModel
                var response: FetchPlaceResponse? = null
                var wEx: WeatherException? = null

                try {
                    val request = FetchPlaceRequest.builder(model.locationQuery, BASIC_PLACE_FIELDS)
                            .setSessionToken(autocompleteToken)
                            .build()

                    response = placesClient.fetchPlace(request).await()

                    autocompleteToken = null
                } catch (e: Throwable) {
                    var ex = e

                    if (ex is ExecutionException && ex.cause is Throwable) {
                        ex = ex.cause!!
                    }

                    if (ex is IOException) {
                        wEx = WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR)
                    } else if (ex is IllegalArgumentException) {
                        wEx = WeatherException(WeatherUtils.ErrorStatus.QUERYNOTFOUND)
                    } else if (ex is ApiException) {
                        when (ex.statusCode) {
                            CommonStatusCodes.NETWORK_ERROR,
                            CommonStatusCodes.RECONNECTION_TIMED_OUT,
                            CommonStatusCodes.RECONNECTION_TIMED_OUT_DURING_UPDATE,
                            CommonStatusCodes.API_NOT_CONNECTED -> {
                                wEx = WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR)
                            }
                            CommonStatusCodes.ERROR, CommonStatusCodes.INTERNAL_ERROR -> {
                                wEx = WeatherException(WeatherUtils.ErrorStatus.UNKNOWN)
                            }
                        }
                    }
                    Logger.writeLine(Log.ERROR, ex, "GoogleLocationProvider: error getting location")
                }

                if (wEx != null) throw wEx

                location = response?.let { LocationQueryViewModel(it, model.weatherSource) }
                           ?: LocationQueryViewModel()

                return@withContext location
            }

    @Throws(WeatherException::class)
    override suspend fun getLocationFromName(model: LocationQueryViewModel
    ): LocationQueryViewModel = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) {
            throw WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR)
        }

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
                wEx = WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR)
            } else if (ex is IllegalArgumentException) {
                wEx = WeatherException(WeatherUtils.ErrorStatus.QUERYNOTFOUND)
            }
            Logger.writeLine(Log.ERROR, ex, "GoogleLocationProvider: error getting location")
        }

        if (wEx != null) throw wEx

        location = result?.let { LocationQueryViewModel(it, model.weatherSource) }
                   ?: LocationQueryViewModel()

        return@withContext location
    }

    @Throws(WeatherException::class)
    override suspend fun getLocation(coordinate: Coordinate, weatherAPI: String?
    ): LocationQueryViewModel = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) {
            throw WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR)
        }

        val location: LocationQueryViewModel
        var result: Address?
        var wEx: WeatherException? = null

        try {
            val geocoder = Geocoder(SimpleLibrary.getInstance().appContext, LocaleUtils.getLocale())
            val addresses = geocoder.getFromLocation(coordinate.latitude, coordinate.longitude, 1)

            result = addresses[0]
        } catch (ex: Exception) {
            result = null
            if (ex is IOException) {
                wEx = WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR)
            } else if (ex is IllegalArgumentException) {
                wEx = WeatherException(WeatherUtils.ErrorStatus.QUERYNOTFOUND)
            }
            Logger.writeLine(Log.ERROR, ex, "GoogleLocationProvider: error getting location")
        }

        if (wEx != null) throw wEx

        location = result?.let { LocationQueryViewModel(it, weatherAPI) }
                   ?: LocationQueryViewModel()

        return@withContext location
    }

    override suspend fun isKeyValid(key: String?): Boolean {
        return false
    }

    override fun getAPIKey(): String? {
        return Keys.getGPlacesKey()
    }
}