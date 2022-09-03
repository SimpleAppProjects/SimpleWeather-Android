package com.thewizrd.simpleweather.ui.weather

import android.app.Activity
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
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
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.wear.compose.material.*
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.dialog.Dialog
import com.google.accompanist.flowlayout.FlowCrossAxisAlignment
import com.google.accompanist.flowlayout.FlowMainAxisAlignment
import com.google.accompanist.flowlayout.FlowRow
import com.google.android.horologist.compose.layout.fillMaxRectangle
import com.google.android.horologist.compose.navscaffold.scrollableColumn
import com.thewizrd.common.controls.*
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.di.localBroadcastManager
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ConversionMethods
import com.thewizrd.shared_resources.utils.StringUtils.removeNonDigitChars
import com.thewizrd.shared_resources.utils.Units
import com.thewizrd.shared_resources.utils.getColorFromTempF
import com.thewizrd.simpleweather.BuildConfig
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.preferences.SettingsActivity
import com.thewizrd.simpleweather.setup.SetupActivity
import com.thewizrd.simpleweather.ui.components.*
import com.thewizrd.simpleweather.ui.navigation.Screen
import com.thewizrd.simpleweather.ui.text.spannableStringToAnnotatedString
import com.thewizrd.simpleweather.ui.theme.findActivity
import com.thewizrd.simpleweather.ui.utils.LogCompositions
import com.thewizrd.simpleweather.viewmodels.WeatherNowState
import com.thewizrd.simpleweather.viewmodels.WeatherNowStateModel
import com.thewizrd.simpleweather.viewmodels.WeatherNowViewModel
import com.thewizrd.simpleweather.wearable.WearableListenerActivity

@Composable
fun WeatherNowScreen(
    navController: NavHostController,
    scrollState: ScrollState,
    focusRequester: FocusRequester,
    wNowViewModel: WeatherNowViewModel,
    uiState: WeatherNowState,
    weather: WeatherUiModel,
    alerts: List<WeatherAlertViewModel>,
    forecasts: List<ForecastItemViewModel>,
    hourlyForecasts: List<HourlyForecastItemViewModel>,
    hasMinutely: Boolean
) {
    val stateModel = viewModel<WeatherNowStateModel>()

    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val lifecycleOwner = LocalLifecycleOwner.current

    val scrollLoading by stateModel.isLoading.collectAsState()

    if (BuildConfig.DEBUG) {
        LogCompositions(tag = "WeatherNow", msg = "WeatherNowScreen")
    }

    LoadingContent(
        empty = uiState.isLoading && (uiState.noLocationAvailable || weather.location.isNullOrEmpty()) || scrollLoading,
        emptyContent = {
            Box(
                modifier = Modifier.fillMaxRectangle(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    trackColor = Color.Transparent
                )
            }
        },
        loading = uiState.isLoading,
        onRefresh = {
            wNowViewModel.refreshWeather(true)
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scrollableColumn(focusRequester, scrollState)
                .verticalScroll(scrollState)
        ) {
            Column(
                modifier = Modifier.padding(top = 24.dp, bottom = 48.dp)
            ) {
                if (uiState.noLocationAvailable) {
                    NoLocationsPrompt(activity)
                }
                if (uiState.showDisconnectedView) {
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
                            navController = navController
                        )
                    }
                    if (hourlyForecasts.isNotEmpty()) {
                        HourlyForecastPanels(
                            hourlyForecasts = hourlyForecasts,
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

                ChangeLocationButton(activity = activity)
                SettingsButton(activity = activity)
                OpenOnPhoneButton()
            }
        }

        LaunchedEffect(Unit) {
            lifecycleOwner.repeatOnLifecycle(state = Lifecycle.State.RESUMED) {
                focusRequester.requestFocus()
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
            painter = painterResource(R.drawable.ic_location_off_24dp),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(id = R.string.prompt_location_not_set),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.body1
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
            tint = MaterialTheme.colors.onSurfaceVariant
        )
        Text(
            text = stringResource(id = R.string.message_disconnected),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.button,
            color = MaterialTheme.colors.onSurfaceVariant
        )
    }
}

@Composable
private fun AlertsBox(navController: NavHostController) {
    Box(contentAlignment = Alignment.Center) {
        CompactButton(
            onClick = {
                navController.navigate(Screen.Alerts.route)
            },
            colors = ButtonDefaults.primaryButtonColors(
                backgroundColor = Color(0xFFFF4500)
            )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_error_white),
                contentDescription = null
            )
        }
    }
}

@Composable
private fun ColumnScope.WeatherLocation(
    locationName: String? = WeatherIcons.EM_DASH,
    isGPSLocation: Boolean = false
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(id = R.dimen.inner_layout_padding)
            )
            .defaultMinSize(minHeight = 48.dp)
            .wrapContentHeight(Alignment.CenterVertically),
        mainAxisAlignment = FlowMainAxisAlignment.Center,
        crossAxisAlignment = FlowCrossAxisAlignment.Center,
        mainAxisSpacing = 7.dp
    ) {
        if (isGPSLocation) {
            Icon(
                modifier = Modifier.size(18.dp),
                painter = painterResource(id = R.drawable.ic_place_white_24dp),
                contentDescription = null
            )
        }
        Text(
            modifier = Modifier.weight(1f),
            text = locationName ?: WeatherIcons.EM_DASH,
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            style = MaterialTheme.typography.button
        )
    }
}

@Composable
private fun IconTempRow(
    weatherIcon: String,
    curTemp: String?,
    tempUnit: String?
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        WeatherIcon(
            modifier = Modifier
                .height(60.dp)
                .weight(1f)
                .padding(end = 8.dp),
            alignment = IconAlignment.End,
            weatherIcon = weatherIcon,
            shouldAnimate = true
        )
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
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
        style = MaterialTheme.typography.caption1,
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
                .offset(x = 4.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = hiTemp ?: WeatherIcons.PLACEHOLDER,
                    style = MaterialTheme.typography.body1,
                    textAlign = TextAlign.End,
                    maxLines = 1
                )
                Icon(
                    modifier = Modifier
                        .size(30.dp)
                        .offset(x = (-4).dp),
                    painter = painterResource(id = R.drawable.wi_direction_up),
                    tint = Color(0xFFFF4500),
                    contentDescription = null
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .offset(x = (4).dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = loTemp ?: WeatherIcons.PLACEHOLDER,
                    style = MaterialTheme.typography.body1,
                    textAlign = TextAlign.End,
                    maxLines = 1
                )
                Icon(
                    modifier = Modifier
                        .size(30.dp)
                        .offset(x = (-4).dp),
                    painter = painterResource(id = R.drawable.wi_direction_down),
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
    val popData = remember(weather) {
        weather.weatherDetailsMap[WeatherDetailsType.POPCHANCE]
    }
    val windData = remember(weather) {
        weather.weatherDetailsMap[WeatherDetailsType.WINDSPEED]
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(id = R.dimen.list_item_padding))
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                navController.navigate(Screen.Details.route)
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (popData != null) {
            Row(
                modifier = Modifier.padding(4.dp)
            ) {
                Icon(
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 4.dp),
                    painter = painterResource(id = R.drawable.wi_umbrella),
                    tint = colorResource(R.color.colorPrimaryLight),
                    contentDescription = null
                )
                Text(
                    text = spannableStringToAnnotatedString(popData.value),
                    style = MaterialTheme.typography.caption1,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    color = colorResource(R.color.colorPrimaryLight)
                )
            }
        }
        if (popData != null && windData != null) {
            Spacer(modifier = Modifier.width(4.dp))
        }
        if (windData != null) {
            Row(
                modifier = Modifier.padding(4.dp)
            ) {
                Icon(
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 4.dp)
                        .rotate(windData.iconRotation.toFloat()),
                    painter = painterResource(id = R.drawable.wi_wind_direction),
                    tint = Color(0xFF20B2AA),
                    contentDescription = null
                )
                Text(
                    text = spannableStringToAnnotatedString(windData.value),
                    style = MaterialTheme.typography.caption1,
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
    val dialogScrollState = rememberScalingLazyListState(0)

    Dialog(
        showDialog = showDialog,
        onDismissRequest = { showDialog = false },
    ) {
        Alert(
            title = {
                Text(
                    text = "",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.onBackground
                )
            },
            positiveButton = {},
            negativeButton = {},
            scrollState = dialogScrollState
        ) {
            Text(
                modifier = Modifier.padding(
                    top = dimensionResource(id = R.dimen.header_top_padding),
                    bottom = 48.dp
                ),
                text = weatherSummary,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.caption1,
                color = MaterialTheme.colors.onBackground,
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
        style = MaterialTheme.typography.caption1,
        color = MaterialTheme.colors.onSurfaceVariant
    )
}

@Composable
private fun ForecastPanels(
    forecasts: List<ForecastItemViewModel>,
    navController: NavHostController
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
                    model = it
                )
            }
        }
    }
}

@Composable
private fun HourlyForecastPanels(
    hourlyForecasts: List<HourlyForecastItemViewModel>,
    navController: NavHostController
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
                HourlyForecastItem(model = it)
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
        style = MaterialTheme.typography.caption2
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
        style = MaterialTheme.typography.caption1
    )
}

@Composable
private fun ForecastsButton(
    navController: NavHostController
) {
    NavigationButton(
        label = stringResource(id = R.string.label_forecast),
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
        label = stringResource(id = R.string.label_hourlyforecast),
        iconDrawableId = R.drawable.ic_access_time_black_24dp
    ) {
        navController.navigate(Screen.Forecast.route)
    }
}

@Composable
private fun MinutelyForecastsButton(
    navController: NavHostController
) {
    NavigationButton(
        label = stringResource(id = R.string.label_precipitation),
        iconDrawableId = R.drawable.wi_raindrops
    ) {
        navController.navigate(Screen.Precipitation.route)
    }
}

@Composable
private fun DetailsButton(
    navController: NavHostController
) {
    NavigationButton(
        label = stringResource(id = R.string.label_details),
        iconDrawableId = R.drawable.ic_list_black_24dp
    ) {
        navController.navigate(Screen.Details.route)
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
        label = stringResource(id = R.string.action_settings),
        iconDrawableId = R.drawable.ic_settings_black_24dp
    ) {
        activity.startActivity(Intent(activity, SettingsActivity::class.java))
    }
}

@Composable
private fun OpenOnPhoneButton() {
    NavigationButton(
        label = stringResource(id = R.string.action_openonphone),
        iconDrawableId = R.drawable.open_on_phone
    ) {
        localBroadcastManager.sendBroadcast(
            Intent(WearableListenerActivity.ACTION_OPENONPHONE)
                .putExtra(WearableListenerActivity.EXTRA_SHOWANIMATION, true)
        )
    }
}

@Composable
private fun NavigationButton(
    label: String,
    @DrawableRes iconDrawableId: Int,
    contentDescription: String? = null,
    onClick: () -> Unit,
) {
    Chip(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = 16.dp),
        onClick = onClick,
        colors = ChipDefaults.secondaryChipColors(),
        label = {
            Text(text = label)
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