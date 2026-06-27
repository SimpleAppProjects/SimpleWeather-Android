@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)

package com.thewizrd.simpleweather.ui.preferences

import android.annotation.SuppressLint
import android.text.format.DateFormat
import android.view.MotionEvent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.pointer.motionEventSpy
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.LocalContentColor
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.thewizrd.common.controls.WeatherDetailsType
import com.thewizrd.shared_resources.DateTimeConstants
import com.thewizrd.shared_resources.designer.initializeDependencies
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.preferences.DetailsWeatherTileUtils
import com.thewizrd.simpleweather.ui.LazyGridStateViewModel
import com.thewizrd.simpleweather.ui.components.CustomTimeText
import com.thewizrd.simpleweather.ui.components.WeatherIcon
import com.thewizrd.simpleweather.ui.compose.LazyGridScrollIndicator
import com.thewizrd.simpleweather.ui.compose.LazyGridScrollInfoProvider
import com.thewizrd.simpleweather.ui.compose.tools.WearPreviewDevices
import com.thewizrd.simpleweather.ui.theme.WearAppTheme
import com.thewizrd.simpleweather.ui.time.ZonedTimeSource
import com.thewizrd.simpleweather.ui.utils.ReorderHapticFeedbackType
import com.thewizrd.simpleweather.ui.utils.rememberFocusRequester
import com.thewizrd.simpleweather.ui.utils.rememberReorderHapticFeedback
import com.thewizrd.simpleweather.viewmodels.WeatherNowState
import com.thewizrd.simpleweather.wearable.tiles.DetailsWeatherTileProviderService
import com.thewizrd.simpleweather.wearable.tiles.WeatherTileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import java.util.Collections
import kotlin.math.roundToInt
import com.thewizrd.shared_resources.R as sharedRes

@Composable
fun DetailsWeatherTileConfigScreen(
    navController: NavController,
    backStackEntry: NavBackStackEntry,
    focusRequester: FocusRequester,
    uiState: WeatherNowState,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollStateViewModel: LazyGridStateViewModel = viewModel(backStackEntry)

    var isConfigChanged by remember { mutableStateOf(false) }

    var tileConfig: List<WeatherDetailsType> by remember {
        mutableStateOf(
            DetailsWeatherTileUtils.getTileConfig() ?: DetailsWeatherTileUtils.DEFAULT_ITEMS
        )
    }

    ScreenScaffold(
        timeText = {
            CustomTimeText(
                visible = true,
                modifier = Modifier.offset {
                    if (0 < scrollStateViewModel.scrollState.layoutInfo.totalItemsCount) {
                        scrollStateViewModel.scrollState.layoutInfo.visibleItemsInfo.find {
                            it.index == 0
                        }?.offset
                            ?: if (scrollStateViewModel.scrollState.layoutInfo.orientation == Orientation.Vertical) {
                                IntOffset(
                                    0,
                                    -36.dp.toPx().roundToInt()
                                )
                            } else {
                                IntOffset(
                                    -36.dp.toPx().roundToInt(), 0
                                )
                            }
                    } else {
                        IntOffset.Zero
                    }
                },
                timeSource = ZonedTimeSource(
                    timeFormat = if (DateFormat.is24HourFormat(LocalContext.current)) {
                        "${DateTimeConstants.CLOCK_FORMAT_24HR} ${DateTimeConstants.TIMEZONE_NAME}"
                    } else {
                        "${DateTimeConstants.CLOCK_FORMAT_12HR} ${DateTimeConstants.TIMEZONE_NAME}"
                    },
                    timeZone = uiState.locationData?.tzLong
                )
            )
        },
        scrollInfoProvider = LazyGridScrollInfoProvider(scrollStateViewModel.scrollState),
        scrollIndicator = {
            LazyGridScrollIndicator(lazyGridState = scrollStateViewModel.scrollState)
        }
    ) {
        DetailsWeatherTileConfigScreen(
            lazyGridState = scrollStateViewModel.scrollState,
            focusRequester = focusRequester,
            tileConfig = tileConfig,
            onSaveItems = { items ->
                if (items.isEmpty()) {
                    DetailsWeatherTileUtils.setTileConfig(null)
                } else {
                    DetailsWeatherTileUtils.setTileConfig(items)
                }

                navController.popBackStack()
            }
        )

        LaunchedEffect(lifecycleOwner) {
            lifecycleOwner.repeatOnLifecycle(state = Lifecycle.State.RESUMED) {
                focusRequester.requestFocus()
            }
        }
    }

    LaunchedEffect(lifecycleOwner) {
        DetailsWeatherTileUtils.getTileConfigFlow().collect {
            tileConfig = it ?: DetailsWeatherTileUtils.DEFAULT_ITEMS
            isConfigChanged = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isConfigChanged) {
                // Trigger tile update
                WeatherTileHelper.requestTileUpdate(
                    context,
                    DetailsWeatherTileProviderService::class.java
                )
            }
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun DetailsWeatherTileConfigScreen(
    lazyGridState: LazyGridState = rememberLazyGridState(),
    focusRequester: FocusRequester = rememberFocusRequester(),
    tileConfig: List<WeatherDetailsType> = DetailsWeatherTileUtils.DEFAULT_ITEMS,
    onSaveItems: (List<WeatherDetailsType>) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptic = rememberReorderHapticFeedback()

    var showConfirmation by remember { mutableStateOf(false) }
    var showAddTileDialog by remember { mutableStateOf(false) }

    val userTileConfigList: MutableList<Any> =
        remember(tileConfig) { tileConfig.toMutableStateList() }
    val selectionList =
        remember { MutableList(DetailsWeatherTileUtils.MAX_BUTTONS) { false }.toMutableStateList() }

    val reorderableGridState = rememberReorderableLazyGridState(
        lazyGridState = lazyGridState
    ) { from, to ->
        // Offset index by 1 to account for header
        Collections.swap(userTileConfigList, from.index - 1, to.index - 1)
        haptic.performHapticFeedback(ReorderHapticFeedbackType.MOVE)
    }

    LazyVerticalGrid(
        modifier = Modifier
            .fillMaxSize()
            .rotaryScrollable(RotaryScrollableDefaults.behavior(lazyGridState), focusRequester)
            .motionEventSpy { event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    selectionList.replaceAll { false }
                }
            },
        columns = GridCells.Fixed(6),
        state = lazyGridState,
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        userScrollEnabled = true
    ) {
        item(span = { GridItemSpan(6) }) {
            ListHeader {
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = stringResource(R.string.title_tile_config),
                    textAlign = TextAlign.Center
                )
            }
        }

        itemsIndexed(
            userTileConfigList,
            key = { index, item -> item as? WeatherDetailsType ?: index },
            span = { index, _ ->
                if (index == 0 || index == 1 || index == 5 || index == 6) {
                    GridItemSpan(3)
                } else {
                    GridItemSpan(2)
                }
            }
        ) { index, item ->
            val itemModifier = remember(index) {
                when (index) {
                    0, 5 -> {
                        Modifier
                            .wrapContentSize(Alignment.CenterEnd)
                            .padding(horizontal = 4.dp)
                    }

                    1, 6 -> {
                        Modifier
                            .wrapContentSize(Alignment.CenterStart)
                            .padding(horizontal = 4.dp)
                    }

                    else -> {
                        Modifier.wrapContentSize()
                    }
                }
            }

            if (item is WeatherDetailsType) {
                ReorderableItem(
                    modifier = itemModifier,
                    state = reorderableGridState,
                    key = item
                ) { _ ->
                    Box(
                        modifier = Modifier
                            .draggableHandle(
                                onDragStarted = {
                                    haptic.performHapticFeedback(ReorderHapticFeedbackType.START)
                                },
                                onDragStopped = {
                                    haptic.performHapticFeedback(ReorderHapticFeedbackType.END)
                                },
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedVisibility(
                            visible = selectionList[index],
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            IconButton(
                                modifier = Modifier.size(ButtonDefaults.Height),
                                colors = IconButtonDefaults.filledIconButtonColors(),
                                onClick = {
                                    userTileConfigList.removeAt(index)
                                    userTileConfigList.add("")
                                }
                            ) {
                                Icon(
                                    modifier = Modifier.size(ButtonDefaults.IconSize),
                                    painter = painterResource(id = sharedRes.drawable.ic_close_white_24dp),
                                    contentDescription = null,
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = !selectionList[index],
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            val onClick: () -> Unit = { selectionList[index] = true }
                            val onLongClick: () -> Unit = {
                                coroutineScope.launch(Dispatchers.Main.immediate) {
                                    Toast.makeText(
                                        context,
                                        item.toLabelStringResId(),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            val iconResId = remember(item) {
                                val wim = sharedDeps.weatherIconsManager.iconProvider
                                wim.getWeatherIconResource(item.toWeatherIcon())
                            }

                            IconButton(
                                colors = IconButtonDefaults.filledTonalIconButtonColors(),
                                onClick = onClick,
                                onLongClick = onLongClick
                            ) {
                                Icon(
                                    modifier = Modifier.size(ButtonDefaults.IconSize),
                                    painter = painterResource(id = iconResId),
                                    contentDescription = stringResource(item.toLabelStringResId())
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = itemModifier,
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        modifier = Modifier.size(ButtonDefaults.Height),
                        colors = IconButtonDefaults.filledIconButtonColors(),
                        onClick = {
                            showAddTileDialog = true
                        }
                    ) {
                        Icon(
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                            painter = painterResource(id = R.drawable.ic_add_24dp),
                            contentDescription = null,
                        )
                    }
                }
            }
        }

        item(span = { GridItemSpan(6) }) {
            Spacer(Modifier.height(8.dp))
        }

        item(span = { GridItemSpan(6) }) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    modifier = Modifier.size(ButtonDefaults.Height),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(),
                    onClick = {
                        showConfirmation = true
                    }
                ) {
                    Icon(
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                        painter = painterResource(id = R.drawable.ic_restart_alt_24dp),
                        contentDescription = stringResource(sharedRes.string.action_reset),
                    )
                }
                IconButton(
                    modifier = Modifier.size(ButtonDefaults.Height),
                    colors = IconButtonDefaults.filledIconButtonColors(),
                    onClick = {
                        onSaveItems.invoke(userTileConfigList.filterIsInstance<WeatherDetailsType>())
                    }
                ) {
                    Icon(
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                        painter = painterResource(id = R.drawable.ic_check_24dp),
                        contentDescription = stringResource(android.R.string.ok),
                    )
                }
            }
        }

        item(span = { GridItemSpan(6) }) {
            Spacer(Modifier.height(48.dp))
        }
    }

    AlertDialog(
        visible = showConfirmation,
        onDismissRequest = {
            showConfirmation = false
        },
        confirmButton = {
            AlertDialogDefaults.ConfirmButton(
                onClick = {
                    userTileConfigList.clear()
                    userTileConfigList.addAll(DetailsWeatherTileUtils.DEFAULT_ITEMS)
                    DetailsWeatherTileUtils.setTileConfig(null)
                    showConfirmation = false
                }
            )
        },
        dismissButton = {
            AlertDialogDefaults.DismissButton(
                onClick = {
                    showConfirmation = false
                }
            )
        },
        title = {
            Text(text = stringResource(id = R.string.message_reset_to_default))
        }
    )

    if (showAddTileDialog) {
        val availableItems = WeatherDetailsType.entries.toMutableList()
        // Remove current actions
        availableItems.removeAll(userTileConfigList.filterIsInstance<WeatherDetailsType>())
        // Remove unsupported actions
        availableItems.removeIf { !DetailsWeatherTileUtils.isTypeAllowed(it) }

        AlertDialog(
            modifier = Modifier.fillMaxSize(),
            visible = showAddTileDialog,
            onDismissRequest = { showAddTileDialog = false },
            title = { Text(text = stringResource(id = sharedRes.string.label_details)) },
            edgeButton = {
                EdgeButton(
                    onClick = { showAddTileDialog = false }
                ) {
                    Icon(
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                        painter = painterResource(id = sharedRes.drawable.ic_close_white_24dp),
                        contentDescription = stringResource(android.R.string.cancel)
                    )
                }
            }
        ) {
            items(availableItems) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    label = {
                        Text(
                            text = stringResource(it.toLabelStringResId())
                        )
                    },
                    icon = {
                        WeatherIcon(
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                            weatherIcon = it.toWeatherIcon(),
                            tint = LocalContentColor.current
                        )
                    },
                    onClick = {
                        val index = userTileConfigList.indexOfFirst { it !is WeatherDetailsType }
                        if (index >= 0) {
                            userTileConfigList[index] = it
                        }
                        showAddTileDialog = false
                    }
                )
            }
        }
    }

    LaunchedEffect(tileConfig) {
        if (userTileConfigList.size < DetailsWeatherTileUtils.MAX_BUTTONS && !userTileConfigList.contains(
                ""
            )
        ) {
            userTileConfigList.add("")
        }
    }
}

@WearPreviewDevices
@Composable
private fun PreviewDetailsWeatherTileConfigScreen() {
    val context = LocalContext.current.also {
        it.initializeDependencies(isPhone = false)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    val focusRequester = rememberFocusRequester()

    WearAppTheme {
        DetailsWeatherTileConfigScreen(
            focusRequester = focusRequester,
            tileConfig = DetailsWeatherTileUtils.DEFAULT_ITEMS
        )
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(state = Lifecycle.State.RESUMED) {
            focusRequester.requestFocus()
        }
    }
}

private fun WeatherDetailsType.toWeatherIcon(): String = when (this) {
    WeatherDetailsType.SUNRISE -> WeatherIcons.SUNRISE
    WeatherDetailsType.SUNSET -> WeatherIcons.SUNSET
    WeatherDetailsType.FEELSLIKE -> WeatherIcons.THERMOMETER
    WeatherDetailsType.WINDSPEED -> WeatherIcons.WIND_DIRECTION
    WeatherDetailsType.WINDGUST -> WeatherIcons.CLOUDY_GUSTS
    WeatherDetailsType.HUMIDITY -> WeatherIcons.HUMIDITY
    WeatherDetailsType.PRESSURE -> WeatherIcons.BAROMETER
    WeatherDetailsType.VISIBILITY -> WeatherIcons.VISIBILITY
    WeatherDetailsType.POPCLOUDINESS -> WeatherIcons.CLOUDY
    WeatherDetailsType.POPCHANCE -> WeatherIcons.UMBRELLA
    WeatherDetailsType.POPRAIN -> WeatherIcons.RAINDROPS
    WeatherDetailsType.POPSNOW -> WeatherIcons.SNOWFLAKE_COLD
    WeatherDetailsType.DEWPOINT -> WeatherIcons.THERMOMETER
    WeatherDetailsType.MOONRISE -> WeatherIcons.MOONRISE
    WeatherDetailsType.MOONSET -> WeatherIcons.MOONSET
    WeatherDetailsType.MOONPHASE -> WeatherIcons.MOON_NEW
    WeatherDetailsType.BEAUFORT -> WeatherIcons.WIND_BEAUFORT_0
    WeatherDetailsType.UV -> WeatherIcons.UV_INDEX
    WeatherDetailsType.AIRQUALITY -> WeatherIcons.AIR_QUALITY
    WeatherDetailsType.TREEPOLLEN -> WeatherIcons.TREE_POLLEN
    WeatherDetailsType.GRASSPOLLEN -> WeatherIcons.GRASS_POLLEN
    WeatherDetailsType.RAGWEEDPOLLEN -> WeatherIcons.RAGWEED_POLLEN
}

private fun WeatherDetailsType.toLabelStringResId(): Int = when (this) {
    WeatherDetailsType.SUNRISE -> sharedRes.string.label_sunrise
    WeatherDetailsType.SUNSET -> sharedRes.string.label_sunset
    WeatherDetailsType.FEELSLIKE -> sharedRes.string.label_feelslike
    WeatherDetailsType.WINDSPEED -> sharedRes.string.label_wind
    WeatherDetailsType.WINDGUST -> sharedRes.string.label_windgust
    WeatherDetailsType.HUMIDITY -> sharedRes.string.label_humidity
    WeatherDetailsType.PRESSURE -> sharedRes.string.label_pressure
    WeatherDetailsType.VISIBILITY -> sharedRes.string.label_visibility
    WeatherDetailsType.POPCHANCE -> sharedRes.string.label_chance
    WeatherDetailsType.POPCLOUDINESS -> sharedRes.string.label_cloudiness
    WeatherDetailsType.POPRAIN -> sharedRes.string.label_qpf_rain
    WeatherDetailsType.POPSNOW -> sharedRes.string.label_qpf_snow
    WeatherDetailsType.DEWPOINT -> sharedRes.string.label_dewpoint
    WeatherDetailsType.MOONRISE -> sharedRes.string.label_moonrise
    WeatherDetailsType.MOONSET -> sharedRes.string.label_moonset
    WeatherDetailsType.MOONPHASE -> sharedRes.string.label_moonphase
    WeatherDetailsType.BEAUFORT -> sharedRes.string.label_beaufort
    WeatherDetailsType.UV -> sharedRes.string.label_uv
    WeatherDetailsType.AIRQUALITY -> sharedRes.string.label_airquality
    WeatherDetailsType.TREEPOLLEN -> sharedRes.string.label_tree_pollen
    WeatherDetailsType.GRASSPOLLEN -> sharedRes.string.label_grass_pollen
    WeatherDetailsType.RAGWEEDPOLLEN -> sharedRes.string.label_ragweed_pollen
}