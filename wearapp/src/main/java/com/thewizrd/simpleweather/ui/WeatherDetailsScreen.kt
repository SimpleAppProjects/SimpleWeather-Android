package com.thewizrd.simpleweather.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.ScalingLazyListAnchorType
import androidx.wear.compose.material.rememberScalingLazyListState
import com.thewizrd.common.controls.DetailItemViewModel
import com.thewizrd.simpleweather.ui.components.WeatherDetailItem

@Composable
fun WeatherDetailsScreen(
    weatherDetails: Collection<DetailItemViewModel>
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = rememberScalingLazyListState(),
        anchorType = ScalingLazyListAnchorType.ItemStart
    ) {
        weatherDetails.forEach {
            item(key = it.detailsType) {
                WeatherDetailItem(model = it)
            }
        }
    }
}