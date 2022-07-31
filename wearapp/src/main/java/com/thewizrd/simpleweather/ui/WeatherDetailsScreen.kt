package com.thewizrd.simpleweather.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.ScalingLazyListAnchorType
import com.thewizrd.simpleweather.ui.components.WeatherDetailItem
import com.thewizrd.simpleweather.ui.theme.activityViewModel
import com.thewizrd.simpleweather.viewmodels.WeatherNowViewModel

@Composable
fun WeatherDetailsScreen(
    backStackEntry: NavBackStackEntry
) {
    val scalingLazyListState = scalingLazyListState(it = backStackEntry)
    val weatherModel = activityViewModel<WeatherNowViewModel>()
    val weather by weatherModel.weather.collectAsState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = scalingLazyListState,
        anchorType = ScalingLazyListAnchorType.ItemStart
    ) {
        weather?.weatherDetailsMap?.values?.forEach {
            item {
                WeatherDetailItem(model = it)
            }
        }
    }
}