package com.thewizrd.simpleweather.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.wear.compose.material.AutoCenteringParams
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.ScalingLazyListAnchorType
import com.thewizrd.common.controls.ForecastsListViewModel
import com.thewizrd.shared_resources.Constants
import com.thewizrd.simpleweather.ui.components.WeatherHourlyForecastPanel
import com.thewizrd.simpleweather.ui.paging.items
import com.thewizrd.simpleweather.ui.theme.activityViewModel

@Composable
fun WeatherHourlyForecastScreen(
    backStackEntry: NavBackStackEntry
) {
    val scalingLazyListState = scalingLazyListState(it = backStackEntry)
    val forecastsView = activityViewModel<ForecastsListViewModel>()
    val hourlyForecasts = forecastsView.getHourlyForecasts().collectAsLazyPagingItems()
    val scrollToPosition = remember(backStackEntry) {
        backStackEntry.arguments?.getInt(Constants.KEY_POSITION) ?: 0
    }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = scalingLazyListState,
        anchorType = ScalingLazyListAnchorType.ItemCenter,
        contentPadding = PaddingValues(vertical = 48.dp),
        autoCentering = if (scrollToPosition != 0) {
            AutoCenteringParams(scrollToPosition)
        } else {
            null
        }
    ) {
        items(hourlyForecasts) {
            it?.let {
                WeatherHourlyForecastPanel(model = it)
            }
        }
    }
}