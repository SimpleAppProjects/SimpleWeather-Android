package com.thewizrd.simpleweather.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.Observable
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.DateTimeConstants
import com.thewizrd.shared_resources.controls.AirQualityViewModel
import com.thewizrd.shared_resources.controls.WeatherNowViewModel
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrColor
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrResourceId
import com.thewizrd.shared_resources.utils.ContextUtils.isLargeTablet
import com.thewizrd.shared_resources.weatherdata.WeatherDataLoader
import com.thewizrd.shared_resources.weatherdata.WeatherManager
import com.thewizrd.shared_resources.weatherdata.WeatherRequest
import com.thewizrd.shared_resources.weatherdata.model.AirQuality
import com.thewizrd.simpleweather.BR
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.adapters.AQIForecastAdapter
import com.thewizrd.simpleweather.adapters.AQIForecastGraphAdapter
import com.thewizrd.simpleweather.adapters.CurrentAQIAdapter
import com.thewizrd.simpleweather.controls.graphs.BarGraphData
import com.thewizrd.simpleweather.controls.graphs.BarGraphDataSet
import com.thewizrd.simpleweather.controls.graphs.BarGraphEntry
import com.thewizrd.simpleweather.controls.graphs.YEntryData
import com.thewizrd.simpleweather.controls.viewmodels.AirQualityForecastViewModel
import com.thewizrd.simpleweather.databinding.FragmentWeatherListBinding
import com.thewizrd.simpleweather.databinding.LayoutLocationHeaderBinding
import com.thewizrd.simpleweather.fragments.ToolbarFragment
import com.thewizrd.simpleweather.snackbar.Snackbar
import com.thewizrd.simpleweather.snackbar.SnackbarManager
import de.twoid.ui.decoration.InsetItemDecoration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.time.LocalDate
import java.time.ZoneOffset

class WeatherAQIFragment : ToolbarFragment() {
    private val weatherView: WeatherNowViewModel by activityViewModels()
    private val aqiView: AirQualityForecastViewModel by viewModels()
    private var locationData: LocationData? = null

    private lateinit var binding: FragmentWeatherListBinding
    private lateinit var headerBinding: LayoutLocationHeaderBinding
    private lateinit var currentAQIAdapter: CurrentAQIAdapter
    private lateinit var aqiForecastAdapter: ListAdapter<*, *>

    private lateinit var args: WeatherAQIFragmentArgs

    private val wm = WeatherManager.instance

    init {
        arguments = Bundle()
    }

    companion object {
        fun newInstance(locData: LocationData): WeatherAQIFragment {
            val fragment = WeatherAQIFragment()
            fragment.locationData = locData
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AnalyticsLogger.logEvent("WeatherAQIFragment: onCreate")

        args = WeatherAQIFragmentArgs.fromBundle(requireArguments())

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

    override fun getScrollTargetViewId(): Int {
        return binding.recyclerView.id
    }

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
        binding.recyclerView.layoutManager = LinearLayoutManager(appCompatActivity).also {
            if (requireContext().isLargeTablet()) {
                val context = requireContext()
                val maxWidth = context.resources.getDimension(R.dimen.wnow_max_view_width)
                binding.recyclerView.addItemDecoration(InsetItemDecoration(it, maxWidth))
            }
        }
        binding.recyclerView.adapter = ConcatAdapter(
                CurrentAQIAdapter().also {
                    currentAQIAdapter = it
                },
                if (requireContext().isLargeTablet()) {
                    AQIForecastAdapter().also {
                        aqiForecastAdapter = it
                    }
                } else {
                    AQIForecastGraphAdapter().also {
                        aqiForecastAdapter = it
                    }
                }
        )

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        args = WeatherAQIFragmentArgs.fromBundle(requireArguments())

        binding.progressBar.visibility = View.VISIBLE

        weatherView.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable, propertyId: Int) {
                if (propertyId == 0 || propertyId == BR.airQuality) {
                    currentAQIAdapter.updateItem(weatherView.airQuality)
                }
            }

        })

        aqiView.getAQIForecastData().observe(viewLifecycleOwner, {
            val forecastList = it?.filterNot { item ->
                item.date.isBefore(LocalDate.now(locationData?.tzOffset
                        ?: ZoneOffset.systemDefault()))
            }

            aqiForecastAdapter.let { adapter ->
                if (adapter is AQIForecastAdapter) {
                    adapter.submitList(forecastList?.map { item -> AirQualityViewModel(item) })
                } else if (adapter is AQIForecastGraphAdapter) {
                    adapter.submitList(forecastList?.createGraphData())
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (!isHidden) {
            AnalyticsLogger.logEvent("WeatherAQIFragment: onResume")
            initialize()
        }
    }

    override fun onPause() {
        AnalyticsLogger.logEvent("WeatherAQIFragment: onPause")
        super.onPause()
    }

    override fun getTitle(): Int {
        return R.string.label_airquality
    }

    private fun initialize() {
        runWithView {
            if (locationData == null) locationData = getSettingsManager().getHomeData()

            if (!weatherView.isValid || locationData != null && locationData!!.query != weatherView.query) {
                runWithView(Dispatchers.Default) {
                    supervisorScope {
                        val weather = WeatherDataLoader(locationData!!).loadWeatherData(
                                WeatherRequest.Builder()
                                        .forceLoadSavedData()
                                        .setErrorListener { wEx ->
                                            when (wEx.errorStatus) {
                                                ErrorStatus.NETWORKERROR, ErrorStatus.NOWEATHER -> {
                                                    // Show error message and prompt to refresh
                                                    showSnackbar(
                                                            Snackbar.make(
                                                                    wEx.message,
                                                                    Snackbar.Duration.LONG
                                                            ), null
                                                    )
                                                }
                                                ErrorStatus.QUERYNOTFOUND -> {
                                                    if (!wm.isRegionSupported(locationData!!.countryCode)) {
                                                        showSnackbar(
                                                                Snackbar.make(
                                                                        R.string.error_message_weather_region_unsupported,
                                                                        Snackbar.Duration.LONG
                                                                ), null
                                                        )
                                                        return@setErrorListener
                                                    }
                                                    // Show error message
                                                    showSnackbar(
                                                            Snackbar.make(
                                                                    wEx.message,
                                                                    Snackbar.Duration.LONG
                                                            ), null
                                                    )
                                                }
                                                else -> {
                                                    showSnackbar(
                                                            Snackbar.make(
                                                                    wEx.message,
                                                                    Snackbar.Duration.LONG
                                                            ), null
                                                    )
                                                }
                                            }
                                        }.build()
                        )

                        ensureActive()

                        launch(Dispatchers.Main) {
                            weatherView.updateView(weather)
                            aqiView.updateForecasts(locationData!!)
                            headerBinding.locationName.text = weatherView.location
                        }
                    }
                }
            } else {
                aqiView.updateForecasts(locationData!!)
                headerBinding.locationName.text = weatherView.location
            }

            binding.progressBar.visibility = View.GONE

            currentAQIAdapter.updateItem(weatherView.airQuality)
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

        if (appCompatActivity == null) return

        var backgroundColor = appCompatActivity!!.getAttrColor(android.R.attr.colorBackground)
        if (getSettingsManager().getUserThemeMode() == UserThemeMode.AMOLED_DARK) {
            backgroundColor = Colors.BLACK
        }

        binding.recyclerView.setBackgroundColor(backgroundColor)
    }

    override fun createSnackManager(): SnackbarManager {
        return SnackbarManager(binding.root).apply {
            setSwipeDismissEnabled(true)
            setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE)
        }
    }

    private fun List<AirQuality>?.createGraphData(): List<BarGraphData> {
        val graphDataList = mutableListOf<BarGraphData>()
        var aqiIndexData: BarGraphData? = null
        var pm25Data: BarGraphData? = null
        var pm10Data: BarGraphData? = null
        var o3Data: BarGraphData? = null
        var coData: BarGraphData? = null
        var no2Data: BarGraphData? = null
        var so2Data: BarGraphData? = null

        this?.forEach { aqi ->
            if (aqi.index != null) {
                if (aqiIndexData == null) {
                    aqiIndexData = BarGraphData().apply {
                        graphLabel = requireContext().getString(R.string.label_airquality)
                    }
                }

                if (aqiIndexData?.getDataSet() == null) {
                    aqiIndexData?.setDataSet(BarGraphDataSet(mutableListOf()).apply {
                        setMinMax(0f)
                    })
                }

                aqiIndexData?.getDataSet()?.addEntry(BarGraphEntry().apply {
                    xLabel = aqi.date.format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.ABBREV_DAY_OF_THE_WEEK))
                    entryData = YEntryData(aqi.index.toFloat(), aqi.index.toString())
                    fillColor = AirQualityUtils.getColorFromIndex(aqi.index)
                })
            }

            if (aqi.pm25 != null) {
                if (pm25Data == null) {
                    pm25Data = BarGraphData().apply {
                        graphLabel = requireContext().getString(R.string.units_pm25)
                    }
                }

                if (pm25Data?.getDataSet() == null) {
                    pm25Data?.setDataSet(BarGraphDataSet(mutableListOf()).apply {
                        setMinMax(0f)
                    })
                }

                pm25Data?.getDataSet()?.addEntry(BarGraphEntry().apply {
                    xLabel = aqi.date.format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.ABBREV_DAY_OF_THE_WEEK))
                    entryData = YEntryData(aqi.pm25.toFloat(), aqi.pm25.toString())
                    fillColor = AirQualityUtils.getColorFromIndex(aqi.pm25)
                })
            }

            if (aqi.pm10 != null) {
                if (pm10Data == null) {
                    pm10Data = BarGraphData().apply {
                        graphLabel = requireContext().getString(R.string.units_pm10)
                    }
                }

                if (pm10Data?.getDataSet() == null) {
                    pm10Data?.setDataSet(BarGraphDataSet(mutableListOf()).apply {
                        setMinMax(0f)
                    })
                }

                pm10Data?.getDataSet()?.addEntry(BarGraphEntry().apply {
                    xLabel = aqi.date.format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.ABBREV_DAY_OF_THE_WEEK))
                    entryData = YEntryData(aqi.pm10.toFloat(), aqi.pm10.toString())
                    fillColor = AirQualityUtils.getColorFromIndex(aqi.pm10)
                })
            }

            if (aqi.o3 != null) {
                if (o3Data == null) {
                    o3Data = BarGraphData().apply {
                        graphLabel = requireContext().getString(R.string.units_o3)
                    }
                }

                if (o3Data?.getDataSet() == null) {
                    o3Data?.setDataSet(BarGraphDataSet(mutableListOf()).apply {
                        setMinMax(0f)
                    })
                }

                o3Data?.getDataSet()?.addEntry(BarGraphEntry().apply {
                    xLabel = aqi.date.format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.ABBREV_DAY_OF_THE_WEEK))
                    entryData = YEntryData(aqi.o3.toFloat(), aqi.o3.toString())
                    fillColor = AirQualityUtils.getColorFromIndex(aqi.o3)
                })
            }

            if (aqi.co != null) {
                if (coData == null) {
                    coData = BarGraphData().apply {
                        graphLabel = requireContext().getString(R.string.units_co)
                    }
                }

                if (coData?.getDataSet() == null) {
                    coData?.setDataSet(BarGraphDataSet(mutableListOf()).apply {
                        setMinMax(0f)
                    })
                }

                coData?.getDataSet()?.addEntry(BarGraphEntry().apply {
                    xLabel = aqi.date.format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.ABBREV_DAY_OF_THE_WEEK))
                    entryData = YEntryData(aqi.co.toFloat(), aqi.co.toString())
                    fillColor = AirQualityUtils.getColorFromIndex(aqi.co)
                })
            }

            if (aqi.no2 != null) {
                if (no2Data == null) {
                    no2Data = BarGraphData().apply {
                        graphLabel = requireContext().getString(R.string.units_no2)
                    }
                }

                if (no2Data?.getDataSet() == null) {
                    no2Data?.setDataSet(BarGraphDataSet(mutableListOf()).apply {
                        setMinMax(0f)
                    })
                }

                no2Data?.getDataSet()?.addEntry(BarGraphEntry().apply {
                    xLabel = aqi.date.format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.ABBREV_DAY_OF_THE_WEEK))
                    entryData = YEntryData(aqi.no2.toFloat(), aqi.no2.toString())
                    fillColor = AirQualityUtils.getColorFromIndex(aqi.no2)
                })
            }

            if (aqi.so2 != null) {
                if (so2Data == null) {
                    so2Data = BarGraphData().apply {
                        graphLabel = requireContext().getString(R.string.units_so2)
                    }
                }

                if (so2Data?.getDataSet() == null) {
                    so2Data?.setDataSet(BarGraphDataSet(mutableListOf()).apply {
                        setMinMax(0f)
                    })
                }

                so2Data?.getDataSet()?.addEntry(BarGraphEntry().apply {
                    xLabel = aqi.date.format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.ABBREV_DAY_OF_THE_WEEK))
                    entryData = YEntryData(aqi.so2.toFloat(), aqi.so2.toString())
                    fillColor = AirQualityUtils.getColorFromIndex(aqi.so2)
                })
            }
        }

        aqiIndexData?.let { d -> d.notifyDataChanged(); graphDataList.add(d) }
        pm25Data?.let { d -> d.notifyDataChanged(); graphDataList.add(d) }
        pm10Data?.let { d -> d.notifyDataChanged(); graphDataList.add(d) }
        o3Data?.let { d -> d.notifyDataChanged(); graphDataList.add(d) }
        coData?.let { d -> d.notifyDataChanged(); graphDataList.add(d) }
        no2Data?.let { d -> d.notifyDataChanged(); graphDataList.add(d) }
        so2Data?.let { d -> d.notifyDataChanged(); graphDataList.add(d) }

        return graphDataList
    }
}