package com.thewizrd.simpleweather.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.core.view.*
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.transition.MaterialContainerTransform
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.controls.LocationQueryViewModel
import com.thewizrd.shared_resources.helpers.ListAdapterOnClickInterface
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.remoteconfig.RemoteConfig
import com.thewizrd.shared_resources.tzdb.TZDBCache
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrColor
import com.thewizrd.shared_resources.weatherdata.WeatherManager
import com.thewizrd.shared_resources.weatherdata.model.Forecasts
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecasts
import com.thewizrd.simpleweather.BuildConfig
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.adapters.LocationQueryAdapter
import com.thewizrd.simpleweather.adapters.LocationQueryFooterAdapter
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
    private lateinit var mAdapter: ConcatAdapter
    private lateinit var mLocationAdapter: LocationQueryAdapter
    private lateinit var mFooterAdapter: LocationQueryFooterAdapter
    private lateinit var mLayoutManager: RecyclerView.LayoutManager
    private val wm = WeatherManager.instance

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

    private val recyclerClickListener =
        object : ListAdapterOnClickInterface<LocationQueryViewModel> {
            override fun onClick(view: View, item: LocationQueryViewModel) {
                val props = Bundle()
                props.putString("method", "recyclerClickListener.onClick")
                AnalyticsLogger.logEvent("$TAG: onClick", props)

                val navController = binding.root.findNavController()

                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main.immediate) {
                    showLoading(true)
                    enableRecyclerView(false)

                    // Cancel other tasks
                    job?.cancel()

                    supervisorScope {
                        val deferredJob =
                            viewLifecycleOwner.lifecycleScope.async(Dispatchers.Default) {
                                var queryResult: LocationQueryViewModel? = LocationQueryViewModel()

                                if (!item.locationQuery.isNullOrBlank())
                                    queryResult = item

                                if (queryResult?.locationQuery.isNullOrBlank()) {
                                    // Stop since there is no valid query
                                    throw CancellationException()
                                }

                                if (getSettingsManager().usePersonalKey() && getSettingsManager().getAPIKEY()
                                        .isNullOrBlank() && wm.isKeyRequired()
                                ) {
                                    throw CustomException(R.string.werror_invalidkey)
                                }

                                ensureActive()

                                // Need to get FULL location data for HERE API
                                // Data provided is incomplete
                                if (wm.getLocationProvider().needsLocationFromID()) {
                                    val loc = queryResult!!
                                    queryResult = withContext(Dispatchers.IO) {
                                        wm.getLocationProvider().getLocationFromID(loc)
                                    }
                                } else if (wm.getLocationProvider().needsLocationFromName()) {
                                    val loc = queryResult!!
                                    queryResult = withContext(Dispatchers.IO) {
                                        wm.getLocationProvider().getLocationFromName(loc)
                                    }
                                } else if (wm.getLocationProvider().needsLocationFromGeocoder()) {
                                    val loc = queryResult!!
                                    queryResult = withContext(Dispatchers.IO) {
                                        wm.getLocationProvider().getLocation(
                                            Coordinate(loc.locationLat, loc.locationLong),
                                            loc.weatherSource
                                        )
                                    }
                                }

                                if (queryResult == null || queryResult.locationQuery.isNullOrBlank()) {
                                    // Stop since there is no valid query
                                    throw CustomException(R.string.error_retrieve_location)
                                } else if (queryResult.locationTZLong.isNullOrBlank() && queryResult.locationLat != 0.0 && queryResult.locationLong != 0.0) {
                                    val tzId =
                                        TZDBCache.getTimeZone(
                                            queryResult.locationLat,
                                            queryResult.locationLong
                                        )
                                    if ("unknown" != tzId)
                                        queryResult.locationTZLong = tzId
                                }

                                if (!getSettingsManager().isWeatherLoaded() && !BuildConfig.IS_NONGMS) {
                                    // Set default provider based on location
                                    val provider =
                                        RemoteConfig.getDefaultWeatherProvider(queryResult.locationCountry)
                                    getSettingsManager().setAPI(provider)
                                    queryResult.updateWeatherSource(provider)
                                }

                                if (!wm.isRegionSupported(queryResult.locationCountry)) {
                                    throw CustomException(R.string.error_message_weather_region_unsupported)
                                }

                                // Check if location already exists
                                val locData = getSettingsManager().getLocationData()
                                val finalQueryResult: LocationQueryViewModel = queryResult
                                val loc =
                                    locData?.find { input -> input != null && input.query == finalQueryResult.locationQuery }

                                if (loc != null) {
                                    // Location exists; return
                                    return@async null
                                }

                                ensureActive()

                                val location = LocationData(queryResult)
                                if (!location.isValid) {
                                    throw CustomException(R.string.werror_noweather)
                                }
                                var weather = getSettingsManager().getWeatherData(location.query)
                                if (weather == null) {
                                    weather = wm.getWeather(location)
                                }

                                if (weather == null) {
                                    throw WeatherException(ErrorStatus.NOWEATHER)
                                } else if (wm.supportsAlerts() && wm.needsExternalAlertData()) {
                                    weather.weatherAlerts = wm.getAlerts(location)
                                }

                                // Save data
                                getSettingsManager().addLocation(location)
                                if (wm.supportsAlerts() && weather.weatherAlerts != null)
                                    getSettingsManager().saveWeatherAlerts(
                                        location,
                                        weather.weatherAlerts
                                    )
                                getSettingsManager().saveWeatherData(weather)
                                getSettingsManager().saveWeatherForecasts(Forecasts(weather))
                                getSettingsManager().saveWeatherForecasts(
                                    location.query,
                                    weather.hrForecast?.map { input ->
                                        HourlyForecasts(
                                            weather.query,
                                            input!!
                                        )
                                    })

                                getSettingsManager().setWeatherLoaded(true)

                                location
                            }.also {
                                job = it
                            }

                        deferredJob.invokeOnCompletion callback@{
                            if (it is CancellationException) {
                                runWithView {
                                    showLoading(false)
                                    enableRecyclerView(true)
                                }
                                return@callback
                            }

                            val t = deferredJob.getCompletionExceptionOrNull()
                            if (t == null) {
                                val result = deferredJob.getCompleted()
                                runWithView { // Go back to where we started
                                    if (result != null) {
                                        navController.previousBackStackEntry?.savedStateHandle?.set(
                                            Constants.KEY_DATA,
                                            withContext(Dispatchers.Default) {
                                                JSONParser.serializer(
                                                    result,
                                                    LocationData::class.java
                                                )
                                            })
                                    }
                                    navController.navigateUp()
                                }
                            } else {
                                runWithView {
                                    if (t is WeatherException || t is CustomException) {
                                        showSnackbar(
                                            Snackbar.make(t.message, Snackbar.Duration.SHORT),
                                            SnackbarWindowAdjustCallback(appCompatActivity!!)
                                        )
                                    } else {
                                        showSnackbar(
                                            Snackbar.make(
                                                R.string.error_retrieve_location,
                                                Snackbar.Duration.SHORT
                                            ),
                                            SnackbarWindowAdjustCallback(appCompatActivity!!)
                                        )
                                    }
                                    showLoading(false)
                                    enableRecyclerView(true)
                                }
                            }
                        }
                    }
                }
            }
        }

    private fun showLoading(show: Boolean) {
        searchBarBinding.searchProgressBar.visibility = if (show) View.VISIBLE else View.GONE

        if (show || searchBarBinding.searchView.text.isNullOrBlank())
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

        ViewCompat.setTransitionName(binding.root, Constants.SHARED_ELEMENT)
        ViewGroupCompat.setTransitionGroup((binding.root as ViewGroup), true)

        // Initialize
        //binding.recyclerView.setOnClickListener { v -> v.findNavController().navigateUp() }
        searchBarBinding.searchBackButton.setOnClickListener { v ->
            v.findNavController().navigateUp()
        }
        searchBarBinding.searchCloseButton.setOnClickListener {
            searchBarBinding.searchView.setText(
                ""
            )
        }
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
                if (e.isBlank()) {
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
                    supervisorScope {
                        ensureActive()
                        val newText = e.toString()
                        searchBarBinding.searchCloseButton.visibility =
                            if (newText.isEmpty()) View.GONE else View.VISIBLE
                        fetchLocations(newText)
                    }
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
        val gestureDetector = GestureDetectorCompat(
            requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {
                private val v = binding.recyclerView
                private var mY = 0
                private var shouldCloseKeyboard = false

                override fun onDown(e: MotionEvent?): Boolean {
                    e?.run {
                        mY = y.toInt()
                    }
                    return super.onDown(e)
                }

                override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                    findNavController().navigateUp()
                    return super.onSingleTapConfirmed(e)
                }

                override fun onScroll(
                    e1: MotionEvent?,
                    e2: MotionEvent?,
                    distanceX: Float,
                    distanceY: Float
                ): Boolean {
                    e2?.run {
                        val newY = y.toInt()
                        val dY = mY - newY
                        mY = newY
                        // Set flag to hide the keyboard if we're scrolling down
                        // So we can see what's behind the keyboard
                        shouldCloseKeyboard = dY > 0
                    }

                    if (shouldCloseKeyboard) {
                        hideInputMethod(v)
                        shouldCloseKeyboard = false
                    }

                    return super.onScroll(e1, e2, distanceX, distanceY)
                }

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent?,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    e2?.run {
                        val newY = y.toInt()
                        val dY = mY - newY
                        mY = newY
                        // Set flag to hide the keyboard if we're scrolling down
                        // So we can see what's behind the keyboard
                        shouldCloseKeyboard = dY > 0
                    }

                    if (shouldCloseKeyboard) {
                        hideInputMethod(v)
                        shouldCloseKeyboard = false
                    }

                    return super.onFling(e1, e2, velocityX, velocityY)
                }
            })

        binding.recyclerView.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        binding.recyclerView.setHasFixedSize(true)

        ViewCompat.setOnApplyWindowInsetsListener(binding.recyclerView) { v, i ->
            val insets = i.getInsets(WindowInsetsCompat.Type.systemBars())

            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = insets.bottom
            }

            i
        }

        // use a linear layout manager
        mLayoutManager = LinearLayoutManager(appCompatActivity)
        binding.recyclerView.layoutManager = mLayoutManager

        // specify an adapter (see also next example)
        mLocationAdapter = LocationQueryAdapter()
        mLocationAdapter.setOnClickListener(recyclerClickListener)
        binding.recyclerView.adapter = ConcatAdapter(mLocationAdapter).also { mAdapter = it }
        mFooterAdapter = LocationQueryFooterAdapter()

        if (savedInstanceState != null) {
            val text = savedInstanceState.getString(KEY_SEARCHTEXT)
            if (!text.isNullOrBlank()) {
                searchBarBinding.searchView.setText(text, TextView.BufferType.EDITABLE)
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requestSearchbarFocus()
    }

    override fun updateWindowColors() {
        var backgroundColor =
            appCompatActivity!!.getAttrColor(android.R.attr.colorBackground)
        var statusBarColor = appCompatActivity!!.getAttrColor(R.attr.colorSurface)
        if (getSettingsManager().getUserThemeMode() == UserThemeMode.AMOLED_DARK) {
            backgroundColor = Colors.BLACK
            statusBarColor = Colors.BLACK
        }

        binding.rootView.setBackgroundColor(backgroundColor)
        if (binding.appBar.background is MaterialShapeDrawable) {
            val materialShapeDrawable = binding.appBar.background as MaterialShapeDrawable
            materialShapeDrawable.fillColor = ColorStateList.valueOf(statusBarColor)
        } else {
            binding.appBar.setBackgroundColor(statusBarColor)
        }
    }

    override fun onDestroyView() {
        job?.cancel()
        hideInputMethod(searchBarBinding.searchView)
        super.onDestroyView()
    }

    fun fetchLocations(queryString: String?) {
        // Cancel pending searches
        job?.cancel()
        if (!queryString.isNullOrBlank()) {
            job = runWithView(Dispatchers.Default) {
                try {
                    val results = withContext(Dispatchers.IO) {
                        wm.getLocations(queryString)
                    }

                    launch(Dispatchers.Main.immediate) {
                        mLocationAdapter.submitList(results.toList())
                        mAdapter.addAdapter(mFooterAdapter)
                    }
                } catch (e: Exception) {
                    launch(Dispatchers.Main.immediate) {
                        if (e is WeatherException) {
                            showSnackbar(
                                Snackbar.make(e.message, Snackbar.Duration.SHORT),
                                SnackbarWindowAdjustCallback(appCompatActivity!!)
                            )
                        }
                        mLocationAdapter.submitList(listOf(LocationQueryViewModel()))
                        mAdapter.addAdapter(mFooterAdapter)
                    }
                }
            }
        } else if (queryString.isNullOrBlank()) {
            // Cancel pending searches
            job?.cancel()
            // Hide flyout if query is empty or null
            mLocationAdapter.submitList(emptyList())
            mAdapter.removeAdapter(mFooterAdapter)
        }
    }

    fun requestSearchbarFocus() {
        searchBarBinding.searchView.requestFocus()
    }

    private fun showInputMethod(view: View?) {
        val imm = appCompatActivity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
                ?: return
        view?.let {
            imm.showSoftInput(it, 0)
        }
    }

    private fun hideInputMethod(view: View?) {
        val imm = appCompatActivity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
                ?: return
        view?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_SEARCHTEXT,
                if (!searchBarBinding.searchView.text.isNullOrBlank()) {
                    searchBarBinding.searchView.text.toString()
                } else {
                    ""
                })

        super.onSaveInstanceState(outState)
    }
}