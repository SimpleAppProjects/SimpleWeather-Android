package com.thewizrd.simpleweather.ui

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.createGraph
import androidx.navigation.navArgument
import androidx.wear.compose.material.*
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.currentBackStackEntryAsState
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.google.android.horologist.compose.layout.fadeAway
import com.google.android.horologist.compose.layout.fadeAwayScalingLazyList
import com.thewizrd.common.controls.WeatherAlertsViewModel
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.DateTimeConstants
import com.thewizrd.simpleweather.ui.components.CustomTimeText
import com.thewizrd.simpleweather.ui.navigation.DestinationScrollType
import com.thewizrd.simpleweather.ui.navigation.SCROLL_TYPE_NAV_ARGUMENT
import com.thewizrd.simpleweather.ui.navigation.Screen
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
    val navController = rememberSwipeDismissableNavController()

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
            },
            positionIndicator = {
                PositionIndicator(scrollState = scrollState)
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
            val currentBackStackEntry by navController.currentBackStackEntryAsState()
            val swipeFocusRequester = remember { FocusRequester() }

            navController.setLifecycleOwner(LocalLifecycleOwner.current)
            navController.setViewModelStore(LocalViewModelStoreOwner.current!!.viewModelStore)
            navController.graph = remember {
                navController.createGraph(
                    startDestination = Screen.WeatherNow.route
                ) {
                    composable(
                        route = Screen.WeatherNow.route
                    ) {}

                    composable(
                        route = Screen.Alerts.route,
                        arguments = listOf(
                            navArgument(SCROLL_TYPE_NAV_ARGUMENT) {
                                type = NavType.EnumType(DestinationScrollType::class.java)
                                defaultValue = DestinationScrollType.SCALING_LAZY_COLUMN_SCROLLING
                            }
                        )
                    ) { backStackEntry ->
                        WeatherAlertsScreen(backStackEntry, swipeFocusRequester, alerts)
                    }

                    composable(
                        route = Screen.Details.route,
                        arguments = listOf(
                            navArgument(SCROLL_TYPE_NAV_ARGUMENT) {
                                type = NavType.EnumType(DestinationScrollType::class.java)
                                defaultValue = DestinationScrollType.SCALING_LAZY_COLUMN_SCROLLING
                            }
                        )
                    ) { backStackEntry ->
                        val detailItems = remember(weather) {
                            weather.weatherDetailsMap.values
                        }

                        WeatherDetailsScreen(backStackEntry, swipeFocusRequester, detailItems)
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
                    ) { backStackEntry ->
                        WeatherForecastScreen(backStackEntry, swipeFocusRequester)
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
                    ) { backStackEntry ->
                        WeatherHourlyForecastScreen(backStackEntry, swipeFocusRequester)
                    }

                    composable(
                        route = Screen.Precipitation.route,
                        arguments = listOf(
                            navArgument(SCROLL_TYPE_NAV_ARGUMENT) {
                                type = NavType.EnumType(DestinationScrollType::class.java)
                                defaultValue = DestinationScrollType.SCALING_LAZY_COLUMN_SCROLLING
                            }
                        )
                    ) { backStackEntry ->
                        WeatherMinutelyForecastScreen(backStackEntry, swipeFocusRequester)
                    }
                }
            }

            if (currentBackStackEntry?.destination?.route != Screen.WeatherNow.route) {
                val scrollType =
                    currentBackStackEntry?.arguments?.getSerializable(SCROLL_TYPE_NAV_ARGUMENT)
                        ?: DestinationScrollType.NONE

                SwipeToDismissBox(
                    onDismissed = {
                        navController.popBackStack(Screen.WeatherNow.route, true)
                    },
                    backgroundKey = SwipeToDismissKeys.Background,
                    contentKey = currentBackStackEntry?.destination?.route ?: "",
                    hasBackground = true
                ) { isBackground ->
                    if (!isBackground) {
                        Scaffold(
                            modifier = modifier.background(MaterialTheme.colors.background),
                            timeText = {
                                // Scaffold places time at top of screen to follow Material Design guidelines.
                                // (Time is hidden while scrolling.)
                                val timeTextModifier =
                                    when (scrollType) {
                                        DestinationScrollType.SCALING_LAZY_COLUMN_SCROLLING -> {
                                            val scrollViewModel: ScalingLazyListStateViewModel =
                                                viewModel(currentBackStackEntry!!)
                                            Modifier.fadeAwayScalingLazyList {
                                                scrollViewModel.scrollState
                                            }
                                        }
                                        DestinationScrollType.COLUMN_SCROLLING -> {
                                            val scrollViewModel: ScrollStateViewModel =
                                                viewModel(currentBackStackEntry!!)
                                            Modifier.fadeAway {
                                                scrollViewModel.scrollState
                                            }
                                        }
                                        DestinationScrollType.TIME_TEXT_ONLY -> {
                                            Modifier
                                        }
                                        else -> {
                                            null
                                        }
                                    }

                                key(currentBackStackEntry?.destination?.route) {
                                    CustomTimeText(
                                        modifier = timeTextModifier ?: Modifier,
                                        visible = timeTextModifier != null,
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
                            },
                            vignette = {
                                // Only show vignette for screens with scrollable content.
                                if (scrollType == DestinationScrollType.SCALING_LAZY_COLUMN_SCROLLING ||
                                    scrollType == DestinationScrollType.COLUMN_SCROLLING
                                ) {
                                    Vignette(vignettePosition = VignettePosition.TopAndBottom)
                                }
                            },
                            positionIndicator = {
                                // Only displays the position indicator for scrollable content.
                                when (scrollType) {
                                    DestinationScrollType.SCALING_LAZY_COLUMN_SCROLLING -> {
                                        // Get or create the ViewModel associated with the current back stack entry
                                        val scrollViewModel: ScalingLazyListStateViewModel =
                                            viewModel(currentBackStackEntry!!)
                                        PositionIndicator(scalingLazyListState = scrollViewModel.scrollState)
                                    }
                                    DestinationScrollType.COLUMN_SCROLLING -> {
                                        // Get or create the ViewModel associated with the current back stack entry
                                        val scrollViewModel: ScrollStateViewModel =
                                            viewModel(currentBackStackEntry!!)
                                        PositionIndicator(scrollState = scrollViewModel.scrollState)
                                    }
                                    else -> {}
                                }
                            }
                        ) {
                            SwipeDismissableNavHost(
                                navController = navController,
                                graph = navController.graph
                            )
                        }

                        LaunchedEffect(Unit) {
                            runCatching {
                                focusRequester.freeFocus()
                            }
                        }

                        DisposableEffect(Unit) {
                            onDispose {
                                runCatching {
                                    focusRequester.requestFocus()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}