package com.thewizrd.simpleweather.ui.components.preferences

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Text
import com.thewizrd.simpleweather.ui.text.toAnnotatedString
import com.thewizrd.simpleweather.ui.tools.WearPreviewDevices

@Composable
fun WearPreference(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    WearPreference(
        title = title,
        subtitle = subtitle?.toAnnotatedString(),
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    )
}

@Composable
fun WearPreference(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: AnnotatedString? = null,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Chip(
        modifier = modifier,
        label = {
            Text(
                text = title,
                maxLines = 1
            )
        },
        secondaryLabel = {
            subtitle?.let {
                Text(
                    text = it,
                    maxLines = 10,
                    softWrap = true,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        onClick = onClick,
        enabled = enabled,
        colors = ChipDefaults.secondaryChipColors()
    )
}

@WearPreviewDevices
@Composable
private fun WearPreferencePreview() {
    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        WearPreference(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .fillMaxWidth()
                .wrapContentHeight(),
            title = "Preference Title",
            subtitle = AnnotatedString(text = "Summary for the preference"),
            onClick = { },
            enabled = true
        )
    }
}