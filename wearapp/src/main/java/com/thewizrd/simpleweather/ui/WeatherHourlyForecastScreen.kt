package com.thewizrd.simpleweather.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.ScalingLazyListAnchorType
import com.thewizrd.common.controls.ForecastsListViewModel
import com.thewizrd.simpleweather.ui.components.LoadingPagingContent
import com.thewizrd.simpleweather.ui.components.WeatherHourlyForecastPanel
import com.thewizrd.simpleweather.ui.paging.items
import com.thewizrd.simpleweather.ui.paging.rememberScalingLazyListState
import com.thewizrd.simpleweather.ui.theme.activityViewModel

@Composable
fun WeatherHourlyForecastScreen(
    scrollToPosition: Int = 0
) {
    val forecastsView = activityViewModel<ForecastsListViewModel>()
    val hourlyForecasts = forecastsView.getHourlyForecasts().collectAsLazyPagingItems()

    val scrollState = hourlyForecasts.rememberScalingLazyListState(scrollToPosition)

    LoadingPagingContent(
        pagingItems = hourlyForecasts
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxWidth(),
            state = scrollState,
            anchorType = ScalingLazyListAnchorType.ItemCenter
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
    }
}