package com.thewizrd.simpleweather.ui.components

import android.content.res.Configuration
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.TimeTextDefaults
import androidx.wear.compose.material.curvedText
import com.thewizrd.simpleweather.BuildConfig

/**
 * Custom version of TimeText (Curved Text) that enables leading text (if wanted) and hides while
 * scrolling so user can just focus on the list's items.
 */
@Composable
fun CustomTimeText(
    visible: Boolean,
    modifier: Modifier = Modifier,
    startText: String? = null,
) {
    val textStyle = TimeTextDefaults.timeTextStyle()
    val debugWarning = remember {
        val isEmulator = Build.PRODUCT.startsWith("sdk_gwear")

        if (BuildConfig.DEBUG && !isEmulator) {
            "Debug (slower)"
        } else {
            null
        }
    }
    val showWarning = debugWarning != null
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        val visibleText = startText != null
        TimeText(
            modifier = modifier,
            startCurvedContent = if (visibleText) {
                {
                    curvedText(
                        text = startText!!,
                        style = CurvedTextStyle(textStyle)
                    )
                }
            } else null,
            startLinearContent = if (visibleText) {
                {
                    Text(
                        text = startText!!,
                        style = textStyle
                    )
                }
            } else null,
            // Trailing text is against Wear UX guidance, used here just for development.
            endCurvedContent = if (showWarning) {
                {
                    curvedText(
                        text = debugWarning!!,
                        style = CurvedTextStyle(textStyle),
                        color = Color.Red
                    )
                }
            } else null,
            endLinearContent = if (showWarning) {
                {
                    Text(
                        text = debugWarning!!,
                        style = textStyle,
                        color = Color.Red
                    )
                }
            } else null,
        )
    }
}

@Preview(
    apiLevel = 26,
    uiMode = Configuration.UI_MODE_TYPE_WATCH,
    showSystemUi = true,
    device = Devices.WEAR_OS_LARGE_ROUND
)
@Preview(
    apiLevel = 26,
    uiMode = Configuration.UI_MODE_TYPE_WATCH,
    showSystemUi = true,
    device = Devices.WEAR_OS_SQUARE
)
@Preview(
    apiLevel = 26,
    uiMode = Configuration.UI_MODE_TYPE_WATCH,
    showSystemUi = true,
    device = Devices.WEAR_OS_SMALL_ROUND
)
// This will only be rendered properly in AS Chipmunk and beyond
@Composable
fun PreviewCustomTimeText() {
    CustomTimeText(
        visible = true,
        startText = "Testing Leading Text..."
    )
}