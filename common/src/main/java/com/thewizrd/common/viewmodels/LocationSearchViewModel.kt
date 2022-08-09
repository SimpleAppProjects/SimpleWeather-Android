package com.thewizrd.common.viewmodels

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thewizrd.common.BuildConfig
import com.thewizrd.common.R
import com.thewizrd.common.location.LocationProvider
import com.thewizrd.common.location.LocationResult
import com.thewizrd.common.utils.ErrorMessage
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.exceptions.WeatherException
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.LocationQuery
import com.thewizrd.shared_resources.locationdata.toLocationData
import com.thewizrd.shared_resources.remoteconfig.remoteConfigService
import com.thewizrd.shared_resources.utils.Coordinate
import com.thewizrd.weather_api.weatherModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface LocationSearchResult {
    val data: LocationData?

    data class Success(
        override val data: LocationData
    ) : LocationSearchResult

    data class AlreadyExists(
        override val data: LocationData
    ) : LocationSearchResult

    data class Failed(
        override val data: LocationData?
    ) : LocationSearchResult
}

data class LocationSearchUiState(
    val isLoading: Boolean = false,
    val errorMessages: List<ErrorMessage> = emptyList(),
    val locations: List<LocationQuery> = emptyList(),
    val currentLocation: LocationData? = null,
    val selectedSearchLocation: LocationSearchResult? = null
)

class LocationSearchViewModel(app: Application) : AndroidViewModel(app) {
    private val viewModelState = MutableStateFlow(LocationSearchUiState())
    private var searchJob: Job? = null

    private val locationProvider = LocationProvider(app)
    private val wm = weatherModule.weatherManager

    val uiState = viewModelState.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        viewModelState.value
    )

    val errorMessages = viewModelState.map {
        it.errorMessages
    }.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        viewModelState.value.errorMessages
    )

    val currentLocation = viewModelState.map {
        it.currentLocation
    }.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        viewModelState.value.currentLocation
    )

    val selectedSearchLocation = viewModelState.map {
        it.selectedSearchLocation
    }.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        viewModelState.value.selectedSearchLocation
    )

    val locations = viewModelState.map {
        it.locations
    }.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        viewModelState.value.locations
    )

    val isLoading = viewModelState.map {
        it.isLoading
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        viewModelState.value.isLoading
    )

    fun fetchGeoLocation() {
        viewModelState.update {
            it.copy(isLoading = true)
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch(Dispatchers.Default) {
            val result = locationProvider.getLatestLocationData()
            var currentLocation: LocationData? = null

            when (result) {
                is LocationResult.Changed -> {
                    currentLocation = result.data
                }
                is LocationResult.PermissionDenied -> {
                    postErrorMessage(R.string.error_location_denied)
                }
                is LocationResult.Error -> {
                    postErrorMessage(result.errorMessage)
                }
                else -> {
                    postErrorMessage(R.string.error_retrieve_location)
                }
            }

            if (currentLocation?.isValid == true) {
                val locQuery = LocationQuery(currentLocation)

                if (!settingsManager.isWeatherLoaded() && !BuildConfig.IS_NONGMS) {
                    // Set default provider based on location
                    val provider =
                        remoteConfigService.getDefaultWeatherProvider(locQuery.locationCountry)
                    settingsManager.setAPI(provider)
                    locQuery.updateWeatherSource(provider)
                }

                if (settingsManager.usePersonalKey() && settingsManager.getAPIKey()
                        .isNullOrBlank() && wm.isKeyRequired()
                ) {
                    postErrorMessage(R.string.werror_invalidkey)
                    viewModelState.update {
                        it.copy(isLoading = false)
                    }
                    return@launch
                }

                if (!wm.isRegionSupported(locQuery.locationCountry)) {
                    postErrorMessage(R.string.error_message_weather_region_unsupported)
                    viewModelState.update {
                        it.copy(isLoading = false)
                    }
                    return@launch
                }

                viewModelState.update {
                    it.copy(currentLocation = currentLocation)
                }
            }

            viewModelState.update {
                it.copy(isLoading = false)
            }
        }
    }

    fun fetchLocations(queryString: String?) {
        viewModelState.update {
            it.copy(isLoading = true)
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch(Dispatchers.Default) {
            if (!queryString.isNullOrBlank()) {
                try {
                    val results = wm.getLocations(queryString)

                    viewModelState.update {
                        it.copy(locations = results?.toList() ?: emptyList())
                    }
                } catch (e: Exception) {
                    if (e is WeatherException) {
                        postErrorMessage(ErrorMessage.WeatherError(e))
                    }

                    viewModelState.update {
                        it.copy(locations = emptyList())
                    }
                }
            } else {
                viewModelState.update {
                    it.copy(locations = emptyList())
                }
            }

            viewModelState.update {
                it.copy(isLoading = false)
            }
        }
    }

    fun onLocationSelected(locQuery: LocationQuery) {
        viewModelState.update {
            it.copy(isLoading = true)
        }

        viewModelScope.launch {
            if (locQuery.locationQuery.isNullOrBlank()) {
                postErrorMessage(R.string.error_retrieve_location)
                viewModelState.update {
                    it.copy(isLoading = false)
                }
                return@launch
            }

            if (settingsManager.usePersonalKey() && settingsManager.getAPIKey()
                    .isNullOrBlank() && wm.isKeyRequired()
            ) {
                postErrorMessage(R.string.werror_invalidkey)
                viewModelState.update {
                    it.copy(isLoading = false)
                }
                return@launch
            }

            var queryResult: LocationQuery? = locQuery

            // Need to get FULL location data for HERE API
            // Data provided is incomplete
            if (wm.getLocationProvider().needsLocationFromID()) {
                queryResult = withContext(Dispatchers.IO) {
                    wm.getLocationProvider().getLocationFromID(locQuery)
                }
            } else if (wm.getLocationProvider().needsLocationFromName()) {
                queryResult = withContext(Dispatchers.IO) {
                    wm.getLocationProvider().getLocationFromName(locQuery)
                }
            } else if (wm.getLocationProvider().needsLocationFromGeocoder()) {
                queryResult = withContext(Dispatchers.IO) {
                    wm.getLocationProvider().getLocation(
                        Coordinate(locQuery.locationLat, locQuery.locationLong),
                        locQuery.weatherSource
                    )
                }
            }

            if (queryResult == null || queryResult.locationQuery.isNullOrBlank()) {
                // Stop since there is no valid query
                postErrorMessage(R.string.error_retrieve_location)
                viewModelState.update {
                    it.copy(isLoading = false)
                }
                return@launch
            } else if (queryResult.locationTZLong.isNullOrBlank() && queryResult.locationLat != 0.0 && queryResult.locationLong != 0.0) {
                val tzId =
                    weatherModule.tzdbService.getTimeZone(
                        queryResult.locationLat,
                        queryResult.locationLong
                    )
                if ("unknown" != tzId)
                    queryResult.locationTZLong = tzId
            }

            if (!settingsManager.isWeatherLoaded() && !BuildConfig.IS_NONGMS) {
                // Set default provider based on location
                val provider =
                    remoteConfigService.getDefaultWeatherProvider(queryResult.locationCountry)
                settingsManager.setAPI(provider)
                queryResult.updateWeatherSource(provider)
            }

            if (!wm.isRegionSupported(queryResult.locationCountry)) {
                postErrorMessage(R.string.error_message_weather_region_unsupported)
                viewModelState.update {
                    it.copy(isLoading = false)
                }
                return@launch
            }

            // Check if location already exists
            val locData = settingsManager.getLocationData()
            val loc = locData.find { input -> input.query == queryResult.locationQuery }

            val result = if (loc == null) {
                // Location does not exist
                LocationSearchResult.Success(queryResult.toLocationData())
            } else {
                LocationSearchResult.AlreadyExists(loc)
            }

            viewModelState.update {
                it.copy(selectedSearchLocation = result, isLoading = false)
            }
        }
    }

    private fun postErrorMessage(@StringRes resId: Int) {
        viewModelState.update {
            val errorMessages = it.errorMessages + ErrorMessage.Resource(resId)
            it.copy(errorMessages = errorMessages)
        }
    }

    private fun postErrorMessage(message: String) {
        viewModelState.update {
            val errorMessages = it.errorMessages + ErrorMessage.String(message)
            it.copy(errorMessages = errorMessages)
        }
    }

    private fun postErrorMessage(error: ErrorMessage) {
        viewModelState.update {
            val errorMessages = it.errorMessages + error
            it.copy(errorMessages = errorMessages)
        }
    }

    fun setErrorMessageShown(error: ErrorMessage) {
        viewModelState.update { state ->
            state.copy(
                errorMessages = state.errorMessages.filterNot { it == error }
            )
        }
    }
}