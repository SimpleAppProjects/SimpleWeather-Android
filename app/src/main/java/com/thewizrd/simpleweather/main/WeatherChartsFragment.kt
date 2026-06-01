package com.thewizrd.simpleweather.main

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.thewizrd.common.helpers.SimpleRecyclerViewAdapterObserver
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrColor
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrResourceId
import com.thewizrd.shared_resources.utils.ContextUtils.isLargeTablet
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.UserThemeMode
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.shared_resources.weatherdata.model.MinutelyForecast
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.adapters.ChartsItemAdapter
import com.thewizrd.simpleweather.adapters.SpacerAdapter
import com.thewizrd.simpleweather.controls.viewmodels.ChartsViewModel
import com.thewizrd.simpleweather.controls.viewmodels.ForecastGraphViewModel
import com.thewizrd.simpleweather.controls.viewmodels.ForecastGraphViewModel.GraphType
import com.thewizrd.simpleweather.controls.viewmodels.ForecastType
import com.thewizrd.simpleweather.databinding.FragmentWeatherListBinding
import com.thewizrd.simpleweather.fragments.CollapsingToolbarFragment
import com.thewizrd.simpleweather.review.InAppReviewManager
import com.thewizrd.simpleweather.snackbar.SnackbarManager
import com.thewizrd.simpleweather.utils.NavigationUtils.navControllerViewModels
import com.thewizrd.simpleweather.viewmodels.TwoPaneStateViewModel
import com.thewizrd.simpleweather.viewmodels.WeatherNowViewModel
import de.twoid.ui.decoration.InsetItemDecoration
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.thewizrd.shared_resources.R as sharedRes

class WeatherChartsFragment : CollapsingToolbarFragment() {
    private val wNowViewModel: WeatherNowViewModel by activityViewModels()
    private val chartsView: ChartsViewModel by viewModels()
    private val twoPaneStateViewModel: TwoPaneStateViewModel by navControllerViewModels(R.id.two_pane_nav_graph)

    private var locationData: LocationData? = null

    private lateinit var binding: FragmentWeatherListBinding
    private lateinit var adapter: ChartsItemAdapter

    private val args: WeatherChartsFragmentArgs by navArgs()

    private var dataJob: Job? = null

    private lateinit var inAppReviewManager: InAppReviewManager

    init {
        arguments = Bundle()
    }

    companion object {
        fun newInstance(locData: LocationData): WeatherChartsFragment {
            val fragment = WeatherChartsFragment()
            fragment.locationData = locData
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AnalyticsLogger.logEvent("WeatherChartsFragment: onCreate")

        enterTransition = null
        exitTransition = null

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(Constants.KEY_DATA)) {
                locationData = JSONParser.deserializer(
                    savedInstanceState.getString(Constants.KEY_DATA),
                    LocationData::class.java
                )
            }
        } else {
            if (args.data != null) {
                locationData = JSONParser.deserializer(args.data, LocationData::class.java)
            }
        }

        inAppReviewManager = InAppReviewManager.create(requireContext())
    }

    override val scrollTargetViewId: Int
        get() = binding.recyclerView.id

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = super.onCreateView(inflater, container, savedInstanceState) as ViewGroup?
        // Use this to return your custom view for this Fragment
        binding = FragmentWeatherListBinding.inflate(inflater, root, true)
        binding.lifecycleOwner = viewLifecycleOwner

        // Setup Actionbar
        toolbar.setNavigationIcon(toolbar.context.getAttrResourceId(androidx.appcompat.R.attr.homeAsUpIndicator))
        toolbar.setNavigationOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the binding.recyclerView
        binding.recyclerView.setHasFixedSize(true)
        // use a linear layout manager
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext()).also {
            if (requireContext().isLargeTablet()) {
                val context = requireContext()
                val maxWidth = context.resources.getDimension(R.dimen.wnow_max_view_width)
                binding.recyclerView.addItemDecoration(InsetItemDecoration(it, maxWidth))
            }
        }
        binding.recyclerView.adapter = ConcatAdapter(
            SpacerAdapter(binding.recyclerView.context.dpToPx(4f).toInt()),
            ChartsItemAdapter().also {
                adapter = it
            },
            SpacerAdapter(binding.recyclerView.context.dpToPx(4f).toInt())
        )

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.progressBar.show()

        adapter.registerAdapterDataObserver(object : SimpleRecyclerViewAdapterObserver() {
            override fun onChanged() {
                adapter.unregisterAdapterDataObserver(this)
                inAppReviewManager.incrementCounter()
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            twoPaneStateViewModel.twoPaneState.collectLatest { state ->
                setNavigationIconVisible(!state.isSideBySide)
                toolbar.subtitle = if (!state.isSideBySide) {
                    wNowViewModel.uiState.value.weather?.location
                } else {
                    ""
                }
            }
        }

        if (args.data.isNullOrBlank() && savedInstanceState?.containsKey(Constants.KEY_DATA) != true) {
            viewLifecycleOwner.lifecycleScope.launch {
                wNowViewModel.uiState.collect {
                    val oldData = locationData
                    locationData = it.locationData

                    toolbar.subtitle = if (!twoPaneStateViewModel.twoPaneState.value.isSideBySide) {
                        wNowViewModel.uiState.value.weather?.location
                    } else {
                        ""
                    }

                    if (oldData != locationData) {
                        initialize()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                initialize()
            }
        }

        // Show review prompt when applicable
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                runCatching {
                    delay(5000)

                    val paneIsOpened = twoPaneStateViewModel.twoPaneState.value.isOpened
                    if (isActive && isVisible && paneIsOpened && isViewAlive && inAppReviewManager.shouldShowReviewFlow()) {
                        // Wait for no movement
                        while (isActive && binding.recyclerView.scrollState != RecyclerView.SCROLL_STATE_IDLE) {
                            delay(2500)
                        }

                        if (isActive) {
                            activity?.run {
                                inAppReviewManager.showReviewFlow(this)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsLogger.logEvent("WeatherChartsFragment: onResume")
    }

    override fun onPause() {
        dataJob?.cancel()
        AnalyticsLogger.logEvent("WeatherChartsFragment: onPause")
        super.onPause()
    }

    override val titleResId: Int
        get() = sharedRes.string.label_forecast

    private fun initialize() {
        if (locationData == null) {
            locationData = wNowViewModel.uiState.value.locationData
        }

        locationData?.let {
            chartsView.updateForecasts(it)
        }

        dataJob?.cancel()

        dataJob = runWithView {
            chartsView.getForecastData().collect {
                adapter.submitList(createGraphModelData(it?.first, it?.second))
            }
        }

        binding.progressBar.hide()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // Save data
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

    private fun createGraphModelData(
        minfcasts: List<MinutelyForecast>?,
        hrfcasts: List<HourlyForecast>?
    ): List<ForecastGraphViewModel> {
        val ctx = requireContext()

        val graphTypes = ForecastType.entries
        val data = ArrayList<ForecastGraphViewModel>(graphTypes.size)

        if (!minfcasts.isNullOrEmpty()) {
            data.add(ForecastGraphViewModel(ctx).apply {
                setMinutelyForecastData(minfcasts, GraphType.Bar)
            })
        }

        if (!hrfcasts.isNullOrEmpty()) {
            // TODO: replace with SortedMap?
            //var tempData: ForecastGraphViewModel? = null
            var popData: ForecastGraphViewModel? = null
            var windData: ForecastGraphViewModel? = null
            var rainData: ForecastGraphViewModel? = null
            var snowData: ForecastGraphViewModel? = null
            var uviData: ForecastGraphViewModel? = null
            var humidityData: ForecastGraphViewModel? = null

            for (i in hrfcasts.indices) {
                val hrfcast = hrfcasts[i]

                if (i == 0) {
                    //tempData = ForecastGraphViewModel()

                    if (hrfcasts.firstOrNull()?.extras?.pop != null || hrfcasts.lastOrNull()?.extras?.pop != null) {
                        popData = ForecastGraphViewModel(ctx)
                    }
                    if (hrfcasts.firstOrNull()?.extras?.windMph != null && hrfcasts.firstOrNull()?.extras?.windKph != null ||
                        hrfcasts.lastOrNull()?.extras?.windMph != null && hrfcasts.lastOrNull()?.extras?.windKph != null
                    ) {
                        windData = ForecastGraphViewModel(ctx)
                    }
                    if (hrfcasts.firstOrNull()?.extras?.qpfRainIn != null && hrfcasts.firstOrNull()?.extras?.qpfRainMm != null ||
                        hrfcasts.lastOrNull()?.extras?.qpfRainIn != null && hrfcasts.lastOrNull()?.extras?.qpfRainMm != null
                    ) {
                        rainData = ForecastGraphViewModel(ctx)
                    }
                    if (hrfcasts.firstOrNull()?.extras?.qpfSnowIn != null && hrfcasts.firstOrNull()?.extras?.qpfSnowCm != null ||
                        hrfcasts.lastOrNull()?.extras?.qpfSnowIn != null && hrfcasts.lastOrNull()?.extras?.qpfSnowCm != null
                    ) {
                        snowData = ForecastGraphViewModel(ctx)
                    }
                    if (hrfcasts.firstOrNull()?.extras?.uvIndex != null || hrfcasts.lastOrNull()?.extras?.uvIndex != null) {
                        uviData = ForecastGraphViewModel(ctx)
                    }
                    if (hrfcasts.firstOrNull()?.extras?.humidity != null || hrfcasts.lastOrNull()?.extras?.humidity != null) {
                        humidityData = ForecastGraphViewModel(ctx)
                    }
                }

                //tempData?.addForecastData(hrfcast, ForecastGraphViewModel.ForecastGraphType.TEMPERATURE)
                if (popData != null) {
                    if (hrfcast.extras?.pop != null) {
                        popData.addForecastData(
                            hrfcast,
                            ForecastType.PRECIPITATION,
                            GraphType.Bar
                        )
                    }
                }
                if (windData != null) {
                    if (hrfcast.extras?.windMph != null && hrfcast.extras?.windKph != null) {
                        windData.addForecastData(
                            hrfcast,
                            ForecastType.WIND,
                            GraphType.Bar
                        )
                    }
                }
                if (rainData != null) {
                    if (hrfcast.extras?.qpfRainIn != null && hrfcast.extras?.qpfRainMm != null) {
                        rainData.addForecastData(
                            hrfcast,
                            ForecastType.RAIN,
                            GraphType.Bar
                        )
                    }
                }
                if (snowData != null) {
                    if (hrfcast.extras?.qpfSnowIn != null && hrfcast.extras?.qpfSnowCm != null) {
                        snowData.addForecastData(
                            hrfcast,
                            ForecastType.SNOW,
                            GraphType.Bar
                        )
                    }
                }
                if (uviData != null) {
                    if (hrfcast.extras?.uvIndex != null) {
                        uviData.addForecastData(
                            hrfcast,
                            ForecastType.UVINDEX,
                            GraphType.Bar
                        )
                    }
                }
                if (humidityData != null) {
                    if (hrfcast.extras?.humidity != null) {
                        humidityData.addForecastData(
                            hrfcast,
                            ForecastType.HUMIDITY,
                            GraphType.Bar
                        )
                    }
                }
            }

            /*
            if (tempData?.graphData?.dataCount ?: 0 > 0) {
                data.add(tempData!!)
            }
             */
            if ((popData?.graphData?.dataCount ?: 0) > 0) {
                data.add(popData!!)
            }
            if ((windData?.graphData?.dataCount ?: 0) > 0) {
                data.add(windData!!)
            }
            if ((humidityData?.graphData?.dataCount ?: 0) > 0) {
                data.add(humidityData!!)
            }
            if ((uviData?.graphData?.dataCount ?: 0) > 0) {
                data.add(uviData!!)
            }
            if ((rainData?.graphData?.dataCount ?: 0) > 0) {
                rainData?.updateDataSetMinMax()
                data.add(rainData!!)
            }
            if ((snowData?.graphData?.dataCount ?: 0) > 0) {
                snowData?.updateDataSetMinMax()
                data.add(snowData!!)
            }
        }

        return data
    }
}