package com.thewizrd.simpleweather.setup

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.lifecycleScope
import com.thewizrd.common.helpers.LocationPermissionLauncher
import com.thewizrd.common.helpers.locationPermissionEnabled
import com.thewizrd.common.location.LocationProvider
import com.thewizrd.common.utils.ActivityUtils.showToast
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.di.localBroadcastManager
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.exceptions.ErrorStatus
import com.thewizrd.shared_resources.exceptions.WeatherException
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.toLocationData
import com.thewizrd.shared_resources.remoteconfig.remoteConfigService
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.shared_resources.utils.CommonActions
import com.thewizrd.shared_resources.utils.CustomException
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.wearable.WearableDataSync
import com.thewizrd.shared_resources.weatherdata.model.Forecasts
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecasts
import com.thewizrd.simpleweather.BuildConfig
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.databinding.FragmentSetupBinding
import com.thewizrd.simpleweather.fragments.LocationSearchFragment
import com.thewizrd.simpleweather.helpers.AcceptDenyDialog
import com.thewizrd.simpleweather.locale.UserLocaleActivity
import com.thewizrd.simpleweather.main.MainActivity
import com.thewizrd.weather_api.weatherModule
import kotlinx.coroutines.*
import kotlin.coroutines.coroutineContext

class SetupActivity : UserLocaleActivity() {
    companion object {
        private const val TAG = "SetupFragment"
        private const val REQUEST_CODE_SYNC_ACTIVITY = 10
    }

    // Views
    private lateinit var binding: FragmentSetupBinding
    private var mLocation: Location? = null
    private lateinit var locationProvider: LocationProvider

    private val wm = weatherModule.weatherManager

    private var job: Job? = null

    private lateinit var locationPermissionLauncher: LocationPermissionLauncher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AnalyticsLogger.logEvent("SetupActivity: onCreate")

        binding = FragmentSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Controls
        binding.searchButton.setOnClickListener { v ->
            supportFragmentManager.beginTransaction()
                .add(android.R.id.content, LocationSearchFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.locationButton.setOnClickListener { fetchGeoLocation() }

        binding.setupPhoneButton.setOnClickListener {
            AcceptDenyDialog.Builder(this) { dialog, which ->
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    startActivityForResult(
                        Intent(this, SetupSyncActivity::class.java),
                        REQUEST_CODE_SYNC_ACTIVITY
                    )
                }
            }.setMessage(R.string.prompt_confirmsetup)
                .show()
        }

        binding.progressBarContainer.visibility = View.GONE

        // Location Listener
        locationProvider = LocationProvider(this)

        locationPermissionLauncher = LocationPermissionLauncher(
            this,
            locationCallback = { granted ->
                if (granted) {
                    // permission was granted, yay!
                    // Do the task you need to do.
                    fetchGeoLocation()
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    lifecycleScope.launch {
                        enableControls(true)
                        showToast(R.string.error_location_denied, Toast.LENGTH_SHORT)
                    }
                }
            }
        )

        supportFragmentManager.setFragmentResultListener(
            Constants.KEY_DATA,
            this
        ) { requestKey, bundle ->
            val data = bundle.getString(requestKey)

            startActivity(
                Intent(this, MainActivity::class.java)
                    .putExtra(Constants.KEY_DATA, data)
            )
            finishAffinity()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_SYNC_ACTIVITY -> {
                if (resultCode == Activity.RESULT_OK) {
                    lifecycleScope.launch {
                        if (settingsManager.getHomeData() != null) {
                            settingsManager.setDataSync(WearableDataSync.DEVICEONLY)
                            settingsManager.setWeatherLoaded(true)
                            // Start WeatherNow Activity
                            startActivity(Intent(this@SetupActivity, MainActivity::class.java))
                            finishAffinity()
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsLogger.logEvent("SetupActivity: onResume")
    }

    override fun onPause() {
        AnalyticsLogger.logEvent("SetupActivity: onPause")
        job?.cancel()
        super.onPause()
    }

    override fun onDestroy() {
        // Cancel pending actions
        job?.cancel()
        super.onDestroy()
    }

    private fun enableControls(enable: Boolean) {
        binding.searchButton.isEnabled = enable
        binding.locationButton.isEnabled = enable
        if (enable) {
            binding.progressBarContainer.visibility = View.GONE
        } else {
            binding.progressBarContainer.visibility = View.VISIBLE
        }
    }

    private fun fetchGeoLocation() {
        lifecycleScope.launch(Dispatchers.Main.immediate) {
            // Show loading bar
            binding.locationButton.isEnabled = false
            enableControls(false)

            if (mLocation == null) {
                // Cancel other tasks
                job?.cancel()

                supervisorScope {
                    job = async(Dispatchers.Default) {
                        updateLocation()
                    }

                    job!!.invokeOnCompletion { e ->
                        if (e != null) {
                            lifecycleScope.launch(Dispatchers.Main.immediate) {
                                if (mLocation == null) {
                                    // Restore controls
                                    enableControls(true)
                                    settingsManager.setFollowGPS(false)
                                    settingsManager.setWeatherLoaded(false)
                                    if (e is WeatherException || e is CustomException) {
                                        showToast(e.message, Toast.LENGTH_SHORT)
                                    } else {
                                        showToast(
                                            R.string.error_retrieve_location,
                                            Toast.LENGTH_SHORT
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Cancel other tasks
                job?.cancel()

                supervisorScope {
                    val deferredJob = async(Dispatchers.IO) {
                        ensureActive()

                        val view = withContext(Dispatchers.IO) {
                            wm.getLocation(mLocation!!)
                        }

                        if (view == null || view.locationQuery.isNullOrBlank()) {
                            throw CustomException(R.string.error_retrieve_location)
                        } else if (view.locationTZLong.isNullOrBlank() && view.locationLat != 0.0 && view.locationLong != 0.0) {
                            val tzId = weatherModule.tzdbService.getTimeZone(
                                view.locationLat,
                                view.locationLong
                            )
                            if ("unknown" != tzId)
                                view.locationTZLong = tzId
                        }

                        if (!settingsManager.isWeatherLoaded() && !BuildConfig.IS_NONGMS) {
                            // Set default provider based on location
                            val provider =
                                remoteConfigService.getDefaultWeatherProvider(view.locationCountry)
                            settingsManager.setAPI(provider)
                            view.updateWeatherSource(provider)
                        }

                        if (settingsManager.usePersonalKey() && settingsManager.getAPIKey()
                                .isNullOrBlank() && wm.isKeyRequired()
                        ) {
                            throw CustomException(R.string.werror_invalidkey)
                        }

                        ensureActive()

                        if (!wm.isRegionSupported(view.locationCountry)) {
                            throw CustomException(R.string.error_message_weather_region_unsupported)
                        }

                        // Get Weather Data
                        val location = view.toLocationData(mLocation!!)
                        if (!location.isValid) {
                            throw CustomException(R.string.werror_noweather)
                        }

                        ensureActive()

                        var weather = settingsManager.getWeatherData(location.query)
                        if (weather == null) {
                            ensureActive()

                            weather = withContext(Dispatchers.IO) {
                                wm.getWeather(location)
                            }
                        }

                        if (weather == null) {
                            throw WeatherException(ErrorStatus.NOWEATHER)
                        } else if (wm.supportsAlerts() && wm.needsExternalAlertData()) {
                            weather.weatherAlerts = wm.getAlerts(location)
                        }

                        ensureActive()

                        // Save weather data
                        settingsManager.saveHomeData(location)
                        if (wm.supportsAlerts() && weather.weatherAlerts != null)
                            settingsManager.saveWeatherAlerts(location, weather.weatherAlerts)
                        settingsManager.saveWeatherData(weather)
                        settingsManager.saveWeatherForecasts(Forecasts(weather))
                        settingsManager.saveWeatherForecasts(
                            location.query,
                            weather.hrForecast?.map { input ->
                                HourlyForecasts(
                                    weather.query,
                                    input
                                )
                            })

                        // If we're changing locations, trigger an update
                        if (settingsManager.isWeatherLoaded()) {
                            localBroadcastManager.sendBroadcast(Intent(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE))
                        }

                        settingsManager.setFollowGPS(true)
                        settingsManager.setWeatherLoaded(true)
                        settingsManager.setDataSync(WearableDataSync.OFF)

                        location
                    }.also {
                        job = it
                    }

                    deferredJob.invokeOnCompletion callback@{
                        if (it is CancellationException) {
                            return@callback
                        }

                        val t = deferredJob.getCompletionExceptionOrNull()
                        if (t == null) {
                            val data = deferredJob.getCompleted()
                            if (data != null) {
                                // Start WeatherNow Activity with weather data
                                val intent = Intent(this@SetupActivity, MainActivity::class.java)
                                intent.putExtra(
                                    Constants.KEY_DATA,
                                    JSONParser.serializer(data, LocationData::class.java)
                                )
                                startActivity(intent)
                                finishAffinity()
                            } else {
                                lifecycleScope.launch {
                                    enableControls(true)
                                }
                            }
                        } else {
                            lifecycleScope.launch {
                                // Restore controls
                                enableControls(true)
                                settingsManager.setFollowGPS(false)
                                settingsManager.setWeatherLoaded(false)
                                if (t is WeatherException || t is CustomException) {
                                    showToast(t.message, Toast.LENGTH_SHORT)
                                } else {
                                    showToast(R.string.error_retrieve_location, Toast.LENGTH_SHORT)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    @Throws(CustomException::class)
    private suspend fun updateLocation() {
        if (!locationPermissionEnabled()) {
            locationPermissionLauncher.requestLocationPermission()
            return
        }

        val locMan = getSystemService(Context.LOCATION_SERVICE) as LocationManager?

        if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
            throw CustomException(R.string.error_enable_location_services)
        }

        var location = withContext(Dispatchers.IO) {
            withTimeoutOrNull(5000) {
                locationProvider.getLastLocation()
            }
        }

        coroutineContext.ensureActive()

        /* Get current location from provider */
        if (location == null) {
            location = withTimeoutOrNull(30000) {
                locationProvider.getCurrentLocation()
            }
        }

        coroutineContext.ensureActive()

        if (location != null) {
            mLocation = location
            fetchGeoLocation()
        }
    }
}