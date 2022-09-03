package com.thewizrd.simpleweather.ui.components.preferences

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import com.thewizrd.simpleweather.ui.tools.WearPreviewDevices

@Composable
fun WearSwitchPreference(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    WearSwitchPreference(
        title = title,
        subtitle = AnnotatedString(text = subtitle),
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
    )
}

@Composable
fun WearSwitchPreference(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: AnnotatedString,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    ToggleChip(
        modifier = modifier,
        label = {
            Text(
                text = title,
                maxLines = 1,
                softWrap = true,
                overflow = TextOverflow.Ellipsis
            )
        },
        secondaryLabel = {
            Text(
                modifier = Modifier.wrapContentHeight(),
                text = subtitle,
                maxLines = 10,
                softWrap = true,
                overflow = TextOverflow.Ellipsis
            )
        },
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        toggleControl = {
            Icon(
                imageVector = ToggleChipDefaults.switchIcon(checked = checked),
                contentDescription = if (checked) "On" else "Off",
            )
        },
        colors = ToggleChipDefaults.toggleChipColors(
            uncheckedToggleControlColor = ToggleChipDefaults.SwitchUncheckedIconColor
        ),
    )
}

@WearPreviewDevices
@Composable
private fun WearSwitchPreferencePreview() {
    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        var checked by remember { mutableStateOf(value = true) }

        WearSwitchPreference(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .fillMaxWidth()
                .wrapContentHeight(),
            title = "Preference Title",
            subtitle = AnnotatedString(text = "Summary for the preference"),
            checked = checked,
            onCheckedChange = { checked = it },
        )
    }
}