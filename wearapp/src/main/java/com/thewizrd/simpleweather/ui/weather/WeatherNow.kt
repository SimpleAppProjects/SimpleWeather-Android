package com.thewizrd.simpleweather.ui.weather

import android.text.format.DateFormat
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.thewizrd.common.controls.WeatherAlertsViewModel
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.DateTimeConstants
import com.thewizrd.simpleweather.ui.components.CustomTimeText
import com.thewizrd.simpleweather.ui.navigation.Screen
import com.thewizrd.simpleweather.ui.preferences.DetailsWeatherTileConfigScreen
import com.thewizrd.simpleweather.ui.theme.WearAppTheme
import com.thewizrd.simpleweather.ui.theme.activityViewModel
import com.thewizrd.simpleweather.ui.time.ZonedTimeSource
import com.thewizrd.simpleweather.ui.utils.rememberFocusRequester
import com.thewizrd.simpleweather.viewmodels.ForecastPanelsViewModel
import com.thewizrd.simpleweather.viewmodels.WeatherDataSyncViewModel
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
    val dataSyncViewModel = viewModel<WeatherDataSyncViewModel>()

    val uiState by wNowViewModel.uiState.collectAsState()
    val weather by wNowViewModel.weather.collectAsState()
    val dataSyncState by dataSyncViewModel.uiState.collectAsState()

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
    val focusRequester = rememberFocusRequester()
    val navController = rememberSwipeDismissableNavController()

    WearAppTheme {
        var showScaffolding by remember { mutableStateOf(true) }

        AppScaffold(
            modifier = modifier,
            timeText = {
                CustomTimeText(
                    visible = showScaffolding,
                    timeSource = ZonedTimeSource(
                        timeFormat = if (DateFormat.is24HourFormat(LocalContext.current)) {
                            "${DateTimeConstants.CLOCK_FORMAT_24HR} ${DateTimeConstants.TIMEZONE_NAME}"
                        } else {
                            "${DateTimeConstants.CLOCK_FORMAT_12HR} ${DateTimeConstants.TIMEZONE_NAME}"
                        },
                        timeZone = uiState.locationData?.tzLong
                    )
                )
            }
        ) {
            val swipeFocusRequester = rememberFocusRequester()

            SwipeDismissableNavHost(
                navController = navController,
                startDestination = Screen.WeatherNow.route
            ) {
                composable(
                    route = Screen.WeatherNow.route
                ) {
                    WeatherNowScreen(
                        navController,
                        scrollState,
                        focusRequester,
                        wNowViewModel,
                        dataSyncViewModel,
                        uiState,
                        dataSyncState,
                        weather,
                        alerts,
                        forecasts,
                        hourlyForecasts,
                        hasMinutely
                    )
                }

                composable(
                    route = Screen.Alerts.route
                ) { backStackEntry ->
                    WeatherAlertsScreen(backStackEntry, swipeFocusRequester, alerts)
                }

                composable(
                    route = Screen.Details.route
                ) { backStackEntry ->
                    val detailItems = remember(weather) {
                        weather.weatherDetailsMap.values
                    }

                    WeatherDetailsScreen(
                        backStackEntry,
                        swipeFocusRequester,
                        detailItems,
                        iconProvider = weather.iconProvider
                    )
                }

                composable(
                    route = Screen.Forecast.route + "?${Constants.KEY_POSITION}={${Constants.KEY_POSITION}}",
                    arguments = listOf(
                        navArgument(Constants.KEY_POSITION) {
                            type = NavType.IntType
                            defaultValue = 0
                        },
                    )
                ) { backStackEntry ->
                    WeatherForecastScreen(
                        backStackEntry,
                        swipeFocusRequester,
                        iconProvider = weather.iconProvider
                    )
                }

                composable(
                    route = Screen.HourlyForecast.route + "?${Constants.KEY_POSITION}={${Constants.KEY_POSITION}}",
                    arguments = listOf(
                        navArgument(Constants.KEY_POSITION) {
                            type = NavType.IntType
                            defaultValue = 0
                        },
                    )
                ) { backStackEntry ->
                    WeatherHourlyForecastScreen(
                        backStackEntry,
                        swipeFocusRequester,
                        iconProvider = weather.iconProvider
                    )
                }

                composable(
                    route = Screen.Precipitation.route,
                ) { backStackEntry ->
                    WeatherMinutelyForecastScreen(
                        backStackEntry,
                        swipeFocusRequester,
                        iconProvider = weather.iconProvider
                    )
                }

                composable(
                    route = Screen.DetailsTileEditor.route,
                ) { backStackEntry ->
                    DetailsWeatherTileConfigScreen(
                        navController,
                        backStackEntry,
                        swipeFocusRequester,
                        uiState
                    )
                }
            }
        }
    }
}