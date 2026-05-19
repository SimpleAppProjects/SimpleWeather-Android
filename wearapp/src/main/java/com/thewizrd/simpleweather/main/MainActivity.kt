package com.thewizrd.simpleweather.main

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.util.ObjectsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.thewizrd.common.controls.ForecastsListViewModel
import com.thewizrd.common.controls.WeatherAlertsViewModel
import com.thewizrd.common.helpers.LocationPermissionLauncher
import com.thewizrd.common.helpers.locationPermissionEnabled
import com.thewizrd.common.location.LocationResult
import com.thewizrd.common.utils.ActivityUtils.showToast
import com.thewizrd.common.utils.ErrorMessage
import com.thewizrd.shared_resources.R as sharedRes
import com.thewizrd.shared_resources.appLib
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.preferences.SettingsManager
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.wearable.WearableDataSync
import com.thewizrd.simpleweather.BuildConfig
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.locale.UserLocaleActivity
import com.thewizrd.simpleweather.services.WeatherUpdaterWorker
import com.thewizrd.simpleweather.services.WidgetUpdaterWorker
import com.thewizrd.simpleweather.ui.weather.WeatherNow
import com.thewizrd.simpleweather.viewmodels.ForecastPanelsViewModel
import com.thewizrd.simpleweather.viewmodels.WeatherDataSyncViewModel
import com.thewizrd.simpleweather.viewmodels.WeatherNowViewModel
import com.thewizrd.simpleweather.wearable.WearableListenerActions.ACTION_REQUESTREFRESHWEATHER
import com.thewizrd.simpleweather.wearable.WearableListenerActions.ACTION_REQUESTSYNCWEATHER
import com.thewizrd.simpleweather.wearable.WearableListenerActions.ACTION_SYNCSETTINGUPDATED
import com.thewizrd.simpleweather.wearable.WearableListenerActions.ACTION_UPDATESYNCSTATUS
import com.thewizrd.simpleweather.wearable.WearableListenerActions.EXTRA_SUCCESS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime

class MainActivity : UserLocaleActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    // View Models
    private val wNowViewModel: WeatherNowViewModel by viewModels()
    private val dataSyncViewModel: WeatherDataSyncViewModel by viewModels()

    private val forecastsView: ForecastsListViewModel by viewModels()
    private val forecastPanelsView: ForecastPanelsViewModel by viewModels()
    private val alertsView: WeatherAlertsViewModel by viewModels()

    // GPS location
    private lateinit var locationPermissionLauncher: LocationPermissionLauncher

    override fun attachBaseContext(newBase: Context) {
        // Use night mode resources (needed for external weather icons)
        super.attachBaseContext(newBase.getThemeContextOverride(false))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AnalyticsLogger.logEvent("$TAG: onCreate")

        locationPermissionLauncher = LocationPermissionLauncher(
            this,
            locationCallback = { granted ->
                if (granted) {
                    // permission was granted, yay!
                    // Do the task you need to do.
                    wNowViewModel.refreshWeather()
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    settingsManager.setFollowGPS(false)
                    showToast(sharedRes.string.error_location_denied, Toast.LENGTH_SHORT)
                }
            }
        )

        setContent {
            WeatherNow()
        }

        wNowViewModel.initialize()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                if (wNowViewModel.isInitialized.value) {
                    initializeState()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        lifecycleScope.launch {
            wNowViewModel.weather.collect {
                wNowViewModel.uiState.value.locationData?.let { locationData ->
                    forecastPanelsView.updateForecasts(locationData)
                    forecastsView.updateForecasts(locationData)
                    alertsView.updateAlerts(locationData)

                    val context = appLib.context
                    val span = Duration.between(
                        ZonedDateTime.now(ZoneOffset.UTC).toLocalDateTime(),
                        settingsManager.getUpdateTime()
                    )
                    if (settingsManager.getDataSync() != WearableDataSync.OFF && span.toMinutes() > SettingsManager.DEFAULT_INTERVAL) {
                        WeatherUpdaterWorker.enqueueAction(
                            context,
                            WeatherUpdaterWorker.ACTION_UPDATEWEATHER
                        )
                    } else {
                        lifecycleScope.launch(Dispatchers.Default) {
                            WidgetUpdaterWorker.requestWidgetUpdate(context)
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            wNowViewModel.errorMessages.collect {
                val error = it.firstOrNull()

                if (error != null) {
                    onErrorMessage(error)
                }
            }
        }

        lifecycleScope.launch {
            wNowViewModel.eventFlow.collect { event ->
                when (event.eventType) {
                    ACTION_REQUESTSYNCWEATHER -> {
                        dataSyncViewModel.syncWeather()
                    }
                }
            }
        }

        lifecycleScope.launch {
            dataSyncViewModel.eventFlow.collect { event ->
                when (event.eventType) {
                    ACTION_UPDATESYNCSTATUS -> {
                        val success = event.data.getBoolean(EXTRA_SUCCESS, false)

                        if (success) {
                            // reinitialize state
                            initializeState()
                        } else {
                            wNowViewModel.refreshWeather(false)
                        }
                    }

                    ACTION_SYNCSETTINGUPDATED -> {
                        initializeState()
                    }

                    ACTION_REQUESTREFRESHWEATHER -> {
                        wNowViewModel.refreshWeather(false)
                    }
                }
            }
        }
    }

    private suspend fun verifyLocationData(): LocationResult = withContext(Dispatchers.IO) {
        var locationData = wNowViewModel.uiState.value.locationData
        var locationChanged = false

        // Check if home location changed
        // For ex. due to GPS setting change
        val homeData = settingsManager.getHomeData()
        if (!ObjectsCompat.equals(locationData, homeData)) {
            locationData = homeData
            locationChanged = true
        }

        if (locationChanged) {
            if (locationData != null) {
                LocationResult.Changed(locationData)
            } else {
                LocationResult.ChangedInvalid(null)
            }
        } else {
            LocationResult.NotChanged(locationData)
        }
    }

    private suspend fun initializeState() {
        val result = verifyLocationData()

        result.data?.let {
            if ((BuildConfig.IS_NONGMS || settingsManager.getDataSync() == WearableDataSync.OFF) && settingsManager.useFollowGPS()) {
                if (!locationPermissionEnabled()) {
                    locationPermissionLauncher.requestLocationPermission()
                }
            }
        }

        if (result is LocationResult.Changed || result is LocationResult.ChangedInvalid) {
            wNowViewModel.initialize(result.data)
        } else {
            wNowViewModel.refreshWeather()
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsLogger.logEvent("$TAG: onResume")
    }

    override fun onPause() {
        AnalyticsLogger.logEvent("$TAG: onPause")
        super.onPause()
    }

    private fun onErrorMessage(error: ErrorMessage) {
        when (error) {
            is ErrorMessage.Resource -> {
                showToast(error.stringId, Toast.LENGTH_SHORT)
            }
            is ErrorMessage.String -> {
                showToast(error.message, Toast.LENGTH_SHORT)
            }
            is ErrorMessage.WeatherError -> {
                showToast(error.exception.message, Toast.LENGTH_SHORT)
            }
        }

        wNowViewModel.setErrorMessageShown(error)
    }
}