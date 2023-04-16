package com.thewizrd.simpleweather.widgets.preferences

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.launch
import androidx.core.location.LocationManagerCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.thewizrd.common.helpers.backgroundLocationPermissionEnabled
import com.thewizrd.common.helpers.getBackgroundLocationRationale
import com.thewizrd.common.helpers.locationPermissionEnabled
import com.thewizrd.common.location.LocationResult
import com.thewizrd.common.viewmodels.LocationSearchResult
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.controls.ComboBoxItem
import com.thewizrd.shared_resources.di.localBroadcastManager
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.LocationQuery
import com.thewizrd.shared_resources.locationdata.toLocationData
import com.thewizrd.shared_resources.utils.CommonActions
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.weatherdata.model.LocationType
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.preferences.ArrayListPreference
import com.thewizrd.simpleweather.snackbar.Snackbar
import com.thewizrd.simpleweather.widgets.WeatherWidgetProvider
import com.thewizrd.simpleweather.widgets.WidgetGraphType
import com.thewizrd.simpleweather.widgets.WidgetType
import com.thewizrd.simpleweather.widgets.WidgetUpdaterHelper
import com.thewizrd.simpleweather.widgets.WidgetUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.android.material.snackbar.Snackbar as materialSnackbar

class WeatherWidgetPreferenceFragment : BaseWeatherWidgetPreferenceFragment() {
    private lateinit var favorites: MutableCollection<LocationData>

    private var initializeWidgetJob: Job? = null

    private lateinit var locationPref: ArrayListPreference
    private lateinit var hideLocNamePref: SwitchPreference
    private lateinit var useTimeZonePref: SwitchPreference
    private lateinit var fcastOptPref: ListPreference
    private lateinit var tap2switchPref: SwitchPreference
    private lateinit var graphTypePref: ListPreference

    override fun getPreferencesResId(): Int = R.xml.pref_widgetconfig

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        favorites = ArrayList()
        locationPref = findPreference(KEY_LOCATION)!!

        lifecycleScope.launch {
            locationPref.addEntry(R.string.pref_item_gpslocation, Constants.KEY_GPS)
            locationPref.addEntry(R.string.label_btn_add_location, Constants.KEY_SEARCH)

            val favs = settingsManager.getFavorites()
            favorites.addAll(favs)
            for (location in favorites) {
                locationPref.insertEntry(
                    locationPref.entryCount - 1,
                    location.name, location.query
                )
            }
            if (locationPref.entryCount > MAX_LOCATIONS)
                locationPref.removeEntry(locationPref.entryCount - 1)

            // Reset value
            locationPref.value = null

            if (!savedInstanceState?.getString(Constants.WIDGETKEY_LOCATION).isNullOrBlank()) {
                locationPref.value = savedInstanceState?.getString(Constants.WIDGETKEY_LOCATION)
            } else if (arguments?.containsKey(WeatherWidgetProvider.EXTRA_LOCATIONQUERY) == true) {
                val locName = arguments?.getString(WeatherWidgetProvider.EXTRA_LOCATIONNAME)
                val locQuery = arguments?.getString(WeatherWidgetProvider.EXTRA_LOCATIONQUERY)

                if (locName != null) {
                    lastSelectedValue = locQuery
                    locationPref.value = lastSelectedValue.toString()
                } else {
                    locationPref.setValueIndex(0)
                }
            } else {
                locationPref.setValueIndex(0)
            }

            viewLifecycleOwnerLiveData.observe(
                this@WeatherWidgetPreferenceFragment,
                object : Observer<LifecycleOwner> {
                    override fun onChanged(value: LifecycleOwner) {
                        viewLifecycleOwnerLiveData.removeObserver(this)
                        if (locationPref.value == Constants.KEY_GPS && !requireContext().backgroundLocationPermissionEnabled()) {
                            binding.bgLocationLayout.visibility = View.VISIBLE
                        }
                        updateLocationView()
                    }
                })
        }

        locationPref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { pref, newValue ->
                val selectedValue = newValue as CharSequence

                updateMockLocation(
                    locationPref.findEntryFromValue(selectedValue)?.toString()
                        ?: pref.context.getString(R.string.pref_location),
                    selectedValue.toString()
                )

                if (Constants.KEY_SEARCH.contentEquals(selectedValue)) {
                    // Setup search UI
                    locationSearchLauncher.launch()
                    searchLocation = null
                    binding.bgLocationLayout.visibility = View.GONE
                    return@OnPreferenceChangeListener false
                } else if (Constants.KEY_GPS.contentEquals(selectedValue)) {
                    lastSelectedValue = null
                    searchLocation = null
                    if (!pref.context.backgroundLocationPermissionEnabled()) {
                        binding.bgLocationLayout.visibility = View.VISIBLE
                    }
                } else {
                    lastSelectedValue = selectedValue
                    binding.bgLocationLayout.visibility = View.GONE
                }

                updateLocationView()
                true
        }

        hideLocNamePref = findPreference(KEY_HIDELOCNAME)!!

        hideLocNamePref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                mWidgetOptions.putBoolean(KEY_HIDELOCNAME, newValue as Boolean)
                hideLocNamePref.isChecked = newValue
                updateWidgetView()
                true
            }

        if (WidgetUtils.isLocationNameOptionalWidget(mWidgetType)) {
            hideLocNamePref.isChecked = WidgetUtils.isLocationNameHidden(mAppWidgetId)
            hideLocNamePref.isVisible = true
        } else {
            hideLocNamePref.isChecked = false
            hideLocNamePref.isVisible = false
        }

        useTimeZonePref = findPreference(KEY_USETIMEZONE)!!

        useTimeZonePref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                mWidgetOptions.putBoolean(KEY_USETIMEZONE, newValue as Boolean)
                updateWidgetView()
                true
            }

        if (WidgetUtils.isClockWidget(mWidgetType) || WidgetUtils.isDateWidget(mWidgetType)) {
            useTimeZonePref.isChecked = WidgetUtils.useTimeZone(mAppWidgetId)
            findPreference<Preference>(KEY_CATCLOCKDATE)!!.isVisible = true
        } else {
            findPreference<Preference>(KEY_CATCLOCKDATE)!!.isVisible = false
        }

        // Forecast Preferences
        fcastOptPref = findPreference(KEY_FORECASTOPTION)!!
        fcastOptPref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                val fcastOptValue = newValue.toString().toInt()
                mWidgetOptions.putSerializable(
                    KEY_FORECASTOPTION,
                    WidgetUtils.ForecastOption.valueOf(fcastOptValue)
                )
                updateWidgetView()

                tap2switchPref.isVisible = (fcastOptValue == WidgetUtils.ForecastOption.FULL.value)

                true
            }

        tap2switchPref = findPreference(KEY_TAP2SWITCH)!!
        tap2switchPref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                mWidgetOptions.putBoolean(KEY_TAP2SWITCH, newValue as Boolean)
                true
            }

        if (WidgetUtils.isForecastWidget(mWidgetType) && mWidgetType != WidgetType.Widget4x2MaterialYou && mWidgetType != WidgetType.Widget4x4MaterialYou) {
            fcastOptPref.setValueIndex(WidgetUtils.getForecastOption(mAppWidgetId).value)
            fcastOptPref.callChangeListener(fcastOptPref.value)
            findPreference<Preference>(KEY_FORECAST)!!.isVisible = true
            tap2switchPref.isChecked = WidgetUtils.isTap2Switch(mAppWidgetId)
        } else if (mWidgetType == WidgetType.Widget4x2Graph) {
            findPreference<Preference>(KEY_FORECAST)!!.isVisible = true
            fcastOptPref.isVisible = false
            tap2switchPref.isVisible = false

            graphTypePref = findPreference(KEY_GRAPHTYPEOPTION)!!
            graphTypePref.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, newValue ->
                    mWidgetOptions.putSerializable(
                        KEY_GRAPHTYPEOPTION,
                        WidgetGraphType.valueOf(newValue.toString().toInt())
                    )
                    updateWidgetView()
                    true
                }
            graphTypePref.isVisible = true

            val graphType = WidgetUtils.getWidgetGraphType(mAppWidgetId)
            graphTypePref.value = graphType.value.toString()
            graphTypePref.callChangeListener(graphTypePref.value)
        } else {
            fcastOptPref.setValueIndex(WidgetUtils.ForecastOption.FULL.value)
            findPreference<Preference>(KEY_FORECAST)!!.isVisible = false
        }
    }

    override fun onSetupActivityResult(result: ActivityResult) {
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                // Get result data
                val dataJson = result.data?.getStringExtra(Constants.KEY_DATA)

                if (dataJson?.isNotBlank() == true) {
                    lifecycleScope.launch {
                        val locData = JSONParser.deserializer<LocationData>(dataJson)

                        if (locData?.locationType == LocationType.SEARCH) {
                            // Add location to adapter and select it
                            favorites.add(locData)
                            val idx = locationPref.entryCount - 1
                            locationPref.insertEntry(idx, locData.name, locData.query)
                            locationPref.setValueIndex(idx)
                            locationPref.callChangeListener(locData.query)
                        } else {
                            // GPS; set to first selection
                            locationPref.setValueIndex(0)
                        }
                    }
                }
            }
            else -> {
                // Setup was cancelled. Cancel widget setup
                cancelActivityResult()
            }
        }
    }

    override fun onLocationSearchResult(result: LocationSearchResult) {
        when (result) {
            is LocationSearchResult.AlreadyExists -> {
                if (result.data.isValid) {
                    val idx = locationPref.findIndexOfValue(result.data.query)
                    if (idx > 0) {
                        locationPref.setValueIndex(idx)
                        locationPref.callChangeListener(result.data.query)
                    }
                }
            }
            is LocationSearchResult.Success -> {
                if (result.data.isValid) {
                    LocationQuery(result.data).also {
                        val item = ComboBoxItem(it.locationName, it.locationQuery)
                        val idx = locationPref.entryCount - 1

                        locationPref.insertEntry(idx, item.display, item.value)
                        locationPref.setValueIndex(idx)

                        if (locationPref.entryCount > MAX_LOCATIONS) {
                            locationPref.removeEntry(locationPref.entryCount - 1)
                        }
                        searchLocation = it

                        locationPref.callChangeListener(item.value)
                    }
                }
            }
            is LocationSearchResult.Failed -> {
                searchLocation = null
            }
        }
    }

    override fun initializeWidget() {
        initializeWidgetJob = viewLifecycleOwner.lifecycleScope.launch {
            binding.widgetContainer.removeAllViews()

            val widgetType = WidgetUtils.getWidgetTypeFromID(mAppWidgetId)
            val info = WidgetUtils.getWidgetProviderInfoFromType(widgetType) ?: return@launch

            val views = withContext(Dispatchers.Default) {
                WidgetUpdaterHelper.buildUpdate(
                    mWidgetViewCtx, info, mAppWidgetId,
                    mockLocationData,
                    mockWeatherModel,
                    mWidgetOptions,
                    false
                )
            }

            val widgetView = views.apply(mWidgetViewCtx, binding.widgetContainer)
            binding.widgetContainer.addView(widgetView)
            widgetView.updateLayoutParams<FrameLayout.LayoutParams> {
                height = mWidgetViewCtx.dpToPx(
                    96f * when (mWidgetType) {
                        WidgetType.Unknown -> 4
                        WidgetType.Widget1x1 -> 1
                        WidgetType.Widget2x2 -> 2
                        WidgetType.Widget4x1 -> 1
                        WidgetType.Widget4x2 -> 2
                        WidgetType.Widget4x1Google -> 1
                        WidgetType.Widget4x1Notification -> 1
                        WidgetType.Widget4x2Clock -> 2
                        WidgetType.Widget4x2Huawei -> 2
                        WidgetType.Widget2x2MaterialYou -> 2
                        WidgetType.Widget2x2PillMaterialYou -> 2
                        WidgetType.Widget4x2MaterialYou -> 2
                        WidgetType.Widget4x4MaterialYou -> 3.5f
                        WidgetType.Widget4x3Locations -> 3
                        WidgetType.Widget3x1MaterialYou -> 1
                        WidgetType.Widget4x2Graph -> 2
                        WidgetType.Widget4x2Tomorrow -> 2
                    }.toFloat()
                ).toInt()
                width = mWidgetViewCtx.dpToPx(
                    96 * when (mWidgetType) {
                        WidgetType.Unknown -> 4
                        WidgetType.Widget1x1 -> 1
                        WidgetType.Widget2x2 -> 4 /* 2 is to small */
                        WidgetType.Widget4x1 -> 4
                        WidgetType.Widget4x2 -> 4
                        WidgetType.Widget4x1Google -> 4
                        WidgetType.Widget4x1Notification -> 4
                        WidgetType.Widget4x2Clock -> 4
                        WidgetType.Widget4x2Huawei -> 4
                        WidgetType.Widget2x2MaterialYou -> 2
                        WidgetType.Widget2x2PillMaterialYou -> 2
                        WidgetType.Widget4x2MaterialYou -> 4
                        WidgetType.Widget4x4MaterialYou -> 4
                        WidgetType.Widget4x3Locations -> 4
                        WidgetType.Widget3x1MaterialYou -> 3
                        WidgetType.Widget4x2Graph -> 4
                        WidgetType.Widget4x2Tomorrow -> 4
                    }.toFloat()
                ).toInt()
                gravity = Gravity.CENTER
            }

            resizeWidgetFrame()

            updateLocationView()
            updateBackground()
        }.also {
            it.invokeOnCompletion {
                initializeWidgetJob = null
            }
        }
    }

    private fun updateLocationView() {
        val locationView = binding.widgetContainer.findViewById<TextView>(R.id.location_name)
        if (locationView != null) {
            locationView.text = lastSelectedValue?.let {
                locationPref.findEntryFromValue(it)?.toString()
            } ?: locationView.context.getString(R.string.pref_location)

            locationView.isVisible = !hideLocNamePref.isChecked
        }
    }

    override fun updateWidgetView() {
        runWithView {
            initializeWidgetJob?.join()

            if (binding.widgetContainer.childCount > 0) {
                val views = withContext(Dispatchers.Default) {
                    WidgetUpdaterHelper.buildUpdate(
                        mWidgetViewCtx, mWidgetInfo, mAppWidgetId,
                        mockLocationData,
                        mockWeatherModel,
                        mWidgetOptions,
                        false
                    )
                }

                views.reapply(mWidgetViewCtx, binding.widgetContainer.getChildAt(0))

                // Create view
                updateLocationView()
                updateBackground()
            } else {
                initializeWidget()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // Reset to last selected item
        if (searchLocation == null && lastSelectedValue != null) {
            locationPref.value = lastSelectedValue.toString()
        }
        outState.putString(Constants.WIDGETKEY_LOCATION, locationPref.value)

        super.onSaveInstanceState(outState)
    }

    override fun prepareWidget() {
        lifecycleScope.launch {
            val ctx = requireContext()

            locationPref.value?.let { locationItemValue ->
                if (Constants.KEY_GPS == locationItemValue) {
                    if (!ctx.locationPermissionEnabled()) {
                        locationPermissionLauncher.requestLocationPermission()
                        return@launch
                    }

                    prepareGPSWidget()
                } else {
                    prepareSearchWidget()
                }
            } ?: cancelActivityResult()
        }
    }

    private suspend fun prepareGPSWidget() {
        val locationAvailable = updateGPSLocation()

        if (locationAvailable) {
            settingsManager.setFollowGPS(true)

            // Save data for widget
            WidgetUtils.deleteWidget(mAppWidgetId)
            WidgetUtils.saveLocationData(mAppWidgetId, null)
            WidgetUtils.addWidgetId(Constants.KEY_GPS, mAppWidgetId)
            finalizeWidgetUpdate()
        } else {
            context?.let {
                showSnackbar(
                    Snackbar.make(it, R.string.error_retrieve_location, Snackbar.Duration.SHORT)
                )
            }
        }
    }

    private suspend fun updateGPSLocation(): Boolean {
        val ctx = requireContext()

        if (settingsManager.useFollowGPS() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !settingsManager.requestedBGAccess() && !ctx.backgroundLocationPermissionEnabled()) {
            val snackbar = Snackbar.make(
                ctx,
                ctx.getBackgroundLocationRationale(),
                Snackbar.Duration.VERY_LONG
            ).apply {
                setAction(android.R.string.ok) {
                    locationPermissionLauncher.requestBackgroundLocationPermission()
                }
            }
            showSnackbar(snackbar, object : materialSnackbar.Callback() {
                override fun onDismissed(transientBottomBar: materialSnackbar?, event: Int) {
                    super.onDismissed(transientBottomBar, event)
                    if (event != BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_ACTION) {
                        prepareWidget()
                    }
                }
            })
            settingsManager.setRequestBGAccess(true)
            return false
        }

        val locMan = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
            showSnackbar(
                Snackbar.make(
                    ctx,
                    R.string.error_enable_location_services,
                    Snackbar.Duration.SHORT
                )
            )
        }

        val lastGPSLocData = settingsManager.getLastGPSLocData()

        // Check if last location exists
        if (lastGPSLocData == null || !lastGPSLocData.isValid) {
            val result = updateLocation()

            return when (result) {
                is LocationResult.Changed -> {
                    // Save location as last known
                    settingsManager.saveLastGPSLocData(result.data)
                    localBroadcastManager.sendBroadcast(Intent(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE))
                    true
                }
                is LocationResult.PermissionDenied -> {
                    showSnackbar(
                        Snackbar.make(
                            ctx,
                            R.string.error_location_denied,
                            Snackbar.Duration.SHORT
                        )
                    )
                    false
                }
                else -> {
                    showSnackbar(
                        Snackbar.make(
                            ctx,
                            R.string.error_retrieve_location,
                            Snackbar.Duration.SHORT
                        )
                    )
                    false
                }
            }
        }

        return true
    }

    private suspend fun prepareSearchWidget() {
        val location = getSearchLocation()

        if (location != null) {
            // Save data for widget
            WidgetUtils.deleteWidget(mAppWidgetId)
            WidgetUtils.saveLocationData(mAppWidgetId, location)
            WidgetUtils.addWidgetId(location.query, mAppWidgetId)
            finalizeWidgetUpdate()
        } else {
            cancelActivityResult()
        }
    }

    private suspend fun getSearchLocation(): LocationData? {
        var locData: LocationData? = null

        // Widget ID exists in prefs
        if (WidgetUtils.exists(mAppWidgetId)) {
            locData = WidgetUtils.getLocationData(mAppWidgetId)
        }

        // Changing location to whatever
        if (locData == null || locationPref.value != locData.query) {
            // Get location data
            val itemValue = locationPref.value
            locData = favorites.firstOrNull { input -> input.query == itemValue }

            if (locData == null) {
                searchLocation?.toLocationData()?.takeIf { it.isValid }
                    ?.let {
                        locData = it

                        // Add location to favs
                        withContext(Dispatchers.IO) {
                            settingsManager.addLocation(it)
                        }
                    }
            }
        }

        return locData
    }

    override fun finalizeWidgetUpdate() {
        // Save widget preferences
        WidgetUtils.setLocationNameHidden(mAppWidgetId, hideLocNamePref.isChecked)
        WidgetUtils.setForecastOption(mAppWidgetId, fcastOptPref.value.toInt())
        WidgetUtils.setTap2Switch(mAppWidgetId, tap2switchPref.isChecked)
        WidgetUtils.setUseTimeZone(mAppWidgetId, useTimeZonePref.isChecked)
        if (mWidgetType == WidgetType.Widget4x2Graph) {
            WidgetUtils.setWidgetGraphType(mAppWidgetId, graphTypePref.value.toInt())
        }

        super.finalizeWidgetUpdate()
    }
}