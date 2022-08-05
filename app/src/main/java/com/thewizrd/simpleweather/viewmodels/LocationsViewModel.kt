package com.thewizrd.simpleweather.viewmodels

import android.app.Application
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thewizrd.common.helpers.locationPermissionEnabled
import com.thewizrd.common.location.LocationProvider
import com.thewizrd.common.location.LocationResult
import com.thewizrd.common.utils.ErrorMessage
import com.thewizrd.common.weatherdata.WeatherDataLoader
import com.thewizrd.common.weatherdata.WeatherRequest
import com.thewizrd.common.weatherdata.WeatherResult
import com.thewizrd.shared_resources.di.localBroadcastManager
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.exceptions.ErrorStatus
import com.thewizrd.shared_resources.exceptions.WeatherException
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.CommonActions
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.controls.LocationPanelUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LocationsUiState(
    val locations: Collection<LocationPanelUiModel> = emptyList(),
    val errorMessages: List<ErrorMessage> = emptyList(),
    val isLoading: Boolean = false
)

class LocationsViewModel(app: Application) : AndroidViewModel(app) {
    private val locationProvider = LocationProvider(app)

    private val errorCounter = BooleanArray(ErrorStatus.values().size)

    private val viewModelState = MutableStateFlow(LocationsUiState(isLoading = true))

    val uiState = viewModelState.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        viewModelState.value
    )

    val locations = viewModelState.map {
        it.locations.toList()
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        viewModelState.value.locations.toList()
    )

    val errorMessages = viewModelState.map {
        it.errorMessages
    }.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        viewModelState.value.errorMessages
    )

    private val weatherEventChannel = Channel<LocationPanelUiModel>()
    val weatherUpdatedFlow = weatherEventChannel.receiveAsFlow().stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        null
    )

    fun loadLocations() {
        viewModelState.update {
            it.copy(isLoading = true)
        }

        viewModelScope.launch {
            val locations = settingsManager.getFavorites().toMutableList()

            if (settingsManager.useFollowGPS()) {
                getGPSData()?.let {
                    locations.add(0, it)
                }
            }

            refreshLocationWeather(locations)
        }
    }

    fun refreshLocations() {
        viewModelScope.launch {
            val locations = settingsManager.getFavorites().toMutableList()

            if (settingsManager.useFollowGPS()) {
                getGPSData()?.let {
                    locations.add(0, it)
                }
            }

            val currentLocations = viewModelState.value.locations

            val reload =
                locations.size != currentLocations.size || (locations.isNotEmpty() && locations.first().weatherSource != settingsManager.getAPI())

            if (reload) {
                loadLocations()
                return@launch
            }

            refreshLocationWeather(locations)
        }
    }

    private fun refreshLocationWeather(locations: MutableList<LocationData>) {
        val locationMap = locations.associateWith { locData ->
            LocationPanelUiModel().apply {
                locationData = locData
            }
        }

        viewModelState.update {
            it.copy(locations = locationMap.values, isLoading = false)
        }

        viewModelScope.launch(Dispatchers.Default) {
            // add a callback to update weather
            for (entry in locationMap) {
                val result = WeatherDataLoader(entry.key)
                    .loadWeatherResult(
                        WeatherRequest.Builder()
                            .forceRefresh(false)
                            .build()
                    )

                when (result) {
                    is WeatherResult.Error -> {
                        // Show error message and only warn once
                        if (!errorCounter[result.exception.errorStatus.ordinal]) {
                            errorCounter[result.exception.errorStatus.ordinal] = true

                            viewModelState.update {
                                val errorMessages =
                                    it.errorMessages + ErrorMessage.WeatherError(result.exception)
                                it.copy(errorMessages = errorMessages)
                            }
                        }

                        entry.value.setWeather(entry.key, null)
                        weatherEventChannel.trySend(entry.value)
                    }
                    is WeatherResult.NoWeather -> {
                        if (!errorCounter[ErrorStatus.NOWEATHER.ordinal]) {
                            errorCounter[ErrorStatus.NOWEATHER.ordinal] = true

                            viewModelState.update {
                                val errorMessages = it.errorMessages + ErrorMessage.WeatherError(
                                    WeatherException(ErrorStatus.NOWEATHER)
                                )
                                it.copy(errorMessages = errorMessages)
                            }
                        }

                        entry.value.setWeather(entry.key, null)
                        weatherEventChannel.trySend(entry.value)
                    }
                    is WeatherResult.Success -> {
                        entry.value.setWeather(entry.key, result.data)
                        weatherEventChannel.trySend(entry.value)
                    }
                    is WeatherResult.WeatherWithError -> {
                        // Show error message and only warn once
                        if (!errorCounter[result.exception.errorStatus.ordinal]) {
                            errorCounter[result.exception.errorStatus.ordinal] = true

                            viewModelState.update {
                                val errorMessages =
                                    it.errorMessages + ErrorMessage.WeatherError(result.exception)
                                it.copy(errorMessages = errorMessages)
                            }
                        }

                        entry.value.setWeather(entry.key, result.data)
                        weatherEventChannel.trySend(entry.value)
                    }
                }
            }
        }
    }

    private suspend fun getGPSData(): LocationData? {
        if (settingsManager.useFollowGPS()) {
            val locData = settingsManager.getLastGPSLocData()

            if (locData != null && locData.isValid) {
                return locData
            } else {
                val result = updateLocation()

                when (result) {
                    is LocationResult.Changed -> {
                        // update location to system
                        settingsManager.saveLastGPSLocData(result.data)
                        localBroadcastManager.sendBroadcast(Intent(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE))

                        return result.data
                    }
                    is LocationResult.NotChanged,
                    is LocationResult.ChangedInvalid -> {
                        if (result.data?.isValid == true) {
                            return result.data
                        }
                    }
                    is LocationResult.Error -> {
                        // propagate error to frontend
                    }
                    is LocationResult.PermissionDenied -> {
                        // propagate error to frontend
                    }
                }
            }
        }

        return null
    }

    private suspend fun updateLocation(): LocationResult {
        if (settingsManager.useFollowGPS()) {
            if (!getApplication<Application>().locationPermissionEnabled()) {
                return LocationResult.PermissionDenied()
            }

            val locMan =
                getApplication<Application>().getSystemService(Context.LOCATION_SERVICE) as? LocationManager

            if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
                return LocationResult.Error(errorMessage = ErrorMessage.Resource(R.string.error_retrieve_location))
            }

            return locationProvider.getLatestLocationData()
        }

        return LocationResult.NotChanged(null)
    }

    fun setErrorMessageShown(error: ErrorMessage) {
        viewModelState.update { state ->
            state.copy(
                errorMessages = state.errorMessages.filterNot { it == error }
            )
        }
    }
}