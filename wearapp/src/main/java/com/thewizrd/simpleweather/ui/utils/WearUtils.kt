package com.thewizrd.simpleweather.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

@Composable
public fun calcInsetPadding(): Dp {
    val isRound = LocalConfiguration.current.isScreenRound
    var inset: Dp = 0.dp

    if (isRound) {
        val screenHeightDp = LocalConfiguration.current.screenHeightDp
        val screenWidthDp = LocalConfiguration.current.smallestScreenWidthDp
        val maxSquareEdge = (sqrt(((screenHeightDp * screenWidthDp) / 2).toDouble()))
        inset = Dp(((screenHeightDp - maxSquareEdge) / 2).toFloat())
    }

    return inset
}