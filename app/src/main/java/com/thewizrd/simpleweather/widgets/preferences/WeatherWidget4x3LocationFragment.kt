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
import androidx.activity.result.ActivityResult
import androidx.core.location.LocationManagerCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.thewizrd.common.helpers.backgroundLocationPermissionEnabled
import com.thewizrd.common.helpers.getBackgroundLocationRationale
import com.thewizrd.common.location.LocationResult
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.di.localBroadcastManager
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.CommonActions
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.weatherdata.model.LocationType
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.preferences.ArrayMultiSelectListPreference
import com.thewizrd.simpleweather.snackbar.Snackbar
import com.thewizrd.simpleweather.widgets.MultiLocationPreferenceDialogFragment
import com.thewizrd.simpleweather.widgets.WidgetUtils
import com.thewizrd.simpleweather.widgets.remoteviews.WeatherWidget4x3LocationsCreator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections

class WeatherWidget4x3LocationFragment : BaseWeatherWidgetPreferenceFragment() {
    private var job: Job? = null
    private var initializeWidgetJob: Job? = null

    private lateinit var locationPref: ArrayMultiSelectListPreference

    override fun getPreferencesResId(): Int = R.xml.pref_widget4x3_locations

    override fun onDestroyView() {
        job?.cancel()
        super.onDestroyView()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        locationPref = findPreference(KEY_LOCATION)!!

        lifecycleScope.launch {
            locationPref.addEntry(R.string.pref_item_gpslocation, Constants.KEY_GPS)

            val favs = settingsManager.getFavorites() ?: emptyList()
            favs.forEach { location ->
                locationPref.addEntry(location.name, location.query)
            }

            // Reset value
            locationPref.values = emptySet()

            if (!savedInstanceState?.getStringArray(Constants.WIDGETKEY_LOCATION).isNullOrEmpty()) {
                locationPref.values =
                    savedInstanceState?.getStringArray(Constants.WIDGETKEY_LOCATION)?.toSet()
            } else {
                WidgetUtils.getLocationDataSet(mAppWidgetId)?.let {
                    locationPref.values = it
                }
            }

            viewLifecycleOwnerLiveData.observe(
                this@WeatherWidget4x3LocationFragment,
                object : Observer<LifecycleOwner> {
                    override fun onChanged(value: LifecycleOwner) {
                        viewLifecycleOwnerLiveData.removeObserver(this)
                        if (locationPref.values?.contains(Constants.KEY_GPS) == true && !requireContext().backgroundLocationPermissionEnabled()) {
                            binding.bgLocationLayout.visibility = View.VISIBLE
                        }
                        value.lifecycleScope.launchWhenStarted {
                            updateWidgetView()
                        }
                    }
                })
        }

        locationPref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { pref, newValue ->
                job?.cancel()

                val selectedValues = newValue as? Set<*>
                if (selectedValues?.contains(Constants.KEY_GPS) == true) {
                    if (!pref.context.backgroundLocationPermissionEnabled()) {
                        binding.bgLocationLayout.visibility = View.VISIBLE
                    }
                } else {
                    binding.bgLocationLayout.visibility = View.GONE
                }

                updateWidgetView()
                true
            }
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is ArrayMultiSelectListPreference && preference.key == KEY_LOCATION) {
            val DIALOG_FRAGMENT_TAG =
                "${MultiLocationPreferenceDialogFragment::class.java.name}.DIALOG"

            // check if dialog is already showing
            if (parentFragmentManager.findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) {
                return
            }

            val f = MultiLocationPreferenceDialogFragment.newInstance(preference.key)
            f.setTargetFragment(this, 0)
            f.show(parentFragmentManager, DIALOG_FRAGMENT_TAG)
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    override fun onSetupActivityResult(result: ActivityResult) {
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                // Get result data
                val dataJson = result.data?.getStringExtra(Constants.KEY_DATA)

                if (dataJson?.isNotBlank() == true) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val locData = JSONParser.deserializer<LocationData>(dataJson)

                        if (locData?.locationType == LocationType.SEARCH) {
                            // Add location to adapter and select it
                            locationPref.addEntry(locData.name, locData.query)
                            locationPref.values = locationPref.values?.let {
                                LinkedHashSet(it).apply {
                                    add(locData.query)
                                }
                            }
                            locationPref.callChangeListener(locData.query)
                        } else {
                            // GPS; set to first selection
                            locationPref.values = locationPref.values?.let {
                                LinkedHashSet<String>(it.size + 1).apply {
                                    add(Constants.KEY_GPS)
                                    addAll(it)
                                }
                            }
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

    override fun initializeWidget() {
        initializeWidgetJob = viewLifecycleOwner.lifecycleScope.launch {
            binding.widgetContainer.removeAllViews()

            val views = withContext(Dispatchers.Default) {
                WeatherWidget4x3LocationsCreator(requireContext()).run {
                    buildUpdate(
                        mAppWidgetId,
                        Collections.nCopies(locationPref.values?.size ?: 0, mockLocationData),
                        Collections.nCopies(locationPref.values?.size ?: 0, mockWeatherModel),
                        mWidgetOptions
                    )
                }
            }

            val widgetView = views.apply(mWidgetViewCtx, binding.widgetContainer)
            binding.widgetContainer.addView(widgetView)
            widgetView.updateLayoutParams<FrameLayout.LayoutParams> {
                height = mWidgetViewCtx.dpToPx(96f * 3).toInt()
                width = mWidgetViewCtx.dpToPx(96f * 4).toInt()
                gravity = Gravity.CENTER
            }

            resizeWidgetFrame()

            updateBackground()
        }.also {
            it.invokeOnCompletion {
                initializeWidgetJob = null
            }
        }
    }

    override fun updateWidgetView() {
        runWithView {
            initializeWidgetJob?.join()

            if (binding.widgetContainer.childCount > 0) {
                val views = withContext(Dispatchers.Default) {
                    WeatherWidget4x3LocationsCreator(requireContext()).run {
                        buildUpdate(
                            mAppWidgetId,
                            Collections.nCopies(locationPref.values?.size ?: 0, mockLocationData),
                            Collections.nCopies(locationPref.values?.size ?: 0, mockWeatherModel),
                            mWidgetOptions
                        )
                    }
                }

                views.reapply(mWidgetViewCtx, binding.widgetContainer.getChildAt(0))

                // Create view
                updateBackground()
            } else {
                initializeWidget()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // Reset to last selected item
        if (!locationPref.values.isNullOrEmpty()) {
            outState.putStringArray(
                Constants.WIDGETKEY_LOCATION,
                locationPref.values.toTypedArray()
            )
        }

        super.onSaveInstanceState(outState)
    }

    override fun prepareWidget() {
        lifecycleScope.launch {
            // Get location data
            val ctx = requireContext()
            val selectedValues = locationPref.values ?: emptySet()

            if (selectedValues.isNotEmpty()) {
                val containsGps = selectedValues.contains(Constants.KEY_GPS)

                if (containsGps && !updateGPSLocation()) {
                    showSnackbar(
                        Snackbar.make(
                            ctx,
                            R.string.error_retrieve_location,
                            Snackbar.Duration.SHORT
                        )
                    )
                    return@launch
                }

                if (containsGps) {
                    settingsManager.setFollowGPS(true)
                }

                val nonSelectedValues = locationPref.entryValues.filterNot {
                    selectedValues.contains(it)
                }.map { it.toString() }

                // Save data for widget
                WidgetUtils.deleteWidget(mAppWidgetId)
                WidgetUtils.saveLocationDataSet(mAppWidgetId, selectedValues)

                nonSelectedValues.forEach {
                    WidgetUtils.removeWidgetId(it, mAppWidgetId, false)
                }

                selectedValues.forEach {
                    WidgetUtils.addWidgetId(it, mAppWidgetId)
                }

                finalizeWidgetUpdate()
            } else {
                showSnackbar(
                    Snackbar.make(ctx, R.string.prompt_location_not_set, Snackbar.Duration.SHORT)
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
            showSnackbar(
                snackbar,
                object : com.google.android.material.snackbar.Snackbar.Callback() {
                    override fun onDismissed(
                        transientBottomBar: com.google.android.material.snackbar.Snackbar?,
                        event: Int
                    ) {
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
}