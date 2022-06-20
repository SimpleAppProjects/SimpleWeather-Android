package com.thewizrd.simpleweather.main

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.thewizrd.common.controls.WeatherNowViewModel
import com.thewizrd.common.weatherdata.WeatherDataLoader
import com.thewizrd.common.weatherdata.WeatherRequest
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.exceptions.ErrorStatus
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrColor
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrResourceId
import com.thewizrd.shared_resources.utils.ContextUtils.isLargeTablet
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.shared_resources.weatherdata.model.MinutelyForecast
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.adapters.ChartsItemAdapter
import com.thewizrd.simpleweather.controls.graphs.LineDataSeries
import com.thewizrd.simpleweather.controls.viewmodels.ChartsViewModel
import com.thewizrd.simpleweather.controls.viewmodels.ForecastGraphViewModel
import com.thewizrd.simpleweather.databinding.FragmentWeatherListBinding
import com.thewizrd.simpleweather.databinding.LayoutLocationHeaderBinding
import com.thewizrd.simpleweather.fragments.ToolbarFragment
import com.thewizrd.simpleweather.snackbar.Snackbar
import com.thewizrd.simpleweather.snackbar.SnackbarManager
import com.thewizrd.weather_api.weatherModule
import de.twoid.ui.decoration.InsetItemDecoration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlin.math.max

class WeatherChartsFragment : ToolbarFragment() {
    private val weatherView: WeatherNowViewModel by activityViewModels()
    private val chartsView: ChartsViewModel by viewModels()
    private var locationData: LocationData? = null

    private lateinit var binding: FragmentWeatherListBinding
    private lateinit var headerBinding: LayoutLocationHeaderBinding
    private lateinit var adapter: ChartsItemAdapter

    private lateinit var args: WeatherChartsFragmentArgs

    private val wm = weatherModule.weatherManager

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

        args = WeatherChartsFragmentArgs.fromBundle(requireArguments())

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
        headerBinding = LayoutLocationHeaderBinding.inflate(inflater, appBarLayout, true)
        binding.lifecycleOwner = viewLifecycleOwner

        // Setup Actionbar
        toolbar.setNavigationIcon(toolbar.context.getAttrResourceId(R.attr.homeAsUpIndicator))
        toolbar.setNavigationOnClickListener { v -> v.findNavController().navigateUp() }

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
        binding.recyclerView.adapter = ChartsItemAdapter().also {
            adapter = it
        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        args = WeatherChartsFragmentArgs.fromBundle(requireArguments())

        binding.progressBar.visibility = View.VISIBLE

        chartsView.getForecastData().observe(viewLifecycleOwner) {
            adapter.submitList(createGraphModelData(it?.first, it?.second))
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isHidden) {
            AnalyticsLogger.logEvent("WeatherChartsFragment: onResume")
            initialize()
        }
    }

    override fun onPause() {
        AnalyticsLogger.logEvent("WeatherChartsFragment: onPause")
        super.onPause()
    }

    override val titleResId: Int
        get() = R.string.label_forecast

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
                                chartsView.updateForecasts(locData)
                                headerBinding.locationName.text = weatherView.location
                            }
                        }
                    }
                }
            } else {
                locationData?.let {
                    chartsView.updateForecasts(it)
                }
                headerBinding.locationName.text = weatherView.location
            }

            binding.progressBar.visibility = View.GONE
        }
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
        val graphTypes = ForecastGraphViewModel.ForecastGraphType.values()
        val data = ArrayList<ForecastGraphViewModel>(graphTypes.size)

        if (!minfcasts.isNullOrEmpty()) {
            data.add(ForecastGraphViewModel().apply {
                setMinutelyForecastData(minfcasts)
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
                        popData = ForecastGraphViewModel()
                    }
                    if (hrfcasts.firstOrNull()?.windMph != null && hrfcasts.firstOrNull()?.windKph != null ||
                        hrfcasts.lastOrNull()?.windMph != null && hrfcasts.lastOrNull()?.windKph != null
                    ) {
                        windData = ForecastGraphViewModel()
                    }
                    if (hrfcasts.firstOrNull()?.extras?.qpfRainIn != null && hrfcasts.firstOrNull()?.extras?.qpfRainMm != null ||
                        hrfcasts.lastOrNull()?.extras?.qpfRainIn != null && hrfcasts.lastOrNull()?.extras?.qpfRainMm != null
                    ) {
                        rainData = ForecastGraphViewModel()
                    }
                    if (hrfcasts.firstOrNull()?.extras?.qpfSnowIn != null && hrfcasts.firstOrNull()?.extras?.qpfSnowCm != null ||
                        hrfcasts.lastOrNull()?.extras?.qpfSnowIn != null && hrfcasts.lastOrNull()?.extras?.qpfSnowCm != null
                    ) {
                        snowData = ForecastGraphViewModel()
                    }
                    if (hrfcasts.firstOrNull()?.extras?.uvIndex != null || hrfcasts.lastOrNull()?.extras?.uvIndex != null) {
                        uviData = ForecastGraphViewModel()
                    }
                    if (hrfcasts.firstOrNull()?.extras?.humidity != null || hrfcasts.lastOrNull()?.extras?.humidity != null) {
                        humidityData = ForecastGraphViewModel()
                    }
                }

                //tempData?.addForecastData(hrfcast, ForecastGraphViewModel.ForecastGraphType.TEMPERATURE)
                if (popData != null) {
                    if (hrfcast.extras?.pop != null) {
                        popData.addForecastData(
                            hrfcast,
                            ForecastGraphViewModel.ForecastGraphType.PRECIPITATION
                        )
                    }
                }
                if (windData != null) {
                    if (hrfcast.windMph != null && hrfcast.windKph != null) {
                        windData.addForecastData(
                            hrfcast,
                            ForecastGraphViewModel.ForecastGraphType.WIND
                        )
                    }
                }
                if (rainData != null) {
                    if (hrfcast.extras?.qpfRainIn != null && hrfcast.extras?.qpfRainMm != null) {
                        rainData.addForecastData(
                            hrfcast,
                            ForecastGraphViewModel.ForecastGraphType.RAIN
                        )
                    }
                }
                if (snowData != null) {
                    if (hrfcast.extras?.qpfSnowIn != null && hrfcast.extras?.qpfSnowCm != null) {
                        snowData.addForecastData(
                            hrfcast,
                            ForecastGraphViewModel.ForecastGraphType.SNOW
                        )
                    }
                }
                if (uviData != null) {
                    if (hrfcast.extras?.uvIndex != null) {
                        uviData.addForecastData(
                            hrfcast,
                            ForecastGraphViewModel.ForecastGraphType.UVINDEX
                        )
                    }
                }
                if (humidityData != null) {
                    if (hrfcast.extras?.humidity != null) {
                        humidityData.addForecastData(
                            hrfcast,
                            ForecastGraphViewModel.ForecastGraphType.HUMIDITY
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
                rainData?.graphData?.dataSets?.forEach {
                    if (it is LineDataSeries) {
                        // Heavy rain — rate is >= 7.6 mm (0.30 in) per hr
                        when (settingsManager.getPrecipitationUnit()) {
                            Units.INCHES -> it.setSeriesMinMax(0f, max(it.yMax, 0.3f))
                            Units.MILLIMETERS -> it.setSeriesMinMax(0f, max(it.yMax, 7.6f))
                            else -> it.setSeriesMinMax(0f, max(it.yMax, 0.3f))
                        }
                    }
                }
                rainData?.graphData?.notifyDataChanged()
                data.add(rainData!!)
            }
            if ((snowData?.graphData?.dataCount ?: 0) > 0) {
                snowData?.graphData?.dataSets?.forEach {
                    if (it is LineDataSeries) {
                        // Snow will often accumulate at a rate of 0.5in (12.7mm) an hour
                        when (settingsManager.getPrecipitationUnit()) {
                            Units.INCHES -> it.setSeriesMinMax(0f, max(it.yMax, 0.5f))
                            Units.MILLIMETERS -> it.setSeriesMinMax(0f, max(it.yMax, 12.7f))
                            else -> it.setSeriesMinMax(0f, max(it.yMax, 0.3f))
                        }
                    }
                }
                snowData?.graphData?.notifyDataChanged()
                data.add(snowData!!)
            }
        }

        return data
    }
}