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
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.ScalingLazyListAnchorType
import androidx.wear.compose.material.items
import com.google.android.horologist.compose.navscaffold.scrollableColumn
import com.thewizrd.common.controls.WeatherAlertViewModel
import com.thewizrd.simpleweather.ui.ScalingLazyListStateViewModel
import com.thewizrd.simpleweather.ui.components.WeatherAlertPanel

@Composable
fun WeatherAlertsScreen(
    backStackEntry: NavBackStackEntry,
    focusRequester: FocusRequester,
    alerts: List<WeatherAlertViewModel>
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollStateViewModel: ScalingLazyListStateViewModel = viewModel(backStackEntry)

    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .scrollableColumn(focusRequester, scrollStateViewModel.scrollState),
        state = scrollStateViewModel.scrollState,
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

    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(state = Lifecycle.State.RESUMED) {
            focusRequester.requestFocus()
        }
    }
}