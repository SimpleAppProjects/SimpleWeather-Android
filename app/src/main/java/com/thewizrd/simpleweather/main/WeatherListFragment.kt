package com.thewizrd.simpleweather.main

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.transition.platform.MaterialSharedAxis
import com.thewizrd.common.adapters.WeatherAlertPanelAdapter
import com.thewizrd.common.controls.*
import com.thewizrd.common.helpers.SimpleRecyclerViewAdapterObserver
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.di.settingsManager
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
import com.thewizrd.simpleweather.snackbar.SnackbarManager
import com.thewizrd.simpleweather.utils.NavigationUtils.navControllerViewModels
import com.thewizrd.simpleweather.viewmodels.TwoPaneStateViewModel
import com.thewizrd.simpleweather.viewmodels.WeatherNowViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WeatherListFragment : ToolbarFragment() {
    private val wNowViewModel: WeatherNowViewModel by activityViewModels()
    private val forecastsView: ForecastsListViewModel by viewModels()
    private val alertsView: WeatherAlertsViewModel by activityViewModels()
    private val twoPaneStateViewModel: TwoPaneStateViewModel by navControllerViewModels(R.id.two_pane_nav_graph)

    private var locationData: LocationData? = null

    private lateinit var binding: FragmentWeatherListBinding
    private lateinit var headerBinding: LayoutLocationHeaderBinding
    private var layoutManager: LinearLayoutManager? = null

    var weatherListType: WeatherListType? = null
        private set

    private val args: WeatherListFragmentArgs by navArgs()

    private var dataJob: Job? = null

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

        if (args.weatherListType == WeatherListType.ALERTS) {
            enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
            returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
            exitTransition = null
        } else {
            enterTransition = null
            exitTransition = null
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
            weatherListType = args.weatherListType
            if (args.data != null) {
                locationData = JSONParser.deserializer(args.data, LocationData::class.java)
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
        headerBinding.lifecycleOwner = viewLifecycleOwner
        headerBinding.viewModel = wNowViewModel

        // Setup Actionbar
        toolbar.setNavigationIcon(toolbar.context.getAttrResourceId(R.attr.homeAsUpIndicator))
        toolbar.setNavigationOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }

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

        binding.progressBar.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            twoPaneStateViewModel.twoPaneState.collectLatest { state ->
                setNavigationIconVisible(!state.isSideBySide)
                headerBinding.root.isVisible = !state.isSideBySide
            }
        }

        if (args.data.isNullOrBlank() && savedInstanceState?.containsKey(Constants.KEY_DATA) != true) {
            viewLifecycleOwner.lifecycleScope.launch {
                wNowViewModel.uiState.collect {
                    locationData = it.locationData
                    initialize()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                initialize()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsLogger.logEvent("WeatherListFragment: onResume")
    }

    override fun onPause() {
        dataJob?.cancel()
        AnalyticsLogger.logEvent("WeatherListFragment: onPause")
        super.onPause()
    }

    override val titleResId: Int
        get() = when (weatherListType) {
            WeatherListType.FORECAST, WeatherListType.HOURLYFORECAST -> R.string.label_forecast
            WeatherListType.ALERTS -> R.string.title_fragment_alerts
            else -> R.string.label_nav_weathernow
        }

    private suspend fun initialize() {
        if (locationData == null) {
            locationData = wNowViewModel.uiState.value.locationData
        }

        locationData?.let {
            forecastsView.updateForecasts(it)
            alertsView.updateAlerts(it)
        }

        dataJob?.cancel()

        // specify an adapter (see also next example)
        when (weatherListType) {
            WeatherListType.FORECAST, WeatherListType.HOURLYFORECAST -> {
                if (weatherListType == WeatherListType.FORECAST) {
                    dataJob = runWithView {
                        forecastsView.getForecasts().collect {
                            val detailsAdapter =
                                getForecastAdapter<ForecastItemViewModel>(binding.recyclerView)
                            detailsAdapter.submitData(it)
                        }
                    }
                } else {
                    dataJob = runWithView {
                        forecastsView.getHourlyForecasts().collect {
                            val detailsAdapter =
                                getForecastAdapter<HourlyForecastItemViewModel>(binding.recyclerView)
                            detailsAdapter.submitData(it)
                        }
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

                dataJob = runWithView {
                    alertsView.getAlerts().collect {
                        alertAdapter.submitList(it)
                    }
                }
            }
            else -> {
                binding.recyclerView.adapter = null
                binding.progressBar.visibility = View.GONE
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
                if (detailsAdapter.itemCount > args.position) {
                    detailsAdapter.unregisterAdapterDataObserver(this)
                    binding.recyclerView.viewTreeObserver.addOnGlobalLayoutListener(object :
                        OnGlobalLayoutListener {
                        override fun onGlobalLayout() {
                            binding.recyclerView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                            runWithView {
                                layoutManager!!.scrollToPositionWithOffset(args.position, 0)
                            }
                        }
                    })
                    binding.progressBar.isVisible = false
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
        return SnackbarManager(binding.root).apply {
            setSwipeDismissEnabled(true)
            setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE)
        }
    }
}