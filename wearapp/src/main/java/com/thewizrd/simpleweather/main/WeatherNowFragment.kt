package com.thewizrd.simpleweather.main

import android.annotation.SuppressLint
import android.content.*
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.support.wearable.input.RotaryEncoder
import android.util.Log
import android.view.*
import android.view.View.OnGenericMotionListener
import android.widget.Toast
import androidx.core.location.LocationManagerCompat
import androidx.core.util.ObjectsCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.findNavController
import androidx.wear.widget.drawer.WearableActionDrawerView
import com.google.android.gms.location.*
import com.ibm.icu.util.ULocale
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.controls.*
import com.thewizrd.shared_resources.helpers.ContextUtils
import com.thewizrd.shared_resources.helpers.locationPermissionEnabled
import com.thewizrd.shared_resources.helpers.requestLocationPermission
import com.thewizrd.shared_resources.location.LocationProvider
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.tzdb.TZDBCache
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.wearable.WearableDataSync
import com.thewizrd.shared_resources.wearable.WearableHelper
import com.thewizrd.shared_resources.weatherdata.*
import com.thewizrd.shared_resources.weatherdata.WeatherRequest.WeatherErrorListener
import com.thewizrd.shared_resources.weatherdata.model.LocationType
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.App
import com.thewizrd.simpleweather.NavGraphDirections
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.controls.ForecastPanelsViewModel
import com.thewizrd.simpleweather.databinding.FragmentWeatherNowBinding
import com.thewizrd.simpleweather.fragments.CustomFragment
import com.thewizrd.simpleweather.services.WeatherUpdaterWorker
import com.thewizrd.simpleweather.services.WidgetUpdaterWorker
import com.thewizrd.simpleweather.wearable.WearableWorker
import kotlinx.coroutines.*
import timber.log.Timber
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*
import kotlin.coroutines.coroutineContext

class WeatherNowFragment : CustomFragment(), OnSharedPreferenceChangeListener, WeatherErrorListener {
    companion object {
        private const val PERMISSION_LOCATION_REQUEST_CODE = 0
    }

    init {
        arguments = Bundle()
    }

    private lateinit var args: WeatherNowFragmentArgs

    private val wm = WeatherManager.instance
    private var wLoader: WeatherDataLoader? = null

    // Views
    private lateinit var binding: FragmentWeatherNowBinding
    private lateinit var mDrawerView: WearableActionDrawerView

    // Data
    private var locationData: LocationData? = null
    private lateinit var weatherLiveData: MutableLiveData<Weather>

    // View Models
    private val weatherView: WeatherNowViewModel by activityViewModels()
    private val forecastsView: ForecastsListViewModel by activityViewModels()
    private val forecastPanelsView: ForecastPanelsViewModel by activityViewModels()
    private val alertsView: WeatherAlertsViewModel by activityViewModels()

    // GPS location
    private var mLocation: Location? = null
    private lateinit var locationProvider: LocationProvider
    private lateinit var locationCallback: LocationProvider.Callback

    /**
     * Tracks the status of the location updates request.
     */
    private var mRequestingLocationUpdates = false

    // Data sync
    private lateinit var syncDataReceiver: BroadcastReceiver
    private var syncReceiverRegistered = false

    // Timer for timing out of operations
    private var syncTimer: Timer? = null
    private var syncTimerEnabled = false

    private val weatherObserver = Observer<Weather> { weather ->
        if (weather != null && weather.isValid) {
            weatherView.updateView(weather)

            if (locationData != null) {
                forecastPanelsView.updateForecasts(locationData!!)
                forecastsView.updateForecasts(locationData!!)
            }
            if (locationData?.locationType == LocationType.GPS) {
                binding.gpsIcon.visibility = View.VISIBLE
            } else {
                binding.gpsIcon.visibility = View.GONE
            }

            binding.swipeRefreshLayout.isRefreshing = false
            binding.scrollView.visibility = View.VISIBLE
            binding.scrollView.requestFocus() // View is now visible; take focus

            val context = App.instance.appContext
            val span = Duration.between(
                ZonedDateTime.now(ZoneOffset.UTC).toLocalDateTime(),
                settingsManager.getUpdateTime()
            )
            if (settingsManager.getDataSync() != WearableDataSync.OFF && span.toMinutes() > SettingsManager.DEFAULTINTERVAL) {
                WeatherUpdaterWorker.enqueueAction(
                    context,
                    WeatherUpdaterWorker.ACTION_UPDATEWEATHER
                )
            } else {
                lifecycleScope.launch(Dispatchers.Default) {
                    WidgetUpdaterWorker.requestWidgetUpdate(context)
                }
            }
        } else {
            showToast(R.string.werror_noweather, Toast.LENGTH_LONG)
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private val alertsObserver = Observer<List<WeatherAlertViewModel>> { data ->
        if (data?.isNotEmpty() == true) {
            binding.alertButton.visibility = View.VISIBLE
        }
    }

    override fun onWeatherError(wEx: WeatherException) {
        if (!wm.isRegionSupported(locationData!!.countryCode)) {
            showToast(R.string.error_message_weather_region_unsupported, Toast.LENGTH_LONG)
        } else {
            showToast(wEx.message, Toast.LENGTH_LONG)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        App.instance.registerAppSharedPreferenceListener(this)
    }

    override fun onDestroy() {
        wLoader = null
        super.onDestroy()
    }

    override fun onDetach() {
        App.instance.unregisterAppSharedPreferenceListener(this)
        wLoader = null
        super.onDetach()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (locationData != null) {
            outState.putString(Constants.KEY_DATA, JSONParser.serializer(locationData, LocationData::class.java))
        }
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AnalyticsLogger.logEvent("WeatherNowFragment: onCreate")

        args = WeatherNowFragmentArgs.fromBundle(requireArguments())

        if (savedInstanceState?.containsKey(Constants.KEY_DATA) == true) {
            locationData = JSONParser.deserializer(
                savedInstanceState.getString(Constants.KEY_DATA),
                LocationData::class.java
            )
        } else if (args.data != null) {
            locationData = JSONParser.deserializer(args.data, LocationData::class.java)
        }

        locationProvider = LocationProvider(fragmentActivity)
        locationCallback = object : LocationProvider.Callback {
            override fun onLocationChanged(location: Location?) {
                stopLocationUpdates()

                runWithView {
                    if (settingsManager.useFollowGPS() && updateLocation()) {
                        // Setup loader from updated location
                        wLoader = WeatherDataLoader(locationData!!)

                        refreshWeather(false)
                    }
                }
            }

            override fun onRequestTimedOut() {
                stopLocationUpdates()
            }
        }

        mRequestingLocationUpdates = false

        syncDataReceiver = object : BroadcastReceiver() {
            private var locationDataReceived = false
            private var weatherDataReceived = false

            override fun onReceive(context: Context, intent: Intent) {
                runWithView {
                    if (WearableHelper.LocationPath == intent.action || WearableHelper.WeatherPath == intent.action) {
                        if (WearableHelper.WeatherPath == intent.action) {
                            weatherDataReceived = true
                        }

                        if (WearableHelper.LocationPath == intent.action) {
                            // We got the location data
                            locationData = settingsManager.getHomeData()
                            locationDataReceived = true
                        }

                        Timber.tag("SyncDataReceiver").d("Action: %s", intent.action)

                        if (locationDataReceived && weatherDataReceived && locationData != null) {
                            if (syncTimerEnabled)
                                cancelTimer()

                            Timber.tag("SyncDataReceiver").d("Loading data...")

                            // We got all our data; now load the weather
                            if (!binding.swipeRefreshLayout.isRefreshing) {
                                binding.swipeRefreshLayout.isRefreshing = true
                            }

                            launch(Dispatchers.IO) {
                                supervisorScope {
                                    wLoader = WeatherDataLoader(locationData!!)
                                    val weather = wLoader!!.loadWeatherData(WeatherRequest.Builder()
                                            .forceLoadSavedData()
                                            .loadAlerts()
                                            .setErrorListener(this@WeatherNowFragment)
                                            .build())

                                    weatherLiveData.postValue(weather)
                                }
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

        // Live Data
        weatherLiveData = MutableLiveData()
        weatherLiveData.observe(this, weatherObserver)
        alertsView.getAlerts()?.observe(this, alertsObserver)

        lifecycle.addObserver(object : LifecycleObserver {
            private var wasStarted = false

            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            private fun onStart() {
                // Use normal if sync is off
                if (settingsManager.getDataSync() == WearableDataSync.OFF) {
                    resume()
                } else {
                    dataSyncResume()
                }
                wasStarted = true
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
            private fun onResume() {
                if (!wasStarted) onStart()
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            private fun onPause() {
                wasStarted = false
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_weather_now, container, false)
        binding.weatherView = weatherView
        binding.alertsView = alertsView
        binding.forecastsView = forecastPanelsView
        binding.lifecycleOwner = this

        val view = binding.root

        mDrawerView = fragmentActivity.findViewById(R.id.bottom_action_drawer)

        // SwipeRefresh
        binding.swipeRefreshLayout.setColorSchemeColors(ContextUtils.getColor(fragmentActivity, R.attr.colorPrimary))
        binding.swipeRefreshLayout.setOnRefreshListener {
            AnalyticsLogger.logEvent("WeatherNowFragment: onRefresh")

            runWithView {
                if (settingsManager.useFollowGPS() && updateLocation()) {
                    // Setup loader from updated location
                    wLoader = WeatherDataLoader(locationData!!)
                }

                refreshWeather(true)
            }
        }

        binding.scrollView.setOnGenericMotionListener(OnGenericMotionListener { v, event ->
            if (event.action == MotionEvent.ACTION_SCROLL && RotaryEncoder.isFromRotaryEncoder(event)) {

                // Don't forget the negation here
                val delta = -RotaryEncoder.getRotaryAxisValue(event) * RotaryEncoder.getScaledScrollFactor(fragmentActivity)

                // Swap these axes if you want to do horizontal scrolling instead
                v.scrollBy(0, Math.round(delta))

                return@OnGenericMotionListener true
            }
            false
        })

        binding.alertButton.setOnClickListener { v ->
            v.findNavController().navigate(WeatherNowFragmentDirections.actionGlobalWeatherAlertsFragment())
        }
        binding.conditionDetails.setOnClickListener { v ->
            v.findNavController()
                .navigate(WeatherNowFragmentDirections.actionGlobalWeatherDetailsFragment())
        }
        binding.forecastContainer.setOnClickListener { v ->
            v.findNavController()
                .navigate(WeatherNowFragmentDirections.actionGlobalWeatherForecastFragment())
        }
        binding.hourlyForecastContainer.setOnClickListener { v ->
            v.findNavController()
                .navigate(WeatherNowFragmentDirections.actionGlobalWeatherHrForecastFragment())
        }

        binding.noLocationsPrompt.setOnClickListener { v ->
            v.findNavController().navigate(NavGraphDirections.actionGlobalSetupActivity())
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        args = WeatherNowFragmentArgs.fromBundle(requireArguments())

        binding.swipeRefreshLayout.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            /* BoxInsetLayout impl */
            private val FACTOR = 0.146447f //(1 - sqrt(2)/2)/2
            private val mIsRound = resources.configuration.isScreenRound

            override fun onPreDraw(): Boolean {
                binding.swipeRefreshLayout.viewTreeObserver.removeOnPreDrawListener(this)
                binding.swipeRefreshLayout.isRefreshing = true

                val innerLayout = binding.scrollView.getChildAt(0)
                val peekContainer = mDrawerView.findViewById<View>(R.id.ws_drawer_view_peek_container)

                runWithView {
                    val verticalPadding = resources.getDimensionPixelSize(R.dimen.inner_frame_layout_padding)
                    val mScreenHeight = Resources.getSystem().displayMetrics.heightPixels
                    val mScreenWidth = Resources.getSystem().displayMetrics.widthPixels
                    val rightEdge = Math.min(binding.swipeRefreshLayout.measuredWidth, mScreenWidth)
                    val bottomEdge = Math.min(binding.swipeRefreshLayout.measuredHeight, mScreenHeight)
                    val verticalInset = (FACTOR * Math.max(rightEdge, bottomEdge)).toInt()
                    innerLayout.setPaddingRelative(
                            innerLayout.paddingStart,
                            verticalPadding,
                            innerLayout.paddingEnd,
                            if (mIsRound) verticalInset else peekContainer.height)
                }

                return true
            }
        })
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    private fun stopLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            Logger.writeLine(
                Log.DEBUG,
                "WeatherNowFragment: stopLocationUpdates: updates never requested, no-op."
            )
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
        AnalyticsLogger.logEvent("WeatherNowFragment: onResume")
        binding.scrollView.requestFocus()
    }

    override fun onPause() {
        AnalyticsLogger.logEvent("WeatherNowFragment: onPause")

        if (syncReceiverRegistered) {
            LocalBroadcastManager.getInstance(fragmentActivity)
                    .unregisterReceiver(syncDataReceiver)
            syncReceiverRegistered = false
        }

        if (syncTimerEnabled)
            cancelTimer()

        // Remove location updates to save battery.
        stopLocationUpdates()
        super.onPause()
    }

    private suspend fun verifyLocationData(): Boolean = withContext(Dispatchers.IO) {
        var locationChanged = false

        if (args.data != null) {
            val location = withContext(Dispatchers.IO) {
                JSONParser.deserializer(args.data, LocationData::class.java)
            }

            if (!ObjectsCompat.equals(location, locationData)) {
                locationData = location
                locationChanged = true
            }
        }

        locationChanged
    }

    private fun resume() {
        runWithView(Dispatchers.Default) {
            val locationChanged = verifyLocationData()

            if (locationChanged || wLoader == null) {
                restore()
            } else {
                // Refresh current fragment instance
                val currentLocale = ULocale.forLocale(LocaleUtils.getLocale())
                val locale =
                    wm.localeToLangCode(currentLocale.language, currentLocale.toLanguageTag())

                // Reset if source || locale is different
                if (settingsManager.getAPI() != weatherView.weatherSource ||
                        wm.supportsWeatherLocale() && locale != weatherView.weatherLocale) {
                    restore()
                } else {
                    // Update weather if needed on resume
                    if (settingsManager.useFollowGPS() && updateLocation()) {
                        // Setup loader from updated location
                        wLoader = WeatherDataLoader(locationData!!)
                    }

                    refreshWeather(false)
                }
            }
        }
    }

    private fun restore() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            supervisorScope {
                val task = async(Dispatchers.IO) {
                    var forceRefresh = false

                    // GPS Follow location
                    if (settingsManager.useFollowGPS() && (locationData == null || locationData!!.locationType == LocationType.GPS)) {
                        val locData = settingsManager.getLastGPSLocData()
                        if (locData == null) {
                            // Update location if not setup
                            updateLocation()
                            forceRefresh = true
                        } else {
                            // Reset locdata if source is different
                            if (settingsManager.getAPI() != locData.weatherSource) settingsManager.saveHomeData(LocationData())
                            if (updateLocation()) {
                                // Setup loader from updated location
                                forceRefresh = true
                            } else {
                                // Setup loader saved location data
                                locationData = locData
                            }
                        }
                    } else if (locationData == null && wLoader == null) {
                        // Weather was loaded before. Lets load it up...
                        locationData = settingsManager.getHomeData()
                    }

                    if (locationData?.isValid == true) {
                        wLoader = WeatherDataLoader(locationData!!)
                    } else {
                        // Show error prompt
                        binding.noLocationsPrompt.visibility = View.VISIBLE
                        this.cancel()
                    }
                    forceRefresh
                }

                task.invokeOnCompletion {
                    val t = task.getCompletionExceptionOrNull()
                    if (t == null) {
                        refreshWeather(task.getCompleted())
                    } else {
                        runWithView {
                            binding.swipeRefreshLayout.isRefreshing = false
                            binding.scrollView.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
    }

    private fun refreshWeather(forceRefresh: Boolean) {
        runWithView {
            binding.swipeRefreshLayout.isRefreshing = true

            if (settingsManager.getDataSync() == WearableDataSync.OFF) {
                val task = launch(Dispatchers.IO) {
                    supervisorScope {
                        val result = wLoader?.loadWeatherResult(
                            WeatherRequest.Builder()
                                .forceRefresh(forceRefresh)
                                .setErrorListener(this@WeatherNowFragment)
                                .build()
                        ) ?: throw CancellationException()

                        weatherLiveData.postValue(result.weather)

                        runWithView {
                            val isRound = resources.configuration.isScreenRound
                            binding.alertButton.visibility = if (isRound) View.INVISIBLE else View.GONE
                        }

                        val weatherAlerts = wLoader?.loadWeatherAlerts(result.isSavedData)

                        runWithView {
                            if (locationData != null) {
                                alertsView.updateAlerts(locationData!!)
                            }
                        }
                    }
                }

                task.invokeOnCompletion {
                    if (it != null) {
                        runWithView {
                            binding.swipeRefreshLayout.isRefreshing = false
                            binding.scrollView.visibility = View.VISIBLE
                        }
                    }
                }
            } else if (settingsManager.getDataSync() != WearableDataSync.OFF) {
                dataSyncResume(forceRefresh)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun updateLocation(): Boolean {
        var locationChanged = false

        if (settingsManager.getDataSync() == WearableDataSync.OFF &&
                settingsManager.useFollowGPS() && (locationData == null || locationData!!.locationType == LocationType.GPS)) {
            if (fragmentActivity != null && !fragmentActivity.locationPermissionEnabled()) {
                requestLocationPermission(PERMISSION_LOCATION_REQUEST_CODE)
                return false
            }

            val locMan =
                fragmentActivity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager?

            if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
                locationData = settingsManager.getHomeData()
                return false
            }

            val location = withContext(Dispatchers.IO) {
                val result: Location? = try {
                    withTimeoutOrNull(5000) {
                        locationProvider.getLastLocation()
                    }
                } catch (e: Exception) {
                    null
                }
                result
            }

            if (!coroutineContext.isActive) return false

            /*
             * Request start of location updates. Does nothing if
             * updates have already been requested.
             */
            if (location == null && !mRequestingLocationUpdates) {
                mRequestingLocationUpdates = true
                locationProvider.requestSingleUpdate(
                    locationCallback,
                    Looper.getMainLooper(),
                    30000
                )
            }

            if (location != null && !mRequestingLocationUpdates) {
                var lastGPSLocData = settingsManager.getLastGPSLocData()

                // Check previous location difference
                if (lastGPSLocData?.query != null &&
                    mLocation != null && ConversionMethods.calculateGeopositionDistance(
                        mLocation,
                        location
                    ) < 1600
                ) {
                    return false
                }

                if (lastGPSLocData?.query != null &&
                        Math.abs(ConversionMethods.calculateHaversine(lastGPSLocData.latitude, lastGPSLocData.longitude,
                                location.latitude, location.longitude)) < 1600) {
                    return false
                }

                val view = try {
                    withContext(Dispatchers.IO) {
                        wm.getLocation(location)
                    }
                } catch (e: WeatherException) {
                    showToast(R.string.error_retrieve_location, Toast.LENGTH_SHORT)
                    return false
                }

                if (view == null || view.locationQuery.isNullOrBlank()) {
                    // Stop since there is no valid query
                    return false
                } else if (view.locationTZLong.isNullOrBlank() && view.locationLat != 0.0 && view.locationLong != 0.0) {
                    val tzId = TZDBCache.getTimeZone(view.locationLat, view.locationLong)
                    if ("unknown" != tzId)
                        view.locationTZLong = tzId
                }

                if (!coroutineContext.isActive) return false

                // Save location as last known
                lastGPSLocData = LocationData(view, location)
                settingsManager.saveHomeData(lastGPSLocData)

                locationData = lastGPSLocData
                mLocation = location
                locationChanged = true
            }
        }

        return locationChanged
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_LOCATION_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    // Do the task you need to do.
                    runWithView {
                        if (settingsManager.useFollowGPS() && updateLocation()) {
                            // Setup loader from updated location
                            wLoader = WeatherDataLoader(locationData!!)

                            refreshWeather(false)
                        }
                    }
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    settingsManager.setFollowGPS(false)
                    showToast(R.string.error_location_denied, Toast.LENGTH_SHORT)
                }
            }
        }
    }

    /* Data Sync */
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        if (key.isNullOrBlank()) return

        when (key) {
            SettingsManager.KEY_DATASYNC -> {
                // If data sync settings changes,
                // reset so we can properly reload
                wLoader = null
                locationData = null
            }
            SettingsManager.KEY_TEMPUNIT,
            SettingsManager.KEY_DISTANCEUNIT,
            SettingsManager.KEY_PRECIPITATIONUNIT,
            SettingsManager.KEY_PRESSUREUNIT,
            SettingsManager.KEY_SPEEDUNIT,
            SettingsManager.KEY_ICONSSOURCE -> {
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    refreshWeather(false)
                }
            }
        }
    }

    private fun dataSyncRestore(forceRefresh: Boolean = false) {
        runWithView {
            // Send request to service to get weather data
            binding.swipeRefreshLayout.isRefreshing = true

            // Check data map if data is available to load
            wLoader = null
            locationData = null
            WearableWorker.enqueueAction(fragmentActivity, WearableWorker.ACTION_REQUESTUPDATE, forceRefresh)

            // Start timeout timer
            resetTimer()
        }
    }

    private fun dataSyncResume(forceRefresh: Boolean = false) {
        runWithView {
            if (!isViewAlive) {
                cancelDataSync()
                return@runWithView
            }

            if (!syncReceiverRegistered) {
                val filter = IntentFilter().apply {
                    addAction(WearableHelper.LocationPath)
                    addAction(WearableHelper.WeatherPath)
                }

                LocalBroadcastManager.getInstance(fragmentActivity)
                    .registerReceiver(syncDataReceiver, filter)

                syncReceiverRegistered = true
            }

            if (wLoader == null || forceRefresh) {
                dataSyncRestore(forceRefresh)
            } else {
                // Update weather if needed on resume
                if (locationData == null || locationData != settingsManager.getHomeData())
                    locationData = settingsManager.getHomeData()

                binding.swipeRefreshLayout.isRefreshing = true

                runWithView(Dispatchers.IO) {
                    supervisorScope {
                        wLoader = WeatherDataLoader(locationData!!)
                        val weather = wLoader!!.loadWeatherData(
                            WeatherRequest.Builder()
                                .forceLoadSavedData()
                                .loadAlerts()
                                .setErrorListener(this@WeatherNowFragment)
                                .build()
                        )

                        if (weather != null) {
                            weatherLiveData.postValue(weather)
                        } else {
                            // Data is null; restore
                            dataSyncRestore()
                        }
                    }
                }
            }
        }
    }

    private suspend fun cancelDataSync() {
        if (syncTimerEnabled)
            cancelTimer()

        if (isViewAlive && settingsManager.getDataSync() != WearableDataSync.OFF) {
            if (locationData == null) {
                // Load whatever we have available
                locationData = settingsManager.getHomeData()
            }

            if (locationData != null) {
                binding.swipeRefreshLayout.isRefreshing = true

                runWithView(Dispatchers.IO) {
                    supervisorScope {
                        wLoader = WeatherDataLoader(locationData!!)
                        val weather = wLoader!!.loadWeatherData(WeatherRequest.Builder()
                                .forceLoadSavedData()
                                .loadAlerts()
                                .setErrorListener(this@WeatherNowFragment)
                                .build())

                        weatherLiveData.postValue(weather)
                    }
                }
            } else {
                binding.swipeRefreshLayout.isRefreshing = false
                showToast(R.string.error_syncing, Toast.LENGTH_LONG)
            }
        }
    }

    private fun resetTimer() {
        if (syncTimerEnabled)
            cancelTimer()

        syncTimer = Timer()
        syncTimer!!.schedule(object : TimerTask() {
            override fun run() {
                // We hit the interval
                // Data syncing is taking a long time to setup
                // Stop and load saved data
                Timber.d("WeatherNow: resetTimer: timeout")
                runWithView { cancelDataSync() }
            }
        }, 35000) // 35sec

        syncTimerEnabled = true
    }

    private fun cancelTimer() {
        syncTimer?.cancel()
        syncTimer?.purge()
        syncTimerEnabled = false
    }
}