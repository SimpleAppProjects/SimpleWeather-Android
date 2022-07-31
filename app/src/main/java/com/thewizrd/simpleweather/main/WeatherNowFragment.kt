package com.thewizrd.simpleweather.main

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.*
import android.view.ViewGroup.MarginLayoutParams
import android.widget.GridLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.util.ObjectsCompat
import androidx.core.view.*
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigator
import androidx.recyclerview.widget.DiffUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.transition.MaterialFadeThrough
import com.thewizrd.common.controls.IconControl
import com.thewizrd.common.controls.WeatherAlertsViewModel
import com.thewizrd.common.helpers.LocationPermissionLauncher
import com.thewizrd.common.helpers.locationPermissionEnabled
import com.thewizrd.common.location.LocationResult
import com.thewizrd.common.utils.ErrorMessage
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.appLib
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.exceptions.ErrorStatus
import com.thewizrd.shared_resources.exceptions.WeatherException
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrColor
import com.thewizrd.shared_resources.utils.ContextUtils.getOrientation
import com.thewizrd.shared_resources.utils.ContextUtils.isLargeTablet
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.UserThemeMode
import com.thewizrd.shared_resources.weatherdata.model.LocationType
import com.thewizrd.simpleweather.BuildConfig
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.adapters.DetailsItemGridAdapter
import com.thewizrd.simpleweather.adapters.HourlyForecastItemAdapter
import com.thewizrd.simpleweather.banner.Banner
import com.thewizrd.simpleweather.banner.BannerManager
import com.thewizrd.simpleweather.banner.BannerManagerInterface
import com.thewizrd.simpleweather.controls.FlowLayout
import com.thewizrd.simpleweather.controls.ObservableNestedScrollView
import com.thewizrd.simpleweather.controls.ObservableNestedScrollView.OnTouchScrollChangeListener
import com.thewizrd.simpleweather.controls.viewmodels.ForecastsNowViewModel
import com.thewizrd.simpleweather.controls.viewmodels.HourlyForecastNowViewModel
import com.thewizrd.simpleweather.databinding.*
import com.thewizrd.simpleweather.fragments.WindowColorFragment
import com.thewizrd.simpleweather.preferences.FeatureSettings
import com.thewizrd.simpleweather.radar.RadarProvider
import com.thewizrd.simpleweather.radar.RadarViewProvider
import com.thewizrd.simpleweather.services.WeatherUpdaterWorker
import com.thewizrd.simpleweather.services.WidgetUpdaterWorker
import com.thewizrd.simpleweather.services.WidgetWorker
import com.thewizrd.simpleweather.snackbar.Snackbar
import com.thewizrd.simpleweather.snackbar.SnackbarManager
import com.thewizrd.simpleweather.utils.NavigationUtils.safeNavigate
import com.thewizrd.simpleweather.viewmodels.WeatherNowViewModel
import com.thewizrd.simpleweather.weatheralerts.WeatherAlertHandler
import com.thewizrd.weather_api.weatherModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.min

class WeatherNowFragment : WindowColorFragment(), BannerManagerInterface {
    init {
        arguments = Bundle()
    }

    private lateinit var args: WeatherNowFragmentArgs

    private val wm = weatherModule.weatherManager
    private var radarViewProvider: RadarViewProvider? = null

    // Views
    private lateinit var binding: FragmentWeatherNowBinding
    private lateinit var conditionPanelBinding: WeathernowConditionPanelBinding
    private var forecastPanelBinding: WeathernowForecastgraphpanelBinding? = null
    private var hrForecastPanelBinding: WeathernowHrforecastlistpanelBinding? = null
    private var precipPanelBinding: WeathernowPrecipitationgraphpanelBinding? = null
    private var detailsContainerBinding: WeathernowDetailscontainerBinding? = null
    private var uvControlBinding: WeathernowUvcontrolBinding? = null
    private var beaufortControlBinding: WeathernowBeaufortcontrolBinding? = null
    private var aqiControlBinding: WeathernowAqicontrolBinding? = null
    private var pollenCountControlBinding: WeathernowPollencountcontrolBinding? = null
    private var moonphaseControlBinding: WeathernowMoonphasecontrolBinding? = null
    private var sunphaseControlBinding: WeathernowSunphasecontrolBinding? = null
    private var radarControlBinding: WeathernowRadarcontrolBinding? = null

    private var mBannerMgr: BannerManager? = null

    // View Models
    private val wNowViewModel: WeatherNowViewModel by activityViewModels()
    private val stateModel: WeatherNowFragmentStateModel by viewModels()
    private val forecastsView: ForecastsNowViewModel by activityViewModels()
    private val alertsView: WeatherAlertsViewModel by activityViewModels()

    // GPS location
    private lateinit var locationPermissionLauncher: LocationPermissionLauncher

    override fun createSnackManager(activity: Activity): SnackbarManager {
        val mSnackMgr = SnackbarManager(binding.root)
        mSnackMgr.setSwipeDismissEnabled(true)
        mSnackMgr.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE)
        return mSnackMgr
    }

    override fun createBannerManager(): BannerManager {
        return BannerManager(binding.listLayout)
    }

    override fun initBannerManager() {
        mBannerMgr = createBannerManager()
    }

    override fun showBanner(banner: Banner) {
        runWithView {
            if (isVisible) {
                if (mBannerMgr == null) {
                    mBannerMgr = createBannerManager()
                }
                mBannerMgr?.show(banner)
            }
        }
    }

    override fun dismissBanner() {
        runWithView { mBannerMgr?.dismiss() }
    }

    override fun unloadBannerManager() {
        dismissBanner()
        mBannerMgr = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AnalyticsLogger.logEvent("WeatherNowFragment: onCreate")

        enterTransition = MaterialFadeThrough()
        exitTransition = MaterialFadeThrough()

        args = WeatherNowFragmentArgs.fromBundle(requireArguments())

        val locationData = if (savedInstanceState?.containsKey(Constants.KEY_DATA) == true) {
            JSONParser.deserializer(
                savedInstanceState.getString(Constants.KEY_DATA),
                LocationData::class.java
            )
        } else if (args.data != null) {
            JSONParser.deserializer(args.data, LocationData::class.java)
        } else {
            null
        }

        wNowViewModel.initialize(locationData)

        locationPermissionLauncher = LocationPermissionLauncher(
            this,
            locationCallback = { granted ->
                if (granted) {
                    // permission was granted, yay!
                    // Do the task you need to do.
                    wNowViewModel.refreshWeather()
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    settingsManager.setFollowGPS(false)
                    showSnackbar(
                        Snackbar.make(
                            binding.rootView.context,
                            R.string.error_location_denied,
                            Snackbar.Duration.SHORT
                        ), null
                    )
                }
            }
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        binding = FragmentWeatherNowBinding.inflate(inflater, container, false)

        binding.viewModel = wNowViewModel
        binding.lifecycleOwner = viewLifecycleOwner

        val view = binding.root
        // Request focus away from RecyclerView
        view.isFocusableInTouchMode = true
        view.requestFocus()

        ViewGroupCompat.setTransitionGroup((view as ViewGroup), true)

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            insets.replaceSystemWindowInsets(
                insets.systemWindowInsetLeft,
                insets.systemWindowInsetTop,
                insets.systemWindowInsetRight,
                0
            )
        }

        if (binding.appBarLayout.background is ColorDrawable) {
            val materialShapeDrawable = MaterialShapeDrawable()
            materialShapeDrawable.fillColor =
                ColorStateList.valueOf((binding.appBarLayout.background as ColorDrawable).color)
            materialShapeDrawable.initializeElevationOverlay(binding.appBarLayout.context)
            ViewCompat.setBackground(binding.appBarLayout, materialShapeDrawable)
        }

        binding.scrollView.setOnScrollChangeListener(object :
            NestedScrollView.OnScrollChangeListener {
            var mShouldLift = false

            @SuppressLint("RestrictedApi")
            override fun onScrollChange(
                v: NestedScrollView,
                scrollX: Int,
                scrollY: Int,
                oldScrollX: Int,
                oldScrollY: Int
            ) {
                runWithView {
                    val offset = v.computeVerticalScrollOffset()
                    val shouldLift = offset > 0
                    if (mShouldLift != shouldLift) {
                        v.postOnAnimationDelayed(150) { updateWindowColors() }
                        mShouldLift = shouldLift
                    }

                    if (v.context.isLargeTablet()) {
                        val locationBottomY = conditionPanelBinding.locationName!!.bottom.toFloat()

                        val adj = min(offset.toFloat(), locationBottomY) / locationBottomY

                        binding.toolbar?.alpha = 1f - adj
                        binding.locationToolbar?.isVisible = offset > locationBottomY
                    }
                }
            }
        })

        binding.scrollView.setOnFlingListener(object : ObservableNestedScrollView.OnFlingListener {
            private var oldScrollY = 0
            private var startvelocityY = 0

            /*
             * Values from OverScroller class
             */
            private val DECELERATION_RATE = (ln(0.78) / ln(0.9)).toFloat()
            private val INFLEXION = 0.35f // Tension lines cross at (INFLEXION, 1)

            // Fling friction
            private val mFlingFriction = ViewConfiguration.getScrollFriction()
            private val ppi = binding.scrollView.context.resources.displayMetrics.density * 160.0f
            private val mPhysicalCoeff = (SensorManager.GRAVITY_EARTH // g (m/s^2)
                    * 39.37f // inch/meter
                    * ppi
                    * 0.84f) // look and feel tuning

            private fun getSplineDeceleration(velocity: Int): Double {
                return ln((INFLEXION * abs(velocity) / (mFlingFriction * mPhysicalCoeff)).toDouble())
            }

            private fun getSplineFlingDistance(velocity: Int): Double {
                val l = getSplineDeceleration(velocity)
                val decelMinusOne = DECELERATION_RATE - 1.0
                return mFlingFriction * mPhysicalCoeff * exp(DECELERATION_RATE / decelMinusOne * l)
            }

            /*
             * End of values from OverScroller class
             */
            override fun onFlingStarted(startScrollY: Int, velocityY: Int) {
                oldScrollY = startScrollY
                startvelocityY = velocityY
            }

            @SuppressLint("RestrictedApi")
            override fun onFlingStopped(scrollY: Int) {
                context?.let {
                    if (it.getOrientation() == Configuration.ORIENTATION_LANDSCAPE || !FeatureSettings.isBackgroundImageEnabled)
                        return

                    runWithView {
                        val condPnlHeight =
                            binding.refreshLayout.height - conditionPanelBinding.conditionPanel.height
                        val THRESHOLD = condPnlHeight / 2
                        val scrollOffset = binding.scrollView.computeVerticalScrollOffset()
                        val dY = scrollY - oldScrollY
                        var mScrollHandled = false

                        if (dY == 0) return@runWithView

                        Timber.tag("ScrollView")
                            .d("onFlingStopped: height: $condPnlHeight; offset|scrollY: $scrollOffset; prevScrollY: $oldScrollY; dY: $dY;")

                        if (dY < 0 && scrollOffset < condPnlHeight - THRESHOLD) {
                            binding.scrollView.smoothScrollTo(0, 0)
                            mScrollHandled = true
                        } else if (scrollOffset < condPnlHeight && scrollOffset >= condPnlHeight - THRESHOLD) {
                            binding.scrollView.smoothScrollTo(0, condPnlHeight)
                            mScrollHandled = true
                        } else if (dY > 0 && scrollOffset < condPnlHeight - THRESHOLD) {
                            binding.scrollView.smoothScrollTo(0, condPnlHeight)
                            mScrollHandled = true
                        }

                        if (!mScrollHandled && scrollOffset < condPnlHeight) {
                            val animDY = getSplineFlingDistance(startvelocityY).toInt()
                            val animScrollY = oldScrollY + animDY

                            Timber.tag("ScrollView")
                                .d("onFlingStopped: height: $condPnlHeight; animScrollY: $animScrollY; prevScrollY: $oldScrollY; animDY: $animDY;")

                            if (startvelocityY < 0 && animScrollY < condPnlHeight - THRESHOLD) {
                                binding.scrollView.smoothScrollTo(0, 0)
                            } else if (animScrollY < condPnlHeight && animScrollY >= condPnlHeight - THRESHOLD) {
                                binding.scrollView.smoothScrollTo(0, condPnlHeight)
                            } else if (startvelocityY > 0 && animScrollY < condPnlHeight - THRESHOLD) {
                                binding.scrollView.smoothScrollTo(0, condPnlHeight)
                            }
                        }
                    }
                }
            }
        })
        binding.scrollView.setTouchScrollListener(object : OnTouchScrollChangeListener {
            @SuppressLint("RestrictedApi")
            override fun onTouchScrollChange(scrollY: Int, oldScrollY: Int) {
                context?.let {
                    if (it.getOrientation() == Configuration.ORIENTATION_LANDSCAPE || !FeatureSettings.isBackgroundImageEnabled)
                        return

                    runWithView {
                        val condPnlHeight =
                            binding.refreshLayout.height - conditionPanelBinding.conditionPanel.height
                        val THRESHOLD = condPnlHeight / 2
                        val scrollOffset = binding.scrollView.computeVerticalScrollOffset()
                        val dY = scrollY - oldScrollY

                        if (dY == 0) return@runWithView

                        Timber.tag("ScrollView")
                            .d("onTouchScrollChange: height: $condPnlHeight; offset: $scrollOffset; scrollY: $scrollY; prevScrollY: $oldScrollY; dY: $dY")

                        if (dY < 0 && scrollY < condPnlHeight - THRESHOLD) {
                            binding.scrollView.smoothScrollTo(0, 0)
                        } else if (scrollY < condPnlHeight && scrollY >= condPnlHeight - THRESHOLD) {
                            binding.scrollView.smoothScrollTo(0, condPnlHeight)
                        } else if (dY > 0 && scrollY < condPnlHeight) {
                            binding.scrollView.smoothScrollTo(0, condPnlHeight)
                        }
                    }
                }
            }
        })

        // SwipeRefresh
        binding.progressBar.show()
        binding.refreshLayout.setProgressBackgroundColorSchemeColor(
            requireContext().getAttrColor(R.attr.colorSurface)
        )
        binding.refreshLayout.setColorSchemeColors(requireContext().getAttrColor(R.attr.colorAccent))
        binding.refreshLayout.setOnRefreshListener {
            AnalyticsLogger.logEvent("WeatherNowFragment: onRefresh")
            wNowViewModel.refreshWeather(true)
        }

        run {
            // Condition
            conditionPanelBinding =
                WeathernowConditionPanelBinding.inflate(inflater, binding.listLayout, false)
            conditionPanelBinding.alertsView = alertsView
            conditionPanelBinding.viewModel = wNowViewModel
            conditionPanelBinding.lifecycleOwner = viewLifecycleOwner

            conditionPanelBinding.bgAttribution.movementMethod = LinkMovementMethod.getInstance()

            // Alerts
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                conditionPanelBinding.alertButton.backgroundTintList = ColorStateList.valueOf(Colors.ORANGERED)
            } else {
                val drawable = conditionPanelBinding.alertButton.background.mutate()
                drawable.setColorFilter(Colors.ORANGERED, PorterDuff.Mode.SRC_IN)
                conditionPanelBinding.alertButton.background = drawable
            }

            conditionPanelBinding.alertButton.setOnClickListener { v ->
                runWithView {
                    AnalyticsLogger.logEvent("WeatherNowFragment: alerts click")
                    v.isEnabled = false
                    // Show Alert Fragment
                    val args =
                            WeatherNowFragmentDirections.actionWeatherNowFragmentToWeatherListFragment()
                                    .setWeatherListType(WeatherListType.ALERTS)
                    v.findNavController().safeNavigate(args)
                }
            }

            binding.listLayout.addView(conditionPanelBinding.root)
            conditionPanelBinding.root.updateLayoutParams<GridLayout.LayoutParams> {
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, GridLayout.CENTER)
                rowSpec = GridLayout.spec(0, GridLayout.CENTER)
            }

            conditionPanelBinding.root.viewTreeObserver.addOnGlobalLayoutListener {
                Log.d("conditionPanelBinding", "onGlobalLayout")

                val context = conditionPanelBinding.root.context
                val imageLandSize = context.dpToPx(560f).toInt()

                val height = binding.refreshLayout.measuredHeight

                val imageContainerParams =
                    conditionPanelBinding.imageViewContainer.layoutParams as MarginLayoutParams
                val conditionPanelParams =
                    conditionPanelBinding.conditionPanel.layoutParams as MarginLayoutParams

                var imageContainerHeight: Int = 0

                if (context.getOrientation() == Configuration.ORIENTATION_LANDSCAPE && height < imageLandSize) {
                    imageContainerHeight = imageLandSize
                } else if (FeatureSettings.isBackgroundImageEnabled && height > 0) {
                    imageContainerHeight =
                        height - conditionPanelBinding.conditionPanel.measuredHeight - imageContainerParams.bottomMargin - imageContainerParams.topMargin
                    if (conditionPanelBinding.alertButton.visibility != View.GONE) {
                        imageContainerHeight -= conditionPanelBinding.alertButton.measuredHeight
                    }
                    if (conditionPanelParams.topMargin < 0) {
                        imageContainerHeight += -conditionPanelParams.topMargin
                    }
                    if (context.isLargeTablet()) {
                        conditionPanelBinding.labelUpdatetime.let { uptime ->
                            imageContainerHeight -= uptime.measuredHeight
                            (uptime.layoutParams as? MarginLayoutParams)?.let { lp ->
                                imageContainerHeight -= (lp.topMargin + lp.bottomMargin)
                            }
                        }
                        conditionPanelBinding.locationLayout?.let { ll ->
                            imageContainerHeight -= ll.measuredHeight
                        }
                    }
                } else {
                    imageContainerHeight = ViewGroup.LayoutParams.WRAP_CONTENT
                }

                if (imageContainerParams.height != imageContainerHeight) {
                    conditionPanelBinding.imageViewContainer.updateLayoutParams {
                        this.height = imageContainerHeight
                    }
                }
            }
        }

        if (FeatureSettings.isForecastEnabled) {
            // Forecast
            forecastPanelBinding =
                WeathernowForecastgraphpanelBinding.inflate(inflater, binding.listLayout, false)
            forecastPanelBinding!!.forecastsView = forecastsView
            forecastPanelBinding!!.lifecycleOwner = viewLifecycleOwner

            forecastPanelBinding!!.rangebarGraphPanel.setOnClickPositionListener(object :
                RecyclerOnClickListenerInterface {
                override fun onClick(view: View, position: Int) {
                    runWithView {
                        AnalyticsLogger.logEvent("WeatherNowFragment: fcast graph click")
                        view.isEnabled = false
                        val args =
                            WeatherNowFragmentDirections.actionWeatherNowFragmentToWeatherListFragment()
                                    .setWeatherListType(WeatherListType.FORECAST)
                                    .setPosition(position)
                        view.findNavController().safeNavigate(args)
                    }
                }
            })

            binding.listLayout.addView(forecastPanelBinding!!.root)
            forecastPanelBinding!!.root.updateLayoutParams<GridLayout.LayoutParams> {
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, GridLayout.CENTER)
                rowSpec = GridLayout.spec(1, GridLayout.CENTER)
            }
        }

        if (FeatureSettings.isHourlyForecastEnabled) {
            // Hourly Forecast
            hrForecastPanelBinding =
                WeathernowHrforecastlistpanelBinding.inflate(inflater, binding.listLayout, false)
            hrForecastPanelBinding!!.forecastsView = forecastsView
            hrForecastPanelBinding!!.lifecycleOwner = viewLifecycleOwner

            // Setup RecyclerView
            val hourlyForecastItemAdapter = HourlyForecastItemAdapter(object :
                DiffUtil.ItemCallback<HourlyForecastNowViewModel>() {
                override fun areItemsTheSame(
                    oldItem: HourlyForecastNowViewModel,
                    newItem: HourlyForecastNowViewModel
                ): Boolean {
                    return ObjectsCompat.equals(oldItem.date, newItem.date)
                }

                override fun areContentsTheSame(
                    oldItem: HourlyForecastNowViewModel,
                    newItem: HourlyForecastNowViewModel
                ): Boolean {
                    return ObjectsCompat.equals(oldItem, newItem)
                }
            })

            hourlyForecastItemAdapter.onClickListener = object : RecyclerOnClickListenerInterface {
                override fun onClick(view: View, position: Int) {
                    runWithView {
                        AnalyticsLogger.logEvent("WeatherNowFragment: hrf panel click")
                        view.isEnabled = false
                        val args =
                            WeatherNowFragmentDirections.actionWeatherNowFragmentToWeatherListFragment()
                                    .setWeatherListType(WeatherListType.HOURLYFORECAST)
                                    .setPosition(position)
                        view.findNavController().safeNavigate(args)
                    }
                }
            }

            hrForecastPanelBinding!!.hourlyForecastList.adapter = hourlyForecastItemAdapter

            binding.listLayout.addView(hrForecastPanelBinding!!.root)
            hrForecastPanelBinding!!.root.updateLayoutParams<GridLayout.LayoutParams> {
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, GridLayout.CENTER)
                rowSpec = GridLayout.spec(2, GridLayout.CENTER)
            }
        }

        if (FeatureSettings.isChartsEnabled) {
            // Precipitation graph
            precipPanelBinding = WeathernowPrecipitationgraphpanelBinding.inflate(
                inflater,
                binding.listLayout,
                false
            )
            precipPanelBinding!!.forecastsView = forecastsView
            precipPanelBinding!!.lifecycleOwner = viewLifecycleOwner

            val onClickListener = object : RecyclerOnClickListenerInterface {
                override fun onClick(view: View, position: Int) {
                    runWithView {
                        AnalyticsLogger.logEvent("WeatherNowFragment: precip graph click")
                        view.isEnabled = false
                        val args =
                            WeatherNowFragmentDirections.actionWeatherNowFragmentToWeatherChartsFragment()
                        view.findNavController().safeNavigate(args)
                    }
                }
            }

            precipPanelBinding!!.minutelyPrecipGraphPanel.setDrawIconLabels(false)
            precipPanelBinding!!.precipGraphPanel.setDrawIconLabels(false)

            precipPanelBinding!!.minutelyPrecipGraphPanel.setOnClickPositionListener(onClickListener)
            precipPanelBinding!!.precipGraphPanel.setOnClickPositionListener(onClickListener)

            binding.listLayout.addView(precipPanelBinding!!.root)
            precipPanelBinding!!.root.updateLayoutParams<GridLayout.LayoutParams> {
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, GridLayout.CENTER)
                rowSpec = GridLayout.spec(3, GridLayout.CENTER)
            }
        }

        if (FeatureSettings.isDetailsEnabled) {
            detailsContainerBinding =
                WeathernowDetailscontainerBinding.inflate(inflater, binding.listLayout, false)

            // Details
            detailsContainerBinding!!.viewModel = wNowViewModel
            detailsContainerBinding!!.lifecycleOwner = viewLifecycleOwner

            detailsContainerBinding!!.detailsContainer.adapter = DetailsItemGridAdapter()

            // Disable touch events on container
            // View does not scroll
            detailsContainerBinding!!.detailsContainer.isFocusable = false
            detailsContainerBinding!!.detailsContainer.isFocusableInTouchMode = false
            detailsContainerBinding!!.detailsContainer.setOnTouchListener { v, event -> true }

            binding.listLayout.addView(detailsContainerBinding!!.root)
            detailsContainerBinding!!.root.updateLayoutParams<GridLayout.LayoutParams> {
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, GridLayout.CENTER)
                rowSpec = GridLayout.spec(4, GridLayout.CENTER)
            }
        }

        binding.detailsWrapLayout.updateLayoutParams<GridLayout.LayoutParams> {
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, GridLayout.CENTER)
            rowSpec = GridLayout.spec(5, GridLayout.CENTER)
        }

        if (FeatureSettings.isUVEnabled) {
            // UV
            uvControlBinding = WeathernowUvcontrolBinding.inflate(
                inflater,
                binding.detailsWrapLayout as ViewGroup,
                true
            )
            uvControlBinding!!.viewModel = wNowViewModel
            uvControlBinding!!.lifecycleOwner = viewLifecycleOwner

            uvControlBinding!!.uvIcon.setOnIconChangedListener(object :
                IconControl.OnIconChangedListener {
                override fun onIconChanged(view: IconControl) {
                    val wim = sharedDeps.weatherIconsManager
                    if (view.iconProvider != null) {
                        view.showAsMonochrome = wim.shouldUseMonochrome(view.iconProvider)
                    } else {
                        view.showAsMonochrome = wim.shouldUseMonochrome()
                    }
                }
            })

            val context = uvControlBinding!!.root.context
            if (context.isLargeTablet()) {
                uvControlBinding!!.root.updateLayoutParams<FlowLayout.LayoutParams> {
                    width = ConstraintLayout.LayoutParams.WRAP_CONTENT
                    itemMinimumWidth = context.resources.getDimensionPixelSize(R.dimen.details_item_min_width)
                }
            }
        }

        if (FeatureSettings.isBeaufortEnabled) {
            // Beaufort
            beaufortControlBinding = WeathernowBeaufortcontrolBinding.inflate(
                inflater,
                binding.detailsWrapLayout as ViewGroup,
                true
            )
            beaufortControlBinding!!.viewModel = wNowViewModel
            beaufortControlBinding!!.lifecycleOwner = viewLifecycleOwner

            beaufortControlBinding!!.beaufortIcon.setOnIconChangedListener(object :
                IconControl.OnIconChangedListener {
                override fun onIconChanged(view: IconControl) {
                    val wim = sharedDeps.weatherIconsManager
                    if (view.iconProvider != null) {
                        view.showAsMonochrome = wim.shouldUseMonochrome(view.iconProvider)
                    } else {
                        view.showAsMonochrome = wim.shouldUseMonochrome()
                    }
                }
            })

            val context = beaufortControlBinding!!.root.context
            if (context.isLargeTablet()) {
                beaufortControlBinding!!.root.updateLayoutParams<FlowLayout.LayoutParams> {
                    width = ConstraintLayout.LayoutParams.WRAP_CONTENT
                    itemMinimumWidth = context.resources.getDimensionPixelSize(R.dimen.details_item_min_width)
                }
            }
        }

        if (FeatureSettings.isAQIndexEnabled) {
            // Air Quality
            aqiControlBinding = WeathernowAqicontrolBinding.inflate(
                inflater,
                binding.detailsWrapLayout as ViewGroup,
                true
            )
            aqiControlBinding!!.viewModel = wNowViewModel
            aqiControlBinding!!.lifecycleOwner = viewLifecycleOwner

            aqiControlBinding!!.root.setOnClickListener { v ->
                runWithView {
                    AnalyticsLogger.logEvent("WeatherNowFragment: aqi panel click")
                    v.isEnabled = false
                    val args =
                        WeatherNowFragmentDirections.actionWeatherNowFragmentToWeatherAQIFragment()
                    view.findNavController().safeNavigate(args)
                }
            }

            val context = aqiControlBinding!!.root.context
            if (context.isLargeTablet()) {
                aqiControlBinding!!.root.updateLayoutParams<FlowLayout.LayoutParams> {
                    width = ConstraintLayout.LayoutParams.WRAP_CONTENT
                    itemMinimumWidth = context.resources.getDimensionPixelSize(R.dimen.details_item_min_width)
                }
            }
        }

        if (FeatureSettings.isPollenEnabled) {
            // Pollen
            pollenCountControlBinding = WeathernowPollencountcontrolBinding.inflate(
                inflater,
                binding.detailsWrapLayout as ViewGroup,
                true
            )
            pollenCountControlBinding!!.viewModel = wNowViewModel
            pollenCountControlBinding!!.lifecycleOwner = viewLifecycleOwner

            val context = pollenCountControlBinding!!.root.context
            if (context.isLargeTablet()) {
                pollenCountControlBinding!!.root.updateLayoutParams<FlowLayout.LayoutParams> {
                    width = ConstraintLayout.LayoutParams.WRAP_CONTENT
                    itemMinimumWidth =
                        context.resources.getDimensionPixelSize(R.dimen.details_item_min_width)
                }
            }
        }

        if (FeatureSettings.isMoonPhaseEnabled) {
            // Moon Phase
            moonphaseControlBinding = WeathernowMoonphasecontrolBinding.inflate(
                inflater,
                binding.detailsWrapLayout as ViewGroup,
                true
            )
            moonphaseControlBinding!!.viewModel = wNowViewModel
            moonphaseControlBinding!!.lifecycleOwner = viewLifecycleOwner

            val context = moonphaseControlBinding!!.root.context
            if (context.isLargeTablet()) {
                moonphaseControlBinding!!.root.updateLayoutParams<FlowLayout.LayoutParams> {
                    width = ConstraintLayout.LayoutParams.WRAP_CONTENT
                    itemMinimumWidth =
                        context.resources.getDimensionPixelSize(R.dimen.details_item_min_width)
                }
            }
        }

        if (FeatureSettings.isSunPhaseEnabled) {
            // Sun Phase
            sunphaseControlBinding =
                WeathernowSunphasecontrolBinding.inflate(inflater, binding.listLayout, false)
            sunphaseControlBinding!!.viewModel = wNowViewModel
            sunphaseControlBinding!!.lifecycleOwner = viewLifecycleOwner

            binding.listLayout.addView(sunphaseControlBinding!!.root)
            sunphaseControlBinding!!.root.updateLayoutParams<GridLayout.LayoutParams> {
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, GridLayout.CENTER)
                rowSpec = GridLayout.spec(6, GridLayout.CENTER)
            }
        }

        // Radar
        if (FeatureSettings.isRadarEnabled) {
            radarControlBinding =
                WeathernowRadarcontrolBinding.inflate(inflater, binding.listLayout, false)

            radarControlBinding!!.radarWebviewCover.setOnClickListener { v ->
                runWithView {
                    AnalyticsLogger.logEvent("WeatherNowFragment: radar view click")
                    v.isEnabled = false
                    v.findNavController()
                        .safeNavigate(
                            WeatherNowFragmentDirections.actionWeatherNowFragmentToWeatherRadarFragment(),
                            FragmentNavigator.Extras.Builder()
                                .addSharedElement(v, "radar")
                                .build()
                        )
                }
            }

            ViewCompat.setTransitionName(radarControlBinding!!.radarWebviewCover, "radar")

            /*
             * NOTE
             * Compat issue: bring container to the front
             * This is handled on API 21+ with the translationZ attribute
             */
            radarControlBinding!!.radarWebviewCover.bringToFront()

            radarControlBinding!!.viewModel = wNowViewModel
            radarControlBinding!!.lifecycleOwner = viewLifecycleOwner

            binding.listLayout.addView(radarControlBinding!!.root)
            radarControlBinding!!.root.updateLayoutParams<GridLayout.LayoutParams> {
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, GridLayout.CENTER)
                rowSpec = GridLayout.spec(7, GridLayout.CENTER)
            }

            radarViewProvider = RadarProvider.getRadarViewProvider(requireContext(), radarControlBinding!!.radarWebviewContainer).apply {
                enableInteractions(false)
                onCreateView(savedInstanceState)
            }
        }

        binding.weatherCredit.updateLayoutParams<GridLayout.LayoutParams> {
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, GridLayout.CENTER)
            rowSpec = GridLayout.spec(8, GridLayout.CENTER)
        }

        binding.panelOverlay?.updateLayoutParams<GridLayout.LayoutParams> {
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, GridLayout.CENTER)
            rowSpec = GridLayout.spec(1, 8, GridLayout.FILL, 1f)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        args = WeatherNowFragmentArgs.fromBundle(requireArguments())

        adjustConditionPanelLayout()
        adjustViewsLayout()

        binding.scrollView.viewTreeObserver.addOnPreDrawListener(object :
            ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                binding.scrollView.viewTreeObserver.removeOnPreDrawListener(this)
                binding.scrollView.postOnAnimationDelayed({
                    runWithView {
                        binding.scrollView.smoothScrollTo(
                            0,
                            stateModel.scrollViewPosition
                        )
                    }
                }, 100)
                return true
            }
        })

        alertsView.getAlerts().observe(viewLifecycleOwner, Observer {
            adjustConditionPanelLayout()
        })

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                initializeState()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            wNowViewModel.uiState.collectLatest {
                adjustConditionPanelLayout()

                if (FeatureSettings.isRadarEnabled) {
                    it.weather?.locationCoord?.let { coords ->
                        radarViewProvider?.updateCoordinates(coords, true)
                    }
                }

                if (it.noLocationAvailable) {
                    showBanner(
                        Banner.make(
                            binding.root.context,
                            R.string.prompt_location_not_set
                        ).apply {
                            setBannerIcon(R.drawable.ic_location_off_24dp)
                            setPrimaryAction(R.string.label_fab_add_location) {
                                binding.root.findNavController().safeNavigate(
                                    WeatherNowFragmentDirections.actionWeatherNowFragmentToLocationsFragment()
                                )
                            }
                        }
                    )
                } else {
                    dismissBanner()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            wNowViewModel.weather.collectLatest {
                wNowViewModel.uiState.value.locationData?.let { locationData ->
                    forecastsView.updateForecasts(locationData)
                    alertsView.updateAlerts(locationData)

                    appLib.appScope.launch(Dispatchers.Default) {
                        val context = appLib.context

                        if (settingsManager.getHomeData() == locationData) {
                            // Update widgets if they haven't been already
                            if (Duration.between(
                                    LocalDateTime.now(ZoneOffset.UTC),
                                    settingsManager.getUpdateTime()
                                ).toMinutes() > settingsManager.getRefreshInterval()
                            ) {
                                WeatherUpdaterWorker.enqueueAction(
                                    context,
                                    WeatherUpdaterWorker.ACTION_UPDATEWEATHER
                                )
                            } else {
                                // Update widgets
                                WidgetUpdaterWorker.enqueueAction(
                                    context,
                                    WidgetUpdaterWorker.ACTION_UPDATEWIDGETS
                                )
                            }
                        } else {
                            // Update widgets anyway
                            WidgetWorker.enqueueRefreshWidgets(context, locationData)
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            wNowViewModel.alerts.collectLatest { weatherAlerts ->
                val locationData = wNowViewModel.uiState.value.locationData

                if (wm.supportsAlerts() && locationData != null) {
                    if (!weatherAlerts.isNullOrEmpty()) {
                        // Alerts are posted to the user here. Set them as notified.
                        appLib.appScope.launch(Dispatchers.Default) {
                            if (BuildConfig.DEBUG) {
                                WeatherAlertHandler.postAlerts(
                                    locationData,
                                    weatherAlerts
                                )
                            }
                            WeatherAlertHandler.setAsNotified(locationData, weatherAlerts)
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            wNowViewModel.imageData.collectLatest {
                val backgroundUri = it?.imageURI
                val imageView = conditionPanelBinding.imageView ?: binding.imageView

                if (imageView != null) {
                    if (FeatureSettings.isBackgroundImageEnabled) {
                        loadBackgroundImage(backgroundUri)
                    } else {
                        binding.refreshLayout.isRefreshing = false
                        binding.progressBar.hide()
                        binding.scrollView.visibility = View.VISIBLE
                    }
                }

                if (it != null) {
                    binding.listLayout.background?.alpha = 217 // 0.85% alpha
                } else {
                    binding.listLayout.background?.alpha = 255
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            wNowViewModel.errorMessages.collect {
                val error = it.firstOrNull()

                if (error != null) {
                    onErrorMessage(error)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        radarViewProvider?.onStart()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsLogger.logEvent("WeatherNowFragment: onResume")

        if (!isHidden) {
            initBannerManager()
        } else {
            dismissBanner()
        }

        radarViewProvider?.onResume()
    }

    override fun onPause() {
        AnalyticsLogger.logEvent("WeatherNowFragment: onPause")

        radarViewProvider?.onPause()

        unloadBannerManager()
        super.onPause()
    }

    override fun onStop() {
        radarViewProvider?.onStop()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        radarViewProvider?.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    @SuppressLint("RestrictedApi")
    override fun onDestroyView() {
        radarViewProvider?.onDestroyView()
        radarViewProvider = null

        stateModel.scrollViewPosition = binding.scrollView.computeVerticalScrollOffset()

        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onDetach() {
        super.onDetach()
    }

    override fun onLowMemory() {
        super.onLowMemory()

        radarViewProvider?.onLowMemory()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        binding.refreshLayout.setProgressBackgroundColorSchemeColor(
            requireContext().getAttrColor(R.attr.colorSurface)
        )
        binding.refreshLayout.setColorSchemeColors(requireContext().getAttrColor(R.attr.colorAccent))

        // Resize necessary views
        adjustConditionPanelLayout()
        adjustViewsLayout()

        loadBackgroundImage(skipCache = true, forceReload = true)

        // Reload Webview
        radarViewProvider?.onConfigurationChanged()
    }

    private fun onErrorMessage(error: ErrorMessage) {
        when (error) {
            is ErrorMessage.Resource -> {
                context?.let {
                    showSnackbar(Snackbar.make(it, error.stringId, Snackbar.Duration.SHORT))
                }
            }
            is ErrorMessage.String -> {
                context?.let {
                    showSnackbar(Snackbar.make(it, error.message, Snackbar.Duration.SHORT))
                }
            }
            is ErrorMessage.WeatherError -> {
                onWeatherError(error.exception)
            }
        }

        wNowViewModel.setErrorMessageShown(error)
    }

    private fun onWeatherError(wEx: WeatherException) {
        when (wEx.errorStatus) {
            ErrorStatus.NETWORKERROR, ErrorStatus.NOWEATHER -> {
                // Show error message and prompt to refresh
                showSnackbar(
                    Snackbar.make(
                        binding.root.context,
                        wEx.message,
                        Snackbar.Duration.LONG
                    ).apply {
                        setAction(R.string.action_retry) {
                            wNowViewModel.refreshWeather(false)
                        }
                    })
            }
            ErrorStatus.QUERYNOTFOUND -> {
                showSnackbar(
                    Snackbar.make(binding.root.context, wEx.message, Snackbar.Duration.LONG)
                )
            }
            else -> {
                // Show error message
                showSnackbar(
                    Snackbar.make(binding.root.context, wEx.message, Snackbar.Duration.LONG)
                )
            }
        }
    }

    private fun loadBackgroundImage(
        imageURI: String? = wNowViewModel.imageData.value?.imageURI,
        skipCache: Boolean = false,
        forceReload: Boolean = false
    ) {
        val imageView = conditionPanelBinding.imageView ?: binding.imageView ?: return

        runWithView {
            // Reload background image
            if (FeatureSettings.isBackgroundImageEnabled) {
                if (forceReload || (!ObjectsCompat.equals(
                        imageView.tag,
                        imageURI
                    ) || imageView.getTag(R.id.glide_custom_view_target_tag) == null)
                ) {
                    imageView.tag = imageURI
                    if (!imageURI.isNullOrBlank()) {
                        Glide.with(this@WeatherNowFragment)
                            .asBitmap()
                            .load(imageURI)
                            //.override(Target.SIZE_ORIGINAL)
                            .apply(
                                RequestOptions.centerCropTransform()
                                    .format(DecodeFormat.PREFER_ARGB_8888)
                                    .skipMemoryCache(skipCache)
                                    .disallowHardwareConfig()
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                            )
                            .transition(BitmapTransitionOptions.withCrossFade())
                            .addListener(object : RequestListener<Bitmap?> {
                                override fun onLoadFailed(
                                    e: GlideException?, model: Any,
                                    target: Target<Bitmap?>,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    binding.refreshLayout.isRefreshing = false
                                    binding.progressBar.hide()
                                    binding.scrollView.visibility = View.VISIBLE
                                    return false
                                }

                                override fun onResourceReady(
                                    resource: Bitmap?, model: Any,
                                    target: Target<Bitmap?>,
                                    dataSource: DataSource,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    binding.refreshLayout.postOnAnimation {
                                        binding.refreshLayout.isRefreshing = false
                                        binding.progressBar.hide()
                                        binding.scrollView.visibility = View.VISIBLE
                                    }
                                    return false
                                }
                            })
                            .into(imageView)
                    } else {
                        Glide.with(this@WeatherNowFragment).clear(imageView)
                        imageView.tag = null
                    }
                }
            } else {
                Glide.with(this@WeatherNowFragment).clear(imageView)
                imageView.tag = null
            }
        }
    }

    private suspend fun verifyLocationData(): LocationResult = withContext(Dispatchers.IO) {
        var locationData = wNowViewModel.uiState.value.locationData
        var locationChanged = false

        // Check if current location still exists (is valid)
        if (locationData?.locationType == LocationType.SEARCH) {
            if (settingsManager.getLocation(locationData.query) == null) {
                locationData = null
                locationChanged = true
            }
        }

        // Load new favorite location if argument data is present
        if (args.home) {
            // Check if home location changed
            // For ex. due to GPS setting change
            val homeData = settingsManager.getHomeData()
            if (!ObjectsCompat.equals(locationData, homeData)) {
                locationData = homeData
                locationChanged = true
            }
        } else if (args.data != null) {
            val location = withContext(Dispatchers.IO) {
                JSONParser.deserializer<LocationData>(args.data)
            }

            if (!ObjectsCompat.equals(location, locationData)) {
                locationData = location
                locationChanged = true
            }
        }

        if (locationChanged) {
            if (locationData != null) {
                LocationResult.Changed(locationData)
            } else {
                LocationResult.ChangedInvalid(null)
            }
        } else {
            LocationResult.NotChanged(locationData)
        }
    }

    private suspend fun initializeState() {
        val result = verifyLocationData()

        result.data?.let {
            if (it.locationType == LocationType.GPS && settingsManager.useFollowGPS()) {
                context?.run {
                    if (!locationPermissionEnabled()) {
                        locationPermissionLauncher.requestLocationPermission()
                    }
                }
            }
        }

        if (result is LocationResult.Changed || result is LocationResult.ChangedInvalid) {
            // Reset position
            withContext(Dispatchers.Main.immediate) {
                stateModel.scrollViewPosition = 0
                binding.scrollView.smoothScrollTo(0, 0)
                binding.progressBar.show()
            }

            wNowViewModel.initialize(result.data)
        } else {
            wNowViewModel.refreshWeather()
        }
    }

    private fun setMaxWidthForView(view: View) {
        if (view.context.isLargeTablet()) {
            val maxWidth = view.context.resources.getDimensionPixelSize(R.dimen.wnow_max_view_width)
            if (view.measuredWidth > maxWidth) {
                view.updateLayoutParams {
                    width = maxWidth
                }
            } else if (view.visibility != View.VISIBLE) {
                view.doOnNextLayout {
                    setMaxWidthForView(view)
                }
            }
        }
    }

    private fun adjustConditionPanelLayout() {
        conditionPanelBinding.root.doOnPreDraw {
            setMaxWidthForView(it)
        }
    }

    private fun adjustViewsLayout() {
        forecastPanelBinding?.root?.doOnPreDraw {
            setMaxWidthForView(it)
        }

        hrForecastPanelBinding?.root?.doOnPreDraw {
            setMaxWidthForView(it)
        }

        precipPanelBinding?.root?.doOnPreDraw {
            setMaxWidthForView(it)
        }

        detailsContainerBinding?.root?.doOnPreDraw {
            setMaxWidthForView(it)
        }

        binding.detailsWrapLayout.doOnPreDraw {
            setMaxWidthForView(it)
        }

        sunphaseControlBinding?.root?.doOnPreDraw {
            setMaxWidthForView(it)
        }

        radarControlBinding?.root?.doOnPreDraw {
            setMaxWidthForView(it)
        }
    }

    override fun updateWindowColors() {
        context?.let {
            var backgroundColor = it.getAttrColor(android.R.attr.colorBackground)
            var navBarColor = it.getAttrColor(R.attr.colorSurface)
            var statusBarColor = navBarColor
            if (settingsManager.getUserThemeMode() == UserThemeMode.AMOLED_DARK) {
                backgroundColor = Colors.BLACK
                navBarColor = Colors.BLACK
                statusBarColor = Colors.BLACK
            }

            binding.rootView.setBackgroundColor(backgroundColor)
            if (binding.appBarLayout.background is MaterialShapeDrawable) {
                val materialShapeDrawable = binding.appBarLayout.background as MaterialShapeDrawable
                materialShapeDrawable.fillColor = ColorStateList.valueOf(statusBarColor)
            } else {
                binding.appBarLayout.setBackgroundColor(statusBarColor)
            }
        }
    }

    class WeatherNowFragmentStateModel(private val state: SavedStateHandle) : ViewModel() {
        var scrollViewPosition: Int
            get() {
                return state["scrollViewPosition"] ?: 0
            }
            set(value) {
                state["scrollViewPosition"] = value
            }
    }
}