package com.thewizrd.simpleweather.ui

import android.text.format.DateFormat
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ScalingLazyListState
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.google.android.horologist.compose.navscaffold.WearNavScaffold
import com.google.android.horologist.compose.navscaffold.scalingLazyColumnComposable
import com.google.android.horologist.compose.navscaffold.scrollStateComposable
import com.thewizrd.common.controls.WeatherAlertsViewModel
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.DateTimeConstants
import com.thewizrd.simpleweather.ui.components.CustomTimeText
import com.thewizrd.simpleweather.ui.navigation.Screen
import com.thewizrd.simpleweather.ui.theme.WearAppTheme
import com.thewizrd.simpleweather.ui.theme.activityViewModel
import com.thewizrd.simpleweather.ui.time.ZonedTimeSource
import com.thewizrd.simpleweather.viewmodels.ForecastPanelsViewModel
import com.thewizrd.simpleweather.viewmodels.WeatherNowViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlin.math.max

@Composable
fun WeatherNow(
    modifier: Modifier = Modifier,
    swipeDismissableNavController: NavHostController = rememberSwipeDismissableNavController()
) {
    val navController by rememberUpdatedState(newValue = swipeDismissableNavController)
    val containerWidth = LocalConfiguration.current.screenWidthDp

    val wNowViewModel = activityViewModel<WeatherNowViewModel>()
    val alertsView = activityViewModel<WeatherAlertsViewModel>()
    val forecastsPanelView = activityViewModel<ForecastPanelsViewModel>()

    val uiState by wNowViewModel.uiState.collectAsState(Dispatchers.Default)
    val weather by wNowViewModel.weather.collectAsState(Dispatchers.Default)

    val alerts by alertsView.getAlerts().collectAsState(Dispatchers.Default)
    val forecasts by remember(forecastsPanelView.getForecasts()) {
        forecastsPanelView.getForecasts().map {
            val maxItemCount = max(4f, containerWidth / 50f).toInt()
            it.take(maxItemCount)
        }
    }.collectAsState(emptyList(), Dispatchers.Default)
    val hourlyForecasts by remember(forecastsPanelView.getHourlyForecasts()) {
        forecastsPanelView.getHourlyForecasts().map {
            it.take(12)
        }
    }.collectAsState(emptyList(), Dispatchers.Default)
    val hasMinutely by remember(forecastsPanelView.getMinutelyForecasts()) {
        forecastsPanelView.getMinutelyForecasts().map {
            it.isNotEmpty()
        }
    }.collectAsState(false, Dispatchers.Default)

    WearAppTheme {
        WearNavScaffold(
            modifier = modifier.background(MaterialTheme.colors.background),
            timeText = {
                CustomTimeText(
                    modifier = it,
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
            navController = navController,
            startDestination = Screen.WeatherNow.route
        ) {
            scrollStateComposable(
                route = Screen.WeatherNow.route,
                scrollStateBuilder = { ScrollState(0) }
            ) {
                WeatherNowScreen(
                    navController,
                    it.scrollableState,
                    it.viewModel.focusRequester,
                    wNowViewModel,
                    uiState,
                    weather,
                    alerts,
                    forecasts,
                    hourlyForecasts,
                    hasMinutely
                )
            }

            scalingLazyColumnComposable(
                route = Screen.Alerts.route,
                scrollStateBuilder = { ScalingLazyListState() }
            ) {
                WeatherAlertsScreen(
                    it.scrollableState,
                    it.viewModel.focusRequester,
                    alerts
                )
            }

            scalingLazyColumnComposable(
                route = Screen.Details.route,
                scrollStateBuilder = { ScalingLazyListState() }
            ) {
                WeatherDetailsScreen(
                    it.scrollableState,
                    it.viewModel.focusRequester,
                    weather.weatherDetailsMap.values
                )
            }

            scalingLazyColumnComposable(
                route = Screen.Forecast.route + "?${Constants.KEY_POSITION}={${Constants.KEY_POSITION}}",
                arguments = listOf(
                    navArgument(Constants.KEY_POSITION) {
                        type = NavType.IntType
                        defaultValue = 0
                    }
                ),
                scrollStateBuilder = { ScalingLazyListState() }
            ) {
                WeatherForecastScreen(
                    it.scrollableState,
                    it.viewModel.focusRequester,
                    it.backStackEntry
                )
            }

            scalingLazyColumnComposable(
                route = Screen.HourlyForecast.route + "?${Constants.KEY_POSITION}={${Constants.KEY_POSITION}}",
                arguments = listOf(
                    navArgument(Constants.KEY_POSITION) {
                        type = NavType.IntType
                        defaultValue = 0
                    }
                ),
                scrollStateBuilder = { ScalingLazyListState() }
            ) {
                WeatherHourlyForecastScreen(
                    it.scrollableState,
                    it.viewModel.focusRequester,
                    it.backStackEntry
                )
            }

            scalingLazyColumnComposable(
                route = Screen.Precipitation.route,
                scrollStateBuilder = { ScalingLazyListState() }
            ) {
                WeatherMinutelyForecastScreen(
                    it.scrollableState,
                    it.viewModel.focusRequester,
                    it.backStackEntry
                )
            }
        }
    }
}