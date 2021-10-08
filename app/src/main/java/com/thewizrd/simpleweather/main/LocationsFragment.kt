package com.thewizrd.simpleweather.main

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.annotation.ColorInt
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.appcompat.widget.Toolbar
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.util.ObjectsCompat
import androidx.core.view.*
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigator
import androidx.recyclerview.widget.*
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.transition.TransitionManager
import com.google.android.material.animation.ArgbEvaluatorCompat
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.transition.MaterialFade
import com.google.android.material.transition.MaterialFadeThrough
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.helpers.*
import com.thewizrd.shared_resources.location.LocationProvider
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.tzdb.TZDBCache
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.utils.ActivityUtils.setLightStatusBar
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrColor
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrResourceId
import com.thewizrd.shared_resources.utils.ContextUtils.getOrientation
import com.thewizrd.shared_resources.utils.ContextUtils.isLargeTablet
import com.thewizrd.shared_resources.weatherdata.*
import com.thewizrd.shared_resources.weatherdata.WeatherRequest.WeatherErrorListener
import com.thewizrd.shared_resources.weatherdata.model.LocationType
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.App
import com.thewizrd.simpleweather.App.Companion.instance
import com.thewizrd.simpleweather.BuildConfig
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.adapters.LocationPanelAdapter
import com.thewizrd.simpleweather.adapters.LocationPanelAdapter.ViewHolderLongClickListener
import com.thewizrd.simpleweather.controls.LocationPanelViewModel
import com.thewizrd.simpleweather.databinding.FragmentLocationsBinding
import com.thewizrd.simpleweather.fragments.ToolbarFragment
import com.thewizrd.simpleweather.helpers.*
import com.thewizrd.simpleweather.snackbar.Snackbar
import com.thewizrd.simpleweather.snackbar.SnackbarManager
import kotlinx.coroutines.*
import timber.log.Timber
import java.lang.Runnable
import java.util.*
import kotlin.coroutines.coroutineContext

class LocationsFragment : ToolbarFragment(), WeatherErrorListener {
    companion object {
        private const val TAG = "LocationsFragment"
        private const val PERMISSION_LOCATION_REQUEST_CODE = 0
    }

    private var mEditMode = false
    private var mDataChanged = false
    private var mHomeChanged = false
    private lateinit var mErrorCounter: BooleanArray

    // Views
    private lateinit var binding: FragmentLocationsBinding
    private lateinit var mAdapter: LocationPanelAdapter
    private lateinit var mLayoutManager: RecyclerView.LayoutManager
    private lateinit var mItemTouchHelper: ItemTouchHelper
    private lateinit var mITHCallback: ItemTouchHelperCallback

    // GPS Location
    private lateinit var locationProvider: LocationProvider
    private lateinit var locationCallback: LocationProvider.Callback

    /**
     * Tracks the status of the location updates request.
     */
    private var mRequestingLocationUpdates = false
    private val mMainHandler = Handler(Looper.getMainLooper())

    private var onBackPressedCallback: OnBackPressedCallback? = null

    private val wm = WeatherManager.instance

    override fun getTitle(): Int {
        return R.string.label_nav_locations
    }

    @WorkerThread
    private suspend fun onWeatherLoaded(location: LocationData, weather: Weather?) {
        withContext(Dispatchers.Default) {
            Timber.tag(TAG).d("onWeatherLoaded: $location")
            val dataSet = mAdapter.getDataset()

            if (weather?.isValid == true) {
                // Update panel weather
                var panel: LocationPanelViewModel?

                panel = if (location.locationType == LocationType.GPS) {
                    dataSet.find { input -> input.locationData?.locationType == LocationType.GPS }
                } else {
                    dataSet.find { input -> input.locationData?.locationType != LocationType.GPS && input.locationData?.query == location.query }
                }

                // Just in case
                if (panel == null) {
                    AnalyticsLogger.logEvent("LocationsFragment: panel == null")
                    panel = dataSet.find { input ->
                        input.locationData?.name == location.name && input.locationData?.latitude == location.latitude && input.locationData?.longitude == location.longitude && input.locationData?.tzLong == location.tzLong
                    }
                }

                if (panel != null) {
                    panel.setWeather(weather)
                    withContext(Dispatchers.Main) {
                        mAdapter.notifyItemChanged(mAdapter.getViewPosition(panel))
                    }

                    launch(Dispatchers.IO) {
                        panel.updateBackground()

                        withContext(Dispatchers.Main) {
                            mAdapter.notifyItemChanged(mAdapter.getViewPosition(panel),
                                    LocationPanelAdapter.Payload.IMAGE_UPDATE)
                        }
                    }
                } else if (BuildConfig.DEBUG) {
                    Logger.writeLine(Log.WARN, "LocationsFragment: Location panel not found")
                    Logger.writeLine(Log.WARN, "LocationsFragment: LocationData: %s", location.toString())
                    Logger.writeLine(Log.WARN, "LocationsFragment: Dumping adapter data...")

                    for (i in dataSet.indices) {
                        val vm = dataSet[i]
                        Logger.writeLine(Log.WARN, "LocationsFragment: Panel: %d; data: %s", i, vm.locationData.toString())
                    }
                }
            }
        }
    }

    override fun onWeatherError(wEx: WeatherException) {
        runWithView(Dispatchers.Main) {
            when (wEx.errorStatus) {
                ErrorStatus.NETWORKERROR, ErrorStatus.NOWEATHER ->
                    // Show error message and prompt to refresh
                    // Only warn once
                    if (!mErrorCounter[wEx.errorStatus.ordinal]) {
                        val snackbar = Snackbar.make(wEx.message, Snackbar.Duration.LONG)
                        snackbar.setAction(R.string.action_retry) { // Reset counter to allow retry
                            mErrorCounter[wEx.errorStatus.ordinal] = false
                            refreshLocations()
                        }
                        showSnackbar(snackbar, null)
                        mErrorCounter[wEx.errorStatus.ordinal] = true
                    }
                ErrorStatus.QUERYNOTFOUND -> {
                    if (!mErrorCounter[wEx.errorStatus.ordinal]) {
                        showSnackbar(Snackbar.make(wEx.message, Snackbar.Duration.LONG), null)
                        mErrorCounter[wEx.errorStatus.ordinal] = true
                    }
                }
                else -> {
                    // Show error message
                    // Only warn once
                    if (!mErrorCounter[wEx.errorStatus.ordinal]) {
                        showSnackbar(Snackbar.make(wEx.message, Snackbar.Duration.LONG), null)
                        mErrorCounter[wEx.errorStatus.ordinal] = true
                    }
                }
            }
        }
    }

    override fun createSnackManager(): SnackbarManager {
        val mSnackMgr = SnackbarManager(rootView)
        mSnackMgr.setSwipeDismissEnabled(true)
        mSnackMgr.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE)
        return mSnackMgr
    }

    // For LocationPanels
    private val onRecyclerClickListener =
        object : ListAdapterOnClickInterface<LocationPanelViewModel> {
            override fun onClick(view: View, item: LocationPanelViewModel) {
                AnalyticsLogger.logEvent("LocationsFragment: recycler click")

                if (view.isEnabled && view.tag is LocationData) {
                    runWithView {
                        val locData = view.tag as LocationData

                        val isHome =
                            ObjectsCompat.equals(locData, getSettingsManager().getHomeData())

                        val args =
                            LocationsFragmentDirections.actionLocationsFragmentToWeatherNowFragment()
                                .setData(withContext(Dispatchers.Default) {
                                    JSONParser.serializer(locData, LocationData::class.java)
                                })
                                .setBackground(item.imageData?.imageURI)
                                .setHome(isHome)

                        try {
                            binding.root.findNavController().navigate(args)
                        } catch (ex: Exception) {
                            if (ex is IllegalArgumentException || ex is IllegalStateException) {
                                val props = Bundle().apply {
                                    putString("method", "onRecyclerClickListener.onClick")
                                    putBoolean("isAlive", isAlive)
                                    putBoolean("isViewAlive", isViewAlive)
                                    putBoolean("isDetached", isDetached)
                                    putBoolean("isResumed", isResumed)
                                    putBoolean("isRemoving", isRemoving)
                                }
                                AnalyticsLogger.logEvent("$TAG: navigation failed", props)

                                Logger.writeLine(Log.ERROR, ex)
                            } else {
                                throw ex
                            }
                        }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exitTransition = MaterialFadeThrough()
        enterTransition = MaterialFadeThrough()

        // Create your fragment here
        AnalyticsLogger.logEvent("LocationsFragment: onCreate")

        mErrorCounter = BooleanArray(ErrorStatus.values().size)

        locationProvider = LocationProvider(appCompatActivity!!)
        locationCallback = object : LocationProvider.Callback {
            override fun onLocationChanged(location: Location?) {
                stopLocationUpdates()

                runWithView {
                    if (location != null) {
                        addGPSPanel()
                    } else {
                        showSnackbar(
                            Snackbar.make(
                                R.string.error_retrieve_location,
                                Snackbar.Duration.SHORT
                            ), null
                        )
                    }
                }
            }

            override fun onRequestTimedOut() {
                stopLocationUpdates()
                showSnackbar(
                    Snackbar.make(
                        R.string.error_retrieve_location,
                        Snackbar.Duration.SHORT
                    ), null
                )
                removeGPSPanel()
            }
        }
        mRequestingLocationUpdates = false

        onBackPressedCallback = object : OnBackPressedCallback(mEditMode) {
            override fun handleOnBackPressed() {
                if (mEditMode) {
                    toggleEditMode()
                }
            }
        }
        appCompatActivity!!.onBackPressedDispatcher.addCallback(this, onBackPressedCallback!!)
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    @SuppressLint("MissingPermission")
    private fun stopLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            Logger.writeLine(Log.DEBUG, "LocationsFragment: stopLocationUpdates: updates never requested, no-op.")
            return
        }

        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        locationProvider.stopLocationUpdates()
        mRequestingLocationUpdates = false
    }

    override fun getScrollTargetViewId(): Int {
        return binding.recyclerView.id
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = super.onCreateView(inflater, container, savedInstanceState) as ViewGroup
        // Inflate the layout for this fragment
        binding = FragmentLocationsBinding.inflate(inflater, root, true)
        binding.lifecycleOwner = viewLifecycleOwner
        // Request focus away from RecyclerView
        root.isFocusableInTouchMode = true
        root.requestFocus()

        /*
           Capture touch events on RecyclerView
           Expand or collapse FAB (MaterialButton) based on scroll direction
           Collapse FAB if we're scrolling to the bottom (so the bottom items behind the keyboard are visible)
           Expand FAB if we're scrolling to the top (items at the top are already visible)
        */
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            private var scrollState = RecyclerView.SCROLL_STATE_IDLE

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                scrollState = newState
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (scrollState != RecyclerView.SCROLL_STATE_IDLE) {
                    if (dy < 0) {
                        binding.fab.extend()
                    } else {
                        binding.fab.shrink()
                    }
                }
            }
        })

        toolbar.setOnMenuItemClickListener(menuItemClickListener)

        // FAB
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            insets.replaceSystemWindowInsets(
                insets.systemWindowInsetLeft,
                insets.systemWindowInsetTop,
                insets.systemWindowInsetRight,
                0
            )
        }
        binding.fab.setOnClickListener {
            binding.root.findNavController()
                .navigate(
                    LocationsFragmentDirections.actionLocationsFragmentToLocationSearchFragment(),
                    FragmentNavigator.Extras.Builder()
                        .addSharedElement(binding.fab, Constants.SHARED_ELEMENT)
                        .build()
                )
        }
        ViewCompat.setTransitionName(binding.fab, Constants.SHARED_ELEMENT)

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        binding.recyclerView.setHasFixedSize(true)

        if (appCompatActivity!!.isLargeTablet()) {
            // use a linear layout manager
            val gridLayoutManager: GridLayoutManager =
                object : GridLayoutManager(appCompatActivity, 2, VERTICAL, false) {
                    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
                        return RecyclerView.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }
                }
            gridLayoutManager.spanSizeLookup = object : SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when (mAdapter.getItemViewType(position)) {
                        LocationPanelAdapter.ItemType.HEADER_FAV, LocationPanelAdapter.ItemType.HEADER_GPS -> gridLayoutManager.spanCount
                        else -> 1
                    }
                }
            }
            mLayoutManager = gridLayoutManager
            binding.recyclerView.addItemDecoration(object : ItemDecoration() {})
        } else {
            // use a linear layout manager
            mLayoutManager = object : LinearLayoutManager(appCompatActivity) {
                override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
                    return RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT)
                }
            }
        }
        binding.recyclerView.layoutManager = mLayoutManager

        // Setup RecyclerView
        LocationPanelAdapter(object : ViewHolderLongClickListener {
            override fun onLongClick(holder: RecyclerView.ViewHolder) {
                mItemTouchHelper.startDrag(holder)
            }
        }).apply {
            setOnClickListener(onRecyclerClickListener)
            setOnLongClickListener(onRecyclerLongClickListener)
            setOnListChangedCallback(onListChangedListener)
            setOnSelectionChangedCallback(onSelectionChangedListener)

            binding.recyclerView.adapter = this
            mAdapter = this
        }
        mITHCallback = ItemTouchHelperCallback(mAdapter)
        mItemTouchHelper = ItemTouchHelper(mITHCallback)
        mItemTouchHelper.attachToRecyclerView(binding.recyclerView)
        mITHCallback.addItemTouchHelperCallbackListener(object : ItemTouchCallbackListener {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) {
                mDataChanged = true
                if (mEditMode) {
                    toggleEditMode()
                } else {
                    val dataSet = mAdapter.getDataset()
                    for (view in dataSet) {
                        if (view.locationType != LocationType.GPS.value) {
                            updateFavoritesPosition(view)
                        }
                    }

                    if (!mAdapter.hasGPSHeader() && mAdapter.hasSearchHeader()) {
                        val firstFavPosition = mAdapter.getViewPosition(mAdapter.getFirstFavPanel())

                        if (viewHolder.adapterPosition == firstFavPosition || target.adapterPosition == firstFavPosition) {
                            mMainHandler.removeCallbacks(sendUpdateRunner)
                            mMainHandler.postDelayed(sendUpdateRunner, 2500)
                        }
                    }
                }
            }

            override fun onClearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {}

            private val sendUpdateRunner = Runnable {
                // Home has changed send notice
                Timber.tag("LocationsFragment").d("Home changed; sending update")
                LocalBroadcastManager.getInstance(instance.appContext)
                        .sendBroadcast(Intent(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE)
                                .putExtra(CommonActions.EXTRA_FORCEUPDATE, false))
                LocalBroadcastManager.getInstance(instance.appContext)
                        .sendBroadcast(Intent(CommonActions.ACTION_WEATHER_SENDWEATHERUPDATE))
            }
        })
        if (!appCompatActivity!!.isLargeTablet()) {
            val swipeDecor = SwipeToDeleteOffSetItemDecoration(
                binding.recyclerView.context, 2f,
                OffsetMargin.TOP or OffsetMargin.BOTTOM
            )
            mITHCallback.addItemTouchHelperCallbackListener(swipeDecor)
            binding.recyclerView.addItemDecoration(swipeDecor)
        } else {
            binding.recyclerView.addItemDecoration(
                LocationPanelOffsetDecoration(
                    binding.recyclerView.context,
                    2f
                )
            )
        }
        binding.recyclerView.itemAnimator = DefaultItemAnimator().apply {
            supportsChangeAnimations = false
        }

        // Enable touch actions
        mITHCallback.isItemViewSwipeEnabled = false

        // Create options menu
        createOptionsMenu()

        // Add Adapter as Lifecycle observer
        this.lifecycle.addObserver(mAdapter)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adjustPanelContainer()
    }

    private fun adjustPanelContainer() {
        if (appCompatActivity!!.isLargeTablet()) {
            binding.recyclerView.viewTreeObserver.addOnPreDrawListener(object :
                ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    binding.recyclerView.viewTreeObserver.removeOnPreDrawListener(this)

                    runWithView(Dispatchers.Main.immediate) {
                        val isLandscape =
                            appCompatActivity!!.getOrientation() == Configuration.ORIENTATION_LANDSCAPE
                        val viewWidth = binding.recyclerView.measuredWidth
                        val minColumns = if (isLandscape) 2 else 1

                        // Minimum width for ea. card
                        val minWidth = appCompatActivity!!.resources.getDimensionPixelSize(R.dimen.location_panel_minwidth)
                        // Available columns based on min card width
                        val availColumns = if ((viewWidth / minWidth) <= 1) minColumns else (viewWidth / minWidth)

                        if (binding.recyclerView.layoutManager is GridLayoutManager) {
                            (binding.recyclerView.layoutManager as GridLayoutManager).spanCount = availColumns
                        }
                    }

                    return true
                }
            })
        }
    }

    override fun onDestroyView() {
        this.lifecycle.removeObserver(mAdapter)
        super.onDestroyView()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        adjustPanelContainer()
        mAdapter.notifyItemRangeChanged(0, mAdapter.itemCount, LocationPanelAdapter.Payload.IMAGE_UPDATE)
    }

    private fun createOptionsMenu() {
        // Inflate the menu; this adds items to the action bar if it is present.
        val menu = toolbar.menu
        menu.clear()
        toolbar.inflateMenu(R.menu.locations)

        val editMenuBtn = menu?.findItem(R.id.action_editmode)
        editMenuBtn?.isVisible = !mEditMode && mAdapter.getFavoritesCount() > 1
    }

    private val menuItemClickListener = Toolbar.OnMenuItemClickListener { item ->
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent AppCompatActivity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_editmode -> {
                toggleEditMode()
                true
            }
            R.id.action_delete -> {
                mAdapter.removeSelectedItems()
                true
            }
            R.id.action_done -> {
                toggleEditMode()
                true
            }
            else -> false
        }
    }

    private fun resume() {
        // Update view on resume
        // ex. If temperature unit changed
        if (mAdapter.getDataCount() == 0) {
            // New instance; Get locations and load up weather data
            loadLocations()
        } else {
            // Refresh view
            refreshLocations()
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsLogger.logEvent("LocationsFragment: onResume")

        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            resume()
        }
    }

    override fun onPause() {
        AnalyticsLogger.logEvent("LocationsFragment: onPause")

        // End edit mode
        if (mEditMode) {
            toggleEditMode()
        }

        // Remove location updates to save battery.
        stopLocationUpdates()

        // Reset error counter
        Arrays.fill(mErrorCounter, 0, mErrorCounter.size, false)
        super.onPause()
    }

    private fun loadLocations() {
        runWithView(Dispatchers.Default) {
            // Load up saved locations
            val locations = ArrayList(getSettingsManager().getFavorites()
                                      ?: Collections.emptyList())
            withContext(Dispatchers.Main) {
                mAdapter.removeAll()
            }

            if (!isActive) return@runWithView

            // Setup saved favorite locations
            var gpsData: LocationData? = null
            if (getSettingsManager().useFollowGPS()) {
                gpsData = getGPSPanel()

                if (gpsData != null) {
                    val gpsPanelViewModel = LocationPanelViewModel()
                    gpsPanelViewModel.locationData = gpsData

                    withContext(Dispatchers.Main) {
                        mAdapter.add(0, gpsPanelViewModel)
                    }
                }
            }

            if (gpsData != null) {
                locations.add(0, gpsData)
            }

            if (!isActive) return@runWithView

            if (locations.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    binding.noLocationsPrompt.visibility = View.GONE
                }

                for (location in locations) {
                    if (location != gpsData) {
                        val panel = LocationPanelViewModel()
                        panel.locationData = location
                        withContext(Dispatchers.Main) {
                            mAdapter.add(panel)
                        }
                    }

                    launch(Dispatchers.Default) {
                        supervisorScope {
                            val weather = WeatherDataLoader(location)
                                .loadWeatherData(
                                    WeatherRequest.Builder()
                                        .forceRefresh(false)
                                        .setErrorListener(this@LocationsFragment)
                                        .build()
                                )

                            onWeatherLoaded(location, weather)
                        }
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    binding.noLocationsPrompt.visibility = View.VISIBLE
                }
            }
        }
    }

    private suspend fun getGPSPanel(): LocationData? = withContext(Dispatchers.IO) {
        // Setup gps panel
        if (appCompatActivity != null && getSettingsManager().useFollowGPS()) {
            var locData = getSettingsManager().getLastGPSLocData()

            if (locData?.query == null) {
                locData = updateLocation()
                if (locData != null) {
                    getSettingsManager().saveLastGPSLocData(locData)
                    LocalBroadcastManager.getInstance(appCompatActivity!!)
                        .sendBroadcast(Intent(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE))
                }
            }

            if (locData?.query != null) {
                return@withContext locData
            }
        }
        null
    }

    private fun refreshLocations() {
        runWithView(Dispatchers.Default) {
            // Reload all panels if needed
            val locations = ArrayList(getSettingsManager().getLocationData()
                                      ?: Collections.emptyList())
            if (getSettingsManager().useFollowGPS()) {
                val homeData = getSettingsManager().getLastGPSLocData()
                locations.add(0, homeData)
            }
            val gpsPanelViewModel = mAdapter.getGPSPanel()

            var reload = locations.size != mAdapter.getDataCount() ||
                    getSettingsManager().useFollowGPS() && gpsPanelViewModel == null ||
                    !getSettingsManager().useFollowGPS() && gpsPanelViewModel != null

            // Reload if weather source differs
            if (getSettingsManager().getAPI() != gpsPanelViewModel?.weatherSource ||
                    mAdapter.getFavoritesCount() > 0 && getSettingsManager().getAPI() != mAdapter.getFirstFavPanel()?.weatherSource) {
                reload = true
            }

            if (getSettingsManager().useFollowGPS()) {
                if (!reload && !ObjectsCompat.equals(locations[0]?.query, gpsPanelViewModel?.locationData?.query)) {
                    reload = true
                }
            }

            if (!isActive) return@runWithView

            if (reload) {
                launch(Dispatchers.Main) {
                    mAdapter.removeAll()
                    loadLocations()
                }
            } else {
                val dataset = mAdapter.getDataset()

                if (dataset.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        binding.noLocationsPrompt.visibility = View.GONE
                    }

                    for (view in dataset) {
                        launch(Dispatchers.Default) {
                            val weather = WeatherDataLoader(view.locationData!!)
                                .loadWeatherData(
                                    WeatherRequest.Builder()
                                        .forceRefresh(false)
                                        .setErrorListener(this@LocationsFragment)
                                        .build()
                                )

                            onWeatherLoaded(view.locationData!!, weather)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        binding.noLocationsPrompt.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun addGPSPanel() {
        runWithView(Dispatchers.Default) {
            // Setup saved favorite locations
            val gpsData: LocationData?
            if (getSettingsManager().useFollowGPS()) {
                gpsData = getGPSPanel()

                if (gpsData != null) {
                    val gpsPanelViewModel = LocationPanelViewModel().apply {
                        locationData = gpsData
                    }
                    launch(Dispatchers.Main) {
                        mAdapter.add(0, gpsPanelViewModel)
                    }
                }
            } else {
                gpsData = null
            }

            if (!isActive) return@runWithView

            if (gpsData != null) {
                launch(Dispatchers.Default) {
                    val weather = WeatherDataLoader(gpsData)
                            .loadWeatherData(WeatherRequest.Builder()
                                    .forceRefresh(false)
                                    .setErrorListener(this@LocationsFragment)
                                    .build())

                    onWeatherLoaded(gpsData, weather)
                }
            }
        }
    }

    @MainThread
    private fun removeGPSPanel() {
        if (mAdapter.hasGPSHeader()) {
            mAdapter.removeGPSPanel()
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun updateLocation(): LocationData? {
        var locationData: LocationData? = null

        if (getSettingsManager().useFollowGPS()) {
            if (appCompatActivity != null && !appCompatActivity!!.locationPermissionEnabled()) {
                this.requestLocationPermission(PERMISSION_LOCATION_REQUEST_CODE)
                return null
            }

            val locMan =
                appCompatActivity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager?

            if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
                return null
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

            if (!coroutineContext.isActive) return null

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
                val view = try {
                    withContext(Dispatchers.IO) {
                        wm.getLocation(location)
                    }
                } catch (e: WeatherException) {
                    Logger.writeLine(Log.ERROR, e)
                    null
                }

                if (view == null || view.locationQuery.isNullOrBlank()) {
                    // Stop since there is no valid query
                    withContext(Dispatchers.Main) { removeGPSPanel() }
                    return null
                }

                if (view.locationTZLong.isNullOrBlank() && view.locationLat != 0.0 && view.locationLong != 0.0) {
                    val tzId = TZDBCache.getTimeZone(view.locationLat, view.locationLong)
                    if ("unknown" != tzId)
                        view.locationTZLong = tzId
                }

                // Save location as last known
                locationData = LocationData(view, location)
            }
        }

        return locationData
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_LOCATION_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    runWithView(Dispatchers.Default) {
                        // permission was granted, yay!
                        // Do the task you need to do.
                        val locData = updateLocation()
                        if (locData != null) {
                            getSettingsManager().saveLastGPSLocData(locData)
                            refreshLocations()
                            Timber.tag("LocationsFragment").d("Location changed; sending update")
                            LocalBroadcastManager.getInstance(appCompatActivity!!)
                                    .sendBroadcast(Intent(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE))
                        } else {
                            launch(Dispatchers.Main) {
                                removeGPSPanel()
                            }
                        }
                    }
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    getSettingsManager().setFollowGPS(false)
                    removeGPSPanel()
                    showSnackbar(Snackbar.make(R.string.error_location_denied, Snackbar.Duration.SHORT), null)
                }
                return
            }
        }
    }

    private val onListChangedListener = object : OnListChangedListener<LocationPanelViewModel>() {
        override fun onChanged(sender: ArrayList<LocationPanelViewModel>, e: ListChangedArgs<LocationPanelViewModel>) {
            runWithView(Dispatchers.Main) {
                val dataMoved =
                    e.action == ListChangedAction.REMOVE || e.action == ListChangedAction.MOVE
                val onlyHomeIsLeft = mAdapter.getFavoritesCount() <= 1

                // Flag that data has changed
                if (mEditMode && dataMoved)
                    mDataChanged = true

                if (mEditMode && (e.newStartingIndex == App.HOMEIDX || e.oldStartingIndex == App.HOMEIDX))
                    mHomeChanged = true

                // Hide FAB; Don't allow adding more locations
                if (mAdapter.getDataCount() >= getSettingsManager().getMaxLocations()) {
                    binding.fab.hide()
                } else {
                    binding.fab.show()
                }

                // Cancel edit Mode
                if (mEditMode && onlyHomeIsLeft) toggleEditMode()

                // Disable EditMode if only single location
                val editMenuBtn = toolbar?.menu?.findItem(R.id.action_editmode)
                editMenuBtn?.isVisible = if (mEditMode) false else !onlyHomeIsLeft
            }
        }
    }
    private val onSelectionChangedListener = object : OnListChangedListener<LocationPanelViewModel>() {
        override fun onChanged(sender: ArrayList<LocationPanelViewModel>, args: ListChangedArgs<LocationPanelViewModel>) {
            runWithView(Dispatchers.Main) {
                if (mEditMode) {
                    toolbar!!.title = if (sender.isNotEmpty()) sender.size.toString() else ""

                    val deleteBtnItem = toolbar!!.menu.findItem(R.id.action_delete)
                    deleteBtnItem?.isVisible = sender.isNotEmpty()
                }
            }
        }
    }
    private val onRecyclerLongClickListener =
        object : ListAdapterOnClickInterface<LocationPanelViewModel> {
            override fun onClick(view: View, item: LocationPanelViewModel) {
                val position = mAdapter.getViewPosition(item)

                if (mAdapter.getItemViewType(position) == LocationPanelAdapter.ItemType.SEARCH_PANEL) {
                    if (!mEditMode && mAdapter.getFavoritesCount() > 1) {
                        toggleEditMode()

                        if (item != null) {
                            item.isChecked = true
                            mAdapter.notifyItemChanged(position)
                        }
                    }
                }
            }
        }

    private fun toggleEditMode() {
        // Toggle EditMode
        mEditMode = !mEditMode
        onBackPressedCallback!!.isEnabled = mEditMode
        mAdapter.setInEditMode(mEditMode)

        // Set Drag & Swipe ability
        mITHCallback.isItemViewSwipeEnabled = mEditMode

        if (mEditMode) {
            // Unregister events
            mAdapter.setOnClickListener(null)
            mAdapter.setOnLongClickListener(null)
        } else {
            // Register events
            mAdapter.setOnClickListener(onRecyclerClickListener)
            mAdapter.setOnLongClickListener(onRecyclerLongClickListener)
            mAdapter.clearSelection()
        }

        updateToolbarForEditMode(mEditMode)

        for (view in mAdapter.getDataset()) {
            view.isEditMode = mEditMode
            if (!mEditMode) view.isChecked = false
            binding.recyclerView.post {
                if (isViewAlive) {
                    mAdapter.notifyItemChanged(mAdapter.getViewPosition(view))
                }
            }
            if (view.locationType != LocationType.GPS.value && !mEditMode && (mDataChanged || mHomeChanged)) {
                updateFavoritesPosition(view)
            }
        }

        if (!mEditMode && mHomeChanged) {
            Timber.tag(TAG).d("Home changed; sending update")
            LocalBroadcastManager.getInstance(instance.appContext)
                .sendBroadcast(
                    Intent(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE)
                        .putExtra(CommonActions.EXTRA_FORCEUPDATE, false)
                )
            LocalBroadcastManager.getInstance(instance.appContext)
                .sendBroadcast(Intent(CommonActions.ACTION_WEATHER_SENDWEATHERUPDATE))
        }

        mDataChanged = false
        mHomeChanged = false
    }

    private fun updateToolbarForEditMode(inEditMode: Boolean) {
        TransitionManager.beginDelayedTransition(appBarLayout, MaterialFade().apply {
            duration = 175
            secondaryAnimatorProvider = null
        })

        if (inEditMode) {
            val navIcon =
                ContextCompat.getDrawable(toolbar.context, R.drawable.ic_close_white_24dp)!!
                    .mutate()
            DrawableCompat.setTint(
                navIcon,
                toolbar.context.getAttrColor(R.attr.colorOnPrimary)
            )
            toolbar.navigationIcon = navIcon
            toolbar.setNavigationOnClickListener {
                toggleEditMode()
            }
            toolbar.title = if (mAdapter.selectedItems.isNotEmpty()) {
                mAdapter.selectedItems.size.toString()
            } else {
                ""
            }
            toolbar.setTitleTextAppearance(
                toolbar.context,
                R.style.TextAppearance_OpenSans_ActionModeTitleOnPrimary
            )
        } else {
            toolbar.navigationIcon = null
            toolbar.setNavigationOnClickListener(null)
            toolbar.setTitle(title)
            toolbar.setTitleTextAppearance(
                toolbar.context,
                toolbar.context.getAttrResourceId(R.attr.textAppearanceHeadline6)
            )
            (appCompatActivity as? WindowColorManager)?.updateWindowColors()
        }

        toolbar.menu.forEach {
            when (it.itemId) {
                R.id.action_editmode -> {
                    it.isVisible = if (inEditMode) {
                        false
                    } else {
                        mAdapter.getFavoritesCount() > 1
                    }
                }
                R.id.action_delete -> {
                    it.isVisible = if (inEditMode) {
                        mAdapter.selectedItems.isNotEmpty()
                    } else {
                        false
                    }
                    MenuItemCompat.setIconTintList(
                        it,
                        ColorStateList.valueOf(toolbar.context.getAttrColor(R.attr.colorOnPrimary))
                    )
                }
                R.id.action_done -> {
                    it.isVisible = inEditMode
                    MenuItemCompat.setIconTintList(
                        it,
                        ColorStateList.valueOf(toolbar.context.getAttrColor(R.attr.colorOnPrimary))
                    )
                }
                else -> it.isVisible = !inEditMode
            }
        }

        runAppBarAnimation(
            if (inEditMode) {
                appBarLayout.context.getAttrColor(R.attr.colorPrimary)
            } else {
                appBarLayout.context.getAttrColor(R.attr.colorSurface)
            }
        )
    }

    private fun updateFavoritesPosition(view: LocationPanelViewModel) {
        val query = view.locationData!!.query
        var dataPosition = mAdapter.getDataset().indexOf(view)
        val pos = if (mAdapter.hasGPSHeader()) --dataPosition else dataPosition
        GlobalScope.launch(Dispatchers.Default) {
            getSettingsManager().moveLocation(query, pos)
        }
    }

    override fun updateWindowColors() {
        super.updateWindowColors()

        if (mEditMode) {
            val statusBarColor = appCompatActivity!!.getAttrColor(R.attr.colorPrimary)

            if (appBarLayout.background is MaterialShapeDrawable) {
                val materialShapeDrawable = appBarLayout.background as MaterialShapeDrawable
                materialShapeDrawable.fillColor = ColorStateList.valueOf(statusBarColor)
            } else {
                appBarLayout.setBackgroundColor(statusBarColor)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                appCompatActivity?.window?.setLightStatusBar(
                    ColorsUtils.isSuperLight(statusBarColor)
                )
            }
        }
    }

    private var mAppBarAnimator: ValueAnimator? = null
    private fun runAppBarAnimation(@ColorInt colorTo: Int) {
        val colorFrom = if (appBarLayout.background is MaterialShapeDrawable) {
            val materialShapeDrawable = appBarLayout.background as MaterialShapeDrawable
            materialShapeDrawable.fillColor?.defaultColor
        } else {
            (appBarLayout.background as? ColorDrawable)?.color
        } ?: appBarLayout.context.getAttrColor(R.attr.colorSurface)
        if (colorFrom != colorTo) {
            if (mAppBarAnimator?.isRunning == true) {
                mAppBarAnimator?.cancel()
            }
            mAppBarAnimator = ValueAnimator.ofObject(ArgbEvaluatorCompat(), colorFrom, colorTo)
            mAppBarAnimator!!.addUpdateListener {
                val statusBarColor = it.animatedValue as Int
                if (appBarLayout.background is MaterialShapeDrawable) {
                    val materialShapeDrawable = appBarLayout.background as MaterialShapeDrawable
                    materialShapeDrawable.fillColor = ColorStateList.valueOf(statusBarColor)
                } else {
                    appBarLayout.setBackgroundColor(statusBarColor)
                }
            }
            mAppBarAnimator!!.doOnEnd {
                updateWindowColors()
            }
            mAppBarAnimator!!.duration = 195
            mAppBarAnimator!!.startDelay = 0
            mAppBarAnimator!!.start()
        }
    }
}