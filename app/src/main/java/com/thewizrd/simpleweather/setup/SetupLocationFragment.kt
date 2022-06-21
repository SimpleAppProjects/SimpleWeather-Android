package com.thewizrd.simpleweather.setup

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
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
import com.thewizrd.common.helpers.LocationPermissionLauncher
import com.thewizrd.common.helpers.locationPermissionEnabled
import com.thewizrd.common.location.LocationProvider
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.exceptions.ErrorStatus
import com.thewizrd.shared_resources.exceptions.WeatherException
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.toLocationData
import com.thewizrd.shared_resources.remoteconfig.remoteConfigService
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.shared_resources.utils.CustomException
import com.thewizrd.shared_resources.utils.JSONParser
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
import kotlin.coroutines.coroutineContext

class SetupLocationFragment : CustomFragment() {
    companion object {
        private const val TAG = "SetupLocationFragment"
    }

    // Views
    private lateinit var binding: FragmentSetupLocationBinding
    private var mLocation: Location? = null
    private lateinit var locationProvider: LocationProvider

    private val viewModel: SetupViewModel by activityViewModels()

    private val wm = weatherModule.weatherManager

    private var job: Job? = null

    private lateinit var locationPermissionLauncher: LocationPermissionLauncher

    override fun createSnackManager(activity: Activity): SnackbarManager? {
        val mStepperNavBar = activity.findViewById<View>(R.id.bottom_nav_bar)
        return SnackbarManager(binding.root).apply {
            setSwipeDismissEnabled(true)
            setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE)
            setAnchorView(mStepperNavBar)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AnalyticsLogger.logEvent("SetupLocation: onCreate")

        // Hold fragment in place for MaterialContainerTransform
        exitTransition = Hold().setDuration(Constants.ANIMATION_DURATION.toLong())

        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)

        // Location Listener
        locationProvider = LocationProvider(requireActivity())

        locationPermissionLauncher = LocationPermissionLauncher(
            requireActivity(),
            locationCallback = { granted ->
                if (granted) {
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
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSetupLocationBinding.inflate(inflater, container, false)

        binding.progressBar.visibility = View.GONE

        /* Event Listeners */
        binding.searchBar.searchViewContainer.setOnClickListener { v ->
            v.isEnabled = false
            binding.gpsFollow.isEnabled = false

            // Setup search UI
            activity?.let {
                val bottomNavBar = it.findViewById<View>(R.id.bottom_nav_bar)
                bottomNavBar.visibility = View.GONE

                v.findNavController()
                    .safeNavigate(
                        SetupLocationFragmentDirections.actionSetupLocationFragmentToLocationSearchFragment3(),
                        FragmentNavigator.Extras.Builder()
                            .addSharedElement(v, Constants.SHARED_ELEMENT)
                            .build()
                    )
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

    override fun onResume() {
        super.onResume()
        AnalyticsLogger.logEvent("SetupLocation: onResume")
    }

    override fun onPause() {
        AnalyticsLogger.logEvent("SetupLocation: onPause")
        job?.cancel()
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
                            runWithView(Dispatchers.Main.immediate) {
                                if (mLocation == null) {
                                    // Restore controls
                                    enableControls(true)
                                    settingsManager.setFollowGPS(false)
                                    settingsManager.setWeatherLoaded(false)
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
                        settingsManager.saveLastGPSLocData(location)
                        settingsManager.deleteLocations()
                        settingsManager.addLocation(view.toLocationData())
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

                        settingsManager.setFollowGPS(true)
                        settingsManager.setWeatherLoaded(true)

                        // Send data for wearables
                        context?.let {
                            WearableWorker.enqueueAction(
                                it,
                                WearableWorkerActions.ACTION_SENDUPDATE
                            )
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
                                    settingsManager.setFollowGPS(false)

                                    context?.let { ctx ->
                                        val locMan =
                                            ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

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
                                settingsManager.setFollowGPS(false)
                                settingsManager.setWeatherLoaded(false)

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
        context?.let {
            if (!it.locationPermissionEnabled()) {
                locationPermissionLauncher.requestLocationPermission()
                return
            }
        }

        val locMan = context?.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

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