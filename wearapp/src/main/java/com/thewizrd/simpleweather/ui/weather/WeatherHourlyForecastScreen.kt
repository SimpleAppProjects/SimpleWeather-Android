package com.thewizrd.simpleweather.ui.weather

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.ScalingLazyListAnchorType
import com.google.android.horologist.compose.navscaffold.scrollableColumn
import com.thewizrd.common.controls.ForecastsListViewModel
import com.thewizrd.shared_resources.Constants
import com.thewizrd.simpleweather.ui.ScalingLazyListStateViewModel
import com.thewizrd.simpleweather.ui.components.LoadingPagingContent
import com.thewizrd.simpleweather.ui.components.WeatherHourlyForecastPanel
import com.thewizrd.simpleweather.ui.paging.items
import com.thewizrd.simpleweather.ui.theme.activityViewModel

@Composable
fun WeatherHourlyForecastScreen(
    backStackEntry: NavBackStackEntry,
    focusRequester: FocusRequester
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    val forecastsView = activityViewModel<ForecastsListViewModel>()
    val hourlyForecasts = forecastsView.getHourlyForecasts().collectAsLazyPagingItems()

    val scrollStateViewModel: ScalingLazyListStateViewModel = viewModel(backStackEntry)

    LoadingPagingContent(
        pagingItems = hourlyForecasts
    ) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .scrollableColumn(focusRequester, scrollStateViewModel.scrollState),
            state = scrollStateViewModel.scrollState,
            anchorType = ScalingLazyListAnchorType.ItemCenter,
            contentPadding = PaddingValues(top = 48.dp)
        ) {
            items(
                hourlyForecasts,
                key = {
                    it.hashCode()
                }
            ) {
                it?.let {
                    WeatherHourlyForecastPanel(model = it)
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