@file:OptIn(ExperimentalHorologistApi::class)

package com.thewizrd.simpleweather.ui.weather

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.ScreenScaffold
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.thewizrd.common.controls.DetailItemViewModel
import com.thewizrd.shared_resources.designer.initializeDependencies
import com.thewizrd.shared_resources.weatherdata.model.AirQuality
import com.thewizrd.shared_resources.weatherdata.model.Beaufort
import com.thewizrd.shared_resources.weatherdata.model.MoonPhase
import com.thewizrd.simpleweather.ui.ScalingLazyListStateViewModel
import com.thewizrd.simpleweather.ui.components.WeatherDetailItem
import com.thewizrd.simpleweather.ui.compose.tools.WearPreviewDevices
import com.thewizrd.simpleweather.ui.utils.rememberFocusRequester

@Composable
fun WeatherDetailsScreen(
    backStackEntry: NavBackStackEntry,
    focusRequester: FocusRequester,
    weatherDetails: Collection<DetailItemViewModel>,
    iconProvider: String? = null
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollStateViewModel: ScalingLazyListStateViewModel = viewModel(backStackEntry)

    ScreenScaffold(scrollState = scrollStateViewModel.scrollState) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .rotaryScrollable(
                    RotaryScrollableDefaults.behavior(scrollStateViewModel.scrollState),
                    focusRequester
                ),
            state = scrollStateViewModel.scrollState,
            anchorType = ScalingLazyListAnchorType.ItemCenter,
            autoCentering = AutoCenteringParams(itemIndex = 0, itemOffset = 0)
        ) {
            weatherDetails.forEach {
                item(key = it.detailsType) {
                    WeatherDetailItem(model = it, iconProvider = iconProvider)
                }
            }
        }

        LaunchedEffect(Unit) {
            lifecycleOwner.repeatOnLifecycle(state = Lifecycle.State.RESUMED) {
                focusRequester.requestFocus()
            }
        }
    }
}

@WearPreviewDevices
@Composable
private fun PreviewWeatherDetailsScreen() {
    val context = LocalContext.current.also {
        it.initializeDependencies(isPhone = false)
    }
    val scrollState = rememberScalingLazyListState()
    val focusRequester = rememberFocusRequester()
    val weatherDetails = remember {
        listOf(
            DetailItemViewModel(MoonPhase.MoonPhaseType.FULL_MOON),
            DetailItemViewModel(Beaufort.BeaufortScale.B10),
            DetailItemViewModel(AirQuality().apply {
                index = 90
            })
        )
    }

    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .rotaryScrollable(RotaryScrollableDefaults.behavior(scrollState), focusRequester),
        state = scrollState,
        anchorType = ScalingLazyListAnchorType.ItemCenter,
        autoCentering = AutoCenteringParams(itemIndex = 0, itemOffset = 0)
    ) {
        items(weatherDetails, key = { it.detailsType }) {
            WeatherDetailItem(model = it)
        }
    }
}