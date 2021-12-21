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
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.controls.WeatherNowViewModel
import com.thewizrd.shared_resources.helpers.SpacerItemDecoration
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrColor
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrResourceId
import com.thewizrd.shared_resources.weatherdata.WeatherDataLoader
import com.thewizrd.shared_resources.weatherdata.WeatherManager
import com.thewizrd.shared_resources.weatherdata.WeatherRequest
import com.thewizrd.simpleweather.BR
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.adapters.AQIForecastAdapter
import com.thewizrd.simpleweather.adapters.CurrentAQIAdapter
import com.thewizrd.simpleweather.controls.viewmodels.AirQualityForecastViewModel
import com.thewizrd.simpleweather.databinding.FragmentWeatherListBinding
import com.thewizrd.simpleweather.databinding.LayoutLocationHeaderBinding
import com.thewizrd.simpleweather.fragments.ToolbarFragment
import com.thewizrd.simpleweather.snackbar.Snackbar
import com.thewizrd.simpleweather.snackbar.SnackbarManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class WeatherAQIFragment : ToolbarFragment() {
    private val weatherView: WeatherNowViewModel by activityViewModels()
    private val aqiView: AirQualityForecastViewModel by viewModels()
    private var locationData: LocationData? = null

    private lateinit var binding: FragmentWeatherListBinding
    private lateinit var headerBinding: LayoutLocationHeaderBinding
    private lateinit var currentAQIAdapter: CurrentAQIAdapter
    private lateinit var aqiForecastAdapter: AQIForecastAdapter

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
        binding.recyclerView.layoutManager = LinearLayoutManager(appCompatActivity)
        binding.recyclerView.adapter = ConcatAdapter(
                CurrentAQIAdapter().also {
                    currentAQIAdapter = it
                },
                AQIForecastAdapter().also {
                    aqiForecastAdapter = it
                }
        )

        binding.recyclerView.addItemDecoration(SpacerItemDecoration(
                verticalSpace = requireContext().dpToPx(8f).toInt()
        ))

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
            aqiForecastAdapter.submitList(it)
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
}