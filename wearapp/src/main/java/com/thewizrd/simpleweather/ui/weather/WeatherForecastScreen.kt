package com.thewizrd.simpleweather.ui.weather

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.ScreenScaffold
import com.thewizrd.common.controls.ForecastsListViewModel
import com.thewizrd.shared_resources.Constants
import com.thewizrd.simpleweather.ui.ScalingLazyListStateViewModel
import com.thewizrd.simpleweather.ui.components.LoadingPagingContent
import com.thewizrd.simpleweather.ui.components.WeatherForecastPanel
import com.thewizrd.simpleweather.ui.paging.items
import com.thewizrd.simpleweather.ui.theme.activityViewModel

@Composable
fun WeatherForecastScreen(
    backStackEntry: NavBackStackEntry,
    focusRequester: FocusRequester,
    iconProvider: String? = null
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    val forecastsView = activityViewModel<ForecastsListViewModel>()
    val forecasts = forecastsView.getForecasts().collectAsLazyPagingItems()

    val scrollStateViewModel: ScalingLazyListStateViewModel = viewModel(backStackEntry)

    ScreenScaffold(scrollState = scrollStateViewModel.scrollState) {
        LoadingPagingContent(
            pagingItems = forecasts
        ) {
            ScalingLazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .rotaryScrollable(
                        RotaryScrollableDefaults.behavior(scrollStateViewModel.scrollState),
                        focusRequester
                    ),
                state = scrollStateViewModel.scrollState,
                anchorType = ScalingLazyListAnchorType.ItemCenter,
                contentPadding = PaddingValues(top = 48.dp)
            ) {
                items(forecasts) {
                    it?.let {
                        WeatherForecastPanel(model = it, iconProvider = iconProvider)
                    }
                }
            }

            LaunchedEffect(Unit) {
                lifecycleOwner.repeatOnLifecycle(state = Lifecycle.State.RESUMED) {
                    focusRequester.requestFocus()
                }
            }
        }

        LaunchedEffect(backStackEntry) {
            backStackEntry.arguments?.getInt(Constants.KEY_POSITION)?.let { position ->
                scrollStateViewModel.scrollState.scrollToItem(position)
            }
        }
    }
}