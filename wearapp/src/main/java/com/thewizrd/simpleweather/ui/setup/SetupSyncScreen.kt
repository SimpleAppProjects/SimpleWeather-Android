package com.thewizrd.simpleweather.ui.setup

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.CircularProgressIndicatorDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.touchTargetAwareSize
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.thewizrd.common.utils.ErrorMessage
import com.thewizrd.shared_resources.R as sharedRes
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.utils.ContextUtils.isLargeWatch
import com.thewizrd.shared_resources.wearable.WearableDataSync
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.main.MainActivity
import com.thewizrd.simpleweather.ui.components.ConfirmationOverlay
import com.thewizrd.simpleweather.ui.compose.tools.WearPreviewDevices
import com.thewizrd.simpleweather.ui.theme.findActivity
import com.thewizrd.simpleweather.ui.utils.rememberFocusRequester
import com.thewizrd.simpleweather.viewmodels.ConfirmationViewModel
import com.thewizrd.simpleweather.viewmodels.SetupSyncState
import com.thewizrd.simpleweather.viewmodels.SetupSyncViewModel
import com.thewizrd.simpleweather.wearable.WearableListenerActions.ACTION_SHOWSTORELISTING
import com.thewizrd.simpleweather.wearable.WearableListenerActions.ACTION_UPDATESYNCSTATUS
import com.thewizrd.simpleweather.wearable.WearableListenerActions.EXTRA_SUCCESS
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun SetupSyncScreen(
    navController: NavController,
    focusRequester: FocusRequester
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val lifecycleOwner = LocalLifecycleOwner.current

    val setupSyncViewModel = viewModel<SetupSyncViewModel>()
    val uiState by setupSyncViewModel.uiState.collectAsState()

    val confirmationViewModel = viewModel<ConfirmationViewModel>()
    val confirmationData by confirmationViewModel.confirmationEventsFlow.collectAsState()

    SetupSyncScreen(
        navController = navController,
        focusRequester = focusRequester,
        uiState = uiState
    )

    ConfirmationOverlay(
        confirmationData = confirmationData,
        onTimeout = { confirmationViewModel.clearFlow() }
    )

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        lifecycleOwner.lifecycleScope.launch {
            setupSyncViewModel.sendSetupStatusRequest()
        }
    }

    LifecycleResumeEffect(activity) {
        val job = lifecycleOwner.lifecycleScope.launch {
            setupSyncViewModel.eventFlow.collect { (eventType, data) ->
                when (eventType) {
                    ACTION_UPDATESYNCSTATUS -> {
                        val success = data.getBoolean(EXTRA_SUCCESS, false)

                        if (isActive) {
                            if (success) {
                                if (settingsManager.getHomeData() != null) {
                                    settingsManager.setDataSync(WearableDataSync.DEVICEONLY)
                                    settingsManager.setWeatherLoaded(true)
                                    // Start WeatherNow Activity
                                    activity.startActivity(
                                        Intent(
                                            activity,
                                            MainActivity::class.java
                                        )
                                    )
                                    activity.finishAffinity()
                                }
                            } else {
                                // Go back
                                navController.popBackStack()
                            }
                        }
                    }

                    ACTION_SHOWSTORELISTING -> {
                        setupSyncViewModel.openPlayStore(showAnimation = true)
                    }
                }
            }
        }

        onPauseOrDispose {
            job.cancel()
        }
    }

    LifecycleResumeEffect(activity) {
        val job = lifecycleOwner.lifecycleScope.launch {
            setupSyncViewModel.errorMessagesFlow.collect { error ->
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
}

@Composable
private fun SetupSyncScreen(
    navController: NavController,
    focusRequester: FocusRequester,
    uiState: SetupSyncState
) {
    val context = LocalContext.current

    ScreenScaffold { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ListHeader {
                Text(text = stringResource(id = uiState.messageStringResId))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
                contentAlignment = Alignment.Center
            ) {
                val buttonSize = remember(context) {
                    if (context.isLargeWatch()) IconButtonDefaults.LargeButtonSize else IconButtonDefaults.SmallButtonSize
                }

                IconButton(
                    modifier = Modifier.touchTargetAwareSize(buttonSize),
                    colors = IconButtonDefaults.filledIconButtonColors(),
                    onClick = {
                        // Cancel timer job
                        navController.popBackStack()
                    },
                ) {
                    Icon(
                        painter = painterResource(sharedRes.drawable.ic_close_white_24dp),
                        contentDescription = stringResource(android.R.string.cancel)
                    )
                }

                if (uiState.progressBarState.isIndeterminate) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(
                            buttonSize + CircularProgressIndicatorDefaults.IndeterminateStrokeWidth
                        ),
                        strokeWidth = CircularProgressIndicatorDefaults.IndeterminateStrokeWidth,
                        colors = ProgressIndicatorDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.onBackground,
                            trackColor = Color.Transparent,
                        )
                    )
                } else {
                    val transition = rememberInfiniteTransition()
                    val currentProgress by transition.animateFloat(
                        0f,
                        1f,
                        infiniteRepeatable(
                            animation = tween(
                                durationMillis = uiState.progressBarState.timeInMillis,
                                easing = LinearEasing,
                                delayMillis = 0
                            )
                        )
                    )

                    CircularProgressIndicator(
                        modifier = Modifier.size(
                            buttonSize + CircularProgressIndicatorDefaults.IndeterminateStrokeWidth
                        ),
                        strokeWidth = CircularProgressIndicatorDefaults.IndeterminateStrokeWidth,
                        colors = ProgressIndicatorDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.onBackground,
                            trackColor = Color.Transparent,
                        ),
                        progress = { currentProgress }
                    )
                }
            }
        }
    }
}

@WearPreviewDevices
@Composable
private fun PreviewSetupSyncScreen() {
    SetupSyncScreen(
        navController = rememberSwipeDismissableNavController(),
        focusRequester = rememberFocusRequester(),
        uiState = SetupSyncState()
    )
}