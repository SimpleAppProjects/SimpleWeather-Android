package com.thewizrd.simpleweather.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.util.ObjectsCompat
import androidx.core.view.MenuItemCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigator
import androidx.recyclerview.widget.*
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.google.android.gms.location.*
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.transition.MaterialFadeThrough
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.controls.LocationQueryViewModel
import com.thewizrd.shared_resources.helpers.*
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.tzdb.TZDBCache
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.utils.WeatherUtils.ErrorStatus
import com.thewizrd.shared_resources.wearable.WearableHelper
import com.thewizrd.shared_resources.weatherdata.*
import com.thewizrd.shared_resources.weatherdata.WeatherRequest.WeatherErrorListener
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
import kotlinx.coroutines.tasks.await
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
    private var actionMode: ActionMode? = null

    // GPS Location
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private var mLocCallback: LocationCallback? = null
    private var mLocListnr: LocationListener? = null

    /**
     * Tracks the status of the location updates request.
     */
    private var mRequestingLocationUpdates = false
    private val mMainHandler = Handler(Looper.getMainLooper())

    // OptionsMenu
    private var optionsMenu: Menu? = null

    private var onBackPressedCallback: OnBackPressedCallback? = null

    private val wm = WeatherManager.getInstance()

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
                    if (!mErrorCounter[wEx.errorStatus.ordinal] && WeatherAPI.NWS == getSettingsManager().getAPI()) {
                        showSnackbar(Snackbar.make(R.string.error_message_weather_us_only, Snackbar.Duration.LONG), null)
                        mErrorCounter[wEx.errorStatus.ordinal] = true
                        return@runWithView
                    }

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
    private val onRecyclerClickListener = RecyclerOnClickListenerInterface { view, position ->
        AnalyticsLogger.logEvent("LocationsFragment: recycler click")

        if (view?.isEnabled == true && view.tag is LocationData) {
            val locData = view.tag as LocationData
            val vm = mAdapter.getPanelViewModel(position)

            val isHome = ObjectsCompat.equals(locData, getSettingsManager().getHomeData())

            val args = LocationsFragmentDirections.actionLocationsFragmentToWeatherNowFragment()
                    .setData(JSONParser.serializer(locData, LocationData::class.java))
                    .setBackground(vm?.imageData?.imageURI)
                    .setHome(isHome)

            try {
                Navigation.findNavController(binding.root).navigate(args)
            } catch (ex: IllegalArgumentException) {
                val props = Bundle()
                props.putString("method", "onRecyclerClickListener.onClick")
                props.putBoolean("isAlive", isAlive)
                props.putBoolean("isViewAlive", isViewAlive)
                props.putBoolean("isDetached", isDetached)
                props.putBoolean("isResumed", isResumed)
                props.putBoolean("isRemoving", isRemoving)
                AnalyticsLogger.logEvent("$TAG: navigation failed", props)

                Logger.writeLine(Log.ERROR, ex)
            }
        }
    }

    private val actionModeCallback: ActionMode.Callback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.title = if (!mAdapter.selectedItems.isEmpty()) mAdapter.selectedItems.size.toString() else ""

            val inflater = mode.menuInflater
            inflater.inflate(R.menu.locations_context, menu)

            val deleteBtnItem = menu.findItem(R.id.action_delete)
            deleteBtnItem?.isVisible = mAdapter.selectedItems.isNotEmpty()

            if (!mEditMode) toggleEditMode()
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            for (i in 0 until menu.size()) {
                MenuItemCompat.setIconTintList(menu.getItem(i), ColorStateList.valueOf(ContextCompat.getColor(appCompatActivity!!, R.color.invButtonColorText)))
            }
            return false
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.action_delete -> {
                    mAdapter.removeSelectedItems()
                    true
                }
                R.id.action_done -> {
                    mode.finish()
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            actionMode = null
            if (mEditMode) toggleEditMode()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exitTransition = MaterialFadeThrough()
        enterTransition = MaterialFadeThrough()

        // Create your fragment here
        AnalyticsLogger.logEvent("LocationsFragment: onCreate")

        mErrorCounter = BooleanArray(ErrorStatus.values().size)

        if (WearableHelper.isGooglePlayServicesInstalled()) {
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(appCompatActivity!!)
            mLocCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    stopLocationUpdates()
                    mMainHandler.removeCallbacks(cancelLocRequestRunner)

                    runWithView {
                        if (locationResult?.lastLocation != null) {
                            addGPSPanel()
                        } else {
                            showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT), null)
                        }
                    }
                }

                override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                    if (!locationAvailability.isLocationAvailable) {
                        stopLocationUpdates()
                        mMainHandler.removeCallbacks(cancelLocRequestRunner)

                        runWithView {
                            showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT), null)
                        }
                    }
                }
            }
        } else {
            mLocListnr = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    mMainHandler.removeCallbacks(cancelLocRequestRunner)
                    stopLocationUpdates()
                    addGPSPanel()
                }

                override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
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

        if (ContextUtils.isLargeTablet(appCompatActivity!!)) {
            // use a linear layout manager
            val gridLayoutManager: GridLayoutManager = object : GridLayoutManager(appCompatActivity, 2, VERTICAL, false) {
                override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
                    return RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT)
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
        if (!ContextUtils.isLargeTablet(appCompatActivity!!)) {
            val swipeDecor = SwipeToDeleteOffSetItemDecoration(binding.recyclerView.context, 2f,
                    OffsetMargin.TOP or OffsetMargin.BOTTOM)
            mITHCallback.addItemTouchHelperCallbackListener(swipeDecor)
            binding.recyclerView.addItemDecoration(swipeDecor)
        } else {
            binding.recyclerView.addItemDecoration(LocationPanelOffsetDecoration(binding.recyclerView.context, 2f))
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
        if (ContextUtils.isLargeTablet(appCompatActivity!!)) {
            binding.recyclerView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    binding.recyclerView.viewTreeObserver.removeOnPreDrawListener(this)

                    runWithView(Dispatchers.Main.immediate) {
                        val isLandscape = ContextUtils.getOrientation(appCompatActivity!!) == Configuration.ORIENTATION_LANDSCAPE
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
        optionsMenu = menu
        menu.clear()
        toolbar.inflateMenu(R.menu.locations)

        val onlyHomeIsLeft = mAdapter.getFavoritesCount() == 1
        val editMenuBtn = optionsMenu!!.findItem(R.id.action_editmode)
        if (editMenuBtn != null) {
            editMenuBtn.isVisible = !onlyHomeIsLeft
            MenuItemCompat.setIconTintList(editMenuBtn, ColorStateList.valueOf(ContextCompat.getColor(appCompatActivity!!, R.color.invButtonColorText)))
        }
    }

    private val menuItemClickListener = Toolbar.OnMenuItemClickListener { item -> // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent AppCompatActivity in AndroidManifest.xml.
        val id = item.itemId

        if (id == R.id.action_editmode) {
            actionMode = appCompatActivity!!.startSupportActionMode(actionModeCallback)
            return@OnMenuItemClickListener true
        }

        false
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

        // End actionmode
        actionMode?.finish()

        // Remove location updates to save battery.
        stopLocationUpdates()

        // Reset error counter
        Arrays.fill(mErrorCounter, 0, mErrorCounter.size, false)
        super.onPause()
    }

    private fun loadLocations() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            // Load up saved locations
            val locations = ArrayList(getSettingsManager().getFavorites()
                    ?: Collections.emptyList())
            launch(Dispatchers.Main) {
                mAdapter.removeAll()
            }

            if (!isActive) return@launch

            // Setup saved favorite locations
            var gpsData: LocationData? = null
            if (getSettingsManager().useFollowGPS()) {
                gpsData = getGPSPanel()

                if (gpsData != null) {
                    val gpsPanelViewModel = LocationPanelViewModel()
                    gpsPanelViewModel.locationData = gpsData

                    launch(Dispatchers.Main) {
                        mAdapter.add(0, gpsPanelViewModel)
                    }
                }
            }

            for (location in locations) {
                val panel = LocationPanelViewModel()
                panel.locationData = location
                launch(Dispatchers.Main) {
                    mAdapter.add(panel)
                }

                WeatherDataLoader(location)
                        .loadWeatherData(WeatherRequest.Builder()
                                .forceRefresh(false)
                                .setErrorListener(this@LocationsFragment)
                                .build())
                        .addOnSuccessListener { weather ->
                            runWithView(Dispatchers.Default) {
                                onWeatherLoaded(location, weather)
                            }
                        }
            }

            if (!isActive) return@launch

            if (gpsData != null) {
                locations.add(0, gpsData)

                WeatherDataLoader(gpsData)
                        .loadWeatherData(WeatherRequest.Builder()
                                .forceRefresh(false)
                                .setErrorListener(this@LocationsFragment)
                                .build())
                        .addOnSuccessListener { weather ->
                            runWithView(Dispatchers.Default) {
                                onWeatherLoaded(gpsData, weather)
                            }
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
                LocalBroadcastManager.getInstance(appCompatActivity!!)
                        .sendBroadcast(Intent(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE))
            }

            if (locData?.query != null) {
                return@withContext locData
            }
        }
        null
    }

    private fun refreshLocations() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
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

            if (!isActive) return@launch

            if (reload) {
                launch(Dispatchers.Main) {
                    mAdapter.removeAll()
                    loadLocations()
                }
            } else {
                val dataset = mAdapter.getDataset()

                for (view in dataset) {
                    WeatherDataLoader(view.locationData!!)
                            .loadWeatherData(WeatherRequest.Builder()
                                    .forceRefresh(false)
                                    .setErrorListener(this@LocationsFragment)
                                    .build())
                            .addOnSuccessListener { weather ->
                                runWithView(Dispatchers.Default) {
                                    onWeatherLoaded(view.locationData!!, weather)
                                }
                            }
                }
            }
        }
    }

    private fun addGPSPanel() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
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

            if (!isActive) return@launch

            if (gpsData != null) {
                WeatherDataLoader(gpsData)
                        .loadWeatherData(WeatherRequest.Builder()
                                .forceRefresh(false)
                                .setErrorListener(this@LocationsFragment)
                                .build())
                        .addOnSuccessListener { weather ->
                            runWithView(Dispatchers.Default) {
                                onWeatherLoaded(gpsData, weather)
                            }
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
            if (appCompatActivity != null && ContextCompat.checkSelfPermission(appCompatActivity!!, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(appCompatActivity!!, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                        PERMISSION_LOCATION_REQUEST_CODE)
                return null
            }

            var location: Location? = null

            val locMan = appCompatActivity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager?

            if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
                return null
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

                if (!coroutineContext.isActive) return null

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

                if (!coroutineContext.isActive) return null

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
                        showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT), null)
                        removeGPSPanel()
                    }
                }
            }

            if (location != null && !mRequestingLocationUpdates) {
                var view = try {
                    withContext(Dispatchers.IO) {
                        wm.getLocation(location)
                    }
                } catch (e: WeatherException) {
                    Logger.writeLine(Log.ERROR, e)
                    // Stop since there is no valid query
                    withContext(Dispatchers.Main) { removeGPSPanel() }
                    null
                }

                if (view?.locationQuery.isNullOrBlank()) {
                    view = LocationQueryViewModel()
                } else if (view?.locationTZLong?.isBlank() == true && view.locationLat != 0.0 && view.locationLong != 0.0) {
                    val tzId = TZDBCache.getTimeZone(view.locationLat, view.locationLong)
                    if ("unknown" != tzId)
                        view.locationTZLong = tzId
                }

                if (view?.locationQuery.isNullOrBlank()) {
                    // Stop since there is no valid query
                    withContext(Dispatchers.Main) { removeGPSPanel() }
                    return null
                }

                // Save location as last known
                locationData = LocationData(view!!, location)
            }
        }

        return locationData
    }

    private val cancelLocRequestRunner = Runnable {
        stopLocationUpdates()
        showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT), null)
        removeGPSPanel()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_LOCATION_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
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
                val dataMoved = e.action == ListChangedAction.REMOVE || e.action == ListChangedAction.MOVE
                val onlyHomeIsLeft = mAdapter.getFavoritesCount() == 1

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
                val editMenuBtn = optionsMenu?.findItem(R.id.action_editmode)
                editMenuBtn?.isVisible = !onlyHomeIsLeft
            }
        }
    }
    private val onSelectionChangedListener = object : OnListChangedListener<LocationPanelViewModel>() {
        override fun onChanged(sender: ArrayList<LocationPanelViewModel>, args: ListChangedArgs<LocationPanelViewModel>) {
            runWithView(Dispatchers.Main) {
                if (actionMode != null) {
                    actionMode!!.title = if (sender.isNotEmpty()) sender.size.toString() else ""

                    val deleteBtnItem = actionMode!!.menu.findItem(R.id.action_delete)
                    deleteBtnItem?.isVisible = sender.isNotEmpty()
                }
            }
        }
    }
    private val onRecyclerLongClickListener = RecyclerOnClickListenerInterface { view, position ->
        if (mAdapter.getItemViewType(position) == LocationPanelAdapter.ItemType.SEARCH_PANEL) {
            if (!mEditMode && mAdapter.getFavoritesCount() > 1) {
                actionMode = appCompatActivity!!.startSupportActionMode(actionModeCallback)

                val model = mAdapter.getPanelViewModel(position)
                if (model != null) {
                    model.isChecked = true
                    mAdapter.notifyItemChanged(position)
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
            actionMode?.finish()
        }

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
            Log.d("LocationsFragment", "Home changed; sending update")
            LocalBroadcastManager.getInstance(instance.appContext)
                    .sendBroadcast(Intent(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE)
                            .putExtra(CommonActions.EXTRA_FORCEUPDATE, false))
            LocalBroadcastManager.getInstance(instance.appContext)
                    .sendBroadcast(Intent(CommonActions.ACTION_WEATHER_SENDWEATHERUPDATE))
        }

        mDataChanged = false
        mHomeChanged = false
    }

    private fun updateFavoritesPosition(view: LocationPanelViewModel) {
        val query = view.locationData!!.query
        var dataPosition = mAdapter.getDataset().indexOf(view)
        val pos = if (mAdapter.hasGPSHeader()) --dataPosition else dataPosition
        GlobalScope.launch(Dispatchers.Default) {
            getSettingsManager().moveLocation(query, pos)
        }
    }
}