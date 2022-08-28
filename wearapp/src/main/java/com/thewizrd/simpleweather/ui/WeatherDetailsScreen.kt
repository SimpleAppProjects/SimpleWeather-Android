package com.thewizrd.simpleweather.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.ScalingLazyListAnchorType
import androidx.wear.compose.material.ScalingLazyListState
import com.google.android.horologist.compose.navscaffold.scrollableColumn
import com.thewizrd.common.controls.DetailItemViewModel
import com.thewizrd.simpleweather.ui.components.WeatherDetailItem

@Composable
fun WeatherDetailsScreen(
    scalingLazyListState: ScalingLazyListState,
    focusRequester: FocusRequester,
    weatherDetails: Collection<DetailItemViewModel>
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .scrollableColumn(focusRequester, scalingLazyListState),
        state = scalingLazyListState,
        anchorType = ScalingLazyListAnchorType.ItemStart
    ) {
        weatherDetails.forEach {
            item(key = it.detailsType) {
                WeatherDetailItem(model = it)
            }
        }
    }

    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(state = Lifecycle.State.RESUMED) {
            focusRequester.requestFocus()
        }
    }
}