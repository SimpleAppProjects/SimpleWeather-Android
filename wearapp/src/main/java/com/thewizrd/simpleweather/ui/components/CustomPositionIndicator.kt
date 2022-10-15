package com.thewizrd.simpleweather.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.ScalingLazyListState

@Composable
fun CustomPositionIndicator(
    visible: Boolean,
    scrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        PositionIndicator(
            modifier = modifier,
            scrollState = scrollState
        )
    }
}

@Composable
fun CustomPositionIndicator(
    visible: Boolean,
    lazyListState: LazyListState,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        PositionIndicator(
            modifier = modifier,
            lazyListState = lazyListState
        )
    }
}

@Composable
fun CustomPositionIndicator(
    visible: Boolean,
    scalingLazyListState: ScalingLazyListState,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        PositionIndicator(
            modifier = modifier,
            scalingLazyListState = scalingLazyListState
        )
    }
}