package com.thewizrd.simpleweather.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.wear.compose.material.*
import com.google.android.horologist.compose.navscaffold.scrollableColumn
import com.thewizrd.shared_resources.Constants
import com.thewizrd.simpleweather.ui.components.WeatherMinutelyForecastPanel
import com.thewizrd.simpleweather.ui.theme.activityViewModel
import com.thewizrd.simpleweather.viewmodels.ForecastPanelsViewModel
import kotlinx.coroutines.delay

@Composable
fun WeatherMinutelyForecastScreen(
    scalingLazyListState: ScalingLazyListState,
    focusRequester: FocusRequester,
    backStackEntry: NavBackStackEntry
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    val scrollToPosition = remember(backStackEntry) {
        backStackEntry.arguments?.getInt(Constants.KEY_POSITION) ?: 0
    }

    val forecastsPanelView = activityViewModel<ForecastPanelsViewModel>()
    val minutelyForecasts by forecastsPanelView.getMinutelyForecasts().collectAsState()

    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .scrollableColumn(focusRequester, scalingLazyListState),
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
            items(
                minFcasts,
                key = {
                    it.hashCode()
                }
            ) {
                WeatherMinutelyForecastPanel(model = it)
            }
        }
    }

    LaunchedEffect(scalingLazyListState) {
        delay(50)
        scalingLazyListState.scrollToItem(scrollToPosition)
    }

    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(state = Lifecycle.State.RESUMED) {
            focusRequester.requestFocus()
        }
    }
}