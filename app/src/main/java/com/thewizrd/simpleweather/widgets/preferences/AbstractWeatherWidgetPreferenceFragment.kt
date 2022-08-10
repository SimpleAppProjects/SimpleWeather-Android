package com.thewizrd.simpleweather.widgets.preferences

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.CallSuper
import androidx.core.content.PermissionChecker
import androidx.core.location.LocationManagerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePaddingRelative
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import com.bumptech.glide.RequestManager
import com.thewizrd.common.controls.WeatherUiModel
import com.thewizrd.common.helpers.LocationPermissionLauncher
import com.thewizrd.common.helpers.backgroundLocationPermissionEnabled
import com.thewizrd.common.helpers.getBackgroundLocationRationale
import com.thewizrd.common.helpers.locationPermissionEnabled
import com.thewizrd.common.location.LocationProvider
import com.thewizrd.common.location.LocationResult
import com.thewizrd.common.utils.ErrorMessage
import com.thewizrd.common.viewmodels.LocationSearchResult
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.LocationQuery
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.utils.ContextUtils.isLandscape
import com.thewizrd.shared_resources.utils.ContextUtils.isNightMode
import com.thewizrd.shared_resources.utils.ContextUtils.isSmallestWidth
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.weatherdata.model.*
import com.thewizrd.simpleweather.GlideApp
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.activities.LocationSearch
import com.thewizrd.simpleweather.databinding.FragmentWidgetSetupBinding
import com.thewizrd.simpleweather.preferences.ToolbarPreferenceFragmentCompat
import com.thewizrd.simpleweather.preferences.colorpreference.ColorPreference
import com.thewizrd.simpleweather.preferences.colorpreference.ColorPreferenceDialogFragment
import com.thewizrd.simpleweather.services.WidgetWorker
import com.thewizrd.simpleweather.setup.SetupActivity
import com.thewizrd.simpleweather.snackbar.Snackbar
import com.thewizrd.simpleweather.widgets.WidgetProviderInfo
import com.thewizrd.simpleweather.widgets.WidgetType
import com.thewizrd.simpleweather.widgets.WidgetUtils
import com.thewizrd.weather_api.weatherModule
import kotlinx.coroutines.isActive
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.properties.Delegates
import kotlin.random.Random

abstract class AbstractWeatherWidgetPreferenceFragment : ToolbarPreferenceFragmentCompat() {
    // Widget id for ConfigurationActivity
    protected var mAppWidgetId by Delegates.notNull<Int>()
    protected lateinit var mWidgetType: WidgetType
    protected lateinit var mWidgetInfo: WidgetProviderInfo
    protected lateinit var mWidgetOptions: Bundle
    protected lateinit var mWidgetViewCtx: Context
    private lateinit var resultValue: Intent

    protected lateinit var locationPermissionLauncher: LocationPermissionLauncher
    protected lateinit var locationSearchLauncher: ActivityResultLauncher<Void?>
    protected lateinit var setupLauncher: ActivityResultLauncher<Intent>
    protected lateinit var wallpaperPermissionLauncher: ActivityResultLauncher<String>

    // Views
    protected lateinit var binding: FragmentWidgetSetupBinding
    protected lateinit var mGlide: RequestManager

    private val wm = weatherModule.weatherManager
    private lateinit var locationProvider: LocationProvider

    protected var searchLocation: LocationQuery? = null
    protected var lastSelectedValue: CharSequence? = null

    protected val mockLocationData by lazy { buildMockLocationData() }
    protected val mockWeatherModel by lazy { buildMockWeatherModel() }

    companion object {
        internal val MAX_LOCATIONS by lazy { settingsManager.getMaxLocations() }
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
        super.onDestroyView()
    }

    override val titleResId: Int
        get() = R.string.widget_configure_prompt

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        /*
         * This should be before the super call,
         * so this is setup before onCreatePreferences is called
         */
        // Find the widget id from the intent.
        mAppWidgetId = arguments?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: savedInstanceState?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            requireActivity().finishAffinity()
            return
        }

        mWidgetType = WidgetUtils.getWidgetTypeFromID(mAppWidgetId)
        mWidgetInfo = WidgetUtils.getWidgetProviderInfoFromType(mWidgetType)!!
        mWidgetOptions = WidgetUtils.getPreviewAppWidgetOptions(requireContext(), mAppWidgetId)

        // Set the result value for WidgetConfigActivity
        resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)

        super.onCreate(savedInstanceState)

        mWidgetViewCtx = requireContext().applicationContext.run {
            this.getThemeContextOverride(!this.isNightMode())
        }

        mGlide = GlideApp.with(this)

        locationProvider = LocationProvider(requireContext())

        locationPermissionLauncher = LocationPermissionLauncher(
            this,
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

        locationSearchLauncher = registerForActivityResult(LocationSearch()) { result ->
            onLocationSearchResult(result)
        }

        wallpaperPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) {
                    loadWallpaperBackground(true)
                }
            }

        setupLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                onSetupActivityResult(it)
            }

        lifecycleScope.launchWhenCreated {
            if (!settingsManager.isWeatherLoaded() && isActive) {
                showToast(R.string.prompt_setup_app_first, Toast.LENGTH_SHORT)

                setupLauncher.launch(
                    Intent(requireContext(), SetupActivity::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)
                    }
                )
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
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
        binding.layoutContainer.addView(inflatedView, layoutIdx)

        // For landscape orientation
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val sysBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.updatePaddingRelative(
                start = sysBarInsets.left,
                end = sysBarInsets.right,
                bottom = sysBarInsets.bottom
            )

            insets
        }

        listView?.isNestedScrollingEnabled = false

        toolbar.setNavigationOnClickListener {
            cancelActivityResult()
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

        loadWallpaperBackground(true)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        savedInstanceState?.getBundle(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS)?.run {
            mWidgetOptions.putAll(this)
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

        loadWallpaperBackground()
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

        resizeWidgetFrame()
        updateWidgetView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)
        outState.putBundle(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, mWidgetOptions)
    }

    protected open fun onLocationSearchResult(result: LocationSearchResult) {}
    protected abstract fun onSetupActivityResult(result: ActivityResult)

    protected abstract fun initializeWidget()
    protected abstract fun updateWidgetView()

    protected fun resizeWidgetContainer() {
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

    protected fun resizeWidgetFrame() {
        val widgetView = binding.widgetContainer.findViewById<View>(R.id.widget)

        if (!mWidgetViewCtx.isSmallestWidth(600) || !mWidgetViewCtx.isLandscape()) {
            if (mWidgetViewCtx.isLandscape()) {
                TransitionManager.beginDelayedTransition(
                    binding.widgetFrame.parent as ViewGroup,
                    AutoTransition().apply {
                        addTarget(binding.widgetFrame)
                    })

                binding.widgetFrame.updateLayoutParams {
                    height = widgetView.layoutParams.height.times(1.1f).roundToInt()
                }
            } else {
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
        }
    }

    protected abstract fun prepareWidget()
    protected abstract fun finalizeWidgetUpdate()

    protected fun pushWidgetUpdate() {
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

    // TODO: Find a fix for Android 13+ (READ_EXTERNAL_STORAGE is deprecated)
    protected fun loadWallpaperBackground(skipPermissions: Boolean = false) {
        if (!skipPermissions && PermissionChecker.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PermissionChecker.PERMISSION_GRANTED
        ) {
            wallpaperPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            return
        }

        runWithView {
            runCatching {
                val wallpaperMgr = WallpaperManager.getInstance(requireContext())
                wallpaperMgr.fastDrawable?.let { drawable ->
                    binding.widgetBackground.setImageDrawable(drawable)
                }
            }.onFailure {
                Logger.writeLine(Log.DEBUG, it)
            }
        }
    }

    protected suspend fun updateLocation(): LocationResult {
        if (settingsManager.useFollowGPS()) {
            if (!requireContext().locationPermissionEnabled()) {
                return LocationResult.PermissionDenied()
            }

            val locMan =
                requireContext().getSystemService(Context.LOCATION_SERVICE) as? LocationManager

            if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
                return LocationResult.Error(errorMessage = ErrorMessage.Resource(R.string.error_retrieve_location))
            }

            return locationProvider.getLatestLocationData()
        }

        return LocationResult.NotChanged(null)
    }

    protected fun cancelActivityResult() {
        activity?.run {
            setResult(Activity.RESULT_CANCELED, resultValue)
            finish()
        }
    }

    protected fun updateMockLocation(locationName: String, locationQuery: String) {
        mockLocationData.name = locationName
        mockLocationData.query = locationQuery

        mockWeatherModel.location = locationName
        mockWeatherModel.weatherData?.query = locationQuery
    }

    private fun buildMockLocationData(): LocationData {
        return LocationData().apply {
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

    private fun buildMockWeatherData(): Weather {
        return Weather().apply {
            location = Location().apply {
                name = getString(R.string.pref_location)
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
            query = ""
        }
    }

    private fun buildMockWeatherModel(data: Weather = buildMockWeatherData()): WeatherUiModel {
        return WeatherUiModel(data)
    }
}