package com.thewizrd.simpleweather.setup

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.findNavController
import com.google.android.gms.location.*
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.tzdb.TZDBCache
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.wearable.WearableDataSync
import com.thewizrd.shared_resources.wearable.WearableHelper
import com.thewizrd.shared_resources.weatherdata.*
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.databinding.FragmentSetupBinding
import com.thewizrd.simpleweather.fragments.CustomFragment
import com.thewizrd.simpleweather.helpers.AcceptDenyDialogBuilder
import com.thewizrd.simpleweather.main.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import kotlin.coroutines.coroutineContext

class SetupFragment : CustomFragment() {
    companion object {
        private const val TAG = "SetupFragment"
        private const val PERMISSION_LOCATION_REQUEST_CODE = 0
        private const val REQUEST_CODE_SYNC_ACTIVITY = 10
    }

    // Views
    private lateinit var binding: FragmentSetupBinding
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private var mLocation: Location? = null
    private var mLocCallback: LocationCallback? = null
    private var mLocListnr: LocationListener? = null

    /**
     * Tracks the status of the location updates request.
     */
    private var mRequestingLocationUpdates = false
    private val mMainHandler = Handler(Looper.getMainLooper())

    private val wm = WeatherManager.instance

    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (WearableHelper.isGooglePlayServicesInstalled()) {
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
            mLocCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    stopLocationUpdates()
                    mMainHandler.removeCallbacks(cancelLocRequestRunner)
                    mLocation = locationResult.lastLocation

                    Timber.tag(TAG).i("Fused: Location update received...")

                    runWithView {
                        if (mLocation == null) {
                            enableControls(true)
                            showToast(R.string.error_retrieve_location, Toast.LENGTH_SHORT)
                        } else {
                            fetchGeoLocation()
                        }
                    }
                }

                override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                    if (!locationAvailability.isLocationAvailable) {
                        stopLocationUpdates()
                        mMainHandler.removeCallbacks(cancelLocRequestRunner)

                        Timber.tag(TAG).i("Fused: Location update unavailable...")
                        runWithView {
                            enableControls(true)
                            showToast(R.string.error_retrieve_location, Toast.LENGTH_SHORT)
                        }
                    }
                }
            }
        } else {
            mLocListnr = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    mMainHandler.removeCallbacks(cancelLocRequestRunner)
                    stopLocationUpdates()

                    Timber.tag(TAG).i("LocMan: Location update received...")

                    mLocation = location
                    runWithView {
                        if (mLocation == null) {
                            enableControls(true)
                            showToast(R.string.error_retrieve_location, Toast.LENGTH_SHORT)
                        } else {
                            fetchGeoLocation()
                        }
                    }
                }

                override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
        }

        mRequestingLocationUpdates = false
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    private fun stopLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            Logger.writeLine(Log.DEBUG, "SetupActivity: stopLocationUpdates: updates never requested, no-op.")
            return
        }

        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        mLocCallback?.let {
            mFusedLocationClient?.removeLocationUpdates(it)
                    ?.addOnCompleteListener { mRequestingLocationUpdates = false }
        }
        mLocListnr?.let {
            val locMan = fragmentActivity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
            locMan?.removeUpdates(it)
            mRequestingLocationUpdates = false
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSetupBinding.inflate(inflater, container, false)

        // Controls
        binding.searchButton.setOnClickListener { v ->
            try {
                v.findNavController().navigate(SetupFragmentDirections.actionSetupFragmentToLocationSearchFragment())
            } catch (ex: IllegalArgumentException) {
                val props = Bundle().apply {
                    putString("method", "searchButton.setOnClickListener")
                    putBoolean("isAlive", isAlive)
                    putBoolean("isViewAlive", isViewAlive)
                    putBoolean("isDetached", isDetached)
                    putBoolean("isResumed", isResumed)
                    putBoolean("isRemoving", isRemoving)
                }

                AnalyticsLogger.logEvent("$TAG: navigation failed", props)
                Logger.writeLine(Log.ERROR, ex)
            }
        }

        binding.locationButton.setOnClickListener { fetchGeoLocation() }

        binding.setupPhoneButton.setOnClickListener {
            AcceptDenyDialogBuilder(requireActivity()) { dialog, which ->
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    startActivityForResult(Intent(requireActivity(), SetupSyncActivity::class.java), REQUEST_CODE_SYNC_ACTIVITY)
                }
            }.setMessage(R.string.prompt_confirmsetup)
                    .show()
        }

        binding.progressBar.visibility = View.GONE

        return binding.root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_SYNC_ACTIVITY -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (settingsManager.getHomeData() != null) {
                        settingsManager.setDataSync(WearableDataSync.DEVICEONLY)
                        settingsManager.setWeatherLoaded(true)
                        // Start WeatherNow Activity
                        startActivity(Intent(requireActivity(), MainActivity::class.java))
                        requireActivity().finishAffinity()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsLogger.logEvent("SetupFragment: onResume")
    }

    override fun onPause() {
        AnalyticsLogger.logEvent("SetupFragment: onPause")
        job?.cancel()
        // Remove location updates to save battery.
        stopLocationUpdates()
        super.onPause()
    }

    override fun onDestroyView() {
        // Cancel pending actions
        job?.cancel()
        super.onDestroyView()
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
            binding.progressBar.visibility = View.GONE
        } else {
            binding.progressBar.visibility = View.VISIBLE
        }
    }

    private fun fetchGeoLocation() {
        runWithView(Dispatchers.Main.immediate) {
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

                            runWithView(Dispatchers.Main.immediate) {
                                if (mLocation == null) {
                                    // Restore controls
                                    enableControls(true)
                                    settingsManager.setFollowGPS(false)
                                    settingsManager.setWeatherLoaded(false)
                                    if (e is WeatherException || e is CustomException) {
                                        showToast(e.message, Toast.LENGTH_SHORT)
                                    } else {
                                        showToast(R.string.error_retrieve_location, Toast.LENGTH_SHORT)
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

                        if (view == null || view.locationQuery?.isBlank() == true) {
                            throw CustomException(R.string.error_retrieve_location)
                        } else if (view.locationTZLong?.isBlank() == true && view.locationLat != 0.0 && view.locationLong != 0.0) {
                            val tzId = TZDBCache.getTimeZone(view.locationLat, view.locationLong)
                            if ("unknown" != tzId)
                                view.locationTZLong = tzId
                        }

                        val isUS = LocationUtils.isUS(view.locationCountry)

                        if (!settingsManager.isWeatherLoaded()) {
                            // Default US provider to NWS
                            if (isUS) {
                                settingsManager.setAPI(WeatherAPI.NWS)
                                view.updateWeatherSource(WeatherAPI.NWS)
                            } else {
                                settingsManager.setAPI(WeatherAPI.WEATHERUNLOCKED)
                                view.updateWeatherSource(WeatherAPI.WEATHERUNLOCKED)
                            }
                            wm.updateAPI()
                        }

                        if (settingsManager.usePersonalKey() && StringUtils.isNullOrWhitespace(settingsManager.getAPIKEY()) && wm.isKeyRequired()) {
                            throw CustomException(R.string.werror_invalidkey)
                        }

                        ensureActive()

                        if (WeatherAPI.NWS == settingsManager.getAPI() && !isUS) {
                            throw CustomException(R.string.error_message_weather_us_only)
                        }

                        // Get Weather Data
                        val location = LocationData(view, mLocation!!)
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
                        settingsManager.saveWeatherForecasts(Forecasts(weather.query, weather.forecast, weather.txtForecast))
                        settingsManager.saveWeatherForecasts(location.query, weather.hrForecast?.map { input -> HourlyForecasts(weather.query, input) })

                        // If we're changing locations, trigger an update
                        if (settingsManager.isWeatherLoaded()) {
                            LocalBroadcastManager.getInstance(fragmentActivity)
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
                                val intent = Intent(fragmentActivity, MainActivity::class.java)
                                intent.putExtra(Constants.KEY_DATA, JSONParser.serializer(data, LocationData::class.java))
                                startActivity(intent)
                                fragmentActivity.finishAffinity()
                            } else {
                                runWithView {
                                    enableControls(true)
                                }
                            }
                        } else {
                            runWithView {
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
        if (ContextCompat.checkSelfPermission(fragmentActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(fragmentActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSION_LOCATION_REQUEST_CODE)
            return
        }

        var location: Location? = null

        val locMan = fragmentActivity.getSystemService(Context.LOCATION_SERVICE) as LocationManager?

        if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
            throw CustomException(R.string.error_enable_location_services)
        }

        if (WearableHelper.isGooglePlayServicesInstalled()) {
            location = withContext(Dispatchers.IO) {
                withTimeoutOrNull(10000) {
                    mFusedLocationClient?.lastLocation?.await()
                }
            }

            coroutineContext.ensureActive()

            /*
             * Request start of location updates. Does nothing if
             * updates have already been requested.
             */
            if (location == null && !mRequestingLocationUpdates) {
                Timber.tag(TAG).i("Fused: Requesting location updates...")

                val mLocationRequest = LocationRequest.create().apply {
                    numUpdates = 1
                    interval = 10000
                    fastestInterval = 1000
                    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                }
                mRequestingLocationUpdates = true
                mFusedLocationClient!!.requestLocationUpdates(mLocationRequest, mLocCallback!!, Looper.getMainLooper())
                mMainHandler.postDelayed(cancelLocRequestRunner, 30000)
            }
        } else {
            val isGPSEnabled = locMan.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetEnabled = locMan.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            coroutineContext.ensureActive()

            if (isGPSEnabled) {
                location = locMan.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            }

            if (isGPSEnabled || isNetEnabled) {
                val provider = if (isGPSEnabled) LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER

                if ((isGPSEnabled && location == null) || (!isGPSEnabled && isNetEnabled)) {
                    location = locMan.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                }

                if (location == null) {
                    Timber.tag(TAG).i("LocMan: Requesting location update...")

                    mRequestingLocationUpdates = true
                    locMan.requestSingleUpdate(provider, mLocListnr!!, Looper.getMainLooper())
                    mMainHandler.postDelayed(cancelLocRequestRunner, 30000)
                }
            }
        }

        coroutineContext.ensureActive()

        if (location != null && !mRequestingLocationUpdates) {
            mLocation = location
            fetchGeoLocation()
        }
    }

    private val cancelLocRequestRunner = Runnable {
        stopLocationUpdates()
        enableControls(true)
        showToast(R.string.error_retrieve_location, Toast.LENGTH_SHORT)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
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
                    runWithView {
                        enableControls(true)
                        showToast(R.string.error_location_denied, Toast.LENGTH_SHORT)
                    }
                }
            }
        }
    }
}