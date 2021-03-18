package com.thewizrd.simpleweather.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.hardware.SensorManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.*
import android.view.ViewGroup.MarginLayoutParams
import android.widget.GridView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.util.ObjectsCompat
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.ViewGroupCompat
import androidx.core.view.WindowInsetsCompat
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
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Tasks
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.transition.MaterialFadeThrough
import com.ibm.icu.util.ULocale
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.controls.*
import com.thewizrd.shared_resources.helpers.ContextUtils
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.tzdb.TZDBCache
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.utils.Units.TemperatureUnits
import com.thewizrd.shared_resources.utils.WeatherUtils.ErrorStatus
import com.thewizrd.shared_resources.wearable.WearableHelper
import com.thewizrd.shared_resources.weatherdata.*
import com.thewizrd.shared_resources.weatherdata.WeatherRequest.WeatherErrorListener
import com.thewizrd.simpleweather.App.Companion.instance
import com.thewizrd.simpleweather.BuildConfig
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.adapters.DetailsItemGridAdapter
import com.thewizrd.simpleweather.adapters.HourlyForecastItemAdapter
import com.thewizrd.simpleweather.controls.ObservableNestedScrollView
import com.thewizrd.simpleweather.controls.ObservableNestedScrollView.OnTouchScrollChangeListener
import com.thewizrd.simpleweather.controls.SunPhaseView
import com.thewizrd.simpleweather.controls.viewmodels.ForecastsNowViewModel
import com.thewizrd.simpleweather.controls.viewmodels.HourlyForecastNowViewModel
import com.thewizrd.simpleweather.databinding.*
import com.thewizrd.simpleweather.fragments.WindowColorFragment
import com.thewizrd.simpleweather.preferences.FeatureSettings
import com.thewizrd.simpleweather.radar.RadarProvider
import com.thewizrd.simpleweather.radar.RadarViewProvider
import com.thewizrd.simpleweather.services.WeatherUpdaterService
import com.thewizrd.simpleweather.services.WeatherUpdaterWorker
import com.thewizrd.simpleweather.services.WidgetUpdaterWorker
import com.thewizrd.simpleweather.snackbar.Snackbar
import com.thewizrd.simpleweather.snackbar.SnackbarManager
import com.thewizrd.simpleweather.weatheralerts.WeatherAlertHandler
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.lang.Runnable
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import kotlin.coroutines.coroutineContext

class WeatherNowFragment : WindowColorFragment(), WeatherErrorListener {
    companion object {
        private const val PERMISSION_LOCATION_REQUEST_CODE = 0
    }

    init {
        arguments = Bundle()
    }

    private lateinit var args: WeatherNowFragmentArgs

    private val wm = WeatherManager.getInstance()
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
    private var moonphaseControlBinding: WeathernowMoonphasecontrolBinding? = null
    private var sunphaseControlBinding: WeathernowSunphasecontrolBinding? = null
    private var radarControlBinding: WeathernowRadarcontrolBinding? = null
    private val dataBindingComponent = WeatherFragmentDataBindingComponent(this)

    // Data
    private var locationData: LocationData? = null
    private lateinit var weatherLiveData: MutableLiveData<Weather>

    // View Models
    private val wNowViewModel: WeatherNowFragmentStateModel by viewModels()
    private val weatherView: WeatherNowViewModel by activityViewModels()
    private val forecastsView: ForecastsNowViewModel by activityViewModels()
    private val alertsView: WeatherAlertsViewModel by activityViewModels()

    // GPS location
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private var mLocation: Location? = null
    private var mLocCallback: LocationCallback? = null
    private var mLocListnr: LocationListener? = null

    /**
     * Tracks the status of the location updates request.
     */
    private var mRequestingLocationUpdates = false
    private val mMainHandler = Handler(Looper.getMainLooper())

    private val weatherObserver = Observer<Weather> { weather ->
        if (weather != null && weather.isValid) {
            weatherView.updateView(weather)

            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                withContext(Dispatchers.Default) {
                    weatherView.updateBackground()
                }

                launch(Dispatchers.Main) {
                    val backgroundUri = weatherView.imageData?.imageURI
                    if (FeatureSettings.isBackgroundImageEnabled() && (!ObjectsCompat.equals(conditionPanelBinding.imageView.tag, backgroundUri) || conditionPanelBinding.imageView.getTag(R.id.glide_custom_view_target_tag) == null)) {
                        loadBackgroundImage(backgroundUri, false)
                    } else {
                        binding.refreshLayout.isRefreshing = false
                        binding.progressBar.hide()
                        binding.scrollView.visibility = View.VISIBLE
                    }

                    radarViewProvider?.updateCoordinates(weatherView.locationCoord, true)
                }
            }

            if (locationData != null) {
                forecastsView.updateForecasts(locationData!!)

                val context = instance.appContext

                if (Settings.getHomeData() == locationData) {
                    // Update widgets if they haven't been already
                    if (Duration.between(LocalDateTime.now(ZoneOffset.UTC), Settings.getUpdateTime()).toMinutes() > Settings.getRefreshInterval()) {
                        WeatherUpdaterWorker.enqueueAction(context, WeatherUpdaterWorker.ACTION_UPDATEWEATHER)
                    } else {
                        // Update widgets
                        WidgetUpdaterWorker.enqueueAction(context, WidgetUpdaterWorker.ACTION_UPDATEWIDGETS)
                    }
                } else {
                    // Update widgets anyway
                    WeatherUpdaterService.enqueueWork(context, Intent(context, WeatherUpdaterService::class.java)
                            .setAction(WeatherUpdaterService.ACTION_REFRESHWIDGETS)
                            .putExtra(WeatherUpdaterService.EXTRA_LOCATIONQUERY, locationData?.query))
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
                    if (WeatherAPI.NWS == Settings.getAPI()) {
                        showSnackbar(Snackbar.make(R.string.error_message_weather_us_only, Snackbar.Duration.LONG), null)
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

        if (WearableHelper.isGooglePlayServicesInstalled()) {
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(appCompatActivity!!)
            mLocCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    stopLocationUpdates()
                    mMainHandler.removeCallbacks(cancelLocRequestRunner)

                    runWithView {
                        if (Settings.useFollowGPS() && updateLocation()) {
                            // Setup loader from updated location
                            wLoader = WeatherDataLoader(locationData!!)

                            refreshWeather(false)
                        }
                    }
                }

                override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                    stopLocationUpdates()
                    mMainHandler.removeCallbacks(cancelLocRequestRunner)
                }
            }
        } else {
            mLocListnr = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    mMainHandler.removeCallbacks(cancelLocRequestRunner)
                    stopLocationUpdates()

                    runWithView {
                        if (Settings.useFollowGPS() && updateLocation()) {
                            // Setup loader from updated location
                            wLoader = WeatherDataLoader(locationData!!)

                            refreshWeather(false)
                        }
                    }
                }

                override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
        }

        mRequestingLocationUpdates = false

        // Live Data
        weatherLiveData = MutableLiveData()
        weatherLiveData.observe(this, weatherObserver)

        alertsView.alerts.observe(this, alertsObserver)

        lifecycle.addObserver(object : LifecycleObserver {
            private var wasStarted = false

            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            private fun onStart() {
                resume()
                wasStarted = true
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
            private fun onResume() {
                if (!wasStarted) onStart()
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            private fun onPause() {
                wasStarted = false
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_weather_now, container, false,
                dataBindingComponent)

        binding.weatherView = weatherView
        binding.lifecycleOwner = viewLifecycleOwner

        val view = binding.root
        // Request focus away from RecyclerView
        view.isFocusableInTouchMode = true
        view.requestFocus()

        ViewGroupCompat.setTransitionGroup((view as ViewGroup), true)

        // Setup ActionBar
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar, object : OnApplyWindowInsetsListener {
            private val paddingStart = ViewCompat.getPaddingStart(binding.toolbar)
            private val paddingTop = binding.toolbar.paddingTop
            private val paddingEnd = ViewCompat.getPaddingEnd(binding.toolbar)
            private val paddingBottom = binding.toolbar.paddingBottom

            override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
                ViewCompat.setPaddingRelative(v,
                        paddingStart + insets.systemWindowInsetLeft,
                        paddingTop + insets.systemWindowInsetTop,
                        paddingEnd + insets.systemWindowInsetRight,
                        paddingBottom)
                return insets
            }
        })

        // For landscape orientation
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val layoutParams = v.layoutParams as MarginLayoutParams
            layoutParams.setMargins(insets.systemWindowInsetLeft, 0, insets.systemWindowInsetRight, 0)
            insets
        }

        binding.scrollView.setOnScrollChangeListener(object : NestedScrollView.OnScrollChangeListener {
            @SuppressLint("RestrictedApi")
            override fun onScrollChange(v: NestedScrollView, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int) {
                runWithView {
                    val offset = v.computeVerticalScrollOffset()
                    if (offset > 0) {
                        ViewCompat.setElevation(binding.toolbar, ContextUtils.dpToPx(appCompatActivity!!, 4f))
                    } else {
                        ViewCompat.setElevation(binding.toolbar, 0f)
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
                if (ContextUtils.getOrientation(appCompatActivity!!) == Configuration.ORIENTATION_LANDSCAPE || !FeatureSettings.isBackgroundImageEnabled())
                    return

                runWithView {
                    val condPnlHeight = binding.refreshLayout.height
                    val THRESHOLD = condPnlHeight / 2
                    val scrollOffset = binding.scrollView.computeVerticalScrollOffset()
                    val dY = scrollY - oldScrollY
                    var mScrollHandled = false

                    if (dY == 0) return@runWithView

                    Timber.tag("ScrollView").d(String.format("onFlingStopped: height: %d; offset|scrollY: %d; prevScrollY: %d; dY: %d;", condPnlHeight, scrollOffset, oldScrollY, dY))

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

                        Timber.tag("ScrollView").d(String.format("onFlingStopped: height: %d; animScrollY: %d; prevScrollY: %d; animDY: %d;", condPnlHeight, animScrollY, oldScrollY, animDY))

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
                if (ContextUtils.getOrientation(appCompatActivity!!) == Configuration.ORIENTATION_LANDSCAPE || !FeatureSettings.isBackgroundImageEnabled())
                    return

                runWithView {
                    val condPnlHeight = binding.refreshLayout.height
                    val THRESHOLD = condPnlHeight / 2
                    val scrollOffset = binding.scrollView.computeVerticalScrollOffset()
                    val dY = scrollY - oldScrollY

                    if (dY == 0) return@runWithView

                    Timber.tag("ScrollView").d(String.format("onTouchScrollChange: height: %d; offset: %d; scrollY: %d; prevScrollY: %d; dY: %d",
                            condPnlHeight, scrollOffset, scrollY, oldScrollY, dY))

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
        binding.refreshLayout.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(appCompatActivity!!, R.color.invButtonColor))
        binding.refreshLayout.setColorSchemeColors(ContextUtils.getColor(appCompatActivity!!, R.attr.colorPrimary))
        binding.refreshLayout.setOnRefreshListener {
            AnalyticsLogger.logEvent("WeatherNowFragment: onRefresh")

            runWithView {
                if (Settings.useFollowGPS() && updateLocation()) {
                    // Setup loader from updated location
                    wLoader = WeatherDataLoader(locationData!!)
                }

                refreshWeather(true)
            }
        }

        kotlin.run {
            // Condition
            conditionPanelBinding = DataBindingUtil.inflate(inflater, R.layout.weathernow_condition_panel, binding.listLayout, false, dataBindingComponent)
            conditionPanelBinding.alertsView = alertsView
            conditionPanelBinding.weatherView = weatherView
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
                AnalyticsLogger.logEvent("WeatherNowFragment: alerts click")
                v.isEnabled = false
                // Show Alert Fragment
                val args = WeatherNowFragmentDirections.actionWeatherNowFragmentToWeatherListFragment()
                        .setData(JSONParser.serializer(locationData, LocationData::class.java))
                        .setWeatherListType(WeatherListType.ALERTS)
                v.findNavController().navigate(args)
            }

            binding.listLayout.addView(conditionPanelBinding.root, Math.min(binding.listLayout.childCount - 1, 0))

            adjustConditionPanelLayout()
        }

        if (FeatureSettings.isForecastEnabled()) {
            // Forecast
            forecastPanelBinding = DataBindingUtil.inflate(inflater, R.layout.weathernow_forecastgraphpanel, binding.listLayout, false, dataBindingComponent)
            forecastPanelBinding!!.forecastsView = forecastsView
            forecastPanelBinding!!.lifecycleOwner = viewLifecycleOwner

            forecastPanelBinding!!.rangebarGraphPanel.setOnClickPositionListener { view, position ->
                AnalyticsLogger.logEvent("WeatherNowFragment: fcast graph click")
                view.isEnabled = false
                val args = WeatherNowFragmentDirections.actionWeatherNowFragmentToWeatherListFragment()
                        .setData(JSONParser.serializer(locationData, LocationData::class.java))
                        .setWeatherListType(WeatherListType.FORECAST)
                        .setPosition(position)
                Navigation.findNavController(view).navigate(args)
            }

            binding.listLayout.addView(forecastPanelBinding!!.root, Math.min(binding.listLayout.childCount - 1, 1))
        }

        if (FeatureSettings.isHourlyForecastEnabled()) {
            // Hourly Forecast
            hrForecastPanelBinding = DataBindingUtil.inflate(inflater, R.layout.weathernow_hrforecastlistpanel, binding.listLayout, false, dataBindingComponent)
            hrForecastPanelBinding!!.forecastsView = forecastsView
            hrForecastPanelBinding!!.lifecycleOwner = viewLifecycleOwner

            // Setup RecyclerView
            val hourlyForecastItemAdapter = HourlyForecastItemAdapter(object : DiffUtil.ItemCallback<HourlyForecastNowViewModel>() {
                override fun areItemsTheSame(oldItem: HourlyForecastNowViewModel, newItem: HourlyForecastNowViewModel): Boolean {
                    return ObjectsCompat.equals(oldItem.date, newItem.date)
                }

                override fun areContentsTheSame(oldItem: HourlyForecastNowViewModel, newItem: HourlyForecastNowViewModel): Boolean {
                    return ObjectsCompat.equals(oldItem, newItem)
                }
            })

            hourlyForecastItemAdapter.onClickListener = RecyclerOnClickListenerInterface { view, position ->
                AnalyticsLogger.logEvent("WeatherNowFragment: hrf panel click")
                view.isEnabled = false
                val args = WeatherNowFragmentDirections.actionWeatherNowFragmentToWeatherListFragment()
                        .setData(JSONParser.serializer(locationData, LocationData::class.java))
                        .setWeatherListType(WeatherListType.HOURLYFORECAST)
                        .setPosition(position)
                Navigation.findNavController(view).navigate(args)
            }

            hrForecastPanelBinding!!.hourlyForecastList.adapter = hourlyForecastItemAdapter

            binding.listLayout.addView(hrForecastPanelBinding!!.root, Math.min(binding.listLayout.childCount - 1, 2))
        }

        if (FeatureSettings.isChartsEnabled()) {
            // Precipitation graph
            precipPanelBinding = DataBindingUtil.inflate(inflater, R.layout.weathernow_precipitationgraphpanel, binding.listLayout, false, dataBindingComponent)
            precipPanelBinding!!.forecastsView = forecastsView
            precipPanelBinding!!.lifecycleOwner = viewLifecycleOwner

            precipPanelBinding!!.precipGraphPanel.setOnClickPositionListener { view, position ->
                AnalyticsLogger.logEvent("WeatherNowFragment: precip graph click")
                view.isEnabled = false
                val args = WeatherNowFragmentDirections.actionWeatherNowFragmentToWeatherChartsFragment()
                        .setData(JSONParser.serializer(locationData, LocationData::class.java))
                Navigation.findNavController(view).navigate(args)
            }

            binding.listLayout.addView(precipPanelBinding!!.root, Math.min(binding.listLayout.childCount - 1, 3))
        }

        if (FeatureSettings.isDetailsEnabled()) {
            detailsContainerBinding = DataBindingUtil.inflate(inflater, R.layout.weathernow_detailscontainer, binding.listLayout, false, dataBindingComponent)

            // Details
            detailsContainerBinding!!.detailsContainer.adapter = DetailsItemGridAdapter()
            detailsContainerBinding!!.weatherView = weatherView
            detailsContainerBinding!!.lifecycleOwner = viewLifecycleOwner

            // Disable touch events on container
            // View does not scroll
            detailsContainerBinding!!.detailsContainer.isFocusable = false
            detailsContainerBinding!!.detailsContainer.isFocusableInTouchMode = false
            detailsContainerBinding!!.detailsContainer.setOnTouchListener { v, event -> true }

            binding.listLayout.addView(detailsContainerBinding!!.root, Math.min(binding.listLayout.childCount - 1, 4))

            adjustDetailsLayout()
        }

        if (FeatureSettings.isUVEnabled()) {
            // UV
            uvControlBinding = DataBindingUtil.inflate(inflater, R.layout.weathernow_uvcontrol, binding.listLayout, false, dataBindingComponent)
            uvControlBinding!!.weatherView = weatherView
            uvControlBinding!!.lifecycleOwner = viewLifecycleOwner

            binding.listLayout.addView(uvControlBinding!!.root, Math.min(binding.listLayout.childCount - 1, 5))
        }

        if (FeatureSettings.isBeaufortEnabled()) {
            // Beaufort
            beaufortControlBinding = DataBindingUtil.inflate(inflater, R.layout.weathernow_beaufortcontrol, binding.listLayout, false, dataBindingComponent)
            beaufortControlBinding!!.weatherView = weatherView
            beaufortControlBinding!!.lifecycleOwner = viewLifecycleOwner

            binding.listLayout.addView(beaufortControlBinding!!.root, Math.min(binding.listLayout.childCount - 1, 6))
        }

        if (FeatureSettings.isAQIndexEnabled()) {
            // Air Quality
            aqiControlBinding = DataBindingUtil.inflate(inflater, R.layout.weathernow_aqicontrol, binding.listLayout, false, dataBindingComponent)
            aqiControlBinding!!.weatherView = weatherView
            aqiControlBinding!!.lifecycleOwner = viewLifecycleOwner

            binding.listLayout.addView(aqiControlBinding!!.root, Math.min(binding.listLayout.childCount - 1, 7))
        }

        if (FeatureSettings.isMoonPhaseEnabled()) {
            // Moon Phase
            moonphaseControlBinding = DataBindingUtil.inflate(inflater, R.layout.weathernow_moonphasecontrol, binding.listLayout, false, dataBindingComponent)
            moonphaseControlBinding!!.weatherView = weatherView
            moonphaseControlBinding!!.lifecycleOwner = viewLifecycleOwner

            binding.listLayout.addView(moonphaseControlBinding!!.root, Math.min(binding.listLayout.childCount - 1, 8))
        }

        if (FeatureSettings.isSunPhaseEnabled()) {
            // Sun Phase
            sunphaseControlBinding = DataBindingUtil.inflate(inflater, R.layout.weathernow_sunphasecontrol, binding.listLayout, false, dataBindingComponent)
            sunphaseControlBinding!!.weatherView = weatherView
            sunphaseControlBinding!!.lifecycleOwner = viewLifecycleOwner

            binding.listLayout.addView(sunphaseControlBinding!!.root, Math.min(binding.listLayout.childCount - 1, 9))
        }

        // Radar
        if (FeatureSettings.isRadarEnabled()) {
            radarControlBinding = DataBindingUtil.inflate(inflater, R.layout.weathernow_radarcontrol, binding.listLayout, false, dataBindingComponent)

            radarControlBinding!!.radarWebviewCover.setOnClickListener { v ->
                AnalyticsLogger.logEvent("WeatherNowFragment: radar view click")
                v.isEnabled = false
                Navigation.findNavController(v)
                        .navigate(
                                WeatherNowFragmentDirections.actionWeatherNowFragmentToWeatherRadarFragment(),
                                FragmentNavigator.Extras.Builder()
                                        .addSharedElement(v, "radar")
                                        .build()
                        )
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

            binding.listLayout.addView(radarControlBinding!!.root, Math.min(binding.listLayout.childCount - 1, 10))

            radarViewProvider = RadarProvider.getRadarViewProvider(requireContext(), radarControlBinding!!.radarWebviewContainer).apply {
                enableInteractions(false)
                onCreateView(savedInstanceState)
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        args = WeatherNowFragmentArgs.fromBundle(requireArguments())

        adjustConditionPanelLayout()
        adjustDetailsLayout()

        // Set property change listeners
        weatherView.addOnPropertyChangedCallback(object : OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable, propertyId: Int) {
                if (propertyId == 0 || propertyId == BR.location) {
                    runWithView {
                        adjustConditionPanelLayout()
                        adjustDetailsLayout()
                    }
                } else if (propertyId == BR.locationCoord) {
                    // Restrict control to Kitkat+ for Chromium WebView
                    if (FeatureSettings.isRadarEnabled()) {
                        radarViewProvider?.updateCoordinates(weatherView.locationCoord, true)
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

        radarViewProvider?.onResume()
    }

    override fun onPause() {
        AnalyticsLogger.logEvent("WeatherNowFragment: onPause")

        radarViewProvider?.onPause()

        // Remove location updates to save battery.
        stopLocationUpdates()
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

        binding.refreshLayout.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(appCompatActivity!!, R.color.invButtonColor))
        binding.refreshLayout.setColorSchemeColors(ContextUtils.getColor(appCompatActivity!!, R.attr.colorPrimary))

        // Resize necessary views
        adjustConditionPanelLayout()
        adjustDetailsLayout()

        val backgroundUri = weatherView.imageData?.imageURI
        loadBackgroundImage(backgroundUri, true)

        // Reload Webview
        radarViewProvider?.onConfigurationChanged()
    }

    private fun loadBackgroundImage(imageURI: String?, skipCache: Boolean) {
        runWithView {
            // Reload background image
            if (FeatureSettings.isBackgroundImageEnabled()) {
                if (!ObjectsCompat.equals(conditionPanelBinding.imageView.tag, imageURI)) {
                    conditionPanelBinding.imageView.tag = imageURI
                    if (!StringUtils.isNullOrWhitespace(imageURI)) {
                        Glide.with(this@WeatherNowFragment)
                                .load(imageURI)
                                .apply(RequestOptions.centerCropTransform()
                                        .format(DecodeFormat.PREFER_RGB_565)
                                        .skipMemoryCache(skipCache))
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .addListener(object : RequestListener<Drawable?> {
                                    override fun onLoadFailed(e: GlideException?, model: Any, target: Target<Drawable?>, isFirstResource: Boolean): Boolean {
                                        binding.refreshLayout.isRefreshing = false
                                        binding.progressBar.hide()
                                        binding.scrollView.visibility = View.VISIBLE
                                        return false
                                    }

                                    override fun onResourceReady(resource: Drawable?, model: Any, target: Target<Drawable?>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                                        binding.refreshLayout.postOnAnimation(Runnable {
                                            binding.refreshLayout.isRefreshing = false
                                            binding.progressBar.hide()
                                            binding.scrollView.visibility = View.VISIBLE
                                        })
                                        return false
                                    }
                                })
                                .into(conditionPanelBinding.imageView)
                    } else {
                        Glide.with(this@WeatherNowFragment).clear(conditionPanelBinding.imageView)
                        conditionPanelBinding.imageView.tag = null
                        if (weatherView.isValid) {
                            binding.refreshLayout.isRefreshing = false
                            binding.progressBar.hide()
                            binding.scrollView.visibility = View.VISIBLE
                        }
                    }
                }
            } else {
                Glide.with(this@WeatherNowFragment).clear(conditionPanelBinding.imageView)
                conditionPanelBinding.imageView.tag = null
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

    private suspend fun verifyLocationData(): Boolean = withContext(Dispatchers.IO) {
        var locationChanged = false

        // Check if current location still exists (is valid)
        if (locationData?.locationType == LocationType.SEARCH) {
            if (Settings.getLocation(locationData?.query) == null) {
                locationData = null
                wLoader = null
                locationChanged = true
            }
        }
        // Load new favorite location if argument data is present
        if (args.home) {
            // Check if home location changed
            // For ex. due to GPS setting change
            val homeData = Settings.getHomeData()
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
                if (Settings.getAPI() != weatherView.weatherSource
                        || wm.supportsWeatherLocale() && locale != weatherView.weatherLocale) {
                    restore()
                } else {
                    // Update weather if needed on resume
                    if (Settings.useFollowGPS() && updateLocation()) {
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
                    if (Settings.useFollowGPS() && (locationData == null || locationData!!.locationType == LocationType.GPS)) {
                        val locData = Settings.getLastGPSLocData()
                        if (locData == null) {
                            // Update location if not setup
                            updateLocation()
                            forceRefresh = true
                        } else {
                            // Reset locdata if source is different
                            if (Settings.getAPI() != locData.weatherSource) Settings.saveLastGPSLocData(LocationData())
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
                        locationData = Settings.getHomeData()
                    }
                    if (locationData != null) wLoader = WeatherDataLoader(locationData!!)
                    forceRefresh
                }

                task.invokeOnCompletion {
                    val t = task.getCompletionExceptionOrNull()
                    if (t == null) {
                        refreshWeather(task.getCompleted())
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

            wLoader?.loadWeatherResult(WeatherRequest.Builder()
                    .forceRefresh(forceRefresh)
                    .setErrorListener(this@WeatherNowFragment)
                    .build())
                    ?.addOnSuccessListener { weather ->
                        weatherLiveData.setValue(weather.weather)
                    }
                    ?.continueWithTask { task ->
                        if (task.isSuccessful) {
                            runWithView {
                                if (conditionPanelBinding.alertButton.visibility != View.GONE) {
                                    conditionPanelBinding.alertButton.visibility = View.GONE
                                    adjustConditionPanelLayout()
                                }
                            }
                            wLoader?.loadWeatherAlerts(task.result.isSavedData)
                        } else {
                            Tasks.forCanceled()
                        }
                    }
                    ?.addOnCompleteListener { task ->
                        runWithView {
                            if (locationData != null) {
                                alertsView.updateAlerts(locationData!!)
                            }

                            if (task.isSuccessful) {
                                if (wm.supportsAlerts() && locationData != null) {
                                    val weatherAlerts = task.result

                                    if (weatherAlerts != null && !weatherAlerts.isEmpty()) {
                                        // Alerts are posted to the user here. Set them as notified.
                                        GlobalScope.launch(Dispatchers.Default) {
                                            if (BuildConfig.DEBUG) {
                                                WeatherAlertHandler.postAlerts(locationData, weatherAlerts)
                                            }
                                            WeatherAlertHandler.setAsNotified(locationData, weatherAlerts)
                                        }
                                    }
                                }
                            }
                        }
                    }
        }
    }

    private fun adjustConditionPanelLayout() {
        conditionPanelBinding.conditionPanel.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            val imageLandSize = ContextUtils.dpToPx(conditionPanelBinding.conditionPanel.context, 560f).toInt()

            override fun onPreDraw(): Boolean {
                conditionPanelBinding.conditionPanel.viewTreeObserver.removeOnPreDrawListener(this)

                runWithView(Dispatchers.Main.immediate) {
                    val context = conditionPanelBinding.conditionPanel.context

                    val height = binding.refreshLayout.measuredHeight

                    val containerLP = conditionPanelBinding.imageViewContainer.layoutParams as MarginLayoutParams
                    val conditionPLP = conditionPanelBinding.conditionPanel.layoutParams as MarginLayoutParams
                    if (ContextUtils.getOrientation(context) == Configuration.ORIENTATION_LANDSCAPE && height < imageLandSize) {
                        containerLP.height = imageLandSize
                    } else if (FeatureSettings.isBackgroundImageEnabled() && height > 0) {
                        containerLP.height = height - conditionPanelBinding.conditionPanel.measuredHeight - containerLP.bottomMargin - containerLP.topMargin
                        if (conditionPanelBinding.alertButton.visibility != View.GONE) {
                            containerLP.height -= conditionPanelBinding.alertButton.measuredHeight
                        }
                        if (conditionPLP.topMargin < 0) {
                            containerLP.height += -conditionPLP.topMargin
                        }
                    } else {
                        containerLP.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    }

                    conditionPanelBinding.imageViewContainer.layoutParams = containerLP
                }

                return true
            }
        })
    }

    private fun adjustDetailsLayout() {
        if (binding.scrollView.childCount != 1) return

        detailsContainerBinding?.detailsContainer?.viewTreeObserver?.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (detailsContainerBinding == null) return true

                detailsContainerBinding!!.detailsContainer.viewTreeObserver.removeOnPreDrawListener(this)

                runWithView(Dispatchers.Main.immediate) {
                    val pxWidth = binding.scrollView.getChildAt(0).measuredWidth

                    val minColumns = if (ContextUtils.isLargeTablet(appCompatActivity!!)) 3 else 2

                    // Minimum width for ea. card
                    val minWidth = appCompatActivity!!.resources.getDimensionPixelSize(R.dimen.detail_grid_column_width)
                    // Available columns based on min card width
                    val availColumns = if (pxWidth / minWidth <= 1) minColumns else pxWidth / minWidth

                    detailsContainerBinding!!.detailsContainer.numColumns = availColumns

                    val isLandscape = ContextUtils.getOrientation(appCompatActivity!!) == Configuration.ORIENTATION_LANDSCAPE

                    val horizMargin = 16
                    val marginMultiplier = if (isLandscape) 2 else 3
                    val itemSpacing = if (availColumns < 3) horizMargin * (availColumns - 1) else horizMargin * marginMultiplier
                    detailsContainerBinding!!.detailsContainer.horizontalSpacing = itemSpacing
                    detailsContainerBinding!!.detailsContainer.verticalSpacing = itemSpacing
                }

                return true
            }
        })
    }

    override fun updateWindowColors() {
        var color = ContextUtils.getColor(appCompatActivity!!, android.R.attr.colorBackground)
        if (Settings.getUserThemeMode() == UserThemeMode.AMOLED_DARK) {
            color = Colors.BLACK
        }

        binding.toolbar.setBackgroundColor(color)
        binding.rootView.setBackgroundColor(color)
        binding.rootView.setStatusBarBackgroundColor(color)
    }

    @SuppressLint("MissingPermission")
    private suspend fun updateLocation(): Boolean {
        var locationChanged = false

        if (appCompatActivity != null && Settings.useFollowGPS() && locationData?.locationType == LocationType.GPS) {
            if (ContextCompat.checkSelfPermission(appCompatActivity!!, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(appCompatActivity!!, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                        PERMISSION_LOCATION_REQUEST_CODE)
                return false
            }

            var location: Location? = null

            val locMan = appCompatActivity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager?

            if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
                locationData = Settings.getLastGPSLocData()
                return false
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

            if (location != null && !mRequestingLocationUpdates) {
                val lastGPSLocData = Settings.getLastGPSLocData()

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

                if (view.locationQuery?.isBlank() == true) {
                    // Stop since there is no valid query
                    return false
                } else if (view.locationTZLong?.isBlank() == true && view.locationLat != 0.0 && view.locationLong != 0.0) {
                    val tzId = TZDBCache.getTimeZone(view.locationLat, view.locationLong)
                    if ("unknown" != tzId)
                        view.locationTZLong = tzId
                }

                if (!coroutineContext.isActive) return false

                // Save location as last known
                lastGPSLocData?.setData(view, location)
                Settings.saveLastGPSLocData(lastGPSLocData)

                LocalBroadcastManager.getInstance(appCompatActivity!!)
                        .sendBroadcast(Intent(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE))

                locationData = lastGPSLocData
                mLocation = location
                locationChanged = true
            }
        }

        return locationChanged
    }

    private val cancelLocRequestRunner = Runnable {
        stopLocationUpdates()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_LOCATION_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    // Do the task you need to do.
                    runWithView {
                        if (Settings.useFollowGPS() && updateLocation()) {
                            // Setup loader from updated location
                            wLoader = WeatherDataLoader(locationData!!)

                            refreshWeather(false)
                        }
                    }
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Settings.setFollowGPS(false)
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
        var imageAlpha = 1.0f
        var gradientAlpha = 1.0f
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

        @BindingAdapter("forecast_data")
        fun updateHrForecastView(view: RecyclerView, forecasts: List<HourlyForecastNowViewModel>?) {
            if (view.adapter is HourlyForecastItemAdapter) {
                (view.adapter as HourlyForecastItemAdapter).submitList(forecasts)
            }
        }

        @BindingAdapter("sunPhase")
        fun updateSunPhasePanel(view: SunPhaseView, sunPhase: SunPhaseViewModel?) {
            if (sunPhase?.sunrise?.isNotBlank() == true && sunPhase.sunset?.isNotBlank() == true && fragment?.locationData != null) {
                val fmt = sunPhase.formatter

                view.setSunriseSetTimes(LocalTime.parse(sunPhase.sunrise, fmt),
                        LocalTime.parse(sunPhase.sunset, fmt),
                        fragment.locationData!!.tzOffset)
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
                        view.context.startActivity(i)
                    }
                }
            } else {
                view.text = ""
                view.setOnClickListener(null)
            }
        }

        @BindingAdapter(value = ["tempTextColor", "tempUnit"], requireAll = false)
        fun tempTextColor(view: TextView, temp: CharSequence?, @TemperatureUnits tempUnit: String?) {
            val temp_str = StringUtils.removeNonDigitChars(temp)
            var temp_f = temp_str.toFloatOrNull()
            if (temp_f != null) {
                if (ObjectsCompat.equals(tempUnit, Units.CELSIUS) || temp?.endsWith(Units.CELSIUS) == true) {
                    temp_f = ConversionMethods.CtoF(temp_f)
                }

                view.setTextColor(WeatherUtils.getColorFromTempF(temp_f))
            } else {
                view.setTextColor(ContextCompat.getColor(view.context, R.color.colorTextPrimary))
            }
        }
    }
}