package com.thewizrd.simpleweather.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.core.view.ViewCompat
import androidx.core.view.ViewGroupCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.transition.MaterialContainerTransform
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.adapters.LocationQueryAdapter
import com.thewizrd.shared_resources.controls.LocationQueryViewModel
import com.thewizrd.shared_resources.helpers.ContextUtils
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.tzdb.TZDBCache
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.utils.WeatherUtils.Coordinate
import com.thewizrd.shared_resources.weatherdata.*
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.databinding.FragmentLocationSearchBinding
import com.thewizrd.simpleweather.databinding.SearchActionBarBinding
import com.thewizrd.simpleweather.snackbar.Snackbar
import com.thewizrd.simpleweather.snackbar.SnackbarManager
import com.thewizrd.simpleweather.snackbar.SnackbarWindowAdjustCallback
import kotlinx.coroutines.*
import java.util.*

class LocationSearchFragment : WindowColorFragment() {
    companion object {
        private const val TAG = "LocSearchFragment"
        private const val KEY_SEARCHTEXT = "search_text"
    }

    private lateinit var binding: FragmentLocationSearchBinding
    private lateinit var searchBarBinding: SearchActionBarBinding
    private lateinit var mAdapter: LocationQueryAdapter
    private lateinit var mLayoutManager: RecyclerView.LayoutManager
    private val wm = WeatherManager.getInstance()

    private var job: Job? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        initSnackManager()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AnalyticsLogger.logEvent("$TAG: onCreate")
        sharedElementEnterTransition = MaterialContainerTransform().setDuration(Constants.ANIMATION_DURATION.toLong())
    }

    override fun onPause() {
        searchBarBinding.searchView.clearFocus()
        super.onPause()
    }

    override fun onDetach() {
        unloadSnackManager()
        super.onDetach()
    }

    override fun createSnackManager(): SnackbarManager {
        val mSnackMgr = SnackbarManager(appCompatActivity!!.findViewById(android.R.id.content))
        mSnackMgr.setSwipeDismissEnabled(true)
        mSnackMgr.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE)
        return mSnackMgr
    }

    private val recyclerClickListener = RecyclerOnClickListenerInterface { view, position ->
        val props = Bundle()
        props.putString("method", "recyclerClickListener.onClick")
        AnalyticsLogger.logEvent("$TAG: onClick", props)
        val navController = Navigation.findNavController(binding.root)
        val savedStateHandle = if (navController.previousBackStackEntry != null) {
            navController.previousBackStackEntry!!.savedStateHandle
        } else {
            // NOTE: This shouldn't happen btw
            val args = Bundle()
            args.putString("method", "recyclerClickListener.onClick")
            args.putBoolean("isAlive", isAlive)
            args.putBoolean("isViewAlive", isViewAlive)
            args.putBoolean("isDetached", isDetached)
            args.putBoolean("isResumed", isResumed)
            args.putBoolean("isRemoving", isRemoving)
            if (navController.currentBackStackEntry != null) {
                args.putString("currentBackStackEntry", navController.currentBackStackEntry!!.destination.toString())
            }
            if (navController.currentDestination != null) {
                args.putString("currentDestination", navController.currentDestination.toString())
            }
            AnalyticsLogger.logEvent("$TAG: prevBackStack null", args)
            Logger.writeLine(Log.ERROR, "$TAG: prevBackStack null")
            return@RecyclerOnClickListenerInterface
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main.immediate) {
            showLoading(true)
            enableRecyclerView(false)

            // Cancel other tasks
            job?.cancel()

            supervisorScope {
                val deferredJob = viewLifecycleOwner.lifecycleScope.async(Dispatchers.IO) {
                    var queryResult: LocationQueryViewModel? = LocationQueryViewModel()

                    if (!StringUtils.isNullOrEmpty(mAdapter.dataset[position].locationQuery))
                        queryResult = mAdapter.dataset[position]

                    if (StringUtils.isNullOrWhitespace(queryResult!!.locationQuery)) {
                        // Stop since there is no valid query
                        throw CustomException(R.string.error_retrieve_location)
                    }

                    if (Settings.usePersonalKey() && StringUtils.isNullOrWhitespace(Settings.getAPIKEY()) && wm.isKeyRequired) {
                        throw CustomException(R.string.werror_invalidkey)
                    }

                    ensureActive()

                    // Need to get FULL location data for HERE API
                    // Data provided is incomplete
                    if (queryResult.locationLat == -1.0 && queryResult.locationLong == -1.0 && queryResult.locationTZLong == null && wm.locationProvider.needsLocationFromID()) {
                        val loc = queryResult
                        queryResult = withContext(Dispatchers.IO) {
                            wm.locationProvider.getLocationFromID(loc)
                        }
                    } else if (wm.locationProvider.needsLocationFromName()) {
                        val loc = queryResult
                        queryResult = withContext(Dispatchers.IO) {
                            wm.locationProvider.getLocationFromName(loc)
                        }
                    } else if (wm.locationProvider.needsLocationFromGeocoder()) {
                        val loc = queryResult
                        queryResult = withContext(Dispatchers.IO) {
                            wm.locationProvider.getLocation(Coordinate(loc.locationLat, loc.locationLong), loc.weatherSource)
                        }
                    }

                    if (queryResult == null) {
                        throw InterruptedException()
                    } else if (StringUtils.isNullOrWhitespace(queryResult.locationTZLong) && queryResult.locationLat != 0.0 && queryResult.locationLong != 0.0) {
                        val tzId = TZDBCache.getTimeZone(queryResult.locationLat, queryResult.locationLong)
                        if ("unknown" != tzId)
                            queryResult.locationTZLong = tzId
                    }

                    val isUS = LocationUtils.isUS(queryResult.locationCountry)

                    if (!Settings.isWeatherLoaded()) {
                        // Default US provider to NWS
                        if (isUS) {
                            Settings.setAPI(WeatherAPI.NWS)
                            queryResult.updateWeatherSource(WeatherAPI.NWS)
                        } else {
                            Settings.setAPI(WeatherAPI.WEATHERUNLOCKED)
                            queryResult.updateWeatherSource(WeatherAPI.WEATHERUNLOCKED)
                        }
                        wm.updateAPI()
                    }

                    if (WeatherAPI.NWS == Settings.getAPI() && !isUS) {
                        throw CustomException(R.string.error_message_weather_us_only)
                    }

                    // Check if location already exists
                    val locData = Settings.getLocationData()
                    val finalQueryResult: LocationQueryViewModel = queryResult
                    val loc = locData?.find { input -> input != null && input.query == finalQueryResult.locationQuery }

                    if (loc != null) {
                        // Location exists; return
                        return@async null
                    }

                    ensureActive()

                    val location = LocationData(queryResult)
                    if (!location.isValid) {
                        throw CustomException(R.string.werror_noweather)
                    }
                    var weather = Settings.getWeatherData(location.query)
                    if (weather == null) {
                        weather = wm.getWeather(location)
                    }

                    if (weather == null) {
                        throw WeatherException(WeatherUtils.ErrorStatus.NOWEATHER)
                    } else if (wm.supportsAlerts() && wm.needsExternalAlertData()) {
                        weather.weatherAlerts = wm.getAlerts(location)
                    }

                    // Save data
                    Settings.addLocation(location)
                    if (wm.supportsAlerts() && weather.weatherAlerts != null)
                        Settings.saveWeatherAlerts(location, weather.weatherAlerts)
                    Settings.saveWeatherData(weather)
                    Settings.saveWeatherForecasts(Forecasts(weather.query, weather.forecast, weather.txtForecast))
                    Settings.saveWeatherForecasts(location.query, weather.hrForecast?.map { input -> HourlyForecasts(weather.query, input!!) })

                    Settings.setWeatherLoaded(true)

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
                        val result = deferredJob.getCompleted()
                        runWithView { // Go back to where we started
                            if (result != null) {
                                savedStateHandle.set(Constants.KEY_DATA, JSONParser.serializer(result, LocationData::class.java))
                            }
                            navController.navigateUp()
                        }
                    } else {
                        runWithView {
                            if (t is WeatherException || t is CustomException) {
                                showSnackbar(Snackbar.make(t.message, Snackbar.Duration.SHORT),
                                        SnackbarWindowAdjustCallback(appCompatActivity!!))
                            } else {
                                showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT),
                                        SnackbarWindowAdjustCallback(appCompatActivity!!))
                            }
                            showLoading(false)
                            enableRecyclerView(true)
                        }
                    }
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        searchBarBinding.searchProgressBar.visibility = if (show) View.VISIBLE else View.GONE

        if (show || StringUtils.isNullOrEmpty(searchBarBinding.searchView.text.toString()))
            searchBarBinding.searchCloseButton.visibility = View.GONE
        else
            searchBarBinding.searchCloseButton.visibility = View.VISIBLE
    }

    fun enableRecyclerView(enable: Boolean) {
        binding.recyclerView.isEnabled = enable
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        binding = FragmentLocationSearchBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        searchBarBinding = binding.searchBar
        searchBarBinding.lifecycleOwner = viewLifecycleOwner
        val view = binding.root

        ViewCompat.setTransitionName(view, Constants.SHARED_ELEMENT)
        ViewGroupCompat.setTransitionGroup((view as ViewGroup), true)

        // Initialize
        view.setOnClickListener { v -> v.findNavController().navigateUp() }
        searchBarBinding.searchBackButton.setOnClickListener { v -> v.findNavController().navigateUp() }

        // For landscape orientation
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val layoutParams = v.layoutParams as MarginLayoutParams
            layoutParams.setMargins(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
            insets
        }

        searchBarBinding.searchCloseButton.setOnClickListener { searchBarBinding.searchView.setText("") }
        searchBarBinding.searchCloseButton.visibility = View.GONE

        searchBarBinding.searchView.addTextChangedListener(object : TextWatcher {
            private var timer: Timer? = Timer()
            private val DELAY: Long = 1000 // milliseconds
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // nothing to do here
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // user is typing: reset already started timer (if existing)
                timer?.cancel()
            }

            override fun afterTextChanged(e: Editable) {
                // If string is null or empty (ex. from clearing text) run right away
                if (StringUtils.isNullOrEmpty(e.toString())) {
                    runSearchOp(e)
                } else {
                    timer = Timer().apply {
                        schedule(object : TimerTask() {
                            override fun run() {
                                runSearchOp(e)
                            }
                        }, DELAY)
                    }
                }
            }

            private fun runSearchOp(e: Editable) {
                runWithView {
                    val newText = e.toString()
                    searchBarBinding.searchCloseButton.visibility = if (newText.isEmpty()) View.GONE else View.VISIBLE
                    fetchLocations(newText)
                }
            }
        })
        searchBarBinding.searchView.onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                showInputMethod(v.findFocus())
            } else {
                hideInputMethod(v)
            }
        }
        searchBarBinding.searchView.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                fetchLocations(v.text.toString())
                hideInputMethod(v)
                return@OnEditorActionListener true
            }
            false
        })

        /*
           Capture touch events on RecyclerView
           We're not using ADJUST_RESIZE so hide the keyboard when necessary
           Hide the keyboard if we're scrolling to the bottom (so the bottom items behind the keyboard are visible)
           Leave the keyboard up if we're scrolling to the top (items at the top are already visible)
        */
        binding.recyclerView.setOnTouchListener(object : OnTouchListener {
            private var mY = 0
            private var shouldCloseKeyboard = false
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> mY = event.y.toInt()
                    MotionEvent.ACTION_UP -> {
                        mY = event.y.toInt()
                        if (shouldCloseKeyboard) {
                            hideInputMethod(v)
                            shouldCloseKeyboard = false
                        }
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val newY = event.y.toInt()
                        val dY = mY - newY
                        mY = newY
                        // Set flag to hide the keyboard if we're scrolling down
                        // So we can see what's behind the keyboard
                        shouldCloseKeyboard = dY > 0
                    }
                }
                return false
            }
        })
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (recyclerView.computeVerticalScrollOffset() > 0) {
                    ViewCompat.setElevation(searchBarBinding.root, ContextUtils.dpToPx(appCompatActivity!!, 4f))
                } else {
                    ViewCompat.setElevation(searchBarBinding.root, 0f)
                }
            }
        })

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        binding.recyclerView.setHasFixedSize(true)

        // use a linear layout manager
        mLayoutManager = LinearLayoutManager(appCompatActivity)
        binding.recyclerView.layoutManager = mLayoutManager

        // specify an adapter (see also next example)
        mAdapter = LocationQueryAdapter(ArrayList())
        mAdapter.setOnClickListener(recyclerClickListener)
        binding.recyclerView.adapter = mAdapter

        if (savedInstanceState != null) {
            val text = savedInstanceState.getString(KEY_SEARCHTEXT)
            if (!StringUtils.isNullOrWhitespace(text)) {
                searchBarBinding.searchView.setText(text, TextView.BufferType.EDITABLE)
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requestSearchbarFocus()
    }

    override fun updateWindowColors() {
        val bg_color = if (Settings.getUserThemeMode() != UserThemeMode.AMOLED_DARK) {
            ContextUtils.getColor(appCompatActivity!!, android.R.attr.colorBackground)
        } else {
            Colors.BLACK
        }

        binding.rootView.setBackgroundColor(bg_color)
        binding.rootView.setStatusBarBackgroundColor(bg_color)
        searchBarBinding.root.setBackgroundColor(bg_color)
    }

    override fun onDestroyView() {
        job?.cancel()
        super.onDestroyView()
    }

    fun fetchLocations(queryString: String?) {
        // Cancel pending searches
        job?.cancel()
        if (!StringUtils.isNullOrWhitespace(queryString)) {
            job = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                try {
                    val results = withContext(Dispatchers.IO) {
                        wm.getLocations(queryString)
                    }

                    launch(Dispatchers.Main.immediate) {
                        mAdapter.setLocations(ArrayList(results))
                    }
                } catch (e: Exception) {
                    launch(Dispatchers.Main.immediate) {
                        if (e is WeatherException) {
                            showSnackbar(Snackbar.make(e.message, Snackbar.Duration.SHORT),
                                    SnackbarWindowAdjustCallback(appCompatActivity!!))
                        }
                        mAdapter.setLocations(listOf(LocationQueryViewModel()))
                    }
                }
            }
        } else if (StringUtils.isNullOrWhitespace(queryString)) {
            // Cancel pending searches
            job?.cancel()
            // Hide flyout if query is empty or null
            mAdapter.dataset.clear()
            mAdapter.notifyDataSetChanged()
        }
    }

    fun requestSearchbarFocus() {
        searchBarBinding.searchView.requestFocus()
    }

    private fun showInputMethod(view: View?) {
        val imm = appCompatActivity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        if (imm != null && view != null) {
            imm.showSoftInput(view, 0)
        }
    }

    private fun hideInputMethod(view: View?) {
        val imm = appCompatActivity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        if (imm != null && view != null) {
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_SEARCHTEXT,
                if (searchBarBinding.searchView.text != null && !StringUtils.isNullOrWhitespace(searchBarBinding.searchView.text.toString())) {
                    searchBarBinding.searchView.text.toString()
                } else {
                    ""
                })

        super.onSaveInstanceState(outState)
    }
}