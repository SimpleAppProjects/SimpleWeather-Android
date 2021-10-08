package com.thewizrd.simpleweather.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.ConcatAdapter
import androidx.wear.widget.SwipeDismissFrameLayout
import androidx.wear.widget.WearableLinearLayoutManager
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.controls.LocationQueryViewModel
import com.thewizrd.shared_resources.helpers.ListAdapterOnClickInterface
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.remoteconfig.RemoteConfig
import com.thewizrd.shared_resources.tzdb.TZDBCache
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.shared_resources.wearable.WearableDataSync
import com.thewizrd.shared_resources.weatherdata.*
import com.thewizrd.shared_resources.weatherdata.model.Forecasts
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecasts
import com.thewizrd.simpleweather.BuildConfig
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.adapters.ListHeaderAdapter
import com.thewizrd.simpleweather.adapters.LocationQueryAdapter
import com.thewizrd.simpleweather.adapters.LocationQueryFooterAdapter
import com.thewizrd.simpleweather.adapters.SpacerAdapter
import com.thewizrd.simpleweather.databinding.FragmentLocationSearchBinding
import com.thewizrd.simpleweather.helpers.CustomScrollingLayoutCallback
import com.thewizrd.simpleweather.helpers.SpacerItemDecoration
import kotlinx.coroutines.*
import java.util.*

class LocationSearchFragment : SwipeDismissFragment() {
    companion object {
        private const val TAG = "LocSearchFragment"
        private const val KEY_SEARCHTEXT = "search_text"
        private const val REQUEST_CODE_VOICE_INPUT = 0
    }

    private lateinit var binding: FragmentLocationSearchBinding
    private lateinit var mAdapter: LocationQueryAdapter
    private lateinit var swipeCallback: SwipeDismissFrameLayout.Callback
    private val wm = WeatherManager.instance

    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AnalyticsLogger.logEvent("$TAG: onCreate")
    }

    private val recyclerClickListener =
        object : ListAdapterOnClickInterface<LocationQueryViewModel> {
            override fun onClick(view: View, item: LocationQueryViewModel) {
                viewLifecycleOwner.lifecycleScope.launch {
                    showLoading(true)
                    binding.recyclerView.isEnabled = false

                    // Cancel other tasks
                    job?.cancel()

                    supervisorScope {
                        val deferredJob =
                            viewLifecycleOwner.lifecycleScope.async(Dispatchers.Default) {
                                var queryResult: LocationQueryViewModel? = LocationQueryViewModel()

                                if (!item.locationQuery.isNullOrEmpty())
                                    queryResult = item

                                if (queryResult?.locationQuery.isNullOrEmpty()) {
                                    // Stop since there is no valid query
                                    throw CancellationException()
                                }

                                if (settingsManager.usePersonalKey() && settingsManager.getAPIKEY()
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
                                            Coordinate(
                                                loc.locationLat,
                                                loc.locationLong
                                            ), loc.weatherSource
                                        )
                                    }
                                }

                                if (queryResult == null) {
                                    throw InterruptedException()
                                } else if (queryResult.locationTZLong.isNullOrEmpty() && queryResult.locationLat != 0.0 && queryResult.locationLong != 0.0) {
                                    val tzId =
                                        TZDBCache.getTimeZone(
                                            queryResult.locationLat,
                                            queryResult.locationLong
                                        )
                                    if ("unknown" != tzId)
                                        queryResult.locationTZLong = tzId
                                }

                                if (!settingsManager.isWeatherLoaded() && !BuildConfig.IS_NONGMS) {
                                    // Set default provider based on location
                                    val provider =
                                        RemoteConfig.getDefaultWeatherProvider(queryResult.locationCountry)
                                    settingsManager.setAPI(provider)
                                    queryResult.updateWeatherSource(provider)
                                    wm.updateAPI()
                                }

                                if (!wm.isRegionSupported(queryResult.locationCountry)) {
                                    throw CustomException(R.string.error_message_weather_region_unsupported)
                                }

                                // Check if location already exists
                                val locData = settingsManager.getLocationData()
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
                                var weather = settingsManager.getWeatherData(location.query)
                                if (weather == null) {
                                    weather = wm.getWeather(location)
                                }

                                if (weather == null) {
                                    throw WeatherException(ErrorStatus.NOWEATHER)
                                } else if (wm.supportsAlerts() && wm.needsExternalAlertData()) {
                                    weather.weatherAlerts = wm.getAlerts(location)
                                }

                                // Save weather data
                                settingsManager.saveHomeData(location)
                                if (wm.supportsAlerts() && weather.weatherAlerts != null)
                                    settingsManager.saveWeatherAlerts(
                                        location,
                                        weather.weatherAlerts
                                    )
                                settingsManager.saveWeatherData(weather)
                                settingsManager.saveWeatherForecasts(Forecasts(weather))
                                settingsManager.saveWeatherForecasts(
                                    location.query,
                                    weather.hrForecast?.map { input ->
                                        HourlyForecasts(
                                            weather.query,
                                            input!!
                                        )
                                    })

                                // If we're changing locations, trigger an update
                                if (settingsManager.isWeatherLoaded()) {
                                    LocalBroadcastManager.getInstance(fragmentActivity)
                                        .sendBroadcast(Intent(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE))
                                }

                                // If we're using search
                                // make sure gps feature is off
                                settingsManager.setFollowGPS(false)
                                settingsManager.setWeatherLoaded(true)
                                settingsManager.setDataSync(WearableDataSync.OFF)
                                location
                            }.also {
                                job = it
                            }

                        deferredJob.invokeOnCompletion callback@{
                            if (it is CancellationException) {
                                runWithView {
                                    showLoading(false)
                                    binding.recyclerView.isEnabled = true
                                }
                                return@callback
                            }

                            val t = deferredJob.getCompletionExceptionOrNull()
                            if (t == null) {
                                val result = deferredJob.getCompleted()
                                runWithView { // Go back to where we started
                                    if (result != null) {
                                        // Send result to SetupActivity
                                        val data = JSONParser.serializer(
                                            result,
                                            LocationData::class.java
                                        )
                                        setFragmentResult(Constants.KEY_DATA, Bundle(1).apply {
                                            putString(Constants.KEY_DATA, data)
                                        })
                                    } else {
                                        showLoading(false)
                                        binding.recyclerView.isEnabled = true
                                    }
                                }
                            } else {
                                runWithView {
                                    if (t is WeatherException || t is CustomException) {
                                        showToast(t.message, Toast.LENGTH_SHORT)
                                    } else {
                                        showToast(
                                            R.string.error_retrieve_location,
                                            Toast.LENGTH_SHORT
                                        )
                                    }
                                    showLoading(false)
                                    binding.recyclerView.isEnabled = true
                                }
                            }
                        }
                    }
                }
            }
        }

    private fun showLoading(show: Boolean) {
        binding.progressBarContainer.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState) as ViewGroup?
        // Inflate the layout for this fragment
        binding = FragmentLocationSearchBinding.inflate(inflater, view, true)
        swipeCallback = object : SwipeDismissFrameLayout.Callback() {
            override fun onDismissed(layout: SwipeDismissFrameLayout) {
                layout.visibility = View.GONE
            }
        }
        binding.recyclerViewLayout.addCallback(swipeCallback)
        binding.keyboardButton.setOnClickListener {
            binding.searchView.requestFocus()
            showInputMethod(binding.searchView)
        }
        binding.voiceButton.setOnClickListener {
            binding.searchView.setText("")
            view!!.requestFocus()
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    .putExtra(RecognizerIntent.EXTRA_PROMPT, fragmentActivity.getString(R.string.location_search_hint))
            startActivityForResult(intent, REQUEST_CODE_VOICE_INPUT)
        }
        binding.searchView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // If we're using searchfragment
                // make sure gps feature is off
                if (settingsManager.useFollowGPS()) {
                    settingsManager.setFollowGPS(false)
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
        binding.searchView.onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                showInputMethod(v)
            } else {
                hideInputMethod(v)
            }
        }
        binding.searchView.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                doSearchAction()

                // If we're using searchfragment
                // make sure gps feature is off
                if (settingsManager.useFollowGPS()) {
                    settingsManager.setFollowGPS(false)
                }
                return@OnEditorActionListener true
            }
            false
        })

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        binding.recyclerView.setHasFixedSize(true)

        binding.recyclerView.addItemDecoration(
            SpacerItemDecoration(
                requireContext().dpToPx(16f).toInt(),
                requireContext().dpToPx(4f).toInt()
            )
        )
        binding.recyclerView.layoutManager =
            WearableLinearLayoutManager(fragmentActivity, CustomScrollingLayoutCallback())

        // specify an adapter (see also next example)
        mAdapter = LocationQueryAdapter()
        mAdapter.setOnClickListener(recyclerClickListener)
        binding.recyclerView.adapter = ConcatAdapter(
            ListHeaderAdapter(getString(R.string.label_nav_locations)),
            mAdapter,
            LocationQueryFooterAdapter(),
            SpacerAdapter(requireContext().dpToPx(48f).toInt())
        )
        binding.recyclerView.requestFocus()

        if (savedInstanceState != null) {
            val text = savedInstanceState.getString(KEY_SEARCHTEXT)
            if (!text.isNullOrEmpty()) {
                binding.searchView.setText(text, TextView.BufferType.EDITABLE)
            }
        }

        return view
    }

    override fun onDestroyView() {
        job?.cancel()
        hideInputMethod(binding.searchView)
        binding.recyclerViewLayout.removeCallback(swipeCallback)
        super.onDestroyView()
    }

    private fun doSearchAction() {
        binding.progressBarContainer.visibility = View.VISIBLE
        binding.recyclerViewLayout.visibility = View.VISIBLE
        binding.recyclerViewLayout.requestFocus()

        hideInputMethod(binding.searchView)

        fetchLocations(binding.searchView.text.toString())
    }

    private fun fetchLocations(queryString: String?) {
        // Cancel pending searches
        job?.cancel()
        if (!queryString.isNullOrEmpty()) {
            job = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                try {
                    val results = withContext(Dispatchers.IO) {
                        wm.getLocations(queryString)
                    }

                    launch(Dispatchers.Main.immediate) {
                        mAdapter.submitList(results.toList())
                        binding.progressBarContainer.visibility = View.GONE
                    }
                } catch (e: Exception) {
                    launch(Dispatchers.Main.immediate) {
                        if (e is WeatherException) {
                            showToast(e.message, Toast.LENGTH_SHORT)
                        }
                        mAdapter.submitList(listOf(LocationQueryViewModel()))
                    }
                }
            }
        } else if (queryString.isNullOrEmpty()) {
            // Cancel pending searches
            job?.cancel()
            binding.progressBarContainer.visibility = View.GONE
            binding.recyclerViewLayout.visibility = View.GONE
            binding.recyclerViewLayout.clearFocus()

            // Hide flyout if query is empty or null
            mAdapter.submitList(emptyList())
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK || data == null) {
            return
        }

        when (requestCode) {
            REQUEST_CODE_VOICE_INPUT -> {
                val results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)

                if (!results.isNullOrEmpty()) {
                    val text = results[0]
                    if (!text.isNullOrEmpty()) {
                        binding.searchView.setText(text)
                        doSearchAction()
                    }
                }
            }
        }
    }

    private fun showInputMethod(view: View?) {
        val imm =
            fragmentActivity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
                ?: return
        view?.let {
            imm.showSoftInput(it, InputMethodManager.SHOW_FORCED)
        }
    }

    private fun hideInputMethod(view: View?) {
        val imm = fragmentActivity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
                ?: return
        view?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_SEARCHTEXT,
                if (!binding.searchView.text.isNullOrEmpty()) {
                    binding.searchView.text.toString()
                } else {
                    ""
                })

        super.onSaveInstanceState(outState)
    }
}