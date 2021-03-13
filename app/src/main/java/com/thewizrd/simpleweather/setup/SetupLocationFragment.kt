package com.thewizrd.simpleweather.setup

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
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
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigator
import com.google.android.gms.location.*
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.transition.Hold
import com.google.android.material.transition.MaterialSharedAxis
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.tzdb.TZDBCache
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.wearable.WearableHelper
import com.thewizrd.shared_resources.weatherdata.*
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.databinding.FragmentSetupLocationBinding
import com.thewizrd.simpleweather.fragments.CustomFragment
import com.thewizrd.simpleweather.snackbar.Snackbar
import com.thewizrd.simpleweather.snackbar.SnackbarManager
import com.thewizrd.simpleweather.wearable.WearableWorker
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import kotlin.coroutines.coroutineContext

class SetupLocationFragment : CustomFragment() {
    companion object {
        private const val TAG = "SetupLocationFragment"
        private const val PERMISSION_LOCATION_REQUEST_CODE = 0
    }

    // Views
    private lateinit var binding: FragmentSetupLocationBinding
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private var mLocation: Location? = null
    private var mLocCallback: LocationCallback? = null
    private var mLocListnr: LocationListener? = null

    /**
     * Tracks the status of the location updates request.
     */
    private var mRequestingLocationUpdates = false
    private val mMainHandler = Handler(Looper.getMainLooper())

    private val viewModel: SetupViewModel by activityViewModels()

    private val wm = WeatherManager.getInstance()

    private var job: Job? = null

    override fun createSnackManager(): SnackbarManager {
        val mStepperNavBar = appCompatActivity!!.findViewById<View>(R.id.bottom_nav_bar)
        val mSnackMgr = SnackbarManager(binding.root)
        mSnackMgr.setSwipeDismissEnabled(true)
        mSnackMgr.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE)
        mSnackMgr.setAnchorView(mStepperNavBar)
        return mSnackMgr
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AnalyticsLogger.logEvent("SetupLocation: onCreate")

        // Hold fragment in place for MaterialContainerTransform
        exitTransition = Hold().setDuration(Constants.ANIMATION_DURATION.toLong())

        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)

        // Location Listener
        if (WearableHelper.isGooglePlayServicesInstalled()) {
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(appCompatActivity!!)
            mLocCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    stopLocationUpdates()
                    mMainHandler.removeCallbacks(cancelLocRequestRunner)
                    mLocation = locationResult.lastLocation

                    Timber.tag(TAG).i("Fused: Location update received...")

                    runWithView {
                        if (mLocation == null) {
                            enableControls(true)
                            showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT), null)
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
                            showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT), null)
                        }
                    }
                }
            }
        } else {
            mLocListnr = object : LocationListener {
                @SuppressLint("MissingPermission")
                override fun onLocationChanged(location: Location) {
                    mMainHandler.removeCallbacks(cancelLocRequestRunner)
                    stopLocationUpdates()

                    Timber.tag(TAG).i("LocMan: Location update received...")

                    mLocation = location
                    runWithView {
                        if (mLocation == null) {
                            enableControls(true)
                            showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT), null)
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSetupLocationBinding.inflate(inflater, container, false)

        binding.progressBar.visibility = View.GONE

        /* Event Listeners */
        binding.searchBar.searchViewContainer.setOnClickListener { v ->
            v.isEnabled = false
            binding.gpsFollow.isEnabled = false

            // Setup search UI
            val bottomNavBar = appCompatActivity!!.findViewById<View>(R.id.bottom_nav_bar)
            bottomNavBar.visibility = View.GONE

            try {
                Navigation.findNavController(v)
                        .navigate(
                                SetupLocationFragmentDirections.actionSetupLocationFragmentToLocationSearchFragment3(),
                                FragmentNavigator.Extras.Builder().addSharedElement(v, Constants.SHARED_ELEMENT)
                                        .build()
                        )
            } catch (ex: IllegalArgumentException) {
                val props = Bundle().apply {
                    putString("method", "searchViewContainer.onClick")
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
        ViewCompat.setTransitionName(binding.searchBar.searchViewContainer, Constants.SHARED_ELEMENT)

        binding.gpsFollow.setOnClickListener { fetchGeoLocation() }

        // Reset focus
        binding.root.requestFocus()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navController = Navigation.findNavController(view)
        val liveData = navController.currentBackStackEntry!!
                .savedStateHandle
                .getLiveData<String>(Constants.KEY_DATA)
        liveData.observe(viewLifecycleOwner, Observer { result ->
            // Do something with the result.
            enableControls(false)
            if (result != null) {
                // Save data
                val data = JSONParser.deserializer(result, LocationData::class.java)
                if (data != null) {
                    // Setup complete
                    viewModel.locationData = data
                    navController.navigate(
                            SetupLocationFragmentDirections.actionSetupLocationFragmentToSetupSettingsFragment()
                    )
                    return@Observer
                }
            }
            enableControls(true)
        })
    }

    override fun onDestroyView() {
        job?.cancel()
        super.onDestroyView()
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    @SuppressLint("MissingPermission")
    private fun stopLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            Logger.writeLine(Log.DEBUG, "SetupLocationFragment: stopLocationUpdates: updates never requested, no-op.")
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
            val locMan = appCompatActivity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
            locMan?.removeUpdates(it)
            mRequestingLocationUpdates = false
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsLogger.logEvent("SetupLocation: onResume")
    }

    override fun onPause() {
        AnalyticsLogger.logEvent("SetupLocation: onPause")
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
        binding.searchBar.searchViewContainer.isEnabled = enable
        binding.gpsFollow.isEnabled = enable
        binding.progressBar.visibility = if (enable) View.GONE else View.VISIBLE
    }

    private fun fetchGeoLocation() {
        runWithView(Dispatchers.Main.immediate) {
            // Show loading bar
            binding.gpsFollow.isEnabled = false
            binding.progressBar.visibility = View.VISIBLE

            if (mLocation == null) {
                // Cancel other tasks
                job?.cancel()

                supervisorScope {
                    job = launch {
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
                                    Settings.setFollowGPS(false)
                                    Settings.setWeatherLoaded(false)
                                    if (e is WeatherException || e is CustomException) {
                                        showSnackbar(Snackbar.make(e.message, Snackbar.Duration.SHORT), null)
                                    } else {
                                        showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT), null)
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
                            wm.getLocation(mLocation)
                        }

                        if (view == null || view.locationQuery?.isBlank() == true) {
                            throw CustomException(R.string.error_retrieve_location)
                        } else if (view.locationTZLong?.isBlank() == true && view.locationLat != 0.0 && view.locationLong != 0.0) {
                            val tzId = TZDBCache.getTimeZone(view.locationLat, view.locationLong)
                            if ("unknown" != tzId)
                                view.locationTZLong = tzId
                        }

                        val isUS = LocationUtils.isUS(view.locationCountry)

                        if (!Settings.isWeatherLoaded()) {
                            // Default US provider to NWS
                            if (isUS) {
                                Settings.setAPI(WeatherAPI.NWS)
                                view.updateWeatherSource(WeatherAPI.NWS)
                            } else {
                                Settings.setAPI(WeatherAPI.WEATHERUNLOCKED)
                                view.updateWeatherSource(WeatherAPI.WEATHERUNLOCKED)
                            }
                            wm.updateAPI()
                        }

                        if (Settings.usePersonalKey() && StringUtils.isNullOrWhitespace(Settings.getAPIKEY()) && wm.isKeyRequired) {
                            throw CustomException(R.string.werror_invalidkey)
                        }

                        ensureActive()

                        if (WeatherAPI.NWS == Settings.getAPI() && !isUS) {
                            throw CustomException(R.string.error_message_weather_us_only)
                        }

                        // Get Weather Data
                        val location = LocationData(view, mLocation)
                        if (!location.isValid) {
                            throw CustomException(R.string.werror_noweather)
                        }

                        ensureActive()

                        var weather = Settings.getWeatherData(location.query)
                        if (weather == null) {
                            ensureActive()

                            weather = withContext(Dispatchers.IO) {
                                wm.getWeather(location)
                            }
                        }

                        if (weather == null) {
                            throw WeatherException(WeatherUtils.ErrorStatus.NOWEATHER)
                        } else if (wm.supportsAlerts() && wm.needsExternalAlertData()) {
                            weather.weatherAlerts = wm.getAlerts(location)
                        }

                        ensureActive()

                        // Save weather data
                        Settings.saveLastGPSLocData(location)
                        Settings.deleteLocations()
                        Settings.addLocation(LocationData(view))
                        if (wm.supportsAlerts() && weather.weatherAlerts != null)
                            Settings.saveWeatherAlerts(location, weather.weatherAlerts)
                        Settings.saveWeatherData(weather)
                        Settings.saveWeatherForecasts(Forecasts(weather.query, weather.forecast, weather.txtForecast))
                        Settings.saveWeatherForecasts(location.query, weather.hrForecast?.map { input -> HourlyForecasts(weather.query, input) })

                        Settings.setFollowGPS(true)
                        Settings.setWeatherLoaded(true)

                        // Send data for wearables
                        if (appCompatActivity != null) {
                            WearableWorker.enqueueAction(appCompatActivity!!, WearableWorker.ACTION_SENDUPDATE)
                        }

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

                            runWithView {
                                if (data.isValid) {
                                    // Setup complete
                                    viewModel.locationData = data
                                    try {
                                        binding.root.findNavController()
                                                .navigate(SetupLocationFragmentDirections.actionSetupLocationFragmentToSetupSettingsFragment())
                                    } catch (ex: IllegalStateException) {
                                        val args = Bundle().apply {
                                            putString("method", "fetchGeoLocation")
                                            putBoolean("isAlive", isAlive)
                                            putBoolean("isViewAlive", isViewAlive)
                                            putBoolean("isDetached", isDetached)
                                            putBoolean("isResumed", isResumed)
                                            putBoolean("isRemoving", isRemoving)
                                        }
                                        AnalyticsLogger.logEvent("$TAG: navigation failed", args)

                                        Logger.writeLine(Log.ERROR, ex)
                                    }
                                } else {
                                    enableControls(true)
                                    Settings.setFollowGPS(false)

                                    val locMan = appCompatActivity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager?

                                    if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
                                        showSnackbar(Snackbar.make(R.string.error_enable_location_services, Snackbar.Duration.LONG), null)
                                    } else {
                                        showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT), null)
                                    }
                                }
                            }
                        } else {
                            runWithView {
                                // Restore controls
                                enableControls(true)
                                Settings.setFollowGPS(false)
                                Settings.setWeatherLoaded(false)

                                if (t is WeatherException || t is CustomException) {
                                    showSnackbar(Snackbar.make(t.message, Snackbar.Duration.SHORT), null)
                                } else {
                                    showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT), null)
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
        if (appCompatActivity != null && ContextCompat.checkSelfPermission(appCompatActivity!!, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(appCompatActivity!!, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSION_LOCATION_REQUEST_CODE)
            return
        }

        var location: Location? = null

        val locMan = appCompatActivity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager?

        if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
            throw CustomException(R.string.error_enable_location_services)
        }

        if (WearableHelper.isGooglePlayServicesInstalled()) {
            location = withContext(Dispatchers.IO) {
                withTimeoutOrNull(5000) {
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
        showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT), null)
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
                        showSnackbar(Snackbar.make(R.string.error_location_denied, Snackbar.Duration.SHORT), null)
                    }
                }
            }
        }
    }
}