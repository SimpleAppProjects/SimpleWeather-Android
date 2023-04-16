package com.thewizrd.simpleweather.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.contentColorFor
import androidx.wear.compose.material.dialog.Alert as WearAlert

@Composable
fun Alert(
    title: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (ColumnScope.() -> Unit)? = null,
    message: @Composable (ColumnScope.() -> Unit)? = null,
    scrollState: ScalingLazyListState = rememberScalingLazyListState(),
    backgroundColor: Color = MaterialTheme.colors.background,
    titleColor: Color = contentColorFor(backgroundColor),
    messageColor: Color = contentColorFor(backgroundColor),
    iconColor: Color = contentColorFor(backgroundColor),
    verticalArrangement: Arrangement.Vertical = AlertDefaults.VerticalArrangement,
    contentPadding: PaddingValues = AlertDefaults.ContentPadding,
    content: ScalingLazyListScope.() -> Unit = {}
) {
    WearAlert(
        title,
        modifier,
        icon,
        message,
        scrollState,
        backgroundColor,
        titleColor,
        messageColor,
        iconColor,
        verticalArrangement,
        contentPadding,
        content
    )
}

public object AlertDefaults {
    public val ContentPadding =
        PaddingValues(start = 10.dp, end = 10.dp, top = 24.dp, bottom = 52.dp)
    public val VerticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Top)
}