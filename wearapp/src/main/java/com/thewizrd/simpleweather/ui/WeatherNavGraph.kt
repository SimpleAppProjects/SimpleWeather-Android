package com.thewizrd.simpleweather.ui

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.map
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import com.thewizrd.common.controls.WeatherAlertsViewModel
import com.thewizrd.shared_resources.Constants
import com.thewizrd.simpleweather.ui.navigation.DestinationScrollType
import com.thewizrd.simpleweather.ui.navigation.SCROLL_TYPE_NAV_ARGUMENT
import com.thewizrd.simpleweather.ui.navigation.Screen
import com.thewizrd.simpleweather.ui.theme.activityViewModel
import com.thewizrd.simpleweather.viewmodels.ForecastPanelsViewModel
import com.thewizrd.simpleweather.viewmodels.WeatherNowViewModel
import kotlin.math.max

@Composable
fun WeatherNavGraph(
    navController: NavHostController,
    startDestination: Screen = Screen.WeatherNow
) {
    val containerWidth = LocalConfiguration.current.screenWidthDp

    val wNowViewModel = activityViewModel<WeatherNowViewModel>()
    val alertsView = activityViewModel<WeatherAlertsViewModel>()
    val forecastsPanelView = activityViewModel<ForecastPanelsViewModel>()

    val uiState by wNowViewModel.uiState.collectAsState()
    val weather by wNowViewModel.weather.collectAsState()

    val alerts by alertsView.getAlerts().collectAsState()
    val forecasts by forecastsPanelView.getForecasts().map {
        val maxItemCount = max(4f, containerWidth / 50f).toInt()
        it.take(maxItemCount)
    }.observeAsState(emptyList())
    val hourlyForecasts by forecastsPanelView.getHourlyForecasts().map {
        it.take(12)
    }.observeAsState(emptyList())
    val hasMinutely by forecastsPanelView.getMinutelyForecasts().map {
        it.isNotEmpty()
    }.observeAsState(false)

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = startDestination.route,
        modifier = Modifier.background(MaterialTheme.colors.background)
    ) {
        composable(
            route = Screen.WeatherNow.route,
            arguments = listOf(
                navArgument(SCROLL_TYPE_NAV_ARGUMENT) {
                    type = NavType.EnumType(DestinationScrollType::class.java)
                    defaultValue = DestinationScrollType.COLUMN_SCROLLING
                }
            )
        ) {
            WeatherNowScreen(
                navController,
                it,
                wNowViewModel,
                uiState,
                weather,
                alerts,
                forecasts,
                hourlyForecasts,
                hasMinutely
            )
        }

        composable(
            route = Screen.Alerts.route,
            arguments = listOf(
                navArgument(SCROLL_TYPE_NAV_ARGUMENT) {
                    type = NavType.EnumType(DestinationScrollType::class.java)
                    defaultValue = DestinationScrollType.SCALING_LAZY_COLUMN_SCROLLING
                }
            )
        ) {
            WeatherAlertsScreen(backStackEntry = it, alerts)
        }

        composable(
            route = Screen.Details.route,
            arguments = listOf(
                navArgument(SCROLL_TYPE_NAV_ARGUMENT) {
                    type = NavType.EnumType(DestinationScrollType::class.java)
                    defaultValue = DestinationScrollType.SCALING_LAZY_COLUMN_SCROLLING
                }
            )
        ) {
            WeatherDetailsScreen(it, weather.weatherDetailsMap.values)
        }

        composable(
            route = Screen.Forecast.route + "?${Constants.KEY_POSITION}={${Constants.KEY_POSITION}}",
            arguments = listOf(
                navArgument(Constants.KEY_POSITION) {
                    type = NavType.IntType
                    defaultValue = 0
                },
                navArgument(SCROLL_TYPE_NAV_ARGUMENT) {
                    type = NavType.EnumType(DestinationScrollType::class.java)
                    defaultValue = DestinationScrollType.SCALING_LAZY_COLUMN_SCROLLING
                }
            )
        ) {
            WeatherForecastScreen(backStackEntry = it)
        }

        composable(
            route = Screen.HourlyForecast.route + "?${Constants.KEY_POSITION}={${Constants.KEY_POSITION}}",
            arguments = listOf(
                navArgument(Constants.KEY_POSITION) {
                    type = NavType.IntType
                    defaultValue = 0
                },
                navArgument(SCROLL_TYPE_NAV_ARGUMENT) {
                    type = NavType.EnumType(DestinationScrollType::class.java)
                    defaultValue = DestinationScrollType.SCALING_LAZY_COLUMN_SCROLLING
                }
            )
        ) {
            WeatherHourlyForecastScreen(backStackEntry = it)
        }

        composable(
            route = Screen.Precipitation.route,
            arguments = listOf(
                navArgument(SCROLL_TYPE_NAV_ARGUMENT) {
                    type = NavType.EnumType(DestinationScrollType::class.java)
                    defaultValue = DestinationScrollType.SCALING_LAZY_COLUMN_SCROLLING
                }
            )
        ) {
            WeatherMinutelyForecastScreen(backStackEntry = it)
        }
    }
}