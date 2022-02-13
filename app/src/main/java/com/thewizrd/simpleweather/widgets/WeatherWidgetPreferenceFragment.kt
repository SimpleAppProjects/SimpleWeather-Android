package com.thewizrd.simpleweather.widgets

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.*
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.core.location.LocationManagerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePaddingRelative
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.findNavController
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.bumptech.glide.load.DecodeFormat
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.controls.ComboBoxItem
import com.thewizrd.shared_resources.controls.LocationQueryViewModel
import com.thewizrd.shared_resources.controls.WeatherNowViewModel
import com.thewizrd.shared_resources.helpers.*
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.location.LocationProvider
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.tzdb.TZDBCache
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.utils.ContextUtils.isNightMode
import com.thewizrd.shared_resources.weatherdata.*
import com.thewizrd.shared_resources.weatherdata.model.*
import com.thewizrd.simpleweather.App
import com.thewizrd.simpleweather.GlideApp
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.databinding.FragmentWidgetSetupBinding
import com.thewizrd.simpleweather.preferences.ArrayListPreference
import com.thewizrd.simpleweather.preferences.ToolbarPreferenceFragmentCompat
import com.thewizrd.simpleweather.services.WidgetWorker
import com.thewizrd.simpleweather.setup.SetupActivity
import com.thewizrd.simpleweather.snackbar.Snackbar
import com.thewizrd.simpleweather.widgets.AppChoiceDialogBuilder.OnAppSelectedListener
import com.thewizrd.simpleweather.widgets.WidgetUtils.WidgetBackground
import com.thewizrd.simpleweather.widgets.WidgetUtils.WidgetBackgroundStyle
import kotlinx.coroutines.*
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*
import kotlin.coroutines.coroutineContext
import com.google.android.material.snackbar.Snackbar as materialSnackbar

class WeatherWidgetPreferenceFragment : ToolbarPreferenceFragmentCompat() {
    // Widget id for ConfigurationActivity
    private var mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var mWidgetType = WidgetType.Unknown
    private lateinit var mWidgetInfo: WidgetProviderInfo
    private lateinit var mWidgetOptions: Bundle
    private lateinit var mWidgetViewCtx: Context
    private var resultValue: Intent? = null

    private lateinit var args: WeatherWidgetPreferenceFragmentArgs

    // Location Search
    private lateinit var favorites: MutableCollection<LocationData>
    private var query_vm: LocationQueryViewModel? = null

    private lateinit var locationProvider: LocationProvider
    private lateinit var locationCallback: LocationProvider.Callback

    /**
     * Tracks the status of the location updates request.
     */
    private var mRequestingLocationUpdates = false

    private var job: Job? = null

    // Weather
    private val wm = WeatherManager.instance

    // Views
    private lateinit var binding: FragmentWidgetSetupBinding
    private var mockLocData: LocationData? = null
    private var mockWeatherModel: WeatherNowViewModel? = null
    private var mockWeatherData: Weather? = null

    private var mLastSelectedValue: CharSequence? = null

    private lateinit var locationPref: ArrayListPreference
    private lateinit var hideLocNamePref: SwitchPreference
    private lateinit var hideSettingsBtnPref: SwitchPreference

    private lateinit var useTimeZonePref: SwitchPreference
    private lateinit var clockPref: Preference
    private lateinit var calPref: Preference

    private lateinit var bgChoicePref: ListPreference
    private lateinit var bgColorPref: ColorPreference
    private lateinit var txtColorPref: ColorPreference
    private lateinit var bgStylePref: ListPreference

    private lateinit var fcastOptPref: ListPreference
    private lateinit var tap2switchPref: SwitchPreference
    private lateinit var graphTypePref: ListPreference

    companion object {
        private val MAX_LOCATIONS = App.instance.settingsManager.getMaxLocations()
        private const val PERMISSION_LOCATION_REQUEST_CODE = 0
        private const val PERMISSION_BGLOCATION_REQUEST_CODE = 1
        private const val SETUP_REQUEST_CODE = 10

        // Preference Keys
        private const val KEY_CATGENERAL = "key_catgeneral"
        private const val KEY_LOCATION = "key_location"
        private const val KEY_HIDELOCNAME = "key_hidelocname"
        private const val KEY_HIDESETTINGSBTN = "key_hidesettingsbtn"

        private const val KEY_CATCLOCKDATE = "key_catclockdate"
        private const val KEY_USETIMEZONE = "key_usetimezone"
        private const val KEY_CLOCKAPP = "key_clockapp"
        private const val KEY_CALENDARAPP = "key_calendarapp"

        private const val KEY_BACKGROUND = "key_background"
        private const val KEY_BGCOLOR = "key_bgcolor"
        private const val KEY_BGCOLORCODE = "key_bgcolorcode"
        private const val KEY_TXTCOLORCODE = "key_txtcolorcode"
        private const val KEY_BGSTYLE = "key_bgstyle"

        private const val KEY_FORECAST = "key_forecast"
        private const val KEY_FORECASTOPTION = "key_fcastoption"
        private const val KEY_TAP2SWITCH = "key_tap2switch"
        private const val KEY_GRAPHTYPEOPTION = "key_graphtypeoption"

        fun newInstance(args: Bundle): WeatherWidgetPreferenceFragment {
            val fragment = WeatherWidgetPreferenceFragment()
            fragment.arguments = args
            return fragment
        }
    }

    init {
        arguments = Bundle()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsLogger.logEvent("WidgetConfig: onResume")
    }

    override fun onPause() {
        AnalyticsLogger.logEvent("WidgetConfig: onPause")
        super.onPause()
    }

    override fun onDestroyView() {
        job?.cancel()
        super.onDestroyView()
    }

    override fun getTitle(): Int {
        return R.string.widget_configure_prompt
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        /*
         * This should be before the super call,
         * so this is setup before onCreatePreferences is called
         */
        args = WeatherWidgetPreferenceFragmentArgs.fromBundle(requireArguments())

        // Find the widget id from the intent.
        mAppWidgetId = args.appWidgetId
        mWidgetType = WidgetUtils.getWidgetTypeFromID(mAppWidgetId)
        mWidgetInfo = WidgetUtils.getWidgetProviderInfoFromType(mWidgetType)!!
        mWidgetOptions = WidgetUtils.getPreviewAppWidgetOptions(requireContext(), mAppWidgetId)

        // Set the result value for WidgetConfigActivity
        resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)

        super.onCreate(savedInstanceState)

        mWidgetViewCtx =
            requireContext().applicationContext.getThemeContextOverride(!requireContext().isNightMode())

        lifecycleScope.launchWhenCreated {
            if (!settingsManager.isWeatherLoaded() && isActive) {
                Toast.makeText(
                    appCompatActivity,
                    R.string.prompt_setup_app_first,
                    Toast.LENGTH_SHORT
                ).show()

                val intent = Intent(appCompatActivity, SetupActivity::class.java)
                    .setAction(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)
                startActivityForResult(intent, SETUP_REQUEST_CODE)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = super.onCreateView(inflater, container, savedInstanceState) as ViewGroup
        val inflatedView = root.getChildAt(root.childCount - 1)
        root.removeView(inflatedView)
        binding = FragmentWidgetSetupBinding.inflate(inflater, root, true)

        val layoutIdx = binding.layoutContainer.indexOfChild(binding.widgetContainer)
        binding.layoutContainer.addView(inflatedView, layoutIdx + 1)

        // For landscape orientation
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            v.updatePaddingRelative(
                start = insets.systemWindowInsetLeft,
                end = insets.systemWindowInsetRight,
                bottom = insets.systemWindowInsetBottom
            )
            insets
        }

        listView?.isNestedScrollingEnabled = false

        appCompatActivity.setSupportActionBar(toolbar)
        appCompatActivity.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.widgetCompleteBtn.setOnClickListener {
            prepareWidget()
        }

        binding.bgLocationRationaleText.text = appCompatActivity.getBackgroundLocationRationale()

        binding.bgLocationSettingsBtn.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (!appCompatActivity.backgroundLocationPermissionEnabled()) {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                        appCompatActivity.openAppSettingsActivity()
                    } else {
                        requestBackgroundLocationPermission(PERMISSION_BGLOCATION_REQUEST_CODE)
                    }
                }
            }
        }

        // Location Listener
        locationProvider = LocationProvider(appCompatActivity)
        locationCallback = object : LocationProvider.Callback {
            override fun onLocationChanged(location: Location?) {
                stopLocationUpdates()

                if (location != null) {
                    Timber.tag("WidgetPrefFrag").i("Location update received...")
                    prepareWidget()
                } else {
                    Timber.tag("WidgetPrefFrag").i("Location update unavailable...")

                    runWithView {
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
            }
        }
        mRequestingLocationUpdates = false

        return root
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

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_widgetconfig, null)

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

            if (!args.simpleWeatherDroidExtraLOCATIONQUERY.isNullOrBlank()) {
                val locName = args.simpleWeatherDroidExtraLOCATIONNAME
                val locQuery = args.simpleWeatherDroidExtraLOCATIONQUERY

                if (locName != null) {
                    mLastSelectedValue = locQuery
                    locationPref.value = mLastSelectedValue.toString()
                } else {
                    locationPref.setValueIndex(0)
                }
            } else {
                locationPref.setValueIndex(0)
            }

            viewLifecycleOwnerLiveData.observe(
                this@WeatherWidgetPreferenceFragment,
                object : Observer<LifecycleOwner> {
                    override fun onChanged(t: LifecycleOwner?) {
                        viewLifecycleOwnerLiveData.removeObserver(this)
                        if (locationPref.value == Constants.KEY_GPS && !appCompatActivity.backgroundLocationPermissionEnabled()) {
                            binding.bgLocationLayout.visibility = View.VISIBLE
                        }
                        updateLocationView()
                    }
                })
        }

        locationPref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference, newValue ->
                job?.cancel()

                val selectedValue = newValue as CharSequence
                if (Constants.KEY_SEARCH.contentEquals(selectedValue)) {
                    // Setup search UI
                    view?.findNavController()
                        ?.navigate(WeatherWidgetPreferenceFragmentDirections.actionWeatherWidgetPreferenceFragmentToLocationSearchFragment2())
                    query_vm = null
                    binding.bgLocationLayout.visibility = View.GONE
                    return@OnPreferenceChangeListener false
                } else if (Constants.KEY_GPS.contentEquals(selectedValue)) {
                    mLastSelectedValue = null
                    query_vm = null
                    if (!appCompatActivity.backgroundLocationPermissionEnabled()) {
                        binding.bgLocationLayout.visibility = View.VISIBLE
                    }
                } else {
                    mLastSelectedValue = selectedValue
                    binding.bgLocationLayout.visibility = View.GONE
                }

                updateLocationView()
                true
        }

        hideLocNamePref = findPreference(KEY_HIDELOCNAME)!!
        hideSettingsBtnPref = findPreference(KEY_HIDESETTINGSBTN)!!

        hideLocNamePref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            WidgetUtils.setLocationNameHidden(mAppWidgetId, newValue as Boolean)
            hideLocNamePref.isChecked = newValue
            updateWidgetView()
            true
        }

        hideSettingsBtnPref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            WidgetUtils.setSettingsButtonHidden(mAppWidgetId, newValue as Boolean)
            hideSettingsBtnPref.isChecked = newValue
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

        hideSettingsBtnPref.isChecked = WidgetUtils.isSettingsButtonHidden(mAppWidgetId)

        if (!WidgetUtils.isSettingsButtonOptional(mWidgetType)) {
            hideSettingsBtnPref.isVisible = false
        }

        // Time and Date
        clockPref = findPreference(KEY_CLOCKAPP)!!
        calPref = findPreference(KEY_CALENDARAPP)!!
        useTimeZonePref = findPreference(KEY_USETIMEZONE)!!

        clockPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            AppChoiceDialogBuilder(requireContext())
                .setOnItemSelectedListener(object : OnAppSelectedListener {
                    override fun onItemSelected(key: String?) {
                            WidgetUtils.setOnClickClockApp(key)
                            updateClockPreference()
                        }
                    }).show()
            true
        }
        calPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            AppChoiceDialogBuilder(requireContext())
                    .setOnItemSelectedListener(object : OnAppSelectedListener {
                        override fun onItemSelected(key: String?) {
                            WidgetUtils.setOnClickCalendarApp(key)
                            updateCalPreference()
                        }
                    }).show()
            true
        }
        useTimeZonePref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            WidgetUtils.setUseTimeZone(mAppWidgetId, newValue as Boolean)
            true
        }

        if (WidgetUtils.isClockWidget(mWidgetType)) {
            updateClockPreference()
            clockPref.isVisible = true
        } else {
            clockPref.isVisible = false
        }

        if (WidgetUtils.isDateWidget(mWidgetType)) {
            updateCalPreference()
            calPref.isVisible = true
        } else {
            calPref.isVisible = false
        }

        if (WidgetUtils.isClockWidget(mWidgetType) || WidgetUtils.isDateWidget(mWidgetType)) {
            useTimeZonePref.isChecked = WidgetUtils.useTimeZone(mAppWidgetId)
            findPreference<Preference>(KEY_CATCLOCKDATE)!!.isVisible = true
        } else {
            findPreference<Preference>(KEY_CATCLOCKDATE)!!.isVisible = false
        }

        // Widget background style
        bgChoicePref = findPreference(KEY_BGCOLOR)!!
        bgStylePref = findPreference(KEY_BGSTYLE)!!
        bgColorPref = findPreference(KEY_BGCOLORCODE)!!
        txtColorPref = findPreference(KEY_TXTCOLORCODE)!!

        bgChoicePref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            val value = newValue.toString().toInt()

            val mWidgetBackground = WidgetBackground.valueOf(value)
            WidgetUtils.setWidgetBackground(mAppWidgetId, value)

            updateWidgetView()

            bgColorPref.isVisible = mWidgetBackground == WidgetBackground.CUSTOM
            txtColorPref.isVisible = mWidgetBackground == WidgetBackground.CUSTOM

            if (mWidgetBackground == WidgetBackground.CURRENT_CONDITIONS) {
                if (mWidgetType == WidgetType.Widget4x2 || mWidgetType == WidgetType.Widget2x2) {
                    bgStylePref.isVisible = true
                    return@OnPreferenceChangeListener true
                }
            }

            bgStylePref.setValueIndex(0)
            bgStylePref.callChangeListener(bgStylePref.value)
            bgStylePref.isVisible = false
            true
        }

        bgStylePref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            WidgetUtils.setBackgroundStyle(mAppWidgetId, newValue.toString().toInt())
            updateWidgetView()
            true
        }

        bgColorPref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            WidgetUtils.setBackgroundColor(mAppWidgetId, (newValue as Int))
            updateWidgetView()
            true
        }

        txtColorPref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            WidgetUtils.setTextColor(mAppWidgetId, (newValue as Int))
            updateWidgetView()
            true
        }

        val styles = WidgetBackgroundStyle.values()
        val styleEntries = arrayOfNulls<CharSequence>(styles.size)
        val styleEntryValues = arrayOfNulls<CharSequence>(styles.size)

        for (i in styles.indices) {
            when (val style = styles[i]) {
                WidgetBackgroundStyle.PANDA -> {
                    styleEntries[i] = requireContext().getString(R.string.label_style_panda)
                    styleEntryValues[i] = style.value.toString()
                    bgStylePref.setDefaultValue(styleEntryValues[i])
                }
                WidgetBackgroundStyle.DARK -> {
                    styleEntries[i] = requireContext().getText(R.string.label_style_dark)
                    styleEntryValues[i] = style.value.toString()
                }
                WidgetBackgroundStyle.LIGHT -> {
                    styleEntries[i] = requireContext().getText(R.string.label_style_light)
                    styleEntryValues[i] = style.value.toString()
                }
            }
        }
        bgStylePref.entries = styleEntries
        bgStylePref.entryValues = styleEntryValues

        val mWidgetBackground = WidgetUtils.getWidgetBackground(mAppWidgetId)
        val mWidgetBGStyle = WidgetUtils.getBackgroundStyle(mAppWidgetId)
        @ColorInt val mWidgetBackgroundColor = WidgetUtils.getBackgroundColor(mAppWidgetId)
        @ColorInt val mWidgetTextColor = WidgetUtils.getTextColor(mAppWidgetId)

        if (WidgetUtils.isBackgroundOptionalWidget(mWidgetType)) {
            bgChoicePref.setValueIndex(mWidgetBackground.value)
            bgChoicePref.callChangeListener(bgChoicePref.value)

            bgStylePref.setValueIndex(listOf(*WidgetBackgroundStyle.values()).indexOf(mWidgetBGStyle))
            bgStylePref.callChangeListener(bgStylePref.value)

            bgColorPref.color = mWidgetBackgroundColor
            bgColorPref.callChangeListener(bgColorPref.color)
            txtColorPref.color = mWidgetTextColor
            txtColorPref.callChangeListener(txtColorPref.color)

            findPreference<Preference>(KEY_BACKGROUND)!!.isVisible = true
            if (WidgetUtils.isBackgroundCustomOnlyWidget(mWidgetType)) {
                bgChoicePref.isVisible = false
                bgStylePref.isVisible = false
            }
        } else {
            bgChoicePref.setValueIndex(WidgetBackground.TRANSPARENT.value)
            findPreference<Preference>(KEY_BACKGROUND)!!.isVisible = false
        }

        // Forecast Preferences
        fcastOptPref = findPreference(KEY_FORECASTOPTION)!!
        fcastOptPref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            val fcastOptValue = newValue.toString().toInt()
            WidgetUtils.setForecastOption(mAppWidgetId, fcastOptValue)
            updateWidgetView()

            tap2switchPref.isVisible = (fcastOptValue == WidgetUtils.ForecastOption.FULL.value)

            true
        }

        tap2switchPref = findPreference(KEY_TAP2SWITCH)!!
        tap2switchPref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                WidgetUtils.setTap2Switch(mAppWidgetId, newValue as Boolean)
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
                    WidgetUtils.setWidgetGraphType(mAppWidgetId, newValue.toString().toInt())
                    updateWidgetView()
                    true
                }
            graphTypePref.isVisible = true

            val graphType = WidgetUtils.getWidgetGraphType(mAppWidgetId)
            graphTypePref.setValueIndex(graphType.value)
            graphTypePref.callChangeListener(graphTypePref.value)
        } else {
            fcastOptPref.setValueIndex(WidgetUtils.ForecastOption.FULL.value)
            findPreference<Preference>(KEY_FORECAST)!!.isVisible = false
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is ColorPreference) {
            val f = ColorPreferenceDialogFragment.newInstance(preference.getKey())
            f.setTargetFragment(this, 0)
            f.show(parentFragmentManager, ColorPreferenceDialogFragment::class.java.name)
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        args = WeatherWidgetPreferenceFragmentArgs.fromBundle(requireArguments())

        val savedStateHandle = view.findNavController().currentBackStackEntry?.savedStateHandle
        savedStateHandle?.getLiveData<String>(Constants.KEY_DATA)
            ?.observe(viewLifecycleOwner) { result ->
                // Do something with the result.
                if (result != null) {
                    // Save data
                    viewLifecycleOwner.lifecycleScope.launch {
                        val data = withContext(Dispatchers.IO) {
                            JSONParser.deserializer(result, LocationData::class.java)
                        }

                        if (data != null) {
                            query_vm = LocationQueryViewModel(data)
                            val item =
                                ComboBoxItem(query_vm!!.locationName, query_vm!!.locationQuery)
                            val idx = locationPref.entryCount - 1

                            locationPref.insertEntry(idx, item.display, item.value)
                            locationPref.setValueIndex(idx)

                            if (locationPref.entryCount > MAX_LOCATIONS) {
                                locationPref.removeEntry(locationPref.entryCount - 1)
                            }

                            locationPref.callChangeListener(item.value)

                                savedStateHandle.remove(Constants.KEY_DATA)
                            } else {
                                query_vm = null
                            }
                        }
                    }
            }

        initializeWidget()

        // Resize necessary views
        binding.root.viewTreeObserver.addOnPreDrawListener(object :
            ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                binding.root.viewTreeObserver.removeOnPreDrawListener(this)
                runWithView { resizeWidgetContainer() }
                return true
            }
        })

        listView.clearOnScrollListeners()
        appBarLayout.liftOnScrollTargetViewId = binding.scrollView.id
    }

    private fun updateClockPreference() {
        val componentName = WidgetUtils.getClockAppComponent(appCompatActivity)
        if (componentName != null) {
            try {
                val appInfo = requireContext().packageManager.getApplicationInfo(componentName.packageName, 0)
                val appLabel = requireContext().packageManager.getApplicationLabel(appInfo)
                clockPref.summary = appLabel
                return
            } catch (e: PackageManager.NameNotFoundException) {
                // App not available
                WidgetUtils.setOnClickClockApp(null)
            }
        }

        clockPref.setSummary(R.string.summary_default)
    }

    private fun updateCalPreference() {
        val componentName = WidgetUtils.getCalendarAppComponent(appCompatActivity)
        if (componentName != null) {
            try {
                val appInfo = requireContext().packageManager.getApplicationInfo(componentName.packageName, 0)
                val appLabel = requireContext().packageManager.getApplicationLabel(appInfo)
                calPref.summary = appLabel
                return
            } catch (e: PackageManager.NameNotFoundException) {
                // App not available
                WidgetUtils.setOnClickClockApp(null)
            }
        }

        calPref.setSummary(R.string.summary_default)
    }

    private fun initializeWidget() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.widgetContainer.removeAllViews()

            val widgetType = WidgetUtils.getWidgetTypeFromID(mAppWidgetId)
            val info = WidgetUtils.getWidgetProviderInfoFromType(widgetType) ?: return@launch

            val views = withContext(Dispatchers.Default) {
                buildMockData()

                val view = WidgetUpdaterHelper.buildUpdate(
                    requireContext(), info, mAppWidgetId,
                    mockLocData!!, mockWeatherModel!!, mWidgetOptions, false
                )
                WidgetUpdaterHelper.buildExtras(
                    requireContext(), mWidgetInfo,
                    mockLocData!!, mockWeatherData!!, view, mAppWidgetId, mWidgetOptions
                )
                view
            }

            val widgetView = views.apply(mWidgetViewCtx, binding.widgetContainer)
            binding.widgetContainer.addView(widgetView)
            widgetView.updateLayoutParams<FrameLayout.LayoutParams> {
                height = mWidgetViewCtx.dpToPx(
                    96 * when (mWidgetType) {
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
                        WidgetType.Widget4x4MaterialYou -> 4
                        WidgetType.Widget4x3Locations -> 3
                        WidgetType.Widget3x1MaterialYou -> 1
                        WidgetType.Widget4x2Graph -> 2
                    }.toFloat()
                ).toInt()
                width = mWidgetViewCtx.dpToPx(
                    96 * when (mWidgetType) {
                        WidgetType.Unknown -> 4
                        WidgetType.Widget1x1 -> 1
                        WidgetType.Widget2x2 -> 2
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
                    }.toFloat()
                ).toInt()
                gravity = Gravity.CENTER
            }

            updateLocationView()
            updateBackground()
        }
    }

    private fun updateBackground() {
        if (WidgetBackground.valueOf(bgChoicePref.value.toInt()) == WidgetBackground.CURRENT_CONDITIONS) {
            val imageView = binding.widgetContainer.findViewById<ImageView>(R.id.widgetBackground)
            if (imageView != null) {
                GlideApp.with(this)
                        .load("file:///android_asset/backgrounds/day.jpg")
                        .format(DecodeFormat.PREFER_RGB_565)
                        .centerCrop()
                        .transform(TransparentOverlay(0x33))
                        .thumbnail(0.75f)
                        .into(imageView)
            }
        }
    }

    private fun updateLocationView() {
        val locationView = binding.widgetContainer.findViewById<TextView>(R.id.location_name)
        if (locationView != null) {
            locationView.text =
                mLastSelectedValue?.let { locationPref.findEntryFromValue(it)?.toString() }
                    ?: this.getString(R.string.pref_location)
            locationView.visibility = if (hideLocNamePref.isChecked) View.GONE else View.VISIBLE
        }
    }

    private fun updateWidgetView() {
        runWithView {
            if (binding.widgetContainer.childCount > 0) {
                val views = withContext(Dispatchers.Default) {
                    buildMockData()

                    val view = WidgetUpdaterHelper.buildUpdate(requireContext(), mWidgetInfo, mAppWidgetId,
                            mockLocData!!, mockWeatherModel!!, mWidgetOptions, false)
                    WidgetUpdaterHelper.buildExtras(requireContext(), mWidgetInfo,
                            mockLocData!!, mockWeatherData!!, view, mAppWidgetId, mWidgetOptions)
                    view
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
        if (query_vm == null && mLastSelectedValue != null)
            locationPref.value = mLastSelectedValue.toString()

        super.onSaveInstanceState(outState)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Resize necessary views
        val observer = binding.root.viewTreeObserver
        observer.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.root.viewTreeObserver.removeOnGlobalLayoutListener(this)
                runWithView { resizeWidgetContainer() }
            }
        })

        val currentNightMode: Int = newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK
        mWidgetViewCtx =
            requireContext().applicationContext.getThemeContextOverride(currentNightMode != Configuration.UI_MODE_NIGHT_YES)

        updateWidgetView()
    }

    private fun resizeWidgetContainer() {
        val widgetView = binding.widgetContainer.findViewById<View>(R.id.widget)
        val screenWidth = binding.scrollView.measuredWidth

        TransitionManager.beginDelayedTransition(binding.scrollView, AutoTransition())

        if (widgetView != null) {
            val widgetParams = widgetView.layoutParams as FrameLayout.LayoutParams
            if (widgetView.measuredWidth > screenWidth) {
                widgetParams.width = screenWidth
            } else {
                widgetParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
            }
            widgetView.layoutParams = widgetParams
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SETUP_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                // Get result data
                val dataJson = data?.getStringExtra(Constants.KEY_DATA)

                if (dataJson?.isNotBlank() == true) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val locData = withContext(Dispatchers.IO) {
                            JSONParser.deserializer(dataJson, LocationData::class.java)
                        }

                        if (locData.locationType == LocationType.SEARCH) {
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
            } else {
                // Setup was cancelled. Cancel widget setup
                appCompatActivity.setResult(Activity.RESULT_CANCELED, resultValue)
                appCompatActivity.finish()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                appCompatActivity.setResult(Activity.RESULT_CANCELED, resultValue)
                appCompatActivity.finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun prepareWidget() {
        lifecycleScope.launch {
            // Get location data
            if (locationPref.value != null) {
                val locationItemValue = locationPref.value

                if (Constants.KEY_GPS == locationItemValue) {
                    job?.cancel()

                    supervisorScope {
                        // Check location
                        val task = async(Dispatchers.Default) {
                            // Changing location to GPS
                            if (!appCompatActivity.locationPermissionEnabled()) {
                                this@WeatherWidgetPreferenceFragment.requestLocationPermission(
                                    PERMISSION_LOCATION_REQUEST_CODE
                                )
                                return@async false
                            }

                            if (settingsManager.useFollowGPS() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !settingsManager.requestedBGAccess() &&
                                !appCompatActivity.backgroundLocationPermissionEnabled()
                            ) {
                                val snackbar = Snackbar.make(
                                    appCompatActivity.getBackgroundLocationRationale(),
                                    Snackbar.Duration.VERY_LONG
                                )
                                snackbar.setAction(android.R.string.ok) {
                                    if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                                        appCompatActivity.openAppSettingsActivity()
                                    } else {
                                        requestBackgroundLocationPermission(
                                            PERMISSION_BGLOCATION_REQUEST_CODE
                                        )
                                    }
                                }
                                showSnackbar(snackbar, object : materialSnackbar.Callback() {
                                    override fun onDismissed(
                                        transientBottomBar: materialSnackbar?,
                                        event: Int
                                    ) {
                                        super.onDismissed(transientBottomBar, event)
                                        if (event != BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_ACTION) {
                                            prepareWidget()
                                        }
                                    }
                                })
                                settingsManager.setRequestBGAccess(true)
                                return@async false
                            }

                            val locMan =
                                appCompatActivity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
                            if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
                                throw CustomException(R.string.error_enable_location_services)
                            }

                            val lastGPSLocData = settingsManager.getLastGPSLocData()

                            // Check if last location exists
                            if ((lastGPSLocData == null || !lastGPSLocData.isValid) && !updateLocation()) {
                                throw CustomException(R.string.error_retrieve_location)
                            }

                            true
                        }.also {
                            job = it
                        }

                        task.invokeOnCompletion {
                            val t = task.getCompletionExceptionOrNull()
                            if (t == null) {
                                val success = task.getCompleted()
                                if (success) {
                                    lifecycleScope.launch {
                                        settingsManager.setFollowGPS(true)

                                        // Save data for widget
                                        WidgetUtils.deleteWidget(mAppWidgetId)
                                        WidgetUtils.saveLocationData(mAppWidgetId, null)
                                        WidgetUtils.addWidgetId(Constants.KEY_GPS, mAppWidgetId)
                                        finalizeWidgetUpdate()
                                    }
                                }
                            } else {
                                if (t is WeatherException || t is CustomException) {
                                    showSnackbar(Snackbar.make(t.message, Snackbar.Duration.SHORT), null)
                                } else {
                                    showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT), null)
                                }
                            }
                        }
                    }
                } else {
                    supervisorScope {
                        val task = async(Dispatchers.Default) {
                            var locData: LocationData? = null

                            // Widget ID exists in prefs
                            if (WidgetUtils.exists(mAppWidgetId)) {
                                locData = WidgetUtils.getLocationData(mAppWidgetId)
                            }

                            // Changing location to whatever
                            if (locData == null || locationItemValue != locData.query) {
                                // Get location data
                                val itemValue = locationPref.value
                                locData = favorites.firstOrNull { input -> input.query == itemValue }

                                if (locData == null && query_vm != null) {
                                    locData = LocationData(query_vm!!)

                                    if (!locData.isValid) {
                                        return@async null
                                    }

                                    // Add location to favs
                                    withContext(Dispatchers.IO) {
                                        settingsManager.addLocation(locData)
                                    }
                                }
                            }
                            locData
                        }.also {
                            job = it
                        }

                        task.invokeOnCompletion {
                            val t = task.getCompletionExceptionOrNull()
                            if (t == null) {
                                val locationData = task.getCompleted()
                                lifecycleScope.launch {
                                    if (locationData != null) {
                                        // Save data for widget
                                        WidgetUtils.deleteWidget(mAppWidgetId)
                                        WidgetUtils.saveLocationData(mAppWidgetId, locationData)
                                        WidgetUtils.addWidgetId(locationData.query, mAppWidgetId)
                                        finalizeWidgetUpdate()
                                    } else {
                                        appCompatActivity.setResult(Activity.RESULT_CANCELED, resultValue)
                                        appCompatActivity.finish()
                                    }
                                }
                            } else {
                                showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT), null)
                            }
                        }
                    }
                }
            } else {
                appCompatActivity.setResult(Activity.RESULT_CANCELED, resultValue)
                appCompatActivity.finish()
            }
        }
    }

    private fun finalizeWidgetUpdate() {
        // Save widget preferences
        WidgetUtils.setWidgetBackground(mAppWidgetId, bgChoicePref.value.toInt())
        WidgetUtils.setBackgroundColor(mAppWidgetId, bgColorPref.color)
        WidgetUtils.setTextColor(mAppWidgetId, txtColorPref.color)
        WidgetUtils.setBackgroundStyle(mAppWidgetId, bgStylePref.value.toInt())
        WidgetUtils.setLocationNameHidden(mAppWidgetId, hideLocNamePref.isChecked)
        WidgetUtils.setSettingsButtonHidden(mAppWidgetId, hideSettingsBtnPref.isChecked)
        WidgetUtils.setForecastOption(mAppWidgetId, fcastOptPref.value.toInt())
        WidgetUtils.setTap2Switch(mAppWidgetId, tap2switchPref.isChecked)
        WidgetUtils.setUseTimeZone(mAppWidgetId, useTimeZonePref.isChecked)
        if (mWidgetType == WidgetType.Widget4x2Graph) {
            WidgetUtils.setWidgetGraphType(mAppWidgetId, graphTypePref.value.toInt())
        }

        // Trigger widget service to update widget
        WidgetWorker.enqueueRefreshWidget(
            appCompatActivity,
            intArrayOf(mAppWidgetId),
            mWidgetInfo
        )

        // Create return intent
        appCompatActivity.setResult(Activity.RESULT_OK, resultValue)
        appCompatActivity.finish()
    }

    @SuppressLint("MissingPermission")
    @Throws(CustomException::class)
    private suspend fun updateLocation(): Boolean {
        var locationChanged = false

        if (appCompatActivity != null && !appCompatActivity.locationPermissionEnabled()) {
            return false
        }

        val locMan = appCompatActivity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager?

        if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
            throw CustomException(R.string.error_enable_location_services)
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
            locationProvider.requestSingleUpdate(locationCallback, Looper.getMainLooper(), 30000)
        }

        if (location != null && coroutineContext.isActive) {
            var query_vm: LocationQueryViewModel? = null

            try {
                query_vm = withContext(Dispatchers.IO) {
                    wm.getLocation(location)
                }
            } catch (e: WeatherException) {
                throw e
            }

            if (query_vm == null || query_vm.locationQuery.isNullOrBlank()) {
                // Stop since there is no valid query
                return false
            } else if (query_vm.locationTZLong.isNullOrBlank() && query_vm.locationLat != 0.0 && query_vm.locationLong != 0.0) {
                val tzId = TZDBCache.getTimeZone(query_vm.locationLat, query_vm.locationLong)

                if ("unknown" != tzId)
                    query_vm.locationTZLong = tzId
            }

            if (!coroutineContext.isActive) return false

            // Save location as last known
            settingsManager.saveLastGPSLocData(LocationData(query_vm, location))

            LocalBroadcastManager.getInstance(appCompatActivity!!)
                    .sendBroadcast(Intent(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE))

            locationChanged = true
        }

        return locationChanged
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_LOCATION_REQUEST_CODE ->
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    // Do the task you need to do.
                    prepareWidget()
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    showSnackbar(
                        Snackbar.make(
                            R.string.error_location_denied,
                            Snackbar.Duration.SHORT
                        ), null
                    )
                }
            PERMISSION_BGLOCATION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    binding.bgLocationLayout.visibility = View.GONE
                } else {
                    showSnackbar(
                        Snackbar.make(
                            R.string.error_location_denied,
                            Snackbar.Duration.SHORT
                        ), null
                    )
                }
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun buildMockData() {
        if (mockLocData == null) {
            mockLocData = LocationData().apply {
                name = mLastSelectedValue?.let { locationPref.findEntryFromValue(it)?.toString() }
                    ?: getString(R.string.pref_location)
                query = locationPref.value
                latitude = 0.toDouble()
                longitude = 0.toDouble()
                tzLong = "UTC"
                locationType =
                    if (locationPref.value == Constants.KEY_GPS) LocationType.GPS else LocationType.SEARCH
                weatherSource = wm.getWeatherAPI()
                locationSource = wm.getLocationProvider().getLocationAPI()
            }
        } else {
            mockLocData?.name =
                mLastSelectedValue?.let { locationPref.findEntryFromValue(it)?.toString() }
                    ?: getString(R.string.pref_location)
            mockLocData?.query = locationPref.value
        }

        if (mockWeatherModel == null) {
            mockWeatherModel = WeatherNowViewModel(Weather().apply {
                location = Location().apply {
                    name = mockLocData?.name
                    tzLong = "UTC"
                }
                updateTime = ZonedDateTime.now()
                forecast = List(6) { index ->
                    Forecast().apply {
                        date = LocalDateTime.now().plusDays(index.toLong())
                        highF = 70f + index
                        highC = 23f + index / 2f
                        lowF = 60f - index
                        lowC = 17f - index / 2f
                        condition = getString(R.string.weather_sunny)
                        icon = WeatherIcons.DAY_SUNNY
                        extras = ForecastExtras().apply {
                            feelslikeF = 80f
                            feelslikeC = 26f
                            humidity = 50
                            dewpointF = 30f
                            dewpointC = -1f
                            uvIndex = 5f
                            pop = 35
                            cloudiness = 25
                            qpfRainIn = 0.05f
                            qpfRainMm = 1.27f
                            qpfSnowIn = 0f
                            qpfSnowCm = 0f
                            pressureIn = 30.05f
                            pressureMb = 1018f
                            windDegrees = 180
                            windMph = 4f
                            windKph = 6.43f
                            windGustKph = 9f
                            windGustKph = 14.5f
                            visibilityMi = 10f
                            visibilityKm = 16.1f
                        }
                    }
                }
                hrForecast = List(6) { index ->
                    HourlyForecast().apply {
                        date = ZonedDateTime.now().plusHours(index.toLong())
                        highF = 70f + index
                        highC = 23f + index / 2f
                        condition = getString(R.string.weather_sunny)
                        icon = WeatherIcons.DAY_SUNNY
                        windMph = 5f
                        windKph = 8f
                        extras = ForecastExtras().apply {
                            feelslikeF = 80f
                            feelslikeC = 26f
                            humidity = 50
                            dewpointF = 30f
                            dewpointC = -1f
                            uvIndex = 5f
                            pop = 35
                            cloudiness = 25
                            qpfRainIn = 0.05f
                            qpfRainMm = 1.27f
                            qpfSnowIn = 0f
                            qpfSnowCm = 0f
                            pressureIn = 30.05f
                            pressureMb = 1018f
                            windDegrees = 180
                            windMph = 4f
                            windKph = 6.43f
                            windGustKph = 9f
                            windGustKph = 14.5f
                            visibilityMi = 10f
                            visibilityKm = 16.1f
                        }
                    }
                }
                aqiForecast = List(6) {
                    AirQuality().apply {
                        date = LocalDate.now().plusDays(it.toLong())
                        index = 10 * (it)
                    }
                }
                condition = Condition().apply {
                    weather = getString(R.string.weather_sunny)
                    tempF = 70f
                    tempC = 21f
                    windMph = 5f
                    windKph = 8f
                    highF = 75f
                    highC = 23f
                    lowF = 60f
                    lowC = 15f
                    icon = WeatherIcons.DAY_SUNNY
                }
                atmosphere = Atmosphere()
                precipitation = Precipitation().apply {
                    pop = 15
                    cloudiness = 25
                    qpfRainIn = 0.05f
                    qpfRainMm = 1.27f
                    qpfSnowIn = 0f
                    qpfSnowCm = 0f
                }
                source = wm.getWeatherAPI()
                query = locationPref.value

                mockWeatherData = this
            })
        } else {
            mockWeatherModel?.location = mockLocData?.name
        }
    }
}