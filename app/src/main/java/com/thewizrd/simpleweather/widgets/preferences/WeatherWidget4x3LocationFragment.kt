package com.thewizrd.simpleweather.widgets.preferences

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
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.location.LocationManagerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePaddingRelative
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.controls.LocationQueryViewModel
import com.thewizrd.shared_resources.controls.WeatherNowViewModel
import com.thewizrd.shared_resources.helpers.*
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.location.LocationProvider
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.preferences.SliderPreference
import com.thewizrd.shared_resources.tzdb.TZDBCache
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.utils.ContextUtils.isNightMode
import com.thewizrd.shared_resources.weatherdata.WeatherManager
import com.thewizrd.shared_resources.weatherdata.model.*
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.databinding.FragmentWidgetSetupBinding
import com.thewizrd.simpleweather.preferences.ArrayMultiSelectListPreference
import com.thewizrd.simpleweather.preferences.ToolbarPreferenceFragmentCompat
import com.thewizrd.simpleweather.services.WidgetWorker
import com.thewizrd.simpleweather.setup.SetupActivity
import com.thewizrd.simpleweather.snackbar.Snackbar
import com.thewizrd.simpleweather.widgets.AppChoiceDialogBuilder
import com.thewizrd.simpleweather.widgets.MultiLocationPreferenceDialogFragment
import com.thewizrd.simpleweather.widgets.WidgetProviderInfo
import com.thewizrd.simpleweather.widgets.WidgetType
import com.thewizrd.simpleweather.widgets.WidgetUtils
import com.thewizrd.simpleweather.widgets.remoteviews.WeatherWidget4x3LocationsCreator
import kotlinx.coroutines.*
import timber.log.Timber
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*
import kotlin.coroutines.coroutineContext

class WeatherWidget4x3LocationFragment : ToolbarPreferenceFragmentCompat() {
    // Widget id for ConfigurationActivity
    private var mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var mWidgetType = WidgetType.Unknown
    private lateinit var mWidgetInfo: WidgetProviderInfo
    private lateinit var mWidgetOptions: Bundle
    private lateinit var mWidgetViewCtx: Context
    private var resultValue: Intent? = null

    private lateinit var locationProvider: LocationProvider
    private lateinit var locationCallback: LocationProvider.Callback

    /**
     * Tracks the status of the location updates request.
     */
    private var mRequestingLocationUpdates = false

    private var job: Job? = null
    private var initializeWidgetJob: Job? = null

    // Weather
    private val wm = WeatherManager.instance

    // Views
    private lateinit var binding: FragmentWidgetSetupBinding
    private var mockLocData: LocationData? = null
    private var mockWeatherModel: WeatherNowViewModel? = null
    private var mockWeatherData: Weather? = null

    private lateinit var locationPref: ArrayMultiSelectListPreference
    private lateinit var hideSettingsBtnPref: SwitchPreference
    private lateinit var hideRefreshBtnPref: SwitchPreference

    private lateinit var clockPref: Preference
    private lateinit var calPref: Preference

    private lateinit var textSizePref: SliderPreference
    private lateinit var iconSizePref: SliderPreference

    companion object {
        private const val PERMISSION_LOCATION_REQUEST_CODE = 0
        private const val PERMISSION_BGLOCATION_REQUEST_CODE = 1
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
    }

    override fun getTitle(): Int {
        return R.string.widget_configure_prompt
    }

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
            Logger.writeLine(
                Log.DEBUG,
                "LocationsFragment: stopLocationUpdates: updates never requested, no-op."
            )
            return
        }

        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        locationProvider.stopLocationUpdates()
        mRequestingLocationUpdates = false
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
                        if (locationPref.values?.contains(Constants.KEY_GPS) == true && !appCompatActivity.backgroundLocationPermissionEnabled()) {
                            binding.bgLocationLayout.visibility = View.VISIBLE
                        }
                        t?.lifecycleScope?.launchWhenStarted {
                            updateWidgetView()
                        }
                    }
                })
        }

        locationPref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                job?.cancel()

                val selectedValues = newValue as? Set<*>
                if (selectedValues?.contains(Constants.KEY_GPS) == true) {
                    if (!appCompatActivity.backgroundLocationPermissionEnabled()) {
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
                WidgetUtils.setSettingsButtonHidden(mAppWidgetId, newValue as Boolean)
                hideSettingsBtnPref.isChecked = newValue
                updateWidgetView()
                true
            }

        hideRefreshBtnPref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                WidgetUtils.setRefreshButtonHidden(mAppWidgetId, newValue as Boolean)
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
            AppChoiceDialogBuilder(requireContext())
                .setOnItemSelectedListener(object : AppChoiceDialogBuilder.OnAppSelectedListener {
                    override fun onItemSelected(key: String?) {
                        WidgetUtils.setOnClickClockApp(key)
                        updateClockPreference()
                    }
                }).show()
            true
        }
        calPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            AppChoiceDialogBuilder(requireContext())
                .setOnItemSelectedListener(object : AppChoiceDialogBuilder.OnAppSelectedListener {
                    override fun onItemSelected(key: String?) {
                        WidgetUtils.setOnClickCalendarApp(key)
                        updateCalPreference()
                    }
                }).show()
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

        findPreference<Preference>(KEY_CATCLOCKDATE)!!.isVisible =
            WidgetUtils.isClockWidget(mWidgetType) || WidgetUtils.isDateWidget(mWidgetType)

        textSizePref = findPreference(KEY_TEXTSIZE)!!
        textSizePref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                val value = newValue as Float
                WidgetUtils.setCustomTextSizeMultiplier(mAppWidgetId, value)
                updateWidgetView()

                true
            }

        iconSizePref = findPreference(KEY_ICONSIZE)!!
        iconSizePref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                val value = newValue as Float
                WidgetUtils.setCustomIconSizeMultiplier(mAppWidgetId, value)
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
    }

    private fun updateClockPreference() {
        val componentName = WidgetUtils.getClockAppComponent(appCompatActivity)
        if (componentName != null) {
            try {
                val appInfo =
                    requireContext().packageManager.getApplicationInfo(componentName.packageName, 0)
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
                val appInfo =
                    requireContext().packageManager.getApplicationInfo(componentName.packageName, 0)
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
        }
        initializeWidgetJob!!.invokeOnCompletion {
            initializeWidgetJob = null
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
            if (!locationPref.values.isNullOrEmpty()) {
                val selectedValues = locationPref.values ?: emptySet()
                val containsGps = selectedValues.contains(Constants.KEY_GPS)

                job?.cancel()

                supervisorScope {
                    // Check location
                    val task = async(Dispatchers.Default) {
                        if (containsGps) {
                            // Changing location to GPS
                            if (!appCompatActivity.locationPermissionEnabled()) {
                                this@WeatherWidget4x3LocationFragment.requestLocationPermission(
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
                                showSnackbar(
                                    snackbar,
                                    object :
                                        com.google.android.material.snackbar.Snackbar.Callback() {
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
                            if (t is WeatherException || t is CustomException) {
                                showSnackbar(
                                    Snackbar.make(t.message, Snackbar.Duration.SHORT),
                                    null
                                )
                            } else {
                                showSnackbar(
                                    Snackbar.make(
                                        R.string.error_retrieve_location,
                                        Snackbar.Duration.SHORT
                                    ), null
                                )
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
        WidgetUtils.setSettingsButtonHidden(mAppWidgetId, hideSettingsBtnPref.isChecked)
        WidgetUtils.setRefreshButtonHidden(mAppWidgetId, hideRefreshBtnPref.isChecked)

        if (WidgetUtils.isCustomSizeWidget(mWidgetType)) {
            WidgetUtils.setCustomTextSizeMultiplier(mAppWidgetId, textSizePref.getValue())
            WidgetUtils.setCustomIconSizeMultiplier(mAppWidgetId, iconSizePref.getValue())
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

        val locMan =
            appCompatActivity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager?

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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
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
}