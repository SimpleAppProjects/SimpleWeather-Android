package com.thewizrd.simpleweather.main

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.databinding.Observable
import androidx.databinding.Observable.OnPropertyChangedCallback
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.*
import com.google.android.horologist.compose.layout.fadeAwayScalingLazyList
import com.thewizrd.common.BR
import com.thewizrd.common.controls.DetailItemViewModel
import com.thewizrd.common.controls.WeatherNowViewModel
import com.thewizrd.shared_resources.DateTimeConstants
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.simpleweather.fragments.SwipeDismissFragment
import com.thewizrd.simpleweather.ui.components.WeatherDetailItem
import com.thewizrd.simpleweather.ui.theme.WearAppTheme
import kotlinx.coroutines.flow.MutableStateFlow

class WeatherDetailsFragment : SwipeDismissFragment() {
    private val weatherView: WeatherNowViewModel by activityViewModels()

    private val detailsFlow = MutableStateFlow<Collection<DetailItemViewModel>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AnalyticsLogger.logEvent("WeatherDetails: onCreate")

        lifecycleScope.launchWhenStarted {
            detailsFlow.emit(weatherView.weatherDetailsMap.values.toList())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Use this to return your custom view for this Fragment
        val outerView = super.onCreateView(inflater, container, savedInstanceState) as ViewGroup

        outerView.addView(ComposeView(inflater.context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val listState = rememberScalingLazyListState()
                val detailsItems = detailsFlow.collectAsState()

                WearAppTheme {
                    Scaffold(
                        positionIndicator = {
                            PositionIndicator(
                                scalingLazyListState = listState,
                                modifier = Modifier
                            )
                        },
                        vignette = {
                            Vignette(vignettePosition = VignettePosition.TopAndBottom)
                        },
                        timeText = {
                            TimeText(
                                modifier = Modifier.fadeAwayScalingLazyList {
                                    listState
                                },
                                timeSource = TimeTextDefaults.timeSource(
                                    if (DateFormat.is24HourFormat(LocalContext.current)) {
                                        TimeTextDefaults.TimeFormat24Hours
                                    } else {
                                        DateTimeConstants.CLOCK_FORMAT_12HR
                                    }
                                )
                            )
                        }
                    ) {
                        ScalingLazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            state = listState,
                            anchorType = ScalingLazyListAnchorType.ItemStart
                        ) {
                            detailsItems.value.forEach {
                                item {
                                    WeatherDetailItem(model = it)
                                }
                            }
                        }
                    }
                }
            }
        })

        return outerView
    }

    override fun onResume() {
        super.onResume()
        AnalyticsLogger.logEvent("WeatherDetails: onResume")
        weatherView.addOnPropertyChangedCallback(propertyChangedCallback)
    }

    override fun onPause() {
        AnalyticsLogger.logEvent("WeatherDetails: onPause")
        weatherView.removeOnPropertyChangedCallback(propertyChangedCallback)
        super.onPause()
    }

    private val propertyChangedCallback = object : OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable, propertyId: Int) {
            runWithView {
                if (propertyId == BR.weatherDetailsMap) {
                    detailsFlow.emit(weatherView.weatherDetailsMap.values.toList())
                }
            }
        }
    }
}