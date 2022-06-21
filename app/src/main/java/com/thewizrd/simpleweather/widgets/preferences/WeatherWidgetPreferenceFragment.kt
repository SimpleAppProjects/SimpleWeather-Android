package com.thewizrd.simpleweather.widgets.preferences

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePaddingRelative
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.thewizrd.common.controls.WeatherNowViewModel
import com.thewizrd.common.helpers.LocationPermissionLauncher
import com.thewizrd.common.helpers.backgroundLocationPermissionEnabled
import com.thewizrd.common.helpers.getBackgroundLocationRationale
import com.thewizrd.common.helpers.locationPermissionEnabled
import com.thewizrd.common.location.LocationProvider
import com.thewizrd.common.preferences.SliderPreference
import com.thewizrd.common.utils.glide.TransparentOverlay
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.controls.ComboBoxItem
import com.thewizrd.shared_resources.di.localBroadcastManager
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.exceptions.WeatherException
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.LocationQuery
import com.thewizrd.shared_resources.locationdata.toLocationData
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.shared_resources.utils.CommonActions
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.utils.ContextUtils.isLandscape
import com.thewizrd.shared_resources.utils.ContextUtils.isNightMode
import com.thewizrd.shared_resources.utils.ContextUtils.isSmallestWidth
import com.thewizrd.shared_resources.utils.CustomException
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.weatherdata.model.*
import com.thewizrd.simpleweather.GlideApp
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.databinding.FragmentWidgetSetupBinding
import com.thewizrd.simpleweather.preferences.ArrayListPreference
import com.thewizrd.simpleweather.preferences.ToolbarPreferenceFragmentCompat
import com.thewizrd.simpleweather.preferences.colorpreference.ColorPreference
import com.thewizrd.simpleweather.preferences.colorpreference.ColorPreferenceDialogFragment
import com.thewizrd.simpleweather.services.WidgetWorker
import com.thewizrd.simpleweather.setup.SetupActivity
import com.thewizrd.simpleweather.snackbar.Snackbar
import com.thewizrd.simpleweather.utils.NavigationUtils.safeNavigate
import com.thewizrd.simpleweather.widgets.*
import com.thewizrd.simpleweather.widgets.AppChoiceDialogBuilder.OnAppSelectedListener
import com.thewizrd.simpleweather.widgets.WidgetUtils.WidgetBackground
import com.thewizrd.simpleweather.widgets.WidgetUtils.WidgetBackgroundStyle
import com.thewizrd.weather_api.weatherModule
import kotlinx.coroutines.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.coroutines.coroutineContext
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random
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
    private var query_vm: LocationQuery? = null

    private lateinit var locationProvider: LocationProvider
    private lateinit var locationPermissionLauncher: LocationPermissionLauncher

    private lateinit var wallpaperPermissionLauncher: ActivityResultLauncher<String>

    private var job: Job? = null

    // Weather
    private val wm = weatherModule.weatherManager

    // Views
    private lateinit var binding: FragmentWidgetSetupBinding
    private var wallpaperLoaded = false
    private var mockLocData: LocationData? = null
    private var mockWeatherModel: WeatherNowViewModel? = null
    private var mockWeatherData: Weather? = null

    private var mLastSelectedValue: CharSequence? = null

    private lateinit var locationPref: ArrayListPreference
    private lateinit var hideLocNamePref: SwitchPreference
    private lateinit var hideSettingsBtnPref: SwitchPreference
    private lateinit var hideRefreshBtnPref: SwitchPreference

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

    private lateinit var textSizePref: SliderPreference
    private lateinit var iconSizePref: SliderPreference

    companion object {
        private val MAX_LOCATIONS by lazy { settingsManager.getMaxLocations() }
        private const val SETUP_REQUEST_CODE = 10

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
        wallpaperLoaded = false
    }

    override val titleResId: Int
        get() = R.string.widget_configure_prompt

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

        mWidgetViewCtx = requireContext().applicationContext.run {
            this.getThemeContextOverride(!this.isNightMode())
        }

        // Location Listener
        locationProvider = LocationProvider(requireActivity())

        locationPermissionLauncher = LocationPermissionLauncher(
            requireActivity(),
            locationCallback = { granted ->
                if (granted) {
                    // permission was granted, yay!
                    // Do the task you need to do.
                    prepareWidget()
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    context?.let {
                        showSnackbar(
                            Snackbar.make(
                                it,
                                R.string.error_location_denied,
                                Snackbar.Duration.SHORT
                            )
                        )
                    }
                }
            },
            bgLocationCallback = { granted ->
                if (granted) {
                    binding.bgLocationLayout.visibility = View.GONE
                } else {
                    context?.let {
                        showSnackbar(
                            Snackbar.make(
                                it,
                                R.string.error_location_denied,
                                Snackbar.Duration.SHORT
                            )
                        )
                    }
                }
            }
        )

        wallpaperPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) {
                    runWithView(Dispatchers.Default) {
                        loadWallpaperBackground(true)
                    }
                }
            }

        lifecycleScope.launchWhenCreated {
            if (!settingsManager.isWeatherLoaded() && isActive) {
                Toast.makeText(
                    requireContext(),
                    R.string.prompt_setup_app_first,
                    Toast.LENGTH_SHORT
                ).show()

                val intent = Intent(requireContext(), SetupActivity::class.java)
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

        val layoutIdx = binding.layoutContainer.indexOfChild(binding.bgLocationLayout)
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

        (requireActivity() as? AppCompatActivity)?.let {
            it.setSupportActionBar(toolbar)
            it.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        binding.widgetCompleteBtn.setOnClickListener {
            prepareWidget()
        }

        binding.bgLocationRationaleText.text = requireContext().getBackgroundLocationRationale()

        binding.bgLocationSettingsBtn.setOnClickListener {
            val ctx = it.context

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (!ctx.backgroundLocationPermissionEnabled()) {
                    locationPermissionLauncher.requestBackgroundLocationPermission()
                }
            }
        }

        lifecycleScope.launch(Dispatchers.Default) {
            loadWallpaperBackground(true)
        }

        return root
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
                        if (locationPref.value == Constants.KEY_GPS && !requireContext().backgroundLocationPermissionEnabled()) {
                            binding.bgLocationLayout.visibility = View.VISIBLE
                        }
                        updateLocationView()
                    }
                })
        }

        locationPref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { pref, newValue ->
                job?.cancel()

                val selectedValue = newValue as CharSequence
                if (Constants.KEY_SEARCH.contentEquals(selectedValue)) {
                    // Setup search UI
                    view?.findNavController()
                        ?.safeNavigate(WeatherWidgetPreferenceFragmentDirections.actionWeatherWidgetPreferenceFragmentToLocationSearchFragment2())
                    query_vm = null
                    binding.bgLocationLayout.visibility = View.GONE
                    return@OnPreferenceChangeListener false
                } else if (Constants.KEY_GPS.contentEquals(selectedValue)) {
                    mLastSelectedValue = null
                    query_vm = null
                    if (!pref.context.backgroundLocationPermissionEnabled()) {
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
        hideRefreshBtnPref = findPreference(KEY_HIDEREFRESHBTN)!!

        hideLocNamePref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                mWidgetOptions.putBoolean(KEY_HIDELOCNAME, newValue as Boolean)
                hideLocNamePref.isChecked = newValue
                updateWidgetView()
                true
            }

        hideSettingsBtnPref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                mWidgetOptions.putBoolean(KEY_HIDESETTINGSBTN, newValue as Boolean)
                hideSettingsBtnPref.isChecked = newValue
                updateWidgetView()
                true
            }

        hideRefreshBtnPref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                mWidgetOptions.putBoolean(KEY_HIDEREFRESHBTN, newValue as Boolean)
                hideRefreshBtnPref.isChecked = newValue
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

        hideRefreshBtnPref.isChecked = WidgetUtils.isRefreshButtonHidden(mAppWidgetId)

        if (WidgetUtils.isMaterialYouWidget(mWidgetType)) {
            hideRefreshBtnPref.isVisible = false
        }

        // Time and Date
        clockPref = findPreference(KEY_CLOCKAPP)!!
        calPref = findPreference(KEY_CALENDARAPP)!!
        useTimeZonePref = findPreference(KEY_USETIMEZONE)!!

        clockPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            activity?.let {
                AppChoiceDialogBuilder(it)
                    .setOnItemSelectedListener(object : OnAppSelectedListener {
                        override fun onItemSelected(key: String?) {
                            WidgetUtils.setOnClickClockApp(key)
                            updateClockPreference(it)
                        }
                    }).show()
            }
            true
        }
        calPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            activity?.let {
                AppChoiceDialogBuilder(it)
                    .setOnItemSelectedListener(object : OnAppSelectedListener {
                        override fun onItemSelected(key: String?) {
                            WidgetUtils.setOnClickCalendarApp(key)
                            updateCalPreference(it)
                        }
                    }).show()
            }
            true
        }
        useTimeZonePref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                mWidgetOptions.putBoolean(KEY_USETIMEZONE, newValue as Boolean)
                updateWidgetView()
                true
            }

        if (WidgetUtils.isClockWidget(mWidgetType)) {
            updateClockPreference(requireContext())
            clockPref.isVisible = true
        } else {
            clockPref.isVisible = false
        }

        if (WidgetUtils.isDateWidget(mWidgetType)) {
            updateCalPreference(requireContext())
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

        bgChoicePref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                val value = newValue.toString().toInt()

                val mWidgetBackground = WidgetBackground.valueOf(value)
                mWidgetOptions.putSerializable(KEY_BGCOLOR, mWidgetBackground)

                updateWidgetView()

                bgColorPref.isVisible = mWidgetBackground == WidgetBackground.CUSTOM
                txtColorPref.isVisible = mWidgetBackground == WidgetBackground.CUSTOM

                if (mWidgetBackground == WidgetBackground.CURRENT_CONDITIONS) {
                    if (WidgetUtils.isPandaWidget(mWidgetType)) {
                        bgStylePref.isVisible = true
                        return@OnPreferenceChangeListener true
                    }
                }

                bgStylePref.setValueIndex(0)
                bgStylePref.callChangeListener(bgStylePref.value)
                bgStylePref.isVisible = false
                true
            }

        bgStylePref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                mWidgetOptions.putSerializable(
                    KEY_BGSTYLE,
                    WidgetBackgroundStyle.valueOf(newValue.toString().toInt())
                )
                updateWidgetView()
                true
            }

        bgColorPref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                mWidgetOptions.putInt(KEY_BGCOLORCODE, newValue as Int)
                updateWidgetView()
                true
            }

        txtColorPref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                mWidgetOptions.putInt(KEY_TXTCOLORCODE, newValue as Int)
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

        textSizePref = findPreference(KEY_TEXTSIZE)!!
        textSizePref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                mWidgetOptions.putFloat(KEY_TEXTSIZE, newValue as Float)
                updateWidgetView()
                true
            }

        iconSizePref = findPreference(KEY_ICONSIZE)!!
        iconSizePref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                mWidgetOptions.putFloat(KEY_ICONSIZE, newValue as Float)
                updateWidgetView()
                true
            }

        if (WidgetUtils.isCustomSizeWidget(mWidgetType)) {
            textSizePref.setValue(WidgetUtils.getCustomTextSizeMultiplier(mAppWidgetId))
            textSizePref.callChangeListener(textSizePref.getValue())
            iconSizePref.setValue(WidgetUtils.getCustomIconSizeMultiplier(mAppWidgetId))
            iconSizePref.callChangeListener(iconSizePref.getValue())

            findPreference<Preference>("key_custom_size")?.isVisible = true
        } else {
            findPreference<Preference>("key_custom_size")?.isVisible = false
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
                            query_vm =
                                LocationQuery(
                                    data
                                )
                            val item =
                                ComboBoxItem(
                                    query_vm!!.locationName,
                                    query_vm!!.locationQuery
                                )
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

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            if (!wallpaperLoaded) {
                loadWallpaperBackground()
            }
        }
    }

    private fun updateClockPreference(context: Context) {
        val componentName = WidgetUtils.getClockAppComponent(context)
        if (componentName != null) {
            try {
                val appInfo =
                    context.packageManager.getApplicationInfo(componentName.packageName, 0)
                val appLabel = context.packageManager.getApplicationLabel(appInfo)
                clockPref.summary = appLabel
                return
            } catch (e: PackageManager.NameNotFoundException) {
                // App not available
                WidgetUtils.setOnClickClockApp(null)
            }
        }

        clockPref.setSummary(R.string.summary_default)
    }

    private fun updateCalPreference(context: Context) {
        val componentName = WidgetUtils.getCalendarAppComponent(context)
        if (componentName != null) {
            try {
                val appInfo =
                    context.packageManager.getApplicationInfo(componentName.packageName, 0)
                val appLabel = context.packageManager.getApplicationLabel(appInfo)
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

                WidgetUpdaterHelper.buildUpdate(
                    mWidgetViewCtx, info, mAppWidgetId,
                    mockLocData!!, mockWeatherModel!!, mWidgetOptions, false
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

            if (!widgetView.context.isSmallestWidth(600) || !widgetView.context.isLandscape()) {
                TransitionManager.beginDelayedTransition(
                    binding.widgetFrame.parent as ViewGroup,
                    AutoTransition().apply {
                        addTarget(binding.widgetFrame)
                    })

                binding.widgetFrame.updateLayoutParams {
                    height = min(
                        widgetView.layoutParams.height * 1.25f,
                        widgetView.context.dpToPx(360f)
                    ).roundToInt()
                }
            }

            updateLocationView()
            updateBackground()
        }
    }

    private fun updateBackground() {
        binding.widgetContainer.findViewById<View>(R.id.widget)?.run {
            if (background == null) {
                setBackgroundResource(R.drawable.app_widget_background_mask)
                clipToOutline = true
            }
        }

        if (WidgetBackground.valueOf(bgChoicePref.value.toInt()) == WidgetBackground.CURRENT_CONDITIONS) {
            val imageView = binding.widgetContainer.findViewById<ImageView>(R.id.widgetBackground)
            if (imageView != null) {
                GlideApp.with(this)
                    .load("file:///android_asset/backgrounds/day.jpg")
                    .apply(
                        RequestOptions.noTransformation()
                            .format(DecodeFormat.PREFER_RGB_565)
                            .transform(
                                TransparentOverlay(0x33),
                                CenterCrop()
                            )
                    )
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

                    WidgetUpdaterHelper.buildUpdate(
                        requireContext(), mWidgetInfo, mAppWidgetId,
                        mockLocData!!, mockWeatherModel!!, mWidgetOptions, false
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
                activity?.run {
                    setResult(Activity.RESULT_CANCELED, resultValue)
                    finish()
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                activity?.run {
                    setResult(Activity.RESULT_CANCELED, resultValue)
                    finish()
                }
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
                            val ctx = requireContext()

                            // Changing location to GPS
                            if (!ctx.locationPermissionEnabled()) {
                                locationPermissionLauncher.requestLocationPermission()
                                return@async false
                            }

                            if (settingsManager.useFollowGPS() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !settingsManager.requestedBGAccess() &&
                                !ctx.backgroundLocationPermissionEnabled()
                            ) {
                                val snackbar = Snackbar.make(
                                    ctx,
                                    ctx.getBackgroundLocationRationale(),
                                    Snackbar.Duration.VERY_LONG
                                )
                                snackbar.setAction(android.R.string.ok) {
                                    locationPermissionLauncher.requestBackgroundLocationPermission()
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
                                ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
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
                                context?.let {
                                    if (t is WeatherException || t is CustomException) {
                                        showSnackbar(
                                            Snackbar.make(
                                                it,
                                                t.message,
                                                Snackbar.Duration.SHORT
                                            )
                                        )
                                    } else {
                                        showSnackbar(
                                            Snackbar.make(
                                                it,
                                                R.string.error_retrieve_location,
                                                Snackbar.Duration.SHORT
                                            )
                                        )
                                    }
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
                                    locData = query_vm!!.toLocationData()

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
                                        activity?.run {
                                            setResult(Activity.RESULT_CANCELED, resultValue)
                                            finish()
                                        }
                                    }
                                }
                            } else {
                                context?.let {
                                    showSnackbar(
                                        Snackbar.make(
                                            it,
                                            R.string.error_retrieve_location,
                                            Snackbar.Duration.SHORT
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                activity?.run {
                    setResult(Activity.RESULT_CANCELED, resultValue)
                    finish()
                }
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
        WidgetUtils.setRefreshButtonHidden(mAppWidgetId, hideRefreshBtnPref.isChecked)
        WidgetUtils.setForecastOption(mAppWidgetId, fcastOptPref.value.toInt())
        WidgetUtils.setTap2Switch(mAppWidgetId, tap2switchPref.isChecked)
        WidgetUtils.setUseTimeZone(mAppWidgetId, useTimeZonePref.isChecked)
        if (mWidgetType == WidgetType.Widget4x2Graph) {
            WidgetUtils.setWidgetGraphType(mAppWidgetId, graphTypePref.value.toInt())
        }
        if (WidgetUtils.isCustomSizeWidget(mWidgetType)) {
            WidgetUtils.setCustomTextSizeMultiplier(mAppWidgetId, textSizePref.getValue())
            WidgetUtils.setCustomIconSizeMultiplier(mAppWidgetId, iconSizePref.getValue())
        }

        activity?.run {
            // Trigger widget service to update widget
            WidgetWorker.enqueueRefreshWidget(
                this,
                intArrayOf(mAppWidgetId),
                mWidgetInfo
            )

            // Create return intent
            setResult(Activity.RESULT_OK, resultValue)
            finish()
        }
    }

    @SuppressLint("MissingPermission")
    @Throws(CustomException::class)
    private suspend fun updateLocation(): Boolean {
        var locationChanged = false

        activity?.run {
            if (!locationPermissionEnabled()) {
                return@updateLocation false
            }
        }

        val locMan = activity?.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

        if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
            throw CustomException(R.string.error_enable_location_services)
        }

        var location = withContext(Dispatchers.IO) {
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

        /* Get current location from provider */
        if (location == null) {
            location = withTimeoutOrNull(30000) {
                locationProvider.getCurrentLocation()
            }
        }

        if (location != null && coroutineContext.isActive) {
            var query_vm: LocationQuery? = null

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
                val tzId = weatherModule.tzdbService.getTimeZone(
                    query_vm.locationLat,
                    query_vm.locationLong
                )

                if ("unknown" != tzId)
                    query_vm.locationTZLong = tzId
            }

            if (!coroutineContext.isActive) return false

            // Save location as last known
            settingsManager.saveLastGPSLocData(query_vm.toLocationData(location))

            localBroadcastManager.sendBroadcast(Intent(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE))

            locationChanged = true
        }

        return locationChanged
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
                minForecast = List(10) {
                    val now = ZonedDateTime.now().truncatedTo(ChronoUnit.MINUTES).let {
                        it.withMinute(it.minute - (it.minute % 10))
                    }

                    MinutelyForecast().apply {
                        date = now.plusMinutes(it.toLong() * 10)
                        rainMm = Random.nextFloat()
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
                    airQuality = AirQuality().apply {
                        index = 46
                    }
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

    private suspend fun loadWallpaperBackground(skipPermissions: Boolean = false) {
        if (!skipPermissions && ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            wallpaperPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            return
        }

        runCatching {
            val wallpaperMgr = WallpaperManager.getInstance(requireContext())

            wallpaperMgr.fastDrawable?.let {
                withContext(Dispatchers.Main.immediate) {
                    binding.widgetBackground.setImageDrawable(it)
                    wallpaperLoaded = true
                }
            }
        }
    }
}