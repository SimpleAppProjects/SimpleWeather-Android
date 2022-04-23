package com.thewizrd.simpleweather.setup

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.thewizrd.common.helpers.locationPermissionEnabled
import com.thewizrd.common.helpers.requestLocationPermission
import com.thewizrd.common.location.LocationProvider
import com.thewizrd.common.utils.ActivityUtils.showToast
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.exceptions.ErrorStatus
import com.thewizrd.shared_resources.exceptions.WeatherException
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.toLocationData
import com.thewizrd.shared_resources.remoteconfig.remoteConfigService
import com.thewizrd.shared_resources.utils.*
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
import timber.log.Timber
import kotlin.coroutines.coroutineContext

class SetupActivity : UserLocaleActivity() {
    companion object {
        private const val TAG = "SetupFragment"
        private const val PERMISSION_LOCATION_REQUEST_CODE = 0
        private const val REQUEST_CODE_SYNC_ACTIVITY = 10
    }

    // Views
    private lateinit var binding: FragmentSetupBinding
    private var mLocation: Location? = null
    private lateinit var locationProvider: LocationProvider
    private lateinit var locationCallback: LocationProvider.Callback

    /**
     * Tracks the status of the location updates request.
     */
    private var mRequestingLocationUpdates = false

    private val wm = weatherModule.weatherManager

    private var job: Job? = null

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
        locationCallback = object : LocationProvider.Callback {
            override fun onLocationChanged(location: Location?) {
                stopLocationUpdates()
                mLocation = location

                Timber.tag(TAG).i("Location update received...")

                lifecycleScope.launch {
                    if (mLocation == null) {
                        enableControls(true)
                        showToast(R.string.error_retrieve_location, Toast.LENGTH_SHORT)
                    } else {
                        fetchGeoLocation()
                    }
                }
            }

            override fun onRequestTimedOut() {
                stopLocationUpdates()
                enableControls(true)
                showToast(R.string.error_retrieve_location, Toast.LENGTH_SHORT)
            }
        }

        mRequestingLocationUpdates = false

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

    /**
     * Removes location updates from the FusedLocationApi.
     */
    private fun stopLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            Logger.writeLine(
                Log.DEBUG,
                "SetupActivity: stopLocationUpdates: updates never requested, no-op."
            )
            return
        }

        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        locationProvider.stopLocationUpdates()
        mRequestingLocationUpdates = false
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
        // Remove location updates to save battery.
        stopLocationUpdates()
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
                            // Cancel requests
                            stopLocationUpdates()

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
                            LocalBroadcastManager.getInstance(this@SetupActivity)
                                .sendBroadcast(Intent(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE))
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
            requestLocationPermission(PERMISSION_LOCATION_REQUEST_CODE)
            return
        }

        val locMan = getSystemService(Context.LOCATION_SERVICE) as LocationManager?

        if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
            throw CustomException(R.string.error_enable_location_services)
        }

        val location = withContext(Dispatchers.IO) {
            withTimeoutOrNull(5000) {
                locationProvider.getLastLocation()
            }
        }

        coroutineContext.ensureActive()

        /*
         * Request start of location updates. Does nothing if
         * updates have already been requested.
         */
        if (location == null && !mRequestingLocationUpdates) {
            Timber.tag(TAG).i("Requesting location updates...")

            mRequestingLocationUpdates = true
            locationProvider.requestSingleUpdate(locationCallback, Looper.getMainLooper(), 30000)
        }

        coroutineContext.ensureActive()

        if (location != null && !mRequestingLocationUpdates) {
            mLocation = location
            fetchGeoLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSION_LOCATION_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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
        }
    }
}