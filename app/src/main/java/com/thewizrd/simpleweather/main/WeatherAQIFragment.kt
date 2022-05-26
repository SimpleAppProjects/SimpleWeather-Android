package com.thewizrd.simpleweather.main

import android.app.Activity
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
import com.thewizrd.common.controls.AirQualityViewModel
import com.thewizrd.common.controls.WeatherNowViewModel
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
import com.thewizrd.shared_resources.utils.ContextUtils.isLargeTablet
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.UserThemeMode
import com.thewizrd.simpleweather.BR
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.adapters.AQIForecastAdapter
import com.thewizrd.simpleweather.adapters.AQIForecastGraphAdapter
import com.thewizrd.simpleweather.adapters.CurrentAQIAdapter
import com.thewizrd.simpleweather.controls.viewmodels.AirQualityForecastViewModel
import com.thewizrd.simpleweather.controls.viewmodels.createGraphData
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

    private val wm = weatherModule.weatherManager

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

        aqiView.getAQIForecastData().observe(viewLifecycleOwner) {
            val forecastList = it?.filterNot { item ->
                item.date.isBefore(
                    LocalDate.now(
                        locationData?.tzOffset
                            ?: ZoneOffset.systemDefault()
                    )
                )
            }

            aqiForecastAdapter.let { adapter ->
                if (adapter is AQIForecastAdapter) {
                    adapter.submitList(forecastList?.filter { it.index != null }
                        ?.map { item -> AirQualityViewModel(item) })
                } else if (adapter is AQIForecastGraphAdapter) {
                    adapter.submitList(forecastList?.createGraphData(requireContext()))
                }
            }
        }
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

    override val titleResId: Int
        get() = R.string.label_airquality

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
                                aqiView.updateForecasts(locData)
                                headerBinding.locationName.text = weatherView.location
                            }
                        }
                    }
                }
            } else {
                locationData?.let {
                    aqiView.updateForecasts(it)
                }
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

        activity?.let {
            var backgroundColor = it.getAttrColor(android.R.attr.colorBackground)
            if (settingsManager.getUserThemeMode() == UserThemeMode.AMOLED_DARK) {
                backgroundColor = Colors.BLACK
            }

            binding.recyclerView.setBackgroundColor(backgroundColor)
        }
    }

    override fun createSnackManager(activity: Activity): SnackbarManager? {
        return SnackbarManager(binding.root).apply {
            setSwipeDismissEnabled(true)
            setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE)
        }
    }
}