package com.thewizrd.simpleweather.widgets

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.appwidget.AppWidgetManager
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
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.view.MenuItemCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.findNavController
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.bumptech.glide.load.DecodeFormat
import com.google.android.gms.location.*
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.controls.ComboBoxItem
import com.thewizrd.shared_resources.controls.LocationQueryViewModel
import com.thewizrd.shared_resources.controls.WeatherNowViewModel
import com.thewizrd.shared_resources.helpers.ContextUtils
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.tzdb.TZDBCache
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.wearable.WearableHelper
import com.thewizrd.shared_resources.weatherdata.*
import com.thewizrd.simpleweather.GlideApp
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.databinding.FragmentWidgetSetupBinding
import com.thewizrd.simpleweather.preferences.ArrayListPreference
import com.thewizrd.simpleweather.preferences.ToolbarPreferenceFragmentCompat
import com.thewizrd.simpleweather.setup.SetupActivity
import com.thewizrd.simpleweather.snackbar.Snackbar
import com.thewizrd.simpleweather.widgets.AppChoiceDialogBuilder.OnAppSelectedListener
import com.thewizrd.simpleweather.widgets.WidgetUtils.WidgetBackground
import com.thewizrd.simpleweather.widgets.WidgetUtils.WidgetBackgroundStyle
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.lang.Runnable
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*
import kotlin.coroutines.coroutineContext

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

    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private var mLocCallback: LocationCallback? = null
    private var mLocListnr: LocationListener? = null

    /**
     * Tracks the status of the location updates request.
     */
    private var mRequestingLocationUpdates = false
    private val mMainHandler = Handler(Looper.getMainLooper())

    private var job: Job? = null

    // Weather
    private val wm = WeatherManager.getInstance()

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

    companion object {
        private val MAX_LOCATIONS = Settings.getMaxLocations()
        private const val PERMISSION_LOCATION_REQUEST_CODE = 0
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
        mWidgetOptions = AppWidgetManager.getInstance(requireContext()).getAppWidgetOptions(mAppWidgetId)

        // Set the result value for WidgetConfigActivity
        resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)

        super.onCreate(savedInstanceState)

        mWidgetViewCtx = ContextUtils.getThemeContextOverride(requireContext().applicationContext, !requireContext().isNightMode())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = super.onCreateView(inflater, container, savedInstanceState) as ViewGroup
        val inflatedView = root.getChildAt(root.childCount - 1)
        root.removeView(inflatedView)
        binding = FragmentWidgetSetupBinding.inflate(inflater, root, true)
        binding.layoutContainer.addView(inflatedView)

        // For landscape orientation
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val layoutParams = v.layoutParams as MarginLayoutParams
            layoutParams.setMargins(insets.systemWindowInsetLeft, 0, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
            insets
        }

        listView?.isNestedScrollingEnabled = false

        // Set fragment view
        setHasOptionsMenu(true)

        appCompatActivity.setSupportActionBar(toolbar)
        appCompatActivity.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val context = root.context
        val navIcon = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.ic_arrow_back_white_24dp)!!)
        DrawableCompat.setTint(navIcon, ContextCompat.getColor(context, R.color.invButtonColorText))
        appCompatActivity.supportActionBar?.setHomeAsUpIndicator(navIcon)

        // Location Listener
        if (WearableHelper.isGooglePlayServicesInstalled()) {
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(appCompatActivity)
            mLocCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    stopLocationUpdates()
                    mMainHandler.removeCallbacks(cancelLocRequestRunner)

                    Timber.tag("WidgetPrefFrag").i("Fused: Location update received...")

                    runWithView {
                        if (locationResult?.lastLocation != null) {
                            prepareWidget()
                        } else {
                            showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT), null)
                        }
                    }
                }

                override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                    if (!locationAvailability.isLocationAvailable) {
                        stopLocationUpdates()
                        mMainHandler.removeCallbacks(cancelLocRequestRunner)

                        Timber.tag("WidgetPrefFrag").i("Fused: Location update unavailable...")

                        runWithView {
                            showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT), null)
                        }
                    }
                }
            }
        } else {
            mLocListnr = object : LocationListener {
                override fun onLocationChanged(location: Location?) {
                    mMainHandler.removeCallbacks(cancelLocRequestRunner)
                    stopLocationUpdates()

                    Timber.tag("WidgetPrefFrag").i("LocMan: Location update received...")

                    runWithView {
                        if (location != null) {
                            prepareWidget()
                        } else {
                            showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT), null)
                        }
                    }
                }

                override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
        }

        mRequestingLocationUpdates = false

        if (!Settings.isWeatherLoaded()) {
            Toast.makeText(appCompatActivity, R.string.prompt_setup_app_first, Toast.LENGTH_SHORT).show()

            val intent = Intent(appCompatActivity, SetupActivity::class.java)
                    .setAction(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)
            startActivityForResult(intent, SETUP_REQUEST_CODE)
        }

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

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_widgetconfig, null)

        locationPref = findPreference(KEY_LOCATION)!!
        locationPref.addEntry(R.string.pref_item_gpslocation, Constants.KEY_GPS)
        locationPref.addEntry(R.string.label_btn_add_location, Constants.KEY_SEARCH)

        val favs = Settings.getFavorites()
        favorites = ArrayList(favs ?: emptyList())
        for (location in favorites) {
            locationPref.insertEntry(locationPref.entryCount - 1,
                    location.name, location.query)
        }
        if (locationPref.entryCount > MAX_LOCATIONS)
            locationPref.removeEntry(locationPref.entryCount - 1)

        locationPref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            job?.cancel()

            val selectedValue = newValue as CharSequence
            if (Constants.KEY_SEARCH.contentEquals(selectedValue)) {
                // Setup search UI
                view?.findNavController()
                        ?.navigate(WeatherWidgetPreferenceFragmentDirections.actionWeatherWidgetPreferenceFragmentToLocationSearchFragment2())
                query_vm = null
                return@OnPreferenceChangeListener false
            } else if (Constants.KEY_GPS.contentEquals(selectedValue)) {
                mLastSelectedValue = null
                query_vm = null
            } else {
                mLastSelectedValue = selectedValue
            }

            updateLocationView()
            true
        }

        if (args.simpleWeatherDroidExtraLOCATIONQUERY?.isNotBlank() == true) {
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

        hideLocNamePref = findPreference(KEY_HIDELOCNAME)!!
        hideSettingsBtnPref = findPreference(KEY_HIDESETTINGSBTN)!!

        hideLocNamePref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            WidgetUtils.setLocationNameHidden(mAppWidgetId, newValue as Boolean)
            hideLocNamePref.isChecked = newValue
            updateLocationView()
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
                WidgetBackgroundStyle.FULLBACKGROUND -> {
                    styleEntries[i] = requireContext().getString(R.string.label_style_fullbg)
                    styleEntryValues[i] = style.value.toString()
                }
                WidgetBackgroundStyle.PANDA -> {
                    styleEntries[i] = requireContext().getString(R.string.label_style_panda)
                    styleEntryValues[i] = style.value.toString()
                    bgStylePref.setDefaultValue(styleEntryValues[i])
                }
                WidgetBackgroundStyle.PENDINGCOLOR -> {
                    styleEntries[i] = requireContext().getText(R.string.label_style_pendingcolor)
                    styleEntryValues[i] = style.value.toString()
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
        } else {
            bgChoicePref.setValueIndex(WidgetBackground.TRANSPARENT.value)
            findPreference<Preference>(KEY_BACKGROUND)!!.isVisible = false
        }

        // Forecast Preferences
        fcastOptPref = findPreference(KEY_FORECASTOPTION)!!
        fcastOptPref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            WidgetUtils.setForecastOption(mAppWidgetId, newValue.toString().toInt())
            updateWidgetView()
            true
        }

        if (WidgetUtils.isForecastWidget(mWidgetType)) {
            fcastOptPref.setValueIndex(WidgetUtils.getForecastOption(mAppWidgetId).value)
            fcastOptPref.callChangeListener(fcastOptPref.value)
            findPreference<Preference>(KEY_FORECAST)!!.isVisible = true
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
                ?.observe(viewLifecycleOwner, { result ->
                    // Do something with the result.
                    if (result != null) {
                        // Save data
                        viewLifecycleOwner.lifecycleScope.launch {
                            val data = withContext(Dispatchers.IO) {
                                JSONParser.deserializer(result, LocationData::class.java)
                            }

                            if (data != null) {
                                query_vm = LocationQueryViewModel(data)
                                val item = ComboBoxItem(query_vm!!.locationName, query_vm!!.locationQuery)
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
                })

        initializeWidget()

        // Resize necessary views
        binding.root.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                binding.root.viewTreeObserver.removeOnPreDrawListener(this)
                runWithView { resizeWidgetContainer() }
                return true
            }
        })
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

                val view = WidgetUpdaterHelper.buildUpdate(requireContext(), info, mAppWidgetId,
                        mockLocData!!, mockWeatherModel!!, mWidgetOptions, false)
                WidgetUpdaterHelper.buildExtras(requireContext(), mWidgetInfo,
                        mockLocData!!, mockWeatherData!!, view, mAppWidgetId, mWidgetOptions)
                view
            }

            val widgetView = views.apply(mWidgetViewCtx, binding.widgetContainer)
            widgetView.minimumWidth = ContextUtils.dpToPx(mWidgetViewCtx, 360f).toInt()
            binding.widgetContainer.addView(widgetView)

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
            locationView.text = if (mLastSelectedValue != null) locationPref.findEntryFromValue(mLastSelectedValue) else this.getString(R.string.pref_location)
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
        mWidgetViewCtx = ContextUtils.getThemeContextOverride(requireContext().applicationContext, currentNightMode != Configuration.UI_MODE_NIGHT_YES)

        updateWidgetView()
    }

    private fun resizeWidgetContainer() {
        val widgetView = binding.widgetContainer.findViewById<View>(R.id.widget)

        val screenWidth = binding.scrollView.measuredWidth

        val preferredHeight = ContextUtils.dpToPx(appCompatActivity, 225f).toInt()
        var minHeight = ContextUtils.dpToPx(appCompatActivity, 96f).toInt()
        val maxCellWidth = minHeight * 5

        if (mWidgetType == WidgetType.Widget2x2 || mWidgetType == WidgetType.Widget4x2) {
            minHeight *= 2
        }

        TransitionManager.beginDelayedTransition(binding.scrollView, AutoTransition())

        if (appCompatActivity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            binding.widgetContainer.minimumHeight = preferredHeight
        } else {
            if (mWidgetType == WidgetType.Widget1x1 || mWidgetType == WidgetType.Widget4x1Google) {
                minHeight = (minHeight * 1.5f).toInt()
            }
            binding.widgetContainer.minimumHeight = minHeight
        }

        if (widgetView != null) {
            val widgetParams = widgetView.layoutParams as FrameLayout.LayoutParams
            if (widgetView.measuredWidth > screenWidth) {
                widgetParams.width = screenWidth
            } else if (widgetView.measuredWidth > maxCellWidth) {
                widgetParams.width = maxCellWidth
            }
            widgetParams.gravity = Gravity.CENTER
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

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater)
        // Inflate the menu; this adds items to the action bar if it is present.
        menu.clear()
        menuInflater.inflate(R.menu.menu_widgetsetup, menu)

        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            MenuItemCompat.setIconTintList(item, ColorStateList.valueOf(ContextCompat.getColor(appCompatActivity, R.color.invButtonColorText)))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                appCompatActivity.setResult(Activity.RESULT_CANCELED, resultValue)
                appCompatActivity.finish()
                return true
            }
            R.id.action_done -> {
                prepareWidget()
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
                            if (ContextCompat.checkSelfPermission(appCompatActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                                    ContextCompat.checkSelfPermission(appCompatActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                                        PERMISSION_LOCATION_REQUEST_CODE)
                                return@async false
                            }

                            val locMan = appCompatActivity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
                            if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
                                throw CustomException(R.string.error_enable_location_services)
                            }

                            val lastGPSLocData = Settings.getLastGPSLocData()

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
                                        Settings.setFollowGPS(true)

                                        // Save data for widget
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
                                    locData = LocationData(query_vm)

                                    if (!locData.isValid) {
                                        return@async null
                                    }

                                    // Add location to favs
                                    withContext(Dispatchers.IO) {
                                        Settings.addLocation(locData)
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
        WidgetUtils.setUseTimeZone(mAppWidgetId, useTimeZonePref.isChecked)

        // Trigger widget service to update widget
        WeatherWidgetService.enqueueWork(appCompatActivity,
                Intent(appCompatActivity, WeatherWidgetService::class.java)
                        .setAction(WeatherWidgetService.ACTION_REFRESHWIDGET)
                        .putExtra(WeatherWidgetProvider.EXTRA_WIDGET_IDS, intArrayOf(mAppWidgetId))
                        .putExtra(WeatherWidgetProvider.EXTRA_WIDGET_TYPE, mWidgetType.value))

        // Create return intent
        appCompatActivity.setResult(Activity.RESULT_OK, resultValue)
        appCompatActivity.finish()
    }

    @SuppressLint("MissingPermission")
    @Throws(CustomException::class)
    private suspend fun updateLocation(): Boolean {
        var locationChanged = false

        if (ContextCompat.checkSelfPermission(appCompatActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(appCompatActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return false
        }

        var location: Location? = null

        val locMan = appCompatActivity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager?

        if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
            throw CustomException(R.string.error_enable_location_services)
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
                    showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.LONG), null)
                }
            }
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

            if (query_vm?.locationQuery.isNullOrBlank()) {
                // Stop since there is no valid query
                return false
            } else if (query_vm?.locationTZLong.isNullOrBlank() && query_vm.locationLat != 0.0 && query_vm.locationLong != 0.0) {
                val tzId = withContext(Dispatchers.IO) {
                    TZDBCache.getTimeZone(query_vm.locationLat, query_vm.locationLong)
                }
                if ("unknown" != tzId)
                    query_vm.locationTZLong = tzId
            }

            if (!coroutineContext.isActive) return false

            // Save location as last known
            Settings.saveLastGPSLocData(LocationData(query_vm, location))

            LocalBroadcastManager.getInstance(appCompatActivity!!)
                    .sendBroadcast(Intent(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE))

            locationChanged = true
        }

        return locationChanged
    }

    private val cancelLocRequestRunner = Runnable {
        stopLocationUpdates()
        showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT), null)
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
                    showSnackbar(Snackbar.make(R.string.error_location_denied, Snackbar.Duration.SHORT), null)
                }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun buildMockData() {
        if (mockLocData == null) {
            mockLocData = LocationData().apply {
                name = if (mLastSelectedValue != null) locationPref.findEntryFromValue(mLastSelectedValue).toString() else getString(R.string.pref_location)
                query = locationPref.value
                latitude = 0.toDouble()
                longitude = 0.toDouble()
                tzLong = "UTC"
                locationType = if (locationPref.value == Constants.KEY_GPS) LocationType.GPS else LocationType.SEARCH
                weatherSource = wm.weatherAPI
                locationSource = wm.locationProvider.locationAPI
            }
        } else {
            mockLocData?.name = if (mLastSelectedValue != null) locationPref.findEntryFromValue(mLastSelectedValue).toString() else getString(R.string.pref_location)
            mockLocData?.query = locationPref.value
        }

        if (mockWeatherModel == null) {
            mockWeatherModel = WeatherNowViewModel(Weather().apply {
                location = Location().apply {
                    name = mockLocData?.name
                    tzLong = "UTC"
                }
                updateTime = ZonedDateTime.now()
                forecast = List(5) { index ->
                    Forecast().apply {
                        date = LocalDateTime.now().plusDays(index.toLong())
                        highF = 75f
                        highC = 23f
                        lowF = 60f
                        lowC = 15f
                        condition = getString(R.string.weather_sunny)
                        icon = WeatherIcons.DAY_SUNNY
                    }
                }
                hrForecast = List(5) { index ->
                    HourlyForecast().apply {
                        date = ZonedDateTime.now().plusHours(index.toLong())
                        highF = 70f
                        highC = 21f
                        condition = getString(R.string.weather_sunny)
                        icon = WeatherIcons.DAY_SUNNY
                        windMph = 5f
                        windKph = 8f
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
                }
                source = wm.weatherAPI
                query = locationPref.value

                mockWeatherData = this
            })
        } else {
            mockWeatherModel?.location = mockLocData?.name
        }
    }

    private fun Context.isNightMode(): Boolean {
        val currentNightMode: Int = this.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }
}