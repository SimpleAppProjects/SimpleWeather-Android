package com.thewizrd.simpleweather.ui

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material.*
import com.google.android.horologist.compose.layout.fadeAway
import com.google.android.horologist.compose.layout.fadeAwayScalingLazyList
import com.thewizrd.common.controls.WeatherAlertsViewModel
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.DateTimeConstants
import com.thewizrd.simpleweather.ui.components.CustomTimeText
import com.thewizrd.simpleweather.ui.navigation.Screen
import com.thewizrd.simpleweather.ui.navigation.WeatherNowNavController
import com.thewizrd.simpleweather.ui.theme.WearAppTheme
import com.thewizrd.simpleweather.ui.theme.activityViewModel
import com.thewizrd.simpleweather.ui.time.ZonedTimeSource
import com.thewizrd.simpleweather.viewmodels.ForecastPanelsViewModel
import com.thewizrd.simpleweather.viewmodels.WeatherNowViewModel
import kotlinx.coroutines.flow.map
import kotlin.math.max

@Composable
fun WeatherNow(
    modifier: Modifier = Modifier
) {
    val containerWidth = LocalConfiguration.current.screenWidthDp

    val wNowViewModel = activityViewModel<WeatherNowViewModel>()
    val alertsView = activityViewModel<WeatherAlertsViewModel>()
    val forecastsPanelView = activityViewModel<ForecastPanelsViewModel>()

    val uiState by wNowViewModel.uiState.collectAsState()
    val weather by wNowViewModel.weather.collectAsState()

    val alerts by alertsView.getAlerts().collectAsState()
    val forecasts by remember(forecastsPanelView.getForecasts()) {
        forecastsPanelView.getForecasts().map {
            val maxItemCount = max(4f, containerWidth / 50f).toInt()
            it.take(maxItemCount)
        }
    }.collectAsState(emptyList())
    val hourlyForecasts by remember(forecastsPanelView.getHourlyForecasts()) {
        forecastsPanelView.getHourlyForecasts().map {
            it.take(12)
        }
    }.collectAsState(emptyList())
    val hasMinutely by remember(forecastsPanelView.getMinutelyForecasts()) {
        forecastsPanelView.getMinutelyForecasts().map {
            it.isNotEmpty()
        }
    }.collectAsState(false)

    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    val navController = remember { WeatherNowNavController() }

    WearAppTheme {
        Scaffold(
            modifier = modifier.background(MaterialTheme.colors.background),
            timeText = {
                CustomTimeText(
                    modifier = Modifier.fadeAway { scrollState },
                    visible = true,
                    timeSource = ZonedTimeSource(
                        timeFormat = if (DateFormat.is24HourFormat(LocalContext.current)) {
                            "${DateTimeConstants.CLOCK_FORMAT_24HR} ${DateTimeConstants.TIMEZONE_NAME}"
                        } else {
                            "${DateTimeConstants.CLOCK_FORMAT_12HR} ${DateTimeConstants.TIMEZONE_NAME}"
                        },
                        timeZone = uiState.locationData?.tzLong
                    )
                )
            },
            vignette = {
                Vignette(vignettePosition = VignettePosition.TopAndBottom)
            }
        ) {
            WeatherNowScreen(
                navController,
                scrollState,
                focusRequester,
                wNowViewModel,
                uiState,
                weather,
                alerts,
                forecasts,
                hourlyForecasts,
                hasMinutely
            )

            /* WeatherNow Detail Views */
            val currentDestination by navController.currentDestinationFlow.collectAsState(null)

            if (currentDestination?.route != null) {
                SwipeToDismissBox(
                    onDismissed = {
                        navController.navigate(null)
                    },
                    backgroundKey = SwipeToDismissKeys.Background,
                    contentKey = currentDestination?.route ?: "",
                    hasBackground = true
                ) { isBackground ->
                    val swipeFocusRequester = remember { FocusRequester() }
                    val fragmentScrollState = rememberScalingLazyListState()

                    if (!isBackground) {
                        Scaffold(
                            modifier = modifier.background(MaterialTheme.colors.background),
                            timeText = {
                                CustomTimeText(
                                    modifier = Modifier.fadeAwayScalingLazyList { fragmentScrollState },
                                    visible = true,
                                    timeSource = ZonedTimeSource(
                                        timeFormat = if (DateFormat.is24HourFormat(LocalContext.current)) {
                                            "${DateTimeConstants.CLOCK_FORMAT_24HR} ${DateTimeConstants.TIMEZONE_NAME}"
                                        } else {
                                            "${DateTimeConstants.CLOCK_FORMAT_12HR} ${DateTimeConstants.TIMEZONE_NAME}"
                                        },
                                        timeZone = uiState.locationData?.tzLong
                                    )
                                )
                            },
                            vignette = {
                                Vignette(vignettePosition = VignettePosition.TopAndBottom)
                            }
                        ) {
                            when (currentDestination?.route) {
                                Screen.Alerts.route -> {
                                    WeatherAlertsScreen(alerts)
                                }
                                Screen.Details.route -> {
                                    val detailItems = remember(weather) {
                                        weather.weatherDetailsMap.values
                                    }

                                    WeatherDetailsScreen(detailItems)
                                }
                                Screen.Forecast.route -> {
                                    val scrollToPosition = remember(currentDestination) {
                                        currentDestination?.args?.getInt(Constants.KEY_POSITION)
                                            ?: 0
                                    }

                                    WeatherForecastScreen(scrollToPosition)
                                }
                                Screen.HourlyForecast.route -> {
                                    val scrollToPosition = remember(currentDestination) {
                                        currentDestination?.args?.getInt(Constants.KEY_POSITION)
                                            ?: 0
                                    }

                                    WeatherHourlyForecastScreen(scrollToPosition)
                                }
                                Screen.Precipitation.route -> {
                                    WeatherMinutelyForecastScreen()
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }
        }
    }
}