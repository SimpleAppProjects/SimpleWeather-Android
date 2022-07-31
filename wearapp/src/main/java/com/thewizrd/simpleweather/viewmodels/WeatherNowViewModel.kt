package com.thewizrd.simpleweather.viewmodels

import android.app.Application
import android.content.*
import android.location.Location
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
import com.thewizrd.common.wearable.WearConnectionStatus
import com.thewizrd.common.wearable.WearableHelper
import com.thewizrd.common.weatherdata.WeatherDataLoader
import com.thewizrd.common.weatherdata.WeatherRequest
import com.thewizrd.common.weatherdata.WeatherResult
import com.thewizrd.shared_resources.appLib
import com.thewizrd.shared_resources.di.localBroadcastManager
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.exceptions.ErrorStatus
import com.thewizrd.shared_resources.exceptions.WeatherException
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.buildEmptyGPSLocation
import com.thewizrd.shared_resources.locationdata.toLocation
import com.thewizrd.shared_resources.locationdata.toLocationData
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.wearable.WearableDataSync
import com.thewizrd.shared_resources.weatherdata.model.LocationType
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.wearable.WearableWorker
import com.thewizrd.weather_api.weatherModule
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import kotlin.coroutines.coroutineContext
import kotlin.math.abs

sealed interface WeatherNowState {
    val weather: WeatherUiModel?
    val isLoading: Boolean
    val errorMessages: List<ErrorMessage>
    val isGPSLocation: Boolean
    val locationData: LocationData?
    val noLocationAvailable: Boolean
    val showDisconnectedView: Boolean

    data class NoWeather(
        override val weather: WeatherUiModel? = null,
        override val isLoading: Boolean,
        override val errorMessages: List<ErrorMessage>,
        override val isGPSLocation: Boolean,
        override val locationData: LocationData? = null,
        override val noLocationAvailable: Boolean = false,
        override val showDisconnectedView: Boolean = false
    ) : WeatherNowState

    data class HasWeather(
        override val weather: WeatherUiModel,
        override val isLoading: Boolean,
        override val errorMessages: List<ErrorMessage>,
        override val isGPSLocation: Boolean,
        override val locationData: LocationData? = null,
        override val noLocationAvailable: Boolean = false,
        override val showDisconnectedView: Boolean = false
    ) : WeatherNowState
}

private data class WeatherNowViewModelState(
    val weather: WeatherUiModel? = null,
    val isLoading: Boolean = false,
    val errorMessages: List<ErrorMessage> = emptyList(),
    val isGPSLocation: Boolean = false,
    val locationData: LocationData? = null,
    val noLocationAvailable: Boolean = false,
    val showDisconnectedView: Boolean = false
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
                showDisconnectedView = showDisconnectedView
            )
        } else {
            WeatherNowState.NoWeather(
                isLoading = isLoading,
                errorMessages = errorMessages,
                isGPSLocation = isGPSLocation,
                locationData = locationData,
                noLocationAvailable = noLocationAvailable,
                showDisconnectedView = showDisconnectedView
            )
        }
    }
}

class WeatherNowViewModel(private val app: Application) : AndroidViewModel(app),
    SharedPreferences.OnSharedPreferenceChangeListener {
    companion object {
        private const val TAG_SYNCRECEIVER = "SyncDataReceiver"
    }

    private val viewModelState =
        MutableStateFlow(WeatherNowViewModelState(isLoading = true, noLocationAvailable = true))
    private val weatherDataLoader = WeatherDataLoader()
    private val wm = weatherModule.weatherManager

    private val locationProvider = LocationProvider(app)

    private var syncTimerJob: Job? = null

    init {
        registerSyncReceiver()
        appLib.registerAppSharedPreferenceListener(this)

        initializeWeatherState()
    }

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

    private fun initializeWeatherState() {
        viewModelState.update {
            it.copy(isLoading = true)
        }

        viewModelScope.launch {
            var locData = settingsManager.getHomeData()

            if (settingsManager.useFollowGPS()) {
                if (locData != null && settingsManager.getAPI() != locData.weatherSource) {
                    settingsManager.updateLocation(buildEmptyGPSLocation())
                }

                val result = updateLocation()

                if (result is LocationResult.Changed) {
                    locData = result.data
                }
            }

            if (locData?.isValid == true) {
                viewModelState.update {
                    it.copy(locationData = locData)
                }
                weatherDataLoader.updateLocation(locData)
                refreshWeather(false)
            } else {
                checkInvalidLocation()

                viewModelState.update {
                    it.copy(isLoading = false)
                }
            }
        }
    }

    fun refreshWeather(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (settingsManager.getDataSync() == WearableDataSync.OFF) {
                if (settingsManager.useFollowGPS()) {
                    val result = updateLocation()
                    if (result is LocationResult.Changed) {
                        weatherDataLoader.updateLocation(result.data)
                    }
                }

                val result = weatherDataLoader.loadWeatherResult(
                    WeatherRequest.Builder()
                        .forceRefresh(forceRefresh)
                        .build()
                )

                updateWeatherState(result)
            } else {
                syncWeather(forceRefresh)
            }
        }
    }

    private fun loadSavedWeather(forceSync: Boolean = false) {
        viewModelScope.launch {
            val result = weatherDataLoader.loadWeatherResult(
                WeatherRequest.Builder()
                    .forceLoadSavedData()
                    .loadAlerts()
                    .build()
            )

            if (forceSync && (result !is WeatherResult.Success && result !is WeatherResult.WeatherWithError)) {
                syncWeather(true)
            } else {
                updateWeatherState(result)
            }
        }
    }

    private fun updateWeatherState(result: WeatherResult) {
        viewModelState.update { state ->
            when (result) {
                is WeatherResult.Error -> {
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
                is WeatherResult.NoWeather -> {
                    val errorMessages =
                        state.errorMessages + ErrorMessage.WeatherError(WeatherException(ErrorStatus.NOWEATHER))
                    state.copy(
                        errorMessages = errorMessages,
                        isLoading = false,
                        noLocationAvailable = false
                    )
                }
                is WeatherResult.Success -> {
                    state.copy(
                        weather = result.data.toUiModel(),
                        isLoading = false,
                        noLocationAvailable = false,
                        isGPSLocation = state.locationData?.locationType == LocationType.GPS
                    )
                }
                is WeatherResult.WeatherWithError -> {
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
                        weather = result.data.toUiModel(),
                        errorMessages = errorMessages,
                        isLoading = false,
                        noLocationAvailable = false,
                        isGPSLocation = state.locationData?.locationType == LocationType.GPS
                    )
                }
            }
        }
    }

    private suspend fun updateLocation(): LocationResult {
        var locationChanged = false
        var locationData = getLocationData()

        if (settingsManager.getDataSync() == WearableDataSync.OFF && settingsManager.useFollowGPS() && (locationData == null || locationData.locationType == LocationType.GPS)) {
            if (!app.locationPermissionEnabled()) {
                return LocationResult.NotChanged(locationData)
            }

            val locMan = app.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

            if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
                locationData = settingsManager.getHomeData()
                return LocationResult.NotChanged(locationData)
            }

            var location = withContext(Dispatchers.IO) {
                val result: Location? = try {
                    withTimeoutOrNull(5000) {
                        locationProvider.getLastLocation()
                    }
                } catch (e: Exception) {
                    null
                }
                result
            }

            if (!coroutineContext.isActive) {
                return LocationResult.NotChanged(locationData)
            }

            /* Get current location from provider */
            if (location == null) {
                location = withTimeoutOrNull(30000) {
                    locationProvider.getCurrentLocation()
                }
            }

            if (!coroutineContext.isActive) {
                return LocationResult.NotChanged(locationData)
            }

            if (location != null) {
                var lastGPSLocData = settingsManager.getLastGPSLocData()

                // Check previous location difference
                if (lastGPSLocData?.isValid == true && locationData != null && ConversionMethods.calculateGeopositionDistance(
                        locationData.toLocation(),
                        location
                    ) < 1600
                ) {
                    return LocationResult.NotChanged(locationData)
                }

                if (lastGPSLocData?.isValid == true &&
                    abs(
                        ConversionMethods.calculateHaversine(
                            lastGPSLocData.latitude, lastGPSLocData.longitude,
                            location.latitude, location.longitude
                        )
                    ) < 1600
                ) {
                    return LocationResult.NotChanged(locationData)
                }

                val view = try {
                    withContext(Dispatchers.IO) {
                        wm.getLocation(location)
                    }
                } catch (e: WeatherException) {
                    viewModelState.update {
                        val errorMessages =
                            it.errorMessages + ErrorMessage.Resource(R.string.error_retrieve_location)
                        it.copy(errorMessages = errorMessages)
                    }
                    return LocationResult.NotChanged(locationData)
                }

                if (view == null || view.locationQuery.isNullOrBlank()) {
                    // Stop since there is no valid query
                    return LocationResult.NotChanged(locationData)
                } else if (view.locationTZLong.isNullOrBlank() && view.locationLat != 0.0 && view.locationLong != 0.0) {
                    val tzId =
                        weatherModule.tzdbService.getTimeZone(view.locationLat, view.locationLong)
                    if ("unknown" != tzId)
                        view.locationTZLong = tzId
                }

                if (!coroutineContext.isActive) {
                    return LocationResult.NotChanged(locationData)
                }

                // Save location as last known
                lastGPSLocData = view.toLocationData(location)
                settingsManager.updateLocation(lastGPSLocData)

                locationData = lastGPSLocData
                locationChanged = true
            }
        }

        return if (locationChanged && locationData != null) {
            LocationResult.Changed(locationData)
        } else {
            return LocationResult.NotChanged(locationData)
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

    /* Wearable Data Sync */
    private val syncDataReceiver = object : BroadcastReceiver() {
        private var locationDataReceived = false
        private var weatherDataReceived = false

        override fun onReceive(context: Context, intent: Intent) {
            viewModelScope.launch {
                if (WearableHelper.LocationPath == intent.action || WearableHelper.WeatherPath == intent.action) {
                    if (WearableHelper.WeatherPath == intent.action) {
                        weatherDataReceived = true
                    }

                    if (WearableHelper.LocationPath == intent.action) {
                        // We got the location data
                        locationDataReceived = true
                    }

                    Timber.tag(TAG_SYNCRECEIVER).d("Action: %s", intent.action)

                    if (locationDataReceived && weatherDataReceived) {
                        syncTimerJob?.cancel()

                        Timber.tag(TAG_SYNCRECEIVER).d("Loading data...")

                        // We got all our data; now load the weather
                        val locationData = settingsManager.getHomeData()

                        viewModelState.update {
                            it.copy(isLoading = true, locationData = locationData)
                        }

                        if (locationData?.isValid == true) {
                            weatherDataLoader.updateLocation(locationData)
                            loadSavedWeather()
                        } else {
                            cancelDataSync()
                        }

                        weatherDataReceived = false
                        locationDataReceived = false
                    }
                } else if (WearableHelper.ErrorPath == intent.action) {
                    // An error occurred; cancel the sync operation
                    weatherDataReceived = false
                    locationDataReceived = false
                    cancelDataSync()
                }
            }
        }
    }

    private fun cancelDataSync() {
        syncTimerJob?.cancel()

        if (settingsManager.getDataSync() != WearableDataSync.OFF) {
            viewModelScope.launch {
                var locationData = getLocationData()

                if (locationData == null) {
                    locationData = settingsManager.getHomeData()
                }

                viewModelState.update {
                    it.copy(locationData = locationData)
                }

                if (locationData?.isValid == true) {
                    weatherDataLoader.updateLocation(locationData)
                    loadSavedWeather()
                } else {
                    viewModelState.update {
                        it.copy(isLoading = false)
                    }

                    if (locationData != null) {
                        checkInvalidLocation()
                    } else {
                        viewModelState.update {
                            val errorMessages =
                                it.errorMessages + ErrorMessage.Resource(R.string.error_syncing)
                            it.copy(errorMessages = errorMessages)
                        }
                    }
                }
            }
        }
    }

    private fun startSyncTimer() {
        syncTimerJob = viewModelScope.launch {
            supervisorScope {
                delay(35000)

                ensureActive()

                // We hit the interval
                // Data syncing is taking a long time to setup
                // Stop and load saved data
                Timber.d("WeatherNow: resetTimer: timeout")

                cancelDataSync()
            }
        }
    }

    private fun registerSyncReceiver() {
        localBroadcastManager.registerReceiver(syncDataReceiver, IntentFilter().apply {
            addAction(WearableHelper.LocationPath)
            addAction(WearableHelper.WeatherPath)
        })
    }

    private suspend fun syncWeather(forceRefresh: Boolean = false) {
        if (forceRefresh) {
            // Request update from connected device
            WearableWorker.enqueueAction(app, WearableWorker.ACTION_REQUESTUPDATE, true)
            startSyncTimer()
        } else {
            val locationData = settingsManager.getHomeData()

            viewModelState.update {
                it.copy(locationData = locationData)
            }

            if (locationData?.isValid == true) {
                weatherDataLoader.updateLocation(locationData)
                loadSavedWeather(true)
            } else {
                checkInvalidLocation()

                viewModelState.update {
                    it.copy(isLoading = false)
                }
            }
        }
    }

    fun updateConnectionStatus(connectionStatus: WearConnectionStatus) {
        viewModelState.update {
            it.copy(showDisconnectedView = connectionStatus != WearConnectionStatus.CONNECTED)
        }
    }

    override fun onCleared() {
        super.onCleared()

        syncTimerJob?.cancel()
        localBroadcastManager.unregisterReceiver(syncDataReceiver)
        appLib.unregisterAppSharedPreferenceListener(this)
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
        if (key.isNullOrBlank()) return

        when (key) {
            SettingsManager.KEY_DATASYNC -> {
                // If data sync settings changes,
                // reset so we can properly reload
                viewModelState.update {
                    it.copy(locationData = null)
                }
            }
            SettingsManager.KEY_TEMPUNIT,
            SettingsManager.KEY_DISTANCEUNIT,
            SettingsManager.KEY_PRECIPITATIONUNIT,
            SettingsManager.KEY_PRESSUREUNIT,
            SettingsManager.KEY_SPEEDUNIT,
            SettingsManager.KEY_ICONSSOURCE -> {
                refreshWeather(false)
            }
        }
    }
}