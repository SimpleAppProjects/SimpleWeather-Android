package com.thewizrd.simpleweather.main

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.wear.compose.material.*
import com.google.android.horologist.compose.layout.fadeAwayScalingLazyList
import com.thewizrd.common.controls.ForecastsListViewModel
import com.thewizrd.common.controls.WeatherAlertsViewModel
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.DateTimeConstants
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.simpleweather.controls.ForecastPanelsViewModel
import com.thewizrd.simpleweather.fragments.SwipeDismissFragment
import com.thewizrd.simpleweather.ui.components.WeatherAlertPanel
import com.thewizrd.simpleweather.ui.components.WeatherForecastPanel
import com.thewizrd.simpleweather.ui.components.WeatherHourlyForecastPanel
import com.thewizrd.simpleweather.ui.components.WeatherMinutelyForecastPanel
import com.thewizrd.simpleweather.ui.paging.items
import com.thewizrd.simpleweather.ui.theme.WearAppTheme

class WeatherListFragment : SwipeDismissFragment() {
    companion object {
        fun newInstance(
            type: WeatherListType,
            data: String? = null,
            scrollToPosition: Int? = null
        ): WeatherListFragment {
            val b = Bundle(3)
            b.putSerializable(Constants.ARGS_WEATHERLISTTYPE, type)
            if (data != null) {
                b.putString(Constants.KEY_DATA, data)
            }
            if (scrollToPosition != null) {
                b.putInt(Constants.KEY_POSITION, scrollToPosition)
            }

            return WeatherListFragment().apply {
                arguments = b
            }
        }
    }

    private val forecastsView: ForecastsListViewModel by activityViewModels()
    private val forecastsPanelView: ForecastPanelsViewModel by activityViewModels()
    private val alertsView: WeatherAlertsViewModel by activityViewModels()
    private var locationData: LocationData? = null

    private var weatherListType: WeatherListType? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AnalyticsLogger.logEvent("WeatherList: onCreate")

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(Constants.ARGS_WEATHERLISTTYPE)) {
                weatherListType =
                    WeatherListType.valueOf(savedInstanceState.getInt(Constants.ARGS_WEATHERLISTTYPE))
            }
            if (savedInstanceState.containsKey(Constants.KEY_DATA)) {
                locationData = JSONParser.deserializer(
                    savedInstanceState.getString(Constants.KEY_DATA),
                    LocationData::class.java
                )
            }
        } else {
            weatherListType =
                arguments?.getSerializable(Constants.ARGS_WEATHERLISTTYPE) as? WeatherListType
            if (arguments?.containsKey(Constants.KEY_DATA) == true) {
                locationData = arguments?.getString(Constants.KEY_DATA)?.let { data ->
                    JSONParser.deserializer(data, LocationData::class.java)
                }
            }
        }

        lifecycleScope.launchWhenResumed {
            initialize()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Use this to return your custom view for this Fragment
        val outerView = super.onCreateView(inflater, container, savedInstanceState) as ViewGroup

        outerView.addView(
            ComposeView(inflater.context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    val listState = rememberScalingLazyListState(0)
                    val forecasts = forecastsView.getForecasts().collectAsLazyPagingItems()
                    val hourlyForecasts =
                        forecastsView.getHourlyForecasts().collectAsLazyPagingItems()
                    val minutelyForecasts =
                        forecastsPanelView.getMinutelyForecasts().observeAsState()
                    val alerts = alertsView.getAlerts().observeAsState()
                    val scrollToPosition = remember {
                        arguments?.getInt(Constants.KEY_POSITION, 0) ?: 0
                    }

                    WearAppTheme {
                        Scaffold(
                            positionIndicator = {
                                PositionIndicator(
                                    scalingLazyListState = listState,
                                    modifier = Modifier
                                )
                            },
                            vignette = {
                                Vignette(vignettePosition = VignettePosition.TopAndBottom)
                            },
                            timeText = {
                                TimeText(
                                    modifier = Modifier.fadeAwayScalingLazyList {
                                        listState
                                    },
                                    timeSource = TimeTextDefaults.timeSource(
                                        if (DateFormat.is24HourFormat(LocalContext.current)) {
                                            TimeTextDefaults.TimeFormat24Hours
                                        } else {
                                            DateTimeConstants.CLOCK_FORMAT_12HR
                                        }
                                    )
                                )
                            }
                        ) {
                            ScalingLazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                state = listState,
                                anchorType = ScalingLazyListAnchorType.ItemCenter,
                                contentPadding = PaddingValues(vertical = 48.dp),
                                autoCentering = if (scrollToPosition != 0) {
                                    AutoCenteringParams(scrollToPosition)
                                } else {
                                    null
                                }
                            ) {
                                when (weatherListType!!) {
                                    WeatherListType.FORECAST -> {
                                        items(forecasts) {
                                            it?.let {
                                                WeatherForecastPanel(model = it)
                                            }
                                        }
                                    }
                                    WeatherListType.HOURLYFORECAST -> {
                                        items(hourlyForecasts) {
                                            it?.let {
                                                WeatherHourlyForecastPanel(model = it)
                                            }
                                        }
                                    }
                                    WeatherListType.ALERTS -> {
                                        alerts.value?.let {
                                            items(it) { alert ->
                                                WeatherAlertPanel(alert)
                                            }
                                        }
                                    }
                                    WeatherListType.PRECIPITATION -> {
                                        minutelyForecasts.value?.let { minFcasts ->
                                            items(minFcasts) {
                                                WeatherMinutelyForecastPanel(model = it)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )

        return outerView
    }

    override fun onResume() {
        super.onResume()
        AnalyticsLogger.logEvent("WeatherList: onResume")
    }

    override fun onPause() {
        AnalyticsLogger.logEvent("WeatherList: onPause")
        super.onPause()
    }

    private suspend fun initialize() {
        if (locationData == null) {
            locationData = settingsManager.getHomeData()
        }

        forecastsView.updateForecasts(locationData!!)
        alertsView.updateAlerts(locationData!!)
        forecastsPanelView.updateForecasts(locationData!!)
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
}