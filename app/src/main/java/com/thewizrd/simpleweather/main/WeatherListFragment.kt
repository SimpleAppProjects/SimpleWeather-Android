package com.thewizrd.simpleweather.main

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.transition.MaterialSharedAxis
import com.thewizrd.common.adapters.WeatherAlertPanelAdapter
import com.thewizrd.common.controls.*
import com.thewizrd.common.helpers.SimpleRecyclerViewAdapterObserver
import com.thewizrd.common.weatherdata.WeatherDataLoader
import com.thewizrd.common.weatherdata.WeatherRequest
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.exceptions.ErrorStatus
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrColor
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrResourceId
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.UserThemeMode
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.adapters.WeatherDetailsAdapter
import com.thewizrd.simpleweather.databinding.FragmentWeatherListBinding
import com.thewizrd.simpleweather.databinding.LayoutLocationHeaderBinding
import com.thewizrd.simpleweather.fragments.ToolbarFragment
import com.thewizrd.simpleweather.snackbar.Snackbar
import com.thewizrd.simpleweather.snackbar.SnackbarManager
import com.thewizrd.weather_api.weatherModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class WeatherListFragment : ToolbarFragment() {
    private val weatherView: WeatherNowViewModel by activityViewModels()
    private val forecastsView: ForecastsListViewModel by viewModels()
    private val alertsView: WeatherAlertsViewModel by activityViewModels()
    private var locationData: LocationData? = null

    private lateinit var binding: FragmentWeatherListBinding
    private lateinit var headerBinding: LayoutLocationHeaderBinding
    private var layoutManager: LinearLayoutManager? = null

    var weatherListType: WeatherListType? = null
        private set

    private var args: WeatherListFragmentArgs? = null

    private val wm = weatherModule.weatherManager

    companion object {
        fun newInstance(type: WeatherListType): WeatherListFragment {
            val fragment = WeatherListFragment().apply {
                weatherListType = type
            }

            fragment.arguments = Bundle().apply {
                putInt(Constants.ARGS_WEATHERLISTTYPE, type.value)
            }

            return fragment
        }

        fun newInstance(locData: LocationData?, type: WeatherListType): WeatherListFragment {
            val fragment = WeatherListFragment().apply {
                weatherListType = type
                locationData = locData
            }

            fragment.arguments = Bundle().apply {
                putInt(Constants.ARGS_WEATHERLISTTYPE, type.value)
            }

            return fragment
        }
    }

    init {
        arguments = Bundle()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AnalyticsLogger.logEvent("WeatherListFragment: onCreate")

        args = WeatherListFragmentArgs.fromBundle(requireArguments())

        if (args?.weatherListType == WeatherListType.ALERTS) {
            enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
            returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
        }

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(Constants.ARGS_WEATHERLISTTYPE)) {
                weatherListType = WeatherListType.valueOf(savedInstanceState.getInt(Constants.ARGS_WEATHERLISTTYPE))
            }
            if (savedInstanceState.containsKey(Constants.KEY_DATA)) {
                locationData = JSONParser.deserializer(
                    savedInstanceState.getString(Constants.KEY_DATA),
                    LocationData::class.java
                )
            }
        } else {
            weatherListType = args?.weatherListType
            if (args?.data != null) {
                locationData = JSONParser.deserializer(args?.data, LocationData::class.java)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?
    ): View {
        val root = super.onCreateView(inflater, container, savedInstanceState) as ViewGroup
        // Use this to return your custom view for this Fragment
        binding = FragmentWeatherListBinding.inflate(inflater, root, true)
        headerBinding = LayoutLocationHeaderBinding.inflate(inflater, appBarLayout, true)
        binding.lifecycleOwner = viewLifecycleOwner

        // Setup Actionbar
        toolbar.setNavigationIcon(toolbar.context.getAttrResourceId(R.attr.homeAsUpIndicator))
        toolbar.setNavigationOnClickListener { v -> v.findNavController().navigateUp() }

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the binding.recyclerView
        binding.recyclerView.setHasFixedSize(true)
        // use a linear layout manager
        binding.recyclerView.layoutManager =
            LinearLayoutManager(requireContext()).also { layoutManager = it }

        return root
    }

    override val scrollTargetViewId: Int
        get() = binding.recyclerView.id

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        args = WeatherListFragmentArgs.fromBundle(requireArguments())

        binding.progressBar.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()

        if (!isHidden) {
            AnalyticsLogger.logEvent("WeatherListFragment: onResume")
            initialize()
        }
    }

    override fun onPause() {
        AnalyticsLogger.logEvent("WeatherListFragment: onPause")
        super.onPause()
    }

    override val titleResId: Int
        get() = when (weatherListType) {
            WeatherListType.FORECAST, WeatherListType.HOURLYFORECAST -> R.string.label_forecast
            WeatherListType.ALERTS -> R.string.title_fragment_alerts
            else -> R.string.label_nav_weathernow
        }

    private fun initialize() {
        runWithView {
            if (locationData == null) {
                locationData = settingsManager.getHomeData()
            }

            if (!weatherView.isValid || locationData != null && locationData!!.query != weatherView.query) {
                locationData?.let { locData ->
                    runWithView(Dispatchers.Default) {
                        supervisorScope {
                            val weather = WeatherDataLoader(locData).loadWeatherData(
                                WeatherRequest.Builder()
                                    .forceLoadSavedData()
                                    .setErrorListener { wEx ->
                                        when (wEx.errorStatus) {
                                            ErrorStatus.NETWORKERROR, ErrorStatus.NOWEATHER -> {
                                                // Show error message and prompt to refresh
                                                showSnackbar(
                                                    Snackbar.make(
                                                        binding.root.context,
                                                        wEx.message,
                                                        Snackbar.Duration.LONG
                                                    )
                                                )
                                            }
                                            ErrorStatus.QUERYNOTFOUND -> {
                                                if (!wm.isRegionSupported(locData.countryCode)) {
                                                    showSnackbar(
                                                        Snackbar.make(
                                                            binding.root.context,
                                                            R.string.error_message_weather_region_unsupported,
                                                            Snackbar.Duration.LONG
                                                        )
                                                    )
                                                    return@setErrorListener
                                                }
                                                // Show error message
                                                showSnackbar(
                                                    Snackbar.make(
                                                        binding.root.context,
                                                        wEx.message,
                                                        Snackbar.Duration.LONG
                                                    )
                                                )
                                            }
                                            else -> {
                                                showSnackbar(
                                                    Snackbar.make(
                                                        binding.root.context,
                                                        wEx.message,
                                                        Snackbar.Duration.LONG
                                                    )
                                                )
                                            }
                                        }
                                    }.build()
                            )

                            ensureActive()

                            launch(Dispatchers.Main) {
                                weatherView.updateView(weather)
                                forecastsView.updateForecasts(locData)
                                alertsView.updateAlerts(locData)
                                headerBinding.locationName.text = weatherView.location
                            }
                        }
                    }
                }
            } else {
                locationData?.let {
                    forecastsView.updateForecasts(it)
                    alertsView.updateAlerts(it)
                }
                headerBinding.locationName.text = weatherView.location
            }

            // specify an adapter (see also next example)
            when (weatherListType) {
                WeatherListType.FORECAST, WeatherListType.HOURLYFORECAST -> {
                    if (weatherListType == WeatherListType.FORECAST) {
                        forecastsView.getForecasts()?.removeObservers(this@WeatherListFragment)
                        forecastsView.getForecasts()
                            ?.observe(this@WeatherListFragment) { forecasts ->
                                val detailsAdapter =
                                    getForecastAdapter<ForecastItemViewModel>(binding.recyclerView)
                                detailsAdapter.submitList(forecasts)
                            }
                    } else {
                        forecastsView.getHourlyForecasts()
                            ?.removeObservers(this@WeatherListFragment)
                        forecastsView.getHourlyForecasts()
                            ?.observe(this@WeatherListFragment) { hrforecasts ->
                                val detailsAdapter =
                                    getForecastAdapter<HourlyForecastItemViewModel>(binding.recyclerView)
                                detailsAdapter.submitList(hrforecasts)
                            }
                    }
                }
                WeatherListType.ALERTS -> {
                    val alertAdapter = binding.recyclerView.adapter as? WeatherAlertPanelAdapter?
                        ?: WeatherAlertPanelAdapter()
                    if (binding.recyclerView.adapter !== alertAdapter) {
                        binding.recyclerView.adapter = alertAdapter
                    }

                    alertAdapter.registerAdapterDataObserver(object :
                        SimpleRecyclerViewAdapterObserver() {
                        override fun onChanged() {
                            alertAdapter.unregisterAdapterDataObserver(this)
                            binding.progressBar.visibility = View.GONE
                        }
                    })

                    alertsView.getAlerts()?.removeObservers(this@WeatherListFragment)
                    alertsView.getAlerts()?.observe(this@WeatherListFragment) { alerts ->
                        alertAdapter.submitList(alerts)
                    }
                }
                else -> {
                    binding.recyclerView.adapter = null
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun <T : BaseForecastItemViewModel> getForecastAdapter(recyclerView: RecyclerView
    ): WeatherDetailsAdapter<T> {
        @Suppress("UNCHECKED_CAST")
        val detailsAdapter: WeatherDetailsAdapter<T> =
            recyclerView.adapter as? WeatherDetailsAdapter<T>?
                ?: WeatherDetailsAdapter()
        if (recyclerView.adapter !== detailsAdapter) {
            recyclerView.adapter = detailsAdapter
        }

        detailsAdapter.registerAdapterDataObserver(object : SimpleRecyclerViewAdapterObserver() {
            override fun onChanged() {
                if (detailsAdapter.currentList != null && detailsAdapter.itemCount > args!!.position) {
                    detailsAdapter.unregisterAdapterDataObserver(this)
                    detailsAdapter.currentList!!.loadAround(args!!.position)
                    binding.recyclerView.viewTreeObserver.addOnGlobalLayoutListener(object :
                            OnGlobalLayoutListener {
                        override fun onGlobalLayout() {
                            binding.recyclerView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                            runWithView {
                                layoutManager!!.scrollToPositionWithOffset(args!!.position, 0)
                            }
                        }
                    })
                    binding.progressBar.visibility = View.GONE
                }
            }
        })

        return detailsAdapter
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // Save data
        outState.putInt(Constants.ARGS_WEATHERLISTTYPE, weatherListType!!.value)
        outState.putString(
            Constants.KEY_DATA,
            JSONParser.serializer(locationData, LocationData::class.java)
        )
        super.onSaveInstanceState(outState)
    }

    override fun updateWindowColors() {
        super.updateWindowColors()

        context?.let { ctx ->
            var backgroundColor = ctx.getAttrColor(android.R.attr.colorBackground)
            if (settingsManager.getUserThemeMode() == UserThemeMode.AMOLED_DARK) {
                backgroundColor = Colors.BLACK
            }

            binding.recyclerView.setBackgroundColor(backgroundColor)
        }
    }

    override fun createSnackManager(activity: Activity): SnackbarManager {
        val mSnackMgr = SnackbarManager(binding.root)
        mSnackMgr.setSwipeDismissEnabled(true)
        mSnackMgr.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE)
        return mSnackMgr
    }
}