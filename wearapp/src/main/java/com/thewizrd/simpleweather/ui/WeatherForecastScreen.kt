package com.thewizrd.simpleweather.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.ScalingLazyListAnchorType
import com.thewizrd.common.controls.ForecastsListViewModel
import com.thewizrd.simpleweather.ui.components.LoadingPagingContent
import com.thewizrd.simpleweather.ui.components.WeatherForecastPanel
import com.thewizrd.simpleweather.ui.paging.items
import com.thewizrd.simpleweather.ui.paging.rememberScalingLazyListState
import com.thewizrd.simpleweather.ui.theme.activityViewModel

@Composable
fun WeatherForecastScreen(
    scrollToPosition: Int = 0
) {
    val forecastsView = activityViewModel<ForecastsListViewModel>()
    val forecasts = forecastsView.getForecasts().collectAsLazyPagingItems()

    val scrollState = forecasts.rememberScalingLazyListState(scrollToPosition)

    LoadingPagingContent(
        pagingItems = forecasts
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxWidth(),
            state = scrollState,
            anchorType = ScalingLazyListAnchorType.ItemCenter
        ) {
            items(
                forecasts,
                key = {
                    it.hashCode()
                }
            ) {
                it?.let {
                    WeatherForecastPanel(model = it)
                }
            }
        }
    }
}