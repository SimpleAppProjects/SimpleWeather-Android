package com.thewizrd.simpleweather.ui.navigation

// Navigation Argument for Screens with scrollable types:
// 1. WatchList -> ScalingLazyColumn
// 2. WatchDetail -> Column (with scaling enabled)
const val SCROLL_TYPE_NAV_ARGUMENT = "scrollType"

/**
 * Represent all Screens (Composables) in the app.
 */
sealed class Screen(
    val route: String
) {
    object WeatherNow : Screen("weathernow")
    object Alerts : Screen("alerts")
    object Details : Screen("weatherdetails")

    // WeatherSummary dialog
    object Forecast : Screen("forecast")
    object HourlyForecast : Screen("hourlyforecast")
    object Precipitation : Screen("precipitation")
}
