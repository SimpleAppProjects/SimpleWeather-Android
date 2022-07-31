package com.thewizrd.simpleweather.ui

import android.text.format.DateFormat
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.wear.compose.material.*
import androidx.wear.compose.navigation.currentBackStackEntryAsState
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.google.android.horologist.compose.layout.fadeAway
import com.google.android.horologist.compose.layout.fadeAwayScalingLazyList
import com.thewizrd.shared_resources.DateTimeConstants
import com.thewizrd.simpleweather.ui.components.CustomTimeText
import com.thewizrd.simpleweather.ui.navigation.DestinationScrollType
import com.thewizrd.simpleweather.ui.navigation.SCROLL_TYPE_NAV_ARGUMENT
import com.thewizrd.simpleweather.ui.theme.WearAppTheme

@Composable
fun WearApp(
    modifier: Modifier = Modifier,
    swipeDismissableNavController: NavHostController = rememberSwipeDismissableNavController()
) {
    WearAppTheme {
        val currentBackStackEntry by swipeDismissableNavController.currentBackStackEntryAsState()

        val scrollType =
            currentBackStackEntry?.arguments?.getSerializable(SCROLL_TYPE_NAV_ARGUMENT)
                ?: DestinationScrollType.NONE

        Scaffold(
            modifier = modifier,
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
                            val viewModel: ScrollStateViewModel =
                                viewModel(currentBackStackEntry!!)
                            Modifier.fadeAway {
                                viewModel.scrollState
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
                        timeSource = TimeTextDefaults.timeSource(
                            if (DateFormat.is24HourFormat(LocalContext.current)) {
                                TimeTextDefaults.TimeFormat24Hours
                            } else {
                                DateTimeConstants.CLOCK_FORMAT_12HR
                            }
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
                        val viewModel: ScrollStateViewModel = viewModel(currentBackStackEntry!!)
                        PositionIndicator(scrollState = viewModel.scrollState)
                    }
                    else -> {}
                }
            }
        ) {
            WeatherNavGraph(
                navController = swipeDismissableNavController
            )
        }
    }
}

@Composable
internal fun scrollState(it: NavBackStackEntry): ScrollState {
    val passedScrollType =
        it.arguments?.getSerializable(SCROLL_TYPE_NAV_ARGUMENT)

    check(passedScrollType == DestinationScrollType.COLUMN_SCROLLING) {
        "Scroll type must be DestinationScrollType.COLUMN_SCROLLING"
    }

    val scrollViewModel: ScrollStateViewModel = viewModel(it)
    return scrollViewModel.scrollState
}

@Composable
internal fun scalingLazyListState(it: NavBackStackEntry): ScalingLazyListState {
    val passedScrollType =
        it.arguments?.getSerializable(SCROLL_TYPE_NAV_ARGUMENT)

    check(
        passedScrollType == DestinationScrollType.SCALING_LAZY_COLUMN_SCROLLING
    ) {
        "Scroll type must be DestinationScrollType.SCALING_LAZY_COLUMN_SCROLLING"
    }

    val scrollViewModel: ScalingLazyListStateViewModel = viewModel(it)

    return scrollViewModel.scrollState
}