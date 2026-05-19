package com.thewizrd.simpleweather.ui.setup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.wear.R as wearRes
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ConfirmationDialog
import androidx.wear.compose.material3.ConfirmationDialogDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.LocalContentColor
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.thewizrd.common.helpers.locationPermissionEnabled
import com.thewizrd.common.utils.ErrorMessage
import com.thewizrd.common.viewmodels.LocationSearchViewModel
import com.thewizrd.shared_resources.R as sharedRes
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.simpleweather.BuildConfig
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.ui.compose.CircularWavyProgressIndicator
import com.thewizrd.simpleweather.ui.compose.tools.WearPreviewDevices
import com.thewizrd.simpleweather.ui.helpers.rememberLocationPermissionLauncher
import com.thewizrd.simpleweather.ui.navigation.Screen
import com.thewizrd.simpleweather.ui.theme.activityViewModel
import com.thewizrd.simpleweather.ui.utils.rememberFocusRequester

@Composable
fun SetupScreen(
    navController: NavController,
    focusRequester: FocusRequester
) {
    val locationSearchViewModel = activityViewModel<LocationSearchViewModel>()
    val isLoading by locationSearchViewModel.isLoading.collectAsState()
    var errorMessage by remember { mutableStateOf<ErrorMessage?>(null) }

    SetupScreen(
        navController = navController,
        focusRequester = focusRequester,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onFetchGeoLocation = {
            locationSearchViewModel.fetchGeoLocation()
        }
    )

    LaunchedEffect(Unit) {
        locationSearchViewModel.errorMessages.collect {
            val error = it.firstOrNull()
            errorMessage = error

            if (error != null) {
                locationSearchViewModel.setErrorMessageShown(error)
            }
        }
    }
}

@Composable
private fun SetupScreen(
    navController: NavController,
    focusRequester: FocusRequester,
    isLoading: Boolean = false,
    errorMessage: ErrorMessage? = null,
    onFetchGeoLocation: () -> Unit = {},
) {
    val context = LocalContext.current

    val scrollState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    var showSetupSyncDialog by remember { mutableStateOf(false) }
    var showLocationDeniedDialog by remember { mutableStateOf(false) }
    var showErrorMessageDialog by remember(errorMessage) { mutableStateOf(errorMessage != null) }

    val locationPermissionLauncher = rememberLocationPermissionLauncher(
        locationCallback = { granted ->
            if (granted) {
                // permission was granted, yay!
                // Do the task you need to do.
                onFetchGeoLocation()
            } else {
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
                settingsManager.setFollowGPS(false)
                showLocationDeniedDialog = true
            }
        }
    )

    fun fetchGeoLocation() {
        if (!context.locationPermissionEnabled()) {
            locationPermissionLauncher.requestLocationPermission()
            return
        }

        onFetchGeoLocation()
    }

    ScreenScaffold(scrollState = scrollState) { contentPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                CircularWavyProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        } else {
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
                    ListHeader {
                        Text(text = stringResource(id = sharedRes.string.app_name))
                    }
                }
                item {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec),
                        colors = ButtonDefaults.filledTonalButtonColors(),
                        onClick = {
                            navController.navigate(Screen.SetupLocationSearch.route)
                        },
                        label = {
                            Text(
                                text = stringResource(id = sharedRes.string.location_search_hint),
                                color = LocalContentColor.current
                            )
                        },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_search_black_24dp),
                                contentDescription = stringResource(id = android.R.string.search_go)
                            )
                        }
                    )
                }
                item {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec),
                        colors = ButtonDefaults.filledTonalButtonColors(),
                        onClick = {
                            fetchGeoLocation()
                        },
                        label = {
                            Text(text = stringResource(id = sharedRes.string.label_gpsfollow))
                        },
                        icon = {
                            Icon(
                                painter = painterResource(id = sharedRes.drawable.ic_my_location_white_24dp),
                                contentDescription = stringResource(id = sharedRes.string.label_gpsfollow)
                            )
                        }
                    )
                }
                if (!BuildConfig.IS_NONGMS) {
                    item {
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .transformedHeight(this, transformationSpec),
                            transformation = SurfaceTransformation(transformationSpec),
                            colors = ButtonDefaults.filledTonalButtonColors(),
                            onClick = {
                                showSetupSyncDialog = true
                            },
                            label = {
                                Text(text = stringResource(id = R.string.action_setupfromphone))
                            },
                            icon = {
                                Icon(
                                    modifier = Modifier.size(ButtonDefaults.IconSize),
                                    painter = painterResource(id = com.google.android.gms.base.R.drawable.common_full_open_on_phone),
                                    contentDescription = stringResource(id = R.string.action_setupfromphone)
                                )
                            }
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }

            ConfirmationDialog(
                visible = showLocationDeniedDialog,
                onDismissRequest = {
                    showLocationDeniedDialog = false
                },
                text = {
                    Text(
                        text = stringResource(id = sharedRes.string.error_location_denied),
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                colors = ConfirmationDialogDefaults.failureColors(),
                content = {
                    Icon(
                        modifier = Modifier.size(ConfirmationDialogDefaults.SmallIconSize),
                        painter = painterResource(id = sharedRes.drawable.ic_location_off_24dp),
                        contentDescription = null
                    )
                }
            )

            ConfirmationDialog(
                visible = showErrorMessageDialog,
                onDismissRequest = {
                    showErrorMessageDialog = false
                },
                text = {
                    Text(
                        text = when (errorMessage) {
                            is ErrorMessage.Resource -> {
                                stringResource(id = errorMessage.stringId)
                            }

                            is ErrorMessage.String -> {
                                errorMessage.message
                            }

                            is ErrorMessage.WeatherError -> {
                                errorMessage.exception.message
                            }

                            else -> stringResource(sharedRes.string.werror_unknown)
                        },
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                colors = ConfirmationDialogDefaults.failureColors(),
                content = {
                    Icon(
                        modifier = Modifier.size(ConfirmationDialogDefaults.SmallIconSize),
                        painter = painterResource(id = sharedRes.drawable.ic_error_white),
                        contentDescription = null
                    )
                }
            )

            AlertDialog(
                visible = showSetupSyncDialog,
                onDismissRequest = {
                    showSetupSyncDialog = false
                },
                title = {},
                text = {
                    Text(text = stringResource(id = R.string.prompt_confirmsetup))
                },
                confirmButton = {
                    AlertDialogDefaults.ConfirmButton(onClick = {
                        navController.navigate(Screen.SetupSync.route)
                    })
                }
            )
        }
    }
}

@WearPreviewDevices
@Composable
private fun PreviewSetupScreen() {
    SetupScreen(
        navController = rememberSwipeDismissableNavController(),
        focusRequester = rememberFocusRequester(),
        isLoading = false
    )
}

@WearPreviewDevices
@Composable
private fun PreviewSetupScreenLoading() {
    SetupScreen(
        navController = rememberSwipeDismissableNavController(),
        focusRequester = rememberFocusRequester(),
        isLoading = true
    )
}