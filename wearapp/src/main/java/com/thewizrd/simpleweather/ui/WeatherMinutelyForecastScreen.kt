package com.thewizrd.simpleweather.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.wear.compose.material.AutoCenteringParams
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.ScalingLazyListAnchorType
import androidx.wear.compose.material.items
import com.thewizrd.shared_resources.Constants
import com.thewizrd.simpleweather.ui.components.WeatherMinutelyForecastPanel
import com.thewizrd.simpleweather.ui.theme.activityViewModel
import com.thewizrd.simpleweather.viewmodels.ForecastPanelsViewModel
import kotlinx.coroutines.delay

@Composable
fun WeatherMinutelyForecastScreen(
    backStackEntry: NavBackStackEntry
) {
    val scalingLazyListState = scalingLazyListState(it = backStackEntry)
    val forecastsPanelView = activityViewModel<ForecastPanelsViewModel>()
    val minutelyForecasts by forecastsPanelView.getMinutelyForecasts().observeAsState()
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
        minutelyForecasts?.let { minFcasts ->
            items(minFcasts) {
                WeatherMinutelyForecastPanel(model = it)
            }
        }
    }

    LaunchedEffect(scalingLazyListState) {
        delay(50)
        scalingLazyListState.scrollToItem(scrollToPosition)
    }
}