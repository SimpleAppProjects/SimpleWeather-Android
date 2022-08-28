package com.thewizrd.simpleweather.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.ScalingLazyListAnchorType
import androidx.wear.compose.material.items
import androidx.wear.compose.material.rememberScalingLazyListState
import com.thewizrd.simpleweather.ui.components.WeatherMinutelyForecastPanel
import com.thewizrd.simpleweather.ui.theme.activityViewModel
import com.thewizrd.simpleweather.viewmodels.ForecastPanelsViewModel

@Composable
fun WeatherMinutelyForecastScreen(
    scrollToPosition: Int = 0
) {
    val forecastsPanelView = activityViewModel<ForecastPanelsViewModel>()
    val minutelyForecasts by forecastsPanelView.getMinutelyForecasts().collectAsState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = rememberScalingLazyListState(scrollToPosition),
        anchorType = ScalingLazyListAnchorType.ItemCenter
    ) {
        items(
            minutelyForecasts,
            key = {
                it.hashCode()
            }
        ) {
            WeatherMinutelyForecastPanel(model = it)
        }
    }
}