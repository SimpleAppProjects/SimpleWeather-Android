package com.thewizrd.simpleweather.ui.weather

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
import androidx.wear.compose.foundation.lazy.items
import com.google.android.horologist.compose.navscaffold.scrollableColumn
import com.thewizrd.shared_resources.Constants
import com.thewizrd.simpleweather.ui.ScalingLazyListStateViewModel
import com.thewizrd.simpleweather.ui.components.WeatherMinutelyForecastPanel
import com.thewizrd.simpleweather.ui.theme.activityViewModel
import com.thewizrd.simpleweather.viewmodels.ForecastPanelsViewModel

@Composable
fun WeatherMinutelyForecastScreen(
    backStackEntry: NavBackStackEntry,
    focusRequester: FocusRequester
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    val forecastsPanelView = activityViewModel<ForecastPanelsViewModel>()
    val minutelyForecasts by forecastsPanelView.getMinutelyForecasts().collectAsState()

    val scrollStateViewModel: ScalingLazyListStateViewModel = viewModel(backStackEntry)

    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .scrollableColumn(focusRequester, scrollStateViewModel.scrollState),
        state = scrollStateViewModel.scrollState,
        anchorType = ScalingLazyListAnchorType.ItemCenter,
        contentPadding = PaddingValues(top = 48.dp)
    ) {
        items(minutelyForecasts) {
            WeatherMinutelyForecastPanel(model = it)
        }
    }

    LaunchedEffect(backStackEntry) {
        backStackEntry.arguments?.getInt(Constants.KEY_POSITION)?.let { position ->
            scrollStateViewModel.scrollState.scrollToItem(position)
        }
    }

    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(state = Lifecycle.State.RESUMED) {
            focusRequester.requestFocus()
        }
    }
}