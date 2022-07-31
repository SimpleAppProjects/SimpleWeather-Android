package com.thewizrd.simpleweather.ui

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import com.thewizrd.shared_resources.Constants
import com.thewizrd.simpleweather.ui.navigation.DestinationScrollType
import com.thewizrd.simpleweather.ui.navigation.SCROLL_TYPE_NAV_ARGUMENT
import com.thewizrd.simpleweather.ui.navigation.Screen

@Composable
fun WeatherNavGraph(
    navController: NavHostController,
    startDestination: Screen = Screen.WeatherNow
) {
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
                backStackEntry = it,
                navController = navController
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
            WeatherAlertsScreen(backStackEntry = it)
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
            WeatherDetailsScreen(it)
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