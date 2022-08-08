package com.thewizrd.simpleweather.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.thewizrd.common.controls.ForecastsListViewModel
import com.thewizrd.common.controls.WeatherAlertsViewModel
import com.thewizrd.common.helpers.LocationPermissionLauncher
import com.thewizrd.common.helpers.locationPermissionEnabled
import com.thewizrd.common.utils.ActivityUtils.showToast
import com.thewizrd.common.utils.ErrorMessage
import com.thewizrd.common.wearable.WearConnectionStatus
import com.thewizrd.shared_resources.appLib
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.utils.SettingsManager
import com.thewizrd.shared_resources.wearable.WearableDataSync
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.services.WeatherUpdaterWorker
import com.thewizrd.simpleweather.services.WidgetUpdaterWorker
import com.thewizrd.simpleweather.ui.WearApp
import com.thewizrd.simpleweather.viewmodels.ForecastPanelsViewModel
import com.thewizrd.simpleweather.viewmodels.WeatherNowViewModel
import com.thewizrd.simpleweather.wearable.WearableListenerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime

class MainActivity : WearableListenerActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    override lateinit var broadcastReceiver: BroadcastReceiver
    override lateinit var intentFilter: IntentFilter

    // View Models
    private val wNowViewModel: WeatherNowViewModel by viewModels()
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

        initWearableSyncReceiver()

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
                    showToast(R.string.error_location_denied, Toast.LENGTH_SHORT)
                }
            }
        )

        setContent {
            WearApp()
        }

        lifecycleScope.launchWhenCreated {
            if (settingsManager.getDataSync() == WearableDataSync.OFF && settingsManager.useFollowGPS()) {
                if (!locationPermissionEnabled()) {
                    locationPermissionLauncher.requestLocationPermission()
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

    /* Data Sync */
    private fun initWearableSyncReceiver() {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                lifecycleScope.launch {
                    when (intent.action) {
                        ACTION_OPENONPHONE -> {
                            val showAni = intent.getBooleanExtra(EXTRA_SHOWANIMATION, false)
                            openAppOnPhone(showAni)
                        }
                        ACTION_REQUESTSETUPSTATUS -> {
                            sendSetupStatusRequest()
                        }
                        ACTION_UPDATECONNECTIONSTATUS -> {
                            val connStatus = WearConnectionStatus.valueOf(
                                intent.getIntExtra(
                                    EXTRA_CONNECTIONSTATUS,
                                    0
                                )
                            )
                            wNowViewModel.updateConnectionStatus(connStatus)
                        }
                    }
                }
            }
        }

        intentFilter = IntentFilter().apply {
            addAction(ACTION_OPENONPHONE)
            addAction(ACTION_REQUESTSETUPSTATUS)
            addAction(ACTION_UPDATECONNECTIONSTATUS)
        }
    }
}