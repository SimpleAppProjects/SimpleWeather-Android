package com.thewizrd.simpleweather.main

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.*
import android.view.ViewGroup.MarginLayoutParams
import android.widget.GridLayout
import android.widget.GridView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.util.ObjectsCompat
import androidx.core.view.*
import androidx.core.widget.NestedScrollView
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingComponent
import androidx.databinding.DataBindingUtil
import androidx.databinding.Observable
import androidx.databinding.Observable.OnPropertyChangedCallback
import androidx.databinding.library.baseAdapters.BR
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
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
import com.ibm.icu.util.ULocale
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.controls.*
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface
import com.thewizrd.shared_resources.helpers.locationPermissionEnabled
import com.thewizrd.shared_resources.helpers.requestLocationPermission
import com.thewizrd.shared_resources.icons.WeatherIconsManager
import com.thewizrd.shared_resources.location.LocationProvider
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.tzdb.TZDBCache
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrColor
import com.thewizrd.shared_resources.utils.ContextUtils.getOrientation
import com.thewizrd.shared_resources.utils.ContextUtils.isLargeTablet
import com.thewizrd.shared_resources.utils.StringUtils.removeNonDigitChars
import com.thewizrd.shared_resources.utils.Units.TemperatureUnits
import com.thewizrd.shared_resources.weatherdata.WeatherDataLoader
import com.thewizrd.shared_resources.weatherdata.WeatherManager
import com.thewizrd.shared_resources.weatherdata.WeatherRequest
import com.thewizrd.shared_resources.weatherdata.WeatherRequest.WeatherErrorListener
import com.thewizrd.shared_resources.weatherdata.model.LocationType
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.App
import com.thewizrd.simpleweather.BuildConfig
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.adapters.DetailsItemAdapter
import com.thewizrd.simpleweather.adapters.DetailsItemGridAdapter
import com.thewizrd.simpleweather.adapters.HourlyForecastItemAdapter
import com.thewizrd.simpleweather.banner.Banner
import com.thewizrd.simpleweather.banner.BannerManager
import com.thewizrd.simpleweather.banner.BannerManagerInterface
import com.thewizrd.simpleweather.controls.FlowLayout
import com.thewizrd.simpleweather.controls.ImageDataViewModel
import com.thewizrd.simpleweather.controls.ObservableNestedScrollView
import com.thewizrd.simpleweather.controls.ObservableNestedScrollView.OnTouchScrollChangeListener
import com.thewizrd.simpleweather.controls.SunPhaseView
import com.thewizrd.simpleweather.controls.viewmodels.ForecastsNowViewModel
import com.thewizrd.simpleweather.controls.viewmodels.HourlyForecastNowViewModel
import com.thewizrd.simpleweather.databinding.*
import com.thewizrd.simpleweather.fragments.WindowColorFragment
import com.thewizrd.simpleweather.images.getImageData
import com.thewizrd.simpleweather.preferences.FeatureSettings
import com.thewizrd.simpleweather.radar.RadarProvider
import com.thewizrd.simpleweather.radar.RadarViewProvider
import com.thewizrd.simpleweather.services.WeatherUpdaterWorker
import com.thewizrd.simpleweather.services.WidgetUpdaterWorker
import com.thewizrd.simpleweather.services.WidgetWorker
import com.thewizrd.simpleweather.snackbar.Snackbar
import com.thewizrd.simpleweather.snackbar.SnackbarManager
import com.thewizrd.simpleweather.utils.NavigationUtils.safeNavigate
import com.thewizrd.simpleweather.weatheralerts.WeatherAlertHandler
import kotlinx.coroutines.*
import timber.log.Timber
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.coroutines.coroutineContext
import kotlin.math.min

class WeatherNowFragment : WindowColorFragment(), WeatherErrorListener, BannerManagerInterface {
    companion object {
        private const val PERMISSION_LOCATION_REQUEST_CODE = 0
    }

    init {
        arguments = Bundle()
    }

    private lateinit var args: WeatherNowFragmentArgs

    private val wm = WeatherManager.instance
    private var wLoader: WeatherDataLoader? = null
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
    private val dataBindingComponent = WeatherFragmentDataBindingComponent(this)

    private var mBannerMgr: BannerManager? = null

    // Data
    private var locationData: LocationData? = null
    private lateinit var weatherLiveData: MutableLiveData<Weather>

    // View Models
    private val wNowViewModel: WeatherNowFragmentStateModel by viewModels()
    private val weatherView: WeatherNowViewModel by activityViewModels()
    private val forecastsView: ForecastsNowViewModel by activityViewModels()
    private val alertsView: WeatherAlertsViewModel by activityViewModels()
    private val imageData = MutableLiveData<ImageDataViewModel?>()

    // GPS location
    private var mLocation: Location? = null
    private lateinit var locationProvider: LocationProvider
    private lateinit var locationCallback: LocationProvider.Callback

    /**
     * Tracks the status of the location updates request.
     */
    private var mRequestingLocationUpdates = false

    private val weatherObserver = Observer<Weather> { weather ->
        if (weather != null && weather.isValid) {
            weatherView.updateView(weather)

            wNowViewModel.isGPSLocation.postValue(locationData?.locationType == LocationType.GPS)

            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                if (FeatureSettings.isBackgroundImageEnabled()) {
                    imageData.postValue(withContext(Dispatchers.Default) {
                        weather.getImageData()
                    })
                } else {
                    imageData.postValue(null)
                }

                launch(Dispatchers.Main) {
                    val backgroundUri = imageData.value?.imageURI
                    val imageView = conditionPanelBinding.imageView ?: binding.imageView

                    if (imageView != null) {
                        if (FeatureSettings.isBackgroundImageEnabled() && (!ObjectsCompat.equals(
                                imageView.tag,
                                backgroundUri
                            ) || imageView.getTag(R.id.glide_custom_view_target_tag) == null)
                        ) {
                            loadBackgroundImage(backgroundUri, false)
                        } else {
                            binding.refreshLayout.isRefreshing = false
                            binding.progressBar.hide()
                            binding.scrollView.visibility = View.VISIBLE
                        }
                    }

                    radarViewProvider?.updateCoordinates(weatherView.locationCoord, true)
                }
            }

            if (locationData != null) {
                forecastsView.updateForecasts(locationData!!)

                GlobalScope.launch(Dispatchers.Default) {
                    val context = App.instance.appContext

                    if (getSettingsManager().getHomeData() == locationData) {
                        // Update widgets if they haven't been already
                        if (Duration.between(
                                LocalDateTime.now(ZoneOffset.UTC),
                                getSettingsManager().getUpdateTime()
                            ).toMinutes() > getSettingsManager().getRefreshInterval()
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
                        locationData?.let {
                            WidgetWorker.enqueueRefreshWidgets(context, it)
                        }
                    }
                }
            }
        }
    }

    private val alertsObserver = Observer<List<WeatherAlertViewModel>> { data ->
        if (data?.isNotEmpty() == true) {
            if (conditionPanelBinding.alertButton.visibility != View.VISIBLE) {
                conditionPanelBinding.alertButton.visibility = View.VISIBLE
            }
        }
        adjustConditionPanelLayout()
    }

    override fun onWeatherError(wEx: WeatherException) {
        runWithView {
            when (wEx.errorStatus) {
                ErrorStatus.NETWORKERROR, ErrorStatus.NOWEATHER -> {
                    // Show error message and prompt to refresh
                    val snackBar = Snackbar.make(wEx.message, Snackbar.Duration.LONG)
                    snackBar.setAction(R.string.action_retry) {
                        binding.refreshLayout.isRefreshing = true
                        refreshWeather(false)
                    }
                    showSnackbar(snackBar, null)
                }
                ErrorStatus.QUERYNOTFOUND -> {
                    if (!wm.isRegionSupported(locationData!!.countryCode)) {
                        showSnackbar(
                            Snackbar.make(
                                R.string.error_message_weather_region_unsupported,
                                Snackbar.Duration.LONG
                            ), null
                        )
                        return@runWithView
                    }
                    showSnackbar(Snackbar.make(wEx.message, Snackbar.Duration.LONG), null)
                }
                else -> {
                    // Show error message
                    showSnackbar(Snackbar.make(wEx.message, Snackbar.Duration.LONG), null)
                }
            }

            binding.refreshLayout.isRefreshing = false
            binding.progressBar.hide()
        }
    }

    override fun createSnackManager(): SnackbarManager {
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
            if (appCompatActivity != null && isVisible) {
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

        if (savedInstanceState?.containsKey(Constants.KEY_DATA) == true) {
            locationData = JSONParser.deserializer(savedInstanceState.getString(Constants.KEY_DATA), LocationData::class.java)
        } else if (args.data != null) {
            locationData = JSONParser.deserializer(args.data, LocationData::class.java)
        }

        locationProvider = LocationProvider(appCompatActivity!!)
        locationCallback = object : LocationProvider.Callback {
            override fun onLocationChanged(location: Location?) {
                stopLocationUpdates()

                runWithView {
                    if (getSettingsManager().useFollowGPS() && updateLocation()) {
                        // Setup loader from updated location
                        wLoader = WeatherDataLoader(locationData!!)

                        refreshWeather(false)
                    }
                }
            }

            override fun onRequestTimedOut() {
                stopLocationUpdates()
            }
        }
        mRequestingLocationUpdates = false

        // Live Data
        weatherLiveData = MutableLiveData()
        weatherLiveData.observe(this, weatherObserver)

        alertsView.getAlerts()?.observe(this, alertsObserver)

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            private var wasStarted = false

            override fun onStart(owner: LifecycleOwner) {
                super.onStart(owner)
                resume()
                wasStarted = true
            }

            override fun onResume(owner: LifecycleOwner) {
                super.onResume(owner)
                if (!wasStarted) onStart()
            }

            override fun onPause(owner: LifecycleOwner) {
                super.onPause(owner)
                wasStarted = false
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_weather_now, container, false,
            dataBindingComponent
        )

        binding.weatherNowState = wNowViewModel
        binding.weatherView = weatherView
        binding.lifecycleOwner = viewLifecycleOwner

        imageData.observe(viewLifecycleOwner, Observer {
            binding.imageData = it

            if (it != null) {
                binding.listLayout.background?.alpha = 217 // 0.85% alpha
            } else {
                binding.listLayout.background?.alpha = 255
            }
        })

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
            private val DECELERATION_RATE = (Math.log(0.78) / Math.log(0.9)).toFloat()
            private val INFLEXION = 0.35f // Tension lines cross at (INFLEXION, 1)

            // Fling friction
            private val mFlingFriction = ViewConfiguration.getScrollFriction()
            private val ppi = appCompatActivity!!.resources.displayMetrics.density * 160.0f
            private val mPhysicalCoeff = (SensorManager.GRAVITY_EARTH // g (m/s^2)
                    * 39.37f // inch/meter
                    * ppi
                    * 0.84f) // look and feel tuning

            private fun getSplineDeceleration(velocity: Int): Double {
                return Math.log((INFLEXION * Math.abs(velocity) / (mFlingFriction * mPhysicalCoeff)).toDouble())
            }

            private fun getSplineFlingDistance(velocity: Int): Double {
                val l = getSplineDeceleration(velocity)
                val decelMinusOne = DECELERATION_RATE - 1.0
                return mFlingFriction * mPhysicalCoeff * Math.exp(DECELERATION_RATE / decelMinusOne * l)
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
                if (appCompatActivity == null || appCompatActivity!!.getOrientation() == Configuration.ORIENTATION_LANDSCAPE || !FeatureSettings.isBackgroundImageEnabled())
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
        })
        binding.scrollView.setTouchScrollListener(object : OnTouchScrollChangeListener {
            @SuppressLint("RestrictedApi")
            override fun onTouchScrollChange(scrollY: Int, oldScrollY: Int) {
                if (appCompatActivity!!.getOrientation() == Configuration.ORIENTATION_LANDSCAPE || !FeatureSettings.isBackgroundImageEnabled())
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
        })

        // SwipeRefresh
        binding.progressBar.show()
        binding.refreshLayout.setProgressBackgroundColorSchemeColor(
            appCompatActivity!!.getAttrColor(
                R.attr.colorSurface
            )
        )
        binding.refreshLayout.setColorSchemeColors(appCompatActivity!!.getAttrColor(R.attr.colorAccent))
        binding.refreshLayout.setOnRefreshListener {
            AnalyticsLogger.logEvent("WeatherNowFragment: onRefresh")

            runWithView {
                if (getSettingsManager().useFollowGPS() && updateLocation()) {
                    // Setup loader from updated location
                    wLoader = WeatherDataLoader(locationData!!)
                }

                refreshWeather(true)
            }
        }

        run {
            // Condition
            conditionPanelBinding = DataBindingUtil.inflate(
                    inflater,
                    R.layout.weathernow_condition_panel,
                    binding.listLayout,
                    false,
                    dataBindingComponent
            )
            conditionPanelBinding.alertsView = alertsView
            conditionPanelBinding.weatherNowState = wNowViewModel
            conditionPanelBinding.weatherView = weatherView
            conditionPanelBinding.lifecycleOwner = viewLifecycleOwner

            conditionPanelBinding.bgAttribution.movementMethod = LinkMovementMethod.getInstance()

            imageData.observe(viewLifecycleOwner, Observer {
                conditionPanelBinding.imageData = it
            })

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
                                    .setData(JSONParser.serializer(locationData, LocationData::class.java))
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
                } else if (FeatureSettings.isBackgroundImageEnabled() && height > 0) {
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

        if (FeatureSettings.isForecastEnabled()) {
            // Forecast
            forecastPanelBinding = DataBindingUtil.inflate(
                inflater,
                R.layout.weathernow_forecastgraphpanel,
                binding.listLayout,
                false,
                dataBindingComponent
            )
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
                                .setData(
                                    JSONParser.serializer(
                                        locationData,
                                            LocationData::class.java
                                    )
                                )
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

        if (FeatureSettings.isHourlyForecastEnabled()) {
            // Hourly Forecast
            hrForecastPanelBinding = DataBindingUtil.inflate(inflater, R.layout.weathernow_hrforecastlistpanel, binding.listLayout, false, dataBindingComponent)
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
                                .setData(
                                    JSONParser.serializer(
                                        locationData,
                                        LocationData::class.java
                                    )
                                )
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

        if (FeatureSettings.isChartsEnabled()) {
            // Precipitation graph
            precipPanelBinding = DataBindingUtil.inflate(
                inflater,
                R.layout.weathernow_precipitationgraphpanel,
                binding.listLayout,
                false,
                dataBindingComponent
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
                                .setData(
                                        JSONParser.serializer(
                                                locationData,
                                                LocationData::class.java
                                        )
                                )
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

        if (FeatureSettings.isDetailsEnabled()) {
            detailsContainerBinding = DataBindingUtil.inflate(inflater, R.layout.weathernow_detailscontainer, binding.listLayout, false, dataBindingComponent)

            // Details
            detailsContainerBinding!!.weatherView = weatherView
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

        if (FeatureSettings.isUVEnabled()) {
            // UV
            uvControlBinding = DataBindingUtil.inflate(
                    inflater,
                    R.layout.weathernow_uvcontrol,
                    binding.detailsWrapLayout as ViewGroup,
                    true,
                    dataBindingComponent
            )
            uvControlBinding!!.weatherView = weatherView
            uvControlBinding!!.lifecycleOwner = viewLifecycleOwner

            uvControlBinding!!.uvIcon.setOnIconChangedListener(object :
                IconControl.OnIconChangedListener {
                override fun onIconChanged(view: IconControl) {
                    val wim = WeatherIconsManager.getInstance()
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

        if (FeatureSettings.isBeaufortEnabled()) {
            // Beaufort
            beaufortControlBinding = DataBindingUtil.inflate(
                    inflater,
                    R.layout.weathernow_beaufortcontrol,
                    binding.detailsWrapLayout as ViewGroup,
                    true,
                    dataBindingComponent
            )
            beaufortControlBinding!!.weatherView = weatherView
            beaufortControlBinding!!.lifecycleOwner = viewLifecycleOwner

            beaufortControlBinding!!.beaufortIcon.setOnIconChangedListener(object :
                IconControl.OnIconChangedListener {
                override fun onIconChanged(view: IconControl) {
                    val wim = WeatherIconsManager.getInstance()
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

        if (FeatureSettings.isAQIndexEnabled()) {
            // Air Quality
            aqiControlBinding = DataBindingUtil.inflate(inflater, R.layout.weathernow_aqicontrol, binding.detailsWrapLayout as ViewGroup, true, dataBindingComponent)
            aqiControlBinding!!.weatherView = weatherView
            aqiControlBinding!!.lifecycleOwner = viewLifecycleOwner

            aqiControlBinding!!.root.setOnClickListener { v ->
                runWithView {
                    AnalyticsLogger.logEvent("WeatherNowFragment: aqi panel click")
                    v.isEnabled = false
                    val args = WeatherNowFragmentDirections.actionWeatherNowFragmentToWeatherAQIFragment()
                            .setData(JSONParser.serializer(locationData, LocationData::class.java))
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

        // TODO: add to FeatureSettings
        run {
            // Pollen
            pollenCountControlBinding = DataBindingUtil.inflate(inflater, R.layout.weathernow_pollencountcontrol, binding.detailsWrapLayout as ViewGroup, true, dataBindingComponent)
            pollenCountControlBinding!!.weatherView = weatherView
            pollenCountControlBinding!!.lifecycleOwner = viewLifecycleOwner

            val context = pollenCountControlBinding!!.root.context
            if (context.isLargeTablet()) {
                pollenCountControlBinding!!.root.updateLayoutParams<FlowLayout.LayoutParams> {
                    width = ConstraintLayout.LayoutParams.WRAP_CONTENT
                    itemMinimumWidth = context.resources.getDimensionPixelSize(R.dimen.details_item_min_width)
                }
            }
        }

        if (FeatureSettings.isMoonPhaseEnabled()) {
            // Moon Phase
            moonphaseControlBinding = DataBindingUtil.inflate(inflater, R.layout.weathernow_moonphasecontrol, binding.detailsWrapLayout as ViewGroup, true, dataBindingComponent)
            moonphaseControlBinding!!.weatherView = weatherView
            moonphaseControlBinding!!.lifecycleOwner = viewLifecycleOwner

            val context = moonphaseControlBinding!!.root.context
            if (context.isLargeTablet()) {
                moonphaseControlBinding!!.root.updateLayoutParams<FlowLayout.LayoutParams> {
                    width = ConstraintLayout.LayoutParams.WRAP_CONTENT
                    itemMinimumWidth = context.resources.getDimensionPixelSize(R.dimen.details_item_min_width)
                }
            }
        }

        if (FeatureSettings.isSunPhaseEnabled()) {
            // Sun Phase
            sunphaseControlBinding = DataBindingUtil.inflate(inflater, R.layout.weathernow_sunphasecontrol, binding.listLayout, false, dataBindingComponent)
            sunphaseControlBinding!!.weatherView = weatherView
            sunphaseControlBinding!!.lifecycleOwner = viewLifecycleOwner

            binding.listLayout.addView(sunphaseControlBinding!!.root)
            sunphaseControlBinding!!.root.updateLayoutParams<GridLayout.LayoutParams> {
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, GridLayout.CENTER)
                rowSpec = GridLayout.spec(6, GridLayout.CENTER)
            }
        }

        // Radar
        if (FeatureSettings.isRadarEnabled()) {
            radarControlBinding = DataBindingUtil.inflate(inflater, R.layout.weathernow_radarcontrol, binding.listLayout, false, dataBindingComponent)

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

            radarControlBinding!!.weatherView = weatherView
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

        // Set property change listeners
        weatherView.addOnPropertyChangedCallback(object : OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable, propertyId: Int) {
                if (propertyId == 0 || propertyId == BR.location) {
                    runWithView {
                        adjustConditionPanelLayout()
                    }
                } else if (propertyId == BR.locationCoord) {
                    // Restrict control to Kitkat+ for Chromium WebView
                    if (FeatureSettings.isRadarEnabled()) {
                        radarViewProvider?.updateCoordinates(weatherView.locationCoord, true)
                    }
                } else if (propertyId == BR.weatherSummary) {
                    runWithView {
                        adjustConditionPanelLayout()
                    }
                }
            }
        })

        binding.scrollView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                binding.scrollView.viewTreeObserver.removeOnPreDrawListener(this)
                binding.scrollView.postOnAnimationDelayed({
                    runWithView { binding.scrollView.smoothScrollTo(0, wNowViewModel.scrollViewPosition) }
                }, 100)
                return true
            }
        })

        if (radarViewProvider != null) {
            radarViewProvider!!.onViewCreated(weatherView.locationCoord)
            updateRadarView()
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

        // Remove location updates to save battery.
        stopLocationUpdates()
        unloadBannerManager()
        super.onPause()
    }

    override fun onStop() {
        radarViewProvider?.onStop()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (locationData != null) {
            outState.putString(Constants.KEY_DATA, JSONParser.serializer(locationData, LocationData::class.java))
        }
        radarViewProvider?.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    @SuppressLint("RestrictedApi")
    override fun onDestroyView() {
        radarViewProvider?.onDestroyView()
        radarViewProvider = null

        wNowViewModel.scrollViewPosition = binding.scrollView.computeVerticalScrollOffset()

        super.onDestroyView()
    }

    override fun onDestroy() {
        wLoader = null
        super.onDestroy()
    }

    override fun onDetach() {
        wLoader = null
        super.onDetach()
    }

    override fun onLowMemory() {
        super.onLowMemory()

        radarViewProvider?.onLowMemory()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        weatherView.notifyChange()

        binding.refreshLayout.setProgressBackgroundColorSchemeColor(
            appCompatActivity!!.getAttrColor(
                R.attr.colorSurface
            )
        )
        binding.refreshLayout.setColorSchemeColors(appCompatActivity!!.getAttrColor(R.attr.colorAccent))

        // Resize necessary views
        adjustConditionPanelLayout()
        adjustViewsLayout()

        val backgroundUri = imageData.value?.imageURI
        loadBackgroundImage(backgroundUri, true)

        // Reload Webview
        radarViewProvider?.onConfigurationChanged()
    }

    private fun loadBackgroundImage(imageURI: String?, skipCache: Boolean) {
        runWithView {
            val imageView =
                conditionPanelBinding.imageView ?: binding.imageView ?: return@runWithView

            // Reload background image
            if (FeatureSettings.isBackgroundImageEnabled()) {
                if (!ObjectsCompat.equals(imageView.tag, imageURI)) {
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
                        if (weatherView.isValid) {
                            binding.refreshLayout.isRefreshing = false
                            binding.progressBar.hide()
                            binding.scrollView.visibility = View.VISIBLE
                        }
                    }
                }
            } else {
                Glide.with(this@WeatherNowFragment).clear(imageView)
                imageView.tag = null
                if (weatherView.isValid) {
                    binding.refreshLayout.isRefreshing = false
                    binding.progressBar.hide()
                    binding.scrollView.visibility = View.VISIBLE
                }
            }
        }
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    @SuppressLint("MissingPermission")
    private fun stopLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            Logger.writeLine(Log.DEBUG, "WeatherNow: stopLocationUpdates: updates never requested, no-op.")
            return
        }

        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        locationProvider.stopLocationUpdates()
        mRequestingLocationUpdates = false
    }

    private suspend fun verifyLocationData(): Boolean = withContext(Dispatchers.IO) {
        var locationChanged = false

        // Check if current location still exists (is valid)
        if (locationData?.locationType == LocationType.SEARCH) {
            if (getSettingsManager().getLocation(locationData?.query) == null) {
                locationData = null
                wLoader = null
                locationChanged = true
            }
        }
        // Load new favorite location if argument data is present
        if (args.home) {
            // Check if home location changed
            // For ex. due to GPS setting change
            val homeData = getSettingsManager().getHomeData()
            if (!ObjectsCompat.equals(locationData, homeData)) {
                locationData = homeData
                locationChanged = true
            }
        } else if (args.data != null) {
            val location = withContext(Dispatchers.IO) {
                JSONParser.deserializer(args.data, LocationData::class.java)
            }

            if (!ObjectsCompat.equals(location, locationData)) {
                locationData = location
                locationChanged = true
            }
        }

        locationChanged
    }

    private fun resume() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            val locationChanged = verifyLocationData()

            if (locationChanged || wLoader == null) {
                restore()
            } else {
                // Refresh current fragment instance
                val currentLocale = ULocale.forLocale(LocaleUtils.getLocale())
                val locale = wm.localeToLangCode(currentLocale.language, currentLocale.toLanguageTag())

                // Check current weather source (API)
                // Reset if source OR locale is different
                if (getSettingsManager().getAPI() != weatherView.weatherSource
                        || wm.supportsWeatherLocale() && locale != weatherView.weatherLocale) {
                    restore()
                } else {
                    // Update weather if needed on resume
                    if (getSettingsManager().useFollowGPS() && updateLocation()) {
                        // Setup loader from updated location
                        wLoader = WeatherDataLoader(locationData!!)
                    }

                    refreshWeather(false)
                }
            }
        }
    }

    private fun restore() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            launch(Dispatchers.Main.immediate) {
                // Reset position
                wNowViewModel.scrollViewPosition = 0
                binding.scrollView.smoothScrollTo(0, 0)
                binding.progressBar.show()
            }

            supervisorScope {
                val task = async(Dispatchers.IO) {
                    var forceRefresh = false

                    // GPS Follow location
                    if (getSettingsManager().useFollowGPS() && (locationData == null || locationData!!.locationType == LocationType.GPS)) {
                        val locData = getSettingsManager().getLastGPSLocData()
                        if (locData == null) {
                            // Update location if not setup
                            updateLocation()
                            forceRefresh = true
                        } else {
                            // Reset locdata if source is different
                            if (getSettingsManager().getAPI() != locData.weatherSource) getSettingsManager().saveLastGPSLocData(LocationData())
                            if (updateLocation()) {
                                // Setup loader from updated location
                                forceRefresh = true
                            } else {
                                // Setup loader saved location data
                                locationData = locData
                            }
                        }
                    } else if (locationData == null && wLoader == null) {
                        // Weather was loaded before. Lets load it up...
                        locationData = getSettingsManager().getHomeData()
                    }

                    if (locationData?.isValid == true) {
                        wLoader = WeatherDataLoader(locationData!!)
                    } else {
                        showBanner(Banner.make(R.string.prompt_location_not_set).apply {
                            setBannerIcon(binding.root.context, R.drawable.ic_location_off_24dp)
                            setPrimaryAction(R.string.label_fab_add_location) {
                                binding.root.findNavController().safeNavigate(
                                    WeatherNowFragmentDirections.actionWeatherNowFragmentToLocationsFragment()
                                )
                            }
                        })
                        this.cancel()
                    }
                    forceRefresh
                }

                task.invokeOnCompletion {
                    val t = task.getCompletionExceptionOrNull()
                    if (t == null) {
                        refreshWeather(task.getCompleted())
                    } else {
                        runWithView {
                            binding.refreshLayout.isRefreshing = false
                            binding.progressBar.hide()
                            binding.scrollView.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
    }

    private fun refreshWeather(forceRefresh: Boolean) {
        runWithView {
            if (wLoader == null && locationData != null) {
                wLoader = WeatherDataLoader(locationData!!)
            }

            val task = launch(Dispatchers.Default) {
                supervisorScope {
                    val result = wLoader?.loadWeatherResult(
                        WeatherRequest.Builder()
                            .forceRefresh(forceRefresh)
                            .setErrorListener(this@WeatherNowFragment)
                            .build()
                    ) ?: throw CancellationException()

                    weatherLiveData.postValue(result.weather)

                    runWithView {
                        if (conditionPanelBinding.alertButton.visibility != View.GONE) {
                            conditionPanelBinding.alertButton.visibility = View.GONE
                            adjustConditionPanelLayout()
                        }
                    }

                    val weatherAlerts = wLoader?.loadWeatherAlerts(result.isSavedData)

                    runWithView {
                        if (locationData != null) {
                            alertsView.updateAlerts(locationData!!)
                        }

                        if (wm.supportsAlerts() && locationData != null) {
                            if (!weatherAlerts.isNullOrEmpty()) {
                                // Alerts are posted to the user here. Set them as notified.
                                GlobalScope.launch(Dispatchers.Default) {
                                    if (BuildConfig.DEBUG) {
                                        WeatherAlertHandler.postAlerts(
                                            locationData!!,
                                            weatherAlerts
                                        )
                                    }
                                    WeatherAlertHandler.setAsNotified(locationData!!, weatherAlerts)
                                }
                            }
                        }
                    }
                }
            }

            task.invokeOnCompletion {
                if (it != null) {
                    runWithView {
                        binding.refreshLayout.isRefreshing = false
                        binding.progressBar.hide()
                        binding.scrollView.visibility = View.VISIBLE
                    }
                }
            }
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

        /* NOTE: are within details wrap layout
        uvControlBinding?.root?.doOnPreDraw {
            setMaxWidthForView(it)
        }

        beaufortControlBinding?.root?.doOnPreDraw {
            setMaxWidthForView(it)
        }

        aqiControlBinding?.root?.doOnPreDraw {
            setMaxWidthForView(it)
        }

        pollenCountControlBinding?.root?.doOnPreDraw {
            setMaxWidthForView(it)
        }

        moonphaseControlBinding?.root?.doOnPreDraw {
            setMaxWidthForView(it)
        }
        */

        sunphaseControlBinding?.root?.doOnPreDraw {
            setMaxWidthForView(it)
        }

        radarControlBinding?.root?.doOnPreDraw {
            setMaxWidthForView(it)
        }
    }

    override fun updateWindowColors() {
        if (appCompatActivity == null) return

        var backgroundColor = appCompatActivity!!.getAttrColor(android.R.attr.colorBackground)
        var navBarColor = appCompatActivity!!.getAttrColor(R.attr.colorSurface)
        var statusBarColor = navBarColor
        if (getSettingsManager().getUserThemeMode() == UserThemeMode.AMOLED_DARK) {
            backgroundColor = Colors.BLACK
            navBarColor = Colors.BLACK
            statusBarColor = Colors.BLACK
        }

        binding.rootView.setBackgroundColor(backgroundColor)
        if (binding.appBarLayout.background is MaterialShapeDrawable) {
            val materialShapeDrawable = binding.appBarLayout.background as MaterialShapeDrawable
            materialShapeDrawable.fillColor = ColorStateList.valueOf(navBarColor)
        } else {
            binding.appBarLayout.setBackgroundColor(navBarColor)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun updateLocation(): Boolean {
        var locationChanged = false

        if (appCompatActivity != null && getSettingsManager().useFollowGPS() && locationData?.locationType == LocationType.GPS) {
            if (!appCompatActivity!!.locationPermissionEnabled()) {
                this.requestLocationPermission(PERMISSION_LOCATION_REQUEST_CODE)
                return false
            }

            val locMan =
                appCompatActivity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager?

            if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
                locationData = getSettingsManager().getLastGPSLocData()
                return false
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
                locationProvider.requestSingleUpdate(
                    locationCallback,
                    Looper.getMainLooper(),
                    30000
                )
            }

            if (location != null && !mRequestingLocationUpdates) {
                var lastGPSLocData = getSettingsManager().getLastGPSLocData()

                // Check previous location difference
                if (lastGPSLocData?.query != null &&
                    mLocation != null && ConversionMethods.calculateGeopositionDistance(mLocation, location) < 1600) {
                    return false
                }

                if (lastGPSLocData?.query != null &&
                        Math.abs(ConversionMethods.calculateHaversine(lastGPSLocData.latitude, lastGPSLocData.longitude,
                                location.latitude, location.longitude)) < 1600) {
                    return false
                }

                val view = try {
                    withContext(Dispatchers.IO) {
                        wm.getLocation(location)
                    }
                } catch (e: WeatherException) {
                    showSnackbar(Snackbar.make(e.message, Snackbar.Duration.SHORT), null)
                    return false
                }

                if (view == null || view.locationQuery.isNullOrBlank()) {
                    // Stop since there is no valid query
                    return false
                } else if (view.locationTZLong.isNullOrBlank() && view.locationLat != 0.0 && view.locationLong != 0.0) {
                    val tzId = TZDBCache.getTimeZone(view.locationLat, view.locationLong)
                    if ("unknown" != tzId)
                        view.locationTZLong = tzId
                }

                if (!coroutineContext.isActive) return false

                // Save location as last known
                lastGPSLocData = LocationData(view, location)
                getSettingsManager().saveLastGPSLocData(lastGPSLocData)

                LocalBroadcastManager.getInstance(appCompatActivity!!)
                        .sendBroadcast(Intent(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE))

                locationData = lastGPSLocData
                mLocation = location
                locationChanged = true
            }
        }

        return locationChanged
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_LOCATION_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    // Do the task you need to do.
                    runWithView {
                        if (getSettingsManager().useFollowGPS() && updateLocation()) {
                            // Setup loader from updated location
                            wLoader = WeatherDataLoader(locationData!!)

                            refreshWeather(false)
                        }
                    }
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    getSettingsManager().setFollowGPS(false)
                    showSnackbar(Snackbar.make(R.string.error_location_denied, Snackbar.Duration.SHORT), null)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun updateRadarView() {
        runWithView {
            if (!FeatureSettings.isRadarEnabled() || radarViewProvider == null)
                return@runWithView

            radarViewProvider?.updateRadarView()
        }
    }

    class WeatherNowFragmentStateModel : ViewModel() {
        var scrollViewPosition = 0
        var isGPSLocation = MutableLiveData(false)
    }

    class WeatherFragmentDataBindingComponent(fragment: WeatherNowFragment?) : DataBindingComponent {
        private val mAdapter = WeatherNowFragmentBindingAdapter(fragment)

        override fun getWeatherNowFragmentBindingAdapter(): WeatherNowFragmentBindingAdapter {
            return mAdapter
        }
    }

    class WeatherNowFragmentBindingAdapter(private val fragment: WeatherNowFragment?) {
        @BindingAdapter("details_data")
        fun updateDetailsContainer(view: GridView, models: List<DetailItemViewModel>?) {
            if (view.adapter is DetailsItemGridAdapter) {
                (view.adapter as DetailsItemGridAdapter).updateItems(models)
            }
        }

        @BindingAdapter("details_data")
        fun updateDetailsContainer(view: RecyclerView, models: List<DetailItemViewModel>?) {
            if (view.adapter is DetailsItemAdapter) {
                (view.adapter as DetailsItemAdapter).submitList(models)
            }
        }

        @BindingAdapter("forecastData")
        fun updateHrForecastView(view: RecyclerView, forecasts: List<HourlyForecastNowViewModel>?) {
            if (view.adapter is HourlyForecastItemAdapter) {
                (view.adapter as HourlyForecastItemAdapter).submitList(forecasts)
            }
        }

        @BindingAdapter("sunPhase")
        fun updateSunPhasePanel(view: SunPhaseView, sunPhase: SunPhaseViewModel?) {
            if (sunPhase?.sunriseTime != null && sunPhase.sunsetTime != null && fragment?.locationData != null) {
                view.setSunriseSetTimes(
                    sunPhase.sunriseTime, sunPhase.sunsetTime,
                    fragment.locationData?.tzOffset ?: ZoneOffset.UTC
                )
            }
        }

        @BindingAdapter("imageData")
        fun getBackgroundAttribution(view: TextView, imageData: ImageDataViewModel?) {
            if (imageData?.originalLink?.isNotBlank() == true) {
                val text = SpannableString(String.format("%s %s (%s)",
                        view.context.getString(R.string.attrib_prefix), imageData.artistName, imageData.siteName))
                text.setSpan(UnderlineSpan(), 0, text.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                view.text = text
                view.setOnClickListener {
                    val i = Intent(Intent.ACTION_VIEW, Uri.parse(imageData.originalLink))
                    if (i.resolveActivity(view.context.packageManager) != null) {
                        runCatching {
                            view.context.startActivity(i)
                        }.onFailure {
                            // NOTE: possible exceptions: SecurityException, ActivityNotFoundException
                            Logger.writeLine(Log.ERROR, it, "Error opening attribution link")
                        }
                    }
                }
            } else {
                view.text = ""
                view.setOnClickListener(null)
            }
        }

        @BindingAdapter(value = ["tempTextColor", "tempUnit"], requireAll = false)
        fun tempTextColor(view: TextView, temp: CharSequence?, @TemperatureUnits tempUnit: String?) {
            val temp_str = temp?.removeNonDigitChars()?.toString()
            var temp_f = temp_str?.toFloatOrNull()
            if (temp_f != null) {
                if (ObjectsCompat.equals(
                        tempUnit,
                        Units.CELSIUS
                    ) || temp?.endsWith(Units.CELSIUS) == true
                ) {
                    temp_f = ConversionMethods.CtoF(temp_f)
                }

                view.setTextColor(getColorFromTempF(temp_f))
            } else {
                view.setTextColor(ContextCompat.getColor(view.context, R.color.colorTextPrimary))
            }
        }
    }
}