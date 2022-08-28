package com.thewizrd.simpleweather.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.ScalingLazyListAnchorType
import androidx.wear.compose.material.items
import androidx.wear.compose.material.rememberScalingLazyListState
import com.thewizrd.common.controls.WeatherAlertViewModel
import com.thewizrd.simpleweather.ui.components.WeatherAlertPanel

@Composable
fun WeatherAlertsScreen(
    alerts: List<WeatherAlertViewModel>
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = rememberScalingLazyListState(),
        anchorType = ScalingLazyListAnchorType.ItemCenter,
        contentPadding = PaddingValues(vertical = 48.dp),
        autoCentering = null
    ) {
        items(alerts, key = {
            it.hashCode()
        }) { alert ->
            WeatherAlertPanel(alert)
        }
    }
}