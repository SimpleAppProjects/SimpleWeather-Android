package com.thewizrd.simpleweather.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.wearable.input.RotaryEncoder
import android.util.Log
import android.view.*
import android.view.View.OnGenericMotionListener
import android.widget.Toast
import androidx.core.content.ContextCompat
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
import com.google.android.gms.tasks.Tasks
import com.ibm.icu.util.ULocale
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.controls.*
import com.thewizrd.shared_resources.helpers.ContextUtils
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.tzdb.TZDBCache
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.wearable.WearableDataSync
import com.thewizrd.shared_resources.wearable.WearableHelper
import com.thewizrd.shared_resources.weatherdata.*
import com.thewizrd.shared_resources.weatherdata.WeatherRequest.WeatherErrorListener
import com.thewizrd.simpleweather.App
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.controls.ForecastPanelsViewModel
import com.thewizrd.simpleweather.databinding.FragmentWeatherNowBinding
import com.thewizrd.simpleweather.fragments.CustomFragment
import com.thewizrd.simpleweather.services.WeatherUpdaterWorker
import com.thewizrd.simpleweather.wearable.WearableWorker
import com.thewizrd.simpleweather.wearable.WeatherComplicationWorker
import com.thewizrd.simpleweather.wearable.WeatherTileWorker
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.lang.Runnable
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

    private val wm = WeatherManager.getInstance()
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
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private var mLocation: Location? = null
    private var mLocCallback: LocationCallback? = null
    private var mLocListnr: LocationListener? = null

    /**
     * Tracks the status of the location updates request.
     */
    private var mRequestingLocationUpdates = false
    private val mMainHandler = Handler(Looper.getMainLooper())

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

            binding.swipeRefreshLayout.isRefreshing = false

            val context = App.getInstance().appContext
            val span = Duration.between(ZonedDateTime.now(ZoneOffset.UTC).toLocalDateTime(), Settings.getUpdateTime())
            if (Settings.getDataSync() != WearableDataSync.OFF && span.toMinutes() > Settings.DEFAULTINTERVAL) {
                WeatherUpdaterWorker.enqueueAction(context, WeatherUpdaterWorker.ACTION_UPDATEWEATHER)
            } else {
                // Update complications if they haven't been already
                WeatherComplicationWorker.enqueueAction(context, Intent(WeatherComplicationWorker.ACTION_UPDATECOMPLICATIONS))

                // Update tile if it hasn't been already
                WeatherTileWorker.enqueueAction(context, Intent(WeatherTileWorker.ACTION_UPDATETILES))
            }
        } else {
            Toast.makeText(fragmentActivity, R.string.werror_noweather, Toast.LENGTH_LONG).show()
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private val alertsObserver = Observer<List<WeatherAlertViewModel>> { data ->
        if (data?.isNotEmpty() == true) {
            binding.alertButton.visibility = View.VISIBLE
        }
    }

    override fun onWeatherError(wEx: WeatherException) {
        runWithView {
            if (wEx.errorStatus == WeatherUtils.ErrorStatus.QUERYNOTFOUND && WeatherAPI.NWS == Settings.getAPI()) {
                Toast.makeText(fragmentActivity, R.string.error_message_weather_us_only, Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(fragmentActivity, wEx.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        App.getInstance().preferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroy() {
        wLoader = null
        super.onDestroy()
    }

    override fun onDetach() {
        App.getInstance().preferences.unregisterOnSharedPreferenceChangeListener(this)
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
            locationData = JSONParser.deserializer(savedInstanceState.getString(Constants.KEY_DATA), LocationData::class.java)
        } else if (args.data != null) {
            locationData = JSONParser.deserializer(args.data, LocationData::class.java)
        }

        if (WearableHelper.isGooglePlayServicesInstalled()) {
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(fragmentActivity)
            mLocCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    stopLocationUpdates()
                    mMainHandler.removeCallbacks(cancelLocRequestRunner)

                    runWithView {
                        if (Settings.useFollowGPS() && updateLocation()) {
                            // Setup loader from updated location
                            wLoader = WeatherDataLoader(locationData!!)

                            refreshWeather(false)
                        }
                    }
                }

                override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                    stopLocationUpdates()
                    mMainHandler.removeCallbacks(cancelLocRequestRunner)
                }
            }
        } else {
            mLocListnr = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    stopLocationUpdates()
                    mMainHandler.removeCallbacks(cancelLocRequestRunner)

                    runWithView {
                        if (Settings.useFollowGPS() && updateLocation()) {
                            // Setup loader from updated location
                            wLoader = WeatherDataLoader(locationData!!)

                            refreshWeather(false)
                        }
                    }
                }

                override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
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
                            locationData = Settings.getHomeData()
                            locationDataReceived = true
                        }

                        Timber.tag("SyncDataReceiver").d("Action: %s", intent.action)

                        if (locationDataReceived && weatherDataReceived || weatherDataReceived && locationData != null) {
                            if (syncTimerEnabled)
                                cancelTimer()

                            Timber.tag("SyncDataReceiver").d("Loading data...")

                            // We got all our data; now load the weather
                            if (!binding.swipeRefreshLayout.isRefreshing) {
                                binding.swipeRefreshLayout.isRefreshing = true
                            }

                            wLoader = WeatherDataLoader(locationData!!)
                            wLoader!!.loadWeatherData(WeatherRequest.Builder()
                                    .forceLoadSavedData()
                                    .loadAlerts()
                                    .setErrorListener(this@WeatherNowFragment)
                                    .build())
                                    .addOnSuccessListener { weather ->
                                        weatherLiveData.value = weather
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
        alertsView.alerts.observe(this, alertsObserver)

        lifecycle.addObserver(object : LifecycleObserver {
            private var wasStarted = false

            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            private fun onStart() {
                // Use normal if sync is off
                if (Settings.getDataSync() == WearableDataSync.OFF) {
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
                if (Settings.useFollowGPS() && updateLocation()) {
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
            v.findNavController().navigate(WeatherNowFragmentDirections.actionGlobalWeatherDetailsFragment())
        }
        binding.forecastContainer.setOnClickListener { v ->
            v.findNavController().navigate(WeatherNowFragmentDirections.actionGlobalWeatherForecastFragment())
        }
        binding.hourlyForecastContainer.setOnClickListener { v ->
            v.findNavController().navigate(WeatherNowFragmentDirections.actionGlobalWeatherHrForecastFragment())
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
            val locMan = fragmentActivity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
            locMan?.removeUpdates(it)
            mRequestingLocationUpdates = false
        }
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
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Unconfined) {
            val locationChanged = verifyLocationData()

            if (locationChanged || wLoader == null) {
                restore()
            } else {
                // Refresh current fragment instance
                val currentLocale = ULocale.forLocale(LocaleUtils.getLocale())
                val locale = wm.localeToLangCode(currentLocale.language, currentLocale.toLanguageTag())

                // Reset if source || locale is different
                if (Settings.getAPI() != weatherView.weatherSource ||
                        wm.supportsWeatherLocale() && locale != weatherView.weatherLocale) {
                    restore()
                } else {
                    // Update weather if needed on resume
                    if (Settings.useFollowGPS() && updateLocation()) {
                        // Setup loader from updated location
                        wLoader = WeatherDataLoader(locationData!!)
                    }

                    refreshWeather(false)
                }
            }
        }
    }

    private fun restore() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Unconfined) {
            supervisorScope {
                val task = async(Dispatchers.IO) {
                    var forceRefresh = false

                    // GPS Follow location
                    if (Settings.useFollowGPS() && (locationData == null || locationData!!.locationType == LocationType.GPS)) {
                        val locData = Settings.getLastGPSLocData()
                        if (locData == null) {
                            // Update location if not setup
                            updateLocation()
                            forceRefresh = true
                        } else {
                            // Reset locdata if source is different
                            if (Settings.getAPI() != locData.weatherSource) Settings.saveHomeData(LocationData())
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
                        locationData = Settings.getHomeData()
                    }
                    if (locationData != null) wLoader = WeatherDataLoader(locationData!!)
                    forceRefresh
                }

                task.invokeOnCompletion {
                    val t = task.getCompletionExceptionOrNull()
                    if (t == null) {
                        refreshWeather(task.getCompleted())
                    }
                }
            }
        }
    }

    private fun refreshWeather(forceRefresh: Boolean) {
        runWithView {
            binding.swipeRefreshLayout.isRefreshing = true

            if (Settings.getDataSync() == WearableDataSync.OFF) {
                wLoader?.loadWeatherResult(WeatherRequest.Builder()
                        .forceRefresh(forceRefresh)
                        .setErrorListener(this@WeatherNowFragment)
                        .build())
                        ?.addOnSuccessListener { weather ->
                            weatherLiveData.value = weather.weather
                        }
                        ?.continueWithTask { task ->
                            if (task.isSuccessful) {
                                runWithView {
                                    val isRound = resources.configuration.isScreenRound
                                    binding.alertButton.visibility = if (isRound) View.INVISIBLE else View.GONE
                                }
                                wLoader?.loadWeatherAlerts(task.result.isSavedData)
                            } else {
                                Tasks.forCanceled()
                            }
                        }
                        ?.addOnCompleteListener {
                            runWithView {
                                if (locationData != null) {
                                    alertsView.updateAlerts(locationData!!)
                                }
                            }
                        }
            } else if (Settings.getDataSync() != WearableDataSync.OFF) {
                dataSyncResume(forceRefresh)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun updateLocation(): Boolean {
        var locationChanged = false

        if (Settings.getDataSync() == WearableDataSync.OFF &&
                Settings.useFollowGPS() && (locationData == null || locationData!!.locationType == LocationType.GPS)) {
            if (fragmentActivity != null && ContextCompat.checkSelfPermission(fragmentActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(fragmentActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                        PERMISSION_LOCATION_REQUEST_CODE)
                return false
            }

            var location: Location? = null

            val locMan = fragmentActivity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager?

            if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
                locationData = Settings.getHomeData()
                return false
            }

            if (WearableHelper.isGooglePlayServicesInstalled()) {
                location = withContext(Dispatchers.IO) {
                    val result: Location? = try {
                        withTimeoutOrNull(5000) {
                            mFusedLocationClient?.lastLocation?.await()
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

                if (!coroutineContext.isActive) return false

                if (isGPSEnabled || isNetEnabled) {
                    val locCriteria = Criteria().apply {
                        accuracy = Criteria.ACCURACY_COARSE
                        isCostAllowed = false
                        powerRequirement = Criteria.POWER_LOW
                    }

                    val provider = locMan.getBestProvider(locCriteria, true)!!
                    location = locMan.getLastKnownLocation(provider)

                    if (location == null) {
                        mRequestingLocationUpdates = true
                        locMan.requestSingleUpdate(provider, mLocListnr!!, Looper.getMainLooper())
                        mMainHandler.postDelayed(cancelLocRequestRunner, 30000)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(fragmentActivity, R.string.error_retrieve_location, Toast.LENGTH_SHORT).show()
                    }
                }
            }

            if (location != null && !mRequestingLocationUpdates) {
                val lastGPSLocData = Settings.getLastGPSLocData()

                // Check previous location difference
                if (lastGPSLocData?.query != null &&
                        mLocation != null && ConversionMethods.calculateGeopositionDistance(mLocation, location) < 1600) {
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
                    withContext(Dispatchers.Main) {
                        Toast.makeText(fragmentActivity, R.string.error_retrieve_location, Toast.LENGTH_SHORT).show()
                    }
                    return false
                }

                if (view.locationQuery?.isBlank() == true) {
                    // Stop since there is no valid query
                    return false
                } else if (view.locationTZLong?.isBlank() == true && view.locationLat != 0.0 && view.locationLong != 0.0) {
                    val tzId = TZDBCache.getTimeZone(view.locationLat, view.locationLong)
                    if ("unknown" != tzId)
                        view.locationTZLong = tzId
                }

                if (!coroutineContext.isActive) return false

                // Save location as last known
                lastGPSLocData?.setData(view, location)
                Settings.saveHomeData(lastGPSLocData)

                locationData = lastGPSLocData
                mLocation = location
                locationChanged = true
            }
        }

        return locationChanged
    }

    private val cancelLocRequestRunner = Runnable {
        stopLocationUpdates()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_LOCATION_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    // Do the task you need to do.
                    runWithView {
                        if (Settings.useFollowGPS() && updateLocation()) {
                            // Setup loader from updated location
                            wLoader = WeatherDataLoader(locationData!!)

                            refreshWeather(false)
                        }
                    }
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Settings.setFollowGPS(false)
                    runWithView {
                        Toast.makeText(fragmentActivity, R.string.error_location_denied, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    /* Data Sync */
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        if (key.isNullOrBlank()) return

        when (key) {
            Settings.KEY_DATASYNC -> {
                // If data sync settings changes,
                // reset so we can properly reload
                wLoader = null
                locationData = null
            }
            Settings.KEY_TEMPUNIT,
            Settings.KEY_DISTANCEUNIT,
            Settings.KEY_PRECIPITATIONUNIT,
            Settings.KEY_PRESSUREUNIT,
            Settings.KEY_SPEEDUNIT,
            Settings.KEY_ICONSSOURCE -> {
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
        if (!isViewAlive) {
            cancelDataSync()
            return
        }

        if (!syncReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(WearableHelper.LocationPath)
                addAction(WearableHelper.WeatherPath)
                addAction(WearableHelper.IsSetupPath)
            }

            LocalBroadcastManager.getInstance(fragmentActivity)
                    .registerReceiver(syncDataReceiver, filter)

            syncReceiverRegistered = true
        }

        if (wLoader == null || forceRefresh) {
            dataSyncRestore(forceRefresh)
        } else {
            // Update weather if needed on resume
            if (locationData == null || locationData != Settings.getHomeData())
                locationData = Settings.getHomeData()

            binding.swipeRefreshLayout.isRefreshing = true
            wLoader = WeatherDataLoader(locationData!!)
            wLoader!!.loadWeatherData(WeatherRequest.Builder()
                    .forceLoadSavedData()
                    .loadAlerts()
                    .setErrorListener(this@WeatherNowFragment)
                    .build())
                    .addOnSuccessListener(fun(weather: Weather?) {
                        if (weather != null) {
                            weatherLiveData.setValue(weather)
                        } else {
                            // Data is null; restore
                            dataSyncRestore()
                        }
                    })
        }
    }

    private fun cancelDataSync() {
        if (syncTimerEnabled)
            cancelTimer()

        if (isViewAlive && Settings.getDataSync() != WearableDataSync.OFF) {
            if (locationData == null) {
                // Load whatever we have available
                locationData = Settings.getHomeData()
            }

            if (locationData != null) {
                binding.swipeRefreshLayout.isRefreshing = true

                wLoader = WeatherDataLoader(locationData!!)
                wLoader!!.loadWeatherData(WeatherRequest.Builder()
                        .forceLoadSavedData()
                        .loadAlerts()
                        .setErrorListener(this@WeatherNowFragment)
                        .build())
                        .addOnSuccessListener { weather ->
                            weatherLiveData.value = weather
                        }
            } else {
                binding.swipeRefreshLayout.isRefreshing = false

                Toast.makeText(fragmentActivity, R.string.error_syncing, Toast.LENGTH_LONG).show()
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
                cancelDataSync()
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