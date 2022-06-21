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
import android.widget.FrameLayout
import android.widget.ImageView
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
import com.thewizrd.shared_resources.di.localBroadcastManager
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.exceptions.WeatherException
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.LocationQuery
import com.thewizrd.shared_resources.locationdata.toLocationData
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
import com.thewizrd.simpleweather.preferences.ArrayMultiSelectListPreference
import com.thewizrd.simpleweather.preferences.ToolbarPreferenceFragmentCompat
import com.thewizrd.simpleweather.preferences.colorpreference.ColorPreference
import com.thewizrd.simpleweather.preferences.colorpreference.ColorPreferenceDialogFragment
import com.thewizrd.simpleweather.services.WidgetWorker
import com.thewizrd.simpleweather.setup.SetupActivity
import com.thewizrd.simpleweather.snackbar.Snackbar
import com.thewizrd.simpleweather.widgets.*
import com.thewizrd.simpleweather.widgets.remoteviews.WeatherWidget4x3LocationsCreator
import com.thewizrd.weather_api.weatherModule
import kotlinx.coroutines.*
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*
import kotlin.coroutines.coroutineContext
import kotlin.math.min
import kotlin.math.roundToInt
import com.google.android.material.snackbar.Snackbar as MaterialSnackbar

class WeatherWidget4x3LocationFragment : ToolbarPreferenceFragmentCompat() {
    // Widget id for ConfigurationActivity
    private var mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var mWidgetType = WidgetType.Unknown
    private lateinit var mWidgetInfo: WidgetProviderInfo
    private lateinit var mWidgetOptions: Bundle
    private lateinit var mWidgetViewCtx: Context
    private var resultValue: Intent? = null

    private lateinit var locationProvider: LocationProvider
    private lateinit var locationPermissionLauncher: LocationPermissionLauncher

    private lateinit var wallpaperPermissionLauncher: ActivityResultLauncher<String>

    private var job: Job? = null
    private var initializeWidgetJob: Job? = null

    // Weather
    private val wm = weatherModule.weatherManager

    // Views
    private lateinit var binding: FragmentWidgetSetupBinding
    private var wallpaperLoaded = false
    private var mockLocData: LocationData? = null
    private var mockWeatherModel: WeatherNowViewModel? = null
    private var mockWeatherData: Weather? = null

    private lateinit var locationPref: ArrayMultiSelectListPreference
    private lateinit var hideSettingsBtnPref: SwitchPreference
    private lateinit var hideRefreshBtnPref: SwitchPreference

    private lateinit var clockPref: Preference
    private lateinit var calPref: Preference

    private lateinit var bgChoicePref: ListPreference
    private lateinit var bgColorPref: ColorPreference
    private lateinit var txtColorPref: ColorPreference
    private lateinit var bgStylePref: ListPreference

    private lateinit var textSizePref: SliderPreference
    private lateinit var iconSizePref: SliderPreference

    companion object {
        private const val SETUP_REQUEST_CODE = 10

        fun newInstance(widgetID: Int): WeatherWidget4x3LocationFragment {
            val fragment = WeatherWidget4x3LocationFragment()
            fragment.requireArguments().putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID)
            return fragment
        }
    }

    init {
        arguments = Bundle()
    }

    override fun onDestroyView() {
        job?.cancel()
        super.onDestroyView()
        wallpaperLoaded = false
    }

    override val titleResId: Int
        get() = R.string.widget_configure_prompt

    override fun onCreate(savedInstanceState: Bundle?) {
        // Find the widget id from the intent.
        mAppWidgetId = requireArguments().getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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

        (activity as? AppCompatActivity)?.let {
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
        setPreferencesFromResource(R.xml.pref_widget4x3_locations, rootKey)

        locationPref = findPreference(KEY_LOCATION)!!

        lifecycleScope.launch {
            locationPref.addEntry(R.string.pref_item_gpslocation, Constants.KEY_GPS)

            val favs = settingsManager.getFavorites() ?: emptyList()
            favs.forEach { location ->
                locationPref.addEntry(location.name, location.query)
            }

            // Reset value
            locationPref.values = emptySet()

            WidgetUtils.getLocationDataSet(mAppWidgetId)?.let {
                locationPref.values = it
            }

            viewLifecycleOwnerLiveData.observe(
                this@WeatherWidget4x3LocationFragment,
                object : Observer<LifecycleOwner> {
                    override fun onChanged(t: LifecycleOwner?) {
                        viewLifecycleOwnerLiveData.removeObserver(this)
                        if (locationPref.values?.contains(Constants.KEY_GPS) == true && !requireContext().backgroundLocationPermissionEnabled()) {
                            binding.bgLocationLayout.visibility = View.VISIBLE
                        }
                        t?.lifecycleScope?.launchWhenStarted {
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

        hideSettingsBtnPref = findPreference(KEY_HIDESETTINGSBTN)!!
        hideRefreshBtnPref = findPreference(KEY_HIDEREFRESHBTN)!!

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

        clockPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            activity?.let {
                AppChoiceDialogBuilder(it)
                    .setOnItemSelectedListener(object :
                        AppChoiceDialogBuilder.OnAppSelectedListener {
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
                    .setOnItemSelectedListener(object :
                        AppChoiceDialogBuilder.OnAppSelectedListener {
                        override fun onItemSelected(key: String?) {
                            WidgetUtils.setOnClickCalendarApp(key)
                            updateCalPreference(it)
                        }
                    }).show()
            }
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

        findPreference<Preference>(KEY_CATCLOCKDATE)!!.isVisible =
            WidgetUtils.isClockWidget(mWidgetType) || WidgetUtils.isDateWidget(mWidgetType)

        // Widget background style
        bgChoicePref = findPreference(KEY_BGCOLOR)!!
        bgStylePref = findPreference(KEY_BGSTYLE)!!
        bgColorPref = findPreference(KEY_BGCOLORCODE)!!
        txtColorPref = findPreference(KEY_TXTCOLORCODE)!!

        bgChoicePref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                val value = newValue.toString().toInt()

                val mWidgetBackground = WidgetUtils.WidgetBackground.valueOf(value)
                mWidgetOptions.putSerializable(KEY_BGCOLOR, mWidgetBackground)

                updateWidgetView()

                bgColorPref.isVisible = mWidgetBackground == WidgetUtils.WidgetBackground.CUSTOM
                txtColorPref.isVisible = mWidgetBackground == WidgetUtils.WidgetBackground.CUSTOM

                if (mWidgetBackground == WidgetUtils.WidgetBackground.CURRENT_CONDITIONS) {
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
                    WidgetUtils.WidgetBackgroundStyle.valueOf(newValue.toString().toInt())
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

        val styles = WidgetUtils.WidgetBackgroundStyle.values()
        val styleEntries = arrayOfNulls<CharSequence>(styles.size)
        val styleEntryValues = arrayOfNulls<CharSequence>(styles.size)

        for (i in styles.indices) {
            when (val style = styles[i]) {
                WidgetUtils.WidgetBackgroundStyle.PANDA -> {
                    styleEntries[i] = requireContext().getString(R.string.label_style_panda)
                    styleEntryValues[i] = style.value.toString()
                    bgStylePref.setDefaultValue(styleEntryValues[i])
                }
                WidgetUtils.WidgetBackgroundStyle.DARK -> {
                    styleEntries[i] = requireContext().getText(R.string.label_style_dark)
                    styleEntryValues[i] = style.value.toString()
                }
                WidgetUtils.WidgetBackgroundStyle.LIGHT -> {
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

            bgStylePref.setValueIndex(
                listOf(*WidgetUtils.WidgetBackgroundStyle.values()).indexOf(
                    mWidgetBGStyle
                )
            )
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
            bgChoicePref.setValueIndex(WidgetUtils.WidgetBackground.TRANSPARENT.value)
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
        } else if (preference is ColorPreference) {
            val f = ColorPreferenceDialogFragment.newInstance(preference.getKey())
            f.setTargetFragment(this, 0)
            f.show(parentFragmentManager, ColorPreferenceDialogFragment::class.java.name)
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
        initializeWidgetJob = viewLifecycleOwner.lifecycleScope.launch {
            binding.widgetContainer.removeAllViews()

            val views = withContext(Dispatchers.Default) {
                buildMockData()

                WeatherWidget4x3LocationsCreator(requireContext()).run {
                    buildUpdate(
                        mAppWidgetId,
                        Collections.nCopies(locationPref.values?.size ?: 0, mockLocData),
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

            updateBackground()
        }
        initializeWidgetJob!!.invokeOnCompletion {
            initializeWidgetJob = null
        }
    }

    private fun updateBackground() {
        binding.widgetContainer.findViewById<View>(R.id.widget)?.run {
            if (background == null) {
                setBackgroundResource(R.drawable.app_widget_background_mask)
                clipToOutline = true
            }
        }

        if (WidgetUtils.WidgetBackground.valueOf(bgChoicePref.value.toInt()) == WidgetUtils.WidgetBackground.CURRENT_CONDITIONS) {
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

    private fun updateWidgetView() {
        runWithView {
            initializeWidgetJob?.join()

            if (binding.widgetContainer.childCount > 0) {
                val views = withContext(Dispatchers.Default) {
                    buildMockData()

                    WeatherWidget4x3LocationsCreator(requireContext()).run {
                        buildUpdate(
                            mAppWidgetId,
                            Collections.nCopies(locationPref.values?.size ?: 0, mockLocData),
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Resize necessary views
        val observer = binding.root.viewTreeObserver
        observer.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
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
            if (!locationPref.values.isNullOrEmpty()) {
                val selectedValues = locationPref.values ?: emptySet()
                val containsGps = selectedValues.contains(Constants.KEY_GPS)

                job?.cancel()

                supervisorScope {
                    // Check location
                    val task = async(Dispatchers.Default) {
                        val ctx = requireContext()

                        if (containsGps) {
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
                                showSnackbar(snackbar, object : MaterialSnackbar.Callback() {
                                    override fun onDismissed(
                                        transientBottomBar: MaterialSnackbar?,
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
                                    if (containsGps) {
                                        settingsManager.setFollowGPS(true)
                                    }

                                    // Save data for widget
                                    WidgetUtils.deleteWidget(mAppWidgetId)
                                    WidgetUtils.saveLocationDataSet(mAppWidgetId, selectedValues)
                                    selectedValues.forEach {
                                        WidgetUtils.addWidgetId(it, mAppWidgetId)
                                    }
                                    finalizeWidgetUpdate()
                                }
                            }
                        } else {
                            context?.let {
                                if (t is WeatherException || t is CustomException) {
                                    showSnackbar(
                                        Snackbar.make(it, t.message, Snackbar.Duration.SHORT)
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
        WidgetUtils.setSettingsButtonHidden(mAppWidgetId, hideSettingsBtnPref.isChecked)
        WidgetUtils.setRefreshButtonHidden(mAppWidgetId, hideRefreshBtnPref.isChecked)

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
                name = getString(R.string.pref_location)
                query = ""
                latitude = 0.toDouble()
                longitude = 0.toDouble()
                tzLong = "UTC"
                locationType = LocationType.SEARCH
                weatherSource = wm.getWeatherAPI()
                locationSource = wm.getLocationProvider().getLocationAPI()
            }
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
                        highF = 75f
                        highC = 23f
                        lowF = 60f
                        lowC = 15f
                        condition = getString(R.string.weather_sunny)
                        icon = WeatherIcons.DAY_SUNNY
                    }
                }
                hrForecast = List(6) { index ->
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
                    airQuality = AirQuality().apply {
                        index = 46
                    }
                }
                atmosphere = Atmosphere()
                precipitation = Precipitation().apply {
                    pop = 15
                }
                source = wm.getWeatherAPI()
                query = ""

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