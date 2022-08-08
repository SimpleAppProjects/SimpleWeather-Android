package com.thewizrd.simpleweather.viewmodels

import android.app.Application
import android.content.Context
import android.location.LocationManager
import android.util.Log
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thewizrd.common.controls.WeatherUiModel
import com.thewizrd.common.controls.toUiModel
import com.thewizrd.common.helpers.locationPermissionEnabled
import com.thewizrd.common.location.LocationProvider
import com.thewizrd.common.location.LocationResult
import com.thewizrd.common.utils.ErrorMessage
import com.thewizrd.common.weatherdata.WeatherDataLoader
import com.thewizrd.common.weatherdata.WeatherRequest
import com.thewizrd.common.weatherdata.WeatherResult
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.exceptions.ErrorStatus
import com.thewizrd.shared_resources.exceptions.WeatherException
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.buildEmptyGPSLocation
import com.thewizrd.shared_resources.utils.CustomException
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.weatherdata.model.LocationType
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlert
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.controls.ImageDataViewModel
import com.thewizrd.weather_api.weatherModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface WeatherNowState {
    val weather: WeatherUiModel?
    val isLoading: Boolean
    val errorMessages: List<ErrorMessage>
    val isGPSLocation: Boolean
    val locationData: LocationData?
    val noLocationAvailable: Boolean
    val showDisconnectedView: Boolean
    val isImageLoading: Boolean

    data class NoWeather(
        override val weather: WeatherUiModel? = null,
        override val isLoading: Boolean,
        override val errorMessages: List<ErrorMessage>,
        override val isGPSLocation: Boolean,
        override val locationData: LocationData? = null,
        override val noLocationAvailable: Boolean = false,
        override val showDisconnectedView: Boolean = false,
        override val isImageLoading: Boolean = false
    ) : WeatherNowState

    data class HasWeather(
        override val weather: WeatherUiModel,
        override val isLoading: Boolean,
        override val errorMessages: List<ErrorMessage>,
        override val isGPSLocation: Boolean,
        override val locationData: LocationData? = null,
        override val noLocationAvailable: Boolean = false,
        override val showDisconnectedView: Boolean = false,
        override val isImageLoading: Boolean = false
    ) : WeatherNowState
}

private data class WeatherNowViewModelState(
    val weather: WeatherUiModel? = null,
    val isLoading: Boolean = false,
    val errorMessages: List<ErrorMessage> = emptyList(),
    val isGPSLocation: Boolean = false,
    val locationData: LocationData? = null,
    val noLocationAvailable: Boolean = false,
    val showDisconnectedView: Boolean = false,
    val scrollViewPosition: Int = 0,
    val isImageLoading: Boolean = false
) {
    fun toWeatherNowState(): WeatherNowState {
        return if (weather?.isValid == true) {
            WeatherNowState.HasWeather(
                weather = weather,
                isLoading = isLoading,
                errorMessages = errorMessages,
                isGPSLocation = isGPSLocation,
                locationData = locationData,
                noLocationAvailable = noLocationAvailable,
                showDisconnectedView = showDisconnectedView,
                isImageLoading = isImageLoading
            )
        } else {
            WeatherNowState.NoWeather(
                isLoading = isLoading,
                errorMessages = errorMessages,
                isGPSLocation = isGPSLocation,
                locationData = locationData,
                noLocationAvailable = noLocationAvailable,
                showDisconnectedView = showDisconnectedView,
                isImageLoading = isImageLoading
            )
        }
    }
}

class WeatherNowViewModel(app: Application) : AndroidViewModel(app) {
    private val viewModelState =
        MutableStateFlow(WeatherNowViewModelState(isLoading = true, noLocationAvailable = true))
    private val alertsState = MutableStateFlow<Collection<WeatherAlert>?>(emptyList())
    private val imageDataState = MutableStateFlow<ImageDataViewModel?>(null)

    private val weatherDataLoader = WeatherDataLoader()
    private val wm = weatherModule.weatherManager

    private val locationProvider = LocationProvider(app)

    val uiState = viewModelState.map {
        it.toWeatherNowState()
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        viewModelState.value.toWeatherNowState()
    )

    val weather = viewModelState.map {
        it.weather
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        null
    )

    val imageData = imageDataState.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        null
    )

    val alerts = alertsState.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        alertsState.value
    )

    val errorMessages = viewModelState.map {
        it.errorMessages
    }.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        viewModelState.value.errorMessages
    )

    private fun getLocationData(): LocationData? {
        return viewModelState.value.locationData
    }

    fun initialize(locationData: LocationData? = null) {
        viewModelState.update {
            it.copy(isLoading = true)
        }

        viewModelScope.launch {
            var locData = locationData ?: settingsManager.getHomeData()

            if (settingsManager.useFollowGPS()) {
                if (locData != null && settingsManager.getAPI() != locData.weatherSource) {
                    settingsManager.updateLocation(buildEmptyGPSLocation())
                }

                val result = updateLocation()

                if (result is LocationResult.Changed) {
                    settingsManager.updateLocation(result.data)
                    locData = result.data
                }
            }

            updateLocation(locData)
        }
    }

    fun refreshWeather(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (settingsManager.useFollowGPS()) {
                val result = updateLocation()
                if (result is LocationResult.Changed) {
                    settingsManager.updateLocation(result.data)
                    weatherDataLoader.updateLocation(result.data)
                }
            }

            val result = weatherDataLoader.loadWeatherResult(
                WeatherRequest.Builder()
                    .forceRefresh(forceRefresh)
                    .loadAlerts()
                    .build()
            )

            updateWeatherState(result)
        }
    }

    private fun updateWeatherState(result: WeatherResult) {
        when (result) {
            is WeatherResult.Error -> {
                viewModelState.update { state ->
                    if (state.locationData?.countryCode?.let { !wm.isRegionSupported(it) } == true) {
                        Logger.writeLine(
                            Log.WARN,
                            "Location: %s",
                            JSONParser.serializer(state.locationData)
                        )
                        Logger.writeLine(
                            Log.WARN,
                            CustomException(R.string.error_message_weather_region_unsupported)
                        )
                    }

                    val errorMessages =
                        state.errorMessages + ErrorMessage.WeatherError(result.exception)
                    state.copy(
                        errorMessages = errorMessages,
                        isLoading = false,
                        noLocationAvailable = false
                    )
                }
            }
            is WeatherResult.NoWeather -> {
                viewModelState.update { state ->
                    val errorMessages =
                        state.errorMessages + ErrorMessage.WeatherError(WeatherException(ErrorStatus.NOWEATHER))
                    state.copy(
                        errorMessages = errorMessages,
                        isLoading = false,
                        noLocationAvailable = false
                    )
                }
            }
            is WeatherResult.Success -> {
                val weatherData = result.data.toUiModel()

                viewModelState.update { state ->
                    state.copy(
                        weather = weatherData,
                        isLoading = false,
                        noLocationAvailable = false,
                        isGPSLocation = state.locationData?.locationType == LocationType.GPS
                    )
                }

                alertsState.update {
                    result.data.weatherAlerts
                }

                viewModelScope.launch {
                    imageDataState.update {
                        weatherData.getImageData()
                    }
                }
            }
            is WeatherResult.WeatherWithError -> {
                val weatherData = result.data.toUiModel()

                viewModelState.update { state ->
                    if (state.locationData?.countryCode?.let { !wm.isRegionSupported(it) } == true) {
                        Logger.writeLine(
                            Log.WARN,
                            "Location: %s",
                            JSONParser.serializer(state.locationData)
                        )
                        Logger.writeLine(
                            Log.WARN,
                            CustomException(R.string.error_message_weather_region_unsupported)
                        )
                    }

                    val errorMessages =
                        state.errorMessages + ErrorMessage.WeatherError(result.exception)
                    state.copy(
                        weather = weatherData,
                        errorMessages = errorMessages,
                        isLoading = false,
                        noLocationAvailable = false,
                        isGPSLocation = state.locationData?.locationType == LocationType.GPS
                    )
                }

                alertsState.update {
                    result.data.weatherAlerts
                }

                viewModelScope.launch {
                    imageDataState.update {
                        weatherData.getImageData()
                    }
                }
            }
        }
    }

    fun setErrorMessageShown(error: ErrorMessage) {
        viewModelState.update { state ->
            state.copy(
                errorMessages = state.errorMessages.filterNot { it == error }
            )
        }
    }

    private suspend fun updateLocation(): LocationResult {
        var locationData = getLocationData()

        if (settingsManager.useFollowGPS() && locationData?.locationType == LocationType.GPS) {
            if (!getApplication<Application>().locationPermissionEnabled()) {
                return LocationResult.NotChanged(locationData)
            }

            val locMan =
                getApplication<Application>().getSystemService(Context.LOCATION_SERVICE) as? LocationManager

            if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
                locationData = settingsManager.getHomeData()
                return LocationResult.NotChanged(locationData)
            }

            return locationProvider.getLatestLocationData(locationData)
        }

        return LocationResult.NotChanged(locationData)
    }

    fun updateLocation(locationData: LocationData?) {
        viewModelState.update {
            it.copy(locationData = locationData)
        }

        if (locationData?.isValid == true) {
            viewModelState.update {
                it.copy(locationData = locationData)
            }
            weatherDataLoader.updateLocation(locationData)
            refreshWeather(false)
        } else {
            checkInvalidLocation()

            viewModelState.update {
                it.copy(isLoading = false)
            }
        }
    }

    private fun checkInvalidLocation() {
        val locationData = getLocationData()

        if (locationData?.isValid != true) {
            viewModelScope.launch {
                withContext(Dispatchers.Default) {
                    Logger.writeLine(
                        Log.WARN,
                        "Location: %s",
                        JSONParser.serializer(locationData, LocationData::class.java)
                    )
                    Logger.writeLine(
                        Log.WARN,
                        "Home: %s",
                        JSONParser.serializer(
                            settingsManager.getHomeData(),
                            LocationData::class.java
                        )
                    )

                    Logger.writeLine(Log.WARN, IllegalStateException("Invalid location data"))
                }

                viewModelState.update {
                    it.copy(noLocationAvailable = true, isLoading = false)
                }
            }
        }
    }

    fun onImageLoading() {
        viewModelState.update {
            it.copy(isImageLoading = true)
        }
    }

    fun onImageLoaded() {
        viewModelState.update {
            it.copy(isImageLoading = false)
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}