package com.thewizrd.simpleweather.ui.weather

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.util.ObjectsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.LocalContentColor
import androidx.wear.compose.material3.LocalTextStyle
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.google.android.horologist.compose.layout.fillMaxRectangle
import com.thewizrd.common.controls.ForecastItemViewModel
import com.thewizrd.common.controls.HourlyForecastItemViewModel
import com.thewizrd.common.controls.WeatherAlertViewModel
import com.thewizrd.common.controls.WeatherDetailsType
import com.thewizrd.common.controls.WeatherUiModel
import com.thewizrd.common.controls.toUiModel
import com.thewizrd.common.utils.ErrorMessage
import com.thewizrd.shared_resources.R as sharedRes
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.designer.initializeDependencies
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.isLargeWatch
import com.thewizrd.shared_resources.utils.ConversionMethods
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.StringUtils.removeNonDigitChars
import com.thewizrd.shared_resources.utils.Units
import com.thewizrd.shared_resources.utils.getColorFromTempF
import com.thewizrd.shared_resources.weatherdata.model.Condition
import com.thewizrd.shared_resources.weatherdata.model.Location
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.BuildConfig
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.preferences.SettingsActivity
import com.thewizrd.simpleweather.setup.SetupActivity
import com.thewizrd.simpleweather.ui.components.ConfirmationOverlay
import com.thewizrd.simpleweather.ui.components.ForecastItem
import com.thewizrd.simpleweather.ui.components.HourlyForecastItem
import com.thewizrd.simpleweather.ui.components.IconAlignment
import com.thewizrd.simpleweather.ui.components.LoadingContent
import com.thewizrd.simpleweather.ui.components.WearDivider
import com.thewizrd.simpleweather.ui.components.WeatherIcon
import com.thewizrd.simpleweather.ui.compose.CircularWavyProgressIndicator
import com.thewizrd.simpleweather.ui.compose.tools.WearPreviewDevices
import com.thewizrd.simpleweather.ui.navigation.Screen
import com.thewizrd.simpleweather.ui.text.spannableStringToAnnotatedString
import com.thewizrd.simpleweather.ui.theme.findActivity
import com.thewizrd.simpleweather.ui.utils.LogCompositions
import com.thewizrd.simpleweather.viewmodels.ConfirmationData
import com.thewizrd.simpleweather.viewmodels.ConfirmationViewModel
import com.thewizrd.simpleweather.viewmodels.WeatherDataSyncState
import com.thewizrd.simpleweather.viewmodels.WeatherDataSyncViewModel
import com.thewizrd.simpleweather.viewmodels.WeatherNowState
import com.thewizrd.simpleweather.viewmodels.WeatherNowStateModel
import com.thewizrd.simpleweather.viewmodels.WeatherNowViewModel
import com.thewizrd.simpleweather.wearable.WearableListenerActions
import kotlinx.coroutines.launch

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun WeatherNowScreen(
    navController: NavHostController,
    scrollState: ScrollState,
    focusRequester: FocusRequester,
    wNowViewModel: WeatherNowViewModel,
    dataSyncViewModel: WeatherDataSyncViewModel,
    uiState: WeatherNowState,
    syncState: WeatherDataSyncState,
    weather: WeatherUiModel,
    alerts: List<WeatherAlertViewModel>,
    forecasts: List<ForecastItemViewModel>,
    hourlyForecasts: List<HourlyForecastItemViewModel>,
    hasMinutely: Boolean
) {
    val stateModel = viewModel<WeatherNowStateModel>()
    val confirmationViewModel = viewModel<ConfirmationViewModel>()
    val confirmationData by confirmationViewModel.confirmationEventsFlow.collectAsState()

    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val lifecycleOwner = LocalLifecycleOwner.current

    val scrollLoading by stateModel.isLoading.collectAsState()

    if (BuildConfig.DEBUG) {
        LogCompositions(tag = "WeatherNow", msg = "WeatherNowScreen")
    }

    ScreenScaffold(scrollState = scrollState) {
        LoadingContent(
            empty = (uiState.isLoading || syncState.isSyncInProgress) && (uiState.noLocationAvailable || weather.location.isNullOrEmpty()) || scrollLoading,
            emptyContent = {
                Box(
                    modifier = Modifier.fillMaxRectangle(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularWavyProgressIndicator(
                        trackColor = Color.Transparent
                    )
                }
            },
            loading = (uiState.isLoading || syncState.isSyncInProgress),
            onRefresh = {
                wNowViewModel.refreshWeather(true)
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .rotaryScrollable(
                        RotaryScrollableDefaults.behavior(scrollState),
                        focusRequester
                    )
                    .verticalScroll(scrollState)
            ) {
                Column(
                    modifier = Modifier.padding(top = 24.dp, bottom = 48.dp)
                ) {
                    if (uiState.noLocationAvailable) {
                        NoLocationsPrompt(activity)
                    }
                    if (syncState.showDisconnectedView) {
                        DisconnectionAlert()
                    }
                    if (alerts.isNotEmpty()) {
                        AlertsBox(navController)
                    }
                    if (!weather.location.isNullOrEmpty()) {
                        WeatherLocation(
                            locationName = weather.location,
                            isGPSLocation = uiState.isGPSLocation
                        )
                        // Icon + Temp
                        IconTempRow(
                            weatherIcon = weather.weatherIcon,
                            iconProvider = weather.iconProvider,
                            curTemp = weather.curTemp,
                            tempUnit = weather.tempUnit
                        )
                        // Condition
                        weather.curCondition?.let { condition ->
                            ConditionText(condition)
                        }

                        // HiLo Layout
                        if (weather.isShowHiLo) {
                            HiLoLayout(
                                hiTemp = weather.hiTemp,
                                loTemp = weather.loTemp
                            )
                        }

                        // Condition Details
                        ConditionDetails(
                            weather = weather,
                            navController = navController
                        )

                        weather.weatherSummary?.let { summary ->
                            WeatherSummary(weatherSummary = summary)
                        }

                        WearDivider()
                        if (forecasts.isNotEmpty()) {
                            ForecastPanels(
                                forecasts = forecasts,
                                iconProvider = weather.iconProvider,
                                navController = navController
                            )
                        }
                        if (hourlyForecasts.isNotEmpty()) {
                            HourlyForecastPanels(
                                hourlyForecasts = hourlyForecasts,
                                iconProvider = weather.iconProvider,
                                navController = navController
                            )
                        }
                        weather.updateDate?.let { date ->
                            UpdateDateText(date = date)
                        }
                        weather.weatherCredit?.let { credit ->
                            WeatherCreditText(credit = credit)
                        }
                    }

                    // Top divider
                    if (forecasts.isNotEmpty() || hourlyForecasts.isNotEmpty() || hasMinutely || weather.weatherDetailsMap.isNotEmpty()) {
                        WearDivider()
                    }
                    if (forecasts.isNotEmpty()) {
                        ForecastsButton(navController = navController)
                    }
                    if (hourlyForecasts.isNotEmpty()) {
                        HourlyForecastsButton(navController = navController)
                    }
                    if (hasMinutely) {
                        MinutelyForecastsButton(navController = navController)
                    }
                    if (weather.weatherDetailsMap.isNotEmpty()) {
                        DetailsButton(navController = navController)
                    }

                    // Navigation divider
                    WearDivider()
                    DetailsTileEditorButton(navController = navController)
                    WearDivider()

                    ChangeLocationButton(activity = activity)
                    SettingsButton(activity = activity)
                    if (!BuildConfig.IS_NONGMS) {
                        OpenOnPhoneButton(
                            onOpenOnPhone = {
                                dataSyncViewModel.openAppOnPhone(showAnimation = true)
                            }
                        )
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

    ConfirmationOverlay(
        confirmationData = confirmationData,
        onTimeout = { confirmationViewModel.clearFlow() }
    )

    LifecycleResumeEffect(activity) {
        val job = lifecycleOwner.lifecycleScope.launch {
            dataSyncViewModel.errorMessagesFlow.collect { error ->
                when (error) {
                    is ErrorMessage.Resource -> {
                        confirmationViewModel.showFailure(context.getString(error.stringId))
                    }

                    is ErrorMessage.String -> {
                        confirmationViewModel.showFailure(error.message)
                    }

                    is ErrorMessage.WeatherError -> {
                        confirmationViewModel.showFailure(error.exception.message)
                    }
                }
            }
        }

        onPauseOrDispose {
            job.cancel()
        }
    }

    if (!BuildConfig.IS_NONGMS) {
        LifecycleResumeEffect(activity) {
            val job = lifecycleOwner.lifecycleScope.launch {
                dataSyncViewModel.eventFlow.collect { event ->
                    when (event.eventType) {
                        WearableListenerActions.ACTION_SHOWCONFIRMATION -> {
                            val jsonData =
                                event.data.getString(WearableListenerActions.EXTRA_EVENTDATA)

                            JSONParser.deserializer<ConfirmationData>(
                                jsonData,
                                ConfirmationData::class.java
                            )?.let {
                                confirmationViewModel.showConfirmation(it)
                            }
                        }
                    }
                }
            }

            onPauseOrDispose {
                job.cancel()
            }
        }
    }

    LaunchedEffect(stateModel) {
        stateModel.updateLoadingState(false)
    }

    DisposableEffect(stateModel) {
        onDispose {
            stateModel.updateLoadingState(true)
        }
    }
}

/* WeatherNow Screen components */
@Composable
private fun NoLocationsPrompt(
    activity: Activity
) {
    Column(
        modifier = Modifier
            .padding(
                horizontal = 16.dp
            )
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                activity.startActivity(Intent(activity, SetupActivity::class.java))
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Icon(
            painter = painterResource(sharedRes.drawable.ic_location_off_24dp),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(id = sharedRes.string.prompt_location_not_set),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun DisconnectionAlert() {
    Column(
        modifier = Modifier
            .padding(
                vertical = 8.dp,
                horizontal = dimensionResource(id = R.dimen.inner_layout_padding)
            )
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_baseline_cloud_off_24),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(id = R.string.message_disconnected),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AlertsBox(navController: NavHostController) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            modifier = Modifier.requiredSize(IconButtonDefaults.ExtraSmallButtonSize),
            onClick = {
                navController.navigate(Screen.Alerts.route)
            },
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Icon(
                modifier = Modifier.size(IconButtonDefaults.SmallIconSize),
                painter = painterResource(id = sharedRes.drawable.ic_error_white),
                tint = MaterialTheme.colorScheme.onErrorContainer,
                contentDescription = null
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.WeatherLocation(
    locationName: String? = WeatherIcons.EM_DASH,
    isGPSLocation: Boolean = false
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(id = R.dimen.inner_layout_padding),
                end = dimensionResource(id = R.dimen.inner_layout_padding),
                top = 2.dp
            )
            .defaultMinSize(minHeight = 56.dp)
            .wrapContentHeight(Alignment.CenterVertically),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        if (isGPSLocation) {
            Icon(
                modifier = Modifier
                    .size(18.dp)
                    .align(Alignment.CenterVertically),
                painter = painterResource(id = sharedRes.drawable.ic_place_white_24dp),
                contentDescription = null
            )
        }
        Text(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically),
            text = locationName ?: WeatherIcons.EM_DASH,
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun IconTempRow(
    weatherIcon: String,
    curTemp: String?,
    tempUnit: String?,
    iconProvider: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        WeatherIcon(
            modifier = Modifier
                .height(60.dp)
                .weight(1f)
                .align(Alignment.CenterVertically),
            alignment = IconAlignment.End,
            weatherIcon = weatherIcon,
            iconProvider = iconProvider,
            shouldAnimate = true
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically),
            text = curTemp ?: WeatherIcons.PLACEHOLDER,
            textAlign = TextAlign.Start,
            maxLines = 1,
            fontSize = 42.sp,
            fontWeight = FontWeight.Light,
            color = tempTextColor(
                temp = curTemp,
                tempUnit = tempUnit
            )
        )
    }
}

@Composable
private fun ConditionText(
    curCondition: String
) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(id = R.dimen.wnow_horizontal_padding),
                vertical = 8.dp
            ),
        text = curCondition,
        textAlign = TextAlign.Center,
        overflow = TextOverflow.Ellipsis,
        letterSpacing = 0.sp,
        maxLines = 2,
        style = MaterialTheme.typography.bodySmall,
        fontSize = 16.sp
    )
}

@Composable
private fun HiLoLayout(
    hiTemp: String?,
    loTemp: String?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .offset(x = 12.dp, y = (-4).dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = hiTemp ?: WeatherIcons.PLACEHOLDER,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.End,
                    maxLines = 1
                )
                Icon(
                    modifier = Modifier
                        .size(30.dp)
                        .offset(x = (-4).dp),
                    painter = painterResource(id = sharedRes.drawable.wi_direction_up),
                    tint = Color(0xFFFF4500),
                    contentDescription = null
                )
            }
        }
        Spacer(modifier = Modifier.size(8.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .offset(x = (0).dp, y = (-4).dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = loTemp ?: WeatherIcons.PLACEHOLDER,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.End,
                    maxLines = 1
                )
                Icon(
                    modifier = Modifier
                        .size(30.dp)
                        .offset(x = (-4).dp),
                    painter = painterResource(id = sharedRes.drawable.wi_direction_down),
                    tint = Color(0xFF87CEFA),
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun ConditionDetails(
    weather: WeatherUiModel,
    navController: NavHostController
) {
    val context = LocalContext.current
    val isLargeWatch = remember(context) { context.isLargeWatch() }

    val popData = remember(weather) {
        weather.weatherDetailsMap[WeatherDetailsType.POPCHANCE]
    }
    val windData = remember(weather) {
        weather.weatherDetailsMap[WeatherDetailsType.WINDSPEED]
    }

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(id = R.dimen.list_item_padding))
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                navController.navigate(Screen.Details.route)
            },
        verticalArrangement = Arrangement.Center,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (popData != null) {
            Row(
                modifier = Modifier.padding(4.dp)
            ) {
                Icon(
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 4.dp)
                        .align(Alignment.CenterVertically),
                    painter = painterResource(id = sharedRes.drawable.wi_umbrella),
                    tint = colorResource(sharedRes.color.colorPrimaryLight),
                    contentDescription = null
                )
                Text(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    text = spannableStringToAnnotatedString(popData.value),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    color = colorResource(sharedRes.color.colorPrimaryLight)
                )
            }
        }
        if (popData != null && windData != null) {
            Spacer(modifier = Modifier.width(8.dp))
        }
        if (windData != null) {
            Row(
                modifier = Modifier.padding(4.dp)
            ) {
                Icon(
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 4.dp)
                        .rotate(windData.iconRotation.toFloat())
                        .align(Alignment.CenterVertically),
                    painter = painterResource(id = sharedRes.drawable.wi_wind_direction),
                    tint = Color(0xFF20B2AA),
                    contentDescription = null
                )
                Text(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    text = spannableStringToAnnotatedString(if (isLargeWatch || popData == null) windData.value else windData.shortValue),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    color = Color(0xFF20B2AA)
                )
            }
        }
    }
}

@Composable
private fun WeatherSummary(
    weatherSummary: String
) {
    var showDialog by remember { mutableStateOf(false) }

    AlertDialog(
        visible = showDialog,
        onDismissRequest = { showDialog = false },
        title = {
            Text(
                text = "",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
    ) {
        item {
            Text(
                modifier = Modifier.padding(
                    top = dimensionResource(id = R.dimen.header_top_padding),
                    bottom = 48.dp
                ),
                text = weatherSummary,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
    WearDivider()
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
            .padding(
                vertical = 8.dp,
                horizontal = 16.dp
            ),
        text = weatherSummary,
        textAlign = TextAlign.Center,
        overflow = TextOverflow.Ellipsis,
        maxLines = 3,
        letterSpacing = 0.sp,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ForecastPanels(
    forecasts: List<ForecastItemViewModel>,
    navController: NavHostController,
    iconProvider: String? = null
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
    ) {
        forecasts.forEachIndexed { idx, it ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        navController.navigate(Screen.Forecast.route + "?${Constants.KEY_POSITION}=$idx")
                    },
                contentAlignment = Alignment.Center
            ) {
                ForecastItem(
                    model = it,
                    iconProvider = iconProvider
                )
            }
        }
    }
}

@Composable
private fun HourlyForecastPanels(
    hourlyForecasts: List<HourlyForecastItemViewModel>,
    navController: NavHostController,
    iconProvider: String? = null
) {
    Column(
        modifier = Modifier
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .fillMaxWidth()
    ) {
        hourlyForecasts.forEachIndexed { idx, it ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        navController.navigate(Screen.HourlyForecast.route + "?${Constants.KEY_POSITION}=$idx")
                    }
            ) {
                HourlyForecastItem(
                    model = it,
                    iconProvider = iconProvider
                )
            }
        }
    }
}

@Composable
private fun UpdateDateText(
    date: String
) {
    Text(
        modifier = Modifier
            .padding(2.dp)
            .fillMaxWidth(),
        textAlign = TextAlign.Center,
        text = date,
        style = MaterialTheme.typography.bodyExtraSmall
    )
}

@Composable
private fun WeatherCreditText(
    credit: String
) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(id = R.dimen.wnow_horizontal_padding),
                vertical = 4.dp
            ),
        textAlign = TextAlign.Center,
        text = credit,
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun ForecastsButton(
    navController: NavHostController
) {
    NavigationButton(
        label = stringResource(id = sharedRes.string.label_forecast),
        iconDrawableId = R.drawable.ic_date_range_black_24dp
    ) {
        navController.navigate(Screen.Forecast.route)
    }
}

@Composable
private fun HourlyForecastsButton(
    navController: NavHostController
) {
    NavigationButton(
        label = stringResource(id = sharedRes.string.label_hourlyforecast),
        iconDrawableId = R.drawable.ic_access_time_black_24dp
    ) {
        navController.navigate(Screen.HourlyForecast.route)
    }
}

@Composable
private fun MinutelyForecastsButton(
    navController: NavHostController
) {
    NavigationButton(
        label = stringResource(id = sharedRes.string.label_precipitation),
        iconDrawableId = sharedRes.drawable.wi_raindrops
    ) {
        navController.navigate(Screen.Precipitation.route)
    }
}

@Composable
private fun DetailsButton(
    navController: NavHostController
) {
    NavigationButton(
        label = stringResource(id = sharedRes.string.label_details),
        iconDrawableId = R.drawable.ic_list_black_24dp
    ) {
        navController.navigate(Screen.Details.route)
    }
}

@Composable
private fun DetailsTileEditorButton(
    navController: NavHostController
) {
    NavigationButton(
        label = stringResource(id = R.string.pref_title_detailstileeditor),
        iconDrawableId = sharedRes.drawable.ic_mode_edit_white_24dp
    ) {
        navController.navigate(Screen.DetailsTileEditor.route)
    }
}

@Composable
private fun ChangeLocationButton(
    activity: Activity
) {
    NavigationButton(
        label = stringResource(id = R.string.action_changelocation),
        iconDrawableId = R.drawable.ic_edit_location_black_24dp
    ) {
        activity.startActivity(Intent(activity, SetupActivity::class.java))
    }
}

@Composable
private fun SettingsButton(
    activity: Activity
) {
    NavigationButton(
        label = stringResource(id = sharedRes.string.action_settings),
        iconDrawableId = sharedRes.drawable.ic_settings_black_24dp
    ) {
        activity.startActivity(Intent(activity, SettingsActivity::class.java))
    }
}

@Composable
private fun OpenOnPhoneButton(
    onOpenOnPhone: () -> Unit
) {
    NavigationButton(
        label = stringResource(id = R.string.action_openonphone),
        iconDrawableId = com.google.android.gms.base.R.drawable.common_full_open_on_phone,
        onClick = onOpenOnPhone
    )
}

@Composable
private fun NavigationButton(
    label: String,
    @DrawableRes iconDrawableId: Int,
    contentDescription: String? = null,
    onClick: () -> Unit,
) {
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(vertical = 2.dp, horizontal = 16.dp),
        onClick = onClick,
        colors = ButtonDefaults.filledTonalButtonColors(),
        label = {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                text = label,
                overflow = TextOverflow.Ellipsis,
                softWrap = true,
                maxLines = 2
            )
        },
        icon = {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(id = iconDrawableId),
                contentDescription = contentDescription
            )
        }
    )
}

@Composable
private fun tempTextColor(temp: CharSequence?, @Units.TemperatureUnits tempUnit: String?): Color {
    val tempStr = temp?.removeNonDigitChars()
    var tempF = tempStr?.toString()?.toFloatOrNull()

    return if (tempF != null) {
        if (ObjectsCompat.equals(tempUnit, Units.CELSIUS) || temp.toString()
                .endsWith(Units.CELSIUS)
        ) {
            tempF = ConversionMethods.CtoF(tempF)
        }

        Color(getColorFromTempF(tempF, Colors.WHITE))
    } else {
        colorResource(id = R.color.colorTextPrimary)
    }
}

@WearPreviewDevices
@Composable
private fun PreviewWeatherNowScreen() {
    val context = LocalContext.current.run {
        initializeDependencies(isPhone = false)

        val oldConfig = resources.configuration
        val newConfig = Configuration(oldConfig)
        newConfig.uiMode =
            Configuration.UI_MODE_NIGHT_YES or (newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv())
        resources.updateConfiguration(newConfig, resources.displayMetrics)

        this
    }
    val weather = remember {
        Weather().apply {
            condition = Condition().apply {
                icon = WeatherIcons.DAY_SUNNY
                tempF = 70f
                weather = "Sunny"
                highF = 75f
                lowF = 60f
            }
            location = Location().apply {
                name = "New York"
            }
        }.toUiModel()
    }

    CompositionLocalProvider(
        LocalContentColor provides Color.White,
        LocalTextStyle provides MaterialTheme.typography.labelLarge,
        LocalContext provides context
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(Color.White.copy(alpha = 0.25f))
                    .align(Alignment.Center)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.25f))
                    .align(Alignment.Center)
            )
            Column {
                WeatherLocation(
                    locationName = "New York",
                    isGPSLocation = true
                )
                // Icon + Temp
                Box(
                    modifier = Modifier.background(Color.White.copy(alpha = 0.1f))
                ) {
                    IconTempRow(
                        weatherIcon = WeatherIcons.DAY_SUNNY,
                        curTemp = "70°F",
                        tempUnit = "F"
                    )
                }
                // Condition
                ConditionText("Sunny")

                // HiLo Layout
                HiLoLayout(
                    hiTemp = "70°",
                    loTemp = "60°"
                )

                // Condition Details
                ConditionDetails(
                    weather = weather,
                    navController = rememberSwipeDismissableNavController()
                )
            }
        }
    }
}