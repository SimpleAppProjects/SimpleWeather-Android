package com.thewizrd.simpleweather.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListSubHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.thewizrd.common.viewmodels.LocationSearchViewModel
import com.thewizrd.shared_resources.R as sharedRes
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.locationdata.LocationQuery
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.ui.compose.CircularWavyProgressIndicator
import com.thewizrd.simpleweather.ui.compose.tools.WearPreviewDevices
import com.thewizrd.simpleweather.ui.theme.activityViewModel
import com.thewizrd.simpleweather.ui.utils.rememberFocusRequester
import com.thewizrd.weather_api.weatherModule

@Composable
fun SetupLocationListScreen(
    navController: NavController,
    backStackEntry: NavBackStackEntry,
    focusRequester: FocusRequester
) {
    val locationSearchViewModel = activityViewModel<LocationSearchViewModel>()
    val isLoading by locationSearchViewModel.isLoading.collectAsState()
    val locations by locationSearchViewModel.locations.collectAsState()
    val locationSource = remember {
        val locationAPI =
            weatherModule.weatherManager.getLocationProvider().getLocationAPI()
        val entry = WeatherAPI.LocationAPIs.find { lapi -> locationAPI == lapi.value }
        entry?.toString() ?: WeatherIcons.EM_DASH
    }

    SetupLocationListScreen(
        focusRequester = focusRequester,
        isLoading = isLoading,
        locations = locations,
        locationSource = locationSource,
        onLocationSelected = {
            if (it != LocationQuery.EMPTY) {
                locationSearchViewModel.onLocationSelected(it)
            }
        },
        onReturnToSearch = {
            navController.popBackStack()
        }
    )

    LaunchedEffect(backStackEntry) {
        backStackEntry.arguments?.getString(Constants.KEY_SEARCH)?.let { text ->
            locationSearchViewModel.fetchLocations(text)
        }
    }
}

@Composable
private fun SetupLocationListScreen(
    focusRequester: FocusRequester,
    isLoading: Boolean = false,
    locationSource: String = "Weather Provider",
    locations: List<LocationQuery> = emptyList(),
    onLocationSelected: ((LocationQuery) -> Unit) = {},
    onReturnToSearch: () -> Unit = {}
) {
    val context = LocalContext.current

    if (isLoading) {
        ScreenScaffold {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                CircularWavyProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    } else {
        if (locations.isNotEmpty()) {
            val scrollState = rememberTransformingLazyColumnState()
            val transformationSpec = rememberTransformationSpec()

            ScreenScaffold(scrollState = scrollState) { contentPadding ->
                TransformingLazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotaryScrollable(
                            RotaryScrollableDefaults.behavior(scrollState),
                            focusRequester
                        ),
                    state = scrollState,
                    contentPadding = contentPadding,
                ) {
                    item {
                        ListHeader(
                            modifier = Modifier
                                .transformedHeight(this, transformationSpec)
                                .animateItem(),
                            transformation = SurfaceTransformation(transformationSpec),
                        ) {
                            Text(text = stringResource(id = sharedRes.string.label_nav_locations))
                        }
                    }

                    items(items = locations) {
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .transformedHeight(this, transformationSpec)
                                .animateItem(),
                            transformation = SurfaceTransformation(transformationSpec),
                            onClick = {
                                onLocationSelected(it)
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(),
                            label = {
                                Text(text = it.locationName ?: "", maxLines = 3)
                            },
                            secondaryLabel = it.locationCountry?.let { country ->
                                {
                                    Text(text = country)
                                }
                            },
                            icon = if (it.locationQuery.isNullOrBlank() && it.locationCountry.isNullOrBlank()) {
                                null
                            } else {
                                {
                                    Icon(
                                        painter = painterResource(id = sharedRes.drawable.ic_place_white_24dp),
                                        contentDescription = null
                                    )
                                }
                            }
                        )
                    }

                    item {
                        ListSubHeader(
                            modifier = Modifier
                                .transformedHeight(this, transformationSpec)
                                .animateItem(),
                            transformation = SurfaceTransformation(transformationSpec),
                        ) {
                            val creditPrefix = stringResource(sharedRes.string.credit_prefix)
                            Text(text = remember(context, locationSource) {
                                buildString {
                                    append(creditPrefix)
                                    append(" ")
                                    append(locationSource)
                                }
                            }, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        } else {
            ScreenScaffold { contentPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
                ) {
                    Text(
                        text = stringResource(id = sharedRes.string.werror_querynotfound),
                        textAlign = TextAlign.Center
                    )
                    CompactButton(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        label = {
                            Text(text = stringResource(id = android.R.string.search_go))
                        },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_search_black_24dp),
                                contentDescription = stringResource(id = android.R.string.search_go)
                            )
                        },
                        onClick = onReturnToSearch
                    )
                }
            }
        }
    }
}

@WearPreviewDevices
@Composable
private fun PreviewSetupLocationListScreen() {
    SetupLocationListScreen(
        focusRequester = rememberFocusRequester(),
        isLoading = false,
        locations = emptyList()
    )
}