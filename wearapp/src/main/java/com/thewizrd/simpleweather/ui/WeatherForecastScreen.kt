package com.thewizrd.simpleweather.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.wear.compose.material.AutoCenteringParams
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.ScalingLazyListAnchorType
import com.google.android.horologist.compose.navscaffold.scrollableColumn
import com.thewizrd.common.controls.ForecastsListViewModel
import com.thewizrd.shared_resources.Constants
import com.thewizrd.simpleweather.ui.components.WeatherForecastPanel
import com.thewizrd.simpleweather.ui.paging.items
import com.thewizrd.simpleweather.ui.theme.activityViewModel
import kotlinx.coroutines.delay

@Composable
fun WeatherForecastScreen(
    backStackEntry: NavBackStackEntry
) {
    val scalingLazyListState = scalingLazyListState(it = backStackEntry)
    val focusRequester = remember { FocusRequester() }
    val lifecycleOwner = LocalLifecycleOwner.current

    val scrollToPosition = remember(backStackEntry) {
        backStackEntry.arguments?.getInt(Constants.KEY_POSITION) ?: 0
    }

    val forecastsView = activityViewModel<ForecastsListViewModel>()
    val forecasts = forecastsView.getForecasts().collectAsLazyPagingItems()

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