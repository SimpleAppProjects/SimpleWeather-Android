package com.thewizrd.simpleweather.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.ScalingLazyListAnchorType
import androidx.wear.compose.material.items
import com.thewizrd.common.controls.WeatherAlertsViewModel
import com.thewizrd.simpleweather.ui.components.WeatherAlertPanel

@Composable
fun WeatherAlertsScreen(
    backStackEntry: NavBackStackEntry
) {
    val scalingLazyListState = scalingLazyListState(it = backStackEntry)
    val alertsView = viewModel<WeatherAlertsViewModel>()
    val alerts by alertsView.getAlerts().observeAsState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = scalingLazyListState,
        anchorType = ScalingLazyListAnchorType.ItemCenter,
        contentPadding = PaddingValues(vertical = 48.dp),
        autoCentering = null
    ) {
        alerts?.let {
            items(it) { alert ->
                WeatherAlertPanel(alert)
            }
        }
    }
}