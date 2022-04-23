package com.thewizrd.simpleweather.setup

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.location.LocationManagerCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigator
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.transition.Hold
import com.google.android.material.transition.MaterialSharedAxis
import com.thewizrd.common.helpers.locationPermissionEnabled
import com.thewizrd.common.helpers.requestLocationPermission
import com.thewizrd.common.location.LocationProvider
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.exceptions.ErrorStatus
import com.thewizrd.shared_resources.exceptions.WeatherException
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.toLocationData
import com.thewizrd.shared_resources.remoteconfig.remoteConfigService
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.shared_resources.utils.CustomException
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.weatherdata.model.Forecasts
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecasts
import com.thewizrd.simpleweather.BuildConfig
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.databinding.FragmentSetupLocationBinding
import com.thewizrd.simpleweather.fragments.CustomFragment
import com.thewizrd.simpleweather.snackbar.Snackbar
import com.thewizrd.simpleweather.snackbar.SnackbarManager
import com.thewizrd.simpleweather.utils.NavigationUtils.safeNavigate
import com.thewizrd.simpleweather.wearable.WearableWorker
import com.thewizrd.simpleweather.wearable.WearableWorkerActions
import com.thewizrd.weather_api.weatherModule
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.coroutines.coroutineContext

class SetupLocationFragment : CustomFragment() {
    companion object {
        private const val TAG = "SetupLocationFragment"
        private const val PERMISSION_LOCATION_REQUEST_CODE = 0
    }

    // Views
    private lateinit var binding: FragmentSetupLocationBinding
    private var mLocation: Location? = null
    private lateinit var locationProvider: LocationProvider
    private lateinit var locationCallback: LocationProvider.Callback

    /**
     * Tracks the status of the location updates request.
     */
    private var mRequestingLocationUpdates = false
    private val mMainHandler = Handler(Looper.getMainLooper())

    private val viewModel: SetupViewModel by activityViewModels()

    private val wm = weatherModule.weatherManager

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
        locationProvider = LocationProvider(appCompatActivity!!)
        locationCallback = object : LocationProvider.Callback {
            override fun onLocationChanged(location: Location?) {
                stopLocationUpdates()
                mLocation = location

                Timber.tag(TAG).i("Location update received...")

                runWithView {
                    if (mLocation == null) {
                        enableControls(true)
                        context?.let {
                            showSnackbar(
                                Snackbar.make(
                                    it,
                                    R.string.error_retrieve_location,
                                    Snackbar.Duration.SHORT
                                )
                            )
                        }
                    } else {
                        fetchGeoLocation()
                    }
                }
            }

            override fun onRequestTimedOut() {
                stopLocationUpdates()
                enableControls(true)
                context?.let {
                    showSnackbar(
                        Snackbar.make(
                            it,
                            R.string.error_retrieve_location,
                            Snackbar.Duration.SHORT
                        )
                    )
                }
            }
        }
        mRequestingLocationUpdates = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSetupLocationBinding.inflate(inflater, container, false)

        binding.progressBar.visibility = View.GONE

        /* Event Listeners */
        binding.searchBar.searchViewContainer.setOnClickListener { v ->
            v.isEnabled = false
            binding.gpsFollow.isEnabled = false

            // Setup search UI
            val bottomNavBar = appCompatActivity!!.findViewById<View>(R.id.bottom_nav_bar)
            bottomNavBar.visibility = View.GONE

            v.findNavController()
                .safeNavigate(
                    SetupLocationFragmentDirections.actionSetupLocationFragmentToLocationSearchFragment3(),
                    FragmentNavigator.Extras.Builder()
                        .addSharedElement(v, Constants.SHARED_ELEMENT)
                        .build()
                )
        }
        ViewCompat.setTransitionName(binding.searchBar.searchViewContainer, Constants.SHARED_ELEMENT)

        binding.gpsFollow.setOnClickListener { fetchGeoLocation() }

        // Reset focus
        binding.root.requestFocus()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navController = view.findNavController()
        navController.currentBackStackEntry?.savedStateHandle
            ?.getLiveData<String>(Constants.KEY_DATA)
            ?.observe(viewLifecycleOwner) { result ->
                viewLifecycleOwner.lifecycleScope.launch {
                    // Do something with the result.
                    enableControls(false)

                    if (result != null) {
                        // Save data
                        val data = withContext(Dispatchers.Default) {
                            JSONParser.deserializer(result, LocationData::class.java)
                        }

                            if (data != null) {
                                // Setup complete
                                viewModel.locationData = data
                                navController.safeNavigate(
                                    SetupLocationFragmentDirections.actionSetupLocationFragmentToSetupSettingsFragment()
                                )
                                return@launch
                            }
                        }
                        enableControls(true)
                    }
            }
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
        locationProvider.stopLocationUpdates()
        mRequestingLocationUpdates = false
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
            val navController = binding.root.findNavController()

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
                                    getSettingsManager().setFollowGPS(false)
                                    getSettingsManager().setWeatherLoaded(false)
                                    context?.let {
                                        if (e is WeatherException || e is CustomException) {
                                            showSnackbar(
                                                Snackbar.make(
                                                    it,
                                                    e.message,
                                                    Snackbar.Duration.SHORT
                                                ), null
                                            )
                                        } else {
                                            showSnackbar(
                                                Snackbar.make(
                                                    it,
                                                    R.string.error_retrieve_location,
                                                    Snackbar.Duration.SHORT
                                                ), null
                                            )
                                        }
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

                        if (!getSettingsManager().isWeatherLoaded() && !BuildConfig.IS_NONGMS) {
                            // Set default provider based on location
                            val provider =
                                remoteConfigService.getDefaultWeatherProvider(view.locationCountry)
                            getSettingsManager().setAPI(provider)
                            view.updateWeatherSource(provider)
                        }

                        if (getSettingsManager().usePersonalKey() && getSettingsManager().getAPIKey()
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

                        var weather = getSettingsManager().getWeatherData(location.query)
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
                        getSettingsManager().saveLastGPSLocData(location)
                        getSettingsManager().deleteLocations()
                        getSettingsManager().addLocation(view.toLocationData())
                        if (wm.supportsAlerts() && weather.weatherAlerts != null)
                            getSettingsManager().saveWeatherAlerts(location, weather.weatherAlerts)
                        getSettingsManager().saveWeatherData(weather)
                        getSettingsManager().saveWeatherForecasts(Forecasts(weather))
                        getSettingsManager().saveWeatherForecasts(location.query, weather.hrForecast?.map { input -> HourlyForecasts(weather.query, input) })

                        getSettingsManager().setFollowGPS(true)
                        getSettingsManager().setWeatherLoaded(true)

                        // Send data for wearables
                        if (appCompatActivity != null) {
                            WearableWorker.enqueueAction(appCompatActivity!!, WearableWorkerActions.ACTION_SENDUPDATE)
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
                                    navController.safeNavigate(
                                        SetupLocationFragmentDirections.actionSetupLocationFragmentToSetupSettingsFragment()
                                    )
                                } else {
                                    enableControls(true)
                                    getSettingsManager().setFollowGPS(false)

                                    val locMan =
                                        appCompatActivity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager?

                                    context?.let { ctx ->
                                        if (locMan == null || !LocationManagerCompat.isLocationEnabled(
                                                locMan
                                            )
                                        ) {
                                            showSnackbar(
                                                Snackbar.make(
                                                    ctx,
                                                    R.string.error_enable_location_services,
                                                    Snackbar.Duration.LONG
                                                ), null
                                            )
                                        } else {
                                            showSnackbar(
                                                Snackbar.make(
                                                    ctx,
                                                    R.string.error_retrieve_location,
                                                    Snackbar.Duration.SHORT
                                                ), null
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            runWithView {
                                // Restore controls
                                enableControls(true)
                                getSettingsManager().setFollowGPS(false)
                                getSettingsManager().setWeatherLoaded(false)

                                context?.let { ctx ->
                                    if (t is WeatherException || t is CustomException) {
                                        showSnackbar(
                                            Snackbar.make(
                                                ctx,
                                                t.message,
                                                Snackbar.Duration.SHORT
                                            ), null
                                        )
                                    } else {
                                        showSnackbar(
                                            Snackbar.make(
                                                ctx,
                                                R.string.error_retrieve_location,
                                                Snackbar.Duration.SHORT
                                            ), null
                                        )
                                    }
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
        if (appCompatActivity != null && !appCompatActivity!!.locationPermissionEnabled()) {
            this.requestLocationPermission(PERMISSION_LOCATION_REQUEST_CODE)
            return
        }

        val locMan =
            appCompatActivity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager?

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
                        showSnackbar(
                            Snackbar.make(
                                requireContext(),
                                R.string.error_location_denied,
                                Snackbar.Duration.SHORT
                            )
                        )
                    }
                }
            }
        }
    }
}